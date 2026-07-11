package net.celestiald.cavesnotcliffs.migration;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CubicImportJournalTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void persistsEveryMonotonicImportStateAndDimensionCommit() throws Exception {
        Path world = temporary.newFolder("world").toPath();
        Path dimension = world.resolve("DIM-1");
        Path source = dimension.resolve("region2d/0.0.2dr");
        Files.createDirectories(source.getParent());
        Files.write(source, "source".getBytes(StandardCharsets.UTF_8));
        List<CubicImportJournal.FileRecord> sources = CubicImportJournal.captureSources(
                world, Collections.singletonList(dimension));

        CubicImportJournal journal = CubicImportJournal.create(2, sources,
                Arrays.asList(".", "DIM-1"));
        Path path = world.resolve(CubicImportJournal.FILE_NAME);
        journal.writeAtomic(path);

        CubicImportJournal loaded = CubicImportJournal.read(path);
        assertEquals(CubicImportJournal.State.DISCOVERED, loaded.getState());
        assertEquals(2, loaded.getTerrainSchema());
        assertEquals(sources, loaded.getSources());
        assertEquals(Arrays.asList(".", "DIM-1"), loaded.getDimensions());

        loaded.recordDimension(".", 10L, 240L, Collections.emptyList());
        loaded.recordDimension("DIM-1", 3L, 48L, Collections.emptyList());
        loaded.transition(CubicImportJournal.State.DISCOVERED,
                CubicImportJournal.State.STAGED);
        loaded.transition(CubicImportJournal.State.STAGED,
                CubicImportJournal.State.VERIFIED);
        loaded.transition(CubicImportJournal.State.VERIFIED,
                CubicImportJournal.State.COMMITTING);
        loaded.markDimensionCommitted(".");
        assertTrue(loaded.isDimensionCommitted("."));
        assertFalse(loaded.isDimensionCommitted("DIM-1"));
        loaded.markDimensionCommitted("DIM-1");
        loaded.transition(CubicImportJournal.State.COMMITTING,
                CubicImportJournal.State.COMMITTED);
        loaded.writeAtomic(path);

        CubicImportJournal complete = CubicImportJournal.read(path);
        assertEquals(CubicImportJournal.State.COMMITTED, complete.getState());
        assertTrue(complete.isDimensionCommitted("."));
        assertTrue(complete.isDimensionCommitted("DIM-1"));
        assertFalse(Files.exists(world.resolve(CubicImportJournal.TEMP_FILE_NAME)));
    }

    @Test
    public void sourceFingerprintChangesWhenBytesChange() throws Exception {
        Path world = temporary.newFolder("fingerprints").toPath();
        Path source = world.resolve("region3d/0.0.0.3dr");
        Files.createDirectories(source.getParent());
        Files.write(source, "one".getBytes(StandardCharsets.UTF_8));
        List<CubicImportJournal.FileRecord> before = CubicImportJournal.captureSources(
                world, Collections.singletonList(world));

        Files.write(source, "two".getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        List<CubicImportJournal.FileRecord> after = CubicImportJournal.captureSources(
                world, Collections.singletonList(world));

        assertEquals(1, before.size());
        assertEquals(1, after.size());
        assertFalse(before.equals(after));
        assertFalse(before.get(0).getSha256().equals(after.get(0).getSha256()));
    }

    @Test
    public void refusesSkippedOrOutOfOrderTransitions() throws Exception {
        CubicImportJournal journal = CubicImportJournal.create(2,
                Collections.singletonList(new CubicImportJournal.FileRecord(
                        "region2d/0.0.2dr", 1L, 2L, "00")),
                Collections.singletonList("."));
        try {
            journal.transition(CubicImportJournal.State.DISCOVERED,
                    CubicImportJournal.State.VERIFIED);
            fail("Expected skipped transition rejection");
        } catch (java.io.IOException expected) {
            assertTrue(expected.getMessage().contains("Invalid cubic import journal transition"));
        }
    }

    @Test
    public void rejectsUnsafePathsAndMalformedFingerprintsOnRead() throws Exception {
        Path world = temporary.newFolder("invalid-journal").toPath();
        CubicImportJournal journal = CubicImportJournal.create(2,
                Collections.singletonList(new CubicImportJournal.FileRecord(
                        "../region2d/0.0.2dr", 1L, 2L, "00")),
                Collections.singletonList("../backup"));
        Path path = world.resolve(CubicImportJournal.FILE_NAME);
        journal.writeAtomic(path);

        try {
            CubicImportJournal.read(path);
            fail("Expected malformed journal rejection");
        } catch (java.io.IOException expected) {
            assertTrue(expected.getMessage().contains("invalid source file record"));
        }
    }
}

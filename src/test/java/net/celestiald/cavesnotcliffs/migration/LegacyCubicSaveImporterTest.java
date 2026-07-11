package net.celestiald.cavesnotcliffs.migration;

import net.celestiald.cavesnotcliffs.world.CavesNotCliffsWorldData;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LegacyCubicSaveImporterTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void commitsOnceAndLeavesCubicRollbackSourcesUntouched() throws Exception {
        Path world = temporary.newFolder("world").toPath();
        writeCubicMetadata(world);
        CubicDimensionStagerTest.writeExtension(
                world.resolve("region2d/0.0.2dr.ext"), 0,
                CubicDimensionStagerTest.column(0, 0));
        for (int cubeY = -4; cubeY < 20; cubeY++) {
            CubicDimensionStagerTest.writeCube(world, 0, cubeY, 0,
                    CubicDimensionStagerTest.cube(0, cubeY, 0, true, true));
        }

        assertTrue(LegacyCubicSaveImporter.importWorld(
                world, CavesNotCliffsWorldData.CURRENT_SCHEMA, 99L));
        assertTrue(Files.isDirectory(world.resolve("region")));
        assertTrue(Files.isRegularFile(world.resolve("region/r.0.0.mca")));
        assertTrue(Files.isDirectory(world.resolve("region2d")));
        assertTrue(Files.isDirectory(world.resolve("region3d")));
        assertFalse(Files.exists(world.resolve(".cavesnotcliffs-cubic-staging")));

        CubicImportJournal journal = CubicImportJournal.read(
                world.resolve(CubicImportJournal.FILE_NAME));
        assertTrue(journal.getState() == CubicImportJournal.State.COMMITTED);
        assertTrue(journal.isDimensionCommitted("."));
        assertFalse(LegacyCubicSaveImporter.importWorld(
                world, CavesNotCliffsWorldData.CURRENT_SCHEMA, 100L));
    }

    @Test
    public void blocksStartupForAnIncompleteJournal() throws Exception {
        Path world = temporary.newFolder("incomplete").toPath();
        writeCubicMetadata(world);
        Path source = world.resolve("region2d/0.0.2dr");
        Files.createDirectories(source.getParent());
        Files.write(source, "source".getBytes(StandardCharsets.UTF_8));
        List<CubicImportJournal.FileRecord> sources = CubicImportJournal.captureSources(
                world, Collections.singletonList(world));
        CubicImportJournal.create(2, sources, Collections.singletonList("."))
                .writeAtomic(world.resolve(CubicImportJournal.FILE_NAME));

        try {
            LegacyCubicSaveImporter.importWorld(world, 2, 0L);
            fail("Expected incomplete journal rejection");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("incomplete at state DISCOVERED"));
            assertTrue(expected.getMessage().contains("Source region2d/region3d files were not modified"));
        }
    }

    @Test
    public void neverOverwritesPreexistingFiniteRegionStorage() throws Exception {
        Path world = temporary.newFolder("existing-target").toPath();
        writeCubicMetadata(world);
        Path source = world.resolve("region2d/source-marker");
        Files.createDirectories(source.getParent());
        Files.write(source, "source".getBytes(StandardCharsets.UTF_8));
        Files.createDirectories(world.resolve("region"));

        try {
            LegacyCubicSaveImporter.importWorld(world, 2, 0L);
            fail("Expected existing target rejection");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("Refusing to overwrite existing finite region"));
        }
        assertFalse(Files.exists(world.resolve(CubicImportJournal.FILE_NAME)));
    }

    @Test
    public void verifiesCommittedOutputHashesBeforeMetadataCleanup() throws Exception {
        Path world = temporary.newFolder("verified-output").toPath();
        writeCubicMetadata(world);
        CubicDimensionStagerTest.writeExtension(
                world.resolve("region2d/0.0.2dr.ext"), 0,
                CubicDimensionStagerTest.column(0, 0));
        for (int cubeY = -4; cubeY < 20; cubeY++) {
            CubicDimensionStagerTest.writeCube(world, 0, cubeY, 0,
                    CubicDimensionStagerTest.cube(0, cubeY, 0, true, true));
        }
        assertTrue(LegacyCubicSaveImporter.importWorld(
                world, CavesNotCliffsWorldData.CURRENT_SCHEMA, 0L));
        CubicImportJournal journal = CubicImportJournal.read(
                world.resolve(CubicImportJournal.FILE_NAME));
        LegacyCubicSaveImporter.verifyCommittedTargets(world, journal, true);

        Files.write(world.resolve("region/r.0.0.mca"), new byte[] {1},
                java.nio.file.StandardOpenOption.APPEND);
        try {
            LegacyCubicSaveImporter.verifyCommittedTargets(world, journal, true);
            fail("Expected changed output rejection");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains(
                    "changed before legacy metadata cleanup"));
        }
    }

    private static void writeCubicMetadata(Path world) throws IOException {
        LegacyCubicDimensionMetadataTest.writeMetadata(world, true, -64, 320,
                "cubicchunks:anvil3d", "cubicchunks:default");
    }
}

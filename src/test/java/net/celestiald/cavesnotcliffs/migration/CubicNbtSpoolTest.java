package net.celestiald.cavesnotcliffs.migration;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CubicNbtSpoolTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void reloadsOneSortedColumnAtATimeAndReleasesItsIndex() throws Exception {
        Path path = temporary.newFile("cubes.spool").toPath();
        try (CubicNbtSpool spool = new CubicNbtSpool(path)) {
            spool.append(-2, 4, 9, tag("four"));
            spool.append(3, 0, -7, tag("other"));
            spool.append(-2, -4, 9, tag("minus-four"));
            spool.append(-2, 19, 9, tag("nineteen"));
            assertEquals(4L, spool.getCubeCount());
            assertTrue(spool.containsColumn(-2, 9));
            spool.force();

            Map<Integer, NBTTagCompound> column = spool.takeColumn(-2, 9);
            assertEquals(3, column.size());
            assertEquals("minus-four", column.get(-4).getString("value"));
            assertEquals("four", column.get(4).getString("value"));
            assertEquals("nineteen", column.get(19).getString("value"));
            assertFalse(spool.containsColumn(-2, 9));

            List<CubicNbtSpool.ColumnKey> remaining = spool.remainingColumns();
            assertEquals(1, remaining.size());
            assertEquals(3, remaining.get(0).getX());
            assertEquals(-7, remaining.get(0).getZ());
        }
    }

    @Test
    public void rejectsDuplicateCoordinatesBeforeWritingAnotherRecord() throws Exception {
        Path path = temporary.newFile("duplicates.spool").toPath();
        try (CubicNbtSpool spool = new CubicNbtSpool(path)) {
            spool.append(1, -1, 2, tag("first"));
            try {
                spool.append(1, -1, 2, tag("second"));
                fail("Expected duplicate cube rejection");
            } catch (java.io.IOException expected) {
                assertTrue(expected.getMessage().contains("Duplicate cubic NBT record"));
            }
            assertEquals(1L, spool.getCubeCount());
        }
    }

    @Test
    public void rejectsOversizedRecordWithoutLeavingFileOrIndexState() throws Exception {
        NBTTagCompound value = tag("bounded");
        int compressedSize = compressedSize(value);
        Path path = temporary.newFile("record-limit.spool").toPath();
        try (CubicNbtSpool spool = new CubicNbtSpool(
                path, compressedSize - 1, Long.MAX_VALUE)) {
            try {
                spool.append(1, 2, 3, value);
                fail("Expected compressed record limit rejection");
            } catch (IOException expected) {
                assertTrue(expected.getMessage().contains("record limit"));
            }
            assertEquals(0L, Files.size(path));
            assertEquals(0L, spool.getCubeCount());
            assertFalse(spool.containsColumn(1, 3));
            assertTrue(spool.remainingColumns().isEmpty());
        }
    }

    @Test
    public void rejectsTotalOverflowAtomicallyAndKeepsFirstRecordReadable() throws Exception {
        NBTTagCompound first = tag("first");
        long oneRecordBytes = Integer.BYTES + compressedSize(first);
        Path path = temporary.newFile("total-limit.spool").toPath();
        try (CubicNbtSpool spool = new CubicNbtSpool(
                path, 1024, oneRecordBytes)) {
            spool.append(1, 2, 3, first);
            long lengthAfterFirst = Files.size(path);
            try {
                spool.append(4, 5, 6, tag("second"));
                fail("Expected total spool limit rejection");
            } catch (IOException expected) {
                assertTrue(expected.getMessage().contains("total limit"));
            }
            assertEquals(lengthAfterFirst, Files.size(path));
            assertEquals(1L, spool.getCubeCount());
            assertTrue(spool.containsColumn(1, 3));
            assertFalse(spool.containsColumn(4, 6));
            assertEquals(1, spool.remainingColumns().size());

            spool.force();
            Map<Integer, NBTTagCompound> column = spool.takeColumn(1, 3);
            assertEquals(1, column.size());
            assertEquals("first", column.get(2).getString("value"));
        }
    }

    @Test
    public void requiresForceBeforeReadingAndDisallowsLaterAppends() throws Exception {
        Path path = temporary.newFile("force.spool").toPath();
        try (CubicNbtSpool spool = new CubicNbtSpool(path)) {
            spool.append(0, 0, 0, tag("value"));
            try {
                spool.takeColumn(0, 0);
                fail("Expected pre-force read rejection");
            } catch (java.io.IOException expected) {
                assertTrue(expected.getMessage().contains("must be forced"));
            }
            spool.force();
            try {
                spool.append(0, 1, 0, tag("late"));
                fail("Expected post-force append rejection");
            } catch (java.io.IOException expected) {
                assertTrue(expected.getMessage().contains("Cannot append"));
            }
        }
    }

    private static NBTTagCompound tag(String value) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("value", value);
        return tag;
    }

    private static int compressedSize(NBTTagCompound tag) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(tag, output);
        return output.size();
    }
}

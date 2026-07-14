package net.celestiald.cavesnotcliffs.migration.cubic;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CubicRegionReaderTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void readsNegativeColumnRegionWithXMajorEntryIds() throws Exception {
        Path directory = temporary.newFolder("region2d").toPath();
        int entryId = (5 << 5) | 7;
        Path region = directory.resolve("-2.3.2dr");
        writeRegion(region, 1024, Collections.singletonMap(entryId, nbt(-59, null, 103, "column")));

        List<CubicRegionEntry> entries = collectColumns(directory);

        assertEquals(1, entries.size());
        CubicRegionEntry entry = entries.get(0);
        assertEquals(CubicRegionEntry.Kind.COLUMN, entry.getKind());
        assertEquals(CubicRegionEntry.Storage.REGION, entry.getStorage());
        assertEquals(-2, entry.getRegionX());
        assertFalse(entry.getRegionY().isPresent());
        assertEquals(3, entry.getRegionZ());
        assertEquals(entryId, entry.getEntryId());
        assertEquals(-59, entry.getX());
        assertFalse(entry.getY().isPresent());
        assertEquals(103, entry.getZ());
        assertEquals(8, entry.getSectorOffset().getAsInt());
        assertEquals(1, entry.getSectorCount().getAsInt());
        assertEquals("column (-59, 103)", entry.describeCoordinates());
        assertEquals("column", entry.getNbt().getString("marker"));
    }

    @Test
    public void readsCubeRegionWithXMajorEntryIds() throws Exception {
        Path directory = temporary.newFolder("region3d").toPath();
        int entryId = (4 << 8) | (6 << 4) | 9;
        Path region = directory.resolve("2.-1.-3.3dr");
        writeRegion(region, 4096, Collections.singletonMap(entryId, nbt(36, -10, -39, "cube")));

        List<CubicRegionEntry> entries = collectCubes(directory);

        assertEquals(1, entries.size());
        CubicRegionEntry entry = entries.get(0);
        assertEquals(CubicRegionEntry.Kind.CUBE, entry.getKind());
        assertEquals(36, entry.getX());
        assertEquals(-10, entry.getY().getAsInt());
        assertEquals(-39, entry.getZ());
        assertEquals(32, entry.getSectorOffset().getAsInt());
        assertEquals("cube (36, -10, -39)", entry.describeCoordinates());
    }

    @Test
    public void usesNumericExtensionEntriesOnlyWhenNormalEntryIsAbsent() throws Exception {
        Path directory = temporary.newFolder("region2d-ext").toPath();
        Path region = directory.resolve("0.0.2dr");
        writeRegion(region, 1024, Collections.singletonMap(1, nbt(0, null, 1, "normal")));

        Path extension = Files.createDirectory(directory.resolve("0.0.2dr.ext"));
        Files.write(extension.resolve("1"), new byte[] {1, 2, 3});

        byte[] randomPayload = new byte[160_000];
        new Random(12345L).nextBytes(randomPayload);
        byte[] oversized = nbtWithPayload(0, 2, randomPayload);
        assertTrue(oversized.length > 255 * CubicRegionReader.SECTOR_BYTES - Integer.BYTES);
        Files.write(extension.resolve("2"), oversized);
        Files.write(extension.resolve("2.tmp"), new byte[] {4, 5, 6});

        List<CubicRegionEntry> entries = collectColumns(directory);

        assertEquals(2, entries.size());
        assertEquals(1, entries.get(0).getEntryId());
        assertEquals(CubicRegionEntry.Storage.REGION, entries.get(0).getStorage());
        assertEquals("normal", entries.get(0).getNbt().getString("marker"));
        assertEquals(2, entries.get(1).getEntryId());
        assertEquals(CubicRegionEntry.Storage.EXTENSION, entries.get(1).getStorage());
        assertFalse(entries.get(1).getSectorOffset().isPresent());
        assertEquals(randomPayload.length,
                entries.get(1).getNbt().getCompoundTag("Level").getByteArray("payload").length);
    }

    @Test
    public void readsExtensionOnlyRegionsAndReturnsDeterministicOrder() throws Exception {
        Path directory = temporary.newFolder("region3d-ext-only").toPath();
        Path positive = Files.createDirectory(directory.resolve("1.0.0.3dr.ext"));
        int positiveId = (1 << 8) | (2 << 4) | 3;
        Files.write(positive.resolve(String.valueOf(positiveId)), nbt(17, 2, 3, "positive"));

        Path negative = Files.createDirectory(directory.resolve("-1.0.0.3dr.ext"));
        int negativeId = (15 << 8) | (1 << 4) | 4;
        Files.write(negative.resolve(String.valueOf(negativeId)), nbt(-1, 1, 4, "negative"));

        List<CubicRegionEntry> entries = collectCubes(directory);

        assertEquals(2, entries.size());
        assertEquals("negative", entries.get(0).getNbt().getString("marker"));
        assertEquals("positive", entries.get(1).getNbt().getString("marker"));
    }

    @Test
    public void rejectsShortAndUnalignedRegionFiles() throws Exception {
        Path shortDirectory = temporary.newFolder("short").toPath();
        Files.write(shortDirectory.resolve("0.0.2dr"), new byte[4095]);
        assertFormat(() -> collectColumns(shortDirectory), "shorter than its 8-sector header");

        Path unalignedDirectory = temporary.newFolder("unaligned").toPath();
        Files.write(unalignedDirectory.resolve("0.0.2dr"), new byte[4097]);
        assertFormat(() -> collectColumns(unalignedDirectory), "not aligned to 512-byte sectors");
    }

    @Test
    public void rejectsHeaderEntriesOutsideDataSectors() throws Exception {
        Path headerDirectory = temporary.newFolder("header-sector").toPath();
        writeRawRegion(headerDirectory.resolve("0.0.2dr"), 1024, 0, packed(7, 1), 9);
        assertFormat(() -> collectColumns(headerDirectory), "starts in the reserved header sectors");

        Path boundsDirectory = temporary.newFolder("bounds").toPath();
        writeRawRegion(boundsDirectory.resolve("0.0.2dr"), 1024, 0, packed(9, 1), 9);
        assertFormat(() -> collectColumns(boundsDirectory), "beyond the file's last sector 8");

        Path zeroDirectory = temporary.newFolder("zero-count").toPath();
        writeRawRegion(zeroDirectory.resolve("0.0.2dr"), 1024, 0, packed(8, 0), 9);
        assertFormat(() -> collectColumns(zeroDirectory), "zero sector count");
    }

    @Test
    public void rejectsOverlappingNormalEntries() throws Exception {
        Path directory = temporary.newFolder("overlap").toPath();
        Path region = directory.resolve("0.0.2dr");
        byte[] bytes = new byte[11 * CubicRegionReader.SECTOR_BYTES];
        ByteBuffer file = ByteBuffer.wrap(bytes);
        file.putInt(0, packed(8, 2));
        file.putInt(Integer.BYTES, packed(9, 2));
        Files.write(region, bytes);

        assertFormat(() -> collectColumns(directory), "entries 0 and 1 overlap at sector 9");
    }

    @Test
    public void rejectsPayloadLengthOutsideAllocatedSectors() throws Exception {
        Path directory = temporary.newFolder("bad-length").toPath();
        Path region = directory.resolve("0.0.2dr");
        byte[] bytes = new byte[9 * CubicRegionReader.SECTOR_BYTES];
        ByteBuffer file = ByteBuffer.wrap(bytes);
        file.putInt(0, packed(8, 1));
        file.putInt(8 * CubicRegionReader.SECTOR_BYTES, 509);
        Files.write(region, bytes);

        assertFormat(() -> collectColumns(directory), "permits 1..508");
    }

    @Test
    public void drainsGzipStreamAndRejectsBadCrc() throws Exception {
        Path directory = temporary.newFolder("bad-crc").toPath();
        byte[] compressed = nbt(0, null, 0, "crc");
        compressed[compressed.length - 1] ^= 0x01;
        writeRegion(directory.resolve("0.0.2dr"), 1024, Collections.singletonMap(0, compressed));

        assertFormat(() -> collectColumns(directory), "not a complete valid gzip NBT payload");
    }

    @Test
    public void rejectsNbtWhoseStoredCoordinatesDoNotMatchItsEntryId() throws Exception {
        Path directory = temporary.newFolder("coordinate-mismatch").toPath();
        writeRegion(directory.resolve("0.0.2dr"), 1024,
                Collections.singletonMap(1, nbt(0, null, 99, "wrong")));

        assertFormat(() -> collectColumns(directory), "resolves to z=1 but its Level tag stores 99");
    }

    @Test
    public void rejectsOutOfRangeNumericExtensionEntry() throws Exception {
        Path directory = temporary.newFolder("bad-ext-id").toPath();
        Path extension = Files.createDirectory(directory.resolve("0.0.2dr.ext"));
        Files.write(extension.resolve("1024"), nbt(0, null, 0, "bad"));

        assertFormat(() -> collectColumns(directory), "outside 0..1023");
    }

    @Test
    public void rejectsUnknownRegionFilesAndCoordinateAliases() throws Exception {
        Path unknown = temporary.newFolder("unknown-region-file").toPath();
        Files.write(unknown.resolve("source-marker"), new byte[] {1});
        assertFormat(() -> collectColumns(unknown), "unrecognized file");

        Path negativeZero = temporary.newFolder("negative-zero-region").toPath();
        Files.write(negativeZero.resolve("-0.0.2dr"), new byte[4096]);
        assertFormat(() -> collectColumns(negativeZero), "unrecognized file");

        Path leadingZero = temporary.newFolder("leading-zero-region").toPath();
        Files.write(leadingZero.resolve("01.0.2dr"), new byte[4096]);
        assertFormat(() -> collectColumns(leadingZero), "unrecognized file");
    }

    @Test
    public void rejectsUnknownAndOrphanedExtensionFiles() throws Exception {
        Path unknown = temporary.newFolder("unknown-extension-file").toPath();
        Path extension = Files.createDirectory(unknown.resolve("0.0.2dr.ext"));
        Files.write(extension.resolve("notes"), new byte[] {1});
        assertFormat(() -> collectColumns(unknown), "unrecognized oversized-entry file");

        Path orphan = temporary.newFolder("orphaned-extension-temp").toPath();
        extension = Files.createDirectory(orphan.resolve("0.0.2dr.ext"));
        Files.write(extension.resolve("2.tmp"), new byte[] {1});
        assertFormat(() -> collectColumns(orphan), "orphaned temporary oversized entry id 2");
    }

    @Test
    public void rejectsExtensionEntriesAboveTheCompressedBudgetBeforeAllocation()
            throws Exception {
        Path directory = temporary.newFolder("oversized-extension").toPath();
        Path extension = Files.createDirectory(directory.resolve("0.0.2dr.ext"));
        Path entry = extension.resolve("0");
        try (RandomAccessFile file = new RandomAccessFile(entry.toFile(), "rw")) {
            file.setLength((long) CubicRegionReader.MAX_COMPRESSED_ENTRY_BYTES + 1L);
        }

        assertFormat(() -> collectColumns(directory), "unsupported compressed length");
    }

    @Test
    public void boundsTrailingDecompressedDataAfterTheRootTag() throws Exception {
        NBTTagCompound root = root(0, null, 0);
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        CompressedStreamTools.write(root, new DataOutputStream(raw));
        long limit = raw.size() + 1024L;

        try {
            CubicRegionReader.readAndValidateGzip(
                    temporary.newFile("bounded-gzip").toPath(), 0,
                    gzipWithTrailing(root, 4096), limit);
            fail("Expected decompressed payload limit rejection");
        } catch (CubicRegionFormatException expected) {
            assertTrue(expected.getMessage().contains("decompressed payload limit"));
        }
    }

    private List<CubicRegionEntry> collectColumns(Path directory) throws IOException {
        List<CubicRegionEntry> entries = new ArrayList<>();
        long count = CubicRegionReader.visitColumns(directory, entries::add);
        assertEquals(count, entries.size());
        return entries;
    }

    private List<CubicRegionEntry> collectCubes(Path directory) throws IOException {
        List<CubicRegionEntry> entries = new ArrayList<>();
        long count = CubicRegionReader.visitCubes(directory, entries::add);
        assertEquals(count, entries.size());
        return entries;
    }

    private static byte[] nbt(int x, Integer y, int z, String marker) throws IOException {
        NBTTagCompound root = root(x, y, z);
        root.setString("marker", marker);
        return compress(root);
    }

    private static byte[] nbtWithPayload(int x, int z, byte[] payload) throws IOException {
        NBTTagCompound root = root(x, null, z);
        root.getCompoundTag("Level").setByteArray("payload", payload);
        return compress(root);
    }

    private static NBTTagCompound root(int x, Integer y, int z) {
        NBTTagCompound level = new NBTTagCompound();
        level.setInteger("x", x);
        if (y != null) {
            level.setInteger("y", y);
        }
        level.setInteger("z", z);
        NBTTagCompound root = new NBTTagCompound();
        root.setTag("Level", level);
        return root;
    }

    private static byte[] compress(NBTTagCompound root) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(root, output);
        return output.toByteArray();
    }

    private static byte[] gzipWithTrailing(NBTTagCompound root, int trailingBytes)
            throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (DataOutputStream data = new DataOutputStream(new GZIPOutputStream(output))) {
            CompressedStreamTools.write(root, data);
            data.write(new byte[trailingBytes]);
        }
        return output.toByteArray();
    }

    private static void writeRegion(Path path, int entryCount, Map<Integer, byte[]> entries) throws IOException {
        int headerSectors = entryCount * Integer.BYTES / CubicRegionReader.SECTOR_BYTES;
        Map<Integer, Integer> offsets = new LinkedHashMap<>();
        int nextSector = headerSectors;
        for (Map.Entry<Integer, byte[]> entry : entries.entrySet()) {
            offsets.put(entry.getKey(), nextSector);
            nextSector += sectorsFor(entry.getValue().length + Integer.BYTES);
        }

        byte[] bytes = new byte[nextSector * CubicRegionReader.SECTOR_BYTES];
        ByteBuffer file = ByteBuffer.wrap(bytes);
        for (Map.Entry<Integer, byte[]> entry : entries.entrySet()) {
            int offset = offsets.get(entry.getKey());
            int sectors = sectorsFor(entry.getValue().length + Integer.BYTES);
            file.putInt(entry.getKey() * Integer.BYTES, packed(offset, sectors));
            file.position(offset * CubicRegionReader.SECTOR_BYTES);
            file.putInt(entry.getValue().length);
            file.put(entry.getValue());
        }
        Files.write(path, bytes);
    }

    private static void writeRawRegion(Path path, int entryCount, int entryId, int packed, int sectors) throws IOException {
        byte[] bytes = new byte[sectors * CubicRegionReader.SECTOR_BYTES];
        ByteBuffer.wrap(bytes).putInt(entryId * Integer.BYTES, packed);
        Files.write(path, bytes);
    }

    private static int packed(int offset, int sectors) {
        return offset << 8 | sectors;
    }

    private static int sectorsFor(int bytes) {
        return (bytes + CubicRegionReader.SECTOR_BYTES - 1) / CubicRegionReader.SECTOR_BYTES;
    }

    private static void assertFormat(ThrowingRunnable runnable, String expectedMessage) throws Exception {
        try {
            runnable.run();
            fail("Expected CubicRegionFormatException containing: " + expectedMessage);
        } catch (CubicRegionFormatException exception) {
            assertTrue("Actual message: " + exception.getMessage(), exception.getMessage().contains(expectedMessage));
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}

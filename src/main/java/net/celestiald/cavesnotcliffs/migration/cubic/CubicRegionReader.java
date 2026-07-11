package net.celestiald.cavesnotcliffs.migration.cubic;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * Dependency-free reader for the RegionLib 0.78 files written by CubicChunks.
 *
 * <p>This class deliberately exposes no write path. A malformed or ambiguous
 * entry fails the scan rather than being repaired heuristically.</p>
 */
public final class CubicRegionReader {
    static final int SECTOR_BYTES = 512;

    private static final String COORDINATE = "(?:0|-?[1-9]\\d*)";
    private static final Pattern COLUMN_NAME = Pattern.compile("^(" + COORDINATE
            + ")\\.(" + COORDINATE + ")\\.2dr$");
    private static final Pattern CUBE_NAME = Pattern.compile("^(" + COORDINATE
            + ")\\.(" + COORDINATE + ")\\.(" + COORDINATE + ")\\.3dr$");
    private static final Pattern NUMERIC_ENTRY_NAME = Pattern.compile("^(0|[1-9]\\d*)$");
    private static final Pattern NUMERIC_TEMP_ENTRY_NAME = Pattern.compile("^(0|[1-9]\\d*)\\.tmp$");
    private static final int NBT_COMPOUND = 10;
    private static final int NBT_ANY_NUMBER = 99;

    private CubicRegionReader() {
    }

    @FunctionalInterface
    public interface EntryVisitor {
        void accept(CubicRegionEntry entry) throws IOException;
    }

    public static long visitColumns(Path regionDirectory, EntryVisitor visitor) throws IOException {
        return visit(regionDirectory, Layout.COLUMNS, visitor);
    }

    public static long visitCubes(Path regionDirectory, EntryVisitor visitor) throws IOException {
        return visit(regionDirectory, Layout.CUBES, visitor);
    }

    private static long visit(Path regionDirectory, Layout layout, EntryVisitor visitor) throws IOException {
        if (!Files.isDirectory(regionDirectory)) {
            throw new IOException("Legacy " + layout.description + " directory does not exist: " + regionDirectory);
        }

        Map<RegionCoordinates, RegionSources> regions = discoverRegions(regionDirectory, layout);
        List<RegionSources> ordered = new ArrayList<>(regions.values());
        Collections.sort(ordered, Comparator.comparing(source -> source.coordinates));

        long count = 0L;
        for (RegionSources sources : ordered) {
            count += visitRegion(sources, visitor);
        }
        return count;
    }

    private static Map<RegionCoordinates, RegionSources> discoverRegions(Path directory, Layout layout) throws IOException {
        Map<RegionCoordinates, RegionSources> regions = new HashMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path path : stream) {
                String name = path.getFileName().toString();
                boolean extension = name.endsWith(".ext");
                String regionName = extension ? name.substring(0, name.length() - 4) : name;
                RegionCoordinates coordinates = parseRegionName(regionName, layout);
                if (coordinates == null) {
                    throw format(path, "unrecognized file in " + layout.description + " directory");
                }

                RegionSources sources = regions.computeIfAbsent(coordinates, RegionSources::new);
                if (extension) {
                    if (Files.isSymbolicLink(path) || !Files.isDirectory(path)) {
                        throw format(path, "oversized-entry path is not a directory");
                    }
                    sources.extensionDirectory = path;
                } else {
                    if (Files.isSymbolicLink(path) || !Files.isRegularFile(path)) {
                        throw format(path, "region path is not a regular file");
                    }
                    sources.regionFile = path;
                }
            }
        }
        return regions;
    }

    private static RegionCoordinates parseRegionName(String name, Layout layout) throws CubicRegionFormatException {
        Matcher matcher = layout.pattern.matcher(name);
        if (!matcher.matches()) {
            return null;
        }
        try {
            int x = Integer.parseInt(matcher.group(1));
            int y = layout == Layout.CUBES ? Integer.parseInt(matcher.group(2)) : 0;
            int z = Integer.parseInt(matcher.group(layout == Layout.CUBES ? 3 : 2));
            return new RegionCoordinates(layout, x, y, z, name);
        } catch (NumberFormatException exception) {
            throw new CubicRegionFormatException("Region coordinates exceed the signed 32-bit range: " + name, exception);
        }
    }

    private static long visitRegion(RegionSources sources, EntryVisitor visitor) throws IOException {
        Map<Integer, Path> extensionEntries = readExtensionIndex(sources);
        if (sources.regionFile == null) {
            return visitEntries(sources, visitor, extensionEntries,
                    new RegionLocation[sources.coordinates.layout.entryCount], null);
        }
        try (FileChannel channel = FileChannel.open(sources.regionFile, StandardOpenOption.READ)) {
            RegionLocation[] locations = readAndValidateHeader(
                    sources.regionFile, sources.coordinates.layout, channel);
            return visitEntries(sources, visitor, extensionEntries, locations, channel);
        }
    }

    private static long visitEntries(RegionSources sources, EntryVisitor visitor,
            Map<Integer, Path> extensionEntries, RegionLocation[] locations,
            FileChannel regionChannel) throws IOException {
        long count = 0L;
        for (int entryId = 0; entryId < locations.length; entryId++) {
            RegionLocation location = locations[entryId];
            if (location != null) {
                byte[] compressed = readRegionPayload(sources.regionFile, regionChannel, entryId, location);
                visitor.accept(decode(sources.coordinates, entryId, CubicRegionEntry.Storage.REGION,
                        sources.regionFile, location, compressed));
                count++;
                continue;
            }

            Path extension = extensionEntries.get(entryId);
            if (extension != null) {
                byte[] compressed = readExtensionPayload(extension, entryId);
                visitor.accept(decode(sources.coordinates, entryId, CubicRegionEntry.Storage.EXTENSION,
                        extension, null, compressed));
                count++;
            }
        }
        return count;
    }

    private static RegionLocation[] readAndValidateHeader(
            Path path, Layout layout, FileChannel channel) throws IOException {
        int headerBytes = layout.entryCount * Integer.BYTES;
        int headerSectors = headerBytes / SECTOR_BYTES;
        long fileBytes = channel.size();
        if (fileBytes < headerBytes) {
            throw format(path, "file is shorter than its " + headerSectors + "-sector header");
        }
        if (fileBytes % SECTOR_BYTES != 0L) {
            throw format(path, "file size " + fileBytes + " is not aligned to " + SECTOR_BYTES + "-byte sectors");
        }

        long totalSectors = fileBytes / SECTOR_BYTES;
        ByteBuffer header = ByteBuffer.allocate(headerBytes);
        readFully(channel, header, 0L, path, "header");
        header.flip();

        RegionLocation[] locations = new RegionLocation[layout.entryCount];
        List<RegionLocation> ordered = new ArrayList<>();
        for (int entryId = 0; entryId < layout.entryCount; entryId++) {
            int packed = header.getInt();
            if (packed == 0) {
                continue;
            }
            int sectorOffset = packed >>> 8;
            int sectorCount = packed & 0xFF;
            if (sectorCount == 0) {
                throw format(path, "entry " + entryId + " has a zero sector count");
            }
            if (sectorOffset < headerSectors) {
                throw format(path, "entry " + entryId + " starts in the reserved header sectors");
            }
            long end = (long) sectorOffset + sectorCount;
            if (end > totalSectors) {
                throw format(path, "entry " + entryId + " spans sectors " + sectorOffset + ".." + (end - 1)
                        + " beyond the file's last sector " + (totalSectors - 1));
            }
            RegionLocation location = new RegionLocation(entryId, sectorOffset, sectorCount);
            locations[entryId] = location;
            ordered.add(location);
        }

        Collections.sort(ordered, Comparator
                .comparingInt((RegionLocation location) -> location.sectorOffset)
                .thenComparingInt(location -> location.entryId));
        RegionLocation previous = null;
        for (RegionLocation location : ordered) {
            if (previous != null && location.sectorOffset < previous.endSector()) {
                throw format(path, "entries " + previous.entryId + " and " + location.entryId
                        + " overlap at sector " + location.sectorOffset);
            }
            previous = location;
        }
        return locations;
    }

    private static Map<Integer, Path> readExtensionIndex(RegionSources sources) throws IOException {
        if (sources.extensionDirectory == null) {
            return Collections.emptyMap();
        }
        Map<Integer, Path> entries = new HashMap<>();
        Map<Integer, Path> temporaryEntries = new HashMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sources.extensionDirectory)) {
            for (Path path : stream) {
                String name = path.getFileName().toString();
                if (!NUMERIC_ENTRY_NAME.matcher(name).matches()) {
                    Matcher temporary = NUMERIC_TEMP_ENTRY_NAME.matcher(name);
                    if (!temporary.matches() || Files.isSymbolicLink(path)
                            || !Files.isRegularFile(path)) {
                        throw format(path, "unrecognized oversized-entry file");
                    }
                    int entryId = parseExtensionId(path, temporary.group(1),
                            sources.coordinates.layout);
                    Path duplicate = temporaryEntries.put(entryId, path);
                    if (duplicate != null) {
                        throw format(path, "duplicates temporary oversized entry id " + entryId
                                + " from " + duplicate);
                    }
                    continue;
                }
                int entryId = parseExtensionId(path, name, sources.coordinates.layout);
                if (Files.isSymbolicLink(path) || !Files.isRegularFile(path)) {
                    throw format(path, "numeric oversized entry is not a regular file");
                }
                Path duplicate = entries.put(entryId, path);
                if (duplicate != null) {
                    throw format(path, "duplicates oversized entry id " + entryId + " from " + duplicate);
                }
            }
        }
        for (Map.Entry<Integer, Path> temporary : temporaryEntries.entrySet()) {
            if (!entries.containsKey(temporary.getKey())) {
                throw format(temporary.getValue(), "orphaned temporary oversized entry id "
                        + temporary.getKey());
            }
        }
        return entries;
    }

    private static int parseExtensionId(Path path, String name, Layout layout)
            throws CubicRegionFormatException {
        int entryId;
        try {
            entryId = Integer.parseInt(name);
        } catch (NumberFormatException exception) {
            throw format(path, "oversized-entry id exceeds the signed 32-bit range", exception);
        }
        if (entryId < 0 || entryId >= layout.entryCount) {
            throw format(path, "oversized-entry id " + entryId + " is outside 0.."
                    + (layout.entryCount - 1));
        }
        return entryId;
    }

    private static byte[] readRegionPayload(Path path, FileChannel channel,
            int entryId, RegionLocation location) throws IOException {
        long byteOffset = (long) location.sectorOffset * SECTOR_BYTES;
        ByteBuffer lengthBuffer = ByteBuffer.allocate(Integer.BYTES);
        readFully(channel, lengthBuffer, byteOffset, path, "entry " + entryId + " length");
        lengthBuffer.flip();
        int length = lengthBuffer.getInt();
        int maximum = location.sectorCount * SECTOR_BYTES - Integer.BYTES;
        if (length <= 0 || length > maximum) {
            throw format(path, "entry " + entryId + " declares compressed length " + length
                    + " but its sector allocation permits 1.." + maximum);
        }
        ByteBuffer payload = ByteBuffer.allocate(length);
        readFully(channel, payload, byteOffset + Integer.BYTES, path, "entry " + entryId + " payload");
        return payload.array();
    }

    private static byte[] readExtensionPayload(Path path, int entryId) throws IOException {
        long length = Files.size(path);
        if (length <= 0L || length > Integer.MAX_VALUE) {
            throw format(path, "oversized entry " + entryId + " has unsupported compressed length " + length);
        }
        return Files.readAllBytes(path);
    }

    private static CubicRegionEntry decode(RegionCoordinates region, int entryId,
            CubicRegionEntry.Storage storage, Path source, RegionLocation location, byte[] compressed) throws IOException {
        NBTTagCompound nbt = readAndValidateGzip(source, entryId, compressed);
        AbsoluteCoordinates coordinates = region.absoluteCoordinates(entryId);
        verifyNbtCoordinates(source, entryId, region.layout, coordinates, nbt);

        return new CubicRegionEntry(region.layout.kind, storage, source,
                region.x, region.y, region.z, entryId,
                coordinates.x, coordinates.y, coordinates.z,
                location == null ? -1 : location.sectorOffset,
                location == null ? -1 : location.sectorCount,
                compressed.length, nbt);
    }

    private static NBTTagCompound readAndValidateGzip(Path source, int entryId, byte[] compressed) throws IOException {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed));
                DataInputStream input = new DataInputStream(gzip)) {
            NBTTagCompound nbt = CompressedStreamTools.read(input);

            byte[] trailing = new byte[8192];
            int trailingBytes = 0;
            int read;
            while ((read = input.read(trailing)) != -1) {
                trailingBytes += read;
            }
            if (trailingBytes != 0) {
                throw format(source, "entry " + entryId + " contains " + trailingBytes
                        + " trailing decompressed bytes after its root NBT tag");
            }
            return nbt;
        } catch (CubicRegionFormatException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw format(source, "entry " + entryId + " is not a complete valid gzip NBT payload", exception);
        }
    }

    private static void verifyNbtCoordinates(Path source, int entryId, Layout layout,
            AbsoluteCoordinates coordinates, NBTTagCompound root) throws CubicRegionFormatException {
        if (!root.hasKey("Level", NBT_COMPOUND)) {
            throw format(source, "entry " + entryId + " has no compound Level tag");
        }
        NBTTagCompound level = root.getCompoundTag("Level");
        verifyCoordinate(source, entryId, level, "x", coordinates.x);
        if (layout == Layout.CUBES) {
            verifyCoordinate(source, entryId, level, "y", coordinates.y);
        }
        verifyCoordinate(source, entryId, level, "z", coordinates.z);
    }

    private static void verifyCoordinate(Path source, int entryId, NBTTagCompound level,
            String name, int expected) throws CubicRegionFormatException {
        if (!level.hasKey(name, NBT_ANY_NUMBER)) {
            throw format(source, "entry " + entryId + " Level tag has no numeric " + name + " coordinate");
        }
        int actual = level.getInteger(name);
        if (actual != expected) {
            throw format(source, "entry " + entryId + " resolves to " + name + '=' + expected
                    + " but its Level tag stores " + actual);
        }
    }

    private static void readFully(FileChannel channel, ByteBuffer buffer, long position,
            Path source, String description) throws IOException {
        long current = position;
        while (buffer.hasRemaining()) {
            int read = channel.read(buffer, current);
            if (read < 0) {
                throw format(source, "unexpected end of file while reading " + description);
            }
            if (read == 0) {
                continue;
            }
            current += read;
        }
    }

    private static CubicRegionFormatException format(Path path, String message) {
        return new CubicRegionFormatException(path + ": " + message);
    }

    private static CubicRegionFormatException format(Path path, String message, Throwable cause) {
        return new CubicRegionFormatException(path + ": " + message, cause);
    }

    private enum Layout {
        COLUMNS("column region", COLUMN_NAME, CubicRegionEntry.Kind.COLUMN, 5, 1024),
        CUBES("cube region", CUBE_NAME, CubicRegionEntry.Kind.CUBE, 4, 4096);

        private final String description;
        private final Pattern pattern;
        private final CubicRegionEntry.Kind kind;
        private final int locationBits;
        private final int entryCount;

        Layout(String description, Pattern pattern, CubicRegionEntry.Kind kind, int locationBits, int entryCount) {
            this.description = description;
            this.pattern = pattern;
            this.kind = kind;
            this.locationBits = locationBits;
            this.entryCount = entryCount;
        }
    }

    private static final class RegionCoordinates implements Comparable<RegionCoordinates> {
        private final Layout layout;
        private final int x;
        private final int y;
        private final int z;
        private final String name;

        private RegionCoordinates(Layout layout, int x, int y, int z, String name) {
            this.layout = layout;
            this.x = x;
            this.y = y;
            this.z = z;
            this.name = name;
        }

        private AbsoluteCoordinates absoluteCoordinates(int entryId) throws CubicRegionFormatException {
            int mask = (1 << layout.locationBits) - 1;
            int relativeX;
            int relativeY;
            int relativeZ;
            if (layout == Layout.COLUMNS) {
                relativeX = entryId >>> layout.locationBits;
                relativeY = 0;
                relativeZ = entryId & mask;
            } else {
                relativeX = entryId >>> (layout.locationBits * 2);
                relativeY = (entryId >>> layout.locationBits) & mask;
                relativeZ = entryId & mask;
            }

            int absoluteX = checkedCoordinate(x, relativeX, layout.locationBits, name, "x");
            int absoluteY = layout == Layout.CUBES
                    ? checkedCoordinate(y, relativeY, layout.locationBits, name, "y") : 0;
            int absoluteZ = checkedCoordinate(z, relativeZ, layout.locationBits, name, "z");
            return new AbsoluteCoordinates(absoluteX, absoluteY, absoluteZ);
        }

        private static int checkedCoordinate(int region, int relative, int bits,
                String regionName, String axis) throws CubicRegionFormatException {
            long value = ((long) region << bits) | relative;
            if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                throw new CubicRegionFormatException("Region " + regionName + " resolves beyond the signed 32-bit "
                        + axis + " coordinate range");
            }
            return (int) value;
        }

        @Override
        public int compareTo(RegionCoordinates other) {
            int byX = Integer.compare(x, other.x);
            if (byX != 0) {
                return byX;
            }
            int byY = Integer.compare(y, other.y);
            if (byY != 0) {
                return byY;
            }
            return Integer.compare(z, other.z);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof RegionCoordinates)) {
                return false;
            }
            RegionCoordinates that = (RegionCoordinates) other;
            return layout == that.layout && x == that.x && y == that.y && z == that.z;
        }

        @Override
        public int hashCode() {
            int result = layout.hashCode();
            result = 31 * result + x;
            result = 31 * result + y;
            return 31 * result + z;
        }
    }

    private static final class RegionSources {
        private final RegionCoordinates coordinates;
        private Path regionFile;
        private Path extensionDirectory;

        private RegionSources(RegionCoordinates coordinates) {
            this.coordinates = coordinates;
        }
    }

    private static final class RegionLocation {
        private final int entryId;
        private final int sectorOffset;
        private final int sectorCount;

        private RegionLocation(int entryId, int sectorOffset, int sectorCount) {
            this.entryId = entryId;
            this.sectorOffset = sectorOffset;
            this.sectorCount = sectorCount;
        }

        private int endSector() {
            return sectorOffset + sectorCount;
        }
    }

    private static final class AbsoluteCoordinates {
        private final int x;
        private final int y;
        private final int z;

        private AbsoluteCoordinates(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}

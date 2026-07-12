package net.celestiald.cavesnotcliffs.migration;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Durable state machine and file manifest for the one-time CubicChunks save import. */
final class CubicImportJournal {
    static final String FILE_NAME = ".cavesnotcliffs-cubic-import.nbt";
    static final String TEMP_FILE_NAME = FILE_NAME + ".tmp";
    static final int IMPORTER_VERSION = 1;
    private static final Pattern OUTPUT_PATH = Pattern.compile(
            "region/r\\.-?\\d+\\.-?\\d+\\.mca");
    private static final Pattern DIMENSION_PATH = Pattern.compile("DIM-?\\d+");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    enum State {
        DISCOVERED,
        STAGED,
        VERIFIED,
        COMMITTING,
        COMMITTED
    }

    static final class FileRecord {
        private final String path;
        private final long size;
        private final long modified;
        private final String sha256;

        FileRecord(String path, long size, long modified, String sha256) {
            this.path = path;
            this.size = size;
            this.modified = modified;
            this.sha256 = sha256;
        }

        String getPath() {
            return path;
        }

        long getSize() {
            return size;
        }

        long getModified() {
            return modified;
        }

        String getSha256() {
            return sha256;
        }

        NBTTagCompound toNbt() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("path", path);
            tag.setLong("size", size);
            tag.setLong("modified", modified);
            tag.setString("sha256", sha256);
            return tag;
        }

        static FileRecord fromNbt(NBTTagCompound tag) throws IOException {
            if (!tag.hasKey("path", 8) || !tag.hasKey("size", 99)
                    || !tag.hasKey("modified", 99) || !tag.hasKey("sha256", 8)) {
                throw new IOException("Cubic import journal contains an incomplete file record");
            }
            return new FileRecord(tag.getString("path"), tag.getLong("size"),
                    tag.getLong("modified"), tag.getString("sha256"));
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof FileRecord)) {
                return false;
            }
            FileRecord record = (FileRecord) other;
            return size == record.size && modified == record.modified
                    && path.equals(record.path) && sha256.equals(record.sha256);
        }

        @Override
        public int hashCode() {
            int result = path.hashCode();
            result = 31 * result + Long.hashCode(size);
            result = 31 * result + Long.hashCode(modified);
            result = 31 * result + sha256.hashCode();
            return result;
        }
    }

    private final NBTTagCompound root;

    private CubicImportJournal(NBTTagCompound root) {
        this.root = root;
    }

    static CubicImportJournal create(int terrainSchema, List<FileRecord> sources,
            List<String> dimensions) {
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("importerVersion", IMPORTER_VERSION);
        root.setString("state", State.DISCOVERED.name());
        root.setInteger("terrainSchema", terrainSchema);
        root.setLong("createdAt", System.currentTimeMillis());
        root.setTag("sources", recordsToNbt(sources));
        NBTTagList dimensionTags = new NBTTagList();
        for (String dimension : dimensions) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("path", dimension);
            tag.setBoolean("committed", false);
            tag.setLong("columns", 0L);
            tag.setLong("cubes", 0L);
            tag.setTag("outputs", new NBTTagList());
            dimensionTags.appendTag(tag);
        }
        root.setTag("dimensions", dimensionTags);
        return new CubicImportJournal(root);
    }

    static CubicImportJournal read(Path path) throws IOException {
        NBTTagCompound root = BoundedNbtReader.readCompressed(
                path, "cubic import journal");
        CubicImportJournal journal = new CubicImportJournal(root);
        journal.validate();
        return journal;
    }

    State getState() throws IOException {
        try {
            return State.valueOf(root.getString("state"));
        } catch (IllegalArgumentException exception) {
            throw new IOException("Unknown cubic import journal state '"
                    + root.getString("state") + "'", exception);
        }
    }

    int getTerrainSchema() {
        return root.getInteger("terrainSchema");
    }

    List<FileRecord> getSources() throws IOException {
        return recordsFromNbt(root.getTagList("sources", 10));
    }

    void transition(State expected, State next) throws IOException {
        State current = getState();
        if (current != expected || next.ordinal() != expected.ordinal() + 1) {
            throw new IOException("Invalid cubic import journal transition "
                    + current + " -> " + next + " (expected " + expected + ")");
        }
        root.setString("state", next.name());
    }

    void recordDimension(String path, long columns, long cubes, List<FileRecord> outputs)
            throws IOException {
        NBTTagCompound dimension = dimension(path);
        if (getState().ordinal() >= State.COMMITTING.ordinal()) {
            throw new IOException("Cannot change staged output metadata after commit begins");
        }
        dimension.setLong("columns", columns);
        dimension.setLong("cubes", cubes);
        dimension.setTag("outputs", recordsToNbt(outputs));
    }

    void markDimensionCommitted(String path) throws IOException {
        if (getState() != State.COMMITTING) {
            throw new IOException("Cannot mark dimension committed while journal is " + getState());
        }
        dimension(path).setBoolean("committed", true);
    }

    boolean isDimensionCommitted(String path) throws IOException {
        return dimension(path).getBoolean("committed");
    }

    List<String> getDimensions() {
        NBTTagList dimensions = root.getTagList("dimensions", 10);
        List<String> result = new ArrayList<String>(dimensions.tagCount());
        for (int index = 0; index < dimensions.tagCount(); index++) {
            result.add(dimensions.getCompoundTagAt(index).getString("path"));
        }
        return result;
    }

    List<FileRecord> getDimensionOutputs(String path) throws IOException {
        return recordsFromNbt(dimension(path).getTagList("outputs", 10));
    }

    void writeAtomic(Path path) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent == null) {
            throw new IOException("Cubic import journal has no parent directory: " + path);
        }
        Files.createDirectories(parent);
        Path temporary = parent.resolve(TEMP_FILE_NAME);
        try (OutputStream output = new BufferedOutputStream(Files.newOutputStream(temporary,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE))) {
            CompressedStreamTools.writeCompressed(root, output);
        }
        forceFile(temporary);
        try {
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            throw new IOException("The save filesystem cannot atomically update the cubic import journal",
                    exception);
        }
        forceDirectoryBestEffort(parent);
    }

    private void validate() throws IOException {
        if (root.getInteger("importerVersion") != IMPORTER_VERSION) {
            throw new IOException("Unsupported cubic import journal version "
                    + root.getInteger("importerVersion"));
        }
        getState();
        if (!root.hasKey("state", 8) || !root.hasKey("terrainSchema", 99)
                || !root.hasKey("sources", 9) || !root.hasKey("dimensions", 9)) {
            throw new IOException("Cubic import journal is missing required metadata");
        }
        List<FileRecord> sources = getSources();
        if (sources.isEmpty()) {
            throw new IOException("Cubic import journal has no source files");
        }
        Set<String> sourcePaths = new HashSet<String>();
        for (FileRecord source : sources) {
            validateRecord(source, "source", false);
            if (!sourcePaths.add(source.getPath())) {
                throw new IOException("Cubic import journal repeats source '"
                        + source.getPath() + "'");
            }
        }
        NBTTagList dimensionTags = root.getTagList("dimensions", 10);
        if (dimensionTags.getTagType() != 10 || dimensionTags.tagCount() == 0) {
            throw new IOException("Cubic import journal has no compound dimensions");
        }
        State state = getState();
        Set<String> names = new HashSet<String>();
        for (int index = 0; index < dimensionTags.tagCount(); index++) {
            NBTTagCompound tag = dimensionTags.getCompoundTagAt(index);
            if (!tag.hasKey("path", 8) || !tag.hasKey("committed", 99)
                    || !tag.hasKey("columns", 99) || !tag.hasKey("cubes", 99)
                    || !tag.hasKey("outputs", 9)) {
                throw new IOException("Cubic import journal contains an incomplete dimension");
            }
            String dimension = tag.getString("path");
            if (!(".".equals(dimension) || DIMENSION_PATH.matcher(dimension).matches())) {
                throw new IOException("Cubic import journal has an unsafe dimension path '"
                        + dimension + "'");
            }
            if (!names.add(dimension)) {
                throw new IOException("Cubic import journal repeats dimension '" + dimension + "'");
            }
            if (tag.getLong("columns") < 0L || tag.getLong("cubes") < 0L) {
                throw new IOException("Cubic import journal has negative counts for " + dimension);
            }
            boolean committed = tag.getBoolean("committed");
            if (state.ordinal() < State.COMMITTING.ordinal() && committed) {
                throw new IOException("Cubic import journal marks " + dimension
                        + " committed before commit began");
            }
            if (state == State.COMMITTED && !committed) {
                throw new IOException("Committed cubic import journal leaves " + dimension
                        + " incomplete");
            }
            NBTTagList outputTags = (NBTTagList) tag.getTag("outputs");
            if (outputTags.getTagType() != 0 && outputTags.getTagType() != 10) {
                throw new IOException("Cubic import journal has non-compound outputs for "
                        + dimension);
            }
            List<FileRecord> outputs = recordsFromNbt(outputTags);
            Set<String> outputPaths = new HashSet<String>();
            for (FileRecord output : outputs) {
                validateRecord(output, "output", true);
                if (!outputPaths.add(output.getPath())) {
                    throw new IOException("Cubic import journal repeats output '"
                            + output.getPath() + "' for " + dimension);
                }
            }
        }
    }

    private static void validateRecord(FileRecord record, String kind, boolean output)
            throws IOException {
        String path = record.getPath();
        if (!isNormalizedRelative(path) || record.getSize() < 0L
                || record.getModified() < 0L || !SHA256.matcher(record.getSha256()).matches()) {
            throw new IOException("Cubic import journal has an invalid " + kind
                    + " file record for '" + path + "'");
        }
        if (output && (!OUTPUT_PATH.matcher(path).matches() || record.getModified() != 0L)) {
            throw new IOException("Cubic import journal has an invalid output path '" + path + "'");
        }
    }

    private static boolean isNormalizedRelative(String path) {
        if (path == null || path.isEmpty() || path.startsWith("/") || path.contains("\\")) {
            return false;
        }
        for (String part : path.split("/", -1)) {
            if (part.isEmpty() || ".".equals(part) || "..".equals(part)) {
                return false;
            }
        }
        return true;
    }

    private NBTTagCompound dimension(String path) throws IOException {
        NBTTagList dimensions = root.getTagList("dimensions", 10);
        for (int index = 0; index < dimensions.tagCount(); index++) {
            NBTTagCompound dimension = dimensions.getCompoundTagAt(index);
            if (path.equals(dimension.getString("path"))) {
                return dimension;
            }
        }
        throw new IOException("Cubic import journal has no dimension '" + path + "'");
    }

    static List<FileRecord> captureSources(Path worldRoot, List<Path> dimensionRoots)
            throws IOException {
        return captureSources(worldRoot, dimensionRoots, Collections.<Path>emptyList());
    }

    static List<FileRecord> captureSources(Path worldRoot, List<Path> dimensionRoots,
            List<Path> metadataFiles) throws IOException {
        List<Path> files = new ArrayList<Path>();
        for (Path dimensionRoot : dimensionRoots) {
            collectFiles(dimensionRoot.resolve("region2d"), files);
            collectFiles(dimensionRoot.resolve("region3d"), files);
        }
        Path normalizedRoot = worldRoot.toAbsolutePath().normalize();
        for (Path metadata : metadataFiles) {
            Path normalized = metadata.toAbsolutePath().normalize();
            if (!normalized.startsWith(normalizedRoot) || Files.isSymbolicLink(normalized)
                    || !Files.isRegularFile(normalized)) {
                throw new IOException("CubicChunks metadata source is not a regular world file: "
                        + metadata);
            }
            files.add(normalized);
        }
        return fingerprint(worldRoot, files, true);
    }

    static List<FileRecord> captureOutputs(Path base, Path regionDirectory) throws IOException {
        List<Path> files = new ArrayList<Path>();
        collectFiles(regionDirectory, files);
        return fingerprint(base, files, false);
    }

    private static void collectFiles(Path directory, List<Path> target) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        if (!Files.isDirectory(directory) || Files.isSymbolicLink(directory)) {
            throw new IOException("Cubic import storage path is not a real directory: " + directory);
        }
        try (Stream<Path> stream = Files.walk(directory)) {
            for (Path path : (Iterable<Path>) stream::iterator) {
                if (Files.isSymbolicLink(path)) {
                    throw new IOException("Cubic import refuses symbolic links: " + path);
                }
                if (Files.isRegularFile(path)) {
                    target.add(path);
                }
            }
        }
    }

    private static List<FileRecord> fingerprint(Path base, List<Path> paths,
            boolean includeModifiedTime) throws IOException {
        Collections.sort(paths, Comparator.comparing(path -> normalizedRelative(base, path)));
        List<FileRecord> records = new ArrayList<FileRecord>(paths.size());
        for (Path path : paths) {
            records.add(new FileRecord(normalizedRelative(base, path), Files.size(path),
                    includeModifiedTime ? Files.getLastModifiedTime(path).toMillis() : 0L,
                    sha256(path)));
        }
        return records;
    }

    private static String normalizedRelative(Path base, Path path) {
        return base.toAbsolutePath().normalize().relativize(path.toAbsolutePath().normalize())
                .toString().replace(path.getFileSystem().getSeparator(), "/");
    }

    private static String sha256(Path path) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("Every Java 8 runtime must provide SHA-256", exception);
        }
        try (InputStream input = new BufferedInputStream(Files.newInputStream(path))) {
            byte[] buffer = new byte[65536];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }
        StringBuilder result = new StringBuilder(64);
        for (byte value : digest.digest()) {
            result.append(String.format("%02x", value & 0xFF));
        }
        return result.toString();
    }

    private static NBTTagList recordsToNbt(List<FileRecord> records) {
        NBTTagList result = new NBTTagList();
        for (FileRecord record : records) {
            result.appendTag(record.toNbt());
        }
        return result;
    }

    private static List<FileRecord> recordsFromNbt(NBTTagList tags) throws IOException {
        List<FileRecord> result = new ArrayList<FileRecord>(tags.tagCount());
        for (int index = 0; index < tags.tagCount(); index++) {
            result.add(FileRecord.fromNbt(tags.getCompoundTagAt(index)));
        }
        return result;
    }

    static void forceFile(Path path) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE,
                LinkOption.NOFOLLOW_LINKS)) {
            channel.force(true);
        }
    }

    static void forceDirectoryBestEffort(Path path) {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            channel.force(true);
        } catch (IOException | UnsupportedOperationException ignored) {
            // Windows does not allow opening a directory as a FileChannel. The file itself was
            // forced before the atomic rename; directory fsync is an additional POSIX safeguard.
        }
    }
}

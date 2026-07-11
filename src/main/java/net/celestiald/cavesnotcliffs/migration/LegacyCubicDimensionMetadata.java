package net.celestiald.cavesnotcliffs.migration;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Strictly identifies the CubicChunks dimensions owned by a legacy world save. */
final class LegacyCubicDimensionMetadata {
    private static final String METADATA_DIRECTORY = "data";
    private static final String METADATA_FILE_NAME = "cubicChunksData.dat";
    private static final String STORAGE_FORMAT = "cubicchunks:anvil3d";
    private static final String COMPATIBILITY_GENERATOR = "cubicchunks:default";
    private static final Pattern DIMENSION_DIRECTORY = Pattern.compile("DIM(-?(?:0|[1-9][0-9]*))");
    private static final Set<String> ROOT_KEYS = Collections.singleton("data");
    private static final Set<String> DATA_KEYS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(
                    "minHeight",
                    "isCubicChunks",
                    "maxHeight",
                    "storageFormat",
                    "compatibilityGeneratorType")));

    private LegacyCubicDimensionMetadata() {
    }

    static Result discoverAndValidate(Path worldRoot) throws IOException {
        Path normalizedRoot = worldRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalizedRoot, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(normalizedRoot)) {
            throw new IOException("Legacy cubic world root is not a real directory: "
                    + normalizedRoot);
        }

        List<Path> candidates = new ArrayList<Path>();
        candidates.add(normalizedRoot);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(normalizedRoot)) {
            for (Path child : stream) {
                Path name = child.getFileName();
                if (name == null || !isCanonicalDimensionDirectory(name.toString())) {
                    continue;
                }
                if (Files.isSymbolicLink(child)
                        || !Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException("Registered dimension path is not a real directory: "
                            + child);
                }
                candidates.add(child.toAbsolutePath().normalize());
            }
        }
        Collections.sort(candidates, Comparator.comparing(
                dimension -> relativeDimension(normalizedRoot, dimension)));

        List<Path> trueDimensions = new ArrayList<Path>();
        List<Path> metadataFiles = new ArrayList<Path>();
        for (Path dimension : candidates) {
            Path metadata = dimension.resolve(METADATA_DIRECTORY)
                    .resolve(METADATA_FILE_NAME);
            boolean hasRegionStorage = validateRegionPath(dimension.resolve("region2d"))
                    | validateRegionPath(dimension.resolve("region3d"));
            if (Files.isSymbolicLink(metadata)
                    || Files.isSymbolicLink(metadata.getParent())) {
                throw new IOException("CubicChunks dimension metadata may not be a symbolic link: "
                        + metadata);
            }
            if (!Files.exists(metadata, LinkOption.NOFOLLOW_LINKS)) {
                if (hasRegionStorage) {
                    throw new IOException("Cubic region storage has no matching true dimension "
                            + "metadata at " + metadata);
                }
                continue;
            }
            if (!Files.isRegularFile(metadata, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("CubicChunks dimension metadata is not a regular file: "
                        + metadata);
            }

            Metadata value = readAndValidate(metadata);
            metadataFiles.add(metadata.toAbsolutePath().normalize());
            if (value.cubic) {
                trueDimensions.add(dimension);
            } else if (hasRegionStorage) {
                throw new IOException("Cubic region storage does not have true dimension metadata "
                        + "at " + metadata);
            }
        }
        return new Result(trueDimensions, metadataFiles);
    }

    private static boolean isCanonicalDimensionDirectory(String name) {
        Matcher matcher = DIMENSION_DIRECTORY.matcher(name);
        if (!matcher.matches()) {
            return false;
        }
        try {
            int dimensionId = Integer.parseInt(matcher.group(1));
            return name.equals("DIM" + dimensionId);
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean validateRegionPath(Path region) throws IOException {
        if (Files.isSymbolicLink(region)) {
            throw new IOException("Cubic region storage may not be a symbolic link: " + region);
        }
        if (!Files.exists(region, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        if (!Files.isDirectory(region, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Cubic region storage is not a directory: " + region);
        }
        return true;
    }

    private static Metadata readAndValidate(Path metadata) throws IOException {
        NBTTagCompound root;
        try (InputStream input = new BufferedInputStream(Files.newInputStream(metadata))) {
            root = CompressedStreamTools.readCompressed(input);
        } catch (IOException exception) {
            throw new IOException("Could not read CubicChunks dimension metadata at " + metadata,
                    exception);
        } catch (RuntimeException exception) {
            throw new IOException("Malformed CubicChunks dimension metadata at " + metadata,
                    exception);
        }
        if (!root.getKeySet().equals(ROOT_KEYS) || !root.hasKey("data", 10)) {
            throw new IOException("CubicChunks dimension metadata has an unsupported root shape at "
                    + metadata + ": " + root.getKeySet());
        }
        NBTTagCompound data = root.getCompoundTag("data");
        if (!data.getKeySet().equals(DATA_KEYS)) {
            throw new IOException("CubicChunks dimension metadata has an unsupported data shape at "
                    + metadata + ": " + data.getKeySet());
        }
        requireTag(data, "minHeight", 3, metadata);
        requireTag(data, "isCubicChunks", 1, metadata);
        requireTag(data, "maxHeight", 3, metadata);
        requireTag(data, "storageFormat", 8, metadata);
        requireTag(data, "compatibilityGeneratorType", 8, metadata);

        byte rawCubic = data.getByte("isCubicChunks");
        if (rawCubic != 0 && rawCubic != 1) {
            throw new IOException("CubicChunks dimension metadata has a non-boolean "
                    + "isCubicChunks value at " + metadata + ": " + rawCubic);
        }
        boolean cubic = rawCubic == 1;
        int expectedMinimum = cubic ? -64 : 0;
        int expectedMaximum = cubic ? 320 : 256;
        int minimum = data.getInteger("minHeight");
        int maximum = data.getInteger("maxHeight");
        if (minimum != expectedMinimum || maximum != expectedMaximum) {
            throw new IOException("Unsupported CubicChunks dimension bounds at " + metadata
                    + ": " + minimum + ".." + maximum + " (expected " + expectedMinimum
                    + ".." + expectedMaximum + ")");
        }
        String storage = data.getString("storageFormat");
        if (!STORAGE_FORMAT.equals(storage)) {
            throw new IOException("Unsupported CubicChunks storage format at " + metadata
                    + ": " + storage);
        }
        String compatibility = data.getString("compatibilityGeneratorType");
        if (!COMPATIBILITY_GENERATOR.equals(compatibility)) {
            throw new IOException("Unsupported CubicChunks compatibility generator at " + metadata
                    + ": " + compatibility);
        }
        return new Metadata(cubic);
    }

    private static void requireTag(NBTTagCompound data, String name, int type, Path metadata)
            throws IOException {
        if (!data.hasKey(name, type)) {
            throw new IOException("CubicChunks dimension metadata has a malformed " + name
                    + " tag at " + metadata);
        }
    }

    private static String relativeDimension(Path worldRoot, Path dimension) {
        Path relative = worldRoot.relativize(dimension);
        return relative.getNameCount() == 0 || relative.toString().isEmpty()
                ? "." : relative.toString();
    }

    private static final class Metadata {
        private final boolean cubic;

        private Metadata(boolean cubic) {
            this.cubic = cubic;
        }
    }

    static final class Result {
        private final List<Path> trueDimensionRoots;
        private final List<Path> metadataFiles;

        private Result(List<Path> trueDimensionRoots, List<Path> metadataFiles) {
            this.trueDimensionRoots = Collections.unmodifiableList(
                    new ArrayList<Path>(trueDimensionRoots));
            this.metadataFiles = Collections.unmodifiableList(
                    new ArrayList<Path>(metadataFiles));
        }

        List<Path> getTrueDimensionRoots() {
            return trueDimensionRoots;
        }

        List<Path> getMetadataFiles() {
            return metadataFiles;
        }
    }
}

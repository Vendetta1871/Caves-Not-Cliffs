package net.celestiald.cavesnotcliffs.migration;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.io.IOException;
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

/** Validates vertical metadata that finite 1.12 structures will continue to consume. */
final class LegacyCubicStructureMetadata {
    private static final int MINIMUM_Y = -64;
    private static final int MAXIMUM_Y = 319;
    private static final List<String> STRUCTURE_FILES = Collections.unmodifiableList(
            Arrays.asList("Mineshaft.dat", "Village.dat", "Stronghold.dat",
                    "Temple.dat", "Monument.dat", "Mansion.dat"));
    private static final List<String> VILLAGE_FILES = Collections.unmodifiableList(
            Arrays.asList("villages.dat", "villages_nether.dat", "villages_end.dat"));
    private static final Pattern FEATURE_KEY = Pattern.compile(
            "\\[(-?(?:0|[1-9][0-9]*)),(-?(?:0|[1-9][0-9]*))\\]");

    private LegacyCubicStructureMetadata() {
    }

    static Result validate(List<Path> cubicDimensionRoots) throws IOException {
        Set<Path> uniqueDimensions = new HashSet<Path>();
        for (Path dimension : cubicDimensionRoots) {
            uniqueDimensions.add(dimension.toAbsolutePath().normalize());
        }
        List<Path> dimensions = new ArrayList<Path>(uniqueDimensions);
        Collections.sort(dimensions, Comparator.comparing(Path::toString));

        List<Path> sources = new ArrayList<Path>();
        for (Path dimension : dimensions) {
            Path data = dimension.resolve("data");
            if (!Files.exists(data, LinkOption.NOFOLLOW_LINKS)) {
                continue;
            }
            if (Files.isSymbolicLink(data)
                    || !Files.isDirectory(data, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Legacy structure data path is not a real directory: "
                        + data);
            }
            for (String name : STRUCTURE_FILES) {
                Path file = regularFileIfPresent(data.resolve(name));
                if (file != null) {
                    validateStructureFile(file);
                    sources.add(file);
                }
            }
            for (String name : VILLAGE_FILES) {
                Path file = regularFileIfPresent(data.resolve(name));
                if (file != null) {
                    validateVillageFile(file);
                    sources.add(file);
                }
            }
        }
        Collections.sort(sources, Comparator.comparing(Path::toString));
        return new Result(sources);
    }

    private static Path regularFileIfPresent(Path file) throws IOException {
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            return null;
        }
        if (Files.isSymbolicLink(file)
                || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Legacy structure metadata is not a regular file: " + file);
        }
        return file.toAbsolutePath().normalize();
    }

    private static void validateStructureFile(Path file) throws IOException {
        NBTTagCompound root = read(file, "structure");
        if (!root.getKeySet().equals(Collections.singleton("data"))
                || !root.hasKey("data", 10)) {
            throw new IOException(file + " has an unsupported structure root shape");
        }
        NBTTagCompound data = root.getCompoundTag("data");
        if (!data.getKeySet().equals(Collections.singleton("Features"))
                || !data.hasKey("Features", 10)) {
            throw new IOException(file + " has an unsupported structure data shape");
        }
        NBTTagCompound features = data.getCompoundTag("Features");
        for (String key : features.getKeySet()) {
            if (!features.hasKey(key, 10)) {
                throw new IOException(file + " structure feature " + key
                        + " is not a compound");
            }
            NBTTagCompound start = features.getCompoundTag(key);
            String location = file + " structure feature " + key;
            validateFeatureKey(key, start, location);
            requireInteger(start, "ChunkY", location);
            if (start.getInteger("ChunkY") != 0) {
                throw new IOException(location + " has unsupported cubic ChunkY="
                        + start.getInteger("ChunkY") + " (expected 0)");
            }
            validateBox(start, location);
            NBTTagList children = requireCompoundList(start, "Children", location);
            for (int index = 0; index < children.tagCount(); index++) {
                validateBox(children.getCompoundTagAt(index),
                        location + " child " + index);
            }
        }
    }

    private static void validateVillageFile(Path file) throws IOException {
        NBTTagCompound root = read(file, "village");
        if (!root.getKeySet().equals(Collections.singleton("data"))
                || !root.hasKey("data", 10)) {
            throw new IOException(file + " has an unsupported village root shape");
        }
        NBTTagCompound data = root.getCompoundTag("data");
        if (!data.hasKey("Tick", 3)) {
            throw new IOException(file + " village data has no integer Tick");
        }
        NBTTagList villages = requireCompoundList(data, "Villages", file + " village data");
        for (int index = 0; index < villages.tagCount(); index++) {
            NBTTagCompound village = villages.getCompoundTagAt(index);
            String location = file + " village " + index;
            validateY(village, "CY", location);
            validateY(village, "ACY", location);
            NBTTagList doors = requireCompoundList(village, "Doors", location);
            for (int door = 0; door < doors.tagCount(); door++) {
                validateY(doors.getCompoundTagAt(door), "Y",
                        location + " door " + door);
            }
        }
    }

    private static NBTTagCompound read(Path file, String kind) throws IOException {
        try {
            return BoundedNbtReader.readCompressed(
                    file, "legacy " + kind + " metadata");
        } catch (IOException exception) {
            throw new IOException("Could not read legacy " + kind + " metadata from " + file,
                    exception);
        } catch (RuntimeException exception) {
            throw new IOException("Malformed legacy " + kind + " metadata in " + file,
                    exception);
        }
    }

    private static void validateFeatureKey(String key, NBTTagCompound start, String location)
            throws IOException {
        Matcher matcher = FEATURE_KEY.matcher(key);
        if (!matcher.matches()) {
            throw new IOException(location + " has a non-canonical coordinate key");
        }
        requireInteger(start, "ChunkX", location);
        requireInteger(start, "ChunkZ", location);
        int keyX;
        int keyZ;
        try {
            keyX = Integer.parseInt(matcher.group(1));
            keyZ = Integer.parseInt(matcher.group(2));
        } catch (NumberFormatException exception) {
            throw new IOException(location + " coordinate key is outside the integer range",
                    exception);
        }
        if (start.getInteger("ChunkX") != keyX || start.getInteger("ChunkZ") != keyZ) {
            throw new IOException(location + " coordinate key does not match ChunkX/ChunkZ");
        }
    }

    private static void validateBox(NBTTagCompound value, String location) throws IOException {
        if (!value.hasKey("BB", 11)) {
            throw new IOException(location + " has no bounding-box int array");
        }
        int[] box = value.getIntArray("BB");
        if (box.length != 6) {
            throw new IOException(location + " has a bounding box of length " + box.length);
        }
        if (box[0] > box[3] || box[1] > box[4] || box[2] > box[5]) {
            throw new IOException(location + " has an inverted bounding box");
        }
        validateY(box[1], location + " minimum BB Y");
        validateY(box[4], location + " maximum BB Y");
    }

    private static NBTTagList requireCompoundList(NBTTagCompound value, String key,
            String location) throws IOException {
        NBTBase raw = value.getTag(key);
        if (!(raw instanceof NBTTagList)) {
            throw new IOException(location + " has no " + key + " list");
        }
        NBTTagList list = (NBTTagList) raw;
        if (list.getTagType() != 0 && list.getTagType() != 10) {
            throw new IOException(location + ' ' + key + " does not contain compounds");
        }
        return list;
    }

    private static void validateY(NBTTagCompound value, String key, String location)
            throws IOException {
        requireInteger(value, key, location);
        validateY(value.getInteger(key), location + ' ' + key);
    }

    private static void requireInteger(NBTTagCompound value, String key, String location)
            throws IOException {
        if (!value.hasKey(key, 3)) {
            throw new IOException(location + " has no integer " + key);
        }
    }

    private static void validateY(int y, String location) throws IOException {
        if (y < MINIMUM_Y || y > MAXIMUM_Y) {
            throw new IOException(location + "=" + y + " is outside "
                    + MINIMUM_Y + ".." + MAXIMUM_Y);
        }
    }

    static final class Result {
        private final List<Path> sourceFiles;

        private Result(List<Path> sourceFiles) {
            this.sourceFiles = Collections.unmodifiableList(
                    new ArrayList<Path>(sourceFiles));
        }

        List<Path> getSourceFiles() {
            return sourceFiles;
        }
    }
}

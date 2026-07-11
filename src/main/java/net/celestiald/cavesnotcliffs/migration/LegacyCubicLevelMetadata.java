package net.celestiald.cavesnotcliffs.migration;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Removes only CubicChunks-owned level metadata after finite region storage is committed. */
final class LegacyCubicLevelMetadata {
    static final String BACKUP_FILE_NAME = ".cavesnotcliffs-cubic-level.dat.backup";
    private static final String BACKUP_TEMP_FILE_NAME = BACKUP_FILE_NAME + ".tmp";
    private static final String LEVEL_TEMP_FILE_NAME = ".cavesnotcliffs-level.dat.tmp";
    private static final Set<String> KNOWN_REGISTRIES = new HashSet<String>(Arrays.asList(
            "cubicchunks:storage_format_provider_registry",
            "cubicchunks:vanilla_compatibility_generators_registry"));
    private static final Set<String> KNOWN_MODS = new HashSet<String>(Arrays.asList(
            "cubicchunks", "cubicchunkscore"));

    private LegacyCubicLevelMetadata() {
    }

    static Result inspect(Path levelFile, boolean requireLegacyMarkers) throws IOException {
        validateLevelFile(levelFile);
        return sanitize(read(levelFile).copy(), requireLegacyMarkers);
    }

    static Result clean(Path levelFile) throws IOException {
        validateLevelFile(levelFile);
        NBTTagCompound original = read(levelFile);
        NBTTagCompound sanitized = original.copy();
        Result result = sanitize(sanitized, false);
        if (!result.changed()) {
            return result;
        }

        Path parent = levelFile.toAbsolutePath().normalize().getParent();
        if (parent == null) {
            throw new IOException("Legacy cubic level metadata has no parent directory: "
                    + levelFile);
        }
        Path backup = parent.resolve(BACKUP_FILE_NAME);
        preserveOriginal(levelFile, backup);

        Path staged = parent.resolve(LEVEL_TEMP_FILE_NAME);
        write(staged, sanitized);
        NBTTagCompound verified = read(staged);
        if (!sanitized.equals(verified) || sanitize(verified.copy(), false).changed()) {
            throw new IOException("Could not verify sanitized legacy cubic level metadata at "
                    + staged);
        }
        if (!filesEqual(levelFile, backup)) {
            throw new IOException("level.dat changed while removing legacy cubic metadata; "
                    + "the original backup remains at " + backup);
        }
        try {
            Files.move(staged, levelFile, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            throw new IOException("The save filesystem cannot atomically replace level.dat; "
                    + "the original remains at " + backup, exception);
        }
        CubicImportJournal.forceDirectoryBestEffort(parent);
        return result;
    }

    private static void validateLevelFile(Path levelFile) throws IOException {
        if (!Files.isRegularFile(levelFile) || Files.isSymbolicLink(levelFile)) {
            throw new IOException("Legacy cubic level metadata is not a regular file: "
                    + levelFile);
        }
    }

    static Result sanitize(NBTTagCompound root) throws IOException {
        return sanitize(root, false);
    }

    private static Result sanitize(NBTTagCompound root, boolean requireLegacyMarkers)
            throws IOException {
        if (!root.hasKey("Data", 10)) {
            throw new IOException("level.dat has no compound Data tag");
        }
        int removedRegistries = 0;
        int removedMods = 0;
        NBTTagCompound fml = null;
        if (root.hasKey("FML")) {
            if (!root.hasKey("FML", 10)) {
                throw new IOException("level.dat has a non-compound FML tag");
            }
            fml = root.getCompoundTag("FML");
        }
        Set<String> foundRegistries = new HashSet<String>();
        if (fml != null && fml.hasKey("Registries")) {
            if (!fml.hasKey("Registries", 10)) {
                throw new IOException("level.dat has a non-compound FML.Registries tag");
            }
            NBTTagCompound registries = fml.getCompoundTag("Registries");
            for (String name : new HashSet<String>(registries.getKeySet())) {
                if (!name.startsWith("cubicchunks:")) {
                    continue;
                }
                if (!KNOWN_REGISTRIES.contains(name)) {
                    throw new IOException("Unsupported CubicChunks registry in level.dat: " + name);
                }
                validateRegistry(name, registries.getTag(name));
                foundRegistries.add(name);
                registries.removeTag(name);
                removedRegistries++;
            }
        }
        if (requireLegacyMarkers && !foundRegistries.equals(KNOWN_REGISTRIES)) {
            throw new IOException("A new cubic import requires the exact CubicChunks registry "
                    + "snapshots " + KNOWN_REGISTRIES + "; found " + foundRegistries);
        }
        if (fml != null && fml.hasKey("ModList")) {
            if (!fml.hasKey("ModList", 9)) {
                throw new IOException("level.dat has a non-list FML.ModList tag");
            }
            NBTTagList mods = fml.getTagList("ModList", 10);
            if (mods.getTagType() != 0 && mods.getTagType() != 10) {
                throw new IOException("level.dat FML.ModList does not contain compounds");
            }
            for (int index = mods.tagCount() - 1; index >= 0; index--) {
                NBTTagCompound mod = mods.getCompoundTagAt(index);
                if (KNOWN_MODS.contains(mod.getString("ModId"))) {
                    mods.removeTag(index);
                    removedMods++;
                }
            }
        }
        NBTTagCompound data = root.getCompoundTag("Data");
        boolean removedWorldFlag = data.hasKey("isCubicWorld");
        if (removedWorldFlag) {
            if (!data.hasKey("isCubicWorld", 99)) {
                throw new IOException("level.dat has a non-numeric Data.isCubicWorld tag");
            }
            if (requireLegacyMarkers && !data.getBoolean("isCubicWorld")) {
                throw new IOException("A new cubic import requires Data.isCubicWorld=true");
            }
            data.removeTag("isCubicWorld");
        } else if (requireLegacyMarkers) {
            throw new IOException("A new cubic import requires Data.isCubicWorld=true");
        }
        return new Result(removedRegistries, removedMods, removedWorldFlag);
    }

    private static void validateRegistry(String name, net.minecraft.nbt.NBTBase value)
            throws IOException {
        if (!(value instanceof NBTTagCompound)) {
            throw new IOException("CubicChunks registry " + name + " is not a compound");
        }
        NBTTagCompound registry = (NBTTagCompound) value;
        Set<String> expectedKeys = new HashSet<String>(Arrays.asList(
                "aliases", "blocked", "ids", "overrides", "dummied"));
        if (!registry.getKeySet().equals(expectedKeys)) {
            throw new IOException("CubicChunks registry " + name
                    + " has an unsupported snapshot shape: " + registry.getKeySet());
        }
        requireEmptyList(registry, "aliases", name);
        requireEmptyList(registry, "overrides", name);
        requireEmptyList(registry, "dummied", name);
        if (!registry.hasKey("blocked", 11) || registry.getIntArray("blocked").length != 0) {
            throw new IOException("CubicChunks registry " + name
                    + " has reserved or malformed numeric IDs");
        }
        if (!registry.hasKey("ids", 9)) {
            throw new IOException("CubicChunks registry " + name + " has no ids list");
        }
        NBTTagList ids = registry.getTagList("ids", 10);
        if (ids.getTagType() != 10 || ids.tagCount() != 1) {
            throw new IOException("CubicChunks registry " + name
                    + " must contain exactly one supported entry");
        }
        NBTTagCompound entry = ids.getCompoundTagAt(0);
        if (!entry.getKeySet().equals(new HashSet<String>(Arrays.asList("K", "V")))
                || !entry.hasKey("K", 8) || !entry.hasKey("V", 3)
                || entry.getInteger("V") != 0) {
            throw new IOException("CubicChunks registry " + name
                    + " has a malformed supported entry");
        }
        String expectedEntry = name.equals("cubicchunks:storage_format_provider_registry")
                ? "cubicchunks:anvil3d" : "cubicchunks:default";
        if (!expectedEntry.equals(entry.getString("K"))) {
            throw new IOException("CubicChunks registry " + name + " selects unsupported entry "
                    + entry.getString("K"));
        }
    }

    private static void requireEmptyList(NBTTagCompound registry, String key, String name)
            throws IOException {
        if (!registry.hasKey(key, 9) || registry.getTagList(key, 10).tagCount() != 0) {
            throw new IOException("CubicChunks registry " + name
                    + " has unsupported " + key + " entries");
        }
    }

    private static void preserveOriginal(Path levelFile, Path backup) throws IOException {
        if (Files.exists(backup)) {
            if (!Files.isRegularFile(backup) || Files.isSymbolicLink(backup)
                    || !filesEqual(levelFile, backup)) {
                throw new IOException("Legacy cubic level backup differs from level.dat: " + backup);
            }
            return;
        }
        Path temporary = backup.resolveSibling(BACKUP_TEMP_FILE_NAME);
        Files.copy(levelFile, temporary, StandardCopyOption.REPLACE_EXISTING);
        CubicImportJournal.forceFile(temporary);
        if (!filesEqual(levelFile, temporary)) {
            throw new IOException("Could not verify legacy cubic level backup at " + temporary);
        }
        try {
            Files.move(temporary, backup, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            throw new IOException("The save filesystem cannot atomically preserve level.dat", exception);
        }
        CubicImportJournal.forceDirectoryBestEffort(backup.getParent());
    }

    private static NBTTagCompound read(Path path) throws IOException {
        try (InputStream input = new BufferedInputStream(Files.newInputStream(path))) {
            return CompressedStreamTools.readCompressed(input);
        }
    }

    private static void write(Path path, NBTTagCompound root) throws IOException {
        try (OutputStream output = new BufferedOutputStream(Files.newOutputStream(path,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE))) {
            CompressedStreamTools.writeCompressed(root, output);
        }
        CubicImportJournal.forceFile(path);
    }

    private static boolean filesEqual(Path first, Path second) throws IOException {
        if (Files.size(first) != Files.size(second)) {
            return false;
        }
        try (InputStream left = new BufferedInputStream(Files.newInputStream(first));
                InputStream right = new BufferedInputStream(Files.newInputStream(second))) {
            byte[] leftBytes = new byte[65536];
            byte[] rightBytes = new byte[65536];
            while (true) {
                int leftRead = left.read(leftBytes);
                int rightRead = right.read(rightBytes);
                if (leftRead != rightRead) {
                    return false;
                }
                if (leftRead < 0) {
                    return true;
                }
                for (int index = 0; index < leftRead; index++) {
                    if (leftBytes[index] != rightBytes[index]) {
                        return false;
                    }
                }
            }
        }
    }

    static final class Result {
        private final int removedRegistries;
        private final int removedMods;
        private final boolean removedWorldFlag;

        private Result(int removedRegistries, int removedMods, boolean removedWorldFlag) {
            this.removedRegistries = removedRegistries;
            this.removedMods = removedMods;
            this.removedWorldFlag = removedWorldFlag;
        }

        boolean changed() {
            return removedRegistries > 0 || removedMods > 0 || removedWorldFlag;
        }

        int getRemovedRegistries() {
            return removedRegistries;
        }

        int getRemovedMods() {
            return removedMods;
        }

        boolean removedWorldFlag() {
            return removedWorldFlag;
        }
    }
}

package net.celestiald.cavesnotcliffs.migration;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Immutable saved numeric registry IDs read from the raw, pre-clean {@code level.dat}. */
final class LegacyCubicRegistrySnapshot {
    private static final String BLOCK_REGISTRY = "minecraft:blocks";
    private static final String BIOME_REGISTRY = "minecraft:biomes";
    private static final Set<String> REGISTRY_KEYS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(
                    "aliases", "blocked", "ids", "overrides", "dummied")));

    private final SavedRegistry blocks;
    private final SavedRegistry biomes;

    private LegacyCubicRegistrySnapshot(SavedRegistry blocks, SavedRegistry biomes) {
        this.blocks = blocks;
        this.biomes = biomes;
    }

    static LegacyCubicRegistrySnapshot read(Path levelFile) throws IOException {
        Path normalized = levelFile.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(normalized)) {
            throw new IOException("Legacy cubic registry snapshot is not a regular file: "
                    + normalized);
        }

        NBTTagCompound root;
        try (InputStream input = new BufferedInputStream(Files.newInputStream(normalized))) {
            root = CompressedStreamTools.readCompressed(input);
        } catch (IOException exception) {
            throw new IOException("Could not read legacy registry snapshot from " + normalized,
                    exception);
        } catch (RuntimeException exception) {
            throw new IOException("Malformed legacy registry snapshot in " + normalized,
                    exception);
        }

        if (!root.hasKey("FML", 10)) {
            throw new IOException("level.dat has no compound FML registry snapshot");
        }
        NBTTagCompound fml = root.getCompoundTag("FML");
        if (!fml.hasKey("Registries", 10)) {
            throw new IOException("level.dat has no compound FML.Registries snapshot");
        }
        NBTTagCompound registries = fml.getCompoundTag("Registries");
        return new LegacyCubicRegistrySnapshot(
                readRegistry(registries, BLOCK_REGISTRY, 4095),
                readRegistry(registries, BIOME_REGISTRY, 255));
    }

    Map<String, Integer> getBlockIdsByName() {
        return blocks.idsByName;
    }

    Map<Integer, String> getBlockNamesById() {
        return blocks.namesById;
    }

    Map<String, Integer> getBiomeIdsByName() {
        return biomes.idsByName;
    }

    Map<Integer, String> getBiomeNamesById() {
        return biomes.namesById;
    }

    Integer getBlockId(String name) {
        return blocks.idsByName.get(name);
    }

    String getBlockName(int id) {
        return blocks.namesById.get(id);
    }

    Integer getBiomeId(String name) {
        return biomes.idsByName.get(name);
    }

    String getBiomeName(int id) {
        return biomes.namesById.get(id);
    }

    private static SavedRegistry readRegistry(NBTTagCompound registries,
            String registryName, int maximumId) throws IOException {
        if (!registries.hasKey(registryName, 10)) {
            throw new IOException("level.dat is missing required registry snapshot "
                    + registryName);
        }
        NBTTagCompound registry = registries.getCompoundTag(registryName);
        if (!registry.getKeySet().equals(REGISTRY_KEYS)) {
            throw new IOException("Registry snapshot " + registryName
                    + " has an unsupported shape: " + registry.getKeySet());
        }
        requireList(registry, "aliases", registryName);
        requireList(registry, "overrides", registryName);
        requireList(registry, "dummied", registryName);
        if (!registry.hasKey("blocked", 11)) {
            throw new IOException("Registry snapshot " + registryName
                    + " has no blocked int array");
        }

        NBTBase rawIds = registry.getTag("ids");
        if (!(rawIds instanceof NBTTagList)) {
            throw new IOException("Registry snapshot " + registryName + " has no ids list");
        }
        NBTTagList ids = (NBTTagList) rawIds;
        if (ids.tagCount() == 0 || ids.getTagType() != 10) {
            throw new IOException("Registry snapshot " + registryName
                    + " ids is not a nonempty compound list");
        }

        Map<String, Integer> idsByName = new LinkedHashMap<String, Integer>();
        Map<Integer, String> namesById = new LinkedHashMap<Integer, String>();
        Set<String> entryKeys = new HashSet<String>(Arrays.asList("K", "V"));
        for (int index = 0; index < ids.tagCount(); index++) {
            NBTTagCompound entry = ids.getCompoundTagAt(index);
            if (!entry.getKeySet().equals(entryKeys)
                    || !entry.hasKey("K", 8) || !entry.hasKey("V", 3)) {
                throw new IOException("Registry snapshot " + registryName
                        + " has malformed K/V entry " + index);
            }
            String name = entry.getString("K");
            validateRegistryName(registryName, name, index);
            int id = entry.getInteger("V");
            if (id < 0 || id > maximumId) {
                throw new IOException("Registry snapshot " + registryName + " ID " + id
                        + " for " + name + " is outside 0.." + maximumId);
            }
            Integer previousId = idsByName.put(name, id);
            if (previousId != null) {
                throw new IOException("Registry snapshot " + registryName
                        + " repeats saved name " + name);
            }
            String previousName = namesById.put(id, name);
            if (previousName != null) {
                throw new IOException("Registry snapshot " + registryName
                        + " repeats saved ID " + id + " for " + previousName
                        + " and " + name);
            }
        }
        return new SavedRegistry(idsByName, namesById);
    }

    private static void requireList(NBTTagCompound registry, String key,
            String registryName) throws IOException {
        if (!registry.hasKey(key, 9)) {
            throw new IOException("Registry snapshot " + registryName
                    + " has no " + key + " list");
        }
    }

    private static void validateRegistryName(String registryName, String name, int index)
            throws IOException {
        try {
            ResourceLocation parsed = new ResourceLocation(name);
            if (name.isEmpty() || !parsed.toString().equals(name)) {
                throw new IllegalArgumentException("non-canonical resource location");
            }
        } catch (RuntimeException exception) {
            throw new IOException("Registry snapshot " + registryName
                    + " has invalid saved name at entry " + index + ": " + name, exception);
        }
    }

    private static final class SavedRegistry {
        private final Map<String, Integer> idsByName;
        private final Map<Integer, String> namesById;

        private SavedRegistry(Map<String, Integer> idsByName,
                Map<Integer, String> namesById) {
            this.idsByName = Collections.unmodifiableMap(
                    new LinkedHashMap<String, Integer>(idsByName));
            this.namesById = Collections.unmodifiableMap(
                    new LinkedHashMap<Integer, String>(namesById));
        }
    }
}

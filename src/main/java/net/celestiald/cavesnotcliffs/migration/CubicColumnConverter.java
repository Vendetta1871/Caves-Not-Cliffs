package net.celestiald.cavesnotcliffs.migration;

import net.celestiald.cavesnotcliffs.world.CavesNotCliffsWorldData;
import net.celestiald.cavesnotcliffs.world.LegacySchemaOnePopulationHandler;
import net.celestiald.cavesnotcliffs.world.LegacySchemaTwoFluidHandler;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Lossless NBT mapping from one draft-v2 CubicChunks column to one finite Anvil chunk. */
public final class CubicColumnConverter {
    public static final int MIN_SECTION_Y = -4;
    public static final int MAX_SECTION_Y_EXCLUSIVE = 20;

    private static final int DATA_VERSION_1_12_2 = 1343;
    private static final int NO_HEIGHT = Integer.MIN_VALUE + 32;
    private static final String CONTENT_ROOT = "cavesnotcliffs";
    private static final String CONTENT_VERSION = "contentVersion";
    private static final String CAULDRON_BRIDGE = "CavesNotCliffsCauldronBridge";
    static final String REBUILD_HEIGHT_MAP = "CavesNotCliffsRebuildHeightMap";
    static final String SCHEMA_ONE_POPULATION = LegacySchemaOnePopulationHandler.NBT_KEY;
    static final int SCHEMA_ONE_POPULATION_VERSION = LegacySchemaOnePopulationHandler.VERSION;
    static final String SCHEMA_TWO_FLUIDS = LegacySchemaTwoFluidHandler.NBT_KEY;
    static final int SCHEMA_TWO_FLUID_VERSION = LegacySchemaTwoFluidHandler.VERSION;

    private static final Set<String> KNOWN_CUBE_ROOT_KEYS = new HashSet<String>(Arrays.asList(
            "DataVersion", "ForgeDataVersion", "Level", CONTENT_ROOT, CAULDRON_BRIDGE));
    private static final Set<String> KNOWN_COLUMN_LEVEL_KEYS = new HashSet<String>(Arrays.asList(
            "v", "x", "z", "InhabitedTime", "Biomes", "OpacityIndex",
            "OpacityIndexClient", "ForgeCaps"));
    private static final Set<String> KNOWN_CUBE_LEVEL_KEYS = new HashSet<String>(Arrays.asList(
            "v", "x", "y", "z", "populated", "isSurfaceTracked", "fullyPopulated",
            "initLightDone", "ForgeCaps", "Sections", "Entities", "TileEntities",
            "TileTicks", "LightingInfoType", "LightingInfo", "Biomes3D"));
    private static final Set<String> KNOWN_SECTION_KEYS = new HashSet<String>(Arrays.asList(
            "Y", "Blocks", "Data", "Add", "Add2", "BlockLight", "SkyLight"));

    private CubicColumnConverter() {
    }

    public static NBTTagCompound convertOverworld(NBTTagCompound columnRoot,
            Map<Integer, NBTTagCompound> cubeRoots, int terrainSchema, long lastUpdate)
            throws CubicColumnConversionException {
        if (terrainSchema != CavesNotCliffsWorldData.LEGACY_SCHEMA
                && terrainSchema != CavesNotCliffsWorldData.CURRENT_SCHEMA) {
            throw new CubicColumnConversionException(
                    "Unsupported Caves Not Cliffs terrain schema " + terrainSchema);
        }
        return convert(columnRoot, cubeRoots, terrainSchema, lastUpdate, true);
    }

    /** Converts a CubicChunks vanilla-compatibility dimension after proving it has no 3D biomes. */
    public static NBTTagCompound convertVanillaDimension(NBTTagCompound columnRoot,
            Map<Integer, NBTTagCompound> cubeRoots, long lastUpdate)
            throws CubicColumnConversionException {
        for (Map.Entry<Integer, NBTTagCompound> entry : cubeRoots.entrySet()) {
            NBTTagCompound root = entry.getValue();
            if (root != null && root.hasKey("Level", 10)
                    && root.getCompoundTag("Level").hasKey("Biomes3D")) {
                throw new CubicColumnConversionException("Vanilla-compatibility cube Y="
                        + entry.getKey() + " has 3D biome data that finite Anvil cannot represent");
            }
        }
        return convert(columnRoot, cubeRoots, 0, lastUpdate, false);
    }

    /** Proves that a cube-only lookahead column contains no state that an Anvil import could lose. */
    public static void validateDiscardableLookahead(Map<Integer, NBTTagCompound> cubeRoots)
            throws CubicColumnConversionException {
        if (cubeRoots.isEmpty()) {
            return;
        }
        NBTTagCompound firstRoot = cubeRoots.values().iterator().next();
        NBTTagCompound first = requireCompound(firstRoot, "Level", "cube-only lookahead");
        int chunkX = requireInt(first, "x", "cube-only lookahead");
        int chunkZ = requireInt(first, "z", "cube-only lookahead");
        for (Map.Entry<Integer, NBTTagCompound> entry : cubeRoots.entrySet()) {
            NBTTagCompound cube = validateCube(
                    entry.getValue(), chunkX, entry.getKey(), chunkZ);
            if (!isDiscardableEmptyLookahead(cube, chunkX, entry.getKey(), chunkZ)) {
                throw fail(chunkX, chunkZ, "is a cube-only lookahead column with stateful cube Y="
                        + entry.getKey());
            }
        }
    }

    /** Proves that cube-only terrain has no populated or dynamic state before oracle comparison. */
    static void validateRegenerableLookahead(Map<Integer, NBTTagCompound> cubeRoots)
            throws CubicColumnConversionException {
        if (cubeRoots.isEmpty()) {
            throw new CubicColumnConversionException(
                    "A regenerable cube-only column contains no cube records");
        }
        NBTTagCompound firstRoot = cubeRoots.values().iterator().next();
        NBTTagCompound first = requireCompound(firstRoot, "Level",
                "cube-only regeneration candidate");
        int chunkX = requireInt(first, "x", "cube-only regeneration candidate");
        int chunkZ = requireInt(first, "z", "cube-only regeneration candidate");
        for (Map.Entry<Integer, NBTTagCompound> entry : cubeRoots.entrySet()) {
            int cubeY = entry.getKey();
            NBTTagCompound cube = validateCube(entry.getValue(), chunkX, cubeY, chunkZ);
            if (cube.getBoolean("populated") || cube.getBoolean("fullyPopulated")) {
                throw fail(chunkX, chunkZ, "cube-only Y=" + cubeY
                        + " has already-populated state that cannot be regenerated");
            }
            if (hasDynamicPayload(cube)) {
                throw fail(chunkX, chunkZ, "cube-only Y=" + cubeY
                        + " has dynamic state that cannot be regenerated");
            }
        }
    }

    private static NBTTagCompound convert(NBTTagCompound columnRoot,
            Map<Integer, NBTTagCompound> cubeRoots, int terrainSchema, long lastUpdate,
            boolean cavesNotCliffsOverworld)
            throws CubicColumnConversionException {
        NBTTagCompound column = requireCompound(columnRoot, "Level", "column root");
        rejectUnknownKeys(column, KNOWN_COLUMN_LEVEL_KEYS, "column Level");
        requireVersion(column, "column");
        int chunkX = requireInt(column, "x", "column");
        int chunkZ = requireInt(column, "z", "column");
        byte[] biomes = requireBytes(column, "Biomes", 256, describe(chunkX, chunkZ));
        if (column.hasKey("ForgeCaps") && !column.hasKey("ForgeCaps", 10)) {
            throw fail(chunkX, chunkZ, "has a non-compound column ForgeCaps tag");
        }
        TreeMap<Integer, NBTTagCompound> cubes = new TreeMap<Integer, NBTTagCompound>(cubeRoots);
        validateRequiredCubes(cubes, terrainSchema, chunkX, chunkZ,
                cavesNotCliffsOverworld);
        validateOutOfRangeCubes(cubes, chunkX, chunkZ);
        validateOpacityIndex(column, chunkX, chunkZ);
        int[] heightMap = rebuildNonAirHeightMap(cubes, chunkX, chunkZ);

        NBTTagList sections = new NBTTagList();
        NBTTagList entities = new NBTTagList();
        NBTTagList tileEntities = new NBTTagList();
        NBTTagList tileTicks = new NBTTagList();
        Set<String> entityUuids = new HashSet<String>();
        Set<String> tilePositions = new HashSet<String>();
        int contentVersion = Integer.MAX_VALUE;
        boolean allCauldronsBridged = true;
        boolean sawContentMarker = false;
        boolean sawCauldronMarker = false;

        for (Map.Entry<Integer, NBTTagCompound> entry : cubes.entrySet()) {
            int cubeY = entry.getKey();
            if (!isInTargetRange(cubeY)) {
                continue;
            }
            NBTTagCompound cubeRoot = entry.getValue();
            NBTTagCompound cube = validateCube(cubeRoot, chunkX, cubeY, chunkZ);
            copySection(cube, cubeY, sections, chunkX, chunkZ,
                    cavesNotCliffsOverworld);
            copyEntities(cube, cubeY, entities, entityUuids, chunkX, chunkZ);
            copyPositionedList(cube, "TileEntities", cubeY, tileEntities,
                    tilePositions, chunkX, chunkZ, true);
            copyPositionedList(cube, "TileTicks", cubeY, tileTicks,
                    null, chunkX, chunkZ, false);
            contentVersion = Math.min(contentVersion, contentVersion(cubeRoot));
            sawContentMarker |= cubeRoot.hasKey(CONTENT_ROOT, 10);
            allCauldronsBridged &= cubeRoot.getInteger(CAULDRON_BRIDGE) >= 1;
            sawCauldronMarker |= cubeRoot.hasKey(CAULDRON_BRIDGE, 99);
        }

        if (contentVersion == Integer.MAX_VALUE) {
            throw fail(chunkX, chunkZ, "contains no finite-range cube data");
        }

        NBTTagCompound result = columnRoot.copy();
        result.setInteger("DataVersion", DATA_VERSION_1_12_2);
        NBTTagCompound target = new NBTTagCompound();
        target.setInteger("xPos", chunkX);
        target.setInteger("zPos", chunkZ);
        target.setLong("LastUpdate", lastUpdate);
        target.setIntArray("HeightMap", heightMap);
        int schemaOnePopulation = schemaOnePopulationMask(
                cubes, terrainSchema, cavesNotCliffsOverworld);
        int schemaTwoPopulation = schemaTwoPopulationMask(
                cubes, terrainSchema, cavesNotCliffsOverworld);
        target.setBoolean("TerrainPopulated", populationState(
                cubes, terrainSchema, chunkX, chunkZ, cavesNotCliffsOverworld,
                schemaOnePopulation, schemaTwoPopulation));
        // CubicChunks lighting-engine metadata has no Anvil peer. The section nibble arrays are
        // preserved, but vanilla must perform its normal finite-column light validation.
        target.setBoolean("LightPopulated", false);
        target.setLong("InhabitedTime", column.getLong("InhabitedTime"));
        target.setTag("Sections", sections);
        target.setByteArray("Biomes", biomes.clone());
        target.setTag("Entities", entities);
        target.setTag("TileEntities", tileEntities);
        target.setTag("TileTicks", tileTicks);
        if (column.hasKey("ForgeCaps", 10)) {
            target.setTag("ForgeCaps", column.getCompoundTag("ForgeCaps").copy());
        }
        result.setTag("Level", target);

        if (schemaOnePopulation >= 0) {
            LegacySchemaOnePopulationHandler.writeInitialMarker(
                    result, schemaOnePopulation);
        }
        if (schemaTwoPopulation >= 0
                && (schemaTwoPopulation & LegacySchemaTwoFluidHandler.bit(0)) != 0) {
            LegacySchemaTwoFluidHandler.writeInitialMarker(result, schemaTwoPopulation);
        }

        if (cavesNotCliffsOverworld || sawContentMarker) {
            NBTTagCompound content = result.hasKey(CONTENT_ROOT, 10)
                    ? result.getCompoundTag(CONTENT_ROOT).copy() : new NBTTagCompound();
            content.setInteger(CONTENT_VERSION, contentVersion);
            result.setTag(CONTENT_ROOT, content);
        }
        if (cavesNotCliffsOverworld || sawCauldronMarker) {
            if (allCauldronsBridged) {
                result.setInteger(CAULDRON_BRIDGE, 1);
            } else {
                result.removeTag(CAULDRON_BRIDGE);
            }
        }
        // Numeric block-state IDs are remapped from level.dat only after this pre-world import.
        // The initial non-air height map is safe for loading; the marker makes the first real
        // chunk load rebuild exact opacity/skylight with the post-remap registry and world context.
        result.setInteger(REBUILD_HEIGHT_MAP, 1);
        return result;
    }

    private static void validateRequiredCubes(Map<Integer, NBTTagCompound> cubes,
            int terrainSchema, int chunkX, int chunkZ, boolean cavesNotCliffsOverworld)
            throws CubicColumnConversionException {
        int requiredMin = cavesNotCliffsOverworld ? MIN_SECTION_Y : 0;
        int requiredMax = terrainSchema == CavesNotCliffsWorldData.LEGACY_SCHEMA
                ? 16 : cavesNotCliffsOverworld ? MAX_SECTION_Y_EXCLUSIVE : 16;
        for (int cubeY = requiredMin; cubeY < requiredMax; cubeY++) {
            if (!cubes.containsKey(cubeY)) {
                throw fail(chunkX, chunkZ, "is missing authoritative cube Y=" + cubeY
                        + (cavesNotCliffsOverworld
                        ? " for terrain schema " + terrainSchema
                        : " for a vanilla-compatibility dimension"));
            }
        }
    }

    private static void validateOutOfRangeCubes(Map<Integer, NBTTagCompound> cubes,
            int chunkX, int chunkZ) throws CubicColumnConversionException {
        for (Map.Entry<Integer, NBTTagCompound> entry : cubes.entrySet()) {
            if (isInTargetRange(entry.getKey())) {
                continue;
            }
            NBTTagCompound cube = validateCube(
                    entry.getValue(), chunkX, entry.getKey(), chunkZ);
            if (!isOutOfRangePayloadEmpty(cube, chunkX, entry.getKey(), chunkZ)) {
                throw fail(chunkX, chunkZ, "has nonempty cube Y=" + entry.getKey()
                        + " outside the finite -64..319 range");
            }
        }
    }

    private static NBTTagCompound validateCube(NBTTagCompound root,
            int chunkX, int cubeY, int chunkZ) throws CubicColumnConversionException {
        rejectUnknownKeys(root, KNOWN_CUBE_ROOT_KEYS,
                "cube " + describe(chunkX, cubeY, chunkZ) + " root");
        if (root.hasKey(CONTENT_ROOT) && !root.hasKey(CONTENT_ROOT, 10)) {
            throw fail(chunkX, chunkZ, "cube Y=" + cubeY
                    + " has a non-compound content marker");
        } else if (root.hasKey(CONTENT_ROOT, 10)) {
            rejectUnknownKeys(root.getCompoundTag(CONTENT_ROOT),
                    new HashSet<String>(Arrays.asList(CONTENT_VERSION)),
                    "cube " + describe(chunkX, cubeY, chunkZ) + " content marker");
        }
        NBTTagCompound cube = requireCompound(root, "Level",
                "cube " + describe(chunkX, cubeY, chunkZ));
        rejectUnknownKeys(cube, KNOWN_CUBE_LEVEL_KEYS,
                "cube " + describe(chunkX, cubeY, chunkZ) + " Level");
        requireVersion(cube, "cube " + describe(chunkX, cubeY, chunkZ));
        if (requireInt(cube, "x", "cube") != chunkX
                || requireInt(cube, "y", "cube") != cubeY
                || requireInt(cube, "z", "cube") != chunkZ) {
            throw fail(chunkX, chunkZ, "contains a cube with mismatched Level coordinates at Y="
                    + cubeY);
        }
        if (cube.hasKey("ForgeCaps", 10) && !cube.getCompoundTag("ForgeCaps").hasNoTags()) {
            throw fail(chunkX, chunkZ, "cube Y=" + cubeY
                    + " has nonempty capabilities with no finite-column merge adapter");
        }
        if (cube.hasKey("ForgeCaps") && !cube.hasKey("ForgeCaps", 10)) {
            throw fail(chunkX, chunkZ, "cube Y=" + cubeY
                    + " has a non-compound ForgeCaps tag");
        }
        if (cube.hasKey("Biomes3D")) {
            requireBytes(cube, "Biomes3D", 64,
                    "cube " + describe(chunkX, cubeY, chunkZ));
        }
        for (String key : Arrays.asList("Sections", "Entities", "TileEntities", "TileTicks")) {
            validateCompoundList(cube, key, chunkX, cubeY, chunkZ);
        }
        return cube;
    }

    private static void validateCompoundList(NBTTagCompound cube, String key,
            int chunkX, int cubeY, int chunkZ) throws CubicColumnConversionException {
        if (!cube.hasKey(key)) {
            return;
        }
        if (!cube.hasKey(key, 9)) {
            throw fail(chunkX, chunkZ, "cube Y=" + cubeY + " has non-list " + key);
        }
        NBTTagList values = (NBTTagList) cube.getTag(key);
        if (values.getTagType() != 0 && values.getTagType() != 10) {
            throw fail(chunkX, chunkZ, "cube Y=" + cubeY
                    + " has non-compound entries in " + key);
        }
    }

    private static void copySection(NBTTagCompound cube, int cubeY, NBTTagList target,
            int chunkX, int chunkZ, boolean requireSkylight)
            throws CubicColumnConversionException {
        if (!cube.hasKey("Sections")) {
            return;
        }
        if (!cube.hasKey("Sections", 9)) {
            throw fail(chunkX, chunkZ, "cube Y=" + cubeY + " has a non-list Sections tag");
        }
        NBTTagList source = cube.getTagList("Sections", 10);
        if (source.tagCount() > 1) {
            throw fail(chunkX, chunkZ, "cube Y=" + cubeY + " has more than one section");
        }
        if (source.tagCount() == 0) {
            return;
        }
        NBTTagCompound section = source.getCompoundTagAt(0).copy();
        rejectUnknownKeys(section, KNOWN_SECTION_KEYS,
                "cube " + describe(chunkX, cubeY, chunkZ) + " section");
        requireBytes(section, "Blocks", 4096, "cube " + describe(chunkX, cubeY, chunkZ));
        requireBytes(section, "Data", 2048, "cube " + describe(chunkX, cubeY, chunkZ));
        requireBytes(section, "BlockLight", 2048,
                "cube " + describe(chunkX, cubeY, chunkZ));
        validateOptionalNibble(section, "Add", chunkX, cubeY, chunkZ);
        if (requireSkylight) {
            requireBytes(section, "SkyLight", 2048,
                    "cube " + describe(chunkX, cubeY, chunkZ));
        } else {
            validateOptionalNibble(section, "SkyLight", chunkX, cubeY, chunkZ);
        }
        if (section.hasKey("Add2")) {
            byte[] add2 = requireBytes(section, "Add2", 2048,
                    "cube " + describe(chunkX, cubeY, chunkZ));
            if (hasNonZero(add2)) {
                throw fail(chunkX, chunkZ, "cube Y=" + cubeY
                        + " uses Add2 block-state IDs that Anvil cannot represent");
            }
            section.removeTag("Add2");
        }
        section.setByte("Y", (byte) cubeY);
        target.appendTag(section);
    }

    private static void copyEntities(NBTTagCompound cube, int cubeY, NBTTagList target,
            Set<String> uuids, int chunkX, int chunkZ) throws CubicColumnConversionException {
        NBTTagList source = list(cube, "Entities", chunkX, cubeY, chunkZ);
        for (int index = 0; index < source.tagCount(); index++) {
            NBTTagCompound entity = source.getCompoundTagAt(index);
            NBTTagList pos = entity.getTagList("Pos", 6);
            if (pos.tagCount() != 3) {
                throw fail(chunkX, chunkZ, "entity " + index + " in cube Y=" + cubeY
                        + " has no three-value Pos list");
            }
            if (section(pos.getDoubleAt(0)) != chunkX
                    || section(pos.getDoubleAt(1)) != cubeY
                    || section(pos.getDoubleAt(2)) != chunkZ) {
                throw fail(chunkX, chunkZ, "entity " + index + " does not belong to cube Y="
                        + cubeY);
            }
            if (!entity.hasUniqueId("UUID")) {
                throw fail(chunkX, chunkZ, "entity " + index + " in cube Y=" + cubeY
                        + " has no UUID");
            }
            String uuid = entity.getLong("UUIDMost") + ":" + entity.getLong("UUIDLeast");
            if (!uuids.add(uuid)) {
                throw fail(chunkX, chunkZ, "contains duplicate entity UUID " + uuid);
            }
            target.appendTag(entity.copy());
        }
    }

    private static void copyPositionedList(NBTTagCompound cube, String key, int cubeY,
            NBTTagList target, Set<String> uniquePositions, int chunkX, int chunkZ,
            boolean requireUnique) throws CubicColumnConversionException {
        NBTTagList source = list(cube, key, chunkX, cubeY, chunkZ);
        for (int index = 0; index < source.tagCount(); index++) {
            NBTTagCompound value = source.getCompoundTagAt(index);
            if (!value.hasKey("x", 99) || !value.hasKey("y", 99) || !value.hasKey("z", 99)) {
                throw fail(chunkX, chunkZ, key + " entry " + index + " in cube Y=" + cubeY
                        + " has no numeric position");
            }
            int x = value.getInteger("x");
            int y = value.getInteger("y");
            int z = value.getInteger("z");
            if ((x >> 4) != chunkX || (y >> 4) != cubeY || (z >> 4) != chunkZ) {
                throw fail(chunkX, chunkZ, key + " entry " + index
                        + " does not belong to cube Y=" + cubeY);
            }
            if (requireUnique) {
                String position = x + ":" + y + ":" + z;
                if (!uniquePositions.add(position)) {
                    throw fail(chunkX, chunkZ, "contains duplicate tile entity at " + position);
                }
            }
            target.appendTag(value.copy());
        }
    }

    private static NBTTagList list(NBTTagCompound cube, String key,
            int chunkX, int cubeY, int chunkZ) throws CubicColumnConversionException {
        if (!cube.hasKey(key)) {
            return new NBTTagList();
        }
        if (!cube.hasKey(key, 9)) {
            throw fail(chunkX, chunkZ,
                    "cube " + describe(chunkX, cubeY, chunkZ) + " has non-list " + key);
        }
        return cube.getTagList(key, 10);
    }

    private static boolean populationState(Map<Integer, NBTTagCompound> cubes,
            int terrainSchema, int chunkX, int chunkZ, boolean cavesNotCliffsOverworld,
            int schemaOnePopulation, int schemaTwoPopulation)
            throws CubicColumnConversionException {
        if (!cavesNotCliffsOverworld) {
            boolean first = cubes.get(0).getCompoundTag("Level").getBoolean("populated");
            for (int cubeY = 1; cubeY < 16; cubeY++) {
                if (cubes.get(cubeY).getCompoundTag("Level").getBoolean("populated") != first) {
                    throw fail(chunkX, chunkZ,
                            "has a mixed vanilla-dimension population state across cubes 0..15");
                }
            }
            return first;
        }
        if (terrainSchema == CavesNotCliffsWorldData.CURRENT_SCHEMA) {
            if (schemaTwoPopulation < 0) {
                throw fail(chunkX, chunkZ, "has no preserved schema-2 population mask");
            }
            return (schemaTwoPopulation & LegacySchemaTwoFluidHandler.bit(0)) != 0;
        }
        if (schemaOnePopulation < 0) {
            throw fail(chunkX, chunkZ, "has no preserved schema-1 population mask");
        }
        // Cube Y=0 owned vanilla terrain population in the CubicChunks generator. If it was
        // already populated, finite Chunk.populate must not replay vanilla/Forge worldgen merely
        // because another cave band still needs its deterministic decorator pass.
        return (schemaOnePopulation & populationBit(0)) != 0;
    }

    private static int schemaOnePopulationMask(Map<Integer, NBTTagCompound> cubes,
            int terrainSchema, boolean cavesNotCliffsOverworld) {
        if (!cavesNotCliffsOverworld
                || terrainSchema != CavesNotCliffsWorldData.LEGACY_SCHEMA) {
            return -1;
        }
        int mask = 0;
        for (int cubeY = MIN_SECTION_Y; cubeY < 4; cubeY++) {
            if (cubes.get(cubeY).getCompoundTag("Level").getBoolean("populated")) {
                mask |= populationBit(cubeY);
            }
        }
        return mask;
    }

    private static int schemaTwoPopulationMask(Map<Integer, NBTTagCompound> cubes,
            int terrainSchema, boolean cavesNotCliffsOverworld) {
        if (!cavesNotCliffsOverworld
                || terrainSchema != CavesNotCliffsWorldData.CURRENT_SCHEMA) {
            return -1;
        }
        int mask = 0;
        for (int cubeY = MIN_SECTION_Y; cubeY < MAX_SECTION_Y_EXCLUSIVE; cubeY++) {
            if (cubes.get(cubeY).getCompoundTag("Level").getBoolean("populated")) {
                mask |= LegacySchemaTwoFluidHandler.bit(cubeY);
            }
        }
        return mask;
    }

    static int populationBit(int cubeY) {
        return LegacySchemaOnePopulationHandler.bit(cubeY);
    }

    private static void validateOpacityIndex(NBTTagCompound column, int chunkX, int chunkZ)
            throws CubicColumnConversionException {
        byte[] encoded = requireBytes(column, "OpacityIndex", -1, describe(chunkX, chunkZ));
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(encoded))) {
            for (int index = 0; index < 256; index++) {
                int min = input.readInt();
                int max = input.readInt();
                int segmentCount = input.readUnsignedShort();
                if (min == NO_HEIGHT || max == NO_HEIGHT) {
                    if (min != NO_HEIGHT || max != NO_HEIGHT || segmentCount != 0) {
                        throw fail(chunkX, chunkZ, "has an inconsistent empty opacity column at "
                                + localPosition(index));
                    }
                    continue;
                }
                if (min > max || segmentCount > 0 && (segmentCount & 1) == 0) {
                    throw fail(chunkX, chunkZ, "has an invalid opacity segment header at "
                            + localPosition(index));
                }
                int previous = Integer.MIN_VALUE;
                for (int segment = 0; segment < segmentCount; segment++) {
                    int value = input.readInt();
                    if (value <= previous || value < min || value > max
                            || segment == 0 && value != min) {
                        throw fail(chunkX, chunkZ, "has invalid opacity segments at "
                                + localPosition(index));
                    }
                    previous = value;
                }
            }
            if (input.read() != -1) {
                throw fail(chunkX, chunkZ, "has trailing bytes in OpacityIndex");
            }
        } catch (EOFException exception) {
            throw new CubicColumnConversionException(
                    describe(chunkX, chunkZ) + " has a truncated OpacityIndex", exception);
        } catch (IOException exception) {
            throw new CubicColumnConversionException(
                    "Could not decode OpacityIndex for " + describe(chunkX, chunkZ), exception);
        }
    }

    private static int[] rebuildNonAirHeightMap(Map<Integer, NBTTagCompound> cubes,
            int chunkX, int chunkZ)
            throws CubicColumnConversionException {
        int[] result = new int[256];
        Arrays.fill(result, MIN_SECTION_Y * 16);
        for (Map.Entry<Integer, NBTTagCompound> entry : cubes.entrySet()) {
            int cubeY = entry.getKey();
            if (!isInTargetRange(cubeY)) {
                continue;
            }
            NBTTagCompound cube = validateCube(entry.getValue(), chunkX, cubeY, chunkZ);
            if (!cube.hasKey("Sections", 9)) {
                continue;
            }
            NBTTagList sections = cube.getTagList("Sections", 10);
            if (sections.tagCount() == 0) {
                continue;
            }
            if (sections.tagCount() != 1) {
                throw fail(chunkX, chunkZ, "cube Y=" + cubeY + " has more than one section");
            }
            NBTTagCompound section = sections.getCompoundTagAt(0);
            byte[] blocks = requireBytes(section, "Blocks", 4096,
                    "cube " + describe(chunkX, cubeY, chunkZ));
            byte[] data = requireBytes(section, "Data", 2048,
                    "cube " + describe(chunkX, cubeY, chunkZ));
            byte[] add = section.hasKey("Add")
                    ? requireBytes(section, "Add", 2048,
                    "cube " + describe(chunkX, cubeY, chunkZ)) : null;
            byte[] add2 = section.hasKey("Add2")
                    ? requireBytes(section, "Add2", 2048,
                    "cube " + describe(chunkX, cubeY, chunkZ)) : null;
            if (add2 != null && hasNonZero(add2)) {
                throw fail(chunkX, chunkZ, "cube Y=" + cubeY
                        + " uses Add2 block-state IDs that Anvil cannot represent");
            }
            for (int index = 0; index < 4096; index++) {
                int stateId = (blocks[index] & 0xFF) << 4 | nibble(data, index)
                        | (add == null ? 0 : nibble(add, index) << 12);
                if (stateId == 0) {
                    continue;
                }
                int localX = index & 15;
                int localY = index >> 8 & 15;
                int localZ = index >> 4 & 15;
                int height = (cubeY << 4) + localY + 1;
                int heightIndex = localZ << 4 | localX;
                if (height > result[heightIndex]) {
                    result[heightIndex] = height;
                }
            }
        }
        return result;
    }

    private static int nibble(byte[] array, int index) {
        int packed = array[index >> 1] & 0xFF;
        return index % 2 == 0 ? packed & 15 : packed >> 4 & 15;
    }

    private static boolean isOutOfRangePayloadEmpty(NBTTagCompound cube,
            int chunkX, int cubeY, int chunkZ)
            throws CubicColumnConversionException {
        if (hasDynamicPayload(cube)) {
            return false;
        }
        if (cube.hasKey("Sections") && !cube.hasKey("Sections", 9)) {
            throw new CubicColumnConversionException(
                    "An out-of-range cube has a non-list Sections tag");
        }
        if (!cube.hasKey("Sections", 9)) {
            return true;
        }
        NBTTagList sections = cube.getTagList("Sections", 10);
        if (sections.tagCount() == 0) {
            return true;
        }
        if (sections.tagCount() != 1) {
            return false;
        }
        NBTTagCompound section = sections.getCompoundTagAt(0);
        rejectUnknownKeys(section, KNOWN_SECTION_KEYS,
                "cube " + describe(chunkX, cubeY, chunkZ) + " section");
        byte[] blocks = requireBytes(section, "Blocks", 4096,
                "cube " + describe(chunkX, cubeY, chunkZ));
        byte[] data = requireBytes(section, "Data", 2048,
                "cube " + describe(chunkX, cubeY, chunkZ));
        byte[] add = section.hasKey("Add") ? requireBytes(section, "Add", 2048,
                "cube " + describe(chunkX, cubeY, chunkZ)) : new byte[0];
        byte[] add2 = section.hasKey("Add2") ? requireBytes(section, "Add2", 2048,
                "cube " + describe(chunkX, cubeY, chunkZ)) : new byte[0];
        return !hasNonZero(blocks) && !hasNonZero(data)
                && !hasNonZero(add) && !hasNonZero(add2);
    }

    private static boolean isDiscardableEmptyLookahead(NBTTagCompound cube,
            int chunkX, int cubeY, int chunkZ) throws CubicColumnConversionException {
        if (cube.getBoolean("populated") || cube.getBoolean("fullyPopulated")
                || hasDynamicPayload(cube)) {
            return false;
        }
        if (!cube.hasKey("Sections")) {
            return true;
        }
        if (!cube.hasKey("Sections", 9)) {
            throw fail(chunkX, chunkZ, "cube-only Y=" + cubeY + " has non-list Sections");
        }
        NBTTagList sections = cube.getTagList("Sections", 10);
        if (sections.tagCount() > 1) {
            return false;
        }
        if (sections.tagCount() == 1) {
            NBTTagCompound section = sections.getCompoundTagAt(0);
            rejectUnknownKeys(section, KNOWN_SECTION_KEYS,
                    "cube " + describe(chunkX, cubeY, chunkZ) + " section");
            byte[] blocks = requireBytes(section, "Blocks", 4096,
                    "cube " + describe(chunkX, cubeY, chunkZ));
            byte[] data = requireBytes(section, "Data", 2048,
                    "cube " + describe(chunkX, cubeY, chunkZ));
            byte[] add = section.hasKey("Add") ? requireBytes(section, "Add", 2048,
                    "cube " + describe(chunkX, cubeY, chunkZ)) : new byte[0];
            byte[] add2 = section.hasKey("Add2") ? requireBytes(section, "Add2", 2048,
                    "cube " + describe(chunkX, cubeY, chunkZ)) : new byte[0];
            requireBytes(section, "BlockLight", 2048,
                    "cube " + describe(chunkX, cubeY, chunkZ));
            validateOptionalNibble(section, "SkyLight", chunkX, cubeY, chunkZ);
            if (hasNonZero(blocks) || hasNonZero(data)
                    || hasNonZero(add) || hasNonZero(add2)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasDynamicPayload(NBTTagCompound cube)
            throws CubicColumnConversionException {
        if (cube.hasKey("ForgeCaps", 10) && !cube.getCompoundTag("ForgeCaps").hasNoTags()) {
            return true;
        }
        for (String key : Arrays.asList("Entities", "TileEntities", "TileTicks")) {
            if (cube.hasKey(key) && !cube.hasKey(key, 9)) {
                throw new CubicColumnConversionException(
                        "A cubic save has a non-list " + key + " tag");
            }
            if (cube.hasKey(key, 9) && cube.getTagList(key, 10).tagCount() > 0) {
                return true;
            }
        }
        return false;
    }

    private static int contentVersion(NBTTagCompound cubeRoot) {
        if (!cubeRoot.hasKey(CONTENT_ROOT, 10)) {
            return 0;
        }
        NBTTagCompound marker = cubeRoot.getCompoundTag(CONTENT_ROOT);
        return marker.hasKey(CONTENT_VERSION, 99) ? marker.getInteger(CONTENT_VERSION) : 0;
    }

    private static void rejectUnknownKeys(NBTTagCompound compound, Set<String> known,
            String description) throws CubicColumnConversionException {
        for (String key : compound.getKeySet()) {
            if (!known.contains(key)) {
                throw new CubicColumnConversionException(
                        description + " contains unsupported data tag '" + key + "'");
            }
        }
    }

    private static NBTTagCompound requireCompound(NBTTagCompound parent, String key,
            String description) throws CubicColumnConversionException {
        if (!parent.hasKey(key, 10)) {
            throw new CubicColumnConversionException(description + " has no compound " + key);
        }
        return parent.getCompoundTag(key);
    }

    private static void requireVersion(NBTTagCompound level, String description)
            throws CubicColumnConversionException {
        if (!level.hasKey("v", 99) || level.getByte("v") != 1) {
            throw new CubicColumnConversionException(description + " has unsupported storage version "
                    + level.getByte("v"));
        }
    }

    private static int requireInt(NBTTagCompound parent, String key, String description)
            throws CubicColumnConversionException {
        if (!parent.hasKey(key, 99)) {
            throw new CubicColumnConversionException(description + " has no numeric " + key);
        }
        return parent.getInteger(key);
    }

    private static byte[] requireBytes(NBTTagCompound parent, String key, int expectedLength,
            String description) throws CubicColumnConversionException {
        if (!parent.hasKey(key, 7)) {
            throw new CubicColumnConversionException(description + " has no byte-array " + key);
        }
        byte[] value = parent.getByteArray(key);
        if (expectedLength >= 0 && value.length != expectedLength) {
            throw new CubicColumnConversionException(description + " has " + key + " length "
                    + value.length + ", expected " + expectedLength);
        }
        return value;
    }

    private static void validateOptionalNibble(NBTTagCompound section, String key,
            int chunkX, int cubeY, int chunkZ) throws CubicColumnConversionException {
        if (section.hasKey(key)) {
            requireBytes(section, key, 2048, "cube " + describe(chunkX, cubeY, chunkZ));
        }
    }

    private static boolean hasNonZero(byte[] values) {
        for (byte value : values) {
            if (value != 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInTargetRange(int cubeY) {
        return cubeY >= MIN_SECTION_Y && cubeY < MAX_SECTION_Y_EXCLUSIVE;
    }

    private static int section(double blockCoordinate) {
        return floor(blockCoordinate) >> 4;
    }

    private static int floor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    private static String localPosition(int index) {
        return "local x=" + (index & 15) + ", z=" + (index >> 4);
    }

    private static String describe(int x, int z) {
        return "column (" + x + "," + z + ")";
    }

    private static String describe(int x, int y, int z) {
        return "(" + x + "," + y + "," + z + ")";
    }

    private static CubicColumnConversionException fail(int chunkX, int chunkZ, String message) {
        return new CubicColumnConversionException(describe(chunkX, chunkZ) + " " + message);
    }
}

package net.celestiald.cavesnotcliffs.migration;

import net.celestiald.cavesnotcliffs.world.CavesNotCliffsWorldData;
import net.celestiald.cavesnotcliffs.world.TerrainProfile;
import net.celestiald.cavesnotcliffs.world.V118BiomeMapper;
import net.celestiald.cavesnotcliffs.world.V118BlockStateMapper;
import net.celestiald.cavesnotcliffs.worldgen.v118.TerrainColumn;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Material;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118NoiseRouterData;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118TerrainColumnGenerator;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldType;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Read-only oracle for proving that an unpopulated schema-2 cubic lookahead cube contains exactly
 * the native terrain that the finite generator would regenerate.
 *
 * <p>The saved FML registry snapshot is deliberately authoritative. CubicChunks serialized a
 * state as {@code (saved block id << 4) | registered metadata}; runtime Forge state ids are not a
 * compatible substitute. Lighting arrays are shape-checked but not value-compared because they
 * are not part of the deterministic terrain generator payload.</p>
 *
 * <p>Instances cache complete generated columns and are intended for the importer's existing
 * single-threaded staging pass. Verification never changes the supplied NBT.</p>
 */
final class LegacySchema2LookaheadOracle {
    private static final int SECTION_BLOCK_COUNT = 16 * 16 * 16;
    private static final int NIBBLE_ARRAY_LENGTH = SECTION_BLOCK_COUNT / 2;
    private static final int CUBE_BIOME_COUNT = 4 * 4 * 4;
    private static final Set<String> SECTION_KEYS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(
                    "Y", "Blocks", "Data", "Add", "Add2", "BlockLight", "SkyLight")));

    private final V118TerrainColumnGenerator generator;
    private final int[] savedStateIds;
    private final int[] savedBiomeIds;

    static LegacySchema2LookaheadOracle create(CavesNotCliffsWorldData contract, long seed,
            LegacyCubicRegistrySnapshot registry, V118BlockStateMapper blockStates)
            throws CubicColumnConversionException {
        if (blockStates == null) {
            throw new CubicColumnConversionException(
                    "Schema-2 lookahead oracle has no registered block-state mapper");
        }
        return create(contract, seed, registry, new MaterialStateResolver() {
            @Override
            public IBlockState stateFor(V118Material material) {
                return blockStates.stateFor(material);
            }
        });
    }

    /** Package-visible resolver seam keeps registry serialization tests independent of Forge globals. */
    static LegacySchema2LookaheadOracle create(CavesNotCliffsWorldData contract, long seed,
            LegacyCubicRegistrySnapshot registry, MaterialStateResolver blockStates)
            throws CubicColumnConversionException {
        V118NoiseRouterData.Profile profile = validateGeneratorContract(contract);
        if (registry == null) {
            throw new CubicColumnConversionException(
                    "Schema-2 lookahead oracle has no raw saved registry snapshot");
        }
        if (blockStates == null) {
            throw new CubicColumnConversionException(
                    "Schema-2 lookahead oracle has no block-state resolver");
        }

        int[] stateIds = resolveSavedStateIds(registry, blockStates);
        int[] biomeIds = resolveSavedBiomeIds(registry);
        try {
            return new LegacySchema2LookaheadOracle(
                    new V118TerrainColumnGenerator(seed, profile), stateIds, biomeIds);
        } catch (RuntimeException exception) {
            throw new CubicColumnConversionException(
                    "Could not construct the schema-2 native terrain oracle for " + profile,
                    exception);
        }
    }

    private LegacySchema2LookaheadOracle(V118TerrainColumnGenerator generator,
            int[] savedStateIds, int[] savedBiomeIds) {
        this.generator = generator;
        this.savedStateIds = savedStateIds.clone();
        this.savedBiomeIds = savedBiomeIds.clone();
    }

    void verifyCube(int cubeX, int cubeY, int cubeZ, NBTTagCompound cubeRoot)
            throws CubicColumnConversionException {
        if (cubeY < TerrainColumn.MIN_CUBE_Y || cubeY > TerrainColumn.MAX_CUBE_Y) {
            throw fail(cubeX, cubeY, cubeZ, "cube Y is outside the schema-2 terrain range "
                    + TerrainColumn.MIN_CUBE_Y + ".." + TerrainColumn.MAX_CUBE_Y);
        }
        if (cubeRoot == null || !cubeRoot.hasKey("Level", 10)) {
            throw fail(cubeX, cubeY, cubeZ, "has no compound Level payload");
        }
        NBTTagCompound level = cubeRoot.getCompoundTag("Level");
        requireCoordinate(level, "x", cubeX, cubeX, cubeY, cubeZ);
        requireCoordinate(level, "y", cubeY, cubeX, cubeY, cubeZ);
        requireCoordinate(level, "z", cubeZ, cubeX, cubeY, cubeZ);

        ExpectedCube expected;
        try {
            expected = generateExpected(cubeX, cubeY, cubeZ);
        } catch (RuntimeException exception) {
            throw new CubicColumnConversionException("Could not generate schema-2 terrain for "
                    + describe(cubeX, cubeY, cubeZ), exception);
        }
        verifyBiomes(level, expected.biomes3d, cubeX, cubeY, cubeZ);
        verifySection(level, expected, cubeX, cubeY, cubeZ);
    }

    private ExpectedCube generateExpected(int cubeX, int cubeY, int cubeZ) {
        TerrainColumn column = generator.column(cubeX, cubeZ);
        char[] materialIds = new char[SECTION_BLOCK_COUNT];
        column.copyCubeMaterialIds(cubeY, materialIds, 0);

        boolean hasNonAir = false;
        boolean hasAdd = false;
        byte[] blocks = new byte[SECTION_BLOCK_COUNT];
        byte[] data = new byte[NIBBLE_ARRAY_LENGTH];
        byte[] add = new byte[NIBBLE_ARRAY_LENGTH];
        for (int index = 0; index < materialIds.length; ++index) {
            V118Material material = V118Material.fromStorageId(materialIds[index]);
            hasNonAir |= material != V118Material.AIR;
            int stateId = savedStateIds[material.ordinal()];
            blocks[index] = (byte) (stateId >>> 4);
            setNibble(data, index, stateId & 15);
            int extendedBlockId = stateId >>> 12 & 15;
            if (extendedBlockId != 0) {
                hasAdd = true;
                setNibble(add, index, extendedBlockId);
            }
        }

        byte[] biomes3d = new byte[CUBE_BIOME_COUNT];
        int centerQuartY = cubeY * 4 + 2;
        for (int quartZ = 0; quartZ < TerrainColumn.QUART_WIDTH; ++quartZ) {
            for (int quartX = 0; quartX < TerrainColumn.QUART_WIDTH; ++quartX) {
                int generatedBiomeId = column.virtualBiomeIdAtQuart(
                        quartX, centerQuartY, quartZ);
                if (generatedBiomeId < 0 || generatedBiomeId >= savedBiomeIds.length) {
                    throw new IllegalStateException("Generated biome storage id is invalid: "
                            + generatedBiomeId);
                }
                byte savedBiomeId = (byte) savedBiomeIds[generatedBiomeId];
                for (int quartY = 0; quartY < TerrainColumn.QUART_WIDTH; ++quartY) {
                    biomes3d[quartX | quartY << 2 | quartZ << 4] = savedBiomeId;
                }
            }
        }
        return new ExpectedCube(hasNonAir, blocks, data, hasAdd ? add : null, biomes3d);
    }

    private static void verifyBiomes(NBTTagCompound level, byte[] expected, int cubeX,
            int cubeY, int cubeZ) throws CubicColumnConversionException {
        byte[] actual = requireByteArray(level, "Biomes3D", CUBE_BIOME_COUNT,
                cubeX, cubeY, cubeZ);
        compareBytes("Biomes3D", expected, actual, cubeX, cubeY, cubeZ);
    }

    private static void verifySection(NBTTagCompound level, ExpectedCube expected,
            int cubeX, int cubeY, int cubeZ) throws CubicColumnConversionException {
        if (!level.hasKey("Sections")) {
            if (expected.hasSection) {
                throw fail(cubeX, cubeY, cubeZ,
                        "is missing its expected non-air terrain section");
            }
            return;
        }
        NBTBase rawSections = level.getTag("Sections");
        if (!(rawSections instanceof NBTTagList)) {
            throw fail(cubeX, cubeY, cubeZ, "has a non-list Sections payload");
        }
        NBTTagList sections = (NBTTagList) rawSections;
        if (sections.tagCount() == 0) {
            if (expected.hasSection) {
                throw fail(cubeX, cubeY, cubeZ,
                        "has an empty Sections list for expected non-air terrain");
            }
            return;
        }
        if (sections.getTagType() != 10 || sections.tagCount() != 1) {
            throw fail(cubeX, cubeY, cubeZ,
                    "does not contain exactly one compound terrain section");
        }
        if (!expected.hasSection) {
            throw fail(cubeX, cubeY, cubeZ,
                    "contains a section where native schema-2 terrain is all air");
        }

        NBTTagCompound section = sections.getCompoundTagAt(0);
        if (!SECTION_KEYS.containsAll(section.getKeySet())) {
            Set<String> unknown = new HashSet<String>(section.getKeySet());
            unknown.removeAll(SECTION_KEYS);
            throw fail(cubeX, cubeY, cubeZ,
                    "section contains unsupported keys " + unknown);
        }
        if (section.hasKey("Y") && !section.hasKey("Y", 99)) {
            throw fail(cubeX, cubeY, cubeZ, "section has a non-numeric Y tag");
        }

        compareBytes("Blocks", expected.blocks,
                requireByteArray(section, "Blocks", SECTION_BLOCK_COUNT,
                        cubeX, cubeY, cubeZ), cubeX, cubeY, cubeZ);
        compareBytes("Data", expected.data,
                requireByteArray(section, "Data", NIBBLE_ARRAY_LENGTH,
                        cubeX, cubeY, cubeZ), cubeX, cubeY, cubeZ);

        if (expected.add == null) {
            if (section.hasKey("Add")) {
                throw fail(cubeX, cubeY, cubeZ,
                        "has an unexpected Add nibble array for saved block IDs below 256");
            }
        } else {
            compareBytes("Add", expected.add,
                    requireByteArray(section, "Add", NIBBLE_ARRAY_LENGTH,
                            cubeX, cubeY, cubeZ), cubeX, cubeY, cubeZ);
        }
        if (section.hasKey("Add2")) {
            throw fail(cubeX, cubeY, cubeZ,
                    "uses an Add2 state-ID array outside the supported saved registry contract");
        }

        requireByteArray(section, "BlockLight", NIBBLE_ARRAY_LENGTH, cubeX, cubeY, cubeZ);
        requireByteArray(section, "SkyLight", NIBBLE_ARRAY_LENGTH, cubeX, cubeY, cubeZ);
    }

    private static byte[] requireByteArray(NBTTagCompound tag, String key, int length,
            int cubeX, int cubeY, int cubeZ) throws CubicColumnConversionException {
        if (!tag.hasKey(key, 7)) {
            throw fail(cubeX, cubeY, cubeZ,
                    "has no byte-array " + key + " payload");
        }
        byte[] value = tag.getByteArray(key);
        if (value.length != length) {
            throw fail(cubeX, cubeY, cubeZ, key + " length " + value.length
                    + " does not match " + length);
        }
        return value;
    }

    private static void compareBytes(String name, byte[] expected, byte[] actual,
            int cubeX, int cubeY, int cubeZ) throws CubicColumnConversionException {
        if (Arrays.equals(expected, actual)) {
            return;
        }
        int mismatch = 0;
        while (mismatch < expected.length && expected[mismatch] == actual[mismatch]) {
            ++mismatch;
        }
        throw fail(cubeX, cubeY, cubeZ, name + " differs from native schema-2 terrain at index "
                + mismatch + " (saved=" + (actual[mismatch] & 255)
                + ", expected=" + (expected[mismatch] & 255) + ")");
    }

    private static void requireCoordinate(NBTTagCompound level, String key, int expected,
            int cubeX, int cubeY, int cubeZ) throws CubicColumnConversionException {
        if (!level.hasKey(key, 99) || level.getInteger(key) != expected) {
            throw fail(cubeX, cubeY, cubeZ,
                    "has a missing or mismatched Level." + key + " coordinate");
        }
    }

    private static V118NoiseRouterData.Profile validateGeneratorContract(
            CavesNotCliffsWorldData contract) throws CubicColumnConversionException {
        if (contract == null) {
            throw new CubicColumnConversionException(
                    "Schema-2 lookahead oracle has no persisted generator contract");
        }
        TerrainProfile terrainProfile = contract.getTerrainProfile();
        WorldType baseType;
        V118NoiseRouterData.Profile generatorProfile;
        switch (terrainProfile) {
            case DEFAULT:
                baseType = WorldType.DEFAULT;
                generatorProfile = V118NoiseRouterData.Profile.DEFAULT;
                break;
            case LARGE_BIOMES:
                baseType = WorldType.LARGE_BIOMES;
                generatorProfile = V118NoiseRouterData.Profile.LARGE_BIOMES;
                break;
            case AMPLIFIED:
                baseType = WorldType.AMPLIFIED;
                generatorProfile = V118NoiseRouterData.Profile.AMPLIFIED;
                break;
            default:
                throw new CubicColumnConversionException("Schema-2 lookahead oracle does not "
                        + "support delegated terrain profile " + terrainProfile);
        }
        if (contract.getGeneratorOptions() == null) {
            throw new CubicColumnConversionException(
                    "Schema-2 lookahead oracle has a null persisted generator-options contract");
        }
        try {
            contract.validateGeneratorContract(CavesNotCliffsWorldData.CURRENT_SCHEMA,
                    baseType, terrainProfile);
        } catch (IllegalStateException exception) {
            throw new CubicColumnConversionException(
                    "Unsupported schema-2 native generator contract: " + exception.getMessage(),
                    exception);
        }
        return generatorProfile;
    }

    private static int[] resolveSavedStateIds(LegacyCubicRegistrySnapshot registry,
            MaterialStateResolver blockStates) throws CubicColumnConversionException {
        int[] result = new int[V118Material.values().length];
        for (V118Material material : V118Material.values()) {
            IBlockState state;
            try {
                state = blockStates.stateFor(material);
            } catch (RuntimeException exception) {
                throw new CubicColumnConversionException(
                        "Could not resolve registered state for generated material " + material,
                        exception);
            }
            if (state == null || state.getBlock() == null) {
                throw new CubicColumnConversionException(
                        "No registered state exists for generated material " + material);
            }
            Block block = state.getBlock();
            ResourceLocation registryName = block.getRegistryName();
            if (registryName == null) {
                throw new CubicColumnConversionException(
                        "Generated material " + material + " maps to an unregistered block");
            }
            Integer savedBlockId = registry.getBlockId(registryName.toString());
            if (savedBlockId == null) {
                throw new CubicColumnConversionException("Raw saved block registry has no mapping "
                        + "for generated material " + material + " (" + registryName + ")");
            }

            int metadata;
            IBlockState roundTrip;
            try {
                metadata = block.getMetaFromState(state);
                roundTrip = block.getStateFromMeta(metadata);
            } catch (RuntimeException exception) {
                throw new CubicColumnConversionException("Could not serialize registered metadata "
                        + "for generated material " + material + " (" + registryName + ")",
                        exception);
            }
            if (metadata < 0 || metadata > 15) {
                throw new CubicColumnConversionException("Registered metadata " + metadata
                        + " for generated material " + material + " (" + registryName
                        + ") is outside the saved four-bit state range");
            }
            if (!state.equals(roundTrip)) {
                throw new CubicColumnConversionException("Registered state for generated material "
                        + material + " (" + registryName + ") cannot round-trip through metadata "
                        + metadata);
            }
            result[material.ordinal()] = savedBlockId << 4 | metadata;
        }
        return result;
    }

    private static int[] resolveSavedBiomeIds(LegacyCubicRegistrySnapshot registry)
            throws CubicColumnConversionException {
        int[] result = new int[V118Biome.values().length];
        for (V118Biome biome : V118Biome.values()) {
            String registryName = V118BiomeMapper.registryId(biome);
            Integer savedBiomeId = registry.getBiomeId(registryName);
            if (savedBiomeId == null) {
                throw new CubicColumnConversionException("Raw saved biome registry has no mapping "
                        + "for generated biome " + biome.id() + " projected as " + registryName);
            }
            result[biome.ordinal()] = savedBiomeId;
        }
        return result;
    }

    private static void setNibble(byte[] values, int index, int value) {
        int packedIndex = index >>> 1;
        int shift = (index & 1) << 2;
        values[packedIndex] = (byte) ((values[packedIndex] & ~(15 << shift))
                | value << shift);
    }

    private static CubicColumnConversionException fail(int cubeX, int cubeY, int cubeZ,
            String message) {
        return new CubicColumnConversionException(
                "Cube " + describe(cubeX, cubeY, cubeZ) + " " + message);
    }

    private static String describe(int cubeX, int cubeY, int cubeZ) {
        return "(" + cubeX + "," + cubeY + "," + cubeZ + ")";
    }

    interface MaterialStateResolver {
        IBlockState stateFor(V118Material material);
    }

    private static final class ExpectedCube {
        private final boolean hasSection;
        private final byte[] blocks;
        private final byte[] data;
        private final byte[] add;
        private final byte[] biomes3d;

        private ExpectedCube(boolean hasSection, byte[] blocks, byte[] data, byte[] add,
                byte[] biomes3d) {
            this.hasSection = hasSection;
            this.blocks = blocks;
            this.data = data;
            this.add = add;
            this.biomes3d = biomes3d;
        }
    }
}

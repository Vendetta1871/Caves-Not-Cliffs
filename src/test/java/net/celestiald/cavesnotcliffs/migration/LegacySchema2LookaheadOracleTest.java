package net.celestiald.cavesnotcliffs.migration;

import net.celestiald.cavesnotcliffs.world.CavesNotCliffsWorldData;
import net.celestiald.cavesnotcliffs.world.TerrainProfile;
import net.celestiald.cavesnotcliffs.world.V118BiomeMapper;
import net.celestiald.cavesnotcliffs.worldgen.v118.TerrainColumn;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Material;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118NoiseRouterData;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118TerrainColumnGenerator;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.WorldInfo;
import org.junit.Rule;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LegacySchema2LookaheadOracleTest {
    private static final long AUTHENTIC_SEED = 298329123048979567L;

    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void verifiesAllNativeProfilesUsingSavedIdsMetadataAndCenterQuartBiomes()
            throws Exception {
        TerrainProfile[] profiles = {
            TerrainProfile.DEFAULT, TerrainProfile.LARGE_BIOMES, TerrainProfile.AMPLIFIED
        };
        int[] cubeXs = {-23, 5, 17};
        int[] cubeZs = {12, 29, -31};

        for (int profileIndex = 0; profileIndex < profiles.length; ++profileIndex) {
            Fixture fixture = fixture(profiles[profileIndex], AUTHENTIC_SEED);
            GeneratedCube generated = generatedCube(fixture, cubeXs[profileIndex], -4,
                    cubeZs[profileIndex]);
            String before = generated.root.toString();

            fixture.oracle.verifyCube(generated.cubeX, generated.cubeY, generated.cubeZ,
                    generated.root);
            fixture.oracle.verifyCube(generated.cubeX, generated.cubeY, generated.cubeZ,
                    generated.root);

            assertEquals("oracle mutated input for " + profiles[profileIndex],
                    before, generated.root.toString());
            NBTTagCompound section = section(generated.root);
            assertTrue("fixture must exercise the saved extended block-ID nibble",
                    section.hasKey("Add", 7));
            int nonAir = firstNonAir(generated.materialIds);
            assertEquals(fixture.palette.savedStateId(generated.materialIds[nonAir]),
                    decodedStateId(section, nonAir));

            byte[] biomes = generated.root.getCompoundTag("Level")
                    .getByteArray("Biomes3D");
            for (int quartZ = 0; quartZ < 4; ++quartZ) {
                for (int quartX = 0; quartX < 4; ++quartX) {
                    int base = quartX | quartZ << 4;
                    for (int quartY = 1; quartY < 4; ++quartY) {
                        assertEquals("saved cube biome projection must repeat its center quart-Y",
                                biomes[base], biomes[base | quartY << 2]);
                    }
                }
            }
        }
    }

    @Test
    public void rejectsUnsupportedSchemaProfilesAndGeneratorContracts() throws Exception {
        Palette palette = Palette.standard();
        LegacyCubicRegistrySnapshot registry = snapshot(palette, null, null);

        expectFactoryFailure(legacyContract(), registry, palette, "schema");
        expectFactoryFailure(currentContract(WorldType.FLAT, TerrainProfile.DELEGATED),
                registry, palette, "delegated terrain profile");
        expectFactoryFailure(currentContract(WorldType.DEFAULT, TerrainProfile.AMPLIFIED),
                registry, palette, "generator contract");

        WorldInfo wrongClassInfo = worldInfo(WorldType.DEFAULT, "");
        CavesNotCliffsWorldData.writeCurrent(
                wrongClassInfo, WorldType.DEFAULT, TerrainProfile.DEFAULT);
        NBTTagCompound dimensionData = wrongClassInfo.getDimensionData(0);
        dimensionData.getCompoundTag("cavesnotcliffs")
                .setString("baseTypeClass", "replaced.missing.WorldType");
        wrongClassInfo.setDimensionData(0, dimensionData);
        expectFactoryFailure(CavesNotCliffsWorldData.read(wrongClassInfo),
                registry, palette, "world type class");
    }

    @Test
    public void failsClosedOnMissingRegistryMappingsAndUnrepresentableMetadata()
            throws Exception {
        Palette standard = Palette.standard();
        String missingBlock = standard.registryName(V118Material.STONE);
        expectFactoryFailure(currentContract(WorldType.DEFAULT, TerrainProfile.DEFAULT),
                snapshot(standard, missingBlock, null), standard, "no mapping for generated material");

        String missingBiome = V118BiomeMapper.registryId(V118Biome.FROZEN_PEAKS);
        expectFactoryFailure(currentContract(WorldType.DEFAULT, TerrainProfile.DEFAULT),
                snapshot(standard, null, missingBiome), standard, "no mapping for generated biome");

        Palette oversizedMetadata = Palette.withOverride(V118Material.STONE, 16, true);
        expectFactoryFailure(currentContract(WorldType.DEFAULT, TerrainProfile.DEFAULT),
                snapshot(oversizedMetadata, null, null), oversizedMetadata,
                "outside the saved four-bit state range");

        Palette lossyMetadata = Palette.withOverride(V118Material.STONE, 3, false);
        expectFactoryFailure(currentContract(WorldType.DEFAULT, TerrainProfile.DEFAULT),
                snapshot(lossyMetadata, null, null), lossyMetadata,
                "cannot round-trip through metadata");
    }

    @Test
    public void rejectsChangedAndMalformedTerrainBiomeAndLightingArrays() throws Exception {
        Fixture fixture = fixture(TerrainProfile.DEFAULT, AUTHENTIC_SEED);
        GeneratedCube generated = generatedCube(fixture, -23, -4, 12);
        NBTTagCompound correct = generated.root;
        fixture.oracle.verifyCube(-23, -4, 12, correct);

        NBTTagCompound changedBlocks = correct.copy();
        byte[] blocks = section(changedBlocks).getByteArray("Blocks");
        blocks[37] ^= 1;
        section(changedBlocks).setByteArray("Blocks", blocks);
        expectCubeFailure(fixture.oracle, changedBlocks, "Blocks differs");

        NBTTagCompound shortData = correct.copy();
        section(shortData).setByteArray("Data", new byte[2047]);
        expectCubeFailure(fixture.oracle, shortData, "Data length 2047");

        NBTTagCompound missingAdd = correct.copy();
        section(missingAdd).removeTag("Add");
        expectCubeFailure(fixture.oracle, missingAdd, "no byte-array Add");

        NBTTagCompound add2 = correct.copy();
        section(add2).setByteArray("Add2", new byte[2048]);
        expectCubeFailure(fixture.oracle, add2, "Add2 state-ID array");

        NBTTagCompound wrongLightType = correct.copy();
        section(wrongLightType).setIntArray("BlockLight", new int[2048]);
        expectCubeFailure(fixture.oracle, wrongLightType, "no byte-array BlockLight");

        NBTTagCompound missingSkyLight = correct.copy();
        section(missingSkyLight).removeTag("SkyLight");
        expectCubeFailure(fixture.oracle, missingSkyLight, "no byte-array SkyLight");

        NBTTagCompound changedBiomes = correct.copy();
        byte[] biomes = changedBiomes.getCompoundTag("Level").getByteArray("Biomes3D");
        biomes[6] ^= 1;
        changedBiomes.getCompoundTag("Level").setByteArray("Biomes3D", biomes);
        expectCubeFailure(fixture.oracle, changedBiomes, "Biomes3D differs");

        NBTTagCompound shortBiomes = correct.copy();
        shortBiomes.getCompoundTag("Level").setByteArray("Biomes3D", new byte[63]);
        expectCubeFailure(fixture.oracle, shortBiomes, "Biomes3D length 63");

        NBTTagCompound nonListSections = correct.copy();
        nonListSections.getCompoundTag("Level").setInteger("Sections", 1);
        expectCubeFailure(fixture.oracle, nonListSections, "non-list Sections");

        NBTTagCompound duplicateSections = correct.copy();
        NBTTagList sectionList = duplicateSections.getCompoundTag("Level")
                .getTagList("Sections", 10);
        sectionList.appendTag(sectionList.getCompoundTagAt(0).copy());
        expectCubeFailure(fixture.oracle, duplicateSections, "exactly one compound");

        NBTTagCompound unknownSectionArray = correct.copy();
        section(unknownSectionArray).setByteArray("Blocks16", new byte[4096]);
        expectCubeFailure(fixture.oracle, unknownSectionArray, "unsupported keys");

        NBTTagCompound wrongCoordinate = correct.copy();
        wrongCoordinate.getCompoundTag("Level").setInteger("x", -22);
        expectCubeFailure(fixture.oracle, wrongCoordinate, "Level.x coordinate");
    }

    @Test
    public void acceptsMissingOrEmptySectionsOnlyForAllAirNativeCubes() throws Exception {
        Fixture fixture = fixture(TerrainProfile.DEFAULT, AUTHENTIC_SEED);
        GeneratedCube allAir = generatedCube(fixture, 5, 19, 29);
        assertFalse(allAir.root.getCompoundTag("Level").hasKey("Sections"));

        fixture.oracle.verifyCube(5, 19, 29, allAir.root);
        NBTTagCompound emptyList = allAir.root.copy();
        emptyList.getCompoundTag("Level").setTag("Sections", new NBTTagList());
        fixture.oracle.verifyCube(5, 19, 29, emptyList);

        NBTTagCompound inventedSection = allAir.root.copy();
        NBTTagCompound section = new NBTTagCompound();
        section.setByteArray("Blocks", new byte[4096]);
        section.setByteArray("Data", new byte[2048]);
        section.setByteArray("BlockLight", new byte[2048]);
        section.setByteArray("SkyLight", new byte[2048]);
        NBTTagList sections = new NBTTagList();
        sections.appendTag(section);
        inventedSection.getCompoundTag("Level").setTag("Sections", sections);
        expectCubeFailure(fixture.oracle, inventedSection, 5, 19, 29,
                "native schema-2 terrain is all air");
    }

    private Fixture fixture(TerrainProfile profile, long seed) throws Exception {
        Palette palette = Palette.standard();
        LegacyCubicRegistrySnapshot registry = snapshot(palette, null, null);
        LegacySchema2LookaheadOracle oracle = LegacySchema2LookaheadOracle.create(
                currentContract(worldType(profile), profile), seed, registry, palette::stateFor);
        return new Fixture(profile, seed, palette, registry, oracle);
    }

    private GeneratedCube generatedCube(Fixture fixture, int cubeX, int cubeY, int cubeZ) {
        TerrainColumn column = new V118TerrainColumnGenerator(fixture.seed,
                noiseProfile(fixture.profile)).column(cubeX, cubeZ);
        char[] materialIds = new char[TerrainColumn.BLOCKS_PER_CUBE];
        column.copyCubeMaterialIds(cubeY, materialIds, 0);

        byte[] blocks = new byte[4096];
        byte[] data = new byte[2048];
        byte[] add = new byte[2048];
        boolean nonAir = false;
        boolean hasAdd = false;
        for (int index = 0; index < materialIds.length; ++index) {
            V118Material material = V118Material.fromStorageId(materialIds[index]);
            nonAir |= material != V118Material.AIR;
            int stateId = fixture.palette.savedStateId(material);
            blocks[index] = (byte) (stateId >>> 4);
            setNibble(data, index, stateId & 15);
            int addValue = stateId >>> 12 & 15;
            if (addValue != 0) {
                hasAdd = true;
                setNibble(add, index, addValue);
            }
        }

        NBTTagCompound level = new NBTTagCompound();
        level.setInteger("x", cubeX);
        level.setInteger("y", cubeY);
        level.setInteger("z", cubeZ);
        if (nonAir) {
            NBTTagCompound section = new NBTTagCompound();
            section.setByteArray("Blocks", blocks);
            section.setByteArray("Data", data);
            if (hasAdd) {
                section.setByteArray("Add", add);
            }
            section.setByteArray("BlockLight", new byte[2048]);
            section.setByteArray("SkyLight", new byte[2048]);
            NBTTagList sections = new NBTTagList();
            sections.appendTag(section);
            level.setTag("Sections", sections);
        }

        byte[] biomes3d = new byte[64];
        int centerQuartY = cubeY * 4 + 2;
        for (int quartZ = 0; quartZ < 4; ++quartZ) {
            for (int quartX = 0; quartX < 4; ++quartX) {
                V118Biome generatedBiome = V118Biome.values()[
                        column.virtualBiomeIdAtQuart(quartX, centerQuartY, quartZ)];
                int savedBiomeId = fixture.registry.getBiomeId(
                        V118BiomeMapper.registryId(generatedBiome));
                for (int quartY = 0; quartY < 4; ++quartY) {
                    biomes3d[quartX | quartY << 2 | quartZ << 4] = (byte) savedBiomeId;
                }
            }
        }
        level.setByteArray("Biomes3D", biomes3d);

        NBTTagCompound root = new NBTTagCompound();
        root.setTag("Level", level);
        return new GeneratedCube(cubeX, cubeY, cubeZ, root, materialIds);
    }

    private LegacyCubicRegistrySnapshot snapshot(Palette palette, String omittedBlock,
            String omittedBiome) throws Exception {
        NBTTagList blockIds = new NBTTagList();
        for (V118Material material : V118Material.values()) {
            String name = palette.registryName(material);
            if (!name.equals(omittedBlock)) {
                blockIds.appendTag(entry(name, palette.savedBlockId(material)));
            }
        }

        Map<String, Integer> biomeIds = new LinkedHashMap<String, Integer>();
        int nextBiomeId = 1;
        for (V118Biome biome : V118Biome.values()) {
            String name = V118BiomeMapper.registryId(biome);
            if (!biomeIds.containsKey(name)) {
                biomeIds.put(name, nextBiomeId++);
            }
        }
        NBTTagList savedBiomes = new NBTTagList();
        for (Map.Entry<String, Integer> biome : biomeIds.entrySet()) {
            if (!biome.getKey().equals(omittedBiome)) {
                savedBiomes.appendTag(entry(biome.getKey(), biome.getValue()));
            }
        }

        NBTTagCompound registries = new NBTTagCompound();
        registries.setTag("minecraft:blocks", registry(blockIds));
        registries.setTag("minecraft:biomes", registry(savedBiomes));
        NBTTagCompound fml = new NBTTagCompound();
        fml.setTag("Registries", registries);
        NBTTagCompound root = new NBTTagCompound();
        root.setTag("FML", fml);

        Path levelFile = temporary.getRoot().toPath()
                .resolve("oracle-registry-" + System.nanoTime() + ".dat");
        try (OutputStream output = new BufferedOutputStream(Files.newOutputStream(levelFile))) {
            CompressedStreamTools.writeCompressed(root, output);
        }
        return LegacyCubicRegistrySnapshot.read(levelFile);
    }

    private static NBTTagCompound registry(NBTTagList ids) {
        NBTTagCompound registry = new NBTTagCompound();
        registry.setTag("aliases", new NBTTagList());
        registry.setIntArray("blocked", new int[0]);
        registry.setTag("ids", ids);
        registry.setTag("overrides", new NBTTagList());
        registry.setTag("dummied", new NBTTagList());
        return registry;
    }

    private static NBTTagCompound entry(String name, int id) {
        NBTTagCompound entry = new NBTTagCompound();
        entry.setString("K", name);
        entry.setInteger("V", id);
        return entry;
    }

    private static CavesNotCliffsWorldData currentContract(WorldType type,
            TerrainProfile profile) {
        WorldInfo info = worldInfo(type, "{\"retainedStructureOption\":true}");
        return CavesNotCliffsWorldData.writeCurrent(info, type, profile);
    }

    private static CavesNotCliffsWorldData legacyContract() {
        WorldInfo info = worldInfo(WorldType.DEFAULT, "");
        return CavesNotCliffsWorldData.writeLegacy(info);
    }

    private static WorldInfo worldInfo(WorldType type, String options) {
        WorldSettings settings = new WorldSettings(AUTHENTIC_SEED, GameType.SURVIVAL,
                true, false, type).setGeneratorOptions(options);
        return new WorldInfo(settings, "oracle-test");
    }

    private static WorldType worldType(TerrainProfile profile) {
        switch (profile) {
            case DEFAULT:
                return WorldType.DEFAULT;
            case LARGE_BIOMES:
                return WorldType.LARGE_BIOMES;
            case AMPLIFIED:
                return WorldType.AMPLIFIED;
            default:
                throw new AssertionError(profile);
        }
    }

    private static V118NoiseRouterData.Profile noiseProfile(TerrainProfile profile) {
        switch (profile) {
            case DEFAULT:
                return V118NoiseRouterData.Profile.DEFAULT;
            case LARGE_BIOMES:
                return V118NoiseRouterData.Profile.LARGE_BIOMES;
            case AMPLIFIED:
                return V118NoiseRouterData.Profile.AMPLIFIED;
            default:
                throw new AssertionError(profile);
        }
    }

    private static void expectFactoryFailure(CavesNotCliffsWorldData contract,
            LegacyCubicRegistrySnapshot registry, Palette palette, String message)
            throws Exception {
        try {
            LegacySchema2LookaheadOracle.create(
                    contract, AUTHENTIC_SEED, registry, palette::stateFor);
            fail("Expected oracle construction failure containing: " + message);
        } catch (CubicColumnConversionException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains(message));
        }
    }

    private static void expectCubeFailure(LegacySchema2LookaheadOracle oracle,
            NBTTagCompound cube, String message) throws Exception {
        expectCubeFailure(oracle, cube, -23, -4, 12, message);
    }

    private static void expectCubeFailure(LegacySchema2LookaheadOracle oracle,
            NBTTagCompound cube, int cubeX, int cubeY, int cubeZ, String message)
            throws Exception {
        try {
            oracle.verifyCube(cubeX, cubeY, cubeZ, cube);
            fail("Expected cube verification failure containing: " + message);
        } catch (CubicColumnConversionException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains(message));
        }
    }

    private static NBTTagCompound section(NBTTagCompound root) {
        return root.getCompoundTag("Level").getTagList("Sections", 10)
                .getCompoundTagAt(0);
    }

    private static int firstNonAir(char[] materialIds) {
        for (int index = 0; index < materialIds.length; ++index) {
            if (materialIds[index] != V118Material.AIR.storageId()) {
                return index;
            }
        }
        throw new AssertionError("Expected a non-air fixture section");
    }

    private static int decodedStateId(NBTTagCompound section, int index) {
        byte[] blocks = section.getByteArray("Blocks");
        byte[] data = section.getByteArray("Data");
        byte[] add = section.hasKey("Add", 7) ? section.getByteArray("Add") : null;
        return (blocks[index] & 255) << 4 | nibble(data, index)
                | (add == null ? 0 : nibble(add, index) << 12);
    }

    private static int nibble(byte[] values, int index) {
        return values[index >>> 1] >>> ((index & 1) << 2) & 15;
    }

    private static void setNibble(byte[] values, int index, int value) {
        int packedIndex = index >>> 1;
        int shift = (index & 1) << 2;
        values[packedIndex] = (byte) ((values[packedIndex] & ~(15 << shift))
                | value << shift);
    }

    private static final class Fixture {
        private final TerrainProfile profile;
        private final long seed;
        private final Palette palette;
        private final LegacyCubicRegistrySnapshot registry;
        private final LegacySchema2LookaheadOracle oracle;

        private Fixture(TerrainProfile profile, long seed, Palette palette,
                LegacyCubicRegistrySnapshot registry, LegacySchema2LookaheadOracle oracle) {
            this.profile = profile;
            this.seed = seed;
            this.palette = palette;
            this.registry = registry;
            this.oracle = oracle;
        }
    }

    private static final class GeneratedCube {
        private final int cubeX;
        private final int cubeY;
        private final int cubeZ;
        private final NBTTagCompound root;
        private final char[] materialIds;

        private GeneratedCube(int cubeX, int cubeY, int cubeZ, NBTTagCompound root,
                char[] materialIds) {
            this.cubeX = cubeX;
            this.cubeY = cubeY;
            this.cubeZ = cubeZ;
            this.root = root;
            this.materialIds = materialIds;
        }
    }

    private static final class Palette {
        private final EnumMap<V118Material, IBlockState> states =
                new EnumMap<V118Material, IBlockState>(V118Material.class);
        private final EnumMap<V118Material, Integer> savedBlockIds =
                new EnumMap<V118Material, Integer>(V118Material.class);

        private static Palette standard() {
            return withOverride(null, 0, true);
        }

        private static Palette withOverride(V118Material overridden, int metadata,
                boolean roundTrips) {
            Palette result = new Palette();
            for (V118Material material : V118Material.values()) {
                int encodedMetadata = material == overridden
                        ? metadata : material.ordinal() * 7 & 15;
                boolean encodedRoundTrip = material != overridden || roundTrips;
                SyntheticMetadataBlock block = new SyntheticMetadataBlock(
                        encodedMetadata, encodedRoundTrip);
                block.setRegistryName(new ResourceLocation("oracle",
                        material.name().toLowerCase(Locale.ROOT)));
                result.states.put(material, block.getDefaultState());
                result.savedBlockIds.put(material, 300 + material.ordinal());
            }
            return result;
        }

        private IBlockState stateFor(V118Material material) {
            return states.get(material);
        }

        private String registryName(V118Material material) {
            return stateFor(material).getBlock().getRegistryName().toString();
        }

        private int savedBlockId(V118Material material) {
            return savedBlockIds.get(material);
        }

        private int savedStateId(char materialStorageId) {
            return savedStateId(V118Material.fromStorageId(materialStorageId));
        }

        private int savedStateId(V118Material material) {
            IBlockState state = stateFor(material);
            return savedBlockId(material) << 4 | state.getBlock().getMetaFromState(state);
        }
    }

    private static final class SyntheticMetadataBlock extends Block {
        private final int metadata;
        private final boolean roundTrips;

        private SyntheticMetadataBlock(int metadata, boolean roundTrips) {
            super(Material.ROCK);
            this.metadata = metadata;
            this.roundTrips = roundTrips;
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return metadata;
        }

        @Override
        public IBlockState getStateFromMeta(int meta) {
            return roundTrips ? getDefaultState() : Blocks.AIR.getDefaultState();
        }
    }
}

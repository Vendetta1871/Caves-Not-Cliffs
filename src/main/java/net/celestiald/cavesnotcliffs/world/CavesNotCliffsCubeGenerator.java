package net.celestiald.cavesnotcliffs.world;

import io.github.opencubicchunks.cubicchunks.api.util.Box;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubeGeneratorsRegistry;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.CubePopulatorEvent;
import io.github.opencubicchunks.cubicchunks.api.worldgen.populator.event.PopulateCubeEvent;
import net.celestiald.cavesnotcliffs.block.BlockDarkStone;
import net.celestiald.cavesnotcliffs.block.BlockDeepslateOres;
import net.celestiald.cavesnotcliffs.block.BlockUnnamedStone;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

/**
 * Finite-height Cubic Chunks generator retained for draft-v2 schema-1 saves.
 *
 * Y=0..255 is copied from the selected vanilla 1.12 generator so surface terrain, structures and
 * seeds remain familiar.  Y=-64..-1 is a deepslate extension with worm caves and deep ores; the
 * New schema-2 worlds use {@link V118CubicChunksGenerator}; this class intentionally preserves the
 * old terrain profile to avoid seams while its population pass emits canonical content only.
 */
public final class CavesNotCliffsCubeGenerator implements ICubeGenerator {
    private static final int VANILLA_MIN_CUBE = 0;
    private static final int VANILLA_MAX_CUBE = 15;
    private static final Box DECORATOR_PREGENERATION_REQUIREMENT =
            new Box(-1, -1, -1, 1, 1, 1);

    private final World world;
    private final IChunkGenerator vanillaGenerator;
    private final TerrainProfile terrainProfile;
    private Chunk cachedVanillaChunk;
    private Biome[] cachedBiomes;
    private Method fakeWorldHeight;

    CavesNotCliffsCubeGenerator(World world, IChunkGenerator vanillaGenerator,
            TerrainProfile terrainProfile) {
        this.world = world;
        this.vanillaGenerator = vanillaGenerator;
        this.terrainProfile = terrainProfile;
    }

    TerrainProfile getTerrainProfile() {
        return terrainProfile;
    }

    @Deprecated
    @Override
    public CubePrimer generateCube(int cubeX, int cubeY, int cubeZ) {
        return generateCube(cubeX, cubeY, cubeZ, new CubePrimer());
    }

    @Override
    public CubePrimer generateCube(int cubeX, int cubeY, int cubeZ, CubePrimer primer) {
        primer.reset();
        int minY = cubeY * 16;
        if (minY < CavesNotCliffsWorldType.MIN_HEIGHT
                || minY >= CavesNotCliffsWorldType.MAX_HEIGHT) {
            return primer;
        }

        if (cubeY < 0) {
            generateDeepLayer(primer, cubeX, cubeY, cubeZ);
            LegacyCaveCarver.carve(primer, world.getSeed(), cubeX, cubeY, cubeZ);
            generateDeepOres(primer, cubeX, cubeY, cubeZ);
        } else if (cubeY <= VANILLA_MAX_CUBE) {
            copyVanillaTerrain(primer, cubeX, cubeY, cubeZ);
        }
        // Cubes 16..19 intentionally stay air. v3's mountain milestone can use that headroom.
        return primer;
    }

    @Override
    public void generateColumn(Chunk column) {
        cachedBiomes = world.getBiomeProvider().getBiomes(cachedBiomes,
                column.x * 16, column.z * 16, 16, 16);
        byte[] biomeArray = column.getBiomeArray();
        for (int index = 0; index < biomeArray.length; index++) {
            biomeArray[index] = (byte) Biome.getIdForBiome(cachedBiomes[index]);
        }
    }

    private void copyVanillaTerrain(CubePrimer primer, int cubeX, int cubeY, int cubeZ) {
        Chunk chunk = getVanillaChunk(cubeX, cubeZ);
        ExtendedBlockStorage storage = chunk.getBlockStorageArray()[cubeY];
        IBlockState deepslate = deepslateState();
        for (int localY = 0; localY < 16; localY++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                for (int localX = 0; localX < 16; localX++) {
                    IBlockState state = storage == null
                            ? Blocks.AIR.getDefaultState() : storage.get(localX, localY, localZ);
                    // Vanilla's old Y=0 bedrock roof must be removed; the new floor is at Y=-64.
                    if (state.getBlock() == Blocks.BEDROCK && cubeY == 0) {
                        state = deepslate;
                    }
                    primer.setBlockState(localX, localY, localZ, state);
                }
            }
        }
    }

    private Chunk getVanillaChunk(int cubeX, int cubeZ) {
        if (cachedVanillaChunk == null || cachedVanillaChunk.x != cubeX || cachedVanillaChunk.z != cubeZ) {
            cachedVanillaChunk = vanillaGenerator.generateChunk(cubeX, cubeZ);
        }
        return cachedVanillaChunk;
    }

    private void generateDeepLayer(CubePrimer primer, int cubeX, int cubeY, int cubeZ) {
        IBlockState deepslate = deepslateState();
        IBlockState tuff = BlockDarkStone.block == null
                ? deepslate : BlockDarkStone.block.getDefaultState();
        int minX = cubeX * 16;
        int minY = cubeY * 16;
        int minZ = cubeZ * 16;

        for (int localY = 0; localY < 16; localY++) {
            int y = minY + localY;
            for (int localZ = 0; localZ < 16; localZ++) {
                int z = minZ + localZ;
                for (int localX = 0; localX < 16; localX++) {
                    int x = minX + localX;
                    if (isBottomBedrock(x, y, z)) {
                        primer.setBlockState(localX, localY, localZ, Blocks.BEDROCK.getDefaultState());
                    } else {
                        double tuffNoise = CaveBiomeSampler.valueNoise(
                                world.getSeed() ^ 0x74A71B65L, x / 22.0, y / 18.0, z / 22.0);
                        primer.setBlockState(localX, localY, localZ,
                                tuffNoise > 0.62 ? tuff : deepslate);
                    }
                }
            }
        }
    }

    private boolean isBottomBedrock(int x, int y, int z) {
        int depth = y - CavesNotCliffsWorldType.MIN_HEIGHT;
        if (depth <= 0) {
            return true;
        }
        if (depth >= 5) {
            return false;
        }
        long hash = CaveBiomeSampler.mix64(world.getSeed()
                ^ (long) x * 73428767L ^ (long) y * 912931L ^ (long) z * 4382893L);
        double unit = (hash >>> 11) * 0x1.0p-53;
        return unit < (5 - depth) / 5.0;
    }

    private IBlockState deepslateState() {
        return BlockUnnamedStone.block == null
                ? Blocks.STONE.getDefaultState() : BlockUnnamedStone.block.getDefaultState();
    }

    private void generateDeepOres(CubePrimer primer, int cubeX, int cubeY, int cubeZ) {
        Random random = new Random(CaveBiomeSampler.mix64(world.getSeed()
                ^ (long) cubeX * 0x632BE59BD9B4E019L
                ^ (long) cubeY * 0x9E3779B97F4A7C15L
                ^ (long) cubeZ * 0x85157AF5D66D3E2BL));

        generateOre(primer, random, BlockDeepslateOres.DEEPSLATE_COAL_ORE, 9, 1, 0.35);
        generateOre(primer, random, BlockDeepslateOres.DEEPSLATE_IRON_ORE, 8, 1, 0.35);
        generateOre(primer, random, BlockDeepslateOres.DEEPSLATE_GOLD_ORE, 7, 0, 0.55);
        generateOre(primer, random, BlockDeepslateOres.DEEPSLATE_REDSTONE_ORE, 7, 1, 0.0);
        generateOre(primer, random, BlockDeepslateOres.DEEPSLATE_LAPIS_ORE, 6, 0, 0.28);
        generateOre(primer, random, BlockDeepslateOres.DEEPSLATE_DIAMOND_ORE, 6, 0, 0.24);

        Biome biome = world.getBiome(new BlockPos(cubeX * 16 + 8, cubeY * 16 + 8, cubeZ * 16 + 8));
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.MOUNTAIN)) {
            generateOre(primer, random, BlockDeepslateOres.DEEPSLATE_EMERALD_ORE, 4, 0, 0.35);
        }
    }

    private void generateOre(CubePrimer primer, Random random, Block ore, int size,
            int guaranteedAttempts, double extraAttemptChance) {
        if (ore == null) {
            return;
        }
        int attempts = guaranteedAttempts + (random.nextDouble() < extraAttemptChance ? 1 : 0);
        for (int attempt = 0; attempt < attempts; attempt++) {
            int x = random.nextInt(16);
            int y = random.nextInt(16);
            int z = random.nextInt(16);
            for (int step = 0; step < size; step++) {
                IBlockState current = primer.getBlockState(x, y, z);
                if (current.getBlock() == BlockUnnamedStone.block) {
                    primer.setBlockState(x, y, z, ore.getDefaultState());
                }
                x = clampLocal(x + random.nextInt(3) - 1);
                y = clampLocal(y + random.nextInt(3) - 1);
                z = clampLocal(z + random.nextInt(3) - 1);
            }
        }
    }

    private int clampLocal(int coordinate) {
        return Math.max(0, Math.min(15, coordinate));
    }

    @Override
    public void populate(ICube cube) {
        if (cube.getY() == 0) {
            populateVanillaColumn(cube.getX(), cube.getZ());
        }

        if (MinecraftForge.EVENT_BUS.post(new CubePopulatorEvent(world, cube))) {
            return;
        }

        CubePos position = cube.getCoords();
        Random random = new Random(CaveBiomeSampler.mix64(world.getSeed()
                ^ (long) position.getX() * 341873128712L
                ^ (long) position.getY() * 132897987541L
                ^ (long) position.getZ() * 42317861L));
        MinecraftForge.EVENT_BUS.post(new PopulateCubeEvent.Pre(world, random,
                position.getX(), position.getY(), position.getZ(), false));
        // The draft-v2 CaveBiomeDecorator emitted state-split lush aliases after load migration
        // (and could place only the lower half of small dripleaf). Do not run it in schema-1 saves:
        // retained vanilla/structure population below remains authoritative, while new schema-2
        // worlds receive the exact 1.18 feature pipeline through V118CubicChunksGenerator.
        CubeGeneratorsRegistry.generateWorld(world, random, position,
                world.getBiome(position.getCenterBlockPos()));
        MinecraftForge.EVENT_BUS.post(new PopulateCubeEvent.Post(world, random,
                position.getX(), position.getY(), position.getZ(), false));
    }

    private void populateVanillaColumn(int chunkX, int chunkZ) {
        setFakeWorldHeight(256);
        try {
            vanillaGenerator.populate(chunkX, chunkZ);
            GameRegistry.generateWorld(chunkX, chunkZ, world, vanillaGenerator, world.getChunkProvider());
        } finally {
            setFakeWorldHeight(0);
        }
    }

    private void setFakeWorldHeight(int height) {
        try {
            if (fakeWorldHeight == null) {
                fakeWorldHeight = world.getClass().getMethod("fakeWorldHeight", int.class);
            }
            fakeWorldHeight.invoke(world, height);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Cubic Chunks did not expose its vanilla-generation "
                    + "height bridge; check the installed 1.12.2 build", exception);
        }
    }

    @Override
    public Box getFullPopulationRequirements(ICube cube) {
        if (cube.getY() >= VANILLA_MIN_CUBE && cube.getY() <= VANILLA_MAX_CUBE) {
            return new Box(-1, -cube.getY(), -1,
                    0, VANILLA_MAX_CUBE - cube.getY(), 0);
        }
        return RECOMMENDED_FULL_POPULATOR_REQUIREMENT;
    }

    @Override
    public Box getPopulationPregenerationRequirements(ICube cube) {
        if (cube.getY() >= VANILLA_MIN_CUBE && cube.getY() <= VANILLA_MAX_CUBE) {
            // Merge vanilla's absolute section range with the decorator's symmetric neighbor reads.
            return new Box(-1, Math.min(-1, VANILLA_MIN_CUBE - cube.getY()), -1,
                    1, Math.max(1, VANILLA_MAX_CUBE - cube.getY()), 1);
        }
        return DECORATOR_PREGENERATION_REQUIREMENT;
    }

    @Override
    public void recreateStructures(ICube cube) {
        // Vanilla structures are column based and are recreated through the overload below.
    }

    @Override
    public void recreateStructures(Chunk column) {
        vanillaGenerator.recreateStructures(column, column.x, column.z);
    }

    @Override
    public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType type, BlockPos pos) {
        return vanillaGenerator.getPossibleCreatures(type, pos);
    }

    @Override
    public BlockPos getClosestStructure(String name, BlockPos pos, boolean findUnexplored) {
        return vanillaGenerator.getNearestStructurePos(world, name, pos, findUnexplored);
    }
}

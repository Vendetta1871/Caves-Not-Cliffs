package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavebiomes.api.ExtendedChunkAPI;
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
import java.util.List;
import java.util.Random;

/**
 * Finite-column generator retained for draft-v2 schema-1 saves.
 *
 * Y=0..255 is copied from the selected vanilla 1.12 generator so surface terrain, structures and
 * seeds remain familiar.  Y=-64..-1 is a deepslate extension with worm caves and deep ores; the
 * New schema-2 worlds use {@link V118CubicChunksGenerator}; this class intentionally preserves the
 * old terrain profile to avoid seams while its population pass emits canonical content only.
 */
public final class CavesNotCliffsCubeGenerator implements IChunkGenerator {
    private final World world;
    private final IChunkGenerator vanillaGenerator;
    private final TerrainProfile terrainProfile;

    CavesNotCliffsCubeGenerator(World world, IChunkGenerator vanillaGenerator,
            TerrainProfile terrainProfile) {
        this.world = world;
        this.vanillaGenerator = vanillaGenerator;
        this.terrainProfile = terrainProfile;
        ExtendedChunkAPI.requireRange("Caves Not Cliffs", CavesNotCliffsWorldType.MIN_HEIGHT,
                CavesNotCliffsWorldType.MAX_HEIGHT);
    }

    TerrainProfile getTerrainProfile() {
        return terrainProfile;
    }

    @Override
    public Chunk generateChunk(int chunkX, int chunkZ) {
        Chunk chunk = vanillaGenerator.generateChunk(chunkX, chunkZ);
        boolean skylight = world.provider.hasSkyLight();
        removeVanillaBedrock(chunk);
        for (int sectionY = -4; sectionY < 0; ++sectionY) {
            ExtendedBlockStorage section = ExtendedChunkAPI.getOrCreateSection(
                    chunk, sectionY << 4, skylight);
            generateDeepLayer(section, chunkX, sectionY, chunkZ);
            LegacyCaveCarver.carve(section, world.getSeed(), chunkX, sectionY, chunkZ);
            generateDeepOres(section, chunkX, sectionY, chunkZ);
        }
        chunk.generateSkylightMap();
        return chunk;
    }

    private void removeVanillaBedrock(Chunk chunk) {
        IBlockState deepslate = deepslateState();
        for (int worldY = 0; worldY <= 4; ++worldY) {
            for (int localZ = 0; localZ < 16; localZ++) {
                for (int localX = 0; localX < 16; localX++) {
                    BlockPos pos = new BlockPos((chunk.x << 4) + localX, worldY,
                            (chunk.z << 4) + localZ);
                    if (chunk.getBlockState(pos).getBlock() == Blocks.BEDROCK) {
                        chunk.setBlockState(pos, deepslate);
                    }
                }
            }
        }
    }

    private void generateDeepLayer(ExtendedBlockStorage primer,
            int cubeX, int cubeY, int cubeZ) {
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
                        primer.set(localX, localY, localZ, Blocks.BEDROCK.getDefaultState());
                    } else {
                        double tuffNoise = CaveBiomeSampler.valueNoise(
                                world.getSeed() ^ 0x74A71B65L, x / 22.0, y / 18.0, z / 22.0);
                        primer.set(localX, localY, localZ,
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

    private void generateDeepOres(ExtendedBlockStorage primer,
            int cubeX, int cubeY, int cubeZ) {
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

    private void generateOre(ExtendedBlockStorage primer, Random random, Block ore, int size,
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
                IBlockState current = primer.get(x, y, z);
                if (current.getBlock() == BlockUnnamedStone.block) {
                    primer.set(x, y, z, ore.getDefaultState());
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
    public void populate(int chunkX, int chunkZ) {
        vanillaGenerator.populate(chunkX, chunkZ);
        for (int sectionY = -4; sectionY < 0; ++sectionY) {
            FiniteSectionPos position = new FiniteSectionPos(chunkX, sectionY, chunkZ);
            Random random = new Random(CaveBiomeSampler.mix64(world.getSeed()
                    ^ (long) chunkX * 341873128712L
                    ^ (long) sectionY * 132897987541L
                    ^ (long) chunkZ * 42317861L));
            CaveBiomeDecorator.decorate(world, random, position);
        }
    }

    @Override
    public boolean generateStructures(Chunk chunk, int chunkX, int chunkZ) {
        return vanillaGenerator.generateStructures(chunk, chunkX, chunkZ);
    }

    @Override
    public void recreateStructures(Chunk column, int chunkX, int chunkZ) {
        vanillaGenerator.recreateStructures(column, chunkX, chunkZ);
    }

    @Override
    public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType type, BlockPos pos) {
        return vanillaGenerator.getPossibleCreatures(type, pos);
    }

    @Override
    public BlockPos getNearestStructurePos(World queriedWorld, String name, BlockPos pos,
            boolean findUnexplored) {
        return vanillaGenerator.getNearestStructurePos(world, name, pos, findUnexplored);
    }

    @Override
    public boolean isInsideStructure(World queriedWorld, String name, BlockPos pos) {
        return vanillaGenerator.isInsideStructure(queriedWorld, name, pos);
    }
}

package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavebiomes.api.ExtendedChunkAPI;
import net.celestiald.cavebiomes.api.IExtendedPopulationGenerator;
import net.celestiald.cavesnotcliffs.worldgen.v118.TerrainColumn;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118BiomeDecorationUnion;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118NoiseRouterData;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118TerrainColumnGenerator;
import net.celestiald.cavesnotcliffs.entity.EntityAxolotl;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.IChunkGenerator;

import java.util.List;
import java.util.Collections;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Set;

/**
 * Native schema-2 finite-column generator for the Java 1.18.2 density columns.
 *
 * <p>This generator retains the selected 1.12 chunk generator for the structure-only bridge, but
 * intentionally does not invoke its terrain or general decorator pipeline. It also does not invoke
 * the legacy cave carver or legacy deep ores. Native ordinary 1.18 ore/blob and lush-cave
 * features run through isolated decoration bridges after structure population.</p>
 */
public final class V118ChunkGenerator implements IChunkGenerator, IExtendedPopulationGenerator {
    private static final int CUBE_SIZE = 16;
    private static final Map<World, WeakReference<V118ChunkGenerator>> ACTIVE_GENERATORS =
        new WeakHashMap<World, WeakReference<V118ChunkGenerator>>();

    private final World world;
    private final TerrainProfile terrainProfile;
    private final IChunkGenerator structureGenerator;
    private final VanillaStructureBridge structures;
    private final V118TerrainColumnGenerator columns;
    private final V118BlockStateMapper blockStates;
    private final V118BiomeMapper biomes;
    private final V118ChunkSlicer slicer;
    private final V118GeodeWorldBridge geodes;
    private final V118DripstoneWorldBridge dripstones;
    private final V118OreWorldBridge ordinaryOres;
    private final V118LushCaveWorldBridge lushCaves;
    private final V118BeeTreeWorldBridge beeTrees;
    private final V118MountainSurfaceWorldBridge mountainSurface;
    private ChunkPrimer cachedStructureColumn;
    private int cachedStructureX;
    private int cachedStructureZ;

    V118ChunkGenerator(World world, TerrainProfile terrainProfile,
            IChunkGenerator structureGenerator) {
        this(world, terrainProfile, structureGenerator,
            new V118TerrainColumnGenerator(world.getSeed(), nativeProfileFor(terrainProfile)),
            V118BlockStateMapper.fromRegisteredBlocks(),
            V118BiomeMapper.fromRegisteredBiomes());
    }

    V118ChunkGenerator(World world, TerrainProfile terrainProfile,
            IChunkGenerator structureGenerator, V118TerrainColumnGenerator columns,
            V118BlockStateMapper blockStates, V118BiomeMapper biomes) {
        if (world == null) {
            throw new NullPointerException("world");
        }
        if (columns == null) {
            throw new NullPointerException("columns");
        }
        if (structureGenerator == null) {
            throw new NullPointerException("structureGenerator");
        }
        ExtendedChunkAPI.requireRange("Caves Not Cliffs", TerrainColumn.MIN_Y,
                TerrainColumn.MAX_Y_EXCLUSIVE);
        this.world = world;
        this.terrainProfile = requireNativeProfile(terrainProfile);
        this.structureGenerator = structureGenerator;
        structures = new VanillaStructureBridge(world, structureGenerator);
        this.columns = columns;
        this.blockStates = blockStates;
        this.biomes = biomes;
        slicer = new V118ChunkSlicer(blockStates, biomes);
        geodes = new V118GeodeWorldBridge(world,
            V118GeodeBlockMapper.fromRegisteredBlocks());
        V118OreBlockMapper oreBlocks = V118OreBlockMapper.fromRegisteredBlocks();
        dripstones = new V118DripstoneWorldBridge(world, this, oreBlocks);
        ordinaryOres = new V118OreWorldBridge(world, this, oreBlocks);
        lushCaves = new V118LushCaveWorldBridge(world, this);
        beeTrees = new V118BeeTreeWorldBridge(world, this);
        mountainSurface = new V118MountainSurfaceWorldBridge(world, this, blockStates);
        registerActiveGenerator(world, this);
    }

    public TerrainProfile getTerrainProfile() {
        return terrainProfile;
    }

    @Override
    public int getPopulationRadius() {
        return 1;
    }

    IChunkGenerator getStructureGenerator() {
        return structureGenerator;
    }

    public V118Biome getVirtualBiome(int blockX, int blockY, int blockZ) {
        if (!hasVirtualBiomeY(blockY)) {
            throw new IllegalArgumentException("Virtual biome Y is outside -64..319: " + blockY);
        }
        return columns.biomeAt(blockX, blockY, blockZ);
    }

    V118Biome getDecorationBiome(int blockX, int blockY, int blockZ) {
        if (!hasDecorationBiomeY(blockY)) {
            throw new IllegalArgumentException(
                "Decoration biome Y is outside -64..320: " + blockY);
        }
        return columns.biomeAt(blockX, blockY, blockZ);
    }

    /** Returns the registered 1.12 projection used for spawning and colors. */
    public Biome getRegisteredVirtualBiome(int blockX, int blockY, int blockZ) {
        return biomes.biomeFor(getVirtualBiome(blockX, blockY, blockZ));
    }

    public static V118ChunkGenerator forWorld(World world) {
        if (world == null) {
            return null;
        }
        synchronized (ACTIVE_GENERATORS) {
            WeakReference<V118ChunkGenerator> reference = ACTIVE_GENERATORS.get(world);
            return reference == null ? null : reference.get();
        }
    }

    @Override
    public Chunk generateChunk(int chunkX, int chunkZ) {
        TerrainColumn terrain = columns.column(chunkX, chunkZ);
        Chunk chunk = new Chunk(world, chunkX, chunkZ);
        boolean skylight = world.provider.hasSkyLight();
        ChunkPrimer structureColumn = structureColumn(chunkX, chunkZ, terrain);
        for (int sectionY = TerrainColumn.MIN_CUBE_Y;
                sectionY <= TerrainColumn.MAX_CUBE_Y; ++sectionY) {
            if (sectionY >= 0 && sectionY < 16) {
                slicer.sliceStructureBlocks(structureColumn, terrain, sectionY, chunk, skylight);
            } else {
                slicer.slice(terrain, sectionY, chunk, skylight);
            }
        }
        slicer.projectSurfaceBiomes(terrain, chunk.getBiomeArray());
        chunk.generateSkylightMap();
        return chunk;
    }

    @Override
    public void populate(int chunkX, int chunkZ) {
        Set<V118Biome> decorationBiomes = decorationBiomeUnion(chunkX, chunkZ);
        structures.populate(chunkX, chunkZ);
        geodes.populate(chunkX, chunkZ);
        dripstones.populateLarge(chunkX, chunkZ, decorationBiomes);
        ordinaryOres.populate(chunkX, chunkZ, decorationBiomes, dripstones);
        // FLUID_SPRINGS step 8 indices 0, 1, then 2, after underground decoration.
        mountainSurface.populateDefaultSprings(chunkX, chunkZ, decorationBiomes);
        mountainSurface.populateFrozenSprings(chunkX, chunkZ, decorationBiomes);
        // VEGETAL_DECORATION index 8 precedes the represented bee-tree sequence at 14-20.
        mountainSurface.populateEarlyDoublePlants(chunkX, chunkZ, decorationBiomes);
        beeTrees.populateBeforeLush(chunkX, chunkZ, decorationBiomes);
        // Index 21 precedes the lush-cave vegetation beginning at index 22.
        mountainSurface.populatePreLushDoublePlants(chunkX, chunkZ, decorationBiomes);
        lushCaves.populate(chunkX, chunkZ, decorationBiomes);
        beeTrees.populateAfterLush(chunkX, chunkZ, decorationBiomes);
        // Large fern index 36 follows the implemented meadow tree at 34.
        mountainSurface.populateLateDoublePlants(chunkX, chunkZ, decorationBiomes);
        // Indices 40, 47-64, 66-69, 71, 72, 74, and 75 follow.
        mountainSurface.populateVegetation(chunkX, chunkZ, decorationBiomes);
        // TOP_LAYER_MODIFICATION step 10 is the last represented decoration stage.
        mountainSurface.populateTopLayer(chunkX, chunkZ, decorationBiomes);
        TerrainColumn column = columns.column(chunkX, chunkZ);

        for (int sectionY = TerrainColumn.MIN_CUBE_Y;
                sectionY <= TerrainColumn.MAX_CUBE_Y; ++sectionY) {
            scheduleFluidTicks(column, chunkX, sectionY, chunkZ);
        }
    }

    void replayImportedFluidTicks(int chunkX, int chunkZ, int populatedMask) {
        TerrainColumn column = columns.column(chunkX, chunkZ);
        for (int sectionY : LegacySchemaTwoFluidHandler.missingBands(populatedMask)) {
            scheduleFluidTicks(column, chunkX, sectionY, chunkZ);
        }
    }

    private void scheduleFluidTicks(TerrainColumn column, int chunkX,
            int sectionY, int chunkZ) {
        slicer.forEachScheduledFluid(column, sectionY,
                (localX, localY, localZ, material) -> {
            IBlockState state = blockStates.stateFor(material);
            Block block = state.getBlock();
            BlockPos position = new BlockPos(
                chunkX * CUBE_SIZE + localX,
                sectionY * CUBE_SIZE + localY,
                chunkZ * CUBE_SIZE + localZ);
            if (world.getBlockState(position).getBlock() == block) {
                world.scheduleUpdate(position, block, block.tickRate(world));
            }
        });
    }

    @Override
    public boolean generateStructures(Chunk column, int chunkX, int chunkZ) {
        return structureGenerator.generateStructures(column, chunkX, chunkZ);
    }

    @Override
    public void recreateStructures(Chunk column, int chunkX, int chunkZ) {
        structures.recreateStructures(column);
    }

    @Override
    public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType type, BlockPos pos) {
        if (type == EnumCreatureType.WATER_CREATURE
                && getVirtualBiome(pos.getX(), pos.getY(), pos.getZ())
                    == V118Biome.LUSH_CAVES) {
            // 1.18 uses a dedicated AXOLOTLS category. Mapping that category onto 1.12's water
            // creature scheduler is narrower than using the projected surface biome and avoids
            // leaking this spawn into ordinary forest water. Tropical fish are intentionally
            // represented only by the bucket bridge in this backport.
            return AXOLOTL_SPAWNS;
        }
        return getRegisteredVirtualBiome(pos.getX(), pos.getY(), pos.getZ())
            .getSpawnableList(type);
    }

    @Override
    public BlockPos getNearestStructurePos(World queriedWorld, String name, BlockPos pos,
            boolean findUnexplored) {
        return structures.getClosestStructure(name, pos, findUnexplored);
    }

    @Override
    public boolean isInsideStructure(World queriedWorld, String name, BlockPos pos) {
        return structureGenerator.isInsideStructure(queriedWorld, name, pos);
    }

    private ChunkPrimer structureColumn(int columnX, int columnZ, TerrainColumn terrain) {
        if (cachedStructureColumn == null || cachedStructureX != columnX
                || cachedStructureZ != columnZ) {
            ChunkPrimer primer = new ChunkPrimer();
            slicer.fillStructureTerrain(terrain, primer);
            structures.generate(columnX, columnZ, primer);
            cachedStructureColumn = primer;
            cachedStructureX = columnX;
            cachedStructureZ = columnZ;
        }
        return cachedStructureColumn;
    }

    static boolean isNativeProfile(TerrainProfile profile) {
        return profile == TerrainProfile.DEFAULT
            || profile == TerrainProfile.LARGE_BIOMES
            || profile == TerrainProfile.AMPLIFIED;
    }

    static V118NoiseRouterData.Profile nativeProfileFor(TerrainProfile profile) {
        switch (requireNativeProfile(profile)) {
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

    static boolean hasVirtualBiomeY(int blockY) {
        return blockY >= TerrainColumn.MIN_Y && blockY <= TerrainColumn.MAX_Y;
    }

    static boolean hasDecorationBiomeY(int blockY) {
        return blockY >= TerrainColumn.MIN_Y
            && blockY <= TerrainColumn.MAX_Y_EXCLUSIVE;
    }

    private static void registerActiveGenerator(World world,
            V118ChunkGenerator generator) {
        synchronized (ACTIVE_GENERATORS) {
            ACTIVE_GENERATORS.put(world, new WeakReference<V118ChunkGenerator>(generator));
        }
    }

    Set<V118Biome> decorationBiomeUnion(int chunkX, int chunkZ) {
        return V118BiomeDecorationUnion.collect(chunkX, chunkZ, columns::column);
    }

    /**
     * Returns the immutable post-surface terrain used for feature-region reads beyond the
     * writable three-by-three chunk area. Earlier features cannot mutate those chunks through a
     * FEATURES-status region, so consulting the column cache avoids loading chunks recursively and
     * keeps large-feature reads independent of request order.
     */
    IBlockState rawTerrainState(int blockX, int blockY, int blockZ) {
        TerrainColumn column = columns.column(blockX >> 4, blockZ >> 4);
        return blockStates.stateFor(column.materialId(blockX & 15, blockY, blockZ & 15));
    }

    private static TerrainProfile requireNativeProfile(TerrainProfile profile) {
        if (!isNativeProfile(profile)) {
            throw new IllegalArgumentException("Not a native 1.18 terrain profile: " + profile);
        }
        return profile;
    }

    private static final List<Biome.SpawnListEntry> AXOLOTL_SPAWNS =
        Collections.singletonList(new Biome.SpawnListEntry(
            EntityAxolotl.EntityCustom.class, 10, 4, 6));
}

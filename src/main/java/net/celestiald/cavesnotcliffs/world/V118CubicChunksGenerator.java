package net.celestiald.cavesnotcliffs.world;

import io.github.opencubicchunks.cubicchunks.api.util.Box;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import io.github.opencubicchunks.cubicchunks.api.worldgen.ICubeGenerator;
import net.celestiald.cavesnotcliffs.worldgen.v118.TerrainColumn;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118NoiseRouterData;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118TerrainColumnGenerator;
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
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Native schema-2 CubicChunks bridge for the Java 1.18.2 density columns.
 *
 * <p>This generator retains the selected 1.12 chunk generator for the later structure-only bridge,
 * but intentionally does not invoke its terrain or population methods. It also does not invoke the
 * legacy cave carver, legacy deep ores, or legacy decorators. Structure bridging and 1.18 feature
 * population are separate checkpoints.</p>
 */
public final class V118CubicChunksGenerator implements ICubeGenerator {
    private static final int CUBE_SIZE = 16;
    private static final Map<World, WeakReference<V118CubicChunksGenerator>> ACTIVE_GENERATORS =
        new WeakHashMap<World, WeakReference<V118CubicChunksGenerator>>();

    private final World world;
    private final TerrainProfile terrainProfile;
    private final IChunkGenerator structureGenerator;
    private final VanillaStructureBridge structures;
    private final V118TerrainColumnGenerator columns;
    private final V118BlockStateMapper blockStates;
    private final V118BiomeMapper biomes;
    private final V118CubeSlicer slicer;
    private ChunkPrimer cachedStructureColumn;
    private int cachedStructureX;
    private int cachedStructureZ;

    V118CubicChunksGenerator(World world, TerrainProfile terrainProfile,
            IChunkGenerator structureGenerator) {
        this(world, terrainProfile, structureGenerator,
            new V118TerrainColumnGenerator(world.getSeed(), nativeProfileFor(terrainProfile)),
            V118BlockStateMapper.fromRegisteredBlocks(),
            V118BiomeMapper.fromRegisteredBiomes());
    }

    V118CubicChunksGenerator(World world, TerrainProfile terrainProfile,
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
        this.world = world;
        this.terrainProfile = requireNativeProfile(terrainProfile);
        this.structureGenerator = structureGenerator;
        structures = new VanillaStructureBridge(world, structureGenerator);
        this.columns = columns;
        this.blockStates = blockStates;
        this.biomes = biomes;
        slicer = new V118CubeSlicer(blockStates, biomes);
        registerActiveGenerator(world, this);
    }

    public TerrainProfile getTerrainProfile() {
        return terrainProfile;
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

    public static V118CubicChunksGenerator forWorld(World world) {
        if (world == null) {
            return null;
        }
        synchronized (ACTIVE_GENERATORS) {
            WeakReference<V118CubicChunksGenerator> reference = ACTIVE_GENERATORS.get(world);
            return reference == null ? null : reference.get();
        }
    }

    @Deprecated
    @Override
    public CubePrimer generateCube(int cubeX, int cubeY, int cubeZ) {
        return generateCube(cubeX, cubeY, cubeZ, new CubePrimer());
    }

    @Override
    public CubePrimer generateCube(int cubeX, int cubeY, int cubeZ, CubePrimer primer) {
        primer.reset();
        if (!isGeneratedCube(cubeY)) {
            return primer;
        }
        TerrainColumn column = columns.column(cubeX, cubeZ);
        if (cubeY >= 0 && cubeY < 16) {
            slicer.sliceStructureBlocks(structureColumn(cubeX, cubeZ, column), column, cubeY,
                primer);
        } else {
            slicer.slice(column, cubeY, primer);
        }
        return primer;
    }

    @Override
    public void generateColumn(Chunk column) {
        TerrainColumn terrain = columns.column(column.x, column.z);
        slicer.projectSurfaceBiomes(terrain, column.getBiomeArray());
    }

    @Override
    public void populate(ICube cube) {
        if (!isGeneratedCube(cube.getY())) {
            return;
        }
        if (cube.getY() == 0) {
            structures.populate(cube.getX(), cube.getZ());
        }
        TerrainColumn column = columns.column(cube.getX(), cube.getZ());

        // CubicChunks 1.12 has no proto-cube scheduled-tick list. Preserve aquifer intent at the
        // earliest safe public seam instead: when the generated cube is populated, schedule only
        // the water/lava positions explicitly retained by TerrainColumn.
        slicer.forEachScheduledFluid(column, cube.getY(), (localX, localY, localZ, material) -> {
            IBlockState state = blockStates.stateFor(material);
            Block block = state.getBlock();
            BlockPos position = new BlockPos(
                cube.getX() * CUBE_SIZE + localX,
                cube.getY() * CUBE_SIZE + localY,
                cube.getZ() * CUBE_SIZE + localZ);
            if (world.getBlockState(position).getBlock() == block) {
                world.scheduleUpdate(position, block, block.tickRate(world));
            }
        });
    }

    @Override
    public boolean supportsConcurrentCubeGeneration() {
        return false;
    }

    @Override
    public boolean supportsConcurrentColumnGeneration() {
        return false;
    }

    @Override
    public Box getFullPopulationRequirements(ICube cube) {
        return fullPopulationRequirements(cube.getY());
    }

    @Override
    public Box getPopulationPregenerationRequirements(ICube cube) {
        return populationPregenerationRequirements(cube.getY());
    }

    @Override
    public void recreateStructures(ICube cube) {
        // The structure-only bridge is a later checkpoint.
    }

    @Override
    public void recreateStructures(Chunk column) {
        structures.recreateStructures(column);
    }

    @Override
    public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType type, BlockPos pos) {
        return biomes.biomeFor(getVirtualBiome(pos.getX(), pos.getY(), pos.getZ()))
            .getSpawnableList(type);
    }

    @Override
    public BlockPos getClosestStructure(String name, BlockPos pos, boolean findUnexplored) {
        return structures.getClosestStructure(name, pos, findUnexplored);
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

    static Box fullPopulationRequirements(int cubeY) {
        if (cubeY >= 0 && cubeY < 16) {
            return new Box(-1, -cubeY, -1, 0, 15 - cubeY, 0);
        }
        return NO_REQUIREMENT;
    }

    static Box populationPregenerationRequirements(int cubeY) {
        if (cubeY >= 0 && cubeY < 16) {
            return new Box(-1, Math.min(-1, -cubeY), -1,
                1, Math.max(1, 15 - cubeY), 1);
        }
        return NO_REQUIREMENT;
    }

    static boolean hasVirtualBiomeY(int blockY) {
        return blockY >= TerrainColumn.MIN_Y && blockY <= TerrainColumn.MAX_Y;
    }

    private static void registerActiveGenerator(World world,
            V118CubicChunksGenerator generator) {
        synchronized (ACTIVE_GENERATORS) {
            ACTIVE_GENERATORS.put(world, new WeakReference<V118CubicChunksGenerator>(generator));
        }
    }

    private static TerrainProfile requireNativeProfile(TerrainProfile profile) {
        if (!isNativeProfile(profile)) {
            throw new IllegalArgumentException("Not a native 1.18 terrain profile: " + profile);
        }
        return profile;
    }

    private static boolean isGeneratedCube(int cubeY) {
        return cubeY >= TerrainColumn.MIN_CUBE_Y && cubeY <= TerrainColumn.MAX_CUBE_Y;
    }
}

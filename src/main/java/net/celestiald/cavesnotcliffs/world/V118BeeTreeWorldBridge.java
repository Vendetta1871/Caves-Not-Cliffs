package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.worldgen.v118.BeeNestDecorator;
import net.celestiald.cavesnotcliffs.content.LushCaveContent;
import net.celestiald.cavesnotcliffs.worldgen.v118.TerrainColumn;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118BeeTreeFeature;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118BeeTreeFeature.LogAxis;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118BeeTreeVegetation;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118BeeTreeVegetation.Heightmap;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118BeeTreeVegetation.Plant;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118BeeTreePlacements;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118BeeTreePlacements.PlacedFeature;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118BeeTreePlacements.TreeKind;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118WorldgenRandom;
import net.minecraft.block.material.Material;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockOldLeaf;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Set;

/**
 * Schema-2 bridge for the six Java 1.18.2 tree features that can generate bee nests.
 *
 * <p>The target runtime's oak, fancy-oak, birch, and tall-birch generators are the direct legacy
 * peers of the corresponding 1.18 trunk/foliage configurations. Only these individual configured
 * features are invoked; the old biome decorator remains disabled.</p>
 */
final class V118BeeTreeWorldBridge implements V118BeeTreeFeature.WorldAccess,
        V118BeeTreeVegetation.WorldAccess {
    private final World world;
    private final V118CubicChunksGenerator generator;

    V118BeeTreeWorldBridge(World world, V118CubicChunksGenerator generator) {
        if (world == null || generator == null) {
            throw new NullPointerException("world and generator are required");
        }
        this.world = world;
        this.generator = generator;
    }

    void populateBeforeLush(int chunkX, int chunkZ, Set<V118Biome> regionBiomes) {
        populateVegetation(chunkX, chunkZ, regionBiomes,
                V118BeeTreeVegetation.PlacedFeature.FLOWER_FOREST_FLOWERS);
        populateTree(chunkX, chunkZ, regionBiomes, PlacedFeature.TREES_FLOWER_FOREST);
        populateVegetation(chunkX, chunkZ, regionBiomes,
                V118BeeTreeVegetation.PlacedFeature.FOREST_FLOWERS);
        populateTree(chunkX, chunkZ, regionBiomes, PlacedFeature.BIRCH_TALL);
        populateTree(chunkX, chunkZ, regionBiomes, PlacedFeature.TREES_BIRCH);
        populateTree(chunkX, chunkZ, regionBiomes, PlacedFeature.TREES_BIRCH_AND_OAK);
    }

    void populateAfterLush(int chunkX, int chunkZ, Set<V118Biome> regionBiomes) {
        populateVegetation(chunkX, chunkZ, regionBiomes,
                V118BeeTreeVegetation.PlacedFeature.PATCH_SUNFLOWER);
        populateTree(chunkX, chunkZ, regionBiomes, PlacedFeature.TREES_PLAINS);
        populateVegetation(chunkX, chunkZ, regionBiomes,
                V118BeeTreeVegetation.PlacedFeature.PATCH_GRASS_PLAIN);
        populateVegetation(chunkX, chunkZ, regionBiomes,
                V118BeeTreeVegetation.PlacedFeature.FLOWER_MEADOW);
        populateTree(chunkX, chunkZ, regionBiomes, PlacedFeature.TREES_MEADOW);
    }

    private void populateTree(int chunkX, int chunkZ, Set<V118Biome> regionBiomes,
            PlacedFeature feature) {
        if (feature.appearsIn(regionBiomes)) {
            populateFeature(chunkX, chunkZ, feature);
        }
    }

    private void populateVegetation(int chunkX, int chunkZ,
            Set<V118Biome> regionBiomes,
            V118BeeTreeVegetation.PlacedFeature feature) {
        if (feature.appearsIn(regionBiomes)) {
            V118BeeTreeVegetation.place(this, world.getSeed(), chunkX, chunkZ, feature);
        }
    }

    private void populateFeature(int chunkX, int chunkZ, PlacedFeature feature) {
        V118WorldgenRandom random = V118BeeTreePlacements.randomFor(
                world.getSeed(), chunkX, chunkZ, feature);
        int attempts = feature.sampleCount(random);
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        for (int attempt = 0; attempt < attempts; ++attempt) {
            int x = originX + random.nextInt(16);
            int z = originZ + random.nextInt(16);
            Surface surface = surfaceAt(x, z);
            if (surface == null || surface.worldSurfaceY != surface.oceanFloorY) {
                continue;
            }
            int y = surface.oceanFloorY;
            if (y < TerrainColumn.MIN_Y || y >= TerrainColumn.MAX_Y_EXCLUSIVE
                    || !feature.supports(generator.getVirtualBiome(x, y, z))) {
                continue;
            }
            BlockPos treeOrigin = new BlockPos(x, y, z);
            if (feature.hasOuterSaplingPlacementFilter()
                    && !saplingHasSupport(treeOrigin)) {
                continue;
            }

            TreeKind kind = feature.selectTree(random);
            if (feature.hasInnerSaplingPlacementFilter()
                    && !saplingHasSupport(treeOrigin)) {
                continue;
            }
            V118BeeTreeFeature.Result generated = V118BeeTreeFeature.place(
                    this, random, treeOrigin, kind);
            if (!generated.placed()) {
                continue;
            }
            BeeNestDecorator.placeInWorld(world, random, feature.nestProbability(),
                    generated.trunks(), generated.foliage());
        }
    }

    private Surface surfaceAt(int x, int z) {
        int worldSurface = Integer.MIN_VALUE;
        int oceanFloor = Integer.MIN_VALUE;
        for (int y = TerrainColumn.MAX_Y; y >= TerrainColumn.MIN_Y; --y) {
            IBlockState state = world.getBlockState(new BlockPos(x, y, z));
            Material material = state.getMaterial();
            if (worldSurface == Integer.MIN_VALUE && material != Material.AIR) {
                worldSurface = y + 1;
            }
            if (material.blocksMovement()) {
                oceanFloor = y + 1;
                break;
            }
        }
        return oceanFloor == Integer.MIN_VALUE
                ? null : new Surface(worldSurface, oceanFloor);
    }

    private boolean saplingHasSupport(BlockPos origin) {
        Block block = world.getBlockState(origin.down()).getBlock();
        return isDirtTag(block) || block == Blocks.FARMLAND;
    }

    @Override
    public int height(Heightmap heightmap, int blockX, int blockZ) {
        for (int y = TerrainColumn.MAX_Y; y >= TerrainColumn.MIN_Y; --y) {
            IBlockState state = world.getBlockState(new BlockPos(blockX, y, blockZ));
            Material material = state.getMaterial();
            if (heightmap == Heightmap.WORLD_SURFACE) {
                if (material != Material.AIR) {
                    return y + 1;
                }
            } else if (material.blocksMovement() || material.isLiquid()) {
                return y + 1;
            }
        }
        return TerrainColumn.MIN_Y;
    }

    @Override
    public V118Biome biome(int blockX, int blockY, int blockZ) {
        int y = Math.max(TerrainColumn.MIN_Y,
                Math.min(TerrainColumn.MAX_Y, blockY));
        return generator.getVirtualBiome(blockX, y, blockZ);
    }

    @Override
    public boolean placePlant(BlockPos pos, Plant plant) {
        if (plant == null || isOutsideBuildHeight(pos) || !world.isAirBlock(pos)
                || !saplingHasSupport(pos)) {
            return false;
        }
        if (plant.isDoublePlant()) {
            if (pos.getY() + 1 >= TerrainColumn.MAX_Y_EXCLUSIVE
                    || !world.isAirBlock(pos.up())) {
                return false;
            }
            Blocks.DOUBLE_PLANT.placeAt(world, pos, doublePlant(plant), 2);
            return true;
        }
        world.setBlockState(pos, singlePlant(plant), 2);
        return true;
    }

    @Override
    public int minBuildHeight() {
        return TerrainColumn.MIN_Y;
    }

    @Override
    public int maxBuildHeight() {
        return TerrainColumn.MAX_Y_EXCLUSIVE;
    }

    @Override
    public boolean isFree(BlockPos pos) {
        if (isOutsideBuildHeight(pos)) {
            return false;
        }
        IBlockState state = world.getBlockState(pos);
        return isValidTreePos(pos) || state.getBlock().isWood(world, pos);
    }

    @Override
    public boolean isValidTreePos(BlockPos pos) {
        if (isOutsideBuildHeight(pos)) {
            return false;
        }
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        Material material = state.getMaterial();
        return material == Material.AIR
                || block.isLeaves(state, world, pos)
                || material == Material.PLANTS
                || material == Material.VINE
                || block == Blocks.WATER
                || block == Blocks.FLOWING_WATER;
    }

    @Override
    public boolean isDirtExceptGrassAndMycelium(BlockPos pos) {
        Block block = world.getBlockState(pos).getBlock();
        return isDirtTag(block) && block != Blocks.GRASS && block != Blocks.MYCELIUM;
    }

    @Override
    public void setDirt(BlockPos pos) {
        world.setBlockState(pos, Blocks.DIRT.getDefaultState(), 2);
    }

    @Override
    public void setLog(BlockPos pos, LogAxis axis, TreeKind kind) {
        BlockLog.EnumAxis legacyAxis;
        switch (axis) {
            case X:
                legacyAxis = BlockLog.EnumAxis.X;
                break;
            case Z:
                legacyAxis = BlockLog.EnumAxis.Z;
                break;
            case Y:
            default:
                legacyAxis = BlockLog.EnumAxis.Y;
                break;
        }
        IBlockState state = Blocks.LOG.getDefaultState()
                .withProperty(BlockOldLog.VARIANT, wood(kind))
                .withProperty(BlockLog.LOG_AXIS, legacyAxis);
        world.setBlockState(pos, state, 2);
    }

    @Override
    public void setLeaves(BlockPos pos, TreeKind kind) {
        IBlockState state = Blocks.LEAVES.getDefaultState()
                .withProperty(BlockOldLeaf.VARIANT, wood(kind))
                .withProperty(BlockLeaves.CHECK_DECAY, false)
                .withProperty(BlockLeaves.DECAYABLE, true);
        world.setBlockState(pos, state, 2);
    }

    private static BlockPlanks.EnumType wood(TreeKind kind) {
        return kind == TreeKind.BIRCH || kind == TreeKind.SUPER_BIRCH
                ? BlockPlanks.EnumType.BIRCH : BlockPlanks.EnumType.OAK;
    }

    private static BlockDoublePlant.EnumPlantType doublePlant(Plant plant) {
        switch (plant) {
            case LILAC:
                return BlockDoublePlant.EnumPlantType.SYRINGA;
            case ROSE_BUSH:
                return BlockDoublePlant.EnumPlantType.ROSE;
            case PEONY:
                return BlockDoublePlant.EnumPlantType.PAEONIA;
            case SUNFLOWER:
                return BlockDoublePlant.EnumPlantType.SUNFLOWER;
            case TALL_GRASS:
                return BlockDoublePlant.EnumPlantType.GRASS;
            default:
                throw new IllegalArgumentException("Not a double plant: " + plant);
        }
    }

    private static IBlockState singlePlant(Plant plant) {
        switch (plant) {
            case SHORT_GRASS:
                return Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE,
                        BlockTallGrass.EnumType.GRASS);
            case DANDELION:
                return Blocks.YELLOW_FLOWER.getDefaultState();
            case ALLIUM:
                return redFlower(BlockFlower.EnumFlowerType.ALLIUM);
            case POPPY:
                return redFlower(BlockFlower.EnumFlowerType.POPPY);
            case AZURE_BLUET:
                return redFlower(BlockFlower.EnumFlowerType.HOUSTONIA);
            case OXEYE_DAISY:
                return redFlower(BlockFlower.EnumFlowerType.OXEYE_DAISY);
            // Java 1.12 has no canonical states for these two later flowers. These legacy flower
            // states preserve survival, occupancy, bee-pollination, and patch geometry.
            case CORNFLOWER:
                return redFlower(BlockFlower.EnumFlowerType.BLUE_ORCHID);
            case LILY_OF_THE_VALLEY:
                return redFlower(BlockFlower.EnumFlowerType.WHITE_TULIP);
            default:
                throw new IllegalArgumentException("Not a single plant: " + plant);
        }
    }

    private static IBlockState redFlower(BlockFlower.EnumFlowerType type) {
        return Blocks.RED_FLOWER.getStateFromMeta(type.getMeta());
    }

    private static boolean isDirtTag(Block block) {
        return block == Blocks.DIRT || block == Blocks.GRASS || block == Blocks.MYCELIUM
                || block == LushCaveContent.ROOTED_DIRT
                || block == LushCaveContent.MOSS_BLOCK;
    }

    private static boolean isOutsideBuildHeight(BlockPos pos) {
        return pos.getY() < TerrainColumn.MIN_Y
                || pos.getY() >= TerrainColumn.MAX_Y_EXCLUSIVE;
    }

    private static final class Surface {
        final int worldSurfaceY;
        final int oceanFloorY;

        Surface(int worldSurfaceY, int oceanFloorY) {
            this.worldSurfaceY = worldSurfaceY;
            this.oceanFloorY = oceanFloorY;
        }
    }
}

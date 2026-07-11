package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.block.BlockPointedDripstone;
import net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics;
import net.celestiald.cavesnotcliffs.worldgen.v118.TerrainColumn;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118DripstoneFeature;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118DripstonePlacements;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118OreMaterial;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118OrePlacements;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118WorldgenRandom;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;

/** Mutable finite-column feature-region view for the exact Java 1.18.2 dripstone port. */
final class V118DripstoneWorldBridge implements V118DripstoneFeature.WorldAccess,
        V118OrePlacements.BetweenDecorationSteps {
    private static final int FEATURE_WRITE_RADIUS_CHUNKS = 1;

    private final World world;
    private final V118ChunkGenerator generator;
    private final V118OreBlockMapper oreBlocks;
    private final Block dripstoneBlock;
    private final BlockPointedDripstone dryPointed;
    private final BlockPointedDripstone waterloggedPointed;
    private final Map<Long, Integer> surfaceHeights = new HashMap<Long, Integer>();
    private int centerChunkX;
    private int centerChunkZ;

    V118DripstoneWorldBridge(World world, V118ChunkGenerator generator,
            V118OreBlockMapper oreBlocks) {
        this(world, generator, oreBlocks, registered("dripstone_block"),
            pointed("pointed_dripstone"), pointed("pointed_dripstone_waterlogged"));
    }

    V118DripstoneWorldBridge(World world, V118ChunkGenerator generator,
            V118OreBlockMapper oreBlocks, Block dripstoneBlock,
            BlockPointedDripstone dryPointed, BlockPointedDripstone waterloggedPointed) {
        if (world == null || generator == null || oreBlocks == null || dripstoneBlock == null
                || dryPointed == null || waterloggedPointed == null) {
            throw new NullPointerException("world, generator, and dripstone blocks are required");
        }
        this.world = world;
        this.generator = generator;
        this.oreBlocks = oreBlocks;
        this.dripstoneBlock = dripstoneBlock;
        this.dryPointed = dryPointed;
        this.waterloggedPointed = waterloggedPointed;
    }

    V118DripstonePlacements.PlacementResult populateLarge(int chunkX, int chunkZ,
            Set<V118Biome> regionBiomes) {
        beginFeatureRegion(chunkX, chunkZ);
        return V118DripstonePlacements.decorateLarge(this, world.getSeed(), chunkX, chunkZ,
            regionBiomes);
    }

    @Override
    public void decorate(long decorationSeed, int chunkX, int chunkZ,
            Set<V118Biome> regionBiomes, V118WorldgenRandom random) {
        beginFeatureRegion(chunkX, chunkZ);
        V118DripstonePlacements.decorateUnderground(this, decorationSeed, chunkX, chunkZ,
            regionBiomes, random);
    }

    private void beginFeatureRegion(int chunkX, int chunkZ) {
        centerChunkX = chunkX;
        centerChunkZ = chunkZ;
        surfaceHeights.clear();
    }

    @Override
    public V118DripstoneFeature.State getState(int blockX, int blockY, int blockZ) {
        if (outsideBuildHeight(blockY)) {
            return V118DripstoneFeature.State.AIR;
        }
        return classify(stateAt(blockX, blockY, blockZ), oreBlocks,
            dripstoneBlock, dryPointed, waterloggedPointed);
    }

    @Override
    public boolean isWaterAt(int blockX, int blockY, int blockZ) {
        if (outsideBuildHeight(blockY)) {
            return false;
        }
        Block block = stateAt(blockX, blockY, blockZ).getBlock();
        return block == Blocks.WATER || block == Blocks.FLOWING_WATER
            || block == waterloggedPointed;
    }

    @Override
    public boolean isSolidAt(int blockX, int blockY, int blockZ) {
        return !outsideBuildHeight(blockY)
            && stateAt(blockX, blockY, blockZ).getMaterial().isSolid();
    }

    @Override
    public void setDripstoneBlock(int blockX, int blockY, int blockZ) {
        setIfWritable(blockX, blockY, blockZ, dripstoneBlock.getDefaultState());
    }

    @Override
    public void setPointedDripstone(int blockX, int blockY, int blockZ,
            V118DripstoneFeature.Direction direction,
            V118DripstoneFeature.Thickness thickness, boolean waterlogged) {
        BlockPointedDripstone block = waterlogged ? waterloggedPointed : dryPointed;
        IBlockState state = block.getDefaultState()
            .withProperty(BlockPointedDripstone.TIP_DIRECTION, facing(direction))
            .withProperty(BlockPointedDripstone.THICKNESS, runtimeThickness(thickness));
        setIfWritable(blockX, blockY, blockZ, state);
    }

    @Override
    public void setWater(int blockX, int blockY, int blockZ) {
        setIfWritable(blockX, blockY, blockZ, Blocks.WATER.getDefaultState());
    }

    private void setIfWritable(int blockX, int blockY, int blockZ, IBlockState state) {
        if (!outsideBuildHeight(blockY)
                && withinWriteRadius(centerChunkX, centerChunkZ, blockX, blockZ)) {
            world.setBlockState(new BlockPos(blockX, blockY, blockZ), state, 2);
        }
    }

    @Override
    public int worldSurfaceHeight(int blockX, int blockZ) {
        long key = ((long) blockX & 0xffffffffL) << 32 | (blockZ & 0xffffffffL);
        Integer cached = surfaceHeights.get(key);
        if (cached != null) {
            return cached.intValue();
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int height = firstAvailableSurfaceY(TerrainColumn.MIN_Y, TerrainColumn.MAX_Y,
            blockY -> {
                cursor.setPos(blockX, blockY, blockZ);
                return stateAt(cursor.getX(), cursor.getY(), cursor.getZ()).getBlock()
                    != Blocks.AIR;
            });
        surfaceHeights.put(key, Integer.valueOf(height));
        return height;
    }

    @Override
    public V118Biome biomeAt(int blockX, int blockY, int blockZ) {
        return generator.getVirtualBiome(blockX, V118OreWorldBridge.clampBiomeY(blockY),
            blockZ);
    }

    @Override
    public int minBuildHeight() {
        return TerrainColumn.MIN_Y;
    }

    @Override
    public int maxBuildHeight() {
        return TerrainColumn.MAX_Y_EXCLUSIVE;
    }

    static V118DripstoneFeature.State classify(IBlockState state,
            V118OreBlockMapper oreBlocks, Block dripstoneBlock, Block dryPointed,
            Block waterloggedPointed) {
        if (state == null || state.getBlock() == Blocks.AIR) {
            return V118DripstoneFeature.State.AIR;
        }
        Block block = state.getBlock();
        if (block == Blocks.WATER || block == Blocks.FLOWING_WATER) {
            return V118DripstoneFeature.State.WATER;
        }
        if (block == Blocks.LAVA || block == Blocks.FLOWING_LAVA) {
            return V118DripstoneFeature.State.LAVA;
        }
        if (block == dripstoneBlock) {
            return V118DripstoneFeature.State.DRIPSTONE_BLOCK;
        }
        if (block == dryPointed || block == waterloggedPointed) {
            return V118DripstoneFeature.State.POINTED_DRIPSTONE;
        }
        return isDripstoneBaseMaterial(oreBlocks.materialFor(state))
            ? V118DripstoneFeature.State.BASE_STONE
            : V118DripstoneFeature.State.OTHER;
    }

    static boolean isDripstoneBaseMaterial(V118OreMaterial material) {
        return material == V118OreMaterial.STONE || material == V118OreMaterial.DEEPSLATE
            || material == V118OreMaterial.TUFF || material == V118OreMaterial.GRANITE
            || material == V118OreMaterial.DIORITE || material == V118OreMaterial.ANDESITE;
    }

    static boolean withinWriteRadius(int centerChunkX, int centerChunkZ,
            int blockX, int blockZ) {
        long deltaX = (long) centerChunkX - (blockX >> 4);
        long deltaZ = (long) centerChunkZ - (blockZ >> 4);
        return Math.abs(deltaX) <= FEATURE_WRITE_RADIUS_CHUNKS
            && Math.abs(deltaZ) <= FEATURE_WRITE_RADIUS_CHUNKS;
    }

    static int firstAvailableSurfaceY(int minimumY, int maximumY,
            IntPredicate nonAirAtY) {
        if (nonAirAtY == null) {
            throw new NullPointerException("nonAirAtY");
        }
        for (int blockY = maximumY; blockY >= minimumY; --blockY) {
            if (nonAirAtY.test(blockY)) {
                return blockY + 1;
            }
        }
        return minimumY;
    }

    static EnumFacing facing(V118DripstoneFeature.Direction direction) {
        if (direction == V118DripstoneFeature.Direction.UP) {
            return EnumFacing.UP;
        }
        if (direction == V118DripstoneFeature.Direction.DOWN) {
            return EnumFacing.DOWN;
        }
        throw new IllegalArgumentException("Pointed dripstone direction must be vertical: "
            + direction);
    }

    static PointedDripstoneMechanics.Thickness runtimeThickness(
            V118DripstoneFeature.Thickness thickness) {
        return PointedDripstoneMechanics.Thickness.valueOf(thickness.name());
    }

    private IBlockState stateAt(int blockX, int blockY, int blockZ) {
        if (withinWriteRadius(centerChunkX, centerChunkZ, blockX, blockZ)) {
            return world.getBlockState(new BlockPos(blockX, blockY, blockZ));
        }
        return generator.rawTerrainState(blockX, blockY, blockZ);
    }

    private boolean outsideBuildHeight(int blockY) {
        return blockY < TerrainColumn.MIN_Y || blockY >= TerrainColumn.MAX_Y_EXCLUSIVE;
    }

    private static BlockPointedDripstone pointed(String path) {
        Block block = registered(path);
        if (!(block instanceof BlockPointedDripstone)) {
            throw new IllegalStateException("Required pointed-dripstone block has wrong type: "
                + path);
        }
        return (BlockPointedDripstone) block;
    }

    private static Block registered(String path) {
        Block block = GameRegistry.findRegistry(Block.class).getValue(
            new ResourceLocation("cavesnotcliffs", path));
        if (block == null) {
            throw new IllegalStateException("Required dripstone-decoration block is not "
                + "registered: " + path);
        }
        return block;
    }
}

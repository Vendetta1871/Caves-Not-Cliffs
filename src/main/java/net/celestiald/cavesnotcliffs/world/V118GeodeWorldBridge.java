package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.worldgen.v118.TerrainColumn;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118GeodeFeature;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118GeodePlacements;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Mutable finite-world view used by the exact amethyst-geode placed feature. */
final class V118GeodeWorldBridge implements V118GeodeFeature.WorldAccess {
    private final World world;
    private final V118GeodeBlockMapper blocks;

    V118GeodeWorldBridge(World world, V118GeodeBlockMapper blocks) {
        if (world == null || blocks == null) {
            throw new NullPointerException("world and blocks are required");
        }
        this.world = world;
        this.blocks = blocks;
    }

    V118GeodePlacements.PlacementResult populate(int chunkX, int chunkZ) {
        return V118GeodePlacements.decorate(this, world.getSeed(), chunkX, chunkZ);
    }

    @Override
    public boolean isAir(int x, int y, int z) {
        return outside(y) || world.isAirBlock(new BlockPos(x, y, z));
    }

    @Override
    public boolean isGeodeInvalid(int x, int y, int z) {
        return outside(y) || isInvalidBlock(world.getBlockState(new BlockPos(x, y, z)));
    }

    @Override
    public boolean canReplace(int x, int y, int z) {
        if (outside(y)) {
            return false;
        }
        BlockPos pos = new BlockPos(x, y, z);
        return world.getTileEntity(pos) == null && !isProtectedBlock(world.getBlockState(pos));
    }

    @Override
    public boolean canClusterGrowAt(int x, int y, int z) {
        if (outside(y)) {
            return false;
        }
        IBlockState state = world.getBlockState(new BlockPos(x, y, z));
        return state.getBlock() == Blocks.AIR || isSourceLiquid(state, Material.WATER);
    }

    @Override
    public boolean isWaterSource(int x, int y, int z) {
        return !outside(y) && isSourceLiquid(
            world.getBlockState(new BlockPos(x, y, z)), Material.WATER);
    }

    @Override
    public boolean hasFluid(int x, int y, int z) {
        if (outside(y)) {
            return false;
        }
        Material material = world.getBlockState(new BlockPos(x, y, z)).getMaterial();
        return material == Material.WATER || material == Material.LAVA;
    }

    @Override
    public void setState(int x, int y, int z, V118GeodeFeature.State state) {
        if (!outside(y)) {
            world.setBlockState(new BlockPos(x, y, z), blocks.stateFor(state), 2);
        }
    }

    @Override
    public void scheduleFluidUpdate(int x, int y, int z) {
        if (outside(y)) {
            return;
        }
        BlockPos pos = new BlockPos(x, y, z);
        Block block = world.getBlockState(pos).getBlock();
        if (block instanceof BlockLiquid) {
            world.scheduleUpdate(pos, block, block.tickRate(world));
        }
    }

    static boolean isInvalidBlock(IBlockState state) {
        Block block = state.getBlock();
        return block == Blocks.BEDROCK || block == Blocks.WATER
            || block == Blocks.FLOWING_WATER || block == Blocks.LAVA
            || block == Blocks.FLOWING_LAVA || block == Blocks.ICE
            || block == Blocks.PACKED_ICE;
    }

    static boolean isProtectedBlock(IBlockState state) {
        Block block = state.getBlock();
        return block == Blocks.BEDROCK || block == Blocks.MOB_SPAWNER
            || block == Blocks.CHEST || block == Blocks.END_PORTAL_FRAME;
    }

    static boolean isSourceLiquid(IBlockState state, Material material) {
        return state.getMaterial() == material && state.getBlock() instanceof BlockLiquid
            && state.getValue(BlockLiquid.LEVEL) == 0;
    }

    private static boolean outside(int y) {
        return y < TerrainColumn.MIN_Y || y > TerrainColumn.MAX_Y;
    }
}

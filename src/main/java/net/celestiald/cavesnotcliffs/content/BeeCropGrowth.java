package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.block.LushCaveVinesBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockStem;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Exact BeeGrowCropGoal state transitions, including cave-vine no-count semantics. */
public final class BeeCropGrowth {
    private BeeCropGrowth() {
    }

    public static Result grow(WorldAccess world, BlockPos pos) {
        IBlockState state = world.getState(pos);
        Block block = state.getBlock();
        if (block instanceof LushCaveVinesBlock.Head
                || block instanceof LushCaveVinesBlock.Body) {
            world.setState(pos, state.withProperty(LushCaveVinesBlock.BERRIES, true));
            // 1.18 calls performBonemeal, then continues without the level event/count branch.
            return Result.CAVE_VINE;
        }
        IBlockState grown = null;
        if (block instanceof BlockCrops
                && state.getValue(BlockCrops.AGE) < ((BlockCrops) block).getMaxAge()) {
            grown = state.withProperty(BlockCrops.AGE,
                    state.getValue(BlockCrops.AGE) + 1);
        } else if (block instanceof BlockStem
                && state.getValue(BlockStem.AGE) < 7) {
            grown = state.withProperty(BlockStem.AGE,
                    state.getValue(BlockStem.AGE) + 1);
        }
        if (grown == null) {
            return Result.NONE;
        }
        world.growthEvent(pos);
        world.setState(pos, grown);
        return Result.COUNTED_CROP;
    }

    public static Result grow(World world, BlockPos pos) {
        return grow(new MinecraftWorldAccess(world), pos);
    }

    public enum Result {
        NONE(false, false),
        CAVE_VINE(false, false),
        COUNTED_CROP(true, true);

        private final boolean emitsGrowthEvent;
        private final boolean incrementsPollinationCount;

        Result(boolean emitsGrowthEvent, boolean incrementsPollinationCount) {
            this.emitsGrowthEvent = emitsGrowthEvent;
            this.incrementsPollinationCount = incrementsPollinationCount;
        }

        public boolean emitsGrowthEvent() {
            return emitsGrowthEvent;
        }

        public boolean incrementsPollinationCount() {
            return incrementsPollinationCount;
        }
    }

    public interface WorldAccess {
        IBlockState getState(BlockPos pos);

        void setState(BlockPos pos, IBlockState state);

        void growthEvent(BlockPos pos);
    }

    private static final class MinecraftWorldAccess implements WorldAccess {
        private final World world;

        MinecraftWorldAccess(World world) {
            this.world = world;
        }

        @Override
        public IBlockState getState(BlockPos pos) {
            return world.getBlockState(pos);
        }

        @Override
        public void setState(BlockPos pos, IBlockState state) {
            world.setBlockState(pos, state, 3);
        }

        @Override
        public void growthEvent(BlockPos pos) {
            world.playEvent(2005, pos, 0);
        }
    }
}

package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.content.CampfireContent;
import net.celestiald.cavesnotcliffs.content.LightningRodContent;
import net.celestiald.cavesnotcliffs.content.LushCaveContent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/** Reads source water retained by Java 1.12 companion blocks and packed states. */
public final class CncFluidState {
    private CncFluidState() {
    }

    public static boolean containsWater(IBlockState state) {
        if (state == null) {
            return false;
        }
        Block block = state.getBlock();
        if (block == Blocks.WATER || block == Blocks.FLOWING_WATER
                || LushWaterlogging.isWaterlogged(state)) {
            return true;
        }
        if (block instanceof BlockAmethystGrowth) {
            return ((BlockAmethystGrowth) block).isWaterlogged(state);
        }
        if (block instanceof BlockPointedDripstone) {
            return ((BlockPointedDripstone) block).isWaterloggedStorage();
        }
        if (block instanceof LightningRodContent.LightningRodBlock) {
            return ((LightningRodContent.LightningRodBlock) block).isWaterloggedStorage();
        }
        return block instanceof CampfireContent.BlockCustom
                && state.getValue(CampfireContent.BlockCustom.WATERLOGGED);
    }

    public static boolean isSourceWater(IBlockState state) {
        if (state == null || state.getBlock() != Blocks.WATER
                && state.getBlock() != Blocks.FLOWING_WATER) {
            return false;
        }
        return state.getValue(BlockLiquid.LEVEL) == 0;
    }

    /** The represented Java 1.18.2 blocks that can retain a water source. */
    public static boolean isKnownWaterContainer(IBlockState state) {
        if (state == null) {
            return false;
        }
        Block block = state.getBlock();
        return block instanceof BlockAmethystGrowth
            || block instanceof BlockPointedDripstone
            || block instanceof LushDripleafBlocks.Small
            || block instanceof LushDripleafBlocks.Head
            || block instanceof LushDripleafBlocks.Stem
            || block instanceof LushAzaleaBlocks.HangingRoots
            || block instanceof BlockCandle
            || block instanceof CampfireContent.BlockCustom
            || block instanceof LightningRodContent.LightningRodBlock;
    }

    public static boolean canPlaceSourceWater(IBlockState state) {
        return isKnownWaterContainer(state) && !containsWater(state);
    }

    /**
     * Places source water into a first-party storage state. Inventory, stats and the common
     * bucket-empty sound remain the caller's responsibility.
     */
    public static boolean placeSourceWater(@Nullable Entity source, World world,
            BlockPos pos, IBlockState state) {
        if (!canPlaceSourceWater(state)) {
            return false;
        }

        Block block = state.getBlock();
        boolean placed;
        if (block instanceof BlockAmethystGrowth) {
            BlockAmethystGrowth growth = (BlockAmethystGrowth) block;
            BlockAmethystGrowth target = growth.getWaterloggedCompanion();
            placed = target != null && world.setBlockState(pos, target.getDefaultState()
                .withProperty(BlockAmethystGrowth.FACING,
                    state.getValue(BlockAmethystGrowth.FACING))
                .withProperty(BlockAmethystGrowth.WATERLOGGED, true), 3);
        } else if (block instanceof BlockPointedDripstone) {
            Block target = BlockPointedDripstoneWaterlogged.block;
            placed = target instanceof BlockPointedDripstone && world.setBlockState(pos,
                BlockPointedDripstone.copyStorageState(state,
                    (BlockPointedDripstone) target), 3);
        } else if (block instanceof LushDripleafBlocks.Small
                || block instanceof LushDripleafBlocks.Stem) {
            placed = world.setBlockState(pos,
                state.withProperty(LushDripleafBlocks.WATERLOGGED, true), 3);
        } else if (block instanceof LushDripleafBlocks.Head) {
            placed = world.setBlockState(pos, LushDripleafBlocks.headState(
                state.getValue(LushDripleafBlocks.FACING), true,
                state.getValue(LushDripleafBlocks.TILT)), 3);
        } else if (block instanceof LushAzaleaBlocks.HangingRoots) {
            Block target = LushCaveContent.HANGING_ROOTS_WATERLOGGED;
            placed = target != null && world.setBlockState(pos, target.getDefaultState(), 3);
        } else if (block instanceof BlockCandle) {
            if (state.getValue(BlockCandle.LIT)) {
                CandleEffects.extinguish(source instanceof EntityPlayer
                    ? (EntityPlayer) source : null, world, pos, state);
                state = world.getBlockState(pos);
            }
            placed = world.setBlockState(pos, state
                .withProperty(BlockCandle.WATERLOGGED, true)
                .withProperty(BlockCandle.LIT, false), 3);
        } else if (block instanceof CampfireContent.BlockCustom) {
            placed = ((CampfireContent.BlockCustom) block)
                .placeSourceWater(source, world, pos, state);
        } else if (block instanceof LightningRodContent.LightningRodBlock) {
            placed = ((LightningRodContent.LightningRodBlock) block)
                .placeSourceWater(world, pos, state);
        } else {
            placed = false;
        }

        if (placed) {
            LushWaterlogging.schedule(world, pos, true);
        }
        return placed;
    }
}

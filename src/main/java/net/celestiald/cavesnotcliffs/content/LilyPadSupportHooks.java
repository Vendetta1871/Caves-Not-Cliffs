package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.block.CncFluidState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Java 1.18.2 lily-pad support shared by generation and the vanilla-block hook. */
public final class LilyPadSupportHooks {
    private LilyPadSupportHooks() {
    }

    public static boolean canStay(World world, BlockPos lilyPos) {
        if (world == null || lilyPos == null) {
            return false;
        }
        IBlockState support = world.getBlockState(lilyPos.down());
        if (support.getMaterial() == Material.ICE) {
            return true;
        }
        Block block = support.getBlock();
        if (block == Blocks.WATER || block == Blocks.FLOWING_WATER) {
            return support.getValue(BlockLiquid.LEVEL) == 0;
        }
        // Backported waterlogged companion states retain a source-water fluid state.
        return CncFluidState.containsWater(support);
    }
}

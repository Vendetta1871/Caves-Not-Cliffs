package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.content.CampfireContent;
import net.celestiald.cavesnotcliffs.content.LightningRodContent;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

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
}

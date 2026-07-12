package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.block.LushAzaleaBlocks;
import net.celestiald.cavesnotcliffs.block.LushMossBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

/** Java 1.18.2 dirt-tag additions used by the two vanilla flower blocks. */
public final class FlowerSupportHooks {
    private FlowerSupportHooks() {
    }

    public static boolean isAdditionalSupport(Block flower, IBlockState support) {
        if ((flower != Blocks.RED_FLOWER && flower != Blocks.YELLOW_FLOWER)
                || support == null) {
            return false;
        }
        Block block = support.getBlock();
        return block == Blocks.MYCELIUM
            || block == LushCaveContent.ROOTED_DIRT
            || block == LushCaveContent.MOSS_BLOCK
            || block instanceof LushAzaleaBlocks.RootedDirt
            || block instanceof LushMossBlocks.Moss;
    }
}

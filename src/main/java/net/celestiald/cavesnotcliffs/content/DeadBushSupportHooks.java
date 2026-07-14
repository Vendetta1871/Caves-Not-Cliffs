package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.block.LushAzaleaBlocks;
import net.celestiald.cavesnotcliffs.block.LushMossBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

/** Java 1.18.2 dead-bush support shared by generation and the vanilla-block hook. */
public final class DeadBushSupportHooks {
    private DeadBushSupportHooks() {
    }

    public static boolean isJava118Support(IBlockState state) {
        if (state == null) {
            return false;
        }
        Block block = state.getBlock();
        return block == Blocks.DIRT || block == Blocks.SAND
            || block == Blocks.HARDENED_CLAY
            || block == Blocks.STAINED_HARDENED_CLAY
            || isAdditionalSupport(state);
    }

    /** Supports added by Java 1.18.2 beyond BlockDeadBush's original 1.12 result. */
    public static boolean isAdditionalSupport(IBlockState state) {
        if (state == null) {
            return false;
        }
        Block block = state.getBlock();
        return block == Blocks.GRASS || block == Blocks.MYCELIUM
            || block == LushCaveContent.ROOTED_DIRT
            || block == LushCaveContent.MOSS_BLOCK
            || block instanceof LushAzaleaBlocks.RootedDirt
            || block instanceof LushMossBlocks.Moss;
    }
}

package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.block.LushAzaleaBlocks;
import net.celestiald.cavesnotcliffs.content.LushCaveContent;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Runtime mapping of TreeFeature's Java 1.18.2 valid-tree-position predicates. */
final class V118TreeStateRules {
    private V118TreeStateRules() {
    }

    static boolean isValidTreePos(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        return block == Blocks.AIR
            || isLeaves(block, state, world, pos)
            || isReplaceablePlant(block)
            || block == Blocks.WATER
            || block == Blocks.FLOWING_WATER;
    }

    private static boolean isLeaves(Block block, IBlockState state,
            World world, BlockPos pos) {
        return block.isLeaves(state, world, pos)
            || block == LushCaveContent.AZALEA_LEAVES
            || block == LushCaveContent.FLOWERING_AZALEA_LEAVES
            || block instanceof LushAzaleaBlocks.AzaleaLeaves;
    }

    private static boolean isReplaceablePlant(Block block) {
        return block == Blocks.TALLGRASS
            || block == Blocks.DEADBUSH
            || block == Blocks.VINE
            || block == Blocks.DOUBLE_PLANT
            || block == LushCaveContent.HANGING_ROOTS
            || block == LushCaveContent.HANGING_ROOTS_WATERLOGGED
            || block instanceof LushAzaleaBlocks.HangingRoots
            || hasPath(block, "glow_lichen");
    }

    private static boolean hasPath(Block block, String path) {
        return block.getRegistryName() != null
            && path.equals(block.getRegistryName().getResourcePath());
    }
}

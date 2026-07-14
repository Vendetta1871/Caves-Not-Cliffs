package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.block.LushAzaleaBlocks;
import net.celestiald.cavesnotcliffs.block.LushMossBlocks;
import net.celestiald.cavesnotcliffs.content.LushCaveContent;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118BeeTreeFeature.LogAxis;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118BeeTreePlacements.TreeKind;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
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

    static boolean canSaplingSurvive(World world, BlockPos pos) {
        Block block = world.getBlockState(pos.down()).getBlock();
        return isDirtTag(block) || block == Blocks.FARMLAND;
    }

    static boolean isDirtExceptGrassAndMycelium(World world, BlockPos pos) {
        Block block = world.getBlockState(pos).getBlock();
        return isDirtTag(block) && block != Blocks.GRASS && block != Blocks.MYCELIUM;
    }

    static boolean isDirtTag(Block block) {
        return block == Blocks.DIRT || block == Blocks.GRASS || block == Blocks.MYCELIUM
            || block == LushCaveContent.ROOTED_DIRT || block == LushCaveContent.MOSS_BLOCK
            || block instanceof LushAzaleaBlocks.RootedDirt
            || block instanceof LushMossBlocks.Moss;
    }

    static void setLog(World world, BlockPos pos, LogAxis axis, TreeKind kind) {
        int axisMetadata = axis == LogAxis.X ? 4 : axis == LogAxis.Z ? 8 : 0;
        world.setBlockState(pos,
            Blocks.LOG.getStateFromMeta(woodMetadata(kind) | axisMetadata), 2);
    }

    static void setLeaves(World world, BlockPos pos, TreeKind kind) {
        world.setBlockState(pos, Blocks.LEAVES.getStateFromMeta(woodMetadata(kind))
            .withProperty(BlockLeaves.CHECK_DECAY, false)
            .withProperty(BlockLeaves.DECAYABLE, true), 2);
    }

    private static int woodMetadata(TreeKind kind) {
        return kind == TreeKind.BIRCH || kind == TreeKind.SUPER_BIRCH ? 2 : 0;
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

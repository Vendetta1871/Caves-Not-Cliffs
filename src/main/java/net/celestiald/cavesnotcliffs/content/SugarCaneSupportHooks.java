package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.block.CncFluidState;
import net.celestiald.cavesnotcliffs.block.LushAzaleaBlocks;
import net.celestiald.cavesnotcliffs.block.LushMossBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Additional Java 1.18.2 sugar-cane persistence accepted by the BlockReed hook. */
public final class SugarCaneSupportHooks {
    private static final EnumFacing[] WATER_ORDER = {
        EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST
    };

    private SugarCaneSupportHooks() {
    }

    public static boolean canPlaceOnFutureSupport(World world, BlockPos canePos) {
        if (world == null || canePos == null) {
            return false;
        }
        BlockPos supportPos = canePos.down();
        IBlockState support = world.getBlockState(supportPos);
        if (!isJava118Ground(support)) {
            return false;
        }
        for (EnumFacing direction : WATER_ORDER) {
            IBlockState neighbor = world.getBlockState(supportPos.offset(direction));
            if (CncFluidState.containsWater(neighbor)
                    || neighbor.getBlock() == Blocks.FROSTED_ICE) {
                return true;
            }
        }
        return false;
    }

    static boolean isJava118Ground(IBlockState state) {
        if (state == null) {
            return false;
        }
        Block block = state.getBlock();
        return block == Blocks.DIRT || block == Blocks.GRASS
            || block == Blocks.MYCELIUM || block == Blocks.SAND
            || block == LushCaveContent.ROOTED_DIRT
            || block == LushCaveContent.MOSS_BLOCK
            || block instanceof LushAzaleaBlocks.RootedDirt
            || block instanceof LushMossBlocks.Moss;
    }
}

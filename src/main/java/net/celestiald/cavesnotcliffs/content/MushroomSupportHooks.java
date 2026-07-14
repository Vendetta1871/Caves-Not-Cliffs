package net.celestiald.cavesnotcliffs.content;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

/** Java 1.18.2 small-mushroom survival used by generation and extended-height persistence. */
public final class MushroomSupportHooks {
    private static final int OVERWORLD_MIN_Y = -64;
    private static final int OVERWORLD_MAX_Y_EXCLUSIVE = 320;

    private MushroomSupportHooks() {
    }

    public static boolean canStay(World world, BlockPos mushroomPos) {
        if (world == null || mushroomPos == null) {
            return false;
        }
        BlockPos supportPos = mushroomPos.down();
        IBlockState support = world.getBlockState(supportPos);
        if (isMushroomGrowBlock(support)) {
            return true;
        }
        if (rawBrightness(world, mushroomPos) >= 13) {
            return false;
        }
        Block block = support.getBlock();
        return !block.isLeaves(support, world, supportPos)
            && support.isOpaqueCube() && support.isFullCube();
    }

    /** Selects positions whose vanilla survival result must use the 1.18.2 contract. */
    public static boolean usesJava118Survival(World world, BlockPos mushroomPos) {
        if (world == null || mushroomPos == null || world.provider == null) {
            return false;
        }
        int y = mushroomPos.getY();
        return world.provider.getDimension() == 0
            && y >= OVERWORLD_MIN_Y && y < OVERWORLD_MAX_Y_EXCLUSIVE;
    }

    private static boolean isMushroomGrowBlock(IBlockState support) {
        Block block = support.getBlock();
        return block == Blocks.MYCELIUM
            || (block == Blocks.DIRT
                && support.getValue(BlockDirt.VARIANT) == BlockDirt.DirtType.PODZOL);
    }

    private static int rawBrightness(World world, BlockPos pos) {
        // World#getLight retains a vanilla negative-Y shortcut. CaveBiomesAPI exposes the
        // extended values through getLightFor, matching 1.18's offset-zero raw brightness.
        int sky = world.getLightFor(EnumSkyBlock.SKY, pos);
        int block = world.getLightFor(EnumSkyBlock.BLOCK, pos);
        return Math.max(sky, block);
    }
}

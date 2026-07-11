package net.celestiald.cavesnotcliffs.content;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Runtime predicates injected into the two missing 1.12 piston adhesion sites. */
public final class HoneyPistonHooks {
    private HoneyPistonHooks() {
    }

    public static boolean canStickBehind(World world, BlockPos behind,
            EnumFacing pushDirection) {
        IBlockState previous = world.getBlockState(behind.offset(pushDirection));
        IBlockState candidate = world.getBlockState(behind);
        return canStickToEachOther(previous, candidate);
    }

    public static boolean canAttachBranch(World world, BlockPos source,
            EnumFacing direction) {
        return canStickToEachOther(world.getBlockState(source),
                world.getBlockState(source.offset(direction)));
    }

    public static boolean canStickToEachOther(IBlockState first, IBlockState second) {
        boolean firstHoney = isHoney(first.getBlock());
        boolean secondHoney = isHoney(second.getBlock());
        boolean firstSlime = first.getBlock() == Blocks.SLIME_BLOCK;
        boolean secondSlime = second.getBlock() == Blocks.SLIME_BLOCK;
        if ((firstHoney && secondSlime) || (firstSlime && secondHoney)) {
            return false;
        }
        return first.getBlock().isStickyBlock(first)
                || second.getBlock().isStickyBlock(second);
    }

    private static boolean isHoney(Block block) {
        if (block instanceof HoneyContent.HoneyBlockCustom) {
            return true;
        }
        ResourceLocation id = block.getRegistryName();
        return CavesNotCliffsIds.HONEY_BLOCK.equals(id);
    }

    /** Isolated holder avoids loading the full mod registry class during core bootstrap. */
    private static final class CavesNotCliffsIds {
        static final ResourceLocation HONEY_BLOCK =
                new ResourceLocation("cavesnotcliffs", "honey_block");
    }
}

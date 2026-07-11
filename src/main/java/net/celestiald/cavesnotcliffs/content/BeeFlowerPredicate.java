package net.celestiald.cavesnotcliffs.content;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/** Position-aware Java 1.18 flower-tag adaptation for 1.12 double plants. */
public final class BeeFlowerPredicate {
    private BeeFlowerPredicate() {
    }

    public static boolean isFlower(IBlockAccess world, BlockPos pos) {
        return resolveFlower(world, pos) != null;
    }

    /** BeePollinateGoal's tag predicate plus its sunflower-upper-half exception. */
    public static boolean isPollinationTarget(IBlockAccess world, BlockPos pos) {
        FlowerState flower = resolveFlower(world, pos);
        return flower != null
                && (flower.variant != BlockDoublePlant.EnumPlantType.SUNFLOWER
                    || flower.half == BlockDoublePlant.EnumBlockHalf.UPPER);
    }

    private static FlowerState resolveFlower(IBlockAccess world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof BlockFlower) {
            return FlowerState.SMALL;
        }
        if (block instanceof BlockDoublePlant) {
            return resolveFloweringDoublePlant(world, pos, state);
        }
        return isFloweringAzalea(block) ? FlowerState.SMALL : null;
    }

    public static boolean isFlowerItem(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemBlock)) {
            return false;
        }
        Block block = ((ItemBlock) stack.getItem()).getBlock();
        if (block instanceof BlockFlower) {
            return true;
        }
        if (block instanceof BlockDoublePlant) {
            BlockDoublePlant.EnumPlantType variant =
                    BlockDoublePlant.EnumPlantType.byMetadata(stack.getMetadata());
            return isTaggedDoublePlant(variant);
        }
        return isFloweringAzalea(block);
    }

    private static FlowerState resolveFloweringDoublePlant(IBlockAccess world, BlockPos pos,
            IBlockState state) {
        BlockDoublePlant.EnumBlockHalf half = state.getValue(BlockDoublePlant.HALF);
        BlockPos lowerPos = half == BlockDoublePlant.EnumBlockHalf.UPPER
                ? pos.down() : pos;
        IBlockState lower = world.getBlockState(lowerPos);
        if (!(lower.getBlock() instanceof BlockDoublePlant)
                || lower.getValue(BlockDoublePlant.HALF)
                    != BlockDoublePlant.EnumBlockHalf.LOWER) {
            return null;
        }
        BlockDoublePlant.EnumPlantType variant =
                lower.getValue(BlockDoublePlant.VARIANT);
        if (!isTaggedDoublePlant(variant)) {
            return null;
        }
        return new FlowerState(variant, half);
    }

    private static boolean isTaggedDoublePlant(BlockDoublePlant.EnumPlantType variant) {
        return variant == BlockDoublePlant.EnumPlantType.SUNFLOWER
                || variant == BlockDoublePlant.EnumPlantType.SYRINGA
                || variant == BlockDoublePlant.EnumPlantType.ROSE
                || variant == BlockDoublePlant.EnumPlantType.PAEONIA;
    }

    private static boolean isFloweringAzalea(Block block) {
        return block.getRegistryName() != null
                && block.getRegistryName().getResourcePath()
                    .contains("flowering_azalea");
    }

    private static final class FlowerState {
        static final FlowerState SMALL = new FlowerState(null, null);

        final BlockDoublePlant.EnumPlantType variant;
        final BlockDoublePlant.EnumBlockHalf half;

        FlowerState(BlockDoublePlant.EnumPlantType variant,
                BlockDoublePlant.EnumBlockHalf half) {
            this.variant = variant;
            this.half = half;
        }
    }
}

package net.celestiald.cavesnotcliffs.powdersnow;

import net.celestiald.cavesnotcliffs.block.BlockPowderSnow;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Java 1.18.2 dispenser placement and pickup for the solid powder-snow bucket. */
public final class PowderSnowDispenserBehavior {
    private PowderSnowDispenserBehavior() {
    }

    public static void register() {
        if (BlockPowderSnow.block == null || BlockPowderSnow.bucket == null) {
            throw new IllegalStateException("Powder snow content was not registered before init");
        }
        IBehaviorDispenseItem originalEmptyBucket =
            BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Items.BUCKET);
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(
            BlockPowderSnow.bucket, new FilledPowderSnowBucket());
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(
            Items.BUCKET, new EmptyBucket(originalEmptyBucket));
    }

    private static final class FilledPowderSnowBucket extends BehaviorDefaultDispenseItem {
        @Override
        protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
            World world = source.getWorld();
            EnumFacing facing = source.getBlockState().getValue(BlockDispenser.FACING);
            BlockPos target = source.getBlockPos().offset(facing);
            // SolidBucketItem#emptyContents in 1.18 accepts only an empty target for dispensers.
            if (!world.isAirBlock(target)) {
                return super.dispenseStack(source, stack);
            }
            if (!world.setBlockState(target, BlockPowderSnow.block.getDefaultState(), 3)) {
                return super.dispenseStack(source, stack);
            }
            world.playSound(null, target, BlockPowderSnow.BUCKET_EMPTY_SOUND,
                SoundCategory.BLOCKS, 1.0F, 1.0F);
            stack.shrink(1);
            return new ItemStack(Items.BUCKET);
        }
    }

    private static final class EmptyBucket implements IBehaviorDispenseItem {
        private final IBehaviorDispenseItem fallback;

        private EmptyBucket(IBehaviorDispenseItem fallback) {
            this.fallback = fallback == null ? IBehaviorDispenseItem.DEFAULT_BEHAVIOR : fallback;
        }

        @Override
        public ItemStack dispense(IBlockSource source, ItemStack stack) {
            World world = source.getWorld();
            EnumFacing facing = source.getBlockState().getValue(BlockDispenser.FACING);
            BlockPos target = source.getBlockPos().offset(facing);
            if (world.getBlockState(target).getBlock() != BlockPowderSnow.block) {
                // Preserve vanilla/Forge water, lava, and fluid-container behavior exactly.
                return fallback.dispense(source, stack);
            }

            int stateId = Block.getStateId(world.getBlockState(target));
            world.setBlockToAir(target);
            world.playEvent(2001, target, stateId);
            world.playSound(null, target, BlockPowderSnow.BUCKET_FILL_SOUND,
                SoundCategory.BLOCKS, 1.0F, 1.0F);
            world.playEvent(1000, source.getBlockPos(), 0);
            world.playEvent(2000, source.getBlockPos(), facing.getIndex());

            ItemStack filled = new ItemStack(BlockPowderSnow.bucket);
            stack.shrink(1);
            if (stack.isEmpty()) {
                return filled;
            }
            TileEntityDispenser dispenser = source.getBlockTileEntity();
            if (dispenser.addItemStack(filled) < 0) {
                IBehaviorDispenseItem.DEFAULT_BEHAVIOR.dispense(source, filled);
            }
            return stack;
        }
    }
}

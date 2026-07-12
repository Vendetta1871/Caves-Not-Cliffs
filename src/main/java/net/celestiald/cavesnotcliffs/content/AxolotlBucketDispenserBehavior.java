package net.celestiald.cavesnotcliffs.content;

import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Java 1.18.2 dispenser release for filled axolotl buckets. */
public final class AxolotlBucketDispenserBehavior extends BehaviorDefaultDispenseItem {
    public static void register(Item bucket) {
        if (bucket == null) {
            throw new IllegalStateException("Axolotl bucket was not injected before init");
        }
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(bucket,
            new AxolotlBucketDispenserBehavior());
    }

    @Override
    protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
        World world = source.getWorld();
        EnumFacing facing = source.getBlockState().getValue(BlockDispenser.FACING);
        BlockPos target = source.getBlockPos().offset(facing);
        if (!AxolotlBucketRelease.emptyWater(null, world, target, null)) {
            return super.dispenseStack(source, stack);
        }

        AxolotlBucketRelease.spawnAxolotl(world, target, stack);
        return new ItemStack(Items.BUCKET);
    }
}

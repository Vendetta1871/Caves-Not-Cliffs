package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.block.CncFluidState;
import net.celestiald.cavesnotcliffs.entity.EntityAxolotl;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.state.IBlockState;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.List;

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
        if (!emptyWater(world, target)) {
            return super.dispenseStack(source, stack);
        }

        spawnAxolotl(world, target, stack);
        return new ItemStack(Items.BUCKET);
    }

    private static boolean emptyWater(World world, BlockPos target) {
        IBlockState state = world.getBlockState(target);
        boolean knownContainer = CncFluidState.isKnownWaterContainer(state);
        boolean canPlaceInContainer = CncFluidState.canPlaceSourceWater(state);
        boolean sourceWater = CncFluidState.isSourceWater(state);
        boolean air = world.isAirBlock(target);
        boolean replaceable = !state.getMaterial().isSolid()
            || state.getBlock().isReplaceable(world, target);
        if (!air && !replaceable && !canPlaceInContainer) {
            // Unknown third-party 1.12 water containers have no common liquid-container API.
            return false;
        }

        if (world.provider.doesWaterVaporize()) {
            vaporizeWater(world, target);
            return true;
        }

        if (knownContainer) {
            // Native BucketItem ignores the container method's return once this branch wins.
            CncFluidState.placeSourceWater(null, world, target, state);
            world.playSound(null, target, AxolotlSoundEvents.BUCKET_EMPTY,
                SoundCategory.NEUTRAL, 1.0F, 1.0F);
            return true;
        }

        if (!air && replaceable && !state.getMaterial().isLiquid()) {
            world.destroyBlock(target, true);
        }
        if (!sourceWater
                && !world.setBlockState(target, Blocks.FLOWING_WATER.getDefaultState(), 11)) {
            return false;
        }
        world.playSound(null, target, AxolotlSoundEvents.BUCKET_EMPTY,
            SoundCategory.NEUTRAL, 1.0F, 1.0F);
        return true;
    }

    private static void vaporizeWater(World world, BlockPos target) {
        world.playSound(null, target, SoundEvents.BLOCK_FIRE_EXTINGUISH,
            SoundCategory.BLOCKS, 0.5F,
            2.6F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8F);
        for (int particle = 0; particle < 8; ++particle) {
            world.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                target.getX() + Math.random(), target.getY() + Math.random(),
                target.getZ() + Math.random(), 0.0D, 0.0D, 0.0D);
        }
    }

    private static void spawnAxolotl(World world, BlockPos target, ItemStack bucket) {
        EntityAxolotl.EntityCustom axolotl = new EntityAxolotl.EntityCustom(world);
        axolotl.setLocationAndAngles(target.getX() + 0.5D, target.getY() + 1.0D,
            target.getZ() + 0.5D, 0.0F, 0.0F);
        double offset = -1.0D;
        List<AxisAlignedBB> collisions = world.getCollisionBoxes(null,
            new AxisAlignedBB(target));
        for (AxisAlignedBB collision : collisions) {
            offset = collision.calculateYOffset(axolotl.getEntityBoundingBox(), offset);
        }
        float yaw = MathHelper.wrapDegrees(world.rand.nextFloat() * 360.0F);
        axolotl.setLocationAndAngles(target.getX() + 0.5D,
            target.getY() + 1.0D + offset,
            target.getZ() + 0.5D, yaw, 0.0F);
        axolotl.rotationYawHead = yaw;
        axolotl.renderYawOffset = yaw;
        axolotl.playLivingSound();
        if (bucket.hasDisplayName()) {
            axolotl.setCustomNameTag(bucket.getDisplayName());
        }
        world.spawnEntity(axolotl);
        axolotl.loadFromBucket(bucket);
    }
}

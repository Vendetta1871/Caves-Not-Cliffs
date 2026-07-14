package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.block.CncFluidState;
import net.celestiald.cavesnotcliffs.entity.EntityAxolotl;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/** Shared Java 1.18.2 emptying and mob-release behavior for axolotl buckets. */
public final class AxolotlBucketRelease {
    private AxolotlBucketRelease() {
    }

    public static boolean emptyWater(@Nullable EntityPlayer player, World world,
            BlockPos target, @Nullable RayTraceResult hit) {
        IBlockState state = world.getBlockState(target);
        boolean knownContainer = CncFluidState.isKnownWaterContainer(state);
        boolean canPlaceInContainer = CncFluidState.canPlaceSourceWater(state);
        boolean sourceWater = CncFluidState.isSourceWater(state);
        boolean air = world.isAirBlock(target);
        boolean replaceable = !state.getMaterial().isSolid()
            || state.getBlock().isReplaceable(world, target);
        if (!air && !replaceable && !canPlaceInContainer) {
            if (hit == null || hit.sideHit == null) {
                // Unknown third-party 1.12 water containers have no common liquid API.
                return false;
            }
            return emptyWater(player, world,
                hit.getBlockPos().offset(hit.sideHit), null);
        }

        if (world.provider.doesWaterVaporize()) {
            vaporizeWater(player, world, target);
            return true;
        }

        if (knownContainer) {
            // BucketItem ignores LiquidBlockContainer#placeLiquid's result once this branch wins.
            CncFluidState.placeSourceWater(player, world, target, state);
            playEmptySound(player, world, target);
            return true;
        }

        if (!world.isRemote && !air && replaceable && !state.getMaterial().isLiquid()) {
            world.destroyBlock(target, true);
        }
        if (!sourceWater
                && !world.setBlockState(target, Blocks.FLOWING_WATER.getDefaultState(), 11)) {
            return false;
        }
        playEmptySound(player, world, target);
        return true;
    }

    public static void spawnAxolotl(@Nullable EntityPlayer player, World world,
            BlockPos target, ItemStack bucket) {
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
        ItemMonsterPlacer.applyItemEntityDataToEntity(world, player, bucket, axolotl);
        world.spawnEntity(axolotl);
        axolotl.loadFromBucket(bucket);
    }

    private static void playEmptySound(@Nullable EntityPlayer player, World world,
            BlockPos target) {
        world.playSound(player, target, AxolotlSoundEvents.BUCKET_EMPTY,
            SoundCategory.NEUTRAL, 1.0F, 1.0F);
    }

    private static void vaporizeWater(@Nullable EntityPlayer player, World world,
            BlockPos target) {
        world.playSound(player, target, SoundEvents.BLOCK_FIRE_EXTINGUISH,
            SoundCategory.BLOCKS, 0.5F,
            2.6F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8F);
        for (int particle = 0; particle < 8; ++particle) {
            world.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                target.getX() + Math.random(), target.getY() + Math.random(),
                target.getZ() + Math.random(), 0.0D, 0.0D, 0.0D);
        }
    }
}

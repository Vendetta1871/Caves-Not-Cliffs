package net.celestiald.cavesnotcliffs.handler;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.block.BlockPointedDripstone;
import net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics.Thickness;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

/** Supplies the dedicated 1.18 death/damage-source identities absent from Java 1.12. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class DripstoneDamageHandler {
    public static final DamageSource FALLING_STALACTITE =
            new DamageSource("fallingStalactite");
    public static final DamageSource STALAGMITE =
            new DamageSource("stalagmite").setDamageBypassesArmor();

    private static final ThreadLocal<Boolean> TRANSLATING = new ThreadLocal<>();

    private DripstoneDamageHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttacked(LivingAttackEvent event) {
        if (Boolean.TRUE.equals(TRANSLATING.get())) {
            return;
        }
        DamageSource replacement = replacementSource(event.getEntityLiving(), event.getSource());
        if (replacement == null) {
            return;
        }
        event.setCanceled(true);
        TRANSLATING.set(Boolean.TRUE);
        try {
            event.getEntityLiving().attackEntityFrom(replacement, event.getAmount());
        } finally {
            TRANSLATING.remove();
        }
    }

    private static DamageSource replacementSource(EntityLivingBase entity,
            DamageSource original) {
        if (original == DamageSource.FALL && standingOnStalagmiteTip(entity)) {
            return STALAGMITE;
        }
        if (original == DamageSource.FALLING_BLOCK && hitByFallingStalactite(entity)) {
            return FALLING_STALACTITE;
        }
        return null;
    }

    private static boolean standingOnStalagmiteTip(EntityLivingBase entity) {
        BlockPos pos = new BlockPos(entity.posX,
                entity.getEntityBoundingBox().minY - 0.01D, entity.posZ);
        IBlockState state = entity.world.getBlockState(pos);
        return state.getBlock() instanceof BlockPointedDripstone
                && state.getValue(BlockPointedDripstone.TIP_DIRECTION) == EnumFacing.UP
                && state.getValue(BlockPointedDripstone.THICKNESS) == Thickness.TIP;
    }

    private static boolean hitByFallingStalactite(EntityLivingBase entity) {
        AxisAlignedBB search = entity.getEntityBoundingBox().grow(1.0D);
        List<EntityFallingBlock> falling = entity.world.getEntitiesWithinAABB(
                EntityFallingBlock.class, search);
        for (EntityFallingBlock candidate : falling) {
            if (candidate.getBlock().getBlock() instanceof BlockPointedDripstone
                    && candidate.getEntityBoundingBox()
                            .intersects(entity.getEntityBoundingBox())) {
                return true;
            }
        }
        return false;
    }
}

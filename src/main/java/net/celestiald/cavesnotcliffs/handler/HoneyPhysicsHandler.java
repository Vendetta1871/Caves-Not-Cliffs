package net.celestiald.cavesnotcliffs.handler;

import net.celestiald.cavesnotcliffs.content.HoneyBlockMechanics;
import net.celestiald.cavesnotcliffs.content.HoneyContent;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Supplies 1.18's block jump-factor hook on the 1.12 movement engine. */
public final class HoneyPhysicsHandler {
    public static final HoneyPhysicsHandler INSTANCE = new HoneyPhysicsHandler();

    private HoneyPhysicsHandler() {
    }

    @SubscribeEvent
    public void onLivingJump(LivingEvent.LivingJumpEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        BlockPos below = new BlockPos(entity.posX,
                entity.getEntityBoundingBox().minY - 0.2D, entity.posZ);
        if (entity.world.getBlockState(below).getBlock() == HoneyContent.honeyBlock) {
            entity.motionY *= HoneyBlockMechanics.JUMP_FACTOR;
        }
    }
}

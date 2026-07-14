package net.celestiald.cavesnotcliffs.client;

import net.celestiald.cavesnotcliffs.content.HoneyContent;
import net.celestiald.cavesnotcliffs.content.HoneySoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundEventAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Replaces 1.12's hardcoded generic drink loop only for active honey bottles. */
@SideOnly(Side.CLIENT)
public final class HoneyDrinkSoundHandler {
    public static final HoneyDrinkSoundHandler INSTANCE = new HoneyDrinkSoundHandler();
    private static final double POSITION_EPSILON = 0.01D;
    private static final ResourceLocation GENERIC_DRINK =
            new ResourceLocation("minecraft", "entity.generic.drink");

    private HoneyDrinkSoundHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlaySound(PlaySoundEvent event) {
        ISound sound = event.getResultSound();
        Minecraft minecraft = Minecraft.getMinecraft();
        if (sound == null || minecraft.world == null || HoneyContent.honeyBottle == null
                || !GENERIC_DRINK.equals(sound.getSoundLocation())
                || !hasHoneyDrinkerAt(sound, minecraft.world.loadedEntityList)) {
            return;
        }

        // Forge fires this event before resolving the weighted sound accessor. Resolve the
        // original once so its base volume/pitch can be retained by the replacement.
        SoundEventAccessor accessor = sound.createAccessor(minecraft.getSoundHandler());
        if (accessor == null) {
            return;
        }
        event.setResultSound(new PositionedSoundRecord(
                HoneySoundEvents.HONEY_DRINK, sound.getCategory(),
                sound.getVolume(), sound.getPitch(), sound.getXPosF(),
                sound.getYPosF(), sound.getZPosF()));
    }

    static boolean hasHoneyDrinkerAt(ISound sound, Iterable<? extends Entity> entities) {
        for (Entity entity : entities) {
            if (!(entity instanceof EntityLivingBase)) {
                continue;
            }
            EntityLivingBase living = (EntityLivingBase) entity;
            if (living.isHandActive() && !living.getActiveItemStack().isEmpty()
                    && living.getActiveItemStack().getItem() == HoneyContent.honeyBottle
                    && Math.abs(living.posX - sound.getXPosF()) <= POSITION_EPSILON
                    && Math.abs(living.posY - sound.getYPosF()) <= POSITION_EPSILON
                    && Math.abs(living.posZ - sound.getZPosF()) <= POSITION_EPSILON) {
                return true;
            }
        }
        return false;
    }
}

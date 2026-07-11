package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.util.SoundEvent;

/** Canonical Java 1.18.2 bee and beehive sound identities. */
public final class BeeSoundEvents {
    public static final SoundEvent BEE_DEATH = sound("entity.bee.death");
    public static final SoundEvent BEE_HURT = sound("entity.bee.hurt");
    public static final SoundEvent BEE_LOOP = sound("entity.bee.loop");
    public static final SoundEvent BEE_LOOP_AGGRESSIVE =
            sound("entity.bee.loop_aggressive");
    public static final SoundEvent BEE_POLLINATE = sound("entity.bee.pollinate");
    public static final SoundEvent BEE_STING = sound("entity.bee.sting");
    public static final SoundEvent BEEHIVE_DRIP = sound("block.beehive.drip");
    public static final SoundEvent BEEHIVE_ENTER = sound("block.beehive.enter");
    public static final SoundEvent BEEHIVE_EXIT = sound("block.beehive.exit");
    public static final SoundEvent BEEHIVE_SHEAR = sound("block.beehive.shear");
    public static final SoundEvent BEEHIVE_WORK = sound("block.beehive.work");

    private BeeSoundEvents() {
    }

    public static void registerAll() {
        register(BEE_DEATH);
        register(BEE_HURT);
        register(BEE_LOOP);
        register(BEE_LOOP_AGGRESSIVE);
        register(BEE_POLLINATE);
        register(BEE_STING);
        register(BEEHIVE_DRIP);
        register(BEEHIVE_ENTER);
        register(BEEHIVE_EXIT);
        register(BEEHIVE_SHEAR);
        register(BEEHIVE_WORK);
    }

    private static SoundEvent sound(String path) {
        return new SoundEvent(CncRegistryIds.id(path));
    }

    private static void register(SoundEvent event) {
        ElementsCavesNotCliffs.sounds.put(event.getSoundName(), event);
    }
}

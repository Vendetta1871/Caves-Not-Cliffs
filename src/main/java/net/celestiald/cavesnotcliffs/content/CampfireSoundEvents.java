package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.SoundType;
import net.minecraft.util.SoundEvent;

/** Canonical campfire and soul-soil sound identities. */
public final class CampfireSoundEvents {
    public static final SoundEvent CAMPFIRE_CRACKLE = sound("block.campfire.crackle");
    public static final SoundEvent SOUL_SOIL_BREAK = sound("block.soul_soil.break");
    public static final SoundEvent SOUL_SOIL_FALL = sound("block.soul_soil.fall");
    public static final SoundEvent SOUL_SOIL_HIT = sound("block.soul_soil.hit");
    public static final SoundEvent SOUL_SOIL_PLACE = sound("block.soul_soil.place");
    public static final SoundEvent SOUL_SOIL_STEP = sound("block.soul_soil.step");

    public static final SoundType SOUL_SOIL = new SoundType(1.0F, 1.0F,
        SOUL_SOIL_BREAK, SOUL_SOIL_STEP, SOUL_SOIL_PLACE, SOUL_SOIL_HIT,
        SOUL_SOIL_FALL);

    private CampfireSoundEvents() {
    }

    public static void registerAll() {
        register(CAMPFIRE_CRACKLE);
        register(SOUL_SOIL_BREAK);
        register(SOUL_SOIL_FALL);
        register(SOUL_SOIL_HIT);
        register(SOUL_SOIL_PLACE);
        register(SOUL_SOIL_STEP);
    }

    private static SoundEvent sound(String path) {
        return new SoundEvent(CncRegistryIds.id(path));
    }

    private static void register(SoundEvent event) {
        ElementsCavesNotCliffs.sounds.put(event.getSoundName(), event);
    }
}

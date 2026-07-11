package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;

/** Canonical Java 1.18.2 cake and candle sound identities. */
public final class CandleSoundEvents {
    public static final SoundEvent CAKE_ADD_CANDLE = sound("block.cake.add_candle");
    public static final SoundEvent CANDLE_AMBIENT = sound("block.candle.ambient");
    public static final SoundEvent CANDLE_BREAK = sound("block.candle.break");
    public static final SoundEvent CANDLE_EXTINGUISH = sound("block.candle.extinguish");
    public static final SoundEvent CANDLE_FALL = sound("block.candle.fall");
    public static final SoundEvent CANDLE_HIT = sound("block.candle.hit");
    public static final SoundEvent CANDLE_PLACE = sound("block.candle.place");
    public static final SoundEvent CANDLE_STEP = sound("block.candle.step");

    private CandleSoundEvents() {
    }

    public static void registerAll() {
        register(CAKE_ADD_CANDLE);
        register(CANDLE_AMBIENT);
        register(CANDLE_BREAK);
        register(CANDLE_EXTINGUISH);
        register(CANDLE_FALL);
        register(CANDLE_HIT);
        register(CANDLE_PLACE);
        register(CANDLE_STEP);
    }

    private static SoundEvent sound(String path) {
        ResourceLocation id = CncRegistryIds.id(path);
        return new SoundEvent(id).setRegistryName(id);
    }

    private static void register(SoundEvent sound) {
        ElementsCavesNotCliffs.sounds.put(sound.getRegistryName(), sound);
    }
}

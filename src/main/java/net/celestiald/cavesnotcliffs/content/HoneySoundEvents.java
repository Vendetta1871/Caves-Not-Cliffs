package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;

/** Canonical honey, honeycomb, and waxing sound identities. */
public final class HoneySoundEvents {
    public static final SoundEvent HONEY_BREAK = sound("block.honey_block.break");
    public static final SoundEvent HONEY_FALL = sound("block.honey_block.fall");
    public static final SoundEvent HONEY_HIT = sound("block.honey_block.hit");
    public static final SoundEvent HONEY_PLACE = sound("block.honey_block.place");
    public static final SoundEvent HONEY_SLIDE = sound("block.honey_block.slide");
    public static final SoundEvent HONEY_STEP = sound("block.honey_block.step");
    public static final SoundEvent HONEY_DRINK = sound("item.honey_bottle.drink");
    public static final SoundEvent WAX_ON = sound("item.honeycomb.wax_on");
    public static final SoundEvent CORAL_BREAK = sound("block.coral_block.break");
    public static final SoundEvent CORAL_FALL = sound("block.coral_block.fall");
    public static final SoundEvent CORAL_HIT = sound("block.coral_block.hit");
    public static final SoundEvent CORAL_PLACE = sound("block.coral_block.place");
    public static final SoundEvent CORAL_STEP = sound("block.coral_block.step");

    private HoneySoundEvents() {
    }

    public static void registerAll() {
        register(HONEY_BREAK);
        register(HONEY_FALL);
        register(HONEY_HIT);
        register(HONEY_PLACE);
        register(HONEY_SLIDE);
        register(HONEY_STEP);
        register(HONEY_DRINK);
        register(WAX_ON);
        register(CORAL_BREAK);
        register(CORAL_FALL);
        register(CORAL_HIT);
        register(CORAL_PLACE);
        register(CORAL_STEP);
    }

    private static SoundEvent sound(String path) {
        ResourceLocation id = CncRegistryIds.id(path);
        return new SoundEvent(id).setRegistryName(id);
    }

    private static void register(SoundEvent sound) {
        ElementsCavesNotCliffs.sounds.put(sound.getRegistryName(), sound);
    }
}

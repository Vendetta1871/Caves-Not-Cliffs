package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.minecraft.block.SoundType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Canonical Java 1.18.2 dripstone sound events and block sound types. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class DripstoneSoundEvents {
    public static final SoundEvent BLOCK_BREAK = sound("block.dripstone_block.break");
    public static final SoundEvent BLOCK_FALL = sound("block.dripstone_block.fall");
    public static final SoundEvent BLOCK_HIT = sound("block.dripstone_block.hit");
    public static final SoundEvent BLOCK_PLACE = sound("block.dripstone_block.place");
    public static final SoundEvent BLOCK_STEP = sound("block.dripstone_block.step");

    public static final SoundEvent POINTED_BREAK = sound("block.pointed_dripstone.break");
    public static final SoundEvent POINTED_FALL = sound("block.pointed_dripstone.fall");
    public static final SoundEvent POINTED_HIT = sound("block.pointed_dripstone.hit");
    public static final SoundEvent POINTED_LAND = sound("block.pointed_dripstone.land");
    public static final SoundEvent POINTED_PLACE = sound("block.pointed_dripstone.place");
    public static final SoundEvent POINTED_STEP = sound("block.pointed_dripstone.step");
    public static final SoundEvent DRIP_LAVA = sound("block.pointed_dripstone.drip_lava");
    public static final SoundEvent DRIP_LAVA_CAULDRON =
            sound("block.pointed_dripstone.drip_lava_into_cauldron");
    public static final SoundEvent DRIP_WATER = sound("block.pointed_dripstone.drip_water");
    public static final SoundEvent DRIP_WATER_CAULDRON =
            sound("block.pointed_dripstone.drip_water_into_cauldron");

    public static final SoundType DRIPSTONE_BLOCK = new SoundType(1.0F, 1.0F,
            BLOCK_BREAK, BLOCK_STEP, BLOCK_PLACE, BLOCK_HIT, BLOCK_FALL);
    public static final SoundType POINTED_DRIPSTONE = new SoundType(1.0F, 1.0F,
            POINTED_BREAK, POINTED_STEP, POINTED_PLACE, POINTED_HIT, POINTED_FALL);

    private DripstoneSoundEvents() {
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().registerAll(
                BLOCK_BREAK, BLOCK_FALL, BLOCK_HIT, BLOCK_PLACE, BLOCK_STEP,
                POINTED_BREAK, POINTED_FALL, POINTED_HIT, POINTED_LAND,
                POINTED_PLACE, POINTED_STEP, DRIP_LAVA, DRIP_LAVA_CAULDRON,
                DRIP_WATER, DRIP_WATER_CAULDRON);
    }

    private static SoundEvent sound(String path) {
        ResourceLocation id = new ResourceLocation(CavesNotCliffs.MODID, path);
        return new SoundEvent(id).setRegistryName(id);
    }
}

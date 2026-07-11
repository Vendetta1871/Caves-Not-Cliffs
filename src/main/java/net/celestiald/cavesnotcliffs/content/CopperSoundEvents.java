package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.minecraft.block.SoundType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Java 1.18.2 copper and copper-interaction sounds. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class CopperSoundEvents {
    public static final SoundEvent BLOCK_BREAK = sound("block.copper.break");
    public static final SoundEvent BLOCK_FALL = sound("block.copper.fall");
    public static final SoundEvent BLOCK_HIT = sound("block.copper.hit");
    public static final SoundEvent BLOCK_PLACE = sound("block.copper.place");
    public static final SoundEvent BLOCK_STEP = sound("block.copper.step");
    public static final SoundEvent AXE_SCRAPE = sound("item.axe.scrape");
    public static final SoundEvent AXE_WAX_OFF = sound("item.axe.wax_off");
    public static final SoundEvent LIGHTNING_ROD_THUNDER =
            sound("item.trident.thunder");

    public static final SoundType COPPER = new SoundType(1.0F, 1.0F,
            BLOCK_BREAK, BLOCK_STEP, BLOCK_PLACE, BLOCK_HIT, BLOCK_FALL);

    private CopperSoundEvents() {
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().registerAll(BLOCK_BREAK, BLOCK_FALL, BLOCK_HIT, BLOCK_PLACE,
                BLOCK_STEP, AXE_SCRAPE, AXE_WAX_OFF, LIGHTNING_ROD_THUNDER);
    }

    private static SoundEvent sound(String path) {
        ResourceLocation id = new ResourceLocation(CavesNotCliffs.MODID, path);
        return new SoundEvent(id).setRegistryName(id);
    }
}

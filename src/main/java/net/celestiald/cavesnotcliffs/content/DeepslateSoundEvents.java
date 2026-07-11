package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.minecraft.block.SoundType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Canonical Java 1.18.2 deepslate sound events and block sound types. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class DeepslateSoundEvents {
    private static final SoundEvent DEEPSLATE_BREAK = sound("block.deepslate.break");
    private static final SoundEvent DEEPSLATE_FALL = sound("block.deepslate.fall");
    private static final SoundEvent DEEPSLATE_HIT = sound("block.deepslate.hit");
    private static final SoundEvent DEEPSLATE_PLACE = sound("block.deepslate.place");
    private static final SoundEvent DEEPSLATE_STEP = sound("block.deepslate.step");

    private static final SoundEvent BRICKS_BREAK = sound("block.deepslate_bricks.break");
    private static final SoundEvent BRICKS_FALL = sound("block.deepslate_bricks.fall");
    private static final SoundEvent BRICKS_HIT = sound("block.deepslate_bricks.hit");
    private static final SoundEvent BRICKS_PLACE = sound("block.deepslate_bricks.place");
    private static final SoundEvent BRICKS_STEP = sound("block.deepslate_bricks.step");

    private static final SoundEvent TILES_BREAK = sound("block.deepslate_tiles.break");
    private static final SoundEvent TILES_FALL = sound("block.deepslate_tiles.fall");
    private static final SoundEvent TILES_HIT = sound("block.deepslate_tiles.hit");
    private static final SoundEvent TILES_PLACE = sound("block.deepslate_tiles.place");
    private static final SoundEvent TILES_STEP = sound("block.deepslate_tiles.step");

    private static final SoundEvent POLISHED_BREAK = sound("block.polished_deepslate.break");
    private static final SoundEvent POLISHED_FALL = sound("block.polished_deepslate.fall");
    private static final SoundEvent POLISHED_HIT = sound("block.polished_deepslate.hit");
    private static final SoundEvent POLISHED_PLACE = sound("block.polished_deepslate.place");
    private static final SoundEvent POLISHED_STEP = sound("block.polished_deepslate.step");

    private static final SoundEvent[] EVENTS = {
            DEEPSLATE_BREAK, DEEPSLATE_FALL, DEEPSLATE_HIT, DEEPSLATE_PLACE, DEEPSLATE_STEP,
            BRICKS_BREAK, BRICKS_FALL, BRICKS_HIT, BRICKS_PLACE, BRICKS_STEP,
            TILES_BREAK, TILES_FALL, TILES_HIT, TILES_PLACE, TILES_STEP,
            POLISHED_BREAK, POLISHED_FALL, POLISHED_HIT, POLISHED_PLACE, POLISHED_STEP
    };

    public static final SoundType DEEPSLATE = type(DEEPSLATE_BREAK, DEEPSLATE_STEP,
            DEEPSLATE_PLACE, DEEPSLATE_HIT, DEEPSLATE_FALL);
    public static final SoundType DEEPSLATE_BRICKS = type(BRICKS_BREAK, BRICKS_STEP,
            BRICKS_PLACE, BRICKS_HIT, BRICKS_FALL);
    public static final SoundType DEEPSLATE_TILES = type(TILES_BREAK, TILES_STEP,
            TILES_PLACE, TILES_HIT, TILES_FALL);
    public static final SoundType POLISHED_DEEPSLATE = type(POLISHED_BREAK, POLISHED_STEP,
            POLISHED_PLACE, POLISHED_HIT, POLISHED_FALL);

    private DeepslateSoundEvents() {
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().registerAll(EVENTS);
    }

    static SoundType forBuildingBlock(String name) {
        if (name.equals("polished_deepslate")) {
            return POLISHED_DEEPSLATE;
        }
        if (name.equals("deepslate_bricks") || name.equals("cracked_deepslate_bricks")
                || name.equals("chiseled_deepslate")) {
            return DEEPSLATE_BRICKS;
        }
        if (name.equals("deepslate_tiles") || name.equals("cracked_deepslate_tiles")) {
            return DEEPSLATE_TILES;
        }
        return DEEPSLATE;
    }

    private static SoundType type(SoundEvent breakSound, SoundEvent stepSound,
            SoundEvent placeSound, SoundEvent hitSound, SoundEvent fallSound) {
        return new SoundType(1.0F, 1.0F, breakSound, stepSound, placeSound, hitSound, fallSound);
    }

    private static SoundEvent sound(String path) {
        ResourceLocation id = new ResourceLocation(CavesNotCliffs.MODID, path);
        return new SoundEvent(id).setRegistryName(id);
    }
}

package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.SoundType;
import net.minecraft.util.SoundEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Canonical Java 1.18.2 lush-cave sound events and sound types. */
public final class LushCaveSounds {
    private static final List<SoundEvent> EVENTS = new ArrayList<>();

    public static final SoundType CAVE_VINES = type("block.cave_vines");
    public static final SoundType SPORE_BLOSSOM = type("block.spore_blossom");
    public static final SoundType AZALEA = type("block.azalea");
    public static final SoundType FLOWERING_AZALEA = type("block.flowering_azalea");
    public static final SoundType AZALEA_LEAVES = type("block.azalea_leaves");
    public static final SoundType MOSS = type("block.moss");
    public static final SoundType MOSS_CARPET = type("block.moss_carpet");
    public static final SoundType BIG_DRIPLEAF = type("block.big_dripleaf");
    public static final SoundType SMALL_DRIPLEAF = type("block.small_dripleaf");
    public static final SoundType ROOTED_DIRT = type("block.rooted_dirt");
    public static final SoundType HANGING_ROOTS = type("block.hanging_roots");

    public static final SoundEvent CAVE_VINES_PICK_BERRIES =
            event("block.cave_vines.pick_berries");
    public static final SoundEvent BIG_DRIPLEAF_TILT_DOWN =
            event("block.big_dripleaf.tilt_down");
    public static final SoundEvent BIG_DRIPLEAF_TILT_UP =
            event("block.big_dripleaf.tilt_up");

    private LushCaveSounds() {
    }

    private static SoundType type(String prefix) {
        return new SoundType(1.0F, 1.0F,
                event(prefix + ".break"),
                event(prefix + ".step"),
                event(prefix + ".place"),
                event(prefix + ".hit"),
                event(prefix + ".fall"));
    }

    private static SoundEvent event(String path) {
        SoundEvent sound = new SoundEvent(CncRegistryIds.id(path));
        EVENTS.add(sound);
        return sound;
    }

    public static List<SoundEvent> events() {
        return Collections.unmodifiableList(EVENTS);
    }

    public static void registerAll() {
        for (SoundEvent event : EVENTS) {
            ElementsCavesNotCliffs.sounds.put(event.getSoundName(), event);
        }
    }
}

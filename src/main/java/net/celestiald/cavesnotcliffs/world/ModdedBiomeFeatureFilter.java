package net.celestiald.cavesnotcliffs.world;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.event.terraingen.OreGenEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Denies the 1.12 ores and terrain features a climate-hosted modded biome's decorator would
 * add over the 1.18 ones (see {@link ModdedBiomeDecoration#decorateHosted}). Every other
 * decoration, including all of the native pipeline's own events, passes untouched.
 */
public final class ModdedBiomeFeatureFilter {
    private ModdedBiomeFeatureFilter() {}

    public static void register() {
        MinecraftForge.TERRAIN_GEN_BUS.register(ModdedBiomeFeatureFilter.class);
        MinecraftForge.ORE_GEN_BUS.register(ModdedBiomeFeatureFilter.class);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDecorate(DecorateBiomeEvent.Decorate event) {
        if (ModdedBiomeDecoration.deniesWhileFiltering(event.getType())
                && ModdedBiomeDecoration.isFiltering(event.getWorld())) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onOre(OreGenEvent.GenerateMinable event) {
        // CUSTOM stays: mods fire it for their own ores, which 1.18 does not replace.
        if (event.getType() != OreGenEvent.GenerateMinable.EventType.CUSTOM
                && ModdedBiomeDecoration.isFiltering(event.getWorld())) {
            event.setResult(Event.Result.DENY);
        }
    }
}

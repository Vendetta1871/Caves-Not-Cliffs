package net.celestiald.cavesnotcliffs.config;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.minecraftforge.common.config.Config;

/** Server-owned settings for Caves Not Cliffs. */
@Config(modid = CavesNotCliffs.MODID, name = CavesNotCliffs.MODID)
public final class CavesNotCliffsConfig {
    @Config.Name("world")
    @Config.Comment("World-creation settings. Changing these values never converts an existing save.")
    public static final World WORLD = new World();

    private CavesNotCliffsConfig() {
    }

    public static final class World {
        @Config.Name("enableForNewOverworlds")
        @Config.Comment({
                "Use the Caves Not Cliffs v2 world format for newly created Overworlds.",
                "Existing worlds keep the format recorded in their save, regardless of this value."
        })
        public boolean enableForNewOverworlds = true;
    }
}

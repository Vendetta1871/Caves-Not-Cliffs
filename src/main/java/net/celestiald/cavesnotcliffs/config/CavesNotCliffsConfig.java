package net.celestiald.cavesnotcliffs.config;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.minecraftforge.common.config.Config;

/** Server-owned settings for Caves Not Cliffs. */
@Config(modid = CavesNotCliffs.MODID, name = CavesNotCliffs.MODID)
public final class CavesNotCliffsConfig {
    @Config.Name("world")
    @Config.Comment("World-creation settings. Changing these values never converts an existing save.")
    public static final World WORLD = new World();

    @Config.Name("moddedBiomes")
    @Config.Comment({
            "How biomes from other mods appear in native 1.18 Overworlds. A modded biome replaces",
            "only the 1.18 biomes it fits (climate, height, biome type), so terrain, caves and",
            "ores stay 1.18. Changing these values changes where modded biomes appear in chunks",
            "generated afterwards, which leaves seams at the edge of explored land."
    })
    public static final ModdedBiomes MODDED_BIOMES = new ModdedBiomes();

    private CavesNotCliffsConfig() {
    }

    public static final class ModdedBiomes {
        @Config.Name("regionSize")
        @Config.Comment({
                "Approximate size in blocks of the regions that each decide which modded biome",
                "(if any) replaces each 1.18 biome. Large Biomes worlds use four times this."
        })
        @Config.RangeInt(min = 128, max = 16384)
        public int regionSize = 1024;

        @Config.Name("vanillaWeight")
        @Config.Comment({
                "Weight of keeping the 1.18 biome, against each fitting modded biome's own weight",
                "(most mods use 10 or less, like vanilla biomes in 1.12). Higher values make",
                "modded biomes rarer; 0 lets them replace every biome they fit."
        })
        @Config.RangeInt(min = 0, max = 1000)
        public int vanillaWeight = 10;

        @Config.Name("blacklist")
        @Config.Comment("Modded biomes that never generate, as modid:biome or modid:* entries.")
        public String[] blacklist = new String[0];

        @Config.Name("hosts")
        @Config.Comment({
                "Pins a modded biome to the 1.18 biomes it replaces, overriding the automatic",
                "choice: modid:biome=host[,host...], hosts given as 1.18 ids such as plains,",
                "swamp or minecraft:snowy_slopes. The log lists the automatic choices."
        })
        public String[] hosts = new String[0];

        @Config.Name("upgradeExistingWorlds")
        @Config.Comment({
                "Worlds created before 1.18-fitted modded biomes keep painting modded biomes where",
                "the 1.12 biome layers put them. Set to true to use the 1.18 fit in their new",
                "chunks too; explored land keeps its biomes, so borders show seams."
        })
        public boolean upgradeExistingWorlds = false;
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

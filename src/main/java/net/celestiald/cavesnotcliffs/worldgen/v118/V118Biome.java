package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Canonical resource identifiers used by the Java 1.18.2 Overworld climate table. */
public enum V118Biome {
    BADLANDS("badlands"),
    BAMBOO_JUNGLE("bamboo_jungle"),
    BEACH("beach"),
    BIRCH_FOREST("birch_forest"),
    COLD_OCEAN("cold_ocean"),
    DARK_FOREST("dark_forest"),
    DEEP_COLD_OCEAN("deep_cold_ocean"),
    DEEP_FROZEN_OCEAN("deep_frozen_ocean"),
    DEEP_LUKEWARM_OCEAN("deep_lukewarm_ocean"),
    DEEP_OCEAN("deep_ocean"),
    DESERT("desert"),
    DRIPSTONE_CAVES("dripstone_caves"),
    ERODED_BADLANDS("eroded_badlands"),
    FLOWER_FOREST("flower_forest"),
    FOREST("forest"),
    FROZEN_OCEAN("frozen_ocean"),
    FROZEN_PEAKS("frozen_peaks"),
    FROZEN_RIVER("frozen_river"),
    GROVE("grove"),
    ICE_SPIKES("ice_spikes"),
    JAGGED_PEAKS("jagged_peaks"),
    JUNGLE("jungle"),
    LUKEWARM_OCEAN("lukewarm_ocean"),
    LUSH_CAVES("lush_caves"),
    MEADOW("meadow"),
    MUSHROOM_FIELDS("mushroom_fields"),
    OCEAN("ocean"),
    OLD_GROWTH_BIRCH_FOREST("old_growth_birch_forest"),
    OLD_GROWTH_PINE_TAIGA("old_growth_pine_taiga"),
    OLD_GROWTH_SPRUCE_TAIGA("old_growth_spruce_taiga"),
    PLAINS("plains"),
    RIVER("river"),
    SAVANNA("savanna"),
    SAVANNA_PLATEAU("savanna_plateau"),
    SNOWY_BEACH("snowy_beach"),
    SNOWY_PLAINS("snowy_plains"),
    SNOWY_SLOPES("snowy_slopes"),
    SNOWY_TAIGA("snowy_taiga"),
    SPARSE_JUNGLE("sparse_jungle"),
    STONY_PEAKS("stony_peaks"),
    STONY_SHORE("stony_shore"),
    SUNFLOWER_PLAINS("sunflower_plains"),
    SWAMP("swamp"),
    TAIGA("taiga"),
    WARM_OCEAN("warm_ocean"),
    WINDSWEPT_FOREST("windswept_forest"),
    WINDSWEPT_GRAVELLY_HILLS("windswept_gravelly_hills"),
    WINDSWEPT_HILLS("windswept_hills"),
    WINDSWEPT_SAVANNA("windswept_savanna"),
    WOODED_BADLANDS("wooded_badlands");

    private static final String NAMESPACE = "minecraft:";
    private static final Map<String, V118Biome> BY_ID;

    static {
        Map<String, V118Biome> values = new LinkedHashMap<String, V118Biome>();
        for (V118Biome biome : values()) {
            V118Biome previous = values.put(biome.id, biome);
            if (previous != null) {
                throw new IllegalStateException("Duplicate 1.18.2 biome id " + biome.id);
            }
        }
        BY_ID = Collections.unmodifiableMap(values);
    }

    private final String id;

    V118Biome(String path) {
        id = NAMESPACE + path;
    }

    public String id() {
        return id;
    }

    public static V118Biome fromId(String id) {
        V118Biome biome = BY_ID.get(id);
        if (biome == null) {
            throw new IllegalArgumentException("Not a Java 1.18.2 Overworld biome id: " + id);
        }
        return biome;
    }
}

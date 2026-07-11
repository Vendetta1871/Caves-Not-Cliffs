package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Canonical resource identifiers used by the Java 1.18.2 Overworld climate table. */
public enum V118Biome {
    BADLANDS("badlands", 1073741824L),
    BAMBOO_JUNGLE("bamboo_jungle", 1064514355L),
    BEACH("beach", 1061997773L),
    BIRCH_FOREST("birch_forest", 1058642330L),
    COLD_OCEAN("cold_ocean", 1056964608L),
    DARK_FOREST("dark_forest", 1060320051L),
    DEEP_COLD_OCEAN("deep_cold_ocean", 1056964608L),
    DEEP_FROZEN_OCEAN("deep_frozen_ocean", 1056964608L, true),
    DEEP_LUKEWARM_OCEAN("deep_lukewarm_ocean", 1056964608L),
    DEEP_OCEAN("deep_ocean", 1056964608L),
    DESERT("desert", 1073741824L),
    DRIPSTONE_CAVES("dripstone_caves", 1061997773L),
    ERODED_BADLANDS("eroded_badlands", 1073741824L),
    FLOWER_FOREST("flower_forest", 1060320051L),
    FOREST("forest", 1060320051L),
    FROZEN_OCEAN("frozen_ocean", 0L, true),
    FROZEN_PEAKS("frozen_peaks", 3207803699L),
    FROZEN_RIVER("frozen_river", 0L),
    GROVE("grove", 3192704205L),
    ICE_SPIKES("ice_spikes", 0L),
    JAGGED_PEAKS("jagged_peaks", 3207803699L),
    JUNGLE("jungle", 1064514355L),
    LUKEWARM_OCEAN("lukewarm_ocean", 1056964608L),
    LUSH_CAVES("lush_caves", 1056964608L),
    MEADOW("meadow", 1056964608L),
    MUSHROOM_FIELDS("mushroom_fields", 1063675494L),
    OCEAN("ocean", 1056964608L),
    OLD_GROWTH_BIRCH_FOREST("old_growth_birch_forest", 1058642330L),
    OLD_GROWTH_PINE_TAIGA("old_growth_pine_taiga", 1050253722L),
    OLD_GROWTH_SPRUCE_TAIGA("old_growth_spruce_taiga", 1048576000L),
    PLAINS("plains", 1061997773L),
    RIVER("river", 1056964608L),
    SAVANNA("savanna", 1073741824L),
    SAVANNA_PLATEAU("savanna_plateau", 1073741824L),
    SNOWY_BEACH("snowy_beach", 1028443341L),
    SNOWY_PLAINS("snowy_plains", 0L),
    SNOWY_SLOPES("snowy_slopes", 3197737370L),
    SNOWY_TAIGA("snowy_taiga", 3204448256L),
    SPARSE_JUNGLE("sparse_jungle", 1064514355L),
    STONY_PEAKS("stony_peaks", 1065353216L),
    STONY_SHORE("stony_shore", 1045220557L),
    SUNFLOWER_PLAINS("sunflower_plains", 1061997773L),
    SWAMP("swamp", 1061997773L),
    TAIGA("taiga", 1048576000L),
    WARM_OCEAN("warm_ocean", 1056964608L),
    WINDSWEPT_FOREST("windswept_forest", 1045220557L),
    WINDSWEPT_GRAVELLY_HILLS("windswept_gravelly_hills", 1045220557L),
    WINDSWEPT_HILLS("windswept_hills", 1045220557L),
    WINDSWEPT_SAVANNA("windswept_savanna", 1073741824L),
    WOODED_BADLANDS("wooded_badlands", 1073741824L);

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
    private final float baseTemperature;
    private final boolean frozenTemperatureModifier;

    V118Biome(String path, long temperatureBits) {
        this(path, temperatureBits, false);
    }

    V118Biome(String path, long temperatureBits, boolean frozenTemperatureModifier) {
        id = NAMESPACE + path;
        baseTemperature = Float.intBitsToFloat((int) temperatureBits);
        this.frozenTemperatureModifier = frozenTemperatureModifier;
    }

    public String id() {
        return id;
    }

    public float baseTemperature() {
        return baseTemperature;
    }

    public boolean hasFrozenTemperatureModifier() {
        return frozenTemperatureModifier;
    }

    public static V118Biome fromId(String id) {
        V118Biome biome = BY_ID.get(id);
        if (biome == null) {
            throw new IllegalArgumentException("Not a Java 1.18.2 Overworld biome id: " + id);
        }
        return biome;
    }
}

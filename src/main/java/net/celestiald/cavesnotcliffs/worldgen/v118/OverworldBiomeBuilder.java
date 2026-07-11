package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dependency-free port of Java 1.18.2's ordered six-parameter Overworld biome table.
 *
 * <p>The order of every entry is part of the compatibility contract: the climate index keeps the
 * first inserted value when two entries have the same fitness.</p>
 */
public final class OverworldBiomeBuilder {
    private static final Climate.Parameter FULL_RANGE = Climate.Parameter.span(-1.0F, 1.0F);

    private static final Climate.Parameter[] TEMPERATURES = {
        Climate.Parameter.span(-1.0F, -0.45F),
        Climate.Parameter.span(-0.45F, -0.15F),
        Climate.Parameter.span(-0.15F, 0.2F),
        Climate.Parameter.span(0.2F, 0.55F),
        Climate.Parameter.span(0.55F, 1.0F)
    };
    private static final Climate.Parameter[] HUMIDITIES = {
        Climate.Parameter.span(-1.0F, -0.35F),
        Climate.Parameter.span(-0.35F, -0.1F),
        Climate.Parameter.span(-0.1F, 0.1F),
        Climate.Parameter.span(0.1F, 0.3F),
        Climate.Parameter.span(0.3F, 1.0F)
    };
    private static final Climate.Parameter[] EROSIONS = {
        Climate.Parameter.span(-1.0F, -0.78F),
        Climate.Parameter.span(-0.78F, -0.375F),
        Climate.Parameter.span(-0.375F, -0.2225F),
        Climate.Parameter.span(-0.2225F, 0.05F),
        Climate.Parameter.span(0.05F, 0.45F),
        Climate.Parameter.span(0.45F, 0.55F),
        Climate.Parameter.span(0.55F, 1.0F)
    };

    private static final Climate.Parameter FROZEN_RANGE = TEMPERATURES[0];
    private static final Climate.Parameter UNFROZEN_RANGE =
        Climate.Parameter.span(TEMPERATURES[1], TEMPERATURES[4]);
    private static final Climate.Parameter MUSHROOM_FIELDS_CONTINENTALNESS =
        Climate.Parameter.span(-1.2F, -1.05F);
    private static final Climate.Parameter DEEP_OCEAN_CONTINENTALNESS =
        Climate.Parameter.span(-1.05F, -0.455F);
    private static final Climate.Parameter OCEAN_CONTINENTALNESS =
        Climate.Parameter.span(-0.455F, -0.19F);
    private static final Climate.Parameter COAST_CONTINENTALNESS =
        Climate.Parameter.span(-0.19F, -0.11F);
    private static final Climate.Parameter INLAND_CONTINENTALNESS =
        Climate.Parameter.span(-0.11F, 0.55F);
    private static final Climate.Parameter NEAR_INLAND_CONTINENTALNESS =
        Climate.Parameter.span(-0.11F, 0.03F);
    private static final Climate.Parameter MID_INLAND_CONTINENTALNESS =
        Climate.Parameter.span(0.03F, 0.3F);
    private static final Climate.Parameter FAR_INLAND_CONTINENTALNESS =
        Climate.Parameter.span(0.3F, 1.0F);

    private static final V118Biome[][] OCEANS = {
        {
            V118Biome.DEEP_FROZEN_OCEAN, V118Biome.DEEP_COLD_OCEAN,
            V118Biome.DEEP_OCEAN, V118Biome.DEEP_LUKEWARM_OCEAN, V118Biome.WARM_OCEAN
        },
        {
            V118Biome.FROZEN_OCEAN, V118Biome.COLD_OCEAN,
            V118Biome.OCEAN, V118Biome.LUKEWARM_OCEAN, V118Biome.WARM_OCEAN
        }
    };
    private static final V118Biome[][] MIDDLE_BIOMES = {
        {
            V118Biome.SNOWY_PLAINS, V118Biome.SNOWY_PLAINS, V118Biome.SNOWY_PLAINS,
            V118Biome.SNOWY_TAIGA, V118Biome.TAIGA
        },
        {
            V118Biome.PLAINS, V118Biome.PLAINS, V118Biome.FOREST,
            V118Biome.TAIGA, V118Biome.OLD_GROWTH_SPRUCE_TAIGA
        },
        {
            V118Biome.FLOWER_FOREST, V118Biome.PLAINS, V118Biome.FOREST,
            V118Biome.BIRCH_FOREST, V118Biome.DARK_FOREST
        },
        {
            V118Biome.SAVANNA, V118Biome.SAVANNA, V118Biome.FOREST,
            V118Biome.JUNGLE, V118Biome.JUNGLE
        },
        {
            V118Biome.DESERT, V118Biome.DESERT, V118Biome.DESERT,
            V118Biome.DESERT, V118Biome.DESERT
        }
    };
    private static final V118Biome[][] MIDDLE_BIOMES_VARIANT = {
        {V118Biome.ICE_SPIKES, null, V118Biome.SNOWY_TAIGA, null, null},
        {null, null, null, null, V118Biome.OLD_GROWTH_PINE_TAIGA},
        {V118Biome.SUNFLOWER_PLAINS, null, null, V118Biome.OLD_GROWTH_BIRCH_FOREST, null},
        {null, null, V118Biome.PLAINS, V118Biome.SPARSE_JUNGLE, V118Biome.BAMBOO_JUNGLE},
        {null, null, null, null, null}
    };
    private static final V118Biome[][] PLATEAU_BIOMES = {
        {
            V118Biome.SNOWY_PLAINS, V118Biome.SNOWY_PLAINS, V118Biome.SNOWY_PLAINS,
            V118Biome.SNOWY_TAIGA, V118Biome.SNOWY_TAIGA
        },
        {
            V118Biome.MEADOW, V118Biome.MEADOW, V118Biome.FOREST,
            V118Biome.TAIGA, V118Biome.OLD_GROWTH_SPRUCE_TAIGA
        },
        {
            V118Biome.MEADOW, V118Biome.MEADOW, V118Biome.MEADOW,
            V118Biome.MEADOW, V118Biome.DARK_FOREST
        },
        {
            V118Biome.SAVANNA_PLATEAU, V118Biome.SAVANNA_PLATEAU, V118Biome.FOREST,
            V118Biome.FOREST, V118Biome.JUNGLE
        },
        {
            V118Biome.BADLANDS, V118Biome.BADLANDS, V118Biome.BADLANDS,
            V118Biome.WOODED_BADLANDS, V118Biome.WOODED_BADLANDS
        }
    };
    private static final V118Biome[][] PLATEAU_BIOMES_VARIANT = {
        {V118Biome.ICE_SPIKES, null, null, null, null},
        {null, null, V118Biome.MEADOW, V118Biome.MEADOW, V118Biome.OLD_GROWTH_PINE_TAIGA},
        {null, null, V118Biome.FOREST, V118Biome.BIRCH_FOREST, null},
        {null, null, null, null, null},
        {V118Biome.ERODED_BADLANDS, V118Biome.ERODED_BADLANDS, null, null, null}
    };
    private static final V118Biome[][] SHATTERED_BIOMES = {
        {
            V118Biome.WINDSWEPT_GRAVELLY_HILLS, V118Biome.WINDSWEPT_GRAVELLY_HILLS,
            V118Biome.WINDSWEPT_HILLS, V118Biome.WINDSWEPT_FOREST,
            V118Biome.WINDSWEPT_FOREST
        },
        {
            V118Biome.WINDSWEPT_GRAVELLY_HILLS, V118Biome.WINDSWEPT_GRAVELLY_HILLS,
            V118Biome.WINDSWEPT_HILLS, V118Biome.WINDSWEPT_FOREST,
            V118Biome.WINDSWEPT_FOREST
        },
        {
            V118Biome.WINDSWEPT_HILLS, V118Biome.WINDSWEPT_HILLS,
            V118Biome.WINDSWEPT_HILLS, V118Biome.WINDSWEPT_FOREST,
            V118Biome.WINDSWEPT_FOREST
        },
        {null, null, null, null, null},
        {null, null, null, null, null}
    };

    private final List<Climate.Entry<V118Biome>> entries;
    private final Climate.ParameterList<V118Biome> parameters;

    public OverworldBiomeBuilder() {
        List<Climate.Entry<V118Biome>> built = new ArrayList<Climate.Entry<V118Biome>>();
        addOffCoastBiomes(built);
        addInlandBiomes(built);
        addUndergroundBiomes(built);
        entries = Collections.unmodifiableList(built);
        parameters = new Climate.ParameterList<V118Biome>(entries);
    }

    /** Returns the immutable table in Mojang's exact insertion order. */
    public List<Climate.Entry<V118Biome>> entries() {
        return entries;
    }

    /** Returns the indexed canonical parameter list used for virtual 3D biome lookup. */
    public Climate.ParameterList<V118Biome> parameters() {
        return parameters;
    }

    public V118Biome resolve(Climate.TargetPoint target) {
        return parameters.findValue(target);
    }

    private static void addOffCoastBiomes(List<Climate.Entry<V118Biome>> entries) {
        addSurfaceBiome(entries, FULL_RANGE, FULL_RANGE, MUSHROOM_FIELDS_CONTINENTALNESS,
            FULL_RANGE, FULL_RANGE, 0.0F, V118Biome.MUSHROOM_FIELDS);
        for (int temperatureIndex = 0; temperatureIndex < TEMPERATURES.length; ++temperatureIndex) {
            Climate.Parameter temperature = TEMPERATURES[temperatureIndex];
            addSurfaceBiome(entries, temperature, FULL_RANGE, DEEP_OCEAN_CONTINENTALNESS,
                FULL_RANGE, FULL_RANGE, 0.0F, OCEANS[0][temperatureIndex]);
            addSurfaceBiome(entries, temperature, FULL_RANGE, OCEAN_CONTINENTALNESS,
                FULL_RANGE, FULL_RANGE, 0.0F, OCEANS[1][temperatureIndex]);
        }
    }

    private static void addInlandBiomes(List<Climate.Entry<V118Biome>> entries) {
        addMidSlice(entries, Climate.Parameter.span(-1.0F, -0.93333334F));
        addHighSlice(entries, Climate.Parameter.span(-0.93333334F, -0.7666667F));
        addPeaks(entries, Climate.Parameter.span(-0.7666667F, -0.56666666F));
        addHighSlice(entries, Climate.Parameter.span(-0.56666666F, -0.4F));
        addMidSlice(entries, Climate.Parameter.span(-0.4F, -0.26666668F));
        addLowSlice(entries, Climate.Parameter.span(-0.26666668F, -0.05F));
        addValleys(entries, Climate.Parameter.span(-0.05F, 0.05F));
        addLowSlice(entries, Climate.Parameter.span(0.05F, 0.26666668F));
        addMidSlice(entries, Climate.Parameter.span(0.26666668F, 0.4F));
        addHighSlice(entries, Climate.Parameter.span(0.4F, 0.56666666F));
        addPeaks(entries, Climate.Parameter.span(0.56666666F, 0.7666667F));
        addHighSlice(entries, Climate.Parameter.span(0.7666667F, 0.93333334F));
        addMidSlice(entries, Climate.Parameter.span(0.93333334F, 1.0F));
    }

    private static void addPeaks(List<Climate.Entry<V118Biome>> entries,
            Climate.Parameter weirdness) {
        for (int temperatureIndex = 0; temperatureIndex < TEMPERATURES.length; ++temperatureIndex) {
            Climate.Parameter temperature = TEMPERATURES[temperatureIndex];
            for (int humidityIndex = 0; humidityIndex < HUMIDITIES.length; ++humidityIndex) {
                Climate.Parameter humidity = HUMIDITIES[humidityIndex];
                V118Biome middle = pickMiddleBiome(temperatureIndex, humidityIndex, weirdness);
                V118Biome middleOrBadlands = pickMiddleBiomeOrBadlandsIfHot(
                    temperatureIndex, humidityIndex, weirdness);
                V118Biome middleBadlandsOrSlope = pickMiddleBiomeOrBadlandsIfHotOrSlopeIfCold(
                    temperatureIndex, humidityIndex, weirdness);
                V118Biome plateau = pickPlateauBiome(temperatureIndex, humidityIndex, weirdness);
                V118Biome shattered = pickShatteredBiome(temperatureIndex, humidityIndex, weirdness);
                V118Biome windswept = maybePickWindsweptSavannaBiome(
                    temperatureIndex, humidityIndex, weirdness, shattered);
                V118Biome peak = pickPeakBiome(temperatureIndex, humidityIndex, weirdness);
                addSurfaceBiome(entries, temperature, humidity,
                    Climate.Parameter.span(COAST_CONTINENTALNESS, FAR_INLAND_CONTINENTALNESS),
                    EROSIONS[0], weirdness, 0.0F, peak);
                addSurfaceBiome(entries, temperature, humidity,
                    Climate.Parameter.span(COAST_CONTINENTALNESS, NEAR_INLAND_CONTINENTALNESS),
                    EROSIONS[1], weirdness, 0.0F, middleBadlandsOrSlope);
                addSurfaceBiome(entries, temperature, humidity,
                    Climate.Parameter.span(MID_INLAND_CONTINENTALNESS, FAR_INLAND_CONTINENTALNESS),
                    EROSIONS[1], weirdness, 0.0F, peak);
                addSurfaceBiome(entries, temperature, humidity,
                    Climate.Parameter.span(COAST_CONTINENTALNESS, NEAR_INLAND_CONTINENTALNESS),
                    Climate.Parameter.span(EROSIONS[2], EROSIONS[3]), weirdness, 0.0F, middle);
                addSurfaceBiome(entries, temperature, humidity,
                    Climate.Parameter.span(MID_INLAND_CONTINENTALNESS, FAR_INLAND_CONTINENTALNESS),
                    EROSIONS[2], weirdness, 0.0F, plateau);
                addSurfaceBiome(entries, temperature, humidity, MID_INLAND_CONTINENTALNESS,
                    EROSIONS[3], weirdness, 0.0F, middleOrBadlands);
                addSurfaceBiome(entries, temperature, humidity, FAR_INLAND_CONTINENTALNESS,
                    EROSIONS[3], weirdness, 0.0F, plateau);
                addSurfaceBiome(entries, temperature, humidity,
                    Climate.Parameter.span(COAST_CONTINENTALNESS, FAR_INLAND_CONTINENTALNESS),
                    EROSIONS[4], weirdness, 0.0F, middle);
                addSurfaceBiome(entries, temperature, humidity,
                    Climate.Parameter.span(COAST_CONTINENTALNESS, NEAR_INLAND_CONTINENTALNESS),
                    EROSIONS[5], weirdness, 0.0F, windswept);
                addSurfaceBiome(entries, temperature, humidity,
                    Climate.Parameter.span(MID_INLAND_CONTINENTALNESS, FAR_INLAND_CONTINENTALNESS),
                    EROSIONS[5], weirdness, 0.0F, shattered);
                addSurfaceBiome(entries, temperature, humidity,
                    Climate.Parameter.span(COAST_CONTINENTALNESS, FAR_INLAND_CONTINENTALNESS),
                    EROSIONS[6], weirdness, 0.0F, middle);
            }
        }
    }

    private static void addHighSlice(List<Climate.Entry<V118Biome>> entries,
            Climate.Parameter weirdness) {
        for (int temperatureIndex = 0; temperatureIndex < TEMPERATURES.length; ++temperatureIndex) {
            Climate.Parameter temperature = TEMPERATURES[temperatureIndex];
            for (int humidityIndex = 0; humidityIndex < HUMIDITIES.length; ++humidityIndex) {
                Climate.Parameter humidity = HUMIDITIES[humidityIndex];
                V118Biome middle = pickMiddleBiome(temperatureIndex, humidityIndex, weirdness);
                V118Biome middleOrBadlands = pickMiddleBiomeOrBadlandsIfHot(
                    temperatureIndex, humidityIndex, weirdness);
                V118Biome middleBadlandsOrSlope = pickMiddleBiomeOrBadlandsIfHotOrSlopeIfCold(
                    temperatureIndex, humidityIndex, weirdness);
                V118Biome plateau = pickPlateauBiome(temperatureIndex, humidityIndex, weirdness);
                V118Biome shattered = pickShatteredBiome(temperatureIndex, humidityIndex, weirdness);
                V118Biome windswept = maybePickWindsweptSavannaBiome(
                    temperatureIndex, humidityIndex, weirdness, middle);
                V118Biome slope = pickSlopeBiome(temperatureIndex, humidityIndex, weirdness);
                V118Biome peak = pickPeakBiome(temperatureIndex, humidityIndex, weirdness);
                addSurfaceBiome(entries, temperature, humidity, COAST_CONTINENTALNESS,
                    Climate.Parameter.span(EROSIONS[0], EROSIONS[1]), weirdness, 0.0F, middle);
                addSurfaceBiome(entries, temperature, humidity, NEAR_INLAND_CONTINENTALNESS,
                    EROSIONS[0], weirdness, 0.0F, slope);
                addSurfaceBiome(entries, temperature, humidity,
                    Climate.Parameter.span(MID_INLAND_CONTINENTALNESS, FAR_INLAND_CONTINENTALNESS),
                    EROSIONS[0], weirdness, 0.0F, peak);
                addSurfaceBiome(entries, temperature, humidity, NEAR_INLAND_CONTINENTALNESS,
                    EROSIONS[1], weirdness, 0.0F, middleBadlandsOrSlope);
                addSurfaceBiome(entries, temperature, humidity,
                    Climate.Parameter.span(MID_INLAND_CONTINENTALNESS, FAR_INLAND_CONTINENTALNESS),
                    EROSIONS[1], weirdness, 0.0F, slope);
                addSurfaceBiome(entries, temperature, humidity,
                    Climate.Parameter.span(COAST_CONTINENTALNESS, NEAR_INLAND_CONTINENTALNESS),
                    Climate.Parameter.span(EROSIONS[2], EROSIONS[3]), weirdness, 0.0F, middle);
                addSurfaceBiome(entries, temperature, humidity,
                    Climate.Parameter.span(MID_INLAND_CONTINENTALNESS, FAR_INLAND_CONTINENTALNESS),
                    EROSIONS[2], weirdness, 0.0F, plateau);
                addSurfaceBiome(entries, temperature, humidity, MID_INLAND_CONTINENTALNESS,
                    EROSIONS[3], weirdness, 0.0F, middleOrBadlands);
                addSurfaceBiome(entries, temperature, humidity, FAR_INLAND_CONTINENTALNESS,
                    EROSIONS[3], weirdness, 0.0F, plateau);
                addSurfaceBiome(entries, temperature, humidity,
                    Climate.Parameter.span(COAST_CONTINENTALNESS, FAR_INLAND_CONTINENTALNESS),
                    EROSIONS[4], weirdness, 0.0F, middle);
                addSurfaceBiome(entries, temperature, humidity,
                    Climate.Parameter.span(COAST_CONTINENTALNESS, NEAR_INLAND_CONTINENTALNESS),
                    EROSIONS[5], weirdness, 0.0F, windswept);
                addSurfaceBiome(entries, temperature, humidity,
                    Climate.Parameter.span(MID_INLAND_CONTINENTALNESS, FAR_INLAND_CONTINENTALNESS),
                    EROSIONS[5], weirdness, 0.0F, shattered);
                addSurfaceBiome(entries, temperature, humidity,
                    Climate.Parameter.span(COAST_CONTINENTALNESS, FAR_INLAND_CONTINENTALNESS),
                    EROSIONS[6], weirdness, 0.0F, middle);
            }
        }
    }

    private static void addMidSlice(List<Climate.Entry<V118Biome>> entries,
            Climate.Parameter weirdness) {
        addSurfaceBiome(entries, FULL_RANGE, FULL_RANGE, COAST_CONTINENTALNESS,
            Climate.Parameter.span(EROSIONS[0], EROSIONS[2]), weirdness, 0.0F,
            V118Biome.STONY_SHORE);
        addSurfaceBiome(entries, UNFROZEN_RANGE, FULL_RANGE,
            Climate.Parameter.span(NEAR_INLAND_CONTINENTALNESS, FAR_INLAND_CONTINENTALNESS),
            EROSIONS[6], weirdness, 0.0F, V118Biome.SWAMP);
        for (int temperatureIndex = 0; temperatureIndex < TEMPERATURES.length; ++temperatureIndex) {
            Climate.Parameter temperature = TEMPERATURES[temperatureIndex];
            for (int humidityIndex = 0; humidityIndex < HUMIDITIES.length; ++humidityIndex) {
                Climate.Parameter humidity = HUMIDITIES[humidityIndex];
                V118Biome middle = pickMiddleBiome(temperatureIndex, humidityIndex, weirdness);
                V118Biome middleOrBadlands = pickMiddleBiomeOrBadlandsIfHot(
                    temperatureIndex, humidityIndex, weirdness);
                V118Biome middleBadlandsOrSlope = pickMiddleBiomeOrBadlandsIfHotOrSlopeIfCold(
                    temperatureIndex, humidityIndex, weirdness);
                V118Biome shattered = pickShatteredBiome(temperatureIndex, humidityIndex, weirdness);
                V118Biome plateau = pickPlateauBiome(temperatureIndex, humidityIndex, weirdness);
                V118Biome beach = pickBeachBiome(temperatureIndex);
                V118Biome windswept = maybePickWindsweptSavannaBiome(
                    temperatureIndex, humidityIndex, weirdness, middle);
                V118Biome shatteredCoast = pickShatteredCoastBiome(
                    temperatureIndex, humidityIndex, weirdness);
                V118Biome slope = pickSlopeBiome(temperatureIndex, humidityIndex, weirdness);
                addSurfaceBiome(entries, temperature, humidity,
                    Climate.Parameter.span(NEAR_INLAND_CONTINENTALNESS, FAR_INLAND_CONTINENTALNESS),
                    EROSIONS[0], weirdness, 0.0F, slope);
                addSurfaceBiome(entries, temperature, humidity,
                    Climate.Parameter.span(NEAR_INLAND_CONTINENTALNESS, MID_INLAND_CONTINENTALNESS),
                    EROSIONS[1], weirdness, 0.0F, middleBadlandsOrSlope);
                addSurfaceBiome(entries, temperature, humidity, FAR_INLAND_CONTINENTALNESS,
                    EROSIONS[1], weirdness, 0.0F,
                    temperatureIndex == 0 ? slope : plateau);
                addSurfaceBiome(entries, temperature, humidity, NEAR_INLAND_CONTINENTALNESS,
                    EROSIONS[2], weirdness, 0.0F, middle);
                addSurfaceBiome(entries, temperature, humidity, MID_INLAND_CONTINENTALNESS,
                    EROSIONS[2], weirdness, 0.0F, middleOrBadlands);
                addSurfaceBiome(entries, temperature, humidity, FAR_INLAND_CONTINENTALNESS,
                    EROSIONS[2], weirdness, 0.0F, plateau);
                addSurfaceBiome(entries, temperature, humidity,
                    Climate.Parameter.span(COAST_CONTINENTALNESS, NEAR_INLAND_CONTINENTALNESS),
                    EROSIONS[3], weirdness, 0.0F, middle);
                addSurfaceBiome(entries, temperature, humidity,
                    Climate.Parameter.span(MID_INLAND_CONTINENTALNESS, FAR_INLAND_CONTINENTALNESS),
                    EROSIONS[3], weirdness, 0.0F, middleOrBadlands);
                if (weirdness.max() < 0L) {
                    addSurfaceBiome(entries, temperature, humidity, COAST_CONTINENTALNESS,
                        EROSIONS[4], weirdness, 0.0F, beach);
                    addSurfaceBiome(entries, temperature, humidity,
                        Climate.Parameter.span(NEAR_INLAND_CONTINENTALNESS,
                            FAR_INLAND_CONTINENTALNESS),
                        EROSIONS[4], weirdness, 0.0F, middle);
                } else {
                    addSurfaceBiome(entries, temperature, humidity,
                        Climate.Parameter.span(COAST_CONTINENTALNESS,
                            FAR_INLAND_CONTINENTALNESS),
                        EROSIONS[4], weirdness, 0.0F, middle);
                }
                addSurfaceBiome(entries, temperature, humidity, COAST_CONTINENTALNESS,
                    EROSIONS[5], weirdness, 0.0F, shatteredCoast);
                addSurfaceBiome(entries, temperature, humidity, NEAR_INLAND_CONTINENTALNESS,
                    EROSIONS[5], weirdness, 0.0F, windswept);
                addSurfaceBiome(entries, temperature, humidity,
                    Climate.Parameter.span(MID_INLAND_CONTINENTALNESS, FAR_INLAND_CONTINENTALNESS),
                    EROSIONS[5], weirdness, 0.0F, shattered);
                if (weirdness.max() < 0L) {
                    addSurfaceBiome(entries, temperature, humidity, COAST_CONTINENTALNESS,
                        EROSIONS[6], weirdness, 0.0F, beach);
                } else {
                    addSurfaceBiome(entries, temperature, humidity, COAST_CONTINENTALNESS,
                        EROSIONS[6], weirdness, 0.0F, middle);
                }
                if (temperatureIndex == 0) {
                    addSurfaceBiome(entries, temperature, humidity,
                        Climate.Parameter.span(NEAR_INLAND_CONTINENTALNESS,
                            FAR_INLAND_CONTINENTALNESS),
                        EROSIONS[6], weirdness, 0.0F, middle);
                }
            }
        }
    }

    private static void addLowSlice(List<Climate.Entry<V118Biome>> entries,
            Climate.Parameter weirdness) {
        addSurfaceBiome(entries, FULL_RANGE, FULL_RANGE, COAST_CONTINENTALNESS,
            Climate.Parameter.span(EROSIONS[0], EROSIONS[2]), weirdness, 0.0F,
            V118Biome.STONY_SHORE);
        addSurfaceBiome(entries, UNFROZEN_RANGE, FULL_RANGE,
            Climate.Parameter.span(NEAR_INLAND_CONTINENTALNESS, FAR_INLAND_CONTINENTALNESS),
            EROSIONS[6], weirdness, 0.0F, V118Biome.SWAMP);
        for (int temperatureIndex = 0; temperatureIndex < TEMPERATURES.length; ++temperatureIndex) {
            Climate.Parameter temperature = TEMPERATURES[temperatureIndex];
            for (int humidityIndex = 0; humidityIndex < HUMIDITIES.length; ++humidityIndex) {
                Climate.Parameter humidity = HUMIDITIES[humidityIndex];
                V118Biome middle = pickMiddleBiome(temperatureIndex, humidityIndex, weirdness);
                V118Biome middleOrBadlands = pickMiddleBiomeOrBadlandsIfHot(
                    temperatureIndex, humidityIndex, weirdness);
                V118Biome middleBadlandsOrSlope = pickMiddleBiomeOrBadlandsIfHotOrSlopeIfCold(
                    temperatureIndex, humidityIndex, weirdness);
                V118Biome beach = pickBeachBiome(temperatureIndex);
                V118Biome windswept = maybePickWindsweptSavannaBiome(
                    temperatureIndex, humidityIndex, weirdness, middle);
                V118Biome shatteredCoast = pickShatteredCoastBiome(
                    temperatureIndex, humidityIndex, weirdness);
                addSurfaceBiome(entries, temperature, humidity, NEAR_INLAND_CONTINENTALNESS,
                    Climate.Parameter.span(EROSIONS[0], EROSIONS[1]), weirdness, 0.0F,
                    middleOrBadlands);
                addSurfaceBiome(entries, temperature, humidity,
                    Climate.Parameter.span(MID_INLAND_CONTINENTALNESS, FAR_INLAND_CONTINENTALNESS),
                    Climate.Parameter.span(EROSIONS[0], EROSIONS[1]), weirdness, 0.0F,
                    middleBadlandsOrSlope);
                addSurfaceBiome(entries, temperature, humidity, NEAR_INLAND_CONTINENTALNESS,
                    Climate.Parameter.span(EROSIONS[2], EROSIONS[3]), weirdness, 0.0F, middle);
                addSurfaceBiome(entries, temperature, humidity,
                    Climate.Parameter.span(MID_INLAND_CONTINENTALNESS, FAR_INLAND_CONTINENTALNESS),
                    Climate.Parameter.span(EROSIONS[2], EROSIONS[3]), weirdness, 0.0F,
                    middleOrBadlands);
                addSurfaceBiome(entries, temperature, humidity, COAST_CONTINENTALNESS,
                    Climate.Parameter.span(EROSIONS[3], EROSIONS[4]), weirdness, 0.0F, beach);
                addSurfaceBiome(entries, temperature, humidity,
                    Climate.Parameter.span(NEAR_INLAND_CONTINENTALNESS, FAR_INLAND_CONTINENTALNESS),
                    EROSIONS[4], weirdness, 0.0F, middle);
                addSurfaceBiome(entries, temperature, humidity, COAST_CONTINENTALNESS,
                    EROSIONS[5], weirdness, 0.0F, shatteredCoast);
                addSurfaceBiome(entries, temperature, humidity, NEAR_INLAND_CONTINENTALNESS,
                    EROSIONS[5], weirdness, 0.0F, windswept);
                addSurfaceBiome(entries, temperature, humidity,
                    Climate.Parameter.span(MID_INLAND_CONTINENTALNESS, FAR_INLAND_CONTINENTALNESS),
                    EROSIONS[5], weirdness, 0.0F, middle);
                addSurfaceBiome(entries, temperature, humidity, COAST_CONTINENTALNESS,
                    EROSIONS[6], weirdness, 0.0F, beach);
                if (temperatureIndex == 0) {
                    addSurfaceBiome(entries, temperature, humidity,
                        Climate.Parameter.span(NEAR_INLAND_CONTINENTALNESS,
                            FAR_INLAND_CONTINENTALNESS),
                        EROSIONS[6], weirdness, 0.0F, middle);
                }
            }
        }
    }

    private static void addValleys(List<Climate.Entry<V118Biome>> entries,
            Climate.Parameter weirdness) {
        addSurfaceBiome(entries, FROZEN_RANGE, FULL_RANGE, COAST_CONTINENTALNESS,
            Climate.Parameter.span(EROSIONS[0], EROSIONS[1]), weirdness, 0.0F,
            weirdness.max() < 0L ? V118Biome.STONY_SHORE : V118Biome.FROZEN_RIVER);
        addSurfaceBiome(entries, UNFROZEN_RANGE, FULL_RANGE, COAST_CONTINENTALNESS,
            Climate.Parameter.span(EROSIONS[0], EROSIONS[1]), weirdness, 0.0F,
            weirdness.max() < 0L ? V118Biome.STONY_SHORE : V118Biome.RIVER);
        addSurfaceBiome(entries, FROZEN_RANGE, FULL_RANGE, NEAR_INLAND_CONTINENTALNESS,
            Climate.Parameter.span(EROSIONS[0], EROSIONS[1]), weirdness, 0.0F,
            V118Biome.FROZEN_RIVER);
        addSurfaceBiome(entries, UNFROZEN_RANGE, FULL_RANGE, NEAR_INLAND_CONTINENTALNESS,
            Climate.Parameter.span(EROSIONS[0], EROSIONS[1]), weirdness, 0.0F, V118Biome.RIVER);
        addSurfaceBiome(entries, FROZEN_RANGE, FULL_RANGE,
            Climate.Parameter.span(COAST_CONTINENTALNESS, FAR_INLAND_CONTINENTALNESS),
            Climate.Parameter.span(EROSIONS[2], EROSIONS[5]), weirdness, 0.0F,
            V118Biome.FROZEN_RIVER);
        addSurfaceBiome(entries, UNFROZEN_RANGE, FULL_RANGE,
            Climate.Parameter.span(COAST_CONTINENTALNESS, FAR_INLAND_CONTINENTALNESS),
            Climate.Parameter.span(EROSIONS[2], EROSIONS[5]), weirdness, 0.0F, V118Biome.RIVER);
        addSurfaceBiome(entries, FROZEN_RANGE, FULL_RANGE, COAST_CONTINENTALNESS,
            EROSIONS[6], weirdness, 0.0F, V118Biome.FROZEN_RIVER);
        addSurfaceBiome(entries, UNFROZEN_RANGE, FULL_RANGE, COAST_CONTINENTALNESS,
            EROSIONS[6], weirdness, 0.0F, V118Biome.RIVER);
        addSurfaceBiome(entries, UNFROZEN_RANGE, FULL_RANGE,
            Climate.Parameter.span(INLAND_CONTINENTALNESS, FAR_INLAND_CONTINENTALNESS),
            EROSIONS[6], weirdness, 0.0F, V118Biome.SWAMP);
        addSurfaceBiome(entries, FROZEN_RANGE, FULL_RANGE,
            Climate.Parameter.span(INLAND_CONTINENTALNESS, FAR_INLAND_CONTINENTALNESS),
            EROSIONS[6], weirdness, 0.0F, V118Biome.FROZEN_RIVER);
        for (int temperatureIndex = 0; temperatureIndex < TEMPERATURES.length; ++temperatureIndex) {
            Climate.Parameter temperature = TEMPERATURES[temperatureIndex];
            for (int humidityIndex = 0; humidityIndex < HUMIDITIES.length; ++humidityIndex) {
                V118Biome biome = pickMiddleBiomeOrBadlandsIfHot(
                    temperatureIndex, humidityIndex, weirdness);
                addSurfaceBiome(entries, temperature, HUMIDITIES[humidityIndex],
                    Climate.Parameter.span(MID_INLAND_CONTINENTALNESS, FAR_INLAND_CONTINENTALNESS),
                    Climate.Parameter.span(EROSIONS[0], EROSIONS[1]), weirdness, 0.0F, biome);
            }
        }
    }

    private static void addUndergroundBiomes(List<Climate.Entry<V118Biome>> entries) {
        addUndergroundBiome(entries, FULL_RANGE, FULL_RANGE, Climate.Parameter.span(0.8F, 1.0F),
            FULL_RANGE, FULL_RANGE, 0.0F, V118Biome.DRIPSTONE_CAVES);
        addUndergroundBiome(entries, FULL_RANGE, Climate.Parameter.span(0.7F, 1.0F),
            FULL_RANGE, FULL_RANGE, FULL_RANGE, 0.0F, V118Biome.LUSH_CAVES);
    }

    private static V118Biome pickMiddleBiome(int temperature, int humidity,
            Climate.Parameter weirdness) {
        if (weirdness.max() < 0L) {
            return MIDDLE_BIOMES[temperature][humidity];
        }
        V118Biome variant = MIDDLE_BIOMES_VARIANT[temperature][humidity];
        return variant == null ? MIDDLE_BIOMES[temperature][humidity] : variant;
    }

    private static V118Biome pickMiddleBiomeOrBadlandsIfHot(int temperature, int humidity,
            Climate.Parameter weirdness) {
        return temperature == 4 ? pickBadlandsBiome(humidity, weirdness)
            : pickMiddleBiome(temperature, humidity, weirdness);
    }

    private static V118Biome pickMiddleBiomeOrBadlandsIfHotOrSlopeIfCold(int temperature,
            int humidity, Climate.Parameter weirdness) {
        return temperature == 0 ? pickSlopeBiome(temperature, humidity, weirdness)
            : pickMiddleBiomeOrBadlandsIfHot(temperature, humidity, weirdness);
    }

    private static V118Biome maybePickWindsweptSavannaBiome(int temperature, int humidity,
            Climate.Parameter weirdness, V118Biome fallback) {
        if (temperature > 1 && humidity < 4 && weirdness.max() >= 0L) {
            return V118Biome.WINDSWEPT_SAVANNA;
        }
        return fallback;
    }

    private static V118Biome pickShatteredCoastBiome(int temperature, int humidity,
            Climate.Parameter weirdness) {
        V118Biome fallback = weirdness.max() >= 0L
            ? pickMiddleBiome(temperature, humidity, weirdness) : pickBeachBiome(temperature);
        return maybePickWindsweptSavannaBiome(temperature, humidity, weirdness, fallback);
    }

    private static V118Biome pickBeachBiome(int temperature) {
        if (temperature == 0) {
            return V118Biome.SNOWY_BEACH;
        }
        if (temperature == 4) {
            return V118Biome.DESERT;
        }
        return V118Biome.BEACH;
    }

    private static V118Biome pickBadlandsBiome(int humidity, Climate.Parameter weirdness) {
        if (humidity < 2) {
            return weirdness.max() < 0L ? V118Biome.ERODED_BADLANDS : V118Biome.BADLANDS;
        }
        if (humidity < 3) {
            return V118Biome.BADLANDS;
        }
        return V118Biome.WOODED_BADLANDS;
    }

    private static V118Biome pickPlateauBiome(int temperature, int humidity,
            Climate.Parameter weirdness) {
        if (weirdness.max() < 0L) {
            return PLATEAU_BIOMES[temperature][humidity];
        }
        V118Biome variant = PLATEAU_BIOMES_VARIANT[temperature][humidity];
        return variant == null ? PLATEAU_BIOMES[temperature][humidity] : variant;
    }

    private static V118Biome pickPeakBiome(int temperature, int humidity,
            Climate.Parameter weirdness) {
        if (temperature <= 2) {
            return weirdness.max() < 0L ? V118Biome.JAGGED_PEAKS : V118Biome.FROZEN_PEAKS;
        }
        if (temperature == 3) {
            return V118Biome.STONY_PEAKS;
        }
        return pickBadlandsBiome(humidity, weirdness);
    }

    private static V118Biome pickSlopeBiome(int temperature, int humidity,
            Climate.Parameter weirdness) {
        if (temperature >= 3) {
            return pickPlateauBiome(temperature, humidity, weirdness);
        }
        return humidity <= 1 ? V118Biome.SNOWY_SLOPES : V118Biome.GROVE;
    }

    private static V118Biome pickShatteredBiome(int temperature, int humidity,
            Climate.Parameter weirdness) {
        V118Biome shattered = SHATTERED_BIOMES[temperature][humidity];
        return shattered == null ? pickMiddleBiome(temperature, humidity, weirdness) : shattered;
    }

    private static void addSurfaceBiome(List<Climate.Entry<V118Biome>> entries,
            Climate.Parameter temperature, Climate.Parameter humidity,
            Climate.Parameter continentalness, Climate.Parameter erosion,
            Climate.Parameter weirdness, float offset, V118Biome biome) {
        entries.add(new Climate.Entry<V118Biome>(Climate.parameters(temperature, humidity,
            continentalness, erosion, Climate.Parameter.point(0.0F), weirdness, offset), biome));
        entries.add(new Climate.Entry<V118Biome>(Climate.parameters(temperature, humidity,
            continentalness, erosion, Climate.Parameter.point(1.0F), weirdness, offset), biome));
    }

    private static void addUndergroundBiome(List<Climate.Entry<V118Biome>> entries,
            Climate.Parameter temperature, Climate.Parameter humidity,
            Climate.Parameter continentalness, Climate.Parameter erosion,
            Climate.Parameter weirdness, float offset, V118Biome biome) {
        entries.add(new Climate.Entry<V118Biome>(Climate.parameters(temperature, humidity,
            continentalness, erosion, Climate.Parameter.span(0.2F, 0.9F), weirdness, offset),
            biome));
    }
}

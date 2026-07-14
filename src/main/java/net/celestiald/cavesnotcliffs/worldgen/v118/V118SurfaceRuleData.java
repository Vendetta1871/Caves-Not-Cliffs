package net.celestiald.cavesnotcliffs.worldgen.v118;

import static net.celestiald.cavesnotcliffs.worldgen.v118.V118SurfaceRules.DEEP_UNDER_FLOOR;
import static net.celestiald.cavesnotcliffs.worldgen.v118.V118SurfaceRules.ON_CEILING;
import static net.celestiald.cavesnotcliffs.worldgen.v118.V118SurfaceRules.ON_FLOOR;
import static net.celestiald.cavesnotcliffs.worldgen.v118.V118SurfaceRules.UNDER_FLOOR;
import static net.celestiald.cavesnotcliffs.worldgen.v118.V118SurfaceRules.VERY_DEEP_UNDER_FLOOR;

/** Exact ordered Overworld rule tree from Java 1.18.2 {@code SurfaceRuleData}. */
public final class V118SurfaceRuleData {
    private static final V118SurfaceRules.RuleSource AIR = state(V118Material.AIR);
    private static final V118SurfaceRules.RuleSource BEDROCK = state(V118Material.BEDROCK);
    private static final V118SurfaceRules.RuleSource WHITE_TERRACOTTA =
        state(V118Material.WHITE_TERRACOTTA);
    private static final V118SurfaceRules.RuleSource ORANGE_TERRACOTTA =
        state(V118Material.ORANGE_TERRACOTTA);
    private static final V118SurfaceRules.RuleSource TERRACOTTA = state(V118Material.TERRACOTTA);
    private static final V118SurfaceRules.RuleSource RED_SAND = state(V118Material.RED_SAND);
    private static final V118SurfaceRules.RuleSource RED_SANDSTONE =
        state(V118Material.RED_SANDSTONE);
    private static final V118SurfaceRules.RuleSource STONE = state(V118Material.STONE);
    private static final V118SurfaceRules.RuleSource DEEPSLATE = state(V118Material.DEEPSLATE);
    private static final V118SurfaceRules.RuleSource DIRT = state(V118Material.DIRT);
    private static final V118SurfaceRules.RuleSource PODZOL = state(V118Material.PODZOL);
    private static final V118SurfaceRules.RuleSource COARSE_DIRT = state(V118Material.COARSE_DIRT);
    private static final V118SurfaceRules.RuleSource MYCELIUM = state(V118Material.MYCELIUM);
    private static final V118SurfaceRules.RuleSource GRASS_BLOCK = state(V118Material.GRASS_BLOCK);
    private static final V118SurfaceRules.RuleSource CALCITE = state(V118Material.CALCITE);
    private static final V118SurfaceRules.RuleSource GRAVEL = state(V118Material.GRAVEL);
    private static final V118SurfaceRules.RuleSource SAND = state(V118Material.SAND);
    private static final V118SurfaceRules.RuleSource SANDSTONE = state(V118Material.SANDSTONE);
    private static final V118SurfaceRules.RuleSource PACKED_ICE = state(V118Material.PACKED_ICE);
    private static final V118SurfaceRules.RuleSource SNOW_BLOCK = state(V118Material.SNOW_BLOCK);
    private static final V118SurfaceRules.RuleSource POWDER_SNOW = state(V118Material.POWDER_SNOW);
    private static final V118SurfaceRules.RuleSource ICE = state(V118Material.ICE);
    private static final V118SurfaceRules.RuleSource WATER = state(V118Material.WATER);

    private V118SurfaceRuleData() {
    }

    public static V118SurfaceRules.RuleSource overworld() {
        return overworldLike(true, false, true);
    }

    /** Exposed for parity tests of the three vanilla boolean branches. */
    static V118SurfaceRules.RuleSource overworldLike(boolean gateByPreliminarySurface,
            boolean bedrockRoof, boolean bedrockFloor) {
        V118SurfaceRules.ConditionSource y97 = yBlock(97, 2);
        V118SurfaceRules.ConditionSource y256 = yBlock(256, 0);
        V118SurfaceRules.ConditionSource start63 = yStart(63, -1);
        V118SurfaceRules.ConditionSource start74 = yStart(74, 1);
        V118SurfaceRules.ConditionSource y62 = yBlock(62, 0);
        V118SurfaceRules.ConditionSource y63 = yBlock(63, 0);
        V118SurfaceRules.ConditionSource waterMinusOne =
            V118SurfaceRules.waterBlockCheck(-1, 0);
        V118SurfaceRules.ConditionSource waterZero = V118SurfaceRules.waterBlockCheck(0, 0);
        V118SurfaceRules.ConditionSource waterStart = V118SurfaceRules.waterStartCheck(-6, -1);
        V118SurfaceRules.ConditionSource hole = V118SurfaceRules.hole();
        V118SurfaceRules.ConditionSource frozenOceans = V118SurfaceRules.isBiome(
            V118Biome.FROZEN_OCEAN, V118Biome.DEEP_FROZEN_OCEAN);
        V118SurfaceRules.ConditionSource steep = V118SurfaceRules.steep();

        V118SurfaceRules.RuleSource grassOrDirt = sequence(ifTrue(waterZero, GRASS_BLOCK), DIRT);
        V118SurfaceRules.RuleSource sand = sequence(ifTrue(ON_CEILING, SANDSTONE), SAND);
        V118SurfaceRules.RuleSource gravel = sequence(ifTrue(ON_CEILING, STONE), GRAVEL);
        V118SurfaceRules.ConditionSource beachOrWarmOcean = V118SurfaceRules.isBiome(
            V118Biome.WARM_OCEAN, V118Biome.BEACH, V118Biome.SNOWY_BEACH);
        V118SurfaceRules.ConditionSource desert = V118SurfaceRules.isBiome(V118Biome.DESERT);

        V118SurfaceRules.RuleSource rockyBiomes = sequence(
            ifTrue(V118SurfaceRules.isBiome(V118Biome.STONY_PEAKS), sequence(
                ifTrue(noise("calcite", -0.0125D, 0.0125D), CALCITE), STONE)),
            ifTrue(V118SurfaceRules.isBiome(V118Biome.STONY_SHORE), sequence(
                ifTrue(noise("gravel", -0.05D, 0.05D), gravel), STONE)),
            ifTrue(V118SurfaceRules.isBiome(V118Biome.WINDSWEPT_HILLS),
                ifTrue(surfaceNoiseAbove(1.0D), STONE)),
            ifTrue(beachOrWarmOcean, sand),
            ifTrue(desert, sand),
            ifTrue(V118SurfaceRules.isBiome(V118Biome.DRIPSTONE_CAVES), STONE));

        V118SurfaceRules.RuleSource narrowPowderSnow = ifTrue(
            noise("powder_snow", 0.45D, 0.58D), ifTrue(waterZero, POWDER_SNOW));
        V118SurfaceRules.RuleSource broadPowderSnow = ifTrue(
            noise("powder_snow", 0.35D, 0.6D), ifTrue(waterZero, POWDER_SNOW));

        V118SurfaceRules.RuleSource underFloorMountain = sequence(
            ifTrue(V118SurfaceRules.isBiome(V118Biome.FROZEN_PEAKS), sequence(
                ifTrue(steep, PACKED_ICE),
                ifTrue(noise("packed_ice", -0.5D, 0.2D), PACKED_ICE),
                ifTrue(noise("ice", -0.0625D, 0.025D), ICE),
                ifTrue(waterZero, SNOW_BLOCK))),
            ifTrue(V118SurfaceRules.isBiome(V118Biome.SNOWY_SLOPES), sequence(
                ifTrue(steep, STONE), narrowPowderSnow, ifTrue(waterZero, SNOW_BLOCK))),
            ifTrue(V118SurfaceRules.isBiome(V118Biome.JAGGED_PEAKS), STONE),
            ifTrue(V118SurfaceRules.isBiome(V118Biome.GROVE), sequence(narrowPowderSnow, DIRT)),
            rockyBiomes,
            ifTrue(V118SurfaceRules.isBiome(V118Biome.WINDSWEPT_SAVANNA),
                ifTrue(surfaceNoiseAbove(1.75D), STONE)),
            ifTrue(V118SurfaceRules.isBiome(V118Biome.WINDSWEPT_GRAVELLY_HILLS), sequence(
                ifTrue(surfaceNoiseAbove(2.0D), gravel),
                ifTrue(surfaceNoiseAbove(1.0D), STONE),
                ifTrue(surfaceNoiseAbove(-1.0D), DIRT), gravel)),
            DIRT);

        V118SurfaceRules.RuleSource onFloorMountain = sequence(
            ifTrue(V118SurfaceRules.isBiome(V118Biome.FROZEN_PEAKS), sequence(
                ifTrue(steep, PACKED_ICE),
                ifTrue(noise("packed_ice", 0.0D, 0.2D), PACKED_ICE),
                ifTrue(noise("ice", 0.0D, 0.025D), ICE),
                ifTrue(waterZero, SNOW_BLOCK))),
            ifTrue(V118SurfaceRules.isBiome(V118Biome.SNOWY_SLOPES), sequence(
                ifTrue(steep, STONE), broadPowderSnow, ifTrue(waterZero, SNOW_BLOCK))),
            ifTrue(V118SurfaceRules.isBiome(V118Biome.JAGGED_PEAKS), sequence(
                ifTrue(steep, STONE), ifTrue(waterZero, SNOW_BLOCK))),
            ifTrue(V118SurfaceRules.isBiome(V118Biome.GROVE), sequence(
                broadPowderSnow, ifTrue(waterZero, SNOW_BLOCK))),
            rockyBiomes,
            ifTrue(V118SurfaceRules.isBiome(V118Biome.WINDSWEPT_SAVANNA), sequence(
                ifTrue(surfaceNoiseAbove(1.75D), STONE),
                ifTrue(surfaceNoiseAbove(-0.5D), COARSE_DIRT))),
            ifTrue(V118SurfaceRules.isBiome(V118Biome.WINDSWEPT_GRAVELLY_HILLS), sequence(
                ifTrue(surfaceNoiseAbove(2.0D), gravel),
                ifTrue(surfaceNoiseAbove(1.0D), STONE),
                ifTrue(surfaceNoiseAbove(-1.0D), grassOrDirt), gravel)),
            ifTrue(V118SurfaceRules.isBiome(V118Biome.OLD_GROWTH_PINE_TAIGA,
                    V118Biome.OLD_GROWTH_SPRUCE_TAIGA), sequence(
                ifTrue(surfaceNoiseAbove(1.75D), COARSE_DIRT),
                ifTrue(surfaceNoiseAbove(-0.95D), PODZOL))),
            ifTrue(V118SurfaceRules.isBiome(V118Biome.ICE_SPIKES),
                ifTrue(waterZero, SNOW_BLOCK)),
            ifTrue(V118SurfaceRules.isBiome(V118Biome.MUSHROOM_FIELDS), MYCELIUM),
            grassOrDirt);

        V118SurfaceRules.ConditionSource surfaceBandLow = noise("surface", -0.909D, -0.5454D);
        V118SurfaceRules.ConditionSource surfaceBandMid = noise("surface", -0.1818D, 0.1818D);
        V118SurfaceRules.ConditionSource surfaceBandHigh = noise("surface", 0.5454D, 0.909D);

        V118SurfaceRules.RuleSource surface = sequence(
            ifTrue(ON_FLOOR, sequence(
                ifTrue(V118SurfaceRules.isBiome(V118Biome.WOODED_BADLANDS), ifTrue(y97,
                    sequence(ifTrue(surfaceBandLow, COARSE_DIRT),
                        ifTrue(surfaceBandMid, COARSE_DIRT),
                        ifTrue(surfaceBandHigh, COARSE_DIRT), grassOrDirt))),
                ifTrue(V118SurfaceRules.isBiome(V118Biome.SWAMP), ifTrue(y62,
                    ifTrue(V118SurfaceRules.not(y63),
                        ifTrue(noise("surface_swamp", 0.0D), WATER)))))),
            ifTrue(V118SurfaceRules.isBiome(V118Biome.BADLANDS, V118Biome.ERODED_BADLANDS,
                    V118Biome.WOODED_BADLANDS), sequence(
                ifTrue(ON_FLOOR, sequence(
                    ifTrue(y256, ORANGE_TERRACOTTA),
                    ifTrue(start74, sequence(
                        ifTrue(surfaceBandLow, TERRACOTTA),
                        ifTrue(surfaceBandMid, TERRACOTTA),
                        ifTrue(surfaceBandHigh, TERRACOTTA),
                        V118SurfaceRules.badlandsBands())),
                    ifTrue(waterMinusOne, sequence(
                        ifTrue(ON_CEILING, RED_SANDSTONE), RED_SAND)),
                    ifTrue(V118SurfaceRules.not(hole), ORANGE_TERRACOTTA),
                    ifTrue(waterStart, WHITE_TERRACOTTA), gravel)),
                ifTrue(start63, sequence(
                    ifTrue(y63, ifTrue(V118SurfaceRules.not(start74), ORANGE_TERRACOTTA)),
                    V118SurfaceRules.badlandsBands())),
                ifTrue(UNDER_FLOOR, ifTrue(waterStart, WHITE_TERRACOTTA)))),
            ifTrue(ON_FLOOR, ifTrue(waterMinusOne, sequence(
                ifTrue(frozenOceans, ifTrue(hole, sequence(
                    ifTrue(waterZero, AIR),
                    ifTrue(V118SurfaceRules.temperature(), ICE), WATER))),
                onFloorMountain))),
            ifTrue(waterStart, sequence(
                ifTrue(ON_FLOOR, ifTrue(frozenOceans, ifTrue(hole, WATER))),
                ifTrue(UNDER_FLOOR, underFloorMountain),
                ifTrue(beachOrWarmOcean, ifTrue(DEEP_UNDER_FLOOR, SANDSTONE)),
                ifTrue(desert, ifTrue(VERY_DEEP_UNDER_FLOOR, SANDSTONE)))),
            ifTrue(ON_FLOOR, sequence(
                ifTrue(V118SurfaceRules.isBiome(V118Biome.FROZEN_PEAKS,
                    V118Biome.JAGGED_PEAKS), STONE),
                ifTrue(V118SurfaceRules.isBiome(V118Biome.WARM_OCEAN,
                    V118Biome.LUKEWARM_OCEAN, V118Biome.DEEP_LUKEWARM_OCEAN), sand),
                gravel)));

        V118SurfaceRules.RuleSource preliminaryGated = ifTrue(
            V118SurfaceRules.abovePreliminarySurface(), surface);
        V118SurfaceRules.RuleSource deepslate = ifTrue(V118SurfaceRules.verticalGradient(
            "deepslate", V118SurfaceRules.VerticalAnchor.absolute(0),
            V118SurfaceRules.VerticalAnchor.absolute(8)), DEEPSLATE);
        if (bedrockRoof && bedrockFloor) {
            return sequence(bedrockRoof(), bedrockFloor(),
                gateByPreliminarySurface ? preliminaryGated : surface, deepslate);
        }
        if (bedrockRoof) {
            return sequence(bedrockRoof(), gateByPreliminarySurface ? preliminaryGated : surface,
                deepslate);
        }
        if (bedrockFloor) {
            return sequence(bedrockFloor(), gateByPreliminarySurface ? preliminaryGated : surface,
                deepslate);
        }
        return sequence(gateByPreliminarySurface ? preliminaryGated : surface, deepslate);
    }

    private static V118SurfaceRules.RuleSource bedrockRoof() {
        return ifTrue(V118SurfaceRules.not(V118SurfaceRules.verticalGradient("bedrock_roof",
            V118SurfaceRules.VerticalAnchor.belowTop(5),
            V118SurfaceRules.VerticalAnchor.top())), BEDROCK);
    }

    private static V118SurfaceRules.RuleSource bedrockFloor() {
        return ifTrue(V118SurfaceRules.verticalGradient("bedrock_floor",
            V118SurfaceRules.VerticalAnchor.bottom(),
            V118SurfaceRules.VerticalAnchor.aboveBottom(5)), BEDROCK);
    }

    private static V118SurfaceRules.ConditionSource yBlock(int y, int multiplier) {
        return V118SurfaceRules.yBlockCheck(V118SurfaceRules.VerticalAnchor.absolute(y),
            multiplier);
    }

    private static V118SurfaceRules.ConditionSource yStart(int y, int multiplier) {
        return V118SurfaceRules.yStartCheck(V118SurfaceRules.VerticalAnchor.absolute(y),
            multiplier);
    }

    private static V118SurfaceRules.ConditionSource noise(String name, double minimum) {
        return V118SurfaceRules.noiseCondition(name, minimum);
    }

    private static V118SurfaceRules.ConditionSource noise(String name, double minimum,
            double maximum) {
        return V118SurfaceRules.noiseCondition(name, minimum, maximum);
    }

    private static V118SurfaceRules.ConditionSource surfaceNoiseAbove(double threshold) {
        return noise("surface", threshold / 8.25D);
    }

    private static V118SurfaceRules.RuleSource state(V118Material material) {
        return V118SurfaceRules.state(material);
    }

    private static V118SurfaceRules.RuleSource ifTrue(
            V118SurfaceRules.ConditionSource condition, V118SurfaceRules.RuleSource rule) {
        return V118SurfaceRules.ifTrue(condition, rule);
    }

    private static V118SurfaceRules.RuleSource sequence(V118SurfaceRules.RuleSource... rules) {
        return V118SurfaceRules.sequence(rules);
    }
}

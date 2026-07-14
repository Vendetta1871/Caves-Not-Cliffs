package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class V118ClimateSamplerTest {
    @Test
    public void samplesTheSixRouterAxesAtOfficialQuartCoordinates() {
        V118NoiseRouter router = V118NoiseRouterData.create(123456789L,
            V118NoiseRouterData.Profile.DEFAULT);
        V118NoiseSettings settings = V118NoiseSettings.overworld(false);
        V118ClimateSampler sampler = new V118ClimateSampler(router, settings,
            new OverworldBiomeBuilder());
        Climate.TargetPoint actual = sampler.sampleQuart(-5, -16, 7);
        DensityFunction.SinglePointContext context =
            new DensityFunction.SinglePointContext(-20, -64, 28);

        assertEquals(Climate.quantizeCoord((float) V118DensityInterpolator
            .realize(router.temperature(), settings).compute(context)), actual.temperature());
        assertEquals(Climate.quantizeCoord((float) V118DensityInterpolator
            .realize(router.humidity(), settings).compute(context)), actual.humidity());
        assertEquals(Climate.quantizeCoord((float) V118DensityInterpolator
            .realize(router.continents(), settings).compute(context)), actual.continentalness());
        assertEquals(Climate.quantizeCoord((float) V118DensityInterpolator
            .realize(router.erosion(), settings).compute(context)), actual.erosion());
        assertEquals(Climate.quantizeCoord((float) V118DensityInterpolator
            .realize(router.depth(), settings).compute(context)), actual.depth());
        assertEquals(Climate.quantizeCoord((float) V118DensityInterpolator
            .realize(router.ridges(), settings).compute(context)), actual.weirdness());
    }

    @Test
    public void blockResolutionUsesFloorDividedQuartCellsAtNegativeBoundaries() {
        V118ClimateSampler sampler = new V118ClimateSampler(
            V118NoiseRouterData.create(-1L, V118NoiseRouterData.Profile.AMPLIFIED),
            V118NoiseSettings.overworld(true), new OverworldBiomeBuilder());
        assertEquals(sampler.resolveQuart(-1, -1, -1), sampler.resolveBlock(-1, -1, -1));
        assertEquals(sampler.resolveQuart(-1, -1, -1), sampler.resolveBlock(-4, -4, -4));
        assertEquals(sampler.resolveQuart(0, 0, 0), sampler.resolveBlock(3, 3, 3));
        assertEquals(sampler.resolveQuart(1, 2, 1), sampler.resolveBlock(4, 8, 4));
    }

    @Test
    public void allNativeProfilesResolveDeterministicallyAcrossBuildRange() {
        for (V118NoiseRouterData.Profile profile : V118NoiseRouterData.Profile.values()) {
            long seed = Long.MIN_VALUE + profile.ordinal();
            V118ClimateSampler first = sampler(seed, profile);
            V118ClimateSampler second = sampler(seed, profile);
            for (int quartY = TerrainColumn.MIN_QUART_Y;
                    quartY <= TerrainColumn.MAX_QUART_Y; quartY += 7) {
                assertEquals(first.resolveQuart(-17, quartY, 31),
                    second.resolveQuart(-17, quartY, 31));
            }
        }
    }

    private static V118ClimateSampler sampler(long seed, V118NoiseRouterData.Profile profile) {
        return new V118ClimateSampler(V118NoiseRouterData.create(seed, profile),
            V118NoiseSettings.overworld(profile.amplified()), new OverworldBiomeBuilder());
    }
}

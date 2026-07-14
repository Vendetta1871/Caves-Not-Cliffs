package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class V118PreliminarySurfaceTest {
    @Test
    public void scansTopToBottomAtExactEightBlockCellBoundaries() {
        V118NoiseSettings settings = V118NoiseSettings.overworld(false);
        DensityFunction density = new DensityFunction.SimpleFunction() {
            @Override
            public double compute(DensityFunction.FunctionContext context) {
                return context.blockY() <= 136 ? 2.0D : -2.0D;
            }

            @Override
            public double minValue() {
                return -2.0D;
            }

            @Override
            public double maxValue() {
                return 2.0D;
            }
        };
        V118PreliminarySurface surface = new V118PreliminarySurface(settings, density);
        assertEquals(136, surface.preliminarySurfaceLevel(-4, 8));
    }

    @Test
    public void returnsOfficialSentinelWhenNoCellPasses() {
        V118PreliminarySurface surface = new V118PreliminarySurface(
            V118NoiseSettings.overworld(false), DensityFunctions.constant(-64.0D));
        assertEquals(Integer.MAX_VALUE, surface.preliminarySurfaceLevel(0, 0));
    }

    @Test
    public void cachesByExactBlockAnchorWithoutChangingResults() {
        V118NoiseRouter router = V118NoiseRouterData.create(1L,
            V118NoiseRouterData.Profile.DEFAULT);
        V118PreliminarySurface surface = new V118PreliminarySurface(
            V118NoiseSettings.overworld(false), router.initialDensityWithoutJaggedness());
        int expected = surface.preliminarySurfaceLevel(-16, 32);
        assertEquals(expected, surface.preliminarySurfaceLevel(-16, 32));
        assertEquals(1, surface.cachedPositions());
        surface.preliminarySurfaceLevel(-12, 32);
        assertEquals(2, surface.cachedPositions());
    }
}

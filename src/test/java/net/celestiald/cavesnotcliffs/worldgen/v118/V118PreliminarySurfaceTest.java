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

    @Test
    public void columnTableMatchesTheLazyScanAcrossTheWholeAquiferHalo() {
        for (V118NoiseRouterData.Profile profile : new V118NoiseRouterData.Profile[] {
                V118NoiseRouterData.Profile.DEFAULT,
                V118NoiseRouterData.Profile.AMPLIFIED}) {
            V118NoiseSettings settings = V118NoiseSettings.overworld(profile.amplified());
            V118NoiseRouter router = V118NoiseRouterData.create(7L, profile);
            V118PreliminarySurface lazy = new V118PreliminarySurface(settings,
                router.initialDensityWithoutJaggedness());
            NoiseBasedAquifer.PreliminarySurfaceLookup table =
                V118PreliminarySurface.forColumn(settings,
                    V118DensityInterpolator.realize(router.initialDensityWithoutJaggedness(),
                        settings),
                    3, -2);
            int minBlockX = 3 * TerrainColumn.WIDTH;
            int minBlockZ = -2 * TerrainColumn.WIDTH;
            for (int blockX = minBlockX - 64; blockX <= minBlockX + 40; blockX += 4) {
                for (int blockZ = minBlockZ - 32; blockZ <= minBlockZ + 40; blockZ += 4) {
                    assertEquals(profile + " at " + blockX + "," + blockZ,
                        lazy.preliminarySurfaceLevel(blockX, blockZ),
                        table.preliminarySurfaceLevel(blockX, blockZ));
                }
            }
        }
    }

    @Test
    public void columnTableRejectsQueriesOutsideItsColumnHalo() {
        V118NoiseSettings settings = V118NoiseSettings.overworld(false);
        V118NoiseRouter router = V118NoiseRouterData.create(7L,
            V118NoiseRouterData.Profile.DEFAULT);
        NoiseBasedAquifer.PreliminarySurfaceLookup table =
            V118PreliminarySurface.forColumn(settings,
                V118DensityInterpolator.realize(router.initialDensityWithoutJaggedness(),
                    settings),
                0, 0);
        try {
            table.preliminarySurfaceLevel(-68, 0);
            org.junit.Assert.fail("queries west of the halo must fail fast");
        } catch (IllegalArgumentException expected) {
            // expected
        }
        try {
            table.preliminarySurfaceLevel(0, 44);
            org.junit.Assert.fail("queries south of the halo must fail fast");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}

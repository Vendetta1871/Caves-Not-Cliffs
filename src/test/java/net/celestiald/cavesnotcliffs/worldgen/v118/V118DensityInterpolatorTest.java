package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class V118DensityInterpolatorTest {
    @Test
    public void interpolatesAcrossNegativeDensityCellsWithOfficialAxisOrder() {
        DensityFunction corners = new CoordinateCode();
        DensityFunction interpolated = V118DensityInterpolator.realize(
            DensityFunctions.interpolated(corners), V118NoiseSettings.overworld(false));

        assertEquals(corners.compute(-4, -8, -4), interpolated.compute(-4, -8, -4), 0.0D);
        assertEquals(corners.compute(0, 0, 0), interpolated.compute(0, 0, 0), 0.0D);
        assertEquals(expected(corners, -4, -8, -4, -1, -1, -1),
            interpolated.compute(-1, -1, -1), 0.0D);
        assertEquals(expected(corners, 0, 0, 0, 3, 7, 3),
            interpolated.compute(3, 7, 3), 0.0D);
    }

    @Test
    public void flatCacheSnapsToQuartCoordinatesAndZeroY() {
        DensityFunction cached = V118DensityInterpolator.realize(
            DensityFunctions.flatCache(new CoordinateCode()),
            V118NoiseSettings.overworld(false));
        assertEquals(new CoordinateCode().compute(-4, 0, -8),
            cached.compute(-1, 319, -5), 0.0D);
        assertEquals(new CoordinateCode().compute(4, 0, 8),
            cached.compute(7, -64, 11), 0.0D);
    }

    @Test
    public void nonSemanticCacheMarkersRemainNumericallyTransparent() {
        DensityFunction base = new CoordinateCode();
        for (DensityFunction marker : new DensityFunction[] {
                DensityFunctions.cache2d(base), DensityFunctions.cacheOnce(base),
                DensityFunctions.cacheAllInCell(base)}) {
            DensityFunction realized = V118DensityInterpolator.realize(marker,
                V118NoiseSettings.overworld(false));
            assertEquals(base.compute(-17, -64, 31), realized.compute(-17, -64, 31), 0.0D);
        }
    }

    @Test
    public void preservesDistinctNoiseChunkAndCellCacheRoundingOrders() {
        DensityFunction corners = new RoundingSensitiveCorners();
        V118NoiseSettings settings = V118NoiseSettings.overworld(false);
        DensityFunction direct = V118DensityInterpolator.realize(
            DensityFunctions.interpolated(corners), settings);
        DensityFunction cached = V118DensityInterpolator.realizeFinalDensity(
            DensityFunctions.interpolated(corners), settings);

        double directExpected = expected(corners, 0, 0, 0, 3, 3, 2);
        double cachedExpected = expectedCached(corners, 0, 0, 0, 3, 3, 2);
        assertEquals(Double.doubleToRawLongBits(directExpected),
            Double.doubleToRawLongBits(direct.compute(3, 3, 2)));
        assertEquals(Double.doubleToRawLongBits(cachedExpected),
            Double.doubleToRawLongBits(cached.compute(3, 3, 2)));
        assertNotEquals("the official evaluation contexts differ by one ULP",
            Double.doubleToRawLongBits(directExpected),
            Double.doubleToRawLongBits(cachedExpected));
    }

    private static double expected(DensityFunction function, int x0, int y0, int z0,
            int x, int y, int z) {
        double dx = (x - x0) / 4.0D;
        double dy = (y - y0) / 8.0D;
        double dz = (z - z0) / 4.0D;
        double noise000 = function.compute(x0, y0, z0);
        double noise100 = function.compute(x0 + 4, y0, z0);
        double noise010 = function.compute(x0, y0 + 8, z0);
        double noise110 = function.compute(x0 + 4, y0 + 8, z0);
        double noise001 = function.compute(x0, y0, z0 + 4);
        double noise101 = function.compute(x0 + 4, y0, z0 + 4);
        double noise011 = function.compute(x0, y0 + 8, z0 + 4);
        double noise111 = function.compute(x0 + 4, y0 + 8, z0 + 4);
        double valueXZ00 = WorldgenMath.lerp(dy, noise000, noise010);
        double valueXZ10 = WorldgenMath.lerp(dy, noise100, noise110);
        double valueXZ01 = WorldgenMath.lerp(dy, noise001, noise011);
        double valueXZ11 = WorldgenMath.lerp(dy, noise101, noise111);
        return WorldgenMath.lerp(dz,
            WorldgenMath.lerp(dx, valueXZ00, valueXZ10),
            WorldgenMath.lerp(dx, valueXZ01, valueXZ11));
    }

    private static double expectedCached(DensityFunction function, int x0, int y0, int z0,
            int x, int y, int z) {
        double dx = (x - x0) / 4.0D;
        double dy = (y - y0) / 8.0D;
        double dz = (z - z0) / 4.0D;
        return WorldgenMath.lerp3(dx, dy, dz,
            function.compute(x0, y0, z0), function.compute(x0 + 4, y0, z0),
            function.compute(x0, y0 + 8, z0), function.compute(x0 + 4, y0 + 8, z0),
            function.compute(x0, y0, z0 + 4), function.compute(x0 + 4, y0, z0 + 4),
            function.compute(x0, y0 + 8, z0 + 4),
            function.compute(x0 + 4, y0 + 8, z0 + 4));
    }

    private static final class CoordinateCode implements DensityFunction.SimpleFunction {
        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return context.blockX() * 10000.0D + context.blockY() * 100.0D
                + context.blockZ();
        }

        @Override
        public double minValue() {
            return Double.NEGATIVE_INFINITY;
        }

        @Override
        public double maxValue() {
            return Double.POSITIVE_INFINITY;
        }
    }

    private static final class RoundingSensitiveCorners
            implements DensityFunction.SimpleFunction {
        private static final double[] VALUES = {
            -7.677241203873919E-8D, -1.14500735309074E-14D,
            -60496942336.0D, -1.2769807739010375E-8D,
            3.921566832786529E-16D, 0.03948966832831502D,
            0.031198229451547377D, -158744123392.0D
        };

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            int x = context.blockX() == 4 ? 1 : 0;
            int y = context.blockY() == 8 ? 1 : 0;
            int z = context.blockZ() == 4 ? 1 : 0;
            return VALUES[z * 4 + y * 2 + x];
        }

        @Override
        public double minValue() {
            return -158744123392.0D;
        }

        @Override
        public double maxValue() {
            return 0.03948966832831502D;
        }
    }
}

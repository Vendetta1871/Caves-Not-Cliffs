package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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

    private static double expected(DensityFunction function, int x0, int y0, int z0,
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
}

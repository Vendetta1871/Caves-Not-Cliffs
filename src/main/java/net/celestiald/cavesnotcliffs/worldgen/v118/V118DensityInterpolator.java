package net.celestiald.cavesnotcliffs.worldgen.v118;

/**
 * Realizes 1.18.2 density-cache markers for deterministic random-access column sampling.
 *
 * <p>The production server normally evaluates these nodes through NoiseChunk's mutable cell
 * loop. Complete columns are this backport's cache unit, so the equivalent trilinear operation is
 * exposed as an immutable function of block coordinates.</p>
 */
public final class V118DensityInterpolator {
    private V118DensityInterpolator() {
    }

    public static DensityFunction realize(DensityFunction function,
            V118NoiseSettings settings) {
        if (function == null) {
            throw new NullPointerException("function");
        }
        if (settings == null) {
            throw new NullPointerException("settings");
        }
        return function.mapAll(new MarkerRealizer(settings.getCellWidth(),
            settings.getCellHeight()));
    }

    private static final class MarkerRealizer implements DensityFunction.Visitor {
        private final int cellWidth;
        private final int cellHeight;

        private MarkerRealizer(int cellWidth, int cellHeight) {
            this.cellWidth = cellWidth;
            this.cellHeight = cellHeight;
        }

        @Override
        public DensityFunction apply(DensityFunction function) {
            if (!(function instanceof DensityFunctions.Marker)) {
                return function;
            }
            DensityFunctions.Marker marker = (DensityFunctions.Marker) function;
            switch (marker.type()) {
                case INTERPOLATED:
                    return new Interpolated(marker.wrapped(), cellWidth, cellHeight);
                case FLAT_CACHE:
                    return new FlatCache(marker.wrapped());
                case CACHE_2D:
                case CACHE_ONCE:
                case CACHE_ALL_IN_CELL:
                    return marker.wrapped();
                default:
                    throw new AssertionError(marker.type());
            }
        }
    }

    private static final class FlatCache implements DensityFunction.SimpleFunction {
        private final DensityFunction wrapped;

        private FlatCache(DensityFunction wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            int quartX = Math.floorDiv(context.blockX(), 4) * 4;
            int quartZ = Math.floorDiv(context.blockZ(), 4) * 4;
            return wrapped.compute(quartX, 0, quartZ);
        }

        @Override
        public double minValue() {
            return wrapped.minValue();
        }

        @Override
        public double maxValue() {
            return wrapped.maxValue();
        }
    }

    private static final class Interpolated implements DensityFunction.SimpleFunction {
        private final DensityFunction wrapped;
        private final int cellWidth;
        private final int cellHeight;

        private Interpolated(DensityFunction wrapped, int cellWidth, int cellHeight) {
            this.wrapped = wrapped;
            this.cellWidth = cellWidth;
            this.cellHeight = cellHeight;
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            int x0 = Math.floorDiv(context.blockX(), cellWidth) * cellWidth;
            int y0 = Math.floorDiv(context.blockY(), cellHeight) * cellHeight;
            int z0 = Math.floorDiv(context.blockZ(), cellWidth) * cellWidth;
            int x1 = x0 + cellWidth;
            int y1 = y0 + cellHeight;
            int z1 = z0 + cellWidth;
            double deltaX = Math.floorMod(context.blockX(), cellWidth) / (double) cellWidth;
            double deltaY = Math.floorMod(context.blockY(), cellHeight) / (double) cellHeight;
            double deltaZ = Math.floorMod(context.blockZ(), cellWidth) / (double) cellWidth;
            return WorldgenMath.lerp3(deltaX, deltaY, deltaZ,
                wrapped.compute(x0, y0, z0), wrapped.compute(x1, y0, z0),
                wrapped.compute(x0, y1, z0), wrapped.compute(x1, y1, z0),
                wrapped.compute(x0, y0, z1), wrapped.compute(x1, y0, z1),
                wrapped.compute(x0, y1, z1), wrapped.compute(x1, y1, z1));
        }

        @Override
        public double minValue() {
            return wrapped.minValue();
        }

        @Override
        public double maxValue() {
            return wrapped.maxValue();
        }
    }
}

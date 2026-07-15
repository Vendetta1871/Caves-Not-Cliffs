package net.celestiald.cavesnotcliffs.worldgen.v118;

/**
 * Realizes 1.18.2 density-cache markers for deterministic random-access column sampling.
 *
 * <p>The production server normally evaluates these nodes through NoiseChunk's mutable cell
 * loop. Complete columns are this backport's cache unit, so the equivalent trilinear operation is
 * exposed as an immutable function of block coordinates.</p>
 */
public final class V118DensityInterpolator {
    private static final int CACHE_2D_SIZE = 64;
    private static final int CACHE_2D_MASK = CACHE_2D_SIZE - 1;

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

    /**
     * Realizes the final terrain density in the same cache-all-in-cell context used by
     * {@code NoiseChunk}'s block-state rule.
     */
    public static DensityFunction realizeFinalDensity(DensityFunction function,
            V118NoiseSettings settings) {
        return realize(DensityFunctions.cacheAllInCell(function), settings);
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
                    return new Cache2D(marker.wrapped());
                case CACHE_ONCE:
                    return marker.wrapped();
                case CACHE_ALL_IN_CELL:
                    return new CellCache(marker.wrapped());
                default:
                    throw new AssertionError(marker.type());
            }
        }
    }

    /** Bounded primitive cache for functions declared independent of Y. */
    private static final class Cache2D implements DensityFunction.SimpleFunction {
        private final DensityFunction wrapped;
        private final long[] keys = new long[CACHE_2D_SIZE];
        private final double[] values = new double[CACHE_2D_SIZE];
        private final boolean[] occupied = new boolean[CACHE_2D_SIZE];

        private Cache2D(DensityFunction wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            int blockX = context.blockX();
            int blockZ = context.blockZ();
            long key = ((long) blockX << 32) ^ (blockZ & 0xFFFFFFFFL);
            int index = cacheIndex(key);
            if (occupied[index] && keys[index] == key) {
                return values[index];
            }
            double value = wrapped.compute(context);
            occupied[index] = true;
            keys[index] = key;
            values[index] = value;
            return value;
        }

        @Override
        public double minValue() {
            return wrapped.minValue();
        }

        @Override
        public double maxValue() {
            return wrapped.maxValue();
        }

        private static int cacheIndex(long key) {
            key ^= key >>> 33;
            key *= 0xff51afd7ed558ccdL;
            key ^= key >>> 33;
            return (int) key & CACHE_2D_MASK;
        }
    }

    /** Marks descendant interpolators as executing during NoiseChunk's cell-cache fill. */
    private static final class CellCache implements DensityFunction.SimpleFunction {
        private final DensityFunction wrapped;
        private final CellContext cellContext = new CellContext();

        private CellCache(DensityFunction wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return wrapped.compute(cellContext.set(context, context.blockX(), context.blockY(),
                context.blockZ()));
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

    private static final class CellContext implements DensityFunction.FunctionContext {
        private DensityFunction.FunctionContext delegate;
        private int blockX;
        private int blockY;
        private int blockZ;

        private CellContext set(DensityFunction.FunctionContext delegate, int blockX, int blockY,
                int blockZ) {
            this.delegate = delegate;
            this.blockX = blockX;
            this.blockY = blockY;
            this.blockZ = blockZ;
            return this;
        }

        @Override
        public int blockX() {
            return blockX;
        }

        @Override
        public int blockY() {
            return blockY;
        }

        @Override
        public int blockZ() {
            return blockZ;
        }

        @Override
        public double blendDensity(double density) {
            return delegate.blendDensity(density);
        }
    }

    private static final class FlatCache implements DensityFunction.SimpleFunction {
        private final DensityFunction wrapped;
        private int lastQuartX = Integer.MIN_VALUE;
        private int lastQuartZ = Integer.MIN_VALUE;
        private double lastValue;

        private FlatCache(DensityFunction wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            int quartX = Math.floorDiv(context.blockX(), 4) * 4;
            int quartZ = Math.floorDiv(context.blockZ(), 4) * 4;
            if (quartX != lastQuartX || quartZ != lastQuartZ) {
                lastQuartX = quartX;
                lastQuartZ = quartZ;
                lastValue = wrapped.compute(quartX, 0, quartZ);
            }
            return lastValue;
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
        private int lastCellX = Integer.MIN_VALUE;
        private int lastCellY = Integer.MIN_VALUE;
        private int lastCellZ = Integer.MIN_VALUE;
        private boolean lastCellCacheContext;
        private final double[] corners = new double[8];
        private final CellContext sampleContext = new CellContext();

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
            boolean cellCacheContext = context instanceof CellContext;
            ensureCell(context, x0, y0, z0, cellCacheContext);
            double deltaX = Math.floorMod(context.blockX(), cellWidth) / (double) cellWidth;
            double deltaY = Math.floorMod(context.blockY(), cellHeight) / (double) cellHeight;
            double deltaZ = Math.floorMod(context.blockZ(), cellWidth) / (double) cellWidth;
            if (cellCacheContext) {
                return WorldgenMath.lerp3(deltaX, deltaY, deltaZ,
                    corners[0], corners[1], corners[2], corners[3],
                    corners[4], corners[5], corners[6], corners[7]);
            }

            // Outside a cache-all fill, NoiseChunk updates each interpolator Y, then X, then Z.
            // This differs from Mth.lerp3 by one ULP for some inputs.
            double valueXZ00 = WorldgenMath.lerp(deltaY, corners[0], corners[2]);
            double valueXZ10 = WorldgenMath.lerp(deltaY, corners[1], corners[3]);
            double valueXZ01 = WorldgenMath.lerp(deltaY, corners[4], corners[6]);
            double valueXZ11 = WorldgenMath.lerp(deltaY, corners[5], corners[7]);
            double valueZ0 = WorldgenMath.lerp(deltaX, valueXZ00, valueXZ10);
            double valueZ1 = WorldgenMath.lerp(deltaX, valueXZ01, valueXZ11);
            return WorldgenMath.lerp(deltaZ, valueZ0, valueZ1);
        }

        private void ensureCell(DensityFunction.FunctionContext context, int x0, int y0,
                int z0, boolean cellCacheContext) {
            if (x0 == lastCellX && y0 == lastCellY && z0 == lastCellZ
                    && cellCacheContext == lastCellCacheContext) {
                return;
            }
            lastCellX = x0;
            lastCellY = y0;
            lastCellZ = z0;
            lastCellCacheContext = cellCacheContext;
            int x1 = x0 + cellWidth;
            int y1 = y0 + cellHeight;
            int z1 = z0 + cellWidth;
            corners[0] = sample(context, x0, y0, z0);
            corners[1] = sample(context, x1, y0, z0);
            corners[2] = sample(context, x0, y1, z0);
            corners[3] = sample(context, x1, y1, z0);
            corners[4] = sample(context, x0, y0, z1);
            corners[5] = sample(context, x1, y0, z1);
            corners[6] = sample(context, x0, y1, z1);
            corners[7] = sample(context, x1, y1, z1);
        }

        private double sample(DensityFunction.FunctionContext context, int x, int y, int z) {
            if (context instanceof CellContext) {
                return wrapped.compute(sampleContext.set(context, x, y, z));
            }
            return wrapped.compute(x, y, z);
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

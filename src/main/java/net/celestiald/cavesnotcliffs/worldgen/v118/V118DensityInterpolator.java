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
        return realize(function, settings, null);
    }

    /**
     * Realizes a function whose interpolated nodes read their cell corners from a column's
     * shared corner grids once {@link ColumnCorners#markReady} has run, and register with
     * {@code lane} so it can fill its share of those grids.
     */
    public static DensityFunction realize(DensityFunction function,
            V118NoiseSettings settings, ColumnCorners.Lane lane) {
        if (function == null) {
            throw new NullPointerException("function");
        }
        if (settings == null) {
            throw new NullPointerException("settings");
        }
        return function.mapAll(new MarkerRealizer(settings.getCellWidth(),
            settings.getCellHeight(), lane));
    }

    /**
     * Realizes the final terrain density in the same cache-all-in-cell context used by
     * {@code NoiseChunk}'s block-state rule.
     */
    public static DensityFunction realizeFinalDensity(DensityFunction function,
            V118NoiseSettings settings) {
        return realizeFinalDensity(function, settings, null);
    }

    public static DensityFunction realizeFinalDensity(DensityFunction function,
            V118NoiseSettings settings, ColumnCorners.Lane lane) {
        return realize(DensityFunctions.cacheAllInCell(function), settings, lane);
    }

    /**
     * Every interpolated node's corner values over one column's cell grid, computed in bulk
     * before the cells are filled, like NoiseChunk's slice caches. Filling a cell then reads
     * its eight corners instead of evaluating them: adjacent cells share corners, so the
     * column evaluates each interpolated node at its 5x49x5 corners rather than 8 per cell.
     *
     * <p>Grids are keyed by the interpolated marker's source identity, so every lane's
     * realization of a node shares one grid. Each lane fills a disjoint set of corner columns
     * with its own realized nodes (their memo caches are single-threaded); density functions
     * are pure in their coordinates, so values do not depend on which lane computed them or
     * in which order. Lanes must finish {@link Lane#fill} and the caller must
     * {@link #markReady} before any lane reads, e.g. across a fork/join.</p>
     */
    public static final class ColumnCorners {
        private final int minX;
        private final int minY;
        private final int minZ;
        private final int cellWidth;
        private final int cellHeight;
        private final int countX;
        private final int countY;
        private final int countZ;
        private final java.util.IdentityHashMap<DensityFunctions.Marker, double[]> grids =
            new java.util.IdentityHashMap<DensityFunctions.Marker, double[]>();
        private boolean ready;

        public ColumnCorners(V118NoiseSettings settings, int minBlockX, int minBlockZ,
                int width) {
            cellWidth = settings.getCellWidth();
            cellHeight = settings.getCellHeight();
            minX = minBlockX;
            minY = settings.minY();
            minZ = minBlockZ;
            countX = width / cellWidth + 1;
            countY = settings.getCellCountY() + 1;
            countZ = width / cellWidth + 1;
        }

        /** Number of (x, z) corner columns, the unit of work {@link Lane#fill} splits. */
        public int cornerColumns() {
            return countX * countZ;
        }

        /** New per-lane registry; create lanes and realize their functions serially. */
        public Lane newLane() {
            return new Lane();
        }

        /** Publishes the filled grids to the interpolated nodes of every lane. */
        public void markReady() {
            ready = true;
        }

        private double[] grid(DensityFunctions.Marker source) {
            double[] grid = grids.get(source);
            if (grid == null) {
                grid = new double[countX * countY * countZ];
                grids.put(source, grid);
            }
            return grid;
        }

        /** Index of the cell whose lowest corner is at (x0, y0, z0), or -1 outside the grid. */
        private int cellIndex(int x0, int y0, int z0) {
            if (!ready) {
                return -1;
            }
            int cellX = (x0 - minX) / cellWidth;
            int cellY = (y0 - minY) / cellHeight;
            int cellZ = (z0 - minZ) / cellWidth;
            if (x0 < minX || y0 < minY || z0 < minZ || cellX + 1 >= countX
                    || cellY + 1 >= countY || cellZ + 1 >= countZ) {
                return -1;
            }
            return (cellX * countZ + cellZ) * countY + cellY;
        }

        /** One fill lane's interpolated nodes, one per marker source. */
        public final class Lane {
            private final java.util.IdentityHashMap<DensityFunctions.Marker, Interpolated>
                nodes = new java.util.IdentityHashMap<DensityFunctions.Marker, Interpolated>();

            private Lane() {
            }

            private ColumnCorners owner() {
                return ColumnCorners.this;
            }

            private double[] register(DensityFunctions.Marker source, Interpolated node) {
                if (!nodes.containsKey(source)) {
                    nodes.put(source, node);
                }
                return grid(source);
            }

            /** Fills corner columns {@code from, from + step, ...} of every grid. */
            public void fill(int from, int step) {
                MutableDensityContext delegate = new MutableDensityContext();
                for (java.util.Map.Entry<DensityFunctions.Marker, Interpolated> entry
                        : nodes.entrySet()) {
                    double[] grid = grids.get(entry.getKey());
                    Interpolated node = entry.getValue();
                    for (int column = from; column < countX * countZ; column += step) {
                        int cornerX = column / countZ;
                        int cornerZ = column % countZ;
                        int blockX = minX + cornerX * cellWidth;
                        int blockZ = minZ + cornerZ * cellWidth;
                        int base = (cornerX * countZ + cornerZ) * countY;
                        for (int cornerY = 0; cornerY < countY; ++cornerY) {
                            grid[base + cornerY] = node.sampleCorner(delegate,
                                blockX, minY + cornerY * cellHeight, blockZ);
                        }
                    }
                }
            }
        }
    }

    private static final class MarkerRealizer implements DensityFunction.Visitor {
        private final int cellWidth;
        private final int cellHeight;
        // mapAll visits every reference to a shared sub-graph, so a cache-once marker reached
        // through two parents (the lerp delta, the spaghetti roughness feeding both spaghetti2D
        // and entrances) would otherwise be wrapped twice and keep recomputing. Keying the
        // wrapper by marker identity preserves the router's sharing, matching NoiseChunk, where
        // each marker node exists once.
        private final java.util.IdentityHashMap<DensityFunction, DensityFunction> cacheOnceNodes =
            new java.util.IdentityHashMap<DensityFunction, DensityFunction>();

        private final ColumnCorners.Lane lane;

        private MarkerRealizer(int cellWidth, int cellHeight, ColumnCorners.Lane lane) {
            this.cellWidth = cellWidth;
            this.cellHeight = cellHeight;
            this.lane = lane;
        }

        @Override
        public DensityFunction apply(DensityFunction function) {
            if (!(function instanceof DensityFunctions.Marker)) {
                return function;
            }
            DensityFunctions.Marker marker = (DensityFunctions.Marker) function;
            switch (marker.type()) {
                case INTERPOLATED: {
                    Interpolated interpolated = new Interpolated(marker.wrapped(), cellWidth,
                        cellHeight);
                    if (lane != null) {
                        interpolated.columnCorners = lane.owner();
                        interpolated.grid = lane.register(marker.source(), interpolated);
                    }
                    return interpolated;
                }
                case FLAT_CACHE:
                    return new FlatCache(marker.wrapped());
                case CACHE_2D:
                    return new Cache2D(marker.wrapped());
                case CACHE_ONCE: {
                    DensityFunction realized = cacheOnceNodes.get(marker.source());
                    if (realized == null) {
                        realized = new CacheOnce(marker.wrapped());
                        cacheOnceNodes.put(marker.source(), realized);
                    }
                    return realized;
                }
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

    /**
     * Single-entry memo of the last sampled position, the deterministic equivalent of
     * NoiseChunk's per-cell cache-once epoch. Density functions are pure in their coordinates,
     * so repeating a sample at the same point — the doubled lerp delta and the sub-trees shared
     * by spaghetti2D/entrances/pillars — returns the identical value without recomputing it.
     */
    private static final class CacheOnce implements DensityFunction.SimpleFunction {
        private final DensityFunction wrapped;
        private int lastX = Integer.MIN_VALUE;
        private int lastY = Integer.MIN_VALUE;
        private int lastZ = Integer.MIN_VALUE;
        private double lastValue;

        private CacheOnce(DensityFunction wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            int blockX = context.blockX();
            int blockY = context.blockY();
            int blockZ = context.blockZ();
            if (blockX != lastX || blockY != lastY || blockZ != lastZ) {
                lastX = blockX;
                lastY = blockY;
                lastZ = blockZ;
                lastValue = wrapped.compute(context);
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
        // Set at realization when this node takes part in a column's bulk corner fill.
        private ColumnCorners columnCorners;
        private double[] grid;

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
            int cell = columnCorners == null ? -1 : columnCorners.cellIndex(x0, y0, z0);
            if (cell >= 0) {
                // Same order as the computed branch below: x fastest, then y, then z.
                int strideY = 1;
                int strideZ = columnCorners.countY;
                int strideX = columnCorners.countZ * columnCorners.countY;
                corners[0] = grid[cell];
                corners[1] = grid[cell + strideX];
                corners[2] = grid[cell + strideY];
                corners[3] = grid[cell + strideX + strideY];
                corners[4] = grid[cell + strideZ];
                corners[5] = grid[cell + strideX + strideZ];
                corners[6] = grid[cell + strideY + strideZ];
                corners[7] = grid[cell + strideX + strideY + strideZ];
                return;
            }
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

        /**
         * A corner value for the bulk fill. Equal to {@link #sample} in either context: the
         * cell-cache context only forwards blendDensity, which is the identity here.
         */
        private double sampleCorner(DensityFunction.FunctionContext delegate, int x, int y,
                int z) {
            return wrapped.compute(sampleContext.set(delegate, x, y, z));
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

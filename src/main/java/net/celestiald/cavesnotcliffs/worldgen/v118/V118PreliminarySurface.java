package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.HashMap;
import java.util.Map;

/** Exact 1.18.2 preliminary-surface scan used by aquifer fluid-level decisions. */
public final class V118PreliminarySurface implements NoiseBasedAquifer.PreliminarySurfaceLookup {
    private static final double DENSITY_OFFSET = -0.703125D;
    private static final double SURFACE_THRESHOLD = 0.390625D;
    // Quart-space reach of the aquifer's surface sampling around one 16-wide column: the fluid
    // centers ring the column by one 16-block grid cell on every side, then
    // NoiseBasedAquifer.SURFACE_SAMPLING_OFFSETS_IN_CHUNKS extends three chunks west and one
    // chunk east/north/south, and the in-chunk surface rule lerps up to one chunk east/north.
    private static final int MIN_QUART_HALO_X = -16;
    private static final int MAX_QUART_HALO_X = 10;
    private static final int MIN_QUART_HALO_Z = -8;
    private static final int MAX_QUART_HALO_Z = 10;

    private final V118NoiseSettings settings;
    private final DensityFunction initialDensityWithoutJaggedness;
    private final Map<Long, Integer> cache = new HashMap<Long, Integer>();
    private final MutableDensityContext sampleContext = new MutableDensityContext();

    public V118PreliminarySurface(V118NoiseSettings settings,
            DensityFunction initialDensityWithoutJaggedness) {
        if (settings == null) {
            throw new NullPointerException("settings");
        }
        if (initialDensityWithoutJaggedness == null) {
            throw new NullPointerException("initialDensityWithoutJaggedness");
        }
        this.settings = settings;
        this.initialDensityWithoutJaggedness = V118DensityInterpolator.realize(
            initialDensityWithoutJaggedness, settings);
    }

    /**
     * Eagerly scans every quart column the column pipeline can query and returns the immutable
     * table. Building it once up front lets every parallel cell-fill lane read the same values
     * instead of each scanning its own copy behind a private cache.
     */
    static NoiseBasedAquifer.PreliminarySurfaceLookup forColumn(V118NoiseSettings settings,
            DensityFunction realizedInitialDensityWithoutJaggedness, int columnX, int columnZ) {
        if (settings == null || realizedInitialDensityWithoutJaggedness == null) {
            throw new NullPointerException("settings and density are required");
        }
        int minQuartX = columnX * TerrainColumn.QUART_WIDTH + MIN_QUART_HALO_X;
        int minQuartZ = columnZ * TerrainColumn.QUART_WIDTH + MIN_QUART_HALO_Z;
        int quartCountX = MAX_QUART_HALO_X - MIN_QUART_HALO_X + 1;
        int quartCountZ = MAX_QUART_HALO_Z - MIN_QUART_HALO_Z + 1;
        int[] levels = new int[quartCountX * quartCountZ];
        MutableDensityContext context = new MutableDensityContext();
        int index = 0;
        for (int quartZ = minQuartZ; quartZ < minQuartZ + quartCountZ; ++quartZ) {
            for (int quartX = minQuartX; quartX < minQuartX + quartCountX; ++quartX) {
                levels[index++] = computeLevel(settings,
                    realizedInitialDensityWithoutJaggedness, context, quartX * 4, quartZ * 4);
            }
        }
        return new ColumnTable(minQuartX, minQuartZ, quartCountX, quartCountZ, levels);
    }

    @Override
    public int preliminarySurfaceLevel(int blockX, int blockZ) {
        long key = ((long) blockX << 32) ^ (blockZ & 0xFFFFFFFFL);
        Integer cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        int computed = computeLevel(settings, initialDensityWithoutJaggedness, sampleContext,
            blockX, blockZ);
        cache.put(key, computed);
        return computed;
    }

    private static int computeLevel(V118NoiseSettings settings, DensityFunction density,
            MutableDensityContext sampleContext, int blockX, int blockZ) {
        int minCellY = settings.getMinCellY();
        int maxCellY = minCellY + settings.getCellCountY();
        for (int cellY = maxCellY; cellY >= minCellY; --cellY) {
            int blockY = cellY * settings.getCellHeight();
            double value = density.compute(sampleContext.set(blockX, blockY, blockZ))
                + DENSITY_OFFSET;
            value = WorldgenMath.clamp(value, -64.0D, 64.0D);
            value = settings.applySlide(value, blockY);
            if (value > SURFACE_THRESHOLD) {
                return blockY;
            }
        }
        return Integer.MAX_VALUE;
    }

    int cachedPositions() {
        return cache.size();
    }

    /** Immutable quart-resolution view of one column's preliminary surface halo. */
    private static final class ColumnTable implements NoiseBasedAquifer.PreliminarySurfaceLookup {
        private final int minQuartX;
        private final int minQuartZ;
        private final int quartCountX;
        private final int quartCountZ;
        private final int[] levels;

        private ColumnTable(int minQuartX, int minQuartZ, int quartCountX, int quartCountZ,
                int[] levels) {
            this.minQuartX = minQuartX;
            this.minQuartZ = minQuartZ;
            this.quartCountX = quartCountX;
            this.quartCountZ = quartCountZ;
            this.levels = levels;
        }

        @Override
        public int preliminarySurfaceLevel(int blockX, int blockZ) {
            // NoiseChunk keys this cache in quart coordinates and evaluates at the quart origin.
            int localX = Math.floorDiv(blockX, 4) - minQuartX;
            int localZ = Math.floorDiv(blockZ, 4) - minQuartZ;
            if (localX < 0 || localX >= quartCountX || localZ < 0 || localZ >= quartCountZ) {
                throw new IllegalArgumentException(
                    "Preliminary surface query outside column table: " + blockX + "," + blockZ);
            }
            return levels[localZ * quartCountX + localX];
        }
    }
}

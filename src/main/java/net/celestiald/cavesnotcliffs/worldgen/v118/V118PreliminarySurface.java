package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.HashMap;
import java.util.Map;

/** Exact 1.18.2 preliminary-surface scan used by aquifer fluid-level decisions. */
public final class V118PreliminarySurface implements NoiseBasedAquifer.PreliminarySurfaceLookup {
    private static final double DENSITY_OFFSET = -0.703125D;
    private static final double SURFACE_THRESHOLD = 0.390625D;

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

    @Override
    public int preliminarySurfaceLevel(int blockX, int blockZ) {
        long key = ((long) blockX << 32) ^ (blockZ & 0xFFFFFFFFL);
        Integer cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        int computed = compute(blockX, blockZ);
        cache.put(key, computed);
        return computed;
    }

    private int compute(int blockX, int blockZ) {
        int minCellY = settings.getMinCellY();
        int maxCellY = minCellY + settings.getCellCountY();
        for (int cellY = maxCellY; cellY >= minCellY; --cellY) {
            int blockY = cellY * settings.getCellHeight();
            double density = initialDensityWithoutJaggedness.compute(
                sampleContext.set(blockX, blockY, blockZ))
                + DENSITY_OFFSET;
            density = WorldgenMath.clamp(density, -64.0D, 64.0D);
            density = settings.applySlide(density, blockY);
            if (density > SURFACE_THRESHOLD) {
                return blockY;
            }
        }
        return Integer.MAX_VALUE;
    }

    int cachedPositions() {
        return cache.size();
    }
}

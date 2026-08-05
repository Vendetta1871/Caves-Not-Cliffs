package net.celestiald.cavesnotcliffs.worldgen.v118;

/** Seeded six-axis climate sampler and virtual 3D Overworld biome resolver. */
public final class V118ClimateSampler {
    private static final int QUART_CACHE_SIZE = 2048;
    private static final int QUART_CACHE_MASK = QUART_CACHE_SIZE - 1;

    private final DensityFunction temperature;
    private final DensityFunction humidity;
    private final DensityFunction continentalness;
    private final DensityFunction erosion;
    private final DensityFunction depth;
    private final DensityFunction weirdness;
    private final OverworldBiomeBuilder biomeTable;
    /*
     * A sampler belongs to one generator or one provider. Its realized density functions already
     * contain mutable cell caches, so this cache intentionally has the same lifetime and thread
     * confinement. It avoids recalculating the six climate axes when Voronoi-neighbouring block
     * queries select a quart corner that was resolved recently.
     */
    private final int[] cachedQuartX = new int[QUART_CACHE_SIZE];
    private final int[] cachedQuartY = new int[QUART_CACHE_SIZE];
    private final int[] cachedQuartZ = new int[QUART_CACHE_SIZE];
    private final V118Biome[] cachedBiomes = new V118Biome[QUART_CACHE_SIZE];

    public V118ClimateSampler(V118NoiseRouter router, V118NoiseSettings settings,
            OverworldBiomeBuilder biomeTable) {
        if (router == null) {
            throw new NullPointerException("router");
        }
        if (settings == null) {
            throw new NullPointerException("settings");
        }
        if (biomeTable == null) {
            throw new NullPointerException("biomeTable");
        }
        temperature = V118DensityInterpolator.realize(router.temperature(), settings);
        humidity = V118DensityInterpolator.realize(router.humidity(), settings);
        continentalness = V118DensityInterpolator.realize(router.continents(), settings);
        erosion = V118DensityInterpolator.realize(router.erosion(), settings);
        depth = V118DensityInterpolator.realize(router.depth(), settings);
        weirdness = V118DensityInterpolator.realize(router.ridges(), settings);
        this.biomeTable = biomeTable;
    }

    /** Matches Climate.Sampler: input coordinates are quart positions, not block positions. */
    public Climate.TargetPoint sampleQuart(int quartX, int quartY, int quartZ) {
        int blockX = quartX * 4;
        int blockY = quartY * 4;
        int blockZ = quartZ * 4;
        DensityFunction.SinglePointContext context =
            new DensityFunction.SinglePointContext(blockX, blockY, blockZ);
        return Climate.target((float) temperature.compute(context),
            (float) humidity.compute(context), (float) continentalness.compute(context),
            (float) erosion.compute(context), (float) depth.compute(context),
            (float) weirdness.compute(context));
    }

    public V118Biome resolveQuart(int quartX, int quartY, int quartZ) {
        int cacheIndex = cacheIndex(quartX, quartY, quartZ);
        V118Biome cached = cachedBiomes[cacheIndex];
        if (cached != null && cachedQuartX[cacheIndex] == quartX
                && cachedQuartY[cacheIndex] == quartY && cachedQuartZ[cacheIndex] == quartZ) {
            return cached;
        }
        V118Biome resolved = biomeTable.resolve(sampleQuart(quartX, quartY, quartZ));
        cachedQuartX[cacheIndex] = quartX;
        cachedQuartY[cacheIndex] = quartY;
        cachedQuartZ[cacheIndex] = quartZ;
        cachedBiomes[cacheIndex] = resolved;
        return resolved;
    }

    /** Resolves the same quart cell Minecraft uses for the supplied block coordinate. */
    public V118Biome resolveBlock(int blockX, int blockY, int blockZ) {
        return resolveQuart(Math.floorDiv(blockX, 4), Math.floorDiv(blockY, 4),
            Math.floorDiv(blockZ, 4));
    }

    private static int cacheIndex(int quartX, int quartY, int quartZ) {
        int hash = quartX * 0x9E3779B9;
        hash ^= Integer.rotateLeft(quartY, 11);
        hash *= 0x85EBCA6B;
        hash ^= Integer.rotateLeft(quartZ, 17);
        hash ^= hash >>> 16;
        return hash & QUART_CACHE_MASK;
    }
}

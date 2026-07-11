package net.celestiald.cavesnotcliffs.worldgen.v118;

/** Seeded six-axis climate sampler and virtual 3D Overworld biome resolver. */
public final class V118ClimateSampler {
    private final DensityFunction temperature;
    private final DensityFunction humidity;
    private final DensityFunction continentalness;
    private final DensityFunction erosion;
    private final DensityFunction depth;
    private final DensityFunction weirdness;
    private final OverworldBiomeBuilder biomeTable;

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
        return biomeTable.resolve(sampleQuart(quartX, quartY, quartZ));
    }

    /** Resolves the same quart cell Minecraft uses for the supplied block coordinate. */
    public V118Biome resolveBlock(int blockX, int blockY, int blockZ) {
        return resolveQuart(Math.floorDiv(blockX, 4), Math.floorDiv(blockY, 4),
            Math.floorDiv(blockZ, 4));
    }
}

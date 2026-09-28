package net.celestiald.cavesnotcliffs.world;

import net.minecraft.world.gen.NoiseGeneratorSimplex;

import java.util.Random;

/**
 * Splits the world into large regions with organic borders; each region rolls, per 1.18 host
 * biome, whether that host shows a modded biome and which one.
 *
 * <p>Regions are jittered Voronoi cells whose sample point is displaced by two octaves of
 * simplex noise, so borders wander instead of forming polygons. A modded biome therefore fills
 * the part of a host biome that lies inside one region, and its outline follows the 1.18
 * climate borders wherever the host ends first. Everything is a pure function of the seed and
 * position, so neighbouring chunks, the chunk biome array and biome queries always agree.</p>
 */
final class ModdedBiomeRegions {
    private static final long SALT = 0x6D6F646465645267L;
    private static final double WARP_AMPLITUDE = 0.35D;
    private static final double DETAIL_AMPLITUDE = 0.08D;

    private final long seed;
    private final double cellSize;
    private final NoiseGeneratorSimplex warpX;
    private final NoiseGeneratorSimplex warpZ;
    private final NoiseGeneratorSimplex detailX;
    private final NoiseGeneratorSimplex detailZ;

    ModdedBiomeRegions(long worldSeed, int cellSize) {
        if (cellSize < 16) {
            throw new IllegalArgumentException("Region size must be at least 16 blocks: "
                    + cellSize);
        }
        seed = mix(worldSeed ^ SALT);
        this.cellSize = cellSize;
        Random random = new Random(seed);
        warpX = new NoiseGeneratorSimplex(random);
        warpZ = new NoiseGeneratorSimplex(random);
        detailX = new NoiseGeneratorSimplex(random);
        detailZ = new NoiseGeneratorSimplex(random);
    }

    int cellSize() {
        return (int) cellSize;
    }

    /** Stable identifier of the region containing the block column. */
    long regionAt(int blockX, int blockZ) {
        double coarse = cellSize;
        double fine = cellSize / 5.0D;
        double x = blockX + WARP_AMPLITUDE * cellSize * warpX.getValue(blockX / coarse,
                blockZ / coarse) + DETAIL_AMPLITUDE * cellSize * detailX.getValue(blockX / fine,
                blockZ / fine);
        double z = blockZ + WARP_AMPLITUDE * cellSize * warpZ.getValue(blockX / coarse,
                blockZ / coarse) + DETAIL_AMPLITUDE * cellSize * detailZ.getValue(blockX / fine,
                blockZ / fine);
        long cellX = (long) Math.floor(x / cellSize);
        long cellZ = (long) Math.floor(z / cellSize);
        long bestX = cellX;
        long bestZ = cellZ;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (long neighbourZ = cellZ - 1; neighbourZ <= cellZ + 1; ++neighbourZ) {
            for (long neighbourX = cellX - 1; neighbourX <= cellX + 1; ++neighbourX) {
                long hash = cellHash(neighbourX, neighbourZ);
                double pointX = (neighbourX + 0.15D + 0.7D * unit(hash)) * cellSize;
                double pointZ = (neighbourZ + 0.15D + 0.7D * unit(mix(hash))) * cellSize;
                double dx = pointX - x;
                double dz = pointZ - z;
                double distance = dx * dx + dz * dz;
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestX = neighbourX;
                    bestZ = neighbourZ;
                }
            }
        }
        return cellHash(bestX, bestZ);
    }

    /** The region's uniform roll in [0, 1) for one host biome. */
    static double roll(long region, int host) {
        return unit(mix(region + 0x9E3779B97F4A7C15L * (host + 1)));
    }

    private long cellHash(long cellX, long cellZ) {
        return mix(seed ^ mix(cellX * 0x632BE59BD9B4E019L + cellZ * 0x85157AF5L));
    }

    private static double unit(long hash) {
        return (hash >>> 11) * 0x1.0p-53;
    }

    /** SplitMix64 finalizer. */
    static long mix(long value) {
        long mixed = value + 0x9E3779B97F4A7C15L;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        return mixed ^ (mixed >>> 31);
    }
}

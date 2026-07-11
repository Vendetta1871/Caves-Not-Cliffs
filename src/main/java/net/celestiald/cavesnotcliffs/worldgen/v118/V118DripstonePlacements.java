package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118DripstoneFeature.WorldAccess;

/** Exact Java 1.18.2 placed-feature wrappers for the three dripstone cave features. */
public final class V118DripstonePlacements {
    public static final int LOCAL_MODIFICATIONS_STEP = 2;
    public static final int UNDERGROUND_DECORATION_STEP = 7;
    public static final int LARGE_DRIPSTONE_INDEX = 3;
    public static final int DRIPSTONE_CLUSTER_INDEX = 0;
    public static final int POINTED_DRIPSTONE_INDEX = 1;
    public static final int TERRAIN_MAXIMUM_Y = 256;
    public static final int LARGE_COUNT_MINIMUM = 10;
    public static final int LARGE_COUNT_MAXIMUM = 48;
    public static final int CLUSTER_COUNT_MINIMUM = 48;
    public static final int CLUSTER_COUNT_MAXIMUM = 96;
    public static final int POINTED_COUNT_MINIMUM = 192;
    public static final int POINTED_COUNT_MAXIMUM = 256;
    public static final int POINTED_REPEATS_MINIMUM = 1;
    public static final int POINTED_REPEATS_MAXIMUM = 5;

    private V118DripstonePlacements() {
    }

    public static PlacementResult decorateLarge(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, Set<V118Biome> regionBiomes) {
        if (world == null || regionBiomes == null) {
            throw new NullPointerException("world and regionBiomes are required");
        }
        V118WorldgenRandom random = new V118WorldgenRandom(0L);
        long decorationSeed = random.setDecorationSeed(worldSeed, chunkX << 4, chunkZ << 4);
        if (!regionBiomes.contains(V118Biome.DRIPSTONE_CAVES)) {
            return new PlacementResult(decorationSeed, 0, 0);
        }
        random.setFeatureSeed(decorationSeed, LARGE_DRIPSTONE_INDEX,
            LOCAL_MODIFICATIONS_STEP);
        int attempts = V118DripstoneFeature.uniformInt(random,
            LARGE_COUNT_MINIMUM, LARGE_COUNT_MAXIMUM);
        int placed = placeSimple(world, random, chunkX, chunkZ, attempts,
            new ConfiguredFeature() {
                @Override
                public boolean place(WorldAccess target, Random source, int x, int y, int z) {
                    return V118LargeDripstoneFeature.place(target, source, x, y, z);
                }
            });
        return new PlacementResult(decorationSeed, attempts, placed);
    }

    public static UndergroundResult decorateUnderground(WorldAccess world,
            long decorationSeed, int chunkX, int chunkZ, Set<V118Biome> regionBiomes,
            V118WorldgenRandom random) {
        if (world == null || regionBiomes == null || random == null) {
            throw new NullPointerException("world, regionBiomes, and random are required");
        }
        if (!regionBiomes.contains(V118Biome.DRIPSTONE_CAVES)) {
            return new UndergroundResult(0, 0, 0, 0);
        }

        FeatureResult clusters = decorateClusters(world, decorationSeed,
            chunkX, chunkZ, random);
        FeatureResult pointed = decoratePointed(world, decorationSeed,
            chunkX, chunkZ, random);
        return new UndergroundResult(clusters.attempts, clusters.placed,
            pointed.attempts, pointed.placed);
    }

    static FeatureResult decorateClusters(WorldAccess world, long decorationSeed,
            int chunkX, int chunkZ, V118WorldgenRandom random) {
        random.setFeatureSeed(decorationSeed, DRIPSTONE_CLUSTER_INDEX,
            UNDERGROUND_DECORATION_STEP);
        int attempts = V118DripstoneFeature.uniformInt(random,
            CLUSTER_COUNT_MINIMUM, CLUSTER_COUNT_MAXIMUM);
        int placed = placeSimple(world, random, chunkX, chunkZ, attempts,
            new ConfiguredFeature() {
                @Override
                public boolean place(WorldAccess target, Random source, int x, int y, int z) {
                    return V118DripstoneClusterFeature.place(target, source, x, y, z);
                }
            });
        return new FeatureResult(attempts, placed);
    }

    static FeatureResult decoratePointed(WorldAccess world, long decorationSeed,
            int chunkX, int chunkZ, V118WorldgenRandom random) {
        random.setFeatureSeed(decorationSeed, POINTED_DRIPSTONE_INDEX,
            UNDERGROUND_DECORATION_STEP);
        int outerAttempts = V118DripstoneFeature.uniformInt(random,
            POINTED_COUNT_MINIMUM, POINTED_COUNT_MAXIMUM);
        int pointedAttempts = 0;
        int pointedPlaced = 0;
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        for (int outer = 0; outer < outerAttempts; ++outer) {
            int x = originX + random.nextInt(16);
            int z = originZ + random.nextInt(16);
            int y = V118DripstoneFeature.randomBetweenInclusive(random,
                world.minBuildHeight(), TERRAIN_MAXIMUM_Y);
            int repeats = V118DripstoneFeature.uniformInt(random,
                POINTED_REPEATS_MINIMUM, POINTED_REPEATS_MAXIMUM);
            for (int repeat = 0; repeat < repeats; ++repeat) {
                int placedX = x + V118DripstoneFeature.clampedNormalInt(random,
                    0.0F, 3.0F, -10, 10);
                int placedY = y + V118DripstoneFeature.clampedNormalInt(random,
                    0.0F, 0.6F, -2, 2);
                int placedZ = z + V118DripstoneFeature.clampedNormalInt(random,
                    0.0F, 3.0F, -10, 10);
                ++pointedAttempts;
                boolean placed = world.biomeAt(placedX, placedY, placedZ)
                    == V118Biome.DRIPSTONE_CAVES
                    && V118PointedDripstoneFeature.selectScanAndPlace(world, random,
                        placedX, placedY, placedZ);
                if (placed) {
                    ++pointedPlaced;
                }
            }
        }
        return new FeatureResult(pointedAttempts, pointedPlaced);
    }

    public static PlacementResult decorateUnderground(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, Set<V118Biome> regionBiomes) {
        V118WorldgenRandom random = new V118WorldgenRandom(0L);
        long decorationSeed = random.setDecorationSeed(worldSeed, chunkX << 4, chunkZ << 4);
        UndergroundResult result = decorateUnderground(world, decorationSeed,
            chunkX, chunkZ, regionBiomes, random);
        return new PlacementResult(decorationSeed,
            result.clusterAttempts + result.pointedAttempts,
            result.clustersPlaced + result.pointedPlaced);
    }

    private static int placeSimple(WorldAccess world, Random random, int chunkX, int chunkZ,
            int attempts, ConfiguredFeature feature) {
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        int placed = 0;
        for (int attempt = 0; attempt < attempts; ++attempt) {
            int x = originX + random.nextInt(16);
            int z = originZ + random.nextInt(16);
            int y = V118DripstoneFeature.randomBetweenInclusive(random,
                world.minBuildHeight(), TERRAIN_MAXIMUM_Y);
            if (world.biomeAt(x, y, z) == V118Biome.DRIPSTONE_CAVES
                    && feature.place(world, random, x, y, z)) {
                ++placed;
            }
        }
        return placed;
    }

    /** Samples the complete modifier chain without invoking configured-feature RNG. */
    public static List<Position> sampleSimpleOrigins(Random random, int chunkX, int chunkZ,
            int minimumY, int minimumCount, int maximumCount) {
        int count = V118DripstoneFeature.uniformInt(random, minimumCount, maximumCount);
        List<Position> result = new ArrayList<Position>(count);
        for (int attempt = 0; attempt < count; ++attempt) {
            int x = (chunkX << 4) + random.nextInt(16);
            int z = (chunkZ << 4) + random.nextInt(16);
            int y = V118DripstoneFeature.randomBetweenInclusive(random,
                minimumY, TERRAIN_MAXIMUM_Y);
            result.add(new Position(x, y, z));
        }
        return Collections.unmodifiableList(result);
    }

    /** Samples pointed dripstone's outer count, square, height, repeat, and random offset chain. */
    public static List<Position> samplePointedOrigins(Random random, int chunkX, int chunkZ,
            int minimumY) {
        int outerCount = V118DripstoneFeature.uniformInt(random,
            POINTED_COUNT_MINIMUM, POINTED_COUNT_MAXIMUM);
        List<Position> result = new ArrayList<Position>(outerCount * 3);
        for (int outer = 0; outer < outerCount; ++outer) {
            int x = (chunkX << 4) + random.nextInt(16);
            int z = (chunkZ << 4) + random.nextInt(16);
            int y = V118DripstoneFeature.randomBetweenInclusive(random,
                minimumY, TERRAIN_MAXIMUM_Y);
            int repeats = V118DripstoneFeature.uniformInt(random,
                POINTED_REPEATS_MINIMUM, POINTED_REPEATS_MAXIMUM);
            for (int repeat = 0; repeat < repeats; ++repeat) {
                result.add(new Position(
                    x + V118DripstoneFeature.clampedNormalInt(random,
                        0.0F, 3.0F, -10, 10),
                    y + V118DripstoneFeature.clampedNormalInt(random,
                        0.0F, 0.6F, -2, 2),
                    z + V118DripstoneFeature.clampedNormalInt(random,
                        0.0F, 3.0F, -10, 10)));
            }
        }
        return Collections.unmodifiableList(result);
    }

    private interface ConfiguredFeature {
        boolean place(WorldAccess world, Random random, int x, int y, int z);
    }

    public static final class PlacementResult {
        private final long decorationSeed;
        private final int attempts;
        private final int placed;

        private PlacementResult(long decorationSeed, int attempts, int placed) {
            this.decorationSeed = decorationSeed;
            this.attempts = attempts;
            this.placed = placed;
        }

        public long decorationSeed() {
            return decorationSeed;
        }

        public int attempts() {
            return attempts;
        }

        public int placed() {
            return placed;
        }
    }

    public static final class UndergroundResult {
        private final int clusterAttempts;
        private final int clustersPlaced;
        private final int pointedAttempts;
        private final int pointedPlaced;

        private UndergroundResult(int clusterAttempts, int clustersPlaced,
                int pointedAttempts, int pointedPlaced) {
            this.clusterAttempts = clusterAttempts;
            this.clustersPlaced = clustersPlaced;
            this.pointedAttempts = pointedAttempts;
            this.pointedPlaced = pointedPlaced;
        }

        public int clusterAttempts() {
            return clusterAttempts;
        }

        public int clustersPlaced() {
            return clustersPlaced;
        }

        public int pointedAttempts() {
            return pointedAttempts;
        }

        public int pointedPlaced() {
            return pointedPlaced;
        }
    }

    static final class FeatureResult {
        private final int attempts;
        private final int placed;

        private FeatureResult(int attempts, int placed) {
            this.attempts = attempts;
            this.placed = placed;
        }

        int attempts() {
            return attempts;
        }

        int placed() {
            return placed;
        }
    }

    public static final class Position {
        private final int x;
        private final int y;
        private final int z;

        private Position(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public int x() {
            return x;
        }

        public int y() {
            return y;
        }

        public int z() {
            return z;
        }
    }
}

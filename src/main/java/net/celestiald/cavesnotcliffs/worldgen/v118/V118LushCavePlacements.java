package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Exact Java 1.18.2 placement chains for the seven lush-cave vegetation features. */
public final class V118LushCavePlacements {
    public static final int VEGETAL_DECORATION_STEP = 9;
    public static final int MAX_TERRAIN_Y = 256;
    public static final int ENVIRONMENT_SCAN_STEPS = 12;

    private V118LushCavePlacements() {
    }

    public enum PlacedFeature {
        CEILING_VEGETATION("lush_caves_ceiling_vegetation", 22, 125, 125,
            ScanDirection.UP, ScanTarget.SOLID, -1),
        CAVE_VINES("cave_vines", 23, 188, 188,
            ScanDirection.UP, ScanTarget.STURDY_DOWN_FACE, -1),
        LUSH_CAVES_CLAY("lush_caves_clay", 24, 62, 62,
            ScanDirection.DOWN, ScanTarget.SOLID, 1),
        FLOOR_VEGETATION("lush_caves_vegetation", 25, 125, 125,
            ScanDirection.DOWN, ScanTarget.SOLID, 1),
        ROOTED_AZALEA_TREE("rooted_azalea_tree", 26, 1, 2,
            ScanDirection.UP, ScanTarget.SOLID, -1),
        SPORE_BLOSSOM("spore_blossom", 27, 25, 25,
            ScanDirection.UP, ScanTarget.SOLID, -1),
        CLASSIC_VINES("classic_vines_cave_feature", 28, 256, 256,
            ScanDirection.NONE, ScanTarget.NONE, 0);

        private final String id;
        private final int globalIndex;
        private final int minimumCount;
        private final int maximumCount;
        private final ScanDirection scanDirection;
        private final ScanTarget scanTarget;
        private final int verticalOffset;

        PlacedFeature(String id, int globalIndex, int minimumCount, int maximumCount,
                ScanDirection scanDirection, ScanTarget scanTarget, int verticalOffset) {
            this.id = id;
            this.globalIndex = globalIndex;
            this.minimumCount = minimumCount;
            this.maximumCount = maximumCount;
            this.scanDirection = scanDirection;
            this.scanTarget = scanTarget;
            this.verticalOffset = verticalOffset;
        }

        public String id() {
            return id;
        }

        public int globalIndex() {
            return globalIndex;
        }

        public int minimumCount() {
            return minimumCount;
        }

        public int maximumCount() {
            return maximumCount;
        }

        public String scanDirection() {
            return scanDirection.name().toLowerCase(java.util.Locale.ROOT);
        }

        public String scanTarget() {
            return scanTarget.name().toLowerCase(java.util.Locale.ROOT);
        }

        public int verticalOffset() {
            return verticalOffset;
        }
    }

    /** Samples count, in-square, height, scan, offset, and optional biome filter in order. */
    public static List<Position> samplePlacementOrigins(PlacedFeature feature, Random random,
            int chunkX, int chunkZ, PlacementAccess world, boolean applyBiomeFilter) {
        if (feature == null || random == null || world == null) {
            throw new NullPointerException("feature, random, and world are required");
        }
        int count = randomBetweenInclusive(random, feature.minimumCount,
            feature.maximumCount);
        List<Position> result = new ArrayList<Position>(count);
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        for (int attempt = 0; attempt < count; ++attempt) {
            int blockX = originX + random.nextInt(16);
            int blockZ = originZ + random.nextInt(16);
            int blockY = randomBetweenInclusive(random, world.minBuildHeight(), MAX_TERRAIN_Y);
            Position position = scan(feature, world, blockX, blockY, blockZ);
            if (position == null) {
                continue;
            }
            if (applyBiomeFilter && world.biomeAt(position.x, position.y, position.z)
                    != V118Biome.LUSH_CAVES) {
                continue;
            }
            result.add(position);
        }
        return Collections.unmodifiableList(result);
    }

    private static Position scan(PlacedFeature feature, PlacementAccess world,
            int blockX, int blockY, int blockZ) {
        if (feature.scanDirection == ScanDirection.NONE) {
            return new Position(blockX, blockY, blockZ);
        }
        if (!world.isAir(blockX, blockY, blockZ)) {
            return null;
        }
        int directionY = feature.scanDirection == ScanDirection.UP ? 1 : -1;
        int cursorY = blockY;
        for (int step = 0; step < ENVIRONMENT_SCAN_STEPS; ++step) {
            if (matches(feature.scanTarget, world, blockX, cursorY, blockZ)) {
                return new Position(blockX, cursorY + feature.verticalOffset, blockZ);
            }
            cursorY += directionY;
            if (cursorY < world.minBuildHeight() || cursorY >= world.maxBuildHeight()) {
                return null;
            }
            if (!world.isAir(blockX, cursorY, blockZ)) {
                break;
            }
        }
        return matches(feature.scanTarget, world, blockX, cursorY, blockZ)
            ? new Position(blockX, cursorY + feature.verticalOffset, blockZ) : null;
    }

    private static boolean matches(ScanTarget target, PlacementAccess world,
            int blockX, int blockY, int blockZ) {
        if (target == ScanTarget.SOLID) {
            return world.isSolid(blockX, blockY, blockZ);
        }
        if (target == ScanTarget.STURDY_DOWN_FACE) {
            return world.hasSturdyDownFace(blockX, blockY, blockZ);
        }
        return true;
    }

    private static int randomBetweenInclusive(Random random, int minimum, int maximum) {
        return minimum == maximum ? minimum : minimum + random.nextInt(maximum - minimum + 1);
    }

    private enum ScanDirection {
        NONE,
        UP,
        DOWN
    }

    private enum ScanTarget {
        NONE,
        SOLID,
        STURDY_DOWN_FACE
    }

    public interface PlacementAccess {
        int minBuildHeight();

        int maxBuildHeight();

        boolean isAir(int blockX, int blockY, int blockZ);

        boolean isSolid(int blockX, int blockY, int blockZ);

        boolean hasSturdyDownFace(int blockX, int blockY, int blockZ);

        V118Biome biomeAt(int blockX, int blockY, int blockZ);
    }

    public static final class Position {
        private final int x;
        private final int y;
        private final int z;

        Position(int x, int y, int z) {
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

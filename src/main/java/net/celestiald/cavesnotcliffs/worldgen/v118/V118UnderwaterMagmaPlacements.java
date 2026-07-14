package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Exact Java 1.18.2 placed-feature wrapper for default Overworld underwater magma. */
public final class V118UnderwaterMagmaPlacements {
    public static final int UNDERGROUND_ORES_STEP = 6;
    public static final int GLOBAL_FEATURE_INDEX = 25;
    public static final int MINIMUM_COUNT = 44;
    public static final int MAXIMUM_COUNT = 52;
    public static final int MINIMUM_Y = -64;
    public static final int MAXIMUM_Y = 256;
    public static final int MAXIMUM_SURFACE_OFFSET = -2;

    private V118UnderwaterMagmaPlacements() {
    }

    static boolean decorate(V118OrePlacements.WorldAccess world, long decorationSeed,
            int chunkX, int chunkZ, V118WorldgenRandom random) {
        random.setFeatureSeed(decorationSeed, GLOBAL_FEATURE_INDEX, UNDERGROUND_ORES_STEP);
        return place(world, random, chunkX << 4, chunkZ << 4);
    }

    private static boolean place(V118OrePlacements.WorldAccess world, Random random,
            int originX, int originZ) {
        int attempts = MINIMUM_COUNT + random.nextInt(MAXIMUM_COUNT - MINIMUM_COUNT + 1);
        boolean placed = false;
        for (int attempt = 0; attempt < attempts; ++attempt) {
            int x = originX + random.nextInt(16);
            int z = originZ + random.nextInt(16);
            int y = MINIMUM_Y + random.nextInt(MAXIMUM_Y - MINIMUM_Y + 1);
            // OCEAN_FLOOR_WG returns first-available Y, whereas this bridge exposes its
            // highest motion-blocking block. firstAvailable - 2 == highestBlock - 1.
            if (y > world.oceanFloorHeight(x, z) - 1) {
                continue;
            }
            if (V118UnderwaterMagmaFeature.place(world, random, x, y, z)) {
                placed = true;
            }
        }
        return placed;
    }

    /** Samples the complete modifier chain before the configured feature consumes RNG. */
    public static List<Position> samplePlacementOrigins(Random random, int originX, int originZ,
            OceanFloorHeightResolver heights) {
        int attempts = MINIMUM_COUNT + random.nextInt(MAXIMUM_COUNT - MINIMUM_COUNT + 1);
        List<Position> result = new ArrayList<Position>(attempts);
        for (int attempt = 0; attempt < attempts; ++attempt) {
            int x = originX + random.nextInt(16);
            int z = originZ + random.nextInt(16);
            int y = MINIMUM_Y + random.nextInt(MAXIMUM_Y - MINIMUM_Y + 1);
            if (y <= heights.firstAvailableY(x, z) + MAXIMUM_SURFACE_OFFSET) {
                result.add(new Position(x, y, z));
            }
        }
        return Collections.unmodifiableList(result);
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

    public interface OceanFloorHeightResolver {
        int firstAvailableY(int blockX, int blockZ);
    }
}

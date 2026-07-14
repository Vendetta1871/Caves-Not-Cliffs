package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.Random;

/** Exact Java 1.18.2 fossil placed-feature chains. */
public final class V118FossilPlacements {
    public static final int UNDERGROUND_STRUCTURES_STEP = 3;
    public static final int FOSSIL_UPPER_INDEX = 0;
    public static final int FOSSIL_LOWER_INDEX = 1;

    private static final int MIN_BUILD_HEIGHT = -64;
    private static final int MAX_BUILD_HEIGHT = 320;

    private V118FossilPlacements() {
    }

    /** Runs fossil_upper followed by fossil_lower in global registration order. */
    public static int decorate(WorldAccess world, long worldSeed, int chunkX, int chunkZ) {
        if (world == null) {
            throw new NullPointerException("world");
        }
        int placed = decoratePlacedFeature(world, worldSeed, chunkX, chunkZ,
            FOSSIL_UPPER_INDEX, 0, MAX_BUILD_HEIGHT - 1, false);
        return placed + decoratePlacedFeature(world, worldSeed, chunkX, chunkZ,
            FOSSIL_LOWER_INDEX, MIN_BUILD_HEIGHT, -8, true);
    }

    private static int decoratePlacedFeature(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, int globalIndex, int minY, int maxY,
            boolean diamondOverlay) {
        V118WorldgenRandom random = new V118WorldgenRandom(0L);
        long decorationSeed = random.setDecorationSeed(worldSeed, chunkX << 4, chunkZ << 4);
        random.setFeatureSeed(decorationSeed, globalIndex, UNDERGROUND_STRUCTURES_STEP);

        // RarityFilter runs before InSquare and HeightRange.
        if (random.nextFloat() >= 1.0F / 64.0F) {
            return 0;
        }
        int x = (chunkX << 4) + random.nextInt(16);
        int z = (chunkZ << 4) + random.nextInt(16);
        int y = minY + random.nextInt(maxY - minY + 1);
        V118Biome biome = world.biomeAt(x, y, z);
        if (biome != V118Biome.DESERT && biome != V118Biome.SWAMP) {
            return 0;
        }
        return world.placeFossil(random, x, y, z, diamondOverlay) ? 1 : 0;
    }

    /** Finite-world bridge for the template-backed configured feature body. */
    public interface WorldAccess {
        V118Biome biomeAt(int blockX, int blockY, int blockZ);

        boolean placeFossil(Random random, int originX, int originY, int originZ,
            boolean diamondOverlay);
    }
}

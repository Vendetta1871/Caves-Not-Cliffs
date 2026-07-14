package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.Random;

/** Exact Java 1.18.2 Overworld lava-lake placements and deprecated lake feature body. */
public final class V118LavaLakePlacements {
    public static final int LAKES_STEP = 1;
    public static final int UNDERGROUND_INDEX = 0;
    public static final int SURFACE_INDEX = 1;

    private static final int UNDERGROUND_RARITY = 9;
    private static final int SURFACE_RARITY = 200;
    private static final int SCAN_STEPS = 32;

    private V118LavaLakePlacements() {
    }

    /** Runs the two global placed features in their 1.18.2 order. */
    public static int decorate(WorldAccess world, long worldSeed, int chunkX, int chunkZ) {
        if (world == null) {
            throw new NullPointerException("world");
        }
        int placed = decorateUnderground(world, worldSeed, chunkX, chunkZ) ? 1 : 0;
        return placed + (decorateSurface(world, worldSeed, chunkX, chunkZ) ? 1 : 0);
    }

    public static boolean decorateUnderground(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ) {
        Random random = featureRandom(worldSeed, chunkX, chunkZ, UNDERGROUND_INDEX);
        if (random.nextFloat() >= 1.0F / UNDERGROUND_RARITY) {
            return false;
        }

        int x = (chunkX << 4) + random.nextInt(16);
        int z = (chunkZ << 4) + random.nextInt(16);
        int y = random.nextInt(world.maxBuildHeight());
        Integer scannedY = scanDown(world, x, y, z);
        if (scannedY == null
                || scannedY > (long) world.oceanFloorWgHeight(x, z) - 5L
                || !world.supportsLavaLake(x, scannedY, z)) {
            return false;
        }
        return place(world, random, x, scannedY, z);
    }

    public static boolean decorateSurface(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ) {
        Random random = featureRandom(worldSeed, chunkX, chunkZ, SURFACE_INDEX);
        if (random.nextFloat() >= 1.0F / SURFACE_RARITY) {
            return false;
        }

        int x = (chunkX << 4) + random.nextInt(16);
        int z = (chunkZ << 4) + random.nextInt(16);
        int y = world.worldSurfaceWgHeight(x, z);
        return y > world.minBuildHeight()
            && world.supportsLavaLake(x, y, z)
            && place(world, random, x, y, z);
    }

    /** Java 1.18.2 {@code LakeFeature} with the configured lava and stone providers. */
    static boolean place(WorldAccess world, Random random, int originX, int originY,
            int originZ) {
        if (originY <= world.minBuildHeight() + 4) {
            return false;
        }
        int baseY = originY - 4;
        boolean[] lake = new boolean[16 * 16 * 8];
        int ellipsoids = random.nextInt(4) + 4;
        for (int ellipsoid = 0; ellipsoid < ellipsoids; ++ellipsoid) {
            double sizeX = random.nextDouble() * 6.0D + 3.0D;
            double sizeY = random.nextDouble() * 4.0D + 2.0D;
            double sizeZ = random.nextDouble() * 6.0D + 3.0D;
            double centerX = random.nextDouble() * (16.0D - sizeX - 2.0D)
                + 1.0D + sizeX / 2.0D;
            double centerY = random.nextDouble() * (8.0D - sizeY - 4.0D)
                + 2.0D + sizeY / 2.0D;
            double centerZ = random.nextDouble() * (16.0D - sizeZ - 2.0D)
                + 1.0D + sizeZ / 2.0D;
            for (int x = 1; x < 15; ++x) {
                for (int z = 1; z < 15; ++z) {
                    for (int y = 1; y < 7; ++y) {
                        double dx = (x - centerX) / (sizeX / 2.0D);
                        double dy = (y - centerY) / (sizeY / 2.0D);
                        double dz = (z - centerZ) / (sizeZ / 2.0D);
                        if (dx * dx + dy * dy + dz * dz < 1.0D) {
                            lake[index(x, y, z)] = true;
                        }
                    }
                }
            }
        }

        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                for (int y = 0; y < 8; ++y) {
                    if (!isBoundary(lake, x, y, z)) {
                        continue;
                    }
                    int blockX = originX + x;
                    int blockY = baseY + y;
                    int blockZ = originZ + z;
                    if (y >= 4 && world.isLiquid(blockX, blockY, blockZ)) {
                        return false;
                    }
                    if (y < 4 && !world.isSolid(blockX, blockY, blockZ)
                            && !world.isSourceLava(blockX, blockY, blockZ)) {
                        return false;
                    }
                }
            }
        }

        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                for (int y = 0; y < 8; ++y) {
                    if (!lake[index(x, y, z)]) {
                        continue;
                    }
                    int blockX = originX + x;
                    int blockY = baseY + y;
                    int blockZ = originZ + z;
                    if (world.featuresCannotReplace(blockX, blockY, blockZ)) {
                        continue;
                    }
                    if (y >= 4) {
                        world.setCaveAir(blockX, blockY, blockZ);
                        world.scheduleCaveAirTick(blockX, blockY, blockZ);
                        world.markAboveForPostProcessing(blockX, blockY, blockZ);
                    } else {
                        world.setSourceLava(blockX, blockY, blockZ);
                    }
                }
            }
        }

        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                for (int y = 0; y < 8; ++y) {
                    if (!isBoundary(lake, x, y, z)
                            || y >= 4 && random.nextInt(2) == 0) {
                        continue;
                    }
                    int blockX = originX + x;
                    int blockY = baseY + y;
                    int blockZ = originZ + z;
                    if (world.isSolid(blockX, blockY, blockZ)
                            && !world.lavaPoolStoneCannotReplace(blockX, blockY, blockZ)) {
                        world.setStone(blockX, blockY, blockZ);
                        world.markAboveForPostProcessing(blockX, blockY, blockZ);
                    }
                }
            }
        }
        return true;
    }

    private static Integer scanDown(WorldAccess world, int x, int startY, int z) {
        int y = startY;
        for (int step = 0; step < SCAN_STEPS; ++step) {
            if (isScanTarget(world, x, y, z)) {
                return y;
            }
            --y;
            if (y < world.minBuildHeight() || y >= world.maxBuildHeight()) {
                return null;
            }
        }
        return isScanTarget(world, x, y, z) ? y : null;
    }

    private static boolean isScanTarget(WorldAccess world, int x, int y, int z) {
        return !world.isOrdinaryAir(x, y, z)
            && y - 5 >= world.minBuildHeight()
            && y - 5 < world.maxBuildHeight();
    }

    private static boolean isBoundary(boolean[] lake, int x, int y, int z) {
        return !lake[index(x, y, z)]
            && (x < 15 && lake[index(x + 1, y, z)]
                || x > 0 && lake[index(x - 1, y, z)]
                || z < 15 && lake[index(x, y, z + 1)]
                || z > 0 && lake[index(x, y, z - 1)]
                || y < 7 && lake[index(x, y + 1, z)]
                || y > 0 && lake[index(x, y - 1, z)]);
    }

    private static int index(int x, int y, int z) {
        return (x * 16 + z) * 8 + y;
    }

    private static Random featureRandom(long worldSeed, int chunkX, int chunkZ,
            int globalIndex) {
        V118WorldgenRandom random = new V118WorldgenRandom(0L);
        long decorationSeed = random.setDecorationSeed(worldSeed, chunkX << 4, chunkZ << 4);
        random.setFeatureSeed(decorationSeed, globalIndex, LAKES_STEP);
        return random;
    }

    /** Forge-independent semantic bridge for the later world adapter. */
    public interface WorldAccess {
        int minBuildHeight();

        /** Exclusive upper build bound; 320 for the native profile. */
        int maxBuildHeight();

        int oceanFloorWgHeight(int blockX, int blockZ);

        int worldSurfaceWgHeight(int blockX, int blockZ);

        boolean supportsLavaLake(int blockX, int blockY, int blockZ);

        /** True only for the ordinary AIR block, matching ONLY_IN_AIR_PREDICATE. */
        boolean isOrdinaryAir(int blockX, int blockY, int blockZ);

        boolean isLiquid(int blockX, int blockY, int blockZ);

        boolean isSolid(int blockX, int blockY, int blockZ);

        /** True only for the configured default source-lava block state. */
        boolean isSourceLava(int blockX, int blockY, int blockZ);

        boolean featuresCannotReplace(int blockX, int blockY, int blockZ);

        boolean lavaPoolStoneCannotReplace(int blockX, int blockY, int blockZ);

        void setCaveAir(int blockX, int blockY, int blockZ);

        void setSourceLava(int blockX, int blockY, int blockZ);

        void setStone(int blockX, int blockY, int blockZ);

        void scheduleCaveAirTick(int blockX, int blockY, int blockZ);

        void markAboveForPostProcessing(int blockX, int blockY, int blockZ);
    }
}

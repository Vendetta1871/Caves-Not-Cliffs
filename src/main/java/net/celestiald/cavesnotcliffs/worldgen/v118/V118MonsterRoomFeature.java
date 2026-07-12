package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.Random;

/** Exact Java 1.18.2 monster-room placements and feature body. */
public final class V118MonsterRoomFeature {
    public static final int UNDERGROUND_STRUCTURES_STEP = 3;
    public static final int MONSTER_ROOM_INDEX = 2;
    public static final int DEEP_MONSTER_ROOM_INDEX = 3;

    private static final int MIN_BUILD_HEIGHT = -64;
    private static final int MAX_BUILD_HEIGHT = 320;
    private static final int[] ATTACHMENT_X = {0, 1, 0, -1};
    private static final int[] ATTACHMENT_Z = {-1, 0, 1, 0};
    private static final MobKind[] MOBS = {
        MobKind.SKELETON, MobKind.ZOMBIE, MobKind.ZOMBIE, MobKind.SPIDER
    };

    private V118MonsterRoomFeature() {
    }

    /** Runs both globally registered placed features in their Java 1.18.2 order. */
    public static int decorate(WorldAccess world, long worldSeed, int chunkX, int chunkZ) {
        if (world == null) {
            throw new NullPointerException("world");
        }
        int placed = decoratePlacedFeature(world, worldSeed, chunkX, chunkZ,
            MONSTER_ROOM_INDEX, 10, 0, MAX_BUILD_HEIGHT - 1);
        return placed + decoratePlacedFeature(world, worldSeed, chunkX, chunkZ,
            DEEP_MONSTER_ROOM_INDEX, 4, MIN_BUILD_HEIGHT + 6, -1);
    }

    private static int decoratePlacedFeature(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, int globalIndex, int count, int minY, int maxY) {
        V118WorldgenRandom random = new V118WorldgenRandom(0L);
        long decorationSeed = random.setDecorationSeed(worldSeed, chunkX << 4, chunkZ << 4);
        random.setFeatureSeed(decorationSeed, globalIndex, UNDERGROUND_STRUCTURES_STEP);
        int placed = 0;
        for (int attempt = 0; attempt < count; ++attempt) {
            int x = (chunkX << 4) + random.nextInt(16);
            int z = (chunkZ << 4) + random.nextInt(16);
            int y = minY + random.nextInt(maxY - minY + 1);
            if (place(world, random, x, y, z)) {
                ++placed;
            }
        }
        return placed;
    }

    /** Java 1.18.2 {@code MonsterRoomFeature}. */
    static boolean place(WorldAccess world, Random random, int originX, int originY,
            int originZ) {
        int radiusX = random.nextInt(2) + 2;
        int minX = -radiusX - 1;
        int maxX = radiusX + 1;
        int radiusZ = random.nextInt(2) + 2;
        int minZ = -radiusZ - 1;
        int maxZ = radiusZ + 1;
        int openings = 0;

        for (int x = minX; x <= maxX; ++x) {
            for (int y = -1; y <= 4; ++y) {
                for (int z = minZ; z <= maxZ; ++z) {
                    int blockX = originX + x;
                    int blockY = originY + y;
                    int blockZ = originZ + z;
                    boolean solid = world.isSolid(blockX, blockY, blockZ);
                    if (y == -1 && !solid || y == 4 && !solid) {
                        return false;
                    }
                    if ((x == minX || x == maxX || z == minZ || z == maxZ)
                            && y == 0
                            && world.isAir(blockX, blockY, blockZ)
                            && world.isAir(blockX, blockY + 1, blockZ)) {
                        ++openings;
                    }
                }
            }
        }
        if (openings < 1 || openings > 5) {
            return false;
        }

        for (int x = minX; x <= maxX; ++x) {
            for (int y = 3; y >= -1; --y) {
                for (int z = minZ; z <= maxZ; ++z) {
                    int blockX = originX + x;
                    int blockY = originY + y;
                    int blockZ = originZ + z;
                    boolean boundary = x == minX || x == maxX || y == -1 || y == 4
                        || z == minZ || z == maxZ;
                    if (boundary) {
                        if (blockY >= MIN_BUILD_HEIGHT
                                && !world.isSolid(blockX, blockY - 1, blockZ)) {
                            world.setCaveAir(blockX, blockY, blockZ, false);
                        } else if (world.isSolid(blockX, blockY, blockZ)
                                && !world.isChest(blockX, blockY, blockZ)) {
                            if (y == -1 && random.nextInt(4) != 0) {
                                world.setMossyCobblestone(blockX, blockY, blockZ);
                            } else {
                                world.setCobblestone(blockX, blockY, blockZ);
                            }
                        }
                    } else if (!world.isChest(blockX, blockY, blockZ)
                            && !world.isSpawner(blockX, blockY, blockZ)) {
                        world.setCaveAir(blockX, blockY, blockZ, true);
                    }
                }
            }
        }

        for (int chest = 0; chest < 2; ++chest) {
            for (int attempt = 0; attempt < 3; ++attempt) {
                int x = originX + random.nextInt(radiusX * 2 + 1) - radiusX;
                int z = originZ + random.nextInt(radiusZ * 2 + 1) - radiusZ;
                if (!world.isAir(x, originY, z)) {
                    continue;
                }
                int solidNeighbors = 0;
                for (int direction = 0; direction < ATTACHMENT_X.length; ++direction) {
                    if (world.isSolid(x + ATTACHMENT_X[direction], originY,
                            z + ATTACHMENT_Z[direction])) {
                        ++solidNeighbors;
                    }
                }
                if (solidNeighbors == 1) {
                    world.setDungeonChest(x, originY, z, chest, random);
                    break;
                }
            }
        }

        world.setSpawner(originX, originY, originZ,
            MOBS[random.nextInt(MOBS.length)]);
        return true;
    }

    public enum MobKind {
        SKELETON,
        ZOMBIE,
        SPIDER
    }

    /** Runtime-independent bridge for the finite Forge 1.12 world. */
    public interface WorldAccess {
        boolean isAir(int blockX, int blockY, int blockZ);

        boolean isSolid(int blockX, int blockY, int blockZ);

        boolean isChest(int blockX, int blockY, int blockZ);

        boolean isSpawner(int blockX, int blockY, int blockZ);

        void setCaveAir(int blockX, int blockY, int blockZ, boolean safe);

        void setMossyCobblestone(int blockX, int blockY, int blockZ);

        void setCobblestone(int blockX, int blockY, int blockZ);

        void setDungeonChest(int blockX, int blockY, int blockZ, int ordinal, Random random);

        void setSpawner(int blockX, int blockY, int blockZ, MobKind mob);
    }
}

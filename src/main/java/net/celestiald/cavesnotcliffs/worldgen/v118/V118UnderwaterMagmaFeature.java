package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.Random;

/** Dependency-free Java 8 port of Java 1.18.2's {@code UnderwaterMagmaFeature}. */
public final class V118UnderwaterMagmaFeature {
    public static final int FLOOR_SEARCH_RANGE = 5;
    public static final int PLACEMENT_RADIUS = 1;
    public static final float PLACEMENT_PROBABILITY = 0.5F;

    private V118UnderwaterMagmaFeature() {
    }

    public static boolean place(WorldAccess world, Random random,
            int originX, int originY, int originZ) {
        Integer floorY = floorY(world, originX, originY, originZ);
        if (floorY == null) {
            return false;
        }
        int placed = 0;
        // BlockPos.betweenClosed traverses X fastest, then Y, then Z.
        for (int z = originZ - PLACEMENT_RADIUS; z <= originZ + PLACEMENT_RADIUS; ++z) {
            for (int y = floorY - PLACEMENT_RADIUS;
                    y <= floorY + PLACEMENT_RADIUS; ++y) {
                for (int x = originX - PLACEMENT_RADIUS;
                        x <= originX + PLACEMENT_RADIUS; ++x) {
                    if (random.nextFloat() >= PLACEMENT_PROBABILITY
                            || !isValidPlacement(world, x, y, z)) {
                        continue;
                    }
                    world.setMaterial(x, y, z, V118OreMaterial.MAGMA);
                    ++placed;
                }
            }
        }
        return placed > 0;
    }

    static Integer floorY(WorldAccess world, int x, int originY, int z) {
        if (world.getMaterial(x, originY, z) != V118OreMaterial.WATER) {
            return null;
        }
        int y = originY;
        for (int distance = 1; distance < FLOOR_SEARCH_RANGE
                && world.getMaterial(x, y, z) == V118OreMaterial.WATER; ++distance) {
            --y;
        }
        return world.getMaterial(x, y, z) == V118OreMaterial.WATER ? null : y;
    }

    static boolean isValidPlacement(WorldAccess world, int x, int y, int z) {
        return !isWaterOrAir(world.getMaterial(x, y, z))
                && !isWaterOrAir(world.getMaterial(x, y - 1, z))
                && !isWaterOrAir(world.getMaterial(x - 1, y, z))
                && !isWaterOrAir(world.getMaterial(x + 1, y, z))
                && !isWaterOrAir(world.getMaterial(x, y, z - 1))
                && !isWaterOrAir(world.getMaterial(x, y, z + 1));
    }

    private static boolean isWaterOrAir(V118OreMaterial material) {
        return material == V118OreMaterial.WATER || material == V118OreMaterial.AIR;
    }

    public interface WorldAccess {
        V118OreMaterial getMaterial(int blockX, int blockY, int blockZ);

        void setMaterial(int blockX, int blockY, int blockZ, V118OreMaterial material);
    }
}

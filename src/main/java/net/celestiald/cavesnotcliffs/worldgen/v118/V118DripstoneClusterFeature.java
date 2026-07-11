package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.Random;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118DripstoneFeature.Column;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118DripstoneFeature.Direction;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118DripstoneFeature.State;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118DripstoneFeature.WorldAccess;

/** Exact Java 8 port of Java 1.18.2's configured {@code DripstoneClusterFeature}. */
public final class V118DripstoneClusterFeature {
    public static final int FLOOR_TO_CEILING_SEARCH_RANGE = 12;
    public static final int MIN_HEIGHT = 3;
    public static final int MAX_HEIGHT = 6;
    public static final int MIN_RADIUS = 2;
    public static final int MAX_RADIUS = 8;
    public static final int MAX_HEIGHT_DIFFERENCE = 1;
    public static final int HEIGHT_DEVIATION = 3;
    public static final int MIN_LAYER_THICKNESS = 2;
    public static final int MAX_LAYER_THICKNESS = 4;
    public static final float MIN_DENSITY = 0.3F;
    public static final float MAX_DENSITY = 0.7F;
    public static final float WETNESS_MEAN = 0.1F;
    public static final float WETNESS_DEVIATION = 0.3F;
    public static final float MIN_WETNESS = 0.1F;
    public static final float MAX_WETNESS = 0.9F;
    public static final float EDGE_COLUMN_CHANCE = 0.1F;
    public static final int EDGE_CHANCE_DISTANCE = 3;
    public static final int CENTER_HEIGHT_BIAS_DISTANCE = 8;

    private static final Direction[] HORIZONTAL = {
        Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };

    private V118DripstoneClusterFeature() {
    }

    public static boolean place(WorldAccess world, Random random,
            int originX, int originY, int originZ) {
        if (!V118DripstoneFeature.isEmptyOrWater(
                world.getState(originX, originY, originZ))) {
            return false;
        }
        int height = V118DripstoneFeature.uniformInt(random, MIN_HEIGHT, MAX_HEIGHT);
        float wetness = V118DripstoneFeature.clampedNormalFloat(random,
            WETNESS_MEAN, WETNESS_DEVIATION, MIN_WETNESS, MAX_WETNESS);
        float density = V118DripstoneFeature.uniformFloat(random,
            MIN_DENSITY, MAX_DENSITY);
        int radiusX = V118DripstoneFeature.uniformInt(random, MIN_RADIUS, MAX_RADIUS);
        int radiusZ = V118DripstoneFeature.uniformInt(random, MIN_RADIUS, MAX_RADIUS);
        for (int offsetX = -radiusX; offsetX <= radiusX; ++offsetX) {
            for (int offsetZ = -radiusZ; offsetZ <= radiusZ; ++offsetZ) {
                double chance = chanceOfColumn(radiusX, radiusZ, offsetX, offsetZ);
                placeColumn(world, random, originX + offsetX, originY, originZ + offsetZ,
                    offsetX, offsetZ, wetness, chance, height, density);
            }
        }
        return true;
    }

    private static void placeColumn(WorldAccess world, Random random,
            int x, int originY, int z, int offsetX, int offsetZ, float wetness,
            double chance, int maximumHeight, float density) {
        Column scanned = V118DripstoneFeature.scanColumn(world, x, originY, z,
            FLOOR_TO_CEILING_SEARCH_RANGE, V118DripstoneFeature.EMPTY_OR_WATER,
            V118DripstoneFeature.NEITHER_EMPTY_NOR_WATER);
        if (scanned == null || scanned.ceiling() == null && scanned.floor() == null) {
            return;
        }
        Integer ceiling = scanned.ceiling();
        Integer floor = scanned.floor();
        boolean wet = random.nextFloat() < wetness;
        Column column;
        if (wet && floor != null && canPlacePool(world, x, floor.intValue(), z)) {
            world.setWater(x, floor.intValue(), z);
            column = scanned.withFloor(Integer.valueOf(floor.intValue() - 1));
        } else {
            column = scanned;
        }
        Integer adjustedFloor = column.floor();

        boolean growFromCeiling = random.nextDouble() < chance;
        int stalactiteHeight;
        if (ceiling != null && growFromCeiling
                && world.getState(x, ceiling.intValue(), z) != State.LAVA) {
            int layer = V118DripstoneFeature.uniformInt(random,
                MIN_LAYER_THICKNESS, MAX_LAYER_THICKNESS);
            replaceBlocks(world, x, ceiling.intValue(), z, layer, Direction.UP);
            int available = adjustedFloor == null ? maximumHeight
                : Math.min(maximumHeight, ceiling.intValue() - adjustedFloor.intValue());
            stalactiteHeight = dripstoneHeight(random, offsetX, offsetZ,
                density, available);
        } else {
            stalactiteHeight = 0;
        }

        boolean growFromFloor = random.nextDouble() < chance;
        int stalagmiteHeight;
        if (adjustedFloor != null && growFromFloor
                && world.getState(x, adjustedFloor.intValue(), z) != State.LAVA) {
            int layer = V118DripstoneFeature.uniformInt(random,
                MIN_LAYER_THICKNESS, MAX_LAYER_THICKNESS);
            replaceBlocks(world, x, adjustedFloor.intValue(), z, layer, Direction.DOWN);
            if (ceiling != null) {
                stalagmiteHeight = Math.max(0, stalactiteHeight
                    + V118DripstoneFeature.randomBetweenInclusive(random,
                        -MAX_HEIGHT_DIFFERENCE, MAX_HEIGHT_DIFFERENCE));
            } else {
                stalagmiteHeight = dripstoneHeight(random, offsetX, offsetZ,
                    density, maximumHeight);
            }
        } else {
            stalagmiteHeight = 0;
        }

        int finalStalactiteHeight;
        int finalStalagmiteHeight;
        if (ceiling != null && adjustedFloor != null
                && ceiling.intValue() - stalactiteHeight
                    <= adjustedFloor.intValue() + stalagmiteHeight) {
            int minimumMeetingY = Math.max(ceiling.intValue() - stalactiteHeight,
                adjustedFloor.intValue() + 1);
            int maximumMeetingY = Math.min(adjustedFloor.intValue() + stalagmiteHeight,
                ceiling.intValue() - 1);
            int meetingY = V118DripstoneFeature.randomBetweenInclusive(random,
                minimumMeetingY, maximumMeetingY + 1);
            finalStalactiteHeight = ceiling.intValue() - meetingY;
            finalStalagmiteHeight = meetingY - 1 - adjustedFloor.intValue();
        } else {
            finalStalactiteHeight = stalactiteHeight;
            finalStalagmiteHeight = stalagmiteHeight;
        }

        boolean merge = random.nextBoolean() && finalStalactiteHeight > 0
            && finalStalagmiteHeight > 0 && column.height() != null
            && finalStalactiteHeight + finalStalagmiteHeight
                == column.height().intValue();
        if (ceiling != null) {
            V118DripstoneFeature.growPointedDripstone(world, x,
                ceiling.intValue() - 1, z, Direction.DOWN,
                finalStalactiteHeight, merge);
        }
        if (adjustedFloor != null) {
            V118DripstoneFeature.growPointedDripstone(world, x,
                adjustedFloor.intValue() + 1, z, Direction.UP,
                finalStalagmiteHeight, merge);
        }
    }

    private static int dripstoneHeight(Random random, int offsetX, int offsetZ,
            float density, int maximumHeight) {
        if (random.nextFloat() > density) {
            return 0;
        }
        int distance = Math.abs(offsetX) + Math.abs(offsetZ);
        float bias = (float) V118DripstoneFeature.clampedMap((double) distance,
            0.0D, CENTER_HEIGHT_BIAS_DISTANCE, maximumHeight / 2.0D, 0.0D);
        return (int) V118DripstoneFeature.clampedNormalFloat(random, bias,
            HEIGHT_DEVIATION, 0.0F, (float) maximumHeight);
    }

    private static boolean canPlacePool(WorldAccess world, int x, int y, int z) {
        State state = world.getState(x, y, z);
        if (state == State.WATER || state == State.DRIPSTONE_BLOCK
                || state == State.POINTED_DRIPSTONE || world.isWaterAt(x, y + 1, z)) {
            return false;
        }
        for (Direction direction : HORIZONTAL) {
            if (!canBeAdjacentToWater(world, x + direction.stepX(),
                    y, z + direction.stepZ())) {
                return false;
            }
        }
        return canBeAdjacentToWater(world, x, y - 1, z);
    }

    private static boolean canBeAdjacentToWater(WorldAccess world, int x, int y, int z) {
        return world.getState(x, y, z) == State.BASE_STONE
            || world.isWaterAt(x, y, z);
    }

    private static void replaceBlocks(WorldAccess world, int x, int y, int z,
            int thickness, Direction direction) {
        for (int distance = 0; distance < thickness; ++distance) {
            if (!V118DripstoneFeature.placeDripstoneBlockIfPossible(world, x, y, z)) {
                return;
            }
            x += direction.stepX();
            y += direction.stepY();
            z += direction.stepZ();
        }
    }

    private static double chanceOfColumn(int radiusX, int radiusZ,
            int offsetX, int offsetZ) {
        int xDistanceFromEdge = radiusX - Math.abs(offsetX);
        int zDistanceFromEdge = radiusZ - Math.abs(offsetZ);
        int distanceFromEdge = Math.min(xDistanceFromEdge, zDistanceFromEdge);
        return V118DripstoneFeature.clampedMap((float) distanceFromEdge,
            0.0F, EDGE_CHANCE_DISTANCE, EDGE_COLUMN_CHANCE, 1.0F);
    }
}

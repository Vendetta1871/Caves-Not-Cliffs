package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.Random;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118DripstoneFeature.Direction;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118DripstoneFeature.WorldAccess;

/** Exact Java 8 port of Java 1.18.2's configured {@code PointedDripstoneFeature}. */
public final class V118PointedDripstoneFeature {
    public static final float TALLER_CHANCE = 0.2F;
    public static final float DIRECTIONAL_SPREAD_CHANCE = 0.7F;
    public static final float SPREAD_RADIUS_TWO_CHANCE = 0.5F;
    public static final float SPREAD_RADIUS_THREE_CHANCE = 0.5F;
    public static final int ENVIRONMENT_SCAN_STEPS = 12;

    private static final Direction[] HORIZONTAL = {
        Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };

    private V118PointedDripstoneFeature() {
    }

    /** Runs the simple-random selector plus its selected inline environment-scan placement. */
    public static boolean selectScanAndPlace(WorldAccess world, Random random,
            int originX, int originY, int originZ) {
        Direction search = random.nextInt(2) == 0 ? Direction.DOWN : Direction.UP;
        Integer supportY = scanForSolid(world, originX, originY, originZ, search);
        if (supportY == null) {
            return false;
        }
        int placedY = supportY.intValue() + (search == Direction.DOWN ? 1 : -1);
        return place(world, random, originX, placedY, originZ);
    }

    public static boolean place(WorldAccess world, Random random,
            int originX, int originY, int originZ) {
        Direction tipDirection = tipDirection(world, random, originX, originY, originZ);
        if (tipDirection == null) {
            return false;
        }
        Direction baseDirection = tipDirection.opposite();
        int baseX = originX + baseDirection.stepX();
        int baseY = originY + baseDirection.stepY();
        int baseZ = originZ + baseDirection.stepZ();
        createPatch(world, random, baseX, baseY, baseZ);
        int forwardX = originX + tipDirection.stepX();
        int forwardY = originY + tipDirection.stepY();
        int forwardZ = originZ + tipDirection.stepZ();
        int height = random.nextFloat() < TALLER_CHANCE
                && V118DripstoneFeature.isEmptyOrWater(
                    world.getState(forwardX, forwardY, forwardZ)) ? 2 : 1;
        V118DripstoneFeature.growPointedDripstone(world, originX, originY, originZ,
            tipDirection, height, false);
        return true;
    }

    static Integer scanForSolid(WorldAccess world, int x, int originY, int z,
            Direction search) {
        if (!isAirOrWater(world, x, originY, z)) {
            return null;
        }
        int y = originY;
        for (int step = 0; step < ENVIRONMENT_SCAN_STEPS; ++step) {
            if (isSolid(world, x, y, z)) {
                return Integer.valueOf(y);
            }
            y += search.stepY();
            if (y < world.minBuildHeight() || y >= world.maxBuildHeight()) {
                return null;
            }
            if (!isAirOrWater(world, x, y, z)) {
                break;
            }
        }
        return isSolid(world, x, y, z) ? Integer.valueOf(y) : null;
    }

    private static Direction tipDirection(WorldAccess world, Random random,
            int x, int y, int z) {
        boolean ceiling = V118DripstoneFeature.isDripstoneBase(
            world.getState(x, y + 1, z));
        boolean floor = V118DripstoneFeature.isDripstoneBase(
            world.getState(x, y - 1, z));
        if (ceiling && floor) {
            return random.nextBoolean() ? Direction.DOWN : Direction.UP;
        }
        if (ceiling) {
            return Direction.DOWN;
        }
        return floor ? Direction.UP : null;
    }

    private static void createPatch(WorldAccess world, Random random,
            int baseX, int baseY, int baseZ) {
        V118DripstoneFeature.placeDripstoneBlockIfPossible(world, baseX, baseY, baseZ);
        for (Direction direction : HORIZONTAL) {
            if (random.nextFloat() > DIRECTIONAL_SPREAD_CHANCE) {
                continue;
            }
            int x = baseX + direction.stepX();
            int y = baseY + direction.stepY();
            int z = baseZ + direction.stepZ();
            V118DripstoneFeature.placeDripstoneBlockIfPossible(world, x, y, z);
            if (random.nextFloat() > SPREAD_RADIUS_TWO_CHANCE) {
                continue;
            }
            Direction radiusTwo = V118DripstoneFeature.randomDirection(random);
            x += radiusTwo.stepX();
            y += radiusTwo.stepY();
            z += radiusTwo.stepZ();
            V118DripstoneFeature.placeDripstoneBlockIfPossible(world, x, y, z);
            if (random.nextFloat() > SPREAD_RADIUS_THREE_CHANCE) {
                continue;
            }
            Direction radiusThree = V118DripstoneFeature.randomDirection(random);
            x += radiusThree.stepX();
            y += radiusThree.stepY();
            z += radiusThree.stepZ();
            V118DripstoneFeature.placeDripstoneBlockIfPossible(world, x, y, z);
        }
    }

    private static boolean isAirOrWater(WorldAccess world, int x, int y, int z) {
        return V118DripstoneFeature.isEmptyOrWater(world.getState(x, y, z));
    }

    private static boolean isSolid(WorldAccess world, int x, int y, int z) {
        return world.isSolidAt(x, y, z);
    }
}

package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.Random;

/** Shared, dependency-free Java 8 port of Java 1.18.2's dripstone feature primitives. */
public final class V118DripstoneFeature {
    private static final Direction[] RANDOM_DIRECTIONS = Direction.values();

    private V118DripstoneFeature() {
    }

    static Column scanColumn(WorldAccess world, int x, int y, int z, int range,
            EmptyPredicate empty, BoundaryPredicate boundary) {
        if (!empty.test(world.getState(x, y, z))) {
            return null;
        }
        Integer ceiling = scanDirection(world, x, y, z, range, 1, empty, boundary);
        Integer floor = scanDirection(world, x, y, z, range, -1, empty, boundary);
        return new Column(floor, ceiling);
    }

    private static Integer scanDirection(WorldAccess world, int x, int originY, int z,
            int range, int step, EmptyPredicate empty, BoundaryPredicate boundary) {
        int y = originY;
        for (int distance = 1; distance < range
                && empty.test(world.getState(x, y, z)); ++distance) {
            y += step;
        }
        return boundary.test(world.getState(x, y, z)) ? Integer.valueOf(y) : null;
    }

    static boolean isEmptyOrWater(State state) {
        return state == State.AIR || state == State.WATER;
    }

    static boolean isEmptyOrWaterOrLava(State state) {
        return state == State.AIR || state == State.WATER || state == State.LAVA;
    }

    static boolean isNeitherEmptyNorWater(State state) {
        return state != State.AIR && state != State.WATER;
    }

    static boolean isDripstoneBase(State state) {
        return state == State.DRIPSTONE_BLOCK || state == State.BASE_STONE;
    }

    static boolean isDripstoneBaseOrLava(State state) {
        return isDripstoneBase(state) || state == State.LAVA;
    }

    static boolean placeDripstoneBlockIfPossible(WorldAccess world, int x, int y, int z) {
        if (world.getState(x, y, z) != State.BASE_STONE) {
            return false;
        }
        world.setDripstoneBlock(x, y, z);
        return true;
    }

    static void growPointedDripstone(WorldAccess world, int x, int y, int z,
            Direction direction, int length, boolean merge) {
        if (!isDripstoneBase(world.getState(x - direction.stepX, y - direction.stepY,
                z - direction.stepZ))) {
            return;
        }
        int cursorX = x;
        int cursorY = y;
        int cursorZ = z;
        if (length >= 3) {
            setPointed(world, cursorX, cursorY, cursorZ, direction, Thickness.BASE);
            cursorX += direction.stepX;
            cursorY += direction.stepY;
            cursorZ += direction.stepZ;
            for (int middle = 0; middle < length - 3; ++middle) {
                setPointed(world, cursorX, cursorY, cursorZ, direction, Thickness.MIDDLE);
                cursorX += direction.stepX;
                cursorY += direction.stepY;
                cursorZ += direction.stepZ;
            }
        }
        if (length >= 2) {
            setPointed(world, cursorX, cursorY, cursorZ, direction, Thickness.FRUSTUM);
            cursorX += direction.stepX;
            cursorY += direction.stepY;
            cursorZ += direction.stepZ;
        }
        if (length >= 1) {
            setPointed(world, cursorX, cursorY, cursorZ, direction,
                merge ? Thickness.TIP_MERGE : Thickness.TIP);
        }
    }

    private static void setPointed(WorldAccess world, int x, int y, int z,
            Direction direction, Thickness thickness) {
        world.setPointedDripstone(x, y, z, direction, thickness,
            world.isWaterAt(x, y, z));
    }

    static Direction randomDirection(Random random) {
        return RANDOM_DIRECTIONS[random.nextInt(RANDOM_DIRECTIONS.length)];
    }

    static int randomBetweenInclusive(Random random, int minimum, int maximum) {
        return random.nextInt(maximum - minimum + 1) + minimum;
    }

    static float randomBetween(Random random, float minimum, float maximum) {
        return random.nextFloat() * (maximum - minimum) + minimum;
    }

    static int uniformInt(Random random, int minimum, int maximum) {
        return randomBetweenInclusive(random, minimum, maximum);
    }

    static float uniformFloat(Random random, float minimum, float maximum) {
        return randomBetween(random, minimum, maximum);
    }

    static float clampedNormalFloat(Random random, float mean, float deviation,
            float minimum, float maximum) {
        float sampled = mean + (float) random.nextGaussian() * deviation;
        return Math.max(minimum, Math.min(maximum, sampled));
    }

    static int clampedNormalInt(Random random, float mean, float deviation,
            int minimum, int maximum) {
        return (int) Math.max(minimum, Math.min(maximum,
            mean + (float) random.nextGaussian() * deviation));
    }

    static float clampedMap(float value, float inputMinimum, float inputMaximum,
            float outputMinimum, float outputMaximum) {
        float delta = (value - inputMinimum) / (inputMaximum - inputMinimum);
        if (delta < 0.0F) {
            return outputMinimum;
        }
        if (delta > 1.0F) {
            return outputMaximum;
        }
        return outputMinimum + delta * (outputMaximum - outputMinimum);
    }

    static double clampedMap(double value, double inputMinimum, double inputMaximum,
            double outputMinimum, double outputMaximum) {
        double delta = (value - inputMinimum) / (inputMaximum - inputMinimum);
        if (delta < 0.0D) {
            return outputMinimum;
        }
        if (delta > 1.0D) {
            return outputMaximum;
        }
        return outputMinimum + delta * (outputMaximum - outputMinimum);
    }

    public enum State {
        AIR,
        WATER,
        LAVA,
        BASE_STONE,
        DRIPSTONE_BLOCK,
        POINTED_DRIPSTONE,
        OTHER
    }

    public enum Direction {
        DOWN(0, -1, 0),
        UP(0, 1, 0),
        NORTH(0, 0, -1),
        SOUTH(0, 0, 1),
        WEST(-1, 0, 0),
        EAST(1, 0, 0);

        private final int stepX;
        private final int stepY;
        private final int stepZ;

        Direction(int stepX, int stepY, int stepZ) {
            this.stepX = stepX;
            this.stepY = stepY;
            this.stepZ = stepZ;
        }

        public int stepX() {
            return stepX;
        }

        public int stepY() {
            return stepY;
        }

        public int stepZ() {
            return stepZ;
        }

        public Direction opposite() {
            switch (this) {
                case DOWN:
                    return UP;
                case UP:
                    return DOWN;
                case NORTH:
                    return SOUTH;
                case SOUTH:
                    return NORTH;
                case WEST:
                    return EAST;
                case EAST:
                    return WEST;
                default:
                    throw new AssertionError(this);
            }
        }
    }

    public enum Thickness {
        TIP_MERGE,
        TIP,
        FRUSTUM,
        MIDDLE,
        BASE
    }

    static final class Column {
        private final Integer floor;
        private final Integer ceiling;

        Column(Integer floor, Integer ceiling) {
            this.floor = floor;
            this.ceiling = ceiling;
        }

        Integer floor() {
            return floor;
        }

        Integer ceiling() {
            return ceiling;
        }

        Integer height() {
            return floor == null || ceiling == null ? null
                : Integer.valueOf(ceiling - floor - 1);
        }

        Column withFloor(Integer replacement) {
            return new Column(replacement, ceiling);
        }
    }

    private interface EmptyPredicate {
        boolean test(State state);
    }

    private interface BoundaryPredicate {
        boolean test(State state);
    }

    static final EmptyPredicate EMPTY_OR_WATER = new EmptyPredicate() {
        @Override
        public boolean test(State state) {
            return isEmptyOrWater(state);
        }
    };

    static final BoundaryPredicate DRIPSTONE_BASE_OR_LAVA = new BoundaryPredicate() {
        @Override
        public boolean test(State state) {
            return isDripstoneBaseOrLava(state);
        }
    };

    static final BoundaryPredicate NEITHER_EMPTY_NOR_WATER = new BoundaryPredicate() {
        @Override
        public boolean test(State state) {
            return isNeitherEmptyNorWater(state);
        }
    };

    public interface WorldAccess {
        State getState(int blockX, int blockY, int blockZ);

        default boolean isWaterAt(int blockX, int blockY, int blockZ) {
            return getState(blockX, blockY, blockZ) == State.WATER;
        }

        default boolean isSolidAt(int blockX, int blockY, int blockZ) {
            State state = getState(blockX, blockY, blockZ);
            return state != State.AIR && state != State.WATER && state != State.LAVA;
        }

        void setDripstoneBlock(int blockX, int blockY, int blockZ);

        void setPointedDripstone(int blockX, int blockY, int blockZ,
                Direction direction, Thickness thickness, boolean waterlogged);

        void setWater(int blockX, int blockY, int blockZ);

        int worldSurfaceHeight(int blockX, int blockZ);

        V118Biome biomeAt(int blockX, int blockY, int blockZ);

        int minBuildHeight();

        int maxBuildHeight();
    }
}

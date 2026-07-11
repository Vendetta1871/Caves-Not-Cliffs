package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** Dependency-free Java 8 port of Java 1.18.2's configured amethyst geode feature. */
public final class V118GeodeFeature {
    private static final int MIN_GEN_OFFSET = -16;
    private static final int MAX_GEN_OFFSET = 16;
    private static final int INVALID_BLOCK_THRESHOLD = 1;
    private static final double FILLING = 1.7D;
    private static final double INNER_LAYER = 2.2D;
    private static final double MIDDLE_LAYER = 3.2D;
    private static final double OUTER_LAYER = 4.2D;
    private static final double GENERATE_CRACK_CHANCE = 0.95D;
    private static final double BASE_CRACK_SIZE = 2.0D;
    private static final int CRACK_POINT_OFFSET = 2;
    private static final double POTENTIAL_PLACEMENT_CHANCE = 0.35D;
    private static final double ALTERNATE_INNER_LAYER_CHANCE = 0.083D;
    private static final double NOISE_MULTIPLIER = 0.05D;
    private static final int OUTER_WALL_DISTANCE_MAX = 6;
    private static final Material[] INNER_PLACEMENTS = {
        Material.SMALL_AMETHYST_BUD,
        Material.MEDIUM_AMETHYST_BUD,
        Material.LARGE_AMETHYST_BUD,
        Material.AMETHYST_CLUSTER
    };

    private V118GeodeFeature() {
    }

    public static boolean place(WorldAccess world, Random random, long worldSeed,
            int originX, int originY, int originZ) {
        if (world == null || random == null) {
            throw new NullPointerException("world and random are required");
        }
        List<Point> distribution = new ArrayList<Point>();
        int distributionPoints = uniform(random, 3, 4);
        NormalNoise noise = NormalNoise.create(new LegacyRandomSource(worldSeed), -4, 1.0D);
        List<Point> crackPoints = new ArrayList<Point>();
        double pointRatio = (double) distributionPoints / OUTER_WALL_DISTANCE_MAX;
        double fillingThreshold = 1.0D / Math.sqrt(FILLING);
        double innerThreshold = 1.0D / Math.sqrt(INNER_LAYER + pointRatio);
        double middleThreshold = 1.0D / Math.sqrt(MIDDLE_LAYER + pointRatio);
        double outerThreshold = 1.0D / Math.sqrt(OUTER_LAYER + pointRatio);
        double crackThreshold = 1.0D / Math.sqrt(BASE_CRACK_SIZE
            + random.nextDouble() / 2.0D + (distributionPoints > 3 ? pointRatio : 0.0D));
        boolean generateCrack = random.nextFloat() < GENERATE_CRACK_CHANCE;

        int invalidBlocks = 0;
        for (int index = 0; index < distributionPoints; ++index) {
            int pointX = originX + uniform(random, 4, 6);
            int pointY = originY + uniform(random, 4, 6);
            int pointZ = originZ + uniform(random, 4, 6);
            if ((world.isAir(pointX, pointY, pointZ)
                    || world.isGeodeInvalid(pointX, pointY, pointZ))
                    && ++invalidBlocks > INVALID_BLOCK_THRESHOLD) {
                return false;
            }
            distribution.add(new Point(pointX, pointY, pointZ, uniform(random, 1, 2)));
        }

        if (generateCrack) {
            int direction = random.nextInt(4);
            int distance = distributionPoints * 2 + 1;
            if (direction == 0) {
                addCrackColumn(crackPoints, originX + distance, originY, originZ);
            } else if (direction == 1) {
                addCrackColumn(crackPoints, originX, originY, originZ + distance);
            } else if (direction == 2) {
                addCrackColumn(crackPoints, originX + distance, originY,
                    originZ + distance);
            } else {
                addCrackColumn(crackPoints, originX, originY, originZ);
            }
        }

        List<Position> potentialPlacements = new ArrayList<Position>();
        for (int z = originZ + MIN_GEN_OFFSET; z <= originZ + MAX_GEN_OFFSET; ++z) {
            for (int y = originY + MIN_GEN_OFFSET; y <= originY + MAX_GEN_OFFSET; ++y) {
                for (int x = originX + MIN_GEN_OFFSET; x <= originX + MAX_GEN_OFFSET; ++x) {
                    double noiseValue = noise.getValue(x, y, z) * NOISE_MULTIPLIER;
                    double distributionValue = sumInverseDistance(distribution, x, y, z,
                        noiseValue);
                    if (distributionValue < outerThreshold) {
                        continue;
                    }
                    double crackValue = sumInverseDistance(crackPoints, x, y, z, noiseValue);
                    if (generateCrack && crackValue >= crackThreshold
                            && distributionValue < fillingThreshold) {
                        safeSet(world, x, y, z, State.AIR);
                        for (Direction direction : Direction.values()) {
                            int neighborX = x + direction.stepX;
                            int neighborY = y + direction.stepY;
                            int neighborZ = z + direction.stepZ;
                            if (world.hasFluid(neighborX, neighborY, neighborZ)) {
                                world.scheduleFluidUpdate(neighborX, neighborY, neighborZ);
                            }
                        }
                    } else if (distributionValue >= fillingThreshold) {
                        safeSet(world, x, y, z, State.AIR);
                    } else if (distributionValue >= innerThreshold) {
                        boolean budding = random.nextFloat() < ALTERNATE_INNER_LAYER_CHANCE;
                        safeSet(world, x, y, z, budding ? State.BUDDING_AMETHYST
                            : State.AMETHYST_BLOCK);
                        if (budding && random.nextFloat() < POTENTIAL_PLACEMENT_CHANCE) {
                            potentialPlacements.add(new Position(x, y, z));
                        }
                    } else if (distributionValue >= middleThreshold) {
                        safeSet(world, x, y, z, State.CALCITE);
                    } else {
                        safeSet(world, x, y, z, State.SMOOTH_BASALT);
                    }
                }
            }
        }

        for (Position placement : potentialPlacements) {
            Material material = INNER_PLACEMENTS[random.nextInt(INNER_PLACEMENTS.length)];
            for (Direction direction : Direction.values()) {
                int x = placement.x + direction.stepX;
                int y = placement.y + direction.stepY;
                int z = placement.z + direction.stepZ;
                if (!world.canClusterGrowAt(x, y, z)) {
                    continue;
                }
                safeSet(world, x, y, z, new State(material, direction,
                    world.isWaterSource(x, y, z)));
                break;
            }
        }
        return true;
    }

    private static void addCrackColumn(List<Point> points, int x, int originY, int z) {
        points.add(new Point(x, originY + 7, z, CRACK_POINT_OFFSET));
        points.add(new Point(x, originY + 5, z, CRACK_POINT_OFFSET));
        points.add(new Point(x, originY + 1, z, CRACK_POINT_OFFSET));
    }

    private static double sumInverseDistance(List<Point> points, int x, int y, int z,
            double noiseValue) {
        double result = 0.0D;
        for (Point point : points) {
            double deltaX = (double) x - point.x;
            double deltaY = (double) y - point.y;
            double deltaZ = (double) z - point.z;
            result += WorldgenMath.fastInvSqrt(deltaX * deltaX + deltaY * deltaY
                + deltaZ * deltaZ + point.offset) + noiseValue;
        }
        return result;
    }

    private static void safeSet(WorldAccess world, int x, int y, int z, State state) {
        if (world.canReplace(x, y, z)) {
            world.setState(x, y, z, state);
        }
    }

    private static int uniform(Random random, int minimum, int maximum) {
        return random.nextInt(maximum - minimum + 1) + minimum;
    }

    public enum Material {
        AIR,
        AMETHYST_BLOCK,
        BUDDING_AMETHYST,
        CALCITE,
        SMOOTH_BASALT,
        SMALL_AMETHYST_BUD,
        MEDIUM_AMETHYST_BUD,
        LARGE_AMETHYST_BUD,
        AMETHYST_CLUSTER;

        public boolean hasFacing() {
            return ordinal() >= SMALL_AMETHYST_BUD.ordinal();
        }
    }

    /** Declaration order is the exact 1.18.2 {@code Direction.values()} order. */
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

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final class State {
        public static final State AIR = new State(Material.AIR, null, false);
        public static final State AMETHYST_BLOCK =
            new State(Material.AMETHYST_BLOCK, null, false);
        public static final State BUDDING_AMETHYST =
            new State(Material.BUDDING_AMETHYST, null, false);
        public static final State CALCITE = new State(Material.CALCITE, null, false);
        public static final State SMOOTH_BASALT =
            new State(Material.SMOOTH_BASALT, null, false);

        private final Material material;
        private final Direction facing;
        private final boolean waterlogged;

        public State(Material material, Direction facing, boolean waterlogged) {
            if (material == null) {
                throw new NullPointerException("material");
            }
            if (material.hasFacing() != (facing != null)) {
                throw new IllegalArgumentException("Facing contract does not match " + material);
            }
            if (waterlogged && !material.hasFacing()) {
                throw new IllegalArgumentException("Only amethyst growth can be waterlogged");
            }
            this.material = material;
            this.facing = facing;
            this.waterlogged = waterlogged;
        }

        public Material material() {
            return material;
        }

        public Direction facing() {
            return facing;
        }

        public boolean waterlogged() {
            return waterlogged;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof State)) {
                return false;
            }
            State other = (State) object;
            return material == other.material && facing == other.facing
                && waterlogged == other.waterlogged;
        }

        @Override
        public int hashCode() {
            int result = 31 * material.hashCode() + (facing == null ? 0 : facing.hashCode());
            return 31 * result + (waterlogged ? 1 : 0);
        }
    }

    public interface WorldAccess {
        boolean isAir(int blockX, int blockY, int blockZ);

        boolean isGeodeInvalid(int blockX, int blockY, int blockZ);

        boolean canReplace(int blockX, int blockY, int blockZ);

        boolean canClusterGrowAt(int blockX, int blockY, int blockZ);

        boolean isWaterSource(int blockX, int blockY, int blockZ);

        boolean hasFluid(int blockX, int blockY, int blockZ);

        void setState(int blockX, int blockY, int blockZ, State state);

        default void scheduleFluidUpdate(int blockX, int blockY, int blockZ) {
        }
    }

    private static final class Point {
        private final int x;
        private final int y;
        private final int z;
        private final int offset;

        private Point(int x, int y, int z, int offset) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.offset = offset;
        }
    }

    private static final class Position {
        private final int x;
        private final int y;
        private final int z;

        private Position(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}

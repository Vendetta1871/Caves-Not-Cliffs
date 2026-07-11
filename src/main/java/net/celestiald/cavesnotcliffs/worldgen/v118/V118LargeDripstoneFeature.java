package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.Random;

import static net.celestiald.cavesnotcliffs.worldgen.v118.V118DripstoneFeature.Direction;
import static net.celestiald.cavesnotcliffs.worldgen.v118.V118DripstoneFeature.State;
import static net.celestiald.cavesnotcliffs.worldgen.v118.V118DripstoneFeature.WorldAccess;

/** Exact Java 8 port of Java 1.18.2's configured {@code LargeDripstoneFeature}. */
public final class V118LargeDripstoneFeature {
    public static final int FLOOR_TO_CEILING_SEARCH_RANGE = 30;
    public static final int MIN_COLUMN_RADIUS = 3;
    public static final int MAX_COLUMN_RADIUS = 19;
    public static final float MIN_HEIGHT_SCALE = 0.4F;
    public static final float MAX_HEIGHT_SCALE = 2.0F;
    public static final float MAX_RADIUS_TO_CAVE_HEIGHT_RATIO = 0.33F;
    public static final float MIN_STALACTITE_BLUNTNESS = 0.3F;
    public static final float MAX_STALACTITE_BLUNTNESS = 0.9F;
    public static final float MIN_STALAGMITE_BLUNTNESS = 0.4F;
    public static final float MAX_STALAGMITE_BLUNTNESS = 1.0F;
    public static final float MIN_WIND_SPEED = 0.0F;
    public static final float MAX_WIND_SPEED = 0.3F;
    public static final int MIN_RADIUS_FOR_WIND = 4;
    public static final float MIN_BLUNTNESS_FOR_WIND = 0.6F;

    private V118LargeDripstoneFeature() {
    }

    public static boolean place(WorldAccess world, Random random,
            int originX, int originY, int originZ) {
        if (!V118DripstoneFeature.isEmptyOrWater(
                world.getState(originX, originY, originZ))) {
            return false;
        }
        V118DripstoneFeature.Column column = V118DripstoneFeature.scanColumn(world,
            originX, originY, originZ, FLOOR_TO_CEILING_SEARCH_RANGE,
            V118DripstoneFeature.EMPTY_OR_WATER,
            V118DripstoneFeature.DRIPSTONE_BASE_OR_LAVA);
        if (column == null || column.floor() == null || column.ceiling() == null
                || column.height().intValue() < 4) {
            return false;
        }

        int maximumFromCave = (int) ((float) column.height().intValue()
            * MAX_RADIUS_TO_CAVE_HEIGHT_RATIO);
        int maximumRadius = Math.max(MIN_COLUMN_RADIUS,
            Math.min(MAX_COLUMN_RADIUS, maximumFromCave));
        int radius = V118DripstoneFeature.randomBetweenInclusive(random,
            MIN_COLUMN_RADIUS, maximumRadius);
        LargeDripstone stalactite = makeDripstone(originX, column.ceiling() - 1, originZ,
            false, random, radius, MIN_STALACTITE_BLUNTNESS,
            MAX_STALACTITE_BLUNTNESS);
        LargeDripstone stalagmite = makeDripstone(originX, column.floor() + 1, originZ,
            true, random, radius, MIN_STALAGMITE_BLUNTNESS,
            MAX_STALAGMITE_BLUNTNESS);
        WindOffsetter wind = stalactite.isSuitableForWind()
                && stalagmite.isSuitableForWind()
            ? new WindOffsetter(originY, random) : WindOffsetter.noWind();
        boolean placeStalactite = stalactite.moveBackAndShrink(world, wind);
        boolean placeStalagmite = stalagmite.moveBackAndShrink(world, wind);
        if (placeStalactite) {
            stalactite.placeBlocks(world, random, wind);
        }
        if (placeStalagmite) {
            stalagmite.placeBlocks(world, random, wind);
        }
        return true;
    }

    static double getDripstoneHeight(double radiusPosition, double radius,
            double scale, double bluntness) {
        if (radiusPosition < bluntness) {
            radiusPosition = bluntness;
        }
        double normalized = radiusPosition / radius * 0.384D;
        double powerFourThirds = 0.75D * Math.pow(normalized, 4.0D / 3.0D);
        double powerTwoThirds = Math.pow(normalized, 2.0D / 3.0D);
        double logarithm = (1.0D / 3.0D) * Math.log(normalized);
        double result = scale * (powerFourThirds - powerTwoThirds - logarithm);
        return Math.max(result, 0.0D) / 0.384D * radius;
    }

    private static LargeDripstone makeDripstone(int x, int y, int z, boolean pointingUp,
            Random random, int radius, float minimumBluntness, float maximumBluntness) {
        double bluntness = V118DripstoneFeature.uniformFloat(random,
            minimumBluntness, maximumBluntness);
        double scale = V118DripstoneFeature.uniformFloat(random,
            MIN_HEIGHT_SCALE, MAX_HEIGHT_SCALE);
        return new LargeDripstone(x, y, z, pointingUp, radius, bluntness, scale);
    }

    private static final class LargeDripstone {
        private int rootX;
        private int rootY;
        private int rootZ;
        private final boolean pointingUp;
        private int radius;
        private final double bluntness;
        private final double scale;

        private LargeDripstone(int rootX, int rootY, int rootZ, boolean pointingUp,
                int radius, double bluntness, double scale) {
            this.rootX = rootX;
            this.rootY = rootY;
            this.rootZ = rootZ;
            this.pointingUp = pointingUp;
            this.radius = radius;
            this.bluntness = bluntness;
            this.scale = scale;
        }

        private int height() {
            return heightAtRadius(0.0F);
        }

        private int heightAtRadius(float position) {
            return (int) getDripstoneHeight(position, radius, scale, bluntness);
        }

        private boolean moveBackAndShrink(WorldAccess world, WindOffsetter wind) {
            while (radius > 1) {
                int cursorY = rootY;
                int search = Math.min(10, height());
                for (int distance = 0; distance < search; ++distance) {
                    if (world.getState(rootX, cursorY, rootZ) == State.LAVA) {
                        return false;
                    }
                    Position offset = wind.offset(rootX, cursorY, rootZ);
                    if (circleMostlyEmbedded(world, offset.x, offset.y, offset.z, radius)) {
                        rootY = cursorY;
                        return true;
                    }
                    cursorY += pointingUp ? -1 : 1;
                }
                radius /= 2;
            }
            return false;
        }

        private void placeBlocks(WorldAccess world, Random random, WindOffsetter wind) {
            for (int offsetX = -radius; offsetX <= radius; ++offsetX) {
                column:
                for (int offsetZ = -radius; offsetZ <= radius; ++offsetZ) {
                    float radialDistance = (float) Math.sqrt(
                        offsetX * offsetX + offsetZ * offsetZ);
                    if (radialDistance > (float) radius) {
                        continue;
                    }
                    int height = heightAtRadius(radialDistance);
                    if (height <= 0) {
                        continue;
                    }
                    if ((double) random.nextFloat() < 0.2D) {
                        height = (int) ((float) height
                            * V118DripstoneFeature.randomBetween(random, 0.8F, 1.0F));
                    }
                    int cursorY = rootY;
                    boolean enteredCave = false;
                    int surfaceHeight = pointingUp
                        ? world.worldSurfaceHeight(rootX + offsetX, rootZ + offsetZ)
                        : Integer.MAX_VALUE;
                    for (int distance = 0; distance < height
                            && cursorY < surfaceHeight; ++distance) {
                        Position position = wind.offset(rootX + offsetX, cursorY,
                            rootZ + offsetZ);
                        State state = world.getState(position.x, position.y, position.z);
                        if (V118DripstoneFeature.isEmptyOrWaterOrLava(state)) {
                            enteredCave = true;
                            world.setDripstoneBlock(position.x, position.y, position.z);
                        } else if (enteredCave && state == State.BASE_STONE) {
                            continue column;
                        }
                        cursorY += pointingUp ? 1 : -1;
                    }
                }
            }
        }

        private boolean isSuitableForWind() {
            return radius >= MIN_RADIUS_FOR_WIND
                && bluntness >= (double) MIN_BLUNTNESS_FOR_WIND;
        }
    }

    private static boolean circleMostlyEmbedded(WorldAccess world, int x, int y, int z,
            int radius) {
        if (V118DripstoneFeature.isEmptyOrWaterOrLava(world.getState(x, y, z))) {
            return false;
        }
        float step = 6.0F / (float) radius;
        for (float angle = 0.0F; angle < (float) Math.PI * 2.0F; angle += step) {
            int offsetX = (int) ((float) Math.cos(angle) * (float) radius);
            int offsetZ = (int) ((float) Math.sin(angle) * (float) radius);
            if (V118DripstoneFeature.isEmptyOrWaterOrLava(
                    world.getState(x + offsetX, y, z + offsetZ))) {
                return false;
            }
        }
        return true;
    }

    private static final class WindOffsetter {
        private final int originY;
        private final boolean enabled;
        private final double speedX;
        private final double speedZ;

        private WindOffsetter(int originY, Random random) {
            this.originY = originY;
            float speed = V118DripstoneFeature.uniformFloat(random,
                MIN_WIND_SPEED, MAX_WIND_SPEED);
            float angle = V118DripstoneFeature.randomBetween(random, 0.0F, (float) Math.PI);
            speedX = (float) Math.cos(angle) * speed;
            speedZ = (float) Math.sin(angle) * speed;
            enabled = true;
        }

        private WindOffsetter() {
            originY = 0;
            enabled = false;
            speedX = 0.0D;
            speedZ = 0.0D;
        }

        private static WindOffsetter noWind() {
            return new WindOffsetter();
        }

        private Position offset(int x, int y, int z) {
            if (!enabled) {
                return new Position(x, y, z);
            }
            int verticalDistance = originY - y;
            return new Position(floor(x + speedX * verticalDistance), y,
                floor(z + speedZ * verticalDistance));
        }

        private static int floor(double value) {
            return (int) Math.floor(value);
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

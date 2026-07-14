package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.Random;

/** Exact Java 1.18.2 Overworld cave-carver geometry and random-consumption order. */
public final class V118CaveWorldCarver
        extends V118WorldCarver<V118CaveWorldCarver.Configuration> {
    private static final int CAVE_BOUND = 15;

    @Override
    public boolean carve(final Configuration configuration, WorldAccess world, Random random,
            int sourceChunkX, int sourceChunkZ, V118CarvingMask mask) {
        int length = (RANGE * 2 - 1) * 16;
        int caveCount = random.nextInt(random.nextInt(random.nextInt(CAVE_BOUND) + 1) + 1);
        for (int cave = 0; cave < caveCount; ++cave) {
            double x = (sourceChunkX << 4) + random.nextInt(16);
            double y = configuration.sampleY(random);
            double z = (sourceChunkZ << 4) + random.nextInt(16);
            double horizontalMultiplier = configuration.sampleHorizontalRadius(random);
            double verticalMultiplier = configuration.sampleVerticalRadius(random);
            final double floor = configuration.sampleFloor(random);
            CarveSkipChecker skip = new CarveSkipChecker() {
                @Override
                public boolean shouldSkip(double normalizedX, double normalizedY,
                        double normalizedZ, int blockY) {
                    return normalizedY <= floor || normalizedX * normalizedX
                        + normalizedY * normalizedY + normalizedZ * normalizedZ >= 1.0D;
                }
            };
            int tunnelCount = 1;
            if (random.nextInt(4) == 0) {
                double yScale = configuration.sampleYScale(random);
                float thickness = 1.0F + random.nextFloat() * 6.0F;
                createRoom(configuration, world, x, y, z, thickness, yScale, mask, skip);
                tunnelCount += random.nextInt(4);
            }
            for (int tunnel = 0; tunnel < tunnelCount; ++tunnel) {
                float horizontalAngle = random.nextFloat() * ((float) Math.PI * 2.0F);
                float verticalAngle = (random.nextFloat() - 0.5F) / 4.0F;
                float thickness = getThickness(random);
                int tunnelLength = length - random.nextInt(length / 4);
                createTunnel(configuration, world, random.nextLong(), x, y, z,
                    horizontalMultiplier, verticalMultiplier, thickness, horizontalAngle,
                    verticalAngle, 0, tunnelLength, 1.0D, mask, skip);
            }
        }
        return true;
    }

    private static float getThickness(Random random) {
        float thickness = random.nextFloat() * 2.0F + random.nextFloat();
        if (random.nextInt(10) == 0) {
            thickness *= random.nextFloat() * random.nextFloat() * 3.0F + 1.0F;
        }
        return thickness;
    }

    private void createRoom(Configuration configuration, WorldAccess world, double x,
            double y, double z, float thickness, double yScale, V118CarvingMask mask,
            CarveSkipChecker skip) {
        double horizontalRadius = 1.5D + sin(1.5707964F) * thickness;
        carveEllipsoid(configuration, world, x + 1.0D, y, z, horizontalRadius,
            horizontalRadius * yScale, mask, skip);
    }

    private void createTunnel(Configuration configuration, WorldAccess world, long seed,
            double x, double y, double z, double horizontalMultiplier,
            double verticalMultiplier, float thickness, float horizontalAngle,
            float verticalAngle, int startStep, int length, double yScale,
            V118CarvingMask mask, CarveSkipChecker skip) {
        Random random = new Random(seed);
        int branchStep = random.nextInt(length / 2) + length / 4;
        boolean gentleVertical = random.nextInt(6) == 0;
        float horizontalVelocity = 0.0F;
        float verticalVelocity = 0.0F;
        for (int step = startStep; step < length; ++step) {
            double horizontalRadius = 1.5D + sin((float) Math.PI * (float) step
                / (float) length) * thickness;
            double verticalRadius = horizontalRadius * yScale;
            float verticalCosine = cos(verticalAngle);
            x += cos(horizontalAngle) * verticalCosine;
            y += sin(verticalAngle);
            z += sin(horizontalAngle) * verticalCosine;
            verticalAngle *= gentleVertical ? 0.92F : 0.7F;
            verticalAngle += verticalVelocity * 0.1F;
            horizontalAngle += horizontalVelocity * 0.1F;
            verticalVelocity *= 0.9F;
            horizontalVelocity *= 0.75F;
            verticalVelocity += (random.nextFloat() - random.nextFloat())
                * random.nextFloat() * 2.0F;
            horizontalVelocity += (random.nextFloat() - random.nextFloat())
                * random.nextFloat() * 4.0F;
            if (step == branchStep && thickness > 1.0F) {
                createTunnel(configuration, world, random.nextLong(), x, y, z,
                    horizontalMultiplier, verticalMultiplier,
                    random.nextFloat() * 0.5F + 0.5F,
                    horizontalAngle - 1.5707964F, verticalAngle / 3.0F,
                    step, length, 1.0D, mask, skip);
                createTunnel(configuration, world, random.nextLong(), x, y, z,
                    horizontalMultiplier, verticalMultiplier,
                    random.nextFloat() * 0.5F + 0.5F,
                    horizontalAngle + 1.5707964F, verticalAngle / 3.0F,
                    step, length, 1.0D, mask, skip);
                return;
            }
            if (random.nextInt(4) == 0) {
                continue;
            }
            if (!canReach(world.targetChunkX(), world.targetChunkZ(), x, z, step, length,
                    thickness)) {
                return;
            }
            carveEllipsoid(configuration, world, x, y, z,
                horizontalRadius * horizontalMultiplier,
                verticalRadius * verticalMultiplier, mask, skip);
        }
    }

    /** Exact configured values from {@code net.minecraft.data.worldgen.Carvers}. */
    public static final class Configuration extends V118CarverConfiguration {
        private final float minimumHorizontalRadius;
        private final float maximumHorizontalRadius;
        private final float minimumVerticalRadius;
        private final float maximumVerticalRadius;
        private final float minimumFloor;
        private final float maximumFloor;

        Configuration(float probability, int minimumY, int maximumY, float minimumYScale,
                float maximumYScale, int lavaLevel, float minimumHorizontalRadius,
                float maximumHorizontalRadius, float minimumVerticalRadius,
                float maximumVerticalRadius, float minimumFloor, float maximumFloor) {
            super(probability, minimumY, maximumY, minimumYScale, maximumYScale, lavaLevel);
            this.minimumHorizontalRadius = minimumHorizontalRadius;
            this.maximumHorizontalRadius = maximumHorizontalRadius;
            this.minimumVerticalRadius = minimumVerticalRadius;
            this.maximumVerticalRadius = maximumVerticalRadius;
            this.minimumFloor = minimumFloor;
            this.maximumFloor = maximumFloor;
        }

        public float minimumHorizontalRadius() {
            return minimumHorizontalRadius;
        }

        public float maximumHorizontalRadius() {
            return maximumHorizontalRadius;
        }

        public float minimumVerticalRadius() {
            return minimumVerticalRadius;
        }

        public float maximumVerticalRadius() {
            return maximumVerticalRadius;
        }

        public float minimumFloor() {
            return minimumFloor;
        }

        public float maximumFloor() {
            return maximumFloor;
        }

        float sampleHorizontalRadius(Random random) {
            return uniform(random, minimumHorizontalRadius, maximumHorizontalRadius);
        }

        float sampleVerticalRadius(Random random) {
            return uniform(random, minimumVerticalRadius, maximumVerticalRadius);
        }

        float sampleFloor(Random random) {
            return uniform(random, minimumFloor, maximumFloor);
        }
    }
}

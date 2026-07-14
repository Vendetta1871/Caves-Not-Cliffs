package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.Random;

/** Exact Java 1.18.2 Overworld canyon-carver geometry and random-consumption order. */
public final class V118CanyonWorldCarver
        extends V118WorldCarver<V118CanyonWorldCarver.Configuration> {
    @Override
    public boolean carve(final Configuration configuration, WorldAccess world, Random random,
            int sourceChunkX, int sourceChunkZ, V118CarvingMask mask) {
        int maximumLength = (RANGE * 2 - 1) * 16;
        double x = (sourceChunkX << 4) + random.nextInt(16);
        double y = configuration.sampleY(random);
        double z = (sourceChunkZ << 4) + random.nextInt(16);
        float horizontalAngle = random.nextFloat() * ((float) Math.PI * 2.0F);
        float verticalAngle = configuration.sampleVerticalRotation(random);
        double yScale = configuration.sampleYScale(random);
        float thickness = configuration.sampleThickness(random);
        int length = (int) (maximumLength * configuration.sampleDistanceFactor(random));
        doCarve(configuration, world, random.nextLong(), x, y, z, thickness,
            horizontalAngle, verticalAngle, 0, length, yScale, mask);
        return true;
    }

    private void doCarve(final Configuration configuration, WorldAccess world, long seed,
            double x, double y, double z, float thickness, float horizontalAngle,
            float verticalAngle, int startStep, int length, double yScale,
            V118CarvingMask mask) {
        Random random = new Random(seed);
        final float[] widthFactors = initializeWidthFactors(world, configuration, random);
        float horizontalVelocity = 0.0F;
        float verticalVelocity = 0.0F;
        for (int step = startStep; step < length; ++step) {
            double horizontalRadius = 1.5D + sin((float) step * (float) Math.PI
                / (float) length) * thickness;
            double verticalRadius = horizontalRadius * yScale;
            horizontalRadius *= configuration.sampleHorizontalRadiusFactor(random);
            verticalRadius = updateVerticalRadius(configuration, random, verticalRadius,
                length, step);
            float verticalCosine = cos(verticalAngle);
            float verticalSine = sin(verticalAngle);
            x += cos(horizontalAngle) * verticalCosine;
            y += verticalSine;
            z += sin(horizontalAngle) * verticalCosine;
            verticalAngle *= 0.7F;
            verticalAngle += verticalVelocity * 0.05F;
            horizontalAngle += horizontalVelocity * 0.05F;
            verticalVelocity *= 0.8F;
            horizontalVelocity *= 0.5F;
            verticalVelocity += (random.nextFloat() - random.nextFloat())
                * random.nextFloat() * 2.0F;
            horizontalVelocity += (random.nextFloat() - random.nextFloat())
                * random.nextFloat() * 4.0F;
            if (random.nextInt(4) == 0) {
                continue;
            }
            if (!canReach(world.targetChunkX(), world.targetChunkZ(), x, z, step, length,
                    thickness)) {
                return;
            }
            carveEllipsoid(configuration, world, x, y, z, horizontalRadius, verticalRadius,
                mask, new CarveSkipChecker() {
                    @Override
                    public boolean shouldSkip(double normalizedX, double normalizedY,
                            double normalizedZ, int blockY) {
                        int index = blockY - world.minBuildHeight();
                        return (normalizedX * normalizedX + normalizedZ * normalizedZ)
                            * widthFactors[index - 1]
                            + normalizedY * normalizedY / 6.0D >= 1.0D;
                    }
                });
        }
    }

    private static float[] initializeWidthFactors(WorldAccess world,
            Configuration configuration, Random random) {
        int depth = world.maxBuildHeight() - world.minBuildHeight();
        float[] factors = new float[depth];
        float factor = 1.0F;
        for (int index = 0; index < depth; ++index) {
            if (index == 0 || random.nextInt(configuration.widthSmoothness()) == 0) {
                factor = 1.0F + random.nextFloat() * random.nextFloat();
            }
            factors[index] = factor * factor;
        }
        return factors;
    }

    private static double updateVerticalRadius(Configuration configuration, Random random,
            double radius, float length, float step) {
        float centerStrength = 1.0F - Math.abs(0.5F - step / length) * 2.0F;
        float factor = configuration.verticalRadiusDefaultFactor()
            + configuration.verticalRadiusCenterFactor() * centerStrength;
        return factor * radius * V118CarverConfiguration.uniform(random, 0.75F, 1.0F);
    }

    /** Exact configured values from {@code net.minecraft.data.worldgen.Carvers}. */
    public static final class Configuration extends V118CarverConfiguration {
        private final float minimumVerticalRotation;
        private final float maximumVerticalRotation;
        private final float minimumDistanceFactor;
        private final float maximumDistanceFactor;
        private final float minimumThickness;
        private final float maximumThickness;
        private final float thicknessPlateau;
        private final int widthSmoothness;
        private final float minimumHorizontalRadiusFactor;
        private final float maximumHorizontalRadiusFactor;
        private final float verticalRadiusDefaultFactor;
        private final float verticalRadiusCenterFactor;

        Configuration(float probability, int minimumY, int maximumY, float minimumYScale,
                float maximumYScale, int lavaLevel, float minimumVerticalRotation,
                float maximumVerticalRotation, float minimumDistanceFactor,
                float maximumDistanceFactor, float minimumThickness, float maximumThickness,
                float thicknessPlateau, int widthSmoothness,
                float minimumHorizontalRadiusFactor, float maximumHorizontalRadiusFactor,
                float verticalRadiusDefaultFactor, float verticalRadiusCenterFactor) {
            super(probability, minimumY, maximumY, minimumYScale, maximumYScale, lavaLevel);
            this.minimumVerticalRotation = minimumVerticalRotation;
            this.maximumVerticalRotation = maximumVerticalRotation;
            this.minimumDistanceFactor = minimumDistanceFactor;
            this.maximumDistanceFactor = maximumDistanceFactor;
            this.minimumThickness = minimumThickness;
            this.maximumThickness = maximumThickness;
            this.thicknessPlateau = thicknessPlateau;
            this.widthSmoothness = widthSmoothness;
            this.minimumHorizontalRadiusFactor = minimumHorizontalRadiusFactor;
            this.maximumHorizontalRadiusFactor = maximumHorizontalRadiusFactor;
            this.verticalRadiusDefaultFactor = verticalRadiusDefaultFactor;
            this.verticalRadiusCenterFactor = verticalRadiusCenterFactor;
        }

        public float minimumVerticalRotation() {
            return minimumVerticalRotation;
        }

        public float maximumVerticalRotation() {
            return maximumVerticalRotation;
        }

        public float minimumDistanceFactor() {
            return minimumDistanceFactor;
        }

        public float maximumDistanceFactor() {
            return maximumDistanceFactor;
        }

        public float minimumThickness() {
            return minimumThickness;
        }

        public float maximumThickness() {
            return maximumThickness;
        }

        public float thicknessPlateau() {
            return thicknessPlateau;
        }

        public int widthSmoothness() {
            return widthSmoothness;
        }

        public float minimumHorizontalRadiusFactor() {
            return minimumHorizontalRadiusFactor;
        }

        public float maximumHorizontalRadiusFactor() {
            return maximumHorizontalRadiusFactor;
        }

        public float verticalRadiusDefaultFactor() {
            return verticalRadiusDefaultFactor;
        }

        public float verticalRadiusCenterFactor() {
            return verticalRadiusCenterFactor;
        }

        float sampleVerticalRotation(Random random) {
            return uniform(random, minimumVerticalRotation, maximumVerticalRotation);
        }

        float sampleDistanceFactor(Random random) {
            return uniform(random, minimumDistanceFactor, maximumDistanceFactor);
        }

        float sampleThickness(Random random) {
            return trapezoid(random, minimumThickness, maximumThickness, thicknessPlateau);
        }

        float sampleHorizontalRadiusFactor(Random random) {
            return uniform(random, minimumHorizontalRadiusFactor,
                maximumHorizontalRadiusFactor);
        }
    }
}

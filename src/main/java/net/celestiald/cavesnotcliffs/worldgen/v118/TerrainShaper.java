package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.Locale;

/**
 * Dependency-free Java 8 port of Minecraft Java 1.18.2's Overworld terrain shaper.
 *
 * <p>The three splines consume continentalness, erosion, ridges, and weirdness. Their output is
 * used by the 1.18 density router as the terrain offset, factor, and jaggedness inputs.</p>
 */
public final class TerrainShaper {
    public static final float GLOBAL_OFFSET = -0.50375F;

    private static final ToFloatFunction<Float> NO_TRANSFORM = value -> value;

    private final Profile profile;
    private final CubicSpline<Point> offsetSampler;
    private final CubicSpline<Point> factorSampler;
    private final CubicSpline<Point> jaggednessSampler;

    private TerrainShaper(Profile profile, CubicSpline<Point> offsetSampler,
            CubicSpline<Point> factorSampler, CubicSpline<Point> jaggednessSampler) {
        this.profile = profile;
        this.offsetSampler = offsetSampler;
        this.factorSampler = factorSampler;
        this.jaggednessSampler = jaggednessSampler;
    }

    public static TerrainShaper overworld(Profile profile) {
        if (profile == null) {
            throw new NullPointerException("profile");
        }
        return overworld(profile, profile == Profile.AMPLIFIED);
    }

    /** Retains the boolean shape of the 1.18.2 factory for direct parity callers. */
    public static TerrainShaper overworld(boolean amplified) {
        return overworld(amplified ? Profile.AMPLIFIED : Profile.NORMAL, amplified);
    }

    private static TerrainShaper overworld(Profile profile, boolean amplified) {
        ToFloatFunction<Float> offsetTransform = amplified
            ? TerrainShaper::getAmplifiedOffset : NO_TRANSFORM;
        ToFloatFunction<Float> factorTransform = amplified
            ? TerrainShaper::getAmplifiedFactor : NO_TRANSFORM;
        ToFloatFunction<Float> jaggednessTransform = amplified
            ? TerrainShaper::getAmplifiedJaggedness : NO_TRANSFORM;

        CubicSpline<Point> erosionOffset0 = buildErosionOffsetSpline(-0.15F, 0.0F, 0.0F,
            0.1F, 0.0F, -0.03F, false, false, offsetTransform);
        CubicSpline<Point> erosionOffset1 = buildErosionOffsetSpline(-0.1F, 0.03F, 0.1F,
            0.1F, 0.01F, -0.03F, false, false, offsetTransform);
        CubicSpline<Point> erosionOffset2 = buildErosionOffsetSpline(-0.1F, 0.03F, 0.1F,
            0.7F, 0.01F, -0.03F, true, true, offsetTransform);
        CubicSpline<Point> erosionOffset3 = buildErosionOffsetSpline(-0.05F, 0.03F, 0.1F,
            1.0F, 0.01F, 0.01F, true, true, offsetTransform);

        CubicSpline<Point> offset = CubicSpline.builder(Coordinate.CONTINENTS, offsetTransform)
            .addPoint(-1.1F, 0.044F, 0.0F)
            .addPoint(-1.02F, -0.2222F, 0.0F)
            .addPoint(-0.51F, -0.2222F, 0.0F)
            .addPoint(-0.44F, -0.12F, 0.0F)
            .addPoint(-0.18F, -0.12F, 0.0F)
            .addPoint(-0.16F, erosionOffset0, 0.0F)
            .addPoint(-0.15F, erosionOffset0, 0.0F)
            .addPoint(-0.1F, erosionOffset1, 0.0F)
            .addPoint(0.25F, erosionOffset2, 0.0F)
            .addPoint(1.0F, erosionOffset3, 0.0F)
            .build();

        CubicSpline<Point> factor = CubicSpline.builder(Coordinate.CONTINENTS, NO_TRANSFORM)
            .addPoint(-0.19F, 3.95F, 0.0F)
            .addPoint(-0.15F, getErosionFactor(6.25F, true, NO_TRANSFORM), 0.0F)
            .addPoint(-0.1F, getErosionFactor(5.47F, true, factorTransform), 0.0F)
            .addPoint(0.03F, getErosionFactor(5.08F, true, factorTransform), 0.0F)
            .addPoint(0.06F, getErosionFactor(4.69F, false, factorTransform), 0.0F)
            .build();

        CubicSpline<Point> jaggedness = CubicSpline.builder(Coordinate.CONTINENTS,
                jaggednessTransform)
            .addPoint(-0.11F, 0.0F, 0.0F)
            .addPoint(0.03F, buildErosionJaggednessSpline(1.0F, 0.5F, 0.0F, 0.0F,
                jaggednessTransform), 0.0F)
            .addPoint(0.65F, buildErosionJaggednessSpline(1.0F, 1.0F, 1.0F, 0.0F,
                jaggednessTransform), 0.0F)
            .build();

        return new TerrainShaper(profile, offset, factor, jaggedness);
    }

    private static CubicSpline<Point> buildErosionJaggednessSpline(float ridgeJaggedness0,
            float ridgeJaggedness1, float weirdnessJaggedness0, float weirdnessJaggedness1,
            ToFloatFunction<Float> transform) {
        CubicSpline<Point> ridge0 = buildRidgeJaggednessSpline(ridgeJaggedness0,
            weirdnessJaggedness0, transform);
        CubicSpline<Point> ridge1 = buildRidgeJaggednessSpline(ridgeJaggedness1,
            weirdnessJaggedness1, transform);
        return CubicSpline.builder(Coordinate.EROSION, transform)
            .addPoint(-1.0F, ridge0, 0.0F)
            .addPoint(-0.78F, ridge1, 0.0F)
            .addPoint(-0.5775F, ridge1, 0.0F)
            .addPoint(-0.375F, 0.0F, 0.0F)
            .build();
    }

    private static CubicSpline<Point> buildRidgeJaggednessSpline(float weirdnessJaggedness0,
            float weirdnessJaggedness1, ToFloatFunction<Float> transform) {
        float ridge0 = peaksAndValleys(0.4F);
        float ridge1 = peaksAndValleys(0.56666666F);
        float ridgeMidpoint = (ridge0 + ridge1) / 2.0F;
        CubicSpline.Builder<Point> builder = CubicSpline.builder(Coordinate.RIDGES, transform)
            .addPoint(ridge0, 0.0F, 0.0F);
        if (weirdnessJaggedness1 > 0.0F) {
            builder.addPoint(ridgeMidpoint,
                buildWeirdnessJaggednessSpline(weirdnessJaggedness1, transform), 0.0F);
        } else {
            builder.addPoint(ridgeMidpoint, 0.0F, 0.0F);
        }
        if (weirdnessJaggedness0 > 0.0F) {
            builder.addPoint(1.0F,
                buildWeirdnessJaggednessSpline(weirdnessJaggedness0, transform), 0.0F);
        } else {
            builder.addPoint(1.0F, 0.0F, 0.0F);
        }
        return builder.build();
    }

    private static CubicSpline<Point> buildWeirdnessJaggednessSpline(float jaggedness,
            ToFloatFunction<Float> transform) {
        float low = 0.63F * jaggedness;
        float high = 0.3F * jaggedness;
        return CubicSpline.builder(Coordinate.WEIRDNESS, transform)
            .addPoint(-0.01F, low, 0.0F)
            .addPoint(0.01F, high, 0.0F)
            .build();
    }

    private static CubicSpline<Point> getErosionFactor(float factor, boolean hasRidges,
            ToFloatFunction<Float> transform) {
        CubicSpline<Point> erosionRidges = CubicSpline.builder(Coordinate.WEIRDNESS, transform)
            .addPoint(-0.2F, 6.3F, 0.0F)
            .addPoint(0.2F, factor, 0.0F)
            .build();
        CubicSpline.Builder<Point> builder = CubicSpline.builder(Coordinate.EROSION, transform)
            .addPoint(-0.6F, erosionRidges, 0.0F)
            .addPoint(-0.5F, CubicSpline.builder(Coordinate.WEIRDNESS, transform)
                .addPoint(-0.05F, 6.3F, 0.0F)
                .addPoint(0.05F, 2.67F, 0.0F)
                .build(), 0.0F)
            .addPoint(-0.35F, erosionRidges, 0.0F)
            .addPoint(-0.25F, erosionRidges, 0.0F)
            .addPoint(-0.1F, CubicSpline.builder(Coordinate.WEIRDNESS, transform)
                .addPoint(-0.05F, 2.67F, 0.0F)
                .addPoint(0.05F, 6.3F, 0.0F)
                .build(), 0.0F)
            .addPoint(0.03F, erosionRidges, 0.0F);
        if (hasRidges) {
            CubicSpline<Point> weirdness = CubicSpline.builder(Coordinate.WEIRDNESS, transform)
                .addPoint(0.0F, factor, 0.0F)
                .addPoint(0.1F, 0.625F, 0.0F)
                .build();
            CubicSpline<Point> ridges = CubicSpline.builder(Coordinate.RIDGES, transform)
                .addPoint(-0.9F, factor, 0.0F)
                .addPoint(-0.69F, weirdness, 0.0F)
                .build();
            builder.addPoint(0.35F, factor, 0.0F)
                .addPoint(0.45F, ridges, 0.0F)
                .addPoint(0.55F, ridges, 0.0F)
                .addPoint(0.62F, factor, 0.0F);
        } else {
            CubicSpline<Point> low = CubicSpline.builder(Coordinate.RIDGES, transform)
                .addPoint(-0.7F, erosionRidges, 0.0F)
                .addPoint(-0.15F, 1.37F, 0.0F)
                .build();
            CubicSpline<Point> high = CubicSpline.builder(Coordinate.RIDGES, transform)
                .addPoint(0.45F, erosionRidges, 0.0F)
                .addPoint(0.7F, 1.56F, 0.0F)
                .build();
            builder.addPoint(0.05F, high, 0.0F)
                .addPoint(0.4F, high, 0.0F)
                .addPoint(0.45F, low, 0.0F)
                .addPoint(0.55F, low, 0.0F)
                .addPoint(0.58F, factor, 0.0F);
        }
        return builder.build();
    }

    private static CubicSpline<Point> buildMountainRidgeSplineWithPoints(float continentalness,
            boolean forcePositive, ToFloatFunction<Float> transform) {
        CubicSpline.Builder<Point> builder = CubicSpline.builder(Coordinate.RIDGES, transform);
        float low = mountainContinentalness(-1.0F, continentalness, -0.7F);
        float high = mountainContinentalness(1.0F, continentalness, -0.7F);
        float zeroPoint = calculateMountainRidgeZeroContinentalnessPoint(continentalness);
        if (-0.65F < zeroPoint && zeroPoint < 1.0F) {
            float atNegativePoint = mountainContinentalness(-0.65F, continentalness, -0.7F);
            float atNegativeSlopePoint = mountainContinentalness(-0.75F, continentalness, -0.7F);
            float negativeSlope = calculateSlope(low, atNegativeSlopePoint, -1.0F, -0.75F);
            builder.addPoint(-1.0F, low, negativeSlope)
                .addPoint(-0.75F, atNegativeSlopePoint, 0.0F)
                .addPoint(-0.65F, atNegativePoint, 0.0F);
            float zeroValue = mountainContinentalness(zeroPoint, continentalness, -0.7F);
            float zeroSlope = calculateSlope(zeroValue, high, zeroPoint, 1.0F);
            builder.addPoint(zeroPoint - 0.01F, zeroValue, 0.0F)
                .addPoint(zeroPoint, zeroValue, zeroSlope)
                .addPoint(1.0F, high, zeroSlope);
        } else {
            float slope = calculateSlope(low, high, -1.0F, 1.0F);
            if (forcePositive) {
                builder.addPoint(-1.0F, Math.max(0.2F, low), 0.0F)
                    .addPoint(0.0F, WorldgenMath.lerp(0.5F, low, high), slope);
            } else {
                builder.addPoint(-1.0F, low, slope);
            }
            builder.addPoint(1.0F, high, slope);
        }
        return builder.build();
    }

    private static float calculateSlope(float value0, float value1, float point0, float point1) {
        return (value1 - value0) / (point1 - point0);
    }

    private static float mountainContinentalness(float ridges, float continentalness,
            float threshold) {
        float factor = 1.0F - (1.0F - continentalness) * 0.5F;
        float offset = 0.5F * (1.0F - continentalness);
        float scaledRidges = (ridges + 1.17F) * 0.46082947F;
        float result = scaledRidges * factor - offset;
        return ridges < threshold ? Math.max(result, -0.2222F) : Math.max(result, 0.0F);
    }

    private static float calculateMountainRidgeZeroContinentalnessPoint(float continentalness) {
        float factor = 1.0F - (1.0F - continentalness) * 0.5F;
        float offset = 0.5F * (1.0F - continentalness);
        return offset / (0.46082947F * factor) - 1.17F;
    }

    private static CubicSpline<Point> buildErosionOffsetSpline(float offset0, float offset1,
            float offset2, float mountainContinentalness, float offset4, float offset5,
            boolean addMidpoints, boolean forcePositive,
            ToFloatFunction<Float> transform) {
        CubicSpline<Point> mountain0 = buildMountainRidgeSplineWithPoints(
            WorldgenMath.lerp(mountainContinentalness, 0.6F, 1.5F), forcePositive, transform);
        CubicSpline<Point> mountain1 = buildMountainRidgeSplineWithPoints(
            WorldgenMath.lerp(mountainContinentalness, 0.6F, 1.0F), forcePositive, transform);
        CubicSpline<Point> mountain2 = buildMountainRidgeSplineWithPoints(
            mountainContinentalness, forcePositive, transform);
        CubicSpline<Point> ridge0 = ridgeSpline(offset0 - 0.15F,
            0.5F * mountainContinentalness, 0.5F * mountainContinentalness,
            0.5F * mountainContinentalness, 0.6F * mountainContinentalness, 0.5F, transform);
        CubicSpline<Point> ridge1 = ridgeSpline(offset0, offset4 * mountainContinentalness,
            offset1 * mountainContinentalness, 0.5F * mountainContinentalness,
            0.6F * mountainContinentalness, 0.5F, transform);
        CubicSpline<Point> ridge2 = ridgeSpline(offset0, offset4, offset4, offset1, offset2,
            0.5F, transform);
        CubicSpline<Point> ridge3 = ridgeSpline(offset0, offset4, offset4, offset1, offset2,
            0.5F, transform);
        CubicSpline<Point> ridge4 = CubicSpline.builder(Coordinate.RIDGES, transform)
            .addPoint(-1.0F, offset0, 0.0F)
            .addPoint(-0.4F, ridge2, 0.0F)
            .addPoint(0.0F, offset2 + 0.07F, 0.0F)
            .build();
        CubicSpline<Point> ridge5 = ridgeSpline(-0.02F, offset5, offset5, offset1, offset2,
            0.0F, transform);

        CubicSpline.Builder<Point> builder = CubicSpline.builder(Coordinate.EROSION, transform)
            .addPoint(-0.85F, mountain0, 0.0F)
            .addPoint(-0.7F, mountain1, 0.0F)
            .addPoint(-0.4F, mountain2, 0.0F)
            .addPoint(-0.35F, ridge0, 0.0F)
            .addPoint(-0.1F, ridge1, 0.0F)
            .addPoint(0.2F, ridge2, 0.0F);
        if (addMidpoints) {
            builder.addPoint(0.4F, ridge3, 0.0F)
                .addPoint(0.45F, ridge4, 0.0F)
                .addPoint(0.55F, ridge4, 0.0F)
                .addPoint(0.58F, ridge3, 0.0F);
        }
        return builder.addPoint(0.7F, ridge5, 0.0F).build();
    }

    private static CubicSpline<Point> ridgeSpline(float ridge0, float ridge1, float ridge2,
            float ridge3, float ridge4, float minimumSlope,
            ToFloatFunction<Float> transform) {
        float slope0 = Math.max(0.5F * (ridge1 - ridge0), minimumSlope);
        float slope1 = 5.0F * (ridge2 - ridge1);
        return CubicSpline.builder(Coordinate.RIDGES, transform)
            .addPoint(-1.0F, ridge0, slope0)
            .addPoint(-0.4F, ridge1, Math.min(slope0, slope1))
            .addPoint(0.0F, ridge2, slope1)
            .addPoint(0.4F, ridge3, 2.0F * (ridge3 - ridge2))
            .addPoint(1.0F, ridge4, 0.7F * (ridge4 - ridge3))
            .build();
    }

    private static float getAmplifiedOffset(float value) {
        return value < 0.0F ? value : value * 2.0F;
    }

    private static float getAmplifiedFactor(float value) {
        return 1.25F - 6.25F / (value + 5.0F);
    }

    private static float getAmplifiedJaggedness(float value) {
        return value * 2.0F;
    }

    public Profile profile() {
        return profile;
    }

    public CubicSpline<Point> offsetSampler() {
        return offsetSampler;
    }

    public CubicSpline<Point> factorSampler() {
        return factorSampler;
    }

    public CubicSpline<Point> jaggednessSampler() {
        return jaggednessSampler;
    }

    public float offset(Point point) {
        return offsetSampler.apply(point) + GLOBAL_OFFSET;
    }

    public float factor(Point point) {
        return factorSampler.apply(point);
    }

    public float jaggedness(Point point) {
        return jaggednessSampler.apply(point);
    }

    public static Point makePoint(float continentalness, float erosion, float weirdness) {
        return new Point(continentalness, erosion, peaksAndValleys(weirdness), weirdness);
    }

    public static float peaksAndValleys(float weirdness) {
        return -(Math.abs(Math.abs(weirdness) - 0.6666667F) - 0.33333334F) * 3.0F;
    }

    public enum Profile {
        NORMAL("normal"),
        AMPLIFIED("amplified");

        private final String serializedName;

        Profile(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public static Profile byName(String name) {
            if (name != null) {
                for (Profile profile : values()) {
                    if (profile.serializedName.equals(name.toLowerCase(Locale.ROOT))) {
                        return profile;
                    }
                }
            }
            throw new IllegalArgumentException("Unknown terrain profile: " + name);
        }
    }

    public enum Coordinate implements ToFloatFunction<Point> {
        CONTINENTS("continents") {
            @Override
            public float apply(Point point) {
                return point.continents();
            }
        },
        EROSION("erosion") {
            @Override
            public float apply(Point point) {
                return point.erosion();
            }
        },
        WEIRDNESS("weirdness") {
            @Override
            public float apply(Point point) {
                return point.weirdness();
            }
        },
        /** Retained because 1.18.2's spline codec exposes this coordinate. */
        @Deprecated
        RIDGES("ridges") {
            @Override
            public float apply(Point point) {
                return point.ridges();
            }
        };

        private final String serializedName;

        Coordinate(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        @Override
        public String toString() {
            return serializedName;
        }
    }

    public static final class Point {
        private final float continents;
        private final float erosion;
        private final float ridges;
        private final float weirdness;

        public Point(float continents, float erosion, float ridges, float weirdness) {
            this.continents = continents;
            this.erosion = erosion;
            this.ridges = ridges;
            this.weirdness = weirdness;
        }

        public float continents() {
            return continents;
        }

        public float erosion() {
            return erosion;
        }

        public float ridges() {
            return ridges;
        }

        public float weirdness() {
            return weirdness;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof Point)) {
                return false;
            }
            Point other = (Point) object;
            return Float.floatToIntBits(continents) == Float.floatToIntBits(other.continents)
                && Float.floatToIntBits(erosion) == Float.floatToIntBits(other.erosion)
                && Float.floatToIntBits(ridges) == Float.floatToIntBits(other.ridges)
                && Float.floatToIntBits(weirdness) == Float.floatToIntBits(other.weirdness);
        }

        @Override
        public int hashCode() {
            int result = Float.floatToIntBits(continents);
            result = 31 * result + Float.floatToIntBits(erosion);
            result = 31 * result + Float.floatToIntBits(ridges);
            result = 31 * result + Float.floatToIntBits(weirdness);
            return result;
        }

        @Override
        public String toString() {
            return "Point{" + continents + ',' + erosion + ',' + ridges + ',' + weirdness + '}';
        }
    }
}

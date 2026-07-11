package net.celestiald.cavesnotcliffs.worldgen.v118;

/** Exact Java 1.18.2 legacy three-Perlin base-density sampler. */
public final class BlendedNoise {
    private static final int[] LIMIT_OCTAVES = range(-15, 0);
    private static final int[] MAIN_OCTAVES = range(-7, 0);

    private final PerlinNoise minLimitNoise;
    private final PerlinNoise maxLimitNoise;
    private final PerlinNoise mainNoise;
    private final double xzScale;
    private final double yScale;
    private final double xzMainScale;
    private final double yMainScale;
    private final int cellWidth;
    private final int cellHeight;
    private final double maxValue;

    public BlendedNoise(RandomSource random, NoiseSamplingSettings settings,
            int cellWidth, int cellHeight) {
        minLimitNoise = PerlinNoise.createLegacyForBlendedNoise(random, LIMIT_OCTAVES);
        maxLimitNoise = PerlinNoise.createLegacyForBlendedNoise(random, LIMIT_OCTAVES);
        mainNoise = PerlinNoise.createLegacyForBlendedNoise(random, MAIN_OCTAVES);
        xzScale = 684.412D * settings.xzScale();
        yScale = 684.412D * settings.yScale();
        xzMainScale = xzScale / settings.xzFactor();
        yMainScale = yScale / settings.yFactor();
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        maxValue = minLimitNoise.maxBrokenValue(yScale);
    }

    public double compute(int blockX, int blockY, int blockZ) {
        int cellX = Math.floorDiv(blockX, cellWidth);
        int cellY = Math.floorDiv(blockY, cellHeight);
        int cellZ = Math.floorDiv(blockZ, cellWidth);
        double minValue = 0.0D;
        double maxValue = 0.0D;
        double mainValue = 0.0D;
        double octaveScale = 1.0D;

        for (int octave = 0; octave < 8; ++octave) {
            ImprovedNoise noise = mainNoise.getOctaveNoise(octave);
            if (noise != null) {
                mainValue += noise.noise(
                        PerlinNoise.wrap(cellX * xzMainScale * octaveScale),
                        PerlinNoise.wrap(cellY * yMainScale * octaveScale),
                        PerlinNoise.wrap(cellZ * xzMainScale * octaveScale),
                        yMainScale * octaveScale,
                        cellY * yMainScale * octaveScale) / octaveScale;
            }
            octaveScale /= 2.0D;
        }

        double blend = (mainValue / 10.0D + 1.0D) / 2.0D;
        boolean onlyMax = blend >= 1.0D;
        boolean onlyMin = blend <= 0.0D;
        octaveScale = 1.0D;
        for (int octave = 0; octave < 16; ++octave) {
            double x = PerlinNoise.wrap(cellX * xzScale * octaveScale);
            double y = PerlinNoise.wrap(cellY * yScale * octaveScale);
            double z = PerlinNoise.wrap(cellZ * xzScale * octaveScale);
            double verticalScale = yScale * octaveScale;
            ImprovedNoise noise;
            if (!onlyMax && (noise = minLimitNoise.getOctaveNoise(octave)) != null) {
                minValue += noise.noise(x, y, z, verticalScale,
                        cellY * verticalScale) / octaveScale;
            }
            if (!onlyMin && (noise = maxLimitNoise.getOctaveNoise(octave)) != null) {
                maxValue += noise.noise(x, y, z, verticalScale,
                        cellY * verticalScale) / octaveScale;
            }
            octaveScale /= 2.0D;
        }
        return clampedLerp(minValue / 512.0D, maxValue / 512.0D, blend) / 128.0D;
    }

    public double minValue() {
        return -maxValue;
    }

    public double maxValue() {
        return maxValue;
    }

    private static double clampedLerp(double start, double end, double delta) {
        if (delta < 0.0D) {
            return start;
        }
        if (delta > 1.0D) {
            return end;
        }
        return WorldgenMath.lerp(delta, start, end);
    }

    private static int[] range(int start, int end) {
        int[] values = new int[end - start + 1];
        for (int index = 0; index < values.length; ++index) {
            values[index] = start + index;
        }
        return values;
    }

}

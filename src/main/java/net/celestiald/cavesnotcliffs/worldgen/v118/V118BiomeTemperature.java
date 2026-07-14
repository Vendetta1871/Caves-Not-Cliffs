package net.celestiald.cavesnotcliffs.worldgen.v118;

/** Exact Java 1.18.2 biome temperature predicates used by Overworld surface rules. */
public final class V118BiomeTemperature {
    private static final PerlinSimplex2D TEMPERATURE_NOISE = new PerlinSimplex2D(1234L,
        new int[] {0});
    private static final PerlinSimplex2D FROZEN_TEMPERATURE_NOISE = new PerlinSimplex2D(3456L,
        new int[] {-2, -1, 0});
    private static final PerlinSimplex2D BIOME_INFO_NOISE = new PerlinSimplex2D(2345L,
        new int[] {0});

    private V118BiomeTemperature() {
    }

    public static float temperature(V118Biome biome, int blockX, int blockY, int blockZ) {
        if (biome == null) {
            throw new NullPointerException("biome");
        }
        float temperature = biome.baseTemperature();
        if (biome.hasFrozenTemperatureModifier()) {
            double frozen = FROZEN_TEMPERATURE_NOISE.getValue(blockX * 0.05D,
                blockZ * 0.05D) * 7.0D;
            double info = BIOME_INFO_NOISE.getValue(blockX * 0.2D, blockZ * 0.2D);
            if (frozen + info < 0.3D
                    && BIOME_INFO_NOISE.getValue(blockX * 0.09D, blockZ * 0.09D) < 0.8D) {
                temperature = 0.2F;
            }
        }
        if (blockY > 80) {
            float heightNoise = (float) (TEMPERATURE_NOISE.getValue(
                (float) blockX / 8.0F, (float) blockZ / 8.0F) * 8.0D);
            temperature -= (heightNoise + (float) blockY - 80.0F) * 0.05F / 40.0F;
        }
        return temperature;
    }

    public static boolean coldEnoughToSnow(V118Biome biome, int blockX, int blockY, int blockZ) {
        return temperature(biome, blockX, blockY, blockZ) < 0.15F;
    }

    public static boolean shouldMeltFrozenOceanIcebergSlightly(V118Biome biome, int blockX,
            int blockY, int blockZ) {
        return temperature(biome, blockX, blockY, blockZ) > 0.1F;
    }

    /** Static Java 1.18.2 biome-info simplex used by vegetation count placement. */
    public static double biomeInfoNoise(double x, double z) {
        return BIOME_INFO_NOISE.getValue(x, z);
    }

    /** Minimal legacy 48-bit random stream used by vanilla's static simplex samplers. */
    private static final class LegacyRandom {
        private static final long MASK = (1L << 48) - 1L;
        private long seed;

        LegacyRandom(long seed) {
            this.seed = (seed ^ 0x5DEECE66DL) & MASK;
        }

        int next(int bits) {
            seed = seed * 25214903917L + 11L & MASK;
            return (int) (seed >>> 48 - bits);
        }

        int nextInt(int bound) {
            if (bound <= 0) {
                throw new IllegalArgumentException("Bound must be positive");
            }
            if ((bound & bound - 1) == 0) {
                return (int) ((long) bound * next(31) >> 31);
            }
            int bits;
            int value;
            do {
                bits = next(31);
                value = bits % bound;
            } while (bits - value + (bound - 1) < 0);
            return value;
        }

        double nextDouble() {
            long value = ((long) next(26) << 27) + next(27);
            return value * (double) 1.110223E-16F;
        }

        void consumeCount(int count) {
            for (int index = 0; index < count; ++index) {
                next(32);
            }
        }
    }

    /** Vanilla's 2D simplex sampler; only the temperature paths require this form. */
    private static final class Simplex2D {
        private static final int[][] GRADIENT = {
            {1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0},
            {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
            {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1},
            {1, 1, 0}, {0, -1, 1}, {-1, 1, 0}, {0, -1, -1}
        };
        private static final double SQRT_3 = Math.sqrt(3.0D);
        private static final double F2 = 0.5D * (SQRT_3 - 1.0D);
        private static final double G2 = (3.0D - SQRT_3) / 6.0D;

        private final int[] permutations = new int[512];
        private final double xo;
        private final double yo;
        private final double zo;

        Simplex2D(LegacyRandom random) {
            xo = random.nextDouble() * 256.0D;
            yo = random.nextDouble() * 256.0D;
            zo = random.nextDouble() * 256.0D;
            for (int index = 0; index < 256; ++index) {
                permutations[index] = index;
            }
            for (int index = 0; index < 256; ++index) {
                int swap = random.nextInt(256 - index);
                int value = permutations[index];
                permutations[index] = permutations[index + swap];
                permutations[index + swap] = value;
            }
        }

        double value(double x, double z) {
            double skew = (x + z) * F2;
            int cellX = floor(x + skew);
            int cellZ = floor(z + skew);
            double unskew = (cellX + cellZ) * G2;
            double originX = cellX - unskew;
            double originZ = cellZ - unskew;
            double localX = x - originX;
            double localZ = z - originZ;
            int offsetX;
            int offsetZ;
            if (localX > localZ) {
                offsetX = 1;
                offsetZ = 0;
            } else {
                offsetX = 0;
                offsetZ = 1;
            }
            double middleX = localX - offsetX + G2;
            double middleZ = localZ - offsetZ + G2;
            double farX = localX - 1.0D + 2.0D * G2;
            double farZ = localZ - 1.0D + 2.0D * G2;
            int maskedX = cellX & 255;
            int maskedZ = cellZ & 255;
            int gradient0 = permutation(maskedX + permutation(maskedZ)) % 12;
            int gradient1 = permutation(maskedX + offsetX
                + permutation(maskedZ + offsetZ)) % 12;
            int gradient2 = permutation(maskedX + 1 + permutation(maskedZ + 1)) % 12;
            return 70.0D * (corner(gradient0, localX, localZ)
                + corner(gradient1, middleX, middleZ)
                + corner(gradient2, farX, farZ));
        }

        private int permutation(int index) {
            return permutations[index & 255];
        }

        private static double corner(int gradient, double x, double z) {
            double falloff = 0.5D - x * x - z * z;
            if (falloff < 0.0D) {
                return 0.0D;
            }
            falloff *= falloff;
            return falloff * falloff * (GRADIENT[gradient][0] * x
                + GRADIENT[gradient][1] * z);
        }
    }

    private static final class PerlinSimplex2D {
        private final Simplex2D[] levels;
        private final double highestFrequencyInputFactor;
        private final double highestFrequencyValueFactor;

        PerlinSimplex2D(long seed, int[] octaves) {
            int minimum = octaves[0];
            int maximum = octaves[0];
            for (int octave : octaves) {
                minimum = Math.min(minimum, octave);
                maximum = Math.max(maximum, octave);
            }
            int zeroIndex = -minimum;
            int levelCount = zeroIndex + maximum + 1;
            LegacyRandom random = new LegacyRandom(seed);
            Simplex2D first = new Simplex2D(random);
            levels = new Simplex2D[levelCount];
            if (maximum >= 0 && maximum < levelCount && contains(octaves, 0)) {
                levels[maximum] = first;
            }
            for (int index = maximum + 1; index < levelCount; ++index) {
                if (index >= 0 && contains(octaves, maximum - index)) {
                    levels[index] = new Simplex2D(random);
                } else {
                    random.consumeCount(262);
                }
            }
            if (maximum > 0) {
                long derivedSeed = (long) (simplex3D(first, first.xo, first.yo, first.zo)
                    * 9.223372036854776E18D);
                LegacyRandom derived = new LegacyRandom(derivedSeed);
                for (int index = maximum - 1; index >= 0; --index) {
                    if (index < levelCount && contains(octaves, maximum - index)) {
                        levels[index] = new Simplex2D(derived);
                    } else {
                        derived.consumeCount(262);
                    }
                }
            }
            highestFrequencyInputFactor = Math.pow(2.0D, maximum);
            highestFrequencyValueFactor = 1.0D / (Math.pow(2.0D, levelCount) - 1.0D);
        }

        double getValue(double x, double z) {
            double result = 0.0D;
            double inputFactor = highestFrequencyInputFactor;
            double valueFactor = highestFrequencyValueFactor;
            for (Simplex2D level : levels) {
                if (level != null) {
                    result += level.value(x * inputFactor, z * inputFactor) * valueFactor;
                }
                inputFactor /= 2.0D;
                valueFactor *= 2.0D;
            }
            return result;
        }

        private static boolean contains(int[] values, int target) {
            for (int value : values) {
                if (value == target) {
                    return true;
                }
            }
            return false;
        }
    }

    // The constructor path used here never has a positive highest octave. Kept as an explicit
    // guard rather than silently approximating the vanilla derived-seed branch.
    private static double simplex3D(Simplex2D noise, double x, double y, double z) {
        throw new AssertionError("positive temperature-noise octaves are not used by 1.18.2");
    }

    private static int floor(double value) {
        int truncated = (int) value;
        return value < truncated ? truncated - 1 : truncated;
    }
}

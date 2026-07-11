package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/** Minecraft 1.18.2's octave stack of improved-noise samplers. */
public class PerlinNoise {
    private static final int ROUND_OFF = 0x2000000;

    private final ImprovedNoise[] noiseLevels;
    private final int firstOctave;
    private final double[] amplitudes;
    private final double lowestFreqValueFactor;
    private final double lowestFreqInputFactor;
    private final double maxValue;

    public static PerlinNoise create(RandomSource random, int firstOctave,
            double firstAmplitude, double... remainingAmplitudes) {
        double[] amplitudes = new double[remainingAmplitudes.length + 1];
        amplitudes[0] = firstAmplitude;
        System.arraycopy(remainingAmplitudes, 0, amplitudes, 1, remainingAmplitudes.length);
        return new PerlinNoise(random, firstOctave, amplitudes, true);
    }

    public static PerlinNoise create(RandomSource random, int firstOctave, List<Double> amplitudes) {
        return new PerlinNoise(random, firstOctave, toArray(amplitudes), true);
    }

    public static PerlinNoise create(RandomSource random, List<Integer> octaves) {
        OctaveSpec spec = makeAmplitudes(new TreeSet<Integer>(octaves));
        return new PerlinNoise(random, spec.firstOctave, spec.amplitudes, true);
    }

    public static PerlinNoise createLegacy(RandomSource random, int firstOctave,
            double firstAmplitude, double... remainingAmplitudes) {
        double[] amplitudes = new double[remainingAmplitudes.length + 1];
        amplitudes[0] = firstAmplitude;
        System.arraycopy(remainingAmplitudes, 0, amplitudes, 1, remainingAmplitudes.length);
        return new PerlinNoise(random, firstOctave, amplitudes, false);
    }

    public static PerlinNoise createLegacy(RandomSource random, int firstOctave,
            List<Double> amplitudes) {
        return new PerlinNoise(random, firstOctave, toArray(amplitudes), false);
    }

    public static PerlinNoise createLegacyForBlendedNoise(RandomSource random, int... octaves) {
        SortedSet<Integer> sorted = new TreeSet<Integer>();
        for (int octave : octaves) {
            sorted.add(octave);
        }
        OctaveSpec spec = makeAmplitudes(sorted);
        return new PerlinNoise(random, spec.firstOctave, spec.amplitudes, false);
    }

    private PerlinNoise(RandomSource random, int firstOctave, double[] amplitudes, boolean modern) {
        if (amplitudes.length < 1) {
            throw new IllegalArgumentException("Need some octaves!");
        }
        this.firstOctave = firstOctave;
        this.amplitudes = amplitudes.clone();
        int octaveCount = amplitudes.length;
        int zeroOctaveIndex = -firstOctave;
        noiseLevels = new ImprovedNoise[octaveCount];

        if (modern) {
            PositionalRandomFactory positional = random.forkPositional();
            for (int index = 0; index < octaveCount; ++index) {
                if (amplitudes[index] != 0.0D) {
                    int octave = firstOctave + index;
                    noiseLevels[index] = new ImprovedNoise(positional.fromHashOf("octave_" + octave));
                }
            }
        } else {
            ImprovedNoise zeroNoise = new ImprovedNoise(random);
            if (zeroOctaveIndex >= 0 && zeroOctaveIndex < octaveCount
                    && amplitudes[zeroOctaveIndex] != 0.0D) {
                noiseLevels[zeroOctaveIndex] = zeroNoise;
            }

            for (int index = zeroOctaveIndex - 1; index >= 0; --index) {
                if (index < octaveCount) {
                    if (amplitudes[index] != 0.0D) {
                        noiseLevels[index] = new ImprovedNoise(random);
                    } else {
                        skipOctave(random);
                    }
                } else {
                    skipOctave(random);
                }
            }

            int actualCount = 0;
            int expectedCount = 0;
            for (int index = 0; index < octaveCount; ++index) {
                if (noiseLevels[index] != null) {
                    ++actualCount;
                }
                if (amplitudes[index] != 0.0D) {
                    ++expectedCount;
                }
            }
            if (actualCount != expectedCount) {
                throw new IllegalStateException(
                    "Failed to create correct number of noise levels for given non-zero amplitudes");
            }
            if (zeroOctaveIndex < octaveCount - 1) {
                throw new IllegalArgumentException("Positive octaves are temporarily disabled");
            }
        }

        lowestFreqInputFactor = Math.pow(2.0D, -zeroOctaveIndex);
        lowestFreqValueFactor = Math.pow(2.0D, octaveCount - 1)
            / (Math.pow(2.0D, octaveCount) - 1.0D);
        maxValue = edgeValue(2.0D);
    }

    public double maxValue() {
        return maxValue;
    }

    public double getValue(double x, double y, double z) {
        return getValue(x, y, z, 0.0D, 0.0D, false);
    }

    public double getValue(double x, double y, double z,
            double yScale, double yMax, boolean useOrigin) {
        double value = 0.0D;
        double inputFactor = lowestFreqInputFactor;
        double valueFactor = lowestFreqValueFactor;
        for (int index = 0; index < noiseLevels.length; ++index) {
            ImprovedNoise noise = noiseLevels[index];
            if (noise != null) {
                double octaveValue = noise.noise(
                    wrap(x * inputFactor),
                    useOrigin ? -noise.yo : wrap(y * inputFactor),
                    wrap(z * inputFactor),
                    yScale * inputFactor,
                    yMax * inputFactor);
                value += amplitudes[index] * octaveValue * valueFactor;
            }
            inputFactor *= 2.0D;
            valueFactor /= 2.0D;
        }
        return value;
    }

    public double maxBrokenValue(double value) {
        return edgeValue(value + 2.0D);
    }

    private double edgeValue(double value) {
        double result = 0.0D;
        double factor = lowestFreqValueFactor;
        for (int index = 0; index < noiseLevels.length; ++index) {
            if (noiseLevels[index] != null) {
                result += amplitudes[index] * value * factor;
            }
            factor /= 2.0D;
        }
        return result;
    }

    public ImprovedNoise getOctaveNoise(int octave) {
        return noiseLevels[noiseLevels.length - 1 - octave];
    }

    public static double wrap(double value) {
        return value - WorldgenMath.lfloor(value / ROUND_OFF + 0.5D) * (double) ROUND_OFF;
    }

    public int firstOctave() {
        return firstOctave;
    }

    public List<Double> amplitudes() {
        List<Double> copy = new ArrayList<Double>(amplitudes.length);
        for (double amplitude : amplitudes) {
            copy.add(amplitude);
        }
        return Collections.unmodifiableList(copy);
    }

    private static void skipOctave(RandomSource random) {
        random.consumeCount(262);
    }

    private static OctaveSpec makeAmplitudes(SortedSet<Integer> octaves) {
        if (octaves.isEmpty()) {
            throw new IllegalArgumentException("Need some octaves!");
        }
        int zeroOctaveIndex = -octaves.first();
        int lastOctave = octaves.last();
        int count = zeroOctaveIndex + lastOctave + 1;
        if (count < 1) {
            throw new IllegalArgumentException("Total number of octaves needs to be >= 1");
        }
        double[] amplitudes = new double[count];
        for (Integer octave : octaves) {
            amplitudes[octave + zeroOctaveIndex] = 1.0D;
        }
        return new OctaveSpec(-zeroOctaveIndex, amplitudes);
    }

    private static double[] toArray(List<Double> amplitudes) {
        double[] values = new double[amplitudes.size()];
        for (int i = 0; i < amplitudes.size(); ++i) {
            values[i] = amplitudes.get(i);
        }
        return values;
    }

    private static final class OctaveSpec {
        private final int firstOctave;
        private final double[] amplitudes;

        private OctaveSpec(int firstOctave, double[] amplitudes) {
            this.firstOctave = firstOctave;
            this.amplitudes = amplitudes;
        }
    }
}

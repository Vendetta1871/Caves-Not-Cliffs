package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Minecraft 1.18.2's two-Perlin normal-noise sampler. */
public final class NormalNoise {
    private static final double INPUT_FACTOR = 1.0181268882175227D;
    private static final double TARGET_DEVIATION = 1.0D / 3.0D;

    private final double valueFactor;
    private final PerlinNoise first;
    private final PerlinNoise second;
    private final double maxValue;
    private final NoiseParameters parameters;
    // Native copy of the octave tables, or null when sampling in Java (see NativeNoise).
    private final NativeNoise.Blob nativeBlob;
    private final long nativeHandle;

    public static NormalNoise create(RandomSource random, int firstOctave, double... amplitudes) {
        return create(random, new NoiseParameters(firstOctave, amplitudes));
    }

    public static NormalNoise create(RandomSource random, NoiseParameters parameters) {
        return new NormalNoise(random, parameters, true, false);
    }

    /**
     * Like {@link #create(RandomSource, NoiseParameters)}, sampled by the native kernel when
     * it is available. For long-lived, heavily sampled noises: each one allocates a direct
     * buffer for its tables.
     */
    static NormalNoise createAccelerated(RandomSource random, NoiseParameters parameters) {
        return new NormalNoise(random, parameters, true, NativeNoise.AVAILABLE);
    }

    public static NormalNoise createLegacy(RandomSource random, NoiseParameters parameters) {
        return new NormalNoise(random, parameters, false, false);
    }

    private NormalNoise(RandomSource random, NoiseParameters parameters, boolean modern,
            boolean accelerated) {
        this.parameters = parameters;
        int firstOctave = parameters.firstOctave();
        List<Double> amplitudes = parameters.amplitudes();
        if (modern) {
            first = PerlinNoise.create(random, firstOctave, amplitudes);
            second = PerlinNoise.create(random, firstOctave, amplitudes);
        } else {
            first = PerlinNoise.createLegacy(random, firstOctave, amplitudes);
            second = PerlinNoise.createLegacy(random, firstOctave, amplitudes);
        }

        int minIndex = Integer.MAX_VALUE;
        int maxIndex = Integer.MIN_VALUE;
        for (int index = 0; index < amplitudes.size(); ++index) {
            if (amplitudes.get(index) != 0.0D) {
                minIndex = Math.min(minIndex, index);
                maxIndex = Math.max(maxIndex, index);
            }
        }
        valueFactor = (1.0D / 6.0D) / expectedDeviation(maxIndex - minIndex);
        maxValue = (first.maxValue() + second.maxValue()) * valueFactor;
        nativeBlob = accelerated ? exportNative() : null;
        nativeHandle = nativeBlob == null ? 0L : nativeBlob.handle;
    }

    /** Whether {@link NativeNoise}'s kernel samples this noise. */
    boolean isNative() {
        return nativeHandle != 0L;
    }

    /** This noise's tables in the native kernel's layout, or null if they do not fit it. */
    NativeNoise.Blob exportNative() {
        return NativeNoise.exportNormal(first, second, INPUT_FACTOR, valueFactor);
    }

    public double maxValue() {
        return maxValue;
    }

    public static double expectedDeviation(int octaveSpan) {
        return 0.1D * (1.0D + 1.0D / (octaveSpan + 1.0D));
    }

    public double getValue(double x, double y, double z) {
        if (nativeHandle != 0L) {
            return NativeNoise.normal(nativeHandle, x, y, z);
        }
        return sampleJava(x, y, z);
    }

    /** The reference sampler; the native kernel must agree with it bit for bit. */
    double sampleJava(double x, double y, double z) {
        double shiftedX = x * INPUT_FACTOR;
        double shiftedY = y * INPUT_FACTOR;
        double shiftedZ = z * INPUT_FACTOR;
        return (first.getValue(x, y, z) + second.getValue(shiftedX, shiftedY, shiftedZ))
            * valueFactor;
    }

    public NoiseParameters parameters() {
        return parameters;
    }

    public static final class NoiseParameters {
        private final int firstOctave;
        private final List<Double> amplitudes;

        public NoiseParameters(int firstOctave, double[] amplitudes) {
            this.firstOctave = firstOctave;
            List<Double> values = new ArrayList<Double>(amplitudes.length);
            for (double amplitude : amplitudes) {
                values.add(amplitude);
            }
            this.amplitudes = Collections.unmodifiableList(values);
        }

        public NoiseParameters(int firstOctave, double firstAmplitude,
                double... remainingAmplitudes) {
            this.firstOctave = firstOctave;
            List<Double> values = new ArrayList<Double>(remainingAmplitudes.length + 1);
            values.add(firstAmplitude);
            for (double amplitude : remainingAmplitudes) {
                values.add(amplitude);
            }
            this.amplitudes = Collections.unmodifiableList(values);
        }

        public NoiseParameters(int firstOctave, List<Double> amplitudes) {
            this.firstOctave = firstOctave;
            this.amplitudes = Collections.unmodifiableList(new ArrayList<Double>(amplitudes));
        }

        public int firstOctave() {
            return firstOctave;
        }

        public List<Double> amplitudes() {
            return amplitudes;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof NoiseParameters)) {
                return false;
            }
            NoiseParameters other = (NoiseParameters) object;
            return firstOctave == other.firstOctave && amplitudes.equals(other.amplitudes);
        }

        @Override
        public int hashCode() {
            return 31 * firstOctave + amplitudes.hashCode();
        }

        @Override
        public String toString() {
            return "NoiseParameters{firstOctave=" + firstOctave + ", amplitudes=" + amplitudes + '}';
        }
    }
}

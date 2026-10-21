package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Assume;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The native kernels must reproduce the Java samplers bit for bit. The oracle tests already run
 * through the native sampler wherever it loads; this compares both paths directly on far more
 * points, every registered noise and the coordinate extremes.
 */
public class NativeNoiseTest {
    private static final long[] SEEDS = {
        0L, 1L, -1L, 123456789L, Long.MIN_VALUE, Long.MAX_VALUE
    };
    private static final int WORLD_BORDER = 29999984;

    @Test
    public void loadsWhereverTheBuildShipsALibrary() {
        String platform = NativeNoise.platform();
        Assume.assumeTrue("no native library for this platform", platform != null);
        Assume.assumeTrue("disabled by -D" + NativeNoise.PROPERTY,
            Boolean.parseBoolean(System.getProperty(NativeNoise.PROPERTY, "true")));
        Assume.assumeTrue("built with -PskipNativeNoise", NativeNoise.class.getResource(
            NativeNoise.RESOURCE_ROOT + platform + "/"
                + NativeNoise.libraryFileName(platform)) != null);
        assertTrue(NativeNoise.status(), NativeNoise.AVAILABLE
            || NativeNoise.status().endsWith("without AVX2"));
    }

    @Test
    public void everyRegisteredNoiseMatchesTheJavaSamplerBitForBit() {
        Assume.assumeTrue(NativeNoise.status(), NativeNoise.AVAILABLE);
        for (long seed : SEEDS) {
            PositionalRandomFactory positional = new XoroshiroRandomSource(seed).forkPositional();
            Random random = new Random(seed);
            for (String name : V118NoiseParameters.all().keySet()) {
                NormalNoise noise = V118NoiseParameters.instantiateAccelerated(name, positional);
                assertTrue(name, noise.isNative());
                for (int sample = 0; sample < 2000; ++sample) {
                    double x = coordinate(random, sample);
                    double y = (random.nextDouble() - 0.5D) * 800.0D;
                    double z = coordinate(random, sample + 1);
                    assertBitsEqual(name + " seed=" + seed + " at " + x + ", " + y + ", " + z,
                        noise.sampleJava(x, y, z), noise.getValue(x, y, z));
                }
            }
        }
    }

    @Test
    public void blendedNoiseMatchesTheJavaSamplerBitForBit() {
        Assume.assumeTrue(NativeNoise.status(), NativeNoise.AVAILABLE);
        for (long seed : SEEDS) {
            for (boolean amplified : new boolean[] {false, true}) {
                V118NoiseSettings settings = V118NoiseSettings.overworld(amplified);
                BlendedNoise noise = new BlendedNoise(
                    new XoroshiroRandomSource(seed).forkPositional()
                        .fromHashOf("minecraft:terrain"),
                    settings.sampling(), settings.getCellWidth(), settings.getCellHeight());
                assertTrue(noise.isNative());
                Random random = new Random(seed ^ (amplified ? 1L : 0L));
                for (int sample = 0; sample < 20000; ++sample) {
                    int x = blockCoordinate(random, sample);
                    int y = random.nextInt(384) - 64;
                    int z = blockCoordinate(random, sample + 1);
                    assertBitsEqual("seed=" + seed + " amplified=" + amplified + " at " + x + ", "
                        + y + ", " + z, noise.computeJava(x, y, z), noise.compute(x, y, z));
                }
            }
        }
    }

    /** Mixes near-origin, ordinary and world-border coordinates, scaled like router inputs. */
    private static double coordinate(Random random, int sample) {
        switch (sample % 4) {
            case 0:
                return (random.nextDouble() - 0.5D) * 64.0D;
            case 1:
                return (random.nextDouble() - 0.5D) * 2.0E5D;
            case 2:
                return (random.nextBoolean() ? 1 : -1) * (WORLD_BORDER - random.nextInt(64))
                    + random.nextDouble();
            default:
                return random.nextInt() * 0.25D;
        }
    }

    private static int blockCoordinate(Random random, int sample) {
        switch (sample % 3) {
            case 0:
                return random.nextInt(128) - 64;
            case 1:
                return random.nextInt(400000) - 200000;
            default:
                return (random.nextBoolean() ? 1 : -1) * (WORLD_BORDER - random.nextInt(64));
        }
    }

    private static void assertBitsEqual(String message, double expected, double actual) {
        assertEquals(message, Double.doubleToRawLongBits(expected),
            Double.doubleToRawLongBits(actual));
    }
}

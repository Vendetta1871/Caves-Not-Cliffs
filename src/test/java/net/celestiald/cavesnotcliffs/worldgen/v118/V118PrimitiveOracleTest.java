package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class V118PrimitiveOracleTest {
    private static final String FIXTURE =
        "/net/celestiald/cavesnotcliffs/worldgen/v118/oracle-1.18.2.tsv";
    private static final double[] AMPLITUDES = {1.0D, 0.5D, 0.0D, 1.5D};

    @Test
    public void positionalFactoryConfigStringMatchesOfficialFormatting() {
        PositionalRandomFactory factory = new XoroshiroRandomSource(1L).forkPositional();
        assertEquals("seedLo:-1033667707219518978, seedHi:6451672561743293322",
            factory.parityConfigString());
    }

    @Test
    public void matchesOfficialMinecraft1182OracleBitForBit() throws IOException {
        Map<Long, NoiseBundle> noiseBySeed = new HashMap<Long, NoiseBundle>();
        List<Long> fixtureSeeds = new ArrayList<Long>();
        int assertions = 0;

        for (String line : fixtureLines()) {
            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }
            String[] fields = line.split("\\t", -1);
            switch (fields[0]) {
                case "zero-state": {
                    Xoroshiro128PlusPlus random = new Xoroshiro128PlusPlus(0L, 0L);
                    for (int i = 1; i < fields.length; ++i) {
                        assertEquals(message(line, i), Long.parseLong(fields[i]), random.nextLong());
                        ++assertions;
                    }
                    break;
                }
                case "seed": {
                    long seed = Long.parseLong(fields[1]);
                    fixtureSeeds.add(seed);
                    RandomSupport.Seed128 upgraded = RandomSupport.upgradeSeedTo128bit(seed);
                    assertEquals(message(line, 2), Long.parseLong(fields[2]), upgraded.seedLo());
                    assertEquals(message(line, 3), Long.parseLong(fields[3]), upgraded.seedHi());
                    assertions += 2;
                    break;
                }
                case "xoroshiro": {
                    long seed = Long.parseLong(fields[1]);
                    Xoroshiro128PlusPlus random =
                        new Xoroshiro128PlusPlus(RandomSupport.upgradeSeedTo128bit(seed));
                    for (int i = 2; i < fields.length; ++i) {
                        assertEquals(message(line, i), Long.parseLong(fields[i]), random.nextLong());
                        ++assertions;
                    }
                    break;
                }
                case "random": {
                    XoroshiroRandomSource random = new XoroshiroRandomSource(Long.parseLong(fields[1]));
                    assertEquals(message(line, 2), Long.parseLong(fields[2]), random.nextLong());
                    assertEquals(message(line, 3), Integer.parseInt(fields[3]), random.nextInt());
                    assertEquals(message(line, 4), Integer.parseInt(fields[4]), random.nextInt(37));
                    assertEquals(message(line, 5), Boolean.parseBoolean(fields[5]), random.nextBoolean());
                    assertEquals(message(line, 6), Integer.parseUnsignedInt(fields[6]),
                        Float.floatToRawIntBits(random.nextFloat()));
                    assertBits(message(line, 7), fields[7], random.nextDouble());
                    assertBits(message(line, 8), fields[8], random.nextGaussian());
                    assertions += 7;
                    break;
                }
                case "positional": {
                    long seed = Long.parseLong(fields[1]);
                    PositionalRandomFactory factory = new XoroshiroRandomSource(seed).forkPositional();
                    RandomSource at = factory.at(Integer.parseInt(fields[2]), Integer.parseInt(fields[3]),
                        Integer.parseInt(fields[4]));
                    assertEquals(message(line, 5), Long.parseLong(fields[5]), at.nextLong());
                    assertEquals(message(line, 6), Integer.parseInt(fields[6]), at.nextInt(97));
                    assertions += 2;
                    break;
                }
                case "hash": {
                    long seed = Long.parseLong(fields[1]);
                    RandomSource hashed = new XoroshiroRandomSource(seed).forkPositional()
                        .fromHashOf(fields[2]);
                    assertEquals(message(line, 3), Long.parseLong(fields[3]), hashed.nextLong());
                    assertEquals(message(line, 4), Integer.parseInt(fields[4]), hashed.nextInt(97));
                    assertions += 2;
                    break;
                }
                case "noise": {
                    long seed = Long.parseLong(fields[1]);
                    NoiseBundle bundle = noiseBySeed.get(seed);
                    if (bundle == null) {
                        bundle = new NoiseBundle(seed);
                        noiseBySeed.put(seed, bundle);
                    }
                    double x = Double.parseDouble(fields[2]);
                    double y = Double.parseDouble(fields[3]);
                    double z = Double.parseDouble(fields[4]);
                    assertBits(message(line, 5), fields[5], bundle.improved.noise(x, y, z));
                    assertBits(message(line, 6), fields[6], bundle.perlin.getValue(x, y, z));
                    assertBits(message(line, 7), fields[7], bundle.normal.getValue(x, y, z));
                    assertBits(message(line, 8), fields[8], bundle.legacyPerlin.getValue(x, y, z));
                    assertBits(message(line, 9), fields[9], bundle.legacyNormal.getValue(x, y, z));
                    double[] derivative = {0.25D, -0.5D, 0.75D};
                    assertBits(message(line, 10), fields[10],
                        bundle.improved.noiseWithDerivative(x, y, z, derivative));
                    assertBits(message(line, 11), fields[11], derivative[0]);
                    assertBits(message(line, 12), fields[12], derivative[1]);
                    assertBits(message(line, 13), fields[13], derivative[2]);
                    assertions += 9;
                    break;
                }
                case "spline": {
                    CubicSpline<Float> spline = fixtureSpline();
                    float input = Float.intBitsToFloat(Integer.parseInt(fields[1]));
                    assertEquals(message(line, 2), Integer.parseUnsignedInt(fields[2]),
                        Float.floatToRawIntBits(spline.apply(input)));
                    ++assertions;
                    break;
                }
                case "climate": {
                    Climate.ParameterPoint first = firstClimatePoint();
                    Climate.ParameterPoint second = secondClimatePoint();
                    Climate.TargetPoint target = climateTarget();
                    assertEquals(message(line, 1), Long.parseLong(fields[1]), first.fitness(target));
                    assertEquals(message(line, 2), Long.parseLong(fields[2]), second.fitness(target));
                    assertions += 2;
                    break;
                }
                case "climate-list": {
                    Climate.ParameterList<String> list = climateList();
                    assertEquals(message(line, 1), fields[1], list.findValue(climateTarget()));
                    assertEquals(message(line, 2), fields[2],
                        list.findValueBruteForce(climateTarget()));
                    assertions += 2;
                    break;
                }
                default:
                    fail("Unknown fixture record: " + line);
            }
        }

        assertEquals("fixture assertion count", 500, assertions);
        assertArrayEquals(new Long[] {
            0L, 1L, -1L, 123456789L, Long.MIN_VALUE, Long.MAX_VALUE
        }, fixtureSeeds.toArray(new Long[0]));
    }

    @Test
    public void validatesPublicPrimitiveContracts() {
        try {
            new XoroshiroRandomSource(0L).nextInt(0);
            fail("zero bound must be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals("Bound must be positive", expected.getMessage());
        }

        try {
            CubicSpline.builder((Float value) -> value)
                .addPoint(0.0F, 0.0F, 0.0F)
                .addPoint(0.0F, 1.0F, 0.0F);
            fail("duplicate spline locations must be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals("Please register points in ascending order", expected.getMessage());
        }

        try {
            Climate.Parameter.span(1.0F, -1.0F);
            fail("reversed climate spans must be rejected");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    @Test
    public void climateIndexMatchesBruteForceAcrossMultiLevelTree() {
        List<Climate.Entry<Integer>> entries = new ArrayList<Climate.Entry<Integer>>();
        for (int i = 0; i < 64; ++i) {
            entries.add(new Climate.Entry<Integer>(Climate.parameters(
                axis(i, 3), axis(i, 5), axis(i, 7), axis(i, 11), axis(i, 13), axis(i, 17),
                (i % 9) * 0.0075F), i));
        }
        Climate.ParameterList<Integer> index = new Climate.ParameterList<Integer>(entries);
        for (int i = 0; i < 1024; ++i) {
            Climate.TargetPoint target = Climate.target(
                axis(i, 19), axis(i, 23), axis(i, 29), axis(i, 31), axis(i, 37), axis(i, 41));
            assertEquals("indexed climate result at sample " + i,
                index.findValueBruteForce(target), index.findValue(target));
        }
    }

    private static CubicSpline<Float> fixtureSpline() {
        return CubicSpline.builder((Float value) -> value)
            .addPoint(-1.0F, -0.75F, 0.25F)
            .addPoint(-0.2F, 0.5F, -0.4F)
            .addPoint(0.7F, 1.25F, 0.8F)
            .build();
    }

    private static Climate.ParameterPoint firstClimatePoint() {
        return Climate.parameters(-0.5F, 0.1F, -0.2F, 0.3F, 0.0F, 0.4F, 0.05F);
    }

    private static Climate.ParameterPoint secondClimatePoint() {
        return Climate.parameters(0.7F, -0.6F, 0.8F, -0.4F, 0.2F, -0.1F, 0.0F);
    }

    private static Climate.TargetPoint climateTarget() {
        return Climate.target(-0.75F, 0.25F, -0.5F, 0.5F, -0.125F, 0.875F);
    }

    private static Climate.ParameterList<String> climateList() {
        return new Climate.ParameterList<String>(Arrays.asList(
            new Climate.Entry<String>(firstClimatePoint(), "first"),
            new Climate.Entry<String>(secondClimatePoint(), "second"),
            new Climate.Entry<String>(Climate.parameters(
                -1.2F, -1.0F, -0.9F, -0.8F, -0.7F, -0.6F, 0.2F), "third")
        ));
    }

    private static float axis(int value, int multiplier) {
        int mixed = value * multiplier * 73 + multiplier * 17;
        return ((mixed & 2047) - 1024) / 640.0F;
    }

    private static List<String> fixtureLines() throws IOException {
        InputStream input = V118PrimitiveOracleTest.class.getResourceAsStream(FIXTURE);
        assertNotNull("missing fixture " + FIXTURE, input);
        List<String> lines = new ArrayList<String>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static void assertBits(String message, String expectedUnsignedBits, double actual) {
        assertEquals(message, Long.parseUnsignedLong(expectedUnsignedBits),
            Double.doubleToRawLongBits(actual));
    }

    private static String message(String line, int field) {
        return "oracle mismatch at field " + field + " in " + line;
    }

    private static final class NoiseBundle {
        private final ImprovedNoise improved;
        private final PerlinNoise perlin;
        private final NormalNoise normal;
        private final PerlinNoise legacyPerlin;
        private final NormalNoise legacyNormal;

        private NoiseBundle(long seed) {
            improved = new ImprovedNoise(new XoroshiroRandomSource(seed));
            perlin = PerlinNoise.create(new XoroshiroRandomSource(seed), -3,
                AMPLITUDES[0], AMPLITUDES[1], AMPLITUDES[2], AMPLITUDES[3]);
            normal = NormalNoise.create(new XoroshiroRandomSource(seed), -3, AMPLITUDES);
            legacyPerlin = PerlinNoise.createLegacy(new XoroshiroRandomSource(seed), -3,
                AMPLITUDES[0], AMPLITUDES[1], AMPLITUDES[2], AMPLITUDES[3]);
            legacyNormal = NormalNoise.createLegacy(new XoroshiroRandomSource(seed),
                new NormalNoise.NoiseParameters(-3, AMPLITUDES));
        }
    }
}

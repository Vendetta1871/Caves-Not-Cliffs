package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class BlendedNoiseOracleTest {
    private static final String FIXTURE =
        "/net/celestiald/cavesnotcliffs/worldgen/v118/blended-noise-oracle-1.18.2.tsv";
    private static final List<Long> REQUIRED_SEEDS = Arrays.asList(
        0L, 1L, -1L, 123456789L, Long.MIN_VALUE, Long.MAX_VALUE);

    @Test
    public void matchesOfficialMinecraft1182BaseDensityBitForBit() throws IOException {
        List<Long> seeds = new ArrayList<Long>();
        BlendedNoise noise = null;
        long activeSeed = 0L;
        int samples = 0;

        for (String line : fixtureLines()) {
            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }
            String[] fields = line.split("\\t", -1);
            long seed = Long.parseLong(fields[1]);
            if (noise == null || seed != activeSeed) {
                activeSeed = seed;
                noise = overworldNoise(seed);
            }
            if ("range".equals(fields[0])) {
                seeds.add(seed);
                assertBits(message(line, 2), fields[2], noise.minValue());
                assertBits(message(line, 3), fields[3], noise.maxValue());
            } else if ("sample".equals(fields[0])) {
                assertBits(message(line, 5), fields[5], noise.compute(
                    Integer.parseInt(fields[2]), Integer.parseInt(fields[3]),
                    Integer.parseInt(fields[4])));
                ++samples;
            } else {
                fail("Unknown fixture record: " + line);
            }
        }

        assertEquals(REQUIRED_SEEDS, seeds);
        assertEquals("fixture sample count", 150, samples);
    }

    @Test
    public void samplingIsConstantInsideEachDensityCell() {
        BlendedNoise noise = overworldNoise(123456789L);
        assertEquals(noise.compute(-16, -64, -16), noise.compute(-13, -57, -13), 0.0D);
        assertEquals(noise.compute(0, 0, 0), noise.compute(3, 7, 3), 0.0D);
        assertEquals(noise.compute(16, 16, 16), noise.compute(19, 23, 19), 0.0D);
    }

    private static BlendedNoise overworldNoise(long seed) {
        return new BlendedNoise(new XoroshiroRandomSource(seed),
            V118NoiseSettings.overworld(false).sampling(), 4, 8);
    }

    private static List<String> fixtureLines() throws IOException {
        InputStream input = BlendedNoiseOracleTest.class.getResourceAsStream(FIXTURE);
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
}

package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class V118NoiseParametersOracleTest {
    private static final String FIXTURE =
        "/net/celestiald/cavesnotcliffs/worldgen/v118/noise-parameters-oracle-1.18.2.tsv";

    @Test
    public void registryAndHashedStreamsMatchOfficialMinecraft1182BitForBit()
            throws IOException {
        Set<String> parameterNames = new HashSet<String>();
        Set<Long> seeds = new HashSet<Long>();
        int parameterAssertions = 0;
        int samples = 0;

        for (String line : fixtureLines()) {
            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }
            String[] fields = line.split("\\t", -1);
            if ("parameter".equals(fields[0])) {
                String name = fields[1];
                parameterNames.add(name);
                NormalNoise.NoiseParameters actual = V118NoiseParameters.get(name);
                assertEquals(message(line, 2), Integer.parseInt(fields[2]), actual.firstOctave());
                assertEquals(message(line, 3), fields.length - 3, actual.amplitudes().size());
                for (int index = 3; index < fields.length; ++index) {
                    assertBits(message(line, index), fields[index],
                        actual.amplitudes().get(index - 3));
                }
                parameterAssertions += fields.length - 1;
            } else if ("sample".equals(fields[0])) {
                long seed = Long.parseLong(fields[1]);
                seeds.add(seed);
                NormalNoise noise = V118NoiseParameters.instantiate(fields[2],
                    new XoroshiroRandomSource(seed).forkPositional());
                assertBits(message(line, 3), fields[3],
                    noise.getValue(12.25D, -63.5D, -9.75D));
                assertBits(message(line, 4), fields[4],
                    noise.getValue(-33554433.25D, 319.5D, 33554432.75D));
                samples += 2;
            } else {
                fail("Unknown fixture record: " + line);
            }
        }

        assertEquals("official parameter count", 60, parameterNames.size());
        assertEquals(parameterNames, V118NoiseParameters.all().keySet());
        assertEquals("required seed count", 6, seeds.size());
        assertEquals("sample assertion count", 720, samples);
        assertEquals("parameter assertion count", 315, parameterAssertions);
    }

    @Test
    public void registryIsImmutableAndRejectsUnknownNames() {
        Map<String, NormalNoise.NoiseParameters> parameters = V118NoiseParameters.all();
        assertFalse(parameters.isEmpty());
        try {
            parameters.clear();
            fail("registry must be immutable");
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }
        try {
            V118NoiseParameters.get("not_a_vanilla_noise");
            fail("unknown keys must be rejected");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    private static List<String> fixtureLines() throws IOException {
        InputStream input = V118NoiseParametersOracleTest.class.getResourceAsStream(FIXTURE);
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

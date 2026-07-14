package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class V118DensityInterpolatorOracleTest {
    private static final String FIXTURE =
        "/net/celestiald/cavesnotcliffs/worldgen/v118/noise-chunk-oracle-1.18.2.tsv";

    @Test
    public void realizedRouterDensitiesMatchOfficialNoiseChunkBitForBit()
            throws IOException {
        Map<String, DensityFunction[]> realized = new HashMap<String, DensityFunction[]>();
        Set<String> profiles = new HashSet<String>();
        Set<Long> seeds = new HashSet<Long>();
        Set<String> positions = new HashSet<String>();
        int records = 0;
        int assertions = 0;

        InputStream input = V118DensityInterpolatorOracleTest.class.getResourceAsStream(FIXTURE);
        assertNotNull("missing fixture " + FIXTURE, input);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] fields = line.split("\\t", -1);
                if (!"sample".equals(fields[0])) {
                    fail("Unknown fixture record: " + line);
                }
                String profileName = fields[1];
                long seed = Long.parseLong(fields[2]);
                String key = profileName + ':' + seed;
                DensityFunction[] functions = realized.get(key);
                if (functions == null) {
                    V118NoiseRouterData.Profile profile = profile(profileName);
                    V118NoiseRouter router = V118NoiseRouterData.create(seed, profile);
                    V118NoiseSettings settings = V118NoiseSettings.overworld(profile.amplified());
                    functions = new DensityFunction[] {
                        V118DensityInterpolator.realize(router.finalDensity(), settings),
                        V118DensityInterpolator.realizeFinalDensity(router.finalDensity(),
                            settings),
                        V118DensityInterpolator.realize(router.veinToggle(), settings),
                        V118DensityInterpolator.realize(router.veinRidged(), settings),
                        V118DensityInterpolator.realize(router.veinGap(), settings)
                    };
                    realized.put(key, functions);
                }
                int x = Integer.parseInt(fields[3]);
                int y = Integer.parseInt(fields[4]);
                int z = Integer.parseInt(fields[5]);
                for (int index = 0; index < functions.length; ++index) {
                    assertBits(message(line, index + 6), fields[index + 6],
                        functions[index].compute(x, y, z));
                    ++assertions;
                }
                profiles.add(profileName);
                seeds.add(seed);
                positions.add(x + "," + y + "," + z);
                ++records;
            }
        }

        assertEquals("profile count", 3, profiles.size());
        assertEquals("seed count", 6, seeds.size());
        assertEquals("position count", 32, positions.size());
        assertEquals("fixture records", 576, records);
        assertEquals("raw-bit assertions", 2880, assertions);
    }

    private static V118NoiseRouterData.Profile profile(String name) {
        if ("default".equals(name)) {
            return V118NoiseRouterData.Profile.DEFAULT;
        }
        if ("large_biomes".equals(name)) {
            return V118NoiseRouterData.Profile.LARGE_BIOMES;
        }
        if ("amplified".equals(name)) {
            return V118NoiseRouterData.Profile.AMPLIFIED;
        }
        throw new AssertionError("Unknown profile: " + name);
    }

    private static void assertBits(String message, String expectedUnsignedBits, double actual) {
        assertEquals(message, Long.parseUnsignedLong(expectedUnsignedBits),
            Double.doubleToRawLongBits(actual));
    }

    private static String message(String line, int field) {
        return "NoiseChunk oracle mismatch at field " + field + " in " + line;
    }
}

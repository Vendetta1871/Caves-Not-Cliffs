package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class V118NoiseRouterOracleTest {
    private static final String FIXTURE =
        "/net/celestiald/cavesnotcliffs/worldgen/v118/noise-router-oracle-1.18.2.tsv";

    @Test
    public void allNativeRouterProfilesMatchOfficialMinecraft1182BitForBit()
            throws IOException {
        Map<String, V118NoiseRouter> routers = new HashMap<String, V118NoiseRouter>();
        Set<String> profiles = new HashSet<String>();
        Set<Long> seeds = new HashSet<Long>();
        Set<String> positions = new HashSet<String>();
        int records = 0;
        int assertions = 0;

        for (String line : fixtureLines()) {
            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }
            String[] fields = line.split("\\t", -1);
            if (!"sample".equals(fields[0])) {
                fail("Unknown fixture record: " + line);
            }
            String profile = fields[1];
            long seed = Long.parseLong(fields[2]);
            String routerKey = profile + ':' + seed;
            V118NoiseRouter router = routers.get(routerKey);
            if (router == null) {
                router = V118NoiseRouterData.create(seed, profile(profile));
                routers.put(routerKey, router);
            }
            int x = Integer.parseInt(fields[3]);
            int y = Integer.parseInt(fields[4]);
            int z = Integer.parseInt(fields[5]);
            DensityFunction[] functions = {
                router.barrierNoise(), router.fluidLevelFloodednessNoise(),
                router.fluidLevelSpreadNoise(), router.lavaNoise(),
                router.temperature(), router.humidity(), router.continents(), router.erosion(),
                router.depth(), router.ridges(), router.initialDensityWithoutJaggedness(),
                router.finalDensity(), router.veinToggle(), router.veinRidged(), router.veinGap()
            };
            DensityFunction.SinglePointContext context =
                new DensityFunction.SinglePointContext(x, y, z);
            for (int index = 0; index < functions.length; ++index) {
                assertBits(message(line, index + 6), fields[index + 6],
                    functions[index].compute(context));
                ++assertions;
            }
            profiles.add(profile);
            seeds.add(seed);
            positions.add(x + "," + y + "," + z);
            ++records;
        }

        assertEquals("profile count", 3, profiles.size());
        assertEquals("seed count", 6, seeds.size());
        assertEquals("position count", 25, positions.size());
        assertEquals("fixture records", 450, records);
        assertEquals("raw-bit assertions", 6750, assertions);
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

    private static List<String> fixtureLines() throws IOException {
        InputStream input = V118NoiseRouterOracleTest.class.getResourceAsStream(FIXTURE);
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

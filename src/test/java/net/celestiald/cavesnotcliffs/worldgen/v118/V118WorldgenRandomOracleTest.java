package net.celestiald.cavesnotcliffs.worldgen.v118;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class V118WorldgenRandomOracleTest {
    private static final String FIXTURE =
        "/net/celestiald/cavesnotcliffs/worldgen/v118/decoration-random-oracle-1.18.2.tsv";

    @Test
    public void decorationAndGlobalFeatureStreamsMatchOfficialServerExactly() throws IOException {
        InputStream input = getClass().getResourceAsStream(FIXTURE);
        assertNotNull("missing fixture " + FIXTURE, input);
        Map<Long, Long> decorationSeeds = new HashMap<Long, Long>();
        Set<Long> seeds = new HashSet<Long>();
        int featureRows = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] fields = line.split("\\t", -1);
                long seed = Long.parseLong(fields[1]);
                int x = Integer.parseInt(fields[2]);
                int z = Integer.parseInt(fields[3]);
                seeds.add(seed);
                if ("decoration".equals(fields[0])) {
                    V118WorldgenRandom random = new V118WorldgenRandom(0L);
                    long actual = random.setDecorationSeed(seed, x, z);
                    assertEquals(line, Long.parseLong(fields[4]), actual);
                    decorationSeeds.put(seed, actual);
                } else {
                    V118WorldgenRandom random = new V118WorldgenRandom(0L);
                    random.setFeatureSeed(decorationSeeds.get(seed),
                        Integer.parseInt(fields[4]), Integer.parseInt(fields[5]));
                    assertEquals(line, Integer.parseInt(fields[6]), random.nextInt(16));
                    assertEquals(line, Integer.parseInt(fields[7]), random.nextInt(385));
                    assertEquals(line, Integer.parseUnsignedInt(fields[8]),
                        Float.floatToRawIntBits(random.nextFloat()));
                    assertEquals(line, Long.parseUnsignedLong(fields[9]),
                        Double.doubleToRawLongBits(random.nextDouble()));
                    assertEquals(line, Long.parseUnsignedLong(fields[10]), random.nextLong());
                    ++featureRows;
                }
            }
        }
        assertEquals("edge seed coverage", 6, seeds.size());
        assertEquals("feature streams", 54, featureRows);
    }
}

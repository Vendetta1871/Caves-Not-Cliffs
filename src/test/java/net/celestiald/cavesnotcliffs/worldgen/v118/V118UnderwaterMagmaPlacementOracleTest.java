package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class V118UnderwaterMagmaPlacementOracleTest {
    private static final String FIXTURE = "/net/celestiald/cavesnotcliffs/worldgen/v118/"
            + "underwater-magma-placement-oracle-1.18.2.tsv";

    @Test
    public void countSquareHeightAndSurfaceThresholdMatchEveryOfficialProfile()
            throws IOException {
        InputStream input = getClass().getResourceAsStream(FIXTURE);
        assertNotNull(FIXTURE, input);
        Set<String> profiles = new HashSet<String>();
        Set<Long> seeds = new HashSet<Long>();
        Map<String, String> profileIndependent = new HashMap<String, String>();
        int cases = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input,
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] fields = line.split("\\t", -1);
                String profile = fields[1];
                long seed = Long.parseLong(fields[2]);
                int chunkX = Integer.parseInt(fields[3]);
                int chunkZ = Integer.parseInt(fields[4]);
                V118WorldgenRandom random = new V118WorldgenRandom(0L);
                long decorationSeed = random.setDecorationSeed(seed, chunkX << 4, chunkZ << 4);
                random.setFeatureSeed(decorationSeed,
                        V118UnderwaterMagmaPlacements.GLOBAL_FEATURE_INDEX,
                        V118UnderwaterMagmaPlacements.UNDERGROUND_ORES_STEP);
                List<V118UnderwaterMagmaPlacements.Position> origins =
                        V118UnderwaterMagmaPlacements.samplePlacementOrigins(random,
                            chunkX << 4, chunkZ << 4,
                            (x, z) -> 24 + Math.floorMod(x * 13 + z * 7, 90));
                assertEquals(line, Integer.parseInt(fields[5]), origins.size());
                String encoded = encode(origins);
                assertEquals(line, "-".equals(fields[6]) ? "" : fields[6], encoded);
                String key = seed + ":" + chunkX + ':' + chunkZ;
                String previous = profileIndependent.put(key, encoded);
                if (previous != null) {
                    assertEquals(key, previous, encoded);
                }
                profiles.add(profile);
                seeds.add(seed);
                ++cases;
            }
        }
        assertEquals(3, profiles.size());
        assertEquals(6, seeds.size());
        assertEquals(18, cases);
    }

    private static String encode(List<V118UnderwaterMagmaPlacements.Position> origins) {
        StringBuilder result = new StringBuilder();
        for (V118UnderwaterMagmaPlacements.Position origin : origins) {
            if (result.length() > 0) {
                result.append(';');
            }
            result.append(origin.x()).append(',').append(origin.y()).append(',')
                    .append(origin.z());
        }
        return result.toString();
    }
}

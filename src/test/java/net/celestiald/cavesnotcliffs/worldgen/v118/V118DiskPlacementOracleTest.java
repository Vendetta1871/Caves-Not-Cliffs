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

public class V118DiskPlacementOracleTest {
    private static final String FIXTURE =
            "/net/celestiald/cavesnotcliffs/worldgen/v118/disk-placement-oracle-1.18.2.tsv";

    @Test
    public void countSquareAndOceanFloorHeightmapMatchAllOfficialProfiles()
            throws IOException {
        Map<String, V118DiskPlacements.PlacedDisk> features = new HashMap<>();
        for (V118DiskPlacements.PlacedDisk feature
                : V118DiskPlacements.PlacedDisk.values()) {
            features.put(feature.id(), feature);
        }
        Map<String, String> profileIndependent = new HashMap<>();
        Set<String> profiles = new HashSet<>();
        Set<Long> seeds = new HashSet<>();
        int cases = 0;
        InputStream input = getClass().getResourceAsStream(FIXTURE);
        assertNotNull(FIXTURE, input);
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
                V118DiskPlacements.PlacedDisk feature = features.get(fields[5]);
                assertNotNull(line, feature);
                V118WorldgenRandom random = new V118WorldgenRandom(0L);
                long decorationSeed = random.setDecorationSeed(seed, chunkX << 4,
                        chunkZ << 4);
                random.setFeatureSeed(decorationSeed, feature.globalFeatureIndex(), 6);
                List<V118DiskPlacements.Position> origins = feature.samplePlacementOrigins(
                        random, chunkX << 4, chunkZ << 4,
                        (x, z) -> 40 + Math.floorMod(x * 31 + z * 17, 120));
                assertEquals(line, Integer.parseInt(fields[6]), origins.size());
                String encoded = encode(origins);
                assertEquals(line, "-".equals(fields[7]) ? "" : fields[7], encoded);
                String key = seed + ":" + chunkX + ':' + chunkZ + ':' + feature.id();
                String previous = profileIndependent.put(key, encoded);
                if (previous != null) {
                    assertEquals(key, previous, encoded);
                }
                profiles.add(profile);
                seeds.add(seed);
                cases++;
            }
        }
        assertEquals(3, profiles.size());
        assertEquals(6, seeds.size());
        assertEquals(3 * 6 * V118DiskPlacements.PlacedDisk.values().length, cases);
    }

    private static String encode(List<V118DiskPlacements.Position> origins) {
        StringBuilder result = new StringBuilder();
        for (V118DiskPlacements.Position origin : origins) {
            if (result.length() > 0) {
                result.append(';');
            }
            result.append(origin.x()).append(',').append(origin.y()).append(',')
                    .append(origin.z());
        }
        return result.toString();
    }
}

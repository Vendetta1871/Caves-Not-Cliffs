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
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class V118OrePlacementOracleTest {
    private static final String FIXTURE =
        "/net/celestiald/cavesnotcliffs/worldgen/v118/ore-placement-oracle-1.18.2.tsv";

    @Test
    public void countRaritySquareAndHeightChainsMatchAllOfficialProfiles() throws IOException {
        Map<String, V118OrePlacements.PlacedOre> features =
            new HashMap<String, V118OrePlacements.PlacedOre>();
        for (V118OrePlacements.PlacedOre feature : V118OrePlacements.PlacedOre.values()) {
            features.put(feature.placedId(), feature);
        }
        Map<String, String> profileIndependent = new HashMap<String, String>();
        Set<Long> seeds = new HashSet<Long>();
        Set<String> profiles = new HashSet<String>();
        int cases = 0;
        InputStream input = getClass().getResourceAsStream(FIXTURE);
        assertNotNull("missing fixture " + FIXTURE, input);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
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
                V118OrePlacements.PlacedOre feature = features.get(fields[5]);
                assertNotNull(line, feature);
                V118WorldgenRandom random = new V118WorldgenRandom(0L);
                long decorationSeed = random.setDecorationSeed(seed, chunkX << 4, chunkZ << 4);
                random.setFeatureSeed(decorationSeed, feature.globalFeatureIndex(),
                    feature.decorationStep());
                List<V118OrePlacements.Position> positions = feature.samplePlacementOrigins(
                    random, chunkX << 4, -64, chunkZ << 4, -64, 320);
                assertEquals(line, Integer.parseInt(fields[6]), positions.size());
                String encoded = encode(positions);
                assertEquals(line, fields[7], encoded);
                String key = seed + ":" + chunkX + ':' + chunkZ + ':' + feature.placedId();
                String previous = profileIndependent.put(key, encoded);
                if (previous != null) {
                    assertEquals("profile-independent placement " + key, previous, encoded);
                }
                seeds.add(seed);
                profiles.add(profile);
                ++cases;
            }
        }
        assertEquals("all official profiles", 3, profiles.size());
        assertEquals("all edge seeds", 6, seeds.size());
        assertEquals("feature/profile/seed cases",
            3 * 6 * V118OrePlacements.PlacedOre.values().length, cases);
    }

    private static String encode(List<V118OrePlacements.Position> positions) {
        StringBuilder result = new StringBuilder();
        for (V118OrePlacements.Position position : positions) {
            if (result.length() > 0) {
                result.append(';');
            }
            result.append(position.x()).append(',').append(position.y()).append(',')
                .append(position.z());
        }
        return result.toString();
    }
}

package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class V118DripstonePlacementOracleTest {
    private static final String FIXTURE = "/net/celestiald/cavesnotcliffs/worldgen/v118/"
        + "dripstone-placement-oracle-1.18.2.tsv";

    @Test
    public void completeModifierChainsMatchOfficialAcrossProfilesAndEdgeSeeds()
            throws IOException, NoSuchAlgorithmException {
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
                String[] fields = line.split("\t", -1);
                String profile = fields[1];
                long seed = Long.parseLong(fields[2]);
                int chunkX = Integer.parseInt(fields[3]);
                int chunkZ = Integer.parseInt(fields[4]);
                String id = fields[5];
                V118WorldgenRandom random = new V118WorldgenRandom(0L);
                long decorationSeed = random.setDecorationSeed(seed,
                    chunkX << 4, chunkZ << 4);
                List<V118DripstonePlacements.Position> positions;
                if ("large_dripstone".equals(id)) {
                    random.setFeatureSeed(decorationSeed,
                        V118DripstonePlacements.LARGE_DRIPSTONE_INDEX,
                        V118DripstonePlacements.LOCAL_MODIFICATIONS_STEP);
                    positions = V118DripstonePlacements.sampleSimpleOrigins(random,
                        chunkX, chunkZ, -64,
                        V118DripstonePlacements.LARGE_COUNT_MINIMUM,
                        V118DripstonePlacements.LARGE_COUNT_MAXIMUM);
                } else if ("dripstone_cluster".equals(id)) {
                    random.setFeatureSeed(decorationSeed,
                        V118DripstonePlacements.DRIPSTONE_CLUSTER_INDEX,
                        V118DripstonePlacements.UNDERGROUND_DECORATION_STEP);
                    positions = V118DripstonePlacements.sampleSimpleOrigins(random,
                        chunkX, chunkZ, -64,
                        V118DripstonePlacements.CLUSTER_COUNT_MINIMUM,
                        V118DripstonePlacements.CLUSTER_COUNT_MAXIMUM);
                } else {
                    assertEquals(line, "pointed_dripstone", id);
                    random.setFeatureSeed(decorationSeed,
                        V118DripstonePlacements.POINTED_DRIPSTONE_INDEX,
                        V118DripstonePlacements.UNDERGROUND_DECORATION_STEP);
                    positions = V118DripstonePlacements.samplePointedOrigins(random,
                        chunkX, chunkZ, -64);
                }
                String actual = positions.size() + "\t" + first(positions) + "\t"
                    + last(positions) + "\t" + digest(positions) + "\t"
                    + random.nextLong();
                String expected = fields[6] + "\t" + fields[7] + "\t" + fields[8]
                    + "\t" + fields[9] + "\t" + fields[10];
                assertEquals(line, expected, actual);
                String key = seed + ":" + chunkX + ':' + chunkZ + ':' + id;
                String previous = profileIndependent.put(key, actual);
                if (previous != null) {
                    assertEquals(key, previous, actual);
                }
                profiles.add(profile);
                seeds.add(seed);
                ++cases;
            }
        }
        assertEquals(3, profiles.size());
        assertEquals(6, seeds.size());
        assertEquals(54, cases);
    }

    private static String first(List<V118DripstonePlacements.Position> positions) {
        return positions.isEmpty() ? "-" : encode(positions.get(0));
    }

    private static String last(List<V118DripstonePlacements.Position> positions) {
        return positions.isEmpty() ? "-" : encode(positions.get(positions.size() - 1));
    }

    private static String encode(V118DripstonePlacements.Position position) {
        return position.x() + "," + position.y() + "," + position.z();
    }

    private static String digest(List<V118DripstonePlacements.Position> positions)
            throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (V118DripstonePlacements.Position position : positions) {
            digest.update((encode(position) + "\n").getBytes(StandardCharsets.UTF_8));
        }
        StringBuilder result = new StringBuilder();
        for (byte value : digest.digest()) {
            result.append(String.format("%02x", value & 255));
        }
        return result.toString();
    }
}

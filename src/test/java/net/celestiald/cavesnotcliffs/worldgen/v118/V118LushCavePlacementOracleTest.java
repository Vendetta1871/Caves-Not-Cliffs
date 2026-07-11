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

public class V118LushCavePlacementOracleTest {
    private static final String FIXTURE = "/net/celestiald/cavesnotcliffs/worldgen/v118/"
        + "lush-cave-placement-oracle-1.18.2.tsv";

    @Test
    public void catalogAndCompleteModifierStreamsMatchOfficialRuntime()
            throws IOException, NoSuchAlgorithmException {
        InputStream input = getClass().getResourceAsStream(FIXTURE);
        assertNotNull(FIXTURE, input);
        Map<String, V118LushCavePlacements.PlacedFeature> byId = new HashMap<>();
        for (V118LushCavePlacements.PlacedFeature feature
                : V118LushCavePlacements.PlacedFeature.values()) {
            byId.put(feature.id(), feature);
        }
        Set<String> profiles = new HashSet<>();
        Set<Long> seeds = new HashSet<>();
        Map<String, String> profileIndependent = new HashMap<>();
        int catalogRows = 0;
        int cases = 0;
        PlacementScaffold world = new PlacementScaffold();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input,
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] fields = line.split("\t", -1);
                if ("catalog".equals(fields[0])) {
                    V118LushCavePlacements.PlacedFeature feature = byId.get(fields[1]);
                    assertNotNull(line, feature);
                    assertEquals(line, V118LushCavePlacements.VEGETAL_DECORATION_STEP,
                        Integer.parseInt(fields[2]));
                    assertEquals(line, feature.globalIndex(), Integer.parseInt(fields[3]));
                    ++catalogRows;
                    continue;
                }
                assertEquals(line, "case", fields[0]);
                String profile = fields[1];
                long seed = Long.parseLong(fields[2]);
                int chunkX = Integer.parseInt(fields[3]);
                int chunkZ = Integer.parseInt(fields[4]);
                V118LushCavePlacements.PlacedFeature feature = byId.get(fields[5]);
                assertNotNull(line, feature);
                V118WorldgenRandom random = new V118WorldgenRandom(0L);
                long decorationSeed = random.setDecorationSeed(seed,
                    chunkX << 4, chunkZ << 4);
                random.setFeatureSeed(decorationSeed, feature.globalIndex(),
                    V118LushCavePlacements.VEGETAL_DECORATION_STEP);
                List<V118LushCavePlacements.Position> positions =
                    V118LushCavePlacements.samplePlacementOrigins(feature, random,
                        chunkX, chunkZ, world, false);
                String actual = positions.size() + "\t" + first(positions) + "\t"
                    + last(positions) + "\t" + digest(positions) + "\t"
                    + random.nextLong();
                String expected = fields[6] + "\t" + fields[7] + "\t" + fields[8]
                    + "\t" + fields[9] + "\t" + fields[10];
                assertEquals(line, expected, actual);
                String key = seed + ":" + chunkX + ':' + chunkZ + ':' + feature.id();
                String previous = profileIndependent.put(key, actual);
                if (previous != null) {
                    assertEquals(key, previous, actual);
                }
                profiles.add(profile);
                seeds.add(seed);
                ++cases;
            }
        }
        assertEquals(7, catalogRows);
        assertEquals(3, profiles.size());
        assertEquals(6, seeds.size());
        assertEquals(126, cases);
    }

    private static String first(List<V118LushCavePlacements.Position> positions) {
        return positions.isEmpty() ? "-" : encode(positions.get(0));
    }

    private static String last(List<V118LushCavePlacements.Position> positions) {
        return positions.isEmpty() ? "-" : encode(positions.get(positions.size() - 1));
    }

    private static String encode(V118LushCavePlacements.Position position) {
        return position.x() + "," + position.y() + "," + position.z();
    }

    private static String digest(List<V118LushCavePlacements.Position> positions)
            throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (V118LushCavePlacements.Position position : positions) {
            digest.update((encode(position) + "\n").getBytes(StandardCharsets.UTF_8));
        }
        StringBuilder result = new StringBuilder();
        for (byte value : digest.digest()) {
            result.append(String.format("%02x", value & 255));
        }
        return result.toString();
    }

    private static final class PlacementScaffold
            implements V118LushCavePlacements.PlacementAccess {
        @Override public int minBuildHeight() { return -64; }
        @Override public int maxBuildHeight() { return 320; }

        @Override
        public boolean isAir(int blockX, int blockY, int blockZ) {
            int phase = Math.floorMod(blockY + 64, 16);
            return phase > 3 && phase < 13;
        }

        @Override
        public boolean isSolid(int blockX, int blockY, int blockZ) {
            return !isAir(blockX, blockY, blockZ);
        }

        @Override
        public boolean hasSturdyDownFace(int blockX, int blockY, int blockZ) {
            return isSolid(blockX, blockY, blockZ);
        }

        @Override public V118Biome biomeAt(int x, int y, int z) {
            return V118Biome.LUSH_CAVES;
        }
    }
}

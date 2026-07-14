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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** End-to-end comparison with the official mapped Java 1.18.2 placed geode. */
public class V118GeodeOracleTest {
    private static final String FIXTURE =
        "/net/celestiald/cavesnotcliffs/worldgen/v118/geode-oracle-1.18.2.tsv";

    @Test
    public void placementsShellsCracksBuddingAndGrowthMatchOfficialServer()
            throws IOException, NoSuchAlgorithmException {
        InputStream input = getClass().getResourceAsStream(FIXTURE);
        assertNotNull("missing fixture " + FIXTURE, input);
        Set<Long> seeds = new HashSet<Long>();
        int cases = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] fields = line.split("\t", -1);
                long seed = Long.parseLong(fields[1]);
                int chunkX = Integer.parseInt(fields[2]);
                int chunkZ = Integer.parseInt(fields[3]);
                SparseWorld world = new SparseWorld(chunkX, chunkZ);
                V118GeodePlacements.PlacementResult placement =
                    V118GeodePlacements.decorate(world, seed, chunkX, chunkZ);
                assertTrue(line, placement.attempted());
                assertTrue(line, placement.placed());
                assertEquals(line, Integer.parseInt(fields[4]), placement.originX());
                assertEquals(line, Integer.parseInt(fields[5]), placement.originY());
                assertEquals(line, Integer.parseInt(fields[6]), placement.originZ());
                Summary actual = world.summary();
                String context = line + "\nactual\t" + actual.total + "\t" + actual.counts
                    + "\t" + actual.digest;
                assertEquals(context, Integer.parseInt(fields[7]), actual.total);
                assertEquals(context, fields[8], actual.counts);
                assertEquals(context, fields[9], actual.digest);
                seeds.add(seed);
                ++cases;
            }
        }
        assertEquals("edge seeds", 6, seeds.size());
        assertEquals("geode cases", 6, cases);
    }

    @Test
    public void placedFeatureUsesItsOfficialGlobalSeedSlotAndHeightBand() {
        assertEquals(2, V118GeodePlacements.LOCAL_MODIFICATIONS_STEP);
        assertEquals(2, V118GeodePlacements.GLOBAL_FEATURE_INDEX);
        assertEquals(24, V118GeodePlacements.AVERAGE_CHUNKS_PER_GEODE);
        assertEquals(-58, V118GeodePlacements.MIN_Y);
        assertEquals(30, V118GeodePlacements.MAX_Y);
    }

    private static final class SparseWorld implements V118GeodeFeature.WorldAccess {
        private final Map<Long, V118GeodeFeature.State> overrides =
            new HashMap<Long, V118GeodeFeature.State>();
        private final int minBlockX;
        private final int maxBlockX;
        private final int minBlockZ;
        private final int maxBlockZ;

        private SparseWorld(int chunkX, int chunkZ) {
            minBlockX = (chunkX - 1) << 4;
            maxBlockX = ((chunkX + 2) << 4) - 1;
            minBlockZ = (chunkZ - 1) << 4;
            maxBlockZ = ((chunkZ + 2) << 4) - 1;
        }

        @Override
        public boolean isAir(int x, int y, int z) {
            if (!inside(x, y, z)) {
                return true;
            }
            V118GeodeFeature.State state = overrides.get(pack(x, y, z));
            return state == null ? cavity(x, y, z)
                : state.material() == V118GeodeFeature.Material.AIR;
        }

        @Override
        public boolean isGeodeInvalid(int x, int y, int z) {
            return false;
        }

        @Override
        public boolean canReplace(int x, int y, int z) {
            return inside(x, y, z);
        }

        @Override
        public boolean canClusterGrowAt(int x, int y, int z) {
            return isAir(x, y, z);
        }

        @Override
        public boolean isWaterSource(int x, int y, int z) {
            return false;
        }

        @Override
        public boolean hasFluid(int x, int y, int z) {
            return false;
        }

        @Override
        public void setState(int x, int y, int z, V118GeodeFeature.State state) {
            overrides.put(pack(x, y, z), state);
        }

        private Summary summary() throws NoSuchAlgorithmException {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            TreeMap<String, Integer> counts = new TreeMap<String, Integer>();
            int total = 0;
            for (int x = minBlockX; x <= maxBlockX; ++x) {
                for (int y = TerrainColumn.MIN_Y; y <= TerrainColumn.MAX_Y; ++y) {
                    for (int z = minBlockZ; z <= maxBlockZ; ++z) {
                        V118GeodeFeature.State state = overrides.get(pack(x, y, z));
                        if (state == null || state.material() == V118GeodeFeature.Material.AIR
                                && cavity(x, y, z)) {
                            continue;
                        }
                        String block = state.material().name().toLowerCase(Locale.ROOT);
                        Integer count = counts.get(block);
                        counts.put(block, count == null ? 1 : count + 1);
                        String serialized = block;
                        if (state.material().hasFacing()) {
                            serialized += ":" + state.facing() + ":water="
                                + state.waterlogged();
                        }
                        digest.update((x + "," + y + "," + z + "," + serialized + "\n")
                            .getBytes(StandardCharsets.UTF_8));
                        ++total;
                    }
                }
            }
            StringBuilder countString = new StringBuilder();
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                if (countString.length() > 0) {
                    countString.append(';');
                }
                countString.append(entry.getKey()).append('=').append(entry.getValue());
            }
            return new Summary(total, countString.toString(), hex(digest.digest()));
        }

        private boolean inside(int x, int y, int z) {
            return x >= minBlockX && x <= maxBlockX && z >= minBlockZ && z <= maxBlockZ
                && y >= TerrainColumn.MIN_Y && y <= TerrainColumn.MAX_Y;
        }

        private static boolean cavity(int x, int y, int z) {
            long hash = (long) x * 73428767L ^ (long) y * 912931L
                ^ (long) z * 42317861L;
            return (hash & 127L) == 0L;
        }

        private static long pack(int x, int y, int z) {
            return (x & 0x3FFFFFFL) << 38 | (z & 0x3FFFFFFL) << 12 | y & 0xFFFL;
        }
    }

    private static final class Summary {
        private final int total;
        private final String counts;
        private final String digest;

        private Summary(int total, String counts, String digest) {
            this.total = total;
            this.counts = counts;
            this.digest = digest;
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(String.format("%02x", value & 0xFF));
        }
        return result.toString();
    }
}

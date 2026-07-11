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

public class V118UnderwaterMagmaDecorationOracleTest {
    private static final String FIXTURE = "/net/celestiald/cavesnotcliffs/worldgen/v118/"
            + "underwater-magma-decoration-oracle-1.18.2.tsv";

    @Test
    public void featureSeedFloorScanPlacementOrderAndDigestMatchOfficialServer()
            throws IOException, NoSuchAlgorithmException {
        InputStream input = getClass().getResourceAsStream(FIXTURE);
        assertNotNull(FIXTURE, input);
        Set<Long> seeds = new HashSet<Long>();
        int cases = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input,
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] fields = line.split("\\t", -1);
                long seed = Long.parseLong(fields[1]);
                int chunkX = Integer.parseInt(fields[2]);
                int chunkZ = Integer.parseInt(fields[3]);
                SparseWorld world = new SparseWorld(chunkX, chunkZ);
                V118WorldgenRandom random = new V118WorldgenRandom(0L);
                long decorationSeed = random.setDecorationSeed(seed, chunkX << 4, chunkZ << 4);
                boolean placed = V118UnderwaterMagmaPlacements.decorate(world,
                        decorationSeed, chunkX, chunkZ, random);
                Summary actual = world.summary();
                String context = line + "\nactual\t" + placed + '\t' + actual.total + '\t'
                        + actual.counts + '\t' + actual.digest;
                assertEquals(context, Boolean.parseBoolean(fields[4]), placed);
                assertEquals(context, Integer.parseInt(fields[5]), actual.total);
                assertEquals(context, fields[6], actual.counts);
                assertEquals(context, fields[7], actual.digest);
                seeds.add(seed);
                ++cases;
            }
        }
        assertEquals(6, seeds.size());
        assertEquals(6, cases);
    }

    private static final class SparseWorld implements V118OrePlacements.WorldAccess {
        private final Map<String, V118OreMaterial> overrides =
                new HashMap<String, V118OreMaterial>();
        private final int minBlockX;
        private final int maxBlockX;
        private final int minBlockZ;
        private final int maxBlockZ;

        SparseWorld(int chunkX, int chunkZ) {
            minBlockX = (chunkX - 1) << 4;
            maxBlockX = ((chunkX + 2) << 4) - 1;
            minBlockZ = (chunkZ - 1) << 4;
            maxBlockZ = ((chunkZ + 2) << 4) - 1;
        }

        @Override
        public int minBuildHeight() {
            return -64;
        }

        @Override
        public int maxBuildHeight() {
            return 320;
        }

        @Override
        public V118OreMaterial getMaterial(int x, int y, int z) {
            V118OreMaterial material = overrides.get(key(x, y, z));
            return material == null ? base(x, y, z) : material;
        }

        @Override
        public void setMaterial(int x, int y, int z, V118OreMaterial material) {
            overrides.put(key(x, y, z), material);
        }

        @Override
        public int oceanFloorHeight(int x, int z) {
            return surface(x, z);
        }

        @Override
        public V118Biome biomeAt(int x, int y, int z) {
            return V118Biome.PLAINS;
        }

        Summary summary() throws NoSuchAlgorithmException {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            TreeMap<String, Integer> counts = new TreeMap<String, Integer>();
            int total = 0;
            for (int x = minBlockX; x <= maxBlockX; ++x) {
                for (int y = -64; y < 320; ++y) {
                    for (int z = minBlockZ; z <= maxBlockZ; ++z) {
                        V118OreMaterial material = getMaterial(x, y, z);
                        if (material == base(x, y, z)) {
                            continue;
                        }
                        String name = material == V118OreMaterial.MAGMA
                                ? "magma_block" : material.name().toLowerCase(Locale.ROOT);
                        Integer count = counts.get(name);
                        counts.put(name, count == null ? 1 : count + 1);
                        digest.update((x + "," + y + "," + z + "," + name + "\n")
                                .getBytes(StandardCharsets.UTF_8));
                        ++total;
                    }
                }
            }
            StringBuilder encoded = new StringBuilder();
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                if (encoded.length() > 0) {
                    encoded.append(';');
                }
                encoded.append(entry.getKey()).append('=').append(entry.getValue());
            }
            return new Summary(total, encoded.toString(), hex(digest.digest()));
        }

        private static int surface(int x, int z) {
            return 96 + Math.floorMod(x * 13 + z * 7, 16);
        }

        private static V118OreMaterial base(int x, int y, int z) {
            if (y > surface(x, z)) {
                return V118OreMaterial.AIR;
            }
            int phase = Math.floorMod(y + 64, 32);
            return y < 80 && phase >= 4 && phase <= 7
                    ? V118OreMaterial.WATER : V118OreMaterial.STONE;
        }

        private static String key(int x, int y, int z) {
            return x + ":" + y + ":" + z;
        }

        private static String hex(byte[] bytes) {
            StringBuilder result = new StringBuilder();
            for (byte value : bytes) {
                result.append(String.format("%02x", value & 255));
            }
            return result.toString();
        }
    }

    private static final class Summary {
        private final int total;
        private final String counts;
        private final String digest;

        Summary(int total, String counts, String digest) {
            this.total = total;
            this.counts = counts;
            this.digest = digest;
        }
    }
}

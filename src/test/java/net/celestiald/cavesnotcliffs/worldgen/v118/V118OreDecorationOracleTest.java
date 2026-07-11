package net.celestiald.cavesnotcliffs.worldgen.v118;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.Test;

/** End-to-end comparison of this scoped decorator against official 1.18.2 feature execution. */
public class V118OreDecorationOracleTest {
    private static final String FIXTURE =
        "/net/celestiald/cavesnotcliffs/worldgen/v118/ore-decoration-oracle-1.18.2.tsv";

    @Test
    public void finalMaterialCountsAndExactPlacementDigestsMatchOfficialServer()
            throws IOException, NoSuchAlgorithmException {
        InputStream input = getClass().getResourceAsStream(FIXTURE);
        assertNotNull("missing fixture " + FIXTURE, input);
        Set<Long> seeds = new HashSet<Long>();
        Set<V118Biome> biomes = EnumSet.noneOf(V118Biome.class);
        int cases = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] fields = line.split("\\t", -1);
                long seed = Long.parseLong(fields[1]);
                int chunkX = Integer.parseInt(fields[2]);
                int chunkZ = Integer.parseInt(fields[3]);
                V118Biome biome = V118Biome.valueOf(fields[4].toUpperCase(Locale.ROOT));
                SparseWorld world = new SparseWorld(chunkX, chunkZ, biome);
                V118OrePlacements.decorate(world, seed, chunkX, chunkZ, EnumSet.of(biome));
                Summary actual = world.summary();
                String context = line + "\nactual\t" + actual.total + "\t" + actual.counts
                    + "\t" + actual.digest;
                assertEquals(context, Integer.parseInt(fields[5]), actual.total);
                assertEquals(context, fields[6], actual.counts);
                assertEquals(context, fields[7], actual.digest);
                seeds.add(seed);
                biomes.add(biome);
                ++cases;
            }
        }
        assertEquals("edge seeds", 6, seeds.size());
        assertEquals("membership representatives", 4, biomes.size());
        assertEquals("decoration cases", 6, cases);
    }

    @Test
    public void targetReplacementFamiliesCoverAllVanillaBaseStoneMembers() {
        V118Material[] natural = {
            V118Material.STONE, V118Material.DEEPSLATE, V118Material.GRANITE,
            V118Material.DIORITE, V118Material.ANDESITE, V118Material.TUFF
        };
        for (V118Material material : natural) {
            assertTrue(material.name(),
                V118OreFeature.TargetRule.NATURAL_STONE.matches(material));
        }
        V118Material[] shallow = {
            V118Material.STONE, V118Material.GRANITE,
            V118Material.DIORITE, V118Material.ANDESITE
        };
        for (V118Material material : shallow) {
            assertTrue(material.name(),
                V118OreFeature.TargetRule.STONE_ORE_REPLACEABLES.matches(material));
        }
        V118Material[] deep = {V118Material.DEEPSLATE, V118Material.TUFF};
        for (V118Material material : deep) {
            assertTrue(material.name(),
                V118OreFeature.TargetRule.DEEPSLATE_ORE_REPLACEABLES.matches(material));
        }
        assertFalse(V118OreFeature.TargetRule.NATURAL_STONE.matches(V118Material.DIRT));
        assertFalse(V118OreFeature.TargetRule.NATURAL_STONE.matches(V118Material.GRAVEL));
        assertFalse(V118OreFeature.TargetRule.STONE_ORE_REPLACEABLES.matches(
            V118Material.DEEPSLATE));
        assertFalse(V118OreFeature.TargetRule.STONE_ORE_REPLACEABLES.matches(
            V118Material.TUFF));
        assertFalse(V118OreFeature.TargetRule.DEEPSLATE_ORE_REPLACEABLES.matches(
            V118Material.STONE));
        assertFalse(V118OreFeature.TargetRule.DEEPSLATE_ORE_REPLACEABLES.matches(
            V118Material.GRANITE));
    }

    private static final class SparseWorld implements V118OrePlacements.WorldAccess {
        private final Map<Long, V118Material> overrides = new HashMap<Long, V118Material>();
        private final int minBlockX;
        private final int maxBlockX;
        private final int minBlockZ;
        private final int maxBlockZ;
        private final V118Biome biome;

        SparseWorld(int chunkX, int chunkZ, V118Biome biome) {
            minBlockX = (chunkX - 1) << 4;
            maxBlockX = ((chunkX + 2) << 4) - 1;
            minBlockZ = (chunkZ - 1) << 4;
            maxBlockZ = ((chunkZ + 2) << 4) - 1;
            this.biome = biome;
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
        public V118Material getMaterial(int x, int y, int z) {
            if (!inside(x, y, z)) {
                return V118Material.AIR;
            }
            V118Material override = overrides.get(pack(x, y, z));
            return override == null ? base(x, y, z) : override;
        }

        @Override
        public void setMaterial(int x, int y, int z, V118Material material) {
            overrides.put(pack(x, y, z), material);
        }

        @Override
        public int oceanFloorHeight(int x, int z) {
            return 319;
        }

        @Override
        public boolean ensureCanWrite(int x, int y, int z) {
            return inside(x, y, z);
        }

        @Override
        public V118Biome biomeAt(int x, int y, int z) {
            return biome;
        }

        Summary summary() throws NoSuchAlgorithmException {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            TreeMap<String, Integer> counts = new TreeMap<String, Integer>();
            int total = 0;
            for (int x = minBlockX; x <= maxBlockX; ++x) {
                for (int y = -64; y < 320; ++y) {
                    for (int z = minBlockZ; z <= maxBlockZ; ++z) {
                        V118Material material = getMaterial(x, y, z);
                        if (material == base(x, y, z)) {
                            continue;
                        }
                        String name = material.name().toLowerCase(Locale.ROOT);
                        Integer count = counts.get(name);
                        counts.put(name, count == null ? 1 : count + 1);
                        String row = x + "," + y + "," + z + "," + name + "\n";
                        digest.update(row.getBytes(StandardCharsets.UTF_8));
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
                && y >= -64 && y < 320;
        }

        private static V118Material base(int x, int y, int z) {
            if (cavity(x, y, z)) {
                return V118Material.AIR;
            }
            return y < 0 ? V118Material.DEEPSLATE : V118Material.STONE;
        }

        private static boolean cavity(int x, int y, int z) {
            long hash = (long) x * 73428767L ^ (long) y * 912931L
                ^ (long) z * 42317861L;
            return (hash & 127L) == 0L;
        }

        private static long pack(int x, int y, int z) {
            return (x & 0x3FFFFFFL) << 38 | (z & 0x3FFFFFFL) << 12 | y & 0xFFFL;
        }

        private static String hex(byte[] bytes) {
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                result.append(String.format("%02x", value & 0xFF));
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

package net.celestiald.cavesnotcliffs.worldgen.v118;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class V118OreFeatureOracleTest {
    private static final String FIXTURE =
        "/net/celestiald/cavesnotcliffs/worldgen/v118/ore-shape-oracle-1.18.2.tsv";

    @Test
    public void ellipsoidsTargetsExposureAndBuildBoundariesMatchOfficialServer()
            throws IOException {
        InputStream input = getClass().getResourceAsStream(FIXTURE);
        assertNotNull("missing fixture " + FIXTURE, input);
        Set<Long> seeds = new HashSet<Long>();
        Set<Integer> sizes = new HashSet<Integer>();
        Set<Integer> discardBits = new HashSet<Integer>();
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
                int caseIndex = Integer.parseInt(fields[4]);
                int size = Integer.parseInt(fields[5]);
                int rawDiscard = Integer.parseUnsignedInt(fields[6]);
                int x = Integer.parseInt(fields[7]);
                int y = Integer.parseInt(fields[8]);
                int z = Integer.parseInt(fields[9]);
                SparseWorld world = new SparseWorld(x, y, z);
                V118OreFeature.Configuration configuration = new V118OreFeature.Configuration(
                    Arrays.asList(
                        new V118OreFeature.Target(
                            V118OreFeature.TargetRule.STONE_ORE_REPLACEABLES,
                            V118Material.GOLD_ORE),
                        new V118OreFeature.Target(
                            V118OreFeature.TargetRule.DEEPSLATE_ORE_REPLACEABLES,
                            V118Material.DEEPSLATE_GOLD_ORE)),
                    size, Float.intBitsToFloat(rawDiscard));
                V118WorldgenRandom random = new V118WorldgenRandom(0L);
                long decorationSeed = random.setDecorationSeed(seed, chunkX << 4, chunkZ << 4);
                random.setFeatureSeed(decorationSeed, caseIndex, 6);
                boolean success = V118OreFeature.place(world, random, configuration, x, y, z);
                assertEquals(line, "1".equals(fields[10]), success);
                String changed = world.changed();
                assertEquals(line, Integer.parseInt(fields[11]),
                    changed.isEmpty() ? 0 : changed.split(";", -1).length);
                assertEquals(line, fields[12], changed);
                seeds.add(seed);
                sizes.add(size);
                discardBits.add(rawDiscard);
                ++cases;
            }
        }
        assertEquals("edge seeds", 6, seeds.size());
        assertEquals("configured sizes", 9, sizes.size());
        assertEquals("discard variants", 4, discardBits.size());
        assertEquals("shape cases", 54, cases);
    }

    private static final class SparseWorld implements V118OreFeature.WorldAccess {
        private final Map<Long, V118Material> blocks = new HashMap<Long, V118Material>();
        private final Set<Long> chunks = new HashSet<Long>();
        private final int minX;
        private final int maxX;
        private final int minY;
        private final int maxY;
        private final int minZ;
        private final int maxZ;

        SparseWorld(int x, int y, int z) {
            minX = x - 22;
            maxX = x + 22;
            minY = Math.max(-64, y - 22);
            maxY = Math.min(319, y + 22);
            minZ = z - 22;
            maxZ = z + 22;
            for (int chunkX = Math.floorDiv(minX, 16);
                    chunkX <= Math.floorDiv(maxX, 16); ++chunkX) {
                for (int chunkZ = Math.floorDiv(minZ, 16);
                        chunkZ <= Math.floorDiv(maxZ, 16); ++chunkZ) {
                    chunks.add(chunkKey(chunkX, chunkZ));
                }
            }
            for (int blockX = minX; blockX <= maxX; ++blockX) {
                for (int blockZ = minZ; blockZ <= maxZ; ++blockZ) {
                    for (int blockY = minY; blockY <= maxY; ++blockY) {
                        if (!cavity(blockX, blockY, blockZ)) {
                            setMaterial(blockX, blockY, blockZ,
                                blockY < 0 ? V118Material.DEEPSLATE : V118Material.STONE);
                        }
                    }
                }
            }
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
            V118Material material = blocks.get(blockKey(x, y, z));
            return material == null ? V118Material.AIR : material;
        }

        @Override
        public void setMaterial(int x, int y, int z, V118Material material) {
            if (material == V118Material.AIR) {
                blocks.remove(blockKey(x, y, z));
            } else {
                blocks.put(blockKey(x, y, z), material);
            }
        }

        @Override
        public int oceanFloorHeight(int x, int z) {
            return 319;
        }

        @Override
        public boolean ensureCanWrite(int x, int y, int z) {
            return y >= -64 && y < 320
                && chunks.contains(chunkKey(Math.floorDiv(x, 16), Math.floorDiv(z, 16)));
        }

        String changed() {
            StringBuilder result = new StringBuilder();
            for (int x = minX; x <= maxX; ++x) {
                for (int y = minY; y <= maxY; ++y) {
                    for (int z = minZ; z <= maxZ; ++z) {
                        V118Material material = getMaterial(x, y, z);
                        if (material != V118Material.GOLD_ORE
                                && material != V118Material.DEEPSLATE_GOLD_ORE) {
                            continue;
                        }
                        if (result.length() > 0) {
                            result.append(';');
                        }
                        result.append(x).append(',').append(y).append(',').append(z).append(',')
                            .append(material.name().toLowerCase(java.util.Locale.ROOT));
                    }
                }
            }
            return result.toString();
        }

        private static boolean cavity(int x, int y, int z) {
            long hash = (long) x * 73428767L ^ (long) y * 912931L ^ (long) z * 42317861L;
            return (hash & 63L) == 0L;
        }

        private static long chunkKey(int x, int z) {
            return (long) x << 32 ^ z & 0xFFFFFFFFL;
        }

        private static long blockKey(int x, int y, int z) {
            return (x & 0x3FFFFFFL) << 38 | (z & 0x3FFFFFFL) << 12 | y & 0xFFFL;
        }
    }
}

package net.celestiald.cavesnotcliffs.worldgen.v118;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class V118SurfaceMaterialOracleTest {
    private static final String FIXTURE =
        "/net/celestiald/cavesnotcliffs/worldgen/v118/surface-materials-oracle-1.18.2.tsv";
    private static final int[] TOPS = {50, 62, 63, 74, 96, 100, 150, 270};

    @Test
    public void everyOverworldBiomeAndEmittedMaterialMatchesOfficialServer() throws IOException {
        InputStream input = getClass().getResourceAsStream(FIXTURE);
        assertNotNull("missing fixture " + FIXTURE, input);
        Map<Long, V118PreliminarySurface> preliminary =
            new HashMap<Long, V118PreliminarySurface>();
        Set<V118Biome> biomes = EnumSet.noneOf(V118Biome.class);
        Set<Long> seeds = new java.util.HashSet<Long>();
        Set<V118Material> observed = EnumSet.noneOf(V118Material.class);
        int cases = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] fields = line.split("\\t", -1);
                if (!"case".equals(fields[0])) {
                    fail("unknown fixture row: " + line);
                }
                long seed = Long.parseLong(fields[1]);
                int chunkX = Integer.parseInt(fields[2]);
                int chunkZ = Integer.parseInt(fields[3]);
                V118Biome biome = V118Biome.fromId(fields[4]);
                V118PreliminarySurface lookup = preliminary.get(seed);
                if (lookup == null) {
                    V118NoiseSettings settings = V118NoiseSettings.overworld(false);
                    V118NoiseRouter router = V118NoiseRouterData.create(seed,
                        V118NoiseRouterData.Profile.DEFAULT);
                    lookup = new V118PreliminarySurface(settings,
                        router.initialDensityWithoutJaggedness());
                    preliminary.put(seed, lookup);
                }
                SyntheticAccess access = new SyntheticAccess(chunkX, chunkZ, biome, lookup);
                V118SurfaceSystem system = new V118SurfaceSystem(seed);
                system.buildSurface(access, chunkX, chunkZ,
                    V118SurfaceRuleData.overworldLike(false, false, true));
                assertEquals(line, fields[5], access.hash());
                Map<V118Material, Integer> expectedCounts = counts(fields[6]);
                assertEquals(line, expectedCounts, access.counts());
                observed.addAll(expectedCounts.keySet());
                biomes.add(biome);
                seeds.add(seed);
                ++cases;
            }
        }
        assertEquals("biome coverage", V118Biome.values().length, biomes.size());
        assertEquals("seed/coordinate coverage", 8, seeds.size());
        assertEquals("case count", 8 * V118Biome.values().length, cases);
        for (V118Material material : surfaceOutputs()) {
            assertTrue("surface material not exercised: " + material, observed.contains(material));
        }
    }

    private static Set<V118Material> surfaceOutputs() {
        return EnumSet.of(V118Material.BEDROCK, V118Material.DEEPSLATE, V118Material.DIRT,
            V118Material.PODZOL, V118Material.COARSE_DIRT, V118Material.MYCELIUM,
            V118Material.GRASS_BLOCK, V118Material.CALCITE, V118Material.GRAVEL,
            V118Material.SAND, V118Material.SANDSTONE, V118Material.RED_SAND,
            V118Material.RED_SANDSTONE, V118Material.TERRACOTTA,
            V118Material.WHITE_TERRACOTTA, V118Material.ORANGE_TERRACOTTA,
            V118Material.YELLOW_TERRACOTTA, V118Material.BROWN_TERRACOTTA,
            V118Material.RED_TERRACOTTA, V118Material.LIGHT_GRAY_TERRACOTTA,
            V118Material.PACKED_ICE, V118Material.SNOW_BLOCK, V118Material.POWDER_SNOW,
            V118Material.ICE, V118Material.WATER, V118Material.AIR, V118Material.STONE);
    }

    private static final class SyntheticAccess implements V118SurfaceSystem.SurfaceAccess {
        private final int minX;
        private final int minZ;
        private final V118Biome biome;
        private final V118PreliminarySurface preliminary;
        private final V118Material[] blocks = new V118Material[TerrainColumn.BLOCK_COUNT];
        private final int[] heights = new int[256];

        SyntheticAccess(int chunkX, int chunkZ, V118Biome biome,
                V118PreliminarySurface preliminary) {
            minX = chunkX << 4;
            minZ = chunkZ << 4;
            this.biome = biome;
            this.preliminary = preliminary;
            java.util.Arrays.fill(blocks, V118Material.AIR);
            java.util.Arrays.fill(heights, TerrainColumn.MIN_Y - 1);
            for (int localX = 0; localX < 16; ++localX) {
                for (int localZ = 0; localZ < 16; ++localZ) {
                    int top = TOPS[(localX * 5 + localZ * 3) & 7];
                    for (int y = TerrainColumn.MIN_Y; y <= top; ++y) {
                        setRaw(localX, y, localZ, V118Material.STONE);
                    }
                    for (int y = top + 1; y < 63; ++y) {
                        setRaw(localX, y, localZ, V118Material.WATER);
                    }
                    if (((localX + localZ) & 3) == 0) {
                        for (int y = 20; y <= 25; ++y) {
                            setRaw(localX, y, localZ, V118Material.AIR);
                        }
                    }
                    if (((localX * 3 + localZ) & 7) == 1) {
                        for (int y = 35; y <= 38; ++y) {
                            setRaw(localX, y, localZ, V118Material.WATER);
                        }
                    }
                    if (((localX * 7 + localZ * 11) & 15) == 2) {
                        setRaw(localX, 29, localZ, V118Material.AIR);
                        setRaw(localX, 31, localZ, V118Material.AIR);
                    }
                    recomputeHeight(localX, localZ);
                }
            }
        }

        @Override
        public int minBuildHeight() {
            return TerrainColumn.MIN_Y;
        }

        @Override
        public int maxBuildHeight() {
            return TerrainColumn.MAX_Y_EXCLUSIVE;
        }

        @Override
        public V118Material getBlock(int x, int y, int z) {
            if (y < TerrainColumn.MIN_Y || y > TerrainColumn.MAX_Y) {
                return V118Material.AIR;
            }
            return blocks[index(x - minX, y, z - minZ)];
        }

        @Override
        public void setBlock(int x, int y, int z, V118Material material) {
            if (y < TerrainColumn.MIN_Y || y > TerrainColumn.MAX_Y) {
                return;
            }
            int localX = x - minX;
            int localZ = z - minZ;
            setRaw(localX, y, localZ, material);
            if (material != V118Material.AIR && y > heights[localZ * 16 + localX]) {
                heights[localZ * 16 + localX] = y;
            } else if (material == V118Material.AIR
                    && y == heights[localZ * 16 + localX]) {
                recomputeHeight(localX, localZ);
            }
        }

        @Override
        public int worldSurfaceHeight(int x, int z) {
            return heights[(z - minZ) * 16 + x - minX];
        }

        @Override
        public int preliminarySurfaceLevel(int x, int z) {
            return preliminary.preliminarySurfaceLevel(x, z);
        }

        @Override
        public V118Biome biomeAt(int x, int y, int z) {
            return biome;
        }

        Map<V118Material, Integer> counts() {
            Map<V118Material, Integer> result = new HashMap<V118Material, Integer>();
            for (V118Material material : blocks) {
                Integer count = result.get(material);
                result.put(material, count == null ? 1 : count + 1);
            }
            return result;
        }

        String hash() {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                for (int localX = 0; localX < 16; ++localX) {
                    for (int localZ = 0; localZ < 16; ++localZ) {
                        for (int y = TerrainColumn.MIN_Y; y <= TerrainColumn.MAX_Y; ++y) {
                            String id = "minecraft:" + blocks[index(localX, y, localZ)]
                                .name().toLowerCase(Locale.ROOT);
                            digest.update(id.getBytes(StandardCharsets.UTF_8));
                            digest.update((byte) 0);
                        }
                    }
                }
                StringBuilder result = new StringBuilder();
                for (byte value : digest.digest()) {
                    result.append(String.format("%02x", value & 255));
                }
                return result.toString();
            } catch (NoSuchAlgorithmException exception) {
                throw new AssertionError(exception);
            }
        }

        private void setRaw(int localX, int y, int localZ, V118Material material) {
            blocks[index(localX, y, localZ)] = material;
        }

        private void recomputeHeight(int localX, int localZ) {
            int y = TerrainColumn.MAX_Y;
            while (y >= TerrainColumn.MIN_Y
                    && blocks[index(localX, y, localZ)] == V118Material.AIR) {
                --y;
            }
            heights[localZ * 16 + localX] = y;
        }

        private static int index(int localX, int y, int localZ) {
            return (y - TerrainColumn.MIN_Y) * 256 + localZ * 16 + localX;
        }
    }

    private static Map<V118Material, Integer> counts(String encoded) {
        Map<V118Material, Integer> result = new HashMap<V118Material, Integer>();
        for (String entry : encoded.split(",")) {
            int separator = entry.lastIndexOf('=');
            result.put(material(entry.substring(0, separator)),
                Integer.parseInt(entry.substring(separator + 1)));
        }
        return result;
    }

    private static V118Material material(String id) {
        return V118Material.valueOf(id.substring("minecraft:".length())
            .toUpperCase(Locale.ROOT));
    }
}

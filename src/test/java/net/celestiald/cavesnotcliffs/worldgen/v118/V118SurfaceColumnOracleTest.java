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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.Test;

public class V118SurfaceColumnOracleTest {
    private static final String FIXTURE =
        "/net/celestiald/cavesnotcliffs/worldgen/v118/surface-columns-oracle-1.18.2.tsv";

    @Test
    public void rawColumnsAndSurfaceOutputsMatchAllOfficialOverworldProfiles() throws IOException {
        InputStream input = getClass().getResourceAsStream(FIXTURE);
        assertNotNull("missing fixture " + FIXTURE, input);
        OracleCase current = null;
        int caseCount = 0;
        int runRows = 0;
        int sampledBlocks = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] fields = line.split("\\t", -1);
                if ("case".equals(fields[0])) {
                    if (current != null) {
                        verify(current);
                        ++caseCount;
                    }
                    current = new OracleCase(fields);
                } else if ("run".equals(fields[0])) {
                    assertNotNull("run row before case", current);
                    current.blocks.add(new ExpectedBlock(fields));
                    ++runRows;
                    sampledBlocks += Integer.parseInt(fields[6]) - Integer.parseInt(fields[5]) + 1;
                } else {
                    fail("unknown fixture row: " + line);
                }
            }
        }
        if (current != null) {
            verify(current);
            ++caseCount;
        }
        assertEquals("profile/seed cases", 18, caseCount);
        assertEquals("sampled raw/final blocks", 18 * 8 * TerrainColumn.HEIGHT, sampledBlocks);
        assertTrue("RLE fixture must contain nontrivial transitions", runRows > 1000);
    }

    private static void verify(OracleCase expected) {
        V118NoiseRouterData.Profile profile = profile(expected.profile);
        V118TerrainColumnGenerator generator = new V118TerrainColumnGenerator(expected.seed,
            profile, false);
        TerrainColumn raw = generator.column(expected.chunkX, expected.chunkZ);
        MutableSurfaceAccess access = new MutableSurfaceAccess(expected.seed, profile, raw);
        for (ExpectedBlock block : expected.blocks) {
            for (int y = block.startY; y <= block.endY; ++y) {
                assertEquals(block.message("raw", y), block.raw,
                    access.getBlock(block.x, y, block.z));
            }
        }
        assertEquals(expected.message("raw counts"), expected.rawCounts, access.counts());

        new V118SurfaceSystem(expected.seed).buildSurface(access, expected.chunkX,
            expected.chunkZ);
        for (ExpectedBlock block : expected.blocks) {
            for (int y = block.startY; y <= block.endY; ++y) {
                assertEquals(block.message("surface", y), block.result,
                    access.getBlock(block.x, y, block.z));
            }
        }
        assertEquals(expected.message("final counts"), expected.finalCounts, access.counts());

        TerrainColumn integrated = new V118TerrainColumnGenerator(expected.seed, profile)
            .column(expected.chunkX, expected.chunkZ);
        int minX = expected.chunkX << 4;
        int minZ = expected.chunkZ << 4;
        for (ExpectedBlock block : expected.blocks) {
            for (int y = block.startY; y <= block.endY; ++y) {
                V118Material actual = V118Material.fromStorageId(
                    integrated.materialId(block.x - minX, y, block.z - minZ));
                assertEquals(block.message("integrated surface", y), block.result, actual);
            }
        }
        assertEquals(expected.message("integrated final counts"), expected.finalCounts,
            counts(integrated));
    }

    private static V118NoiseRouterData.Profile profile(String name) {
        if ("large".equals(name)) {
            return V118NoiseRouterData.Profile.LARGE_BIOMES;
        }
        if ("amplified".equals(name)) {
            return V118NoiseRouterData.Profile.AMPLIFIED;
        }
        return V118NoiseRouterData.Profile.DEFAULT;
    }

    private static final class MutableSurfaceAccess implements V118SurfaceSystem.SurfaceAccess {
        private final int minX;
        private final int minZ;
        private final V118Material[] blocks = new V118Material[TerrainColumn.BLOCK_COUNT];
        private final int[] heights = new int[TerrainColumn.SURFACE_BIOME_COUNT];
        private final V118PreliminarySurface preliminarySurface;
        private final V118BiomeManager biomeManager;

        MutableSurfaceAccess(long seed, V118NoiseRouterData.Profile profile, TerrainColumn raw) {
            minX = raw.columnX() << 4;
            minZ = raw.columnZ() << 4;
            V118NoiseSettings settings = V118NoiseSettings.overworld(profile.amplified());
            V118NoiseRouter router = V118NoiseRouterData.create(seed, profile);
            preliminarySurface = new V118PreliminarySurface(settings,
                router.initialDensityWithoutJaggedness());
            final V118ClimateSampler climate = new V118ClimateSampler(router, settings,
                new OverworldBiomeBuilder());
            biomeManager = new V118BiomeManager(new V118BiomeManager.NoiseBiomeSource() {
                @Override
                public V118Biome getNoiseBiome(int quartX, int quartY, int quartZ) {
                    return climate.resolveQuart(quartX, quartY, quartZ);
                }
            }, seed);
            java.util.Arrays.fill(heights, TerrainColumn.MIN_Y - 1);
            for (int localX = 0; localX < 16; ++localX) {
                for (int localZ = 0; localZ < 16; ++localZ) {
                    for (int y = TerrainColumn.MIN_Y; y <= TerrainColumn.MAX_Y; ++y) {
                        V118Material material = V118Material.fromStorageId(
                            raw.materialId(localX, y, localZ));
                        blocks[index(localX, y, localZ)] = material;
                        if (material != V118Material.AIR) {
                            heights[heightIndex(localX, localZ)] = y;
                        }
                    }
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
            blocks[index(localX, y, localZ)] = material;
            int heightIndex = heightIndex(localX, localZ);
            if (material != V118Material.AIR && y > heights[heightIndex]) {
                heights[heightIndex] = y;
            } else if (material == V118Material.AIR && y == heights[heightIndex]) {
                int scan = y - 1;
                while (scan >= TerrainColumn.MIN_Y
                        && getBlock(x, scan, z) == V118Material.AIR) {
                    --scan;
                }
                heights[heightIndex] = scan;
            }
        }

        @Override
        public int worldSurfaceHeight(int x, int z) {
            return heights[heightIndex(x - minX, z - minZ)];
        }

        @Override
        public int preliminarySurfaceLevel(int x, int z) {
            return preliminarySurface.preliminarySurfaceLevel(x, z);
        }

        @Override
        public V118Biome biomeAt(int x, int y, int z) {
            return biomeManager.getBiome(x, y, z);
        }

        Map<V118Material, Integer> counts() {
            Map<V118Material, Integer> result = new HashMap<V118Material, Integer>();
            for (V118Material material : blocks) {
                Integer count = result.get(material);
                result.put(material, count == null ? 1 : count + 1);
            }
            return result;
        }

        private static int index(int localX, int y, int localZ) {
            return (y - TerrainColumn.MIN_Y) * 256 + localZ * 16 + localX;
        }

        private static int heightIndex(int localX, int localZ) {
            return localZ * 16 + localX;
        }
    }

    private static final class OracleCase {
        final String profile;
        final long seed;
        final int chunkX;
        final int chunkZ;
        final Map<V118Material, Integer> rawCounts;
        final Map<V118Material, Integer> finalCounts;
        final List<ExpectedBlock> blocks = new ArrayList<ExpectedBlock>();

        OracleCase(String[] fields) {
            profile = fields[1];
            seed = Long.parseLong(fields[2]);
            chunkX = Integer.parseInt(fields[3]);
            chunkZ = Integer.parseInt(fields[4]);
            rawCounts = counts(fields[7]);
            finalCounts = counts(fields[8]);
        }

        String message(String detail) {
            return profile + " seed=" + seed + " chunk=" + chunkX + ',' + chunkZ + ' '
                + detail;
        }
    }

    private static final class ExpectedBlock {
        final String profile;
        final long seed;
        final int x;
        final int z;
        final int startY;
        final int endY;
        final V118Material raw;
        final V118Material result;

        ExpectedBlock(String[] fields) {
            profile = fields[1];
            seed = Long.parseLong(fields[2]);
            x = Integer.parseInt(fields[3]);
            z = Integer.parseInt(fields[4]);
            startY = Integer.parseInt(fields[5]);
            endY = Integer.parseInt(fields[6]);
            raw = material(fields[7]);
            result = material(fields[8]);
        }

        String message(String stage, int y) {
            return stage + ' ' + profile + " seed=" + seed + " at " + x + ',' + y + ',' + z;
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

    private static Map<V118Material, Integer> counts(TerrainColumn column) {
        Map<V118Material, Integer> result = new HashMap<V118Material, Integer>();
        for (int y = TerrainColumn.MIN_Y; y <= TerrainColumn.MAX_Y; ++y) {
            for (int z = 0; z < TerrainColumn.WIDTH; ++z) {
                for (int x = 0; x < TerrainColumn.WIDTH; ++x) {
                    V118Material material = V118Material.fromStorageId(
                        column.materialId(x, y, z));
                    Integer count = result.get(material);
                    result.put(material, count == null ? 1 : count + 1);
                }
            }
        }
        return result;
    }

    private static V118Material material(String id) {
        String path = id.substring("minecraft:".length());
        return V118Material.valueOf(path.toUpperCase(Locale.ROOT));
    }
}

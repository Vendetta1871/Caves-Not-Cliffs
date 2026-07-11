package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class V118CarverOracleTest {
    private static final String FIXTURE =
        "/net/celestiald/cavesnotcliffs/worldgen/v118/carver-oracle-1.18.2.tsv";

    @Test
    public void configuredCatalogMatchesOfficialRawFloatValues() throws IOException {
        InputStream input = getClass().getResourceAsStream(FIXTURE);
        assertNotNull("missing fixture " + FIXTURE, input);
        int entries = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input,
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("catalog\t")) {
                    continue;
                }
                String[] fields = line.split("\t", -1);
                V118OverworldCarvers.Entry entry = V118OverworldCarvers.catalog().get(
                    Integer.parseInt(fields[2]));
                assertEquals(line, fields[1], entry.id());
                assertEquals(line, Integer.parseInt(fields[2]), entry.index());
                V118CarverConfiguration configuration = entry.configuration();
                assertFloatBits(line, fields[3], configuration.probability());
                assertEquals(line, Integer.parseInt(fields[4]), configuration.minimumY());
                assertEquals(line, Integer.parseInt(fields[5]), configuration.maximumY());
                assertFloatBits(line, fields[6], configuration.minimumYScale());
                assertFloatBits(line, fields[7], configuration.maximumYScale());
                assertEquals(line, Integer.parseInt(fields[8]), configuration.lavaLevel());
                if (configuration instanceof V118CaveWorldCarver.Configuration) {
                    V118CaveWorldCarver.Configuration cave =
                        (V118CaveWorldCarver.Configuration) configuration;
                    assertFloatBits(line, fields[9], cave.minimumHorizontalRadius());
                    assertFloatBits(line, fields[10], cave.maximumHorizontalRadius());
                    assertFloatBits(line, fields[11], cave.minimumVerticalRadius());
                    assertFloatBits(line, fields[12], cave.maximumVerticalRadius());
                    assertFloatBits(line, fields[13], cave.minimumFloor());
                    assertFloatBits(line, fields[14], cave.maximumFloor());
                } else {
                    V118CanyonWorldCarver.Configuration canyon =
                        (V118CanyonWorldCarver.Configuration) configuration;
                    assertFloatBits(line, fields[9], canyon.minimumVerticalRotation());
                    assertFloatBits(line, fields[10], canyon.maximumVerticalRotation());
                    assertFloatBits(line, fields[11], canyon.minimumDistanceFactor());
                    assertFloatBits(line, fields[12], canyon.maximumDistanceFactor());
                    assertFloatBits(line, fields[13], canyon.minimumThickness());
                    assertFloatBits(line, fields[14], canyon.maximumThickness());
                    assertFloatBits(line, fields[15], canyon.thicknessPlateau());
                    assertEquals(line, Integer.parseInt(fields[16]),
                        canyon.widthSmoothness());
                    assertFloatBits(line, fields[17],
                        canyon.minimumHorizontalRadiusFactor());
                    assertFloatBits(line, fields[18],
                        canyon.maximumHorizontalRadiusFactor());
                    assertFloatBits(line, fields[19],
                        canyon.verticalRadiusDefaultFactor());
                    assertFloatBits(line, fields[20],
                        canyon.verticalRadiusCenterFactor());
                }
                ++entries;
            }
        }
        assertEquals(3, entries);
    }

    @Test
    public void deterministicScaffoldShapesMasksAndAquiferOutputsMatchOfficialServer()
            throws IOException, NoSuchAlgorithmException {
        InputStream input = getClass().getResourceAsStream(FIXTURE);
        assertNotNull("missing fixture " + FIXTURE, input);
        int cases = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input,
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] fields = line.split("\t", -1);
                if (!"scaffold".equals(fields[0])) {
                    continue;
                }
                long seed = Long.parseLong(fields[1]);
                int chunkX = Integer.parseInt(fields[2]);
                int chunkZ = Integer.parseInt(fields[3]);
                ScaffoldWorld world = new ScaffoldWorld(chunkX, chunkZ);
                V118OverworldCarvers.Result result = V118OverworldCarvers.carve(seed,
                    chunkX, chunkZ, world);
                Summary summary = world.summary();
                String context = line + "\nactual\t" + result.starts().size() + '\t'
                    + startCounts(result.starts()) + '\t' + startHash(result.starts()) + '\t'
                    + result.maskCardinality() + '\t' + hashMask(result.mask()) + '\t'
                    + summary.changed + '\t' + summary.minimumChangedY + '\t'
                    + summary.maximumChangedY + '\t' + summary.changedCounts + '\t'
                    + summary.chunkHash + '\t' + summary.postprocessCount + '\t'
                    + summary.postprocessHash;
                assertEquals(context, Integer.parseInt(fields[4]), result.starts().size());
                assertEquals(context, fields[5], startCounts(result.starts()));
                assertEquals(context, fields[6], startHash(result.starts()));
                assertEquals(context, Integer.parseInt(fields[7]), result.maskCardinality());
                assertEquals(context, fields[8], hashMask(result.mask()));
                assertEquals(context, Integer.parseInt(fields[9]), summary.changed);
                assertEquals(context, Integer.parseInt(fields[10]), summary.minimumChangedY);
                assertEquals(context, Integer.parseInt(fields[11]), summary.maximumChangedY);
                assertEquals(context, fields[12], summary.changedCounts);
                assertEquals(context, fields[13], summary.chunkHash);
                assertEquals(context, Integer.parseInt(fields[14]), summary.postprocessCount);
                assertEquals(context, fields[15], summary.postprocessHash);
                ++cases;
            }
        }
        assertEquals(6, cases);
    }

    static String startCounts(List<V118OverworldCarvers.Start> starts) {
        Map<String, Integer> counts = new TreeMap<String, Integer>();
        for (V118OverworldCarvers.Start start : starts) {
            Integer count = counts.get(start.id());
            counts.put(start.id(), count == null ? 1 : count + 1);
        }
        return encodeCounts(counts);
    }

    static String startHash(List<V118OverworldCarvers.Start> starts)
            throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (V118OverworldCarvers.Start start : starts) {
            String value = start.id() + ',' + start.index() + ',' + start.sourceChunkX()
                + ',' + start.sourceChunkZ() + ',' + start.featureSeed() + "\n";
            digest.update(value.getBytes(StandardCharsets.UTF_8));
        }
        return hex(digest.digest());
    }

    static String hashMask(long[] mask) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (long value : mask) {
            for (int shift = 56; shift >= 0; shift -= 8) {
                digest.update((byte) (value >>> shift));
            }
        }
        return hex(digest.digest());
    }

    static String encodeCounts(Map<String, Integer> counts) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (result.length() > 0) {
                result.append(',');
            }
            result.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return result.length() == 0 ? "-" : result.toString();
    }

    static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte value : bytes) {
            result.append(String.format("%02x", value & 255));
        }
        return result.toString();
    }

    private static void assertFloatBits(String message, String expectedBits, float actual) {
        assertEquals(message, Integer.parseInt(expectedBits), Float.floatToRawIntBits(actual));
    }

    private static final class ScaffoldWorld implements V118WorldCarver.WorldAccess {
        private final int chunkX;
        private final int chunkZ;
        private final V118Material[] blocks = new V118Material[TerrainColumn.BLOCK_COUNT];
        private final boolean[] postprocess = new boolean[TerrainColumn.BLOCK_COUNT];
        private boolean lastSchedule;

        ScaffoldWorld(int chunkX, int chunkZ) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            int minX = chunkX << 4;
            int minZ = chunkZ << 4;
            for (int localX = 0; localX < 16; ++localX) {
                for (int y = TerrainColumn.MIN_Y; y <= TerrainColumn.MAX_Y; ++y) {
                    for (int localZ = 0; localZ < 16; ++localZ) {
                        blocks[index(localX, y, localZ)] = scaffold(minX + localX, y,
                            minZ + localZ);
                    }
                }
            }
        }

        @Override
        public int targetChunkX() {
            return chunkX;
        }

        @Override
        public int targetChunkZ() {
            return chunkZ;
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
        public V118Material getMaterial(int x, int y, int z) {
            if (y < TerrainColumn.MIN_Y || y > TerrainColumn.MAX_Y) {
                return V118Material.AIR;
            }
            return blocks[index(x - (chunkX << 4), y, z - (chunkZ << 4))];
        }

        @Override
        public void setMaterial(int x, int y, int z, V118Material material,
                boolean scheduleFluidUpdate) {
            int index = index(x - (chunkX << 4), y, z - (chunkZ << 4));
            blocks[index] = material;
            postprocess[index] = scheduleFluidUpdate;
        }

        @Override
        public V118Material computeAquiferMaterial(int x, int y, int z) {
            int selector = (int) Math.floorMod((long) x * 912931L ^ (long) y * 42317861L
                ^ (long) z * 73428767L, 17L);
            lastSchedule = selector == 1 || selector == 3 || selector == 5;
            if (selector == 0) {
                return null;
            }
            if (selector == 1 || selector == 2) {
                return V118Material.WATER;
            }
            if (selector == 3 || selector == 4) {
                return V118Material.LAVA;
            }
            return V118Material.AIR;
        }

        @Override
        public boolean shouldScheduleAquiferFluidUpdate() {
            return lastSchedule;
        }

        @Override
        public V118Material topMaterial(int blockX, int blockY, int blockZ,
                boolean hasFluidAbove) {
            return null;
        }

        Summary summary() throws NoSuchAlgorithmException {
            MessageDigest chunkDigest = MessageDigest.getInstance("SHA-256");
            Map<String, Integer> changedCounts = new TreeMap<String, Integer>();
            List<String> scheduled = new ArrayList<String>();
            int changed = 0;
            int minimumChangedY = Integer.MAX_VALUE;
            int maximumChangedY = Integer.MIN_VALUE;
            int minX = chunkX << 4;
            int minZ = chunkZ << 4;
            for (int localX = 0; localX < 16; ++localX) {
                for (int y = TerrainColumn.MIN_Y; y <= TerrainColumn.MAX_Y; ++y) {
                    for (int localZ = 0; localZ < 16; ++localZ) {
                        int index = index(localX, y, localZ);
                        V118Material material = blocks[index];
                        String name = name(material);
                        String value = localX + "," + y + "," + localZ + "," + name
                            + "\n";
                        chunkDigest.update(value.getBytes(StandardCharsets.UTF_8));
                        V118Material original = scaffold(minX + localX, y, minZ + localZ);
                        if (material != original) {
                            ++changed;
                            minimumChangedY = Math.min(minimumChangedY, y);
                            maximumChangedY = Math.max(maximumChangedY, y);
                            Integer count = changedCounts.get(name);
                            changedCounts.put(name, count == null ? 1 : count + 1);
                        }
                        if (postprocess[index]) {
                            scheduled.add((minX + localX) + "," + y + ","
                                + (minZ + localZ));
                        }
                    }
                }
            }
            Collections.sort(scheduled);
            MessageDigest postprocessDigest = MessageDigest.getInstance("SHA-256");
            for (String position : scheduled) {
                postprocessDigest.update((position + "\n").getBytes(StandardCharsets.UTF_8));
            }
            return new Summary(changed, minimumChangedY, maximumChangedY,
                encodeCounts(changedCounts), hex(chunkDigest.digest()), scheduled.size(),
                hex(postprocessDigest.digest()));
        }

        private static V118Material scaffold(int x, int y, int z) {
            int selector = (int) Math.floorMod((long) x * 73428767L ^ (long) y * 912931L
                ^ (long) z * 42317861L, 37L);
            if (selector == 0) {
                return V118Material.BEDROCK;
            }
            if (selector == 1) {
                return V118Material.WATER;
            }
            if (selector == 2) {
                return V118Material.TUFF;
            }
            if (selector == 3) {
                return V118Material.CALCITE;
            }
            return y < 0 ? V118Material.DEEPSLATE : V118Material.STONE;
        }

        private static int index(int localX, int y, int localZ) {
            return (y - TerrainColumn.MIN_Y) * 256 + localZ * 16 + localX;
        }

        private static String name(V118Material material) {
            return material.name().toLowerCase(Locale.ROOT);
        }
    }

    private static final class Summary {
        private final int changed;
        private final int minimumChangedY;
        private final int maximumChangedY;
        private final String changedCounts;
        private final String chunkHash;
        private final int postprocessCount;
        private final String postprocessHash;

        Summary(int changed, int minimumChangedY, int maximumChangedY,
                String changedCounts, String chunkHash, int postprocessCount,
                String postprocessHash) {
            this.changed = changed;
            this.minimumChangedY = minimumChangedY;
            this.maximumChangedY = maximumChangedY;
            this.changedCounts = changedCounts;
            this.chunkHash = chunkHash;
            this.postprocessCount = postprocessCount;
            this.postprocessHash = postprocessHash;
        }
    }
}

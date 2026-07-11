package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class V118CarverCatalogTest {
    private static final long[] EDGE_SEEDS = {
        0L, 1L, -1L, 123456789L, Long.MIN_VALUE, Long.MAX_VALUE
    };

    @Test
    public void catalogMatchesTheOfficialOverworldAirCarverOrderAndValues() {
        assertEquals(3, V118OverworldCarvers.catalog().size());
        assertEntry(V118OverworldCarvers.CAVE, "cave", 0, 0.15F, -56, 180,
            0.1F, 0.9F, -56);
        assertEntry(V118OverworldCarvers.CAVE_EXTRA_UNDERGROUND,
            "cave_extra_underground", 1, 0.07F, -56, 47, 0.1F, 0.9F, -56);
        assertEntry(V118OverworldCarvers.CANYON, "canyon", 2, 0.01F, 10, 67,
            3.0F, 3.0F, -56);

        V118CaveWorldCarver.Configuration cave = (V118CaveWorldCarver.Configuration)
            V118OverworldCarvers.CAVE.configuration();
        assertEquals(0.7F, cave.minimumHorizontalRadius(), 0.0F);
        assertEquals(1.4F, cave.maximumHorizontalRadius(), 0.0F);
        assertEquals(0.8F, cave.minimumVerticalRadius(), 0.0F);
        assertEquals(1.3F, cave.maximumVerticalRadius(), 0.0F);
        assertEquals(-1.0F, cave.minimumFloor(), 0.0F);
        assertEquals(-0.4F, cave.maximumFloor(), 0.0F);

        V118CanyonWorldCarver.Configuration canyon = (V118CanyonWorldCarver.Configuration)
            V118OverworldCarvers.CANYON.configuration();
        assertEquals(-0.125F, canyon.minimumVerticalRotation(), 0.0F);
        assertEquals(0.125F, canyon.maximumVerticalRotation(), 0.0F);
        assertEquals(0.75F, canyon.minimumDistanceFactor(), 0.0F);
        assertEquals(1.0F, canyon.maximumDistanceFactor(), 0.0F);
        assertEquals(0.0F, canyon.minimumThickness(), 0.0F);
        assertEquals(6.0F, canyon.maximumThickness(), 0.0F);
        assertEquals(2.0F, canyon.thicknessPlateau(), 0.0F);
        assertEquals(3, canyon.widthSmoothness());
        assertEquals(0.75F, canyon.minimumHorizontalRadiusFactor(), 0.0F);
        assertEquals(1.0F, canyon.maximumHorizontalRadiusFactor(), 0.0F);
        assertEquals(1.0F, canyon.verticalRadiusDefaultFactor(), 0.0F);
        assertEquals(0.0F, canyon.verticalRadiusCenterFactor(), 0.0F);
    }

    @Test
    public void carvingMaskUsesTheOfficialXZYBitLayoutAcrossEveryCubeBoundary() {
        V118CarvingMask mask = new V118CarvingMask(384, -64);
        int expected = 0;
        for (int y : new int[] {-64, -63, -49, -48, -33, -32, -17, -16, -1, 0,
                15, 16, 31, 32, 63, 64, 255, 256, 319}) {
            int x = Math.floorMod(y, 16);
            int z = Math.floorMod(y >> 4, 16);
            assertFalse(mask.get(x, y, z));
            mask.set(x, y, z);
            assertTrue(mask.get(x, y, z));
            ++expected;
        }
        assertEquals(expected, mask.cardinality());
        assertTrue(mask.toLongArray().length > 0);
    }

    @Test
    public void sourceHaloAndFiniteCarveBoundsHoldForEveryRequiredEdgeSeed() {
        int[][] chunks = {{0, 0}, {-1, 0}, {0, -1}, {-1, -1}, {17, -33}, {-33, 17}};
        Set<String> carverKinds = new HashSet<String>();
        int totalStarts = 0;
        for (int index = 0; index < EDGE_SEEDS.length; ++index) {
            RecordingWorld world = new RecordingWorld(chunks[index][0], chunks[index][1]);
            V118OverworldCarvers.Result result = V118OverworldCarvers.carve(
                EDGE_SEEDS[index], chunks[index][0], chunks[index][1], world);
            assertTrue(result.maskCardinality() > 0);
            assertTrue(result.maskCardinality() <= TerrainColumn.BLOCK_COUNT);
            for (V118OverworldCarvers.Start start : result.starts()) {
                assertTrue(start.sourceChunkX() >= chunks[index][0] - 8);
                assertTrue(start.sourceChunkX() <= chunks[index][0] + 8);
                assertTrue(start.sourceChunkZ() >= chunks[index][1] - 8);
                assertTrue(start.sourceChunkZ() <= chunks[index][1] + 8);
                carverKinds.add(start.id());
            }
            totalStarts += result.starts().size();
            assertTrue(world.minimumChangedY >= -62);
            assertTrue(world.maximumChangedY <= 312);
            assertEquals(0, world.scheduledCount);
        }
        assertTrue(totalStarts > 0);
        assertTrue(carverKinds.contains("cave"));
        assertTrue(carverKinds.contains("cave_extra_underground"));
        // A one-percent canyon start is not guaranteed in every small deterministic matrix.
    }

    @Test
    public void largeFeatureSeedResetsTheLegacyStreamBeforeEveryCarver() {
        Random shared = new Random(99L);
        long first = V118OverworldCarvers.setLargeFeatureSeed(shared,
            Long.MIN_VALUE, -17, 33);
        int firstInt = shared.nextInt();
        shared.nextLong();
        long second = V118OverworldCarvers.setLargeFeatureSeed(shared,
            Long.MIN_VALUE, -17, 33);
        assertEquals(first, second);
        assertEquals(firstInt, shared.nextInt());
    }

    private static void assertEntry(V118OverworldCarvers.Entry entry, String id, int index,
            float probability, int minimumY, int maximumY, float minimumYScale,
            float maximumYScale, int lavaLevel) {
        assertEquals(id, entry.id());
        assertEquals(index, entry.index());
        V118CarverConfiguration configuration = entry.configuration();
        assertEquals(probability, configuration.probability(), 0.0F);
        assertEquals(minimumY, configuration.minimumY());
        assertEquals(maximumY, configuration.maximumY());
        assertEquals(minimumYScale, configuration.minimumYScale(), 0.0F);
        assertEquals(maximumYScale, configuration.maximumYScale(), 0.0F);
        assertEquals(lavaLevel, configuration.lavaLevel());
    }

    private static final class RecordingWorld implements V118WorldCarver.WorldAccess {
        private final int chunkX;
        private final int chunkZ;
        private final Map<String, V118Material> overrides =
            new HashMap<String, V118Material>();
        private int minimumChangedY = Integer.MAX_VALUE;
        private int maximumChangedY = Integer.MIN_VALUE;
        private int lavaCount;
        private int scheduledCount;
        private boolean lastSchedule;

        RecordingWorld(int chunkX, int chunkZ) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
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
            V118Material override = overrides.get(key(x, y, z));
            return override == null ? V118Material.STONE : override;
        }

        @Override
        public void setMaterial(int x, int y, int z, V118Material material,
                boolean scheduleFluidUpdate) {
            assertTrue(x >= (chunkX << 4) && x <= (chunkX << 4) + 15);
            assertTrue(z >= (chunkZ << 4) && z <= (chunkZ << 4) + 15);
            assertTrue(y >= TerrainColumn.MIN_Y && y <= TerrainColumn.MAX_Y);
            overrides.put(key(x, y, z), material);
            minimumChangedY = Math.min(minimumChangedY, y);
            maximumChangedY = Math.max(maximumChangedY, y);
            if (material == V118Material.LAVA) {
                ++lavaCount;
                assertTrue(y <= V118OverworldCarvers.LAVA_LEVEL);
            }
            if (scheduleFluidUpdate) {
                ++scheduledCount;
            }
        }

        @Override
        public V118Material computeAquiferMaterial(int blockX, int blockY, int blockZ) {
            lastSchedule = false;
            return V118Material.AIR;
        }

        @Override
        public boolean shouldScheduleAquiferFluidUpdate() {
            return lastSchedule;
        }

        @Override
        public V118Material topMaterial(int blockX, int blockY, int blockZ,
                boolean hasFluidAbove) {
            return V118Material.DIRT;
        }

        private static String key(int x, int y, int z) {
            return x + ":" + y + ":" + z;
        }
    }
}

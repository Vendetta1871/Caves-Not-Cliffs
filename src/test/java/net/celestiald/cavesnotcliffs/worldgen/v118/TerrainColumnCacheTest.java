package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TerrainColumnCacheTest {
    @Test
    public void defaultsToTheRequiredDualLimits() {
        TerrainColumnCache cache = new TerrainColumnCache(this::deterministicColumn);
        assertEquals(64, cache.maxColumns());
        assertEquals(32L * 1024L * 1024L, cache.maxWeightBytes());
    }

    @Test
    public void defaultCountLimitActuallyCapsSixtyFiveGeneratedColumns() {
        TerrainColumnCache cache = new TerrainColumnCache(this::deterministicColumn);
        for (int x = -32; x <= 32; ++x) {
            cache.get(x, -x);
        }
        assertEquals(64, cache.size());
        assertFalse(cache.isCached(-32, 32));
        assertTrue(cache.isCached(-31, 31));
        assertTrue(cache.isCached(32, -32));
        assertTrue(cache.currentWeightBytes() <= cache.maxWeightBytes());
    }

    @Test
    public void evictsByAccessOrderAtTheCountLimit() {
        CountingLoader loader = new CountingLoader(false);
        TerrainColumnCache cache = new TerrainColumnCache(loader, 2, Long.MAX_VALUE);
        TerrainColumn first = cache.get(-1, 0);
        TerrainColumn second = cache.get(0, -1);
        assertSame(first, cache.get(-1, 0));

        cache.get(1, 1);
        assertEquals(2, cache.size());
        assertTrue(cache.isCached(-1, 0));
        assertFalse(cache.isCached(0, -1));
        assertTrue(cache.isCached(1, 1));
        assertEquals(1, loader.loads(-1, 0));
        assertEquals(1, loader.loads(0, -1));

        assertNotSame(second, cache.get(0, -1));
        assertEquals(2, loader.loads(0, -1));
        assertFalse(cache.isCached(-1, 0));
    }

    @Test
    public void distinctNegativeAndExtremeCoordinateKeysNeverAlias() {
        CountingLoader loader = new CountingLoader(false);
        TerrainColumnCache cache = new TerrainColumnCache(loader, 8, Long.MAX_VALUE);
        int[][] coordinates = {
            {0, -1}, {-1, 0}, {-1, -1}, {0, 0},
            {Integer.MIN_VALUE, Integer.MAX_VALUE},
            {Integer.MAX_VALUE, Integer.MIN_VALUE}
        };
        for (int[] coordinate : coordinates) {
            TerrainColumn column = cache.get(coordinate[0], coordinate[1]);
            assertEquals(coordinate[0], column.columnX());
            assertEquals(coordinate[1], column.columnZ());
        }
        assertEquals(coordinates.length, cache.size());
        for (int[] coordinate : coordinates) {
            assertEquals(1, loader.loads(coordinate[0], coordinate[1]));
            assertTrue(cache.isCached(coordinate[0], coordinate[1]));
        }
    }

    @Test
    public void evictsByActualRetainedWeightAndAccountsExactly() {
        TerrainColumn small = TerrainColumn.builder(0, 0).fillMaterialIds(1).build();
        TerrainColumn large = denselyVariedColumn(1, 0);
        long limit = small.estimatedRetainedBytes() + large.estimatedRetainedBytes();
        CountingLoader loader = new CountingLoader(true);
        TerrainColumnCache cache = new TerrainColumnCache(loader, 10, limit);

        TerrainColumn loadedSmall = cache.get(0, 0);
        assertEquals(loadedSmall.estimatedRetainedBytes(), cache.currentWeightBytes());
        TerrainColumn loadedLarge = cache.get(1, 0);
        assertEquals(limit, cache.currentWeightBytes());
        assertEquals(2, cache.size());

        TerrainColumn anotherSmall = cache.get(2, 0);
        assertFalse(cache.isCached(0, 0));
        assertTrue(cache.isCached(1, 0));
        assertTrue(cache.isCached(2, 0));
        assertEquals(loadedLarge.estimatedRetainedBytes()
            + anotherSmall.estimatedRetainedBytes(), cache.currentWeightBytes());

        cache.invalidate(1, 0);
        assertEquals(anotherSmall.estimatedRetainedBytes(), cache.currentWeightBytes());
        cache.invalidate(99, 99);
        assertEquals(anotherSmall.estimatedRetainedBytes(), cache.currentWeightBytes());
        cache.clear();
        assertEquals(0, cache.size());
        assertEquals(0L, cache.currentWeightBytes());
    }

    @Test
    public void valuesLargerThanTheWeightLimitAreReturnedButNeverCached() {
        CountingLoader loader = new CountingLoader(false);
        TerrainColumn sample = loader.load(7, 8);
        TerrainColumnCache cache = new TerrainColumnCache(loader, 64,
            sample.estimatedRetainedBytes() - 1L);

        TerrainColumn first = cache.get(7, 8);
        TerrainColumn second = cache.get(7, 8);
        assertNotSame(first, second);
        assertEquals(0, cache.size());
        assertEquals(0L, cache.currentWeightBytes());
        // One direct setup load plus both cache attempts.
        assertEquals(3, loader.loads(7, 8));
    }

    @Test
    public void anOversizeLoadDoesNotEvictAnExistingEntry() {
        TerrainColumn small = TerrainColumn.builder(0, 0).fillMaterialIds(1).build();
        CountingLoader loader = new CountingLoader(true);
        TerrainColumnCache cache = new TerrainColumnCache(loader, 64,
            small.estimatedRetainedBytes() * 2L);

        TerrainColumn cachedSmall = cache.get(0, 0);
        long cachedWeight = cache.currentWeightBytes();
        TerrainColumn oversize = cache.get(1, 0);
        assertTrue(oversize.estimatedRetainedBytes() > cache.maxWeightBytes());
        assertSame(cachedSmall, cache.get(0, 0));
        assertTrue(cache.isCached(0, 0));
        assertFalse(cache.isCached(1, 0));
        assertEquals(1, cache.size());
        assertEquals(cachedWeight, cache.currentWeightBytes());
    }

    @Test
    public void reloadAfterEvictionIsIndependentOfRequestOrder() {
        TerrainColumnCache firstOrder = new TerrainColumnCache(this::deterministicColumn, 2,
            Long.MAX_VALUE);
        TerrainColumn original = firstOrder.get(-123, 456);
        long originalDigest = digest(original);
        firstOrder.get(1, 1);
        firstOrder.get(2, 2);
        assertFalse(firstOrder.isCached(-123, 456));
        TerrainColumn reloaded = firstOrder.get(-123, 456);
        assertNotSame(original, reloaded);
        assertEquals(originalDigest, digest(reloaded));

        TerrainColumnCache oppositeOrder = new TerrainColumnCache(this::deterministicColumn, 2,
            Long.MAX_VALUE);
        oppositeOrder.get(2, 2);
        oppositeOrder.get(1, 1);
        TerrainColumn independentlyLoaded = oppositeOrder.get(-123, 456);
        assertEquals(originalDigest, digest(independentlyLoaded));
        assertColumnSamplesEqual(original, independentlyLoaded);
    }

    @Test
    public void rejectsNullAndCoordinateMismatchedLoaderResultsWithoutCaching() {
        TerrainColumnCache nullCache = new TerrainColumnCache((x, z) -> null);
        IllegalStateException nullError = expectThrows(IllegalStateException.class,
            () -> nullCache.get(1, 2));
        assertTrue(nullError.getMessage().contains("null"));
        assertEquals(0, nullCache.size());

        TerrainColumnCache mismatchedCache = new TerrainColumnCache(
            (x, z) -> TerrainColumn.builder(x + 1, z).build());
        IllegalStateException coordinateError = expectThrows(IllegalStateException.class,
            () -> mismatchedCache.get(-1, -2));
        assertTrue(coordinateError.getMessage().contains("returned (0, -2)"));
        assertEquals(0, mismatchedCache.size());
    }

    @Test
    public void rejectsReentrantLoadsAndRecoversAfterLoaderFailure() {
        final TerrainColumnCache[] holder = new TerrainColumnCache[1];
        final boolean[] recurse = {true};
        holder[0] = new TerrainColumnCache((x, z) -> {
            if (recurse[0]) {
                recurse[0] = false;
                return holder[0].get(x, z);
            }
            return deterministicColumn(x, z);
        });

        IllegalStateException exception = expectThrows(IllegalStateException.class,
            () -> holder[0].get(3, 4));
        assertTrue(exception.getMessage().contains("Reentrant"));
        assertEquals(0, holder[0].size());

        TerrainColumn recovered = holder[0].get(3, 4);
        assertEquals(3, recovered.columnX());
        assertEquals(4, recovered.columnZ());
        assertEquals(1, holder[0].size());
    }

    @Test
    public void rejectsEveryStatefulAccessFromAnotherThread() throws InterruptedException {
        final TerrainColumnCache cache = new TerrainColumnCache(this::deterministicColumn);
        cache.get(0, 0);
        final AtomicReference<Throwable> getFailure = new AtomicReference<Throwable>();
        final AtomicReference<Throwable> sizeFailure = new AtomicReference<Throwable>();
        Thread foreign = new Thread(() -> {
            try {
                cache.get(1, 1);
            } catch (Throwable throwable) {
                getFailure.set(throwable);
            }
            try {
                cache.size();
            } catch (Throwable throwable) {
                sizeFailure.set(throwable);
            }
        }, "foreign-column-worker");
        foreign.start();
        foreign.join(10_000L);
        assertFalse("foreign worker remained alive", foreign.isAlive());
        assertTrue(getFailure.get() instanceof IllegalStateException);
        assertTrue(getFailure.get().getMessage().contains("foreign-column-worker"));
        assertTrue(sizeFailure.get() instanceof IllegalStateException);
        assertEquals(1, cache.size());
        assertTrue(cache.isCached(0, 0));
        assertFalse(cache.isCached(1, 1));
    }

    @Test
    public void concurrentAccessIsRejectedWhileTheOwnerIsInsideTheLoader() {
        final TerrainColumnCache[] holder = new TerrainColumnCache[1];
        final AtomicReference<Throwable> concurrentFailure = new AtomicReference<Throwable>();
        holder[0] = new TerrainColumnCache((x, z) -> {
            Thread concurrent = new Thread(() -> {
                try {
                    holder[0].get(x + 1, z + 1);
                } catch (Throwable throwable) {
                    concurrentFailure.set(throwable);
                }
            }, "concurrent-column-worker");
            concurrent.start();
            try {
                concurrent.join(10_000L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError(exception);
            }
            assertFalse("concurrent worker remained alive", concurrent.isAlive());
            return deterministicColumn(x, z);
        });

        TerrainColumn loaded = holder[0].get(8, -8);
        assertEquals(8, loaded.columnX());
        assertEquals(-8, loaded.columnZ());
        assertTrue(concurrentFailure.get() instanceof IllegalStateException);
        assertTrue(concurrentFailure.get().getMessage().contains("concurrent-column-worker"));
        assertEquals(1, holder[0].size());
    }

    @Test
    public void validatesLimitsWithoutIntegerOrLongOverflow() {
        expectThrows(NullPointerException.class, () -> new TerrainColumnCache(null));
        expectThrows(IllegalArgumentException.class,
            () -> new TerrainColumnCache(this::deterministicColumn, 0, 1));
        expectThrows(IllegalArgumentException.class,
            () -> new TerrainColumnCache(this::deterministicColumn, 1, 0));
        expectThrows(IllegalArgumentException.class,
            () -> new TerrainColumnCache(this::deterministicColumn, 1, Long.MIN_VALUE));

        TerrainColumnCache enormousLimit = new TerrainColumnCache(this::deterministicColumn,
            Integer.MAX_VALUE, Long.MAX_VALUE);
        TerrainColumn a = enormousLimit.get(Integer.MIN_VALUE, Integer.MIN_VALUE);
        TerrainColumn b = enormousLimit.get(Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertEquals(a.estimatedRetainedBytes() + b.estimatedRetainedBytes(),
            enormousLimit.currentWeightBytes());
    }

    private TerrainColumn deterministicColumn(int columnX, int columnZ) {
        long mixed = mix(((long) columnX << 32) ^ (columnZ & 0xFFFFFFFFL));
        int baseMaterial = (int) mixed & 0xFFFF;
        int surfaceBiome = (int) (mixed >>> 16) & 0xFFFF;
        int caveBiome = (int) (mixed >>> 32) & 0xFFFF;
        TerrainColumn.Builder builder = TerrainColumn.builder(columnX, columnZ)
            .fillMaterialIds(baseMaterial)
            .fillSurfaceBiomeIds(surfaceBiome)
            .fillVirtualBiomeIds(caveBiome);
        for (int y = TerrainColumn.MIN_Y; y <= TerrainColumn.MAX_Y; y += 16) {
            builder.setMaterialId(0, y, 0, (baseMaterial + y - TerrainColumn.MIN_Y) & 0xFFFF)
                .setMaterialId(15, Math.min(y + 15, TerrainColumn.MAX_Y), 15,
                    (baseMaterial ^ y) & 0xFFFF);
        }
        return builder.build();
    }

    private static TerrainColumn denselyVariedColumn(int columnX, int columnZ) {
        TerrainColumn.Builder builder = TerrainColumn.builder(columnX, columnZ);
        int index = 0;
        for (int y = TerrainColumn.MIN_Y; y <= TerrainColumn.MAX_Y; ++y) {
            for (int z = 0; z < 16; ++z) {
                for (int x = 0; x < 16; ++x) {
                    builder.setMaterialId(x, y, z, index++ & 0xFFFF);
                }
            }
        }
        return builder.build();
    }

    private static long digest(TerrainColumn column) {
        long value = 0xCBF29CE484222325L;
        for (int cubeY = TerrainColumn.MIN_CUBE_Y; cubeY <= TerrainColumn.MAX_CUBE_Y; ++cubeY) {
            char[] cube = new char[TerrainColumn.BLOCKS_PER_CUBE];
            column.copyCubeMaterialIds(cubeY, cube, 0);
            for (char material : cube) {
                value = (value ^ material) * 0x100000001B3L;
            }
        }
        for (int z = 0; z < 16; ++z) {
            for (int x = 0; x < 16; ++x) {
                value = (value ^ column.surfaceBiomeId(x, z)) * 0x100000001B3L;
            }
        }
        for (int quartY = TerrainColumn.MIN_QUART_Y;
                quartY <= TerrainColumn.MAX_QUART_Y; ++quartY) {
            for (int quartZ = 0; quartZ < 4; ++quartZ) {
                for (int quartX = 0; quartX < 4; ++quartX) {
                    value = (value ^ column.virtualBiomeIdAtQuart(quartX, quartY, quartZ))
                        * 0x100000001B3L;
                }
            }
        }
        return value;
    }

    private static void assertColumnSamplesEqual(TerrainColumn expected, TerrainColumn actual) {
        int[] ys = {-64, -63, -1, 0, 15, 16, 319};
        for (int y : ys) {
            assertEquals(expected.materialId(0, y, 0), actual.materialId(0, y, 0));
            assertEquals(expected.materialId(15, y, 15), actual.materialId(15, y, 15));
            assertEquals(expected.virtualBiomeId(0, y, 0), actual.virtualBiomeId(0, y, 0));
        }
        assertEquals(expected.surfaceBiomeId(0, 0), actual.surfaceBiomeId(0, 0));
        assertEquals(expected.surfaceBiomeId(15, 15), actual.surfaceBiomeId(15, 15));
    }

    private static long mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        return value ^ value >>> 33;
    }

    private static <T extends Throwable> T expectThrows(Class<T> type, ThrowingRunnable runnable) {
        try {
            runnable.run();
            fail("Expected " + type.getName());
            return null;
        } catch (Throwable throwable) {
            if (!type.isInstance(throwable)) {
                throw new AssertionError("Expected " + type.getName() + " but got " + throwable,
                    throwable);
            }
            return type.cast(throwable);
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    @Test
    public void offerInsertsWithoutLoaderAndIgnoresDuplicates() {
        CountingLoader loader = new CountingLoader(false);
        TerrainColumnCache cache = new TerrainColumnCache(loader, 4, Long.MAX_VALUE);
        TerrainColumn offered = deterministicColumn(1, 1);
        cache.offer(1, 1, offered);
        assertEquals(0, loader.loads(1, 1));
        assertSame(offered, cache.get(1, 1));

        cache.offer(1, 1, deterministicColumn(1, 1));
        assertSame(offered, cache.get(1, 1));
        assertEquals(0, loader.loads(1, 1));
    }

    @Test
    public void offerValidatesNullAndCoordinateMismatches() {
        TerrainColumnCache cache = new TerrainColumnCache(this::deterministicColumn);
        expectThrows(NullPointerException.class, () -> cache.offer(0, 0, null));
        TerrainColumn mismatched = deterministicColumn(2, 2);
        expectThrows(IllegalStateException.class, () -> cache.offer(3, 3, mismatched));
        assertEquals(0, cache.size());
    }

    @Test
    public void offeredColumnsEvictAndAccountExactlyLikeLoadedOnes() {
        CountingLoader loader = new CountingLoader(false);
        TerrainColumnCache cache = new TerrainColumnCache(loader, 2, Long.MAX_VALUE);
        TerrainColumn first = deterministicColumn(-1, 0);
        cache.offer(-1, 0, first);
        TerrainColumn second = cache.get(0, -1);
        assertSame(first, cache.get(-1, 0));
        assertEquals(2, cache.size());
        assertEquals(first.estimatedRetainedBytes() + second.estimatedRetainedBytes(),
            cache.currentWeightBytes());

        cache.get(1, 1);
        assertFalse(cache.isCached(0, -1));
        assertTrue(cache.isCached(-1, 0));
        assertEquals(1, loader.loads(0, -1));
    }

    private final class CountingLoader implements TerrainColumnCache.Loader {
        private final Map<String, Integer> counts = new HashMap<String, Integer>();
        private final boolean denseAtOne;

        private CountingLoader(boolean denseAtOne) {
            this.denseAtOne = denseAtOne;
        }

        @Override
        public TerrainColumn load(int columnX, int columnZ) {
            String key = columnX + ":" + columnZ;
            Integer oldCount = counts.get(key);
            counts.put(key, oldCount == null ? 1 : oldCount + 1);
            if (denseAtOne) {
                return columnX == 1 ? denselyVariedColumn(columnX, columnZ)
                    : TerrainColumn.builder(columnX, columnZ).fillMaterialIds(1).build();
            }
            return deterministicColumn(columnX, columnZ);
        }

        private int loads(int columnX, int columnZ) {
            Integer count = counts.get(columnX + ":" + columnZ);
            return count == null ? 0 : count;
        }
    }
}

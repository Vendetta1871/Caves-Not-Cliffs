package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TerrainColumnTest {
    @Test
    public void coversTheExactFiniteWorldAndCubeBoundaries() {
        assertEquals(384, TerrainColumn.HEIGHT);
        assertEquals(98_304, TerrainColumn.BLOCK_COUNT);
        assertEquals(-4, TerrainColumn.MIN_CUBE_Y);
        assertEquals(19, TerrainColumn.MAX_CUBE_Y);
        assertEquals(24, TerrainColumn.CUBE_COUNT);
        assertEquals(-16, TerrainColumn.MIN_QUART_Y);
        assertEquals(79, TerrainColumn.MAX_QUART_Y);
        assertEquals(96, TerrainColumn.QUART_HEIGHT);
        assertEquals(1_536, TerrainColumn.VIRTUAL_BIOME_COUNT);

        TerrainColumn.Builder builder = TerrainColumn.builder(-1, Integer.MIN_VALUE);
        int[] boundaryY = {-64, -63, -49, -48, -17, -16, -1, 0, 15, 16, 255, 256, 319};
        for (int y : boundaryY) {
            builder.setMaterialId(0, y, 0, expectedMaterial(0, y, 0));
            builder.setMaterialId(15, y, 15, expectedMaterial(15, y, 15));
        }
        TerrainColumn column = builder.build();

        assertEquals(-1, column.columnX());
        assertEquals(Integer.MIN_VALUE, column.columnZ());
        assertEquals(-16L, column.minBlockX());
        assertEquals((long) Integer.MIN_VALUE * 16L, column.minBlockZ());
        for (int y : boundaryY) {
            assertEquals(expectedMaterial(0, y, 0), column.materialId(0, y, 0));
            assertEquals(expectedMaterial(15, y, 15), column.materialId(15, y, 15));
        }
    }

    @Test
    public void copiesEveryNegativeAndPositiveCubeWithoutChangingOrder() {
        TerrainColumn.Builder builder = TerrainColumn.builder(0, 0);
        for (int y = TerrainColumn.MIN_Y; y <= TerrainColumn.MAX_Y; ++y) {
            for (int z = 0; z < TerrainColumn.WIDTH; ++z) {
                for (int x = 0; x < TerrainColumn.WIDTH; ++x) {
                    builder.setMaterialId(x, y, z, expectedMaterial(x, y, z));
                }
            }
        }
        TerrainColumn column = builder.build();
        char[] cube = new char[TerrainColumn.BLOCKS_PER_CUBE + 6];
        Arrays.fill(cube, (char) 0xFFFF);

        for (int cubeY = TerrainColumn.MIN_CUBE_Y; cubeY <= TerrainColumn.MAX_CUBE_Y; ++cubeY) {
            column.copyCubeMaterialIds(cubeY, cube, 3);
            assertEquals(0xFFFF, cube[2]);
            assertEquals(0xFFFF, cube[3 + TerrainColumn.BLOCKS_PER_CUBE]);
            for (int localY = 0; localY < 16; ++localY) {
                int worldY = cubeY * 16 + localY;
                for (int z = 0; z < 16; ++z) {
                    for (int x = 0; x < 16; ++x) {
                        int index = 3 + localY * 256 + z * 16 + x;
                        assertEquals("cube=" + cubeY + " x=" + x + " y=" + worldY + " z=" + z,
                            expectedMaterial(x, worldY, z), cube[index]);
                    }
                }
            }
        }
    }

    @Test
    public void retainsSurfaceAndQuartResolutionVirtualBiomes() {
        TerrainColumn.Builder builder = TerrainColumn.builder(-20, 31);
        builder.setSurfaceBiomeId(0, 0, 7)
            .setSurfaceBiomeId(15, 15, 65_535)
            .setVirtualBiomeIdAtQuart(0, -16, 0, 101)
            .setVirtualBiomeIdAtQuart(3, -15, 3, 102)
            .setVirtualBiomeIdAtQuart(3, 79, 3, 103);
        TerrainColumn column = builder.build();

        assertEquals(7, column.surfaceBiomeId(0, 0));
        assertEquals(65_535, column.surfaceBiomeId(15, 15));
        assertEquals(101, column.virtualBiomeId(0, -64, 0));
        assertEquals(101, column.virtualBiomeId(3, -61, 3));
        assertEquals(102, column.virtualBiomeId(12, -60, 12));
        assertEquals(102, column.virtualBiomeId(15, -57, 15));
        assertEquals(103, column.virtualBiomeId(15, 319, 15));
        assertEquals(103, column.virtualBiomeIdAtQuart(3, 79, 3));
    }

    @Test
    public void retainsExactAquiferFluidUpdateFlagsAcrossCubeBoundaries() {
        TerrainColumn.Builder builder = TerrainColumn.builder(-1, 2)
            .setScheduledFluidUpdate(0, -64, 0, true)
            .setScheduledFluidUpdate(15, -49, 15, true)
            .setScheduledFluidUpdate(0, -48, 0, true)
            .setScheduledFluidUpdate(15, 319, 15, true)
            .setScheduledFluidUpdate(0, 0, 0, true)
            .setScheduledFluidUpdate(0, 0, 0, false);
        TerrainColumn column = builder.build();

        assertTrue(column.shouldScheduleFluidUpdate(0, -64, 0));
        assertTrue(column.shouldScheduleFluidUpdate(15, -49, 15));
        assertTrue(column.shouldScheduleFluidUpdate(0, -48, 0));
        assertTrue(column.shouldScheduleFluidUpdate(15, 319, 15));
        assertFalse(column.shouldScheduleFluidUpdate(0, 0, 0));

        boolean[] flags = new boolean[TerrainColumn.BLOCKS_PER_CUBE + 2];
        column.copyCubeFluidUpdateFlags(-4, flags, 1);
        assertTrue(flags[1]);
        assertTrue(flags[TerrainColumn.BLOCKS_PER_CUBE]);
        column.copyCubeFluidUpdateFlags(-3, flags, 1);
        assertTrue(flags[1]);
        assertFalse(flags[TerrainColumn.BLOCKS_PER_CUBE]);
    }

    @Test
    public void builtColumnsAreUnaffectedByLaterBuilderChanges() {
        TerrainColumn.Builder builder = TerrainColumn.builder(4, 5)
            .fillMaterialIds(11)
            .fillSurfaceBiomeIds(12)
            .fillVirtualBiomeIds(13);
        TerrainColumn first = builder.build();

        builder.fillMaterialIds(21)
            .fillSurfaceBiomeIds(22)
            .fillVirtualBiomeIds(23);
        TerrainColumn second = builder.build();

        assertEquals(11, first.materialId(8, 100, 8));
        assertEquals(12, first.surfaceBiomeId(8, 8));
        assertEquals(13, first.virtualBiomeId(8, 100, 8));
        assertEquals(21, second.materialId(8, 100, 8));
        assertEquals(22, second.surfaceBiomeId(8, 8));
        assertEquals(23, second.virtualBiomeId(8, 100, 8));
    }

    @Test
    public void adaptivelyUsesTheSmallerMaterialRepresentation() {
        TerrainColumn uniform = TerrainColumn.builder(0, 0).fillMaterialIds(65_535).build();
        assertTrue(uniform.hasPaletteEncodedMaterials());
        assertEquals(1, uniform.materialPaletteSize());
        assertEquals(2L, uniform.materialStorageBytes());
        assertEquals(65_535, uniform.materialId(15, 319, 15));

        TerrainColumn.Builder variedBuilder = TerrainColumn.builder(0, 1);
        int index = 0;
        for (int y = TerrainColumn.MIN_Y; y <= TerrainColumn.MAX_Y; ++y) {
            for (int z = 0; z < 16; ++z) {
                for (int x = 0; x < 16; ++x) {
                    variedBuilder.setMaterialId(x, y, z, index++ & 0xFFFF);
                }
            }
        }
        TerrainColumn varied = variedBuilder.build();
        assertFalse(varied.hasPaletteEncodedMaterials());
        assertEquals(65_536, varied.materialPaletteSize());
        assertEquals((long) TerrainColumn.BLOCK_COUNT * Character.BYTES,
            varied.materialStorageBytes());
        assertEquals(expectedSequentialIndex(0, -64, 0), varied.materialId(0, -64, 0));
        assertEquals(expectedSequentialIndex(15, 319, 15), varied.materialId(15, 319, 15));
        assertTrue(varied.estimatedRetainedBytes() > uniform.estimatedRetainedBytes());
    }

    @Test
    public void bitPackingHandlesWordCrossingsAtEveryPaletteWidth() {
        for (int paletteSize = 2; paletteSize <= 257; paletteSize = paletteSize * 2 + 1) {
            TerrainColumn.Builder builder = TerrainColumn.builder(paletteSize, -paletteSize);
            int index = 0;
            for (int y = TerrainColumn.MIN_Y; y <= TerrainColumn.MAX_Y; ++y) {
                for (int z = 0; z < 16; ++z) {
                    for (int x = 0; x < 16; ++x) {
                        builder.setMaterialId(x, y, z, index++ % paletteSize);
                    }
                }
            }
            TerrainColumn column = builder.build();
            assertTrue("palette size " + paletteSize, column.hasPaletteEncodedMaterials());
            assertEquals(paletteSize, column.materialPaletteSize());

            int[] samples = {0, 1, 31, 32, 63, 64, 65, 127, 128, 255, 256,
                4_095, 4_096, TerrainColumn.BLOCK_COUNT - 1};
            for (int sample : samples) {
                int yOffset = sample / 256;
                int withinLayer = sample % 256;
                int z = withinLayer / 16;
                int x = withinLayer % 16;
                assertEquals("palette=" + paletteSize + " sample=" + sample,
                    sample % paletteSize,
                    column.materialId(x, TerrainColumn.MIN_Y + yOffset, z));
            }
        }
    }

    @Test
    public void rejectsEveryOutOfBoundsAxisAndUnsignedIdOverflow() {
        final TerrainColumn.Builder builder = TerrainColumn.builder(0, 0);
        expectThrows(IndexOutOfBoundsException.class,
            () -> builder.setMaterialId(-1, -64, 0, 0));
        expectThrows(IndexOutOfBoundsException.class,
            () -> builder.setMaterialId(16, -64, 0, 0));
        expectThrows(IndexOutOfBoundsException.class,
            () -> builder.setMaterialId(0, -65, 0, 0));
        expectThrows(IndexOutOfBoundsException.class,
            () -> builder.setMaterialId(0, 320, 0, 0));
        expectThrows(IndexOutOfBoundsException.class,
            () -> builder.setMaterialId(0, -64, -1, 0));
        expectThrows(IndexOutOfBoundsException.class,
            () -> builder.setMaterialId(0, -64, 16, 0));
        expectThrows(IllegalArgumentException.class,
            () -> builder.setMaterialId(0, -64, 0, -1));
        expectThrows(IllegalArgumentException.class,
            () -> builder.setMaterialId(0, -64, 0, 65_536));
        expectThrows(IllegalArgumentException.class,
            () -> builder.setSurfaceBiomeId(0, 0, Integer.MAX_VALUE));
        expectThrows(IndexOutOfBoundsException.class,
            () -> builder.setVirtualBiomeIdAtQuart(-1, -16, 0, 0));
        expectThrows(IndexOutOfBoundsException.class,
            () -> builder.setVirtualBiomeIdAtQuart(4, -16, 0, 0));
        expectThrows(IndexOutOfBoundsException.class,
            () -> builder.setVirtualBiomeIdAtQuart(0, -17, 0, 0));
        expectThrows(IndexOutOfBoundsException.class,
            () -> builder.setVirtualBiomeIdAtQuart(0, 80, 0, 0));
        expectThrows(IndexOutOfBoundsException.class,
            () -> builder.setVirtualBiomeIdAtQuart(0, -16, 4, 0));

        final TerrainColumn column = builder.build();
        expectThrows(IndexOutOfBoundsException.class, () -> column.materialId(0, 320, 0));
        expectThrows(IndexOutOfBoundsException.class, () -> column.surfaceBiomeId(16, 0));
        expectThrows(IndexOutOfBoundsException.class, () -> column.virtualBiomeId(0, -65, 0));
        expectThrows(IndexOutOfBoundsException.class,
            () -> column.copyCubeMaterialIds(-5, new char[4_096], 0));
        expectThrows(IndexOutOfBoundsException.class,
            () -> column.copyCubeMaterialIds(20, new char[4_096], 0));
        expectThrows(NullPointerException.class,
            () -> column.copyCubeMaterialIds(-4, null, 0));
        expectThrows(IndexOutOfBoundsException.class,
            () -> column.copyCubeMaterialIds(-4, new char[4_096], -1));
        expectThrows(IndexOutOfBoundsException.class,
            () -> column.copyCubeMaterialIds(-4, new char[4_096], 1));
        expectThrows(IndexOutOfBoundsException.class,
            () -> column.copyCubeMaterialIds(-4, new char[4_096], Integer.MAX_VALUE));
    }

    private static int expectedMaterial(int x, int y, int z) {
        return ((y - TerrainColumn.MIN_Y) * 521 + z * 17 + x + 1) & 0xFFFF;
    }

    private static int expectedSequentialIndex(int x, int y, int z) {
        return ((y - TerrainColumn.MIN_Y) * 256 + z * 16 + x) & 0xFFFF;
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
}

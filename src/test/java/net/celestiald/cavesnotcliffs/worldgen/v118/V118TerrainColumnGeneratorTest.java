package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class V118TerrainColumnGeneratorTest {
    private static final long[] EDGE_SEEDS = {
        0L, 1L, -1L, 123456789L, Long.MIN_VALUE, Long.MAX_VALUE
    };
    private static final V118NoiseRouterData.Profile[] EDGE_PROFILES = {
        V118NoiseRouterData.Profile.DEFAULT,
        V118NoiseRouterData.Profile.LARGE_BIOMES,
        V118NoiseRouterData.Profile.AMPLIFIED,
        V118NoiseRouterData.Profile.AMPLIFIED,
        V118NoiseRouterData.Profile.LARGE_BIOMES,
        V118NoiseRouterData.Profile.DEFAULT
    };

    @Test
    public void generatesTheCompleteFiniteColumnAndVirtualBiomeVolume() {
        V118TerrainColumnGenerator generator = new V118TerrainColumnGenerator(0L,
            V118NoiseRouterData.Profile.DEFAULT);
        TerrainColumn column = generator.column(-1, -1);

        assertEquals(-1, column.columnX());
        assertEquals(-1, column.columnZ());
        for (int worldY : new int[] {-64, -63, -1, 0, 63, 64, 255, 319}) {
            V118Material material = V118Material.fromStorageId(
                column.materialId(0, worldY, 0));
            assertTrue(material != null);
        }
        for (int quartY : new int[] {-16, -15, -1, 0, 15, 16, 63, 79}) {
            int biomeId = column.virtualBiomeIdAtQuart(0, quartY, 0);
            assertTrue(biomeId >= 0 && biomeId < V118Biome.values().length);
        }
        assertTrue(column.surfaceBiomeId(0, 0) < V118Biome.values().length);
        assertSame(column, generator.column(-1, -1));
        assertEquals(1, generator.cache().size());
    }

    @Test
    public void completeColumnsAreIndependentOfRequestOrderAcrossEdgeSeedsAndProfiles() {
        for (int index = 0; index < EDGE_SEEDS.length; ++index) {
            V118TerrainColumnGenerator targetFirst = new V118TerrainColumnGenerator(
                EDGE_SEEDS[index], EDGE_PROFILES[index]);
            long expected = digest(targetFirst.column(-2, 1));
            targetFirst.column(3, -4);

            V118TerrainColumnGenerator targetLast = new V118TerrainColumnGenerator(
                EDGE_SEEDS[index], EDGE_PROFILES[index]);
            targetLast.column(3, -4);
            long actual = digest(targetLast.column(-2, 1));
            assertEquals("seed=" + EDGE_SEEDS[index] + ", profile=" + EDGE_PROFILES[index],
                expected, actual);
        }
    }

    @Test
    public void parallelCellFillMatchesSerialGenerationBitForBit() {
        int[][] coordinates = {{0, 0}, {1, 0}, {-3, 2}, {7, -5}};
        for (int index = 0; index < EDGE_SEEDS.length; ++index) {
            V118TerrainColumnGenerator serial = new V118TerrainColumnGenerator(
                EDGE_SEEDS[index], EDGE_PROFILES[index], true, false, 1);
            V118TerrainColumnGenerator parallel = new V118TerrainColumnGenerator(
                EDGE_SEEDS[index], EDGE_PROFILES[index], true, false, 4);
            for (int[] coordinate : coordinates) {
                long expected = digest(serial.column(coordinate[0], coordinate[1]));
                long actual = digest(parallel.column(coordinate[0], coordinate[1]));
                assertEquals("seed=" + EDGE_SEEDS[index] + ", profile=" + EDGE_PROFILES[index]
                    + ", column=(" + coordinate[0] + ", " + coordinate[1] + ")",
                    expected, actual);
            }
        }
    }

    private static long digest(TerrainColumn column) {
        long value = 0xCBF29CE484222325L;
        char[] materials = new char[TerrainColumn.BLOCKS_PER_CUBE];
        boolean[] fluidUpdates = new boolean[TerrainColumn.BLOCKS_PER_CUBE];
        for (int cubeY = TerrainColumn.MIN_CUBE_Y;
                cubeY <= TerrainColumn.MAX_CUBE_Y; ++cubeY) {
            column.copyCubeMaterialIds(cubeY, materials, 0);
            column.copyCubeFluidUpdateFlags(cubeY, fluidUpdates, 0);
            for (int block = 0; block < TerrainColumn.BLOCKS_PER_CUBE; ++block) {
                value = (value ^ materials[block]) * 0x100000001B3L;
                value = (value ^ (fluidUpdates[block] ? 1L : 0L)) * 0x100000001B3L;
            }
        }
        for (int z = 0; z < TerrainColumn.WIDTH; ++z) {
            for (int x = 0; x < TerrainColumn.WIDTH; ++x) {
                value = (value ^ column.surfaceBiomeId(x, z)) * 0x100000001B3L;
            }
        }
        for (int quartY = TerrainColumn.MIN_QUART_Y;
                quartY <= TerrainColumn.MAX_QUART_Y; ++quartY) {
            for (int quartZ = 0; quartZ < TerrainColumn.QUART_WIDTH; ++quartZ) {
                for (int quartX = 0; quartX < TerrainColumn.QUART_WIDTH; ++quartX) {
                    value = (value ^ column.virtualBiomeIdAtQuart(quartX, quartY, quartZ))
                        * 0x100000001B3L;
                }
            }
        }
        return value;
    }
}

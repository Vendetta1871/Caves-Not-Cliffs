package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class V118TerrainColumnGeneratorTest {
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
}

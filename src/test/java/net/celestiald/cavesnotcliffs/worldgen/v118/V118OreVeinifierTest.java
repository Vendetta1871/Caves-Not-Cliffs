package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class V118OreVeinifierTest {
    @Test
    public void rejectsSamplesOutsideTheExactVerticalBands() {
        V118OreVeinifier copper = veinifier(0.9D, -1.0D, 1.0D, 0L);
        assertNull(copper.compute(0, -1, 0));
        assertNull(copper.compute(0, 51, 0));
        V118OreVeinifier iron = veinifier(-0.9D, -1.0D, 1.0D, 0L);
        assertNull(iron.compute(0, -61, 0));
        assertNull(iron.compute(0, -7, 0));
    }

    @Test
    public void ridgeAndEdgeThresholdsShortCircuitBeforeMaterialSelection() {
        assertNull(veinifier(0.39D, -1.0D, 1.0D, 1L).compute(0, 25, 0));
        assertNull(veinifier(0.9D, 0.0D, 1.0D, 1L).compute(0, 25, 0));
        assertNull(veinifier(-0.9D, 0.0D, 1.0D, 1L).compute(0, -32, 0));
    }

    @Test
    public void seededSelectionsOnlyProduceTheOfficialVeinFamilies() {
        V118OreVeinifier copper = veinifier(0.9D, -1.0D, 1.0D, 123456789L);
        V118OreVeinifier iron = veinifier(-0.9D, -1.0D, 1.0D, 123456789L);
        for (int x = -32; x <= 32; ++x) {
            V118Material copperMaterial = copper.compute(x, 25, -x);
            if (copperMaterial != null) {
                assertTrue(copperMaterial == V118Material.COPPER_ORE
                    || copperMaterial == V118Material.RAW_COPPER_BLOCK
                    || copperMaterial == V118Material.GRANITE);
            }
            V118Material ironMaterial = iron.compute(x, -32, -x);
            if (ironMaterial != null) {
                assertTrue(ironMaterial == V118Material.DEEPSLATE_IRON_ORE
                    || ironMaterial == V118Material.RAW_IRON_BLOCK
                    || ironMaterial == V118Material.TUFF);
            }
        }
    }

    @Test
    public void materialStorageIdsRoundTrip() {
        for (V118Material material : V118Material.values()) {
            assertEquals(material, V118Material.fromStorageId(material.storageId()));
        }
    }

    private static V118OreVeinifier veinifier(double toggle, double ridged, double gap,
            long seed) {
        return new V118OreVeinifier(DensityFunctions.constant(toggle),
            DensityFunctions.constant(ridged), DensityFunctions.constant(gap),
            new XoroshiroRandomSource(seed).forkPositional());
    }
}

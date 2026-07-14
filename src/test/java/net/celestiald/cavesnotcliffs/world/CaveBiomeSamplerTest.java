package net.celestiald.cavesnotcliffs.world;

import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CaveBiomeSamplerTest {
    @Test
    public void isDeterministicForPositiveAndNegativeCoordinates() {
        long seed = -812734982734L;
        int[][] points = {
                {0, -63, 0}, {16, -24, -33}, {-8192, 12, 4096}, {200000, 48, -90000}
        };
        for (int[] point : points) {
            CaveBiomeSampler.Type first = CaveBiomeSampler.sample(seed, point[0], point[1], point[2]);
            assertEquals(first, CaveBiomeSampler.sample(seed, point[0], point[1], point[2]));
        }
    }

    @Test
    public void producesAllThreeRegionTypes() {
        EnumSet<CaveBiomeSampler.Type> found = EnumSet.noneOf(CaveBiomeSampler.Type.class);
        for (int x = -1024; x <= 1024; x += 32) {
            for (int z = -1024; z <= 1024; z += 32) {
                found.add(CaveBiomeSampler.sample(123456789L, x, -24, z));
            }
        }
        assertEquals(EnumSet.allOf(CaveBiomeSampler.Type.class), found);
    }

    @Test
    public void neighboringSamplesAreUsuallyCoherent() {
        int equal = 0;
        int total = 0;
        long seed = 99887766L;
        for (int x = -512; x < 512; x += 8) {
            for (int z = -512; z < 512; z += 8) {
                CaveBiomeSampler.Type type = CaveBiomeSampler.sample(seed, x, -20, z);
                if (type == CaveBiomeSampler.sample(seed, x + 4, -20, z + 4)) {
                    equal++;
                }
                total++;
            }
        }
        assertTrue("nearby positions should normally share a cave region", equal > total * 0.90);
    }

    @Test
    public void samplerStopsAtTheWorldAndCaveCeilings() {
        assertEquals(CaveBiomeSampler.Type.NORMAL,
                CaveBiomeSampler.sample(1L, 0, CavesNotCliffsWorldType.MIN_HEIGHT - 1, 0));
        assertEquals(CaveBiomeSampler.Type.NORMAL, CaveBiomeSampler.sample(1L, 0, 64, 0));
    }
}

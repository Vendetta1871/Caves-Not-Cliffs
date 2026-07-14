package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class V118LushCavePlacementTest {
    @Test
    public void catalogPinsTheOfficialVegetalDecorationIndicesAndCounts() {
        V118LushCavePlacements.PlacedFeature[] features =
            V118LushCavePlacements.PlacedFeature.values();
        assertEquals(7, features.length);
        for (int index = 0; index < features.length; ++index) {
            assertEquals(22 + index, features[index].globalIndex());
        }
        assertEquals(125, features[0].minimumCount());
        assertEquals(188, features[1].minimumCount());
        assertEquals(62, features[2].minimumCount());
        assertEquals(125, features[3].minimumCount());
        assertEquals(1, features[4].minimumCount());
        assertEquals(2, features[4].maximumCount());
        assertEquals(25, features[5].minimumCount());
        assertEquals(256, features[6].minimumCount());
        assertEquals(9, V118LushCavePlacements.VEGETAL_DECORATION_STEP);
        assertEquals(256, V118LushCavePlacements.MAX_TERRAIN_Y);
        assertEquals(12, V118LushCavePlacements.ENVIRONMENT_SCAN_STEPS);
    }

    @Test
    public void scanHonorsBuildLimitsAndFinalPositionBiomeFilter() {
        LayeredAccess lush = new LayeredAccess(V118Biome.LUSH_CAVES);
        Random random = new Random(1L);
        List<V118LushCavePlacements.Position> vines =
            V118LushCavePlacements.samplePlacementOrigins(
                V118LushCavePlacements.PlacedFeature.CAVE_VINES, random,
                -1, -1, lush, true);
        for (V118LushCavePlacements.Position position : vines) {
            assertTrue(position.y() >= -64 && position.y() < 320);
            assertTrue(lush.isAir(position.x(), position.y(), position.z()));
            assertTrue(lush.hasSturdyDownFace(position.x(), position.y() + 1,
                position.z()));
        }
        LayeredAccess plains = new LayeredAccess(V118Biome.PLAINS);
        assertTrue(V118LushCavePlacements.samplePlacementOrigins(
            V118LushCavePlacements.PlacedFeature.CLASSIC_VINES, new Random(1L),
            0, 0, plains, true).isEmpty());
    }

    private static final class LayeredAccess
            implements V118LushCavePlacements.PlacementAccess {
        private final V118Biome biome;

        LayeredAccess(V118Biome biome) {
            this.biome = biome;
        }

        @Override public int minBuildHeight() { return -64; }
        @Override public int maxBuildHeight() { return 320; }

        @Override
        public boolean isAir(int x, int y, int z) {
            int phase = Math.floorMod(y + 64, 8);
            return phase >= 1 && phase <= 6;
        }

        @Override
        public boolean isSolid(int x, int y, int z) {
            return !isAir(x, y, z);
        }

        @Override
        public boolean hasSturdyDownFace(int x, int y, int z) {
            return isSolid(x, y, z);
        }

        @Override public V118Biome biomeAt(int x, int y, int z) { return biome; }
    }
}

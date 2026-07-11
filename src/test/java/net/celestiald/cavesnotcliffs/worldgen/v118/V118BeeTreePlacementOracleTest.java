package net.celestiald.cavesnotcliffs.worldgen.v118;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118BeeTreePlacements.HorizontalSample;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118BeeTreePlacements.PlacedFeature;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Count, in-square, and trailing-RNG fixtures captured from Java 1.18.2. */
public class V118BeeTreePlacementOracleTest {
    @Test
    public void seedZeroFeatureIndicesAndHorizontalSamplingMatchOfficialRuntime() {
        check(0L, 0, 0, PlacedFeature.TREES_FLOWER_FOREST,
                "0,14;14,12;8,14;0,2;2,2;12,1", 1958671783425641139L);
        check(0L, 0, 0, PlacedFeature.BIRCH_TALL,
                "15,12;8,7;7,1;0,6;8,15;11,14;9,14;14,2;9,11;15,6",
                -7402420081303727122L);
        check(0L, 0, 0, PlacedFeature.TREES_BIRCH,
                "2,14;13,7;7,5;12,10;0,1;8,9;7,2;4,9;1,3;11,7",
                422422114386307323L);
        check(0L, 0, 0, PlacedFeature.TREES_BIRCH_AND_OAK,
                "6,11;0,4;11,8;7,11;5,4;12,11;12,10;14,13;6,10;0,2",
                -3786281244098417159L);
        check(0L, 0, 0, PlacedFeature.TREES_PLAINS, "-",
                6108363126295787546L);
        check(0L, 0, 0, PlacedFeature.TREES_MEADOW, "-",
                6068033028531988908L);
    }

    @Test
    public void negativeChunksAndRarePlacementsMatchOfficialRuntime() {
        check(1L, -1, 0, PlacedFeature.TREES_BIRCH_AND_OAK,
                "-11,11;-14,2;-6,12;-13,13;-1,12;-6,8;-14,3;-3,3;-6,14;-16,8;-14,3",
                -6764545743215093534L);
        check(5L, 0, 0, PlacedFeature.TREES_PLAINS, "10,5",
                1392709928529941497L);
        check(335L, 0, 0, PlacedFeature.TREES_MEADOW, "1,1",
                -4262109806425131293L);
    }

    private static void check(long seed, int chunkX, int chunkZ,
            PlacedFeature feature, String positions, long trailingLong) {
        HorizontalSample sample = V118BeeTreePlacements.horizontalSample(
                seed, chunkX, chunkZ, feature);
        assertEquals(positions, sample.encodedPositions());
        assertEquals(trailingLong, sample.trailingRandomLong());
    }
}

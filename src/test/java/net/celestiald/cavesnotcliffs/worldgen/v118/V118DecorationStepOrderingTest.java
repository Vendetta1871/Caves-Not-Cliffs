package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class V118DecorationStepOrderingTest {
    @Test
    public void bridgeRunsOnceBetweenStepSixAndInfestedOreIndexTwo() {
        final long worldSeed = 123456789L;
        final int chunkX = -7;
        final int chunkZ = 11;
        final TrackingWorld world = new TrackingWorld();
        final long expectedDecorationSeed = new V118WorldgenRandom(0L)
            .setDecorationSeed(worldSeed, chunkX << 4, chunkZ << 4);

        V118OrePlacements.DecorationResult result = V118OrePlacements.decorate(world,
            worldSeed, chunkX, chunkZ, EnumSet.of(V118Biome.MEADOW),
            new V118OrePlacements.BetweenDecorationSteps() {
                @Override
                public void decorate(long decorationSeed, int actualChunkX, int actualChunkZ,
                        Set<V118Biome> regionBiomes, V118WorldgenRandom random) {
                    assertEquals(expectedDecorationSeed, decorationSeed);
                    assertEquals(chunkX, actualChunkX);
                    assertEquals(chunkZ, actualChunkZ);
                    assertTrue(regionBiomes.contains(V118Biome.MEADOW));
                    ++world.boundaryCalls;
                }
            });

        assertEquals(expectedDecorationSeed, result.decorationSeed());
        assertEquals(1, world.boundaryCalls);
        assertTrue("infested ore must run after the step boundary",
            world.writesAfterBoundary > 0);
        assertTrue(result.featureResults().containsKey(V118OrePlacements.PlacedOre.ORE_INFESTED));
    }

    private static final class TrackingWorld implements V118OrePlacements.WorldAccess {
        private int boundaryCalls;
        private int writesAfterBoundary;

        @Override
        public int minBuildHeight() {
            return -64;
        }

        @Override
        public int maxBuildHeight() {
            return 320;
        }

        @Override
        public V118OreMaterial getMaterial(int x, int y, int z) {
            return y < 96 ? V118OreMaterial.STONE : V118OreMaterial.AIR;
        }

        @Override
        public void setMaterial(int x, int y, int z, V118OreMaterial material) {
            if (boundaryCalls > 0) {
                ++writesAfterBoundary;
            }
        }

        @Override
        public int oceanFloorHeight(int x, int z) {
            return 96;
        }

        @Override
        public V118Biome biomeAt(int x, int y, int z) {
            return V118Biome.MEADOW;
        }
    }
}

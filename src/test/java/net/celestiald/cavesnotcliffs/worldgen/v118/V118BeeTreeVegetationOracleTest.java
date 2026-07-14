package net.celestiald.cavesnotcliffs.worldgen.v118;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118BeeTreeVegetation.Heightmap;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118BeeTreeVegetation.PlacedFeature;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118BeeTreeVegetation.Plant;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Synthetic flat-world fixtures captured from Java 1.18.2's configured features. */
public class V118BeeTreeVegetationOracleTest {
    @Test
    public void forestFlowerPrerequisitesMatchOfficialCountsAndRandomStream() {
        Fixture flowerForest = run(0L, PlacedFeature.FLOWER_FOREST_FLOWERS,
                V118Biome.FLOWER_FOREST);
        assertEquals(23, flowerForest.successfulPlants);
        assertEquals(46, flowerForest.world.states.size());
        assertEquals(23, flowerForest.world.count(Plant.ROSE_BUSH, false));
        assertEquals(23, flowerForest.world.count(Plant.ROSE_BUSH, true));
        assertEquals(-2618791836915498436L, flowerForest.trailingLong);

        Fixture forest = run(66L, PlacedFeature.FOREST_FLOWERS, V118Biome.FOREST);
        assertEquals(22, forest.successfulPlants);
        assertEquals(44, forest.world.states.size());
        assertEquals(22, forest.world.count(Plant.LILAC, false));
        assertEquals(22, forest.world.count(Plant.LILAC, true));
        assertEquals(5108963033088585885L, forest.trailingLong);
    }

    @Test
    public void plainsAndMeadowPrerequisitesMatchOfficialCountsAndRandomStream() {
        Fixture sunflowers = run(1L, PlacedFeature.PATCH_SUNFLOWER,
                V118Biome.SUNFLOWER_PLAINS);
        assertEquals(21, sunflowers.successfulPlants);
        assertEquals(42, sunflowers.world.states.size());
        assertEquals(21, sunflowers.world.count(Plant.SUNFLOWER, false));
        assertEquals(21, sunflowers.world.count(Plant.SUNFLOWER, true));
        assertEquals(9018363323065449451L, sunflowers.trailingLong);

        Fixture grass = run(0L, PlacedFeature.PATCH_GRASS_PLAIN, V118Biome.PLAINS);
        assertEquals(70, grass.successfulPlants);
        assertEquals(70, grass.world.states.size());
        assertEquals(70, grass.world.count(Plant.SHORT_GRASS, false));
        assertEquals(7584187675945943012L, grass.trailingLong);
        assertTrue(grass.world.hasRaisedWorldSurface());

        Fixture meadow = run(0L, PlacedFeature.FLOWER_MEADOW, V118Biome.MEADOW);
        assertEquals(35, meadow.successfulPlants);
        assertEquals(35, meadow.world.states.size());
        assertEquals(35, meadow.world.count(Plant.DANDELION, false));
        assertEquals(3919866042315130274L, meadow.trailingLong);
    }

    private static Fixture run(long seed, PlacedFeature feature, V118Biome biome) {
        FakeWorld world = new FakeWorld(biome);
        Random random = V118BeeTreePlacements.randomFor(seed, 0, 0,
                feature.globalIndex());
        int successfulPlants = V118BeeTreeVegetation.placeWithRandom(
                world, random, 0, 0, feature);
        return new Fixture(world, successfulPlants, random.nextLong());
    }

    private static final class Fixture {
        final FakeWorld world;
        final int successfulPlants;
        final long trailingLong;

        Fixture(FakeWorld world, int successfulPlants, long trailingLong) {
            this.world = world;
            this.successfulPlants = successfulPlants;
            this.trailingLong = trailingLong;
        }
    }

    private static final class FakeWorld implements V118BeeTreeVegetation.WorldAccess {
        final V118Biome biome;
        final Map<BlockPos, PlantState> states = new HashMap<>();

        FakeWorld(V118Biome biome) {
            this.biome = biome;
        }

        @Override
        public int height(Heightmap heightmap, int blockX, int blockZ) {
            if (heightmap == Heightmap.MOTION_BLOCKING) {
                return 64;
            }
            int height = 64;
            for (BlockPos pos : states.keySet()) {
                if (pos.getX() == blockX && pos.getZ() == blockZ) {
                    height = Math.max(height, pos.getY() + 1);
                }
            }
            return height;
        }

        @Override
        public V118Biome biome(int blockX, int blockY, int blockZ) {
            return biome;
        }

        @Override
        public boolean placePlant(BlockPos pos, Plant plant) {
            if (pos.getY() < 64 || pos.getY() >= 320 || states.containsKey(pos)) {
                return false;
            }
            // The fixture is farmland at y=63 and solid dirt below it. Only the first air layer
            // has valid support because higher plants are not dirt-tagged support blocks.
            if (pos.getY() != 64) {
                return false;
            }
            if (plant.isDoublePlant()) {
                BlockPos upper = pos.up();
                if (states.containsKey(upper)) {
                    return false;
                }
                states.put(pos.toImmutable(), new PlantState(plant, false));
                states.put(upper.toImmutable(), new PlantState(plant, true));
            } else {
                states.put(pos.toImmutable(), new PlantState(plant, false));
            }
            return true;
        }

        int count(Plant plant, boolean upper) {
            int count = 0;
            for (PlantState state : states.values()) {
                if (state.plant == plant && state.upper == upper) {
                    ++count;
                }
            }
            return count;
        }

        boolean hasRaisedWorldSurface() {
            for (BlockPos pos : states.keySet()) {
                if (height(Heightmap.WORLD_SURFACE, pos.getX(), pos.getZ())
                        > height(Heightmap.MOTION_BLOCKING, pos.getX(), pos.getZ())) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class PlantState {
        final Plant plant;
        final boolean upper;

        PlantState(Plant plant, boolean upper) {
            this.plant = plant;
            this.upper = upper;
        }
    }
}

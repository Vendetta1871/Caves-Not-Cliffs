package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class V118DiskFeatureTest {
    @Test
    public void clayDiskUsesInclusiveCircleAndExactVerticalBand() {
        SparseWorld world = new SparseWorld(V118OreMaterial.STONE);
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                world.setMaterial(x, 9, z, V118OreMaterial.DIRT);
            }
        }
        world.setMaterial(0, 10, 0, V118OreMaterial.WATER);
        V118DiskFeature.Configuration config = new V118DiskFeature.Configuration(
                V118OreMaterial.CLAY, 2, 2, 1, V118OreMaterial.DIRT,
                V118OreMaterial.CLAY);
        assertTrue(V118DiskFeature.place(world, new Random(0L), config, 0, 10, 0));
        int clay = 0;
        for (V118OreMaterial value : world.overrides.values()) {
            if (value == V118OreMaterial.CLAY) {
                clay++;
            }
        }
        assertEquals(13, clay);
        assertEquals(V118OreMaterial.STONE, world.getMaterial(2, 10, 1));
        assertEquals(V118OreMaterial.STONE, world.getMaterial(0, 8, 0));
    }

    @Test
    public void fallingDiskStabilizesAReplacedBlockAboveAirAsSandstone() {
        SparseWorld world = new SparseWorld(V118OreMaterial.STONE);
        world.setMaterial(0, 10, 0, V118OreMaterial.WATER);
        world.setMaterial(0, 9, 0, V118OreMaterial.DIRT);
        world.setMaterial(0, 8, 0, V118OreMaterial.AIR);
        V118DiskFeature.Configuration config = new V118DiskFeature.Configuration(
                V118OreMaterial.SAND, 0, 0, 1, V118OreMaterial.DIRT);
        assertTrue(V118DiskFeature.place(world, new Random(0L), config, 0, 10, 0));
        assertEquals(V118OreMaterial.SANDSTONE, world.getMaterial(0, 9, 0));
        assertEquals(V118OreMaterial.AIR, world.getMaterial(0, 8, 0));
    }

    @Test
    public void returnsFalseWhenNoTargetStateMatches() {
        SparseWorld world = new SparseWorld(V118OreMaterial.STONE);
        world.setMaterial(0, 10, 0, V118OreMaterial.WATER);
        V118DiskFeature.Configuration config = new V118DiskFeature.Configuration(
                V118OreMaterial.GRAVEL, 2, 5, 2, V118OreMaterial.DIRT,
                V118OreMaterial.GRASS_BLOCK);
        assertFalse(V118DiskFeature.place(world, new Random(1L), config, 0, 10, 0));
        assertEquals(1, world.overrides.size());
        assertEquals(V118OreMaterial.WATER, world.getMaterial(0, 10, 0));
    }

    private static final class SparseWorld implements V118DiskFeature.WorldAccess {
        private final V118OreMaterial base;
        private final Map<String, V118OreMaterial> overrides = new HashMap<>();

        SparseWorld(V118OreMaterial base) {
            this.base = base;
        }

        @Override
        public V118OreMaterial getMaterial(int x, int y, int z) {
            V118OreMaterial value = overrides.get(key(x, y, z));
            return value == null ? base : value;
        }

        @Override
        public void setMaterial(int x, int y, int z, V118OreMaterial material) {
            overrides.put(key(x, y, z), material);
        }

        private static String key(int x, int y, int z) {
            return x + ":" + y + ":" + z;
        }
    }
}

package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class V118UnderwaterMagmaFeatureTest {
    @Test
    public void findsFirstNonWaterFloorWithinExactFiveBlockScan() {
        SparseWorld world = waterColumn(10, 13, 9);
        assertEquals(Integer.valueOf(9),
                V118UnderwaterMagmaFeature.floorY(world, 0, 12, 0));
        assertNull(V118UnderwaterMagmaFeature.floorY(world, 0, 14, 0));

        SparseWorld tooDeep = waterColumn(10, 20, 9);
        assertNull(V118UnderwaterMagmaFeature.floorY(tooDeep, 0, 14, 0));
    }

    @Test
    public void rejectsWaterAirAndHorizontallyExposedCandidates() {
        SparseWorld world = new SparseWorld(V118OreMaterial.STONE);
        assertTrue(V118UnderwaterMagmaFeature.isValidPlacement(world, 0, 0, 0));
        world.setMaterial(1, 0, 0, V118OreMaterial.WATER);
        assertFalse(V118UnderwaterMagmaFeature.isValidPlacement(world, 0, 0, 0));
        world.setMaterial(1, 0, 0, V118OreMaterial.STONE);
        world.setMaterial(0, -1, 0, V118OreMaterial.AIR);
        assertFalse(V118UnderwaterMagmaFeature.isValidPlacement(world, 0, 0, 0));
    }

    @Test
    public void placesOnlyValidHalfProbabilityFloorVolumeInOfficialTraversalOrder() {
        SparseWorld world = waterColumn(10, 12, 9);
        Random random = new Random(123456789L);
        assertTrue(V118UnderwaterMagmaFeature.place(world, random, 0, 11, 0));

        SparseWorld expected = waterColumn(10, 12, 9);
        Random replay = new Random(123456789L);
        int count = 0;
        for (int z = -1; z <= 1; z++) {
            for (int y = 8; y <= 10; y++) {
                for (int x = -1; x <= 1; x++) {
                    if (replay.nextFloat() < 0.5F
                            && V118UnderwaterMagmaFeature.isValidPlacement(expected, x, y, z)) {
                        expected.setMaterial(x, y, z, V118OreMaterial.MAGMA);
                        count++;
                    }
                }
            }
        }
        assertTrue(count > 0);
        assertEquals(expected.overrides, world.overrides);
        assertEquals(replay.nextLong(), random.nextLong());
    }

    private static SparseWorld waterColumn(int minimumWaterY, int maximumWaterY, int floorY) {
        SparseWorld world = new SparseWorld(V118OreMaterial.STONE);
        for (int y = minimumWaterY; y <= maximumWaterY; y++) {
            world.setMaterial(0, y, 0, V118OreMaterial.WATER);
        }
        world.setMaterial(0, floorY, 0, V118OreMaterial.STONE);
        return world;
    }

    private static final class SparseWorld implements V118UnderwaterMagmaFeature.WorldAccess {
        private final V118OreMaterial base;
        private final Map<String, V118OreMaterial> overrides = new HashMap<String, V118OreMaterial>();

        SparseWorld(V118OreMaterial base) {
            this.base = base;
        }

        @Override
        public V118OreMaterial getMaterial(int x, int y, int z) {
            V118OreMaterial material = overrides.get(key(x, y, z));
            return material == null ? base : material;
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

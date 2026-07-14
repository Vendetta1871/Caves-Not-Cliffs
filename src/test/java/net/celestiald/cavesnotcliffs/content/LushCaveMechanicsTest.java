package net.celestiald.cavesnotcliffs.content;

import org.junit.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LushCaveMechanicsTest {
    @Test
    public void caveVineAgeBandsRoundTripEveryOfficialState() {
        Set<String> encoded = new HashSet<>();
        for (int age = 0; age <= 25; age++) {
            for (boolean berries : new boolean[]{false, true}) {
                String path = LushCaveMechanics.caveVineHeadPath(age);
                int metadata = LushCaveMechanics.caveVineHeadMeta(age, berries);
                assertTrue(LushCaveMechanics.isCaveVineHead(path));
                assertEquals(age, LushCaveMechanics.caveVineHeadAge(path, metadata));
                assertEquals(berries, LushCaveMechanics.caveVineHasBerries(metadata));
                assertTrue(encoded.add(path + '#' + metadata));
            }
        }
        assertEquals(52, encoded.size());
        assertEquals(4, LushCaveMechanics.caveVineHeadPaths().size());
        assertTrue(LushCaveMechanics.isCaveVine(LushCaveMechanics.CAVE_VINES_PLANT));
        assertFalse(LushCaveMechanics.isCaveVine("glow_berries"));
    }

    @Test
    public void dripleafMetadataExhaustsEachRepresentedStateExactlyOnce() {
        Set<Integer> heads = new HashSet<>();
        Set<Integer> small = new HashSet<>();
        for (int facing = 0; facing < 4; facing++) {
            for (LushCaveMechanics.Tilt tilt : LushCaveMechanics.Tilt.values()) {
                int metadata = LushCaveMechanics.bigDripleafMeta(facing, tilt);
                assertEquals(facing, LushCaveMechanics.bigDripleafFacing(metadata));
                assertEquals(tilt, LushCaveMechanics.bigDripleafTilt(metadata));
                assertTrue(heads.add(metadata));
            }
            for (boolean upper : new boolean[]{false, true}) {
                for (boolean waterlogged : new boolean[]{false, true}) {
                    int metadata = LushCaveMechanics.smallDripleafMeta(facing, upper,
                            waterlogged);
                    assertEquals(facing, LushCaveMechanics.dripleafFacing(metadata));
                    assertEquals(upper, LushCaveMechanics.dripleafUpper(metadata));
                    assertEquals(waterlogged,
                            LushCaveMechanics.dripleafWaterlogged(metadata));
                    assertTrue(small.add(metadata));
                }
            }
        }
        assertEquals(16, heads.size());
        assertEquals(16, small.size());
    }

    @Test
    public void tiltStateMachineMatchesTheThreeOfficialDelays() {
        assertEquals(-1, LushCaveMechanics.Tilt.NONE.getDelay());
        assertEquals(10, LushCaveMechanics.Tilt.UNSTABLE.getDelay());
        assertEquals(10, LushCaveMechanics.Tilt.PARTIAL.getDelay());
        assertEquals(100, LushCaveMechanics.Tilt.FULL.getDelay());
        assertEquals(LushCaveMechanics.Tilt.UNSTABLE,
                LushCaveMechanics.Tilt.NONE.next());
        assertEquals(LushCaveMechanics.Tilt.PARTIAL,
                LushCaveMechanics.Tilt.UNSTABLE.next());
        assertEquals(LushCaveMechanics.Tilt.FULL,
                LushCaveMechanics.Tilt.PARTIAL.next());
        assertEquals(LushCaveMechanics.Tilt.NONE,
                LushCaveMechanics.Tilt.FULL.next());
    }

    @Test
    public void pinsOfficialGrowthAndBonemealNumbers() {
        assertEquals(0.10D, LushCaveMechanics.CAVE_VINE_GROWTH_CHANCE, 0.0D);
        assertEquals(0.11F, LushCaveMechanics.CAVE_VINE_BERRY_GROWTH_CHANCE, 0.0F);
        assertEquals(14, LushCaveMechanics.CAVE_VINE_LIGHT);
        assertEquals(0.45F, LushCaveMechanics.AZALEA_BONEMEAL_SUCCESS_CHANCE, 0.0F);
        assertEquals(5, LushCaveMechanics.MOSS_BONEMEAL_VERTICAL_RANGE);
        assertEquals(0.75F, LushCaveMechanics.MOSS_BONEMEAL_EDGE_CHANCE, 0.0F);
        assertEquals(0.60F, LushCaveMechanics.MOSS_BONEMEAL_VEGETATION_CHANCE, 0.0F);

        Set<Integer> heights = new HashSet<>();
        Random random = new Random(0L);
        for (int index = 0; index < 1000; index++) {
            int height = LushCaveMechanics.generatedBigDripleafHeight(random);
            assertTrue(height >= 2 && height <= 5);
            heights.add(height);
        }
        assertEquals(4, heights.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnrepresentableCaveVineAge() {
        LushCaveMechanics.caveVineHeadMeta(26, false);
    }
}

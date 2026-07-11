package net.celestiald.cavesnotcliffs.content;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CampfireMechanicsTest {
    @Test
    public void allSixteenMetadataStatesRoundTrip() {
        boolean[] values = {false, true};
        int seen = 0;
        for (int facing = 0; facing < 4; ++facing) {
            for (boolean lit : values) {
                for (boolean waterlogged : values) {
                    int metadata = CampfireMechanics.metadata(facing, lit, waterlogged);
                    assertEquals(facing, CampfireMechanics.horizontalFacing(metadata));
                    assertEquals(lit, CampfireMechanics.lit(metadata));
                    assertEquals(waterlogged, CampfireMechanics.waterlogged(metadata));
                    seen |= 1 << metadata;
                }
            }
        }
        assertEquals(0xffff, seen);
    }

    @Test
    public void waterControlsPlacementAndRelighting() {
        assertTrue(CampfireMechanics.placementLit(false));
        assertFalse(CampfireMechanics.placementLit(true));
        assertTrue(CampfireMechanics.canLight(false, false));
        assertFalse(CampfireMechanics.canLight(true, false));
        assertFalse(CampfireMechanics.canLight(false, true));
    }

    @Test
    public void coolingAndDamageUseOfficialConstants() {
        assertEquals(0, CampfireMechanics.coolProgress(0));
        assertEquals(0, CampfireMechanics.coolProgress(1));
        assertEquals(0, CampfireMechanics.coolProgress(2));
        assertEquals(1, CampfireMechanics.coolProgress(3));
        assertEquals(1, CampfireMechanics.fireDamage(false));
        assertEquals(2, CampfireMechanics.fireDamage(true));
        assertEquals(4, CampfireMechanics.SLOT_COUNT);
        assertEquals(600, CampfireMechanics.DEFAULT_COOKING_TIME);
        assertEquals(5, CampfireMechanics.SMOKE_DISTANCE);
    }
}

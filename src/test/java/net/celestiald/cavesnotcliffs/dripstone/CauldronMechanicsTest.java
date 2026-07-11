package net.celestiald.cavesnotcliffs.dripstone;

import net.celestiald.cavesnotcliffs.dripstone.CauldronMechanics.DripFluid;
import net.celestiald.cavesnotcliffs.dripstone.CauldronMechanics.State;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CauldronMechanicsTest {
    @Test
    public void dripMatrixMatchesJava1182() {
        State empty = CauldronMechanics.empty();
        assertEquals(CauldronMechanics.water(1),
                CauldronMechanics.receiveDrip(empty, DripFluid.WATER));
        assertEquals(CauldronMechanics.lava(),
                CauldronMechanics.receiveDrip(empty, DripFluid.LAVA));

        for (int level = 1; level <= 3; ++level) {
            State water = CauldronMechanics.water(level);
            assertEquals(level < 3, CauldronMechanics.canReceiveDrip(water, DripFluid.WATER));
            assertFalse(CauldronMechanics.canReceiveDrip(water, DripFluid.LAVA));
            assertEquals(level == 3 ? water : CauldronMechanics.water(level + 1),
                    CauldronMechanics.receiveDrip(water, DripFluid.WATER));
            assertFalse(CauldronMechanics.canReceiveDrip(
                    CauldronMechanics.powderSnow(level), DripFluid.WATER));
        }
        assertFalse(CauldronMechanics.canReceiveDrip(
                CauldronMechanics.lava(), DripFluid.LAVA));
    }

    @Test
    public void bucketComparatorAndHeightMatricesAreExact() {
        assertFalse(CauldronMechanics.canFillBucket(CauldronMechanics.empty()));
        assertTrue(CauldronMechanics.canFillBucket(CauldronMechanics.lava()));
        assertEquals(15.0D / 16.0D,
                CauldronMechanics.contentHeight(CauldronMechanics.lava()), 0.0D);
        for (int level = 1; level <= 3; ++level) {
            assertEquals(level == 3,
                    CauldronMechanics.canFillBucket(CauldronMechanics.water(level)));
            assertEquals(level, CauldronMechanics.comparatorSignal(
                    CauldronMechanics.water(level)));
            assertEquals((6.0D + level * 3.0D) / 16.0D,
                    CauldronMechanics.contentHeight(CauldronMechanics.water(level)), 0.0D);
        }
    }

    @Test
    public void precipitationAndExtinguishingUseStrictOfficialThresholds() {
        State empty = CauldronMechanics.empty();
        assertEquals(CauldronMechanics.water(1), CauldronMechanics.precipitation(
                empty, false, Math.nextDown(0.05F)));
        assertEquals(empty, CauldronMechanics.precipitation(empty, false, 0.05F));
        assertEquals(CauldronMechanics.powderSnow(1), CauldronMechanics.precipitation(
                empty, true, Math.nextDown(0.1F)));
        assertEquals(empty, CauldronMechanics.precipitation(empty, true, 0.1F));
        assertEquals(CauldronMechanics.water(2), CauldronMechanics.precipitation(
                CauldronMechanics.water(1), false, 0.0F));
        assertEquals(CauldronMechanics.water(2),
                CauldronMechanics.extinguishInPowderSnow(
                        CauldronMechanics.powderSnow(3)));
        assertEquals(empty, CauldronMechanics.extinguishInPowderSnow(
                CauldronMechanics.powderSnow(1)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsLayeredLava() {
        new State(CauldronMechanics.Content.LAVA, 1);
    }
}

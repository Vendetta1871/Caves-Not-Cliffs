package net.celestiald.cavesnotcliffs.dripstone;

import net.celestiald.cavesnotcliffs.dripstone.CauldronMechanics.DripFluid;
import net.celestiald.cavesnotcliffs.dripstone.CauldronMechanics.Interaction;
import net.celestiald.cavesnotcliffs.dripstone.CauldronMechanics.State;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CauldronMechanicsTest {
    private static final State[] ALL_STATES = {
            CauldronMechanics.empty(),
            CauldronMechanics.water(1),
            CauldronMechanics.water(2),
            CauldronMechanics.water(3),
            CauldronMechanics.lava(),
            CauldronMechanics.powderSnow(1),
            CauldronMechanics.powderSnow(2),
            CauldronMechanics.powderSnow(3)
    };

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
            assertEquals(level, CauldronMechanics.comparatorSignal(
                    CauldronMechanics.powderSnow(level)));
        }
    }

    @Test
    public void everyFilledBucketReplacesEveryOfficialContentState() {
        for (State before : ALL_STATES) {
            assertTrue(CauldronMechanics.canInteract(before, Interaction.FILL_WATER));
            assertEquals(CauldronMechanics.water(3),
                    CauldronMechanics.interact(before, Interaction.FILL_WATER));
            assertTrue(CauldronMechanics.canInteract(before, Interaction.FILL_LAVA));
            assertEquals(CauldronMechanics.lava(),
                    CauldronMechanics.interact(before, Interaction.FILL_LAVA));
            assertTrue(CauldronMechanics.canInteract(
                    before, Interaction.FILL_POWDER_SNOW));
            assertEquals(CauldronMechanics.powderSnow(3),
                    CauldronMechanics.interact(before, Interaction.FILL_POWDER_SNOW));
        }
    }

    @Test
    public void extractionBottleAndCleaningMapsCoverEveryOfficialState() {
        for (State before : ALL_STATES) {
            boolean fullBucket = before.equals(CauldronMechanics.water(3))
                    || before.equals(CauldronMechanics.lava())
                    || before.equals(CauldronMechanics.powderSnow(3));
            assertEquals(fullBucket,
                    CauldronMechanics.canInteract(before, Interaction.TAKE_BUCKET));
            assertEquals(fullBucket ? CauldronMechanics.empty() : before,
                    CauldronMechanics.interact(before, Interaction.TAKE_BUCKET));

            boolean water = before.content == CauldronMechanics.Content.WATER;
            State lowered = water ? CauldronMechanics.lowerLayer(before) : before;
            assertEquals(water,
                    CauldronMechanics.canInteract(before, Interaction.TAKE_BOTTLE));
            assertEquals(lowered,
                    CauldronMechanics.interact(before, Interaction.TAKE_BOTTLE));
            assertEquals(water,
                    CauldronMechanics.canInteract(before, Interaction.CLEAN));
            assertEquals(lowered,
                    CauldronMechanics.interact(before, Interaction.CLEAN));
        }
    }

    @Test
    public void waterBottleMapCoversEmptyAndEveryLayeredOrForeignContent() {
        for (State before : ALL_STATES) {
            boolean accepted = before.content == CauldronMechanics.Content.EMPTY
                    || before.content == CauldronMechanics.Content.WATER
                    && before.level < CauldronMechanics.MAX_LEVEL;
            State expected = before;
            if (before.content == CauldronMechanics.Content.EMPTY) {
                expected = CauldronMechanics.water(1);
            } else if (accepted) {
                expected = CauldronMechanics.water(before.level + 1);
            }
            assertEquals(accepted, CauldronMechanics.canInteract(
                    before, Interaction.POUR_WATER_BOTTLE));
            assertEquals(expected, CauldronMechanics.interact(
                    before, Interaction.POUR_WATER_BOTTLE));
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

        for (State before : ALL_STATES) {
            State rain = before;
            if (before.content == CauldronMechanics.Content.EMPTY) {
                rain = CauldronMechanics.water(1);
            } else if (before.content == CauldronMechanics.Content.WATER
                    && before.level < 3) {
                rain = CauldronMechanics.water(before.level + 1);
            }
            State snow = before;
            if (before.content == CauldronMechanics.Content.EMPTY) {
                snow = CauldronMechanics.powderSnow(1);
            } else if (before.content == CauldronMechanics.Content.POWDER_SNOW
                    && before.level < 3) {
                snow = CauldronMechanics.powderSnow(before.level + 1);
            }
            assertEquals(rain, CauldronMechanics.precipitation(before, false, 0.0F));
            assertEquals(snow, CauldronMechanics.precipitation(before, true, 0.0F));
            assertEquals(before, CauldronMechanics.precipitation(before, false, 0.05F));
            assertEquals(before, CauldronMechanics.precipitation(before, true, 0.1F));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsLayeredLava() {
        new State(CauldronMechanics.Content.LAVA, 1);
    }
}

package net.celestiald.cavesnotcliffs.powdersnow;

import org.junit.Test;

import static net.celestiald.cavesnotcliffs.powdersnow.PowderSnowMechanics.CollisionShape.FALLING;
import static net.celestiald.cavesnotcliffs.powdersnow.PowderSnowMechanics.CollisionShape.FULL;
import static net.celestiald.cavesnotcliffs.powdersnow.PowderSnowMechanics.CollisionShape.NONE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PowderSnowMechanicsTest {
    @Test
    public void freezeAndThawEveryValidStateExactlyLikeJava1182() {
        for (int current = 0; current <= 140; ++current) {
            assertEquals(Math.min(140, current + 1),
                PowderSnowMechanics.nextFrozenTicks(current, true, true));
            assertEquals(Math.max(0, current - 2),
                PowderSnowMechanics.nextFrozenTicks(current, false, true));
            assertEquals(Math.max(0, current - 2),
                PowderSnowMechanics.nextFrozenTicks(current, true, false));
            assertEquals(Math.max(0, current - 2),
                PowderSnowMechanics.nextFrozenTicks(current, false, false));
        }
        assertEquals(0, PowderSnowMechanics.nextFrozenTicks(-50, false, true));
        assertEquals(140, PowderSnowMechanics.nextFrozenTicks(500, true, true));
    }

    @Test
    public void frozenDamageUsesExactThresholdCadenceAndAmounts() {
        for (int ticksFrozen = 0; ticksFrozen <= 140; ++ticksFrozen) {
            for (int entityTick = 0; entityTick < 400; ++entityTick) {
                boolean expected = ticksFrozen == 140 && entityTick % 40 == 0;
                assertEquals(expected, PowderSnowMechanics.shouldApplyFrozenDamage(
                    entityTick, ticksFrozen, true));
                assertFalse(PowderSnowMechanics.shouldApplyFrozenDamage(
                    entityTick, ticksFrozen, false));
            }
        }
        assertEquals(1, PowderSnowMechanics.frozenDamage(false));
        assertEquals(5, PowderSnowMechanics.frozenDamage(true));
        assertEquals(0.0F, PowderSnowMechanics.frozenPercent(0), 0.0F);
        assertEquals(0.5F, PowderSnowMechanics.frozenPercent(70), 0.0F);
        assertEquals(1.0F, PowderSnowMechanics.frozenPercent(140), 0.0F);
        assertEquals(-0.025D, PowderSnowMechanics.movementSpeedModifier(70), 0.0D);
    }

    @Test
    public void collisionTruthTablePreservesOfficialPrecedence() {
        boolean[] values = {false, true};
        for (boolean fallingBlock : values) {
            for (boolean canWalk : values) {
                for (boolean above : values) {
                    for (boolean descending : values) {
                        assertEquals(FALLING, PowderSnowMechanics.collisionShape(2.5001F,
                            fallingBlock, canWalk, above, descending));
                        PowderSnowMechanics.CollisionShape expected = fallingBlock
                            || canWalk && above && !descending ? FULL : NONE;
                        assertEquals(expected, PowderSnowMechanics.collisionShape(2.5F,
                            fallingBlock, canWalk, above, descending));
                        assertEquals(expected, PowderSnowMechanics.collisionShape(0.0F,
                            fallingBlock, canWalk, above, descending));
                    }
                }
            }
        }
    }

    @Test
    public void fallSoundThresholdsAreInclusive() {
        assertFalse(PowderSnowMechanics.shouldPlayFallSound(3.999F, true));
        assertTrue(PowderSnowMechanics.shouldPlayFallSound(4.0F, true));
        assertFalse(PowderSnowMechanics.shouldPlayFallSound(20.0F, false));
        assertFalse(PowderSnowMechanics.shouldPlayBigFallSound(6.999F));
        assertTrue(PowderSnowMechanics.shouldPlayBigFallSound(7.0F));
    }

    @Test
    public void powderSnowCauldronUsesExactChanceLevelsAndHeights() {
        assertTrue(PowderSnowMechanics.shouldFillFromSnow(0.0D));
        assertTrue(PowderSnowMechanics.shouldFillFromSnow(
            Math.nextAfter(0.1D, Double.NEGATIVE_INFINITY)));
        assertFalse(PowderSnowMechanics.shouldFillFromSnow(0.1D));
        assertFalse(PowderSnowMechanics.shouldFillFromSnow(0.999999D));

        for (int level = 1; level <= 3; ++level) {
            assertEquals(level, PowderSnowMechanics.nextPowderSnowCauldronLevel(
                level, false, 0.0D));
            assertEquals(Math.min(3, level + 1),
                PowderSnowMechanics.nextPowderSnowCauldronLevel(level, true, 0.0D));
            assertEquals(level, PowderSnowMechanics.nextPowderSnowCauldronLevel(
                level, true, 0.1D));
            assertEquals(level - 1,
                PowderSnowMechanics.waterLevelAfterExtinguishing(level));
            assertEquals((6.0D + 3.0D * level) / 16.0D,
                PowderSnowMechanics.cauldronContentHeight(level), 0.0D);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidCauldronLevel() {
        PowderSnowMechanics.cauldronContentHeight(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidRandomUnit() {
        PowderSnowMechanics.shouldFillFromSnow(1.0D);
    }
}

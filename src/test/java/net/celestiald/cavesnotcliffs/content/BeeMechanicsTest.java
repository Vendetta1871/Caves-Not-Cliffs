package net.celestiald.cavesnotcliffs.content;

import net.minecraft.world.EnumDifficulty;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BeeMechanicsTest {
    @Test
    public void exactOccupationThresholdsAreStrict() {
        assertEquals(600, BeeMechanics.minimumOccupationTicks(false));
        assertEquals(2400, BeeMechanics.minimumOccupationTicks(true));
        assertFalse(BeeMechanics.occupationComplete(600, 600));
        assertTrue(BeeMechanics.occupationComplete(601, 600));
        assertFalse(BeeMechanics.occupationComplete(2400, 2400));
        assertTrue(BeeMechanics.occupationComplete(2401, 2400));
    }

    @Test
    public void honeyIncrementUsesExactOnePercentBranchAndCap() {
        assertEquals(2, BeeMechanics.honeyIncrement(0, new FixedRandom(0)));
        assertEquals(1, BeeMechanics.honeyIncrement(0, new FixedRandom(1)));
        assertEquals(1, BeeMechanics.honeyIncrement(4, new FixedRandom(0)));
        assertEquals(0, BeeMechanics.honeyIncrement(5, new NoCallRandom()));
    }

    @Test
    public void angerRangeIsTwentyThroughThirtyNineSeconds() {
        assertEquals(400, BeeMechanics.nextAngerTime(new FixedRandom(0)));
        assertEquals(780, BeeMechanics.nextAngerTime(new FixedRandom(19)));
    }

    @Test
    public void stingPoisonMatchesDifficultyTable() {
        assertEquals(0, BeeMechanics.poisonDurationTicks(EnumDifficulty.PEACEFUL));
        assertEquals(0, BeeMechanics.poisonDurationTicks(EnumDifficulty.EASY));
        assertEquals(200, BeeMechanics.poisonDurationTicks(EnumDifficulty.NORMAL));
        assertEquals(360, BeeMechanics.poisonDurationTicks(EnumDifficulty.HARD));
    }

    @Test
    public void stingDeathUsesEveryFifthTickAndShrinkingBound() {
        assertFalse(BeeMechanics.diesAfterSting(4, new NoCallRandom()));
        assertTrue(BeeMechanics.diesAfterSting(5, new FixedRandom(0)));
        assertFalse(BeeMechanics.diesAfterSting(5, new FixedRandom(1)));
        assertTrue(BeeMechanics.diesAfterSting(1200, new FixedRandom(0)));
        assertTrue(BeeMechanics.diesAfterSting(1205, new FixedRandom(0)));
    }

    @Test
    public void hiveEntryTruthTableMatchesOracle() {
        assertTrue(BeeMechanics.wantsToEnterHive(0, false, false, false,
                0, false, false, true, false));
        assertTrue(BeeMechanics.wantsToEnterHive(0, false, false, false,
                3601, false, false, false, false));
        assertTrue(BeeMechanics.wantsToEnterHive(0, false, false, false,
                0, true, false, false, false));
        assertFalse(BeeMechanics.wantsToEnterHive(1, false, false, false,
                0, false, false, true, false));
        assertFalse(BeeMechanics.wantsToEnterHive(0, true, false, false,
                0, false, false, true, false));
        assertFalse(BeeMechanics.wantsToEnterHive(0, false, true, false,
                0, false, false, true, false));
        assertFalse(BeeMechanics.wantsToEnterHive(0, false, false, true,
                0, false, false, true, false));
        assertFalse(BeeMechanics.wantsToEnterHive(0, false, false, false,
                0, false, false, true, true));
    }

    @Test
    public void releaseAndPollinationBoundariesAreExact() {
        assertTrue(BeeMechanics.canRelease(false, false, false, false));
        assertFalse(BeeMechanics.canRelease(false, true, false, false));
        assertFalse(BeeMechanics.canRelease(false, false, true, false));
        assertFalse(BeeMechanics.canRelease(false, false, false, true));
        assertTrue(BeeMechanics.canRelease(true, true, true, true));
        assertFalse(BeeMechanics.pollinatedLongEnough(400));
        assertTrue(BeeMechanics.pollinatedLongEnough(401));
        assertFalse(BeeMechanics.pollinationTimedOut(600));
        assertTrue(BeeMechanics.pollinationTimedOut(601));
    }

    @Test
    public void cropGrowthPinsRandomAndCountGates() {
        assertFalse(BeeMechanics.canGrowCrop(10, 0.99F, true, true));
        assertFalse(BeeMechanics.canGrowCrop(0, 0.299999F, true, true));
        assertTrue(BeeMechanics.canGrowCrop(0, 0.3F, true, true));
        assertFalse(BeeMechanics.canGrowCrop(0, 0.3F, false, true));
        assertFalse(BeeMechanics.canGrowCrop(0, 0.3F, true, false));
    }

    private static class FixedRandom extends Random {
        private final int value;

        FixedRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            return Math.floorMod(value, bound);
        }
    }

    private static final class NoCallRandom extends Random {
        @Override
        public int nextInt(int bound) {
            throw new AssertionError("RNG must not be consumed");
        }
    }
}

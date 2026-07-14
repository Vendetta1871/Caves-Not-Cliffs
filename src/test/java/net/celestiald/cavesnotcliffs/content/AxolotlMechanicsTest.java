package net.celestiald.cavesnotcliffs.content;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AxolotlMechanicsTest {
    @Test
    public void variantsHaveCanonicalIdsAndNames() {
        String[] names = {"lucy", "wild", "gold", "cyan", "blue"};
        for (int id = 0; id < names.length; id++) {
            AxolotlMechanics.Variant variant = AxolotlMechanics.Variant.byId(id);
            assertEquals(id, variant.id());
            assertEquals(names[id], variant.serializedName());
            assertEquals(id != 4, variant.isCommon());
        }
        assertEquals(AxolotlMechanics.Variant.LUCY,
                AxolotlMechanics.Variant.byId(-1));
        assertEquals(AxolotlMechanics.Variant.LUCY,
                AxolotlMechanics.Variant.byId(5));
    }

    @Test
    public void commonSpawnVariantsNeverSelectBlue() {
        Random random = new Random(123456789L);
        boolean[] seen = new boolean[4];
        for (int i = 0; i < 10000; i++) {
            AxolotlMechanics.Variant variant = AxolotlMechanics.Variant.common(random);
            assertTrue(variant.isCommon());
            seen[variant.id()] = true;
        }
        for (boolean value : seen) {
            assertTrue(value);
        }
    }

    @Test
    public void breedingUsesExactOneInTwelveHundredBlueRoll() {
        int blue = 0;
        Random random = new Random(0xA701071L);
        for (int i = 0; i < 1_200_000; i++) {
            if (AxolotlMechanics.childVariant(AxolotlMechanics.Variant.WILD,
                    AxolotlMechanics.Variant.GOLD, random)
                    == AxolotlMechanics.Variant.BLUE) {
                blue++;
            }
        }
        // Deterministic Java Random oracle; this also pins RNG call ordering.
        assertEquals(991, blue);
    }

    @Test
    public void playDeadRequiresEveryVanillaCondition() {
        Random eligible = new FixedRandom(0, 0);
        assertTrue(AxolotlMechanics.shouldPlayDead(eligible,
                2.0F, 10.0F, 14.0F, true, true, false));
        assertFalse(AxolotlMechanics.shouldPlayDead(new FixedRandom(1, 0),
                2.0F, 10.0F, 14.0F, true, true, false));
        assertFalse(AxolotlMechanics.shouldPlayDead(new FixedRandom(0, 2),
                2.0F, 10.0F, 14.0F, true, true, false));
        assertFalse(AxolotlMechanics.shouldPlayDead(new FixedRandom(0, 0),
                10.0F, 10.0F, 14.0F, true, true, false));
        assertFalse(AxolotlMechanics.shouldPlayDead(new FixedRandom(0, 0),
                2.0F, 10.0F, 14.0F, false, true, false));
        assertFalse(AxolotlMechanics.shouldPlayDead(new FixedRandom(0, 0),
                2.0F, 10.0F, 14.0F, true, false, false));
        assertFalse(AxolotlMechanics.shouldPlayDead(new FixedRandom(0, 0),
                2.0F, 10.0F, 14.0F, true, true, true));
        assertTrue(AxolotlMechanics.shouldPlayDead(new FixedRandom(0, 2),
                2.0F, 6.0F, 14.0F, true, true, false));
    }

    @Test
    public void airRegenerationAndSpawnSurfaceMatchOracle() {
        assertEquals(1800, AxolotlMechanics.rehydratedAir(0));
        assertEquals(6000, AxolotlMechanics.rehydratedAir(5999));
        assertEquals(100, AxolotlMechanics.regenerationDuration(0));
        assertEquals(2400, AxolotlMechanics.regenerationDuration(2399));
        assertTrue(AxolotlMechanics.isSpawnableSurface("minecraft:clay"));
        assertFalse(AxolotlMechanics.isSpawnableSurface("minecraft:stone"));
    }

    private static final class FixedRandom extends Random {
        private final int[] values;
        private int index;

        FixedRandom(int... values) {
            this.values = values;
        }

        @Override
        public int nextInt(int bound) {
            return values[index++] % bound;
        }
    }
}

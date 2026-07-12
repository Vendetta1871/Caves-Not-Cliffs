package net.celestiald.cavesnotcliffs.content;

import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class VanillaRawOreDropsTest {
    @BeforeClass
    public static void bootstrapMinecraftRegistries() {
        Bootstrap.register();
    }

    @Test
    public void replacesOnlyVanillaIronAndGoldWithoutSilkTouch() {
        assertEquals("raw_iron", VanillaRawOreDrops.replacementPath(Blocks.IRON_ORE, false));
        assertEquals("raw_gold", VanillaRawOreDrops.replacementPath(Blocks.GOLD_ORE, false));
        assertNull(VanillaRawOreDrops.replacementPath(Blocks.COAL_ORE, false));
        assertNull(VanillaRawOreDrops.replacementPath(Blocks.IRON_ORE, true));
        assertNull(VanillaRawOreDrops.replacementPath(Blocks.GOLD_ORE, true));
    }

    @Test
    public void vanillaRawOresUseTheExactOreDropsFortuneBounds() {
        for (int fortune = 0; fortune <= 3; fortune++) {
            Random random = new Random(0x118200L + fortune);
            boolean sawMinimum = false;
            boolean sawMaximum = false;
            int maximum = fortune + 1;
            for (int sample = 0; sample < 20_000; sample++) {
                int count = VanillaRawOreDrops.rollRawCount(fortune, random);
                assertTrue(count >= 1);
                assertTrue(count <= maximum);
                sawMinimum |= count == 1;
                sawMaximum |= count == maximum;
            }
            assertTrue("fortune " + fortune + " never rolled its minimum", sawMinimum);
            assertTrue("fortune " + fortune + " never rolled its maximum", sawMaximum);
        }
    }

    @Test
    public void onlyTransformsTheUntouchedVanillaSelfDrop() {
        assertTrue(VanillaRawOreDrops.isVanillaSelfDrop(Blocks.IRON_ORE,
                Collections.singletonList(new ItemStack(Blocks.IRON_ORE))));
        assertTrue(VanillaRawOreDrops.isVanillaSelfDrop(Blocks.GOLD_ORE,
                Collections.singletonList(new ItemStack(Blocks.GOLD_ORE))));
        assertFalse(VanillaRawOreDrops.isVanillaSelfDrop(Blocks.IRON_ORE,
                Collections.<ItemStack>emptyList()));

        List<ItemStack> augmented = new ArrayList<>();
        augmented.add(new ItemStack(Blocks.IRON_ORE));
        augmented.add(new ItemStack(Blocks.COAL_ORE));
        assertFalse(VanillaRawOreDrops.isVanillaSelfDrop(Blocks.IRON_ORE, augmented));
    }

    @Test
    public void explosionDecayFiltersFortuneDropsPerItemAndIncludesTheBoundary() {
        SequenceRandom random = new SequenceRandom(
                0.10F, 0.90F, 0.50F, 0.5001F, 0.0F);
        assertEquals(3, VanillaRawOreDrops.applyExplosionDecay(5, 0.50F, random));
        assertEquals(5, random.getCalls());

        assertEquals(0, VanillaRawOreDrops.applyExplosionDecay(
                5, 0.0F, new SequenceRandom(0.0F)));
        assertEquals(5, VanillaRawOreDrops.applyExplosionDecay(
                5, 1.0F, new SequenceRandom(1.0F)));
    }

    @Test
    public void fortuneThenExplosionUsesOneDeterministicRandomSequence() {
        long seed = 0x1182CAFE5EEDL;
        Random actual = new Random(seed);
        int fortuneCount = VanillaRawOreDrops.rollRawCount(3, actual);
        int surviving = VanillaRawOreDrops.applyExplosionDecay(
                fortuneCount, 0.35F, actual);

        Random oracle = new Random(seed);
        int expectedCount = OreDropLogic.applyOreBonus(1, 3, oracle);
        int expectedSurviving = 0;
        for (int item = 0; item < expectedCount; item++) {
            if (oracle.nextFloat() <= 0.35F) {
                expectedSurviving++;
            }
        }
        assertEquals(expectedCount, fortuneCount);
        assertEquals(expectedSurviving, surviving);
    }

    private static final class SequenceRandom extends Random {
        private final float[] values;
        private int index;

        private SequenceRandom(float... values) {
            this.values = values;
        }

        @Override
        public float nextFloat() {
            if (index >= values.length) {
                throw new AssertionError("SequenceRandom exhausted");
            }
            return values[index++];
        }

        private int getCalls() {
            return index;
        }
    }
}

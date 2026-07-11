package net.celestiald.cavesnotcliffs.content;

import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
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
}

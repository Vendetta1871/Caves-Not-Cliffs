package net.celestiald.cavesnotcliffs.content;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OreDropLogicTest {
    @Test
    public void noFortuneLeavesTheBaseCountAlone() {
        assertEquals(5, OreDropLogic.applyOreBonus(5, 0, new Random(1L)));
        assertEquals(5, OreDropLogic.applyUniformBonus(5, 0, new Random(1L)));
    }

    @Test
    public void oreFormulaMatchesTheVanillaMultiplierBounds() {
        for (int fortune = 1; fortune <= 3; fortune++) {
            Random random = new Random(0xC0FFEE + fortune);
            for (int sample = 0; sample < 10_000; sample++) {
                int count = OreDropLogic.applyOreBonus(5, fortune, random);
                assertTrue(count >= 5);
                assertTrue(count <= 5 * (fortune + 1));
                assertEquals(0, count % 5);
            }
        }
    }

    @Test
    public void redstoneFormulaAddsAtMostOnePerFortuneLevel() {
        for (int fortune = 1; fortune <= 3; fortune++) {
            Random random = new Random(0x51A7E + fortune);
            for (int sample = 0; sample < 10_000; sample++) {
                int count = OreDropLogic.applyUniformBonus(4, fortune, random);
                assertTrue(count >= 4);
                assertTrue(count <= 4 + fortune);
            }
        }
    }
}

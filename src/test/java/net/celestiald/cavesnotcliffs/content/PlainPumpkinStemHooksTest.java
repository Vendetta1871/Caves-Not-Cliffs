package net.celestiald.cavesnotcliffs.content;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PlainPumpkinStemHooksTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void pumpkinStemRecognizesOnlyThePlainFruitPeer() {
        Block plain = PlainPumpkinContent.createBlock();

        assertFalse(PlainPumpkinStemHooks.matchesFruit(
                Blocks.PUMPKIN, Blocks.PUMPKIN, plain));
        assertTrue(PlainPumpkinStemHooks.matchesFruit(
                plain, Blocks.PUMPKIN, plain));
        assertFalse(PlainPumpkinStemHooks.matchesFruit(
                Blocks.MELON_BLOCK, Blocks.PUMPKIN, plain));
    }

    @Test
    public void melonIdentityAndPlacementRemainVanilla() {
        Block plain = PlainPumpkinContent.createBlock();

        assertTrue(PlainPumpkinStemHooks.matchesFruit(
                Blocks.MELON_BLOCK, Blocks.MELON_BLOCK, plain));
        assertFalse(PlainPumpkinStemHooks.matchesFruit(
                plain, Blocks.MELON_BLOCK, plain));
        assertSame(Blocks.MELON_BLOCK.getDefaultState(),
                PlainPumpkinStemHooks.fruitState(Blocks.MELON_BLOCK, plain));
    }

    @Test
    public void pumpkinPlacementUsesPlainPeerAndFailsClearlyIfItIsMissing() {
        Block plain = PlainPumpkinContent.createBlock();
        assertSame(plain.getDefaultState(),
                PlainPumpkinStemHooks.fruitState(Blocks.PUMPKIN, plain));

        try {
            PlainPumpkinStemHooks.fruitState(Blocks.PUMPKIN, null);
            fail("Pumpkin growth without the registered plain peer must abort");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("Plain pumpkin"));
        }
    }
}

package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HoneyPistonHooksTest {
    private static IBlockState honey;

    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
        Block block = new HoneyContent.HoneyBlockCustom()
                .setRegistryName(CncRegistryIds.HONEY_BLOCK);
        honey = block.getDefaultState();
    }

    @Test
    public void honeyAndSlimeNeverStickInEitherOrder() {
        assertFalse(HoneyPistonHooks.canStickToEachOther(
                honey, Blocks.SLIME_BLOCK.getDefaultState()));
        assertFalse(HoneyPistonHooks.canStickToEachOther(
                Blocks.SLIME_BLOCK.getDefaultState(), honey));
    }

    @Test
    public void everyOtherStickyPairKeepsForgeSemantics() {
        assertTrue(HoneyPistonHooks.canStickToEachOther(
                honey, Blocks.STONE.getDefaultState()));
        assertTrue(HoneyPistonHooks.canStickToEachOther(
                Blocks.STONE.getDefaultState(), honey));
        assertTrue(HoneyPistonHooks.canStickToEachOther(
                honey, honey));
        assertTrue(HoneyPistonHooks.canStickToEachOther(
                Blocks.SLIME_BLOCK.getDefaultState(),
                Blocks.SLIME_BLOCK.getDefaultState()));
        assertFalse(HoneyPistonHooks.canStickToEachOther(
                Blocks.STONE.getDefaultState(), Blocks.DIRT.getDefaultState()));
    }
}

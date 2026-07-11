package net.celestiald.cavesnotcliffs.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeepslateRedstoneOreTest {
    @BeforeClass
    public static void bootstrapMinecraftRegistries() {
        Bootstrap.register();
    }

    @Test
    public void litStateRoundTripsThroughLegacyMetadata() {
        BlockDeepslateOres.DeepslateRedstoneOreBlock block =
                new BlockDeepslateOres.DeepslateRedstoneOreBlock();
        IBlockState unlit = block.getDefaultState();
        IBlockState lit = unlit.withProperty(
                BlockDeepslateOres.DeepslateRedstoneOreBlock.LIT, true);

        assertFalse(unlit.getValue(BlockDeepslateOres.DeepslateRedstoneOreBlock.LIT));
        assertTrue(block.getStateFromMeta(1).getValue(
                BlockDeepslateOres.DeepslateRedstoneOreBlock.LIT));
        assertEquals(0, block.getMetaFromState(unlit));
        assertEquals(1, block.getMetaFromState(lit));
    }

    @Test
    public void onlyTheLitStateEmitsTheCanonicalLightLevel() {
        BlockDeepslateOres.DeepslateRedstoneOreBlock block =
                new BlockDeepslateOres.DeepslateRedstoneOreBlock();
        IBlockState unlit = block.getDefaultState();
        IBlockState lit = unlit.withProperty(
                BlockDeepslateOres.DeepslateRedstoneOreBlock.LIT, true);

        assertEquals(0, block.getLightValue(unlit, null, null));
        assertEquals(9, block.getLightValue(lit, null, null));
    }
}

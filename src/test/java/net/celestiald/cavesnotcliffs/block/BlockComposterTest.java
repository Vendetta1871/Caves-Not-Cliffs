package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.content.ComposterMechanics;
import net.celestiald.cavesnotcliffs.tile.TileEntityComposter;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.inventory.ISidedInventory;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BlockComposterTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void persistsEveryLevelInFourMetadataBits() {
        BlockComposter.BlockCustom block = new BlockComposter.BlockCustom();
        for (int level = 0; level <= 8; ++level) {
            IBlockState state = block.getStateFromMeta(level);
            assertEquals(level, state.getValue(BlockComposter.BlockCustom.LEVEL).intValue());
            assertEquals(level, block.getMetaFromState(state));
            assertEquals(level, ComposterMechanics.comparatorOutput(level));
        }
        assertEquals(0, block.getMetaFromState(block.getStateFromMeta(-1)));
        assertEquals(8, block.getMetaFromState(block.getStateFromMeta(15)));
    }

    @Test
    public void isHollowModelBlockWithSidedInventoryBridge() {
        BlockComposter.BlockCustom block = new BlockComposter.BlockCustom();
        assertFalse(block.isOpaqueCube(block.getDefaultState()));
        assertFalse(block.isFullCube(block.getDefaultState()));
        assertTrue(block.hasComparatorInputOverride(block.getDefaultState()));
        assertTrue(new TileEntityComposter() instanceof ISidedInventory);
    }
}

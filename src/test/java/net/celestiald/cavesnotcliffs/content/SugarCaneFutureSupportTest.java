package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.block.BlockAmethystGrowth;
import net.celestiald.cavesnotcliffs.block.BlockCandle;
import net.celestiald.cavesnotcliffs.block.BlockPointedDripstone;
import net.celestiald.cavesnotcliffs.block.CncFluidState;
import net.celestiald.cavesnotcliffs.block.LushDripleafBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SugarCaneFutureSupportTest {
    @BeforeClass
    public static void bootstrapMinecraftRegistries() {
        net.minecraft.init.Bootstrap.register();
    }

    @Test
    public void retainedWaterReaderCoversEveryBackportedStorageScheme() {
        LushDripleafBlocks.Small dripleaf = new LushDripleafBlocks.Small();
        BlockCandle candle = new BlockCandle(CandleMechanics.Color.UNCOLORED);
        BlockAmethystGrowth amethyst = BlockAmethystGrowth.waterloggedCompanion(
                "small_amethyst_bud", 3, 4, 1, false);
        BlockPointedDripstone dripstone = new BlockPointedDripstone(true);
        LightningRodContent.LightningRodBlock rod =
                new LightningRodContent.LightningRodBlock(true);
        CampfireContent.BlockCustom campfire = new CampfireContent.BlockCustom(false);

        assertTrue(CncFluidState.containsWater(Blocks.WATER.getDefaultState()));
        assertTrue(CncFluidState.containsWater(Blocks.FLOWING_WATER.getDefaultState()));
        assertTrue(CncFluidState.containsWater(dripleaf.getDefaultState()
                .withProperty(LushDripleafBlocks.WATERLOGGED, true)));
        assertTrue(CncFluidState.containsWater(candle.getDefaultState()
                .withProperty(BlockCandle.WATERLOGGED, true)));
        assertTrue(CncFluidState.containsWater(amethyst.getDefaultState()));
        assertTrue(CncFluidState.containsWater(dripstone.getDefaultState()));
        assertTrue(CncFluidState.containsWater(rod.getDefaultState()));
        assertTrue(CncFluidState.containsWater(campfire.getDefaultState()
                .withProperty(CampfireContent.BlockCustom.WATERLOGGED, true)));

        assertFalse(CncFluidState.containsWater(null));
        assertFalse(CncFluidState.containsWater(Blocks.STONE.getDefaultState()));
        assertFalse(CncFluidState.containsWater(
                new Block(Material.WATER) { }.getDefaultState()));
        assertFalse(CncFluidState.containsWater(dripleaf.getDefaultState()));
        assertFalse(CncFluidState.containsWater(candle.getDefaultState()));
        assertFalse(CncFluidState.containsWater(
                new BlockPointedDripstone(false).getDefaultState()));
        assertFalse(CncFluidState.containsWater(
                new LightningRodContent.LightningRodBlock(false).getDefaultState()));
        assertFalse(CncFluidState.containsWater(campfire.getDefaultState()));
    }
}

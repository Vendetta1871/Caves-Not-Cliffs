package net.celestiald.cavesnotcliffs.content;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class CncBlockPropertiesTest {
    @Test
    public void convertsModernResistanceToTheLegacyApiScale() {
        assertEquals(5.0F, CncBlockProperties.legacyResistance(3.0F), 0.0F);
        assertEquals(10.0F, CncBlockProperties.legacyResistance(6.0F), 0.0F);
    }

    @Test
    public void installsCanonical118MapColorsAtTheirStableSlots() {
        assertSame(CncBlockProperties.DEEPSLATE,
                net.minecraft.block.material.MapColor.COLORS[59]);
        assertSame(CncBlockProperties.RAW_IRON,
                net.minecraft.block.material.MapColor.COLORS[60]);
        assertSame(CncBlockProperties.WARPED_NYLIUM,
                net.minecraft.block.material.MapColor.COLORS[55]);
        assertSame(CncBlockProperties.WARPED_STEM,
                net.minecraft.block.material.MapColor.COLORS[56]);
        assertEquals(0x646464, CncBlockProperties.DEEPSLATE.colorValue);
        assertEquals(0xD8AF93, CncBlockProperties.RAW_IRON.colorValue);
        assertEquals(0x167E86, CncBlockProperties.WARPED_NYLIUM.colorValue);
        assertEquals(0x3A8E8C, CncBlockProperties.WARPED_STEM.colorValue);
    }

    @Test
    public void rawCopperBlockUsesCanonicalMetalProperties() {
        Bootstrap.register();
        Block block = CncMaterialContent.createRawCopperBlock();
        assertSame(Material.IRON, block.getDefaultState().getMaterial());
        assertSame(SoundType.METAL, block.getSoundType());
        assertSame(MapColor.ADOBE,
                block.getMapColor(block.getDefaultState(), null, BlockPos.ORIGIN));
        assertEquals(5.0F,
                block.getBlockHardness(block.getDefaultState(), null, BlockPos.ORIGIN), 0.0F);
        assertEquals(6.0F, block.getExplosionResistance(null), 0.0F);
        assertEquals(1, block.getHarvestLevel(block.getDefaultState()));
    }

    @Test
    public void lightningRodUsesCanonicalCopperPropertiesAndColor() {
        Bootstrap.register();
        Block rod = new LightningRodContent.LightningRodBlock(false);
        assertSame(Material.IRON, rod.getDefaultState().getMaterial());
        assertSame(CopperSoundEvents.COPPER, rod.getSoundType());
        assertSame(MapColor.ADOBE,
                rod.getMapColor(rod.getDefaultState(), null, BlockPos.ORIGIN));
        assertEquals(3.0F,
                rod.getBlockHardness(rod.getDefaultState(), null, BlockPos.ORIGIN), 0.0F);
        assertEquals(6.0F, rod.getExplosionResistance(null), 0.0F);
        assertEquals(1, rod.getHarvestLevel(rod.getDefaultState()));
    }
}

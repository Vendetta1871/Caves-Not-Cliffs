package net.celestiald.cavesnotcliffs.content;

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
}

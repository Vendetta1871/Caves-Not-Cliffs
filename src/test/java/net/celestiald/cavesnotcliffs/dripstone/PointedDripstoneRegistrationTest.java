package net.celestiald.cavesnotcliffs.dripstone;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.block.BlockPointedDripstone;
import net.celestiald.cavesnotcliffs.block.BlockPointedDripstoneWaterlogged;
import net.celestiald.cavesnotcliffs.block.BlockLegacyStalactiteAlias;
import net.celestiald.cavesnotcliffs.block.BlockStalactite;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.Block;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PointedDripstoneRegistrationTest {
    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void exposesOneCanonicalItemAndOneBlockOnlyWaterloggedCompanion() {
        ElementsCavesNotCliffs elements = new ElementsCavesNotCliffs();
        new BlockStalactite(elements).initElements();
        new BlockPointedDripstoneWaterlogged(elements).initElements();

        assertEquals(2, elements.blocks.size());
        assertEquals(1, elements.items.size());
        Block dry = elements.blocks.get(0).get();
        Block wet = elements.blocks.get(1).get();
        Item publicItem = elements.items.get(0).get();
        assertEquals(CncRegistryIds.POINTED_DRIPSTONE, dry.getRegistryName());
        assertEquals(CncRegistryIds.POINTED_DRIPSTONE_WATERLOGGED, wet.getRegistryName());
        assertTrue(dry instanceof BlockPointedDripstone);
        assertTrue(wet instanceof BlockPointedDripstone);
        assertFalse(((BlockPointedDripstone) dry).isWaterloggedStorage());
        assertTrue(((BlockPointedDripstone) wet).isWaterloggedStorage());
        assertTrue(publicItem instanceof ItemBlock);
        assertEquals(CncRegistryIds.POINTED_DRIPSTONE, publicItem.getRegistryName());
    }

    @Test
    public void releasedStalactiteIdRemainsAHiddenBlockOnlyMigrationAlias() {
        ElementsCavesNotCliffs elements = new ElementsCavesNotCliffs();
        new BlockLegacyStalactiteAlias(elements).initElements();
        assertEquals(1, elements.blocks.size());
        assertTrue(elements.items.isEmpty());
        assertEquals("cavesnotcliffs:stalactite",
                elements.blocks.get(0).get().getRegistryName().toString());
    }
}

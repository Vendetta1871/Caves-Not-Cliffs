package net.celestiald.cavesnotcliffs.dripstone;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.block.BlockPointedDripstone;
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
import static org.junit.Assert.assertTrue;

public class PointedDripstoneRegistrationTest {
    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void exposesOneCanonicalPointedDripstoneBlockAndItem() {
        ElementsCavesNotCliffs elements = new ElementsCavesNotCliffs();
        new BlockStalactite(elements).initElements();

        assertEquals(1, elements.blocks.size());
        assertEquals(1, elements.items.size());
        Block pointed = elements.blocks.get(0).get();
        Item publicItem = elements.items.get(0).get();
        assertEquals(CncRegistryIds.POINTED_DRIPSTONE, pointed.getRegistryName());
        assertTrue(pointed instanceof BlockPointedDripstone);
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

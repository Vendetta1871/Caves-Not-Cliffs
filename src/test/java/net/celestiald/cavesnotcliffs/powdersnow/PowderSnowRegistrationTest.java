package net.celestiald.cavesnotcliffs.powdersnow;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.block.BlockPowderSnow;
import net.celestiald.cavesnotcliffs.block.BlockPowderSnowCauldron;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCauldron;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class PowderSnowRegistrationTest {
    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void powderSnowHasOnlyItsCanonicalSolidBucketItem() {
        ElementsCavesNotCliffs elements = new ElementsCavesNotCliffs();
        new BlockPowderSnow(elements).initElements();
        assertEquals(1, elements.blocks.size());
        assertEquals(1, elements.items.size());

        Block block = elements.blocks.get(0).get();
        Item bucket = elements.items.get(0).get();
        assertEquals(CncRegistryIds.POWDER_SNOW, block.getRegistryName());
        assertEquals(CncRegistryIds.POWDER_SNOW_BUCKET, bucket.getRegistryName());
        assertFalse("powder snow must not expose an ordinary ItemBlock",
            bucket instanceof ItemBlock);
        assertEquals(1, bucket.getItemStackLimit());
        assertTrue(bucket.hasContainerItem(new ItemStack(bucket)));
        assertSame(Items.BUCKET,
            bucket.getContainerItem(new ItemStack(bucket)).getItem());
    }

    @Test
    public void powderSnowCauldronIsBlockOnlyAndRoundTripsLevelsOneToThree() {
        ElementsCavesNotCliffs elements = new ElementsCavesNotCliffs();
        new BlockPowderSnowCauldron(elements).initElements();
        assertEquals(1, elements.blocks.size());
        assertTrue("hidden cauldron companion must not register an item",
            elements.items.isEmpty());

        Block block = elements.blocks.get(0).get();
        assertEquals(CncRegistryIds.POWDER_SNOW_CAULDRON, block.getRegistryName());
        assertTrue(block instanceof BlockPowderSnowCauldron.BlockCustom);
        assertEquals(Integer.valueOf(1),
            block.getDefaultState().getValue(BlockCauldron.LEVEL));
        for (int level = 1; level <= 3; ++level) {
            assertEquals(level, block.getMetaFromState(block.getStateFromMeta(level)));
        }
        assertSame(Items.CAULDRON,
            block.getItemDropped(block.getDefaultState(), new java.util.Random(0L), 0));
    }
}

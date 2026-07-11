package net.celestiald.cavesnotcliffs.entity;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.block.BlockBeehive;
import net.celestiald.cavesnotcliffs.item.ItemBlockBeehive;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.Block;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BeeRegistrationTest {
    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void canonicalPublicAndHiddenRegistryIdsAreStable() {
        assertEquals("cavesnotcliffs:bee", CncRegistryIds.BEE.toString());
        assertEquals("cavesnotcliffs:bee_nest", CncRegistryIds.BEE_NEST.toString());
        assertEquals("cavesnotcliffs:beehive", CncRegistryIds.BEEHIVE.toString());
        assertEquals("cavesnotcliffs:honeycomb", CncRegistryIds.HONEYCOMB.toString());
        assertEquals("cavesnotcliffs:bee_nest_honey",
                CncRegistryIds.BEE_NEST_HONEY.toString());
        assertEquals("cavesnotcliffs:beehive_honey",
                CncRegistryIds.BEEHIVE_HONEY.toString());
        assertTrue(EntityAnimal.class.isAssignableFrom(EntityBee.EntityCustom.class));
    }

    @Test
    public void honeyVisualCompanionsAreBlockOnly() {
        ElementsCavesNotCliffs elements = new ElementsCavesNotCliffs();
        new BlockBeehive(elements).initElements();
        assertEquals(4, elements.blocks.size());
        assertEquals(3, elements.items.size());

        Block nest = elements.blocks.get(0).get();
        Block hive = elements.blocks.get(1).get();
        Block nestHoney = elements.blocks.get(2).get();
        Block hiveHoney = elements.blocks.get(3).get();
        assertEquals(CncRegistryIds.BEE_NEST, nest.getRegistryName());
        assertEquals(CncRegistryIds.BEEHIVE, hive.getRegistryName());
        assertEquals(CncRegistryIds.BEE_NEST_HONEY, nestHoney.getRegistryName());
        assertEquals(CncRegistryIds.BEEHIVE_HONEY, hiveHoney.getRegistryName());
        assertTrue(elements.items.get(0).get() instanceof ItemBlockBeehive);
        assertTrue(elements.items.get(1).get() instanceof ItemBlockBeehive);
        Item comb = elements.items.get(2).get();
        assertEquals(CncRegistryIds.HONEYCOMB, comb.getRegistryName());
        for (java.util.function.Supplier<Item> item : elements.items) {
            String id = item.get().getRegistryName().toString();
            assertFalse(id.endsWith("_honey"));
        }
    }

    @Test
    public void facingMetadataAndCanonicalHoneyPropertyRoundTrip() {
        ElementsCavesNotCliffs elements = new ElementsCavesNotCliffs();
        new BlockBeehive(elements).initElements();
        for (java.util.function.Supplier<Block> supplier : elements.blocks) {
            Block block = supplier.get();
            for (int meta = 0; meta < 4; meta++) {
                assertEquals(meta, block.getMetaFromState(block.getStateFromMeta(meta)));
            }
        }
        NBTTagCompound canonical = new NBTTagCompound();
        canonical.setString("honey_level", "5");
        assertEquals(5, ItemBlockBeehive.readHoneyLevel(canonical));
        NBTTagCompound legacyDraft = new NBTTagCompound();
        legacyDraft.setInteger("honey_level", 4);
        assertEquals(4, ItemBlockBeehive.readHoneyLevel(legacyDraft));
        canonical.setString("honey_level", "invalid");
        assertEquals(0, ItemBlockBeehive.readHoneyLevel(canonical));
    }
}

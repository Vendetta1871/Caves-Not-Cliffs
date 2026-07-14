package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.tile.TileEntityCampfire;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.BeforeClass;
import org.junit.Test;
import net.minecraftforge.fml.common.registry.GameRegistry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TileEntityCampfireTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
        GameRegistry.registerTileEntity(TileEntityCampfire.class,
            CncRegistryIds.CAMPFIRE);
    }

    @Test
    public void acceptsFourSingleFoodsAndRejectsTheFifth() {
        TileEntityCampfire tile = new TileEntityCampfire();
        ItemStack potatoes = new ItemStack(Items.POTATO, 5);
        CampfireCooking.Recipe recipe = CampfireCooking.find(potatoes);
        assertNotNull(recipe);
        for (int slot = 0; slot < 4; ++slot) {
            assertTrue(tile.placeFood(potatoes, recipe));
            assertEquals(1, tile.getStackInSlot(slot).getCount());
            assertEquals(600, tile.cookingTime(slot));
            assertEquals(0, tile.cookingProgress(slot));
        }
        assertEquals(1, potatoes.getCount());
        assertFalse(tile.placeFood(potatoes, recipe));
        assertEquals(4, tile.getSizeInventory());
    }

    @Test
    public void saveReloadPreservesItemsAndCookingArrays() {
        TileEntityCampfire source = new TileEntityCampfire();
        ItemStack beef = new ItemStack(Items.BEEF, 2);
        assertTrue(source.placeFood(beef, CampfireCooking.find(beef)));
        NBTTagCompound saved = source.writeToNBT(new NBTTagCompound());
        assertTrue(saved.hasKey("Items", 9));
        assertEquals(4, saved.getIntArray("CookingTimes").length);
        assertEquals(4, saved.getIntArray("CookingTotalTimes").length);

        TileEntityCampfire restored = new TileEntityCampfire();
        restored.readFromNBT(saved);
        assertEquals(Items.BEEF, restored.getStackInSlot(0).getItem());
        assertEquals(0, restored.cookingProgress(0));
        assertEquals(600, restored.cookingTime(0));
    }

    @Test
    public void networkTagCarriesOnlyTheRenderedInventory() {
        TileEntityCampfire tile = new TileEntityCampfire();
        ItemStack chicken = new ItemStack(Items.CHICKEN);
        assertTrue(tile.placeFood(chicken, CampfireCooking.find(chicken)));
        NBTTagCompound update = tile.getUpdateTag();
        assertTrue(update.hasKey("Items", 9));
        assertFalse(update.hasKey("CookingTimes"));
        assertFalse(update.hasKey("CookingTotalTimes"));
    }
}

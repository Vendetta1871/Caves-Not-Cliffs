package net.celestiald.cavesnotcliffs.content;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.init.Items;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.junit.Test;
import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TropicalFishBucketRecipeTest {
    private final Item output = new Item();
    private final TropicalFishBucketRecipe recipe = new TropicalFishBucketRecipe(output);

    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void matchesOnlyWaterBucketAndClownfishMetadataTwo() {
        InventoryCrafting valid = inventory(new ItemStack(Items.WATER_BUCKET),
                new ItemStack(Items.FISH, 1, 2));
        assertTrue(recipe.matches(valid, null));
        assertEquals(output, recipe.getCraftingResult(valid).getItem());
        assertTrue(recipe.canFit(2, 1));
        assertFalse(recipe.canFit(1, 1));

        assertFalse(recipe.matches(inventory(new ItemStack(Items.WATER_BUCKET),
                new ItemStack(Items.FISH, 1, 0)), null));
        assertFalse(recipe.matches(inventory(new ItemStack(Items.BUCKET),
                new ItemStack(Items.FISH, 1, 2)), null));
        assertFalse(recipe.matches(inventory(new ItemStack(Items.WATER_BUCKET),
                new ItemStack(Items.FISH, 1, 2), new ItemStack(Items.STICK)), null));
    }

    @Test
    public void deliberatelyReturnsNoEmptyBucketRemainder() {
        InventoryCrafting inventory = inventory(new ItemStack(Items.WATER_BUCKET),
                new ItemStack(Items.FISH, 1, 2));
        for (ItemStack remainder : recipe.getRemainingItems(inventory)) {
            assertTrue(remainder.isEmpty());
        }
    }

    private static InventoryCrafting inventory(ItemStack... values) {
        InventoryCrafting inventory = new InventoryCrafting(new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return false;
            }
        }, 3, 3);
        for (int index = 0; index < values.length; index++) {
            inventory.setInventorySlotContents(index, values[index]);
        }
        return inventory;
    }
}

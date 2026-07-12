package net.celestiald.cavesnotcliffs.content;

import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapelessRecipes;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlainPumpkinRecipeTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void canonicalSeedRecipeAcceptsOnlyThePlainPeer() {
        ItemBlock plain = PlainPumpkinContent.createItem(
                PlainPumpkinContent.createBlock());
        ShapelessRecipes recipe = PlainPumpkinContent.pumpkinSeedsRecipe(plain);

        assertEquals(PlainPumpkinContent.PUMPKIN_SEEDS_RECIPE_ID,
                recipe.getRegistryName());
        assertEquals(1, recipe.getIngredients().size());
        assertTrue(recipe.getIngredients().get(0).apply(new ItemStack(plain)));
        assertFalse(recipe.getIngredients().get(0)
                .apply(new ItemStack(Blocks.PUMPKIN)));
        assertEquals(Items.PUMPKIN_SEEDS, recipe.getRecipeOutput().getItem());
        assertEquals(4, recipe.getRecipeOutput().getCount());
    }

    @Test
    public void canonicalPieRecipeIsShapelessAndRejectsCarvedPumpkin() {
        ItemBlock plain = PlainPumpkinContent.createItem(
                PlainPumpkinContent.createBlock());
        ShapelessRecipes recipe = PlainPumpkinContent.pumpkinPieRecipe(plain);

        assertEquals(PlainPumpkinContent.PUMPKIN_PIE_RECIPE_ID,
                recipe.getRegistryName());
        assertEquals(3, recipe.getIngredients().size());
        assertTrue(recipe.getIngredients().get(0).apply(new ItemStack(plain)));
        assertFalse(recipe.getIngredients().get(0)
                .apply(new ItemStack(Blocks.PUMPKIN)));
        assertTrue(recipe.getIngredients().get(1)
                .apply(new ItemStack(Items.SUGAR)));
        assertTrue(recipe.getIngredients().get(2)
                .apply(new ItemStack(Items.EGG)));
        assertEquals(Items.PUMPKIN_PIE, recipe.getRecipeOutput().getItem());
        assertEquals(1, recipe.getRecipeOutput().getCount());
    }
}

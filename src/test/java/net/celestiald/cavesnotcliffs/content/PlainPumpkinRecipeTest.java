package net.celestiald.cavesnotcliffs.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapelessRecipes;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

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

    @Test
    public void recipeBookUnlocksMatchThe1182PlainAndCarvedContracts()
            throws IOException {
        JsonObject seeds = advancement(
                "assets/minecraft/advancements/recipes/misc/pumpkin_seeds.json");
        assertEquals("cavesnotcliffs:pumpkin",
                criterionItem(seeds, "has_pumpkin"));
        assertEquals("minecraft:pumpkin_seeds",
                seeds.getAsJsonObject("rewards").getAsJsonArray("recipes")
                        .get(0).getAsString());
        assertRequirementGroup(seeds, "has_pumpkin", "has_the_recipe");

        JsonObject pie = advancement(
                "assets/minecraft/advancements/recipes/food/pumpkin_pie.json");
        assertEquals("cavesnotcliffs:pumpkin",
                criterionItem(pie, "has_pumpkin"));
        assertEquals("minecraft:pumpkin",
                criterionItem(pie, "has_carved_pumpkin"));
        assertEquals("minecraft:pumpkin_pie",
                pie.getAsJsonObject("rewards").getAsJsonArray("recipes")
                        .get(0).getAsString());
        assertRequirementGroup(pie, "has_carved_pumpkin", "has_pumpkin",
                "has_the_recipe");
    }

    private static JsonObject advancement(String path) throws IOException {
        InputStream stream = PlainPumpkinRecipeTest.class.getClassLoader()
                .getResourceAsStream(path);
        if (stream == null) {
            throw new IOException("Missing advancement " + path);
        }
        try {
            return new JsonParser().parse(new InputStreamReader(
                    stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } finally {
            stream.close();
        }
    }

    private static String criterionItem(JsonObject advancement, String name) {
        return advancement.getAsJsonObject("criteria").getAsJsonObject(name)
                .getAsJsonObject("conditions").getAsJsonArray("items")
                .get(0).getAsJsonObject().get("item").getAsString();
    }

    private static void assertRequirementGroup(JsonObject advancement,
            String... expected) {
        JsonArray groups = advancement.getAsJsonArray("requirements");
        assertEquals(1, groups.size());
        JsonArray group = groups.get(0).getAsJsonArray();
        assertEquals(expected.length, group.size());
        for (int index = 0; index < expected.length; ++index) {
            assertEquals(expected[index], group.get(index).getAsString());
        }
    }
}

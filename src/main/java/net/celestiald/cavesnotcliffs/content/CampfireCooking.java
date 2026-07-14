package net.celestiald.cavesnotcliffs.content;

import net.minecraft.item.Item;
import net.minecraft.item.ItemFishFood;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The eight Java 1.18.2 campfire recipes representable by Minecraft 1.12 items. */
public final class CampfireCooking {
    private static final int WILDCARD = -1;
    private static final List<Recipe> RECIPES = createRecipes();

    private CampfireCooking() {
    }

    public static Recipe find(ItemStack input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        for (Recipe recipe : RECIPES) {
            if (recipe.matches(input)) {
                return recipe;
            }
        }
        return null;
    }

    public static List<Recipe> recipes() {
        return RECIPES;
    }

    private static List<Recipe> createRecipes() {
        List<Recipe> recipes = new ArrayList<Recipe>();
        add(recipes, "baked_potato", Items.POTATO, WILDCARD,
            new ItemStack(Items.BAKED_POTATO), 0.35F);
        add(recipes, "cooked_beef", Items.BEEF, WILDCARD,
            new ItemStack(Items.COOKED_BEEF), 0.35F);
        add(recipes, "cooked_chicken", Items.CHICKEN, WILDCARD,
            new ItemStack(Items.COOKED_CHICKEN), 0.35F);
        add(recipes, "cooked_cod", Items.FISH,
            ItemFishFood.FishType.COD.getMetadata(),
            new ItemStack(Items.COOKED_FISH, 1,
                ItemFishFood.FishType.COD.getMetadata()), 0.35F);
        add(recipes, "cooked_mutton", Items.MUTTON, WILDCARD,
            new ItemStack(Items.COOKED_MUTTON), 0.35F);
        add(recipes, "cooked_porkchop", Items.PORKCHOP, WILDCARD,
            new ItemStack(Items.COOKED_PORKCHOP), 0.35F);
        add(recipes, "cooked_rabbit", Items.RABBIT, WILDCARD,
            new ItemStack(Items.COOKED_RABBIT), 0.35F);
        add(recipes, "cooked_salmon", Items.FISH,
            ItemFishFood.FishType.SALMON.getMetadata(),
            new ItemStack(Items.COOKED_FISH, 1,
                ItemFishFood.FishType.SALMON.getMetadata()), 0.35F);
        return Collections.unmodifiableList(recipes);
    }

    private static void add(List<Recipe> recipes, String id, Item input, int metadata,
            ItemStack output, float experience) {
        recipes.add(new Recipe(id, input, metadata, output, experience,
            CampfireMechanics.DEFAULT_COOKING_TIME));
    }

    public static final class Recipe {
        private final String id;
        private final Item input;
        private final int inputMetadata;
        private final ItemStack output;
        private final float experience;
        private final int cookingTime;

        private Recipe(String id, Item input, int inputMetadata, ItemStack output,
                float experience, int cookingTime) {
            this.id = id;
            this.input = input;
            this.inputMetadata = inputMetadata;
            this.output = output.copy();
            this.experience = experience;
            this.cookingTime = cookingTime;
        }

        public boolean matches(ItemStack stack) {
            return stack.getItem() == input && (inputMetadata == WILDCARD
                || stack.getMetadata() == inputMetadata);
        }

        public String id() {
            return id;
        }

        public Item input() {
            return input;
        }

        public int inputMetadata() {
            return inputMetadata;
        }

        public ItemStack output() {
            return output.copy();
        }

        public float experience() {
            return experience;
        }

        public int cookingTime() {
            return cookingTime;
        }
    }
}

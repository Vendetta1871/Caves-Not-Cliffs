package net.celestiald.cavesnotcliffs.stonecutter;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Immutable stonecutting recipes adapted from Java 1.18.2's generated data.
 *
 * <p>Java 1.12 has no stonecutting recipe type and still stores several flattened
 * blocks as item metadata. Keeping the recipes as stable stack descriptors lets
 * the server and client build the same ordered list without adding a global
 * crafting recipe or guessing from ore-dictionary entries.</p>
 */
public final class StonecutterRecipeCatalog {
    public enum Family {
        VANILLA_1_12_ADAPTER,
        DEEPSLATE_1_18_2,
        TUFF_1_21,
        CALCITE_CUSTOM,
        COPPER_1_18_2
    }

    public interface Resolver {
        ItemStack resolve(StackReference reference);
    }

    private static final Resolver FORGE_RESOLVER = reference -> {
        Item item = ForgeRegistries.ITEMS.getValue(reference.itemId);
        return item == null ? ItemStack.EMPTY
                : new ItemStack(item, 1, reference.metadata);
    };

    private static final List<Definition> DEFINITIONS = createDefinitions();

    private StonecutterRecipeCatalog() {
    }

    public static List<Definition> definitions() {
        return DEFINITIONS;
    }

    public static List<ResolvedRecipe> recipesFor(ItemStack input) {
        return recipesFor(input, FORGE_RESOLVER);
    }

    static List<ResolvedRecipe> recipesFor(ItemStack input, Resolver resolver) {
        if (input.isEmpty()) {
            return Collections.emptyList();
        }
        List<ResolvedRecipe> matches = new ArrayList<>();
        for (Definition definition : DEFINITIONS) {
            if (!definition.input.matches(input)) {
                continue;
            }
            ItemStack result = resolver.resolve(definition.result);
            if (result == null || result.isEmpty()) {
                continue;
            }
            result.setCount(definition.count);
            matches.add(new ResolvedRecipe(definition, result));
        }
        // RecipeManager#getRecipesFor performs this exact result-description sort
        // before the 1.18.2 menu receives its list.
        matches.sort(Comparator.comparing(recipe ->
                recipe.definition.resultDescriptionId));
        return Collections.unmodifiableList(matches);
    }

    public static boolean hasRecipe(ItemStack input) {
        return !recipesFor(input).isEmpty();
    }

    private static List<Definition> createDefinitions() {
        List<Definition> recipes = new ArrayList<>();

        addLegacyVanillaRecipes(recipes);
        addDeepslateRecipes(recipes);
        addShapeRecipes(recipes, Family.TUFF_1_21, "tuff");
        addShapeRecipes(recipes, Family.CALCITE_CUSTOM, "calcite");
        addCopperRecipes(recipes);

        Set<ResourceLocation> ids = new LinkedHashSet<>();
        for (Definition definition : recipes) {
            if (!ids.add(definition.id)) {
                throw new IllegalStateException("Duplicate stonecutting recipe " + definition.id);
            }
        }
        return Collections.unmodifiableList(recipes);
    }

    /** Recipes whose exact 1.18 block peers exist in the 1.12 registry. */
    private static void addLegacyVanillaRecipes(List<Definition> recipes) {
        Family family = Family.VANILLA_1_12_ADAPTER;
        StackReference stone = vanilla("stone", 0);
        StackReference stoneBricks = vanilla("stonebrick", 0);
        // The legacy metadata-zero slab became smooth_stone_slab during flattening,
        // not the distinct 1.18 stone_slab, so there is no faithful result to expose.
        add(recipes, family, "stone_bricks_from_stone", stone, stoneBricks, 1);
        add(recipes, family, "stone_brick_slab_from_stone", stone,
                vanilla("stone_slab", 5), 2);
        add(recipes, family, "stone_brick_stairs_from_stone", stone,
                vanilla("stone_brick_stairs"), 1);
        add(recipes, family, "chiseled_stone_bricks_from_stone", stone,
                vanilla("stonebrick", 3), 1);

        StackReference sandstone = vanilla("sandstone", 0);
        StackReference cutSandstone = vanilla("sandstone", 2);
        add(recipes, family, "cut_sandstone_from_sandstone", sandstone,
                cutSandstone, 1);
        add(recipes, family, "sandstone_slab_from_sandstone", sandstone,
                vanilla("stone_slab", 1), 2);
        add(recipes, family, "sandstone_stairs_from_sandstone", sandstone,
                vanilla("sandstone_stairs"), 1);
        add(recipes, family, "chiseled_sandstone_from_sandstone", sandstone,
                vanilla("sandstone", 1), 1);

        StackReference redSandstone = vanilla("red_sandstone", 0);
        add(recipes, family, "cut_red_sandstone_from_red_sandstone", redSandstone,
                vanilla("red_sandstone", 2), 1);
        add(recipes, family, "red_sandstone_slab_from_red_sandstone", redSandstone,
                vanilla("stone_slab2", 0), 2);
        add(recipes, family, "red_sandstone_stairs_from_red_sandstone", redSandstone,
                vanilla("red_sandstone_stairs"), 1);
        add(recipes, family, "chiseled_red_sandstone_from_red_sandstone", redSandstone,
                vanilla("red_sandstone", 1), 1);

        StackReference quartz = vanilla("quartz_block", 0);
        add(recipes, family, "quartz_slab_from_quartz_block", quartz,
                vanilla("stone_slab", 7), 2);
        add(recipes, family, "quartz_stairs_from_quartz_block", quartz,
                vanilla("quartz_stairs"), 1);
        add(recipes, family, "quartz_pillar_from_quartz_block", quartz,
                vanilla("quartz_block", 2), 1);
        add(recipes, family, "chiseled_quartz_block_from_quartz_block", quartz,
                vanilla("quartz_block", 1), 1);

        StackReference cobblestone = vanilla("cobblestone");
        add(recipes, family, "cobblestone_stairs_from_cobblestone", cobblestone,
                vanilla("stone_stairs"), 1);
        add(recipes, family, "cobblestone_slab_from_cobblestone", cobblestone,
                vanilla("stone_slab", 3), 2);
        add(recipes, family, "cobblestone_wall_from_cobblestone", cobblestone,
                vanilla("cobblestone_wall", 0), 1);

        add(recipes, family, "stone_brick_slab_from_stone_bricks", stoneBricks,
                vanilla("stone_slab", 5), 2);
        add(recipes, family, "stone_brick_stairs_from_stone_bricks", stoneBricks,
                vanilla("stone_brick_stairs"), 1);
        add(recipes, family, "chiseled_stone_bricks_from_stone_bricks", stoneBricks,
                vanilla("stonebrick", 3), 1);

        StackReference bricks = vanilla("brick_block");
        add(recipes, family, "brick_slab_from_bricks", bricks,
                vanilla("stone_slab", 4), 2);
        add(recipes, family, "brick_stairs_from_bricks", bricks,
                vanilla("brick_stairs"), 1);

        StackReference netherBricks = vanilla("nether_brick");
        add(recipes, family, "nether_brick_slab_from_nether_bricks", netherBricks,
                vanilla("stone_slab", 6), 2);
        add(recipes, family, "nether_brick_stairs_from_nether_bricks", netherBricks,
                vanilla("nether_brick_stairs"), 1);

        StackReference purpur = vanilla("purpur_block");
        add(recipes, family, "purpur_slab_from_purpur_block", purpur,
                vanilla("purpur_slab"), 2);
        add(recipes, family, "purpur_stairs_from_purpur_block", purpur,
                vanilla("purpur_stairs"), 1);
        add(recipes, family, "purpur_pillar_from_purpur_block", purpur,
                vanilla("purpur_pillar"), 1);

        StackReference legacyStone = vanilla("stone", 1);
        add(recipes, family, "polished_granite_from_granite", legacyStone,
                vanilla("stone", 2), 1);
        add(recipes, family, "polished_diorite_from_diorite", vanilla("stone", 3),
                vanilla("stone", 4), 1);
        add(recipes, family, "polished_andesite_from_andesite", vanilla("stone", 5),
                vanilla("stone", 6), 1);

        add(recipes, family, "mossy_cobblestone_wall_from_mossy_cobblestone",
                vanilla("mossy_cobblestone"), vanilla("cobblestone_wall", 1), 1);
        add(recipes, family, "end_stone_bricks_from_end_stone",
                vanilla("end_stone"), vanilla("end_bricks"), 1);
    }

    private static void addDeepslateRecipes(List<Definition> recipes) {
        Family family = Family.DEEPSLATE_1_18_2;
        String cobbled = "cobbled_deepslate";
        addMod(recipes, family, cobbled, "cobbled_deepslate_slab", 2);
        addMod(recipes, family, cobbled, "cobbled_deepslate_stairs", 1);
        addMod(recipes, family, cobbled, "cobbled_deepslate_wall", 1);
        addMod(recipes, family, cobbled, "chiseled_deepslate", 1);
        addMod(recipes, family, cobbled, "polished_deepslate", 1);
        addMod(recipes, family, cobbled, "polished_deepslate_slab", 2);
        addMod(recipes, family, cobbled, "polished_deepslate_stairs", 1);
        addMod(recipes, family, cobbled, "polished_deepslate_wall", 1);
        addMod(recipes, family, cobbled, "deepslate_bricks", 1);
        addMod(recipes, family, cobbled, "deepslate_brick_slab", 2);
        addMod(recipes, family, cobbled, "deepslate_brick_stairs", 1);
        addMod(recipes, family, cobbled, "deepslate_brick_wall", 1);
        addMod(recipes, family, cobbled, "deepslate_tiles", 1);
        addMod(recipes, family, cobbled, "deepslate_tile_slab", 2);
        addMod(recipes, family, cobbled, "deepslate_tile_stairs", 1);
        addMod(recipes, family, cobbled, "deepslate_tile_wall", 1);

        String polished = "polished_deepslate";
        addMod(recipes, family, polished, "polished_deepslate_slab", 2);
        addMod(recipes, family, polished, "polished_deepslate_stairs", 1);
        addMod(recipes, family, polished, "polished_deepslate_wall", 1);
        addMod(recipes, family, polished, "deepslate_bricks", 1);
        addMod(recipes, family, polished, "deepslate_brick_slab", 2);
        addMod(recipes, family, polished, "deepslate_brick_stairs", 1);
        addMod(recipes, family, polished, "deepslate_brick_wall", 1);
        addMod(recipes, family, polished, "deepslate_tiles", 1);
        addMod(recipes, family, polished, "deepslate_tile_slab", 2);
        addMod(recipes, family, polished, "deepslate_tile_stairs", 1);
        addMod(recipes, family, polished, "deepslate_tile_wall", 1);

        String bricks = "deepslate_bricks";
        addMod(recipes, family, bricks, "deepslate_brick_slab", 2);
        addMod(recipes, family, bricks, "deepslate_brick_stairs", 1);
        addMod(recipes, family, bricks, "deepslate_brick_wall", 1);
        addMod(recipes, family, bricks, "deepslate_tiles", 1);
        addMod(recipes, family, bricks, "deepslate_tile_slab", 2);
        addMod(recipes, family, bricks, "deepslate_tile_stairs", 1);
        addMod(recipes, family, bricks, "deepslate_tile_wall", 1);

        String tiles = "deepslate_tiles";
        addMod(recipes, family, tiles, "deepslate_tile_slab", 2);
        addMod(recipes, family, tiles, "deepslate_tile_stairs", 1);
        addMod(recipes, family, tiles, "deepslate_tile_wall", 1);
    }

    private static void addShapeRecipes(List<Definition> recipes, Family family,
            String base) {
        addMod(recipes, family, base, base + "_slab", 2);
        addMod(recipes, family, base, base + "_stairs", 1);
        addMod(recipes, family, base, base + "_wall", 1);
    }

    private static void addCopperRecipes(List<Definition> recipes) {
        addCopperStage(recipes, "copper_block", "cut_copper");
        addCopperStage(recipes, "exposed_copper", "exposed_cut_copper");
        addCopperStage(recipes, "weathered_copper", "weathered_cut_copper");
        addCopperStage(recipes, "oxidized_copper", "oxidized_cut_copper");
        addCopperStage(recipes, "waxed_copper_block", "waxed_cut_copper");
        addCopperStage(recipes, "waxed_exposed_copper", "waxed_exposed_cut_copper");
        addCopperStage(recipes, "waxed_weathered_copper", "waxed_weathered_cut_copper");
        addCopperStage(recipes, "waxed_oxidized_copper", "waxed_oxidized_cut_copper");
    }

    private static void addCopperStage(List<Definition> recipes, String base, String cut) {
        Family family = Family.COPPER_1_18_2;
        addMod(recipes, family, cut, cut + "_slab", 2);
        addMod(recipes, family, cut, cut + "_stairs", 1);
        addMod(recipes, family, base, cut, 4);
        addMod(recipes, family, base, cut + "_stairs", 4);
        addMod(recipes, family, base, cut + "_slab", 8);
    }

    private static void addMod(List<Definition> recipes, Family family,
            String input, String output, int count) {
        add(recipes, family, output + "_from_" + input,
                mod(input), mod(output), count);
    }

    private static void add(List<Definition> recipes, Family family, String name,
            StackReference input, StackReference result, int count) {
        int separator = name.indexOf("_from_");
        String resultPath = separator < 0 ? result.itemId.getResourcePath()
                : name.substring(0, separator);
        recipes.add(new Definition(new ResourceLocation(CavesNotCliffs.MODID,
                "stonecutting/" + name), family, input, result,
                "block.minecraft." + resultPath, count));
    }

    private static StackReference vanilla(String path) {
        return vanilla(path, 0);
    }

    private static StackReference vanilla(String path, int metadata) {
        return new StackReference(new ResourceLocation("minecraft", path), metadata);
    }

    private static StackReference mod(String path) {
        return new StackReference(new ResourceLocation(CavesNotCliffs.MODID, path), 0);
    }

    public static final class StackReference {
        public final ResourceLocation itemId;
        public final int metadata;

        public StackReference(ResourceLocation itemId, int metadata) {
            if (itemId == null || metadata < 0) {
                throw new IllegalArgumentException("Invalid stonecutting stack reference");
            }
            this.itemId = itemId;
            this.metadata = metadata;
        }

        public boolean matches(ItemStack stack) {
            return !stack.isEmpty() && stack.getItem().getRegistryName() != null
                    && itemId.equals(stack.getItem().getRegistryName())
                    && metadata == stack.getMetadata();
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof StackReference)) {
                return false;
            }
            StackReference that = (StackReference) other;
            return itemId.equals(that.itemId) && metadata == that.metadata;
        }

        @Override
        public int hashCode() {
            return 31 * itemId.hashCode() + metadata;
        }

        @Override
        public String toString() {
            return itemId + "#" + metadata;
        }
    }

    public static final class Definition {
        public final ResourceLocation id;
        public final Family family;
        public final StackReference input;
        public final StackReference result;
        public final String resultDescriptionId;
        public final int count;

        private Definition(ResourceLocation id, Family family, StackReference input,
                StackReference result, String resultDescriptionId, int count) {
            if (count <= 0) {
                throw new IllegalArgumentException("Stonecutting count must be positive");
            }
            this.id = id;
            this.family = family;
            this.input = input;
            this.result = result;
            this.resultDescriptionId = resultDescriptionId;
            this.count = count;
        }
    }

    public static final class ResolvedRecipe {
        public final Definition definition;
        private final ItemStack result;

        private ResolvedRecipe(Definition definition, ItemStack result) {
            this.definition = definition;
            this.result = result.copy();
        }

        public ItemStack result() {
            return result.copy();
        }
    }
}

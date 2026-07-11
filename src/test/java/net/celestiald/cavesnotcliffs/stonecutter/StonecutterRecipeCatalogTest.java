package net.celestiald.cavesnotcliffs.stonecutter;

import net.celestiald.cavesnotcliffs.stonecutter.StonecutterRecipeCatalog.Definition;
import net.celestiald.cavesnotcliffs.stonecutter.StonecutterRecipeCatalog.Family;
import net.celestiald.cavesnotcliffs.stonecutter.StonecutterRecipeCatalog.ResolvedRecipe;
import net.celestiald.cavesnotcliffs.stonecutter.StonecutterRecipeCatalog.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.ResourceLocation;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StonecutterRecipeCatalogTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void catalogIsCompleteUniqueAndDeterministicallyPartitioned() {
        List<Definition> definitions = StonecutterRecipeCatalog.definitions();
        assertEquals(117, definitions.size());

        Map<Family, Integer> counts = new EnumMap<>(Family.class);
        Set<ResourceLocation> ids = new HashSet<>();
        for (Definition definition : definitions) {
            counts.put(definition.family, counts.getOrDefault(definition.family, 0) + 1);
            assertTrue(definition.id.toString(), ids.add(definition.id));
            assertTrue(definition.count > 0);
        }
        assertEquals(Integer.valueOf(34), counts.get(Family.VANILLA_1_12_ADAPTER));
        assertEquals(Integer.valueOf(37), counts.get(Family.DEEPSLATE_1_18_2));
        assertEquals(Integer.valueOf(3), counts.get(Family.TUFF_1_21));
        assertEquals(Integer.valueOf(3), counts.get(Family.CALCITE_CUSTOM));
        assertEquals(Integer.valueOf(40), counts.get(Family.COPPER_1_18_2));

        assertEquals("stonecutting/stone_bricks_from_stone",
                definitions.get(0).id.getResourcePath());
        assertEquals(Family.DEEPSLATE_1_18_2, definitions.get(34).family);
        assertEquals(Family.TUFF_1_21, definitions.get(71).family);
        assertEquals(Family.CALCITE_CUSTOM, definitions.get(74).family);
        assertEquals(Family.COPPER_1_18_2, definitions.get(77).family);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void publicDefinitionListCannotBeMutated() {
        StonecutterRecipeCatalog.definitions().clear();
    }

    @Test
    public void exactDeepslateProgressionMatchesAllThirtySeven1182Recipes() {
        assertOutputs(Family.DEEPSLATE_1_18_2, "cobbled_deepslate",
                "cobbled_deepslate_slab:2", "cobbled_deepslate_stairs:1",
                "cobbled_deepslate_wall:1", "chiseled_deepslate:1",
                "polished_deepslate:1", "polished_deepslate_slab:2",
                "polished_deepslate_stairs:1", "polished_deepslate_wall:1",
                "deepslate_bricks:1", "deepslate_brick_slab:2",
                "deepslate_brick_stairs:1", "deepslate_brick_wall:1",
                "deepslate_tiles:1", "deepslate_tile_slab:2",
                "deepslate_tile_stairs:1", "deepslate_tile_wall:1");
        assertOutputs(Family.DEEPSLATE_1_18_2, "polished_deepslate",
                "polished_deepslate_slab:2", "polished_deepslate_stairs:1",
                "polished_deepslate_wall:1", "deepslate_bricks:1",
                "deepslate_brick_slab:2", "deepslate_brick_stairs:1",
                "deepslate_brick_wall:1", "deepslate_tiles:1",
                "deepslate_tile_slab:2", "deepslate_tile_stairs:1",
                "deepslate_tile_wall:1");
        assertOutputs(Family.DEEPSLATE_1_18_2, "deepslate_bricks",
                "deepslate_brick_slab:2", "deepslate_brick_stairs:1",
                "deepslate_brick_wall:1", "deepslate_tiles:1",
                "deepslate_tile_slab:2", "deepslate_tile_stairs:1",
                "deepslate_tile_wall:1");
        assertOutputs(Family.DEEPSLATE_1_18_2, "deepslate_tiles",
                "deepslate_tile_slab:2", "deepslate_tile_stairs:1",
                "deepslate_tile_wall:1");
    }

    @Test
    public void tuffAndRetainedCalciteExtrasHaveStandardShapeYields() {
        assertOutputs(Family.TUFF_1_21, "tuff",
                "tuff_slab:2", "tuff_stairs:1", "tuff_wall:1");
        assertOutputs(Family.CALCITE_CUSTOM, "calcite",
                "calcite_slab:2", "calcite_stairs:1", "calcite_wall:1");
    }

    @Test
    public void everyCopperStageHasExactCutAndShapeYields() {
        String[][] stages = {
                {"copper_block", "cut_copper"},
                {"exposed_copper", "exposed_cut_copper"},
                {"weathered_copper", "weathered_cut_copper"},
                {"oxidized_copper", "oxidized_cut_copper"},
                {"waxed_copper_block", "waxed_cut_copper"},
                {"waxed_exposed_copper", "waxed_exposed_cut_copper"},
                {"waxed_weathered_copper", "waxed_weathered_cut_copper"},
                {"waxed_oxidized_copper", "waxed_oxidized_cut_copper"}
        };
        for (String[] stage : stages) {
            assertOutputs(Family.COPPER_1_18_2, stage[1],
                    stage[1] + "_slab:2", stage[1] + "_stairs:1");
            assertOutputs(Family.COPPER_1_18_2, stage[0],
                    stage[1] + ":4", stage[1] + "_stairs:4",
                    stage[1] + "_slab:8");
        }
    }

    @Test
    public void legacyAdapterPreservesExactFlattenedMetadataAndCounts() {
        String[] expected = {
                "stone#0>stonebrick#0=1", "stone#0>stone_slab#5=2",
                "stone#0>stone_brick_stairs#0=1", "stone#0>stonebrick#3=1",
                "sandstone#0>sandstone#2=1", "sandstone#0>stone_slab#1=2",
                "sandstone#0>sandstone_stairs#0=1", "sandstone#0>sandstone#1=1",
                "red_sandstone#0>red_sandstone#2=1",
                "red_sandstone#0>stone_slab2#0=2",
                "red_sandstone#0>red_sandstone_stairs#0=1",
                "red_sandstone#0>red_sandstone#1=1",
                "quartz_block#0>stone_slab#7=2", "quartz_block#0>quartz_stairs#0=1",
                "quartz_block#0>quartz_block#2=1", "quartz_block#0>quartz_block#1=1",
                "cobblestone#0>stone_stairs#0=1", "cobblestone#0>stone_slab#3=2",
                "cobblestone#0>cobblestone_wall#0=1",
                "stonebrick#0>stone_slab#5=2", "stonebrick#0>stone_brick_stairs#0=1",
                "stonebrick#0>stonebrick#3=1", "brick_block#0>stone_slab#4=2",
                "brick_block#0>brick_stairs#0=1", "nether_brick#0>stone_slab#6=2",
                "nether_brick#0>nether_brick_stairs#0=1",
                "purpur_block#0>purpur_slab#0=2", "purpur_block#0>purpur_stairs#0=1",
                "purpur_block#0>purpur_pillar#0=1", "stone#1>stone#2=1",
                "stone#3>stone#4=1", "stone#5>stone#6=1",
                "mossy_cobblestone#0>cobblestone_wall#1=1",
                "end_stone#0>end_bricks#0=1"
        };
        List<String> actual = new ArrayList<>();
        for (Definition definition : StonecutterRecipeCatalog.definitions()) {
            if (definition.family == Family.VANILLA_1_12_ADAPTER) {
                actual.add(shortRef(definition.input) + ">" + shortRef(definition.result)
                        + "=" + definition.count);
            }
        }
        assertEquals(java.util.Arrays.asList(expected), actual);
    }

    @Test
    public void runtimeResolutionIsMetadataExactSkipsMissingOutputsAndIsImmutable() {
        Item inputItem = new Item().setRegistryName("cavesnotcliffs", "cobbled_deepslate");
        ItemStack input = new ItemStack(inputItem, 5, 0);
        Map<StackReference, Item> resolved = new HashMap<>();
        StonecutterRecipeCatalog.Resolver resolver = reference -> {
            if (reference.itemId.getResourcePath().equals("deepslate_tile_wall")) {
                return ItemStack.EMPTY;
            }
            Item item = resolved.computeIfAbsent(reference,
                    key -> new Item().setRegistryName(key.itemId));
            return new ItemStack(item, 1, reference.metadata);
        };

        List<ResolvedRecipe> recipes = StonecutterRecipeCatalog.recipesFor(input, resolver);
        assertEquals(15, recipes.size());
        assertEquals("chiseled_deepslate",
                recipes.get(0).definition.result.itemId.getResourcePath());
        assertEquals(1, recipes.get(0).result().getCount());
        ItemStack copy = recipes.get(0).result();
        copy.setCount(64);
        assertEquals(1, recipes.get(0).result().getCount());

        String previous = "";
        for (ResolvedRecipe recipe : recipes) {
            assertTrue(previous.compareTo(recipe.definition.resultDescriptionId) <= 0);
            previous = recipe.definition.resultDescriptionId;
        }

        assertTrue(StonecutterRecipeCatalog.recipesFor(
                new ItemStack(inputItem, 1, 1), resolver).isEmpty());
        assertFalse(recipes.isEmpty());
        try {
            recipes.clear();
            throw new AssertionError("Resolved recipe list was mutable");
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }
    }

    private static void assertOutputs(Family family, String input, String... expected) {
        List<String> actual = new ArrayList<>();
        for (Definition definition : StonecutterRecipeCatalog.definitions()) {
            if (definition.family == family
                    && definition.input.itemId.getResourcePath().equals(input)) {
                actual.add(definition.result.itemId.getResourcePath() + ":"
                        + definition.count);
            }
        }
        assertEquals(input, java.util.Arrays.asList(expected), actual);
    }

    private static String shortRef(StackReference reference) {
        return reference.itemId.getResourcePath() + "#" + reference.metadata;
    }
}

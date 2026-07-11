package net.celestiald.cavesnotcliffs.content;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MaterialRecipeTest {
    private static final String[] SHAPES = {
            "cobbled_deepslate", "polished_deepslate",
            "deepslate_brick", "deepslate_tile"
    };

    @Test
    public void deepslateShapeRecipesUseExactVanillaCounts() {
        for (String shape : SHAPES) {
            assertResult(shape + "_slab", shape + "_slab", 6);
            assertResult(shape + "_stairs", shape + "_stairs", 4);
            assertResult(shape + "_wall", shape + "_wall", 6);
        }
    }

    @Test
    public void deepslateProgressionAndChiseledRecipesMatch1182() {
        assertResult("polished_deepslate", "polished_deepslate", 4);
        assertResult("deepslate_bricks", "deepslate_bricks", 4);
        assertResult("deepslate_tiles", "deepslate_tiles", 4);
        assertResult("chiseled_deepslate", "chiseled_deepslate", 1);
    }

    @Test
    public void rawMaterialCompressionRoundTripsNineToOne() {
        for (String material : new String[]{"raw_copper", "raw_iron", "raw_gold"}) {
            assertResult(material + "_block", material + "_block", 1);
            assertResult(material, material, 9);
        }
    }

    @Test
    public void everyNewPublicObjectHasItsModelOrTextureResource() {
        ClassLoader loader = MaterialRecipeTest.class.getClassLoader();
        for (String item : new String[]{
                "raw_copper", "copper_ingot", "raw_iron", "raw_gold", "amethyst_shard"}) {
            assertNotNull(item, loader.getResource(
                    "assets/cavesnotcliffs/models/item/" + item + ".json"));
            assertNotNull(item, loader.getResource(
                    "assets/cavesnotcliffs/textures/items/" + item + ".png"));
        }
        for (String block : new String[]{
                "cobbled_deepslate", "polished_deepslate", "deepslate_bricks",
                "cracked_deepslate_bricks", "deepslate_tiles", "cracked_deepslate_tiles",
                "chiseled_deepslate", "raw_copper_block", "raw_iron_block", "raw_gold_block",
                "deepslate_copper_ore"}) {
            assertNotNull(block, loader.getResource(
                    "assets/cavesnotcliffs/blockstates/" + block + ".json"));
            assertNotNull(block, loader.getResource(
                    "assets/cavesnotcliffs/textures/blocks/" + block + ".png"));
        }
    }

    @Test
    public void everyMaterialItemModelResolvesItsCompleteParentAndTextureTree() {
        Set<String> models = new HashSet<>();
        for (String item : new String[]{
                "raw_copper", "copper_ingot", "raw_iron", "raw_gold", "amethyst_shard",
                "deepslate", "cobbled_deepslate", "polished_deepslate", "deepslate_bricks",
                "cracked_deepslate_bricks", "deepslate_tiles", "cracked_deepslate_tiles",
                "chiseled_deepslate", "raw_copper_block", "raw_iron_block", "raw_gold_block",
                "deepslate_copper_ore", "cobbled_deepslate_slab",
                "cobbled_deepslate_stairs", "cobbled_deepslate_wall",
                "polished_deepslate_slab", "polished_deepslate_stairs",
                "polished_deepslate_wall", "deepslate_brick_slab",
                "deepslate_brick_stairs", "deepslate_brick_wall", "deepslate_tile_slab",
                "deepslate_tile_stairs", "deepslate_tile_wall"}) {
            assertModelTree("item/" + item, models);
        }
    }

    private static void assertResult(String recipeName, String expectedPath, int expectedCount) {
        JsonObject result = recipe(recipeName).getAsJsonObject("result");
        assertEquals("cavesnotcliffs:" + expectedPath, result.get("item").getAsString());
        assertEquals(expectedCount, result.has("count") ? result.get("count").getAsInt() : 1);
    }

    private static JsonObject recipe(String name) {
        String path = "assets/cavesnotcliffs/recipes/" + name + ".json";
        InputStream stream = MaterialRecipeTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(path, stream);
        return new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8))
                .getAsJsonObject();
    }

    private static void assertModelTree(String model, Set<String> visited) {
        if (!visited.add(model)) {
            return;
        }
        String path = "assets/cavesnotcliffs/models/" + model + ".json";
        InputStream stream = MaterialRecipeTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(path, stream);
        JsonObject json = new JsonParser().parse(
                new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();

        if (json.has("parent")) {
            String parent = json.get("parent").getAsString();
            String prefix = "cavesnotcliffs:";
            if (parent.startsWith(prefix)) {
                assertModelTree(parent.substring(prefix.length()), visited);
            }
        }

        if (json.has("textures")) {
            for (Map.Entry<String, com.google.gson.JsonElement> texture
                    : json.getAsJsonObject("textures").entrySet()) {
                String value = texture.getValue().getAsString();
                if (value.startsWith("cavesnotcliffs:") && !value.startsWith("#")) {
                    String texturePath = "assets/cavesnotcliffs/textures/"
                            + value.substring("cavesnotcliffs:".length()) + ".png";
                    assertNotNull(texturePath, MaterialRecipeTest.class.getClassLoader()
                            .getResource(texturePath));
                }
            }
        }
    }
}

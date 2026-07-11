package net.celestiald.cavesnotcliffs.content;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CopperResourceMatrixTest {
    private static final String ROOT = "assets/cavesnotcliffs/";

    @Test
    public void everyCopperStateHasCompleteCanonicalResources() {
        for (CopperWeathering.Variant variant : CopperWeathering.variants()) {
            String path = variant.getPath();
            JsonObject state = json("blockstates/" + path + ".json");
            JsonObject variants = state.getAsJsonObject("variants");
            assertNotNull(path, variants);
            switch (variant.getShape()) {
                case BLOCK:
                case CUT:
                case DOUBLE_SLAB:
                    assertEquals(path, 1, variants.size());
                    break;
                case STAIRS:
                    assertEquals(path, 40, variants.size());
                    assertTrue(path, variants.has("facing=north,half=top,shape=outer_left"));
                    break;
                case SLAB:
                    assertEquals(path, 2, variants.size());
                    assertTrue(path, variants.has("half=bottom,variant=default"));
                    assertTrue(path, variants.has("half=top,variant=default"));
                    break;
                default:
                    throw new AssertionError(variant.getShape());
            }

            assertResource("models/block/" + path + ".json");
            if (variant.getShape() == CopperWeathering.Shape.STAIRS) {
                assertResource("models/block/" + path + "_inner.json");
                assertResource("models/block/" + path + "_outer.json");
            } else if (variant.getShape() == CopperWeathering.Shape.SLAB) {
                assertResource("models/block/" + path + "_top.json");
            }

            if (variant.hasPublicItem()) {
                assertResource("models/item/" + path + ".json");
            } else {
                assertFalse("Hidden double slab gained an item model: " + path,
                        exists("models/item/" + path + ".json"));
            }
        }
    }

    @Test
    public void canonicalCraftingMatrixUsesExactVanillaCounts() {
        for (boolean waxed : new boolean[]{false, true}) {
            for (CopperWeathering.Stage stage : CopperWeathering.Stage.values()) {
                String full = CopperWeathering.path(stage,
                        CopperWeathering.Shape.BLOCK, waxed);
                String cut = CopperWeathering.path(stage,
                        CopperWeathering.Shape.CUT, waxed);
                String stairs = CopperWeathering.path(stage,
                        CopperWeathering.Shape.STAIRS, waxed);
                String slab = CopperWeathering.path(stage,
                        CopperWeathering.Shape.SLAB, waxed);
                assertRecipe(cut, full, cut, 4, "##", "##");
                assertRecipe(stairs, cut, stairs, 4, "#  ", "## ", "###");
                assertRecipe(slab, cut, slab, 6, "###");
            }
        }
        assertRecipe("copper_block", "copper_ingot", "copper_block", 1,
                "###", "###", "###");
        JsonObject ingot = json("recipes/copper_ingot.json");
        assertEquals("minecraft:crafting_shapeless", ingot.get("type").getAsString());
        assertEquals("cavesnotcliffs:copper_block", ingot.getAsJsonArray("ingredients")
                .get(0).getAsJsonObject().get("item").getAsString());
        assertEquals("cavesnotcliffs:copper_ingot", ingot.getAsJsonObject("result")
                .get("item").getAsString());
        assertEquals(9, ingot.getAsJsonObject("result").get("count").getAsInt());
    }

    @Test
    public void copiedTextureBytesMatchTheOfficial1182Client() throws Exception {
        Map<String, String> hashes = new LinkedHashMap<>();
        hashes.put("copper_block", "f2a4e847dbf7ffee41cda7a1695661e637fc11e53b0d7a28d7cdee76fb41981d");
        hashes.put("exposed_copper", "f4fcba201af0f8d979c53e681247419117ae51076c1717948eb6a166715fdb55");
        hashes.put("weathered_copper", "a6c9862236c513902067390fce49f811af6075583e1c3d45e179bff6f6dc0462");
        hashes.put("oxidized_copper", "17eed8682317770fd3f2985873eeae17506f38274f5eace9a42e86c7c5eee06c");
        hashes.put("cut_copper", "3fed363d2ddf1f7ed3405e20c2efcc70763c065307252645b9dbdbda12cc32dc");
        hashes.put("exposed_cut_copper", "b979186ba645265ceff3dd7f4b67c8b59fb9d82272a73c7fa60716a07d093145");
        hashes.put("weathered_cut_copper", "e0bdcd3bb9ec483ebcf17184df6d1366ff89ec0c287749a0e71de931c2ef175d");
        hashes.put("oxidized_cut_copper", "ea5fb99d461914caad9a832c460f94c102ce8d66528e9049e3ad9b3fd31009b0");
        for (Map.Entry<String, String> entry : hashes.entrySet()) {
            assertEquals(entry.getKey(), entry.getValue(),
                    sha256("textures/blocks/" + entry.getKey() + ".png"));
        }
    }

    @Test
    public void soundDefinitionsAndEveryReferencedOggArePresent() {
        JsonObject sounds = json("sounds.json");
        for (String event : new String[]{
                "block.copper.break", "block.copper.fall", "block.copper.hit",
                "block.copper.place", "block.copper.step", "item.axe.scrape",
                "item.axe.wax_off", "item.trident.thunder"}) {
            assertTrue(event, sounds.has(event));
            sounds.getAsJsonObject(event).getAsJsonArray("sounds").forEach(element -> {
                String name = element.isJsonPrimitive() ? element.getAsString()
                        : element.getAsJsonObject().get("name").getAsString();
                assertTrue(name, name.startsWith("cavesnotcliffs:"));
                assertResource("sounds/" + name.substring("cavesnotcliffs:".length()) + ".ogg");
            });
        }
    }

    private static void assertRecipe(String recipe, String ingredient, String result,
            int count, String... pattern) {
        JsonObject json = json("recipes/" + recipe + ".json");
        assertEquals("minecraft:crafting_shaped", json.get("type").getAsString());
        assertEquals("cavesnotcliffs:" + ingredient,
                json.getAsJsonObject("key").getAsJsonObject("#").get("item").getAsString());
        assertEquals("cavesnotcliffs:" + result,
                json.getAsJsonObject("result").get("item").getAsString());
        assertEquals(count, json.getAsJsonObject("result").has("count")
                ? json.getAsJsonObject("result").get("count").getAsInt() : 1);
        assertEquals(pattern.length, json.getAsJsonArray("pattern").size());
        for (int i = 0; i < pattern.length; i++) {
            assertEquals(pattern[i], json.getAsJsonArray("pattern").get(i).getAsString());
        }
    }

    private static String sha256(String path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        InputStream stream = resource(path);
        byte[] buffer = new byte[4096];
        int read;
        while ((read = stream.read(buffer)) >= 0) {
            digest.update(buffer, 0, read);
        }
        stream.close();
        StringBuilder result = new StringBuilder();
        for (byte value : digest.digest()) {
            result.append(String.format("%02x", value & 0xFF));
        }
        return result.toString();
    }

    private static JsonObject json(String path) {
        return new JsonParser().parse(new InputStreamReader(resource(path),
                StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static boolean exists(String path) {
        return CopperResourceMatrixTest.class.getClassLoader().getResource(ROOT + path) != null;
    }

    private static void assertResource(String path) {
        assertNotNull(path, CopperResourceMatrixTest.class.getClassLoader().getResource(ROOT + path));
    }

    private static InputStream resource(String path) {
        InputStream stream = CopperResourceMatrixTest.class.getClassLoader()
                .getResourceAsStream(ROOT + path);
        assertNotNull(path, stream);
        return stream;
    }
}

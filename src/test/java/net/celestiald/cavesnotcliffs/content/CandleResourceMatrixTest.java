package net.celestiald.cavesnotcliffs.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CandleResourceMatrixTest {
    private static final String ROOT = "assets/cavesnotcliffs/";

    @Test
    public void everyCandleAndCakeStateResolvesCanonicalModels() {
        Set<String> blockModels = new HashSet<>();
        for (CandleMechanics.Color color : CandleMechanics.colors()) {
            String candle = color.getCandlePath();
            JsonArray multipart = json("blockstates/" + candle + ".json")
                    .getAsJsonArray("multipart");
            assertEquals(candle, 9, multipart.size());
            Set<String> renderedStates = new HashSet<>();
            boolean waterOverlay = false;
            for (JsonElement element : multipart) {
                JsonObject part = element.getAsJsonObject();
                JsonObject when = part.getAsJsonObject("when");
                String model = part.getAsJsonObject("apply").get("model").getAsString();
                if (when.has("waterlogged")) {
                    assertEquals("true", when.get("waterlogged").getAsString());
                    assertEquals("cavesnotcliffs:amethyst_water_overlay", model);
                    waterOverlay = true;
                } else {
                    renderedStates.add("candles=" + when.get("candles").getAsString()
                            + ",lit=" + when.get("lit").getAsString());
                    assertModel(model, blockModels);
                }
            }
            assertTrue(candle, waterOverlay);
            for (int count = 1; count <= 4; count++) {
                for (boolean lit : new boolean[]{false, true}) {
                    String key = "candles=" + count + ",lit=" + lit;
                    assertTrue(candle + " " + key, renderedStates.contains(key));
                }
            }

            String cake = color.getCandleCakePath();
            JsonObject cakeVariants = json("blockstates/" + cake + ".json")
                    .getAsJsonObject("variants");
            assertEquals(cake, 2, cakeVariants.size());
            for (boolean lit : new boolean[]{false, true}) {
                assertModel(cakeVariants.getAsJsonObject("lit=" + lit)
                        .get("model").getAsString(), blockModels);
            }

            JsonObject item = json("models/item/" + candle + ".json");
            assertEquals("minecraft:item/generated", item.get("parent").getAsString());
            assertEquals("cavesnotcliffs:items/" + candle,
                    item.getAsJsonObject("textures").get("layer0").getAsString());
            assertResource("textures/items/" + candle + ".png");
            assertResource("textures/blocks/" + candle + ".png");
            assertResource("textures/blocks/" + candle + "_lit.png");
            assertFalse("Candle cakes must stay hidden block states: " + cake,
                    exists("models/item/" + cake + ".json"));
        }
        assertEquals(170, blockModels.size());
        for (String template : new String[]{"template_candle", "template_two_candles",
                "template_three_candles", "template_four_candles",
                "template_cake_with_candle"}) {
            json("models/block/" + template + ".json");
        }
    }

    @Test
    public void craftingUsesOneStringOneHoneycombAndExactLegacyDyeDamage() {
        JsonObject base = json("recipes/candle.json");
        assertEquals("minecraft:crafting_shaped", base.get("type").getAsString());
        assertEquals(2, base.getAsJsonArray("pattern").size());
        assertEquals("S", base.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("H", base.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals("minecraft:string", base.getAsJsonObject("key")
                .getAsJsonObject("S").get("item").getAsString());
        assertEquals("cavesnotcliffs:honeycomb", base.getAsJsonObject("key")
                .getAsJsonObject("H").get("item").getAsString());
        assertEquals("cavesnotcliffs:candle", base.getAsJsonObject("result")
                .get("item").getAsString());

        for (CandleMechanics.Color color : CandleMechanics.colors()) {
            if (!color.isDyed()) {
                continue;
            }
            JsonObject recipe = json("recipes/" + color.getCandlePath() + ".json");
            assertEquals("minecraft:crafting_shapeless",
                    recipe.get("type").getAsString());
            assertEquals("dyed_candle", recipe.get("group").getAsString());
            JsonArray ingredients = recipe.getAsJsonArray("ingredients");
            assertEquals(2, ingredients.size());
            assertEquals("cavesnotcliffs:candle", ingredients.get(0)
                    .getAsJsonObject().get("item").getAsString());
            assertEquals("minecraft:dye", ingredients.get(1)
                    .getAsJsonObject().get("item").getAsString());
            assertEquals(color.getDyeDamage(), ingredients.get(1)
                    .getAsJsonObject().get("data").getAsInt());
            assertEquals("cavesnotcliffs:" + color.getCandlePath(),
                    recipe.getAsJsonObject("result").get("item").getAsString());
            assertFalse(recipe.getAsJsonObject("result").has("count"));
        }
    }

    @Test
    public void soundDefinitionsPreserveExactEntryMatrixAndResolveEveryOgg() {
        JsonObject sounds = json("sounds.json");
        assertSoundEntries(sounds, "block.cake.add_candle", 3);
        assertSoundEntries(sounds, "block.candle.ambient", 27);
        assertSoundEntries(sounds, "block.candle.break", 5);
        assertSoundEntries(sounds, "block.candle.extinguish", 18);
        assertSoundEntries(sounds, "block.candle.fall", 5);
        assertSoundEntries(sounds, "block.candle.hit", 5);
        assertSoundEntries(sounds, "block.candle.place", 5);
        assertSoundEntries(sounds, "block.candle.step", 5);
    }

    @Test
    public void everyCopiedBinaryMatchesItsOfficialAssetHash() throws Exception {
        InputStream stream = resource("oracles/candle-assets.sha1");
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream,
                StandardCharsets.UTF_8));
        int count = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] parts = line.split("  ", 2);
            assertEquals(line, 2, parts.length);
            assertEquals(parts[1], parts[0], sha1(parts[1]));
            count++;
        }
        assertEquals(76, count);
    }

    private static void assertModel(String model, Set<String> models) {
        assertTrue(model, model.startsWith("cavesnotcliffs:block/"));
        String path = model.substring("cavesnotcliffs:block/".length());
        json("models/block/" + path + ".json");
        models.add(path);
    }

    private static void assertSoundEntries(JsonObject sounds, String event,
            int expected) {
        JsonArray entries = sounds.getAsJsonObject(event).getAsJsonArray("sounds");
        assertEquals(event, expected, entries.size());
        for (JsonElement entry : entries) {
            String name = entry.isJsonPrimitive() ? entry.getAsString()
                    : entry.getAsJsonObject().get("name").getAsString();
            assertTrue(name, name.startsWith("cavesnotcliffs:"));
            assertResource("sounds/" + name.substring("cavesnotcliffs:".length())
                    + ".ogg");
        }
    }

    private static JsonObject json(String path) {
        return new JsonParser().parse(new InputStreamReader(resource(ROOT + path),
                StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static boolean exists(String path) {
        return CandleResourceMatrixTest.class.getClassLoader()
                .getResource(ROOT + path) != null;
    }

    private static void assertResource(String path) {
        assertNotNull(path, CandleResourceMatrixTest.class.getClassLoader()
                .getResource(ROOT + path));
    }

    private static InputStream resource(String path) {
        InputStream stream = CandleResourceMatrixTest.class.getClassLoader()
                .getResourceAsStream(path);
        assertNotNull(path, stream);
        return stream;
    }

    private static String sha1(String path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        InputStream stream = resource(path);
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stream.read(buffer)) >= 0) {
            digest.update(buffer, 0, read);
        }
        stream.close();
        StringBuilder result = new StringBuilder();
        for (byte value : digest.digest()) {
            result.append(String.format("%02x", value & 255));
        }
        return result.toString();
    }
}

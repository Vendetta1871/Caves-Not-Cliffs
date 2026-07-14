package net.celestiald.cavesnotcliffs.stonecutter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StonecutterAssetsTest {
    @Test
    public void canonicalModelsGeometryRotationsGuiAndCraftingRecipeArePackaged()
            throws Exception {
        JsonObject states = json("assets/cavesnotcliffs/blockstates/stonecutter.json")
                .getAsJsonObject("variants");
        assertEquals(90, states.getAsJsonObject("facing=east").get("y").getAsInt());
        assertEquals(180, states.getAsJsonObject("facing=south").get("y").getAsInt());
        assertEquals(270, states.getAsJsonObject("facing=west").get("y").getAsInt());

        JsonObject model = json("assets/cavesnotcliffs/models/block/stonecutter.json");
        assertEquals("block/block", model.get("parent").getAsString());
        JsonArray elements = model.getAsJsonArray("elements");
        assertEquals(2, elements.size());
        assertEquals(9, elements.get(0).getAsJsonObject().getAsJsonArray("to")
                .get(1).getAsInt());
        assertEquals(8, elements.get(1).getAsJsonObject().getAsJsonArray("from")
                .get(2).getAsInt());
        assertEquals("cavesnotcliffs:block/stonecutter",
                json("assets/cavesnotcliffs/models/item/stonecutter.json")
                        .get("parent").getAsString());

        BufferedImage gui = ImageIO.read(resource(
                "assets/cavesnotcliffs/textures/gui/container/stonecutter.png"));
        assertEquals(256, gui.getWidth());
        assertEquals(256, gui.getHeight());

        JsonObject recipe = json("assets/cavesnotcliffs/recipes/stonecutter.json");
        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        assertEquals(" I ", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("###", recipe.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals("minecraft:iron_ingot", recipe.getAsJsonObject("key")
                .getAsJsonObject("I").get("item").getAsString());
        assertEquals("minecraft:stone", recipe.getAsJsonObject("key")
                .getAsJsonObject("#").get("item").getAsString());
        assertEquals(0, recipe.getAsJsonObject("key").getAsJsonObject("#")
                .get("data").getAsInt());
        assertEquals("cavesnotcliffs:stonecutter", recipe.getAsJsonObject("result")
                .get("item").getAsString());
    }

    @Test
    public void copiedMojangAssetsMatchTheOfficial1182AndAssetIndexHashes()
            throws Exception {
        Map<String, String> hashes = new LinkedHashMap<>();
        hashes.put("textures/blocks/stonecutter_bottom.png",
                "ae7a7b4a1617d20f2e57413f206e59667a3375a9");
        hashes.put("textures/blocks/stonecutter_saw.png",
                "6d58737868f9111b21814ca8a527c2248f7b21af");
        hashes.put("textures/blocks/stonecutter_saw.png.mcmeta",
                "be5a17f00fda2331a8dd8a674014def300099899");
        hashes.put("textures/blocks/stonecutter_side.png",
                "70bdcd148f1b14a22a453200c72c15065cf8f4ec");
        hashes.put("textures/blocks/stonecutter_top.png",
                "d93b3604ddb8e3f986aae17fb9ecc4cf2be25be7");
        hashes.put("textures/gui/container/stonecutter.png",
                "876d1271e6934749a2ba124b8e6d3ff918fa5dc6");
        hashes.put("sounds/ui/stonecutter/cut1.ogg",
                "f9c33914acfd606ea5c624f25d33cccc60663e22");
        hashes.put("sounds/ui/stonecutter/cut2.ogg",
                "34eec70d1a60aba94b92065274fd456a97b1e036");
        for (Map.Entry<String, String> entry : hashes.entrySet()) {
            assertEquals(entry.getKey(), entry.getValue(), sha1(read(
                    "assets/cavesnotcliffs/" + entry.getKey())));
        }
    }

    @Test
    public void exactSoundDefinitionsAndLanguageKeysAreWired() {
        JsonObject sounds = json("assets/cavesnotcliffs/sounds.json");
        JsonObject select = sounds.getAsJsonObject("ui.stonecutter.select_recipe");
        assertEquals("minecraft:random/click",
                select.getAsJsonArray("sounds").get(0).getAsString());
        JsonObject take = sounds.getAsJsonObject("ui.stonecutter.take_result");
        assertEquals(4, take.getAsJsonArray("sounds").size());
        assertEquals(0.92F, take.getAsJsonArray("sounds").get(1)
                .getAsJsonObject().get("pitch").getAsFloat(), 0.0F);
        assertEquals("subtitles.ui.stonecutter.take_result",
                take.get("subtitle").getAsString());

        String language = new String(readUnchecked(
                "assets/cavesnotcliffs/lang/en_us.lang"), StandardCharsets.UTF_8);
        assertTrue(language.contains("tile.stonecutter.name=Stonecutter"));
        assertTrue(language.contains("container.stonecutter=Stonecutter"));
        assertTrue(language.contains(
                "subtitles.ui.stonecutter.take_result=Stonecutter used"));
        assertTrue(language.contains(
                "stat.interactWithStonecutter=Interactions with Stonecutter"));
    }

    private static JsonObject json(String path) {
        return new JsonParser().parse(new InputStreamReader(resource(path),
                StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static InputStream resource(String path) {
        InputStream input = StonecutterAssetsTest.class.getClassLoader()
                .getResourceAsStream(path);
        assertNotNull(path, input);
        return input;
    }

    private static byte[] readUnchecked(String path) {
        try {
            return read(path);
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private static byte[] read(String path) throws IOException {
        InputStream input = resource(path);
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        } finally {
            input.close();
        }
    }

    private static String sha1(byte[] data) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-1").digest(data);
        StringBuilder value = new StringBuilder();
        for (byte part : digest) {
            value.append(String.format("%02x", part & 255));
        }
        return value.toString();
    }
}

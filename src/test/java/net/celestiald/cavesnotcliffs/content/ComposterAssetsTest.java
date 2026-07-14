package net.celestiald.cavesnotcliffs.content;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import org.junit.Test;

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

public class ComposterAssetsTest {
    @Test
    public void registryModelsRecipeAndAllNineLevelsArePackaged() {
        assertEquals("cavesnotcliffs:composter", CncRegistryIds.COMPOSTER.toString());
        resource("assets/cavesnotcliffs/blockstates/composter.json");
        resource("assets/cavesnotcliffs/models/block/composter.json");
        resource("assets/cavesnotcliffs/models/item/composter.json");
        resource("assets/cavesnotcliffs/recipes/composter.json");
        for (int level = 1; level <= 7; ++level) {
            JsonObject model = json("assets/cavesnotcliffs/models/block/composter_contents"
                + level + ".json");
            assertEquals(1 + level * 2, model.getAsJsonArray("elements").get(0)
                .getAsJsonObject().getAsJsonArray("to").get(1).getAsInt());
        }
        resource("assets/cavesnotcliffs/models/block/composter_contents_ready.json");

        JsonObject recipe = json("assets/cavesnotcliffs/recipes/composter.json");
        assertEquals("forge:ore_shaped", recipe.get("type").getAsString());
        assertEquals("# #", recipe.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("###", recipe.getAsJsonArray("pattern").get(2).getAsString());
        assertEquals("slabWood", recipe.getAsJsonObject("key").getAsJsonObject("#")
            .get("ore").getAsString());
    }

    @Test
    public void copiedMojangAssetsMatchOfficialHashes() throws Exception {
        Map<String, String> hashes = new LinkedHashMap<>();
        hashes.put("textures/blocks/composter_bottom.png", "f87b1c4ed70da45aada337b26bc294090d4a1fbc");
        hashes.put("textures/blocks/composter_compost.png", "ede35f400448b966fcc4f069a160ab31272227fa");
        hashes.put("textures/blocks/composter_ready.png", "4c6d2e62f18e3ca4146d7a11ea8cc6c5123364f5");
        hashes.put("textures/blocks/composter_side.png", "c7949a33ac8eee8fda23851bb92b3ee650fbf79f");
        hashes.put("textures/blocks/composter_top.png", "ab8a6ef23a0e51e990a3f3044f6ead1c681243fb");
        hashes.put("sounds/block/composter/empty1.ogg", "b97899f3c27ab46d7bab3edd0d1d73fecf5469e3");
        hashes.put("sounds/block/composter/empty2.ogg", "77109350e5cf7cfaa0cc1543294dde3d76324645");
        hashes.put("sounds/block/composter/empty3.ogg", "e76c482f23201967692b2b8442dd0a52aac48701");
        hashes.put("sounds/block/composter/fill1.ogg", "4b9469a8a151cba0d28f14d87723244d8de74950");
        hashes.put("sounds/block/composter/fill2.ogg", "8a930adbadfc8fe0168595302c03d3117dc60e0e");
        hashes.put("sounds/block/composter/fill3.ogg", "0c6bb010b1b50f592881a1f858d5db5f0a181f22");
        hashes.put("sounds/block/composter/fill4.ogg", "9afc4d2fafd6dc6d779dd8cbf4b23bb50c165dc4");
        hashes.put("sounds/block/composter/fill_success1.ogg", "4b736a9adb36eda550acc630b51f32c5ddbca76f");
        hashes.put("sounds/block/composter/fill_success2.ogg", "3aafe51655104d95dcf41b55de9731622db1845f");
        hashes.put("sounds/block/composter/fill_success3.ogg", "d82a2dfe9007742a19637fb82256c8b3bb2248ef");
        hashes.put("sounds/block/composter/fill_success4.ogg", "b21d524d15e9bf9007bb959ec13640033f06e939");
        hashes.put("sounds/block/composter/ready1.ogg", "feb0871002430ce84966f81c0a019a2c51be794f");
        hashes.put("sounds/block/composter/ready2.ogg", "9f0f729bb824c7220788403a331c2c90ae126f23");
        hashes.put("sounds/block/composter/ready3.ogg", "7017e094d989c29d8759d726382df34301c16a85");
        hashes.put("sounds/block/composter/ready4.ogg", "9c55aa1efbb74527aecb5009cd0e658d7978234c");

        for (Map.Entry<String, String> entry : hashes.entrySet()) {
            assertEquals(entry.getKey(), entry.getValue(),
                sha1(read("assets/cavesnotcliffs/" + entry.getKey())));
        }
    }

    @Test
    public void exactSoundEventsAndSubtitlesAreWired() {
        JsonObject sounds = json("assets/cavesnotcliffs/sounds.json");
        for (String event : new String[]{"block.composter.empty", "block.composter.fill",
                "block.composter.fill_success", "block.composter.ready"}) {
            assertNotNull(event, sounds.get(event));
        }
        assertEquals(3, sounds.getAsJsonObject("block.composter.empty")
            .getAsJsonArray("sounds").size());
        assertEquals(4, sounds.getAsJsonObject("block.composter.fill")
            .getAsJsonArray("sounds").size());
        assertEquals(0.8F, sounds.getAsJsonObject("block.composter.fill")
            .getAsJsonArray("sounds").get(0).getAsJsonObject()
            .get("pitch").getAsFloat(), 0.0F);
        assertEquals(0.3F, sounds.getAsJsonObject("block.composter.fill")
            .getAsJsonArray("sounds").get(0).getAsJsonObject()
            .get("volume").getAsFloat(), 0.0F);

        String language = new String(readUnchecked(
            "assets/cavesnotcliffs/lang/en_us.lang"), StandardCharsets.UTF_8);
        assertTrue(language.contains("tile.composter.name=Composter"));
        assertTrue(language.contains("subtitles.block.composter.ready=Composter ready"));
    }

    private static JsonObject json(String path) {
        InputStream stream = resource(path);
        return new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8))
            .getAsJsonObject();
    }

    private static InputStream resource(String path) {
        InputStream stream = ComposterAssetsTest.class.getClassLoader()
            .getResourceAsStream(path);
        assertNotNull(path, stream);
        return stream;
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

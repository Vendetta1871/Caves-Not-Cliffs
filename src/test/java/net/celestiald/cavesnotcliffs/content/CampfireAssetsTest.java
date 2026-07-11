package net.celestiald.cavesnotcliffs.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CampfireAssetsTest {
    private static final String ROOT = "assets/cavesnotcliffs/";

    @Test
    public void modelsPreserveOfficialGeometryAndLegacyPathSyntax() {
        JsonObject template = json("models/block/template_campfire.json");
        JsonObject off = json("models/block/campfire_off.json");
        assertEquals(7, template.getAsJsonArray("elements").size());
        assertEquals(5, off.getAsJsonArray("elements").size());
        assertEquals("cavesnotcliffs:blocks/campfire_log",
            template.getAsJsonObject("textures").get("log").getAsString());
        assertEquals("cavesnotcliffs:block/template_campfire",
            json("models/block/campfire.json").get("parent").getAsString());
        assertEquals("cavesnotcliffs:block/template_campfire",
            json("models/block/soul_campfire.json").get("parent").getAsString());
        assertEquals("block/cube_all",
            json("models/block/soul_soil.json").get("parent").getAsString());
        assertEquals("item/generated",
            json("models/item/campfire.json").get("parent").getAsString());
        assertEquals("item/generated",
            json("models/item/soul_campfire.json").get("parent").getAsString());
        assertEquals("cavesnotcliffs:block/soul_soil",
            json("models/item/soul_soil.json").get("parent").getAsString());
    }

    @Test
    public void blockstatesCoverFacingLightingAndWaterWithoutInvalidBlockPrefixes() {
        for (String id : new String[] {"campfire", "soul_campfire"}) {
            JsonArray multipart = json("blockstates/" + id + ".json")
                .getAsJsonArray("multipart");
            assertEquals(9, multipart.size());
            int waterOverlays = 0;
            for (JsonElement element : multipart) {
                JsonObject part = element.getAsJsonObject();
                String model = part.getAsJsonObject("apply").get("model").getAsString();
                assertFalse(model.contains(":block/"));
                if (part.has("when") && part.getAsJsonObject("when").has("waterlogged")) {
                    assertEquals("true", part.getAsJsonObject("when")
                        .get("waterlogged").getAsString());
                    assertEquals("cavesnotcliffs:amethyst_water_overlay", model);
                    ++waterOverlays;
                }
            }
            assertEquals(1, waterOverlays);
        }
        assertEquals("cavesnotcliffs:soul_soil", json("blockstates/soul_soil.json")
            .getAsJsonObject("variants").getAsJsonObject("normal")
            .get("model").getAsString());
    }

    @Test
    public void craftingPatternsAndIngredientFamiliesMatchJava1182() {
        JsonObject normal = json("recipes/campfire.json");
        assertEquals("forge:ore_shaped", normal.get("type").getAsString());
        assertEquals(" S ", normal.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("SCS", normal.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals("LLL", normal.getAsJsonArray("pattern").get(2).getAsString());
        assertEquals("logWood", normal.getAsJsonObject("key")
            .getAsJsonObject("L").get("ore").getAsString());
        assertEquals("coal", normal.getAsJsonObject("key")
            .getAsJsonObject("C").get("ore").getAsString());

        JsonObject soul = json("recipes/soul_campfire.json");
        assertEquals("S#S", soul.getAsJsonArray("pattern").get(1).getAsString());
        JsonArray bases = soul.getAsJsonObject("key").getAsJsonArray("#");
        assertEquals(2, bases.size());
        assertEquals("minecraft:soul_sand", bases.get(0).getAsJsonObject()
            .get("item").getAsString());
        assertEquals("cavesnotcliffs:soul_soil", bases.get(1).getAsJsonObject()
            .get("item").getAsString());
    }

    @Test
    public void soundDefinitionsPreserveOfficialCountsAndVolumes() {
        JsonObject sounds = json("sounds.json");
        assertEquals(6, entries(sounds, "block.campfire.crackle").size());
        assertEquals(6, entries(sounds, "block.soul_soil.break").size());
        assertEquals(5, entries(sounds, "block.soul_soil.fall").size());
        assertEquals(5, entries(sounds, "block.soul_soil.hit").size());
        assertEquals(6, entries(sounds, "block.soul_soil.place").size());
        assertEquals(5, entries(sounds, "block.soul_soil.step").size());
        for (JsonElement entry : entries(sounds, "block.soul_soil.break")) {
            assertEquals(0.8F, entry.getAsJsonObject().get("volume").getAsFloat(), 0.0F);
        }
        for (JsonElement entry : entries(sounds, "block.soul_soil.hit")) {
            assertEquals(0.66F, entry.getAsJsonObject().get("volume").getAsFloat(), 0.0F);
        }
    }

    @Test
    public void copiedBinaryAssetsMatchTheOfficialJava1182Hashes() throws Exception {
        Map<String, String> hashes = new LinkedHashMap<String, String>();
        hashes.put("textures/blocks/campfire_fire.png", "676c1edc982a35dbd86abdd41ddaf1d8131b268e");
        hashes.put("textures/blocks/campfire_fire.png.mcmeta", "11af65f9ec75a1e408a003168b0610b3965b4ed6");
        hashes.put("textures/blocks/campfire_log.png", "c417aaabcb61cab834e1c1a87c8a40558c9c1129");
        hashes.put("textures/blocks/campfire_log_lit.png", "f3756f44c099bef85c5a9ee277cc33ab06264605");
        hashes.put("textures/blocks/campfire_log_lit.png.mcmeta", "cb302ecd5aa57c8ca1268664c00ad92185f7ea12");
        hashes.put("textures/blocks/soul_campfire_fire.png", "5c66c59b51a3148390fdeaf66a56ebb136b98e65");
        hashes.put("textures/blocks/soul_campfire_fire.png.mcmeta", "406b4f3dceacb7f3b665dc739f949d1b97d702ee");
        hashes.put("textures/blocks/soul_campfire_log_lit.png", "38beddab743350e91c5a059fa167bed967336800");
        hashes.put("textures/blocks/soul_campfire_log_lit.png.mcmeta", "cb302ecd5aa57c8ca1268664c00ad92185f7ea12");
        hashes.put("textures/blocks/soul_soil.png", "128cbfdba34111b17d119bb29e36123db4811b6b");
        hashes.put("textures/items/campfire.png", "afc1b5820376ac66707556410e576dd1a58a9134");
        hashes.put("textures/items/soul_campfire.png", "68dcd476312bf9119858b49160f1a62d559ef4fa");
        addSoundHashes(hashes);
        assertEquals(29, hashes.size());
        for (Map.Entry<String, String> entry : hashes.entrySet()) {
            assertEquals(entry.getKey(), entry.getValue(), sha1(entry.getKey()));
        }
    }

    private static void addSoundHashes(Map<String, String> hashes) {
        hashes.put("sounds/block/campfire/crackle1.ogg", "5cc75e9e824a6ae798e657282694cc4174bfdd15");
        hashes.put("sounds/block/campfire/crackle2.ogg", "c076377ba38821a3b050ebcf333043cb4cf59b81");
        hashes.put("sounds/block/campfire/crackle3.ogg", "f1a2ede27259b0b2321e36b2d70c2fd63d82e862");
        hashes.put("sounds/block/campfire/crackle4.ogg", "d696e2d6640747987a056319dfd6e15c25e75191");
        hashes.put("sounds/block/campfire/crackle5.ogg", "2b15d255ecf10baa81d8f51df3d2e43c68b52152");
        hashes.put("sounds/block/campfire/crackle6.ogg", "e1e793d3c910f0c270eeb9d590774583451f94f5");
        hashes.put("sounds/block/soul_soil/break1.ogg", "1e197317f402ccc3a62f0ce331889a277cb24365");
        hashes.put("sounds/block/soul_soil/break2.ogg", "6be94b5f5daf271e5d8e059eba721f9edecbbc57");
        hashes.put("sounds/block/soul_soil/break3.ogg", "e23c57397e1a7f079ac9afbcf3ee020f4ebc835c");
        hashes.put("sounds/block/soul_soil/break4.ogg", "609fe1ad7765f15a862cd1fb6dcfde7c26f02044");
        hashes.put("sounds/block/soul_soil/break5.ogg", "51151ce3073683d4054a8f1207ac3d8c0d808fa9");
        hashes.put("sounds/block/soul_soil/break6.ogg", "56beb0dbd524e0ac7f56c4458d9bf4083b5370d8");
        hashes.put("sounds/block/soul_soil/step1.ogg", "4bb6b399df5efa0f3693285fc3ae6534b0d81ec3");
        hashes.put("sounds/block/soul_soil/step2.ogg", "85ff3e2aabe317a1aa8637c27188b6940fd759dd");
        hashes.put("sounds/block/soul_soil/step3.ogg", "099962527f625857b566bc0eb4e385ea252b23cd");
        hashes.put("sounds/block/soul_soil/step4.ogg", "129780e9ac9587edca1ed59b1cefbb7369a7202f");
        hashes.put("sounds/block/soul_soil/step5.ogg", "34051ededea1343bfea6fadd6556e642b6e5cce9");
    }

    private static JsonArray entries(JsonObject sounds, String id) {
        return sounds.getAsJsonObject(id).getAsJsonArray("sounds");
    }

    private static JsonObject json(String path) {
        return new JsonParser().parse(new InputStreamReader(resource(ROOT + path),
            StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static String sha1(String path) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-1").digest(read(ROOT + path));
        StringBuilder value = new StringBuilder();
        for (byte octet : digest) {
            value.append(String.format("%02x", octet & 255));
        }
        return value.toString();
    }

    private static byte[] read(String path) throws IOException {
        InputStream stream = resource(path);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        for (int count; (count = stream.read(buffer)) >= 0;) {
            output.write(buffer, 0, count);
        }
        stream.close();
        return output.toByteArray();
    }

    private static InputStream resource(String path) {
        InputStream stream = CampfireAssetsTest.class.getClassLoader()
            .getResourceAsStream(path);
        if (stream == null) {
            throw new AssertionError("Missing resource: " + path);
        }
        return stream;
    }
}

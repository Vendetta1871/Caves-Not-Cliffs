package net.celestiald.cavesnotcliffs.entity;

import com.google.gson.JsonArray;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BeeAssetsTest {
    @Test
    public void allPublicAndHiddenModelsResolveTheirCanonicalTextures() {
        for (String block : new String[]{"bee_nest", "bee_nest_honey",
                "beehive", "beehive_honey"}) {
            JsonObject state = json("assets/cavesnotcliffs/blockstates/"
                    + block + ".json");
            assertEquals(4, state.getAsJsonObject("variants").size());
            JsonObject model = json("assets/cavesnotcliffs/models/block/"
                    + block + ".json");
            assertEquals("cavesnotcliffs:block/orientable_with_bottom",
                    model.get("parent").getAsString());
            assertTrue(model.getAsJsonObject("textures").get("front")
                    .getAsString().contains(block.startsWith("bee_nest")
                            ? "bee_nest_front" : "beehive_front"));
        }
        for (String item : new String[]{"bee_nest", "beehive", "honeycomb"}) {
            resource("assets/cavesnotcliffs/models/item/" + item + ".json");
        }
    }

    @Test
    public void beehiveRecipeUsesSixPlanksAndThreeHoneycombs() {
        JsonObject recipe = json("assets/cavesnotcliffs/recipes/beehive.json");
        assertEquals("forge:ore_shaped", recipe.get("type").getAsString());
        JsonArray pattern = recipe.getAsJsonArray("pattern");
        assertEquals("PPP", pattern.get(0).getAsString());
        assertEquals("HHH", pattern.get(1).getAsString());
        assertEquals("PPP", pattern.get(2).getAsString());
        assertEquals("plankWood", recipe.getAsJsonObject("key")
                .getAsJsonObject("P").get("ore").getAsString());
        assertEquals("cavesnotcliffs:honeycomb", recipe.getAsJsonObject("key")
                .getAsJsonObject("H").get("item").getAsString());
        assertEquals("cavesnotcliffs:beehive", recipe.getAsJsonObject("result")
                .get("item").getAsString());
    }

    @Test
    public void soundDefinitionsRetainOfficialEntryCountsAndMixValues() {
        JsonObject sounds = json("assets/cavesnotcliffs/sounds.json");
        assertEquals(12, event(sounds, "block.beehive.drip").size());
        assertEquals(2, event(sounds, "block.beehive.enter").size());
        assertEquals(3, event(sounds, "block.beehive.exit").size());
        assertEquals(3, event(sounds, "block.beehive.shear").size());
        assertEquals(4, event(sounds, "block.beehive.work").size());
        assertEquals(2, event(sounds, "entity.bee.death").size());
        assertEquals(6, event(sounds, "entity.bee.hurt").size());
        assertEquals(6, event(sounds, "entity.bee.loop").size());
        assertEquals(3, event(sounds, "entity.bee.loop_aggressive").size());
        assertEquals(4, event(sounds, "entity.bee.pollinate").size());
        assertEquals(2, event(sounds, "entity.bee.sting").size());
        JsonObject drip = event(sounds, "block.beehive.drip").get(0)
                .getAsJsonObject();
        assertEquals(8, drip.get("attenuation_distance").getAsInt());
        assertEquals(0.7F, drip.get("pitch").getAsFloat(), 0.0F);
        assertEquals(0.3F, drip.get("volume").getAsFloat(), 0.0F);
    }

    @Test
    public void copiedAssetsMatchOfficialClientAndAssetIndexHashes() throws Exception {
        Map<String, String> hashes = new LinkedHashMap<String, String>();
        hashes.put("textures/blocks/bee_nest_bottom.png", "90a4387e1f9e766278ee86eff52a78a97275fd39");
        hashes.put("textures/blocks/bee_nest_front.png", "3442afbf945745f6f71215909b766de71a905458");
        hashes.put("textures/blocks/bee_nest_front_honey.png", "fdb051dfb41e7b2960b3d4735b3f8d3c08525321");
        hashes.put("textures/blocks/bee_nest_side.png", "d0f71d9ffe64148620b4b25e6f894a960a490626");
        hashes.put("textures/blocks/bee_nest_top.png", "6d41bf09ae5b609228c5ed96ad52cfa3ab2b4b73");
        hashes.put("textures/blocks/beehive_end.png", "03a77aa147ec8c78ac34b5765cafdcca0b93c9a5");
        hashes.put("textures/blocks/beehive_front.png", "aed209c0e1032e0b1cbddbe1cdd377270bbb78b0");
        hashes.put("textures/blocks/beehive_front_honey.png", "b504453868f8824b8c0d55490bce02760b1bc80f");
        hashes.put("textures/blocks/beehive_side.png", "2aa8e9e554e1568447bbdbbaad1e83bcbd233b2e");
        hashes.put("textures/items/honeycomb.png", "443ff949a601f10d5aabc4f3f24337a9bb5c3f41");
        hashes.put("textures/entity/bee/bee.png", "0574daa97c494a22cced4c7a78b7f3d8461d7e2e");
        hashes.put("textures/entity/bee/bee_angry.png", "475e61daa5c435c49f9baf8b103d4685f3ce78db");
        hashes.put("textures/entity/bee/bee_angry_nectar.png", "ce6ae024436d575ee88c74554863b65a04f32cc8");
        hashes.put("textures/entity/bee/bee_nectar.png", "2adb8bfefd5ffce088cc7af0e553bf7e8db3e2f3");
        hashes.put("textures/entity/bee/bee_stinger.png", "4fe0f4f7f50e6ed2508d687bc2148756dcfc0209");
        hashes.put("sounds/block/beehive/drip1.ogg", "92e5c59c01a77c6b8e3520166a99d4c85fabd32b");
        hashes.put("sounds/block/beehive/drip2.ogg", "babba94d1823b3a43dfe1e32b55263f391568c39");
        hashes.put("sounds/block/beehive/drip3.ogg", "6876f57693dda08f22402814218e81a2a162b889");
        hashes.put("sounds/block/beehive/drip4.ogg", "2ed111eb51ab8c30bbf32da361b31f747f713ee2");
        hashes.put("sounds/block/beehive/drip5.ogg", "b42ab99f7c5546d4ccc9e7bfe4dd2ae942abe5f3");
        hashes.put("sounds/block/beehive/drip6.ogg", "89dcde689715952f9bd96b5a2e02bb0a22b5870e");
        hashes.put("sounds/block/beehive/enter.ogg", "4dc5acc50501a33b6f85216e3344a9c08d0b0570");
        hashes.put("sounds/block/beehive/exit.ogg", "c72964143e2c5ea656f88d820937bb14275093db");
        hashes.put("sounds/block/beehive/shear.ogg", "64dac9f0ca2c90da7002d2db65ab70527fa78758");
        hashes.put("sounds/block/beehive/work1.ogg", "aa244e75b37714930d5a0c4d01c4921705d78e5f");
        hashes.put("sounds/block/beehive/work2.ogg", "df0659c8e7120b4a49050d8cbd344017c83beb62");
        hashes.put("sounds/block/beehive/work3.ogg", "c5ec705763a6e1fe6e9578a00746b4cde7634929");
        hashes.put("sounds/block/beehive/work4.ogg", "6863a95d23db5e180e1427c80175e246c8911fba");
        hashes.put("sounds/mob/bee/aggressive1.ogg", "3431b625ae64610cb748dbcdacdf6c9578051fbb");
        hashes.put("sounds/mob/bee/aggressive2.ogg", "acbc52bac82416f50d8af54e424485a2ac9f162d");
        hashes.put("sounds/mob/bee/aggressive3.ogg", "b2f1c66c77f08d1673d751c459d06d2c2699137f");
        hashes.put("sounds/mob/bee/death1.ogg", "b561c9e89f5f16cc8c61974ac44e0cdf57b65e33");
        hashes.put("sounds/mob/bee/death2.ogg", "14ce421343b73f7850bccc0805db2d076e5ae1b9");
        hashes.put("sounds/mob/bee/hurt1.ogg", "e3398e28955949fc10e89614a92e472df0e5f3ca");
        hashes.put("sounds/mob/bee/hurt2.ogg", "a5c687f700791afe54a3b948a6ae91dd2be0d140");
        hashes.put("sounds/mob/bee/hurt3.ogg", "802a2c039bfcc76abebde6cbede997eca94c5e1c");
        hashes.put("sounds/mob/bee/loop1.ogg", "a49599ccb17bff94074692a3cbfc4643355bafd5");
        hashes.put("sounds/mob/bee/loop2.ogg", "c97ad8228e7d9fff827e30243695e5d98259679d");
        hashes.put("sounds/mob/bee/loop3.ogg", "d79b164ee314385cf178db7247a0601bb07863dd");
        hashes.put("sounds/mob/bee/loop4.ogg", "f4f3b88d93c35e60b5af8ddb0a99d1b466f047b5");
        hashes.put("sounds/mob/bee/loop5.ogg", "baaf01880361c143df0abbbef934e60089bd602f");
        hashes.put("sounds/mob/bee/pollinate1.ogg", "2506c9b39df178c2ca6a22256220ff7f885a1b43");
        hashes.put("sounds/mob/bee/pollinate2.ogg", "8fc8add729c5f3146edcf77755bdb644279dbd07");
        hashes.put("sounds/mob/bee/pollinate3.ogg", "430b4fa14f229d37140b63f522ed4873855ba670");
        hashes.put("sounds/mob/bee/pollinate4.ogg", "24f9d27b2f110847f7e02cad9f53d0dc65558ec4");
        hashes.put("sounds/mob/bee/sting.ogg", "aa8df3091e36e109aab2e0042c4a5559cc57f4b4");

        assertEquals(46, hashes.size());
        for (Map.Entry<String, String> entry : hashes.entrySet()) {
            assertEquals(entry.getKey(), entry.getValue(), sha1(read(
                    "assets/cavesnotcliffs/" + entry.getKey())));
        }
    }

    private static JsonArray event(JsonObject sounds, String key) {
        return sounds.getAsJsonObject(key).getAsJsonArray("sounds");
    }

    private static JsonObject json(String path) {
        return new JsonParser().parse(new InputStreamReader(resource(path),
                StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static InputStream resource(String path) {
        InputStream input = BeeAssetsTest.class.getClassLoader().getResourceAsStream(path);
        assertNotNull(path, input);
        return input;
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

    private static String sha1(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-1").digest(bytes);
        StringBuilder result = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            result.append(String.format("%02x", value & 255));
        }
        return result.toString();
    }
}

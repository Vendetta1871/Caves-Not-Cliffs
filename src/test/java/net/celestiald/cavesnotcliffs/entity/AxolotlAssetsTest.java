package net.celestiald.cavesnotcliffs.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.celestiald.cavesnotcliffs.content.AxolotlMechanics;
import net.celestiald.cavesnotcliffs.item.ItemAxolotlBucket;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.entity.passive.EntityAnimal;
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

public class AxolotlAssetsTest {
    @Test
    public void registryBucketBridgeAndRuntimeTypeAreCanonical() {
        assertEquals("cavesnotcliffs:axolotl", CncRegistryIds.AXOLOTL.toString());
        assertEquals("cavesnotcliffs:axolotl_bucket",
                CncRegistryIds.AXOLOTL_BUCKET.toString());
        assertEquals("cavesnotcliffs:tropical_fish_bucket",
                CncRegistryIds.TROPICAL_FISH_BUCKET.toString());
        assertTrue(EntityAnimal.class.isAssignableFrom(EntityAxolotl.EntityCustom.class));
        assertEquals(1, new ItemAxolotlBucket().getItemStackLimit());

        JsonObject recipe = json("assets/cavesnotcliffs/recipes/tropical_fish_bucket.json");
        JsonArray ingredients = recipe.getAsJsonArray("ingredients");
        assertEquals("minecraft:water_bucket",
                ingredients.get(0).getAsJsonObject().get("item").getAsString());
        assertEquals("minecraft:fish",
                ingredients.get(1).getAsJsonObject().get("item").getAsString());
        assertEquals(2, ingredients.get(1).getAsJsonObject().get("data").getAsInt());
        assertEquals("cavesnotcliffs:tropical_fish_bucket",
                recipe.getAsJsonObject("result").get("item").getAsString());
    }

    @Test
    public void fiveVariantAndTwoBucketModelsArePackaged() {
        for (AxolotlMechanics.Variant variant : AxolotlMechanics.Variant.values()) {
            resource("assets/cavesnotcliffs/textures/entity/axolotl/axolotl_"
                    + variant.serializedName() + ".png");
        }
        for (String item : new String[]{"axolotl_bucket", "tropical_fish_bucket"}) {
            JsonObject model = json("assets/cavesnotcliffs/models/item/" + item + ".json");
            assertEquals("item/generated", model.get("parent").getAsString());
            assertEquals("cavesnotcliffs:items/" + item,
                    model.getAsJsonObject("textures").get("layer0").getAsString());
            resource("assets/cavesnotcliffs/textures/items/" + item + ".png");
        }
    }

    @Test
    public void copiedAssetsMatchOfficialClientAndAssetIndexHashes() throws Exception {
        Map<String, String> hashes = new LinkedHashMap<String, String>();
        hashes.put("textures/entity/axolotl/axolotl_blue.png", "63ad9748befc3acbe23f7d4ae4d0a4c4284818ab");
        hashes.put("textures/entity/axolotl/axolotl_cyan.png", "77ad308872c203b82137cdeeaf38ff7c576d6177");
        hashes.put("textures/entity/axolotl/axolotl_gold.png", "270a247171c517e7ea6c5347833e78611e8b83cd");
        hashes.put("textures/entity/axolotl/axolotl_lucy.png", "c3eb36572a8e447cc2b8c8bb778586114b3c37e1");
        hashes.put("textures/entity/axolotl/axolotl_wild.png", "f547bd8b54ff03ec9b3282e06db25dbe2557404b");
        hashes.put("textures/items/axolotl_bucket.png", "0dfe23e12d049514b5f86ff2fecd157ff5c2cc91");
        hashes.put("textures/items/tropical_fish_bucket.png", "684e47b64a283a82eda1282332afb25ef539c068");
        hashes.put("sounds/entity/fish/swim5.ogg", "2fc54a0a9f4946c4df344399dcb8872e79d1c379");
        hashes.put("sounds/entity/fish/swim6.ogg", "4c51f308cb42bc51779a5461d317d5415c07b294");
        hashes.put("sounds/entity/fish/swim7.ogg", "b4af31228aaf97aa8f83260ca90f64ce6fbc3d9b");
        hashes.put("sounds/item/bucket/empty_fish1.ogg", "ce9704faf305ae2ef170a315ff929067f9ac923c");
        hashes.put("sounds/item/bucket/empty_fish2.ogg", "8fa72261eea571314273ddaf73032b1c9944130c");
        hashes.put("sounds/item/bucket/empty_fish3.ogg", "eb4ef46ee676b18c8bd853188e3ad6a53b37d7c1");
        hashes.put("sounds/item/bucket/fill_axolotl1.ogg", "d36b56ae93d86b47d5d99e9540c2718b826fa2ae");
        hashes.put("sounds/item/bucket/fill_axolotl2.ogg", "41a44dadfb327a146be5898b715bad6eec86adc7");
        hashes.put("sounds/item/bucket/fill_axolotl3.ogg", "cddd681bfba010f8de2616d2a7ecf95d69f5d0d8");
        hashes.put("sounds/mob/axolotl/attack1.ogg", "16ea969a1db6ae466df28025bee90d704d81e969");
        hashes.put("sounds/mob/axolotl/attack2.ogg", "ff6de6ba71b05e968b6c4f75c29bedc349d4a442");
        hashes.put("sounds/mob/axolotl/attack3.ogg", "273bea0342e06591b3e2681f737571f1e002207a");
        hashes.put("sounds/mob/axolotl/attack4.ogg", "75f4ba885bd66d43e9ff6499dd8d7997bfc50f91");
        hashes.put("sounds/mob/axolotl/death1.ogg", "02bd11acbb9717286afd8cd5e2042da90ed21998");
        hashes.put("sounds/mob/axolotl/death2.ogg", "6b225a6f8864f16ba774adbaf58a8bfd729bd334");
        hashes.put("sounds/mob/axolotl/hurt1.ogg", "0c42b94014a3b58e1ae24defb6beaeb84011f9b8");
        hashes.put("sounds/mob/axolotl/hurt2.ogg", "588db45a8132cda050dfa447345b7d1a46774818");
        hashes.put("sounds/mob/axolotl/hurt3.ogg", "cc2638df5c31944928d7cf9e13d9e824a5bb7768");
        hashes.put("sounds/mob/axolotl/hurt4.ogg", "1574c9e6ffd5748852b29951ecfab97e6135f98e");
        hashes.put("sounds/mob/axolotl/idle1.ogg", "79b636ffca3ff472de7e1d7c8cfb46a77874417a");
        hashes.put("sounds/mob/axolotl/idle2.ogg", "03f59217fa2f8ca8820b1ee13187cedfd42d86ce");
        hashes.put("sounds/mob/axolotl/idle3.ogg", "15cdf2534718e12a55668a6ededb358a86fa26c5");
        hashes.put("sounds/mob/axolotl/idle4.ogg", "9f7359689f083bc4184a632ea6169b7a6d95f86c");
        hashes.put("sounds/mob/axolotl/idle5.ogg", "cb1848fdfc37c5ebefd65e29fbefc2d3495f11fc");
        hashes.put("sounds/mob/axolotl/idle_air1.ogg", "398bdfb1b7b64dc83706f0d6ad824b70ce1f9569");
        hashes.put("sounds/mob/axolotl/idle_air2.ogg", "49ac74489b7a0463f8f08d54db100762ffbe2532");
        hashes.put("sounds/mob/axolotl/idle_air3.ogg", "bc781ad9f0f2a4480dea4646952c7d7bb1949d3d");
        hashes.put("sounds/mob/axolotl/idle_air4.ogg", "26549ef538f7406fb870432f3fe11a807c16ef82");
        hashes.put("sounds/mob/axolotl/idle_air5.ogg", "1c262c483a694704cfe7badd978a9eeb49a167a0");
        hashes.put("sounds/mob/dolphin/splash1.ogg", "1125cbb42bed5ce7d91ff530d543c844ac80f735");
        hashes.put("sounds/mob/dolphin/splash2.ogg", "ea6877421607da5668fe07debac455f054868809");
        hashes.put("sounds/mob/dolphin/splash3.ogg", "487dd072ac3d62348c57e255a9d800bd06f505af");
        hashes.put("sounds/mob/dolphin/swim1.ogg", "5c46b478067d883d19756255b7fb132c686389b7");
        hashes.put("sounds/mob/dolphin/swim2.ogg", "5f4b4f79c04df853893c38c64f40d625e5e96929");
        hashes.put("sounds/mob/dolphin/swim3.ogg", "c2ec60b2cdcda5725fbe192c9a4832d322551ee4");
        hashes.put("sounds/mob/dolphin/swim4.ogg", "13beba907bd219f3b2d3f50e716805fa63124cbf");

        assertEquals(43, hashes.size());
        for (Map.Entry<String, String> entry : hashes.entrySet()) {
            assertEquals(entry.getKey(), entry.getValue(), sha1(read(
                    "assets/cavesnotcliffs/" + entry.getKey())));
        }
    }

    @Test
    public void soundDefinitionsRetainOfficialWeightsPitchAndVolume() {
        JsonObject sounds = json("assets/cavesnotcliffs/sounds.json");
        JsonArray attack = sounds.getAsJsonObject("entity.axolotl.attack")
                .getAsJsonArray("sounds");
        assertEquals(4, attack.size());
        assertEquals(0.5F, attack.get(0).getAsJsonObject().get("volume").getAsFloat(), 0.0F);
        JsonObject airThree = sounds.getAsJsonObject("entity.axolotl.idle_air")
                .getAsJsonArray("sounds").get(2).getAsJsonObject();
        assertEquals(1.2F, airThree.get("pitch").getAsFloat(), 0.0F);
        assertEquals(0.8F, airThree.get("volume").getAsFloat(), 0.0F);
        JsonObject splash = sounds.getAsJsonObject("entity.axolotl.splash")
                .getAsJsonArray("sounds").get(0).getAsJsonObject();
        assertEquals(1.2F, splash.get("pitch").getAsFloat(), 0.0F);
        assertEquals(0.9F, splash.get("volume").getAsFloat(), 0.0F);
        assertEquals(7, sounds.getAsJsonObject("entity.axolotl.swim")
                .getAsJsonArray("sounds").size());
        assertEquals(3, sounds.getAsJsonObject("item.bucket.fill_axolotl")
                .getAsJsonArray("sounds").size());
    }

    private static JsonObject json(String path) {
        return new JsonParser().parse(new InputStreamReader(resource(path),
                StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static InputStream resource(String path) {
        InputStream input = AxolotlAssetsTest.class.getClassLoader().getResourceAsStream(path);
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

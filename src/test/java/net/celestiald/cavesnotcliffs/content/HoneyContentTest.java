package net.celestiald.cavesnotcliffs.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.celestiald.cavesnotcliffs.item.ItemHoneyBottle;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.Block;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.junit.BeforeClass;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class HoneyContentTest {
    private static final String ROOT = "assets/cavesnotcliffs/";

    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void registryAndBottleContractsAreCanonical() {
        assertEquals("cavesnotcliffs:honey_bottle",
                CncRegistryIds.HONEY_BOTTLE.toString());
        assertEquals("cavesnotcliffs:honey_block",
                CncRegistryIds.HONEY_BLOCK.toString());
        assertEquals("cavesnotcliffs:honeycomb_block",
                CncRegistryIds.HONEYCOMB_BLOCK.toString());

        ItemHoneyBottle bottle = new ItemHoneyBottle();
        ItemStack stack = new ItemStack(bottle);
        assertEquals(6, bottle.getHealAmount(stack));
        assertEquals(0.1F, bottle.getSaturationModifier(stack), 0.0F);
        assertEquals(40, bottle.getMaxItemUseDuration(stack));
        assertEquals(16, bottle.getItemStackLimit(stack));
        assertEquals(EnumAction.DRINK, bottle.getItemUseAction(stack));
        assertTrue(bottle.hasContainerItem(stack));
        assertSame(Items.GLASS_BOTTLE,
                bottle.getContainerItem(stack).getItem());
    }

    @Test
    public void collisionAndSlideThresholdsMatchTheOfficialOracle() {
        assertFalse(HoneyBlockMechanics.isSliding(true, 64.0D, 64,
                -1.0D, 1.0D, 0.0D, 0.6F));
        assertFalse(HoneyBlockMechanics.isSliding(false,
                64.9375D, 64, -1.0D, 1.0D, 0.0D, 0.6F));
        assertFalse(HoneyBlockMechanics.isSliding(false, 64.5D, 64,
                -0.08D, 1.0D, 0.0D, 0.6F));
        double threshold = 0.4375D + 0.6D / 2.0D;
        assertFalse(HoneyBlockMechanics.isSliding(false, 64.5D, 64,
                -0.0800001D, threshold - 2.0E-7D, 0.0D, 0.6F));
        assertTrue(HoneyBlockMechanics.isSliding(false, 64.5D, 64,
                -0.0800001D, threshold, 0.0D, 0.6F));

        HoneyBlockMechanics.Velocity fast =
                HoneyBlockMechanics.slide(1.0D, -0.2D, 2.0D);
        assertEquals(0.25D, fast.x, 0.0D);
        assertEquals(-0.05D, fast.y, 0.0D);
        assertEquals(0.5D, fast.z, 0.0D);
        HoneyBlockMechanics.Velocity slow =
                HoneyBlockMechanics.slide(1.0D, -0.13D, 2.0D);
        assertEquals(1.0D, slow.x, 0.0D);
        assertEquals(-0.05D, slow.y, 0.0D);
        assertEquals(2.0D, slow.z, 0.0D);
        assertEquals(0.2F, HoneyBlockMechanics.FALL_DAMAGE_MULTIPLIER, 0.0F);
        assertEquals(0.4D, HoneyBlockMechanics.SPEED_FACTOR, 0.0D);
        assertEquals(0.5D, HoneyBlockMechanics.JUMP_FACTOR, 0.0D);
    }

    @Test
    public void runtimeBlocksExposeExactShapeStickinessAndStrength() {
        HoneyContent.HoneyBlockCustom honey = new HoneyContent.HoneyBlockCustom();
        AxisAlignedBB box = honey.getCollisionBoundingBox(
                honey.getDefaultState(), null, BlockPos.ORIGIN);
        assertEquals(1.0D / 16.0D, box.minX, 0.0D);
        assertEquals(0.0D, box.minY, 0.0D);
        assertEquals(15.0D / 16.0D, box.maxX, 0.0D);
        assertEquals(15.0D / 16.0D, box.maxY, 0.0D);
        assertTrue(honey.isStickyBlock(honey.getDefaultState()));
        assertFalse(honey.isOpaqueCube(honey.getDefaultState()));
        assertFalse(honey.isFullCube(honey.getDefaultState()));
        assertEquals(0.0F, honey.getBlockHardness(
                honey.getDefaultState(), null, BlockPos.ORIGIN), 0.0F);

        Block comb = new HoneyContent.HoneycombBlockCustom();
        assertEquals(0.6F, comb.getBlockHardness(
                comb.getDefaultState(), null, BlockPos.ORIGIN), 0.0F);
    }

    @Test
    public void recipeAndModelFamilyMatchesJava1182() {
        JsonObject honeyBlock = json("recipes/honey_block.json");
        assertEquals(2, honeyBlock.getAsJsonArray("pattern").size());
        assertEquals("HH", honeyBlock.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("cavesnotcliffs:honey_bottle",
                honeyBlock.getAsJsonObject("key").getAsJsonObject("H")
                        .get("item").getAsString());

        JsonObject bottles = json("recipes/honey_bottle.json");
        assertEquals(5, bottles.getAsJsonArray("ingredients").size());
        assertEquals(4, bottles.getAsJsonObject("result").get("count").getAsInt());
        JsonObject sugar = json("recipes/sugar_from_honey_bottle.json");
        assertEquals("sugar", sugar.get("group").getAsString());
        assertEquals(3, sugar.getAsJsonObject("result").get("count").getAsInt());

        JsonObject model = json("models/block/honey_block.json");
        assertEquals(2, model.getAsJsonArray("elements").size());
        assertEquals(15, model.getAsJsonArray("elements").get(1)
                .getAsJsonObject().getAsJsonArray("to").get(0).getAsInt());
        json("models/block/honeycomb_block.json");
        json("models/item/honey_bottle.json");
    }

    @Test
    public void soundDefinitionsKeepOfficialEntryCounts() {
        JsonObject sounds = json("sounds.json");
        assertEquals(5, entries(sounds, "block.honey_block.break").size());
        assertEquals(5, entries(sounds, "block.honey_block.fall").size());
        assertEquals(5, entries(sounds, "block.honey_block.hit").size());
        assertEquals(5, entries(sounds, "block.honey_block.place").size());
        assertEquals(8, entries(sounds, "block.honey_block.slide").size());
        assertEquals(5, entries(sounds, "block.honey_block.step").size());
        assertEquals(4, entries(sounds, "item.honey_bottle.drink").size());
        assertEquals(9, entries(sounds, "item.honeycomb.wax_on").size());
        assertEquals(4, entries(sounds, "block.coral_block.break").size());
        assertEquals(6, entries(sounds, "block.coral_block.step").size());
    }

    @Test
    public void copiedTexturesAndSoundsMatchOfficialAssetHashes() throws Exception {
        Map<String, String> hashes = new LinkedHashMap<>();
        hashes.put("textures/blocks/honey_block_bottom.png", "fc5132777e9e40aada8fc4e6d42a0c1045e47c43");
        hashes.put("textures/blocks/honey_block_side.png", "b4a3ffa2bb686c43173efb0f2f48b549dc85ee98");
        hashes.put("textures/blocks/honey_block_top.png", "9e20d72215463ce893d3a5ab15fd1edf6f544d95");
        hashes.put("textures/blocks/honeycomb_block.png", "8d7cf19b5ecf11a0540f6d559f38f4250052c53a");
        hashes.put("textures/items/honey_bottle.png", "5bc0d5159ac1583500b53ea9d835f499d2b9d7fa");
        addSoundHashes(hashes);
        assertEquals(35, hashes.size());
        for (Map.Entry<String, String> entry : hashes.entrySet()) {
            assertEquals(entry.getKey(), entry.getValue(), sha1(entry.getKey()));
        }
    }

    private static void addSoundHashes(Map<String, String> h) {
        h.put("sounds/block/honeyblock/break1.ogg", "5b16028a58fbd888b1ccc5062e2592b2995a159a");
        h.put("sounds/block/honeyblock/break2.ogg", "a99000d5d82b75a4c18b44c8579ed1da123fa782");
        h.put("sounds/block/honeyblock/break3.ogg", "b2f3ee9fbe0bdef433cf03fe759f23e04fd46e29");
        h.put("sounds/block/honeyblock/break4.ogg", "f577d81300052c0ee8d5c090384a166a775bfc98");
        h.put("sounds/block/honeyblock/break5.ogg", "7e5e2978d628f7fc22ddb0fee1e108228c4aaf65");
        h.put("sounds/block/honeyblock/slide1.ogg", "8245b9f22fa32f4e74e150fd225fff00362e74cc");
        h.put("sounds/block/honeyblock/slide2.ogg", "013b1518d41176f1884ef17944104a8473d408ce");
        h.put("sounds/block/honeyblock/slide3.ogg", "d4ad02bc3a9c3bd5eac9774d1130bac5cd73ba94");
        h.put("sounds/block/honeyblock/slide4.ogg", "102613b04262cd0f94702afd14063fa0187bc158");
        h.put("sounds/block/honeyblock/step1.ogg", "ec34853a3a7def9e2ed0c530fd79035c1b41d16d");
        h.put("sounds/block/honeyblock/step2.ogg", "67dd095ff29dea6092530dbf8bcf9cc77efdb49a");
        h.put("sounds/block/honeyblock/step3.ogg", "d5d7e396f52574ac18b6cedbd713144c887a7931");
        h.put("sounds/block/honeyblock/step4.ogg", "1ec3dff91a5db985385bf08d47749872cecc08d9");
        h.put("sounds/block/honeyblock/step5.ogg", "5dac0e85e722c9238e95439970693f1838106acd");
        h.put("sounds/item/bottle/drink_honey1.ogg", "072397f08683ce80e7ab2511b19bbc8f4816f125");
        h.put("sounds/item/bottle/drink_honey2.ogg", "2d8f504deed070027dd587a2db213acbc88e01e7");
        h.put("sounds/item/bottle/drink_honey3.ogg", "1389244058c7257571f36d6aaa4f861821cf6066");
        h.put("sounds/item/honeycomb/wax_on1.ogg", "4a73e322395e001dd4e47da97ebd47600240e00b");
        h.put("sounds/item/honeycomb/wax_on2.ogg", "0c7bc9616e7cb18b994e01642be868b5e05fd8a3");
        h.put("sounds/item/honeycomb/wax_on3.ogg", "c7f1331e81208196026edef71721e9cdaf8a0425");
        h.put("sounds/dig/coral1.ogg", "cd61da7894c952afa4ce081c6dff0bc00e20d5ac");
        h.put("sounds/dig/coral2.ogg", "251f772fcc263d83b98d448f1a234b877ae24730");
        h.put("sounds/dig/coral3.ogg", "c7697521726dfabb88db2db7eeba3f9a3efbcc83");
        h.put("sounds/dig/coral4.ogg", "e398c144e0b7ff60ac7400020015bb370a70d1f6");
        h.put("sounds/step/coral1.ogg", "9ba8e3b6d047fe102091805ca23b2e65d2c1e7a9");
        h.put("sounds/step/coral2.ogg", "c3aa24d9f9fa7ef3ebbc25ca816f269fec058de2");
        h.put("sounds/step/coral3.ogg", "40b34e36a30bb0269c16043715f723f1e4b8d481");
        h.put("sounds/step/coral4.ogg", "a321b775f190bddfa3a2bb393f653b7a324d35cc");
        h.put("sounds/step/coral5.ogg", "18a0bafb39a86b9c32e8efe3658a8d111e5492ea");
        h.put("sounds/step/coral6.ogg", "0f58866321e3000bf611fa2820f21e390a004696");
    }

    private static JsonArray entries(JsonObject sounds, String key) {
        return sounds.getAsJsonObject(key).getAsJsonArray("sounds");
    }

    private static JsonObject json(String path) {
        return new JsonParser().parse(new InputStreamReader(resource(ROOT + path),
                StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static String sha1(String path) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-1")
                .digest(read(ROOT + path));
        StringBuilder result = new StringBuilder();
        for (byte value : digest) {
            result.append(String.format("%02x", value & 255));
        }
        return result.toString();
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

    private static InputStream resource(String path) {
        InputStream input = HoneyContentTest.class.getClassLoader()
                .getResourceAsStream(path);
        assertNotNull(path, input);
        return input;
    }
}

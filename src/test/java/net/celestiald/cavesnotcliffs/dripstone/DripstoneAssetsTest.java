package net.celestiald.cavesnotcliffs.dripstone;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.io.InputStream;
import java.security.MessageDigest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DripstoneAssetsTest {
    private static final String ROOT = "assets/cavesnotcliffs/";

    @Test
    public void everyCanonicalDirectionThicknessModelAndTextureExists() throws Exception {
        JsonObject variants = json(ROOT + "blockstates/pointed_dripstone.json")
                .getAsJsonObject("variants");
        assertEquals(10, variants.size());
        assertNotNull(resource(ROOT + "blockstates/pointed_dripstone_waterlogged.json"));
        assertNotNull(resource(ROOT + "models/block/pointed_dripstone.json"));
        for (String direction : new String[]{"up", "down"}) {
            for (String thickness : new String[]{"tip_merge", "tip", "frustum", "middle", "base"}) {
                assertNotNull(resource(ROOT + "models/block/pointed_dripstone_"
                        + direction + "_" + thickness + ".json"));
                assertNotNull(resource(ROOT + "textures/blocks/pointed_dripstone_"
                        + direction + "_" + thickness + ".png"));
            }
        }
        assertNotNull(resource(ROOT + "models/item/pointed_dripstone.json"));
        assertNotNull(resource(ROOT + "textures/items/pointed_dripstone.png"));
    }

    @Test
    public void officialClientTextureBytesArePinned() throws Exception {
        assertEquals("ae5e48c1fb116a8adadc27a45096c5121df714ce",
                sha1(ROOT + "textures/items/pointed_dripstone.png"));
        assertEquals("10641314c68cee2a19ba3d998a290ff3d629fcd0",
                sha1(ROOT + "textures/blocks/pointed_dripstone_down_base.png"));
        assertEquals("2730d8d33b61703169d758b0a4cce32785b4b4ed",
                sha1(ROOT + "textures/blocks/pointed_dripstone_up_tip.png"));
        assertEquals("77e65833e9fefc63540ec886bcd9b531f2420478",
                sha1(ROOT + "textures/blocks/dripstone_block.png"));
    }

    @Test
    public void completeOfficialSoundFamiliesArePresent() throws Exception {
        JsonObject sounds = json(ROOT + "sounds.json");
        assertEquals(15, sounds.getAsJsonObject("block.pointed_dripstone.drip_water")
                .getAsJsonArray("sounds").size());
        assertEquals(8, sounds.getAsJsonObject(
                "block.pointed_dripstone.drip_water_into_cauldron")
                .getAsJsonArray("sounds").size());
        assertEquals(6, sounds.getAsJsonObject("block.pointed_dripstone.drip_lava")
                .getAsJsonArray("sounds").size());
        assertEquals(4, sounds.getAsJsonObject(
                "block.pointed_dripstone.drip_lava_into_cauldron")
                .getAsJsonArray("sounds").size());
        assertEquals(5, sounds.getAsJsonObject("block.pointed_dripstone.land")
                .getAsJsonArray("sounds").size());
        assertEquals("3b6a7430672f147418d8aca9e8675d8e0e177b17",
                sha1(ROOT + "sounds/block/pointed_dripstone/drip_lava1.ogg"));
        assertEquals("bdda3824c586d34156daccf5c4a76c0cb54221a6",
                sha1(ROOT + "sounds/block/pointed_dripstone/drip_water_cauldron8.ogg"));
    }

    private static JsonObject json(String path) throws Exception {
        InputStream stream = resource(path);
        try {
            return new JsonParser().parse(new java.io.InputStreamReader(stream, "UTF-8"))
                    .getAsJsonObject();
        } finally {
            stream.close();
        }
    }

    private static String sha1(String path) throws Exception {
        InputStream stream = resource(path);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[8192];
            for (int read; (read = stream.read(buffer)) >= 0;) {
                digest.update(buffer, 0, read);
            }
            StringBuilder value = new StringBuilder();
            for (byte part : digest.digest()) {
                value.append(String.format("%02x", part & 0xff));
            }
            return value.toString();
        } finally {
            stream.close();
        }
    }

    private static InputStream resource(String path) {
        InputStream stream = DripstoneAssetsTest.class.getClassLoader()
                .getResourceAsStream(path);
        assertNotNull(path, stream);
        return stream;
    }
}

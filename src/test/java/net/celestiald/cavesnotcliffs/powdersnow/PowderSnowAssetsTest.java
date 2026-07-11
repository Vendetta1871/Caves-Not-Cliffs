package net.celestiald.cavesnotcliffs.powdersnow;

import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PowderSnowAssetsTest {
    @Test
    public void canonicalRegistryIdsAndModelsAreComplete() {
        assertEquals("cavesnotcliffs:powder_snow", CncRegistryIds.POWDER_SNOW.toString());
        assertEquals("cavesnotcliffs:powder_snow_bucket",
            CncRegistryIds.POWDER_SNOW_BUCKET.toString());
        assertEquals("cavesnotcliffs:powder_snow_cauldron",
            CncRegistryIds.POWDER_SNOW_CAULDRON.toString());

        String[] resources = {
            "assets/cavesnotcliffs/blockstates/powder_snow.json",
            "assets/cavesnotcliffs/blockstates/powder_snow_cauldron.json",
            "assets/cavesnotcliffs/models/block/powder_snow.json",
            "assets/cavesnotcliffs/models/block/powder_snow_cauldron_level1.json",
            "assets/cavesnotcliffs/models/block/powder_snow_cauldron_level2.json",
            "assets/cavesnotcliffs/models/block/powder_snow_cauldron_full.json",
            "assets/cavesnotcliffs/models/item/powder_snow_bucket.json",
            "assets/cavesnotcliffs/textures/blocks/powder_snow.png",
            "assets/cavesnotcliffs/textures/items/powder_snow_bucket.png"
        };
        for (String resource : resources) {
            assertNotNull("missing powder-snow resource " + resource,
                getClass().getClassLoader().getResource(resource));
        }
    }

    @Test
    public void copiedMojangAssetsMatchTheOfficial1182AssetIndex() throws Exception {
        Map<String, String> hashes = new LinkedHashMap<String, String>();
        hashes.put("textures/blocks/powder_snow.png",
            "78851af1f38b8aab2f7d3a798ae0bad45776c144");
        hashes.put("textures/items/powder_snow_bucket.png",
            "55cff8a7e365a175107bc4a59d83191189484c00");
        hashes.put("sounds/block/powder_snow/break1.ogg", "1e5627cdb081851d666923bb8aa92a29ca56dc36");
        hashes.put("sounds/block/powder_snow/break2.ogg", "9929be86fe1606d155e965d70c387f2b2cb8a326");
        hashes.put("sounds/block/powder_snow/break3.ogg", "b7e64d990e44daa87fc57808627c3f116ad12868");
        hashes.put("sounds/block/powder_snow/break4.ogg", "638b448f4f4298cdb65fe8d4b99fd90daaa672b1");
        hashes.put("sounds/block/powder_snow/break5.ogg", "0027d19bb0cd9c914d3ac148aeee6316d9786341");
        hashes.put("sounds/block/powder_snow/break6.ogg", "2ed2ba2403409c9eedda1b3bcbf49595e40f237a");
        hashes.put("sounds/block/powder_snow/break7.ogg", "50775dd4c91553d43b7c9c1d409002f3ee582354");
        hashes.put("sounds/block/powder_snow/step1.ogg", "d86bd0068709f9bacdb694a1ea3f1b321dbd4e44");
        hashes.put("sounds/block/powder_snow/step2.ogg", "fd5a0ea09e1643206c9a33592440e0f87a556455");
        hashes.put("sounds/block/powder_snow/step3.ogg", "34bd6800e8fc87778b8aed0e3616566ad51d7ff5");
        hashes.put("sounds/block/powder_snow/step4.ogg", "d06ea8fa95da528b0113f6ad0ff50bc0a5d49cd9");
        hashes.put("sounds/block/powder_snow/step5.ogg", "f857fcb908528ca66c51b4b8608d7c76b178cb92");
        hashes.put("sounds/block/powder_snow/step6.ogg", "5628a7a3a5ce397716dc8f4bab5d2a998af0eff0");
        hashes.put("sounds/block/powder_snow/step7.ogg", "015f37d3f2adec2f62dfea46fa1cb9dc1262900d");
        hashes.put("sounds/block/powder_snow/step8.ogg", "4556ec1597a31dd58289580a933cbaa9375ad272");
        hashes.put("sounds/block/powder_snow/step9.ogg", "99edd889de4dabb9f4b7b92f548dfceb2c7d16c6");
        hashes.put("sounds/block/powder_snow/step10.ogg", "192e2f57b6d878465f7baa500493fb11a5ccb220");
        hashes.put("sounds/item/bucket/empty_powder_snow1.ogg", "0cb553eb257614ee1a58e57f8d1c292e9cd393c2");
        hashes.put("sounds/item/bucket/empty_powder_snow2.ogg", "54ab71a9e53979af80cbfbfdb3d542ae14b66d0a");
        hashes.put("sounds/item/bucket/fill_powder_snow1.ogg", "bfdeec56755e5c566fa045a678d3fd1c062065f3");
        hashes.put("sounds/item/bucket/fill_powder_snow2.ogg", "fba31761b635d23b80a9c48c1bfc5556f7441679");

        for (Map.Entry<String, String> asset : hashes.entrySet()) {
            String resource = "assets/cavesnotcliffs/" + asset.getKey();
            assertEquals(resource, asset.getValue(), sha1(readResource(resource)));
        }
    }

    @Test
    public void soundDefinitionsReferenceEveryOfficialClip() throws Exception {
        String json = new String(readResource("assets/cavesnotcliffs/sounds.json"), "UTF-8");
        String[] events = {
            "block.powder_snow.break", "block.powder_snow.fall",
            "block.powder_snow.hit", "block.powder_snow.place",
            "block.powder_snow.step", "item.bucket.empty_powder_snow",
            "item.bucket.fill_powder_snow"
        };
        for (String event : events) {
            assertTrue("missing sound event " + event, json.contains("\"" + event + "\""));
        }
        for (int index = 1; index <= 7; ++index) {
            assertTrue(json.contains("block/powder_snow/break" + index));
        }
        for (int index = 1; index <= 10; ++index) {
            assertTrue(json.contains("block/powder_snow/step" + index));
        }
        for (String action : new String[]{"empty", "fill"}) {
            for (int index = 1; index <= 2; ++index) {
                assertTrue(json.contains("item/bucket/" + action
                    + "_powder_snow" + index));
            }
        }
    }

    private byte[] readResource(String path) throws IOException {
        InputStream input = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull("missing resource " + path, input);
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } finally {
            input.close();
        }
    }

    private String sha1(byte[] data) throws NoSuchAlgorithmException {
        byte[] digest = MessageDigest.getInstance("SHA-1").digest(data);
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            hex.append(String.format("%02x", value & 255));
        }
        return hex.toString();
    }
}

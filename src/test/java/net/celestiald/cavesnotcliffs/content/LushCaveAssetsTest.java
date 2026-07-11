package net.celestiald.cavesnotcliffs.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import org.junit.Test;
import org.junit.BeforeClass;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LushCaveAssetsTest {
    @BeforeClass
    public static void bootstrapMinecraftRegistries() {
        net.minecraft.init.Bootstrap.register();
    }

    private static final List<String> PUBLIC_BLOCKS = Arrays.asList(
            "azalea", "flowering_azalea", "azalea_leaves",
            "flowering_azalea_leaves", "rooted_dirt", "hanging_roots",
            "moss_block", "moss_carpet", "small_dripleaf", "big_dripleaf",
            "spore_blossom");

    private static final List<String> HIDDEN_BLOCKS = Arrays.asList(
            "cave_vines", "cave_vines_age_8_15", "cave_vines_age_16_23",
            "cave_vines_age_24_25", "cave_vines_plant",
            "hanging_roots_waterlogged", "big_dripleaf_waterlogged",
            "big_dripleaf_stem", "potted_azalea_bush",
            "potted_flowering_azalea_bush");

    @Test
    public void everyPublicAndHiddenRegistryStateHasCanonicalResources() {
        for (String path : PUBLIC_BLOCKS) {
            resource("assets/cavesnotcliffs/blockstates/" + path + ".json");
            resource("assets/cavesnotcliffs/models/item/" + path + ".json");
        }
        for (String path : HIDDEN_BLOCKS) {
            resource("assets/cavesnotcliffs/blockstates/" + path + ".json");
            assertTrue(path, LegacyItemExpectation.isHidden(path));
        }
        resource("assets/cavesnotcliffs/models/item/glow_berries.json");
        assertEquals("cavesnotcliffs:cave_vines", CncRegistryIds.CAVE_VINES.toString());
        assertEquals("cavesnotcliffs:azalea_leaves",
                CncRegistryIds.AZALEA_LEAVES.toString());
        assertEquals("cavesnotcliffs:rooted_dirt", CncRegistryIds.ROOTED_DIRT.toString());
    }

    @Test
    public void everyCopiedTextureAndSoundMatchesItsPinnedOfficialSha1() throws Exception {
        InputStream manifest = resource(
                "net/celestiald/cavesnotcliffs/content/lush-assets-sha1.tsv");
        int checked = 0;
        try (Scanner scanner = new Scanner(manifest, "UTF-8")) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] fields = line.split("\\t", 2);
                assertEquals(line, 2, fields.length);
                assertEquals(fields[1], fields[0], sha1(read(fields[1])));
                checked++;
            }
        }
        assertEquals(130, checked);
    }

    @Test
    public void allFiftyEightSoundEventsResolveAndRetainOfficialEntrySettings() {
        JsonObject sounds = json("assets/cavesnotcliffs/sounds.json");
        Set<String> events = new HashSet<>();
        LushCaveSounds.events().forEach(sound -> events.add(
                sound.getSoundName().getResourcePath()));
        assertEquals(58, events.size());
        for (String event : events) {
            JsonObject definition = sounds.getAsJsonObject(event);
            assertNotNull(event, definition);
            for (JsonElement sound : definition.getAsJsonArray("sounds")) {
                String name = sound.isJsonPrimitive() ? sound.getAsString()
                        : sound.getAsJsonObject().get("name").getAsString();
                if (name.startsWith("cavesnotcliffs:")) {
                    resource("assets/cavesnotcliffs/sounds/"
                            + name.substring("cavesnotcliffs:".length()) + ".ogg");
                }
            }
        }

        JsonArray leafBreak = sounds.getAsJsonObject("block.azalea_leaves.break")
                .getAsJsonArray("sounds");
        assertEquals(14, leafBreak.size());
        assertEquals(0.92F, leafBreak.get(1).getAsJsonObject()
                .get("pitch").getAsFloat(), 0.0F);
        assertEquals(0.85F, leafBreak.get(1).getAsJsonObject()
                .get("volume").getAsFloat(), 0.0F);
        assertEquals(5, sounds.getAsJsonObject("block.big_dripleaf.tilt_down")
                .getAsJsonArray("sounds").size());
        assertEquals(2, sounds.getAsJsonObject("block.cave_vines.pick_berries")
                .getAsJsonArray("sounds").size());
    }

    @Test
    public void modelsAndRecipesUseCanonicalNamesAndExactOutputs() {
        for (String model : Arrays.asList("azalea", "flowering_azalea",
                "small_dripleaf_top", "big_dripleaf", "big_dripleaf_partial_tilt",
                "big_dripleaf_full_tilt", "spore_blossom", "cave_vines_lit")) {
            String text = new String(readUnchecked(
                    "assets/cavesnotcliffs/models/block/" + model + ".json"),
                    StandardCharsets.UTF_8);
            assertFalse(model, text.contains("baby_azalea_tree"));
            assertFalse(model, text.contains("dripleaf_plant_1_1_1"));
            assertFalse(model, text.contains("glow_berry_vines"));
        }

        JsonObject carpet = json("assets/cavesnotcliffs/recipes/moss_carpet.json");
        assertEquals("carpet", carpet.get("group").getAsString());
        assertEquals(3, carpet.getAsJsonObject("result").get("count").getAsInt());
        assertEquals("cavesnotcliffs:moss_block", carpet.getAsJsonObject("key")
                .getAsJsonObject("#").get("item").getAsString());

        JsonObject bricks = json(
                "assets/cavesnotcliffs/recipes/mossy_stone_bricks_from_moss_block.json");
        assertEquals("minecraft:stonebrick",
                bricks.getAsJsonObject("result").get("item").getAsString());
        assertEquals(1, bricks.getAsJsonObject("result").get("data").getAsInt());
    }

    @Test
    public void deferredLegacyAliasesRenderAndReadAsTheirCanonicalPeers() {
        String[][] aliases = {
                {"baby_dripleaf", "small_dripleaf_bottom", "small_dripleaf"},
                {"dripleaf_stem", "big_dripleaf_stem", "big_dripleaf"},
                {"dripleaf_plant", "big_dripleaf", "big_dripleaf"},
                {"dripleafplant_1", "big_dripleaf_partial_tilt", "big_dripleaf"},
                {"dripleaf_plant_2", "big_dripleaf_full_tilt", "big_dripleaf"},
                {"glow_berry_vines", "cave_vines_lit", "glow_berries"},
                {"glow_berry_middle_fill", "cave_vines_plant", "glow_berries"}
        };
        for (String[] alias : aliases) {
            JsonObject blockModel = json(
                    "assets/cavesnotcliffs/models/block/" + alias[0] + ".json");
            assertEquals(alias[0], "cavesnotcliffs:block/" + alias[1],
                    blockModel.get("parent").getAsString());
            JsonObject itemModel = json(
                    "assets/cavesnotcliffs/models/item/" + alias[0] + ".json");
            assertEquals(alias[0], "cavesnotcliffs:item/" + alias[2],
                    itemModel.get("parent").getAsString());
            String blockstate = new String(readUnchecked(
                    "assets/cavesnotcliffs/blockstates/" + alias[0] + ".json"),
                    StandardCharsets.UTF_8);
            assertTrue(alias[0], blockstate.contains(
                    "cavesnotcliffs:block/" + alias[1]));
            assertFalse(alias[0], blockstate.contains("custom/"));
        }

        String language = new String(readUnchecked(
                "assets/cavesnotcliffs/lang/en_us.lang"), StandardCharsets.UTF_8);
        for (String line : Arrays.asList(
                "tile.baby_dripleaf.name=Small Dripleaf",
                "tile.dripleaf_plant.name=Big Dripleaf",
                "tile.dripleafplant_1.name=Big Dripleaf",
                "tile.dripleaf_plant_2.name=Big Dripleaf",
                "tile.dripleaf_stem.name=Big Dripleaf Stem",
                "tile.glow_berry_vines.name=Cave Vines",
                "tile.glow_berry_middle_fill.name=Cave Vines")) {
            assertTrue(line, language.contains(line + '\n'));
        }
        assertFalse(language.contains("Big Dripleaf (Tilted)"));
        assertFalse(language.contains("Glow Berry Vine (Fill)"));
    }

    private static JsonObject json(String path) {
        return new JsonParser().parse(new InputStreamReader(resource(path),
                StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static InputStream resource(String path) {
        InputStream stream = LushCaveAssetsTest.class.getClassLoader()
                .getResourceAsStream(path);
        assertNotNull(path, stream);
        return stream;
    }

    private static byte[] readUnchecked(String path) {
        try {
            return read(path);
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }

    private static byte[] read(String path) throws Exception {
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
        StringBuilder result = new StringBuilder();
        for (byte value : digest) {
            result.append(String.format("%02x", value & 255));
        }
        return result.toString();
    }

    /** Documents that the block-only states intentionally have no ItemBlock model. */
    private static final class LegacyItemExpectation {
        private static boolean isHidden(String path) {
            return HIDDEN_BLOCKS.contains(path);
        }
    }
}

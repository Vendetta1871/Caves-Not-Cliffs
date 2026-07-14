package net.celestiald.cavesnotcliffs.content;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.block.SoundType;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class DeepslateSoundEventsTest {
    @BeforeClass
    public static void bootstrapMinecraftRegistries() {
        Bootstrap.register();
    }

    @Test
    public void soundTypesUseTheCanonicalFiveEventsAtUnitVolumeAndPitch() {
        assertType(DeepslateSoundEvents.DEEPSLATE, "block.deepslate");
        assertType(DeepslateSoundEvents.DEEPSLATE_BRICKS, "block.deepslate_bricks");
        assertType(DeepslateSoundEvents.DEEPSLATE_TILES, "block.deepslate_tiles");
        assertType(DeepslateSoundEvents.POLISHED_DEEPSLATE, "block.polished_deepslate");
    }

    @Test
    public void everyDeepslateFamilySelectsIts1182SoundType() {
        assertSame(DeepslateSoundEvents.DEEPSLATE,
                DeepslateSoundEvents.forBuildingBlock("cobbled_deepslate"));
        assertSame(DeepslateSoundEvents.POLISHED_DEEPSLATE,
                DeepslateSoundEvents.forBuildingBlock("polished_deepslate"));
        assertSame(DeepslateSoundEvents.DEEPSLATE_BRICKS,
                DeepslateSoundEvents.forBuildingBlock("deepslate_bricks"));
        assertSame(DeepslateSoundEvents.DEEPSLATE_BRICKS,
                DeepslateSoundEvents.forBuildingBlock("cracked_deepslate_bricks"));
        assertSame(DeepslateSoundEvents.DEEPSLATE_BRICKS,
                DeepslateSoundEvents.forBuildingBlock("chiseled_deepslate"));
        assertSame(DeepslateSoundEvents.DEEPSLATE_TILES,
                DeepslateSoundEvents.forBuildingBlock("deepslate_tiles"));
        assertSame(DeepslateSoundEvents.DEEPSLATE_TILES,
                DeepslateSoundEvents.forBuildingBlock("cracked_deepslate_tiles"));
    }

    @Test
    public void soundDefinitionReferencesOnlyPackagedAudio() {
        JsonObject sounds = resourceJson("assets/cavesnotcliffs/sounds.json");
        String[] families = {
                "block.deepslate", "block.deepslate_bricks", "block.deepslate_tiles",
                "block.polished_deepslate"
        };
        String[] actions = {"break", "fall", "hit", "place", "step"};
        for (String family : families) {
            for (String action : actions) {
                assertNotNull(family + "." + action, sounds.get(family + "." + action));
            }
        }

        for (java.util.Map.Entry<String, JsonElement> definition : sounds.entrySet()) {
            JsonArray entries = definition.getValue().getAsJsonObject().getAsJsonArray("sounds");
            for (JsonElement entry : entries) {
                String name = entry.isJsonPrimitive()
                        ? entry.getAsString()
                        : entry.getAsJsonObject().get("name").getAsString();
                if (name.startsWith("cavesnotcliffs:")) {
                    String path = "assets/cavesnotcliffs/sounds/"
                            + name.substring("cavesnotcliffs:".length()) + ".ogg";
                    assertNotNull(path, getClass().getClassLoader().getResource(path));
                }
            }
        }
    }

    @Test
    public void brickAndTileVariantsKeepMojangsPitchAndVolumeMix() {
        JsonObject sounds = resourceJson("assets/cavesnotcliffs/sounds.json");
        JsonObject brickStep = sounds.getAsJsonObject("block.deepslate_bricks.step")
                .getAsJsonArray("sounds").get(0).getAsJsonObject();
        assertEquals(0.9F, brickStep.get("pitch").getAsFloat(), 0.0F);

        JsonObject tileBreak = sounds.getAsJsonObject("block.deepslate_tiles.break")
                .getAsJsonArray("sounds").get(0).getAsJsonObject();
        assertEquals(1.3F, tileBreak.get("pitch").getAsFloat(), 0.0F);
        assertEquals(0.92F, tileBreak.get("volume").getAsFloat(), 0.0F);

        JsonObject tilePlace = sounds.getAsJsonObject("block.deepslate_tiles.place")
                .getAsJsonArray("sounds").get(0).getAsJsonObject();
        assertEquals(1.2F, tilePlace.get("pitch").getAsFloat(), 0.0F);
        assertEquals(0.85F, tilePlace.get("volume").getAsFloat(), 0.0F);
    }

    private static void assertType(SoundType type, String prefix) {
        assertEquals(1.0F, type.getVolume(), 0.0F);
        assertEquals(1.0F, type.getPitch(), 0.0F);
        assertEquals("cavesnotcliffs:" + prefix + ".break",
                type.getBreakSound().getSoundName().toString());
        assertEquals("cavesnotcliffs:" + prefix + ".step",
                type.getStepSound().getSoundName().toString());
        assertEquals("cavesnotcliffs:" + prefix + ".place",
                type.getPlaceSound().getSoundName().toString());
        assertEquals("cavesnotcliffs:" + prefix + ".hit",
                type.getHitSound().getSoundName().toString());
        assertEquals("cavesnotcliffs:" + prefix + ".fall",
                type.getFallSound().getSoundName().toString());
    }

    private static JsonObject resourceJson(String path) {
        InputStream stream = DeepslateSoundEventsTest.class.getClassLoader()
                .getResourceAsStream(path);
        assertNotNull(path, stream);
        return new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8))
                .getAsJsonObject();
    }
}

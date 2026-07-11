package net.celestiald.cavesnotcliffs.content;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.celestiald.cavesnotcliffs.block.AmethystWaterlogging;
import net.minecraft.block.SoundType;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AmethystContentResourceTest {
    @BeforeClass
    public static void bootstrapMinecraftRegistries() {
        Bootstrap.register();
    }

    @Test
    public void amethystSoundTypesUseTheOfficialEvents() {
        assertType(AmethystSoundEvents.AMETHYST, "block.amethyst_block");
        assertType(AmethystSoundEvents.AMETHYST_CLUSTER, "block.amethyst_cluster");
        assertBudType(AmethystSoundEvents.SMALL_AMETHYST_BUD,
                "block.small_amethyst_bud");
        assertBudType(AmethystSoundEvents.MEDIUM_AMETHYST_BUD,
                "block.medium_amethyst_bud");
        assertBudType(AmethystSoundEvents.LARGE_AMETHYST_BUD,
                "block.large_amethyst_bud");
        assertType(AmethystSoundEvents.BASALT, "block.basalt");
        assertEquals("cavesnotcliffs:block.amethyst_block.chime",
                AmethystSoundEvents.BLOCK_CHIME.getSoundName().toString());
    }

    @Test
    public void exactAmethystAndSpyglassSoundDefinitionsArePackaged() {
        JsonObject sounds = json("assets/cavesnotcliffs/sounds.json");
        String[] names = {
                "block.amethyst_block.break", "block.amethyst_block.chime",
                "block.amethyst_block.fall", "block.amethyst_block.hit",
                "block.amethyst_block.place", "block.amethyst_block.step",
                "block.amethyst_cluster.break", "block.amethyst_cluster.fall",
                "block.amethyst_cluster.hit", "block.amethyst_cluster.place",
                "block.amethyst_cluster.step", "block.small_amethyst_bud.break",
                "block.small_amethyst_bud.place", "block.medium_amethyst_bud.break",
                "block.medium_amethyst_bud.place", "block.large_amethyst_bud.break",
                "block.large_amethyst_bud.place", "item.spyglass.use",
                "item.spyglass.stop_using", "block.basalt.break", "block.basalt.fall",
                "block.basalt.hit", "block.basalt.place", "block.basalt.step"
        };
        for (String name : names) {
            assertNotNull(name, sounds.get(name));
        }
        JsonObject chime = sounds.getAsJsonObject("block.amethyst_block.chime")
                .getAsJsonArray("sounds").get(0).getAsJsonObject();
        assertEquals(0.2F, chime.get("volume").getAsFloat(), 0.0F);
        assertEquals(4, sounds.getAsJsonObject("item.spyglass.use")
                .getAsJsonArray("sounds").size());
        assertEquals(3, sounds.getAsJsonObject("item.spyglass.stop_using")
                .getAsJsonArray("sounds").size());
    }

    @Test
    public void recipesMatchJava1182LayoutsAndCounts() {
        JsonObject block = json("assets/cavesnotcliffs/recipes/amethyst_block.json");
        assertEquals("SS", block.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("cavesnotcliffs:amethyst_block",
                block.getAsJsonObject("result").get("item").getAsString());

        JsonObject tinted = json("assets/cavesnotcliffs/recipes/tinted_glass.json");
        assertEquals(" S ", tinted.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals("SGS", tinted.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals(2, tinted.getAsJsonObject("result").get("count").getAsInt());

        JsonObject spyglass = json("assets/cavesnotcliffs/recipes/spyglass.json");
        assertEquals(" # ", spyglass.getAsJsonArray("pattern").get(0).getAsString());
        assertEquals(" X ", spyglass.getAsJsonArray("pattern").get(1).getAsString());
        assertEquals(" X ", spyglass.getAsJsonArray("pattern").get(2).getAsString());
    }

    @Test
    public void everyNewPublicAmethystObjectHasCompleteResources() {
        ClassLoader loader = getClass().getClassLoader();
        for (String block : new String[]{
                "budding_amethyst", "large_amethyst_bud", "tinted_glass"}) {
            assertNotNull(block, loader.getResource(
                    "assets/cavesnotcliffs/blockstates/" + block + ".json"));
            assertNotNull(block, loader.getResource(
                    "assets/cavesnotcliffs/models/block/" + block + ".json"));
            assertNotNull(block, loader.getResource(
                    "assets/cavesnotcliffs/models/item/" + block + ".json"));
            assertNotNull(block, loader.getResource(
                    "assets/cavesnotcliffs/textures/blocks/" + block + ".png"));
        }
        assertNotNull(loader.getResource("assets/cavesnotcliffs/models/item/spyglass.json"));
        assertNotNull(loader.getResource("assets/cavesnotcliffs/models/block/smooth_basalt.json"));
        assertNotNull(loader.getResource("assets/cavesnotcliffs/models/item/smooth_basalt.json"));
        assertNotNull(loader.getResource("assets/cavesnotcliffs/textures/blocks/smooth_basalt.png"));
        assertNotNull(loader.getResource("assets/cavesnotcliffs/textures/items/spyglass.png"));
        assertNotNull(loader.getResource(
                "assets/cavesnotcliffs/textures/misc/spyglass_scope.png"));
    }

    @Test
    public void waterloggedStagesHaveBlockOnlyModelsAndUseTheRuntimeWaterTexture() {
        ClassLoader loader = getClass().getClassLoader();
        for (String publicStage : AmethystWaterlogging.PUBLIC_STAGES) {
            String companion = AmethystWaterlogging.companionPath(publicStage);
            assertNotNull(companion, loader.getResource(
                    "assets/cavesnotcliffs/blockstates/" + companion + ".json"));
            assertNull(companion, loader.getResource(
                    "assets/cavesnotcliffs/models/item/" + companion + ".json"));
        }
        JsonObject overlay = json(
                "assets/cavesnotcliffs/models/block/amethyst_water_overlay.json");
        assertEquals("minecraft:blocks/water_still",
                overlay.getAsJsonObject("textures").get("water").getAsString());
        assertEquals(0, overlay.getAsJsonArray("elements").get(0).getAsJsonObject()
                .getAsJsonObject("faces").getAsJsonObject("up")
                .get("tintindex").getAsInt());
    }

    @Test
    public void canonicalBudTransformsAndBasaltAssetsArePackaged() {
        JsonObject small = json(
                "assets/cavesnotcliffs/models/item/small_amethyst_bud.json");
        assertEquals("cavesnotcliffs:item/amethyst_bud",
                small.get("parent").getAsString());
        JsonObject clusterState = json(
                "assets/cavesnotcliffs/blockstates/amethyst_cluster.json");
        assertEquals(90, clusterState.getAsJsonObject("variants")
                .getAsJsonObject("facing=north").get("x").getAsInt());

        ClassLoader loader = getClass().getClassLoader();
        for (String sound : new String[]{
                "break1", "break2", "break3", "break4", "break5",
                "step1", "step2", "step3", "step4", "step5", "step6"}) {
            assertNotNull(sound, loader.getResource(
                    "assets/cavesnotcliffs/sounds/block/basalt/" + sound + ".ogg"));
        }
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

    private static void assertBudType(SoundType type, String prefix) {
        assertEquals("cavesnotcliffs:" + prefix + ".break",
                type.getBreakSound().getSoundName().toString());
        assertEquals("cavesnotcliffs:block.amethyst_cluster.step",
                type.getStepSound().getSoundName().toString());
        assertEquals("cavesnotcliffs:" + prefix + ".place",
                type.getPlaceSound().getSoundName().toString());
        assertEquals("cavesnotcliffs:block.amethyst_cluster.hit",
                type.getHitSound().getSoundName().toString());
        assertEquals("cavesnotcliffs:block.amethyst_cluster.fall",
                type.getFallSound().getSoundName().toString());
    }

    private static JsonObject json(String path) {
        InputStream stream = AmethystContentResourceTest.class.getClassLoader()
                .getResourceAsStream(path);
        assertNotNull(path, stream);
        return new JsonParser().parse(
                new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}

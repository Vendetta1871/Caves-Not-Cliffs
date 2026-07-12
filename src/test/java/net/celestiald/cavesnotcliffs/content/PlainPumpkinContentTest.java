package net.celestiald.cavesnotcliffs.content;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.BlockPumpkin;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.stats.StatList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class PlainPumpkinContentTest {
    private static final String ROOT = "assets/cavesnotcliffs/";
    private static final String MINECRAFT_ROOT = "assets/minecraft/";

    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void publicPeerIsStatelessAndUsesExactPhysicalProperties() {
        PlainPumpkinContent.PlainPumpkinBlock block = PlainPumpkinContent.createBlock();
        ItemBlock item = PlainPumpkinContent.createItem(block);
        IBlockState state = block.getDefaultState();

        assertEquals("cavesnotcliffs:pumpkin", CncRegistryIds.PUMPKIN.toString());
        assertEquals(CncRegistryIds.PUMPKIN, block.getRegistryName());
        assertEquals(CncRegistryIds.PUMPKIN, item.getRegistryName());
        assertSame(block, item.getBlock());
        assertEquals("tile.plain_pumpkin", block.getUnlocalizedName());
        assertTrue(block.getBlockState().getProperties().isEmpty());
        assertEquals(1, block.getBlockState().getValidStates().size());
        assertSame(Material.GOURD, block.getMaterial(state));
        assertSame(MapColor.ADOBE, block.getMapColor(state, null, BlockPos.ORIGIN));
        assertSame(SoundType.WOOD,
                block.getSoundType(state, null, BlockPos.ORIGIN, null));
        assertEquals(1.0F,
                block.getBlockHardness(state, null, BlockPos.ORIGIN), 0.0F);
        assertSame(EnumPushReaction.DESTROY, block.getMobilityFlag(state));
        assertTrue(block.isToolEffective("axe", state));
        assertTrue(EntityEnderman.getCarriable(block));
        assertEquals(0, block.getMetaFromState(state));
        assertSame(state, block.getStateFromMeta(15));
    }

    @Test
    public void carvingDirectionAndSeedMotionMatchJava1182() {
        assertEquals(11, PlainPumpkinContent.CARVED_UPDATE_FLAGS);
        assertEquals(4, PlainPumpkinContent.CARVE_SEED_COUNT);
        assertEquals(1, PlainPumpkinContent.SHEARS_DAMAGE);
        assertEquals(0.1D, PlainPumpkinContent.SEED_Y_OFFSET, 0.0D);
        assertEquals(0.05D, PlainPumpkinContent.SEED_Y_MOTION, 0.0D);
        assertSame(EnumFacing.EAST,
                PlainPumpkinContent.carvedFacing(EnumFacing.EAST, EnumFacing.NORTH));
        assertSame(EnumFacing.SOUTH,
                PlainPumpkinContent.carvedFacing(EnumFacing.UP, EnumFacing.NORTH));
        assertSame(EnumFacing.WEST,
                PlainPumpkinContent.carvedFacing(EnumFacing.DOWN, EnumFacing.EAST));
        IBlockState carved = PlainPumpkinContent.carvedState(EnumFacing.WEST);
        assertSame(Blocks.PUMPKIN, carved.getBlock());
        assertSame(EnumFacing.WEST, carved.getValue(BlockPumpkin.FACING));
        assertNotNull(StatList.getObjectUseStats(Items.SHEARS));

        assertEquals(10.5D,
                PlainPumpkinContent.seedHorizontalPosition(10, 0), 0.0D);
        assertEquals(11.15D,
                PlainPumpkinContent.seedHorizontalPosition(10, 1), 0.0D);
        assertEquals(9.85D,
                PlainPumpkinContent.seedHorizontalPosition(10, -1), 0.0D);
        assertEquals(0.055D,
                PlainPumpkinContent.seedHorizontalMotion(1, 0.25D), 1.0E-12D);
        assertEquals(0.005D,
                PlainPumpkinContent.seedHorizontalMotion(0, 0.25D), 1.0E-12D);
        assertEquals(-0.045D,
                PlainPumpkinContent.seedHorizontalMotion(-1, 0.25D), 1.0E-12D);
        assertEquals("cavesnotcliffs:block.pumpkin.carve",
                PlainPumpkinContent.PUMPKIN_CARVE.getRegistryName().toString());
    }

    @Test
    public void plainAndCarvedPumpkinsKeepTheirVanillaCompostChance() {
        assertEquals(0.65F, ComposterCompostables.chance(
                CncRegistryIds.PUMPKIN, 0), 0.0F);
        assertEquals(0.65F, ComposterCompostables.chance(
                new ResourceLocation("minecraft:pumpkin"), 0), 0.0F);
    }

    @Test
    public void canonicalModelsSoundsAndAssetsAreComplete() throws Exception {
        JsonObject state = json("blockstates/pumpkin.json");
        assertEquals("cavesnotcliffs:pumpkin", state.getAsJsonObject("variants")
                .getAsJsonObject("normal").get("model").getAsString());
        JsonObject model = json("models/block/pumpkin.json");
        assertEquals("block/cube_column", model.get("parent").getAsString());
        assertEquals("cavesnotcliffs:blocks/pumpkin_top",
                model.getAsJsonObject("textures").get("end").getAsString());
        assertEquals("cavesnotcliffs:block/pumpkin",
                json("models/item/pumpkin.json").get("parent").getAsString());

        JsonObject carve = json("sounds.json").getAsJsonObject("block.pumpkin.carve");
        assertEquals(2, carve.getAsJsonArray("sounds").size());
        assertEquals("subtitles.block.pumpkin.carve",
                carve.get("subtitle").getAsString());

        assertEquals("0561abfa3282a08b86e0c84bb5c799172ca24490",
                sha1("textures/blocks/pumpkin_side.png"));
        assertEquals("cdc099ca26227975d32d09dbf82c52c33d6e0c57",
                sha1("textures/blocks/pumpkin_top.png"));
        assertEquals("e528a3ccb3b90e3a2eb075b69a3663635b05d1d6",
                sha1("sounds/block/pumpkin/carve1.ogg"));
        assertEquals("1997fd410166b4e52ede02d23fb8947306f8630d",
                sha1("sounds/block/pumpkin/carve2.ogg"));

        assertEquals("0561abfa3282a08b86e0c84bb5c799172ca24490",
                sha1Resource(MINECRAFT_ROOT + "textures/blocks/pumpkin_side.png"));
        assertEquals("cdc099ca26227975d32d09dbf82c52c33d6e0c57",
                sha1Resource(MINECRAFT_ROOT + "textures/blocks/pumpkin_top.png"));
        assertEquals("3f436abe370a2236e74aba4e27f8fa8c64acfef4",
                sha1Resource(MINECRAFT_ROOT + "textures/blocks/pumpkin_face_off.png"));
        assertEquals("a2da548d7f93f99b6e736cdf07a33dee779de016",
                sha1Resource(MINECRAFT_ROOT + "textures/blocks/pumpkin_face_on.png"));
    }

    private static JsonObject json(String path) {
        return new JsonParser().parse(new InputStreamReader(resource(ROOT + path),
                StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static String sha1(String path) throws Exception {
        return sha1Resource(ROOT + path);
    }

    private static String sha1Resource(String path) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-1").digest(read(path));
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
        InputStream input = PlainPumpkinContentTest.class.getClassLoader()
                .getResourceAsStream(path);
        assertNotNull(path, input);
        return input;
    }
}

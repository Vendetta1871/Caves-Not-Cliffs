package net.celestiald.cavesnotcliffs.migration;

import net.celestiald.cavesnotcliffs.world.CavesNotCliffsWorldData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CubicColumnConverterTest {
    private static final int CHUNK_X = -2;
    private static final int CHUNK_Z = 9;

    @Test
    public void mapsCompleteSchemaTwoColumnWithoutLosingVerticalPayloads() throws Exception {
        NBTTagCompound column = column();
        Map<Integer, NBTTagCompound> cubes = completeCubes(-4, 20, true);
        cube(cubes, -3).getCompoundTag("Level").setTag("Entities",
                list(entity(-31.5D, -47.0D, 151.5D, 11L, 22L)));
        cube(cubes, 4).getCompoundTag("Level").setTag("TileEntities",
                list(positioned(-17, 66, 153)));
        cube(cubes, 1).getCompoundTag("Level").setTag("TileTicks",
                list(positioned(-18, 17, 158)));
        NBTTagCompound section = cube(cubes, 7).getCompoundTag("Level")
                .getTagList("Sections", 10).getCompoundTagAt(0);
        section.setByteArray("Add2", new byte[2048]);
        cube(cubes, 5).getCompoundTag("cavesnotcliffs").setInteger("contentVersion", 2);

        NBTTagCompound converted = CubicColumnConverter.convertOverworld(
                column, cubes, CavesNotCliffsWorldData.CURRENT_SCHEMA, 91234L);
        NBTTagCompound level = converted.getCompoundTag("Level");

        assertEquals(1343, converted.getInteger("DataVersion"));
        assertTrue(converted.hasKey("ForgeDataVersion", 10));
        assertEquals(CHUNK_X, level.getInteger("xPos"));
        assertEquals(CHUNK_Z, level.getInteger("zPos"));
        assertEquals(91234L, level.getLong("LastUpdate"));
        assertEquals(77L, level.getLong("InhabitedTime"));
        assertTrue(level.getBoolean("TerrainPopulated"));
        assertFalse(level.getBoolean("LightPopulated"));
        assertArrayEquals(column.getCompoundTag("Level").getByteArray("Biomes"),
                level.getByteArray("Biomes"));
        assertEquals(101, level.getIntArray("HeightMap")[0]);
        assertEquals(103, level.getIntArray("HeightMap")[2]);

        NBTTagList sections = level.getTagList("Sections", 10);
        assertEquals(24, sections.tagCount());
        assertEquals(-4, sections.getCompoundTagAt(0).getByte("Y"));
        assertEquals(19, sections.getCompoundTagAt(23).getByte("Y"));
        assertFalse(sections.getCompoundTagAt(11).hasKey("Add2"));
        assertTrue(section.hasKey("Add2"));
        assertEquals(1, level.getTagList("Entities", 10).tagCount());
        assertEquals(1, level.getTagList("TileEntities", 10).tagCount());
        assertEquals(1, level.getTagList("TileTicks", 10).tagCount());
        assertEquals(2, converted.getCompoundTag("cavesnotcliffs")
                .getInteger("contentVersion"));
        assertEquals(1, converted.getInteger("CavesNotCliffsCauldronBridge"));
    }

    @Test
    public void schemaOneSynthesizesOnlyDocumentedEmptyTopSections() throws Exception {
        Map<Integer, NBTTagCompound> cubes = completeCubes(-4, 16, false);
        NBTTagCompound converted = CubicColumnConverter.convertOverworld(
                column(), cubes, CavesNotCliffsWorldData.LEGACY_SCHEMA, 0L);
        assertEquals(20, converted.getCompoundTag("Level")
                .getTagList("Sections", 10).tagCount());
        assertFalse(converted.getCompoundTag("Level").getBoolean("TerrainPopulated"));
    }

    @Test
    public void rejectsMixedSchemaOnePopulationState() throws Exception {
        Map<Integer, NBTTagCompound> cubes = completeCubes(-4, 16, true);
        cube(cubes, 1).getCompoundTag("Level").setBoolean("populated", false);
        expectFailure("mixed schema-1 population", column(), cubes,
                CavesNotCliffsWorldData.LEGACY_SCHEMA);
    }

    @Test
    public void rejectsMissingAuthoritativeCube() throws Exception {
        Map<Integer, NBTTagCompound> cubes = completeCubes(-4, 20, true);
        cubes.remove(12);
        expectFailure("missing authoritative cube Y=12", column(), cubes,
                CavesNotCliffsWorldData.CURRENT_SCHEMA);
    }

    @Test
    public void rejectsNonzeroAddTwoInsteadOfTruncatingStateIds() throws Exception {
        Map<Integer, NBTTagCompound> cubes = completeCubes(-4, 20, true);
        byte[] add2 = new byte[2048];
        add2[41] = 1;
        cube(cubes, -1).getCompoundTag("Level").getTagList("Sections", 10)
                .getCompoundTagAt(0).setByteArray("Add2", add2);
        expectFailure("Add2 block-state IDs", column(), cubes,
                CavesNotCliffsWorldData.CURRENT_SCHEMA);
    }

    @Test
    public void rejectsDuplicateTileEntityPositions() throws Exception {
        Map<Integer, NBTTagCompound> cubes = completeCubes(-4, 20, true);
        NBTTagCompound tile = positioned(-17, 66, 153);
        cube(cubes, 4).getCompoundTag("Level").setTag("TileEntities",
                list(tile, tile.copy()));
        expectFailure("duplicate tile entity", column(), cubes,
                CavesNotCliffsWorldData.CURRENT_SCHEMA);
    }

    @Test
    public void rejectsEntityStoredInTheWrongCube() throws Exception {
        Map<Integer, NBTTagCompound> cubes = completeCubes(-4, 20, true);
        cube(cubes, -3).getCompoundTag("Level").setTag("Entities",
                list(entity(-31.5D, 70.0D, 151.5D, 11L, 22L)));
        expectFailure("does not belong to cube Y=-3", column(), cubes,
                CavesNotCliffsWorldData.CURRENT_SCHEMA);
    }

    @Test
    public void permitsOnlyTrulyEmptyOutOfRangeLookaheadCubes() throws Exception {
        Map<Integer, NBTTagCompound> cubes = completeCubes(-4, 20, true);
        NBTTagCompound lookahead = cubeRoot(-6, false);
        lookahead.getCompoundTag("Level").removeTag("Sections");
        cubes.put(-6, lookahead);
        CubicColumnConverter.convertOverworld(
                column(), cubes, CavesNotCliffsWorldData.CURRENT_SCHEMA, 0L);

        lookahead.getCompoundTag("Level").setTag("TileEntities",
                list(positioned(-17, -95, 153)));
        expectFailure("nonempty cube Y=-6", column(), cubes,
                CavesNotCliffsWorldData.CURRENT_SCHEMA);
    }

    @Test
    public void rejectsUnknownPerCubeEventData() throws Exception {
        Map<Integer, NBTTagCompound> cubes = completeCubes(-4, 20, true);
        cube(cubes, 0).setString("thirdPartyCubePayload", "must not disappear");
        expectFailure("unsupported data tag 'thirdPartyCubePayload'", column(), cubes,
                CavesNotCliffsWorldData.CURRENT_SCHEMA);
    }

    @Test
    public void rejectsTruncatedOpacityIndex() throws Exception {
        NBTTagCompound column = column();
        column.getCompoundTag("Level").setByteArray("OpacityIndex", new byte[9]);
        expectFailure("truncated OpacityIndex", column, completeCubes(-4, 20, true),
                CavesNotCliffsWorldData.CURRENT_SCHEMA);
    }

    @Test
    public void convertsCoherentVanillaCompatibilityDimension() throws Exception {
        Map<Integer, NBTTagCompound> cubes = completeCubes(0, 16, true);
        for (NBTTagCompound root : cubes.values()) {
            root.removeTag("cavesnotcliffs");
            root.removeTag("CavesNotCliffsCauldronBridge");
            root.getCompoundTag("Level").removeTag("Biomes3D");
        }
        NBTTagCompound converted = CubicColumnConverter.convertVanillaDimension(
                column(), cubes, 15L);
        assertEquals(16, converted.getCompoundTag("Level")
                .getTagList("Sections", 10).tagCount());
        assertTrue(converted.getCompoundTag("Level").getBoolean("TerrainPopulated"));
        assertFalse(converted.hasKey("cavesnotcliffs"));
        assertFalse(converted.hasKey("CavesNotCliffsCauldronBridge"));
    }

    @Test
    public void rejectsVanillaDimensionThreeDimensionalBiomes() throws Exception {
        Map<Integer, NBTTagCompound> cubes = completeCubes(0, 16, true);
        expectVanillaFailure("3D biome data", column(), cubes);
    }

    @Test
    public void rejectsMixedVanillaDimensionPopulationState() throws Exception {
        Map<Integer, NBTTagCompound> cubes = completeCubes(0, 16, true);
        for (NBTTagCompound root : cubes.values()) {
            root.getCompoundTag("Level").removeTag("Biomes3D");
        }
        cube(cubes, 8).getCompoundTag("Level").setBoolean("populated", false);
        expectVanillaFailure("mixed vanilla-dimension population", column(), cubes);
    }

    @Test
    public void validatesCubeOnlyLookaheadColumnsIndependently() throws Exception {
        Map<Integer, NBTTagCompound> cubes = new TreeMap<Integer, NBTTagCompound>();
        NBTTagCompound empty = cubeRoot(-6, false);
        empty.getCompoundTag("Level").removeTag("Sections");
        cubes.put(-6, empty);
        CubicColumnConverter.validateDiscardableLookahead(cubes);

        empty.getCompoundTag("Level").setTag("Entities",
                list(entity(-17.5D, -95.0D, 153.5D, 7L, 8L)));
        try {
            CubicColumnConverter.validateDiscardableLookahead(cubes);
            fail("Expected stateful lookahead rejection");
        } catch (CubicColumnConversionException expected) {
            assertTrue(expected.getMessage().contains("stateful cube Y=-6"));
        }
    }

    private static NBTTagCompound column() throws IOException {
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("DataVersion", 0);
        NBTTagCompound forge = new NBTTagCompound();
        forge.setInteger("minecraft", 1343);
        root.setTag("ForgeDataVersion", forge);
        NBTTagCompound level = new NBTTagCompound();
        level.setByte("v", (byte) 1);
        level.setInteger("x", CHUNK_X);
        level.setInteger("z", CHUNK_Z);
        level.setLong("InhabitedTime", 77L);
        byte[] biomes = new byte[256];
        for (int index = 0; index < biomes.length; index++) {
            biomes[index] = (byte) index;
        }
        level.setByteArray("Biomes", biomes);
        level.setByteArray("OpacityIndex", opacityIndex());
        NBTTagCompound caps = new NBTTagCompound();
        caps.setInteger("columnCapability", 9);
        level.setTag("ForgeCaps", caps);
        root.setTag("Level", level);
        return root;
    }

    private static byte[] opacityIndex() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        for (int index = 0; index < 256; index++) {
            output.writeInt(-64);
            output.writeInt(100 + index % 3);
            output.writeShort(0);
        }
        output.close();
        return bytes.toByteArray();
    }

    private static Map<Integer, NBTTagCompound> completeCubes(
            int minY, int maxY, boolean populated) {
        Map<Integer, NBTTagCompound> cubes = new TreeMap<Integer, NBTTagCompound>();
        for (int cubeY = minY; cubeY < maxY; cubeY++) {
            cubes.put(cubeY, cubeRoot(cubeY, populated));
        }
        return cubes;
    }

    private static NBTTagCompound cubeRoot(int cubeY, boolean populated) {
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("DataVersion", 0);
        NBTTagCompound forge = new NBTTagCompound();
        forge.setInteger("minecraft", 1343);
        root.setTag("ForgeDataVersion", forge);
        NBTTagCompound content = new NBTTagCompound();
        content.setInteger("contentVersion", 3);
        root.setTag("cavesnotcliffs", content);
        root.setInteger("CavesNotCliffsCauldronBridge", 1);

        NBTTagCompound level = new NBTTagCompound();
        level.setByte("v", (byte) 1);
        level.setInteger("x", CHUNK_X);
        level.setInteger("y", cubeY);
        level.setInteger("z", CHUNK_Z);
        level.setBoolean("populated", populated);
        level.setBoolean("isSurfaceTracked", true);
        level.setBoolean("fullyPopulated", populated);
        level.setBoolean("initLightDone", true);
        NBTTagCompound section = new NBTTagCompound();
        byte[] blocks = new byte[4096];
        blocks[0] = 1;
        section.setByteArray("Blocks", blocks);
        section.setByteArray("Data", new byte[2048]);
        section.setByteArray("BlockLight", new byte[2048]);
        section.setByteArray("SkyLight", new byte[2048]);
        level.setTag("Sections", list(section));
        level.setTag("Entities", new NBTTagList());
        level.setTag("TileEntities", new NBTTagList());
        level.setTag("TileTicks", new NBTTagList());
        level.setString("LightingInfoType", "cubicchunks:lighting");
        level.setTag("LightingInfo", new NBTTagCompound());
        level.setByteArray("Biomes3D", new byte[64]);
        root.setTag("Level", level);
        return root;
    }

    private static NBTTagCompound cube(Map<Integer, NBTTagCompound> cubes, int cubeY) {
        return cubes.get(cubeY);
    }

    private static NBTTagCompound entity(double x, double y, double z, long most, long least) {
        NBTTagCompound entity = new NBTTagCompound();
        entity.setString("id", "minecraft:armor_stand");
        entity.setLong("UUIDMost", most);
        entity.setLong("UUIDLeast", least);
        NBTTagList pos = new NBTTagList();
        pos.appendTag(new NBTTagDouble(x));
        pos.appendTag(new NBTTagDouble(y));
        pos.appendTag(new NBTTagDouble(z));
        entity.setTag("Pos", pos);
        return entity;
    }

    private static NBTTagCompound positioned(int x, int y, int z) {
        NBTTagCompound value = new NBTTagCompound();
        value.setInteger("x", x);
        value.setInteger("y", y);
        value.setInteger("z", z);
        return value;
    }

    private static NBTTagList list(NBTTagCompound... values) {
        NBTTagList result = new NBTTagList();
        for (NBTTagCompound value : values) {
            result.appendTag(value);
        }
        return result;
    }

    private static void expectFailure(String message, NBTTagCompound column,
            Map<Integer, NBTTagCompound> cubes, int schema) throws Exception {
        try {
            CubicColumnConverter.convertOverworld(column, cubes, schema, 0L);
            fail("Expected conversion failure containing: " + message);
        } catch (CubicColumnConversionException expected) {
            assertTrue("actual message: " + expected.getMessage(),
                    expected.getMessage().contains(message));
        }
    }

    private static void expectVanillaFailure(String message, NBTTagCompound column,
            Map<Integer, NBTTagCompound> cubes) throws Exception {
        try {
            CubicColumnConverter.convertVanillaDimension(column, cubes, 0L);
            fail("Expected vanilla-dimension conversion failure containing: " + message);
        } catch (CubicColumnConversionException expected) {
            assertTrue("actual message: " + expected.getMessage(),
                    expected.getMessage().contains(message));
        }
    }
}

package net.celestiald.cavesnotcliffs.migration;

import net.celestiald.cavesnotcliffs.world.CavesNotCliffsWorldData;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.chunk.storage.RegionFileCache;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CubicDimensionStagerTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void stagesVerifiesAndFingerprintsACompleteColumn() throws Exception {
        Path source = temporary.newFolder("source").toPath();
        writeExtension(source.resolve("region2d/0.0.2dr.ext"), 0, column(0, 0));
        for (int cubeY = -4; cubeY < 20; cubeY++) {
            writeCube(source, 0, cubeY, 0, cube(0, cubeY, 0, true, true));
        }
        // CubicChunks can persist unpopulated generator lookahead without a column record. It is
        // deliberately scanned and discarded only after the converter proves it has no state.
        writeCube(source, 1, 0, 0, cube(1, 0, 0, false, false));

        Path staging = temporary.getRoot().toPath().resolve("stage");
        CubicDimensionStager.Result result = CubicDimensionStager.stage(source, staging,
                true, CavesNotCliffsWorldData.CURRENT_SCHEMA, 1234L);

        assertEquals(1L, result.getColumns());
        assertEquals(25L, result.getCubes());
        assertEquals(1L, result.getDiscardedLookaheadColumns());
        assertTrue(result.hasOutputRegion());
        assertFalse(result.getOutputs().isEmpty());
        assertTrue(Files.isRegularFile(staging.resolve("region/r.0.0.mca")));

        DataInputStream input = RegionFileCache.getChunkInputStream(staging.toFile(), 0, 0);
        NBTTagCompound output;
        try (DataInputStream stream = input) {
            output = CompressedStreamTools.read(stream);
        } finally {
            RegionFileCache.clearRegionFileReferences();
        }
        assertEquals(1343, output.getInteger("DataVersion"));
        assertEquals(24, output.getCompoundTag("Level").getTagList("Sections", 10).tagCount());
        assertEquals(1234L, output.getCompoundTag("Level").getLong("LastUpdate"));
    }

    @Test
    public void refusesStatefulCubeOnlyColumns() throws Exception {
        Path source = temporary.newFolder("stateful-source").toPath();
        NBTTagCompound stateful = cube(2, 0, 3, false, false);
        NBTTagCompound tile = new NBTTagCompound();
        tile.setInteger("x", 32);
        tile.setInteger("y", 1);
        tile.setInteger("z", 48);
        stateful.getCompoundTag("Level").setTag("TileEntities", list(tile));
        writeCube(source, 2, 0, 3, stateful);

        try {
            CubicDimensionStager.stage(source,
                    temporary.getRoot().toPath().resolve("stateful-stage"),
                    true, CavesNotCliffsWorldData.CURRENT_SCHEMA, 0L);
            fail("Expected stateful lookahead rejection");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("cube-only column"));
            assertTrue(expected.getMessage().contains("stateful cube Y=0"));
        }
    }

    private static NBTTagCompound column(int x, int z) throws IOException {
        NBTTagCompound root = new NBTTagCompound();
        root.setInteger("DataVersion", 0);
        NBTTagCompound forge = new NBTTagCompound();
        forge.setInteger("minecraft", 1343);
        root.setTag("ForgeDataVersion", forge);
        NBTTagCompound level = new NBTTagCompound();
        level.setByte("v", (byte) 1);
        level.setInteger("x", x);
        level.setInteger("z", z);
        level.setLong("InhabitedTime", 5L);
        level.setByteArray("Biomes", new byte[256]);
        ByteArrayOutputStream opacityBytes = new ByteArrayOutputStream();
        DataOutputStream opacity = new DataOutputStream(opacityBytes);
        for (int index = 0; index < 256; index++) {
            opacity.writeInt(-64);
            opacity.writeInt(100);
            opacity.writeShort(0);
        }
        opacity.close();
        level.setByteArray("OpacityIndex", opacityBytes.toByteArray());
        root.setTag("Level", level);
        return root;
    }

    private static NBTTagCompound cube(int x, int y, int z,
            boolean populated, boolean withSection) {
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
        level.setInteger("x", x);
        level.setInteger("y", y);
        level.setInteger("z", z);
        level.setBoolean("populated", populated);
        level.setBoolean("isSurfaceTracked", populated);
        level.setBoolean("fullyPopulated", populated);
        level.setBoolean("initLightDone", populated);
        if (withSection) {
            NBTTagCompound section = new NBTTagCompound();
            byte[] blocks = new byte[4096];
            blocks[0] = 1;
            section.setByteArray("Blocks", blocks);
            section.setByteArray("Data", new byte[2048]);
            section.setByteArray("BlockLight", new byte[2048]);
            section.setByteArray("SkyLight", new byte[2048]);
            level.setTag("Sections", list(section));
        }
        level.setTag("Entities", new NBTTagList());
        level.setTag("TileEntities", new NBTTagList());
        level.setTag("TileTicks", new NBTTagList());
        level.setString("LightingInfoType", "cubicchunks:lighting");
        level.setTag("LightingInfo", new NBTTagCompound());
        level.setByteArray("Biomes3D", new byte[64]);
        root.setTag("Level", level);
        return root;
    }

    private static void writeCube(Path source, int x, int y, int z,
            NBTTagCompound root) throws IOException {
        int regionX = x >> 4;
        int regionY = y >> 4;
        int regionZ = z >> 4;
        int entryId = (x & 15) << 8 | (y & 15) << 4 | (z & 15);
        Path extension = source.resolve("region3d/" + regionX + "." + regionY + "."
                + regionZ + ".3dr.ext");
        writeExtension(extension, entryId, root);
    }

    private static void writeExtension(Path directory, int entryId, NBTTagCompound root)
            throws IOException {
        Files.createDirectories(directory);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(root, output);
        Files.write(directory.resolve(Integer.toString(entryId)), output.toByteArray());
    }

    private static NBTTagList list(NBTTagCompound... values) {
        NBTTagList result = new NBTTagList();
        for (NBTTagCompound value : values) {
            result.appendTag(value);
        }
        return result;
    }
}

package net.celestiald.cavesnotcliffs.migration;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LegacyCubicStructureMetadataTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void acceptsAuthenticStructuresAndVillagesAndReturnsSortedSources()
            throws Exception {
        Path world = temporary.newFolder("authentic").toPath();
        NBTTagCompound first = structureRoot(feature(-22, 33, 0,
                box(-390, 12, 455, -277, 31, 621),
                box(-384, -64, 464, -370, 19, 470)));
        NBTTagCompound second = structureRoot(feature(-5, 27, 0,
                box(-117, 15, 389, -19, 30, 522),
                box(-100, 20, 400, -90, 319, 410)));
        Path stronghold = write(world, "Stronghold.dat", first);
        Path mineshaft = write(world, "Mineshaft.dat", second);
        Path villages = write(world, "villages.dat", villageRoot(-64, 319, -20));

        LegacyCubicStructureMetadata.Result result =
                LegacyCubicStructureMetadata.validate(Arrays.asList(world, world));

        assertEquals(Arrays.asList(mineshaft, stronghold, villages), result.getSourceFiles());
        try {
            result.getSourceFiles().clear();
            fail("Expected immutable source list");
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }
    }

    @Test
    public void acceptsEmptyAuthenticFeatureAndVillageContainers() throws Exception {
        Path world = temporary.newFolder("empty").toPath();
        write(world, "Mansion.dat", structureRoot());
        write(world, "villages.dat", villageRoot());

        LegacyCubicStructureMetadata.Result result =
                LegacyCubicStructureMetadata.validate(Collections.singletonList(world));

        assertEquals(2, result.getSourceFiles().size());
    }

    @Test
    public void rejectsNonzeroOrMalformedCubicChunkY() throws Exception {
        assertStructureFailure(feature(4, -7, 1,
                box(64, 0, -112, 79, 20, -97)), "ChunkY=1");

        NBTTagCompound feature = feature(4, -7, 0,
                box(64, 0, -112, 79, 20, -97));
        feature.setString("ChunkY", "0");
        assertStructureFailure(feature, "no integer ChunkY");
    }

    @Test
    public void rejectsStartAndChildBoundingBoxesOutsideFiniteRange() throws Exception {
        assertStructureFailure(feature(0, 0, 0,
                box(0, -65, 0, 15, 20, 15)), "outside -64..319");
        assertStructureFailure(feature(0, 0, 0,
                box(0, 10, 0, 15, 320, 15)), "outside -64..319");
        assertStructureFailure(feature(0, 0, 0,
                box(0, 0, 0, 15, 20, 15),
                box(1, 4, 1, 2, 320, 2)), "child 0 maximum BB Y=320");
    }

    @Test
    public void rejectsMalformedBoundingBoxesAndChildLists() throws Exception {
        NBTTagCompound shortBox = feature(0, 0, 0,
                box(0, 0, 0, 15, 20, 15));
        shortBox.setIntArray("BB", new int[] {0, 0, 0, 15, 20});
        assertStructureFailure(shortBox, "bounding box of length 5");

        assertStructureFailure(feature(0, 0, 0,
                box(15, 20, 15, 0, 0, 0)), "inverted bounding box");

        NBTTagCompound wrongChildren = feature(0, 0, 0,
                box(0, 0, 0, 15, 20, 15));
        NBTTagList strings = new NBTTagList();
        strings.appendTag(new NBTTagString("not-a-child"));
        wrongChildren.setTag("Children", strings);
        assertStructureFailure(wrongChildren, "Children does not contain compounds");
    }

    @Test
    public void rejectsNoncanonicalOrMismatchedFeatureCoordinates() throws Exception {
        Path world = temporary.newFolder("feature-key").toPath();
        NBTTagCompound data = new NBTTagCompound();
        NBTTagCompound features = new NBTTagCompound();
        features.setTag("[01,0]", feature(1, 0, 0,
                box(16, 0, 0, 31, 20, 15)));
        data.setTag("Features", features);
        NBTTagCompound root = new NBTTagCompound();
        root.setTag("data", data);
        write(world, "Temple.dat", root);
        assertFailure(world, "non-canonical coordinate key");

        world = temporary.newFolder("feature-mismatch").toPath();
        data = new NBTTagCompound();
        features = new NBTTagCompound();
        features.setTag("[1,0]", feature(2, 0, 0,
                box(16, 0, 0, 31, 20, 15)));
        data.setTag("Features", features);
        root = new NBTTagCompound();
        root.setTag("data", data);
        write(world, "Temple.dat", root);
        assertFailure(world, "does not match ChunkX/ChunkZ");
    }

    @Test
    public void rejectsVillageCentersAveragesAndDoorsOutsideFiniteRange() throws Exception {
        assertVillageFailure(villageRoot(-65, 0, 0), "CY=-65");
        assertVillageFailure(villageRoot(0, 320, 0), "ACY=320");
        assertVillageFailure(villageRoot(0, 0, -65), "door 0 Y=-65");
        assertVillageFailure(villageRoot(0, 0, 320), "door 0 Y=320");
    }

    @Test
    public void ignoresUnrelatedSavedDataButRejectsRecognizedNonFiles() throws Exception {
        Path world = temporary.newFolder("unrelated").toPath();
        Path data = Files.createDirectories(world.resolve("data"));
        Files.write(data.resolve("map_0.dat"), new byte[] {1, 2, 3});
        Files.write(data.resolve("capabilities.dat"), new byte[] {4, 5, 6});
        assertTrue(LegacyCubicStructureMetadata.validate(
                Collections.singletonList(world)).getSourceFiles().isEmpty());

        Files.createDirectory(data.resolve("Mineshaft.dat"));
        assertFailure(world, "not a regular file");
    }

    private void assertStructureFailure(NBTTagCompound feature, String message)
            throws Exception {
        Path world = temporary.newFolder("bad-structure-" + System.nanoTime()).toPath();
        write(world, "Mineshaft.dat", structureRoot(feature));
        assertFailure(world, message);
    }

    private void assertVillageFailure(NBTTagCompound root, String message) throws Exception {
        Path world = temporary.newFolder("bad-village-" + System.nanoTime()).toPath();
        write(world, "villages.dat", root);
        assertFailure(world, message);
    }

    private static void assertFailure(Path world, String message) throws Exception {
        try {
            LegacyCubicStructureMetadata.validate(Collections.singletonList(world));
            fail("Expected structure metadata rejection containing: " + message);
        } catch (IOException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains(message));
        }
    }

    private static NBTTagCompound structureRoot(NBTTagCompound... starts) {
        NBTTagCompound features = new NBTTagCompound();
        for (NBTTagCompound start : starts) {
            features.setTag('[' + Integer.toString(start.getInteger("ChunkX")) + ','
                    + start.getInteger("ChunkZ") + ']', start);
        }
        NBTTagCompound data = new NBTTagCompound();
        data.setTag("Features", features);
        NBTTagCompound root = new NBTTagCompound();
        root.setTag("data", data);
        return root;
    }

    private static NBTTagCompound feature(int chunkX, int chunkZ, int chunkY,
            int[] startBox, int[]... childBoxes) {
        NBTTagCompound start = new NBTTagCompound();
        start.setIntArray("BB", startBox);
        start.setInteger("ChunkX", chunkX);
        start.setInteger("ChunkY", chunkY);
        start.setInteger("ChunkZ", chunkZ);
        start.setString("id", "Mineshaft");
        NBTTagList children = new NBTTagList();
        for (int[] childBox : childBoxes) {
            NBTTagCompound child = new NBTTagCompound();
            child.setIntArray("BB", childBox);
            child.setString("id", "MSCorridor");
            children.appendTag(child);
        }
        start.setTag("Children", children);
        return start;
    }

    private static int[] box(int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ) {
        return new int[] {minX, minY, minZ, maxX, maxY, maxZ};
    }

    private static NBTTagCompound villageRoot(int... ys) {
        NBTTagList villages = new NBTTagList();
        if (ys.length > 0) {
            NBTTagCompound village = new NBTTagCompound();
            village.setInteger("CY", ys[0]);
            village.setInteger("ACY", ys[1]);
            NBTTagCompound door = new NBTTagCompound();
            door.setInteger("Y", ys[2]);
            NBTTagList doors = new NBTTagList();
            doors.appendTag(door);
            village.setTag("Doors", doors);
            villages.appendTag(village);
        }
        NBTTagCompound data = new NBTTagCompound();
        data.setInteger("Tick", 18105);
        data.setTag("Villages", villages);
        NBTTagCompound root = new NBTTagCompound();
        root.setTag("data", data);
        return root;
    }

    private static Path write(Path world, String name, NBTTagCompound root) throws IOException {
        Path data = Files.createDirectories(world.resolve("data"));
        Path file = data.resolve(name).toAbsolutePath().normalize();
        try (OutputStream output = new BufferedOutputStream(Files.newOutputStream(file))) {
            CompressedStreamTools.writeCompressed(root, output);
        }
        return file;
    }
}

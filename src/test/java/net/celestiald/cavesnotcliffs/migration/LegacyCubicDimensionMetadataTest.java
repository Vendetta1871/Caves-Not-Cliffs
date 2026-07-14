package net.celestiald.cavesnotcliffs.migration;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LegacyCubicDimensionMetadataTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void returnsSortedTrueDimensionsAndEveryValidatedMetadataFile() throws Exception {
        Path world = temporary.newFolder("world").toPath();
        Path rootMetadata = writeMetadata(world, true, -64, 320,
                "cubicchunks:anvil3d", "cubicchunks:default");
        Path falseMetadata = writeMetadata(Files.createDirectory(world.resolve("DIM1")),
                false, 0, 256, "cubicchunks:anvil3d", "cubicchunks:default");
        Path nether = Files.createDirectory(world.resolve("DIM-1"));
        Path netherMetadata = writeMetadata(nether, true, -64, 320,
                "cubicchunks:anvil3d", "cubicchunks:default");
        Files.createDirectory(nether.resolve("region3d"));

        Path backup = Files.createDirectories(world.resolve("backup/DIM2"));
        writeMetadata(backup, true, -64, 320,
                "cubicchunks:anvil3d", "cubicchunks:default");
        Files.createDirectory(backup.resolve("region2d"));
        Path nonCanonical = Files.createDirectory(world.resolve("DIM01"));
        writeMetadata(nonCanonical, true, -64, 320,
                "cubicchunks:anvil3d", "cubicchunks:default");

        LegacyCubicDimensionMetadata.Result result =
                LegacyCubicDimensionMetadata.discoverAndValidate(world);

        assertEquals(Arrays.asList(world.toAbsolutePath().normalize(),
                        nether.toAbsolutePath().normalize()),
                result.getTrueDimensionRoots());
        assertEquals(Arrays.asList(rootMetadata.toAbsolutePath().normalize(),
                        netherMetadata.toAbsolutePath().normalize(),
                        falseMetadata.toAbsolutePath().normalize()),
                result.getMetadataFiles());
        try {
            result.getMetadataFiles().clear();
            fail("Expected immutable metadata file list");
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }
    }

    @Test
    public void rejectsUnsupportedTrueMetadataWithoutRegionStorage() throws Exception {
        Path wrongBounds = temporary.newFolder("wrong-bounds").toPath();
        writeMetadata(wrongBounds, true, 0, 256,
                "cubicchunks:anvil3d", "cubicchunks:default");
        assertFailure(wrongBounds, "bounds");

        Path wrongStorage = temporary.newFolder("wrong-storage").toPath();
        writeMetadata(wrongStorage, true, -64, 320,
                "thirdparty:custom", "cubicchunks:default");
        assertFailure(wrongStorage, "storage format");

        Path wrongGenerator = temporary.newFolder("wrong-generator").toPath();
        writeMetadata(wrongGenerator, true, -64, 320,
                "cubicchunks:anvil3d", "thirdparty:custom");
        assertFailure(wrongGenerator, "compatibility generator");
    }

    @Test
    public void requiresExactVanillaBoundsAndProvidersForFalseDimensions() throws Exception {
        Path accepted = temporary.newFolder("accepted-false").toPath();
        Path metadata = writeMetadata(accepted, false, 0, 256,
                "cubicchunks:anvil3d", "cubicchunks:default");
        LegacyCubicDimensionMetadata.Result result =
                LegacyCubicDimensionMetadata.discoverAndValidate(accepted);
        assertTrue(result.getTrueDimensionRoots().isEmpty());
        assertEquals(Arrays.asList(metadata.toAbsolutePath().normalize()),
                result.getMetadataFiles());

        Path wrongBounds = temporary.newFolder("false-wrong-bounds").toPath();
        writeMetadata(wrongBounds, false, -64, 320,
                "cubicchunks:anvil3d", "cubicchunks:default");
        assertFailure(wrongBounds, "bounds");

        Path wrongStorage = temporary.newFolder("false-wrong-storage").toPath();
        writeMetadata(wrongStorage, false, 0, 256,
                "thirdparty:custom", "cubicchunks:default");
        assertFailure(wrongStorage, "storage format");
    }

    @Test
    public void rejectsRegionStorageWithoutMatchingTrueMetadata() throws Exception {
        Path missing = temporary.newFolder("missing").toPath();
        Files.createDirectory(missing.resolve("region2d"));
        assertFailure(missing, "no matching true dimension metadata");

        Path falseDimension = temporary.newFolder("false-with-regions").toPath();
        writeMetadata(falseDimension, false, 0, 256,
                "cubicchunks:anvil3d", "cubicchunks:default");
        Files.createDirectory(falseDimension.resolve("region3d"));
        assertFailure(falseDimension, "does not have true dimension metadata");

        Path childMissing = temporary.newFolder("child-missing").toPath();
        Files.createDirectories(childMissing.resolve("DIM7/region3d"));
        assertFailure(childMissing, "no matching true dimension metadata");
    }

    @Test
    public void requiresExactRootAndDataShapesAndTagTypes() throws Exception {
        Path extraRoot = temporary.newFolder("extra-root").toPath();
        NBTTagCompound root = metadata(true, -64, 320,
                "cubicchunks:anvil3d", "cubicchunks:default");
        root.setInteger("extra", 1);
        write(extraRoot, root);
        assertFailure(extraRoot, "root shape");

        Path extraData = temporary.newFolder("extra-data").toPath();
        root = metadata(true, -64, 320,
                "cubicchunks:anvil3d", "cubicchunks:default");
        root.getCompoundTag("data").setInteger("extra", 1);
        write(extraData, root);
        assertFailure(extraData, "data shape");

        Path wrongType = temporary.newFolder("wrong-type").toPath();
        root = metadata(true, -64, 320,
                "cubicchunks:anvil3d", "cubicchunks:default");
        root.getCompoundTag("data").setString("minHeight", "-64");
        write(wrongType, root);
        assertFailure(wrongType, "malformed minHeight");

        Path nonBoolean = temporary.newFolder("non-boolean").toPath();
        root = metadata(true, -64, 320,
                "cubicchunks:anvil3d", "cubicchunks:default");
        root.getCompoundTag("data").setByte("isCubicChunks", (byte) 2);
        write(nonBoolean, root);
        assertFailure(nonBoolean, "non-boolean");
    }

    static Path writeMetadata(Path dimension, boolean cubic, int minimum, int maximum,
            String storage, String compatibility) throws IOException {
        return write(dimension, metadata(cubic, minimum, maximum, storage, compatibility));
    }

    private static Path write(Path dimension, NBTTagCompound root) throws IOException {
        Path file = Files.createDirectories(dimension.resolve("data"))
                .resolve("cubicChunksData.dat");
        try (OutputStream output = new BufferedOutputStream(Files.newOutputStream(file))) {
            CompressedStreamTools.writeCompressed(root, output);
        }
        return file;
    }

    private static NBTTagCompound metadata(boolean cubic, int minimum, int maximum,
            String storage, String compatibility) {
        NBTTagCompound root = new NBTTagCompound();
        NBTTagCompound data = new NBTTagCompound();
        data.setInteger("minHeight", minimum);
        data.setBoolean("isCubicChunks", cubic);
        data.setInteger("maxHeight", maximum);
        data.setString("storageFormat", storage);
        data.setString("compatibilityGeneratorType", compatibility);
        root.setTag("data", data);
        return root;
    }

    private static void assertFailure(Path world, String message) throws Exception {
        try {
            LegacyCubicDimensionMetadata.discoverAndValidate(world);
            fail("Expected metadata validation failure containing: " + message);
        } catch (IOException expected) {
            assertTrue("Unexpected message: " + expected.getMessage(),
                    expected.getMessage().contains(message));
        }
    }
}

package net.celestiald.cavesnotcliffs.migration;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BoundedNbtReaderTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void readsValidCompressedAndUncompressedRoots() throws Exception {
        NBTTagCompound expected = root("valid");
        Path compressed = temporary.newFile("valid.nbt.gz").toPath();
        try (OutputStream output = new BufferedOutputStream(
                Files.newOutputStream(compressed))) {
            CompressedStreamTools.writeCompressed(expected, output);
        }
        assertEquals(expected, BoundedNbtReader.readCompressed(compressed, "test NBT"));

        Path uncompressed = temporary.newFile("valid.nbt").toPath();
        try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(
                Files.newOutputStream(uncompressed)))) {
            CompressedStreamTools.write(expected, output);
        }
        assertEquals(expected, BoundedNbtReader.readUncompressed(
                uncompressed, "test uncompressed NBT"));
    }

    @Test
    public void rejectsEncodedInputAboveItsLimitBeforeDecoding() throws Exception {
        Path path = temporary.newFile("oversized.nbt.gz").toPath();
        Files.write(path, new byte[33]);

        expectFailure(() -> BoundedNbtReader.readCompressed(
                path, "oversized test NBT", 32L, 1024L),
                "encoded input limit of 32 bytes");
    }

    @Test
    public void rejectsHighlyCompressedNbtAboveItsDecompressedLimit() throws Exception {
        NBTTagCompound bomb = root("bomb");
        bomb.setByteArray("payload", new byte[8192]);
        Path path = temporary.newFile("bomb.nbt.gz").toPath();
        try (OutputStream output = new BufferedOutputStream(
                Files.newOutputStream(path))) {
            CompressedStreamTools.writeCompressed(bomb, output);
        }
        assertTrue(Files.size(path) < 1024L);

        expectFailure(() -> BoundedNbtReader.readCompressed(
                path, "compressed bomb", 1024L, 512L),
                "exceeds its limit of 512 bytes");
    }

    @Test
    public void rejectsOverflowSizedDeclaredArrayBeforeMinecraftAllocatesIt() throws Exception {
        Path path = temporary.newFile("declared-array.nbt.gz").toPath();
        try (DataOutputStream output = new DataOutputStream(new GZIPOutputStream(
                new BufferedOutputStream(Files.newOutputStream(path))))) {
            output.writeByte(10);
            output.writeUTF("");
            output.writeByte(7);
            output.writeUTF("payload");
            output.writeInt(1 << 29);
            output.writeByte(0);
        }

        expectFailure(() -> BoundedNbtReader.readCompressed(
                path, "declared array test", 1024L, 1024L),
                "declared payload length 536870912");
    }

    @Test
    public void rejectsTrailingDecompressedBytesAfterTheRootTag() throws Exception {
        Path path = temporary.newFile("trailing.nbt.gz").toPath();
        try (DataOutputStream output = new DataOutputStream(new GZIPOutputStream(
                new BufferedOutputStream(Files.newOutputStream(path))))) {
            CompressedStreamTools.write(root("trailing"), output);
            output.writeInt(0x12345678);
        }

        expectFailure(() -> BoundedNbtReader.readCompressed(path, "trailing test NBT"),
                "4 trailing decompressed bytes");
    }

    @Test
    public void refusesSymbolicLinkInputs() throws Exception {
        Path directory = temporary.newFolder("symlink").toPath();
        Path target = directory.resolve("target.nbt.gz");
        try (OutputStream output = new BufferedOutputStream(Files.newOutputStream(target))) {
            CompressedStreamTools.writeCompressed(root("target"), output);
        }
        Path link = directory.resolve("link.nbt.gz");
        try {
            Files.createSymbolicLink(link, target.getFileName());
        } catch (IOException | UnsupportedOperationException | SecurityException exception) {
            Assume.assumeNoException(exception);
        }

        expectFailure(() -> BoundedNbtReader.readCompressed(link, "linked test NBT"),
                "is not a regular file");
    }

    private static NBTTagCompound root(String value) {
        NBTTagCompound root = new NBTTagCompound();
        root.setString("value", value);
        return root;
    }

    private static void expectFailure(ThrowingRunnable action, String expectedMessage)
            throws Exception {
        try {
            action.run();
            fail("Expected IOException containing: " + expectedMessage);
        } catch (IOException expected) {
            assertTrue("Actual message: " + expected.getMessage(),
                    expected.getMessage().contains(expectedMessage));
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}

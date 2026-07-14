package net.celestiald.cavesnotcliffs.migration;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LegacyCubicLevelMetadataTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void removesOnlyCubicMetadataAndPreservesAnExactBackup() throws Exception {
        Path world = temporary.newFolder("world").toPath();
        Path levelFile = world.resolve("level.dat");
        write(levelFile, level("cubicchunks:storage_format_provider_registry",
                "cubicchunks:vanilla_compatibility_generators_registry"));
        byte[] original = Files.readAllBytes(levelFile);

        LegacyCubicLevelMetadata.Result inspected =
                LegacyCubicLevelMetadata.inspect(levelFile, true);
        assertEquals(2, inspected.getRemovedRegistries());
        assertEquals(2, inspected.getRemovedMods());
        assertTrue(inspected.removedWorldFlag());
        assertArrayEquals(original, Files.readAllBytes(levelFile));

        LegacyCubicLevelMetadata.Result cleaned = LegacyCubicLevelMetadata.clean(levelFile);
        assertTrue(cleaned.changed());
        Path backup = world.resolve(LegacyCubicLevelMetadata.BACKUP_FILE_NAME);
        assertArrayEquals(original, Files.readAllBytes(backup));

        NBTTagCompound result = read(levelFile);
        NBTTagCompound registries = result.getCompoundTag("FML")
                .getCompoundTag("Registries");
        assertFalse(registries.hasKey("cubicchunks:storage_format_provider_registry"));
        assertFalse(registries.hasKey(
                "cubicchunks:vanilla_compatibility_generators_registry"));
        assertEquals(73, registries.getCompoundTag("minecraft:blocks")
                .getInteger("sentinel"));
        assertEquals(1, result.getCompoundTag("FML")
                .getTagList("ModList", 10).tagCount());
        assertEquals("cavesnotcliffs", result.getCompoundTag("FML")
                .getTagList("ModList", 10).getCompoundTagAt(0).getString("ModId"));
        assertFalse(result.getCompoundTag("Data").hasKey("isCubicWorld"));
        assertEquals(91, result.getCompoundTag("Data").getInteger("preserved"));

        byte[] firstClean = Files.readAllBytes(levelFile);
        assertFalse(LegacyCubicLevelMetadata.clean(levelFile).changed());
        assertArrayEquals(firstClean, Files.readAllBytes(levelFile));
        assertArrayEquals(original, Files.readAllBytes(backup));
    }

    @Test
    public void refusesUnknownCubicRegistriesWithoutWritingAnything() throws Exception {
        Path world = temporary.newFolder("unknown").toPath();
        Path levelFile = world.resolve("level.dat");
        write(levelFile, level("cubicchunks:third_party_storage"));
        byte[] original = Files.readAllBytes(levelFile);

        try {
            LegacyCubicLevelMetadata.clean(levelFile);
            fail("Expected unknown CubicChunks registry rejection");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("Unsupported CubicChunks registry"));
        }
        assertArrayEquals(original, Files.readAllBytes(levelFile));
        assertFalse(Files.exists(world.resolve(LegacyCubicLevelMetadata.BACKUP_FILE_NAME)));
    }

    @Test
    public void refusesKnownRegistryNamesWithUnsupportedProviders() throws Exception {
        Path world = temporary.newFolder("provider").toPath();
        Path levelFile = world.resolve("level.dat");
        NBTTagCompound root = level("cubicchunks:storage_format_provider_registry",
                "cubicchunks:vanilla_compatibility_generators_registry");
        NBTTagCompound storage = root.getCompoundTag("FML").getCompoundTag("Registries")
                .getCompoundTag("cubicchunks:storage_format_provider_registry");
        storage.getTagList("ids", 10).getCompoundTagAt(0)
                .setString("K", "thirdparty:custom_storage");
        write(levelFile, root);

        try {
            LegacyCubicLevelMetadata.inspect(levelFile, true);
            fail("Expected custom storage provider rejection");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("selects unsupported entry"));
        }
    }

    @Test
    public void staleFixedTempSymlinksCannotClobberExternalFiles() throws Exception {
        Path world = temporary.newFolder("temp-symlinks").toPath();
        Path levelFile = world.resolve("level.dat");
        write(levelFile, level("cubicchunks:storage_format_provider_registry",
                "cubicchunks:vanilla_compatibility_generators_registry"));
        Path externalLevel = temporary.newFile("external-level").toPath();
        Path externalBackup = temporary.newFile("external-backup").toPath();
        byte[] levelSentinel = new byte[] {1, 3, 5, 7};
        byte[] backupSentinel = new byte[] {2, 4, 6, 8};
        Files.write(externalLevel, levelSentinel);
        Files.write(externalBackup, backupSentinel);

        Path oldLevelTemp = world.resolve(".cavesnotcliffs-level.dat.tmp");
        Path oldBackupTemp = world.resolve(
                LegacyCubicLevelMetadata.BACKUP_FILE_NAME + ".tmp");
        try {
            Files.createSymbolicLink(oldLevelTemp, externalLevel.toAbsolutePath());
            Files.createSymbolicLink(oldBackupTemp, externalBackup.toAbsolutePath());
        } catch (IOException | UnsupportedOperationException exception) {
            Assume.assumeNoException("Symbolic links are unavailable", exception);
        }

        LegacyCubicLevelMetadata.clean(levelFile);

        assertArrayEquals(levelSentinel, Files.readAllBytes(externalLevel));
        assertArrayEquals(backupSentinel, Files.readAllBytes(externalBackup));
        assertTrue(Files.isSymbolicLink(oldLevelTemp));
        assertTrue(Files.isSymbolicLink(oldBackupTemp));
        assertTrue(Files.isRegularFile(levelFile));
        assertFalse(Files.isSymbolicLink(levelFile));
        assertTrue(Files.isRegularFile(
                world.resolve(LegacyCubicLevelMetadata.BACKUP_FILE_NAME)));
    }

    @Test
    public void requiresTheCubicWorldFlagForAFirstImport() throws Exception {
        Path world = temporary.newFolder("flag").toPath();
        Path levelFile = world.resolve("level.dat");
        NBTTagCompound root = level("cubicchunks:storage_format_provider_registry",
                "cubicchunks:vanilla_compatibility_generators_registry");
        root.getCompoundTag("Data").setBoolean("isCubicWorld", false);
        write(levelFile, root);

        try {
            LegacyCubicLevelMetadata.inspect(levelFile, true);
            fail("Expected false cubic-world flag rejection");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("Data.isCubicWorld=true"));
        }
    }

    static NBTTagCompound level(String... cubicRegistries) {
        NBTTagCompound root = new NBTTagCompound();
        NBTTagCompound data = new NBTTagCompound();
        data.setBoolean("isCubicWorld", true);
        data.setInteger("preserved", 91);
        root.setTag("Data", data);

        NBTTagCompound fml = new NBTTagCompound();
        NBTTagCompound registries = new NBTTagCompound();
        NBTTagCompound blocks = new NBTTagCompound();
        blocks.setInteger("sentinel", 73);
        registries.setTag("minecraft:blocks", blocks);
        for (String name : cubicRegistries) {
            registries.setTag(name, registry(name));
        }
        fml.setTag("Registries", registries);
        NBTTagList mods = new NBTTagList();
        mods.appendTag(mod("cubicchunkscore"));
        mods.appendTag(mod("cavesnotcliffs"));
        mods.appendTag(mod("cubicchunks"));
        fml.setTag("ModList", mods);
        root.setTag("FML", fml);
        return root;
    }

    private static NBTTagCompound registry(String name) {
        NBTTagCompound registry = new NBTTagCompound();
        registry.setTag("aliases", new NBTTagList());
        registry.setIntArray("blocked", new int[0]);
        NBTTagCompound entry = new NBTTagCompound();
        entry.setString("K", name.equals("cubicchunks:storage_format_provider_registry")
                ? "cubicchunks:anvil3d" : "cubicchunks:default");
        entry.setInteger("V", 0);
        NBTTagList ids = new NBTTagList();
        ids.appendTag(entry);
        registry.setTag("ids", ids);
        registry.setTag("overrides", new NBTTagList());
        registry.setTag("dummied", new NBTTagList());
        return registry;
    }

    private static NBTTagCompound mod(String id) {
        NBTTagCompound mod = new NBTTagCompound();
        mod.setString("ModId", id);
        mod.setString("ModVersion", "preserved-version");
        return mod;
    }

    static void write(Path path, NBTTagCompound root) throws IOException {
        try (OutputStream output = new BufferedOutputStream(Files.newOutputStream(path))) {
            CompressedStreamTools.writeCompressed(root, output);
        }
    }

    static NBTTagCompound read(Path path) throws IOException {
        try (InputStream input = new BufferedInputStream(Files.newInputStream(path))) {
            return CompressedStreamTools.readCompressed(input);
        }
    }
}

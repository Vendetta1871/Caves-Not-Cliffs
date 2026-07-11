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
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LegacyCubicRegistrySnapshotTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void readsAuthenticShapedSnapshotsWithoutChangingLevelDat() throws Exception {
        NBTTagCompound root = root(
                registry(entry("cavesnotcliffs:powder_snow", 367),
                        entry("minecraft:air", 0),
                        entry("cavesnotcliffs:deepslate", 354),
                        entry("minecraft:stone", 1)),
                registry(entry("minecraft:forest", 4),
                        entry("minecraft:ocean", 0),
                        entry("cavesnotcliffs:frozen_peaks", 67),
                        entry("minecraft:plains", 1)));
        root.setTag("Data", new NBTTagCompound());
        Path levelFile = write("authentic-level.dat", root);
        byte[] before = Files.readAllBytes(levelFile);

        LegacyCubicRegistrySnapshot snapshot =
                LegacyCubicRegistrySnapshot.read(levelFile);

        assertEquals(Integer.valueOf(367),
                snapshot.getBlockId("cavesnotcliffs:powder_snow"));
        assertEquals("cavesnotcliffs:deepslate", snapshot.getBlockName(354));
        assertEquals(Integer.valueOf(67),
                snapshot.getBiomeId("cavesnotcliffs:frozen_peaks"));
        assertEquals("minecraft:forest", snapshot.getBiomeName(4));
        assertEquals(4, snapshot.getBlockIdsByName().size());
        assertEquals(4, snapshot.getBlockNamesById().size());
        assertEquals(4, snapshot.getBiomeIdsByName().size());
        assertEquals(4, snapshot.getBiomeNamesById().size());
        assertArrayEquals(before, Files.readAllBytes(levelFile));
        assertImmutable(snapshot.getBlockIdsByName());
        assertImmutable(snapshot.getBlockNamesById());
        assertImmutable(snapshot.getBiomeIdsByName());
        assertImmutable(snapshot.getBiomeNamesById());
    }

    @Test
    public void acceptsShuffledNonSequentialIdsAndKeepsBothDirectionsExact()
            throws Exception {
        Path levelFile = write("shuffled.dat", root(
                registry(entry("example:last", 4095), entry("example:first", 17)),
                registry(entry("example:high", 255), entry("example:low", 23))));

        LegacyCubicRegistrySnapshot snapshot =
                LegacyCubicRegistrySnapshot.read(levelFile);

        assertEquals(Integer.valueOf(4095), snapshot.getBlockId("example:last"));
        assertEquals("example:first", snapshot.getBlockName(17));
        assertEquals(Integer.valueOf(255), snapshot.getBiomeId("example:high"));
        assertEquals("example:low", snapshot.getBiomeName(23));
    }

    @Test
    public void rejectsDuplicateSavedNamesAndIds() throws Exception {
        assertFailure(root(
                registry(entry("minecraft:air", 0), entry("minecraft:air", 1)),
                registry(entry("minecraft:ocean", 0))), "repeats saved name");
        assertFailure(root(
                registry(entry("minecraft:air", 0), entry("minecraft:stone", 0)),
                registry(entry("minecraft:ocean", 0))), "repeats saved ID");
        assertFailure(root(
                registry(entry("minecraft:air", 0)),
                registry(entry("minecraft:ocean", 0), entry("minecraft:plains", 0))),
                "repeats saved ID");
    }

    @Test
    public void rejectsMalformedEntryKeysAndTypes() throws Exception {
        NBTTagCompound missingKey = new NBTTagCompound();
        missingKey.setString("K", "minecraft:air");
        assertMalformedEntry(missingKey);

        NBTTagCompound wrongKeyType = entry("minecraft:air", 0);
        wrongKeyType.setInteger("K", 4);
        assertMalformedEntry(wrongKeyType);

        NBTTagCompound wrongValueType = new NBTTagCompound();
        wrongValueType.setString("K", "minecraft:air");
        wrongValueType.setLong("V", 0L);
        assertMalformedEntry(wrongValueType);

        NBTTagCompound extraKey = entry("minecraft:air", 0);
        extraKey.setBoolean("extra", true);
        assertMalformedEntry(extraKey);

        assertFailure(root(registry(entry("Not Canonical", 0)),
                registry(entry("minecraft:ocean", 0))), "invalid saved name");
    }

    @Test
    public void rejectsMissingRequiredRegistryHierarchy() throws Exception {
        assertFailure(new NBTTagCompound(), "no compound FML registry snapshot");

        NBTTagCompound noRegistries = new NBTTagCompound();
        noRegistries.setTag("FML", new NBTTagCompound());
        assertFailure(noRegistries, "no compound FML.Registries snapshot");

        NBTTagCompound onlyBlocks = new NBTTagCompound();
        NBTTagCompound fml = new NBTTagCompound();
        NBTTagCompound registries = new NBTTagCompound();
        registries.setTag("minecraft:blocks",
                registry(entry("minecraft:air", 0)));
        fml.setTag("Registries", registries);
        onlyBlocks.setTag("FML", fml);
        assertFailure(onlyBlocks, "missing required registry snapshot minecraft:biomes");

        NBTTagCompound onlyBiomes = new NBTTagCompound();
        fml = new NBTTagCompound();
        registries = new NBTTagCompound();
        registries.setTag("minecraft:biomes",
                registry(entry("minecraft:ocean", 0)));
        fml.setTag("Registries", registries);
        onlyBiomes.setTag("FML", fml);
        assertFailure(onlyBiomes, "missing required registry snapshot minecraft:blocks");
    }

    @Test
    public void rejectsIdsOutsideTheirSerializedRanges() throws Exception {
        assertFailure(root(
                registry(entry("minecraft:air", -1)),
                registry(entry("minecraft:ocean", 0))), "outside 0..4095");
        assertFailure(root(
                registry(entry("minecraft:air", 4096)),
                registry(entry("minecraft:ocean", 0))), "outside 0..4095");
        assertFailure(root(
                registry(entry("minecraft:air", 0)),
                registry(entry("minecraft:ocean", -1))), "outside 0..255");
        assertFailure(root(
                registry(entry("minecraft:air", 0)),
                registry(entry("minecraft:ocean", 256))), "outside 0..255");
    }

    @Test
    public void rejectsMalformedRegistryContainers() throws Exception {
        NBTTagCompound malformed = root(
                registry(entry("minecraft:air", 0)),
                registry(entry("minecraft:ocean", 0)));
        malformed.getCompoundTag("FML").getCompoundTag("Registries")
                .getCompoundTag("minecraft:blocks").setInteger("unexpected", 1);
        assertFailure(malformed, "unsupported shape");

        malformed = root(
                registry(entry("minecraft:air", 0)),
                registry(entry("minecraft:ocean", 0)));
        NBTTagList strings = new NBTTagList();
        strings.appendTag(new NBTTagString("not-a-compound"));
        malformed.getCompoundTag("FML").getCompoundTag("Registries")
                .getCompoundTag("minecraft:blocks").setTag("ids", strings);
        assertFailure(malformed, "ids is not a nonempty compound list");

        assertFailure(root(registry(), registry(entry("minecraft:ocean", 0))),
                "ids is not a nonempty compound list");
    }

    private void assertMalformedEntry(NBTTagCompound malformed) throws Exception {
        assertFailure(root(registry(malformed),
                registry(entry("minecraft:ocean", 0))), "malformed K/V entry");
    }

    private void assertFailure(NBTTagCompound root, String message) throws Exception {
        Path levelFile = write("invalid-" + System.nanoTime() + ".dat", root);
        try {
            LegacyCubicRegistrySnapshot.read(levelFile);
            fail("Expected registry snapshot rejection containing: " + message);
        } catch (IOException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains(message));
        }
    }

    private static NBTTagCompound root(NBTTagCompound blocks, NBTTagCompound biomes) {
        NBTTagCompound registries = new NBTTagCompound();
        registries.setTag("minecraft:blocks", blocks);
        registries.setTag("minecraft:biomes", biomes);
        NBTTagCompound fml = new NBTTagCompound();
        fml.setTag("Registries", registries);
        NBTTagCompound root = new NBTTagCompound();
        root.setTag("FML", fml);
        return root;
    }

    private static NBTTagCompound registry(NBTTagCompound... entries) {
        NBTTagList ids = new NBTTagList();
        for (NBTTagCompound entry : entries) {
            ids.appendTag(entry);
        }
        NBTTagCompound registry = new NBTTagCompound();
        registry.setTag("aliases", new NBTTagList());
        registry.setIntArray("blocked", new int[0]);
        registry.setTag("ids", ids);
        registry.setTag("overrides", new NBTTagList());
        registry.setTag("dummied", new NBTTagList());
        return registry;
    }

    private static NBTTagCompound entry(String name, int id) {
        NBTTagCompound entry = new NBTTagCompound();
        entry.setString("K", name);
        entry.setInteger("V", id);
        return entry;
    }

    private Path write(String name, NBTTagCompound root) throws IOException {
        Path path = temporary.getRoot().toPath().resolve(name);
        try (OutputStream output = new BufferedOutputStream(Files.newOutputStream(path))) {
            CompressedStreamTools.writeCompressed(root, output);
        }
        return path;
    }

    private static <K, V> void assertImmutable(Map<K, V> values) {
        assertNotNull(values);
        try {
            values.clear();
            fail("Expected immutable registry map");
        } catch (UnsupportedOperationException expected) {
            // Expected.
        }
    }
}

package net.celestiald.cavesnotcliffs.migration;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagList;
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

public class LegacyCubicPositionMetadataTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void acceptsExactBoundsAndReturnsEveryValidatedSource() throws Exception {
        Path world = temporary.newFolder("world").toPath();
        NBTTagCompound integrated = player(-64.0D, -64);
        integrated.setTag("Spawns", spawns(-64, 319));
        integrated.setBoolean("HasSpawnDimensionSet", true);
        integrated.setInteger("SpawnDimension", -1);
        Path level = writeLevel(world, -64, integrated);
        Path playerDirectory = Files.createDirectory(world.resolve("playerdata"));
        Path second = write(playerDirectory.resolve("b.dat"), player(319.999D, 319));
        Path first = write(playerDirectory.resolve("a.dat"), player(-64.0D, -64));

        LegacyCubicPositionMetadata.Result result =
                LegacyCubicPositionMetadata.validate(world, level);

        assertEquals(Arrays.asList(level.toAbsolutePath().normalize(),
                        first.toAbsolutePath().normalize(), second.toAbsolutePath().normalize()),
                result.getSourceFiles());
    }

    @Test
    public void rejectsDedicatedAndIntegratedPlayersOutsideFiniteHeight() throws Exception {
        Path dedicated = temporary.newFolder("dedicated").toPath();
        Path level = writeLevel(dedicated, 64, null);
        Path playerDirectory = Files.createDirectory(dedicated.resolve("playerdata"));
        Path player = write(playerDirectory.resolve("bad-player.dat"), player(-65.0D, 64));
        assertFailure(dedicated, level, "bad-player.dat Pos", "Y=-65.0");
        assertTrue(Files.isRegularFile(player));

        Path integrated = temporary.newFolder("integrated").toPath();
        level = writeLevel(integrated, 64, player(320.0D, 64));
        assertFailure(integrated, level, "Data.Player Pos", "Y=320.0");
    }

    @Test
    public void rejectsWorldAndBedSpawnsOutsideFiniteHeight() throws Exception {
        Path worldSpawn = temporary.newFolder("world-spawn").toPath();
        Path level = writeLevel(worldSpawn, 320, null);
        assertFailure(worldSpawn, level, "Data.SpawnY", "Y=320.0");

        Path bedSpawn = temporary.newFolder("bed-spawn").toPath();
        level = writeLevel(bedSpawn, 64, player(64.0D, -65));
        assertFailure(bedSpawn, level, "SpawnY", "Y=-65.0");

        Path forgeSpawn = temporary.newFolder("forge-spawn").toPath();
        NBTTagCompound player = player(64.0D, 64);
        player.setTag("Spawns", spawns(319, 320));
        level = writeLevel(forgeSpawn, 64, player);
        assertFailure(forgeSpawn, level, "Spawns[1]", "Y=320.0");
    }

    @Test
    public void rejectsNonFiniteAndMalformedPlayerPositions() throws Exception {
        Path world = temporary.newFolder("nan").toPath();
        Path level = writeLevel(world, 64, player(Double.NaN, 64));
        assertFailure(world, level, "Data.Player Pos", "Y=NaN");

        Path malformed = temporary.newFolder("malformed").toPath();
        NBTTagCompound player = player(64.0D, 64);
        player.removeTag("Dimension");
        level = writeLevel(malformed, 64, player);
        assertFailure(malformed, level, "no numeric Dimension");
    }

    private static Path writeLevel(Path world, int spawnY, NBTTagCompound player)
            throws IOException {
        NBTTagCompound data = new NBTTagCompound();
        data.setInteger("SpawnY", spawnY);
        if (player != null) {
            data.setTag("Player", player);
        }
        NBTTagCompound root = new NBTTagCompound();
        root.setTag("Data", data);
        return write(world.resolve("level.dat"), root);
    }

    private static NBTTagCompound player(double y, int spawnY) {
        NBTTagCompound player = new NBTTagCompound();
        player.setInteger("Dimension", 0);
        NBTTagList position = new NBTTagList();
        position.appendTag(new NBTTagDouble(0.5D));
        position.appendTag(new NBTTagDouble(y));
        position.appendTag(new NBTTagDouble(0.5D));
        player.setTag("Pos", position);
        player.setInteger("SpawnY", spawnY);
        return player;
    }

    private static NBTTagList spawns(int... values) {
        NBTTagList spawns = new NBTTagList();
        for (int index = 0; index < values.length; index++) {
            NBTTagCompound spawn = new NBTTagCompound();
            spawn.setInteger("Dim", index - 1);
            spawn.setInteger("SpawnY", values[index]);
            spawns.appendTag(spawn);
        }
        return spawns;
    }

    private static Path write(Path path, NBTTagCompound root) throws IOException {
        try (OutputStream output = new BufferedOutputStream(Files.newOutputStream(path))) {
            CompressedStreamTools.writeCompressed(root, output);
        }
        return path;
    }

    private static void assertFailure(Path world, Path level, String... messages)
            throws Exception {
        try {
            LegacyCubicPositionMetadata.validate(world, level);
            fail("Expected position metadata rejection");
        } catch (IOException expected) {
            for (String message : messages) {
                assertTrue("Unexpected message: " + expected.getMessage(),
                        expected.getMessage().contains(message));
            }
        }
    }
}

package net.celestiald.cavesnotcliffs.migration;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LegacyCubicTicketMetadataTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void acceptsEmptyAndOrdinaryForgeTickets() throws Exception {
        Path world = temporary.newFolder("world").toPath();
        Path nether = Files.createDirectory(world.resolve("DIM-1"));
        Path rootFile = write(world, new NBTTagList());
        NBTTagCompound ordinary = new NBTTagCompound();
        ordinary.setByte("Type", (byte) 0);
        ordinary.setTag("ModData", new NBTTagCompound());
        Path netherFile = write(nether, holders("examplemod", ordinary));

        LegacyCubicTicketMetadata.Result result =
                LegacyCubicTicketMetadata.validate(Arrays.asList(nether, world));

        assertEquals(Arrays.asList(rootFile.toAbsolutePath().normalize(),
                        netherFile.toAbsolutePath().normalize()),
                result.getSourceFiles());
    }

    @Test
    public void permitsOnlyAnEmptyWellFormedCubicTicketMap() throws Exception {
        Path world = temporary.newFolder("empty-cubic").toPath();
        NBTTagCompound ticket = new NBTTagCompound();
        NBTTagCompound cubic = new NBTTagCompound();
        cubic.setInteger("entityCubeY", 0);
        cubic.setTag("chunkMap", new NBTTagList());
        ticket.setTag("cubicchunks", cubic);
        write(world, holders("examplemod", ticket));

        LegacyCubicTicketMetadata.validate(Collections.singletonList(world));
    }

    @Test
    public void rejectsNonemptyCubicTicketMapsWithoutChangingTheFile() throws Exception {
        Path world = temporary.newFolder("forced-cubes").toPath();
        NBTTagCompound ticket = new NBTTagCompound();
        NBTTagCompound cubic = new NBTTagCompound();
        cubic.setInteger("entityCubeY", -3);
        NBTTagCompound entry = new NBTTagCompound();
        entry.setInteger("x", 12);
        entry.setInteger("z", -7);
        entry.setIntArray("cubes", new int[] {-4, -3, -2});
        NBTTagList chunkMap = new NBTTagList();
        chunkMap.appendTag(entry);
        cubic.setTag("chunkMap", chunkMap);
        ticket.setTag("cubicchunks", cubic);
        Path file = write(world, holders("chunkloader", ticket));
        byte[] before = Files.readAllBytes(file);

        try {
            LegacyCubicTicketMetadata.validate(Collections.singletonList(world));
            fail("Expected forced-cube ticket rejection");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("ticket chunkloader[0] forces 1"));
            assertTrue(expected.getMessage().contains("x=12, z=-7"));
        }
        assertTrue(Arrays.equals(before, Files.readAllBytes(file)));
    }

    @Test
    public void rejectsMalformedCubicTicketData() throws Exception {
        Path world = temporary.newFolder("malformed-ticket").toPath();
        NBTTagCompound ticket = new NBTTagCompound();
        ticket.setString("cubicchunks", "not-a-compound");
        write(world, holders("examplemod", ticket));

        try {
            LegacyCubicTicketMetadata.validate(Collections.singletonList(world));
            fail("Expected malformed ticket rejection");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("non-compound CubicChunks data"));
        }
    }

    private static NBTTagList holders(String owner, NBTTagCompound... tickets) {
        NBTTagCompound holder = new NBTTagCompound();
        holder.setString("Owner", owner);
        NBTTagList ticketList = new NBTTagList();
        for (NBTTagCompound ticket : tickets) {
            ticketList.appendTag(ticket);
        }
        holder.setTag("Tickets", ticketList);
        NBTTagList holders = new NBTTagList();
        holders.appendTag(holder);
        return holders;
    }

    private static Path write(Path dimension, NBTTagList holders) throws IOException {
        NBTTagCompound root = new NBTTagCompound();
        root.setTag("TicketList", holders);
        Path file = dimension.resolve("forcedchunks.dat");
        CompressedStreamTools.write(root, file.toFile());
        return file;
    }
}

package net.celestiald.cavesnotcliffs.migration;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Rejects CubicChunks-only forced-cube tickets that Forge cannot restore as finite columns. */
final class LegacyCubicTicketMetadata {
    private LegacyCubicTicketMetadata() {
    }

    static Result validate(List<Path> dimensionRoots) throws IOException {
        Set<Path> uniqueDimensions = new HashSet<Path>();
        for (Path dimension : dimensionRoots) {
            uniqueDimensions.add(dimension.toAbsolutePath().normalize());
        }
        List<Path> dimensions = new ArrayList<Path>(uniqueDimensions);
        Collections.sort(dimensions, Comparator.comparing(Path::toString));

        List<Path> sources = new ArrayList<Path>();
        for (Path dimension : dimensions) {
            Path file = dimension.resolve("forcedchunks.dat");
            if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
                continue;
            }
            if (Files.isSymbolicLink(file)
                    || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Legacy forced-chunk metadata is not a regular file: "
                        + file);
            }
            validateFile(file);
            sources.add(file);
        }
        return new Result(sources);
    }

    private static void validateFile(Path file) throws IOException {
        NBTTagCompound root;
        try {
            root = CompressedStreamTools.read(file.toFile());
        } catch (IOException exception) {
            throw new IOException("Could not read legacy forced-chunk metadata from " + file,
                    exception);
        } catch (RuntimeException exception) {
            throw new IOException("Malformed legacy forced-chunk metadata in " + file,
                    exception);
        }
        if (!root.getKeySet().equals(Collections.singleton("TicketList"))
                || !root.hasKey("TicketList", 9)) {
            throw new IOException(file + " has an unsupported forcedchunks.dat root shape");
        }
        NBTTagList holders = (NBTTagList) root.getTag("TicketList");
        if (holders.getTagType() != 0 && holders.getTagType() != 10) {
            throw new IOException(file + " TicketList does not contain compounds");
        }
        for (int holderIndex = 0; holderIndex < holders.tagCount(); holderIndex++) {
            NBTTagCompound holder = holders.getCompoundTagAt(holderIndex);
            if (!holder.hasKey("Owner", 8) || !holder.hasKey("Tickets", 9)) {
                throw new IOException(file + " TicketList[" + holderIndex
                        + "] is missing Owner or Tickets");
            }
            String owner = holder.getString("Owner");
            NBTTagList tickets = (NBTTagList) holder.getTag("Tickets");
            if (tickets.getTagType() != 0 && tickets.getTagType() != 10) {
                throw new IOException(file + " tickets for " + owner
                        + " do not contain compounds");
            }
            for (int ticketIndex = 0; ticketIndex < tickets.tagCount(); ticketIndex++) {
                NBTTagCompound ticket = tickets.getCompoundTagAt(ticketIndex);
                if (!ticket.hasKey("cubicchunks")) {
                    continue;
                }
                String location = file + " ticket " + owner + '[' + ticketIndex + ']';
                if (!ticket.hasKey("cubicchunks", 10)) {
                    throw new IOException(location + " has non-compound CubicChunks data");
                }
                NBTTagCompound cubic = ticket.getCompoundTag("cubicchunks");
                if (!cubic.getKeySet().equals(new HashSet<String>(java.util.Arrays.asList(
                        "entityCubeY", "chunkMap")))
                        || !cubic.hasKey("entityCubeY", 3)
                        || !cubic.hasKey("chunkMap", 9)) {
                    throw new IOException(location + " has unsupported CubicChunks ticket data");
                }
                NBTTagList chunkMap = (NBTTagList) cubic.getTag("chunkMap");
                if (chunkMap.getTagType() != 0 && chunkMap.getTagType() != 10) {
                    throw new IOException(location + " has a malformed cubic chunkMap");
                }
                if (chunkMap.tagCount() > 0) {
                    NBTTagCompound first = chunkMap.getCompoundTagAt(0);
                    throw new IOException(location + " forces " + chunkMap.tagCount()
                            + " cubic columns; first entry is x=" + first.getInteger("x")
                            + ", z=" + first.getInteger("z")
                            + ". Restore CubicChunks and remove or migrate these tickets first.");
                }
            }
        }
    }

    static final class Result {
        private final List<Path> sourceFiles;

        private Result(List<Path> sourceFiles) {
            this.sourceFiles = Collections.unmodifiableList(
                    new ArrayList<Path>(sourceFiles));
        }

        List<Path> getSourceFiles() {
            return sourceFiles;
        }
    }
}

package net.celestiald.cavesnotcliffs.migration;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Fail-closed validation for world/player positions that cannot exist outside finite height. */
final class LegacyCubicPositionMetadata {
    private static final int MIN_Y = -64;
    private static final int MAX_Y_EXCLUSIVE = 320;

    private LegacyCubicPositionMetadata() {
    }

    static Result validate(Path worldRoot, Path levelFile) throws IOException {
        Path normalizedRoot = worldRoot.toAbsolutePath().normalize();
        Path normalizedLevel = levelFile.toAbsolutePath().normalize();
        if (!normalizedLevel.startsWith(normalizedRoot)
                || Files.isSymbolicLink(normalizedLevel)
                || !Files.isRegularFile(normalizedLevel, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Legacy cubic level.dat is not a regular world file: "
                    + levelFile);
        }

        NBTTagCompound levelRoot = read(normalizedLevel);
        if (!levelRoot.hasKey("Data", 10)) {
            throw new IOException(normalizedLevel + " has no compound Data tag");
        }
        NBTTagCompound data = levelRoot.getCompoundTag("Data");
        if (data.hasKey("SpawnY")) {
            requireIntegralY(data, "SpawnY", normalizedLevel + " Data.SpawnY");
        }
        if (data.hasKey("Player")) {
            if (!data.hasKey("Player", 10)) {
                throw new IOException(normalizedLevel + " Data.Player is not a compound");
            }
            validatePlayer(data.getCompoundTag("Player"),
                    normalizedLevel + " Data.Player");
        }

        List<Path> sources = new ArrayList<Path>();
        sources.add(normalizedLevel);
        Path playerDirectory = normalizedRoot.resolve("playerdata");
        if (Files.exists(playerDirectory, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.isSymbolicLink(playerDirectory)
                    || !Files.isDirectory(playerDirectory, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Legacy cubic playerdata is not a real directory: "
                        + playerDirectory);
            }
            List<Path> players = new ArrayList<Path>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(playerDirectory,
                    "*.dat")) {
                for (Path player : stream) {
                    if (Files.isSymbolicLink(player)
                            || !Files.isRegularFile(player, LinkOption.NOFOLLOW_LINKS)) {
                        throw new IOException("Legacy cubic player data is not a regular file: "
                                + player);
                    }
                    players.add(player.toAbsolutePath().normalize());
                }
            }
            Collections.sort(players, Comparator.comparing(
                    player -> player.getFileName().toString()));
            for (Path player : players) {
                validatePlayer(read(player), player.toString());
                sources.add(player);
            }
        }
        return new Result(sources);
    }

    private static void validatePlayer(NBTTagCompound player, String source)
            throws IOException {
        if (player.hasKey("Pos")) {
            if (!player.hasKey("Pos", 9)) {
                throw new IOException(source + " Pos is not a list");
            }
            NBTBase raw = player.getTag("Pos");
            NBTTagList position = (NBTTagList) raw;
            if (position.getTagType() != 6 || position.tagCount() != 3) {
                throw new IOException(source + " Pos is not a three-double list");
            }
            if (!player.hasKey("Dimension", 99)) {
                throw new IOException(source + " has Pos but no numeric Dimension");
            }
            double y = position.getDoubleAt(1);
            requireY(y, source + " Pos in dimension " + player.getInteger("Dimension"));
        }
        if (player.hasKey("SpawnY")) {
            requireIntegralY(player, "SpawnY", source + " SpawnY");
        }
        if (player.hasKey("Spawns")) {
            if (!player.hasKey("Spawns", 9)) {
                throw new IOException(source + " Spawns is not a list");
            }
            NBTTagList spawns = (NBTTagList) player.getTag("Spawns");
            if (spawns.getTagType() != 0 && spawns.getTagType() != 10) {
                throw new IOException(source + " Spawns does not contain compounds");
            }
            for (int index = 0; index < spawns.tagCount(); index++) {
                NBTTagCompound spawn = spawns.getCompoundTagAt(index);
                if (!spawn.hasKey("Dim", 99)) {
                    throw new IOException(source + " Spawns[" + index
                            + "] has no numeric Dim");
                }
                requireIntegralY(spawn, "SpawnY", source + " Spawns[" + index
                        + "] in dimension " + spawn.getInteger("Dim"));
            }
        }
        if (player.getBoolean("HasSpawnDimensionSet")
                && !player.hasKey("SpawnDimension", 99)) {
            throw new IOException(source
                    + " sets HasSpawnDimensionSet without numeric SpawnDimension");
        }
    }

    private static void requireIntegralY(NBTTagCompound owner, String key, String source)
            throws IOException {
        if (!owner.hasKey(key, 99)) {
            throw new IOException(source + " is not numeric");
        }
        requireY(owner.getInteger(key), source);
    }

    private static void requireY(double y, String source) throws IOException {
        if (!Double.isFinite(y) || y < MIN_Y || y >= MAX_Y_EXCLUSIVE) {
            throw new IOException(source + " stores Y=" + y
                    + " outside the finite -64..319 world");
        }
    }

    private static NBTTagCompound read(Path file) throws IOException {
        try (InputStream input = new BufferedInputStream(Files.newInputStream(file))) {
            return CompressedStreamTools.readCompressed(input);
        } catch (IOException exception) {
            throw new IOException("Could not read legacy cubic position metadata from " + file,
                    exception);
        } catch (RuntimeException exception) {
            throw new IOException("Malformed legacy cubic position metadata in " + file,
                    exception);
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

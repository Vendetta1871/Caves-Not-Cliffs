package net.celestiald.cavesnotcliffs.migration;

import net.celestiald.cavesnotcliffs.migration.cubic.CubicRegionReader;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.chunk.storage.RegionFileCache;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Builds and logically verifies one dimension's complete Anvil output under staging. */
final class CubicDimensionStager {
    static final class Result {
        private final long columns;
        private final long cubes;
        private final long discardedLookaheadColumns;
        private final List<CubicImportJournal.FileRecord> outputs;

        private Result(long columns, long cubes, long discardedLookaheadColumns,
                List<CubicImportJournal.FileRecord> outputs) {
            this.columns = columns;
            this.cubes = cubes;
            this.discardedLookaheadColumns = discardedLookaheadColumns;
            this.outputs = outputs;
        }

        long getColumns() {
            return columns;
        }

        long getCubes() {
            return cubes;
        }

        long getDiscardedLookaheadColumns() {
            return discardedLookaheadColumns;
        }

        List<CubicImportJournal.FileRecord> getOutputs() {
            return outputs;
        }

        boolean hasOutputRegion() {
            return !outputs.isEmpty();
        }
    }

    private CubicDimensionStager() {
    }

    static Result stage(Path sourceDimension, Path stagingDimension,
            boolean cavesNotCliffsOverworld, int terrainSchema, long lastUpdate)
            throws IOException {
        if (Files.exists(stagingDimension)) {
            throw new IOException("Cubic import staging path already exists: " + stagingDimension);
        }
        Files.createDirectories(stagingDimension);
        Path spoolPath = stagingDimension.resolve("cubes.spool");
        long cubeCount;
        long[] columnCount = {0L};
        long discardedLookahead = 0L;

        try (CubicNbtSpool spool = new CubicNbtSpool(spoolPath)) {
            Path cubeRegions = sourceDimension.resolve("region3d");
            if (Files.isDirectory(cubeRegions)) {
                CubicRegionReader.visitCubes(cubeRegions, entry -> spool.append(
                        entry.getX(), entry.getY().getAsInt(), entry.getZ(), entry.getNbt()));
            }
            spool.force();
            cubeCount = spool.getCubeCount();

            Path columnRegions = sourceDimension.resolve("region2d");
            if (Files.isDirectory(columnRegions)) {
                CubicRegionReader.visitColumns(columnRegions, entry -> {
                    columnCount[0]++;
                    Map<Integer, NBTTagCompound> cubes =
                            spool.takeColumn(entry.getX(), entry.getZ());
                    NBTTagCompound converted;
                    try {
                        converted = cavesNotCliffsOverworld
                                ? CubicColumnConverter.convertOverworld(entry.getNbt(), cubes,
                                terrainSchema, lastUpdate)
                                : CubicColumnConverter.convertVanillaDimension(
                                entry.getNbt(), cubes, lastUpdate);
                    } catch (CubicColumnConversionException exception) {
                        throw new IOException("Cannot convert " + entry.describeCoordinates()
                                + " from " + entry.getSource() + ": " + exception.getMessage(),
                                exception);
                    }
                    writeAndVerify(stagingDimension, entry.getX(), entry.getZ(), converted);
                });
            }

            for (CubicNbtSpool.ColumnKey key : spool.remainingColumns()) {
                Map<Integer, NBTTagCompound> cubes = spool.takeColumn(key.getX(), key.getZ());
                try {
                    CubicColumnConverter.validateDiscardableLookahead(cubes);
                } catch (CubicColumnConversionException exception) {
                    throw new IOException("Cannot discard cube-only column " + key + " in "
                            + sourceDimension + ": " + exception.getMessage(), exception);
                }
                discardedLookahead++;
            }
        } finally {
            // No world is loaded while the importer runs, so closing the global Anvil cache here
            // cannot race ordinary chunk I/O and is required before hashing or renaming outputs.
            RegionFileCache.clearRegionFileReferences();
        }

        Path outputRegion = stagingDimension.resolve("region");
        List<CubicImportJournal.FileRecord> outputs;
        if (Files.isDirectory(outputRegion)) {
            forceRegularFiles(outputRegion);
            outputs = CubicImportJournal.captureOutputs(stagingDimension, outputRegion);
            if (columnCount[0] > 0 && outputs.isEmpty()) {
                throw new IOException("Anvil staging wrote no region files for " + columnCount[0]
                        + " columns from " + sourceDimension);
            }
        } else {
            outputs = Collections.emptyList();
            if (columnCount[0] > 0) {
                throw new IOException("Anvil staging produced no region directory for "
                        + sourceDimension);
            }
        }
        return new Result(columnCount[0], cubeCount, discardedLookahead, outputs);
    }

    private static void writeAndVerify(Path stagingDimension, int chunkX, int chunkZ,
            NBTTagCompound expected) throws IOException {
        DataOutputStream output = RegionFileCache.getChunkOutputStream(
                stagingDimension.toFile(), chunkX, chunkZ);
        if (output == null) {
            throw new IOException("Anvil refused an output stream for column (" + chunkX
                    + "," + chunkZ + ")");
        }
        try (DataOutputStream stream = output) {
            CompressedStreamTools.write(expected, stream);
        }

        DataInputStream input = RegionFileCache.getChunkInputStream(
                stagingDimension.toFile(), chunkX, chunkZ);
        if (input == null) {
            throw new IOException("Anvil could not reopen staged column (" + chunkX
                    + "," + chunkZ + ")");
        }
        NBTTagCompound actual;
        try (DataInputStream stream = input) {
            actual = CompressedStreamTools.read(stream);
        }
        if (!expected.equals(actual)) {
            throw new IOException("Logical verification failed for staged column (" + chunkX
                    + "," + chunkZ + ")");
        }
    }

    private static void forceRegularFiles(Path directory) throws IOException {
        try (Stream<Path> stream = Files.walk(directory)) {
            for (Path path : (Iterable<Path>) stream::iterator) {
                if (Files.isSymbolicLink(path)) {
                    throw new IOException("Cubic import staging contains a symbolic link: " + path);
                }
                if (Files.isRegularFile(path)) {
                    CubicImportJournal.forceFile(path);
                }
            }
        }
    }
}

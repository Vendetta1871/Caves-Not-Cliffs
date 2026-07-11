package net.celestiald.cavesnotcliffs.migration;

import net.celestiald.cavesnotcliffs.world.CavesNotCliffsFiniteWorldType;
import net.celestiald.cavesnotcliffs.world.CavesNotCliffsWorldData;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.WorldInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/** Pre-world, one-time, transactional importer for draft-v2 CubicChunks saves. */
public final class LegacyCubicSaveImporter {
    private static final Logger LOGGER = LogManager.getLogger("CavesNotCliffs/CubicImport");
    private static final String STAGING_DIRECTORY = ".cavesnotcliffs-cubic-staging";

    private LegacyCubicSaveImporter() {
    }

    public static void prepareForServer(MinecraftServer server) {
        String folderName = server.getFolderName();
        if (folderName == null || folderName.trim().isEmpty()) {
            throw new IllegalStateException("Cannot resolve the world folder before cubic save import");
        }
        ISaveFormat saves = server.getActiveAnvilConverter();
        File levelFile = saves.getFile(folderName, "level.dat");
        Path worldRoot = levelFile.toPath().toAbsolutePath().normalize().getParent();
        if (worldRoot == null) {
            throw new IllegalStateException("Cannot resolve the world root for " + levelFile);
        }

        try {
            List<Path> dimensions = discoverDimensions(worldRoot);
            List<CubicImportJournal.FileRecord> sources =
                    CubicImportJournal.captureSources(worldRoot, dimensions);
            Path journalPath = worldRoot.resolve(CubicImportJournal.FILE_NAME);
            if (sources.isEmpty() && !Files.exists(journalPath)) {
                return;
            }

            WorldInfo worldInfo = readWorldInfo(levelFile.toPath());
            if (worldInfo == null) {
                throw new IOException("Cubic storage exists but level.dat and level.dat_old are unreadable");
            }
            int terrainSchema = terrainSchema(worldInfo);
            if (terrainSchema == 0) {
                throw new IOException("This save contains CubicChunks storage but is not a persisted "
                        + "Caves Not Cliffs world. Refusing to generate finite chunks over it; restore "
                        + "CubicChunks or convert that world with its owning mod.");
            }
            importWorld(worldRoot, terrainSchema, worldInfo.getWorldTotalTime(),
                    dimensions, sources);
        } catch (IOException exception) {
            throw new IllegalStateException("Caves Not Cliffs could not safely import legacy "
                    + "CubicChunks storage in " + worldRoot + ": " + exception.getMessage(), exception);
        }
    }

    static boolean importWorld(Path worldRoot, int terrainSchema, long lastUpdate)
            throws IOException {
        Path normalizedRoot = worldRoot.toAbsolutePath().normalize();
        List<Path> dimensions = discoverDimensions(normalizedRoot);
        List<CubicImportJournal.FileRecord> sources =
                CubicImportJournal.captureSources(normalizedRoot, dimensions);
        return importWorld(normalizedRoot, terrainSchema, lastUpdate, dimensions, sources);
    }

    private static boolean importWorld(Path worldRoot, int terrainSchema, long lastUpdate,
            List<Path> dimensions, List<CubicImportJournal.FileRecord> sources) throws IOException {
        Path journalPath = worldRoot.resolve(CubicImportJournal.FILE_NAME);
        Path temporaryJournal = worldRoot.resolve(CubicImportJournal.TEMP_FILE_NAME);
        Path stagingRoot = worldRoot.resolve(STAGING_DIRECTORY);
        if (Files.exists(temporaryJournal)) {
            throw new IOException("An interrupted journal update remains at " + temporaryJournal
                    + "; inspect the save backup before retrying");
        }
        if (Files.exists(journalPath)) {
            CubicImportJournal existing = CubicImportJournal.read(journalPath);
            if (existing.getState() == CubicImportJournal.State.COMMITTED) {
                verifyCommittedTargets(worldRoot, existing);
                return false;
            }
            throw incomplete(existing.getState(), journalPath, stagingRoot);
        }
        if (Files.exists(stagingRoot)) {
            throw new IOException("Cubic import staging exists without a journal at " + stagingRoot
                    + "; inspect and remove only that importer-owned directory before retrying");
        }
        if (sources.isEmpty()) {
            return false;
        }
        if (terrainSchema != CavesNotCliffsWorldData.LEGACY_SCHEMA
                && terrainSchema != CavesNotCliffsWorldData.CURRENT_SCHEMA) {
            throw new IOException("Unsupported Caves Not Cliffs terrain schema " + terrainSchema);
        }
        preflightTargets(dimensions);

        List<String> dimensionNames = new ArrayList<String>(dimensions.size());
        for (Path dimension : dimensions) {
            dimensionNames.add(relativeDimension(worldRoot, dimension));
        }
        CubicImportJournal journal = CubicImportJournal.create(
                terrainSchema, sources, dimensionNames);
        journal.writeAtomic(journalPath);

        Map<String, StagedDimension> staged = new LinkedHashMap<String, StagedDimension>();
        for (int index = 0; index < dimensions.size(); index++) {
            Path dimension = dimensions.get(index);
            String name = dimensionNames.get(index);
            Path stagingDimension = stagingRoot.resolve(String.format("dimension-%04d", index));
            LOGGER.info("Staging legacy cubic dimension {} from {}", name, dimension);
            CubicDimensionStager.Result result = CubicDimensionStager.stage(
                    dimension, stagingDimension, ".".equals(name), terrainSchema, lastUpdate);
            journal.recordDimension(name, result.getColumns(), result.getCubes(),
                    result.getOutputs());
            journal.writeAtomic(journalPath);
            staged.put(name, new StagedDimension(dimension, stagingDimension, result));
            LOGGER.info("Verified {} columns and {} cubes in {}; discarded {} empty lookahead columns",
                    result.getColumns(), result.getCubes(), name,
                    result.getDiscardedLookaheadColumns());
        }

        journal.transition(CubicImportJournal.State.DISCOVERED,
                CubicImportJournal.State.STAGED);
        journal.writeAtomic(journalPath);
        List<CubicImportJournal.FileRecord> sourcesAfterStaging =
                CubicImportJournal.captureSources(worldRoot, dimensions);
        if (!sources.equals(sourcesAfterStaging)) {
            throw new IOException("Cubic source files changed while staging; no output was committed");
        }
        journal.transition(CubicImportJournal.State.STAGED,
                CubicImportJournal.State.VERIFIED);
        journal.writeAtomic(journalPath);
        preflightTargets(dimensions);
        journal.transition(CubicImportJournal.State.VERIFIED,
                CubicImportJournal.State.COMMITTING);
        journal.writeAtomic(journalPath);

        for (Map.Entry<String, StagedDimension> entry : staged.entrySet()) {
            String name = entry.getKey();
            StagedDimension dimension = entry.getValue();
            if (dimension.result.hasOutputRegion()) {
                Path sourceRegion = dimension.staging.resolve("region");
                Path targetRegion = dimension.target.resolve("region");
                try {
                    Files.move(sourceRegion, targetRegion, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException exception) {
                    throw new IOException("The save filesystem cannot atomically commit dimension "
                            + name + "; source cubic storage remains untouched", exception);
                }
            }
            journal.markDimensionCommitted(name);
            journal.writeAtomic(journalPath);
        }
        journal.transition(CubicImportJournal.State.COMMITTING,
                CubicImportJournal.State.COMMITTED);
        journal.writeAtomic(journalPath);
        deleteTreeBestEffort(stagingRoot);
        LOGGER.info("Committed finite Anvil storage for {} dimensions. Legacy region2d/region3d "
                + "files remain untouched as the rollback source.", dimensions.size());
        return true;
    }

    private static void preflightTargets(List<Path> dimensions) throws IOException {
        for (Path dimension : dimensions) {
            Path target = dimension.resolve("region");
            if (Files.exists(target)) {
                throw new IOException("Refusing to overwrite existing finite region storage at "
                        + target + ". Restore a clean backup or complete the prior import explicitly.");
            }
        }
    }

    private static void verifyCommittedTargets(Path worldRoot, CubicImportJournal journal)
            throws IOException {
        for (String name : journal.getDimensions()) {
            if (!journal.isDimensionCommitted(name)) {
                throw new IOException("Committed cubic import journal still marks dimension "
                        + name + " incomplete");
            }
            List<CubicImportJournal.FileRecord> outputs = journal.getDimensionOutputs(name);
            if (outputs.isEmpty()) {
                continue;
            }
            Path dimension = ".".equals(name) ? worldRoot : worldRoot.resolve(name).normalize();
            if (!dimension.startsWith(worldRoot)) {
                throw new IOException("Committed cubic import journal dimension escapes the world root: "
                        + name);
            }
            Path targetRegion = dimension.resolve("region");
            if (!Files.isDirectory(targetRegion)) {
                throw new IOException("Committed finite region directory is missing: " + targetRegion);
            }
            for (CubicImportJournal.FileRecord output : outputs) {
                Path target = dimension.resolve(output.getPath()).normalize();
                if (!target.startsWith(dimension) || !Files.isRegularFile(target)) {
                    throw new IOException("Committed finite region file is missing: " + target);
                }
            }
        }
    }

    private static IOException incomplete(CubicImportJournal.State state, Path journal,
            Path staging) {
        return new IOException("Cubic import journal is incomplete at state " + state + " ("
                + journal + "). World startup is blocked. Source region2d/region3d files were not "
                + "modified; restore the save backup, or remove only importer-owned finite region "
                + "outputs plus " + staging + " and the journal after verifying rollback.");
    }

    private static int terrainSchema(WorldInfo worldInfo) {
        CavesNotCliffsWorldData persisted = CavesNotCliffsWorldData.read(worldInfo);
        if (persisted != null) {
            return persisted.getTerrainSchema();
        }
        WorldType selected = worldInfo.getTerrainType();
        return selected instanceof CavesNotCliffsFiniteWorldType
                ? ((CavesNotCliffsFiniteWorldType) selected).getTerrainSchema() : 0;
    }

    private static WorldInfo readWorldInfo(Path levelFile) throws IOException {
        IOException primaryFailure = null;
        try {
            WorldInfo primary = readWorldInfoFile(levelFile);
            if (primary != null) {
                return primary;
            }
        } catch (IOException exception) {
            primaryFailure = exception;
        }
        try {
            WorldInfo backup = readWorldInfoFile(levelFile.resolveSibling("level.dat_old"));
            if (backup != null) {
                LOGGER.warn("Using level.dat_old metadata to identify legacy cubic storage because "
                        + "level.dat is unavailable or invalid");
                return backup;
            }
        } catch (IOException backupFailure) {
            if (primaryFailure != null) {
                primaryFailure.addSuppressed(backupFailure);
                throw primaryFailure;
            }
            throw backupFailure;
        }
        if (primaryFailure != null) {
            throw primaryFailure;
        }
        return null;
    }

    private static WorldInfo readWorldInfoFile(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try (InputStream input = new BufferedInputStream(new FileInputStream(file.toFile()))) {
            NBTTagCompound root = CompressedStreamTools.readCompressed(input);
            if (!root.hasKey("Data", 10)) {
                throw new IOException(file + " has no compound Data tag");
            }
            return new WorldInfo(root.getCompoundTag("Data"));
        }
    }

    private static List<Path> discoverDimensions(Path worldRoot) throws IOException {
        if (!Files.isDirectory(worldRoot)) {
            return Collections.emptyList();
        }
        Set<Path> dimensions = new LinkedHashSet<Path>();
        try (Stream<Path> stream = Files.walk(worldRoot, 6)) {
            for (Path path : (Iterable<Path>) stream::iterator) {
                Path name = path.getFileName();
                if (name == null || !Files.isDirectory(path)) {
                    continue;
                }
                String text = name.toString();
                if ("region2d".equals(text) || "region3d".equals(text)) {
                    Path dimension = path.getParent().toAbsolutePath().normalize();
                    if (!dimension.startsWith(worldRoot)) {
                        throw new IOException("Cubic dimension escapes the world root: " + dimension);
                    }
                    dimensions.add(dimension);
                }
            }
        }
        List<Path> ordered = new ArrayList<Path>(dimensions);
        Collections.sort(ordered, Comparator.comparing(
                dimension -> relativeDimension(worldRoot, dimension)));
        return ordered;
    }

    private static String relativeDimension(Path worldRoot, Path dimension) {
        Path relative = worldRoot.relativize(dimension);
        return relative.getNameCount() == 0 || relative.toString().isEmpty()
                ? "." : relative.toString().replace(File.separatorChar, '/');
    }

    private static void deleteTreeBestEffort(Path root) {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(root)) {
            List<Path> paths = new ArrayList<Path>();
            for (Path path : (Iterable<Path>) stream::iterator) {
                paths.add(path);
            }
            Collections.sort(paths, Comparator.reverseOrder());
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        } catch (IOException exception) {
            LOGGER.warn("Committed cubic import but could not remove staging directory {}",
                    root, exception);
        }
    }

    private static final class StagedDimension {
        private final Path target;
        private final Path staging;
        private final CubicDimensionStager.Result result;

        private StagedDimension(Path target, Path staging, CubicDimensionStager.Result result) {
            this.target = target;
            this.staging = staging;
            this.result = result;
        }
    }
}

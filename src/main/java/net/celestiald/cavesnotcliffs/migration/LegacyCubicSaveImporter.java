package net.celestiald.cavesnotcliffs.migration;

import net.celestiald.cavesnotcliffs.world.CavesNotCliffsFiniteWorldType;
import net.celestiald.cavesnotcliffs.world.CavesNotCliffsWorldData;
import net.celestiald.cavesnotcliffs.world.V118BlockStateMapper;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.WorldInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Pre-world, one-time, transactional importer for draft-v2 CubicChunks saves. */
public final class LegacyCubicSaveImporter {
    private static final Logger LOGGER = LogManager.getLogger("CavesNotCliffs/CubicImport");
    private static final String STAGING_DIRECTORY = ".cavesnotcliffs-cubic-staging";
    private static final Pattern STAGED_REGION_FILE = Pattern.compile(
            "r\\.-?\\d+\\.-?\\d+\\.mca");

    private LegacyCubicSaveImporter() {
    }

    interface PrecommitHook {
        void afterStaging(Path worldRoot) throws IOException;
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
            CubicImportJournal completed = existingJournal(worldRoot, levelFile.toPath());
            if (completed != null) {
                finishCommittedImport(worldRoot, levelFile.toPath(), completed);
                return;
            }
            SourceDiscovery sources = discoverSources(worldRoot, levelFile.toPath());
            if (sources.files.isEmpty()) {
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
            // Fail before committing finite regions if this CubicChunks version left metadata
            // that the narrow, deterministic cleaner does not understand.
            LegacyCubicLevelMetadata.Result metadata = LegacyCubicLevelMetadata.inspect(
                    levelFile.toPath(), true);
            CubicDimensionStager.CubeVerifier lookaheadVerifier = lookaheadVerifier(
                    terrainSchema, worldInfo, levelFile.toPath());
            boolean imported = importWorld(worldRoot, terrainSchema, worldInfo.getWorldTotalTime(),
                    levelFile.toPath(), sources, lookaheadVerifier, null);
            if (metadata.changed() && !imported) {
                finishCommittedStorage(worldRoot, CubicImportJournal.read(
                        worldRoot.resolve(CubicImportJournal.FILE_NAME)), true);
            }
            cleanLevelMetadata(levelFile.toPath());
        } catch (IOException exception) {
            throw new IllegalStateException("Caves Not Cliffs could not safely import legacy "
                    + "CubicChunks storage in " + worldRoot + ": " + exception.getMessage(), exception);
        }
    }

    static boolean importWorld(Path worldRoot, int terrainSchema, long lastUpdate)
            throws IOException {
        return importWorld(worldRoot, terrainSchema, lastUpdate, null, null);
    }

    static boolean importWorld(Path worldRoot, int terrainSchema, long lastUpdate,
            Path levelFile, PrecommitHook precommitHook) throws IOException {
        Path normalizedRoot = worldRoot.toAbsolutePath().normalize();
        CubicImportJournal completed = existingJournal(normalizedRoot, levelFile);
        if (completed != null) {
            finishCommittedStorage(normalizedRoot, completed, false);
            return false;
        }
        SourceDiscovery sources = discoverSources(normalizedRoot, levelFile);
        return importWorld(normalizedRoot, terrainSchema, lastUpdate, levelFile,
                sources, null, precommitHook);
    }

    private static boolean importWorld(Path worldRoot, int terrainSchema, long lastUpdate,
            Path levelFile, SourceDiscovery sources,
            CubicDimensionStager.CubeVerifier lookaheadVerifier,
            PrecommitHook precommitHook) throws IOException {
        Path journalPath = worldRoot.resolve(CubicImportJournal.FILE_NAME);
        Path stagingRoot = worldRoot.resolve(STAGING_DIRECTORY);
        CubicImportJournal completed = existingJournal(worldRoot, levelFile);
        if (completed != null) {
            finishCommittedStorage(worldRoot, completed, false);
            return false;
        }
        if (sources.files.isEmpty()) {
            return false;
        }
        if (terrainSchema != CavesNotCliffsWorldData.LEGACY_SCHEMA
                && terrainSchema != CavesNotCliffsWorldData.CURRENT_SCHEMA) {
            throw new IOException("Unsupported Caves Not Cliffs terrain schema " + terrainSchema);
        }
        preflightTargets(sources.dimensions);

        List<String> dimensionNames = new ArrayList<String>(sources.dimensions.size());
        for (Path dimension : sources.dimensions) {
            dimensionNames.add(relativeDimension(worldRoot, dimension));
        }
        CubicImportJournal journal = CubicImportJournal.create(
                terrainSchema, sources.files, dimensionNames);
        journal.writeAtomic(journalPath);

        Map<String, StagedDimension> staged = new LinkedHashMap<String, StagedDimension>();
        for (int index = 0; index < sources.dimensions.size(); index++) {
            Path dimension = sources.dimensions.get(index);
            String name = dimensionNames.get(index);
            Path stagingDimension = stagingRoot.resolve(stagingDimensionName(index));
            LOGGER.info("Staging legacy cubic dimension {} from {}", name, dimension);
            CubicDimensionStager.Result result = CubicDimensionStager.stage(
                    dimension, stagingDimension, ".".equals(name), terrainSchema, lastUpdate,
                    ".".equals(name) ? lookaheadVerifier : null);
            journal.recordDimension(name, result.getColumns(), result.getCubes(),
                    result.getOutputs());
            journal.writeAtomic(journalPath);
            staged.put(name, new StagedDimension(dimension, stagingDimension, result));
            LOGGER.info("Verified {} columns and {} cubes in {}; discarded {} empty lookahead "
                            + "columns and approved {} native terrain columns for regeneration",
                    result.getColumns(), result.getCubes(), name,
                    result.getDiscardedLookaheadColumns(),
                    result.getVerifiedRegenerationColumns());
        }

        journal.transition(CubicImportJournal.State.DISCOVERED,
                CubicImportJournal.State.STAGED);
        journal.writeAtomic(journalPath);
        if (precommitHook != null) {
            precommitHook.afterStaging(worldRoot);
        }
        SourceDiscovery sourcesAfterStaging = discoverSources(worldRoot, levelFile);
        if (!sources.dimensions.equals(sourcesAfterStaging.dimensions)) {
            throw new IOException("Cubic dimension set changed while staging; no output was "
                    + "committed");
        }
        if (!sources.files.equals(sourcesAfterStaging.files)) {
            throw new IOException("Cubic source files changed while staging; no output was "
                    + "committed");
        }
        journal.transition(CubicImportJournal.State.STAGED,
                CubicImportJournal.State.VERIFIED);
        journal.writeAtomic(journalPath);
        preflightTargets(sources.dimensions);
        journal.transition(CubicImportJournal.State.VERIFIED,
                CubicImportJournal.State.COMMITTING);
        journal.writeAtomic(journalPath);

        for (Map.Entry<String, StagedDimension> entry : staged.entrySet()) {
            String name = entry.getKey();
            StagedDimension dimension = entry.getValue();
            if (dimension.result.hasOutputRegion()) {
                Path sourceRegion = dimension.staging.resolve("region");
                Path targetRegion = dimension.target.resolve("region");
                verifyOutputManifest(name, dimension.result.getOutputs(),
                        dimension.staging, sourceRegion, "staged");
                try {
                    Files.move(sourceRegion, targetRegion, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException exception) {
                    throw new IOException("The save filesystem cannot atomically commit dimension "
                            + name + "; source cubic storage remains untouched", exception);
                }
                CubicImportJournal.forceDirectoryBestEffort(dimension.target);
                verifyOutputManifest(name, dimension.result.getOutputs(),
                        dimension.target, targetRegion, "target");
            }
            journal.markDimensionCommitted(name);
            journal.writeAtomic(journalPath);
        }
        // This runs while Minecraft's save-session lock excludes cooperating writers. It closes
        // the last integrity window before the durable COMMITTED state permits metadata cleanup.
        verifyCommittedTargets(worldRoot, journal, true);
        journal.transition(CubicImportJournal.State.COMMITTING,
                CubicImportJournal.State.COMMITTED);
        journal.writeAtomic(journalPath);
        deleteImporterStaging(stagingRoot, journal);
        LOGGER.info("Committed finite Anvil storage for {} dimensions. Legacy region2d/region3d "
                + "files remain untouched as the rollback source.", sources.dimensions.size());
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
        verifyCommittedTargets(worldRoot, journal, false);
    }

    static void verifyCommittedTargets(Path worldRoot, CubicImportJournal journal,
            boolean verifyHashes) throws IOException {
        for (String name : journal.getDimensions()) {
            if (!journal.isDimensionCommitted(name)) {
                throw new IOException("Committed cubic import journal still marks dimension "
                        + name + " incomplete");
            }
            List<CubicImportJournal.FileRecord> outputs = journal.getDimensionOutputs(name);
            Path dimension = ".".equals(name) ? worldRoot : worldRoot.resolve(name).normalize();
            if (!dimension.startsWith(worldRoot)) {
                throw new IOException("Committed cubic import journal dimension escapes the world root: "
                        + name);
            }
            Path targetRegion = dimension.resolve("region");
            if (outputs.isEmpty()) {
                if (verifyHashes && Files.exists(targetRegion)
                        && !CubicImportJournal.captureOutputs(
                        dimension, targetRegion).isEmpty()) {
                    throw new IOException("Committed finite region output is not recorded for "
                            + name);
                }
                continue;
            }
            if (!Files.isDirectory(targetRegion) || Files.isSymbolicLink(targetRegion)) {
                throw new IOException("Committed finite region directory is missing: " + targetRegion);
            }
            for (CubicImportJournal.FileRecord output : outputs) {
                Path target = dimension.resolve(output.getPath()).normalize();
                if (!target.startsWith(dimension) || !Files.isRegularFile(target)) {
                    throw new IOException("Committed finite region file is missing: " + target);
                }
            }
            if (verifyHashes) {
                List<CubicImportJournal.FileRecord> actual =
                        CubicImportJournal.captureOutputs(dimension, targetRegion);
                if (!outputs.equals(actual)) {
                    throw new IOException("Committed finite region files changed before legacy "
                            + "metadata cleanup for " + name);
                }
            }
        }
    }

    private static CubicImportJournal existingJournal(Path worldRoot, Path levelFile)
            throws IOException {
        Path journal = worldRoot.resolve(CubicImportJournal.FILE_NAME);
        Path temporary = worldRoot.resolve(CubicImportJournal.TEMP_FILE_NAME);
        Path staging = worldRoot.resolve(STAGING_DIRECTORY);
        boolean hasJournal = Files.exists(journal, LinkOption.NOFOLLOW_LINKS);
        boolean hasTemporary = Files.exists(temporary, LinkOption.NOFOLLOW_LINKS);
        if (!hasJournal) {
            if (hasTemporary) {
                requireImporterFile(temporary, "temporary cubic import journal");
                CubicImportJournal temporaryJournal = CubicImportJournal.read(temporary);
                if (temporaryJournal.getState() != CubicImportJournal.State.DISCOVERED) {
                    throw new IOException("Temporary cubic import journal has no durable journal "
                            + "and is already at state " + temporaryJournal.getState()
                            + "; startup is blocked because that state is ambiguous");
                }
                verifyRecoverySources(worldRoot, levelFile, temporaryJournal);
                if (Files.exists(staging, LinkOption.NOFOLLOW_LINKS)
                        || hasAnyFiniteRegion(worldRoot)) {
                    throw new IOException("An interrupted journal update at " + temporary
                            + " has no durable journal, but staging or finite region storage "
                            + "exists. Startup is blocked because that state is ambiguous.");
                }
                Files.delete(temporary);
                CubicImportJournal.forceDirectoryBestEffort(worldRoot);
            }
            if (Files.exists(staging, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Cubic import staging exists without a journal at " + staging
                        + "; inspect and remove only that importer-owned directory before retrying");
            }
            return null;
        }
        CubicImportJournal existing = CubicImportJournal.read(journal);
        if (hasTemporary) {
            requireImporterFile(temporary, "temporary cubic import journal");
            CubicImportJournal interrupted = null;
            try {
                interrupted = CubicImportJournal.readDecoded(temporary);
            } catch (IOException incompleteWrite) {
                LOGGER.warn("Discarding an incomplete temporary cubic import journal because "
                                + "the durable journal remains authoritative: {}",
                        temporary, incompleteWrite);
            }
            if (interrupted != null) {
                interrupted.validateComplete();
                interrupted.validateTemporarySuccessorOf(existing);
            }
            Files.delete(temporary);
            CubicImportJournal.forceDirectoryBestEffort(worldRoot);
        }
        CubicImportJournal.State state = existing.getState();
        if (state == CubicImportJournal.State.COMMITTED) {
            return existing;
        }
        verifyRecoverySources(worldRoot, levelFile, existing);
        validateStagingOwnership(staging, existing);
        if (state == CubicImportJournal.State.COMMITTING) {
            reconcileCommitting(worldRoot, journal, staging, existing);
            return existing;
        }
        recoverPrecommit(worldRoot, journal, staging, existing);
        return null;
    }

    private static void recoverPrecommit(Path worldRoot, Path journal, Path staging,
            CubicImportJournal existing) throws IOException {
        for (String name : existing.getDimensions()) {
            Path targetRegion = dimensionPath(worldRoot, name).resolve("region");
            if (Files.exists(targetRegion, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Cubic import journal is incomplete at state "
                        + existing.getState() + ", but finite region storage exists at "
                        + targetRegion + ". Refusing automatic rollback because output may have "
                        + "been committed.");
            }
        }
        deleteImporterStaging(staging, existing);
        Files.delete(journal);
        CubicImportJournal.forceDirectoryBestEffort(worldRoot);
        LOGGER.info("Discarded importer-owned {} cubic staging after verifying that no finite "
                        + "region was committed; restarting the import",
                existing.getState());
    }

    private static void reconcileCommitting(Path worldRoot, Path journal, Path staging,
            CubicImportJournal existing) throws IOException {
        List<String> dimensions = existing.getDimensions();
        List<CommitRecovery> recoveries = new ArrayList<CommitRecovery>(dimensions.size());
        for (int index = 0; index < dimensions.size(); index++) {
            String name = dimensions.get(index);
            Path targetDimension = dimensionPath(worldRoot, name);
            Path targetRegion = targetDimension.resolve("region");
            Path stagedDimension = staging.resolve(stagingDimensionName(index));
            Path stagedRegion = stagedDimension.resolve("region");
            List<CubicImportJournal.FileRecord> expected =
                    existing.getDimensionOutputs(name);
            boolean targetExists = Files.exists(targetRegion, LinkOption.NOFOLLOW_LINKS);
            boolean stagedExists = Files.exists(stagedRegion, LinkOption.NOFOLLOW_LINKS);
            boolean marked = existing.isDimensionCommitted(name);

            if (expected.isEmpty()) {
                if (targetExists) {
                    throw ambiguousCommit(name, "journal records no output, but target region exists");
                }
                if (stagedExists && !CubicImportJournal.captureOutputs(
                        stagedDimension, stagedRegion).isEmpty()) {
                    throw ambiguousCommit(name,
                            "journal records no output, but staged region contains files");
                }
                recoveries.add(new CommitRecovery(name, targetDimension, targetRegion,
                        stagedDimension, stagedRegion, expected, false, !marked));
                continue;
            }

            if (targetExists && stagedExists) {
                throw ambiguousCommit(name, "both staged and target regions exist");
            }
            if (!targetExists && !stagedExists) {
                throw ambiguousCommit(name, "neither staged nor target region exists");
            }
            if (marked && !targetExists) {
                throw ambiguousCommit(name,
                        "journal marks the dimension committed, but only staged output exists");
            }

            if (targetExists) {
                verifyOutputManifest(name, expected, targetDimension, targetRegion, "target");
            } else {
                verifyOutputManifest(name, expected, stagedDimension, stagedRegion, "staged");
            }
            recoveries.add(new CommitRecovery(name, targetDimension, targetRegion,
                    stagedDimension, stagedRegion, expected, !targetExists, !marked));
        }

        for (CommitRecovery recovery : recoveries) {
            if (recovery.expected.isEmpty()
                    && Files.exists(recovery.targetRegion, LinkOption.NOFOLLOW_LINKS)) {
                throw ambiguousCommit(recovery.name,
                        "target region appeared while recovery was being validated");
            }
            if (recovery.move) {
                if (Files.exists(recovery.targetRegion, LinkOption.NOFOLLOW_LINKS)) {
                    throw ambiguousCommit(recovery.name,
                            "target region appeared while recovery was being validated");
                }
                verifyOutputManifest(recovery.name, recovery.expected,
                        recovery.stagedDimension, recovery.stagedRegion, "staged");
                try {
                    Files.move(recovery.stagedRegion, recovery.targetRegion,
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException exception) {
                    throw new IOException("The save filesystem cannot atomically resume cubic "
                            + "import for dimension " + recovery.name, exception);
                }
                CubicImportJournal.forceDirectoryBestEffort(recovery.targetDimension);
                verifyOutputManifest(recovery.name, recovery.expected,
                        recovery.targetDimension, recovery.targetRegion, "target");
            }
            if (recovery.mark) {
                existing.markDimensionCommitted(recovery.name);
                existing.writeAtomic(journal);
            }
        }
        verifyCommittedTargets(worldRoot, existing, true);
        existing.transition(CubicImportJournal.State.COMMITTING,
                CubicImportJournal.State.COMMITTED);
        existing.writeAtomic(journal);
        deleteImporterStaging(staging, existing);
        LOGGER.info("Recovered and completed an interrupted cubic import commit");
    }

    private static IOException ambiguousCommit(String dimension, String reason) {
        return new IOException("Cannot safely recover cubic import for dimension " + dimension
                + ": " + reason + ". No existing finite output was overwritten.");
    }

    private static void verifyOutputManifest(String name,
            List<CubicImportJournal.FileRecord> expected, Path base, Path region,
            String location) throws IOException {
        List<CubicImportJournal.FileRecord> actual =
                CubicImportJournal.captureOutputs(base, region);
        if (!expected.equals(actual)) {
            throw ambiguousCommit(name, location + " output does not match the journal hashes");
        }
    }

    private static void verifyRecoverySources(Path worldRoot, Path levelFile,
            CubicImportJournal existing) throws IOException {
        SourceDiscovery current = discoverSources(worldRoot, levelFile);
        List<String> dimensions = new ArrayList<String>(current.dimensions.size());
        for (Path dimension : current.dimensions) {
            dimensions.add(relativeDimension(worldRoot, dimension));
        }
        if (!existing.getDimensions().equals(dimensions)
                || !existing.getSources().equals(current.files)) {
            throw new IOException("Legacy cubic sources changed after the import journal was "
                    + "written. Startup is blocked so recovery cannot discard or commit stale "
                    + "output.");
        }
    }

    private static void validateStagingOwnership(Path staging,
            CubicImportJournal existing) throws IOException {
        if (!Files.exists(staging, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        if (Files.isSymbolicLink(staging)
                || !Files.isDirectory(staging, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Cubic import staging is not an owned directory: " + staging);
        }
        Set<String> dimensionDirectories = new HashSet<String>();
        for (int index = 0; index < existing.getDimensions().size(); index++) {
            dimensionDirectories.add(stagingDimensionName(index));
        }
        try (Stream<Path> stream = Files.walk(staging)) {
            for (Path path : (Iterable<Path>) stream::iterator) {
                if (path.equals(staging)) {
                    continue;
                }
                if (Files.isSymbolicLink(path)) {
                    throw new IOException("Cubic import staging contains a symbolic link: " + path);
                }
                Path relative = staging.relativize(path);
                String first = relative.getName(0).toString();
                boolean allowed;
                if (relative.getNameCount() == 1) {
                    allowed = dimensionDirectories.contains(first)
                            && Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS);
                } else if (relative.getNameCount() == 2) {
                    String second = relative.getName(1).toString();
                    allowed = dimensionDirectories.contains(first)
                            && (("cubes.spool".equals(second)
                            && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                            || ("region".equals(second)
                            && Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)));
                } else if (relative.getNameCount() == 3) {
                    allowed = dimensionDirectories.contains(first)
                            && "region".equals(relative.getName(1).toString())
                            && STAGED_REGION_FILE.matcher(
                            relative.getName(2).toString()).matches()
                            && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS);
                } else {
                    allowed = false;
                }
                if (!allowed) {
                    throw new IOException("Cubic import staging contains an unowned path: " + path);
                }
            }
        }
    }

    private static void deleteImporterStaging(Path staging,
            CubicImportJournal journal) throws IOException {
        validateStagingOwnership(staging, journal);
        if (!Files.exists(staging, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(staging)) {
            List<Path> paths = new ArrayList<Path>();
            for (Path path : (Iterable<Path>) stream::iterator) {
                paths.add(path);
            }
            Collections.sort(paths, Comparator.reverseOrder());
            for (Path path : paths) {
                Files.delete(path);
            }
        }
        CubicImportJournal.forceDirectoryBestEffort(staging.getParent());
    }

    private static void requireImporterFile(Path path, String description) throws IOException {
        if (Files.isSymbolicLink(path)
                || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(description + " is not a regular importer-owned file: " + path);
        }
    }

    private static boolean hasAnyFiniteRegion(Path worldRoot) throws IOException {
        if (Files.exists(worldRoot.resolve("region"), LinkOption.NOFOLLOW_LINKS)) {
            return true;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(worldRoot)) {
            for (Path child : stream) {
                String name = child.getFileName() == null ? "" : child.getFileName().toString();
                if (name.matches("DIM-?\\d+")
                        && Files.exists(child.resolve("region"), LinkOption.NOFOLLOW_LINKS)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Path dimensionPath(Path worldRoot, String name) throws IOException {
        Path dimension = ".".equals(name) ? worldRoot : worldRoot.resolve(name).normalize();
        if (!dimension.startsWith(worldRoot)) {
            throw new IOException("Cubic import journal dimension escapes the world root: " + name);
        }
        return dimension;
    }

    private static String stagingDimensionName(int index) {
        return String.format(Locale.ROOT, "dimension-%04d", index);
    }

    private static void cleanLevelMetadata(Path levelFile) throws IOException {
        LegacyCubicLevelMetadata.Result cleaned = LegacyCubicLevelMetadata.clean(levelFile);
        if (cleaned.changed()) {
            LOGGER.info("Removed {} CubicChunks registries, {} stale mod entries, and the "
                            + "cubic-world flag={} from level.dat. The byte-identical original "
                            + "is preserved as {}.",
                    cleaned.getRemovedRegistries(), cleaned.getRemovedMods(),
                    cleaned.removedWorldFlag(), LegacyCubicLevelMetadata.BACKUP_FILE_NAME);
        }
    }

    static void finishCommittedImport(Path worldRoot, Path levelFile,
            CubicImportJournal journal) throws IOException {
        LegacyCubicLevelMetadata.Result metadata =
                LegacyCubicLevelMetadata.inspect(levelFile, false);
        finishCommittedStorage(worldRoot, journal, metadata.changed());
        cleanLevelMetadata(levelFile);
    }

    private static void finishCommittedStorage(Path worldRoot, CubicImportJournal journal,
            boolean verifyHashes) throws IOException {
        verifyCommittedTargets(worldRoot, journal, verifyHashes);
        deleteImporterStaging(worldRoot.resolve(STAGING_DIRECTORY), journal);
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

    private static CubicDimensionStager.CubeVerifier lookaheadVerifier(int terrainSchema,
            WorldInfo worldInfo, Path levelFile) throws IOException {
        if (terrainSchema != CavesNotCliffsWorldData.CURRENT_SCHEMA) {
            return null;
        }
        final CavesNotCliffsWorldData contract = CavesNotCliffsWorldData.read(worldInfo);
        if (contract == null) {
            throw new IOException("Schema-2 cubic storage has no persisted Caves Not Cliffs "
                    + "generator contract in level.dat");
        }
        final LegacyCubicRegistrySnapshot registry =
                LegacyCubicRegistrySnapshot.read(levelFile);
        final long seed = worldInfo.getSeed();
        return new CubicDimensionStager.CubeVerifier() {
            private LegacySchema2LookaheadOracle oracle;

            @Override
            public void verifyCube(int cubeX, int cubeY, int cubeZ,
                    NBTTagCompound cubeRoot) throws CubicColumnConversionException {
                if (oracle == null) {
                    V118BlockStateMapper mapper;
                    try {
                        mapper = V118BlockStateMapper.fromRegisteredBlocks();
                    } catch (RuntimeException exception) {
                        throw new CubicColumnConversionException(
                                "Could not resolve registered schema-2 terrain states", exception);
                    }
                    oracle = LegacySchema2LookaheadOracle.create(
                            contract, seed, registry, mapper);
                }
                oracle.verifyCube(cubeX, cubeY, cubeZ, cubeRoot);
            }
        };
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
        NBTTagCompound root = BoundedNbtReader.readCompressed(
                file, "legacy cubic world metadata");
        if (!root.hasKey("Data", 10)) {
            throw new IOException(file + " has no compound Data tag");
        }
        return new WorldInfo(root.getCompoundTag("Data"));
    }

    private static LegacyCubicDimensionMetadata.Result discoverDimensions(Path worldRoot)
            throws IOException {
        LegacyCubicDimensionMetadata.Result result =
                LegacyCubicDimensionMetadata.discoverAndValidate(worldRoot);
        Path normalizedRoot = worldRoot.toAbsolutePath().normalize();
        if (!result.getTrueDimensionRoots().isEmpty()
                && !result.getTrueDimensionRoots().contains(normalizedRoot)) {
            throw new IOException("The Overworld is not marked as a supported -64..320 "
                    + "CubicChunks dimension in data/cubicChunksData.dat");
        }
        return result;
    }

    private static SourceDiscovery discoverSources(Path worldRoot, Path levelFile)
            throws IOException {
        LegacyCubicDimensionMetadata.Result discovery = discoverDimensions(worldRoot);
        if (discovery.getTrueDimensionRoots().isEmpty()) {
            return new SourceDiscovery(Collections.<Path>emptyList(),
                    Collections.<CubicImportJournal.FileRecord>emptyList());
        }
        List<Path> dimensions = discovery.getTrueDimensionRoots();
        List<Path> metadata = new ArrayList<Path>(discovery.getMetadataFiles());
        if (levelFile != null) {
            metadata.addAll(LegacyCubicPositionMetadata.validate(
                    worldRoot, levelFile).getSourceFiles());
        }
        metadata.addAll(LegacyCubicTicketMetadata.validate(
                dimensionRoots(discovery)).getSourceFiles());
        metadata.addAll(LegacyCubicStructureMetadata.validate(
                dimensions).getSourceFiles());
        return new SourceDiscovery(dimensions,
                CubicImportJournal.captureSources(worldRoot, dimensions, metadata));
    }

    private static List<Path> dimensionRoots(LegacyCubicDimensionMetadata.Result discovery) {
        List<Path> result = new ArrayList<Path>(discovery.getTrueDimensionRoots());
        for (Path metadata : discovery.getMetadataFiles()) {
            Path dataDirectory = metadata.getParent();
            Path dimension = dataDirectory == null ? null : dataDirectory.getParent();
            if (dimension != null && !result.contains(dimension)) {
                result.add(dimension);
            }
        }
        Collections.sort(result, Comparator.comparing(Path::toString));
        return result;
    }

    private static String relativeDimension(Path worldRoot, Path dimension) {
        Path relative = worldRoot.relativize(dimension);
        return relative.getNameCount() == 0 || relative.toString().isEmpty()
                ? "." : relative.toString().replace(File.separatorChar, '/');
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

    private static final class CommitRecovery {
        private final String name;
        private final Path targetDimension;
        private final Path targetRegion;
        private final Path stagedDimension;
        private final Path stagedRegion;
        private final List<CubicImportJournal.FileRecord> expected;
        private final boolean move;
        private final boolean mark;

        private CommitRecovery(String name, Path targetDimension, Path targetRegion,
                Path stagedDimension, Path stagedRegion,
                List<CubicImportJournal.FileRecord> expected, boolean move, boolean mark) {
            this.name = name;
            this.targetDimension = targetDimension;
            this.targetRegion = targetRegion;
            this.stagedDimension = stagedDimension;
            this.stagedRegion = stagedRegion;
            this.expected = expected;
            this.move = move;
            this.mark = mark;
        }
    }

    private static final class SourceDiscovery {
        private final List<Path> dimensions;
        private final List<CubicImportJournal.FileRecord> files;

        private SourceDiscovery(List<Path> dimensions,
                List<CubicImportJournal.FileRecord> files) {
            this.dimensions = dimensions;
            this.files = files;
        }
    }
}

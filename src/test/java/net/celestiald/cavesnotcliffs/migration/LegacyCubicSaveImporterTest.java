package net.celestiald.cavesnotcliffs.migration;

import net.celestiald.cavesnotcliffs.world.CavesNotCliffsWorldData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LegacyCubicSaveImporterTest {
    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void commitsOnceAndLeavesCubicRollbackSourcesUntouched() throws Exception {
        Path world = temporary.newFolder("world").toPath();
        writeCubicMetadata(world);
        CubicDimensionStagerTest.writeExtension(
                world.resolve("region2d/0.0.2dr.ext"), 0,
                CubicDimensionStagerTest.column(0, 0));
        for (int cubeY = -4; cubeY < 20; cubeY++) {
            CubicDimensionStagerTest.writeCube(world, 0, cubeY, 0,
                    CubicDimensionStagerTest.cube(0, cubeY, 0, true, true));
        }
        LegacyCubicStructureMetadataTest.write(world, "Mineshaft.dat",
                LegacyCubicStructureMetadataTest.structureRoot(
                        LegacyCubicStructureMetadataTest.feature(0, 0, 0,
                                LegacyCubicStructureMetadataTest.box(
                                        0, 0, 0, 15, 20, 15))));

        assertTrue(LegacyCubicSaveImporter.importWorld(
                world, CavesNotCliffsWorldData.CURRENT_SCHEMA, 99L));
        assertTrue(Files.isDirectory(world.resolve("region")));
        assertTrue(Files.isRegularFile(world.resolve("region/r.0.0.mca")));
        assertTrue(Files.isDirectory(world.resolve("region2d")));
        assertTrue(Files.isDirectory(world.resolve("region3d")));
        assertFalse(Files.exists(world.resolve(".cavesnotcliffs-cubic-staging")));

        CubicImportJournal journal = CubicImportJournal.read(
                world.resolve(CubicImportJournal.FILE_NAME));
        assertTrue(journal.getState() == CubicImportJournal.State.COMMITTED);
        assertTrue(journal.isDimensionCommitted("."));
        boolean recordedStructure = false;
        for (CubicImportJournal.FileRecord source : journal.getSources()) {
            recordedStructure |= "data/Mineshaft.dat".equals(source.getPath());
        }
        assertTrue(recordedStructure);

        NBTTagCompound vanillaStructure = LegacyCubicStructureMetadataTest.feature(
                0, 0, 0, LegacyCubicStructureMetadataTest.box(
                        0, 0, 0, 15, 20, 15));
        vanillaStructure.removeTag("ChunkY");
        LegacyCubicStructureMetadataTest.write(world, "Mineshaft.dat",
                LegacyCubicStructureMetadataTest.structureRoot(vanillaStructure));
        assertFalse(LegacyCubicSaveImporter.importWorld(
                world, CavesNotCliffsWorldData.CURRENT_SCHEMA, 100L));
    }

    @Test
    public void refusesToDeleteUnownedStagingForAnIncompleteJournal() throws Exception {
        Path world = temporary.newFolder("incomplete").toPath();
        writeCubicMetadata(world);
        Path source = world.resolve("region2d/0.0.2dr");
        Files.createDirectories(source.getParent());
        Files.write(source, "source".getBytes(StandardCharsets.UTF_8));
        List<CubicImportJournal.FileRecord> sources = captureCurrentSources(
                world, Collections.singletonList(world));
        CubicImportJournal.create(2, sources, Collections.singletonList("."))
                .writeAtomic(world.resolve(CubicImportJournal.FILE_NAME));
        Path stagingMarker = writeStagingMarker(world);

        try {
            LegacyCubicSaveImporter.importWorld(world, 2, 0L);
            fail("Expected unowned staging rejection");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("unowned path"));
        }
        assertTrue(Files.isRegularFile(stagingMarker));
    }

    @Test
    public void restartsEveryPrecommitStateAfterOwnedRollback() throws Exception {
        for (CubicImportJournal.State state : Arrays.asList(
                CubicImportJournal.State.DISCOVERED,
                CubicImportJournal.State.STAGED,
                CubicImportJournal.State.VERIFIED)) {
            Path world = temporary.newFolder("recover-" + state.name()).toPath();
            writeCubicMetadata(world);
            CubicImportJournal journal = CubicImportJournal.create(2,
                    captureCurrentSources(world, Collections.singletonList(world)),
                    Collections.singletonList("."));
            journal.recordDimension(".", 0L, 0L,
                    Collections.<CubicImportJournal.FileRecord>emptyList());
            if (state.ordinal() >= CubicImportJournal.State.STAGED.ordinal()) {
                journal.transition(CubicImportJournal.State.DISCOVERED,
                        CubicImportJournal.State.STAGED);
            }
            if (state.ordinal() >= CubicImportJournal.State.VERIFIED.ordinal()) {
                journal.transition(CubicImportJournal.State.STAGED,
                        CubicImportJournal.State.VERIFIED);
            }
            journal.writeAtomic(world.resolve(CubicImportJournal.FILE_NAME));
            if (state != CubicImportJournal.State.VERIFIED) {
                writeOwnedStagingMarker(world, 0);
            }

            assertTrue(LegacyCubicSaveImporter.importWorld(world, 2, 0L));
            CubicImportJournal recovered = CubicImportJournal.read(
                    world.resolve(CubicImportJournal.FILE_NAME));
            assertTrue(recovered.getState() == CubicImportJournal.State.COMMITTED);
            assertFalse(Files.exists(world.resolve(
                    ".cavesnotcliffs-cubic-staging")));
        }
    }

    @Test
    public void precommitRecoveryRefusesAChangedSource() throws Exception {
        Path world = temporary.newFolder("changed-source").toPath();
        writeCubicMetadata(world);
        Path source = world.resolve("region2d/source-marker");
        Files.createDirectories(source.getParent());
        Files.write(source, "before".getBytes(StandardCharsets.UTF_8));
        CubicImportJournal.create(2,
                captureCurrentSources(world, Collections.singletonList(world)),
                Collections.singletonList("."))
                .writeAtomic(world.resolve(CubicImportJournal.FILE_NAME));
        Path staging = writeOwnedStagingMarker(world, 0);
        Files.write(source, "after".getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

        try {
            LegacyCubicSaveImporter.importWorld(world, 2, 0L);
            fail("Expected changed source rejection");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("sources changed"));
        }
        assertTrue(Files.isRegularFile(staging));
        assertTrue(Files.isRegularFile(world.resolve(CubicImportJournal.FILE_NAME)));
    }

    @Test
    public void precommitRecoveryNeverDeletesFiniteOutput() throws Exception {
        Path world = temporary.newFolder("precommit-target").toPath();
        writeCubicMetadata(world);
        CubicImportJournal.create(2,
                captureCurrentSources(world, Collections.singletonList(world)),
                Collections.singletonList("."))
                .writeAtomic(world.resolve(CubicImportJournal.FILE_NAME));
        Path staging = writeOwnedStagingMarker(world, 0);
        Path target = Files.createDirectories(world.resolve("region"))
                .resolve("r.0.0.mca");
        Files.write(target, "existing".getBytes(StandardCharsets.UTF_8));

        try {
            LegacyCubicSaveImporter.importWorld(world, 2, 0L);
            fail("Expected finite target rejection");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("output may have been committed"));
        }
        assertTrue(Files.isRegularFile(target));
        assertTrue(Files.isRegularFile(staging));
    }

    @Test
    public void committedRestartRemovesLeftoverStaging() throws Exception {
        Path world = temporary.newFolder("committed-staging").toPath();
        writeCommittedJournal(world);
        Path stagingMarker = writeOwnedStagingMarker(world, 0);

        assertFalse(LegacyCubicSaveImporter.importWorld(world, 2, 0L));
        assertFalse(Files.exists(stagingMarker.getParent().getParent()));
    }

    @Test
    public void durableJournalMakesInterruptedTemporaryUpdateDisposable() throws Exception {
        Path world = temporary.newFolder("temporary-journal").toPath();
        writeCommittedJournal(world);
        Path stagingMarker = writeOwnedStagingMarker(world, 0);
        Path journalTemporary = world.resolve(CubicImportJournal.TEMP_FILE_NAME);
        Files.write(journalTemporary,
                "interrupted".getBytes(StandardCharsets.UTF_8));

        assertFalse(LegacyCubicSaveImporter.importWorld(world, 2, 0L));
        assertFalse(Files.exists(journalTemporary));
        assertFalse(Files.exists(stagingMarker.getParent().getParent()));
    }

    @Test
    public void temporaryJournalWithoutDurableStateIsDiscardedOnlyWhenNoOutputExists()
            throws Exception {
        Path world = temporary.newFolder("temporary-only").toPath();
        writeCubicMetadata(world);
        Path journal = world.resolve(CubicImportJournal.FILE_NAME);
        Path journalTemporary = world.resolve(CubicImportJournal.TEMP_FILE_NAME);
        CubicImportJournal.create(2,
                captureCurrentSources(world, Collections.singletonList(world)),
                Collections.singletonList(".")).writeAtomic(journal);
        Files.move(journal, journalTemporary);

        assertTrue(LegacyCubicSaveImporter.importWorld(world, 2, 0L));
        assertFalse(Files.exists(journalTemporary));
        assertTrue(CubicImportJournal.read(world.resolve(CubicImportJournal.FILE_NAME))
                .getState() == CubicImportJournal.State.COMMITTED);
    }

    @Test
    public void malformedTemporaryJournalWithoutDurableStateIsPreserved() throws Exception {
        Path world = temporary.newFolder("temporary-malformed").toPath();
        writeCubicMetadata(world);
        Path journalTemporary = world.resolve(CubicImportJournal.TEMP_FILE_NAME);
        Files.write(journalTemporary, "partial".getBytes(StandardCharsets.UTF_8));

        try {
            LegacyCubicSaveImporter.importWorld(world, 2, 0L);
            fail("Expected malformed temporary journal rejection");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("truncated bounded NBT")
                    || expected.getMessage().contains("Not in GZIP format"));
        }
        assertTrue(Files.isRegularFile(journalTemporary));
        assertFalse(Files.exists(world.resolve(CubicImportJournal.FILE_NAME)));
    }

    @Test
    public void temporaryJournalMustBeAMonotonicSuccessorOfDurableState()
            throws Exception {
        Path world = temporary.newFolder("temporary-order").toPath();
        writeCubicMetadata(world);
        Path journalPath = world.resolve(CubicImportJournal.FILE_NAME);
        Path journalTemporary = world.resolve(CubicImportJournal.TEMP_FILE_NAME);
        CubicImportJournal journal = CubicImportJournal.create(2,
                captureCurrentSources(world, Collections.singletonList(world)),
                Collections.singletonList("."));
        journal.writeAtomic(journalPath);
        byte[] discovered = Files.readAllBytes(journalPath);
        journal.recordDimension(".", 0L, 0L,
                Collections.<CubicImportJournal.FileRecord>emptyList());
        journal.transition(CubicImportJournal.State.DISCOVERED,
                CubicImportJournal.State.STAGED);
        journal.writeAtomic(journalPath);
        Files.write(journalTemporary, discovered);

        try {
            LegacyCubicSaveImporter.importWorld(world, 2, 0L);
            fail("Expected stale temporary journal rejection");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("not a monotonic successor"));
        }
        assertTrue(Files.isRegularFile(journalPath));
        assertTrue(Files.isRegularFile(journalTemporary));
    }

    @Test
    public void validTemporarySuccessorDefersToDurableCommitRecovery() throws Exception {
        Path world = temporary.newFolder("temporary-successor").toPath();
        writeCubicMetadata(world);
        Path journalPath = world.resolve(CubicImportJournal.FILE_NAME);
        Path journalTemporary = world.resolve(CubicImportJournal.TEMP_FILE_NAME);
        writeOwnedStagingMarker(world, 0);
        CubicImportJournal journal = CubicImportJournal.create(2,
                captureCurrentSources(world, Collections.singletonList(world)),
                Collections.singletonList("."));
        journal.recordDimension(".", 0L, 0L,
                Collections.<CubicImportJournal.FileRecord>emptyList());
        journal.transition(CubicImportJournal.State.DISCOVERED,
                CubicImportJournal.State.STAGED);
        journal.transition(CubicImportJournal.State.STAGED,
                CubicImportJournal.State.VERIFIED);
        journal.transition(CubicImportJournal.State.VERIFIED,
                CubicImportJournal.State.COMMITTING);
        journal.writeAtomic(journalPath);
        byte[] durable = Files.readAllBytes(journalPath);
        journal.markDimensionCommitted(".");
        journal.writeAtomic(journalPath);
        byte[] successor = Files.readAllBytes(journalPath);
        Files.write(journalPath, durable, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        Files.write(journalTemporary, successor);

        assertFalse(LegacyCubicSaveImporter.importWorld(world, 2, 0L));
        assertFalse(Files.exists(journalTemporary));
        assertTrue(CubicImportJournal.read(journalPath).getState()
                == CubicImportJournal.State.COMMITTED);
    }

    @Test
    public void temporaryJournalWithoutDurableStatePreservesAmbiguousStaging()
            throws Exception {
        Path world = temporary.newFolder("temporary-ambiguous").toPath();
        writeCubicMetadata(world);
        Path journal = world.resolve(CubicImportJournal.FILE_NAME);
        Path journalTemporary = world.resolve(CubicImportJournal.TEMP_FILE_NAME);
        CubicImportJournal.create(2,
                captureCurrentSources(world, Collections.singletonList(world)),
                Collections.singletonList(".")).writeAtomic(journal);
        Files.move(journal, journalTemporary);
        Path staging = writeOwnedStagingMarker(world, 0);

        try {
            LegacyCubicSaveImporter.importWorld(world, 2, 0L);
            fail("Expected ambiguous temporary journal rejection");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("state is ambiguous"));
        }
        assertTrue(Files.isRegularFile(journalTemporary));
        assertTrue(Files.isRegularFile(staging));
    }

    @Test
    public void orphanStagingWithoutJournalBlocksCleanup() throws Exception {
        Path world = temporary.newFolder("orphan-staging").toPath();
        Path stagingMarker = writeStagingMarker(world);

        try {
            LegacyCubicSaveImporter.importWorld(world, 2, 0L);
            fail("Expected orphan staging rejection");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("staging exists without a journal"));
        }
        assertTrue(Files.isRegularFile(stagingMarker));
    }

    @Test
    public void neverOverwritesPreexistingFiniteRegionStorage() throws Exception {
        Path world = temporary.newFolder("existing-target").toPath();
        writeCubicMetadata(world);
        Path source = world.resolve("region2d/source-marker");
        Files.createDirectories(source.getParent());
        Files.write(source, "source".getBytes(StandardCharsets.UTF_8));
        Files.createDirectories(world.resolve("region"));

        try {
            LegacyCubicSaveImporter.importWorld(world, 2, 0L);
            fail("Expected existing target rejection");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("Refusing to overwrite existing finite region"));
        }
        assertFalse(Files.exists(world.resolve(CubicImportJournal.FILE_NAME)));
    }

    @Test
    public void reconcilesAllCommittingCrashPointsFromHashesAndPresence() throws Exception {
        Path world = temporary.newFolder("committing-recovery").toPath();
        Path nether = world.resolve("DIM-1");
        Path custom = world.resolve("DIM1");
        writeCubicMetadata(world);
        writeCubicMetadata(nether);
        writeCubicMetadata(custom);
        List<Path> dimensions = Arrays.asList(world, nether, custom);
        List<String> names = Arrays.asList(".", "DIM-1", "DIM1");

        Path staging = world.resolve(".cavesnotcliffs-cubic-staging");
        Path rootStage = writeOwnedStagingMarker(world, 0).getParent();
        Path netherStage = writeOwnedStagingMarker(world, 1).getParent();
        Path customStage = writeOwnedStagingMarker(world, 2).getParent();
        Path rootTarget = writeRegionFile(world, "committed-bit");
        Path netherTarget = writeRegionFile(nether, "moved-before-bit");
        Path customStaged = writeRegionFile(customStage, "before-move");

        CubicImportJournal journal = CubicImportJournal.create(2,
                captureCurrentSources(world, dimensions), names);
        journal.recordDimension(".", 1L, 24L,
                CubicImportJournal.captureOutputs(world, rootTarget.getParent()));
        journal.recordDimension("DIM-1", 1L, 24L,
                CubicImportJournal.captureOutputs(nether, netherTarget.getParent()));
        journal.recordDimension("DIM1", 1L, 24L,
                CubicImportJournal.captureOutputs(customStage, customStaged.getParent()));
        journal.transition(CubicImportJournal.State.DISCOVERED,
                CubicImportJournal.State.STAGED);
        journal.transition(CubicImportJournal.State.STAGED,
                CubicImportJournal.State.VERIFIED);
        journal.transition(CubicImportJournal.State.VERIFIED,
                CubicImportJournal.State.COMMITTING);
        journal.markDimensionCommitted(".");
        journal.writeAtomic(world.resolve(CubicImportJournal.FILE_NAME));

        assertFalse(LegacyCubicSaveImporter.importWorld(world, 2, 0L));
        CubicImportJournal recovered = CubicImportJournal.read(
                world.resolve(CubicImportJournal.FILE_NAME));
        assertTrue(recovered.getState() == CubicImportJournal.State.COMMITTED);
        for (String name : names) {
            assertTrue(recovered.isDimensionCommitted(name));
        }
        assertTrue(Files.readAllBytes(rootTarget).length > 0);
        assertTrue(Files.readAllBytes(netherTarget).length > 0);
        assertTrue(Files.isRegularFile(custom.resolve("region/r.0.0.mca")));
        assertFalse(Files.exists(staging));
        assertFalse(Files.exists(rootStage));
        assertFalse(Files.exists(netherStage));
    }

    @Test
    public void committingRecoveryNeverOverwritesAConflictingTarget() throws Exception {
        Path world = temporary.newFolder("committing-conflict").toPath();
        writeCubicMetadata(world);
        Path stagedDimension = writeOwnedStagingMarker(world, 0).getParent();
        Path stagedOutput = writeRegionFile(stagedDimension, "expected");
        Path targetOutput = writeRegionFile(world, "preexisting");
        byte[] targetBefore = Files.readAllBytes(targetOutput);

        CubicImportJournal journal = CubicImportJournal.create(2,
                captureCurrentSources(world, Collections.singletonList(world)),
                Collections.singletonList("."));
        journal.recordDimension(".", 1L, 24L,
                CubicImportJournal.captureOutputs(
                        stagedDimension, stagedOutput.getParent()));
        journal.transition(CubicImportJournal.State.DISCOVERED,
                CubicImportJournal.State.STAGED);
        journal.transition(CubicImportJournal.State.STAGED,
                CubicImportJournal.State.VERIFIED);
        journal.transition(CubicImportJournal.State.VERIFIED,
                CubicImportJournal.State.COMMITTING);
        journal.writeAtomic(world.resolve(CubicImportJournal.FILE_NAME));

        try {
            LegacyCubicSaveImporter.importWorld(world, 2, 0L);
            fail("Expected ambiguous target rejection");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains(
                    "both staged and target regions exist"));
        }
        assertTrue(Arrays.equals(targetBefore, Files.readAllBytes(targetOutput)));
        assertTrue(Files.isRegularFile(stagedOutput));
        assertTrue(CubicImportJournal.read(world.resolve(CubicImportJournal.FILE_NAME))
                .getState() == CubicImportJournal.State.COMMITTING);
    }

    @Test
    public void committingRecoveryRejectsATargetWithDifferentBytes() throws Exception {
        Path world = temporary.newFolder("committing-hash-conflict").toPath();
        writeCubicMetadata(world);
        Path stagedDimension = writeOwnedStagingMarker(world, 0).getParent();
        Path stagedOutput = writeRegionFile(stagedDimension, "expected");
        List<CubicImportJournal.FileRecord> expected =
                CubicImportJournal.captureOutputs(
                        stagedDimension, stagedOutput.getParent());
        Files.delete(stagedOutput);
        Files.delete(stagedOutput.getParent());
        Path targetOutput = writeRegionFile(world, "different");
        byte[] targetBefore = Files.readAllBytes(targetOutput);

        CubicImportJournal journal = CubicImportJournal.create(2,
                captureCurrentSources(world, Collections.singletonList(world)),
                Collections.singletonList("."));
        journal.recordDimension(".", 1L, 24L, expected);
        journal.transition(CubicImportJournal.State.DISCOVERED,
                CubicImportJournal.State.STAGED);
        journal.transition(CubicImportJournal.State.STAGED,
                CubicImportJournal.State.VERIFIED);
        journal.transition(CubicImportJournal.State.VERIFIED,
                CubicImportJournal.State.COMMITTING);
        journal.writeAtomic(world.resolve(CubicImportJournal.FILE_NAME));

        try {
            LegacyCubicSaveImporter.importWorld(world, 2, 0L);
            fail("Expected target hash rejection");
        } catch (IOException expectedFailure) {
            assertTrue(expectedFailure.getMessage().contains(
                    "target output does not match the journal hashes"));
        }
        assertTrue(Arrays.equals(targetBefore, Files.readAllBytes(targetOutput)));
        assertTrue(CubicImportJournal.read(world.resolve(CubicImportJournal.FILE_NAME))
                .getState() == CubicImportJournal.State.COMMITTING);
    }

    @Test
    public void committingRecoveryFailsClosedWhenCommittedBitHasNoTarget() throws Exception {
        Path world = temporary.newFolder("committing-missing-target").toPath();
        writeCubicMetadata(world);
        Path stagedDimension = writeOwnedStagingMarker(world, 0).getParent();
        Path stagedOutput = writeRegionFile(stagedDimension, "expected");
        CubicImportJournal journal = CubicImportJournal.create(2,
                captureCurrentSources(world, Collections.singletonList(world)),
                Collections.singletonList("."));
        journal.recordDimension(".", 1L, 24L,
                CubicImportJournal.captureOutputs(
                        stagedDimension, stagedOutput.getParent()));
        journal.transition(CubicImportJournal.State.DISCOVERED,
                CubicImportJournal.State.STAGED);
        journal.transition(CubicImportJournal.State.STAGED,
                CubicImportJournal.State.VERIFIED);
        journal.transition(CubicImportJournal.State.VERIFIED,
                CubicImportJournal.State.COMMITTING);
        journal.markDimensionCommitted(".");
        journal.writeAtomic(world.resolve(CubicImportJournal.FILE_NAME));

        try {
            LegacyCubicSaveImporter.importWorld(world, 2, 0L);
            fail("Expected committed-bit inconsistency rejection");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains(
                    "marks the dimension committed"));
        }
        assertTrue(Files.isRegularFile(stagedOutput));
        assertFalse(Files.exists(world.resolve("region")));
    }

    @Test
    public void verifiesCommittedOutputHashesBeforeMetadataCleanup() throws Exception {
        Path world = temporary.newFolder("verified-output").toPath();
        writeCubicMetadata(world);
        CubicDimensionStagerTest.writeExtension(
                world.resolve("region2d/0.0.2dr.ext"), 0,
                CubicDimensionStagerTest.column(0, 0));
        for (int cubeY = -4; cubeY < 20; cubeY++) {
            CubicDimensionStagerTest.writeCube(world, 0, cubeY, 0,
                    CubicDimensionStagerTest.cube(0, cubeY, 0, true, true));
        }
        assertTrue(LegacyCubicSaveImporter.importWorld(
                world, CavesNotCliffsWorldData.CURRENT_SCHEMA, 0L));
        CubicImportJournal journal = CubicImportJournal.read(
                world.resolve(CubicImportJournal.FILE_NAME));
        LegacyCubicSaveImporter.verifyCommittedTargets(world, journal, true);

        Files.write(world.resolve("region/r.0.0.mca"), new byte[] {1},
                java.nio.file.StandardOpenOption.APPEND);
        try {
            LegacyCubicSaveImporter.verifyCommittedTargets(world, journal, true);
            fail("Expected changed output rejection");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains(
                    "changed before legacy metadata cleanup"));
        }
    }

    @Test
    public void verifiesCommittedHashesOnlyUntilLegacyLevelMetadataIsCleaned()
            throws Exception {
        Path world = temporary.newFolder("committed-cleanup-window").toPath();
        writeCubicMetadata(world);
        CubicDimensionStagerTest.writeExtension(
                world.resolve("region2d/0.0.2dr.ext"), 0,
                CubicDimensionStagerTest.column(0, 0));
        for (int cubeY = -4; cubeY < 20; cubeY++) {
            CubicDimensionStagerTest.writeCube(world, 0, cubeY, 0,
                    CubicDimensionStagerTest.cube(0, cubeY, 0, true, true));
        }
        assertTrue(LegacyCubicSaveImporter.importWorld(
                world, CavesNotCliffsWorldData.CURRENT_SCHEMA, 0L));
        CubicImportJournal journal = CubicImportJournal.read(
                world.resolve(CubicImportJournal.FILE_NAME));
        Path levelFile = world.resolve("level.dat");
        LegacyCubicLevelMetadataTest.write(levelFile,
                LegacyCubicLevelMetadataTest.level(
                        "cubicchunks:storage_format_provider_registry",
                        "cubicchunks:vanilla_compatibility_generators_registry"));
        Path output = world.resolve("region/r.0.0.mca");
        byte[] committedBytes = Files.readAllBytes(output);

        Files.write(output, new byte[] {1}, StandardOpenOption.APPEND);
        try {
            LegacyCubicSaveImporter.finishCommittedImport(world, levelFile, journal);
            fail("Expected output verification before metadata cleanup");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains(
                    "changed before legacy metadata cleanup"));
        }
        assertTrue(LegacyCubicLevelMetadata.inspect(levelFile, false).changed());

        Files.write(output, committedBytes, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        LegacyCubicLevelMetadata.clean(levelFile);
        Files.write(output, new byte[] {2}, StandardOpenOption.APPEND);
        LegacyCubicSaveImporter.finishCommittedImport(world, levelFile, journal);
    }

    @Test
    public void refusesCubicChunkTicketsBeforeCreatingAJournal() throws Exception {
        Path world = temporary.newFolder("cubic-ticket").toPath();
        writeCubicMetadata(world);
        NBTTagCompound ticket = new NBTTagCompound();
        NBTTagCompound cubic = new NBTTagCompound();
        cubic.setInteger("entityCubeY", 0);
        NBTTagCompound entry = new NBTTagCompound();
        entry.setInteger("x", 4);
        entry.setInteger("z", 5);
        entry.setIntArray("cubes", new int[] {0});
        NBTTagList map = new NBTTagList();
        map.appendTag(entry);
        cubic.setTag("chunkMap", map);
        ticket.setTag("cubicchunks", cubic);
        LegacyCubicTicketMetadataTest.write(world,
                LegacyCubicTicketMetadataTest.holders("chunkloader", ticket));

        try {
            LegacyCubicSaveImporter.importWorld(
                    world, CavesNotCliffsWorldData.CURRENT_SCHEMA, 0L);
            fail("Expected cubic ticket rejection");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("forces 1 cubic columns"));
        }
        assertFalse(Files.exists(world.resolve(CubicImportJournal.FILE_NAME)));
    }

    @Test
    public void refusesUnsafeStructureMetadataBeforeCreatingAJournal() throws Exception {
        Path world = temporary.newFolder("unsafe-structure").toPath();
        writeCubicMetadata(world);
        LegacyCubicStructureMetadataTest.write(world, "Mineshaft.dat",
                LegacyCubicStructureMetadataTest.structureRoot(
                        LegacyCubicStructureMetadataTest.feature(0, 0, 1,
                                LegacyCubicStructureMetadataTest.box(
                                        0, 0, 0, 15, 20, 15))));

        try {
            LegacyCubicSaveImporter.importWorld(
                    world, CavesNotCliffsWorldData.CURRENT_SCHEMA, 0L);
            fail("Expected unsafe structure metadata rejection");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("unsupported cubic ChunkY=1"));
        }
        assertFalse(Files.exists(world.resolve(CubicImportJournal.FILE_NAME)));
    }

    @Test
    public void rejectsStagedOutputChangedBeforeItsAtomicMove() throws Exception {
        Path world = temporary.newFolder("changed-staging").toPath();
        writeCubicMetadata(world);
        CubicDimensionStagerTest.writeExtension(
                world.resolve("region2d/0.0.2dr.ext"), 0,
                CubicDimensionStagerTest.column(0, 0));
        for (int cubeY = -4; cubeY < 20; cubeY++) {
            CubicDimensionStagerTest.writeCube(world, 0, cubeY, 0,
                    CubicDimensionStagerTest.cube(0, cubeY, 0, true, true));
        }
        Path stagedOutput = world.resolve(
                ".cavesnotcliffs-cubic-staging/dimension-0000/region/r.0.0.mca");

        try {
            LegacyCubicSaveImporter.importWorld(world,
                    CavesNotCliffsWorldData.CURRENT_SCHEMA, 0L, null, ignored ->
                            Files.write(stagedOutput, new byte[] {1},
                                    StandardOpenOption.APPEND));
            fail("Expected changed staged output rejection");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains(
                    "staged output does not match the journal hashes"));
        }
        assertFalse(Files.exists(world.resolve("region")));
        assertTrue(Files.isRegularFile(stagedOutput));
        assertTrue(CubicImportJournal.read(world.resolve(CubicImportJournal.FILE_NAME))
                .getState() == CubicImportJournal.State.COMMITTING);
    }

    @Test
    public void rejectsPlayerMetadataCreatedWhileStaging() throws Exception {
        assertLateSourceRejected("late-player", true, world -> {
            Path player = Files.createDirectories(world.resolve("playerdata"))
                    .resolve("late.dat");
            LegacyCubicLevelMetadataTest.write(player, new NBTTagCompound());
        }, "source files changed");
    }

    @Test
    public void rejectsStructureMetadataCreatedWhileStaging() throws Exception {
        assertLateSourceRejected("late-structure", false, world ->
                LegacyCubicStructureMetadataTest.write(world, "Mineshaft.dat",
                        LegacyCubicStructureMetadataTest.structureRoot()),
                "source files changed");
    }

    @Test
    public void rejectsForcedChunkMetadataCreatedWhileStaging() throws Exception {
        assertLateSourceRejected("late-forcedchunks", false, world ->
                LegacyCubicTicketMetadataTest.write(world, new NBTTagList()),
                "source files changed");
    }

    @Test
    public void rejectsDimensionCreatedWhileStaging() throws Exception {
        assertLateSourceRejected("late-dimension", false, world ->
                LegacyCubicDimensionMetadataTest.writeMetadata(
                        world.resolve("DIM2"), true, -64, 320,
                        "cubicchunks:anvil3d", "cubicchunks:default"),
                "dimension set changed");
    }

    private void assertLateSourceRejected(String name, boolean includeLevel,
            LegacyCubicSaveImporter.PrecommitHook hook, String message) throws Exception {
        Path world = temporary.newFolder(name).toPath();
        writeCubicMetadata(world);
        Path levelFile = null;
        if (includeLevel) {
            levelFile = world.resolve("level.dat");
            LegacyCubicLevelMetadataTest.write(levelFile,
                    LegacyCubicLevelMetadataTest.level());
        }

        try {
            LegacyCubicSaveImporter.importWorld(world,
                    CavesNotCliffsWorldData.CURRENT_SCHEMA, 0L, levelFile, hook);
            fail("Expected concurrent source rejection");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains(message));
        }
        assertFalse(Files.exists(world.resolve("region")));
        assertTrue(CubicImportJournal.read(world.resolve(CubicImportJournal.FILE_NAME))
                .getState() == CubicImportJournal.State.STAGED);
    }

    private static void writeCubicMetadata(Path world) throws IOException {
        LegacyCubicDimensionMetadataTest.writeMetadata(world, true, -64, 320,
                "cubicchunks:anvil3d", "cubicchunks:default");
    }

    private static List<CubicImportJournal.FileRecord> captureCurrentSources(
            Path world, List<Path> dimensions) throws IOException {
        List<Path> metadata = new java.util.ArrayList<Path>();
        for (Path dimension : dimensions) {
            metadata.add(dimension.resolve("data/cubicChunksData.dat"));
        }
        return CubicImportJournal.captureSources(world, dimensions, metadata);
    }

    private static Path writeRegionFile(Path dimension, String value) throws IOException {
        Path output = Files.createDirectories(dimension.resolve("region"))
                .resolve("r.0.0.mca");
        Files.write(output, value.getBytes(StandardCharsets.UTF_8));
        return output;
    }

    private static void writeCommittedJournal(Path world) throws IOException {
        Path source = world.resolve("region2d/source-marker");
        Files.createDirectories(source.getParent());
        Files.write(source, "source".getBytes(StandardCharsets.UTF_8));
        CubicImportJournal journal = CubicImportJournal.create(2,
                CubicImportJournal.captureSources(world, Collections.singletonList(world)),
                Collections.singletonList("."));
        journal.recordDimension(".", 0L, 0L,
                Collections.<CubicImportJournal.FileRecord>emptyList());
        journal.transition(CubicImportJournal.State.DISCOVERED,
                CubicImportJournal.State.STAGED);
        journal.transition(CubicImportJournal.State.STAGED,
                CubicImportJournal.State.VERIFIED);
        journal.transition(CubicImportJournal.State.VERIFIED,
                CubicImportJournal.State.COMMITTING);
        journal.markDimensionCommitted(".");
        journal.transition(CubicImportJournal.State.COMMITTING,
                CubicImportJournal.State.COMMITTED);
        journal.writeAtomic(world.resolve(CubicImportJournal.FILE_NAME));
    }

    private static Path writeStagingMarker(Path world) throws IOException {
        Path marker = world.resolve(
                ".cavesnotcliffs-cubic-staging/dimension-0000/marker");
        Files.createDirectories(marker.getParent());
        Files.write(marker, "staged".getBytes(StandardCharsets.UTF_8));
        return marker;
    }

    private static Path writeOwnedStagingMarker(Path world, int dimension) throws IOException {
        Path marker = world.resolve(".cavesnotcliffs-cubic-staging")
                .resolve(String.format("dimension-%04d", dimension))
                .resolve("cubes.spool");
        Files.createDirectories(marker.getParent());
        Files.write(marker, "staged".getBytes(StandardCharsets.UTF_8));
        return marker;
    }
}

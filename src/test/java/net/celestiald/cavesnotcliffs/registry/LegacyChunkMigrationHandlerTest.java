package net.celestiald.cavesnotcliffs.registry;

import net.celestiald.cavesnotcliffs.content.LushCaveMechanics;
import net.celestiald.cavesnotcliffs.worldgen.v118.TerrainColumn;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LegacyChunkMigrationHandlerTest {
    @Test
    public void chunkBoundsCoverEveryExtendedSection() {
        LegacyChunkMigration.Bounds bounds =
                LegacyChunkMigrationHandler.columnBounds(-2, 3);

        assertEquals(-32, bounds.minX);
        assertEquals(TerrainColumn.MIN_Y, bounds.minY);
        assertEquals(48, bounds.minZ);
        assertEquals(16, bounds.sizeX);
        assertEquals(TerrainColumn.HEIGHT, bounds.sizeY);
        assertEquals(16, bounds.sizeZ);
        assertEquals(TerrainColumn.MAX_Y_EXCLUSIVE, bounds.minY + bounds.sizeY);
    }

    @Test
    public void extendedColumnMigratesAcrossSectionBoundaryInOnePass() {
        Map<String, State> blocks = new HashMap<>();
        blocks.put(key(0, 31, 0), new State("baby_dripleaf", 0));
        ColumnVolume volume = new ColumnVolume(blocks);
        TestStorage storage = new TestStorage();
        LegacyChunkMigration.Bounds bounds = new LegacyChunkMigration.Bounds(
                0, TerrainColumn.MIN_Y, 0, 1, TerrainColumn.HEIGHT, 1);

        NBTTagCompound data = new NBTTagCompound();
        ContentMigrationVersion.write(data,
                CncDataVersions.POINTED_DRIPSTONE_CONTENT_VERSION);
        LegacyChunkMigrationHandler.migrateLoaded(
                data, 0L, bounds, volume, storage);

        assertEquals("small_dripleaf", volume.pathAt(0, 31, 0));
        assertEquals(LushCaveMechanics.smallDripleafMeta(2, false, false),
                volume.metadataAt(0, 31, 0));
        assertEquals("small_dripleaf", volume.pathAt(0, 32, 0));
        assertEquals(LushCaveMechanics.smallDripleafMeta(2, true, false),
                volume.metadataAt(0, 32, 0));
        assertTrue(storage.dirtyCount > 0);
        assertEquals(CncDataVersions.CURRENT_CONTENT_VERSION,
                ContentMigrationVersion.read(data));

        NBTTagCompound completedSave = new NBTTagCompound();
        LegacyChunkMigrationHandler.writeCompletedVersion(completedSave,
                bounds, volume, storage.identity());
        assertEquals(CncDataVersions.CURRENT_CONTENT_VERSION,
                ContentMigrationVersion.read(completedSave));
    }

    @Test
    public void absoluteTopDripleafIsPreservedWithoutEnteringPendingRetryQueue() {
        Map<String, State> blocks = new HashMap<>();
        blocks.put(key(0, 319, 0), new State("baby_dripleaf", 0));
        ColumnVolume volume = new ColumnVolume(blocks);
        TestStorage storage = new TestStorage();
        LegacyChunkMigration.Bounds bounds = new LegacyChunkMigration.Bounds(
                0, TerrainColumn.MIN_Y, 0, 1, TerrainColumn.HEIGHT, 1);
        NBTTagCompound data = new NBTTagCompound();
        ContentMigrationVersion.write(data,
                CncDataVersions.POINTED_DRIPSTONE_CONTENT_VERSION);

        LegacyChunkMigrationHandler.migrateLoaded(data, 0L, bounds, volume, storage);
        assertEquals("baby_dripleaf", volume.pathAt(0, 319, 0));
        assertEquals(0, storage.dirtyCount);

        NBTTagCompound firstSave = new NBTTagCompound();
        LegacyChunkMigrationHandler.writeCompletedVersion(firstSave,
                bounds, volume, storage.identity());
        assertEquals(CncDataVersions.POINTED_DRIPSTONE_CONTENT_VERSION,
                ContentMigrationVersion.read(firstSave));

        LegacyChunkMigrationHandler.migrateLoaded(
                firstSave, 0L, bounds, volume, storage);
        NBTTagCompound secondSave = new NBTTagCompound();
        LegacyChunkMigrationHandler.writeCompletedVersion(secondSave,
                bounds, volume, storage.identity());
        assertEquals(CncDataVersions.POINTED_DRIPSTONE_CONTENT_VERSION,
                ContentMigrationVersion.read(secondSave));
        assertEquals("baby_dripleaf", volume.pathAt(0, 319, 0));
        assertEquals(0, storage.dirtyCount);
    }

    @Test
    public void occupiedUpperCellStaysPreservedUntilUnblockedReload() {
        Map<String, State> blocks = new HashMap<>();
        blocks.put(key(0, 31, 0), new State("baby_dripleaf", 0));
        blocks.put(key(0, 32, 0), new State("stone", 0));
        ColumnVolume volume = new ColumnVolume(blocks);
        TestStorage storage = new TestStorage();
        LegacyChunkMigration.Bounds bounds = new LegacyChunkMigration.Bounds(
                0, TerrainColumn.MIN_Y, 0, 1, TerrainColumn.HEIGHT, 1);
        NBTTagCompound data = new NBTTagCompound();
        ContentMigrationVersion.write(data,
                CncDataVersions.POINTED_DRIPSTONE_CONTENT_VERSION);

        LegacyChunkMigrationHandler.migrateLoaded(data, 0L, bounds, volume, storage);
        assertEquals("baby_dripleaf", volume.pathAt(0, 31, 0));
        assertEquals("stone", volume.pathAt(0, 32, 0));
        assertEquals(0, storage.dirtyCount);

        NBTTagCompound blockedSave = new NBTTagCompound();
        LegacyChunkMigrationHandler.writeCompletedVersion(blockedSave,
                bounds, volume, storage.identity());
        assertEquals(CncDataVersions.POINTED_DRIPSTONE_CONTENT_VERSION,
                ContentMigrationVersion.read(blockedSave));
        LegacyChunkMigrationHandler.migrateLoaded(
                blockedSave, 0L, bounds, volume, storage);
        assertEquals("baby_dripleaf", volume.pathAt(0, 31, 0));
        assertEquals("stone", volume.pathAt(0, 32, 0));
        assertEquals(0, storage.dirtyCount);

        blocks.remove(key(0, 32, 0));
        NBTTagCompound unblockedSave = new NBTTagCompound();
        LegacyChunkMigrationHandler.writeCompletedVersion(unblockedSave,
                bounds, volume, storage.identity());
        assertEquals(CncDataVersions.POINTED_DRIPSTONE_CONTENT_VERSION,
                ContentMigrationVersion.read(unblockedSave));
        LegacyChunkMigrationHandler.migrateLoaded(
                unblockedSave, 0L, bounds, volume, storage);

        assertEquals("small_dripleaf", volume.pathAt(0, 31, 0));
        assertEquals("small_dripleaf", volume.pathAt(0, 32, 0));
        assertTrue(storage.dirtyCount > 0);
        NBTTagCompound completedSave = new NBTTagCompound();
        LegacyChunkMigrationHandler.writeCompletedVersion(completedSave,
                bounds, volume, storage.identity());
        assertEquals(CncDataVersions.CURRENT_CONTENT_VERSION,
                ContentMigrationVersion.read(completedSave));
    }

    public static final class TestStorage
            implements LegacyChunkMigrationHandler.MigrationStorage {
        private int dirtyCount;

        @Override
        public Object identity() {
            return this;
        }

        @Override
        public void markDirty() {
            dirtyCount++;
        }
    }

    private static final class ColumnVolume implements LegacyChunkMigration.Volume {
        private final Map<String, State> blocks;

        private ColumnVolume(Map<String, State> blocks) {
            this.blocks = blocks;
        }

        @Override
        public String blockPathAt(int x, int y, int z) {
            return pathAt(x, y, z);
        }

        private String pathAt(int x, int y, int z) {
            State state = blocks.get(key(x, y, z));
            return state == null ? null : state.path;
        }

        @Override
        public int blockMetadataAt(int x, int y, int z) {
            return metadataAt(x, y, z);
        }

        private int metadataAt(int x, int y, int z) {
            State state = blocks.get(key(x, y, z));
            return state == null ? 0 : state.metadata;
        }

        @Override
        public boolean isPositionAvailable(int x, int y, int z) {
            return true;
        }

        @Override
        public boolean canStoreAt(int x, int y, int z) {
            return y >= TerrainColumn.MIN_Y && y < TerrainColumn.MAX_Y_EXCLUSIVE;
        }

        @Override
        public boolean hasTarget(String registryPath) {
            return "small_dripleaf".equals(registryPath);
        }

        @Override
        public boolean replace(int x, int y, int z, String targetRegistryPath) {
            return replace(x, y, z, targetRegistryPath, 0);
        }

        @Override
        public boolean replace(int x, int y, int z, String targetRegistryPath,
                int metadata) {
            if (!hasTarget(targetRegistryPath)) {
                return false;
            }
            blocks.put(key(x, y, z), new State(targetRegistryPath, metadata));
            return true;
        }

        @Override
        public boolean replacePair(int firstX, int firstY, int firstZ,
                String firstTarget, int firstMetadata, int secondX, int secondY, int secondZ,
                String secondTarget, int secondMetadata) {
            if (!isPositionAvailable(firstX, firstY, firstZ)
                    || !isPositionAvailable(secondX, secondY, secondZ)
                    || !canStoreAt(firstX, firstY, firstZ)
                    || !canStoreAt(secondX, secondY, secondZ)
                    || !hasTarget(firstTarget) || !hasTarget(secondTarget)) {
                return false;
            }
            blocks.put(key(secondX, secondY, secondZ),
                    new State(secondTarget, secondMetadata));
            blocks.put(key(firstX, firstY, firstZ),
                    new State(firstTarget, firstMetadata));
            return true;
        }
    }

    private static final class State {
        private final String path;
        private final int metadata;

        private State(String path, int metadata) {
            this.path = path;
            this.metadata = metadata;
        }
    }

    private static String key(int x, int y, int z) {
        return x + "," + y + "," + z;
    }
}

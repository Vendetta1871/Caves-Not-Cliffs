package net.celestiald.cavesnotcliffs.registry;

import net.celestiald.cavesnotcliffs.content.LushCaveMechanics;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LegacyChunkMigrationHandlerTest {
    @Test
    public void upperCubeLoadAutomaticallyRetriesPendingLowerCubeWithoutGeneration() {
        Map<String, State> blocks = new HashMap<>();
        blocks.put(key(0, 31, 0), new State("baby_dripleaf", 0));
        Set<Integer> loadedCubeYs = new HashSet<>();
        loadedCubeYs.add(1);
        HaloVolume volume = new HaloVolume(blocks, loadedCubeYs);

        TestStorage lowerStorage = new TestStorage();
        Access lower = new Access(lowerStorage,
                new LegacyChunkMigration.Bounds(0, 16, 0, 1, 16, 1), volume);
        Map<Integer, LegacyChunkMigrationHandler.CubeMigrationAccess> loaded = new HashMap<>();
        loaded.put(1, lower);
        List<Integer> lookups = new ArrayList<>();

        NBTTagCompound lowerData = new NBTTagCompound();
        ContentMigrationVersion.write(lowerData,
                CncDataVersions.POINTED_DRIPSTONE_CONTENT_VERSION);
        LegacyChunkMigrationHandler.migrateLoadedCube(lowerData, 0L, lower,
                offset -> {
                    lookups.add(offset);
                    return loaded.get(1 + offset);
                });

        assertEquals("baby_dripleaf", volume.pathAt(0, 31, 0));
        assertEquals(CncDataVersions.POINTED_DRIPSTONE_CONTENT_VERSION,
                ContentMigrationVersion.read(lowerData));
        NBTTagCompound deferredSave = new NBTTagCompound();
        LegacyChunkMigrationHandler.writeCompletedVersion(deferredSave,
                lower.bounds(), lower.volume(), lower.storage());
        assertEquals(CncDataVersions.POINTED_DRIPSTONE_CONTENT_VERSION,
                ContentMigrationVersion.read(deferredSave));

        loadedCubeYs.add(2);
        TestStorage upperStorage = new TestStorage();
        Access upper = new Access(upperStorage,
                new LegacyChunkMigration.Bounds(0, 32, 0, 1, 16, 1), volume);
        loaded.put(2, upper);
        NBTTagCompound upperData = new NBTTagCompound();
        LegacyChunkMigrationHandler.migrateLoadedCube(upperData, 0L, upper,
                offset -> {
                    lookups.add(offset);
                    return loaded.get(2 + offset);
                });

        assertEquals("small_dripleaf", volume.pathAt(0, 31, 0));
        assertEquals(LushCaveMechanics.smallDripleafMeta(2, false, false),
                volume.metadataAt(0, 31, 0));
        assertEquals("small_dripleaf", volume.pathAt(0, 32, 0));
        assertEquals(LushCaveMechanics.smallDripleafMeta(2, true, false),
                volume.metadataAt(0, 32, 0));
        assertTrue(lowerStorage.dirtyCount > 0);
        assertEquals(java.util.Arrays.asList(-1, 1, -1, 1), lookups);

        NBTTagCompound completedSave = new NBTTagCompound();
        LegacyChunkMigrationHandler.writeCompletedVersion(completedSave,
                lower.bounds(), lower.volume(), lower.storage());
        assertEquals(CncDataVersions.CURRENT_CONTENT_VERSION,
                ContentMigrationVersion.read(completedSave));
    }

    @Test
    public void absoluteTopDripleafIsPreservedWithoutEnteringPendingRetryQueue() {
        Map<String, State> blocks = new HashMap<>();
        blocks.put(key(0, 319, 0), new State("baby_dripleaf", 0));
        Set<Integer> loadedCubeYs = new HashSet<>();
        loadedCubeYs.add(19);
        HaloVolume volume = new HaloVolume(blocks, loadedCubeYs);
        TestStorage storage = new TestStorage();
        Access access = new Access(storage,
                new LegacyChunkMigration.Bounds(0, 304, 0, 1, 16, 1), volume);
        NBTTagCompound data = new NBTTagCompound();
        ContentMigrationVersion.write(data,
                CncDataVersions.POINTED_DRIPSTONE_CONTENT_VERSION);

        LegacyChunkMigrationHandler.migrateLoadedCube(data, 0L, access, offset -> null);
        assertEquals("baby_dripleaf", volume.pathAt(0, 319, 0));
        assertEquals(0, storage.dirtyCount);

        NBTTagCompound firstSave = new NBTTagCompound();
        LegacyChunkMigrationHandler.writeCompletedVersion(firstSave,
                access.bounds(), access.volume(), access.storage());
        assertEquals(CncDataVersions.POINTED_DRIPSTONE_CONTENT_VERSION,
                ContentMigrationVersion.read(firstSave));

        LegacyChunkMigrationHandler.migrateLoadedCube(firstSave, 0L, access, offset -> null);
        NBTTagCompound secondSave = new NBTTagCompound();
        LegacyChunkMigrationHandler.writeCompletedVersion(secondSave,
                access.bounds(), access.volume(), access.storage());
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
        Set<Integer> loadedCubeYs = new HashSet<>();
        loadedCubeYs.add(1);
        loadedCubeYs.add(2);
        HaloVolume volume = new HaloVolume(blocks, loadedCubeYs);
        TestStorage storage = new TestStorage();
        Access access = new Access(storage,
                new LegacyChunkMigration.Bounds(0, 16, 0, 1, 16, 1), volume);
        NBTTagCompound data = new NBTTagCompound();
        ContentMigrationVersion.write(data,
                CncDataVersions.POINTED_DRIPSTONE_CONTENT_VERSION);

        LegacyChunkMigrationHandler.migrateLoadedCube(data, 0L, access, offset -> null);
        assertEquals("baby_dripleaf", volume.pathAt(0, 31, 0));
        assertEquals("stone", volume.pathAt(0, 32, 0));
        assertEquals(0, storage.dirtyCount);

        NBTTagCompound blockedSave = new NBTTagCompound();
        LegacyChunkMigrationHandler.writeCompletedVersion(blockedSave,
                access.bounds(), access.volume(), access.storage());
        assertEquals(CncDataVersions.POINTED_DRIPSTONE_CONTENT_VERSION,
                ContentMigrationVersion.read(blockedSave));
        LegacyChunkMigrationHandler.migrateLoadedCube(
                blockedSave, 0L, access, offset -> null);
        assertEquals("baby_dripleaf", volume.pathAt(0, 31, 0));
        assertEquals("stone", volume.pathAt(0, 32, 0));
        assertEquals(0, storage.dirtyCount);

        blocks.remove(key(0, 32, 0));
        NBTTagCompound unblockedSave = new NBTTagCompound();
        LegacyChunkMigrationHandler.writeCompletedVersion(unblockedSave,
                access.bounds(), access.volume(), access.storage());
        assertEquals(CncDataVersions.POINTED_DRIPSTONE_CONTENT_VERSION,
                ContentMigrationVersion.read(unblockedSave));
        LegacyChunkMigrationHandler.migrateLoadedCube(
                unblockedSave, 0L, access, offset -> null);

        assertEquals("small_dripleaf", volume.pathAt(0, 31, 0));
        assertEquals("small_dripleaf", volume.pathAt(0, 32, 0));
        assertTrue(storage.dirtyCount > 0);
        NBTTagCompound completedSave = new NBTTagCompound();
        LegacyChunkMigrationHandler.writeCompletedVersion(completedSave,
                access.bounds(), access.volume(), access.storage());
        assertEquals(CncDataVersions.CURRENT_CONTENT_VERSION,
                ContentMigrationVersion.read(completedSave));
    }

    public static final class TestStorage {
        private int dirtyCount;

        public void markDirty() {
            dirtyCount++;
        }
    }

    private static final class Access
            implements LegacyChunkMigrationHandler.CubeMigrationAccess {
        private final TestStorage storage;
        private final LegacyChunkMigration.Bounds bounds;
        private final LegacyChunkMigration.Volume volume;

        private Access(TestStorage storage, LegacyChunkMigration.Bounds bounds,
                LegacyChunkMigration.Volume volume) {
            this.storage = storage;
            this.bounds = bounds;
            this.volume = volume;
        }

        @Override
        public Object storage() {
            return storage;
        }

        @Override
        public LegacyChunkMigration.Bounds bounds() {
            return bounds;
        }

        @Override
        public LegacyChunkMigration.Volume volume() {
            return volume;
        }
    }

    private static final class HaloVolume implements LegacyChunkMigration.Volume {
        private final Map<String, State> blocks;
        private final Set<Integer> loadedCubeYs;

        private HaloVolume(Map<String, State> blocks, Set<Integer> loadedCubeYs) {
            this.blocks = blocks;
            this.loadedCubeYs = loadedCubeYs;
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
            return y < -64 || y >= 320 || loadedCubeYs.contains(y >> 4);
        }

        @Override
        public boolean canStoreAt(int x, int y, int z) {
            return y >= -64 && y < 320;
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

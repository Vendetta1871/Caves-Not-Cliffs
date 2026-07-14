package net.celestiald.cavesnotcliffs.registry;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.worldgen.v118.TerrainColumn;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Applies the versioned content migration to a complete CaveBiomesAPI-height chunk column. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class LegacyChunkMigrationHandler {
    private static final Logger LOGGER = LogManager.getLogger("CavesNotCliffs/ChunkMigration");
    private static final Set<Object> COMPLETED = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<Object, Integer> PENDING = new WeakHashMap<>();
    private static final Map<Object, Integer> PRESERVED = new WeakHashMap<>();

    private LegacyChunkMigrationHandler() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkDataEvent.Load event) {
        World world = event.getWorld();
        if (world.isRemote) {
            return;
        }

        Chunk chunk = event.getChunk();
        LegacyChunkMigration.Bounds bounds = chunkBounds(chunk);
        migrateLoaded(event.getData(), world.getSeed(), bounds,
                new ChunkVolume(world, chunk), new ChunkMigrationStorage(chunk));
    }

    @SubscribeEvent
    public static void onChunkSave(ChunkDataEvent.Save event) {
        World world = event.getWorld();
        if (world.isRemote) {
            return;
        }
        writeCompletedVersion(event.getData(), chunkBounds(event.getChunk()),
                new ChunkVolume(world, event.getChunk()), event.getChunk());
    }

    static void migrateLoaded(NBTTagCompound data, long worldSeed,
            LegacyChunkMigration.Bounds bounds, LegacyChunkMigration.Volume volume,
            MigrationStorage storage) {
        int version = ContentMigrationVersion.read(data);
        LegacyChunkMigration.Result result =
                LegacyChunkMigration.migrate(version, worldSeed, bounds, volume);
        if (result.getConvertedBlocks() > 0) {
            storage.markDirty();
        }
        if (result.getResultingVersion() > version) {
            ContentMigrationVersion.write(data, result.getResultingVersion());
            storage.markDirty();
        }
        if (result.isComplete()) {
            rememberCompleted(storage.identity());
        } else if (result.getDeferredBlocks() > 0) {
            rememberPending(storage.identity(), result.getResultingVersion());
            LOGGER.warn("Deferred {} legacy content blocks because their canonical target or "
                            + "paired state is unavailable; migration remains at version {}",
                    result.getDeferredBlocks(), result.getResultingVersion());
        } else if (result.getPreservedBlocks() > 0) {
            // A top-edge or obstructed small dripleaf has no lossless canonical two-block
            // representation. Keep both world states and the honest schema version, but do not
            // queue futile retries; a later load can converge if the obstruction changes.
            rememberPreserved(storage.identity(), result.getResultingVersion());
        }
    }

    static void writeCompletedVersion(NBTTagCompound data,
            LegacyChunkMigration.Bounds bounds, LegacyChunkMigration.Volume volume,
            Object storage) {
        Integer pending = pendingVersion(storage);
        Integer preserved = preservedVersion(storage);
        Integer retainedVersion = pending != null ? pending : preserved;
        if (retainedVersion != null
                && LegacyChunkMigration.containsLegacyContent(bounds, volume)) {
            ContentMigrationVersion.write(data,
                    Math.max(ContentMigrationVersion.read(data), retainedVersion));
            return;
        }
        if (retainedVersion != null) {
            forgetPending(storage);
            forgetPreserved(storage);
        }
        if (isRememberedCompleted(storage)
                || !LegacyChunkMigration.containsLegacyContent(bounds, volume)) {
            ContentMigrationVersion.write(data,
                    CncDataVersions.CURRENT_CONTENT_VERSION);
            rememberCompleted(storage);
        }
    }

    private static LegacyChunkMigration.Bounds chunkBounds(Chunk chunk) {
        return columnBounds(chunk.x, chunk.z);
    }

    static LegacyChunkMigration.Bounds columnBounds(int chunkX, int chunkZ) {
        return new LegacyChunkMigration.Bounds(chunkX * 16, TerrainColumn.MIN_Y,
                chunkZ * 16, 16, TerrainColumn.HEIGHT, 16);
    }

    private static void rememberCompleted(Object storage) {
        forgetPending(storage);
        forgetPreserved(storage);
        synchronized (COMPLETED) {
            COMPLETED.add(storage);
        }
    }

    private static void rememberPending(Object storage, int version) {
        forgetCompleted(storage);
        forgetPreserved(storage);
        synchronized (PENDING) {
            PENDING.put(storage, version);
        }
    }

    private static Integer pendingVersion(Object storage) {
        synchronized (PENDING) {
            return PENDING.get(storage);
        }
    }

    private static void forgetPending(Object storage) {
        synchronized (PENDING) {
            PENDING.remove(storage);
        }
    }

    private static void rememberPreserved(Object storage, int version) {
        forgetPending(storage);
        forgetCompleted(storage);
        synchronized (PRESERVED) {
            PRESERVED.put(storage, version);
        }
    }

    private static Integer preservedVersion(Object storage) {
        synchronized (PRESERVED) {
            return PRESERVED.get(storage);
        }
    }

    private static void forgetPreserved(Object storage) {
        synchronized (PRESERVED) {
            PRESERVED.remove(storage);
        }
    }

    private static void forgetCompleted(Object storage) {
        synchronized (COMPLETED) {
            COMPLETED.remove(storage);
        }
    }

    private static boolean isRememberedCompleted(Object storage) {
        synchronized (COMPLETED) {
            return COMPLETED.contains(storage);
        }
    }

    private abstract static class RegistryVolume implements LegacyChunkMigration.Volume {
        @Override
        public boolean hasTarget(String registryPath) {
            return ForgeRegistries.BLOCKS.containsKey(CncRegistryIds.id(registryPath));
        }

        protected String path(IBlockState state) {
            ResourceLocation name = state.getBlock().getRegistryName();
            if (name == null || !CavesNotCliffs.MODID.equals(name.getResourceDomain())) {
                return null;
            }
            return name.getResourcePath();
        }

        protected IBlockState target(String registryPath) {
            Block block = ForgeRegistries.BLOCKS.getValue(CncRegistryIds.id(registryPath));
            return block == null ? null : block.getDefaultState();
        }

        protected IBlockState target(String registryPath, int metadata) {
            Block block = ForgeRegistries.BLOCKS.getValue(CncRegistryIds.id(registryPath));
            return block == null ? null : block.getStateFromMeta(metadata);
        }
    }

    interface MigrationStorage {
        Object identity();

        void markDirty();
    }

    private static final class ChunkMigrationStorage implements MigrationStorage {
        private final Chunk chunk;

        private ChunkMigrationStorage(Chunk chunk) {
            this.chunk = chunk;
        }

        @Override
        public Object identity() {
            return chunk;
        }

        @Override
        public void markDirty() {
            chunk.markDirty();
        }
    }

    private static final class ChunkVolume extends RegistryVolume {
        private final World world;
        private final Chunk chunk;

        private ChunkVolume(World world, Chunk chunk) {
            this.world = world;
            this.chunk = chunk;
        }

        @Override
        public String blockPathAt(int x, int y, int z) {
            if (outsideBuildHeight(y)) {
                return null;
            }
            return path(chunk.getBlockState(new BlockPos(x, y, z)));
        }

        @Override
        public int blockMetadataAt(int x, int y, int z) {
            if (outsideBuildHeight(y)) {
                return 0;
            }
            IBlockState state = chunk.getBlockState(new BlockPos(x, y, z));
            return state.getBlock().getMetaFromState(state);
        }

        @Override
        public boolean isAirAt(int x, int y, int z) {
            return outsideBuildHeight(y)
                    || chunk.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.AIR;
        }

        @Override
        public boolean isPositionAvailable(int x, int y, int z) {
            return outsideBuildHeight(y) || isStoredPosition(x, y, z);
        }

        @Override
        public boolean canStoreAt(int x, int y, int z) {
            return isStoredPosition(x, y, z);
        }

        @Override
        public boolean replace(int x, int y, int z, String targetRegistryPath) {
            IBlockState target = target(targetRegistryPath);
            return target != null && isStoredPosition(x, y, z)
                    && chunk.setBlockState(new BlockPos(x, y, z), target) != null;
        }

        @Override
        public boolean replace(int x, int y, int z, String targetRegistryPath,
                int metadata) {
            IBlockState target = target(targetRegistryPath, metadata);
            return target != null && isStoredPosition(x, y, z)
                    && chunk.setBlockState(new BlockPos(x, y, z), target) != null;
        }

        @Override
        public boolean replacePair(int firstX, int firstY, int firstZ,
                String firstTarget, int firstMetadata, int secondX, int secondY, int secondZ,
                String secondTarget, int secondMetadata) {
            if (!isStoredPosition(firstX, firstY, firstZ)
                    || !isStoredPosition(secondX, secondY, secondZ)) {
                return false;
            }
            IBlockState firstState = target(firstTarget, firstMetadata);
            IBlockState secondState = target(secondTarget, secondMetadata);
            if (firstState == null || secondState == null) {
                return false;
            }
            BlockPos firstPos = new BlockPos(firstX, firstY, firstZ);
            BlockPos secondPos = new BlockPos(secondX, secondY, secondZ);
            IBlockState secondBefore = chunk.getBlockState(secondPos);
            if (chunk.setBlockState(secondPos, secondState) == null) {
                return false;
            }
            if (chunk.setBlockState(firstPos, firstState) == null) {
                chunk.setBlockState(secondPos, secondBefore);
                chunk.markDirty();
                return false;
            }
            chunk.markDirty();
            return true;
        }

        private boolean isStoredPosition(int x, int y, int z) {
            return !outsideBuildHeight(y) && (x >> 4) == chunk.x && (z >> 4) == chunk.z;
        }

        private boolean outsideBuildHeight(int y) {
            return y < TerrainColumn.MIN_Y || y >= TerrainColumn.MAX_Y_EXCLUSIVE;
        }

        @Override
        public void scheduleUpdate(int x, int y, int z, String targetRegistryPath,
                int delay) {
            Block block = ForgeRegistries.BLOCKS.getValue(CncRegistryIds.id(targetRegistryPath));
            if (block != null) {
                world.scheduleUpdate(new BlockPos(x, y, z), block, delay);
            }
        }
    }

}

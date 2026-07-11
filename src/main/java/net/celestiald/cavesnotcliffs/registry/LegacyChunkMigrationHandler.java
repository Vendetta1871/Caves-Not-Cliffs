package net.celestiald.cavesnotcliffs.registry;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.CubeDataEvent;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.world.CavesNotCliffsWorldType;
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Applies the versioned content migration to both ordinary chunks and CubicChunks cubes. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class LegacyChunkMigrationHandler {
    private static final Logger LOGGER = LogManager.getLogger("CavesNotCliffs/ChunkMigration");
    private static final Set<Object> COMPLETED = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<Object, Integer> PENDING = new WeakHashMap<>();

    private LegacyChunkMigrationHandler() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkDataEvent.Load event) {
        World world = event.getWorld();
        if (world.isRemote || isCubic(world)) {
            return;
        }

        Chunk chunk = event.getChunk();
        LegacyChunkMigration.Bounds bounds = chunkBounds(chunk);
        migrateLoaded(event.getData(), world.getSeed(), bounds,
                new ChunkVolume(world, chunk), chunk);
    }

    @SubscribeEvent
    public static void onChunkSave(ChunkDataEvent.Save event) {
        World world = event.getWorld();
        if (world.isRemote || isCubic(world)) {
            return;
        }
        writeCompletedVersion(event.getData(), chunkBounds(event.getChunk()),
                new ChunkVolume(world, event.getChunk()), event.getChunk());
    }

    @SubscribeEvent
    public static void onCubeLoad(CubeDataEvent.Load event) {
        World world = event.getWorld();
        if (world.isRemote || !isCubic(world)) {
            return;
        }

        ICube cube = event.getCube();
        CubeMigrationAccess current = new LiveCubeMigrationAccess(world, cube);
        ICubicWorld cubicWorld = (ICubicWorld) world;
        CubePos position = cube.getCoords();
        migrateLoadedCube(event.getData(), world.getSeed(), current,
                verticalOffset -> {
                    ICube neighbor = cubicWorld.getCubeCache().getLoadedCube(
                            position.getX(), position.getY() + verticalOffset,
                            position.getZ());
                    return neighbor == null ? null
                            : new LiveCubeMigrationAccess(world, neighbor);
                });
    }

    @SubscribeEvent
    public static void onCubeSave(CubeDataEvent.Save event) {
        World world = event.getWorld();
        if (world.isRemote || !isCubic(world)) {
            return;
        }
        ICube cube = event.getCube();
        writeCompletedVersion(event.getData(), cubeBounds(cube.getCoords()),
                new CubeVolume(world, cube), cube);
    }

    private static void migrateLoaded(NBTTagCompound data, long worldSeed,
            LegacyChunkMigration.Bounds bounds, LegacyChunkMigration.Volume volume,
            Object storage) {
        int version = ContentMigrationVersion.read(data);
        LegacyChunkMigration.Result result =
                LegacyChunkMigration.migrate(version, worldSeed, bounds, volume);
        if (result.getConvertedBlocks() > 0) {
            markDirty(storage);
        }
        if (result.getResultingVersion() > version) {
            ContentMigrationVersion.write(data, result.getResultingVersion());
            markDirty(storage);
        }
        if (result.isComplete()) {
            rememberCompleted(storage);
        } else if (result.getDeferredBlocks() > 0) {
            rememberPending(storage, result.getResultingVersion());
            LOGGER.warn("Deferred {} legacy content blocks because their canonical target or "
                            + "loaded boundary halo is unavailable; migration remains at version {}",
                    result.getDeferredBlocks(), result.getResultingVersion());
        }
    }

    static void migrateLoadedCube(NBTTagCompound data, long worldSeed,
            CubeMigrationAccess current, LoadedVerticalNeighborLookup neighbors) {
        migrateLoaded(data, worldSeed, current.bounds(), current.volume(), current.storage());
        for (int verticalOffset : new int[]{-1, 1}) {
            CubeMigrationAccess neighbor = neighbors.loaded(verticalOffset);
            if (neighbor != null && neighbor.storage() != current.storage()) {
                retryPending(worldSeed, neighbor);
            }
        }
    }

    private static void retryPending(long worldSeed, CubeMigrationAccess access) {
        Integer version = pendingVersion(access.storage());
        if (version == null) {
            return;
        }
        LegacyChunkMigration.Result result = LegacyChunkMigration.migrate(
                version, worldSeed, access.bounds(), access.volume());
        if (result.getConvertedBlocks() > 0 || result.getResultingVersion() > version) {
            markDirty(access.storage());
        }
        if (result.isComplete()) {
            rememberCompleted(access.storage());
            return;
        }
        rememberPending(access.storage(), result.getResultingVersion());
    }

    static void writeCompletedVersion(NBTTagCompound data,
            LegacyChunkMigration.Bounds bounds, LegacyChunkMigration.Volume volume,
            Object storage) {
        Integer pending = pendingVersion(storage);
        if (pending != null) {
            ContentMigrationVersion.write(data,
                    Math.max(ContentMigrationVersion.read(data), pending));
            return;
        }
        if (isRememberedCompleted(storage)
                || !LegacyChunkMigration.containsLegacyContent(bounds, volume)) {
            ContentMigrationVersion.write(data,
                    CncDataVersions.CURRENT_CONTENT_VERSION);
            rememberCompleted(storage);
        }
    }

    private static LegacyChunkMigration.Bounds chunkBounds(Chunk chunk) {
        return new LegacyChunkMigration.Bounds(chunk.x * 16, 0, chunk.z * 16,
                16, 256, 16);
    }

    private static LegacyChunkMigration.Bounds cubeBounds(CubePos cube) {
        return new LegacyChunkMigration.Bounds(cube.getMinBlockX(), cube.getMinBlockY(),
                cube.getMinBlockZ(), 16, 16, 16);
    }

    private static boolean isCubic(World world) {
        return world instanceof ICubicWorld && ((ICubicWorld) world).isCubicWorld();
    }

    private static void rememberCompleted(Object storage) {
        forgetPending(storage);
        synchronized (COMPLETED) {
            COMPLETED.add(storage);
        }
    }

    private static void rememberPending(Object storage, int version) {
        forgetCompleted(storage);
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

    private static void markDirty(Object storage) {
        if (storage instanceof Chunk) {
            ((Chunk) storage).markDirty();
            return;
        }

        // CubicChunks 1.12 exposes Cube.markDirty publicly but omitted it from ICube.
        try {
            Method markDirty = storage.getClass().getMethod("markDirty");
            markDirty.invoke(storage);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException error) {
            throw new IllegalStateException("Loaded CubicChunks cube cannot be marked dirty: "
                    + storage.getClass().getName(), error);
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

    interface CubeMigrationAccess {
        Object storage();

        LegacyChunkMigration.Bounds bounds();

        LegacyChunkMigration.Volume volume();
    }

    interface LoadedVerticalNeighborLookup {
        CubeMigrationAccess loaded(int verticalOffset);
    }

    private static final class LiveCubeMigrationAccess implements CubeMigrationAccess {
        private final ICube cube;
        private final LegacyChunkMigration.Bounds bounds;
        private final CubeVolume volume;

        private LiveCubeMigrationAccess(World world, ICube cube) {
            this.cube = cube;
            this.bounds = cubeBounds(cube.getCoords());
            this.volume = new CubeVolume(world, cube);
        }

        @Override
        public Object storage() {
            return cube;
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
        public boolean replace(int x, int y, int z, String targetRegistryPath) {
            IBlockState target = target(targetRegistryPath);
            return target != null
                    && chunk.setBlockState(new BlockPos(x, y, z), target) != null;
        }

        @Override
        public boolean replace(int x, int y, int z, String targetRegistryPath,
                int metadata) {
            IBlockState target = target(targetRegistryPath, metadata);
            return target != null
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
            return y < 0 || y >= 256;
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

    private static final class CubeVolume extends RegistryVolume {
        private final World world;
        private final ICube cube;

        private CubeVolume(World world, ICube cube) {
            this.world = world;
            this.cube = cube;
        }

        @Override
        public String blockPathAt(int x, int y, int z) {
            IBlockState state = stateAt(x, y, z);
            return state == null ? null : path(state);
        }

        @Override
        public int blockMetadataAt(int x, int y, int z) {
            IBlockState state = stateAt(x, y, z);
            return state == null ? 0 : state.getBlock().getMetaFromState(state);
        }

        @Override
        public boolean isAirAt(int x, int y, int z) {
            IBlockState state = stateAt(x, y, z);
            return state == null || state.getBlock() == Blocks.AIR;
        }

        @Override
        public boolean isPositionAvailable(int x, int y, int z) {
            return outsideFiniteHeight(y) || cubeAt(x, y, z) != null;
        }

        @Override
        public boolean replace(int x, int y, int z, String targetRegistryPath) {
            IBlockState target = target(targetRegistryPath);
            ICube targetCube = cubeAt(x, y, z);
            return target != null && targetCube != null
                    && targetCube.setBlockState(new BlockPos(x, y, z), target) != null;
        }

        @Override
        public boolean replace(int x, int y, int z, String targetRegistryPath,
                int metadata) {
            IBlockState target = target(targetRegistryPath, metadata);
            ICube targetCube = cubeAt(x, y, z);
            return target != null && targetCube != null
                    && targetCube.setBlockState(new BlockPos(x, y, z), target) != null;
        }

        @Override
        public boolean replacePair(int firstX, int firstY, int firstZ,
                String firstTarget, int firstMetadata, int secondX, int secondY, int secondZ,
                String secondTarget, int secondMetadata) {
            ICube firstCube = cubeAt(firstX, firstY, firstZ);
            ICube secondCube = cubeAt(secondX, secondY, secondZ);
            IBlockState firstState = target(firstTarget, firstMetadata);
            IBlockState secondState = target(secondTarget, secondMetadata);
            if (firstCube == null || secondCube == null
                    || firstState == null || secondState == null) {
                return false;
            }

            BlockPos firstPos = new BlockPos(firstX, firstY, firstZ);
            BlockPos secondPos = new BlockPos(secondX, secondY, secondZ);
            IBlockState secondBefore = secondCube.getBlockState(secondPos);
            if (secondCube.setBlockState(secondPos, secondState) == null) {
                return false;
            }
            if (firstCube.setBlockState(firstPos, firstState) == null) {
                secondCube.setBlockState(secondPos, secondBefore);
                markDirty(secondCube);
                return false;
            }
            markDirty(firstCube);
            if (secondCube != firstCube) {
                markDirty(secondCube);
            }
            return true;
        }

        @Override
        public void scheduleUpdate(int x, int y, int z, String targetRegistryPath,
                int delay) {
            Block block = ForgeRegistries.BLOCKS.getValue(CncRegistryIds.id(targetRegistryPath));
            if (block != null) {
                world.scheduleUpdate(new BlockPos(x, y, z), block, delay);
            }
        }

        private IBlockState stateAt(int x, int y, int z) {
            ICube targetCube = cubeAt(x, y, z);
            return targetCube == null ? null
                    : targetCube.getBlockState(new BlockPos(x, y, z));
        }

        private ICube cubeAt(int x, int y, int z) {
            if (outsideFiniteHeight(y)) {
                return null;
            }
            BlockPos position = new BlockPos(x, y, z);
            if (cube.containsBlockPos(position)) {
                return cube;
            }
            return ((ICubicWorld) world).getCubeCache().getLoadedCube(
                    x >> 4, y >> 4, z >> 4);
        }

        private boolean outsideFiniteHeight(int y) {
            return y < CavesNotCliffsWorldType.MIN_HEIGHT
                    || y >= CavesNotCliffsWorldType.MAX_HEIGHT;
        }
    }
}

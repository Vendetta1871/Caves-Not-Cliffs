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
import java.util.Set;
import java.util.WeakHashMap;

/** Applies the versioned content migration to both ordinary chunks and CubicChunks cubes. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class LegacyChunkMigrationHandler {
    private static final Logger LOGGER = LogManager.getLogger("CavesNotCliffs/ChunkMigration");
    private static final Set<Object> COMPLETED = Collections.newSetFromMap(new WeakHashMap<>());

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
        LegacyChunkMigration.Bounds bounds = cubeBounds(cube.getCoords());
        migrateLoaded(event.getData(), world.getSeed(), bounds,
                new CubeVolume(world, cube), cube);
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
            LOGGER.warn("Deferred {} legacy content blocks because their canonical target is not "
                            + "registered or could not be stored; migration remains at version {}",
                    result.getDeferredBlocks(), result.getResultingVersion());
        }
    }

    private static void writeCompletedVersion(NBTTagCompound data,
            LegacyChunkMigration.Bounds bounds, LegacyChunkMigration.Volume volume,
            Object storage) {
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
        synchronized (COMPLETED) {
            COMPLETED.add(storage);
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

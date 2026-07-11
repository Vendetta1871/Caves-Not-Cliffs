package net.celestiald.cavesnotcliffs.registry;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.CubeDataEvent;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import net.celestiald.cavesnotcliffs.CavesNotCliffs;
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
        migrateLoaded(event.getData(), world.getSeed(), bounds, new ChunkVolume(chunk), chunk);
    }

    @SubscribeEvent
    public static void onChunkSave(ChunkDataEvent.Save event) {
        World world = event.getWorld();
        if (world.isRemote || isCubic(world)) {
            return;
        }
        writeCompletedVersion(event.getData(), chunkBounds(event.getChunk()),
                new ChunkVolume(event.getChunk()), event.getChunk());
    }

    @SubscribeEvent
    public static void onCubeLoad(CubeDataEvent.Load event) {
        World world = event.getWorld();
        if (world.isRemote || !isCubic(world)) {
            return;
        }

        ICube cube = event.getCube();
        LegacyChunkMigration.Bounds bounds = cubeBounds(cube.getCoords());
        migrateLoaded(event.getData(), world.getSeed(), bounds, new CubeVolume(cube), cube);
    }

    @SubscribeEvent
    public static void onCubeSave(CubeDataEvent.Save event) {
        World world = event.getWorld();
        if (world.isRemote || !isCubic(world)) {
            return;
        }
        ICube cube = event.getCube();
        writeCompletedVersion(event.getData(), cubeBounds(cube.getCoords()),
                new CubeVolume(cube), cube);
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
        private final Chunk chunk;

        private ChunkVolume(Chunk chunk) {
            this.chunk = chunk;
        }

        @Override
        public String blockPathAt(int x, int y, int z) {
            return path(chunk.getBlockState(new BlockPos(x, y, z)));
        }

        @Override
        public int blockMetadataAt(int x, int y, int z) {
            IBlockState state = chunk.getBlockState(new BlockPos(x, y, z));
            return state.getBlock().getMetaFromState(state);
        }

        @Override
        public boolean isAirAt(int x, int y, int z) {
            return chunk.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.AIR;
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
    }

    private static final class CubeVolume extends RegistryVolume {
        private final ICube cube;

        private CubeVolume(ICube cube) {
            this.cube = cube;
        }

        @Override
        public String blockPathAt(int x, int y, int z) {
            return path(cube.getBlockState(new BlockPos(x, y, z)));
        }

        @Override
        public int blockMetadataAt(int x, int y, int z) {
            IBlockState state = cube.getBlockState(new BlockPos(x, y, z));
            return state.getBlock().getMetaFromState(state);
        }

        @Override
        public boolean isAirAt(int x, int y, int z) {
            return cube.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.AIR;
        }

        @Override
        public boolean replace(int x, int y, int z, String targetRegistryPath) {
            IBlockState target = target(targetRegistryPath);
            return target != null
                    && cube.setBlockState(new BlockPos(x, y, z), target) != null;
        }

        @Override
        public boolean replace(int x, int y, int z, String targetRegistryPath,
                int metadata) {
            IBlockState target = target(targetRegistryPath, metadata);
            return target != null
                    && cube.setBlockState(new BlockPos(x, y, z), target) != null;
        }
    }
}

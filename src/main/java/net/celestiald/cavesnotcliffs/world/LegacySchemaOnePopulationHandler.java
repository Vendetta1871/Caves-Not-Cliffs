package net.celestiald.cavesnotcliffs.world;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;

/** Replays only missing draft-v2 schema-1 cube decorators after cubic-to-finite import. */
public final class LegacySchemaOnePopulationHandler {
    public static final LegacySchemaOnePopulationHandler INSTANCE =
            new LegacySchemaOnePopulationHandler();
    public static final String NBT_KEY = "CavesNotCliffsSchema1Population";
    public static final int VERSION = 2;
    public static final int COMPLETE_MASK = 0xff;

    private static final int LEGACY_VERSION = 1;
    private static final int MIN_CUBE_Y = -4;
    private static final int MAX_CUBE_Y = 3;
    private static final Set<String> VERSION_ONE_KEYS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList("version", "mask")));
    private static final Set<String> VERSION_TWO_KEYS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList("version", "mask", "phase")));

    private final Map<Chunk, Progress> progress = new WeakHashMap<Chunk, Progress>();
    private final Set<Chunk> pending =
            Collections.newSetFromMap(new WeakHashMap<Chunk, Boolean>());
    private final Set<Chunk> running =
            Collections.newSetFromMap(new WeakHashMap<Chunk, Boolean>());

    private LegacySchemaOnePopulationHandler() {
    }

    @SubscribeEvent
    public synchronized void onChunkDataLoad(ChunkDataEvent.Load event) {
        if (event.getWorld().isRemote) {
            return;
        }
        Progress loaded = readProgress(event.getData());
        if (loaded == null) {
            return;
        }
        requireRuntimeContract(event.getWorld());
        Chunk chunk = event.getChunk();
        validateTerrainFlag(chunk, loaded);
        if (loaded.phase == Phase.RUNNING || loaded.phase == Phase.FAILED) {
            String operation = loaded.phase == Phase.RUNNING
                    ? "vanilla/Forge world generation" : "cave-band decoration";
            throw new IllegalStateException("Schema-1 population for chunk (" + chunk.x + ','
                    + chunk.z + ") was interrupted during " + operation + ". Restore a "
                    + "known-good backup; for a newly generated chunk, delete and regenerate "
                    + "only that chunk after confirming it contains no retained player data.");
        }
        if (loaded.mask == COMPLETE_MASK) {
            event.getData().removeTag(NBT_KEY);
            chunk.markDirty();
            return;
        }
        progress.put(chunk, loaded);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public synchronized void onChunkLoad(ChunkEvent.Load event) {
        if (event.getWorld().isRemote) {
            return;
        }
        Chunk chunk = event.getChunk();
        Progress state = progress.get(chunk);
        if (state != null && state.phase == Phase.COMPLETE
                && state.mask != COMPLETE_MASK) {
            pending.add(chunk);
        }
    }

    @SubscribeEvent
    public synchronized void onChunkDataSave(ChunkDataEvent.Save event) {
        if (event.getWorld().isRemote) {
            return;
        }
        Chunk chunk = event.getChunk();
        Progress state = progress.get(chunk);
        if (state == null) {
            return;
        }
        writeProgress(event.getData(), state, true);
        if (state.mask == COMPLETE_MASK) {
            progress.remove(chunk);
            pending.remove(chunk);
            running.remove(chunk);
        }
    }

    @SubscribeEvent
    public synchronized void onWorldTick(TickEvent.WorldTickEvent event) {
        World world = event.world;
        if (event.phase != TickEvent.Phase.END || world.isRemote
                || world.provider.getDimension() != 0) {
            return;
        }
        if (!hasPendingForWorld(world)) {
            return;
        }
        requireRuntimeContract(world);

        Chunk selected = selectReadyChunk(world);
        if (selected == null) {
            return;
        }
        if (!running.add(selected)) {
            throw new IllegalStateException("Nested schema-1 population replay for chunk ("
                    + selected.x + ',' + selected.z + ')');
        }
        try {
            replayMissingBands(world, selected);
        } finally {
            running.remove(selected);
            Progress remaining = progress.get(selected);
            if (remaining != null && remaining.phase == Phase.COMPLETE
                    && remaining.mask != COMPLETE_MASK && isLoaded(world, selected)) {
                pending.add(selected);
            }
        }
    }

    /** Marks the start of schema-1 cube-Y=0 vanilla population. */
    synchronized void beginVanillaPopulation(Chunk chunk) {
        Progress state = progress.get(chunk);
        if (state == null) {
            requireRuntimeContract(chunk.getWorld());
            state = new Progress(0, Phase.NEEDS_VANILLA);
            progress.put(chunk, state);
        }
        requireRuntimeContract(chunk.getWorld());
        if (state.phase != Phase.NEEDS_VANILLA || (state.mask & bit(0)) != 0) {
            throw new IllegalStateException("Imported schema-1 chunk (" + chunk.x + ','
                    + chunk.z + ") cannot begin vanilla population from phase "
                    + state.phase + " and mask " + state.mask);
        }
        state.phase = Phase.RUNNING;
        chunk.markDirty();
    }

    /** Coremod hook immediately after Forge's GameRegistry.generateWorld call returns. */
    public static void afterForgeWorldGeneration(Chunk chunk) {
        INSTANCE.finishVanillaPopulation(chunk);
    }

    private synchronized void finishVanillaPopulation(Chunk chunk) {
        Progress state = progress.get(chunk);
        if (state == null) {
            return;
        }
        requireRuntimeContract(chunk.getWorld());
        if (state.phase != Phase.RUNNING) {
            throw new IllegalStateException("Imported schema-1 chunk (" + chunk.x + ','
                    + chunk.z + ") reached the post-Forge hook from phase " + state.phase);
        }
        state.phase = Phase.COMPLETE;
        pending.add(chunk);
        chunk.markDirty();
    }

    synchronized void markPopulated(Chunk chunk, int cubeY) {
        Progress state = progress.get(chunk);
        if (state == null) {
            return;
        }
        if (state.phase != Phase.COMPLETE) {
            throw new IllegalStateException("Cannot mark schema-1 cube Y=" + cubeY
                    + " before vanilla/Forge population completes");
        }
        state.mask |= bit(cubeY);
        chunk.markDirty();
        if (state.mask == COMPLETE_MASK) {
            pending.remove(chunk);
        }
    }

    private Chunk selectReadyChunk(World world) {
        Chunk selected = null;
        for (Chunk chunk : pending) {
            Progress state = progress.get(chunk);
            if (chunk.getWorld() != world || state == null || state.phase != Phase.COMPLETE
                    || state.mask == COMPLETE_MASK || running.contains(chunk)
                    || !isLoaded(world, chunk)
                    || !neighborsLoaded(world, chunk.x, chunk.z)) {
                continue;
            }
            if (selected == null || chunk.x < selected.x
                    || chunk.x == selected.x && chunk.z < selected.z) {
                selected = chunk;
            }
        }
        if (selected != null) {
            pending.remove(selected);
        }
        return selected;
    }

    private boolean hasPendingForWorld(World world) {
        for (Chunk chunk : pending) {
            if (chunk.getWorld() == world) {
                return true;
            }
        }
        return false;
    }

    private void replayMissingBands(World world, Chunk chunk) {
        Progress state = progress.get(chunk);
        if (state == null || state.phase != Phase.COMPLETE) {
            throw new IllegalStateException("Schema-1 replay lost its population state");
        }
        for (int cubeY : missingBands(state.mask)) {
            try {
                decorateBandAtomically(world, chunk.x, cubeY, chunk.z);
                markPopulated(chunk, cubeY);
            } catch (RuntimeException | Error failure) {
                state.phase = Phase.FAILED;
                chunk.markDirty();
                throw failure;
            }
        }
    }

    private static void decorateBandAtomically(World world, int chunkX, int cubeY, int chunkZ) {
        IBlockState[] before = snapshotBand(world, chunkX, cubeY, chunkZ);
        try {
            decorateBand(world, chunkX, cubeY, chunkZ);
        } catch (Throwable failure) {
            try {
                restoreBand(world, chunkX, cubeY, chunkZ, before);
            } catch (Throwable restoreFailure) {
                failure.addSuppressed(restoreFailure);
            }
            if (failure instanceof RuntimeException) {
                throw (RuntimeException) failure;
            }
            if (failure instanceof Error) {
                throw (Error) failure;
            }
            throw new IllegalStateException("Schema-1 decorator failed for cube (" + chunkX
                    + ',' + cubeY + ',' + chunkZ + ')', failure);
        }
    }

    static void decorateBand(World world, int chunkX, int cubeY, int chunkZ) {
        FiniteSectionPos position = new FiniteSectionPos(chunkX, cubeY, chunkZ);
        Random random = new Random(CaveBiomeSampler.mix64(world.getSeed()
                ^ (long) chunkX * 341873128712L
                ^ (long) cubeY * 132897987541L
                ^ (long) chunkZ * 42317861L));
        CaveBiomeDecorator.decorate(world, random, position);
    }

    private static IBlockState[] snapshotBand(World world, int chunkX, int cubeY, int chunkZ) {
        IBlockState[] states = new IBlockState[16 * 16 * 16];
        int index = 0;
        int minX = chunkX << 4;
        int minY = cubeY << 4;
        int minZ = chunkZ << 4;
        for (int localY = 0; localY < 16; localY++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                for (int localX = 0; localX < 16; localX++) {
                    states[index++] = world.getBlockState(new BlockPos(
                            minX + localX, minY + localY, minZ + localZ));
                }
            }
        }
        return states;
    }

    private static void restoreBand(World world, int chunkX, int cubeY, int chunkZ,
            IBlockState[] states) {
        int index = 0;
        int minX = chunkX << 4;
        int minY = cubeY << 4;
        int minZ = chunkZ << 4;
        for (int localY = 0; localY < 16; localY++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                for (int localX = 0; localX < 16; localX++) {
                    BlockPos position = new BlockPos(
                            minX + localX, minY + localY, minZ + localZ);
                    if (!world.getBlockState(position).equals(states[index])) {
                        world.setBlockState(position, states[index], 2);
                    }
                    index++;
                }
            }
        }
    }

    static int[] missingBands(int mask) {
        validateMask(mask);
        int[] result = new int[Integer.bitCount(COMPLETE_MASK & ~mask)];
        int index = 0;
        for (int cubeY = MIN_CUBE_Y; cubeY <= MAX_CUBE_Y; cubeY++) {
            if ((mask & bit(cubeY)) == 0) {
                result[index++] = cubeY;
            }
        }
        return result;
    }

    public static int bit(int cubeY) {
        if (cubeY < MIN_CUBE_Y || cubeY > MAX_CUBE_Y) {
            throw new IllegalArgumentException(
                    "Schema-1 population cube is outside -4..3: " + cubeY);
        }
        return 1 << (cubeY - MIN_CUBE_Y);
    }

    /** Writes the converter's initial marker, including the separate vanilla/Forge phase. */
    public static void writeInitialMarker(NBTTagCompound data, int mask) {
        validateMask(mask);
        Phase phase = (mask & bit(0)) == 0 ? Phase.NEEDS_VANILLA : Phase.COMPLETE;
        writeProgress(data, new Progress(mask, phase), false);
    }

    static Progress readProgress(NBTTagCompound data) {
        if (data == null || !data.hasKey(NBT_KEY)) {
            return null;
        }
        if (!data.hasKey(NBT_KEY, 10)) {
            throw new IllegalStateException(
                    "Schema-1 population marker is not a compound");
        }
        NBTTagCompound marker = data.getCompoundTag(NBT_KEY);
        if (!marker.hasKey("version", 3) || !marker.hasKey("mask", 3)) {
            throw new IllegalStateException(
                    "Schema-1 population marker has an unsupported shape");
        }
        int version = marker.getInteger("version");
        int mask = marker.getInteger("mask");
        validateMask(mask);
        Phase phase;
        if (version == LEGACY_VERSION && marker.getKeySet().equals(VERSION_ONE_KEYS)) {
            phase = (mask & bit(0)) == 0 ? Phase.NEEDS_VANILLA : Phase.COMPLETE;
        } else if (version == VERSION && marker.getKeySet().equals(VERSION_TWO_KEYS)
                && marker.hasKey("phase", 3)) {
            phase = Phase.byId(marker.getInteger("phase"));
        } else {
            throw new IllegalStateException(
                    "Unsupported schema-1 population marker version or shape " + version);
        }
        validateState(mask, phase);
        return new Progress(mask, phase);
    }

    static void writeProgress(NBTTagCompound data, Progress state, boolean removeComplete) {
        if (data == null) {
            throw new NullPointerException("data");
        }
        validateState(state.mask, state.phase);
        if (removeComplete && state.mask == COMPLETE_MASK) {
            data.removeTag(NBT_KEY);
            return;
        }
        NBTTagCompound marker = new NBTTagCompound();
        marker.setInteger("version", VERSION);
        marker.setInteger("mask", state.mask);
        marker.setInteger("phase", state.phase.id);
        data.setTag(NBT_KEY, marker);
    }

    private static void validateState(int mask, Phase phase) {
        validateMask(mask);
        if (phase == null) {
            throw new IllegalStateException("Schema-1 population phase is missing");
        }
        boolean cubeZero = (mask & bit(0)) != 0;
        if (cubeZero && phase != Phase.COMPLETE && phase != Phase.FAILED) {
            throw new IllegalStateException(
                    "Schema-1 cube Y=0 is complete before vanilla/Forge phase " + phase);
        }
        if (!cubeZero || phase == Phase.COMPLETE || phase == Phase.FAILED) {
            return;
        }
        throw new IllegalStateException("Unsupported schema-1 population state");
    }

    private static void validateTerrainFlag(Chunk chunk, Progress state) {
        boolean terrainPopulated = chunk.isTerrainPopulated();
        if (state.phase == Phase.NEEDS_VANILLA && terrainPopulated) {
            throw new IllegalStateException("Schema-1 chunk (" + chunk.x + ',' + chunk.z
                    + ") needs vanilla population but TerrainPopulated is already true");
        }
        if ((state.phase == Phase.COMPLETE || state.phase == Phase.FAILED)
                && !terrainPopulated) {
            throw new IllegalStateException("Schema-1 chunk (" + chunk.x + ',' + chunk.z
                    + ") completed vanilla population but TerrainPopulated is false");
        }
    }

    private static void validateMask(int mask) {
        if (mask < 0 || (mask & ~COMPLETE_MASK) != 0) {
            throw new IllegalStateException(
                    "Schema-1 population mask is outside 0..255: " + mask);
        }
    }

    private static boolean neighborsLoaded(World world, int chunkX, int chunkZ) {
        IChunkProvider provider = world.getChunkProvider();
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                Chunk loaded = provider.getLoadedChunk(
                        chunkX + offsetX, chunkZ + offsetZ);
                if (loaded == null || !loaded.isLoaded()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isLoaded(World world, Chunk chunk) {
        return world.getChunkProvider().getLoadedChunk(chunk.x, chunk.z) == chunk;
    }

    private static void requireRuntimeContract(World world) {
        if (!(world instanceof WorldServer) || world.provider.getDimension() != 0
                || world.getMinecraftServer() == null
                || !world.getMinecraftServer().isCallingFromMinecraftThread()) {
            throw new IllegalStateException(
                    "Schema-1 population replay requires the server-thread Overworld");
        }
        CavesNotCliffsWorldData contract = CavesNotCliffsWorldData.read(world.getWorldInfo());
        if (contract == null
                || contract.getTerrainSchema() != CavesNotCliffsWorldData.LEGACY_SCHEMA) {
            throw new IllegalStateException(
                    "Schema-1 population progress exists outside its persisted schema contract");
        }
        if (!(world.getChunkProvider() instanceof ChunkProviderServer)
                || !(((ChunkProviderServer) world.getChunkProvider()).chunkGenerator
                instanceof LegacyFiniteChunkGenerator)) {
            throw new IllegalStateException(
                    "Schema-1 population replay requires LegacyFiniteChunkGenerator");
        }
    }

    enum Phase {
        NEEDS_VANILLA(0),
        RUNNING(1),
        COMPLETE(2),
        FAILED(3);

        private final int id;

        Phase(int id) {
            this.id = id;
        }

        private static Phase byId(int id) {
            for (Phase phase : values()) {
                if (phase.id == id) {
                    return phase;
                }
            }
            throw new IllegalStateException(
                    "Unsupported schema-1 population phase " + id);
        }
    }

    static final class Progress {
        private int mask;
        private Phase phase;

        Progress(int mask, Phase phase) {
            validateState(mask, phase);
            this.mask = mask;
            this.phase = phase;
        }

        int getMask() {
            return mask;
        }

        Phase getPhase() {
            return phase;
        }
    }
}

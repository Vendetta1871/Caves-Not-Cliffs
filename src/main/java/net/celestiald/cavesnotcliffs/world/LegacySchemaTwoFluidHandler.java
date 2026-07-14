package net.celestiald.cavesnotcliffs.world;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Replays aquifer fluid ticks for schema-2 cubes that had not populated before import. */
public final class LegacySchemaTwoFluidHandler {
    public static final LegacySchemaTwoFluidHandler INSTANCE =
            new LegacySchemaTwoFluidHandler();
    public static final String NBT_KEY = "CavesNotCliffsSchema2Fluids";
    public static final int VERSION = 1;
    public static final int MIN_CUBE_Y = -4;
    public static final int MAX_CUBE_Y = 19;
    public static final int COMPLETE_MASK = (1 << 24) - 1;

    private static final Set<String> MARKER_KEYS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList("version", "mask")));

    private final Map<Chunk, Integer> pending = new WeakHashMap<Chunk, Integer>();

    private LegacySchemaTwoFluidHandler() {
    }

    @SubscribeEvent
    public synchronized void onChunkDataLoad(ChunkDataEvent.Load event) {
        if (event.getWorld().isRemote) {
            return;
        }
        int mask = readMask(event.getData());
        if (mask < 0) {
            return;
        }
        Chunk chunk = event.getChunk();
        requireRuntimeContract(event.getWorld(), chunk);
        if (!chunk.isTerrainPopulated()) {
            throw new IllegalStateException("Schema-2 fluid replay marker exists on unpopulated "
                    + "chunk (" + chunk.x + ',' + chunk.z + ')');
        }
        if (mask == COMPLETE_MASK) {
            event.getData().removeTag(NBT_KEY);
            chunk.markDirty();
            return;
        }
        pending.put(chunk, mask);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public synchronized void onChunkLoad(ChunkEvent.Load event) {
        if (event.getWorld().isRemote) {
            return;
        }
        Chunk chunk = event.getChunk();
        Integer mask = pending.get(chunk);
        if (mask == null) {
            return;
        }
        V118ChunkGenerator generator = requireRuntimeContract(event.getWorld(), chunk);
        generator.replayImportedFluidTicks(chunk.x, chunk.z, mask);
        pending.remove(chunk);
        chunk.markDirty();
    }

    @SubscribeEvent
    public synchronized void onChunkDataSave(ChunkDataEvent.Save event) {
        if (event.getWorld().isRemote) {
            return;
        }
        Integer mask = pending.get(event.getChunk());
        if (mask != null) {
            writeInitialMarker(event.getData(), mask);
        }
    }

    private static V118ChunkGenerator requireRuntimeContract(World world, Chunk chunk) {
        if (world.provider.getDimension() != 0) {
            throw new IllegalStateException("Schema-2 fluid replay marker exists outside the "
                    + "Overworld on chunk (" + chunk.x + ',' + chunk.z + ')');
        }
        V118ChunkGenerator generator = V118ChunkGenerator.forWorld(world);
        if (generator == null) {
            throw new IllegalStateException("Schema-2 fluid replay marker has no native terrain "
                    + "generator for chunk (" + chunk.x + ',' + chunk.z + ')');
        }
        return generator;
    }

    public static void writeInitialMarker(NBTTagCompound data, int mask) {
        validateMask(mask);
        if ((mask & bit(0)) == 0) {
            throw new IllegalStateException(
                    "Schema-2 fluid replay cannot precede column-feature population");
        }
        if (mask == COMPLETE_MASK) {
            data.removeTag(NBT_KEY);
            return;
        }
        NBTTagCompound marker = new NBTTagCompound();
        marker.setInteger("version", VERSION);
        marker.setInteger("mask", mask);
        data.setTag(NBT_KEY, marker);
    }

    static int readMask(NBTTagCompound data) {
        if (data == null || !data.hasKey(NBT_KEY)) {
            return -1;
        }
        if (!data.hasKey(NBT_KEY, 10)) {
            throw new IllegalStateException("Schema-2 fluid replay marker is not a compound");
        }
        NBTTagCompound marker = data.getCompoundTag(NBT_KEY);
        if (!marker.getKeySet().equals(MARKER_KEYS)
                || marker.getInteger("version") != VERSION
                || !marker.hasKey("version", 3) || !marker.hasKey("mask", 3)) {
            throw new IllegalStateException(
                    "Schema-2 fluid replay marker has an unsupported version or shape");
        }
        int mask = marker.getInteger("mask");
        validateMask(mask);
        if ((mask & bit(0)) == 0) {
            throw new IllegalStateException(
                    "Schema-2 fluid replay marker is missing populated cube Y=0");
        }
        return mask;
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
                    "Schema-2 population cube is outside -4..19: " + cubeY);
        }
        return 1 << (cubeY - MIN_CUBE_Y);
    }

    private static void validateMask(int mask) {
        if (mask < 0 || (mask & ~COMPLETE_MASK) != 0) {
            throw new IllegalStateException(
                    "Schema-2 population mask is outside 0.." + COMPLETE_MASK + ": " + mask);
        }
    }
}

package net.celestiald.cavesnotcliffs.handler;

import net.celestiald.cavesnotcliffs.block.BlockLavaCauldron;
import net.celestiald.cavesnotcliffs.block.BlockPowderSnowCauldron;
import net.celestiald.cavesnotcliffs.dripstone.CauldronMechanics;
import net.celestiald.cavesnotcliffs.dripstone.CauldronStateBridge;
import net.celestiald.cavesnotcliffs.dripstone.VanillaCauldronMeta;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * One-time migration of the legacy hidden v2 cauldron storage blocks back to the vanilla
 * cauldron. Since the 1.18.2 contents now live on {@code minecraft:cauldron} itself (see
 * {@code CauldronMixin}), every hidden {@code lava_cauldron}/{@code powder_snow_cauldron} block
 * found in an existing world is converted to the equivalent vanilla state; vanilla cauldrons
 * and third-party blocks are never touched.
 */
public final class LavaCauldronHandler {
    public static final LavaCauldronHandler INSTANCE = new LavaCauldronHandler();

    static final String BRIDGE_VERSION_KEY = "CavesNotCliffsCauldronBridge";
    /** Version 2: hidden storage blocks are migrated back to vanilla (was: bridged to hidden). */
    static final int BRIDGE_VERSION = 2;

    private static final Set<Chunk> SKIP_SCAN =
            Collections.newSetFromMap(new WeakHashMap<Chunk, Boolean>());
    private static final Set<Chunk> COMPLETED =
            Collections.newSetFromMap(new WeakHashMap<Chunk, Boolean>());

    private LavaCauldronHandler() {
    }

    interface Volume {
        IBlockState stateAt(int x, int y, int z);

        boolean migrateAt(int x, int y, int z);
    }

    @SubscribeEvent
    public void onChunkDataLoad(ChunkDataEvent.Load event) {
        if (!event.getWorld().isRemote && hasCurrentVersion(event.getData())) {
            rememberSkip(event.getChunk());
        }
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        World world = event.getWorld();
        if (world.isRemote) {
            return;
        }
        if (consumeSkip(event.getChunk())) {
            rememberCompleted(event.getChunk());
            return;
        }
        migrateChunk(world, event.getChunk());
    }

    @SubscribeEvent
    public void onChunkDataSave(ChunkDataEvent.Save event) {
        if (!event.getWorld().isRemote && isCompleted(event.getChunk())) {
            writeCurrentVersion(event.getData());
        }
    }

    private static void migrateChunk(final World world, final Chunk chunk) {
        int minX = chunk.x << 4;
        int minZ = chunk.z << 4;
        ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();
        for (final ExtendedBlockStorage section : sections) {
            if (section == null || section == Chunk.NULL_BLOCK_STORAGE || section.isEmpty()) {
                continue;
            }
            final int minY = section.getYLocation();
            migrateVolume(minX, minY, minZ, 16, 16, 16, new Volume() {
                @Override
                public IBlockState stateAt(int x, int y, int z) {
                    return section.get(x & 15, y & 15, z & 15);
                }

                @Override
                public boolean migrateAt(int x, int y, int z) {
                    return migrateVanillaAt(world, new BlockPos(x, y, z));
                }
            });
        }
        rememberCompleted(chunk);
        chunk.markDirty();
    }

    static int migrateVolume(int minX, int minY, int minZ,
            int sizeX, int sizeY, int sizeZ, Volume volume) {
        if (sizeX < 0 || sizeY < 0 || sizeZ < 0 || volume == null) {
            throw new IllegalArgumentException("Cauldron migration volume is invalid");
        }
        int converted = 0;
        for (int y = minY; y < minY + sizeY; ++y) {
            for (int z = minZ; z < minZ + sizeZ; ++z) {
                for (int x = minX; x < minX + sizeX; ++x) {
                    if (isLegacyHiddenCauldron(volume.stateAt(x, y, z))
                            && volume.migrateAt(x, y, z)) {
                        converted++;
                    }
                }
            }
        }
        return converted;
    }

    /** The hidden v2 storage identities; vanilla and modded cauldrons stay untouched. */
    static boolean isLegacyHiddenCauldron(IBlockState state) {
        if (state == null) {
            return false;
        }
        Block block = state.getBlock();
        return block instanceof BlockLavaCauldron.BlockCustom
                || block instanceof BlockPowderSnowCauldron.BlockCustom;
    }

    /** Vanilla metadata holding the same contents as the given hidden storage state. */
    static int legacyHiddenMeta(IBlockState state) {
        Block block = state.getBlock();
        if (block instanceof BlockLavaCauldron.BlockCustom) {
            return VanillaCauldronMeta.toMeta(
                    ((BlockLavaCauldron.BlockCustom) block).mechanicsState(state));
        }
        if (block instanceof BlockPowderSnowCauldron.BlockCustom) {
            int level = Math.max(1, Math.min(CauldronMechanics.MAX_LEVEL,
                    state.getValue(BlockCauldron.LEVEL)));
            return VanillaCauldronMeta.toMeta(CauldronMechanics.powderSnow(level));
        }
        throw new IllegalArgumentException("Not a legacy hidden cauldron: " + state);
    }

    static boolean migrateVanillaAt(World world, BlockPos pos) {
        IBlockState current = world.getBlockState(pos);
        if (!isLegacyHiddenCauldron(current)) {
            return false;
        }
        return CauldronStateBridge.setState(world, pos,
                Blocks.CAULDRON.getStateFromMeta(legacyHiddenMeta(current)));
    }

    static boolean hasCurrentVersion(NBTTagCompound data) {
        return data != null && data.getInteger(BRIDGE_VERSION_KEY) >= BRIDGE_VERSION;
    }

    static void writeCurrentVersion(NBTTagCompound data) {
        if (data == null) {
            throw new IllegalArgumentException("Chunk data is required");
        }
        data.setInteger(BRIDGE_VERSION_KEY, BRIDGE_VERSION);
    }

    private static void rememberSkip(Chunk storage) {
        synchronized (SKIP_SCAN) {
            SKIP_SCAN.add(storage);
        }
    }

    private static boolean consumeSkip(Chunk storage) {
        synchronized (SKIP_SCAN) {
            return SKIP_SCAN.remove(storage);
        }
    }

    private static void rememberCompleted(Chunk storage) {
        synchronized (COMPLETED) {
            COMPLETED.add(storage);
        }
    }

    private static boolean isCompleted(Chunk storage) {
        synchronized (COMPLETED) {
            return COMPLETED.contains(storage);
        }
    }

}

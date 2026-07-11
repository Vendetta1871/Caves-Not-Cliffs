package net.celestiald.cavesnotcliffs.handler;

import net.celestiald.cavesnotcliffs.block.BlockLavaCauldron;
import net.celestiald.cavesnotcliffs.dripstone.CauldronStateBridge;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * One-time and lazy bridge from the exact vanilla 1.12 cauldron identity to the hidden 1.18.2
 * content identities. Vanilla levels are preserved; third-party cauldron blocks are untouched.
 */
public final class LavaCauldronHandler {
    public static final LavaCauldronHandler INSTANCE = new LavaCauldronHandler();

    static final String BRIDGE_VERSION_KEY = "CavesNotCliffsCauldronBridge";
    static final int BRIDGE_VERSION = 1;

    private static final Set<Chunk> SKIP_SCAN =
            Collections.newSetFromMap(new WeakHashMap<Chunk, Boolean>());
    private static final Set<Chunk> COMPLETED =
            Collections.newSetFromMap(new WeakHashMap<Chunk, Boolean>());

    private LavaCauldronHandler() {
    }

    interface Volume {
        IBlockState stateAt(int x, int y, int z);

        boolean bridgeAt(int x, int y, int z);
    }

    @SubscribeEvent
    public void onCauldronPlaced(BlockEvent.PlaceEvent event) {
        World world = event.getWorld();
        if (world.isRemote || event.getPlacedBlock().getBlock() != Blocks.CAULDRON
                || BlockLavaCauldron.block == null) {
            return;
        }
        CauldronStateBridge.bridgeVanillaAt(world, event.getPos());
    }

    /**
     * Lazy fallback for cauldrons introduced after a one-time chunk scan. Running at LOWEST lets
     * protection handlers deny the interaction before this bridge mutates the stored identity.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRightClickCauldron(PlayerInteractEvent.RightClickBlock event) {
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState vanilla = world.getBlockState(pos);
        EntityPlayer player = event.getEntityPlayer();
        if (!CauldronStateBridge.isVanillaCauldron(vanilla)
                || BlockLavaCauldron.block == null
                || !world.isRemote && !world.isBlockModifiable(player, pos)) {
            return;
        }

        IBlockState bridged = CauldronStateBridge.bridgeVanillaState(
                vanilla, BlockLavaCauldron.block);
        if (!world.isRemote) {
            CauldronStateBridge.setState(world, pos, bridged);
            bridged = world.getBlockState(pos);
        }
        if (!(bridged.getBlock() instanceof BlockLavaCauldron.BlockCustom)) {
            return;
        }

        Vec3d hit = event.getHitVec();
        float hitX = hit == null ? 0.5F : (float) (hit.x - pos.getX());
        float hitY = hit == null ? 0.5F : (float) (hit.y - pos.getY());
        float hitZ = hit == null ? 0.5F : (float) (hit.z - pos.getZ());
        EnumFacing face = event.getFace() == null ? EnumFacing.UP : event.getFace();
        boolean handled = ((BlockLavaCauldron.BlockCustom) bridged.getBlock())
                .onBlockActivated(world, pos, bridged, player, event.getHand(), face,
                        hitX, hitY, hitZ);
        if (handled) {
            event.setCancellationResult(EnumActionResult.SUCCESS);
            event.setCanceled(true);
        }
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
        bridgeChunk(world, event.getChunk());
    }

    @SubscribeEvent
    public void onChunkDataSave(ChunkDataEvent.Save event) {
        if (!event.getWorld().isRemote && isCompleted(event.getChunk())) {
            writeCurrentVersion(event.getData());
        }
    }

    /** Catches vanilla structure cauldrons added after a newly generated chunk's load event. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onChunkPopulated(PopulateChunkEvent.Post event) {
        World world = event.getWorld();
        if (!world.isRemote) {
            bridgeChunk(world,
                    world.getChunkFromChunkCoords(event.getChunkX(), event.getChunkZ()));
        }
    }

    private static void bridgeChunk(final World world, final Chunk chunk) {
        if (BlockLavaCauldron.block == null) {
            return;
        }
        int minX = chunk.x << 4;
        int minZ = chunk.z << 4;
        ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();
        for (final ExtendedBlockStorage section : sections) {
            if (section == null || section == Chunk.NULL_BLOCK_STORAGE || section.isEmpty()) {
                continue;
            }
            final int minY = section.getYLocation();
            bridgeVolume(minX, minY, minZ, 16, 16, 16, new Volume() {
                @Override
                public IBlockState stateAt(int x, int y, int z) {
                    return section.get(x & 15, y & 15, z & 15);
                }

                @Override
                public boolean bridgeAt(int x, int y, int z) {
                    return CauldronStateBridge.bridgeVanillaAt(
                            world, new BlockPos(x, y, z));
                }
            });
        }
        rememberCompleted(chunk);
        chunk.markDirty();
    }

    static int bridgeVolume(int minX, int minY, int minZ,
            int sizeX, int sizeY, int sizeZ, Volume volume) {
        if (sizeX < 0 || sizeY < 0 || sizeZ < 0 || volume == null) {
            throw new IllegalArgumentException("Cauldron bridge volume is invalid");
        }
        int converted = 0;
        for (int y = minY; y < minY + sizeY; ++y) {
            for (int z = minZ; z < minZ + sizeZ; ++z) {
                for (int x = minX; x < minX + sizeX; ++x) {
                    if (CauldronStateBridge.isVanillaCauldron(volume.stateAt(x, y, z))
                            && volume.bridgeAt(x, y, z)) {
                        converted++;
                    }
                }
            }
        }
        return converted;
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

package net.celestiald.cavesnotcliffs.migration;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Rebuilds imported height/skylight data after Forge has restored the saved numeric registry. */
public final class ImportedHeightMapRebuildHandler {
    public static final ImportedHeightMapRebuildHandler INSTANCE =
            new ImportedHeightMapRebuildHandler();

    private ImportedHeightMapRebuildHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChunkDataLoad(ChunkDataEvent.Load event) {
        if (event.getWorld().isRemote || !consumeMarker(event.getData())) {
            return;
        }
        Chunk chunk = event.getChunk();
        chunk.generateSkylightMap();
        chunk.markDirty();
    }

    static boolean consumeMarker(NBTTagCompound data) {
        if (data == null || data.getInteger(CubicColumnConverter.REBUILD_HEIGHT_MAP) < 1) {
            return false;
        }
        data.removeTag(CubicColumnConverter.REBUILD_HEIGHT_MAP);
        return true;
    }
}

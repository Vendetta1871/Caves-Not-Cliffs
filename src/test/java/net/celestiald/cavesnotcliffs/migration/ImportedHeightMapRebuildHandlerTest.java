package net.celestiald.cavesnotcliffs.migration;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ImportedHeightMapRebuildHandlerTest {
    @Test
    public void consumesTheImporterMarkerExactlyOnce() {
        NBTTagCompound data = new NBTTagCompound();
        data.setInteger(CubicColumnConverter.REBUILD_HEIGHT_MAP, 1);

        assertTrue(ImportedHeightMapRebuildHandler.consumeMarker(data));
        assertFalse(data.hasKey(CubicColumnConverter.REBUILD_HEIGHT_MAP));
        assertFalse(ImportedHeightMapRebuildHandler.consumeMarker(data));
    }

    @Test
    public void ignoresMissingAndLegacyZeroMarkers() {
        assertFalse(ImportedHeightMapRebuildHandler.consumeMarker(null));
        NBTTagCompound data = new NBTTagCompound();
        data.setInteger(CubicColumnConverter.REBUILD_HEIGHT_MAP, 0);
        assertFalse(ImportedHeightMapRebuildHandler.consumeMarker(data));
        assertTrue(data.hasKey(CubicColumnConverter.REBUILD_HEIGHT_MAP));
    }

    @Test
    public void rebuildsAfterNormalPriorityContentMigrations() throws Exception {
        Method handler = ImportedHeightMapRebuildHandler.class.getMethod(
                "onChunkDataLoad", ChunkDataEvent.Load.class);
        SubscribeEvent subscription = handler.getAnnotation(SubscribeEvent.class);

        assertTrue(subscription != null);
        assertTrue(subscription.priority() == EventPriority.LOWEST);
    }
}

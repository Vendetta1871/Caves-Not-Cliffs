package net.celestiald.cavesnotcliffs.world;

import net.minecraft.world.World;
import net.minecraft.world.WorldServerMulti;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * Seeds Cubic Chunks' per-world metadata before its normal-priority capability handler runs.
 * This uses reflection only for Cubic Chunks' persistence class; the supported generation API is
 * used everywhere else.  Pre-creating the record avoids changing Cubic Chunks' global defaults or
 * silently converting existing vanilla worlds.
 */
public final class WorldHeightBootstrap {
    private static final String DATA_NAME = "cubicChunksData";
    private static final String DATA_CLASS =
            "io.github.opencubicchunks.cubicchunks.core.world.WorldSavedCubicChunksData";

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void beforeCubicChunksInitialization(AttachCapabilitiesEvent<World> event) {
        World world = event.getObject();
        // AttachCapabilities fires from the World constructor, before secondary providers have
        // their dimension id assigned. WorldServerMulti is therefore the reliable dimension gate.
        if (world.isRemote || world instanceof WorldServerMulti
                || !CavesNotCliffsWorldType.isCavesNotCliffs(world)) {
            return;
        }

        try {
            Class<?> rawClass = Class.forName(DATA_CLASS);
            @SuppressWarnings("unchecked")
            Class<? extends WorldSavedData> dataClass =
                    (Class<? extends WorldSavedData>) rawClass.asSubclass(WorldSavedData.class);
            MapStorage storage = world.getPerWorldStorage();
            WorldSavedData data = storage.getOrLoadData(dataClass, DATA_NAME);

            if (data == null) {
                Constructor<? extends WorldSavedData> constructor =
                        dataClass.getConstructor(String.class, boolean.class, int.class, int.class);
                data = constructor.newInstance(DATA_NAME, true,
                        CavesNotCliffsWorldType.MIN_HEIGHT, CavesNotCliffsWorldType.MAX_HEIGHT);
                storage.setData(DATA_NAME, data);
            } else {
                // The world type did not exist before v2, but keep its contract self-healing if a
                // prerelease wrote Cubic Chunks' unbounded defaults into the save.
                setField(rawClass, data, "isCubicChunks", true);
                setField(rawClass, data, "minHeight", CavesNotCliffsWorldType.MIN_HEIGHT);
                setField(rawClass, data, "maxHeight", CavesNotCliffsWorldType.MAX_HEIGHT);
            }
            data.markDirty();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Caves Not Cliffs v2 requires a compatible Cubic Chunks "
                    + "1.12.2 build; unable to create the -64..319 world-height record", exception);
        }
    }

    private static void setField(Class<?> owner, Object target, String name, Object value)
            throws ReflectiveOperationException {
        Field field = owner.getField(name);
        field.set(target, value);
    }
}

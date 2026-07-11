package net.celestiald.cavesnotcliffs.world;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldType;
import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.config.CavesNotCliffsConfig;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerMulti;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.WorldInfo;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * Chooses the immutable world-format contract and seeds Cubic Chunks' per-world metadata before
 * its normal-priority capability handler runs. This uses reflection only for Cubic Chunks'
 * persistence class; the supported generation API is used everywhere else.
 */
public final class WorldHeightBootstrap {
    private static final String DATA_NAME = "cubicChunksData";
    private static final String DATA_CLASS =
            "io.github.opencubicchunks.cubicchunks.core.world.WorldSavedCubicChunksData";

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void beforeCubicChunksInitialization(AttachCapabilitiesEvent<World> event) {
        World world = event.getObject();
        // Every secondary dimension shares the Overworld's WorldInfo, so WorldServerMulti is the
        // essential gate: only the primary Overworld may choose or persist the terrain contract.
        if (world.isRemote || !(world instanceof WorldServer) || world instanceof WorldServerMulti
                || world.provider.getDimension() != 0) {
            return;
        }

        boolean existingWorld = world.getSaveHandler().loadWorldInfo() != null;
        // Deliberately do not even read the live config for an existing save. Its persisted schema
        // is the sole authority after first creation.
        boolean enabledForCreation = !existingWorld
                && CavesNotCliffsConfig.WORLD.enableForNewOverworlds;
        applyWorldFormatDecision(world, existingWorld, enabledForCreation);
        if (!CavesNotCliffsWorldType.isCavesNotCliffs(world)) {
            return;
        }

        seedCubicChunksHeight(world);
    }

    private void applyWorldFormatDecision(World world, boolean existingWorld, boolean enabled) {
        WorldInfo worldInfo = world.getWorldInfo();
        WorldType selected = worldInfo.getTerrainType();
        CavesNotCliffsWorldData saved = CavesNotCliffsWorldData.read(worldInfo);
        int schema = saved == null ? 0 : saved.getTerrainSchema();
        WorldActivationPolicy.SelectedKind selectedKind = selectedKind(selected);
        WorldActivationPolicy.Action action = WorldActivationPolicy.decide(
                existingWorld, enabled, selectedKind, schema);

        switch (action) {
            case ACTIVATE_SCHEMA_2:
                activateCurrentSchema(worldInfo, selected);
                break;
            case KEEP_SCHEMA_1:
                if (saved == null) {
                    CavesNotCliffsWorldData.writeLegacy(worldInfo);
                }
                break;
            case KEEP_SCHEMA_2:
                keepOrRepairCurrentSchema(worldInfo, selected, saved);
                break;
            case RESTORE_SCHEMA_1:
                requireLegacyAlias();
                worldInfo.setTerrainType(CavesNotCliffs.WORLD_TYPE);
                break;
            case RESTORE_SCHEMA_2:
                worldInfo.setTerrainType(requirePersistedWrapper(saved));
                break;
            case REVERT_NEW_SELECTION:
                worldInfo.setTerrainType(selected instanceof CavesNotCliffsWorldTypeWrapper
                        ? ((CavesNotCliffsWorldTypeWrapper) selected).getBaseType()
                        : WorldType.DEFAULT);
                break;
            case NONE:
            default:
                break;
        }
    }

    private void activateCurrentSchema(WorldInfo worldInfo, WorldType selected) {
        WorldType baseType;
        if (selected instanceof CavesNotCliffsWorldTypeWrapper) {
            baseType = ((CavesNotCliffsWorldTypeWrapper) selected).getBaseType();
        } else if (selected instanceof CavesNotCliffsWorldType) {
            // A stale dedicated-server level-type must not opt around the config contract.
            baseType = WorldType.DEFAULT;
        } else {
            baseType = selected;
        }

        CavesNotCliffsWorldTypeWrapper wrapper = CavesNotCliffsWorldTypes.wrapperForBase(baseType);
        if (wrapper == null) {
            throw new IllegalStateException("No Caves Not Cliffs wrapper was registered for world type "
                    + baseType.getName());
        }
        CavesNotCliffsWorldData.writeCurrent(worldInfo, baseType, wrapper.getTerrainProfile());
        worldInfo.setTerrainType(wrapper);
    }

    private void keepOrRepairCurrentSchema(WorldInfo worldInfo, WorldType selected,
            CavesNotCliffsWorldData saved) {
        if (saved == null) {
            if (!(selected instanceof CavesNotCliffsWorldTypeWrapper)) {
                throw new IllegalStateException("Schema-2 Caves Not Cliffs world lost its wrapper metadata");
            }
            CavesNotCliffsWorldTypeWrapper wrapper = (CavesNotCliffsWorldTypeWrapper) selected;
            CavesNotCliffsWorldData.writeCurrent(
                    worldInfo, wrapper.getBaseType(), wrapper.getTerrainProfile());
            return;
        }

        CavesNotCliffsWorldTypeWrapper expected = requirePersistedWrapper(saved);
        if (selected != expected) {
            worldInfo.setTerrainType(expected);
        }
    }

    private CavesNotCliffsWorldTypeWrapper requirePersistedWrapper(
            CavesNotCliffsWorldData saved) {
        if (saved == null) {
            throw new IllegalStateException("Schema-2 Caves Not Cliffs world has no persisted generator data");
        }
        CavesNotCliffsWorldTypeWrapper wrapper = CavesNotCliffsWorldTypes.wrapperForPersistedBase(
                saved.getBaseTypeName(), saved.getBaseTypeClass());
        if (wrapper == null) {
            throw new IllegalStateException("The saved Caves Not Cliffs base world type is unavailable: "
                    + saved.getBaseTypeName() + " (" + saved.getBaseTypeClass() + ")");
        }
        return wrapper;
    }

    private void requireLegacyAlias() {
        if (CavesNotCliffs.WORLD_TYPE == null) {
            throw new IllegalStateException("The Caves Not Cliffs schema-1 compatibility alias is unavailable");
        }
    }

    private WorldActivationPolicy.SelectedKind selectedKind(WorldType selected) {
        if (selected instanceof CavesNotCliffsWorldType) {
            return WorldActivationPolicy.SelectedKind.LEGACY_ALIAS;
        }
        if (selected instanceof CavesNotCliffsWorldTypeWrapper) {
            return WorldActivationPolicy.SelectedKind.WRAPPER;
        }
        if (selected instanceof ICubicWorldType) {
            return WorldActivationPolicy.SelectedKind.OTHER_CUBIC;
        }
        return WorldActivationPolicy.SelectedKind.COMPATIBLE_TWO_DIMENSIONAL;
    }

    private void seedCubicChunksHeight(World world) {

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

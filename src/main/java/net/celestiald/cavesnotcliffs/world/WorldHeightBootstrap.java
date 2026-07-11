package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavebiomes.api.ExtendedChunkAPI;
import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.config.CavesNotCliffsConfig;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerMulti;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Chooses the immutable world-format contract before the Overworld creates its chunk generator.
 */
public final class WorldHeightBootstrap {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void chooseWorldFormat(AttachCapabilitiesEvent<World> event) {
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
        ExtendedChunkAPI.requireRange("Caves Not Cliffs",
                CavesNotCliffsWorldType.MIN_HEIGHT, CavesNotCliffsWorldType.MAX_HEIGHT);
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
        if (CavesNotCliffsWorldTypes.isExternalCubicWorldType(selected)) {
            return WorldActivationPolicy.SelectedKind.OTHER_CUBIC;
        }
        return WorldActivationPolicy.SelectedKind.COMPATIBLE_TWO_DIMENSIONAL;
    }
}

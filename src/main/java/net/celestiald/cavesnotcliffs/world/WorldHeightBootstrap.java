package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavebiomes.api.ExtendedChunkAPI;
import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.config.CavesNotCliffsConfig;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerMulti;
import net.minecraft.world.WorldType;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Chooses the immutable world-format contract before the Overworld creates its chunk generator.
 */
public final class WorldHeightBootstrap {
    /** Core-hook entry point used before {@link WorldServer} constructs its chunk provider. */
    public static void prepareBeforeWorldConstruction(
            WorldInfo worldInfo, ISaveHandler saveHandler) {
        if (worldInfo == null || saveHandler == null) {
            throw new NullPointerException("worldInfo and saveHandler are required");
        }
        CavesNotCliffsWorldTypes.registerWrappers();
        boolean existingWorld = saveHandler.loadWorldInfo() != null;
        boolean enabledForCreation = !existingWorld
                && CavesNotCliffsConfig.WORLD.enableForNewOverworlds;
        applyWorldFormatDecision(worldInfo, existingWorld, enabledForCreation);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void chooseWorldFormat(AttachCapabilitiesEvent<World> event) {
        World world = event.getObject();
        // Every secondary dimension shares the Overworld's WorldInfo, so WorldServerMulti is the
        // essential gate: only the primary Overworld may choose or persist the terrain contract.
        if (world.isRemote || !(world instanceof WorldServer) || world instanceof WorldServerMulti
                || world.provider.getDimension() != 0) {
            return;
        }

        // This event fires after WorldServer has already constructed its chunk generator. World
        // selection belongs in the preconstruction core hook; mutating it here would stamp a save
        // with a generator contract that was not used for the current session.
        CavesNotCliffsWorldData saved = CavesNotCliffsWorldData.read(world.getWorldInfo());
        boolean selected = CavesNotCliffsWorldType.isCavesNotCliffs(world);
        if (saved == null && !selected) {
            return;
        }
        if (saved == null) {
            throw new IllegalStateException("Caves Not Cliffs world type reached world attachment "
                    + "without a preconstructed terrain schema");
        }
        if (!selected) {
            throw new IllegalStateException("Saved Caves Not Cliffs terrain schema "
                    + saved.getTerrainSchema() + " reached world attachment without its world type");
        }

        CavesNotCliffsFiniteWorldType selectedType =
                (CavesNotCliffsFiniteWorldType) world.getWorldType();
        if (selectedType.getTerrainSchema() != saved.getTerrainSchema()) {
            throw new IllegalStateException("Constructed Caves Not Cliffs world type uses terrain "
                    + "schema " + selectedType.getTerrainSchema() + " but level.dat requires "
                    + saved.getTerrainSchema());
        }
        if (!(world.getChunkProvider() instanceof ChunkProviderServer)) {
            throw new IllegalStateException("Caves Not Cliffs Overworld has no server chunk provider");
        }
        validateConstructedGenerator(saved,
                ((ChunkProviderServer) world.getChunkProvider()).chunkGenerator);
        ExtendedChunkAPI.requireRange("Caves Not Cliffs",
                CavesNotCliffsWorldType.MIN_HEIGHT, CavesNotCliffsWorldType.MAX_HEIGHT);
    }

    private static void validateConstructedGenerator(CavesNotCliffsWorldData saved,
            IChunkGenerator generator) {
        if (saved.getTerrainSchema() == CavesNotCliffsWorldData.LEGACY_SCHEMA) {
            if (!(generator instanceof LegacyFiniteChunkGenerator)) {
                throw generatorMismatch(saved, generator, LegacyFiniteChunkGenerator.class);
            }
            return;
        }
        if (saved.getTerrainSchema() != CavesNotCliffsWorldData.CURRENT_SCHEMA) {
            throw new IllegalStateException("Unsupported Caves Not Cliffs terrain schema "
                    + saved.getTerrainSchema());
        }
        if (V118ChunkGenerator.isNativeProfile(saved.getTerrainProfile())) {
            if (!(generator instanceof V118ChunkGenerator)) {
                throw generatorMismatch(saved, generator, V118ChunkGenerator.class);
            }
        } else if (!(generator instanceof DelegatingFiniteChunkGenerator)) {
            throw generatorMismatch(saved, generator, DelegatingFiniteChunkGenerator.class);
        }
    }

    private static IllegalStateException generatorMismatch(CavesNotCliffsWorldData saved,
            IChunkGenerator actual, Class<?> expected) {
        return new IllegalStateException("Caves Not Cliffs terrain schema "
                + saved.getTerrainSchema() + " (" + saved.getTerrainProfile().getSerializedName()
                + ") constructed " + (actual == null ? "null" : actual.getClass().getName())
                + " instead of " + expected.getName());
    }

    static void applyWorldFormatDecision(
            WorldInfo worldInfo, boolean existingWorld, boolean enabled) {
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

    private static void activateCurrentSchema(WorldInfo worldInfo, WorldType selected) {
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

    private static void keepOrRepairCurrentSchema(WorldInfo worldInfo, WorldType selected,
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

    private static CavesNotCliffsWorldTypeWrapper requirePersistedWrapper(
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

    private static void requireLegacyAlias() {
        if (CavesNotCliffs.WORLD_TYPE == null) {
            throw new IllegalStateException("The Caves Not Cliffs schema-1 compatibility alias is unavailable");
        }
    }

    private static WorldActivationPolicy.SelectedKind selectedKind(WorldType selected) {
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

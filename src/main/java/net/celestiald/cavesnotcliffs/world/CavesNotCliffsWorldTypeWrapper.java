package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavebiomes.api.ExtendedChunkAPI;
import net.celestiald.cavebiomes.api.IWrappedWorldType;
import net.celestiald.cavesnotcliffs.config.CavesNotCliffsConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.ChunkGeneratorSettings;
import net.minecraft.world.gen.layer.GenLayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/** Hidden finite-world type which preserves a selected two-dimensional generator as its base. */
public final class CavesNotCliffsWorldTypeWrapper extends WorldType
        implements CavesNotCliffsFiniteWorldType, IWrappedWorldType {
    private static final Logger LOGGER = LogManager.getLogger("CavesNotCliffs/WorldType");
    /** One overlay per world, shared by its chunk generator and biome provider. */
    private static final Map<World, ModdedBiomeOverlay> OVERLAYS =
            new WeakHashMap<World, ModdedBiomeOverlay>();
    private final WorldType baseType;
    private final TerrainProfile terrainProfile;

    CavesNotCliffsWorldTypeWrapper(String name, WorldType baseType, TerrainProfile terrainProfile) {
        super(name);
        this.baseType = baseType;
        this.terrainProfile = terrainProfile;
    }

    public WorldType getBaseType() {
        return baseType;
    }

    @Override
    public WorldType getBaseWorldType() {
        return baseType;
    }

    @Override
    public int getTerrainSchema() {
        return CavesNotCliffsWorldData.CURRENT_SCHEMA;
    }

    @Override
    public TerrainProfile getTerrainProfile() {
        return terrainProfile;
    }

    @Override
    public IChunkGenerator getChunkGenerator(World world, String generatorOptions) {
        CavesNotCliffsWorldData data = CavesNotCliffsWorldData.read(world.getWorldInfo());
        if (data == null) {
            throw new IllegalStateException("Schema-2 Caves Not Cliffs world has no persisted generator data");
        }
        data.validateGeneratorContract(getTerrainSchema(), baseType, terrainProfile);
        String options = data.getGeneratorOptions();
        IChunkGenerator baseGenerator = delegate(world,
                () -> baseType.getChunkGenerator(world, options == null ? "" : options));
        if (world.provider.getDimension() != 0) {
            return baseGenerator;
        }
        ExtendedChunkAPI.requireRange("Caves Not Cliffs",
                CavesNotCliffsWorldType.MIN_HEIGHT, CavesNotCliffsWorldType.MAX_HEIGHT);
        TerrainProfile persistedProfile = data.getTerrainProfile();
        if (V118ChunkGenerator.isNativeProfile(persistedProfile)) {
            return new V118ChunkGenerator(world, persistedProfile, baseGenerator,
                moddedBiomeOverlay(world, data));
        }
        return DelegatingFiniteChunkGenerator.wrap(baseGenerator);
    }

    @Override
    public boolean canBeCreated() {
        return false;
    }

    @Override
    public boolean isCustomizable() {
        return false;
    }

    @Override
    public BiomeProvider getBiomeProvider(World world) {
        // Native-profile worlds lay biomes down with the 1.18 multi-noise climate map, so
        // anything biome-driven outside chunk generation — structure viability checks above
        // all — must consult that same map instead of the untouched vanilla GenLayer chain.
        if (world.provider.getDimension() == 0) {
            CavesNotCliffsWorldData data = CavesNotCliffsWorldData.read(world.getWorldInfo());
            if (data != null && V118ChunkGenerator.isNativeProfile(data.getTerrainProfile())) {
                return new V118BiomeProvider(world.getSeed(),
                    V118ChunkGenerator.nativeProfileFor(data.getTerrainProfile()),
                    V118BiomeMapper.fromRegisteredBiomes(), moddedBiomeOverlay(world, data));
            }
        }
        return delegate(world, () -> baseType.getBiomeProvider(world));
    }

    /**
     * The modded-biome overlay of a native world, in the layout its save records (see
     * {@link ModdedBiomeOverlay}). Both layouts consult the vanilla biome chain of the base
     * world type, with every biome other mods injected through BiomeManager: the legacy one
     * paints from it, the climate-hosted one only discovers biomes in it. Any failure leaves
     * the overlay disabled instead of breaking world creation.
     */
    private ModdedBiomeOverlay moddedBiomeOverlay(World world, CavesNotCliffsWorldData data) {
        synchronized (OVERLAYS) {
            ModdedBiomeOverlay cached = OVERLAYS.get(world);
            if (cached != null) {
                return cached;
            }
        }
        ModdedBiomeOverlay overlay;
        try {
            BiomeProvider chain = delegate(world, () -> baseType.getBiomeProvider(world));
            CavesNotCliffsConfig.ModdedBiomes config = CavesNotCliffsConfig.MODDED_BIOMES;
            boolean climateHosted = data.getModdedBiomeLayout()
                    >= CavesNotCliffsWorldData.CLIMATE_HOSTED_MODDED_BIOME_LAYOUT
                    || config.upgradeExistingWorlds;
            if (climateHosted) {
                int regionSize = config.regionSize
                        * (data.getTerrainProfile() == TerrainProfile.LARGE_BIOMES ? 4 : 1);
                ModdedBiomeOverlay.Sampler sampler = ModdedBiomeOverlay.chainSampler(chain);
                ModdedBiomeCatalog.Settings settings =
                        ModdedBiomeCatalog.parse(config.blacklist, config.hosts);
                double vanillaWeight = config.vanillaWeight;
                overlay = ModdedBiomeOverlay.climateHosted(
                        new ModdedBiomeRegions(world.getSeed(), regionSize),
                        () -> ModdedBiomeCatalog.buildHosts(sampler, settings, vanillaWeight));
            } else {
                overlay = ModdedBiomeOverlay.fromVanillaProvider(chain);
            }
        } catch (RuntimeException failure) {
            LOGGER.warn("Could not sample the base world type's biome chain; modded biomes"
                    + " stay disabled for this world", failure);
            overlay = ModdedBiomeOverlay.disabled();
        }
        synchronized (OVERLAYS) {
            ModdedBiomeOverlay raced = OVERLAYS.get(world);
            if (raced != null) {
                return raced;
            }
            OVERLAYS.put(world, overlay);
        }
        return overlay;
    }

    @Override
    public int getMinimumSpawnHeight(World world) {
        return delegate(world, () -> baseType.getMinimumSpawnHeight(world));
    }

    @Override
    public double getHorizon(World world) {
        return delegate(world, () -> baseType.getHorizon(world));
    }

    @Override
    public double voidFadeMagnitude() {
        return baseType.voidFadeMagnitude();
    }

    @Override
    public boolean handleSlimeSpawnReduction(Random random, World world) {
        return delegate(world, () -> baseType.handleSlimeSpawnReduction(random, world));
    }

    @Override
    public int getSpawnFuzz(WorldServer world, MinecraftServer server) {
        return delegate(world, () -> baseType.getSpawnFuzz(world, server));
    }

    @Override
    public float getCloudHeight() {
        return baseType.getCloudHeight();
    }

    @Override
    public GenLayer getBiomeLayer(long seed, GenLayer parent, ChunkGeneratorSettings settings) {
        return baseType.getBiomeLayer(seed, parent, settings);
    }

    private <T> T delegate(World world, Supplier<T> operation) {
        synchronized (world.getWorldInfo()) {
            WorldType selected = world.getWorldInfo().getTerrainType();
            world.getWorldInfo().setTerrainType(baseType);
            try {
                return operation.get();
            } finally {
                world.getWorldInfo().setTerrainType(selected);
            }
        }
    }
}

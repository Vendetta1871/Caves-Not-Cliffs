package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

/**
 * Decides where biomes that other mods add (e.g. Thaumcraft's Magical Forest) replace the 1.18
 * climate-map projection. Without it a native-profile world would never contain a single
 * modded biome, which breaks biome-driven mod content such as Silverwood trees or grass tints.
 *
 * <p>Two layouts exist, and a world keeps the one recorded in its save:</p>
 * <ul>
 * <li><b>Climate-hosted</b> (new worlds): a modded biome only stands in for the
 * 1.18 biomes it fits ({@link ModdedBiomeHosts}), across large organic regions
 * ({@link ModdedBiomeRegions}). Terrain stays 1.18, and a modded swamp appears where 1.18
 * generated a swamp, so the world keeps the 1.18 layout.</li>
 * <li><b>Legacy chain</b> (worlds created with 2.0.3 and 2.0.4): modded biomes are painted
 * wherever the base world type's 1.12 GenLayer chain puts them, regardless of the 1.18 terrain
 * underneath.</li>
 * </ul>
 *
 * <p>The overlay never crashes the game over another mod's behavior: a failing sampler or
 * catalog is reported once and the overlay silently stops claiming cells.</p>
 */
final class ModdedBiomeOverlay {
    private static final Logger LOGGER = LogManager.getLogger("CavesNotCliffs/ModdedBiomes");
    private static final ModdedBiomeOverlay DISABLED = new ModdedBiomeOverlay(null, null, null);
    private static final V118Biome[] V118_BIOMES = V118Biome.values();

    /** Rectangular biome grid source in the two coordinate scales biome queries use. */
    interface Sampler {
        /** Generation-scale grid: coordinates arrive in quart (1:4) units. */
        Biome[] generationBiomes(int quartX, int quartZ, int width, int height);

        /** Block-scale grid with the vanilla Voronoi zoom applied. */
        Biome[] blockBiomes(int blockX, int blockZ, int width, int length);
    }

    private Sampler sampler;
    private final ModdedBiomeRegions regions;
    private final Supplier<ModdedBiomeHosts> hostSource;
    private volatile ModdedBiomeHosts hosts;
    private boolean warned;

    private ModdedBiomeOverlay(Sampler sampler, ModdedBiomeRegions regions,
            Supplier<ModdedBiomeHosts> hostSource) {
        this.sampler = sampler;
        this.regions = regions;
        this.hostSource = hostSource;
    }

    static ModdedBiomeOverlay disabled() {
        return DISABLED;
    }

    /** Legacy chain layout backed by {@code sampler}. */
    static ModdedBiomeOverlay of(Sampler sampler) {
        if (sampler == null) {
            return DISABLED;
        }
        return new ModdedBiomeOverlay(sampler, null, null);
    }

    /** Legacy chain layout backed by the base world type's biome provider. */
    static ModdedBiomeOverlay fromVanillaProvider(BiomeProvider provider) {
        return of(chainSampler(provider));
    }

    /** Climate-hosted layout; {@code hosts} is resolved once, on first use. */
    static ModdedBiomeOverlay climateHosted(ModdedBiomeRegions regions,
            Supplier<ModdedBiomeHosts> hosts) {
        if (regions == null || hosts == null) {
            throw new NullPointerException("regions and hosts are required");
        }
        return new ModdedBiomeOverlay(null, regions, hosts);
    }

    static Sampler chainSampler(BiomeProvider provider) {
        if (provider == null) {
            return null;
        }
        return new Sampler() {
            @Override
            public Biome[] generationBiomes(int quartX, int quartZ, int width, int height) {
                return provider.getBiomesForGeneration(null, quartX, quartZ, width, height);
            }

            @Override
            public Biome[] blockBiomes(int blockX, int blockZ, int width, int length) {
                return provider.getBiomes(null, blockX, blockZ, width, length, true);
            }
        };
    }

    boolean isEnabled() {
        return sampler != null || regions != null;
    }

    boolean isClimateHosted() {
        return regions != null;
    }

    /**
     * Builds the host table now. Discovery samples the vanilla layer chain, whose IntCache is
     * not thread-safe, so the generator calls this on the server thread before any other
     * thread can ask for biomes.
     */
    void prepare() {
        if (regions != null) {
            hosts();
        }
    }

    /**
     * Generation-scale grid overlay; {@code base} cells keep the 1.18 projection by default.
     * {@code hostGrid} holds the 1.18 biome of each cell (ignored by the legacy layout).
     */
    Biome[] overlayForGeneration(Biome[] base, V118Biome[] hostGrid, int quartX, int quartZ,
            int width, int height) {
        if (regions != null) {
            if (hostGrid == null || !hasAnyCandidates()) {
                return base;
            }
            for (int localZ = 0; localZ < height; ++localZ) {
                for (int localX = 0; localX < width; ++localX) {
                    int index = localX + localZ * width;
                    if (index >= base.length || index >= hostGrid.length) {
                        continue;
                    }
                    Biome claim = claim(hostGrid[index], ((quartX + localX) << 2) + 2,
                            ((quartZ + localZ) << 2) + 2);
                    if (claim != null) {
                        base[index] = claim;
                    }
                }
            }
            return base;
        }
        Biome[] vanilla = sampleGeneration(quartX, quartZ, width, height);
        if (vanilla == null) {
            return base;
        }
        for (int index = 0; index < base.length && index < vanilla.length; ++index) {
            if (isModded(vanilla[index])) {
                base[index] = vanilla[index];
            }
        }
        return base;
    }

    /** Block-scale grid overlay; {@code base} cells keep the 1.18 projection by default. */
    Biome[] overlayBlock(Biome[] base, V118Biome[] hostGrid, int blockX, int blockZ,
            int width, int length) {
        Biome[] modded = moddedBlockBiomes(hostGrid, blockX, blockZ, width, length);
        if (modded == null) {
            return base;
        }
        for (int index = 0; index < base.length && index < modded.length; ++index) {
            if (modded[index] != null) {
                base[index] = modded[index];
            }
        }
        return base;
    }

    /**
     * Block-scale grid holding the modded biome for each claimed cell, or null per cell where
     * the 1.18 projection stays. Null array when disabled, failing or nothing can be claimed.
     */
    Biome[] moddedBlockBiomes(V118Biome[] hostGrid, int blockX, int blockZ, int width,
            int length) {
        if (regions != null) {
            if (hostGrid == null || !hasAnyCandidates()) {
                return null;
            }
            Biome[] claims = new Biome[width * length];
            boolean any = false;
            for (int localZ = 0; localZ < length; ++localZ) {
                for (int localX = 0; localX < width; ++localX) {
                    int index = localX + localZ * width;
                    if (index >= hostGrid.length) {
                        continue;
                    }
                    Biome claim = claim(hostGrid[index], blockX + localX, blockZ + localZ);
                    claims[index] = claim;
                    any |= claim != null;
                }
            }
            return any ? claims : null;
        }
        Biome[] vanilla = sampleBlock(blockX, blockZ, width, length);
        if (vanilla == null) {
            return null;
        }
        for (int index = 0; index < vanilla.length; ++index) {
            if (!isModded(vanilla[index])) {
                vanilla[index] = null;
            }
        }
        return vanilla;
    }

    /**
     * The modded biome claiming a single block column, or null where the 1.18 projection
     * stays. {@code host} is the 1.18 biome there (ignored by the legacy layout).
     */
    Biome moddedBiomeAt(V118Biome host, int blockX, int blockZ) {
        if (regions != null) {
            return claim(host, blockX, blockZ);
        }
        Biome[] modded = moddedBlockBiomes(null, blockX, blockZ, 1, 1);
        return modded == null || modded.length == 0 ? null : modded[0];
    }

    /** Climate-hosted claim for one column; always null for the legacy layout. */
    Biome claim(V118Biome host, int blockX, int blockZ) {
        if (regions == null || host == null) {
            return null;
        }
        ModdedBiomeHosts table = hosts();
        if (!table.hasCandidates(host)) {
            return null;
        }
        long region = regions.regionAt(blockX, blockZ);
        return table.pick(host, ModdedBiomeRegions.roll(region, host.ordinal()));
    }

    /** Surface host of a column from a terrain column's stored biome id. */
    static V118Biome hostFor(int v118BiomeId) {
        return V118_BIOMES[v118BiomeId];
    }

    private boolean hasAnyCandidates() {
        return !hosts().isEmpty();
    }

    private ModdedBiomeHosts hosts() {
        ModdedBiomeHosts table = hosts;
        if (table != null) {
            return table;
        }
        synchronized (this) {
            if (hosts == null) {
                ModdedBiomeHosts built;
                try {
                    built = hostSource.get();
                } catch (RuntimeException failure) {
                    LOGGER.warn("Could not build the modded biome host table; modded biomes"
                            + " stay disabled for this world", failure);
                    built = null;
                }
                hosts = built == null ? ModdedBiomeHosts.empty() : built;
            }
            return hosts;
        }
    }

    /**
     * Whether the biome comes from another mod. Caves Not Cliffs' own biomes (meadow, grove,
     * the peaks and slopes, lush/dripstone caves) are part of the 1.18 projection itself, not
     * overlay claims: treating them as modded made every mountain column bypass the resolver's
     * climate projection and its cache.
     */
    static boolean isModded(Biome biome) {
        if (biome == null || biome.getRegistryName() == null) {
            return false;
        }
        String domain = biome.getRegistryName().getResourceDomain();
        return !"minecraft".equals(domain) && !CavesNotCliffs.MODID.equals(domain);
    }

    private Biome[] sampleGeneration(int quartX, int quartZ, int width, int height) {
        if (sampler == null) {
            return null;
        }
        try {
            return sampler.generationBiomes(quartX, quartZ, width, height);
        } catch (RuntimeException failure) {
            return disable(failure);
        }
    }

    private Biome[] sampleBlock(int blockX, int blockZ, int width, int length) {
        if (sampler == null) {
            return null;
        }
        try {
            return sampler.blockBiomes(blockX, blockZ, width, length);
        } catch (RuntimeException failure) {
            return disable(failure);
        }
    }

    private Biome[] disable(RuntimeException failure) {
        if (!warned) {
            LOGGER.warn("Modded-biome overlay sampler failed; modded biomes stay disabled"
                    + " for this world", failure);
            warned = true;
        }
        sampler = null;
        return null;
    }
}

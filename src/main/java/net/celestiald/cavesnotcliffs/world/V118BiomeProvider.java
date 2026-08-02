package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.worldgen.v118.OverworldBiomeBuilder;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118BiomeManager;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118ClimateSampler;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118NoiseRouter;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118NoiseRouterData;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118NoiseSettings;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;

import java.util.List;
import java.util.Random;

/**
 * Biome provider backed by the Java 1.18.2 multi-noise climate map instead of the vanilla
 * GenLayer chain. The native 1.18 chunk generator lays terrain down by that map, while
 * vanilla structure viability checks (villages, ocean monuments, scattered features, the
 * woodland mansion) and spawn search ask this provider; without the rerouting the two
 * layouts disagree and villages pass their biome check in the middle of a 1.18 ocean.
 *
 * <p>All queries sample the sea-level band (block y 64). In the 1.18 biome table every
 * surface biome is registered at both depth extremes, so the surface projection is
 * height-independent there; the only depth-gated entries are the lush and dripstone cave
 * biomes, which may legitimately win in the same humid lowland spots where the real
 * surface-height projection shows them too.</p>
 */
public final class V118BiomeProvider extends BiomeProvider {
    private static final int SURFACE_SAMPLE_Y = 64;
    private static final int SURFACE_SAMPLE_QUART_Y = SURFACE_SAMPLE_Y >> 2;

    private final V118ClimateSampler climateSampler;
    private final V118BiomeManager biomeManager;
    private final V118BiomeMapper biomes;

    public V118BiomeProvider(long seed, V118NoiseRouterData.Profile profile) {
        this(seed, profile, V118BiomeMapper.fromRegisteredBiomes());
    }

    /** Test seam: resolves the projection table without the live mod biome registry. */
    V118BiomeProvider(long seed, V118NoiseRouterData.Profile profile, V118BiomeMapper biomes) {
        if (profile == null) {
            throw new NullPointerException("profile");
        }
        if (biomes == null) {
            throw new NullPointerException("biomes");
        }
        V118NoiseSettings settings = V118NoiseSettings.overworld(profile.amplified());
        V118NoiseRouter router = V118NoiseRouterData.create(seed, profile);
        OverworldBiomeBuilder biomeTable = new OverworldBiomeBuilder();
        climateSampler = new V118ClimateSampler(router, settings, biomeTable);
        biomeManager = new V118BiomeManager(climateSampler::resolveQuart, seed);
        this.biomes = biomes;
    }

    /** Generation-scale grid: coordinates arrive in quart (1:4) units, no Voronoi zoom. */
    @Override
    public Biome[] getBiomesForGeneration(Biome[] reuse, int x, int z, int width, int height) {
        if (reuse == null || reuse.length < width * height) {
            reuse = new Biome[width * height];
        }
        for (int localZ = 0; localZ < height; ++localZ) {
            for (int localX = 0; localX < width; ++localX) {
                reuse[localX + localZ * width] = biomes.biomeFor(climateSampler.resolveQuart(
                    x + localX, SURFACE_SAMPLE_QUART_Y, z + localZ));
            }
        }
        return reuse;
    }

    /** Block-scale grid: applies the same Voronoi zoom the chunk biome array is written with. */
    @Override
    public Biome[] getBiomes(Biome[] reuse, int x, int z, int width, int length,
            boolean cacheFlag) {
        if (reuse == null || reuse.length < width * length) {
            reuse = new Biome[width * length];
        }
        for (int localZ = 0; localZ < length; ++localZ) {
            for (int localX = 0; localX < width; ++localX) {
                reuse[localX + localZ * width] = biomes.biomeFor(biomeManager.getBiome(
                    x + localX, SURFACE_SAMPLE_Y, z + localZ));
            }
        }
        return reuse;
    }

    @Override
    public boolean areBiomesViable(int x, int z, int radius, List<Biome> allowed) {
        int minQuartX = x - radius >> 2;
        int minQuartZ = z - radius >> 2;
        int maxQuartX = x + radius >> 2;
        int maxQuartZ = z + radius >> 2;
        for (int quartZ = minQuartZ; quartZ <= maxQuartZ; ++quartZ) {
            for (int quartX = minQuartX; quartX <= maxQuartX; ++quartX) {
                Biome biome = biomes.biomeFor(climateSampler.resolveQuart(
                    quartX, SURFACE_SAMPLE_QUART_Y, quartZ));
                if (!allowed.contains(biome)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public BlockPos findBiomePosition(int x, int z, int range, List<Biome> biomeList,
            Random random) {
        int minQuartX = x - range >> 2;
        int minQuartZ = z - range >> 2;
        int maxQuartX = x + range >> 2;
        int maxQuartZ = z + range >> 2;
        int width = maxQuartX - minQuartX + 1;
        int height = maxQuartZ - minQuartZ + 1;
        BlockPos found = null;
        int matches = 0;
        for (int index = 0; index < width * height; ++index) {
            int quartX = minQuartX + index % width;
            int quartZ = minQuartZ + index / width;
            Biome biome = biomes.biomeFor(climateSampler.resolveQuart(
                quartX, SURFACE_SAMPLE_QUART_Y, quartZ));
            if (biomeList.contains(biome)) {
                if (found == null || random.nextInt(matches + 1) == 0) {
                    found = new BlockPos(quartX << 2, 0, quartZ << 2);
                }
                ++matches;
            }
        }
        return found;
    }
}

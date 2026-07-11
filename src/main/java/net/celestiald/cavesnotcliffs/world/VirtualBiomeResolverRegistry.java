package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.worldgen.v118.TerrainColumn;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118TerrainColumnGenerator;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.Map;
import java.util.WeakHashMap;

/** Client-side deterministic resolver installations keyed by the active World instance. */
public final class VirtualBiomeResolverRegistry {
    private static final Map<World, Resolver> RESOLVERS = new WeakHashMap<World, Resolver>();

    private VirtualBiomeResolverRegistry() {}

    public static void install(World world, long seed, TerrainProfile profile) {
        if (world == null || !V118CubicChunksGenerator.isNativeProfile(profile)) {
            return;
        }
        Resolver resolver = new Resolver(
                new V118TerrainColumnGenerator(seed,
                        V118CubicChunksGenerator.nativeProfileFor(profile)),
                V118BiomeMapper.fromRegisteredBiomes());
        synchronized (RESOLVERS) {
            RESOLVERS.put(world, resolver);
        }
    }

    public static void remove(World world) {
        synchronized (RESOLVERS) {
            RESOLVERS.remove(world);
        }
    }

    public static Biome resolve(World world, int x, int y, int z, Biome base) {
        V118CubicChunksGenerator server = V118CubicChunksGenerator.forWorld(world);
        if (server != null && V118CubicChunksGenerator.hasVirtualBiomeY(y)) {
            return server.getRegisteredVirtualBiome(x, y, z);
        }
        Resolver resolver;
        synchronized (RESOLVERS) {
            resolver = RESOLVERS.get(world);
        }
        return resolver == null || y < TerrainColumn.MIN_Y || y > TerrainColumn.MAX_Y
                ? base : resolver.resolve(x, y, z);
    }

    private static final class Resolver {
        private final V118TerrainColumnGenerator columns;
        private final V118BiomeMapper biomes;

        private Resolver(V118TerrainColumnGenerator columns, V118BiomeMapper biomes) {
            this.columns = columns;
            this.biomes = biomes;
        }

        private synchronized Biome resolve(int x, int y, int z) {
            V118Biome biome = columns.biomeAt(x, y, z);
            return biomes.biomeFor(biome);
        }
    }
}

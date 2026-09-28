package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.worldgen.v118.TerrainColumn;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118TerrainColumnGenerator;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/** Client-side deterministic resolver installations keyed by the active World instance. */
public final class VirtualBiomeResolverRegistry {
    private static final int BLOCK_CACHE_SIZE = 2048;
    private static final int BLOCK_CACHE_MASK = BLOCK_CACHE_SIZE - 1;
    private static final Map<World, Resolver> RESOLVERS = new WeakHashMap<World, Resolver>();
    private static volatile ResolverEntry lastResolver = new ResolverEntry(null, null);

    private VirtualBiomeResolverRegistry() {}

    public static void install(World world, long seed, TerrainProfile profile) {
        if (world == null || !V118ChunkGenerator.isNativeProfile(profile)) {
            return;
        }
        synchronized (RESOLVERS) {
            // The contract is resent on every respawn; a same-dimension respawn keeps its
            // WorldClient, and rebuilding the noise router for it would only drop the caches.
            Resolver installed = RESOLVERS.get(world);
            if (installed != null && installed.matches(seed, profile)) {
                return;
            }
        }
        Resolver resolver = new Resolver(seed, profile,
                new V118TerrainColumnGenerator(seed,
                        V118ChunkGenerator.nativeProfileFor(profile)),
                V118BiomeMapper.fromRegisteredBiomes());
        synchronized (RESOLVERS) {
            RESOLVERS.put(world, resolver);
            lastResolver = new ResolverEntry(world, resolver);
        }
    }

    public static void remove(World world) {
        synchronized (RESOLVERS) {
            RESOLVERS.remove(world);
            if (lastResolver.world.get() == world) {
                lastResolver = new ResolverEntry(world, null);
            }
        }
    }

    public static boolean hasResolver(World world) {
        if (world != null && !world.isRemote && V118ChunkGenerator.forWorld(world) != null) {
            return true;
        }
        return resolverFor(world) != null;
    }

    public static Biome resolve(World world, int x, int y, int z, Biome base) {
        V118ChunkGenerator server = world != null && !world.isRemote
                ? V118ChunkGenerator.forWorld(world) : null;
        if (server != null && V118ChunkGenerator.hasVirtualBiomeY(y)) {
            return resolveWithOverlay(server.getVirtualBiome(x, y, z), base,
                    server.getRegisteredVirtualBiome(x, y, z));
        }
        Resolver resolver = resolverFor(world);
        return resolver == null || y < TerrainColumn.MIN_Y || y > TerrainColumn.MAX_Y
                ? base : resolver.resolve(x, y, z, base);
    }

    /**
     * The chunk biome array carries modded overlay biomes (e.g. Thaumcraft's Magical
     * Forest) which the 1.18 climate map cannot express. Where the virtual biome is an
     * ordinary surface biome, the overlay claim wins; underground cave biomes (lush /
     * dripstone caves) still override the 2D array as designed.
     */
    static Biome resolveWithOverlay(V118Biome virtual, Biome base, Biome projected) {
        if (ModdedBiomeOverlay.isModded(base) && isSurfaceVirtualBiome(virtual)) {
            return base;
        }
        return projected;
    }

    private static boolean isSurfaceVirtualBiome(V118Biome biome) {
        return biome != V118Biome.LUSH_CAVES && biome != V118Biome.DRIPSTONE_CAVES;
    }

    private static Resolver resolverFor(World world) {
        if (world == null) {
            return null;
        }
        ResolverEntry cached = lastResolver;
        if (cached.world.get() == world) {
            return cached.resolver.get();
        }
        synchronized (RESOLVERS) {
            Resolver resolver = RESOLVERS.get(world);
            lastResolver = new ResolverEntry(world, resolver);
            return resolver;
        }
    }

    private static final class ResolverEntry {
        private final WeakReference<World> world;
        private final WeakReference<Resolver> resolver;

        private ResolverEntry(World world, Resolver resolver) {
            this.world = new WeakReference<World>(world);
            this.resolver = new WeakReference<Resolver>(resolver);
        }
    }

    private static final class Resolver {
        private final long seed;
        private final TerrainProfile profile;
        private final V118TerrainColumnGenerator columns;
        private final V118BiomeMapper biomes;
        private final int[] cachedX = new int[BLOCK_CACHE_SIZE];
        private final int[] cachedY = new int[BLOCK_CACHE_SIZE];
        private final int[] cachedZ = new int[BLOCK_CACHE_SIZE];
        private final Biome[] cachedBiomes = new Biome[BLOCK_CACHE_SIZE];

        private Resolver(long seed, TerrainProfile profile,
                V118TerrainColumnGenerator columns, V118BiomeMapper biomes) {
            this.seed = seed;
            this.profile = profile;
            this.columns = columns;
            this.biomes = biomes;
        }

        private boolean matches(long seed, TerrainProfile profile) {
            return this.seed == seed && this.profile == profile;
        }

        private synchronized Biome resolve(int x, int y, int z, Biome base) {
            // Overlay claims depend on the base biome, not just the coordinates, so
            // they neither read nor write the coordinate cache.
            if (ModdedBiomeOverlay.isModded(base)) {
                V118Biome virtual = columns.biomeAt(x, y, z);
                return resolveWithOverlay(virtual, base, biomes.biomeFor(virtual));
            }
            int cacheIndex = cacheIndex(x, y, z);
            Biome cached = cachedBiomes[cacheIndex];
            if (cached != null && cachedX[cacheIndex] == x && cachedY[cacheIndex] == y
                    && cachedZ[cacheIndex] == z) {
                return cached;
            }
            V118Biome biome = columns.biomeAt(x, y, z);
            Biome resolved = biomes.biomeFor(biome);
            cachedX[cacheIndex] = x;
            cachedY[cacheIndex] = y;
            cachedZ[cacheIndex] = z;
            cachedBiomes[cacheIndex] = resolved;
            return resolved;
        }

        private static int cacheIndex(int x, int y, int z) {
            int hash = x * 0x9E3779B9;
            hash ^= Integer.rotateLeft(y, 11);
            hash *= 0x85EBCA6B;
            hash ^= Integer.rotateLeft(z, 17);
            hash ^= hash >>> 16;
            return hash & BLOCK_CACHE_MASK;
        }
    }
}

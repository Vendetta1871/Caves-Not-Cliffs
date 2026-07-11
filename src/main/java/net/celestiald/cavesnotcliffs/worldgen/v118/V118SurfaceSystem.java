package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Java 8 port of Java 1.18.2's seeded Overworld {@code SurfaceSystem}.
 *
 * <p>The caller owns the raw terrain and climate data through {@link SurfaceAccess}.  This keeps
 * the numeric implementation independent of both 1.12 block states and CubicChunks while
 * preserving vanilla's 16-by-16 chunk scan order.</p>
 */
public final class V118SurfaceSystem {
    public static final int OVERWORLD_MIN_Y = -64;
    public static final int OVERWORLD_MAX_Y = 320;
    public static final int OVERWORLD_SEA_LEVEL = 63;

    private static final int CHUNK_SIZE = 16;
    private static final int CLAY_BAND_COUNT = 192;

    private final V118Material defaultBlock;
    private final int seaLevel;
    private final PositionalRandomFactory randomFactory;
    private final V118Material[] clayBands;
    private final NormalNoise clayBandsOffsetNoise;
    private final NormalNoise badlandsPillarNoise;
    private final NormalNoise badlandsPillarRoofNoise;
    private final NormalNoise badlandsSurfaceNoise;
    private final NormalNoise icebergPillarNoise;
    private final NormalNoise icebergPillarRoofNoise;
    private final NormalNoise icebergSurfaceNoise;
    private final NormalNoise surfaceNoise;
    private final NormalNoise surfaceSecondaryNoise;
    private final Map<String, NormalNoise> noises = new HashMap<String, NormalNoise>();
    private final Map<String, PositionalRandomFactory> positionalRandoms =
        new HashMap<String, PositionalRandomFactory>();

    public V118SurfaceSystem(long seed) {
        this(seed, V118Material.STONE, OVERWORLD_SEA_LEVEL);
    }

    public V118SurfaceSystem(long seed, V118Material defaultBlock, int seaLevel) {
        if (defaultBlock == null) {
            throw new NullPointerException("defaultBlock");
        }
        this.defaultBlock = defaultBlock;
        this.seaLevel = seaLevel;
        randomFactory = new XoroshiroRandomSource(seed).forkPositional();
        clayBandsOffsetNoise = instantiate("clay_bands_offset");
        clayBands = generateBands(randomFactory.fromHashOf("minecraft:clay_bands"));
        surfaceNoise = instantiate("surface");
        surfaceSecondaryNoise = instantiate("surface_secondary");
        badlandsPillarNoise = instantiate("badlands_pillar");
        badlandsPillarRoofNoise = instantiate("badlands_pillar_roof");
        badlandsSurfaceNoise = instantiate("badlands_surface");
        icebergPillarNoise = instantiate("iceberg_pillar");
        icebergPillarRoofNoise = instantiate("iceberg_pillar_roof");
        icebergSurfaceNoise = instantiate("iceberg_surface");
    }

    /** Applies the canonical 1.18.2 Overworld rules to one aligned 16-by-16 chunk. */
    public void buildSurface(SurfaceAccess access, int chunkX, int chunkZ) {
        buildSurface(access, chunkX, chunkZ, V118SurfaceRuleData.overworld());
    }

    /** Applies a supplied rule tree; primarily useful for independent oracle testing. */
    public void buildSurface(SurfaceAccess access, int chunkX, int chunkZ,
            V118SurfaceRules.RuleSource rule) {
        if (access == null) {
            throw new NullPointerException("access");
        }
        if (rule == null) {
            throw new NullPointerException("rule");
        }
        int chunkMinX = chunkX << 4;
        int chunkMinZ = chunkZ << 4;
        V118SurfaceRules.Context context = new V118SurfaceRules.Context(this, access,
            chunkMinX, chunkMinZ);
        for (int localX = 0; localX < CHUNK_SIZE; ++localX) {
            for (int localZ = 0; localZ < CHUNK_SIZE; ++localZ) {
                int blockX = chunkMinX + localX;
                int blockZ = chunkMinZ + localZ;
                int firstAvailable = access.worldSurfaceHeight(blockX, blockZ) + 1;
                V118Biome surfaceBiome = access.biomeAt(blockX,
                    access.useLegacyRandomSource() ? 0 : firstAvailable, blockZ);
                if (surfaceBiome == V118Biome.ERODED_BADLANDS) {
                    erodedBadlandsExtension(access, blockX, blockZ, firstAvailable);
                }

                int scanTop = access.worldSurfaceHeight(blockX, blockZ) + 1;
                context.updateXZ(blockX, blockZ);
                int stoneDepthAbove = 0;
                int fluidHeight = Integer.MIN_VALUE;
                int stoneBottom = Integer.MAX_VALUE;
                int minY = access.minBuildHeight();
                for (int blockY = scanTop; blockY >= minY; --blockY) {
                    V118Material current = access.getBlock(blockX, blockY, blockZ);
                    if (current == V118Material.AIR) {
                        stoneDepthAbove = 0;
                        fluidHeight = Integer.MIN_VALUE;
                        continue;
                    }
                    if (isFluid(current)) {
                        if (fluidHeight == Integer.MIN_VALUE) {
                            fluidHeight = blockY + 1;
                        }
                        continue;
                    }
                    if (stoneBottom >= blockY) {
                        stoneBottom = Integer.MIN_VALUE / 2;
                        for (int belowY = blockY - 1; belowY >= minY - 1; --belowY) {
                            V118Material below = belowY < minY
                                ? V118Material.AIR : access.getBlock(blockX, belowY, blockZ);
                            if (isStone(below)) {
                                continue;
                            }
                            stoneBottom = belowY + 1;
                            break;
                        }
                    }
                    int stoneDepthBelow = blockY - stoneBottom + 1;
                    context.updateY(++stoneDepthAbove, stoneDepthBelow, fluidHeight,
                        blockX, blockY, blockZ);
                    if (current == defaultBlock) {
                        V118Material replacement = rule.tryApply(context);
                        if (replacement != null) {
                            access.setBlock(blockX, blockY, blockZ, replacement);
                        }
                    }
                }

                if (surfaceBiome == V118Biome.FROZEN_OCEAN
                        || surfaceBiome == V118Biome.DEEP_FROZEN_OCEAN) {
                    frozenOceanExtension(context.minimumSurfaceLevel(), surfaceBiome, access,
                        blockX, blockZ, firstAvailable);
                }
            }
        }
    }

    int getSurfaceDepth(int blockX, int blockZ) {
        double value = surfaceNoise.getValue(blockX, 0.0D, blockZ);
        return (int) (value * 2.75D + 3.0D
            + randomFactory.at(blockX, 0, blockZ).nextDouble() * 0.25D);
    }

    double getSurfaceSecondary(int blockX, int blockZ) {
        return surfaceSecondaryNoise.getValue(blockX, 0.0D, blockZ);
    }

    NormalNoise getOrCreateNoise(String name) {
        NormalNoise noise = noises.get(name);
        if (noise == null) {
            noise = instantiate(name);
            noises.put(name, noise);
        }
        return noise;
    }

    PositionalRandomFactory getOrCreateRandomFactory(String name) {
        PositionalRandomFactory factory = positionalRandoms.get(name);
        if (factory == null) {
            RandomSource hashed = randomFactory.fromHashOf(resourceLocation(name));
            factory = hashed.forkPositional();
            positionalRandoms.put(name, factory);
        }
        return factory;
    }

    V118Material getBand(int blockX, int blockY, int blockZ) {
        int offset = (int) Math.round(clayBandsOffsetNoise.getValue(blockX, 0.0D, blockZ) * 4.0D);
        return clayBands[(blockY + offset + clayBands.length) % clayBands.length];
    }

    private NormalNoise instantiate(String name) {
        return V118NoiseParameters.instantiate(name, randomFactory);
    }

    private void erodedBadlandsExtension(SurfaceAccess access, int blockX, int blockZ,
            int firstAvailable) {
        double pillar = Math.min(Math.abs(badlandsSurfaceNoise.getValue(blockX, 0.0D, blockZ)
                * 8.25D),
            badlandsPillarNoise.getValue(blockX * 0.2D, 0.0D, blockZ * 0.2D) * 15.0D);
        if (pillar <= 0.0D) {
            return;
        }
        double roof = Math.abs(badlandsPillarRoofNoise.getValue(blockX * 0.75D, 0.0D,
            blockZ * 0.75D) * 1.5D);
        int pillarTop = floor(64.0D + Math.min(pillar * pillar * 2.5D,
            Math.ceil(roof * 50.0D) + 24.0D));
        if (firstAvailable > pillarTop) {
            return;
        }
        int minY = access.minBuildHeight();
        int y = pillarTop;
        for (; y >= minY && access.getBlock(blockX, y, blockZ) != defaultBlock; --y) {
            if (access.getBlock(blockX, y, blockZ) == V118Material.WATER) {
                return;
            }
        }
        for (y = pillarTop; y >= minY
                && access.getBlock(blockX, y, blockZ) == V118Material.AIR; --y) {
            access.setBlock(blockX, y, blockZ, defaultBlock);
        }
    }

    private void frozenOceanExtension(int minimumSurfaceLevel, V118Biome biome,
            SurfaceAccess access, int blockX, int blockZ, int firstAvailable) {
        double pillar = Math.min(Math.abs(icebergSurfaceNoise.getValue(blockX, 0.0D, blockZ)
                * 8.25D),
            icebergPillarNoise.getValue(blockX * 1.28D, 0.0D, blockZ * 1.28D) * 15.0D);
        if (pillar <= 1.8D) {
            return;
        }
        double roof = Math.abs(icebergPillarRoofNoise.getValue(blockX * 1.17D, 0.0D,
            blockZ * 1.17D) * 1.5D);
        double icebergHeight = Math.min(pillar * pillar * 1.2D,
            Math.ceil(roof * 40.0D) + 14.0D);
        if (access.shouldMeltFrozenOceanIcebergSlightly(biome, blockX, 63, blockZ)) {
            icebergHeight -= 2.0D;
        }
        double icebergBottom;
        if (icebergHeight > 2.0D) {
            icebergBottom = seaLevel - icebergHeight - 7.0D;
            icebergHeight += seaLevel;
        } else {
            icebergHeight = 0.0D;
            icebergBottom = 0.0D;
        }
        double icebergTop = icebergHeight;
        RandomSource random = randomFactory.at(blockX, 0, blockZ);
        int snowCap = 2 + random.nextInt(4);
        int snowMinimumY = seaLevel + 18 + random.nextInt(10);
        int snowCount = 0;
        for (int y = Math.max(firstAvailable, (int) icebergTop + 1);
                y >= minimumSurfaceLevel; --y) {
            V118Material current = access.getBlock(blockX, y, blockZ);
            boolean placeInAir = current == V118Material.AIR && y < (int) icebergTop
                && random.nextDouble() > 0.01D;
            boolean placeInWater = current == V118Material.WATER && y > (int) icebergBottom
                && y < seaLevel && icebergBottom != 0.0D && random.nextDouble() > 0.15D;
            if (!placeInAir && !placeInWater) {
                continue;
            }
            if (snowCount <= snowCap && y > snowMinimumY) {
                access.setBlock(blockX, y, blockZ, V118Material.SNOW_BLOCK);
                ++snowCount;
            } else {
                access.setBlock(blockX, y, blockZ, V118Material.PACKED_ICE);
            }
        }
    }

    private static V118Material[] generateBands(RandomSource random) {
        V118Material[] bands = new V118Material[CLAY_BAND_COUNT];
        Arrays.fill(bands, V118Material.TERRACOTTA);
        for (int index = 0; index < bands.length; ++index) {
            index += random.nextInt(5) + 1;
            if (index < bands.length) {
                bands[index] = V118Material.ORANGE_TERRACOTTA;
            }
        }
        makeBands(random, bands, 1, V118Material.YELLOW_TERRACOTTA);
        makeBands(random, bands, 2, V118Material.BROWN_TERRACOTTA);
        makeBands(random, bands, 1, V118Material.RED_TERRACOTTA);
        int whiteBandCount = random.nextIntBetweenInclusive(9, 15);
        int count = 0;
        for (int index = 0; count < whiteBandCount && index < bands.length;
                ++count, index += random.nextInt(16) + 4) {
            bands[index] = V118Material.WHITE_TERRACOTTA;
            if (index - 1 > 0 && random.nextBoolean()) {
                bands[index - 1] = V118Material.LIGHT_GRAY_TERRACOTTA;
            }
            if (index + 1 < bands.length && random.nextBoolean()) {
                bands[index + 1] = V118Material.LIGHT_GRAY_TERRACOTTA;
            }
        }
        return bands;
    }

    private static void makeBands(RandomSource random, V118Material[] bands, int minimumWidth,
            V118Material material) {
        int count = random.nextIntBetweenInclusive(6, 15);
        for (int band = 0; band < count; ++band) {
            int width = minimumWidth + random.nextInt(3);
            int start = random.nextInt(bands.length);
            for (int offset = 0; start + offset < bands.length && offset < width; ++offset) {
                bands[start + offset] = material;
            }
        }
    }

    private static boolean isFluid(V118Material material) {
        return material == V118Material.WATER || material == V118Material.LAVA;
    }

    private static boolean isStone(V118Material material) {
        return material != V118Material.AIR && !isFluid(material);
    }

    private static String resourceLocation(String name) {
        return name.indexOf(':') >= 0 ? name : "minecraft:" + name;
    }

    private static int floor(double value) {
        int truncated = (int) value;
        return value < truncated ? truncated - 1 : truncated;
    }

    /**
     * Raw chunk contract used by the isolated surface pass.
     * {@link #worldSurfaceHeight} is the highest occupied Y (vanilla ChunkAccess#getHeight), not
     * the first free Y.
     */
    public interface SurfaceAccess {
        int minBuildHeight();

        int maxBuildHeight();

        V118Material getBlock(int blockX, int blockY, int blockZ);

        void setBlock(int blockX, int blockY, int blockZ, V118Material material);

        int worldSurfaceHeight(int blockX, int blockZ);

        int preliminarySurfaceLevel(int blockX, int blockZ);

        V118Biome biomeAt(int blockX, int blockY, int blockZ);

        boolean coldEnoughToSnow(V118Biome biome, int blockX, int blockY, int blockZ);

        boolean shouldMeltFrozenOceanIcebergSlightly(V118Biome biome, int blockX, int blockY,
            int blockZ);

        /** Mirrors NoiseBasedChunkGenerator's legacy-random-source biome lookup switch. */
        default boolean useLegacyRandomSource() {
            return false;
        }
    }
}

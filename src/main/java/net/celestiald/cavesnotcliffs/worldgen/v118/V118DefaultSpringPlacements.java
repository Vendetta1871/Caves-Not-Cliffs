package net.celestiald.cavesnotcliffs.worldgen.v118;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Exact Java 1.18.2 default Overworld water- and lava-spring placed features. */
public final class V118DefaultSpringPlacements {
    public static final int FLUID_SPRINGS_STEP = 8;
    public static final int SPRING_WATER_INDEX = 0;
    public static final int SPRING_LAVA_INDEX = 1;
    public static final int WATER_ATTEMPTS = 25;
    public static final int LAVA_ATTEMPTS = 20;
    public static final int WATER_MAX_Y = 192;

    private static final Set<V118Biome> DEFAULT_SPRING_BIOMES =
        Collections.unmodifiableSet(EnumSet.allOf(V118Biome.class));

    private V118DefaultSpringPlacements() {
    }

    public static DecorationResult decorate(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, Set<V118Biome> regionBiomes) {
        requireArguments(world, regionBiomes);
        DecorationResult result = new DecorationResult();
        if (!appearsIn(regionBiomes)) {
            return result;
        }

        // MultiNoiseBiomeSource's global step ordering is water index 0, then lava index 1.
        decorateFluid(world, worldSeed, chunkX, chunkZ, SpringFluid.WATER, result);
        decorateFluid(world, worldSeed, chunkX, chunkZ, SpringFluid.LAVA, result);
        return result;
    }

    static List<BlockPos> candidates(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, SpringFluid fluid) {
        if (world == null || fluid == null) {
            throw new NullPointerException("world and fluid are required");
        }
        int globalIndex = fluid == SpringFluid.WATER
            ? SPRING_WATER_INDEX : SPRING_LAVA_INDEX;
        int count = fluid == SpringFluid.WATER ? WATER_ATTEMPTS : LAVA_ATTEMPTS;
        V118WorldgenRandom random = featureRandom(worldSeed, chunkX, chunkZ, globalIndex);
        List<BlockPos> result = new ArrayList<BlockPos>(count);
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        for (int attempt = 0; attempt < count; ++attempt) {
            int x = originX + random.nextInt(16);
            int z = originZ + random.nextInt(16);
            int y = fluid == SpringFluid.WATER
                ? betweenInclusive(random, world.minBuildHeight(), WATER_MAX_Y)
                : veryBiasedToBottom(random, world.minBuildHeight(),
                    world.maxBuildHeight() - 1 - 8, 8);
            result.add(new BlockPos(x, y, z));
        }
        return Collections.unmodifiableList(result);
    }

    static boolean placeSpring(WorldAccess world, BlockPos origin, SpringFluid fluid) {
        if (!world.isSpringValid(origin.up(), fluid)
                || !world.isSpringValid(origin.down(), fluid)
                || !world.isAir(origin) && !world.isSpringValid(origin, fluid)) {
            return false;
        }

        int rockCount = 0;
        int holeCount = 0;
        BlockPos[] neighbors = {
            origin.west(), origin.east(), origin.north(), origin.south(), origin.down()
        };
        for (BlockPos neighbor : neighbors) {
            if (world.isSpringValid(neighbor, fluid)) {
                rockCount++;
            }
            if (world.isAir(neighbor)) {
                holeCount++;
            }
        }
        if (rockCount != 4 || holeCount != 1) {
            return false;
        }

        world.setSpring(origin, fluid);
        world.scheduleSpringTick(origin, fluid);
        return true;
    }

    static boolean supportsDefaultSpring(V118Biome biome) {
        return DEFAULT_SPRING_BIOMES.contains(biome);
    }

    private static void decorateFluid(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, SpringFluid fluid, DecorationResult result) {
        for (BlockPos candidate : candidates(world, worldSeed, chunkX, chunkZ, fluid)) {
            // BiomeFilter is the last placement modifier and checks the final sampled position.
            if (!DEFAULT_SPRING_BIOMES.contains(world.biomeAt(candidate))) {
                continue;
            }
            result.incrementAttempts(fluid);
            if (placeSpring(world, candidate, fluid)) {
                result.incrementPlaced(fluid);
            }
        }
    }

    private static V118WorldgenRandom featureRandom(long worldSeed,
            int chunkX, int chunkZ, int globalIndex) {
        V118WorldgenRandom random = new V118WorldgenRandom(0L);
        long decorationSeed = random.setDecorationSeed(
            worldSeed, chunkX << 4, chunkZ << 4);
        random.setFeatureSeed(decorationSeed, globalIndex, FLUID_SPRINGS_STEP);
        return random;
    }

    private static int veryBiasedToBottom(Random random, int minimum,
            int maximum, int inner) {
        if (maximum - minimum - inner + 1 <= 0) {
            return minimum;
        }
        int outer = betweenInclusive(random, minimum + inner, maximum);
        int middle = betweenInclusive(random, minimum, outer - 1);
        return betweenInclusive(random, minimum, middle - 1 + inner);
    }

    private static int betweenInclusive(Random random, int minimum, int maximum) {
        return minimum + random.nextInt(maximum - minimum + 1);
    }

    private static boolean appearsIn(Set<V118Biome> regionBiomes) {
        for (V118Biome biome : regionBiomes) {
            if (DEFAULT_SPRING_BIOMES.contains(biome)) {
                return true;
            }
        }
        return false;
    }

    private static void requireArguments(WorldAccess world, Set<V118Biome> regionBiomes) {
        if (world == null || regionBiomes == null) {
            throw new NullPointerException("world and regionBiomes are required");
        }
    }

    public enum SpringFluid {
        WATER,
        LAVA
    }

    public interface WorldAccess {
        int minBuildHeight();

        int maxBuildHeight();

        V118Biome biomeAt(BlockPos pos);

        boolean isAir(BlockPos pos);

        boolean isSpringValid(BlockPos pos, SpringFluid fluid);

        void setSpring(BlockPos pos, SpringFluid fluid);

        void scheduleSpringTick(BlockPos pos, SpringFluid fluid);
    }

    public static final class DecorationResult {
        private int waterAttempts;
        private int waterSpringsPlaced;
        private int lavaAttempts;
        private int lavaSpringsPlaced;

        public int waterAttempts() {
            return waterAttempts;
        }

        public int waterSpringsPlaced() {
            return waterSpringsPlaced;
        }

        public int lavaAttempts() {
            return lavaAttempts;
        }

        public int lavaSpringsPlaced() {
            return lavaSpringsPlaced;
        }

        private void incrementAttempts(SpringFluid fluid) {
            if (fluid == SpringFluid.WATER) {
                waterAttempts++;
            } else {
                lavaAttempts++;
            }
        }

        private void incrementPlaced(SpringFluid fluid) {
            if (fluid == SpringFluid.WATER) {
                waterSpringsPlaced++;
            } else {
                lavaSpringsPlaced++;
            }
        }
    }
}

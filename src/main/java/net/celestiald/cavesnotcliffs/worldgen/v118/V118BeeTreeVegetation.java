package net.celestiald.cavesnotcliffs.worldgen.v118;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Java 1.18.2 vegetation features that are prerequisites of bee-bearing trees.
 *
 * <p>These patches are not cosmetic to tree parity: they raise WORLD_SURFACE while leaving
 * OCEAN_FLOOR unchanged, causing the subsequent tree-threshold modifier to reject occupied
 * columns. Each feature owns the same independently seeded step/index random stream as the
 * official biome decorator.</p>
 */
public final class V118BeeTreeVegetation {
    private static final long PROVIDER_SEED = 2345L;
    private static final float PLAIN_FLOWER_SCALE = 0.005F;
    private static final float PLAIN_FLOWER_THRESHOLD = -0.8F;
    private static final float PLAIN_FLOWER_HIGH_CHANCE = 0.33333334F;
    private static final NormalNoise PLAIN_FLOWER_NOISE = NormalNoise.create(
            new LegacyRandomSource(PROVIDER_SEED), 0, 1.0D);
    private static final NormalNoise MEADOW_SLOW_NOISE = NormalNoise.create(
            new LegacyRandomSource(PROVIDER_SEED), -10, 1.0D);
    private static final NormalNoise MEADOW_FAST_NOISE = NormalNoise.create(
            new LegacyRandomSource(PROVIDER_SEED), -3, 1.0D);
    private static final Plant[] MEADOW_STATES = {
        Plant.TALL_GRASS, Plant.ALLIUM, Plant.POPPY, Plant.AZURE_BLUET,
        Plant.DANDELION, Plant.CORNFLOWER, Plant.OXEYE_DAISY, Plant.SHORT_GRASS
    };
    private static final Plant[] PLAIN_FLOWER_LOW_STATES = {
        Plant.ORANGE_TULIP, Plant.RED_TULIP, Plant.PINK_TULIP, Plant.WHITE_TULIP
    };
    private static final Plant[] PLAIN_FLOWER_HIGH_STATES = {
        Plant.POPPY, Plant.AZURE_BLUET, Plant.OXEYE_DAISY, Plant.CORNFLOWER
    };

    private V118BeeTreeVegetation() {
    }

    public static int place(WorldAccess world, long worldSeed, int chunkX, int chunkZ,
            PlacedFeature feature) {
        if (world == null || feature == null) {
            throw new NullPointerException("Vegetation feature arguments");
        }
        Random random = V118BeeTreePlacements.randomFor(worldSeed, chunkX, chunkZ,
                feature.globalIndex);
        return placeWithRandom(world, random, chunkX, chunkZ, feature);
    }

    static int placeWithRandom(WorldAccess world, Random random, int chunkX, int chunkZ,
            PlacedFeature feature) {
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        switch (feature) {
            case FLOWER_FOREST_FLOWERS:
            case FOREST_FLOWERS:
                return placeForestFlowers(world, random, originX, originZ, feature);
            case PATCH_SUNFLOWER:
                return placeSunflowers(world, random, originX, originZ, feature);
            case FLOWER_PLAINS:
                return placePlainFlowers(world, random, originX, originZ, feature);
            case PATCH_GRASS_PLAIN:
                return placePlainGrass(world, random, originX, originZ, feature);
            case FLOWER_MEADOW:
                return placeMeadowFlowers(world, random, originX, originZ, feature);
            default:
                throw new AssertionError(feature);
        }
    }

    private static int placeForestFlowers(WorldAccess world, Random random,
            int originX, int originZ, PlacedFeature feature) {
        if (random.nextFloat() >= 1.0F / 7.0F) {
            return 0;
        }
        int x = originX + random.nextInt(16);
        int z = originZ + random.nextInt(16);
        int y = world.height(Heightmap.MOTION_BLOCKING, x, z);
        int uniform = random.nextInt(5) + (feature == PlacedFeature.FLOWER_FOREST_FLOWERS
                ? -1 : -3);
        int count = clamp(uniform, 0,
                feature == PlacedFeature.FLOWER_FOREST_FLOWERS ? 3 : 1);
        int placed = 0;
        for (int attempt = 0; attempt < count; ++attempt) {
            if (feature.supports(world.biome(x, y, z))) {
                Plant selected;
                switch (random.nextInt(4)) {
                    case 0:
                        selected = Plant.LILAC;
                        break;
                    case 1:
                        selected = Plant.ROSE_BUSH;
                        break;
                    case 2:
                        selected = Plant.PEONY;
                        break;
                    case 3:
                    default:
                        selected = Plant.LILY_OF_THE_VALLEY;
                        break;
                }
                placed += randomPatch(world, random, new BlockPos(x, y, z),
                        96, 7, 3, selected, null);
            }
        }
        return placed;
    }

    private static int placeSunflowers(WorldAccess world, Random random,
            int originX, int originZ, PlacedFeature feature) {
        if (random.nextFloat() >= 1.0F / 3.0F) {
            return 0;
        }
        int x = originX + random.nextInt(16);
        int z = originZ + random.nextInt(16);
        int y = world.height(Heightmap.MOTION_BLOCKING, x, z);
        if (!feature.supports(world.biome(x, y, z))) {
            return 0;
        }
        return randomPatch(world, random, new BlockPos(x, y, z),
                96, 7, 3, Plant.SUNFLOWER, null);
    }

    private static int placePlainGrass(WorldAccess world, Random random,
            int originX, int originZ, PlacedFeature feature) {
        double noise = V118BiomeTemperature.biomeInfoNoise(
                (double) originX / 200.0D, (double) originZ / 200.0D);
        int count = noise < -0.8D ? 5 : 10;
        int placed = 0;
        for (int attempt = 0; attempt < count; ++attempt) {
            int x = originX + random.nextInt(16);
            int z = originZ + random.nextInt(16);
            int y = world.height(Heightmap.WORLD_SURFACE, x, z);
            if (feature.supports(world.biome(x, y, z))) {
                placed += randomPatch(world, random, new BlockPos(x, y, z),
                        32, 7, 3, Plant.SHORT_GRASS, null);
            }
        }
        return placed;
    }

    private static int placePlainFlowers(WorldAccess world, Random random,
            int originX, int originZ, PlacedFeature feature) {
        double noise = V118BiomeTemperature.biomeInfoNoise(
                (double) originX / 200.0D, (double) originZ / 200.0D);
        int count = noise < -0.8D ? 15 : 4;
        int placed = 0;
        for (int attempt = 0; attempt < count; ++attempt) {
            if (random.nextFloat() >= 1.0F / 32.0F) {
                continue;
            }
            int x = originX + random.nextInt(16);
            int z = originZ + random.nextInt(16);
            int y = world.height(Heightmap.MOTION_BLOCKING, x, z);
            if (!feature.supports(world.biome(x, y, z))) {
                continue;
            }
            placed += randomPatch(world, random, new BlockPos(x, y, z),
                    64, 6, 2, null, V118BeeTreeVegetation::plainFlower);
        }
        return placed;
    }

    private static int placeMeadowFlowers(WorldAccess world, Random random,
            int originX, int originZ, PlacedFeature feature) {
        int x = originX + random.nextInt(16);
        int z = originZ + random.nextInt(16);
        int y = world.height(Heightmap.MOTION_BLOCKING, x, z);
        if (!feature.supports(world.biome(x, y, z))) {
            return 0;
        }
        return randomPatch(world, random, new BlockPos(x, y, z),
                96, 6, 2, null, V118BeeTreeVegetation::meadowPlant);
    }

    private static int randomPatch(WorldAccess world, Random random, BlockPos origin,
            int tries, int xzSpread, int ySpread, Plant constant,
            PlantProvider provider) {
        int placed = 0;
        int horizontalBound = xzSpread + 1;
        int verticalBound = ySpread + 1;
        for (int attempt = 0; attempt < tries; ++attempt) {
            BlockPos position = origin.add(
                    random.nextInt(horizontalBound) - random.nextInt(horizontalBound),
                    random.nextInt(verticalBound) - random.nextInt(verticalBound),
                    random.nextInt(horizontalBound) - random.nextInt(horizontalBound));
            if (provider != null && !world.isAir(position)) {
                continue;
            }
            Plant plant = provider == null ? constant : provider.at(random, position);
            if (world.placePlant(position, plant)) {
                ++placed;
            }
        }
        return placed;
    }

    private static Plant plainFlower(Random random, BlockPos position) {
        double scale = (double) PLAIN_FLOWER_SCALE;
        double noise = PLAIN_FLOWER_NOISE.getValue(
                (double) position.getX() * scale,
                (double) position.getY() * scale,
                (double) position.getZ() * scale);
        if (noise < (double) PLAIN_FLOWER_THRESHOLD) {
            return PLAIN_FLOWER_LOW_STATES[random.nextInt(PLAIN_FLOWER_LOW_STATES.length)];
        }
        if (random.nextFloat() < PLAIN_FLOWER_HIGH_CHANCE) {
            return PLAIN_FLOWER_HIGH_STATES[
                    random.nextInt(PLAIN_FLOWER_HIGH_STATES.length)];
        }
        return Plant.DANDELION;
    }

    private static Plant meadowPlant(Random random, BlockPos position) {
        double slowValue = meadowSlowNoise(position);
        int variety = (int) clampedMap(slowValue, -1.0D, 1.0D, 1.0D, 4.0D);
        List<Plant> localStates = new ArrayList<>(variety);
        for (int index = 0; index < variety; ++index) {
            BlockPos sample = position.add(index * 54545, 0, index * 34234);
            localStates.add(noiseSelected(MEADOW_STATES, meadowSlowNoise(sample)));
        }
        double fastValue = MEADOW_FAST_NOISE.getValue(position.getX(),
                position.getY(), position.getZ());
        return noiseSelected(localStates.toArray(new Plant[localStates.size()]),
                fastValue);
    }

    private static double meadowSlowNoise(BlockPos position) {
        return MEADOW_SLOW_NOISE.getValue((float) position.getX(),
                (float) position.getY(), (float) position.getZ());
    }

    private static Plant noiseSelected(Plant[] states, double noise) {
        double mapped = clamp((1.0D + noise) / 2.0D, 0.0D, 0.9999D);
        return states[(int) (mapped * states.length)];
    }

    private static double clampedMap(double value, double oldMin, double oldMax,
            double newMin, double newMax) {
        double delta = (value - oldMin) / (oldMax - oldMin);
        return newMin + clamp(delta, 0.0D, 1.0D) * (newMax - newMin);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public enum Heightmap {
        MOTION_BLOCKING,
        WORLD_SURFACE
    }

    public enum Plant {
        LILAC(true),
        ROSE_BUSH(true),
        PEONY(true),
        LILY_OF_THE_VALLEY(false),
        SUNFLOWER(true),
        SHORT_GRASS(false),
        TALL_GRASS(true),
        ALLIUM(false),
        POPPY(false),
        AZURE_BLUET(false),
        DANDELION(false),
        CORNFLOWER(false),
        ORANGE_TULIP(false),
        RED_TULIP(false),
        PINK_TULIP(false),
        WHITE_TULIP(false),
        OXEYE_DAISY(false);

        private final boolean doublePlant;

        Plant(boolean doublePlant) {
            this.doublePlant = doublePlant;
        }

        public boolean isDoublePlant() {
            return doublePlant;
        }
    }

    public enum PlacedFeature {
        FLOWER_FOREST_FLOWERS(14, EnumSet.of(V118Biome.FLOWER_FOREST)),
        FOREST_FLOWERS(17, EnumSet.of(V118Biome.OLD_GROWTH_BIRCH_FOREST,
                V118Biome.BIRCH_FOREST, V118Biome.FOREST, V118Biome.DARK_FOREST)),
        PATCH_SUNFLOWER(29, EnumSet.of(V118Biome.SUNFLOWER_PLAINS)),
        FLOWER_PLAINS(31, EnumSet.of(V118Biome.PLAINS,
                V118Biome.SUNFLOWER_PLAINS, V118Biome.DRIPSTONE_CAVES)),
        PATCH_GRASS_PLAIN(32, EnumSet.of(V118Biome.PLAINS,
                V118Biome.SUNFLOWER_PLAINS, V118Biome.DRIPSTONE_CAVES,
                V118Biome.MEADOW)),
        FLOWER_MEADOW(33, EnumSet.of(V118Biome.MEADOW));

        private final int globalIndex;
        private final Set<V118Biome> biomes;

        PlacedFeature(int globalIndex, Set<V118Biome> biomes) {
            this.globalIndex = globalIndex;
            this.biomes = Collections.unmodifiableSet(EnumSet.copyOf(biomes));
        }

        public int globalIndex() {
            return globalIndex;
        }

        public boolean supports(V118Biome biome) {
            return biomes.contains(biome);
        }

        public boolean appearsIn(Set<V118Biome> regionBiomes) {
            for (V118Biome biome : biomes) {
                if (regionBiomes.contains(biome)) {
                    return true;
                }
            }
            return false;
        }
    }

    public interface WorldAccess {
        int height(Heightmap heightmap, int blockX, int blockZ);

        V118Biome biome(int blockX, int blockY, int blockZ);

        default boolean isAir(BlockPos pos) {
            return true;
        }

        boolean placePlant(BlockPos pos, Plant plant);
    }

    private interface PlantProvider {
        Plant at(Random random, BlockPos position);
    }
}

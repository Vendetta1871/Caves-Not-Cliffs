package net.celestiald.cavesnotcliffs.worldgen.v118;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118BeeTreePlacements.TreeKind;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118MountainTreeFeature.Kind;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118MountainTreeFeature.Result;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Native Java 1.18.2 placements for the first represented mountain-surface checkpoint.
 *
 * <p>The global indices are the official {@code MultiNoiseBiomeSource#featuresPerStep}
 * indices. Each entry owns an independent feature seed, so omitted neighboring features retain
 * their vanilla slots and request order cannot move a placement stream.</p>
 */
public final class V118MountainSurfacePlacements {
    private static final V118LushCaveFeature.Direction[] VINE_DIRECTIONS = {
        V118LushCaveFeature.Direction.DOWN, V118LushCaveFeature.Direction.UP,
        V118LushCaveFeature.Direction.NORTH, V118LushCaveFeature.Direction.SOUTH,
        V118LushCaveFeature.Direction.WEST, V118LushCaveFeature.Direction.EAST
    };
    public static final int LOCAL_MODIFICATIONS_STEP = 2;
    public static final int FLUID_SPRINGS_STEP = 8;
    public static final int VEGETAL_DECORATION_STEP = 9;
    public static final int TOP_LAYER_MODIFICATION_STEP = 10;

    public static final int FOREST_ROCK_INDEX = 4;
    public static final int SPRING_LAVA_FROZEN_INDEX = 2;
    public static final int TREES_BADLANDS_INDEX = 5;
    public static final int TREES_WINDSWEPT_SAVANNA_INDEX = 1;
    public static final int PATCH_TALL_GRASS_INDEX = 8;
    public static final int TREES_SAVANNA_INDEX = 9;
    public static final int FLOWER_WARM_INDEX = 10;
    public static final int PATCH_GRASS_JUNGLE_INDEX = 11;
    public static final int PATCH_GRASS_SAVANNA_INDEX = 12;
    public static final int PATCH_TALL_GRASS_2_INDEX = 21;
    public static final int TREES_WINDSWEPT_FOREST_INDEX = 35;
    public static final int PATCH_LARGE_FERN_INDEX = 36;
    public static final int TREES_TAIGA_INDEX = 39;
    public static final int TREES_GROVE_INDEX = 40;
    public static final int TREES_WINDSWEPT_HILLS_INDEX = 41;
    public static final int TREES_SNOWY_INDEX = 42;
    public static final int FLOWER_SWAMP_INDEX = 44;
    public static final int TREES_WATER_INDEX = 45;
    public static final int FLOWER_DEFAULT_INDEX = 46;
    public static final int PATCH_GRASS_TAIGA_INDEX = 47;
    public static final int PATCH_GRASS_FOREST_INDEX = 48;
    public static final int PATCH_GRASS_TAIGA_2_INDEX = 49;
    public static final int PATCH_GRASS_NORMAL_INDEX = 50;
    public static final int PATCH_DEAD_BUSH_INDEX = 51;
    public static final int BROWN_MUSHROOM_OLD_GROWTH_INDEX = 52;
    public static final int RED_MUSHROOM_OLD_GROWTH_INDEX = 53;
    public static final int PATCH_WATERLILY_INDEX = 54;
    public static final int BROWN_MUSHROOM_SWAMP_INDEX = 55;
    public static final int RED_MUSHROOM_SWAMP_INDEX = 56;
    public static final int PATCH_GRASS_BADLANDS_INDEX = 57;
    public static final int PATCH_DEAD_BUSH_2_INDEX = 58;
    public static final int PATCH_DEAD_BUSH_BADLANDS_INDEX = 59;
    public static final int BROWN_MUSHROOM_NORMAL_INDEX = 60;
    public static final int RED_MUSHROOM_NORMAL_INDEX = 61;
    public static final int PATCH_SUGAR_CANE_DESERT_INDEX = 62;
    public static final int PATCH_SUGAR_CANE_BADLANDS_INDEX = 63;
    public static final int PATCH_SUGAR_CANE_SWAMP_INDEX = 64;
    public static final int BROWN_MUSHROOM_TAIGA_INDEX = 66;
    public static final int RED_MUSHROOM_TAIGA_INDEX = 67;
    public static final int PATCH_SUGAR_CANE_INDEX = 68;
    public static final int PATCH_PUMPKIN_INDEX = 69;
    public static final int PATCH_CACTUS_DESERT_INDEX = 71;
    public static final int PATCH_CACTUS_DECORATED_INDEX = 72;
    public static final int VINES_INDEX = 73;
    public static final int PATCH_MELON_SPARSE_INDEX = 74;
    public static final int PATCH_MELON_INDEX = 75;
    public static final int FREEZE_TOP_LAYER_INDEX = 0;

    // Every registered Java 1.18.2 Overworld biome uses globalOverworldGeneration,
    // which installs the shared freeze_top_layer placed feature.
    private static final Set<V118Biome> FREEZE_TOP_LAYER_BIOMES =
        Collections.unmodifiableSet(EnumSet.allOf(V118Biome.class));
    private static final Set<V118Biome> FROZEN_SPRING_BIOMES = immutableSet(
        V118Biome.GROVE, V118Biome.SNOWY_SLOPES,
        V118Biome.FROZEN_PEAKS, V118Biome.JAGGED_PEAKS);
    private static final Set<V118Biome> FOREST_ROCK_BIOMES = immutableSet(
        V118Biome.OLD_GROWTH_PINE_TAIGA,
        V118Biome.OLD_GROWTH_SPRUCE_TAIGA);
    private static final Set<V118Biome> BADLANDS_TREE_BIOMES = immutableSet(
        V118Biome.WOODED_BADLANDS);
    private static final Set<V118Biome> WINDSWEPT_SAVANNA_TREE_BIOMES = immutableSet(
        V118Biome.WINDSWEPT_SAVANNA);
    private static final Set<V118Biome> SAVANNA_TREE_BIOMES = immutableSet(
        V118Biome.SAVANNA, V118Biome.SAVANNA_PLATEAU);
    private static final Set<V118Biome> TALL_GRASS_BIOMES = immutableSet(
        V118Biome.SAVANNA, V118Biome.SAVANNA_PLATEAU);
    private static final Set<V118Biome> FLOWER_WARM_BIOMES = immutableSet(
        V118Biome.BAMBOO_JUNGLE, V118Biome.JUNGLE, V118Biome.SAVANNA,
        V118Biome.SAVANNA_PLATEAU, V118Biome.SPARSE_JUNGLE);
    private static final Set<V118Biome> GRASS_JUNGLE_BIOMES = immutableSet(
        V118Biome.BAMBOO_JUNGLE, V118Biome.JUNGLE, V118Biome.SPARSE_JUNGLE);
    private static final Set<V118Biome> GRASS_SAVANNA_BIOMES = immutableSet(
        V118Biome.SAVANNA, V118Biome.SAVANNA_PLATEAU);
    private static final Set<V118Biome> TALL_GRASS_2_BIOMES = immutableSet(
        V118Biome.DRIPSTONE_CAVES, V118Biome.LUSH_CAVES, V118Biome.MEADOW,
        V118Biome.PLAINS, V118Biome.SUNFLOWER_PLAINS);
    private static final Set<V118Biome> LARGE_FERN_BIOMES = immutableSet(
        V118Biome.OLD_GROWTH_PINE_TAIGA, V118Biome.OLD_GROWTH_SPRUCE_TAIGA,
        V118Biome.SNOWY_TAIGA, V118Biome.TAIGA);
    private static final Set<V118Biome> WINDSWEPT_FOREST_TREE_BIOMES = immutableSet(
        V118Biome.WINDSWEPT_FOREST);
    private static final Set<V118Biome> TAIGA_TREE_BIOMES = immutableSet(
        V118Biome.SNOWY_TAIGA, V118Biome.TAIGA);
    private static final Set<V118Biome> GROVE_TREE_BIOMES = immutableSet(
        V118Biome.GROVE);
    private static final Set<V118Biome> WINDSWEPT_HILLS_TREE_BIOMES = immutableSet(
        V118Biome.WINDSWEPT_GRAVELLY_HILLS, V118Biome.WINDSWEPT_HILLS);
    private static final Set<V118Biome> SNOWY_TREE_BIOMES = immutableSet(
        V118Biome.ICE_SPIKES, V118Biome.SNOWY_PLAINS);
    private static final Set<V118Biome> FLOWER_SWAMP_BIOMES = immutableSet(
        V118Biome.SWAMP);
    private static final Set<V118Biome> WATER_TREE_BIOMES = immutableSet(
        V118Biome.COLD_OCEAN, V118Biome.DEEP_COLD_OCEAN,
        V118Biome.DEEP_FROZEN_OCEAN, V118Biome.DEEP_LUKEWARM_OCEAN,
        V118Biome.DEEP_OCEAN, V118Biome.FROZEN_OCEAN,
        V118Biome.FROZEN_RIVER, V118Biome.LUKEWARM_OCEAN,
        V118Biome.OCEAN, V118Biome.RIVER, V118Biome.WARM_OCEAN);
    private static final Set<V118Biome> FLOWER_DEFAULT_BIOMES = immutableSet(
        V118Biome.BEACH, V118Biome.BIRCH_FOREST, V118Biome.COLD_OCEAN,
        V118Biome.DARK_FOREST, V118Biome.DEEP_COLD_OCEAN,
        V118Biome.DEEP_FROZEN_OCEAN, V118Biome.DEEP_LUKEWARM_OCEAN,
        V118Biome.DEEP_OCEAN, V118Biome.DESERT, V118Biome.FOREST,
        V118Biome.FROZEN_OCEAN, V118Biome.FROZEN_RIVER, V118Biome.ICE_SPIKES,
        V118Biome.LUKEWARM_OCEAN, V118Biome.OCEAN,
        V118Biome.OLD_GROWTH_BIRCH_FOREST, V118Biome.OLD_GROWTH_PINE_TAIGA,
        V118Biome.OLD_GROWTH_SPRUCE_TAIGA, V118Biome.RIVER,
        V118Biome.SNOWY_BEACH, V118Biome.SNOWY_PLAINS, V118Biome.SNOWY_TAIGA,
        V118Biome.STONY_SHORE, V118Biome.TAIGA, V118Biome.WARM_OCEAN,
        V118Biome.WINDSWEPT_FOREST, V118Biome.WINDSWEPT_GRAVELLY_HILLS,
        V118Biome.WINDSWEPT_HILLS, V118Biome.WINDSWEPT_SAVANNA);
    private static final Set<V118Biome> GRASS_TAIGA_BIOMES = immutableSet(
        V118Biome.OLD_GROWTH_PINE_TAIGA, V118Biome.OLD_GROWTH_SPRUCE_TAIGA);
    private static final Set<V118Biome> GRASS_FOREST_BIOMES = immutableSet(
        V118Biome.BIRCH_FOREST, V118Biome.DARK_FOREST, V118Biome.FOREST,
        V118Biome.OLD_GROWTH_BIRCH_FOREST);
    private static final Set<V118Biome> GRASS_TAIGA_2_BIOMES = immutableSet(
        V118Biome.SNOWY_TAIGA, V118Biome.TAIGA);
    private static final Set<V118Biome> GRASS_NORMAL_BIOMES = immutableSet(
        V118Biome.SWAMP, V118Biome.WINDSWEPT_SAVANNA);
    private static final Set<V118Biome> GRASS_BADLANDS_BIOMES = immutableSet(
        V118Biome.BADLANDS, V118Biome.BEACH, V118Biome.COLD_OCEAN,
        V118Biome.DEEP_COLD_OCEAN, V118Biome.DEEP_FROZEN_OCEAN,
        V118Biome.DEEP_LUKEWARM_OCEAN, V118Biome.DEEP_OCEAN, V118Biome.DESERT,
        V118Biome.ERODED_BADLANDS, V118Biome.FLOWER_FOREST,
        V118Biome.FROZEN_OCEAN, V118Biome.FROZEN_RIVER, V118Biome.ICE_SPIKES,
        V118Biome.LUKEWARM_OCEAN, V118Biome.OCEAN, V118Biome.RIVER,
        V118Biome.SNOWY_BEACH, V118Biome.SNOWY_PLAINS, V118Biome.STONY_SHORE,
        V118Biome.WARM_OCEAN, V118Biome.WINDSWEPT_FOREST,
        V118Biome.WINDSWEPT_GRAVELLY_HILLS, V118Biome.WINDSWEPT_HILLS,
        V118Biome.WOODED_BADLANDS);
    private static final Set<V118Biome> DEAD_BUSH_BIOMES = immutableSet(
        V118Biome.OLD_GROWTH_PINE_TAIGA,
        V118Biome.OLD_GROWTH_SPRUCE_TAIGA, V118Biome.SWAMP);
    private static final Set<V118Biome> MUSHROOM_NORMAL_BIOMES = allBiomesExcept(
        V118Biome.FROZEN_PEAKS, V118Biome.GROVE, V118Biome.JAGGED_PEAKS,
        V118Biome.LUSH_CAVES, V118Biome.MEADOW, V118Biome.MUSHROOM_FIELDS,
        V118Biome.SNOWY_SLOPES, V118Biome.SNOWY_TAIGA, V118Biome.STONY_PEAKS,
        V118Biome.TAIGA);
    private static final Set<V118Biome> MUSHROOM_TAIGA_BIOMES = immutableSet(
        V118Biome.MUSHROOM_FIELDS, V118Biome.SNOWY_TAIGA, V118Biome.TAIGA);
    private static final Set<V118Biome> MUSHROOM_OLD_GROWTH_BIOMES = immutableSet(
        V118Biome.OLD_GROWTH_PINE_TAIGA, V118Biome.OLD_GROWTH_SPRUCE_TAIGA);
    private static final Set<V118Biome> MUSHROOM_SWAMP_BIOMES = immutableSet(
        V118Biome.SWAMP);
    private static final Set<V118Biome> WATERLILY_BIOMES = immutableSet(
        V118Biome.SWAMP);
    private static final Set<V118Biome> DEAD_BUSH_2_BIOMES = immutableSet(
        V118Biome.DESERT);
    private static final Set<V118Biome> DEAD_BUSH_BADLANDS_BIOMES = immutableSet(
        V118Biome.BADLANDS, V118Biome.ERODED_BADLANDS,
        V118Biome.WOODED_BADLANDS);
    private static final Set<V118Biome> SUGAR_CANE_DESERT_BIOMES = immutableSet(
        V118Biome.DESERT);
    private static final Set<V118Biome> SUGAR_CANE_BADLANDS_BIOMES = immutableSet(
        V118Biome.BADLANDS, V118Biome.ERODED_BADLANDS,
        V118Biome.WOODED_BADLANDS);
    private static final Set<V118Biome> SUGAR_CANE_SWAMP_BIOMES = immutableSet(
        V118Biome.SWAMP);
    private static final Set<V118Biome> ORDINARY_SUGAR_CANE_BIOMES = allBiomesExcept(
        V118Biome.BADLANDS, V118Biome.DESERT, V118Biome.ERODED_BADLANDS,
        V118Biome.FROZEN_PEAKS, V118Biome.JAGGED_PEAKS, V118Biome.LUSH_CAVES,
        V118Biome.MEADOW, V118Biome.STONY_PEAKS, V118Biome.SWAMP,
        V118Biome.WOODED_BADLANDS);
    private static final Set<V118Biome> PUMPKIN_BIOMES = allBiomesExcept(
        V118Biome.FROZEN_PEAKS, V118Biome.JAGGED_PEAKS, V118Biome.LUSH_CAVES,
        V118Biome.MEADOW, V118Biome.STONY_PEAKS);
    private static final Set<V118Biome> CACTUS_DESERT_BIOMES = immutableSet(
        V118Biome.DESERT);
    private static final Set<V118Biome> CACTUS_DECORATED_BIOMES = immutableSet(
        V118Biome.BADLANDS, V118Biome.ERODED_BADLANDS,
        V118Biome.WOODED_BADLANDS);
    private static final Set<V118Biome> VINE_BIOMES = immutableSet(
        V118Biome.BAMBOO_JUNGLE, V118Biome.JUNGLE, V118Biome.SPARSE_JUNGLE);
    private static final Set<V118Biome> MELON_SPARSE_BIOMES = immutableSet(
        V118Biome.SPARSE_JUNGLE);
    private static final Set<V118Biome> MELON_BIOMES = immutableSet(
        V118Biome.BAMBOO_JUNGLE, V118Biome.JUNGLE);

    private V118MountainSurfacePlacements() {
    }

    public static DecorationResult decorateForestRock(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, Set<V118Biome> regionBiomes) {
        requireArguments(world, regionBiomes);
        DecorationResult result = new DecorationResult();
        if (!world.supportsForestRockPlacement()
                || !appearsIn(FOREST_ROCK_BIOMES, regionBiomes)) {
            return result;
        }

        V118WorldgenRandom random = featureRandom(worldSeed, chunkX, chunkZ,
            FOREST_ROCK_INDEX, LOCAL_MODIFICATIONS_STEP);
        // CountPlacement is lazy: the complete first blob consumes its configured-feature
        // stream before InSquarePlacement samples the second origin.
        for (int attempt = 0; attempt < 2; ++attempt) {
            BlockPos origin = squareHeightmapPosition(world, random, chunkX, chunkZ);
            if (origin == null || !FOREST_ROCK_BIOMES.contains(world.biomeAt(origin))) {
                continue;
            }
            result.forestRockAttempts++;
            if (placeForestRock(world, random, origin)) {
                result.forestRocksPlaced++;
            }
        }
        return result;
    }

    public static DecorationResult decorateEarlyTrees(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, Set<V118Biome> regionBiomes) {
        requireArguments(world, regionBiomes);
        DecorationResult result = new DecorationResult();
        if (world.supportsBroadleafTreePlacement()
                && appearsIn(BADLANDS_TREE_BIOMES, regionBiomes)) {
            placeOakTrees(world, worldSeed, chunkX, chunkZ,
                TREES_BADLANDS_INDEX, 5, false,
                BADLANDS_TREE_BIOMES, result);
        }
        return result;
    }

    public static DecorationResult decorateWindsweptSavannaTrees(WorldAccess world,
            long worldSeed, int chunkX, int chunkZ, Set<V118Biome> regionBiomes) {
        requireArguments(world, regionBiomes);
        DecorationResult result = new DecorationResult();
        if (world.supportsAcaciaTreePlacement()
                && world.supportsBroadleafTreePlacement()
                && appearsIn(WINDSWEPT_SAVANNA_TREE_BIOMES, regionBiomes)) {
            placeSavannaTrees(world, worldSeed, chunkX, chunkZ,
                TREES_WINDSWEPT_SAVANNA_INDEX, 2,
                WINDSWEPT_SAVANNA_TREE_BIOMES, result);
        }
        return result;
    }

    public static DecorationResult decorateSavannaTrees(WorldAccess world,
            long worldSeed, int chunkX, int chunkZ, Set<V118Biome> regionBiomes) {
        requireArguments(world, regionBiomes);
        DecorationResult result = new DecorationResult();
        if (world.supportsAcaciaTreePlacement()
                && world.supportsBroadleafTreePlacement()
                && appearsIn(SAVANNA_TREE_BIOMES, regionBiomes)) {
            placeSavannaTrees(world, worldSeed, chunkX, chunkZ,
                TREES_SAVANNA_INDEX, 1, SAVANNA_TREE_BIOMES, result);
        }
        return result;
    }

    public static DecorationResult decorateEarlyDoublePlants(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, Set<V118Biome> regionBiomes) {
        requireArguments(world, regionBiomes);
        DecorationResult result = new DecorationResult();
        if (world.supportsDoublePlantPlacement()
                && appearsIn(TALL_GRASS_BIOMES, regionBiomes)) {
            placeDoublePlantPatch(world, worldSeed, chunkX, chunkZ,
                PATCH_TALL_GRASS_INDEX, 1, 5, false, TALL_GRASS_BIOMES, result);
        }
        return result;
    }

    public static DecorationResult decoratePreLushDoublePlants(WorldAccess world,
            long worldSeed, int chunkX, int chunkZ, Set<V118Biome> regionBiomes) {
        requireArguments(world, regionBiomes);
        DecorationResult result = new DecorationResult();
        if (world.supportsDoublePlantPlacement()
                && appearsIn(TALL_GRASS_2_BIOMES, regionBiomes)) {
            placeDoublePlantPatch(world, worldSeed, chunkX, chunkZ,
                PATCH_TALL_GRASS_2_INDEX, tallGrass2Count(chunkX, chunkZ), 32,
                false, TALL_GRASS_2_BIOMES, result);
        }
        return result;
    }

    public static DecorationResult decorateEarlyFlowers(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, Set<V118Biome> regionBiomes) {
        requireArguments(world, regionBiomes);
        DecorationResult result = new DecorationResult();
        if (world.supportsFlowerPlacement()
                && appearsIn(FLOWER_WARM_BIOMES, regionBiomes)) {
            placeFlowerPatch(world, worldSeed, chunkX, chunkZ,
                FLOWER_WARM_INDEX, 16, 7, 3, false,
                FLOWER_WARM_BIOMES, result);
        }
        return result;
    }

    public static DecorationResult decorateEarlyShortGrass(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, Set<V118Biome> regionBiomes) {
        requireArguments(world, regionBiomes);
        DecorationResult result = new DecorationResult();
        if (!world.supportsShortGrassPlacement()) {
            return result;
        }
        if (appearsIn(GRASS_JUNGLE_BIOMES, regionBiomes)) {
            placeShortGrassPatch(world, worldSeed, chunkX, chunkZ,
                PATCH_GRASS_JUNGLE_INDEX, 25, 3, 1, true,
                GRASS_JUNGLE_BIOMES, result);
        }
        if (appearsIn(GRASS_SAVANNA_BIOMES, regionBiomes)) {
            placeShortGrassPatch(world, worldSeed, chunkX, chunkZ,
                PATCH_GRASS_SAVANNA_INDEX, 20, 1, 0, false,
                GRASS_SAVANNA_BIOMES, result);
        }
        return result;
    }

    public static DecorationResult decorateLateDoublePlants(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, Set<V118Biome> regionBiomes) {
        requireArguments(world, regionBiomes);
        DecorationResult result = new DecorationResult();
        if (world.supportsDoublePlantPlacement()
                && appearsIn(LARGE_FERN_BIOMES, regionBiomes)) {
            placeDoublePlantPatch(world, worldSeed, chunkX, chunkZ,
                PATCH_LARGE_FERN_INDEX, 1, 5, true, LARGE_FERN_BIOMES, result);
        }
        return result;
    }

    public static DecorationResult decoratePreLateTrees(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, Set<V118Biome> regionBiomes) {
        requireArguments(world, regionBiomes);
        DecorationResult result = new DecorationResult();
        if (world.supportsBroadleafTreePlacement()
                && appearsIn(WINDSWEPT_FOREST_TREE_BIOMES, regionBiomes)) {
            placeWindsweptTrees(world, worldSeed, chunkX, chunkZ,
                TREES_WINDSWEPT_FOREST_INDEX, 3,
                WINDSWEPT_FOREST_TREE_BIOMES, result);
        }
        return result;
    }

    public static DecorationResult decorateFrozenSprings(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, Set<V118Biome> regionBiomes) {
        requireArguments(world, regionBiomes);
        DecorationResult result = new DecorationResult();
        if (!appearsIn(FROZEN_SPRING_BIOMES, regionBiomes)) {
            return result;
        }
        for (BlockPos candidate : frozenSpringCandidates(world, worldSeed, chunkX, chunkZ)) {
            if (!FROZEN_SPRING_BIOMES.contains(world.biomeAt(candidate))) {
                continue;
            }
            result.frozenSpringAttempts++;
            if (placeFrozenSpring(world, candidate)) {
                result.frozenSpringsPlaced++;
            }
        }
        return result;
    }

    public static DecorationResult decorateVegetation(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, Set<V118Biome> regionBiomes) {
        requireArguments(world, regionBiomes);
        DecorationResult result = new DecorationResult();
        if (appearsIn(TAIGA_TREE_BIOMES, regionBiomes)) {
            placeTaigaTrees(world, worldSeed, chunkX, chunkZ, result);
        }
        if (regionBiomes.contains(V118Biome.GROVE)) {
            placeGroveTrees(world, worldSeed, chunkX, chunkZ, result);
        }
        if (world.supportsBroadleafTreePlacement()
                && appearsIn(WINDSWEPT_HILLS_TREE_BIOMES, regionBiomes)) {
            placeWindsweptTrees(world, worldSeed, chunkX, chunkZ,
                TREES_WINDSWEPT_HILLS_INDEX, 0,
                WINDSWEPT_HILLS_TREE_BIOMES, result);
        }
        if (appearsIn(SNOWY_TREE_BIOMES, regionBiomes)) {
            placeSnowyTrees(world, worldSeed, chunkX, chunkZ, result);
        }
        if (regionBiomes.contains(V118Biome.SWAMP)) {
            V118SwampTreeFeature.Result trees = V118SwampTreeFeature.place(
                world, worldSeed, chunkX, chunkZ);
            result.treesPlaced += trees.trees();
            result.oaksPlaced += trees.trees();
            result.logsPlaced += trees.logs();
            result.leavesPlaced += trees.leaves();
        }
        boolean flowers = world.supportsFlowerPlacement();
        if (flowers && regionBiomes.contains(V118Biome.SWAMP)) {
            placeFlowerPatch(world, worldSeed, chunkX, chunkZ,
                FLOWER_SWAMP_INDEX, 32, 6, 2, true,
                FLOWER_SWAMP_BIOMES, result);
        }
        if (world.supportsBroadleafTreePlacement()
                && appearsIn(WATER_TREE_BIOMES, regionBiomes)) {
            placeOakTrees(world, worldSeed, chunkX, chunkZ,
                TREES_WATER_INDEX, 0, true,
                WATER_TREE_BIOMES, result);
        }
        if (flowers && appearsIn(FLOWER_DEFAULT_BIOMES, regionBiomes)) {
            placeFlowerPatch(world, worldSeed, chunkX, chunkZ,
                FLOWER_DEFAULT_INDEX, 32, 7, 3, false,
                FLOWER_DEFAULT_BIOMES, result);
        }
        boolean shortGrass = world.supportsShortGrassPlacement();
        if (shortGrass && appearsIn(GRASS_TAIGA_BIOMES, regionBiomes)) {
            placeShortGrassPatch(world, worldSeed, chunkX, chunkZ,
                PATCH_GRASS_TAIGA_INDEX, 7, 1, 4, false,
                GRASS_TAIGA_BIOMES, result);
        }
        if (shortGrass && appearsIn(GRASS_FOREST_BIOMES, regionBiomes)) {
            placeShortGrassPatch(world, worldSeed, chunkX, chunkZ,
                PATCH_GRASS_FOREST_INDEX, 2, 1, 0, false,
                GRASS_FOREST_BIOMES, result);
        }
        if (shortGrass && appearsIn(GRASS_TAIGA_2_BIOMES, regionBiomes)) {
            placeShortGrassPatch(world, worldSeed, chunkX, chunkZ,
                PATCH_GRASS_TAIGA_2_INDEX, 1, 1, 4, false,
                GRASS_TAIGA_2_BIOMES, result);
        }
        if (shortGrass && appearsIn(GRASS_NORMAL_BIOMES, regionBiomes)) {
            placeShortGrassPatch(world, worldSeed, chunkX, chunkZ,
                PATCH_GRASS_NORMAL_INDEX, 5, 1, 0, false,
                GRASS_NORMAL_BIOMES, result);
        }
        if (appearsIn(DEAD_BUSH_BIOMES, regionBiomes)) {
            placeDeadBushPatch(world, worldSeed, chunkX, chunkZ,
                PATCH_DEAD_BUSH_INDEX, 1, DEAD_BUSH_BIOMES, result);
        }
        boolean mushrooms = world.supportsMushroomPlacement();
        // Preserve the dense registered sequence: grass 47-50, shared dead bush 51,
        // old-growth mushrooms 52/53, waterlily 54, swamp mushrooms 55/56,
        // default grass 57, then desert/badlands bushes 58/59.
        if (mushrooms && appearsIn(MUSHROOM_OLD_GROWTH_BIOMES, regionBiomes)) {
            placeMushroomPatch(world, worldSeed, chunkX, chunkZ,
                BROWN_MUSHROOM_OLD_GROWTH_INDEX, 3, 4, false,
                MUSHROOM_OLD_GROWTH_BIOMES, result);
            placeMushroomPatch(world, worldSeed, chunkX, chunkZ,
                RED_MUSHROOM_OLD_GROWTH_INDEX, 1, 171, true,
                MUSHROOM_OLD_GROWTH_BIOMES, result);
        }
        if (world.supportsWaterlilyPlacement()
                && regionBiomes.contains(V118Biome.SWAMP)) {
            placeWaterlilyPatch(world, worldSeed, chunkX, chunkZ, result);
        }
        if (mushrooms && regionBiomes.contains(V118Biome.SWAMP)) {
            placeMushroomPatch(world, worldSeed, chunkX, chunkZ,
                BROWN_MUSHROOM_SWAMP_INDEX, 2, 0, false,
                MUSHROOM_SWAMP_BIOMES, result);
            placeMushroomPatch(world, worldSeed, chunkX, chunkZ,
                RED_MUSHROOM_SWAMP_INDEX, 1, 64, true,
                MUSHROOM_SWAMP_BIOMES, result);
        }
        if (shortGrass && appearsIn(GRASS_BADLANDS_BIOMES, regionBiomes)) {
            placeShortGrassPatch(world, worldSeed, chunkX, chunkZ,
                PATCH_GRASS_BADLANDS_INDEX, 1, 1, 0, false,
                GRASS_BADLANDS_BIOMES, result);
        }
        if (regionBiomes.contains(V118Biome.DESERT)) {
            placeDeadBushPatch(world, worldSeed, chunkX, chunkZ,
                PATCH_DEAD_BUSH_2_INDEX, 2, DEAD_BUSH_2_BIOMES, result);
        }
        if (appearsIn(DEAD_BUSH_BADLANDS_BIOMES, regionBiomes)) {
            placeDeadBushPatch(world, worldSeed, chunkX, chunkZ,
                PATCH_DEAD_BUSH_BADLANDS_INDEX, 20,
                DEAD_BUSH_BADLANDS_BIOMES, result);
        }
        if (mushrooms && appearsIn(MUSHROOM_NORMAL_BIOMES, regionBiomes)) {
            placeMushroomPatch(world, worldSeed, chunkX, chunkZ,
                BROWN_MUSHROOM_NORMAL_INDEX, 1, 256, false,
                MUSHROOM_NORMAL_BIOMES, result);
            placeMushroomPatch(world, worldSeed, chunkX, chunkZ,
                RED_MUSHROOM_NORMAL_INDEX, 1, 512, true,
                MUSHROOM_NORMAL_BIOMES, result);
        }
        if (regionBiomes.contains(V118Biome.DESERT)) {
            placeSugarCanePatch(world, worldSeed, chunkX, chunkZ,
                PATCH_SUGAR_CANE_DESERT_INDEX, 0,
                SUGAR_CANE_DESERT_BIOMES, result);
        }
        if (appearsIn(SUGAR_CANE_BADLANDS_BIOMES, regionBiomes)) {
            placeSugarCanePatch(world, worldSeed, chunkX, chunkZ,
                PATCH_SUGAR_CANE_BADLANDS_INDEX, 5,
                SUGAR_CANE_BADLANDS_BIOMES, result);
        }
        if (regionBiomes.contains(V118Biome.SWAMP)) {
            placeSugarCanePatch(world, worldSeed, chunkX, chunkZ,
                PATCH_SUGAR_CANE_SWAMP_INDEX, 3,
                SUGAR_CANE_SWAMP_BIOMES, result);
        }
        if (mushrooms && appearsIn(MUSHROOM_TAIGA_BIOMES, regionBiomes)) {
            placeMushroomPatch(world, worldSeed, chunkX, chunkZ,
                BROWN_MUSHROOM_TAIGA_INDEX, 1, 4, false,
                MUSHROOM_TAIGA_BIOMES, result);
            placeMushroomPatch(world, worldSeed, chunkX, chunkZ,
                RED_MUSHROOM_TAIGA_INDEX, 1, 256, true,
                MUSHROOM_TAIGA_BIOMES, result);
        }
        if (appearsIn(ORDINARY_SUGAR_CANE_BIOMES, regionBiomes)) {
            placeSugarCanePatch(world, worldSeed, chunkX, chunkZ,
                PATCH_SUGAR_CANE_INDEX, 6,
                ORDINARY_SUGAR_CANE_BIOMES, result);
        }
        if (appearsIn(PUMPKIN_BIOMES, regionBiomes)) {
            placePumpkinPatch(world, worldSeed, chunkX, chunkZ, result);
        }
        // Preserve the registered tail of global step 9: pumpkin 69, cactus 71/72,
        // vines 73, sparse melon 74, then regular melon 75.
        if (world.supportsCactusPlacement()) {
            if (regionBiomes.contains(V118Biome.DESERT)) {
                placeCactusPatch(world, worldSeed, chunkX, chunkZ,
                    PATCH_CACTUS_DESERT_INDEX, 6, CACTUS_DESERT_BIOMES, result);
            }
            if (appearsIn(CACTUS_DECORATED_BIOMES, regionBiomes)) {
                placeCactusPatch(world, worldSeed, chunkX, chunkZ,
                    PATCH_CACTUS_DECORATED_INDEX, 13,
                    CACTUS_DECORATED_BIOMES, result);
            }
        }
        if (world.supportsVinePlacement()
                && appearsIn(VINE_BIOMES, regionBiomes)) {
            placeVines(world, worldSeed, chunkX, chunkZ, result);
        }
        if (world.supportsMelonPlacement()) {
            if (regionBiomes.contains(V118Biome.SPARSE_JUNGLE)) {
                placeMelonPatch(world, worldSeed, chunkX, chunkZ,
                    PATCH_MELON_SPARSE_INDEX, 64, MELON_SPARSE_BIOMES, result);
            }
            if (appearsIn(MELON_BIOMES, regionBiomes)) {
                placeMelonPatch(world, worldSeed, chunkX, chunkZ,
                    PATCH_MELON_INDEX, 6, MELON_BIOMES, result);
            }
        }
        return result;
    }

    public static DecorationResult decorateTopLayer(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, Set<V118Biome> regionBiomes) {
        requireArguments(world, regionBiomes);
        DecorationResult result = new DecorationResult();
        // FREEZE_TOP_LAYER has no random placement modifier. Seeding is retained as an explicit
        // contract even though SnowAndFreezeFeature consumes no random values.
        featureRandom(worldSeed, chunkX, chunkZ,
            FREEZE_TOP_LAYER_INDEX, TOP_LAYER_MODIFICATION_STEP);
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        // BiomeFilter runs once at the placed-feature origin. SnowAndFreezeFeature then
        // evaluates every column, including columns whose surface biome differs from it.
        BlockPos featureOrigin = new BlockPos(originX, world.minBuildHeight(), originZ);
        if (!FREEZE_TOP_LAYER_BIOMES.contains(world.biomeAt(featureOrigin))) {
            return result;
        }
        for (int localX = 0; localX < 16; ++localX) {
            for (int localZ = 0; localZ < 16; ++localZ) {
                int x = originX + localX;
                int z = originZ + localZ;
                int y = world.motionBlockingHeight(x, z);
                if (y < world.minBuildHeight() || y > world.maxBuildHeight()) {
                    continue;
                }
                BlockPos surface = new BlockPos(x, y, z);
                V118Biome biome = world.biomeAt(surface);
                BlockPos below = surface.down();
                if (V118BiomeTemperature.coldEnoughToSnow(biome, x, y - 1, z)
                        && world.blockLight(below) < 10 && world.isWater(below)) {
                    world.setIce(below);
                    result.waterFrozen++;
                }
                if (y < world.maxBuildHeight()
                        && V118BiomeTemperature.coldEnoughToSnow(biome, x, y, z)
                        && world.blockLight(surface) < 10 && world.isAir(surface)
                        && world.canSnowSurvive(surface)) {
                    world.setSnowLayer(surface);
                    result.snowLayersPlaced++;
                }
            }
        }
        return result;
    }

    static List<BlockPos> frozenSpringCandidates(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ) {
        V118WorldgenRandom random = featureRandom(worldSeed, chunkX, chunkZ,
            SPRING_LAVA_FROZEN_INDEX, FLUID_SPRINGS_STEP);
        List<BlockPos> result = new ArrayList<BlockPos>(20);
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        for (int attempt = 0; attempt < 20; ++attempt) {
            int x = originX + random.nextInt(16);
            int z = originZ + random.nextInt(16);
            int y = veryBiasedToBottom(random, world.minBuildHeight(),
                world.maxBuildHeight() - 1 - 8, 8);
            result.add(new BlockPos(x, y, z));
        }
        return Collections.unmodifiableList(result);
    }

    static float initialFeatureFloat(long worldSeed, int chunkX, int chunkZ,
            int globalIndex, int decorationStep) {
        return featureRandom(worldSeed, chunkX, chunkZ, globalIndex, decorationStep)
            .nextFloat();
    }

    static boolean supportsFrozenSpring(V118Biome biome) {
        return FROZEN_SPRING_BIOMES.contains(biome);
    }

    static boolean supportsForestRock(V118Biome biome) {
        return FOREST_ROCK_BIOMES.contains(biome);
    }

    static boolean supportsGroveTrees(V118Biome biome) {
        return biome == V118Biome.GROVE;
    }

    static boolean supportsTallGrass(V118Biome biome) {
        return TALL_GRASS_BIOMES.contains(biome);
    }

    static boolean supportsTallGrass2(V118Biome biome) {
        return TALL_GRASS_2_BIOMES.contains(biome);
    }

    static boolean supportsJungleGrass(V118Biome biome) {
        return GRASS_JUNGLE_BIOMES.contains(biome);
    }

    static boolean supportsSavannaGrass(V118Biome biome) {
        return GRASS_SAVANNA_BIOMES.contains(biome);
    }

    static boolean supportsLargeFern(V118Biome biome) {
        return LARGE_FERN_BIOMES.contains(biome);
    }

    static boolean supportsTaigaGrass(V118Biome biome) {
        return GRASS_TAIGA_BIOMES.contains(biome);
    }

    static boolean supportsForestGrass(V118Biome biome) {
        return GRASS_FOREST_BIOMES.contains(biome);
    }

    static boolean supportsTaigaGrass2(V118Biome biome) {
        return GRASS_TAIGA_2_BIOMES.contains(biome);
    }

    static boolean supportsNormalGrass(V118Biome biome) {
        return GRASS_NORMAL_BIOMES.contains(biome);
    }

    static boolean supportsBadlandsGrass(V118Biome biome) {
        return GRASS_BADLANDS_BIOMES.contains(biome);
    }

    static int tallGrass2Count(int chunkX, int chunkZ) {
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        double noise = V118BiomeTemperature.biomeInfoNoise(
            originX / 200.0D, originZ / 200.0D);
        return noise < -0.8D ? 0 : 7;
    }

    static boolean supportsDeadBush2(V118Biome biome) {
        return DEAD_BUSH_2_BIOMES.contains(biome);
    }

    static boolean supportsDeadBush(V118Biome biome) {
        return DEAD_BUSH_BIOMES.contains(biome);
    }

    static boolean supportsNormalMushroom(V118Biome biome) {
        return MUSHROOM_NORMAL_BIOMES.contains(biome);
    }

    static boolean supportsTaigaMushroom(V118Biome biome) {
        return MUSHROOM_TAIGA_BIOMES.contains(biome);
    }

    static boolean supportsOldGrowthMushroom(V118Biome biome) {
        return MUSHROOM_OLD_GROWTH_BIOMES.contains(biome);
    }

    static boolean supportsSwampMushroom(V118Biome biome) {
        return MUSHROOM_SWAMP_BIOMES.contains(biome);
    }

    static boolean supportsWaterlily(V118Biome biome) {
        return WATERLILY_BIOMES.contains(biome);
    }

    static boolean supportsDeadBushBadlands(V118Biome biome) {
        return DEAD_BUSH_BADLANDS_BIOMES.contains(biome);
    }

    static boolean supportsOrdinarySugarCane(V118Biome biome) {
        return ORDINARY_SUGAR_CANE_BIOMES.contains(biome);
    }

    static boolean supportsDesertSugarCane(V118Biome biome) {
        return SUGAR_CANE_DESERT_BIOMES.contains(biome);
    }

    static boolean supportsBadlandsSugarCane(V118Biome biome) {
        return SUGAR_CANE_BADLANDS_BIOMES.contains(biome);
    }

    static boolean supportsSwampSugarCane(V118Biome biome) {
        return SUGAR_CANE_SWAMP_BIOMES.contains(biome);
    }

    static boolean supportsPumpkin(V118Biome biome) {
        return PUMPKIN_BIOMES.contains(biome);
    }

    static boolean supportsDesertCactus(V118Biome biome) {
        return CACTUS_DESERT_BIOMES.contains(biome);
    }

    static boolean supportsDecoratedCactus(V118Biome biome) {
        return CACTUS_DECORATED_BIOMES.contains(biome);
    }

    static boolean supportsSparseMelon(V118Biome biome) {
        return MELON_SPARSE_BIOMES.contains(biome);
    }

    static boolean supportsMelon(V118Biome biome) {
        return MELON_BIOMES.contains(biome);
    }

    static boolean supportsFreezeTopLayer(V118Biome biome) {
        return FREEZE_TOP_LAYER_BIOMES.contains(biome);
    }

    private static boolean placeFrozenSpring(WorldAccess world, BlockPos origin) {
        if (!world.isFrozenSpringValid(origin.up())
                || !world.isFrozenSpringValid(origin.down())
                || !world.isAir(origin) && !world.isFrozenSpringValid(origin)) {
            return false;
        }
        int rockCount = 0;
        int holeCount = 0;
        BlockPos[] neighbors = {
            origin.west(), origin.east(), origin.north(), origin.south(), origin.down()
        };
        for (BlockPos neighbor : neighbors) {
            if (world.isFrozenSpringValid(neighbor)) {
                rockCount++;
            }
            if (world.isAir(neighbor)) {
                holeCount++;
            }
        }
        if (rockCount != 4 || holeCount != 1) {
            return false;
        }
        world.setLava(origin);
        world.scheduleLavaTick(origin);
        return true;
    }

    private static void placeGroveTrees(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, DecorationResult result) {
        V118WorldgenRandom random = featureRandom(worldSeed, chunkX, chunkZ,
            TREES_GROVE_INDEX, VEGETAL_DECORATION_STEP);
        int attempts = 10 + (random.nextInt(10) == 9 ? 1 : 0);
        for (int attempt = 0; attempt < attempts; ++attempt) {
            BlockPos origin = sampleTreeOrigin(world, random, chunkX, chunkZ,
                GROVE_TREE_BIOMES);
            if (origin == null) {
                continue;
            }

            Kind kind = random.nextFloat() < 0.33333334F ? Kind.PINE : Kind.SPRUCE;
            BlockPos treeOrigin = scanAbovePowderSnow(world, origin);
            if (treeOrigin == null || !world.isSnowTreeSupport(treeOrigin.down())) {
                continue;
            }
            recordTree(V118MountainTreeFeature.place(world, random, treeOrigin, kind),
                result);
        }
    }

    private static void placeTaigaTrees(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, DecorationResult result) {
        V118WorldgenRandom random = featureRandom(worldSeed, chunkX, chunkZ,
            TREES_TAIGA_INDEX, VEGETAL_DECORATION_STEP);
        int attempts = 10 + (random.nextInt(10) == 9 ? 1 : 0);
        for (int attempt = 0; attempt < attempts; ++attempt) {
            BlockPos origin = sampleTreeOrigin(world, random, chunkX, chunkZ,
                TAIGA_TREE_BIOMES);
            if (origin == null) {
                continue;
            }

            // RandomSelector chooses the child before its checked-sapling filter runs.
            Kind kind = random.nextFloat() < 0.33333334F ? Kind.PINE : Kind.SPRUCE;
            if (!world.canSpruceSaplingSurvive(origin)) {
                continue;
            }
            recordTree(V118MountainTreeFeature.place(world, random, origin, kind), result);
        }
    }

    private static void placeSnowyTrees(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, DecorationResult result) {
        V118WorldgenRandom random = featureRandom(worldSeed, chunkX, chunkZ,
            TREES_SNOWY_INDEX, VEGETAL_DECORATION_STEP);
        int attempts = random.nextInt(10) == 9 ? 1 : 0;
        for (int attempt = 0; attempt < attempts; ++attempt) {
            BlockPos origin = sampleTreeOrigin(world, random, chunkX, chunkZ,
                SNOWY_TREE_BIOMES);
            if (origin == null || !world.canSpruceSaplingSurvive(origin)) {
                continue;
            }
            recordTree(V118MountainTreeFeature.place(world, random, origin, Kind.SPRUCE),
                result);
        }
    }

    private static void placeWindsweptTrees(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, int globalIndex, int baseCount,
            Set<V118Biome> featureBiomes, DecorationResult result) {
        V118WorldgenRandom random = featureRandom(worldSeed, chunkX, chunkZ,
            globalIndex, VEGETAL_DECORATION_STEP);
        int attempts = baseCount + (random.nextInt(10) == 9 ? 1 : 0);
        for (int attempt = 0; attempt < attempts; ++attempt) {
            BlockPos origin = sampleTreeOrigin(world, random, chunkX, chunkZ,
                featureBiomes);
            if (origin == null) {
                continue;
            }

            // RandomSelector evaluates children in order. A selected checked child that cannot
            // survive fails the configured feature instead of falling through to the next tree.
            if (random.nextFloat() < 0.666F) {
                if (!world.canSpruceSaplingSurvive(origin)) {
                    continue;
                }
                recordTree(V118MountainTreeFeature.place(
                    world, random, origin, Kind.SPRUCE), result);
                continue;
            }

            TreeKind kind = random.nextFloat() < 0.1F
                ? TreeKind.FANCY_OAK : TreeKind.OAK;
            if (!world.canBroadleafSaplingSurvive(origin)) {
                continue;
            }
            recordBroadleafTree(V118BeeTreeFeature.place(
                world, random, origin, kind), origin, kind, result);
        }
    }

    private static void placeSavannaTrees(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, int globalIndex, int baseCount,
            Set<V118Biome> featureBiomes, DecorationResult result) {
        V118WorldgenRandom random = featureRandom(worldSeed, chunkX, chunkZ,
            globalIndex, VEGETAL_DECORATION_STEP);
        int attempts = baseCount + (random.nextInt(10) == 9 ? 1 : 0);
        for (int attempt = 0; attempt < attempts; ++attempt) {
            BlockPos origin = sampleTreeOrigin(world, random, chunkX, chunkZ,
                featureBiomes);
            if (origin == null) {
                continue;
            }
            // RandomSelector checks its 0.8 acacia child first and otherwise uses oak.
            boolean acacia = random.nextFloat() < 0.8F;
            if (!world.canBroadleafSaplingSurvive(origin)) {
                continue;
            }
            if (acacia) {
                V118AcaciaTreeFeature.Result tree = V118AcaciaTreeFeature.place(
                    world, random, origin);
                if (tree.placed()) {
                    result.treesPlaced++;
                    result.acaciasPlaced++;
                    result.logsPlaced += tree.trunks();
                    result.leavesPlaced += tree.foliage();
                }
            } else {
                recordBroadleafTree(V118BeeTreeFeature.place(
                    world, random, origin, TreeKind.OAK), origin,
                    TreeKind.OAK, result);
            }
        }
    }

    private static boolean placeForestRock(WorldAccess world, Random random,
            BlockPos origin) {
        BlockPos center = origin;
        while (center.getY() > world.minBuildHeight() + 3) {
            BlockPos below = center.down();
            if (!world.isAir(below) && world.isForestRockGround(below)) {
                break;
            }
            center = below;
        }
        if (center.getY() <= world.minBuildHeight() + 3) {
            return false;
        }

        for (int round = 0; round < 3; ++round) {
            int radiusX = random.nextInt(2);
            int radiusY = random.nextInt(2);
            int radiusZ = random.nextInt(2);
            float radius = (float) (radiusX + radiusY + radiusZ) * 0.333F + 0.5F;
            float radiusSquared = radius * radius;

            // BlockPos.betweenClosed advances X fastest, then Y, then Z.
            for (int z = center.getZ() - radiusZ;
                    z <= center.getZ() + radiusZ; ++z) {
                for (int y = center.getY() - radiusY;
                        y <= center.getY() + radiusY; ++y) {
                    for (int x = center.getX() - radiusX;
                            x <= center.getX() + radiusX; ++x) {
                        int dx = x - center.getX();
                        int dy = y - center.getY();
                        int dz = z - center.getZ();
                        if ((double) (dx * dx + dy * dy + dz * dz)
                                <= (double) radiusSquared) {
                            world.setMossyCobblestone(new BlockPos(x, y, z));
                        }
                    }
                }
            }

            // The official feature consumes this recenter triplet after round three too.
            center = center.add(-1 + random.nextInt(2), -random.nextInt(2),
                -1 + random.nextInt(2));
        }
        return true;
    }

    private static void placeOakTrees(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, int globalIndex, int baseCount,
            boolean fancySelector, Set<V118Biome> featureBiomes,
            DecorationResult result) {
        V118WorldgenRandom random = featureRandom(worldSeed, chunkX, chunkZ,
            globalIndex, VEGETAL_DECORATION_STEP);
        int attempts = baseCount + (random.nextInt(10) == 9 ? 1 : 0);
        for (int attempt = 0; attempt < attempts; ++attempt) {
            BlockPos origin = sampleTreeOrigin(world, random, chunkX, chunkZ,
                featureBiomes);
            if (origin == null) {
                continue;
            }

            // trees_water selects its checked child before support. trees_badlands has no
            // selector and applies its outer checked-sapling filter before the raw oak feature.
            TreeKind kind = fancySelector && random.nextFloat() < 0.1F
                ? TreeKind.FANCY_OAK : TreeKind.OAK;
            if (!world.canBroadleafSaplingSurvive(origin)) {
                continue;
            }
            recordBroadleafTree(V118BeeTreeFeature.place(
                world, random, origin, kind), origin, kind, result);
        }
    }

    private static BlockPos sampleTreeOrigin(WorldAccess world, Random random,
            int chunkX, int chunkZ, Set<V118Biome> featureBiomes) {
        int x = (chunkX << 4) + random.nextInt(16);
        int z = (chunkZ << 4) + random.nextInt(16);
        // SurfaceWaterDepthFilter queries OCEAN_FLOOR before WORLD_SURFACE. The following
        // height placement performs a second OCEAN_FLOOR query on accepted columns.
        int oceanFloor = world.oceanFloorHeight(x, z);
        int worldSurface = world.worldSurfaceHeight(x, z);
        if (worldSurface - oceanFloor > 0) {
            return null;
        }
        int y = world.oceanFloorHeight(x, z);
        if (y <= world.minBuildHeight()) {
            return null;
        }
        BlockPos origin = new BlockPos(x, y, z);
        return featureBiomes.contains(world.biomeAt(origin)) ? origin : null;
    }

    private static void recordTree(Result tree, DecorationResult result) {
        if (!tree.placed()) {
            return;
        }
        result.treesPlaced++;
        result.logsPlaced += tree.logs();
        result.leavesPlaced += tree.leaves();
        if (tree.kind() == Kind.PINE) {
            result.pinesPlaced++;
        } else {
            result.sprucesPlaced++;
        }
    }

    private static void recordBroadleafTree(V118BeeTreeFeature.Result tree,
            BlockPos origin, TreeKind kind, DecorationResult result) {
        if (!tree.placed()) {
            return;
        }
        result.treesPlaced++;
        int dirtWrites = tree.trunks().contains(origin.down()) ? 1 : 0;
        result.logsPlaced += tree.trunks().size() - dirtWrites;
        result.leavesPlaced += tree.foliage().size();
        if (kind == TreeKind.FANCY_OAK) {
            result.fancyOaksPlaced++;
        } else {
            result.oaksPlaced++;
        }
    }

    private static BlockPos scanAbovePowderSnow(WorldAccess world, BlockPos origin) {
        BlockPos cursor = origin;
        for (int step = 0; step < 8; ++step) {
            if (!world.isPowderSnow(cursor)) {
                return cursor;
            }
            cursor = cursor.up();
            if (cursor.getY() >= world.maxBuildHeight()) {
                return null;
            }
        }
        return world.isPowderSnow(cursor) ? null : cursor;
    }

    private static void placeDoublePlantPatch(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, int globalIndex, int outerAttempts,
            int rarityChance, boolean largeFern, Set<V118Biome> featureBiomes,
            DecorationResult result) {
        V118WorldgenRandom random = featureRandom(worldSeed, chunkX, chunkZ,
            globalIndex, VEGETAL_DECORATION_STEP);
        // Count/noise-count streams are lazy: each output runs rarity and completes its
        // configured patch before the next output is evaluated.
        for (int outer = 0; outer < outerAttempts; ++outer) {
            if (random.nextFloat() >= 1.0F / rarityChance) {
                continue;
            }
            BlockPos origin = squareHeightmapPosition(world, random, chunkX, chunkZ);
            if (origin == null || !featureBiomes.contains(world.biomeAt(origin))) {
                continue;
            }
            for (int attempt = 0; attempt < 96; ++attempt) {
                BlockPos candidate = origin.add(random.nextInt(8) - random.nextInt(8),
                    random.nextInt(4) - random.nextInt(4),
                    random.nextInt(8) - random.nextInt(8));
                if (!world.isDoublePlantPlacementAir(candidate)
                        || !world.canDoublePlantSurvive(candidate)
                        || !world.isDoublePlantUpperEmpty(candidate.up())) {
                    continue;
                }
                if (largeFern) {
                    world.setLargeFern(candidate);
                    result.largeFernsPlaced++;
                } else {
                    world.setTallGrass(candidate);
                    result.tallGrassPlaced++;
                }
            }
        }
    }

    private static void placeShortGrassPatch(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, int globalIndex, int outerAttempts,
            int grassWeight, int fernWeight, boolean excludePodzol,
            Set<V118Biome> featureBiomes,
            DecorationResult result) {
        V118WorldgenRandom random = featureRandom(worldSeed, chunkX, chunkZ,
            globalIndex, VEGETAL_DECORATION_STEP);
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        // CountPlacement is lazy: each origin completes all 32 configured-patch tries
        // before the next InSquare position is sampled from this feature stream.
        for (int outer = 0; outer < outerAttempts; ++outer) {
            int x = originX + random.nextInt(16);
            int z = originZ + random.nextInt(16);
            int y = world.worldSurfaceHeight(x, z);
            if (y <= world.minBuildHeight()) {
                continue;
            }
            BlockPos origin = new BlockPos(x, y, z);
            if (!featureBiomes.contains(world.biomeAt(origin))) {
                continue;
            }
            for (int attempt = 0; attempt < 32; ++attempt) {
                BlockPos candidate = origin.add(random.nextInt(8) - random.nextInt(8),
                    random.nextInt(4) - random.nextInt(4),
                    random.nextInt(8) - random.nextInt(8));
                if (!world.isShortGrassPlacementAir(candidate)) {
                    continue;
                }
                if (excludePodzol && world.isPodzol(candidate.down())) {
                    continue;
                }
                // WeightedStateProvider is evaluated after the configured predicates
                // and before the selected state's survival query.
                boolean fern = fernWeight > 0
                    && random.nextInt(grassWeight + fernWeight) >= grassWeight;
                if (!world.canShortGrassSurvive(candidate)) {
                    continue;
                }
                if (fern) {
                    world.setFern(candidate);
                    result.fernsPlaced++;
                } else {
                    world.setShortGrass(candidate);
                    result.shortGrassPlaced++;
                }
            }
        }
    }

    private static void placeFlowerPatch(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, int globalIndex, int rarityChance,
            int xzSpread, int ySpread, boolean blueOrchid,
            Set<V118Biome> featureBiomes, DecorationResult result) {
        V118WorldgenRandom random = featureRandom(worldSeed, chunkX, chunkZ,
            globalIndex, VEGETAL_DECORATION_STEP);
        if (random.nextFloat() >= 1.0F / rarityChance) {
            return;
        }
        BlockPos origin = squareHeightmapPosition(world, random, chunkX, chunkZ);
        if (origin == null || !featureBiomes.contains(world.biomeAt(origin))) {
            return;
        }
        int horizontalBound = xzSpread + 1;
        int verticalBound = ySpread + 1;
        for (int attempt = 0; attempt < 64; ++attempt) {
            BlockPos candidate = origin.add(
                random.nextInt(horizontalBound) - random.nextInt(horizontalBound),
                random.nextInt(verticalBound) - random.nextInt(verticalBound),
                random.nextInt(horizontalBound) - random.nextInt(horizontalBound));
            if (!world.isFlowerPlacementAir(candidate)) {
                continue;
            }
            // onlyWhenEmpty filters before the provider. The shared warm/default provider
            // therefore consumes no RNG for occupied candidates.
            int flower;
            if (blueOrchid) {
                flower = 2;
            } else {
                flower = random.nextInt(3) < 2 ? 0 : 1;
            }
            if (!world.canFlowerSurvive(candidate)) {
                continue;
            }
            if (flower == 0) {
                world.setPoppy(candidate);
                result.poppiesPlaced++;
            } else if (flower == 1) {
                world.setDandelion(candidate);
                result.dandelionsPlaced++;
            } else {
                world.setBlueOrchid(candidate);
                result.blueOrchidsPlaced++;
            }
        }
    }

    private static void placeDeadBushPatch(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, int globalIndex, int outerAttempts,
            Set<V118Biome> featureBiomes, DecorationResult result) {
        V118WorldgenRandom random = featureRandom(worldSeed, chunkX, chunkZ,
            globalIndex, VEGETAL_DECORATION_STEP);
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        // CountPlacement's stream is lazy: finish each configured patch (and its random
        // consumption) before InSquarePlacement samples the next origin.
        for (int outer = 0; outer < outerAttempts; ++outer) {
            int x = originX + random.nextInt(16);
            int z = originZ + random.nextInt(16);
            int y = world.worldSurfaceHeight(x, z);
            if (y <= world.minBuildHeight()) {
                continue;
            }
            BlockPos origin = new BlockPos(x, y, z);
            if (!featureBiomes.contains(world.biomeAt(origin))) {
                continue;
            }
            for (int attempt = 0; attempt < 4; ++attempt) {
                BlockPos candidate = origin.add(random.nextInt(8) - random.nextInt(8),
                    random.nextInt(4) - random.nextInt(4),
                    random.nextInt(8) - random.nextInt(8));
                if (!world.isAir(candidate) || !world.canDeadBushSurvive(candidate)) {
                    continue;
                }
                world.setDeadBush(candidate);
                result.deadBushesPlaced++;
            }
        }
    }

    private static void placeWaterlilyPatch(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, DecorationResult result) {
        V118WorldgenRandom random = featureRandom(worldSeed, chunkX, chunkZ,
            PATCH_WATERLILY_INDEX, VEGETAL_DECORATION_STEP);
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        // CountPlacement is lazy: each accepted origin completes its configured patch before
        // InSquarePlacement samples the next origin from this feature stream.
        for (int outer = 0; outer < 4; ++outer) {
            int x = originX + random.nextInt(16);
            int z = originZ + random.nextInt(16);
            int y = world.worldSurfaceHeight(x, z);
            if (y <= world.minBuildHeight()) {
                continue;
            }
            BlockPos origin = new BlockPos(x, y, z);
            if (!WATERLILY_BIOMES.contains(world.biomeAt(origin))) {
                continue;
            }
            for (int attempt = 0; attempt < 10; ++attempt) {
                BlockPos candidate = origin.add(random.nextInt(8) - random.nextInt(8),
                    random.nextInt(4) - random.nextInt(4),
                    random.nextInt(8) - random.nextInt(8));
                if (!world.isWaterlilyPlacementAir(candidate)
                        || !world.canWaterlilySurvive(candidate)) {
                    continue;
                }
                world.setWaterlily(candidate);
                result.waterliliesPlaced++;
            }
        }
    }

    private static void placeMushroomPatch(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, int globalIndex, int outerAttempts,
            int rarityChance, boolean redMushroom, Set<V118Biome> featureBiomes,
            DecorationResult result) {
        V118WorldgenRandom random = featureRandom(worldSeed, chunkX, chunkZ,
            globalIndex, VEGETAL_DECORATION_STEP);
        // CountPlacement and every following modifier are lazy. In particular, old-growth
        // brown mushrooms rerun rarity, origin sampling, and the configured patch per count.
        for (int outer = 0; outer < outerAttempts; ++outer) {
            if (rarityChance > 0 && random.nextFloat() >= 1.0F / rarityChance) {
                continue;
            }
            BlockPos origin = squareHeightmapPosition(world, random, chunkX, chunkZ);
            if (origin == null || !featureBiomes.contains(world.biomeAt(origin))) {
                continue;
            }
            for (int attempt = 0; attempt < 96; ++attempt) {
                BlockPos candidate = origin.add(random.nextInt(8) - random.nextInt(8),
                    random.nextInt(4) - random.nextInt(4),
                    random.nextInt(8) - random.nextInt(8));
                if (!world.isMushroomPlacementAir(candidate)
                        || !world.canMushroomSurvive(candidate)) {
                    continue;
                }
                if (redMushroom) {
                    world.setRedMushroom(candidate);
                    result.redMushroomsPlaced++;
                } else {
                    world.setBrownMushroom(candidate);
                    result.brownMushroomsPlaced++;
                }
            }
        }
    }

    private static void placeSugarCanePatch(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, int globalIndex, int rarityChance,
            Set<V118Biome> featureBiomes, DecorationResult result) {
        V118WorldgenRandom random = featureRandom(worldSeed, chunkX, chunkZ,
            globalIndex, VEGETAL_DECORATION_STEP);
        if (rarityChance > 0 && random.nextFloat() >= 1.0F / rarityChance) {
            return;
        }
        BlockPos origin = squareHeightmapPosition(world, random, chunkX, chunkZ);
        if (origin == null
                || !featureBiomes.contains(world.biomeAt(origin))) {
            return;
        }
        for (int attempt = 0; attempt < 20; ++attempt) {
            BlockPos candidate = origin.add(random.nextInt(5) - random.nextInt(5),
                random.nextInt(1) - random.nextInt(1),
                random.nextInt(5) - random.nextInt(5));
            if (!world.isSugarCanePlacementAir(candidate)
                    || !world.canSugarCaneSurvive(candidate)
                    || !world.hasAdjacentWaterBelow(candidate)) {
                continue;
            }
            int height = 2 + random.nextInt(random.nextInt(3) + 1);
            int allowedHeight = height;
            for (int above = 1; above <= height; ++above) {
                if (!world.isSugarCanePlacementAir(candidate.up(above))) {
                    allowedHeight = above - 1;
                    break;
                }
            }
            for (int layer = 0; layer < allowedHeight; ++layer) {
                world.setSugarCane(candidate.up(layer));
                result.sugarCanePlaced++;
            }
        }
    }

    private static void placePumpkinPatch(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, DecorationResult result) {
        V118WorldgenRandom random = featureRandom(worldSeed, chunkX, chunkZ,
            PATCH_PUMPKIN_INDEX, VEGETAL_DECORATION_STEP);
        if (random.nextFloat() >= 1.0F / 300.0F) {
            return;
        }
        BlockPos origin = squareHeightmapPosition(world, random, chunkX, chunkZ);
        if (origin == null
                || !PUMPKIN_BIOMES.contains(world.biomeAt(origin))) {
            return;
        }
        for (int attempt = 0; attempt < 96; ++attempt) {
            BlockPos candidate = origin.add(random.nextInt(8) - random.nextInt(8),
                random.nextInt(4) - random.nextInt(4),
                random.nextInt(8) - random.nextInt(8));
            if (world.isAir(candidate) && world.isGrassBlock(candidate.down())) {
                world.setPumpkin(candidate);
                result.pumpkinsPlaced++;
            }
        }
    }

    private static void placeCactusPatch(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, int globalIndex, int rarityChance,
            Set<V118Biome> featureBiomes, DecorationResult result) {
        V118WorldgenRandom random = featureRandom(worldSeed, chunkX, chunkZ,
            globalIndex, VEGETAL_DECORATION_STEP);
        if (random.nextFloat() >= 1.0F / rarityChance) {
            return;
        }
        BlockPos origin = squareHeightmapPosition(world, random, chunkX, chunkZ);
        if (origin == null || !featureBiomes.contains(world.biomeAt(origin))) {
            return;
        }
        for (int attempt = 0; attempt < 10; ++attempt) {
            BlockPos candidate = origin.add(random.nextInt(8) - random.nextInt(8),
                random.nextInt(4) - random.nextInt(4),
                random.nextInt(8) - random.nextInt(8));
            if (!world.isCactusPlacementAir(candidate)
                    || !world.canCactusSurvive(candidate)) {
                continue;
            }
            int height = 1 + random.nextInt(random.nextInt(3) + 1);
            int allowedHeight = height;
            for (int above = 1; above <= height; ++above) {
                if (!world.isCactusPlacementAir(candidate.up(above))) {
                    allowedHeight = above - 1;
                    break;
                }
            }
            for (int layer = 0; layer < allowedHeight; ++layer) {
                world.setCactus(candidate.up(layer));
                result.cactusPlaced++;
            }
        }
    }

    private static void placeMelonPatch(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, int globalIndex, int rarityChance,
            Set<V118Biome> featureBiomes, DecorationResult result) {
        V118WorldgenRandom random = featureRandom(worldSeed, chunkX, chunkZ,
            globalIndex, VEGETAL_DECORATION_STEP);
        if (random.nextFloat() >= 1.0F / rarityChance) {
            return;
        }
        BlockPos origin = squareHeightmapPosition(world, random, chunkX, chunkZ);
        if (origin == null || !featureBiomes.contains(world.biomeAt(origin))) {
            return;
        }
        for (int attempt = 0; attempt < 64; ++attempt) {
            BlockPos candidate = origin.add(random.nextInt(8) - random.nextInt(8),
                random.nextInt(4) - random.nextInt(4),
                random.nextInt(8) - random.nextInt(8));
            if (!world.isMelonReplaceable(candidate)
                    || !world.isGrassBlock(candidate.down())) {
                continue;
            }
            world.setMelon(candidate);
            result.melonsPlaced++;
        }
    }

    private static void placeVines(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, DecorationResult result) {
        V118WorldgenRandom random = featureRandom(worldSeed, chunkX, chunkZ,
            VINES_INDEX, VEGETAL_DECORATION_STEP);
        for (int attempt = 0; attempt < 127; ++attempt) {
            int x = (chunkX << 4) + random.nextInt(16);
            int z = (chunkZ << 4) + random.nextInt(16);
            int y = 64 + random.nextInt(37);
            BlockPos origin = new BlockPos(x, y, z);
            if (!VINE_BIOMES.contains(world.biomeAt(origin))) {
                continue;
            }
            if (placeVine(world, x, y, z)) {
                result.vinesPlaced++;
            }
        }
    }

    private static boolean placeVine(WorldAccess world, int x, int y, int z) {
        if (!world.isAir(x, y, z)) {
            return false;
        }
        for (V118LushCaveFeature.Direction direction : VINE_DIRECTIONS) {
            if (direction == V118LushCaveFeature.Direction.DOWN) {
                continue;
            }
            int neighborX = x + direction.stepX();
            int neighborY = y + direction.stepY();
            int neighborZ = z + direction.stepZ();
            if (world.isAcceptableVineNeighbor(
                    neighborX, neighborY, neighborZ, direction)) {
                world.setVine(x, y, z, direction);
                return true;
            }
        }
        return false;
    }

    private static BlockPos squareHeightmapPosition(WorldAccess world, Random random,
            int chunkX, int chunkZ) {
        int x = (chunkX << 4) + random.nextInt(16);
        int z = (chunkZ << 4) + random.nextInt(16);
        int y = world.motionBlockingHeight(x, z);
        return y > world.minBuildHeight()
                ? new BlockPos(x, y, z) : null;
    }

    private static V118WorldgenRandom featureRandom(long worldSeed, int chunkX,
            int chunkZ, int globalIndex, int decorationStep) {
        V118WorldgenRandom random = new V118WorldgenRandom(0L);
        long decorationSeed = random.setDecorationSeed(worldSeed, chunkX << 4, chunkZ << 4);
        random.setFeatureSeed(decorationSeed, globalIndex, decorationStep);
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

    private static boolean appearsIn(Set<V118Biome> featureBiomes,
            Set<V118Biome> regionBiomes) {
        for (V118Biome biome : featureBiomes) {
            if (regionBiomes.contains(biome)) {
                return true;
            }
        }
        return false;
    }

    private static Set<V118Biome> immutableSet(V118Biome first, V118Biome... rest) {
        EnumSet<V118Biome> values = EnumSet.of(first, rest);
        return Collections.unmodifiableSet(values);
    }

    private static Set<V118Biome> allBiomesExcept(V118Biome... excluded) {
        EnumSet<V118Biome> values = EnumSet.allOf(V118Biome.class);
        for (V118Biome biome : excluded) {
            values.remove(biome);
        }
        return Collections.unmodifiableSet(values);
    }

    private static void requireArguments(WorldAccess world, Set<V118Biome> regionBiomes) {
        if (world == null || regionBiomes == null) {
            throw new NullPointerException("world and regionBiomes are required");
        }
    }

    public interface WorldAccess extends V118MountainTreeFeature.WorldAccess,
            V118BeeTreeFeature.WorldAccess, V118AcaciaTreeFeature.WorldAccess,
            V118SwampTreeFeature.WorldAccess {
        V118Biome biomeAt(BlockPos pos);

        int worldSurfaceHeight(int blockX, int blockZ);

        int oceanFloorHeight(int blockX, int blockZ);

        int motionBlockingHeight(int blockX, int blockZ);

        int blockLight(BlockPos pos);

        boolean isAir(BlockPos pos);

        default boolean isAir(int blockX, int blockY, int blockZ) {
            return isAir(new BlockPos(blockX, blockY, blockZ));
        }

        boolean isWater(BlockPos pos);

        boolean isPowderSnow(BlockPos pos);

        boolean isSnowTreeSupport(BlockPos pos);

        default boolean supportsForestRockPlacement() {
            return false;
        }

        default boolean isForestRockGround(BlockPos pos) {
            return false;
        }

        default void setMossyCobblestone(BlockPos pos) {
            throw new UnsupportedOperationException("Forest rocks are not available");
        }

        default boolean canSpruceSaplingSurvive(BlockPos pos) {
            return false;
        }

        default boolean canBroadleafSaplingSurvive(BlockPos pos) {
            return canSpruceSaplingSurvive(pos);
        }

        default boolean supportsBroadleafTreePlacement() {
            return false;
        }

        default boolean supportsAcaciaTreePlacement() {
            return false;
        }

        @Override
        default void setAcaciaLog(BlockPos pos) {
            throw new UnsupportedOperationException("Acacia logs are not available");
        }

        @Override
        default void setAcaciaLeaves(BlockPos pos) {
            throw new UnsupportedOperationException("Acacia leaves are not available");
        }

        @Override
        default boolean isDirtExceptGrassAndMycelium(BlockPos pos) {
            return false;
        }

        @Override
        default void setLog(BlockPos pos, V118BeeTreeFeature.LogAxis axis,
                TreeKind kind) {
            throw new UnsupportedOperationException("Broadleaf logs are not available");
        }

        @Override
        default void setLeaves(BlockPos pos, TreeKind kind) {
            throw new UnsupportedOperationException("Broadleaf leaves are not available");
        }

        boolean isFrozenSpringValid(BlockPos pos);

        boolean isGrassBlock(BlockPos pos);

        boolean canDeadBushSurvive(BlockPos pos);

        boolean canSugarCaneSurvive(BlockPos pos);

        boolean isSugarCanePlacementAir(BlockPos pos);

        default boolean isDoublePlantPlacementAir(BlockPos pos) {
            return false;
        }

        default boolean canDoublePlantSurvive(BlockPos pos) {
            return false;
        }

        default boolean isDoublePlantUpperEmpty(BlockPos pos) {
            return false;
        }

        default boolean supportsDoublePlantPlacement() {
            return false;
        }

        default boolean isShortGrassPlacementAir(BlockPos pos) {
            return false;
        }

        default boolean canShortGrassSurvive(BlockPos pos) {
            return false;
        }

        default boolean isPodzol(BlockPos pos) {
            return false;
        }

        default boolean supportsShortGrassPlacement() {
            return false;
        }

        default boolean isFlowerPlacementAir(BlockPos pos) {
            return false;
        }

        default boolean canFlowerSurvive(BlockPos pos) {
            return false;
        }

        default boolean supportsFlowerPlacement() {
            return false;
        }

        default boolean isMushroomPlacementAir(BlockPos pos) {
            return false;
        }

        default boolean canMushroomSurvive(BlockPos pos) {
            return false;
        }

        default boolean supportsMushroomPlacement() {
            return false;
        }

        default boolean isWaterlilyPlacementAir(BlockPos pos) {
            return false;
        }

        default boolean canWaterlilySurvive(BlockPos pos) {
            return false;
        }

        default boolean supportsWaterlilyPlacement() {
            return false;
        }

        default boolean isCactusPlacementAir(BlockPos pos) {
            return isSugarCanePlacementAir(pos);
        }

        default boolean canCactusSurvive(BlockPos pos) {
            return false;
        }

        default boolean supportsCactusPlacement() {
            return false;
        }

        default boolean supportsVinePlacement() {
            return false;
        }

        default boolean isAcceptableVineNeighbor(int blockX, int blockY, int blockZ,
                V118LushCaveFeature.Direction attachmentDirection) {
            return false;
        }

        default void setVine(int blockX, int blockY, int blockZ,
                V118LushCaveFeature.Direction attachmentDirection) {
            throw new UnsupportedOperationException("Vine placement is not available");
        }

        default boolean isMelonReplaceable(BlockPos pos) {
            return false;
        }

        default boolean supportsMelonPlacement() {
            return false;
        }

        boolean hasAdjacentWaterBelow(BlockPos pos);

        boolean canSnowSurvive(BlockPos pos);

        void setLava(BlockPos pos);

        void scheduleLavaTick(BlockPos pos);

        void setIce(BlockPos pos);

        void setSnowLayer(BlockPos pos);

        void setDeadBush(BlockPos pos);

        void setSugarCane(BlockPos pos);

        default void setTallGrass(BlockPos pos) {
            throw new UnsupportedOperationException("Tall-grass placement is not available");
        }

        default void setLargeFern(BlockPos pos) {
            throw new UnsupportedOperationException("Large-fern placement is not available");
        }

        default void setShortGrass(BlockPos pos) {
            throw new UnsupportedOperationException("Short-grass placement is not available");
        }

        default void setFern(BlockPos pos) {
            throw new UnsupportedOperationException("Fern placement is not available");
        }

        default void setPoppy(BlockPos pos) {
            throw new UnsupportedOperationException("Poppy placement is not available");
        }

        default void setDandelion(BlockPos pos) {
            throw new UnsupportedOperationException("Dandelion placement is not available");
        }

        default void setBlueOrchid(BlockPos pos) {
            throw new UnsupportedOperationException("Blue-orchid placement is not available");
        }

        default void setBrownMushroom(BlockPos pos) {
            throw new UnsupportedOperationException("Brown mushroom placement is not available");
        }

        default void setRedMushroom(BlockPos pos) {
            throw new UnsupportedOperationException("Red mushroom placement is not available");
        }

        default void setWaterlily(BlockPos pos) {
            throw new UnsupportedOperationException("Waterlily placement is not available");
        }

        default void setCactus(BlockPos pos) {
            throw new UnsupportedOperationException("Cactus placement is not available");
        }

        default void setMelon(BlockPos pos) {
            throw new UnsupportedOperationException("Melon placement is not available");
        }

        void setPumpkin(BlockPos pos);
    }

    public static final class DecorationResult {
        private int forestRockAttempts;
        private int forestRocksPlaced;
        private int frozenSpringAttempts;
        private int frozenSpringsPlaced;
        private int treesPlaced;
        private int pinesPlaced;
        private int sprucesPlaced;
        private int oaksPlaced;
        private int fancyOaksPlaced;
        private int acaciasPlaced;
        private int logsPlaced;
        private int leavesPlaced;
        private int tallGrassPlaced;
        private int largeFernsPlaced;
        private int shortGrassPlaced;
        private int fernsPlaced;
        private int poppiesPlaced;
        private int dandelionsPlaced;
        private int blueOrchidsPlaced;
        private int deadBushesPlaced;
        private int brownMushroomsPlaced;
        private int redMushroomsPlaced;
        private int waterliliesPlaced;
        private int sugarCanePlaced;
        private int cactusPlaced;
        private int vinesPlaced;
        private int melonsPlaced;
        private int pumpkinsPlaced;
        private int waterFrozen;
        private int snowLayersPlaced;

        public int forestRockAttempts() {
            return forestRockAttempts;
        }

        public int forestRocksPlaced() {
            return forestRocksPlaced;
        }

        public int frozenSpringAttempts() {
            return frozenSpringAttempts;
        }

        public int frozenSpringsPlaced() {
            return frozenSpringsPlaced;
        }

        public int treesPlaced() {
            return treesPlaced;
        }

        public int pinesPlaced() {
            return pinesPlaced;
        }

        public int sprucesPlaced() {
            return sprucesPlaced;
        }

        public int oaksPlaced() {
            return oaksPlaced;
        }

        public int fancyOaksPlaced() {
            return fancyOaksPlaced;
        }

        public int acaciasPlaced() {
            return acaciasPlaced;
        }

        public int logsPlaced() {
            return logsPlaced;
        }

        public int leavesPlaced() {
            return leavesPlaced;
        }

        public int tallGrassPlaced() {
            return tallGrassPlaced;
        }

        public int largeFernsPlaced() {
            return largeFernsPlaced;
        }

        public int shortGrassPlaced() {
            return shortGrassPlaced;
        }

        public int fernsPlaced() {
            return fernsPlaced;
        }

        public int poppiesPlaced() {
            return poppiesPlaced;
        }

        public int dandelionsPlaced() {
            return dandelionsPlaced;
        }

        public int blueOrchidsPlaced() {
            return blueOrchidsPlaced;
        }

        public int deadBushesPlaced() {
            return deadBushesPlaced;
        }

        public int brownMushroomsPlaced() {
            return brownMushroomsPlaced;
        }

        public int redMushroomsPlaced() {
            return redMushroomsPlaced;
        }

        public int waterliliesPlaced() {
            return waterliliesPlaced;
        }

        public int sugarCanePlaced() {
            return sugarCanePlaced;
        }

        public int cactusPlaced() {
            return cactusPlaced;
        }

        public int vinesPlaced() {
            return vinesPlaced;
        }

        public int melonsPlaced() {
            return melonsPlaced;
        }

        public int pumpkinsPlaced() {
            return pumpkinsPlaced;
        }

        public int waterFrozen() {
            return waterFrozen;
        }

        public int snowLayersPlaced() {
            return snowLayersPlaced;
        }
    }
}

package net.celestiald.cavesnotcliffs.worldgen.v118;

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
    public static final int FLUID_SPRINGS_STEP = 8;
    public static final int VEGETAL_DECORATION_STEP = 9;
    public static final int TOP_LAYER_MODIFICATION_STEP = 10;

    public static final int SPRING_LAVA_FROZEN_INDEX = 2;
    public static final int TREES_GROVE_INDEX = 40;
    public static final int PATCH_DEAD_BUSH_INDEX = 51;
    public static final int PATCH_DEAD_BUSH_2_INDEX = 58;
    public static final int PATCH_DEAD_BUSH_BADLANDS_INDEX = 59;
    public static final int PATCH_SUGAR_CANE_DESERT_INDEX = 62;
    public static final int PATCH_SUGAR_CANE_BADLANDS_INDEX = 63;
    public static final int PATCH_SUGAR_CANE_SWAMP_INDEX = 64;
    public static final int PATCH_SUGAR_CANE_INDEX = 68;
    public static final int PATCH_PUMPKIN_INDEX = 69;
    public static final int FREEZE_TOP_LAYER_INDEX = 0;

    // Every registered Java 1.18.2 Overworld biome uses globalOverworldGeneration,
    // which installs the shared freeze_top_layer placed feature.
    private static final Set<V118Biome> FREEZE_TOP_LAYER_BIOMES =
        Collections.unmodifiableSet(EnumSet.allOf(V118Biome.class));
    private static final Set<V118Biome> FROZEN_SPRING_BIOMES = immutableSet(
        V118Biome.GROVE, V118Biome.SNOWY_SLOPES,
        V118Biome.FROZEN_PEAKS, V118Biome.JAGGED_PEAKS);
    private static final Set<V118Biome> DEAD_BUSH_BIOMES = immutableSet(
        V118Biome.OLD_GROWTH_PINE_TAIGA,
        V118Biome.OLD_GROWTH_SPRUCE_TAIGA, V118Biome.SWAMP);
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

    private V118MountainSurfacePlacements() {
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
        if (regionBiomes.contains(V118Biome.GROVE)) {
            placeGroveTrees(world, worldSeed, chunkX, chunkZ, result);
        }
        if (appearsIn(DEAD_BUSH_BIOMES, regionBiomes)) {
            placeDeadBushPatch(world, worldSeed, chunkX, chunkZ,
                PATCH_DEAD_BUSH_INDEX, 1, DEAD_BUSH_BIOMES, result);
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
        if (appearsIn(ORDINARY_SUGAR_CANE_BIOMES, regionBiomes)) {
            placeSugarCanePatch(world, worldSeed, chunkX, chunkZ,
                PATCH_SUGAR_CANE_INDEX, 6,
                ORDINARY_SUGAR_CANE_BIOMES, result);
        }
        if (appearsIn(PUMPKIN_BIOMES, regionBiomes)) {
            placePumpkinPatch(world, worldSeed, chunkX, chunkZ, result);
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

    static boolean supportsGroveTrees(V118Biome biome) {
        return biome == V118Biome.GROVE;
    }

    static boolean supportsDeadBush2(V118Biome biome) {
        return DEAD_BUSH_2_BIOMES.contains(biome);
    }

    static boolean supportsDeadBush(V118Biome biome) {
        return DEAD_BUSH_BIOMES.contains(biome);
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
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        for (int attempt = 0; attempt < attempts; ++attempt) {
            int x = originX + random.nextInt(16);
            int z = originZ + random.nextInt(16);
            if (world.worldSurfaceHeight(x, z) - world.oceanFloorHeight(x, z) > 0) {
                continue;
            }
            int y = world.oceanFloorHeight(x, z);
            if (y <= world.minBuildHeight()
                    || world.biomeAt(new BlockPos(x, y, z)) != V118Biome.GROVE) {
                continue;
            }

            Kind kind = random.nextFloat() < 0.33333334F ? Kind.PINE : Kind.SPRUCE;
            BlockPos treeOrigin = scanAbovePowderSnow(world, new BlockPos(x, y, z));
            if (treeOrigin == null || !world.isSnowTreeSupport(treeOrigin.down())) {
                continue;
            }
            Result tree = V118MountainTreeFeature.place(world, random, treeOrigin, kind);
            if (!tree.placed()) {
                continue;
            }
            result.treesPlaced++;
            result.logsPlaced += tree.logs();
            result.leavesPlaced += tree.leaves();
            if (kind == Kind.PINE) {
                result.pinesPlaced++;
            } else {
                result.sprucesPlaced++;
            }
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

    public interface WorldAccess extends V118MountainTreeFeature.WorldAccess {
        V118Biome biomeAt(BlockPos pos);

        int worldSurfaceHeight(int blockX, int blockZ);

        int oceanFloorHeight(int blockX, int blockZ);

        int motionBlockingHeight(int blockX, int blockZ);

        int blockLight(BlockPos pos);

        boolean isAir(BlockPos pos);

        boolean isWater(BlockPos pos);

        boolean isPowderSnow(BlockPos pos);

        boolean isSnowTreeSupport(BlockPos pos);

        boolean isFrozenSpringValid(BlockPos pos);

        boolean isGrassBlock(BlockPos pos);

        boolean canDeadBushSurvive(BlockPos pos);

        boolean canSugarCaneSurvive(BlockPos pos);

        boolean isSugarCanePlacementAir(BlockPos pos);

        boolean hasAdjacentWaterBelow(BlockPos pos);

        boolean canSnowSurvive(BlockPos pos);

        void setLava(BlockPos pos);

        void scheduleLavaTick(BlockPos pos);

        void setIce(BlockPos pos);

        void setSnowLayer(BlockPos pos);

        void setDeadBush(BlockPos pos);

        void setSugarCane(BlockPos pos);

        void setPumpkin(BlockPos pos);
    }

    public static final class DecorationResult {
        private int frozenSpringAttempts;
        private int frozenSpringsPlaced;
        private int treesPlaced;
        private int pinesPlaced;
        private int sprucesPlaced;
        private int logsPlaced;
        private int leavesPlaced;
        private int deadBushesPlaced;
        private int sugarCanePlaced;
        private int pumpkinsPlaced;
        private int waterFrozen;
        private int snowLayersPlaced;

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

        public int logsPlaced() {
            return logsPlaced;
        }

        public int leavesPlaced() {
            return leavesPlaced;
        }

        public int deadBushesPlaced() {
            return deadBushesPlaced;
        }

        public int sugarCanePlaced() {
            return sugarCanePlaced;
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

package net.celestiald.cavesnotcliffs.worldgen.v118;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118BeeTreePlacements.TreeKind;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118MushroomIslandVegetationFeature.MushroomKind;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Exact Java 1.18.2 {@code dark_forest_vegetation} placed feature. */
public final class V118DarkForestVegetationFeature {
    public static final int VEGETAL_DECORATION_STEP = 9;
    public static final int DARK_FOREST_VEGETATION_INDEX = 13;

    private V118DarkForestVegetationFeature() {
    }

    public static Result decorate(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ) {
        if (world == null) {
            throw new NullPointerException("Dark forest world is required");
        }
        V118WorldgenRandom random = new V118WorldgenRandom(0L);
        long decorationSeed = random.setDecorationSeed(
                worldSeed, chunkX << 4, chunkZ << 4);
        random.setFeatureSeed(decorationSeed, DARK_FOREST_VEGETATION_INDEX,
                VEGETAL_DECORATION_STEP);
        Result result = new Result();
        for (int attempt = 0; attempt < 16; ++attempt) {
            int x = (chunkX << 4) + random.nextInt(16);
            int z = (chunkZ << 4) + random.nextInt(16);
            int oceanFloor = world.oceanFloorHeight(x, z);
            if (world.worldSurfaceHeight(x, z) - oceanFloor > 0) {
                continue;
            }
            BlockPos origin = new BlockPos(x, world.oceanFloorHeight(x, z), z);
            if (world.biomeAt(origin) != V118Biome.DARK_FOREST) {
                continue;
            }
            placeSelected(world, random, origin, result);
        }
        return result;
    }

    private static void placeSelected(WorldAccess world, Random random,
            BlockPos origin, Result result) {
        if (random.nextFloat() < 0.025F) {
            recordMushroom(V118MushroomIslandVegetationFeature.place(
                    world, random, origin, MushroomKind.BROWN), result);
            return;
        }
        if (random.nextFloat() < 0.05F) {
            recordMushroom(V118MushroomIslandVegetationFeature.place(
                    world, random, origin, MushroomKind.RED), result);
            return;
        }
        if (random.nextFloat() < 0.6666667F) {
            if (world.canDarkOakSaplingSurvive(origin)) {
                recordDarkOak(placeDarkOak(world, random, origin), result);
            }
            return;
        }
        TreeKind kind;
        if (random.nextFloat() < 0.2F) {
            kind = TreeKind.BIRCH;
        } else if (random.nextFloat() < 0.1F) {
            kind = TreeKind.FANCY_OAK;
        } else {
            kind = TreeKind.OAK;
        }
        if (!world.canBroadleafSaplingSurvive(origin)) {
            return;
        }
        V118BeeTreeFeature.Result tree = V118BeeTreeFeature.place(
                world, random, origin, kind);
        if (!tree.placed()) {
            return;
        }
        result.trees++;
        result.logs += tree.trunks().size()
                - (tree.trunks().contains(origin.down()) ? 1 : 0);
        result.leaves += tree.foliage().size();
    }

    private static DarkOakResult placeDarkOak(WorldAccess world, Random random,
            BlockPos origin) {
        int height = 6 + random.nextInt(3) + random.nextInt(2);
        if (origin.getY() < world.minBuildHeight() + 1
                || origin.getY() + height + 1 > world.maxBuildHeight()) {
            return DarkOakResult.NOT_PLACED;
        }
        int freeHeight = maxFreeHeight(world, origin, height);
        if (freeHeight < height) {
            return DarkOakResult.NOT_PLACED;
        }

        Set<BlockPos> logs = new HashSet<>();
        Set<BlockPos> leaves = new HashSet<>();
        BlockPos ground = origin.down();
        setDirt(world, ground);
        setDirt(world, ground.east());
        setDirt(world, ground.south());
        setDirt(world, ground.east().south());

        int direction = random.nextInt(4); // north, east, south, west
        int stepX = direction == 1 ? 1 : direction == 3 ? -1 : 0;
        int stepZ = direction == 0 ? -1 : direction == 2 ? 1 : 0;
        int bendStart = height - random.nextInt(4);
        int bendLength = 2 - random.nextInt(3);
        int trunkX = origin.getX();
        int trunkZ = origin.getZ();
        int canopyY = origin.getY() + height - 1;
        for (int layer = 0; layer < height; ++layer) {
            if (layer >= bendStart && bendLength > 0) {
                trunkX += stepX;
                trunkZ += stepZ;
                --bendLength;
            }
            BlockPos trunk = new BlockPos(trunkX, origin.getY() + layer, trunkZ);
            if (!world.isAirOrLeaves(trunk)) {
                continue;
            }
            placeDarkOakLog(world, trunk, logs);
            placeDarkOakLog(world, trunk.east(), logs);
            placeDarkOakLog(world, trunk.south(), logs);
            placeDarkOakLog(world, trunk.east().south(), logs);
        }

        BlockPos primaryAttachment = new BlockPos(trunkX, canopyY, trunkZ);
        List<BlockPos> branchAttachments = new ArrayList<>();
        for (int dx = -1; dx <= 2; ++dx) {
            for (int dz = -1; dz <= 2; ++dz) {
                if (dx >= 0 && dx <= 1 && dz >= 0 && dz <= 1
                        || random.nextInt(3) > 0) {
                    continue;
                }
                int branchHeight = random.nextInt(3) + 2;
                for (int layer = 0; layer < branchHeight; ++layer) {
                    placeDarkOakLog(world, new BlockPos(origin.getX() + dx,
                            canopyY - layer - 1, origin.getZ() + dz), logs);
                }
                branchAttachments.add(
                        new BlockPos(trunkX + dx, canopyY, trunkZ + dz));
            }
        }
        // TreeFeature finishes the entire trunk placer before foliage RNG begins.
        placeDarkOakFoliage(world, random, primaryAttachment, true, leaves);
        for (BlockPos attachment : branchAttachments) {
            placeDarkOakFoliage(world, random, attachment, false, leaves);
        }
        return logs.isEmpty() && leaves.isEmpty()
                ? DarkOakResult.NOT_PLACED
                : new DarkOakResult(true, logs.size(), leaves.size());
    }

    private static int maxFreeHeight(WorldAccess world, BlockPos origin,
            int height) {
        for (int layer = 0; layer <= height + 1; ++layer) {
            int radius = layer < 1 ? 0 : layer >= height - 1 ? 2 : 1;
            for (int dx = -radius; dx <= radius; ++dx) {
                for (int dz = -radius; dz <= radius; ++dz) {
                    if (!world.isFree(origin.add(dx, layer, dz))) {
                        return layer - 2;
                    }
                }
            }
        }
        return height;
    }

    private static void placeDarkOakFoliage(WorldAccess world, Random random,
            BlockPos attachment, boolean doubleTrunk, Set<BlockPos> leaves) {
        if (doubleTrunk) {
            placeLeavesRow(world, attachment, 2, -1, true, leaves);
            placeLeavesRow(world, attachment, 3, 0, true, leaves);
            placeLeavesRow(world, attachment, 2, 1, true, leaves);
            if (random.nextBoolean()) {
                placeLeavesRow(world, attachment, 0, 2, true, leaves);
            }
        } else {
            placeLeavesRow(world, attachment, 2, -1, false, leaves);
            placeLeavesRow(world, attachment, 1, 0, false, leaves);
        }
    }

    private static void placeLeavesRow(WorldAccess world, BlockPos center,
            int radius, int y, boolean doubleTrunk, Set<BlockPos> leaves) {
        int extra = doubleTrunk ? 1 : 0;
        for (int dx = -radius; dx <= radius + extra; ++dx) {
            for (int dz = -radius; dz <= radius + extra; ++dz) {
                if (y == 0 && doubleTrunk
                        && (dx == -radius || dx >= radius)
                        && (dz == -radius || dz >= radius)) {
                    continue;
                }
                int absX = doubleTrunk
                        ? Math.min(Math.abs(dx), Math.abs(dx - 1)) : Math.abs(dx);
                int absZ = doubleTrunk
                        ? Math.min(Math.abs(dz), Math.abs(dz - 1)) : Math.abs(dz);
                if (skipLeaf(absX, y, absZ, radius, doubleTrunk)) {
                    continue;
                }
                BlockPos pos = center.add(dx, y, dz);
                if (world.isValidTreePos(pos)) {
                    world.setDarkOakLeaves(pos);
                    leaves.add(pos.toImmutable());
                }
            }
        }
    }

    private static boolean skipLeaf(int x, int y, int z, int radius,
            boolean doubleTrunk) {
        if (y == -1 && !doubleTrunk) {
            return x == radius && z == radius;
        }
        return y == 1 && x + z > radius * 2 - 2;
    }

    private static void setDirt(WorldAccess world, BlockPos pos) {
        if (!world.isDirtExceptGrassAndMycelium(pos)) {
            world.setDirt(pos);
        }
    }

    private static void placeDarkOakLog(WorldAccess world, BlockPos pos,
            Set<BlockPos> logs) {
        if (world.isValidTreePos(pos)) {
            world.setDarkOakLog(pos);
            logs.add(pos.toImmutable());
        }
    }

    private static void recordMushroom(
            V118MushroomIslandVegetationFeature.Result mushroom, Result result) {
        if (mushroom.placed()) {
            result.mushrooms++;
            result.mushroomBlocks += mushroom.capBlocks() + mushroom.stemBlocks();
        }
    }

    private static void recordDarkOak(DarkOakResult tree, Result result) {
        if (tree.placed) {
            result.trees++;
            result.darkOaks++;
            result.logs += tree.logs;
            result.leaves += tree.leaves;
        }
    }

    public interface WorldAccess extends V118BeeTreeFeature.WorldAccess,
            V118MushroomIslandVegetationFeature.WorldAccess {
        int oceanFloorHeight(int blockX, int blockZ);

        int worldSurfaceHeight(int blockX, int blockZ);

        V118Biome biomeAt(BlockPos pos);

        boolean canDarkOakSaplingSurvive(BlockPos pos);

        boolean canBroadleafSaplingSurvive(BlockPos pos);

        boolean isAirOrLeaves(BlockPos pos);

        void setDarkOakLog(BlockPos pos);

        void setDarkOakLeaves(BlockPos pos);
    }

    public static final class Result {
        private int trees;
        private int darkOaks;
        private int mushrooms;
        private int logs;
        private int leaves;
        private int mushroomBlocks;

        public int trees() { return trees; }
        public int darkOaks() { return darkOaks; }
        public int mushrooms() { return mushrooms; }
        public int logs() { return logs; }
        public int leaves() { return leaves; }
        public int mushroomBlocks() { return mushroomBlocks; }
    }

    private static final class DarkOakResult {
        static final DarkOakResult NOT_PLACED = new DarkOakResult(false, 0, 0);
        final boolean placed;
        final int logs;
        final int leaves;

        DarkOakResult(boolean placed, int logs, int leaves) {
            this.placed = placed;
            this.logs = logs;
            this.leaves = leaves;
        }
    }
}

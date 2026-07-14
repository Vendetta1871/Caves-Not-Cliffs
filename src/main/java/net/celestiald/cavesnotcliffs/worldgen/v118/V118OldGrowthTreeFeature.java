package net.celestiald.cavesnotcliffs.worldgen.v118;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Java 1.18.2 old-growth pine and spruce taiga tree placements. */
public final class V118OldGrowthTreeFeature {
    public static final int PINE_GLOBAL_INDEX = 37;
    public static final int SPRUCE_GLOBAL_INDEX = 38;
    private static final int VEGETAL_DECORATION_STEP = 9;

    private V118OldGrowthTreeFeature() {
    }

    /** Places the complete step/index feature, including count and terrain/biome filters. */
    public static Result place(WorldAccess world, long worldSeed, int chunkX, int chunkZ,
            Family family) {
        if (world == null || family == null) {
            throw new NullPointerException("Old-growth tree feature arguments");
        }
        V118WorldgenRandom random = new V118WorldgenRandom(0L);
        long decorationSeed = random.setDecorationSeed(worldSeed, chunkX << 4, chunkZ << 4);
        random.setFeatureSeed(decorationSeed, family.globalIndex,
                VEGETAL_DECORATION_STEP);

        Result result = new Result(family);
        int attempts = 10 + (random.nextInt(10) == 9 ? 1 : 0);
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        for (int attempt = 0; attempt < attempts; ++attempt) {
            result.attempts++;
            int x = originX + random.nextInt(16);
            int z = originZ + random.nextInt(16);
            int oceanFloor = world.oceanFloorHeight(x, z);
            if (world.worldSurfaceHeight(x, z) - oceanFloor > 0) {
                continue;
            }
            // HEIGHTMAP_OCEAN_FLOOR runs after SurfaceWaterDepthFilter's own
            // OCEAN_FLOOR -> WORLD_SURFACE queries.
            BlockPos origin = new BlockPos(x, world.oceanFloorHeight(x, z), z);
            if (world.biomeAt(origin) != family.biome) {
                continue;
            }
            placeSelected(world, random, origin, family, result);
        }
        return result;
    }

    /** Places one configured selector at an already filtered tree origin. */
    public static boolean placeSelected(WorldAccess world, Random random, BlockPos origin,
            Family family) {
        if (world == null || random == null || origin == null || family == null) {
            throw new NullPointerException("Old-growth selected tree arguments");
        }
        return placeSelected(world, random, origin, family, null);
    }

    private static boolean placeSelected(WorldAccess world, Random random, BlockPos origin,
            Family family, Result result) {
        Tree tree = family.select(random);
        if (!world.canSpruceSaplingSurvive(origin)) {
            return false;
        }

        boolean placed;
        if (tree == Tree.MEGA_PINE || tree == Tree.MEGA_SPRUCE) {
            MegaResult mega = placeMega(world, random, origin, tree);
            placed = mega.placed;
            if (result != null && placed) {
                result.megaTrees++;
                result.logs += mega.logs;
                result.leaves += mega.leaves;
                result.podzol += mega.podzol;
            }
        } else {
            V118MountainTreeFeature.Kind kind = tree == Tree.PINE
                    ? V118MountainTreeFeature.Kind.PINE
                    : V118MountainTreeFeature.Kind.SPRUCE;
            V118MountainTreeFeature.Result ordinary =
                    V118MountainTreeFeature.place(world, random, origin, kind);
            placed = ordinary.placed();
            if (result != null && placed) {
                result.ordinaryTrees++;
                result.logs += ordinary.logs();
                result.leaves += ordinary.leaves();
            }
        }
        if (result != null && placed) {
            result.trees++;
        }
        return placed;
    }

    private static MegaResult placeMega(WorldAccess world, Random random, BlockPos origin,
            Tree tree) {
        int height = 13 + random.nextInt(3) + random.nextInt(15);
        int foliageHeight = tree == Tree.MEGA_SPRUCE
                ? 13 + random.nextInt(5) : 3 + random.nextInt(5);
        if (origin.getY() < world.minBuildHeight() + 1
                || origin.getY() + height + 1 > world.maxBuildHeight()
                || maxFreeTreeHeight(world, height, origin) < height) {
            return MegaResult.NOT_PLACED;
        }

        Set<BlockPos> trunkPositions = new HashSet<>();
        int logs = 0;
        BlockPos dirtOrigin = origin.down();
        setDirtAt(world, dirtOrigin, trunkPositions);
        setDirtAt(world, dirtOrigin.east(), trunkPositions);
        setDirtAt(world, dirtOrigin.south(), trunkPositions);
        setDirtAt(world, dirtOrigin.east().south(), trunkPositions);
        for (int y = 0; y < height; ++y) {
            logs += placeLogIfFree(world, origin.add(0, y, 0), trunkPositions);
            if (y >= height - 1) {
                continue;
            }
            logs += placeLogIfFree(world, origin.add(1, y, 0), trunkPositions);
            logs += placeLogIfFree(world, origin.add(1, y, 1), trunkPositions);
            logs += placeLogIfFree(world, origin.add(0, y, 1), trunkPositions);
        }

        int leaves = placeMegaFoliage(world, origin.up(height), foliageHeight);
        if (trunkPositions.isEmpty() && leaves == 0) {
            return MegaResult.NOT_PLACED;
        }
        int podzol = placePodzol(world, random, trunkPositions);
        return new MegaResult(true, logs, leaves, podzol);
    }

    private static int maxFreeTreeHeight(WorldAccess world, int height, BlockPos origin) {
        for (int layer = 0; layer <= height + 1; ++layer) {
            int radius = layer < 1 ? 1 : 2;
            for (int dx = -radius; dx <= radius; ++dx) {
                for (int dz = -radius; dz <= radius; ++dz) {
                    BlockPos position = origin.add(dx, layer, dz);
                    if (!world.isFree(position) || world.isVine(position)) {
                        return layer - 2;
                    }
                }
            }
        }
        return height;
    }

    private static void setDirtAt(WorldAccess world, BlockPos position,
            Set<BlockPos> trunkPositions) {
        if (world.isDirtExceptGrassAndMycelium(position)) {
            return;
        }
        world.setDirt(position);
        trunkPositions.add(position);
    }

    private static int placeLogIfFree(WorldAccess world, BlockPos position,
            Set<BlockPos> trunkPositions) {
        if (!world.isFree(position) || !world.isValidTreePos(position)) {
            return 0;
        }
        world.setSpruceLog(position);
        trunkPositions.add(position);
        return 1;
    }

    private static int placeMegaFoliage(WorldAccess world, BlockPos origin,
            int foliageHeight) {
        int previousRadius = 0;
        int leaves = 0;
        for (int y = origin.getY() - foliageHeight; y <= origin.getY(); ++y) {
            int distanceFromTop = origin.getY() - y;
            int radius = (int) Math.floor((float) distanceFromTop
                    / (float) foliageHeight * 3.5F);
            int effectiveRadius = distanceFromTop > 0 && radius == previousRadius
                    && (y & 1) == 0 ? radius + 1 : radius;
            leaves += placeMegaLeavesRow(world,
                    new BlockPos(origin.getX(), y, origin.getZ()), effectiveRadius);
            previousRadius = radius;
        }
        return leaves;
    }

    private static int placeMegaLeavesRow(WorldAccess world, BlockPos origin, int radius) {
        int leaves = 0;
        for (int dx = -radius; dx <= radius + 1; ++dx) {
            for (int dz = -radius; dz <= radius + 1; ++dz) {
                int x = Math.min(Math.abs(dx), Math.abs(dx - 1));
                int z = Math.min(Math.abs(dz), Math.abs(dz - 1));
                if (x + z >= 7 || x * x + z * z > radius * radius) {
                    continue;
                }
                BlockPos position = origin.add(dx, 0, dz);
                if (world.isValidTreePos(position)) {
                    world.setSpruceLeaves(position);
                    leaves++;
                }
            }
        }
        return leaves;
    }

    private static int placePodzol(WorldAccess world, Random random,
            Set<BlockPos> trunkPositions) {
        if (trunkPositions.isEmpty()) {
            return 0;
        }
        List<BlockPos> ordered = new ArrayList<>(trunkPositions);
        Collections.sort(ordered, Comparator.comparingInt(BlockPos::getY));
        int bottomY = ordered.get(0).getY();
        int placed = 0;
        for (BlockPos position : ordered) {
            if (position.getY() != bottomY) {
                continue;
            }
            placed += placePodzolCircle(world, position.add(-1, 0, -1));
            placed += placePodzolCircle(world, position.add(2, 0, -1));
            placed += placePodzolCircle(world, position.add(-1, 0, 2));
            placed += placePodzolCircle(world, position.add(2, 0, 2));
            for (int attempt = 0; attempt < 5; ++attempt) {
                int packed = random.nextInt(64);
                int x = packed % 8;
                int z = packed / 8;
                if (x == 0 || x == 7 || z == 0 || z == 7) {
                    placed += placePodzolCircle(world,
                            position.add(-3 + x, 0, -3 + z));
                }
            }
        }
        return placed;
    }

    private static int placePodzolCircle(WorldAccess world, BlockPos center) {
        int placed = 0;
        for (int dx = -2; dx <= 2; ++dx) {
            for (int dz = -2; dz <= 2; ++dz) {
                if (Math.abs(dx) == 2 && Math.abs(dz) == 2) {
                    continue;
                }
                placed += placePodzolColumn(world, center.add(dx, 0, dz));
            }
        }
        return placed;
    }

    private static int placePodzolColumn(WorldAccess world, BlockPos origin) {
        for (int offset = 2; offset >= -3; --offset) {
            BlockPos position = origin.up(offset);
            if (world.isGrassOrDirt(position)) {
                world.setPodzol(position);
                return 1;
            }
            if (!world.isAir(position) && offset < 0) {
                break;
            }
        }
        return 0;
    }

    public enum Family {
        OLD_GROWTH_SPRUCE_TAIGA(SPRUCE_GLOBAL_INDEX,
                V118Biome.OLD_GROWTH_SPRUCE_TAIGA),
        OLD_GROWTH_PINE_TAIGA(PINE_GLOBAL_INDEX,
                V118Biome.OLD_GROWTH_PINE_TAIGA);

        private final int globalIndex;
        private final V118Biome biome;

        Family(int globalIndex, V118Biome biome) {
            this.globalIndex = globalIndex;
            this.biome = biome;
        }

        Tree select(Random random) {
            if (this == OLD_GROWTH_SPRUCE_TAIGA) {
                if (random.nextFloat() < 0.33333334F) {
                    return Tree.MEGA_SPRUCE;
                }
                return random.nextFloat() < 0.33333334F ? Tree.PINE : Tree.SPRUCE;
            }
            if (random.nextFloat() < 0.025641026F) {
                return Tree.MEGA_SPRUCE;
            }
            if (random.nextFloat() < 0.30769232F) {
                return Tree.MEGA_PINE;
            }
            return random.nextFloat() < 0.33333334F ? Tree.PINE : Tree.SPRUCE;
        }
    }

    private enum Tree {
        MEGA_SPRUCE,
        MEGA_PINE,
        PINE,
        SPRUCE
    }

    public interface WorldAccess extends V118MountainTreeFeature.WorldAccess {
        V118Biome biomeAt(BlockPos pos);

        int worldSurfaceHeight(int blockX, int blockZ);

        int oceanFloorHeight(int blockX, int blockZ);

        boolean canSpruceSaplingSurvive(BlockPos pos);

        boolean isVine(BlockPos pos);

        boolean isDirtExceptGrassAndMycelium(BlockPos pos);

        boolean isGrassOrDirt(BlockPos pos);

        boolean isAir(BlockPos pos);

        void setPodzol(BlockPos pos);
    }

    public static final class Result {
        private final Family family;
        private int attempts;
        private int trees;
        private int megaTrees;
        private int ordinaryTrees;
        private int logs;
        private int leaves;
        private int podzol;

        private Result(Family family) {
            this.family = family;
        }

        public Family family() { return family; }
        public int attempts() { return attempts; }
        public int trees() { return trees; }
        public int megaTrees() { return megaTrees; }
        public int ordinaryTrees() { return ordinaryTrees; }
        public int logs() { return logs; }
        public int leaves() { return leaves; }
        public int podzol() { return podzol; }
    }

    private static final class MegaResult {
        static final MegaResult NOT_PLACED = new MegaResult(false, 0, 0, 0);

        final boolean placed;
        final int logs;
        final int leaves;
        final int podzol;

        MegaResult(boolean placed, int logs, int leaves, int podzol) {
            this.placed = placed;
            this.logs = logs;
            this.leaves = leaves;
            this.podzol = podzol;
        }
    }
}

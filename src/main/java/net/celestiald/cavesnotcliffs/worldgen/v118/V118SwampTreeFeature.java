package net.celestiald.cavesnotcliffs.worldgen.v118;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Exact Java 1.18.2 {@code trees_swamp} placement and {@code swamp_oak} shape. */
public final class V118SwampTreeFeature {
    public static final int GLOBAL_INDEX = 43;
    private static final int VEGETAL_DECORATION_STEP = 9;
    private static final Comparator<BlockPos> DECORATOR_ORDER =
            Comparator.comparingInt(BlockPos::getY);

    private V118SwampTreeFeature() {
    }

    /** Places the complete step-9/index-43 feature, including every placement filter. */
    public static Result place(WorldAccess world, long worldSeed, int chunkX, int chunkZ) {
        if (world == null) {
            throw new NullPointerException("Swamp tree world");
        }
        V118WorldgenRandom random = new V118WorldgenRandom(0L);
        long decorationSeed = random.setDecorationSeed(worldSeed, chunkX << 4, chunkZ << 4);
        random.setFeatureSeed(decorationSeed, GLOBAL_INDEX, VEGETAL_DECORATION_STEP);

        Result result = new Result();
        int attempts = 2 + (random.nextInt(10) == 9 ? 1 : 0);
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        for (int attempt = 0; attempt < attempts; ++attempt) {
            result.attempts++;
            int x = originX + random.nextInt(16);
            int z = originZ + random.nextInt(16);

            // SurfaceWaterDepthFilter deliberately queries OCEAN_FLOOR first.
            int oceanFloor = world.oceanFloorHeight(x, z);
            int worldSurface = world.worldSurfaceHeight(x, z);
            if (worldSurface - oceanFloor > 2) {
                continue;
            }
            // HeightmapPlacement performs a second OCEAN_FLOOR query.
            BlockPos origin = new BlockPos(x, world.oceanFloorHeight(x, z), z);
            if (world.biomeAt(origin) != V118Biome.SWAMP
                    || !world.canOakSaplingSurvive(origin)) {
                continue;
            }
            TreeResult tree = placeSelectedInternal(world, random, origin);
            if (tree.placed) {
                result.trees++;
                result.logs += tree.logs;
                result.leaves += tree.leaves;
                result.vines += tree.vines;
            }
        }
        return result;
    }

    /** Places one already-filtered {@code swamp_oak} configured feature. */
    public static boolean placeSelected(WorldAccess world, Random random, BlockPos origin) {
        if (world == null || random == null || origin == null) {
            throw new NullPointerException("Swamp oak arguments");
        }
        return placeSelectedInternal(world, random, origin).placed;
    }

    private static TreeResult placeSelectedInternal(WorldAccess world, Random random,
            BlockPos origin) {
        int height = 5 + random.nextInt(4) + random.nextInt(1);
        if (origin.getY() < world.minBuildHeight() + 1
                || origin.getY() + height + 1 > world.maxBuildHeight()
                || maxFreeTreeHeight(world, height, origin) < height) {
            return TreeResult.NOT_PLACED;
        }

        Set<BlockPos> trunks = new HashSet<>();
        Set<BlockPos> foliage = new HashSet<>();
        setDirtAt(world, origin.down(), trunks);
        for (int layer = 0; layer < height; ++layer) {
            BlockPos position = origin.up(layer);
            if (world.isValidTreePos(position)) {
                world.setOakLog(position);
                trunks.add(position.toImmutable());
            }
        }
        placeBlobFoliage(world, random, origin.up(height), foliage);
        if (trunks.isEmpty() && foliage.isEmpty()) {
            return TreeResult.NOT_PLACED;
        }

        int vines = placeLeafVines(world, random, foliage);
        return new TreeResult(true, trunks.size(), foliage.size(), vines);
    }

    private static int maxFreeTreeHeight(WorldAccess world, int height, BlockPos origin) {
        for (int layer = 0; layer <= height + 1; ++layer) {
            int radius = layer < 1 ? 0 : 1;
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
            Set<BlockPos> trunks) {
        if (!world.isDirtExceptGrassAndMycelium(position)) {
            world.setDirt(position);
            trunks.add(position.toImmutable());
        }
    }

    private static void placeBlobFoliage(WorldAccess world, Random random,
            BlockPos origin, Set<BlockPos> foliage) {
        for (int row = 0; row >= -3; --row) {
            int radius = Math.max(2 - row / 2, 0);
            for (int dx = -radius; dx <= radius; ++dx) {
                for (int dz = -radius; dz <= radius; ++dz) {
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius
                            && (random.nextInt(2) == 0 || row == 0)) {
                        continue;
                    }
                    BlockPos position = origin.add(dx, row, dz);
                    if (world.isValidTreePos(position)) {
                        world.setOakLeaves(position);
                        foliage.add(position.toImmutable());
                    }
                }
            }
        }
    }

    private static int placeLeafVines(WorldAccess world, Random random,
            Set<BlockPos> foliage) {
        List<BlockPos> ordered = new ArrayList<>(foliage);
        ordered.sort(DECORATOR_ORDER);
        int vines = 0;
        for (BlockPos leaf : ordered) {
            vines += tryHangingVine(world, random, leaf.west(), VineAttachment.EAST);
            vines += tryHangingVine(world, random, leaf.east(), VineAttachment.WEST);
            vines += tryHangingVine(world, random, leaf.north(), VineAttachment.SOUTH);
            vines += tryHangingVine(world, random, leaf.south(), VineAttachment.NORTH);
        }
        return vines;
    }

    private static int tryHangingVine(WorldAccess world, Random random,
            BlockPos position, VineAttachment attachment) {
        if (random.nextInt(4) != 0 || !world.isAir(position)) {
            return 0;
        }
        int placed = 0;
        world.setVine(position, attachment);
        placed++;
        position = position.down();
        for (int remaining = 4; remaining > 0 && world.isAir(position); --remaining) {
            world.setVine(position, attachment);
            placed++;
            position = position.down();
        }
        return placed;
    }

    /** The single vanilla vine face property set to true, not the occupied neighbor side. */
    public enum VineAttachment {
        NORTH,
        EAST,
        SOUTH,
        WEST
    }

    public interface WorldAccess {
        int minBuildHeight();

        int maxBuildHeight();

        int oceanFloorHeight(int blockX, int blockZ);

        int worldSurfaceHeight(int blockX, int blockZ);

        V118Biome biomeAt(BlockPos pos);

        /** Exact final placement filter: an oak sapling would survive at the origin. */
        boolean canOakSaplingSurvive(BlockPos pos);

        /** TreeFeature free predicate, including replaceable plants, water, and logs. */
        boolean isFree(BlockPos pos);

        boolean isVine(BlockPos pos);

        /** Trunk/foliage replacement predicate, including source and flowing water. */
        boolean isValidTreePos(BlockPos pos);

        boolean isDirtExceptGrassAndMycelium(BlockPos pos);

        boolean isAir(BlockPos pos);

        void setDirt(BlockPos pos);

        void setOakLog(BlockPos pos);

        void setOakLeaves(BlockPos pos);

        void setVine(BlockPos pos, VineAttachment attachment);
    }

    public static final class Result {
        private int attempts;
        private int trees;
        private int logs;
        private int leaves;
        private int vines;

        private Result() {
        }

        public int attempts() {
            return attempts;
        }

        public int trees() {
            return trees;
        }

        public int logs() {
            return logs;
        }

        public int leaves() {
            return leaves;
        }

        public int vines() {
            return vines;
        }
    }

    private static final class TreeResult {
        static final TreeResult NOT_PLACED = new TreeResult(false, 0, 0, 0);
        final boolean placed;
        final int logs;
        final int leaves;
        final int vines;

        TreeResult(boolean placed, int logs, int leaves, int vines) {
            this.placed = placed;
            this.logs = logs;
            this.leaves = leaves;
            this.vines = vines;
        }
    }
}

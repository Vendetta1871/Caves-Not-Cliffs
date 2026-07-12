package net.celestiald.cavesnotcliffs.worldgen.v118;

import net.minecraft.util.math.BlockPos;

import java.util.Random;
import java.util.Set;

/**
 * Java 1.18.2 {@code mushroom_island_vegetation}, including its configured
 * random selector and the two huge-mushroom feature bodies.
 */
public final class V118MushroomIslandVegetationFeature {
    public static final int VEGETAL_DECORATION_STEP = 9;
    public static final int MUSHROOM_ISLAND_VEGETATION_INDEX = 65;

    private V118MushroomIslandVegetationFeature() {
    }

    /** Runs the exact InSquare, WORLD_SURFACE_WG and BiomeFilter placement. */
    public static Result decorate(WorldAccess world, long worldSeed,
            int chunkX, int chunkZ, Set<V118Biome> regionBiomes) {
        requireArguments(world, regionBiomes);
        if (!regionBiomes.contains(V118Biome.MUSHROOM_FIELDS)) {
            return Result.NOT_PLACED;
        }

        V118WorldgenRandom random = new V118WorldgenRandom(0L);
        long decorationSeed = random.setDecorationSeed(
                worldSeed, chunkX << 4, chunkZ << 4);
        random.setFeatureSeed(decorationSeed,
                MUSHROOM_ISLAND_VEGETATION_INDEX, VEGETAL_DECORATION_STEP);

        int x = (chunkX << 4) + random.nextInt(16);
        int z = (chunkZ << 4) + random.nextInt(16);
        int y = world.worldSurfaceHeight(x, z);
        BlockPos origin = new BlockPos(x, y, z);
        if (world.biomeAt(origin) != V118Biome.MUSHROOM_FIELDS) {
            return Result.NOT_PLACED;
        }
        return place(world, random, origin);
    }

    /** Runs the configured feature at an already placed origin. */
    public static Result place(WorldAccess world, Random random, BlockPos origin) {
        if (world == null || random == null || origin == null) {
            throw new NullPointerException("Huge mushroom feature arguments");
        }

        // RandomBooleanSelectorFeature: true selects the first (red) holder.
        MushroomKind kind = random.nextBoolean()
                ? MushroomKind.RED : MushroomKind.BROWN;
        return place(world, random, origin, kind);
    }

    /** Runs one selected configured huge-mushroom feature without selector RNG. */
    public static Result place(WorldAccess world, Random random, BlockPos origin,
            MushroomKind kind) {
        if (world == null || random == null || origin == null || kind == null) {
            throw new NullPointerException("Huge mushroom feature arguments");
        }
        int height = random.nextInt(3) + 4;
        if (random.nextInt(12) == 0) {
            height *= 2;
        }
        int radius = kind == MushroomKind.RED ? 2 : 3;
        if (!isValidPosition(world, origin, height, radius, kind)) {
            return Result.NOT_PLACED;
        }

        int capBlocks = kind == MushroomKind.RED
                ? makeRedCap(world, origin, height, radius)
                : makeBrownCap(world, origin, height, radius);
        int stemBlocks = placeTrunk(world, origin, height);
        return new Result(true, kind, height, capBlocks, stemBlocks);
    }

    private static boolean isValidPosition(WorldAccess world, BlockPos origin,
            int height, int radius, MushroomKind kind) {
        int y = origin.getY();
        if (y < world.minBuildHeight() + 1
                || y + height + 1 >= world.maxBuildHeight()) {
            return false;
        }
        if (!world.isDirtOrMushroomGrowBlock(origin.down())) {
            return false;
        }

        for (int layer = 0; layer <= height; ++layer) {
            int layerRadius = treeRadiusForHeight(kind, height, radius, layer);
            for (int dx = -layerRadius; dx <= layerRadius; ++dx) {
                for (int dz = -layerRadius; dz <= layerRadius; ++dz) {
                    BlockPos position = origin.add(dx, layer, dz);
                    if (!world.isAir(position) && !world.isLeaves(position)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static int treeRadiusForHeight(MushroomKind kind, int height,
            int radius, int layer) {
        if (kind == MushroomKind.BROWN) {
            return layer <= 3 ? 0 : radius;
        }
        // AbstractHugeMushroomFeature validates with height=-1 and foliageRadius=-1;
        // HugeRedMushroomFeature therefore returns zero for every validation layer.
        return 0;
    }

    private static int makeBrownCap(WorldAccess world, BlockPos origin,
            int height, int radius) {
        int placed = 0;
        for (int dx = -radius; dx <= radius; ++dx) {
            for (int dz = -radius; dz <= radius; ++dz) {
                boolean westEdge = dx == -radius;
                boolean eastEdge = dx == radius;
                boolean northEdge = dz == -radius;
                boolean southEdge = dz == radius;
                boolean xEdge = westEdge || eastEdge;
                boolean zEdge = northEdge || southEdge;
                if (xEdge && zEdge) {
                    continue;
                }

                BlockPos position = origin.add(dx, height, dz);
                if (world.isSolidRender(position)) {
                    continue;
                }
                MushroomFaces faces = new MushroomFaces(
                        northEdge || xEdge && dz == 1 - radius,
                        eastEdge || zEdge && dx == radius - 1,
                        southEdge || xEdge && dz == radius - 1,
                        westEdge || zEdge && dx == 1 - radius,
                        true, false);
                world.setMushroomCap(position, MushroomKind.BROWN, faces);
                ++placed;
            }
        }
        return placed;
    }

    private static int makeRedCap(WorldAccess world, BlockPos origin,
            int height, int radius) {
        int placed = 0;
        int innerRadius = radius - 2;
        for (int layer = height - 3; layer <= height; ++layer) {
            int layerRadius = layer < height ? radius : radius - 1;
            for (int dx = -layerRadius; dx <= layerRadius; ++dx) {
                for (int dz = -layerRadius; dz <= layerRadius; ++dz) {
                    boolean xEdge = dx == -layerRadius || dx == layerRadius;
                    boolean zEdge = dz == -layerRadius || dz == layerRadius;
                    if (layer < height && xEdge == zEdge) {
                        continue;
                    }

                    BlockPos position = origin.add(dx, layer, dz);
                    if (world.isSolidRender(position)) {
                        continue;
                    }
                    MushroomFaces faces = new MushroomFaces(
                            dz < -innerRadius, dx > innerRadius,
                            dz > innerRadius, dx < -innerRadius,
                            layer >= height - 1, false);
                    world.setMushroomCap(position, MushroomKind.RED, faces);
                    ++placed;
                }
            }
        }
        return placed;
    }

    private static int placeTrunk(WorldAccess world, BlockPos origin, int height) {
        int placed = 0;
        for (int layer = 0; layer < height; ++layer) {
            BlockPos position = origin.up(layer);
            if (world.isSolidRender(position)) {
                continue;
            }
            world.setMushroomStem(position);
            ++placed;
        }
        return placed;
    }

    private static void requireArguments(WorldAccess world,
            Set<V118Biome> regionBiomes) {
        if (world == null || regionBiomes == null) {
            throw new NullPointerException("world and regionBiomes are required");
        }
    }

    public interface WorldAccess {
        int minBuildHeight();

        /** Exclusive upper build bound, matching LevelHeightAccessor#getMaxBuildHeight. */
        int maxBuildHeight();

        int worldSurfaceHeight(int blockX, int blockZ);

        V118Biome biomeAt(BlockPos pos);

        boolean isDirtOrMushroomGrowBlock(BlockPos pos);

        boolean isAir(BlockPos pos);

        boolean isLeaves(BlockPos pos);

        /** Java 1.18.2 BlockState#isSolidRender used by cap and trunk writes. */
        boolean isSolidRender(BlockPos pos);

        void setMushroomCap(BlockPos pos, MushroomKind kind,
                MushroomFaces faces);

        void setMushroomStem(BlockPos pos);
    }

    public enum MushroomKind {
        BROWN,
        RED
    }

    /** The six native HugeMushroomBlock face properties, retained without loss. */
    public static final class MushroomFaces {
        private final boolean north;
        private final boolean east;
        private final boolean south;
        private final boolean west;
        private final boolean up;
        private final boolean down;

        private MushroomFaces(boolean north, boolean east, boolean south,
                boolean west, boolean up, boolean down) {
            this.north = north;
            this.east = east;
            this.south = south;
            this.west = west;
            this.up = up;
            this.down = down;
        }

        public boolean north() {
            return north;
        }

        public boolean east() {
            return east;
        }

        public boolean south() {
            return south;
        }

        public boolean west() {
            return west;
        }

        public boolean up() {
            return up;
        }

        public boolean down() {
            return down;
        }
    }

    public static final class Result {
        private static final Result NOT_PLACED =
                new Result(false, null, 0, 0, 0);

        private final boolean placed;
        private final MushroomKind kind;
        private final int height;
        private final int capBlocks;
        private final int stemBlocks;

        private Result(boolean placed, MushroomKind kind, int height,
                int capBlocks, int stemBlocks) {
            this.placed = placed;
            this.kind = kind;
            this.height = height;
            this.capBlocks = capBlocks;
            this.stemBlocks = stemBlocks;
        }

        public boolean placed() {
            return placed;
        }

        public MushroomKind kind() {
            return kind;
        }

        public int height() {
            return height;
        }

        public int capBlocks() {
            return capBlocks;
        }

        public int stemBlocks() {
            return stemBlocks;
        }
    }
}

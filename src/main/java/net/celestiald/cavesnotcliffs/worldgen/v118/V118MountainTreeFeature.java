package net.celestiald.cavesnotcliffs.worldgen.v118;

import net.minecraft.util.math.BlockPos;

import java.util.Random;

/** Exact 1.18.2 pine and spruce tree shapes used by {@code trees_grove}. */
public final class V118MountainTreeFeature {
    private V118MountainTreeFeature() {
    }

    public static Result place(WorldAccess world, Random random, BlockPos origin,
            Kind kind) {
        if (world == null || random == null || origin == null || kind == null) {
            throw new NullPointerException("Mountain tree feature arguments");
        }
        Config config = Config.forKind(kind);
        int height = config.baseHeight + random.nextInt(config.heightRandA + 1)
                + random.nextInt(config.heightRandB + 1);

        // TreeFeature samples foliage height, radius, then validates the requested tree volume.
        int foliageHeight;
        int foliageRadius;
        if (kind == Kind.SPRUCE) {
            foliageHeight = Math.max(4, height - randomBetweenInclusive(random, 1, 2));
            foliageRadius = randomBetweenInclusive(random, 2, 3);
        } else {
            foliageHeight = randomBetweenInclusive(random, 3, 4);
            int trunkBelowFoliage = height - foliageHeight;
            foliageRadius = 1 + random.nextInt(Math.max(trunkBelowFoliage + 1, 1));
        }

        if (origin.getY() < world.minBuildHeight() + 1
                || origin.getY() + height + 1 > world.maxBuildHeight()
                || maxFreeTreeHeight(world, height, origin) < height) {
            return Result.notPlaced(kind);
        }

        world.setDirt(origin.down());
        int logs = 0;
        for (int layer = 0; layer < height; ++layer) {
            BlockPos position = origin.up(layer);
            if (world.isValidTreePos(position)) {
                world.setSpruceLog(position);
                logs++;
            }
        }

        BlockPos foliageOrigin = origin.up(height);
        int leaves = kind == Kind.SPRUCE
                ? placeSpruceFoliage(world, random, foliageOrigin,
                    foliageHeight, foliageRadius)
                : placePineFoliage(world, random, foliageOrigin,
                    foliageHeight, foliageRadius);
        return logs == 0 && leaves == 0
                ? Result.notPlaced(kind) : new Result(true, kind, logs, leaves);
    }

    private static int maxFreeTreeHeight(WorldAccess world, int height,
            BlockPos origin) {
        for (int layer = 0; layer <= height + 1; ++layer) {
            int radius = layer < 2 ? 0 : 2;
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

    private static int placeSpruceFoliage(WorldAccess world, Random random,
            BlockPos origin, int foliageHeight, int foliageRadius) {
        int offset = randomBetweenInclusive(random, 0, 2);
        int radius = random.nextInt(2);
        int radiusLimit = 1;
        int previousRadius = 0;
        int leaves = 0;
        for (int row = offset; row >= -foliageHeight; --row) {
            leaves += placeLeavesRow(world, origin, radius, row);
            if (radius >= radiusLimit) {
                radius = previousRadius;
                previousRadius = 1;
                radiusLimit = Math.min(radiusLimit + 1, foliageRadius);
            } else {
                radius++;
            }
        }
        return leaves;
    }

    private static int placePineFoliage(WorldAccess world, Random random,
            BlockPos origin, int foliageHeight, int foliageRadius) {
        int offset = 1;
        int radius = 0;
        int leaves = 0;
        for (int row = offset; row >= offset - foliageHeight; --row) {
            leaves += placeLeavesRow(world, origin, radius, row);
            if (radius >= 1 && row == offset - foliageHeight + 1) {
                radius--;
            } else if (radius < foliageRadius) {
                radius++;
            }
        }
        return leaves;
    }

    private static int placeLeavesRow(WorldAccess world, BlockPos origin,
            int radius, int row) {
        int leaves = 0;
        for (int dx = -radius; dx <= radius; ++dx) {
            for (int dz = -radius; dz <= radius; ++dz) {
                if (Math.abs(dx) == radius && Math.abs(dz) == radius && radius > 0) {
                    continue;
                }
                BlockPos position = origin.add(dx, row, dz);
                if (world.isValidTreePos(position)) {
                    world.setSpruceLeaves(position);
                    leaves++;
                }
            }
        }
        return leaves;
    }

    private static int randomBetweenInclusive(Random random, int minimum, int maximum) {
        return minimum + random.nextInt(maximum - minimum + 1);
    }

    public enum Kind {
        PINE,
        SPRUCE
    }

    public interface WorldAccess {
        int minBuildHeight();

        int maxBuildHeight();

        boolean isFree(BlockPos pos);

        boolean isValidTreePos(BlockPos pos);

        void setDirt(BlockPos pos);

        void setSpruceLog(BlockPos pos);

        void setSpruceLeaves(BlockPos pos);
    }

    public static final class Result {
        private final boolean placed;
        private final Kind kind;
        private final int logs;
        private final int leaves;

        private Result(boolean placed, Kind kind, int logs, int leaves) {
            this.placed = placed;
            this.kind = kind;
            this.logs = logs;
            this.leaves = leaves;
        }

        static Result notPlaced(Kind kind) {
            return new Result(false, kind, 0, 0);
        }

        public boolean placed() {
            return placed;
        }

        public Kind kind() {
            return kind;
        }

        public int logs() {
            return logs;
        }

        public int leaves() {
            return leaves;
        }
    }

    private static final class Config {
        final int baseHeight;
        final int heightRandA;
        final int heightRandB;

        private Config(int baseHeight, int heightRandA, int heightRandB) {
            this.baseHeight = baseHeight;
            this.heightRandA = heightRandA;
            this.heightRandB = heightRandB;
        }

        static Config forKind(Kind kind) {
            return kind == Kind.PINE ? new Config(6, 4, 0) : new Config(5, 2, 1);
        }
    }
}

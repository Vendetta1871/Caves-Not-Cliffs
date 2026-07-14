package net.celestiald.cavesnotcliffs.worldgen.v118;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Exact Java 1.18.2 {@code TreeFeatures.ACACIA} tree shape. */
public final class V118AcaciaTreeFeature {
    private static final HorizontalDirection[] HORIZONTAL_DIRECTIONS = {
            HorizontalDirection.NORTH,
            HorizontalDirection.EAST,
            HorizontalDirection.SOUTH,
            HorizontalDirection.WEST
    };

    private V118AcaciaTreeFeature() {
    }

    public static Result place(WorldAccess world, Random random, BlockPos origin) {
        if (world == null || random == null || origin == null) {
            throw new NullPointerException("Acacia tree feature arguments");
        }

        int height = 5 + random.nextInt(3) + random.nextInt(3);
        if (origin.getY() < world.minBuildHeight() + 1
                || origin.getY() + height + 1 > world.maxBuildHeight()
                || maxFreeTreeHeight(world, height, origin) < height) {
            return Result.NOT_PLACED;
        }

        Set<BlockPos> trunks = new HashSet<>();
        Set<BlockPos> foliage = new HashSet<>();
        List<FoliageAttachment> attachments = placeTrunk(world, random, origin,
                height, trunks);
        for (FoliageAttachment attachment : attachments) {
            placeFoliage(world, attachment, foliage);
        }
        return trunks.isEmpty() && foliage.isEmpty()
                ? Result.NOT_PLACED
                : new Result(true, trunks.size(), foliage.size());
    }

    private static int maxFreeTreeHeight(WorldAccess world, int height,
            BlockPos origin) {
        for (int layer = 0; layer <= height + 1; ++layer) {
            int radius = layer < 1 ? 0 : 2;
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

    private static List<FoliageAttachment> placeTrunk(WorldAccess world,
            Random random, BlockPos origin, int height, Set<BlockPos> trunks) {
        setDirtAt(world, origin.down(), trunks);
        List<FoliageAttachment> attachments = new ArrayList<>(2);
        HorizontalDirection direction = randomHorizontal(random);
        int bendStart = height - random.nextInt(4) - 1;
        int bendLength = 3 - random.nextInt(3);
        int x = origin.getX();
        int z = origin.getZ();
        int lastLogY = Integer.MIN_VALUE;

        for (int layer = 0; layer < height; ++layer) {
            int y = origin.getY() + layer;
            if (layer >= bendStart && bendLength > 0) {
                x += direction.stepX;
                z += direction.stepZ;
                --bendLength;
            }
            if (placeLog(world, new BlockPos(x, y, z), trunks)) {
                lastLogY = y + 1;
            }
        }
        if (lastLogY != Integer.MIN_VALUE) {
            attachments.add(new FoliageAttachment(new BlockPos(x, lastLogY, z), 1));
        }

        x = origin.getX();
        z = origin.getZ();
        HorizontalDirection branchDirection = randomHorizontal(random);
        if (branchDirection != direction) {
            int branchStart = bendStart - random.nextInt(2) - 1;
            int branchLength = 1 + random.nextInt(3);
            lastLogY = Integer.MIN_VALUE;
            for (int layer = branchStart;
                    layer < height && branchLength > 0;
                    ++layer, --branchLength) {
                if (layer < 1) {
                    continue;
                }
                x += branchDirection.stepX;
                z += branchDirection.stepZ;
                int y = origin.getY() + layer;
                if (placeLog(world, new BlockPos(x, y, z), trunks)) {
                    lastLogY = y + 1;
                }
            }
            if (lastLogY != Integer.MIN_VALUE) {
                attachments.add(new FoliageAttachment(
                        new BlockPos(x, lastLogY, z), 0));
            }
        }
        return attachments;
    }

    private static void placeFoliage(WorldAccess world,
            FoliageAttachment attachment, Set<BlockPos> foliage) {
        BlockPos center = attachment.position;
        placeLeavesRow(world, center, 2 + attachment.radiusOffset, -1, foliage);
        placeLeavesRow(world, center, 1, 0, foliage);
        placeLeavesRow(world, center, 1 + attachment.radiusOffset, 0, foliage);
    }

    private static void placeLeavesRow(WorldAccess world, BlockPos center,
            int radius, int row, Set<BlockPos> foliage) {
        for (int dx = -radius; dx <= radius; ++dx) {
            for (int dz = -radius; dz <= radius; ++dz) {
                int absX = Math.abs(dx);
                int absZ = Math.abs(dz);
                boolean skip = row == 0
                        ? (absX > 1 || absZ > 1) && absX != 0 && absZ != 0
                        : absX == radius && absZ == radius && radius > 0;
                if (skip) {
                    continue;
                }
                BlockPos position = center.add(dx, row, dz);
                if (world.isValidTreePos(position)) {
                    world.setAcaciaLeaves(position);
                    foliage.add(position.toImmutable());
                }
            }
        }
    }

    private static void setDirtAt(WorldAccess world, BlockPos position,
            Set<BlockPos> trunks) {
        if (!world.isDirtExceptGrassAndMycelium(position)) {
            world.setDirt(position);
            trunks.add(position.toImmutable());
        }
    }

    private static boolean placeLog(WorldAccess world, BlockPos position,
            Set<BlockPos> trunks) {
        if (!world.isValidTreePos(position)) {
            return false;
        }
        // ForkingTrunkPlacer never rewrites the provider's default vertical axis.
        world.setAcaciaLog(position);
        trunks.add(position.toImmutable());
        return true;
    }

    private static HorizontalDirection randomHorizontal(Random random) {
        return HORIZONTAL_DIRECTIONS[random.nextInt(HORIZONTAL_DIRECTIONS.length)];
    }

    public interface WorldAccess {
        int minBuildHeight();

        int maxBuildHeight();

        /** 1.18.2 {@code TreeFeature.isFree}, including vines. */
        boolean isFree(BlockPos pos);

        /** 1.18.2 replaceability predicate shared by trunk and foliage placement. */
        boolean isValidTreePos(BlockPos pos);

        boolean isDirtExceptGrassAndMycelium(BlockPos pos);

        void setDirt(BlockPos pos);

        /** Writes the default vertical acacia-log state. */
        void setAcaciaLog(BlockPos pos);

        void setAcaciaLeaves(BlockPos pos);
    }

    public static final class Result {
        private static final Result NOT_PLACED = new Result(false, 0, 0);
        private final boolean placed;
        private final int trunks;
        private final int foliage;

        private Result(boolean placed, int trunks, int foliage) {
            this.placed = placed;
            this.trunks = trunks;
            this.foliage = foliage;
        }

        public boolean placed() {
            return placed;
        }

        public int trunks() {
            return trunks;
        }

        public int foliage() {
            return foliage;
        }
    }

    private enum HorizontalDirection {
        NORTH(0, -1),
        EAST(1, 0),
        SOUTH(0, 1),
        WEST(-1, 0);

        final int stepX;
        final int stepZ;

        HorizontalDirection(int stepX, int stepZ) {
            this.stepX = stepX;
            this.stepZ = stepZ;
        }
    }

    private static final class FoliageAttachment {
        final BlockPos position;
        final int radiusOffset;

        FoliageAttachment(BlockPos position, int radiusOffset) {
            this.position = position;
            this.radiusOffset = radiusOffset;
        }
    }
}

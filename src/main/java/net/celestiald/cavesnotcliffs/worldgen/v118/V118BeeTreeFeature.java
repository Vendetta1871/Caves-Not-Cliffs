package net.celestiald.cavesnotcliffs.worldgen.v118;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118BeeTreePlacements.TreeKind;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Dependency-light port of the four Java 1.18.2 configured trees used by bee-bearing features.
 *
 * <p>This deliberately does not delegate to the 1.12 tree generators. Their height RNG, build
 * height checks, fancy-oak limbs, replaceability rules, and foliage loops all differ enough to
 * move the shared decoration random stream. The implementation mirrors {@code TreeFeature},
 * {@code StraightTrunkPlacer}, {@code FancyTrunkPlacer}, {@code BlobFoliagePlacer}, and
 * {@code FancyFoliagePlacer} from Java 1.18.2.</p>
 */
public final class V118BeeTreeFeature {
    private static final Comparator<BlockPos> DECORATOR_ORDER =
            Comparator.comparingInt(BlockPos::getY)
                    .thenComparingInt(BlockPos::getX)
                    .thenComparingInt(BlockPos::getZ);

    private V118BeeTreeFeature() {
    }

    public static Result place(WorldAccess world, Random random, BlockPos origin,
            TreeKind kind) {
        if (world == null || random == null || origin == null || kind == null) {
            throw new NullPointerException("Tree feature arguments");
        }
        Config config = Config.forKind(kind);
        int requestedHeight = config.baseHeight
                + random.nextInt(config.heightRandA + 1)
                + random.nextInt(config.heightRandB + 1);
        int foliageHeight = config.fancy ? 4 : 3;
        int foliageRadius = 2;
        if (origin.getY() < world.minBuildHeight() + 1
                || origin.getY() + requestedHeight + 1 > world.maxBuildHeight()) {
            return Result.notPlaced();
        }

        int freeHeight = maxFreeTreeHeight(world, requestedHeight, origin, config.fancy);
        if (freeHeight < requestedHeight && (!config.fancy || freeHeight < 4)) {
            return Result.notPlaced();
        }

        Set<BlockPos> trunks = new HashSet<>();
        Set<BlockPos> foliage = new HashSet<>();
        List<FoliageAttachment> attachments;
        if (config.fancy) {
            attachments = placeFancyTrunk(world, random, origin, freeHeight,
                    kind, trunks);
        } else {
            attachments = placeStraightTrunk(world, origin, freeHeight, kind, trunks);
        }
        for (FoliageAttachment attachment : attachments) {
            if (config.fancy) {
                placeFancyFoliage(world, random, attachment, foliageHeight,
                        foliageRadius, kind, foliage);
            } else {
                placeBlobFoliage(world, random, attachment, foliageHeight,
                        foliageRadius, kind, foliage);
            }
        }
        if (trunks.isEmpty() && foliage.isEmpty()) {
            return Result.notPlaced();
        }
        return new Result(true, sorted(trunks), sorted(foliage));
    }

    private static int maxFreeTreeHeight(WorldAccess world, int height,
            BlockPos origin, boolean fancy) {
        for (int layer = 0; layer <= height + 1; ++layer) {
            int radius = fancy || layer < 1 ? 0 : 1;
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

    private static List<FoliageAttachment> placeStraightTrunk(WorldAccess world,
            BlockPos origin, int height, TreeKind kind, Set<BlockPos> trunks) {
        setDirtAt(world, origin.down(), trunks);
        for (int layer = 0; layer < height; ++layer) {
            placeLog(world, origin.up(layer), LogAxis.Y, kind, trunks);
        }
        return Collections.singletonList(new FoliageAttachment(origin.up(height)));
    }

    private static List<FoliageAttachment> placeFancyTrunk(WorldAccess world,
            Random random, BlockPos origin, int height, TreeKind kind,
            Set<BlockPos> trunks) {
        int totalHeight = height + 2;
        int trunkHeight = floor(totalHeight * 0.618D);
        setDirtAt(world, origin.down(), trunks);
        int clustersPerLayer = Math.min(1,
                floor(1.382D + Math.pow((double) totalHeight / 13.0D, 2.0D)));
        int foliageY = origin.getY() + trunkHeight;
        int layer = totalHeight - 5;
        List<FancyFoliageCoords> coordinates = new ArrayList<>();
        coordinates.add(new FancyFoliageCoords(origin.up(layer), foliageY));
        for (; layer >= 0; --layer) {
            float shape = treeShape(totalHeight, layer);
            if (shape < 0.0F) {
                continue;
            }
            for (int cluster = 0; cluster < clustersPerLayer; ++cluster) {
                double length = (double) shape * ((double) random.nextFloat() + 0.328D);
                double angle = (double) (random.nextFloat() * 2.0F) * Math.PI;
                double offsetX = length * Math.sin(angle) + 0.5D;
                double offsetZ = length * Math.cos(angle) + 0.5D;
                BlockPos leafBase = offset(origin, offsetX, layer - 1, offsetZ);
                if (!makeLimb(world, leafBase, leafBase.up(5), false,
                        kind, trunks)) {
                    continue;
                }
                int deltaX = origin.getX() - leafBase.getX();
                int deltaZ = origin.getZ() - leafBase.getZ();
                double slopedY = (double) leafBase.getY()
                        - Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) * 0.381D;
                int branchY = slopedY > (double) foliageY ? foliageY : (int) slopedY;
                BlockPos branchBase = new BlockPos(origin.getX(), branchY,
                        origin.getZ());
                if (makeLimb(world, branchBase, leafBase, false, kind, trunks)) {
                    coordinates.add(new FancyFoliageCoords(leafBase, branchY));
                }
            }
        }
        makeLimb(world, origin, origin.up(trunkHeight), true, kind, trunks);
        for (FancyFoliageCoords coordinate : coordinates) {
            BlockPos branchBase = new BlockPos(origin.getX(), coordinate.branchBaseY,
                    origin.getZ());
            if (!branchBase.equals(coordinate.attachment.pos)
                    && trimBranch(totalHeight,
                            coordinate.branchBaseY - origin.getY())) {
                makeLimb(world, branchBase, coordinate.attachment.pos, true,
                        kind, trunks);
            }
        }
        List<FoliageAttachment> attachments = new ArrayList<>();
        for (FancyFoliageCoords coordinate : coordinates) {
            if (trimBranch(totalHeight, coordinate.branchBaseY - origin.getY())) {
                attachments.add(coordinate.attachment);
            }
        }
        return attachments;
    }

    private static boolean makeLimb(WorldAccess world, BlockPos start, BlockPos end,
            boolean place, TreeKind kind, Set<BlockPos> trunks) {
        if (!place && start.equals(end)) {
            return true;
        }
        int deltaX = end.getX() - start.getX();
        int deltaY = end.getY() - start.getY();
        int deltaZ = end.getZ() - start.getZ();
        int steps = Math.max(Math.abs(deltaX),
                Math.max(Math.abs(deltaY), Math.abs(deltaZ)));
        float stepX = (float) deltaX / (float) steps;
        float stepY = (float) deltaY / (float) steps;
        float stepZ = (float) deltaZ / (float) steps;
        for (int step = 0; step <= steps; ++step) {
            BlockPos position = offset(start,
                    0.5F + (float) step * stepX,
                    0.5F + (float) step * stepY,
                    0.5F + (float) step * stepZ);
            if (place) {
                placeLog(world, position, logAxis(start, position), kind, trunks);
            } else if (!world.isFree(position)) {
                return false;
            }
        }
        return true;
    }

    private static LogAxis logAxis(BlockPos start, BlockPos position) {
        int deltaX = Math.abs(position.getX() - start.getX());
        int deltaZ = Math.abs(position.getZ() - start.getZ());
        int horizontal = Math.max(deltaX, deltaZ);
        if (horizontal == 0) {
            return LogAxis.Y;
        }
        return deltaX == horizontal ? LogAxis.X : LogAxis.Z;
    }

    private static void placeBlobFoliage(WorldAccess world, Random random,
            FoliageAttachment attachment, int foliageHeight, int foliageRadius,
            TreeKind kind, Set<BlockPos> foliage) {
        for (int row = 0; row >= -foliageHeight; --row) {
            int radius = Math.max(foliageRadius - 1 - row / 2, 0);
            placeLeavesRow(world, random, attachment.pos, radius, row, false,
                    kind, foliage);
        }
    }

    private static void placeFancyFoliage(WorldAccess world, Random random,
            FoliageAttachment attachment, int foliageHeight, int foliageRadius,
            TreeKind kind, Set<BlockPos> foliage) {
        int offset = 4;
        for (int row = offset; row >= offset - foliageHeight; --row) {
            int radius = foliageRadius
                    + (row == offset || row == offset - foliageHeight ? 0 : 1);
            placeLeavesRow(world, random, attachment.pos, radius, row, true,
                    kind, foliage);
        }
    }

    private static void placeLeavesRow(WorldAccess world, Random random,
            BlockPos center, int radius, int row, boolean fancy, TreeKind kind,
            Set<BlockPos> foliage) {
        for (int dx = -radius; dx <= radius; ++dx) {
            for (int dz = -radius; dz <= radius; ++dz) {
                int absX = Math.abs(dx);
                int absZ = Math.abs(dz);
                boolean skip;
                if (fancy) {
                    float x = (float) absX + 0.5F;
                    float z = (float) absZ + 0.5F;
                    skip = x * x + z * z > (float) (radius * radius);
                } else {
                    skip = absX == radius && absZ == radius
                            && (random.nextInt(2) == 0 || row == 0);
                }
                if (skip) {
                    continue;
                }
                BlockPos position = center.add(dx, row, dz);
                if (world.isValidTreePos(position)) {
                    world.setLeaves(position, kind);
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

    private static void placeLog(WorldAccess world, BlockPos position,
            LogAxis axis, TreeKind kind, Set<BlockPos> trunks) {
        if (world.isValidTreePos(position)) {
            world.setLog(position, axis, kind);
            trunks.add(position.toImmutable());
        }
    }

    private static float treeShape(int height, int layer) {
        if ((float) layer < (float) height * 0.3F) {
            return -1.0F;
        }
        float half = (float) height / 2.0F;
        float delta = half - (float) layer;
        float shape = (float) Math.sqrt(half * half - delta * delta);
        if (delta == 0.0F) {
            shape = half;
        } else if (Math.abs(delta) >= half) {
            return 0.0F;
        }
        return shape * 0.5F;
    }

    private static boolean trimBranch(int height, int relativeY) {
        return (double) relativeY >= (double) height * 0.2D;
    }

    private static BlockPos offset(BlockPos origin, double x, double y, double z) {
        return new BlockPos((double) origin.getX() + x,
                (double) origin.getY() + y, (double) origin.getZ() + z);
    }

    private static int floor(double value) {
        int integer = (int) value;
        return value < (double) integer ? integer - 1 : integer;
    }

    private static List<BlockPos> sorted(Set<BlockPos> positions) {
        List<BlockPos> result = new ArrayList<>(positions);
        result.sort(DECORATOR_ORDER);
        return result;
    }

    public enum LogAxis {
        X,
        Y,
        Z
    }

    public interface WorldAccess {
        int minBuildHeight();

        int maxBuildHeight();

        boolean isFree(BlockPos pos);

        boolean isValidTreePos(BlockPos pos);

        boolean isDirtExceptGrassAndMycelium(BlockPos pos);

        void setDirt(BlockPos pos);

        void setLog(BlockPos pos, LogAxis axis, TreeKind kind);

        void setLeaves(BlockPos pos, TreeKind kind);
    }

    public static final class Result {
        private static final Result NOT_PLACED = new Result(false,
                Collections.<BlockPos>emptyList(), Collections.<BlockPos>emptyList());
        private final boolean placed;
        private final List<BlockPos> trunks;
        private final List<BlockPos> foliage;

        private Result(boolean placed, List<BlockPos> trunks, List<BlockPos> foliage) {
            this.placed = placed;
            this.trunks = Collections.unmodifiableList(new ArrayList<>(trunks));
            this.foliage = Collections.unmodifiableList(new ArrayList<>(foliage));
        }

        private static Result notPlaced() {
            return NOT_PLACED;
        }

        public boolean placed() {
            return placed;
        }

        public List<BlockPos> trunks() {
            return trunks;
        }

        public List<BlockPos> foliage() {
            return foliage;
        }
    }

    private static final class Config {
        final int baseHeight;
        final int heightRandA;
        final int heightRandB;
        final boolean fancy;

        private Config(int baseHeight, int heightRandA, int heightRandB,
                boolean fancy) {
            this.baseHeight = baseHeight;
            this.heightRandA = heightRandA;
            this.heightRandB = heightRandB;
            this.fancy = fancy;
        }

        static Config forKind(TreeKind kind) {
            switch (kind) {
                case OAK:
                    return new Config(4, 2, 0, false);
                case BIRCH:
                    return new Config(5, 2, 0, false);
                case SUPER_BIRCH:
                    return new Config(5, 2, 6, false);
                case FANCY_OAK:
                    return new Config(3, 11, 0, true);
                default:
                    throw new AssertionError(kind);
            }
        }
    }

    private static final class FoliageAttachment {
        final BlockPos pos;

        FoliageAttachment(BlockPos pos) {
            this.pos = pos;
        }
    }

    private static final class FancyFoliageCoords {
        final FoliageAttachment attachment;
        final int branchBaseY;

        FancyFoliageCoords(BlockPos pos, int branchBaseY) {
            this.attachment = new FoliageAttachment(pos);
            this.branchBaseY = branchBaseY;
        }
    }
}

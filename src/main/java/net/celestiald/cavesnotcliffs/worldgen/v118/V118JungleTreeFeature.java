package net.celestiald.cavesnotcliffs.worldgen.v118;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118BeeTreePlacements.TreeKind;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Java 1.18.2 sparse-jungle and jungle tree selectors and configured trees. */
public final class V118JungleTreeFeature {
    public static final int SPARSE_GLOBAL_INDEX = 4;
    public static final int JUNGLE_GLOBAL_INDEX = 7;
    private static final int VEGETAL_DECORATION_STEP = 9;
    private static final Comparator<BlockPos> DECORATOR_ORDER =
            Comparator.comparingInt(BlockPos::getY);
    private static final HorizontalDirection[] HORIZONTAL_DIRECTIONS = {
            HorizontalDirection.NORTH, HorizontalDirection.EAST,
            HorizontalDirection.SOUTH, HorizontalDirection.WEST
    };

    private V118JungleTreeFeature() {
    }

    /** Places one complete registered placed feature, including every modifier and filter. */
    public static Result place(WorldAccess world, long worldSeed, int chunkX, int chunkZ,
            Family family) {
        if (world == null || family == null) {
            throw new NullPointerException("Jungle tree feature arguments");
        }
        V118WorldgenRandom random = new V118WorldgenRandom(0L);
        long decorationSeed = random.setDecorationSeed(worldSeed, chunkX << 4, chunkZ << 4);
        random.setFeatureSeed(decorationSeed, family.globalIndex,
                VEGETAL_DECORATION_STEP);

        Result result = new Result(family);
        int attempts = family.baseCount + (random.nextInt(10) == 9 ? 1 : 0);
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
            BlockPos origin = new BlockPos(x, world.oceanFloorHeight(x, z), z);
            if (world.biomeAt(origin) != family.biome) {
                continue;
            }
            TreeResult tree = placeSelected(world, random, origin, family);
            if (tree.placed) {
                result.trees++;
                result.logs += tree.logs;
                result.leaves += tree.leaves;
                result.vines += tree.vines;
                result.cocoa += tree.cocoa;
            }
        }
        return result;
    }

    private static TreeResult placeSelected(WorldAccess world, Random random,
            BlockPos origin, Family family) {
        Tree tree;
        if (random.nextFloat() < 0.1F) {
            tree = Tree.FANCY_OAK;
        } else if (random.nextFloat() < 0.5F) {
            tree = Tree.JUNGLE_BUSH;
        } else if (family == Family.JUNGLE && random.nextFloat() < 0.33333334F) {
            tree = Tree.MEGA_JUNGLE;
        } else {
            tree = Tree.JUNGLE;
        }

        boolean survives = tree == Tree.FANCY_OAK || tree == Tree.JUNGLE_BUSH
                ? world.canOakSaplingSurvive(origin)
                : world.canJungleSaplingSurvive(origin);
        if (!survives) {
            return TreeResult.NOT_PLACED;
        }
        switch (tree) {
            case FANCY_OAK:
                V118BeeTreeFeature.Result fancy = V118BeeTreeFeature.place(
                        world, random, origin, TreeKind.FANCY_OAK);
                return fancy.placed()
                        ? new TreeResult(true, fancy.trunks().size(),
                                fancy.foliage().size(), 0, 0)
                        : TreeResult.NOT_PLACED;
            case JUNGLE_BUSH:
                return placeBush(world, random, origin);
            case MEGA_JUNGLE:
                return placeMegaJungle(world, random, origin);
            case JUNGLE:
                return placeJungle(world, random, origin);
            default:
                throw new AssertionError(tree);
        }
    }

    private static TreeResult placeJungle(WorldAccess world, Random random,
            BlockPos origin) {
        int height = 4 + random.nextInt(9) + random.nextInt(1);
        if (!fits(world, origin, height, TreeShape.NORMAL)) {
            return TreeResult.NOT_PLACED;
        }
        Set<BlockPos> trunks = new HashSet<>();
        Set<BlockPos> foliage = new HashSet<>();
        setDirtAt(world, origin.down(), trunks);
        for (int layer = 0; layer < height; ++layer) {
            placeJungleLog(world, origin.up(layer), trunks);
        }
        placeBlobFoliage(world, random, origin.up(height), 2, 3, 0, foliage);
        if (trunks.isEmpty() && foliage.isEmpty()) {
            return TreeResult.NOT_PLACED;
        }
        Decoration decoration = decorateJungle(world, random, trunks, foliage, true);
        return new TreeResult(true, trunks.size(), foliage.size(),
                decoration.vines, decoration.cocoa);
    }

    private static TreeResult placeBush(WorldAccess world, Random random,
            BlockPos origin) {
        int height = 1 + random.nextInt(1) + random.nextInt(1);
        if (!fits(world, origin, height, TreeShape.BUSH)) {
            return TreeResult.NOT_PLACED;
        }
        Set<BlockPos> trunks = new HashSet<>();
        Set<BlockPos> foliage = new HashSet<>();
        setDirtAt(world, origin.down(), trunks);
        for (int layer = 0; layer < height; ++layer) {
            placeJungleLog(world, origin.up(layer), trunks);
        }
        BlockPos attachment = origin.up(height);
        for (int row = 1; row >= -1; --row) {
            int radius = 1 - row;
            placeBushLeavesRow(world, random, attachment, radius, row, foliage);
        }
        return trunks.isEmpty() && foliage.isEmpty()
                ? TreeResult.NOT_PLACED
                : new TreeResult(true, trunks.size(), foliage.size(), 0, 0);
    }

    private static TreeResult placeMegaJungle(WorldAccess world, Random random,
            BlockPos origin) {
        int height = 10 + random.nextInt(3) + random.nextInt(20);
        if (!fits(world, origin, height, TreeShape.MEGA)) {
            return TreeResult.NOT_PLACED;
        }
        Set<BlockPos> trunks = new HashSet<>();
        Set<BlockPos> foliage = new HashSet<>();
        setDirtAt(world, origin.down(), trunks);
        setDirtAt(world, origin.add(1, -1, 0), trunks);
        setDirtAt(world, origin.add(0, -1, 1), trunks);
        setDirtAt(world, origin.add(1, -1, 1), trunks);

        List<FoliageAttachment> attachments = new ArrayList<>();
        for (int layer = 0; layer < height; ++layer) {
            placeJungleLogIfFree(world, origin.add(0, layer, 0), trunks);
            if (layer < height - 1) {
                placeJungleLogIfFree(world, origin.add(1, layer, 0), trunks);
                placeJungleLogIfFree(world, origin.add(1, layer, 1), trunks);
                placeJungleLogIfFree(world, origin.add(0, layer, 1), trunks);
            }
        }
        attachments.add(new FoliageAttachment(origin.up(height), 0, true));
        for (int branchY = height - 2 - random.nextInt(4);
                branchY > height / 2;
                branchY -= 2 + random.nextInt(4)) {
            float angle = random.nextFloat() * ((float) Math.PI * 2.0F);
            int dx = 0;
            int dz = 0;
            for (int length = 0; length < 5; ++length) {
                dx = (int) (1.5F + MathHelper.cos(angle) * (float) length);
                dz = (int) (1.5F + MathHelper.sin(angle) * (float) length);
                placeJungleLog(world,
                        origin.add(dx, branchY - 3 + length / 2, dz), trunks);
            }
            attachments.add(new FoliageAttachment(
                    origin.add(dx, branchY, dz), -2, false));
        }
        for (FoliageAttachment attachment : attachments) {
            int rows = attachment.doubleTrunk ? 2 : 1 + random.nextInt(2);
            for (int row = 0; row >= -rows; --row) {
                int radius = 3 + attachment.radiusOffset - row;
                placeMegaLeavesRow(world, attachment.position, radius, row,
                        attachment.doubleTrunk, foliage);
            }
        }
        if (trunks.isEmpty() && foliage.isEmpty()) {
            return TreeResult.NOT_PLACED;
        }
        Decoration decoration = decorateJungle(world, random, trunks, foliage, false);
        return new TreeResult(true, trunks.size(), foliage.size(),
                decoration.vines, 0);
    }

    private static boolean fits(WorldAccess world, BlockPos origin, int height,
            TreeShape shape) {
        if (origin.getY() < world.minBuildHeight() + 1
                || origin.getY() + height + 1 > world.maxBuildHeight()) {
            return false;
        }
        for (int layer = 0; layer <= height + 1; ++layer) {
            int radius = shape == TreeShape.BUSH ? 0
                    : shape == TreeShape.MEGA ? (layer < 1 ? 1 : 2)
                    : (layer < 1 ? 0 : 1);
            for (int dx = -radius; dx <= radius; ++dx) {
                for (int dz = -radius; dz <= radius; ++dz) {
                    BlockPos position = origin.add(dx, layer, dz);
                    if (!world.isFree(position)
                            || shape != TreeShape.NORMAL && world.isVine(position)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static void setDirtAt(WorldAccess world, BlockPos position,
            Set<BlockPos> trunks) {
        if (!world.isDirtExceptGrassAndMycelium(position)) {
            world.setDirt(position);
            trunks.add(position.toImmutable());
        }
    }

    private static void placeJungleLog(WorldAccess world, BlockPos position,
            Set<BlockPos> trunks) {
        if (world.isValidTreePos(position)) {
            world.setJungleLog(position);
            trunks.add(position.toImmutable());
        }
    }

    private static void placeJungleLogIfFree(WorldAccess world, BlockPos position,
            Set<BlockPos> trunks) {
        if (world.isFree(position)) {
            placeJungleLog(world, position, trunks);
        }
    }

    private static void placeBlobFoliage(WorldAccess world, Random random,
            BlockPos attachment, int baseRadius, int height, int offset,
            Set<BlockPos> foliage) {
        for (int row = offset; row >= offset - height; --row) {
            int radius = Math.max(baseRadius - 1 - row / 2, 0);
            for (int dx = -radius; dx <= radius; ++dx) {
                for (int dz = -radius; dz <= radius; ++dz) {
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius
                            && (random.nextInt(2) == 0 || row == 0)) {
                        continue;
                    }
                    placeJungleLeaf(world, attachment.add(dx, row, dz), foliage);
                }
            }
        }
    }

    private static void placeBushLeavesRow(WorldAccess world, Random random,
            BlockPos attachment, int radius, int row, Set<BlockPos> foliage) {
        for (int dx = -radius; dx <= radius; ++dx) {
            for (int dz = -radius; dz <= radius; ++dz) {
                if (Math.abs(dx) == radius && Math.abs(dz) == radius
                        && random.nextInt(2) == 0) {
                    continue;
                }
                BlockPos position = attachment.add(dx, row, dz);
                if (world.isValidTreePos(position)) {
                    world.setOakLeaves(position);
                    foliage.add(position.toImmutable());
                }
            }
        }
    }

    private static void placeMegaLeavesRow(WorldAccess world, BlockPos attachment,
            int radius, int row, boolean doubleTrunk, Set<BlockPos> foliage) {
        int extra = doubleTrunk ? 1 : 0;
        for (int dx = -radius; dx <= radius + extra; ++dx) {
            for (int dz = -radius; dz <= radius + extra; ++dz) {
                int x = doubleTrunk
                        ? Math.min(Math.abs(dx), Math.abs(dx - 1)) : Math.abs(dx);
                int z = doubleTrunk
                        ? Math.min(Math.abs(dz), Math.abs(dz - 1)) : Math.abs(dz);
                if (x + z >= 7 || x * x + z * z > radius * radius) {
                    continue;
                }
                placeJungleLeaf(world, attachment.add(dx, row, dz), foliage);
            }
        }
    }

    private static void placeJungleLeaf(WorldAccess world, BlockPos position,
            Set<BlockPos> foliage) {
        if (world.isValidTreePos(position)) {
            world.setJungleLeaves(position);
            foliage.add(position.toImmutable());
        }
    }

    private static Decoration decorateJungle(WorldAccess world, Random random,
            Set<BlockPos> trunks, Set<BlockPos> foliage, boolean cocoa) {
        List<BlockPos> orderedTrunks = sorted(trunks);
        List<BlockPos> orderedFoliage = sorted(foliage);
        int cocoaPlaced = cocoa ? placeCocoa(world, random, orderedTrunks) : 0;
        int vines = placeTrunkVines(world, random, orderedTrunks)
                + placeLeafVines(world, random, orderedFoliage);
        return new Decoration(vines, cocoaPlaced);
    }

    private static int placeCocoa(WorldAccess world, Random random,
            List<BlockPos> trunks) {
        if (random.nextFloat() >= 0.2F) {
            return 0;
        }
        int bottomY = trunks.get(0).getY();
        int placed = 0;
        for (BlockPos trunk : trunks) {
            if (trunk.getY() - bottomY > 2) {
                continue;
            }
            for (HorizontalDirection direction : HORIZONTAL_DIRECTIONS) {
                HorizontalDirection outward = direction.opposite();
                BlockPos position = trunk.add(outward.stepX, 0, outward.stepZ);
                if (random.nextFloat() <= 0.25F && world.isAir(position)) {
                    world.setCocoa(position, direction, random.nextInt(3));
                    placed++;
                }
            }
        }
        return placed;
    }

    private static int placeTrunkVines(WorldAccess world, Random random,
            List<BlockPos> trunks) {
        int placed = 0;
        for (BlockPos trunk : trunks) {
            placed += placeSingleVine(world, random, trunk.west(),
                    HorizontalDirection.EAST, 3, false);
            placed += placeSingleVine(world, random, trunk.east(),
                    HorizontalDirection.WEST, 3, false);
            placed += placeSingleVine(world, random, trunk.north(),
                    HorizontalDirection.SOUTH, 3, false);
            placed += placeSingleVine(world, random, trunk.south(),
                    HorizontalDirection.NORTH, 3, false);
        }
        return placed;
    }

    private static int placeLeafVines(WorldAccess world, Random random,
            List<BlockPos> foliage) {
        int placed = 0;
        for (BlockPos leaf : foliage) {
            placed += placeSingleVine(world, random, leaf.west(),
                    HorizontalDirection.EAST, 4, true);
            placed += placeSingleVine(world, random, leaf.east(),
                    HorizontalDirection.WEST, 4, true);
            placed += placeSingleVine(world, random, leaf.north(),
                    HorizontalDirection.SOUTH, 4, true);
            placed += placeSingleVine(world, random, leaf.south(),
                    HorizontalDirection.NORTH, 4, true);
        }
        return placed;
    }

    private static int placeSingleVine(WorldAccess world, Random random,
            BlockPos position, HorizontalDirection attachment, int chance,
            boolean hanging) {
        boolean selected = chance == 3 ? random.nextInt(3) > 0 : random.nextInt(4) == 0;
        if (!selected || !world.isAir(position)) {
            return 0;
        }
        int placed = 1;
        world.setVine(position, attachment);
        if (!hanging) {
            return placed;
        }
        position = position.down();
        for (int remaining = 4; remaining > 0 && world.isAir(position); --remaining) {
            world.setVine(position, attachment);
            placed++;
            position = position.down();
        }
        return placed;
    }

    private static List<BlockPos> sorted(Set<BlockPos> positions) {
        List<BlockPos> result = new ArrayList<>(positions);
        Collections.sort(result, DECORATOR_ORDER);
        return result;
    }

    public enum Family {
        SPARSE_JUNGLE(SPARSE_GLOBAL_INDEX, 2, V118Biome.SPARSE_JUNGLE),
        JUNGLE(JUNGLE_GLOBAL_INDEX, 50, V118Biome.JUNGLE);

        final int globalIndex;
        final int baseCount;
        final V118Biome biome;

        Family(int globalIndex, int baseCount, V118Biome biome) {
            this.globalIndex = globalIndex;
            this.baseCount = baseCount;
            this.biome = biome;
        }
    }

    private enum Tree {
        FANCY_OAK,
        JUNGLE_BUSH,
        MEGA_JUNGLE,
        JUNGLE
    }

    private enum TreeShape {
        NORMAL,
        BUSH,
        MEGA
    }

    public enum HorizontalDirection {
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

        HorizontalDirection opposite() {
            switch (this) {
                case NORTH: return SOUTH;
                case EAST: return WEST;
                case SOUTH: return NORTH;
                case WEST: return EAST;
                default: throw new AssertionError(this);
            }
        }
    }

    public interface WorldAccess extends V118BeeTreeFeature.WorldAccess {
        V118Biome biomeAt(BlockPos pos);

        int oceanFloorHeight(int blockX, int blockZ);

        int worldSurfaceHeight(int blockX, int blockZ);

        boolean isVine(BlockPos pos);

        boolean isAir(BlockPos pos);

        boolean canOakSaplingSurvive(BlockPos pos);

        boolean canJungleSaplingSurvive(BlockPos pos);

        void setJungleLog(BlockPos pos);

        void setJungleLeaves(BlockPos pos);

        void setOakLeaves(BlockPos pos);

        void setVine(BlockPos pos, HorizontalDirection attachment);

        void setCocoa(BlockPos pos, HorizontalDirection facing, int age);
    }

    public static final class Result {
        private final Family family;
        private int attempts;
        private int trees;
        private int logs;
        private int leaves;
        private int vines;
        private int cocoa;

        private Result(Family family) {
            this.family = family;
        }

        public Family family() { return family; }
        public int attempts() { return attempts; }
        public int trees() { return trees; }
        public int logs() { return logs; }
        public int leaves() { return leaves; }
        public int vines() { return vines; }
        public int cocoa() { return cocoa; }
    }

    private static final class TreeResult {
        static final TreeResult NOT_PLACED = new TreeResult(false, 0, 0, 0, 0);
        final boolean placed;
        final int logs;
        final int leaves;
        final int vines;
        final int cocoa;

        TreeResult(boolean placed, int logs, int leaves, int vines, int cocoa) {
            this.placed = placed;
            this.logs = logs;
            this.leaves = leaves;
            this.vines = vines;
            this.cocoa = cocoa;
        }
    }

    private static final class FoliageAttachment {
        final BlockPos position;
        final int radiusOffset;
        final boolean doubleTrunk;

        FoliageAttachment(BlockPos position, int radiusOffset, boolean doubleTrunk) {
            this.position = position;
            this.radiusOffset = radiusOffset;
            this.doubleTrunk = doubleTrunk;
        }
    }

    private static final class Decoration {
        final int vines;
        final int cocoa;

        Decoration(int vines, int cocoa) {
            this.vines = vines;
            this.cocoa = cocoa;
        }
    }
}

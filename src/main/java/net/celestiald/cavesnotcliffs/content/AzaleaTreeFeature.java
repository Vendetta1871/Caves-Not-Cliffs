package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.world.CavesNotCliffsWorldType;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

/** Java 1.18.2 azalea-tree configured feature, expressed against the 1.12 world API. */
public final class AzaleaTreeFeature {
    private static final int[][] HORIZONTAL = {
            {0, -1}, // north
            {1, 0},  // east
            {0, 1},  // south
            {-1, 0}  // west
    };

    private AzaleaTreeFeature() {
    }

    public enum Kind {
        ROOTED_DIRT,
        OAK_LOG,
        AZALEA_LEAVES,
        FLOWERING_AZALEA_LEAVES
    }

    public static final class Placement {
        public final BlockPos position;
        public final Kind kind;
        /** Leaves distance is 1..7; non-leaf placements use zero. */
        public final int distance;

        private Placement(BlockPos position, Kind kind, int distance) {
            this.position = position;
            this.kind = kind;
            this.distance = distance;
        }
    }

    /**
     * Produces the exact empty-world random call sequence of the 1.18.2 AZALEA_TREE feature.
     * The supplied predicate represents TreeFeature.validTreePos at the original world state.
     */
    public static List<Placement> plan(Random random, BlockPos origin,
            Predicate<BlockPos> replaceable) {
        int height = 4 + random.nextInt(3) + random.nextInt(1);
        if (!hasClearance(origin, height, replaceable)) {
            return Collections.emptyList();
        }

        int[] direction = HORIZONTAL[random.nextInt(HORIZONTAL.length)];
        LinkedHashMap<BlockPos, Kind> blocks = new LinkedHashMap<>();
        List<BlockPos> attachments = new ArrayList<>();
        blocks.put(origin.down(), Kind.ROOTED_DIRT);

        BlockPos cursor = origin;
        int topIndex = height - 1;
        for (int index = 0; index <= topIndex; index++) {
            if (index + 1 >= topIndex + random.nextInt(2)) {
                cursor = cursor.add(direction[0], 0, direction[1]);
            }
            if (isReplaceable(cursor, replaceable, blocks)) {
                blocks.put(cursor, Kind.OAK_LOG);
            }
            if (index >= 3) {
                attachments.add(cursor);
            }
            cursor = cursor.up();
        }

        int bendLength = 1 + random.nextInt(2);
        for (int index = 0; index <= bendLength; index++) {
            if (isReplaceable(cursor, replaceable, blocks)) {
                blocks.put(cursor, Kind.OAK_LOG);
            }
            attachments.add(cursor);
            cursor = cursor.add(direction[0], 0, direction[1]);
        }

        // RandomSpreadFoliagePlacer(radius=3, height=2, attempts=50), once per attachment.
        for (BlockPos attachment : attachments) {
            for (int attempt = 0; attempt < 50; attempt++) {
                BlockPos leaf = attachment.add(
                        random.nextInt(3) - random.nextInt(3),
                        random.nextInt(2) - random.nextInt(2),
                        random.nextInt(3) - random.nextInt(3));
                if (!isReplaceable(leaf, replaceable, blocks)) {
                    continue;
                }
                Kind kind = random.nextInt(4) < 3
                        ? Kind.AZALEA_LEAVES : Kind.FLOWERING_AZALEA_LEAVES;
                blocks.put(leaf, kind);
            }
        }

        Map<BlockPos, Integer> distances = leafDistances(blocks);
        List<Placement> result = new ArrayList<>(blocks.size());
        for (Map.Entry<BlockPos, Kind> entry : blocks.entrySet()) {
            Integer distance = distances.get(entry.getKey());
            result.add(new Placement(entry.getKey(), entry.getValue(),
                    distance == null ? 0 : distance));
        }
        return Collections.unmodifiableList(result);
    }

    private static boolean hasClearance(BlockPos origin, int height,
            Predicate<BlockPos> replaceable) {
        if (origin.getY() < CavesNotCliffsWorldType.MIN_HEIGHT + 1
                || origin.getY() + height + 1 > CavesNotCliffsWorldType.MAX_HEIGHT) {
            return false;
        }
        for (int dy = 0; dy <= height + 1; dy++) {
            int radius = dy < 1 ? 0 : 1;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (!replaceable.test(origin.add(dx, dy, dz))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean isReplaceable(BlockPos position, Predicate<BlockPos> original,
            Map<BlockPos, Kind> placed) {
        Kind current = placed.get(position);
        if (current == Kind.OAK_LOG || current == Kind.ROOTED_DIRT) {
            return false;
        }
        return current == Kind.AZALEA_LEAVES
                || current == Kind.FLOWERING_AZALEA_LEAVES
                || original.test(position);
    }

    private static Map<BlockPos, Integer> leafDistances(Map<BlockPos, Kind> blocks) {
        Map<BlockPos, Integer> distances = new LinkedHashMap<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        for (Map.Entry<BlockPos, Kind> entry : blocks.entrySet()) {
            if (entry.getValue() == Kind.OAK_LOG) {
                for (EnumFacing facing : EnumFacing.values()) {
                    BlockPos neighbor = entry.getKey().offset(facing);
                    if (isLeaf(blocks.get(neighbor)) && !distances.containsKey(neighbor)) {
                        distances.put(neighbor, 1);
                        queue.add(neighbor);
                    }
                }
            }
        }
        while (!queue.isEmpty()) {
            BlockPos position = queue.removeFirst();
            int distance = distances.get(position);
            if (distance >= 6) {
                continue;
            }
            for (EnumFacing facing : EnumFacing.values()) {
                BlockPos neighbor = position.offset(facing);
                if (isLeaf(blocks.get(neighbor)) && !distances.containsKey(neighbor)) {
                    distances.put(neighbor, distance + 1);
                    queue.addLast(neighbor);
                }
            }
        }
        for (Map.Entry<BlockPos, Kind> entry : blocks.entrySet()) {
            if (isLeaf(entry.getValue()) && !distances.containsKey(entry.getKey())) {
                distances.put(entry.getKey(), 7);
            }
        }
        return distances;
    }

    private static boolean isLeaf(Kind kind) {
        return kind == Kind.AZALEA_LEAVES || kind == Kind.FLOWERING_AZALEA_LEAVES;
    }

    public static boolean grow(World world, Random random, BlockPos origin,
            Block rootedDirt, Block azaleaLeaves, Block floweringAzaleaLeaves,
            net.minecraft.block.properties.PropertyInteger distanceProperty,
            net.minecraft.block.properties.PropertyBool persistentProperty) {
        List<Placement> plan = plan(random, origin,
                position -> isTreeReplaceable(world.getBlockState(position)));
        if (plan.isEmpty()) {
            return false;
        }
        for (Placement placement : plan) {
            IBlockState state;
            switch (placement.kind) {
                case ROOTED_DIRT:
                    state = rootedDirt.getDefaultState();
                    break;
                case OAK_LOG:
                    state = Blocks.LOG.getDefaultState();
                    break;
                case FLOWERING_AZALEA_LEAVES:
                    state = floweringAzaleaLeaves.getDefaultState()
                            .withProperty(distanceProperty, placement.distance)
                            .withProperty(persistentProperty, false);
                    break;
                case AZALEA_LEAVES:
                default:
                    state = azaleaLeaves.getDefaultState()
                            .withProperty(distanceProperty, placement.distance)
                            .withProperty(persistentProperty, false);
                    break;
            }
            world.setBlockState(placement.position, state, 19);
        }
        return true;
    }

    private static boolean isTreeReplaceable(IBlockState state) {
        Material material = state.getMaterial();
        return material == Material.AIR
                || material == Material.LEAVES
                || material == Material.PLANTS
                || material == Material.VINE
                || material == Material.WATER
                || material.isReplaceable();
    }
}

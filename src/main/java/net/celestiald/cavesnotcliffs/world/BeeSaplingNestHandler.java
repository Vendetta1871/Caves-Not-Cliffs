package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.entity.EntityBee;
import net.celestiald.cavesnotcliffs.worldgen.v118.BeeNestDecorator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.terraingen.SaplingGrowTreeEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Growth bridge for the 1.18 oak/birch sapling flower rule.
 *
 * <p>The Forge event captures the pre-growth tree state. A narrow core hook calls
 * {@link #finishGrowth(World, BlockPos, Random)} at every return from the vanilla sapling
 * generation method, after that individual tree has consumed its random stream. This preserves
 * 1.18's tree-then-decorator ordering even when several saplings grow in one tick.</p>
 */
public final class BeeSaplingNestHandler {
    public static final BeeSaplingNestHandler INSTANCE = new BeeSaplingNestHandler();
    private final Map<World, List<PendingGrowth>> pending = new WeakHashMap<>();

    private BeeSaplingNestHandler() {
    }

    @SubscribeEvent
    public void onSaplingGrow(SaplingGrowTreeEvent event) {
        World world = event.getWorld();
        if (world.isRemote) {
            return;
        }
        IBlockState state = world.getBlockState(event.getPos());
        if (state.getBlock() != Blocks.SAPLING) {
            return;
        }
        BlockPlanks.EnumType type = state.getValue(BlockSapling.TYPE);
        if (type != BlockPlanks.EnumType.OAK && type != BlockPlanks.EnumType.BIRCH) {
            return;
        }
        if (!hasFlowers(world, event.getPos())) {
            return;
        }
        pending.computeIfAbsent(world, ignored -> new ArrayList<>())
                .add(new PendingGrowth(event.getPos(), event.getRand(),
                        snapshotTreeBlocks(world, event.getPos())));
    }

    public static void finishGrowth(World world, BlockPos origin, Random random) {
        INSTANCE.finish(world, origin, random);
    }

    private void finish(World world, BlockPos origin, Random random) {
        List<PendingGrowth> requests = pending.get(world);
        if (requests == null) {
            return;
        }
        for (int index = requests.size() - 1; index >= 0; index--) {
            PendingGrowth request = requests.get(index);
            if (!request.origin.equals(origin) || request.random != random) {
                continue;
            }
            requests.remove(index);
            if (requests.isEmpty()) {
                pending.remove(world);
            }
            decorateFinishedTree(world, request);
            return;
        }
    }

    private static void decorateFinishedTree(World world, PendingGrowth request) {
        TreeBlocks tree = collectFinishedTree(world, request.origin,
                request.preexistingTreeBlocks);
        if (tree.trunks.isEmpty()) {
            return;
        }
        BeeNestDecorator.placeInWorld(world, request.random,
                BeeNestDecorator.PROBABILITY_005, tree.trunks, tree.foliage);
    }

    static Set<BlockPos> snapshotTreeBlocks(IBlockAccess world, BlockPos origin) {
        Set<BlockPos> result = new HashSet<>();
        for (BlockPos candidate : scanBox(origin)) {
            Block block = world.getBlockState(candidate).getBlock();
            if (isTreeBlock(block)) {
                result.add(candidate.toImmutable());
            }
        }
        return result;
    }

    static TreeBlocks collectFinishedTree(IBlockAccess world, BlockPos origin,
            Set<BlockPos> preexistingTreeBlocks) {
        if (!isLog(world.getBlockState(origin).getBlock())) {
            return TreeBlocks.EMPTY;
        }
        Set<BlockPos> newLogs = new HashSet<>();
        Set<BlockPos> newLeaves = new HashSet<>();
        for (BlockPos candidate : scanBox(origin)) {
            BlockPos immutable = candidate.toImmutable();
            if (preexistingTreeBlocks.contains(immutable)) {
                continue;
            }
            Block block = world.getBlockState(candidate).getBlock();
            if (isLog(block)) {
                newLogs.add(immutable);
            } else if (isLeaves(block)) {
                newLeaves.add(immutable);
            }
        }
        BlockPos immutableOrigin = origin.toImmutable();
        if (!newLogs.contains(immutableOrigin)) {
            return TreeBlocks.EMPTY;
        }

        Set<BlockPos> anchoredLogs = connectedComponent(newLogs,
                Collections.singleton(immutableOrigin));
        Set<BlockPos> foliageSeeds = neighboringPositions(newLeaves, anchoredLogs);
        Set<BlockPos> anchoredLeaves = connectedComponent(newLeaves, foliageSeeds);

        List<BlockPos> trunks = new ArrayList<>(anchoredLogs);
        List<BlockPos> foliage = new ArrayList<>(anchoredLeaves);
        Comparator<BlockPos> order = Comparator.comparingInt(BlockPos::getY)
                .thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ);
        trunks.sort(order);
        foliage.sort(order);
        return new TreeBlocks(trunks, foliage);
    }

    private static Iterable<BlockPos> scanBox(BlockPos origin) {
        return BlockPos.getAllInBox(origin.add(-4, 0, -4),
                origin.add(4, 20, 4));
    }

    private static Set<BlockPos> connectedComponent(Set<BlockPos> candidates,
            Set<BlockPos> seeds) {
        Set<BlockPos> connected = new HashSet<>();
        Deque<BlockPos> frontier = new ArrayDeque<>();
        for (BlockPos seed : seeds) {
            if (candidates.contains(seed) && connected.add(seed)) {
                frontier.add(seed);
            }
        }
        while (!frontier.isEmpty()) {
            BlockPos current = frontier.removeFirst();
            forEachNeighbor(current, neighbor -> {
                if (candidates.contains(neighbor) && connected.add(neighbor)) {
                    frontier.addLast(neighbor);
                }
            });
        }
        return connected;
    }

    private static Set<BlockPos> neighboringPositions(Set<BlockPos> candidates,
            Set<BlockPos> anchors) {
        Set<BlockPos> result = new HashSet<>();
        for (BlockPos anchor : anchors) {
            forEachNeighbor(anchor, neighbor -> {
                if (candidates.contains(neighbor)) {
                    result.add(neighbor);
                }
            });
        }
        return result;
    }

    private static void forEachNeighbor(BlockPos center,
            java.util.function.Consumer<BlockPos> consumer) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        consumer.accept(center.add(x, y, z));
                    }
                }
            }
        }
    }

    private static boolean isTreeBlock(Block block) {
        return isLog(block) || isLeaves(block);
    }

    private static boolean isLog(Block block) {
        return block == Blocks.LOG || block == Blocks.LOG2;
    }

    private static boolean isLeaves(Block block) {
        return block == Blocks.LEAVES || block == Blocks.LEAVES2;
    }

    private static boolean hasFlowers(World world, BlockPos sapling) {
        for (BlockPos candidate : BlockPos.getAllInBox(sapling.add(-2, -1, -2),
                sapling.add(2, 1, 2))) {
            if (EntityBee.EntityCustom.isFlowerAt(world, candidate)) {
                return true;
            }
        }
        return false;
    }

    private static final class PendingGrowth {
        final BlockPos origin;
        final Random random;
        final Set<BlockPos> preexistingTreeBlocks;

        PendingGrowth(BlockPos origin, Random random,
                Set<BlockPos> preexistingTreeBlocks) {
            this.origin = origin.toImmutable();
            this.random = random;
            this.preexistingTreeBlocks = preexistingTreeBlocks;
        }
    }

    static final class TreeBlocks {
        static final TreeBlocks EMPTY = new TreeBlocks(
                Collections.emptyList(), Collections.emptyList());

        final List<BlockPos> trunks;
        final List<BlockPos> foliage;

        TreeBlocks(List<BlockPos> trunks, List<BlockPos> foliage) {
            this.trunks = trunks;
            this.foliage = foliage;
        }
    }
}

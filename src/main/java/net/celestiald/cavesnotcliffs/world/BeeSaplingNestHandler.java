package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.entity.EntityBee;
import net.celestiald.cavesnotcliffs.worldgen.v118.BeeNestDecorator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.terraingen.SaplingGrowTreeEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

/**
 * Post-growth bridge for the 1.18 oak/birch sapling flower rule.
 *
 * <p>The event fires before 1.12 builds the tree. Work is deferred to the world-tick tail so the
 * original tree generator consumes its RNG first and the decorator receives the finished trunk
 * and foliage lists.</p>
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
                .add(new PendingGrowth(event.getPos(), event.getRand()));
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote) {
            return;
        }
        List<PendingGrowth> requests = pending.remove(event.world);
        if (requests == null) {
            return;
        }
        for (PendingGrowth request : requests) {
            decorateFinishedTree(event.world, request);
        }
    }

    private static void decorateFinishedTree(World world, PendingGrowth request) {
        List<BlockPos> trunks = new ArrayList<>();
        List<BlockPos> foliage = new ArrayList<>();
        BlockPos origin = request.origin;
        for (BlockPos candidate : BlockPos.getAllInBox(origin.add(-4, 0, -4),
                origin.add(4, 20, 4))) {
            Block block = world.getBlockState(candidate).getBlock();
            if (block == Blocks.LOG || block == Blocks.LOG2) {
                trunks.add(candidate.toImmutable());
            } else if (block == Blocks.LEAVES || block == Blocks.LEAVES2) {
                foliage.add(candidate.toImmutable());
            }
        }
        if (trunks.isEmpty()) {
            return;
        }
        Comparator<BlockPos> order = Comparator.comparingInt(BlockPos::getY)
                .thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ);
        trunks.sort(order);
        foliage.sort(order);
        BeeNestDecorator.placeInWorld(world, request.random,
                BeeNestDecorator.PROBABILITY_005, trunks, foliage);
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

        PendingGrowth(BlockPos origin, Random random) {
            this.origin = origin.toImmutable();
            this.random = random;
        }
    }
}

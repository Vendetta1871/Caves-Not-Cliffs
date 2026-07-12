package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.Explosion;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Java 1.12 bridge for the source-water fluid state retained by lush-cave blocks in 1.18.
 *
 * <p>A normal block tick cannot represent the fluid tick: big dripleaf already uses scheduled
 * ticks for its exact 10/10/100 tilt delays. A small per-world queue therefore delivers the
 * five-tick water update independently, matching {@code SimpleWaterloggedBlock#updateShape}
 * without perturbing the plant state machines.</p>
 */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class LushWaterlogging {
    public static final int WATER_TICK_DELAY = 5;
    private static final EnumFacing[] FLOW_DIRECTIONS = {
            EnumFacing.DOWN, EnumFacing.NORTH, EnumFacing.SOUTH,
            EnumFacing.WEST, EnumFacing.EAST
    };
    private static final Map<World, Map<BlockPos, Long>> PENDING = new WeakHashMap<>();

    private LushWaterlogging() {
    }

    public static void schedule(World world, BlockPos pos, boolean waterlogged) {
        if (!waterlogged || world == null || world.isRemote) {
            return;
        }
        long due = dueTime(world.getTotalWorldTime());
        synchronized (PENDING) {
            Map<BlockPos, Long> ticks = PENDING.get(world);
            if (ticks == null) {
                ticks = new HashMap<>();
                PENDING.put(world, ticks);
            }
            BlockPos immutable = pos.toImmutable();
            Long existing = ticks.get(immutable);
            if (existing == null || due < existing) {
                ticks.put(immutable, due);
            }
        }
    }

    public static long dueTime(long currentWorldTime) {
        return currentWorldTime + WATER_TICK_DELAY;
    }

    public static EnumFacing[] flowDirections() {
        return FLOW_DIRECTIONS.clone();
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world == null
                || event.world.isRemote) {
            return;
        }
        World world = event.world;
        for (BlockPos pos : drainDue(world, world.getTotalWorldTime())) {
            if (world.isBlockLoaded(pos)
                    && CncFluidState.containsWater(world.getBlockState(pos))) {
                emitSourceWater(world, pos);
            }
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        synchronized (PENDING) {
            PENDING.remove(event.getWorld());
        }
    }

    private static List<BlockPos> drainDue(World world, long now) {
        List<BlockPos> due = new ArrayList<>();
        synchronized (PENDING) {
            Map<BlockPos, Long> ticks = PENDING.get(world);
            if (ticks == null) {
                return due;
            }
            Iterator<Map.Entry<BlockPos, Long>> iterator = ticks.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<BlockPos, Long> tick = iterator.next();
                if (tick.getValue() <= now) {
                    due.add(tick.getKey());
                    iterator.remove();
                }
            }
            if (ticks.isEmpty()) {
                PENDING.remove(world);
            }
        }
        return due;
    }

    /** A source spreads down and horizontally in 1.18; it never creates water above itself. */
    public static void emitSourceWater(World world, BlockPos pos) {
        if (world == null || world.isRemote) {
            return;
        }
        for (EnumFacing direction : FLOW_DIRECTIONS) {
            BlockPos neighbor = pos.offset(direction);
            if (world.isAirBlock(neighbor)) {
                world.setBlockState(neighbor, Blocks.FLOWING_WATER.getDefaultState(), 2);
            }
        }
    }

    public static boolean isWaterlogged(IBlockState state) {
        if (state == null) {
            return false;
        }
        Block block = state.getBlock();
        if (block instanceof LushWaterloggedBlock) {
            return ((LushWaterloggedBlock) block).hasRetainedWater(state);
        }
        return false;
    }

    public static Boolean isEntityInsideMaterial(boolean waterlogged, Material requested) {
        if (!waterlogged) {
            return null;
        }
        return requested == Material.WATER;
    }

    public static Boolean isAabbInsideMaterial(boolean waterlogged, Material requested,
            AxisAlignedBB bounds, BlockPos pos) {
        if (!waterlogged) {
            return null;
        }
        return requested == Material.WATER && intersectsSource(bounds, pos);
    }

    public static Boolean isAabbInsideLiquid(boolean waterlogged, AxisAlignedBB bounds,
            BlockPos pos) {
        return waterlogged ? intersectsSource(bounds, pos) : null;
    }

    public static boolean intersectsSource(AxisAlignedBB bounds, BlockPos pos) {
        return bounds != null && pos != null && bounds.intersects(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1.0D, pos.getY() + 1.0D, pos.getZ() + 1.0D);
    }

    public static IBlockState removalState(boolean waterlogged) {
        return waterlogged ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState();
    }

    /** PathNodeType.WATER is ordinal six in 1.12; ordinal lookup survives SRG reobfuscation. */
    public static PathNodeType waterPathNodeType() {
        return PathNodeType.values()[6];
    }

    public static void restoreAfterRemoval(World world, BlockPos pos, boolean waterlogged) {
        if (waterlogged && world.isAirBlock(pos)) {
            world.setBlockState(pos, Blocks.WATER.getDefaultState(), 3);
        }
    }
}

/** Common fluid/entity/removal hooks for the four lush blocks that retain source water. */
abstract class LushWaterloggedBlock extends Block {
    LushWaterloggedBlock(Material material) {
        super(material);
    }

    LushWaterloggedBlock(Material material, MapColor color) {
        super(material, color);
    }

    protected abstract boolean hasRetainedWater(IBlockState state);

    protected final void scheduleRetainedWater(World world, BlockPos pos, IBlockState state) {
        LushWaterlogging.schedule(world, pos, hasRetainedWater(state));
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        super.onBlockAdded(world, pos, state);
        scheduleRetainedWater(world, pos, state);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        super.breakBlock(world, pos, state);
        LushWaterlogging.restoreAfterRemoval(world, pos, hasRetainedWater(state));
    }

    @Override
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
        IBlockState state = world.getBlockState(pos);
        super.onBlockExploded(world, pos, explosion);
        LushWaterlogging.restoreAfterRemoval(world, pos, hasRetainedWater(state));
    }

    @Override
    public Boolean isEntityInsideMaterial(IBlockAccess world, BlockPos pos, IBlockState state,
            Entity entity, double yToTest, Material material, boolean testingHead) {
        return LushWaterlogging.isEntityInsideMaterial(hasRetainedWater(state), material);
    }

    @Override
    public Boolean isAABBInsideMaterial(World world, BlockPos pos, AxisAlignedBB bounds,
            Material material) {
        return LushWaterlogging.isAabbInsideMaterial(hasRetainedWater(
                world.getBlockState(pos)), material, bounds, pos);
    }

    @Override
    public Boolean isAABBInsideLiquid(World world, BlockPos pos, AxisAlignedBB bounds) {
        return LushWaterlogging.isAabbInsideLiquid(hasRetainedWater(
                world.getBlockState(pos)), bounds, pos);
    }

    @Override
    public float getBlockLiquidHeight(World world, BlockPos pos, IBlockState state,
            Material material) {
        return hasRetainedWater(state) && material == Material.WATER ? 1.0F : 0.0F;
    }

    @Override
    public IBlockState getStateAtViewpoint(IBlockState state, IBlockAccess world,
            BlockPos pos, Vec3d viewpoint) {
        return hasRetainedWater(state) && viewpoint.y < pos.getY() + 1.0D
                ? Blocks.WATER.getDefaultState() : state;
    }

    @Override
    public PathNodeType getAiPathNodeType(IBlockState state, IBlockAccess world,
            BlockPos pos) {
        return hasRetainedWater(state) ? LushWaterlogging.waterPathNodeType()
                : super.getAiPathNodeType(state, world, pos);
    }
}

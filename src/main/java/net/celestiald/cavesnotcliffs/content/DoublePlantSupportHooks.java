package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.block.LushAzaleaBlocks;
import net.celestiald.cavesnotcliffs.block.LushMossBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.List;

/** Exact Java 1.18.2 tall-grass and large-fern support, pairing, and seed drops. */
public final class DoublePlantSupportHooks {
    private static final int OVERWORLD_MIN_Y = -64;
    private static final int OVERWORLD_MAX_Y_EXCLUSIVE = 320;
    private static final ThreadLocal<HarvestPair> HARVEST_PAIR = new ThreadLocal<>();

    private DoublePlantSupportHooks() {
    }

    public static boolean canLowerSurvive(World world, BlockPos lowerPos) {
        return world != null && lowerPos != null
            && isFutureSupport(world.getBlockState(lowerPos.down()));
    }

    public static boolean usesJava118Survival(World world, BlockPos pos, IBlockState state) {
        if (!isFiniteOverworld(world, pos) || state == null
                || state.getBlock() != Blocks.DOUBLE_PLANT) {
            return false;
        }
        if (state.getValue(BlockDoublePlant.HALF)
                == BlockDoublePlant.EnumBlockHalf.LOWER) {
            return isRepresentedLower(state);
        }
        return isRepresentedLower(world.getBlockState(pos.down()));
    }

    public static boolean canStay(World world, BlockPos pos, IBlockState state) {
        if (world == null || pos == null || state == null
                || state.getBlock() != Blocks.DOUBLE_PLANT) {
            return false;
        }
        if (state.getValue(BlockDoublePlant.HALF)
                == BlockDoublePlant.EnumBlockHalf.UPPER) {
            return isRepresentedLower(world.getBlockState(pos.down()));
        }
        if (!isRepresentedLower(state)) {
            return false;
        }
        IBlockState upper = world.getBlockState(pos.up());
        return upper.getBlock() == Blocks.DOUBLE_PLANT
            && upper.getValue(BlockDoublePlant.HALF)
                == BlockDoublePlant.EnumBlockHalf.UPPER
            && canLowerSurvive(world, pos);
    }

    /** Removes an invalid represented pair without running 1.12's support-loss loot path. */
    public static boolean handleCheckAndDrop(World world, BlockPos pos, IBlockState state) {
        if (!usesJava118Survival(world, pos, state)) {
            return false;
        }
        if (canStay(world, pos, state)) {
            return true;
        }

        HARVEST_PAIR.remove();
        BlockPos lowerPos = state.getValue(BlockDoublePlant.HALF)
            == BlockDoublePlant.EnumBlockHalf.UPPER ? pos.down() : pos;
        BlockPos upperPos = lowerPos.up();
        IBlockState upper = world.getBlockState(upperPos);
        if (upper.getBlock() == Blocks.DOUBLE_PLANT
                && upper.getValue(BlockDoublePlant.HALF)
                    == BlockDoublePlant.EnumBlockHalf.UPPER) {
            world.setBlockState(upperPos, Blocks.AIR.getDefaultState(), 2);
        }
        if (isRepresentedLower(world.getBlockState(lowerPos))) {
            world.setBlockState(lowerPos, Blocks.AIR.getDefaultState(), 3);
        }
        return true;
    }

    /** Captures the intact pair before 1.12 removes the upper half ahead of lower-half loot. */
    public static void beginHarvest(World world, BlockPos pos, IBlockState state,
            EntityPlayer player) {
        HARVEST_PAIR.remove();
        if (world == null || world.isRemote || player == null
                || player.capabilities.isCreativeMode
                || !isFiniteOverworld(world, pos)) {
            return;
        }
        BlockPos lowerPos = representedLowerPos(world, pos, state);
        if (lowerPos == null || !hasUpper(world, lowerPos)) {
            return;
        }
        IBlockState lower = world.getBlockState(lowerPos);
        HARVEST_PAIR.set(new HarvestPair(world, lowerPos,
            lower.getValue(BlockDoublePlant.VARIANT)));
    }

    /** Pair-aware loot from either half, with a live-pair fallback for direct destruction. */
    public static boolean addDrops(NonNullList<ItemStack> drops, IBlockAccess access,
            BlockPos pos, IBlockState state, int fortune) {
        if (!(access instanceof World)) {
            return false;
        }
        World world = (World) access;
        if (!isFiniteOverworld(world, pos) || state == null
                || state.getBlock() != Blocks.DOUBLE_PLANT) {
            return false;
        }

        HarvestPair captured = HARVEST_PAIR.get();
        BlockPos lowerPos = representedLowerPos(world, pos, state);
        BlockDoublePlant.EnumPlantType type;
        if (lowerPos != null) {
            IBlockState lower = state.getValue(BlockDoublePlant.HALF)
                == BlockDoublePlant.EnumBlockHalf.LOWER
                    ? state : world.getBlockState(lowerPos);
            type = lower.getValue(BlockDoublePlant.VARIANT);
        } else if (state.getValue(BlockDoublePlant.HALF)
                == BlockDoublePlant.EnumBlockHalf.UPPER) {
            // The 1.12 upper state carries no plant variant. A harvest snapshot can still
            // identify a just-removed represented lower; every other orphan upper already
            // has the same empty-drop result under the retained 1.12 families.
            if (captured == null || !captured.matchesUpper(world, pos)) {
                return true;
            }
            lowerPos = captured.lowerPos;
            type = captured.type;
        } else {
            return false;
        }

        boolean paired = captured != null && captured.matches(world, lowerPos, type);
        if (paired) {
            HARVEST_PAIR.remove();
        }
        if (!paired) {
            paired = hasUpper(world, lowerPos);
        }
        if (paired && world.rand.nextFloat() < 0.125F) {
            drops.add(new ItemStack(Items.WHEAT_SEEDS));
        }
        return true;
    }

    public static void endHarvest() {
        HARVEST_PAIR.remove();
    }

    /** Selects represented halves, including an orphan lower that must not be shearable. */
    public static boolean usesJava118Shearing(IBlockAccess access, BlockPos pos) {
        if (!(access instanceof World)) {
            return false;
        }
        World world = (World) access;
        if (!isFiniteOverworld(world, pos)) {
            return false;
        }
        IBlockState state = world.getBlockState(pos);
        return isRepresentedLower(state)
            || state.getBlock() == Blocks.DOUBLE_PLANT
                && state.getValue(BlockDoublePlant.HALF)
                    == BlockDoublePlant.EnumBlockHalf.UPPER
                && isRepresentedLower(world.getBlockState(pos.down()));
    }

    public static boolean canShear(IBlockAccess access, BlockPos pos) {
        if (!(access instanceof World)) {
            return false;
        }
        World world = (World) access;
        BlockPos lowerPos = representedLowerPos(world, pos, world.getBlockState(pos));
        return lowerPos != null && hasUpper(world, lowerPos);
    }

    public static List<ItemStack> shearedDrops(IBlockAccess access, BlockPos pos) {
        if (!(access instanceof World)) {
            return Collections.emptyList();
        }
        World world = (World) access;
        BlockPos lowerPos = representedLowerPos(world, pos, world.getBlockState(pos));
        if (lowerPos == null || !hasUpper(world, lowerPos)) {
            return Collections.emptyList();
        }
        BlockDoublePlant.EnumPlantType type = world.getBlockState(lowerPos)
            .getValue(BlockDoublePlant.VARIANT);
        int shortTypeMetadata = type == BlockDoublePlant.EnumPlantType.byMetadata(3)
            ? 2 : 1;
        return Collections.singletonList(new ItemStack(Blocks.TALLGRASS, 2,
            shortTypeMetadata));
    }

    private static boolean isFiniteOverworld(World world, BlockPos pos) {
        if (world == null || pos == null || world.provider == null
                || world.provider.getDimension() != 0) {
            return false;
        }
        int y = pos.getY();
        return y >= OVERWORLD_MIN_Y && y < OVERWORLD_MAX_Y_EXCLUSIVE;
    }

    private static BlockPos representedLowerPos(World world, BlockPos pos,
            IBlockState state) {
        if (world == null || pos == null || state == null
                || state.getBlock() != Blocks.DOUBLE_PLANT) {
            return null;
        }
        if (state.getValue(BlockDoublePlant.HALF)
                == BlockDoublePlant.EnumBlockHalf.LOWER) {
            return isRepresentedLower(state) ? pos : null;
        }
        BlockPos lowerPos = pos.down();
        return isRepresentedLower(world.getBlockState(lowerPos)) ? lowerPos : null;
    }

    private static boolean hasUpper(IBlockAccess world, BlockPos lowerPos) {
        IBlockState upper = world.getBlockState(lowerPos.up());
        return upper.getBlock() == Blocks.DOUBLE_PLANT
            && upper.getValue(BlockDoublePlant.HALF)
                == BlockDoublePlant.EnumBlockHalf.UPPER;
    }

    private static boolean isRepresentedLower(IBlockState state) {
        if (state == null || state.getBlock() != Blocks.DOUBLE_PLANT
                || state.getValue(BlockDoublePlant.HALF)
                    != BlockDoublePlant.EnumBlockHalf.LOWER) {
            return false;
        }
        BlockDoublePlant.EnumPlantType type = state.getValue(BlockDoublePlant.VARIANT);
        return type == BlockDoublePlant.EnumPlantType.byMetadata(2)
            || type == BlockDoublePlant.EnumPlantType.byMetadata(3);
    }

    private static boolean isFutureSupport(IBlockState state) {
        Block block = state.getBlock();
        return block == Blocks.DIRT || block == Blocks.GRASS
            || block == Blocks.MYCELIUM || block == Blocks.FARMLAND
            || block == LushCaveContent.ROOTED_DIRT
            || block == LushCaveContent.MOSS_BLOCK
            || block instanceof LushAzaleaBlocks.RootedDirt
            || block instanceof LushMossBlocks.Moss;
    }

    private static final class HarvestPair {
        private final World world;
        private final BlockPos lowerPos;
        private final BlockDoublePlant.EnumPlantType type;

        private HarvestPair(World world, BlockPos lowerPos,
                BlockDoublePlant.EnumPlantType type) {
            this.world = world;
            this.lowerPos = new BlockPos(lowerPos.getX(), lowerPos.getY(), lowerPos.getZ());
            this.type = type;
        }

        private boolean matches(World candidateWorld, BlockPos candidatePos,
                BlockDoublePlant.EnumPlantType candidateType) {
            return world == candidateWorld && lowerPos.equals(candidatePos)
                && type == candidateType;
        }

        private boolean matchesUpper(World candidateWorld, BlockPos upperPos) {
            return world == candidateWorld && lowerPos.up().equals(upperPos);
        }
    }
}

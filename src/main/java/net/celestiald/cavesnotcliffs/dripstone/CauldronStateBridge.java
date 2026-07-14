package net.celestiald.cavesnotcliffs.dripstone;

import net.celestiald.cavesnotcliffs.block.BlockLavaCauldron;
import net.celestiald.cavesnotcliffs.block.BlockPowderSnowCauldron;
import net.celestiald.cavesnotcliffs.dripstone.CauldronMechanics.Content;
import net.celestiald.cavesnotcliffs.dripstone.CauldronMechanics.State;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Converts 1.12's stateful vanilla cauldron into the hidden 1.18 content identities and performs
 * every cauldron mutation with {@code setBlockAndUpdate} equivalent notifications.
 */
public final class CauldronStateBridge {
    /** 1.12's flags 1 (neighbors/observers) + 2 (client synchronization). */
    public static final int UPDATE_FLAGS = 3;

    private CauldronStateBridge() {
    }

    /** Minimal mutation surface used by the observer/comparator contract test. */
    interface UpdateAccess {
        boolean set(IBlockState state, int flags);

        void updateComparator(Block block);
    }

    public static boolean isVanillaCauldron(IBlockState state) {
        return state != null && state.getBlock() == Blocks.CAULDRON;
    }

    /** Maps all four vanilla 1.12 levels without changing unrelated or modded cauldrons. */
    public static IBlockState bridgeVanillaState(IBlockState state, Block storage) {
        if (!isVanillaCauldron(state)) {
            return state;
        }
        BlockLavaCauldron.BlockCustom cauldron = requirePrimaryStorage(storage);
        int level = state.getValue(BlockCauldron.LEVEL);
        return cauldron.blockState(level == 0
                ? CauldronMechanics.empty() : CauldronMechanics.water(level));
    }

    public static IBlockState stateFor(State contents, Block primaryStorage,
            Block powderSnowStorage) {
        if (contents == null) {
            throw new IllegalArgumentException("Cauldron contents are required");
        }
        if (contents.content == Content.POWDER_SNOW) {
            if (!(powderSnowStorage instanceof BlockPowderSnowCauldron.BlockCustom)) {
                throw new IllegalStateException("Powder-snow cauldron storage is unavailable");
            }
            return powderSnowStorage.getDefaultState()
                    .withProperty(BlockCauldron.LEVEL, contents.level);
        }
        return requirePrimaryStorage(primaryStorage).blockState(contents);
    }

    public static IBlockState stateFor(State contents) {
        return stateFor(contents, BlockLavaCauldron.block, BlockPowderSnowCauldron.block);
    }

    public static boolean bridgeVanillaAt(World world, BlockPos pos) {
        IBlockState current = world.getBlockState(pos);
        if (!isVanillaCauldron(current)) {
            return false;
        }
        return setState(world, pos, bridgeVanillaState(current, BlockLavaCauldron.block));
    }

    public static boolean setContents(World world, BlockPos pos, State contents) {
        return setState(world, pos, stateFor(contents));
    }

    public static boolean setState(final World world, final BlockPos pos,
            IBlockState next) {
        if (world == null || pos == null) {
            throw new IllegalArgumentException("World and cauldron position are required");
        }
        return setState(new UpdateAccess() {
            @Override
            public boolean set(IBlockState state, int flags) {
                return world.setBlockState(pos, state, flags);
            }

            @Override
            public void updateComparator(Block block) {
                world.updateComparatorOutputLevel(pos, block);
            }
        }, next);
    }

    static boolean setState(UpdateAccess access, IBlockState next) {
        if (access == null || next == null) {
            throw new IllegalArgumentException("Cauldron update access and state are required");
        }
        boolean changed = access.set(next, UPDATE_FLAGS);
        // setBlockAndUpdate does not cover the explicit 1.12 comparator cache notification.
        access.updateComparator(next.getBlock());
        return changed;
    }

    private static BlockLavaCauldron.BlockCustom requirePrimaryStorage(Block storage) {
        if (!(storage instanceof BlockLavaCauldron.BlockCustom)) {
            throw new IllegalStateException("Primary cauldron storage is unavailable");
        }
        return (BlockLavaCauldron.BlockCustom) storage;
    }
}

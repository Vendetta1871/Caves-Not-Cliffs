package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.content.CncMaterialContent;
import net.minecraft.block.Block;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

/** The sole growth source for the Java 1.18.2 amethyst lifecycle. */
public final class BlockBuddingAmethyst extends BlockAmethystBase {
    public BlockBuddingAmethyst() {
        super("budding_amethyst");
        setTickRandomly(true);
    }

    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (random.nextInt(5) != 0) {
            return;
        }

        EnumFacing direction = EnumFacing.values()[random.nextInt(EnumFacing.values().length)];
        BlockPos growthPos = pos.offset(direction);
        IBlockState growthState = world.getBlockState(growthPos);
        boolean sourceWater = growthState.getBlock() == Blocks.WATER
                && growthState.getValue(net.minecraft.block.BlockLiquid.LEVEL) == 0;
        boolean retainedWater = growthState.getBlock() instanceof BlockAmethystGrowth
                && ((BlockAmethystGrowth) growthState.getBlock()).isWaterlogged(growthState);

        Block next = null;
        if (growthState.getBlock().isAir(growthState, world, growthPos) || sourceWater) {
            next = CncMaterialContent.block("small_amethyst_bud");
        } else if (hasMatchingFacing(growthState, direction, "small_amethyst_bud")) {
            next = CncMaterialContent.block("medium_amethyst_bud");
        } else if (hasMatchingFacing(growthState, direction, "medium_amethyst_bud")) {
            next = CncMaterialContent.block("large_amethyst_bud");
        } else if (hasMatchingFacing(growthState, direction, "large_amethyst_bud")) {
            next = CncMaterialContent.block("amethyst_cluster");
        }

        if (next instanceof BlockAmethystGrowth) {
            BlockAmethystGrowth nextGrowth = (BlockAmethystGrowth) next;
            boolean waterlogged = sourceWater || retainedWater;
            if (waterlogged) {
                BlockAmethystGrowth companion = nextGrowth.getWaterloggedCompanion();
                if (companion == null) {
                    return;
                }
                nextGrowth = companion;
            }
            world.setBlockState(growthPos, nextGrowth.getDefaultState()
                    .withProperty(BlockAmethystGrowth.WATERLOGGED, waterlogged)
                    .withProperty(BlockAmethystGrowth.FACING, direction), 3);
        }
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return Items.AIR;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    protected boolean canSilkHarvest() {
        return false;
    }

    @Override
    public EnumPushReaction getMobilityFlag(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    private static boolean hasMatchingFacing(IBlockState state, EnumFacing direction,
            String expectedName) {
        return state.getBlock() instanceof BlockAmethystGrowth
                && expectedName.equals(
                        ((BlockAmethystGrowth) state.getBlock()).getPublicStagePath())
                && state.getValue(BlockAmethystGrowth.FACING) == direction;
    }
}

package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.content.LushCaveContent;
import net.celestiald.cavesnotcliffs.content.LushCaveSounds;
import net.celestiald.cavesnotcliffs.content.MossBonemealFeature;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/** Faithful moss block and one-pixel moss carpet implementations. */
public final class LushMossBlocks {
    private LushMossBlocks() {
    }

    public static final class Moss extends Block implements IGrowable {
        public Moss() {
            super(Material.GRASS, MapColor.GREEN);
            setUnlocalizedName("moss_block");
            setHardness(0.1F);
            setSoundType(LushCaveSounds.MOSS);
            setCreativeTab(net.minecraft.creativetab.CreativeTabs.BUILDING_BLOCKS);
        }

        @Override
        public boolean canGrow(World world, BlockPos pos, IBlockState state,
                boolean isClient) {
            return world.isAirBlock(pos.up());
        }

        @Override
        public boolean canUseBonemeal(World world, Random random, BlockPos pos,
                IBlockState state) {
            return true;
        }

        @Override
        public void grow(World world, Random random, BlockPos pos, IBlockState state) {
            MossBonemealFeature.place(world, random, pos.up(), this,
                    LushCaveContent.MOSS_CARPET,
                    LushCaveContent.AZALEA,
                    LushCaveContent.FLOWERING_AZALEA);
        }
    }

    public static final class Carpet extends Block {
        private static final AxisAlignedBB SHAPE =
                new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D / 16.0D, 1.0D);

        public Carpet() {
            super(Material.PLANTS, MapColor.GREEN);
            setUnlocalizedName("moss_carpet");
            setHardness(0.1F);
            setSoundType(LushCaveSounds.MOSS_CARPET);
            setCreativeTab(net.minecraft.creativetab.CreativeTabs.DECORATIONS);
        }

        @Override
        public boolean canPlaceBlockAt(World world, BlockPos pos) {
            return world.getBlockState(pos.down()).isSideSolid(world, pos.down(),
                    EnumFacing.UP);
        }

        @Override
        public void neighborChanged(IBlockState state, World world, BlockPos pos,
                Block changed, BlockPos changedPos) {
            if (!canPlaceBlockAt(world, pos)) {
                dropBlockAsItem(world, pos, state, 0);
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
            }
        }

        @Override public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world,
                BlockPos pos) { return SHAPE; }
        @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState state,
                IBlockAccess world, BlockPos pos) { return SHAPE; }
        @Override public boolean isOpaqueCube(IBlockState state) { return false; }
        @Override public boolean isFullCube(IBlockState state) { return false; }
    }
}

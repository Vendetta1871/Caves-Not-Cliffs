package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.content.LushCaveContent;
import net.celestiald.cavesnotcliffs.content.LushCaveSounds;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/** Ceiling support and fourteen-attempt ambient particle contract from Java 1.18.2. */
public final class LushSporeBlossomBlock extends Block {
    private static final AxisAlignedBB SHAPE =
            new AxisAlignedBB(2.0D / 16.0D, 13.0D / 16.0D, 2.0D / 16.0D,
                    14.0D / 16.0D, 1.0D, 14.0D / 16.0D);

    public LushSporeBlossomBlock() {
        super(Material.PLANTS);
        setUnlocalizedName("spore_blossom");
        setHardness(0.0F);
        setSoundType(LushCaveSounds.SPORE_BLOSSOM);
        setCreativeTab(net.minecraft.creativetab.CreativeTabs.DECORATIONS);
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return world.getBlockState(pos).getMaterial() != Material.WATER
                && world.getBlockState(pos.up()).isSideSolid(world, pos.up(),
                EnumFacing.DOWN);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos,
            Block changed, BlockPos changedPos) {
        if (!canPlaceBlockAt(world, pos)) {
            dropBlockAsItem(world, pos, state, 0);
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos,
            Random random) {
        world.spawnParticle(EnumParticleTypes.FALLING_DUST,
                pos.getX() + random.nextDouble(), pos.getY() + 0.7D,
                pos.getZ() + random.nextDouble(), 0.0D, 0.0D, 0.0D,
                Block.getStateId(LushCaveContent.MOSS_BLOCK.getDefaultState()));
        for (int attempt = 0; attempt < 14; attempt++) {
            BlockPos particlePos = pos.add(-10 + random.nextInt(21),
                    -random.nextInt(10), -10 + random.nextInt(21));
            if (world.getBlockState(particlePos).isFullCube()) {
                continue;
            }
            world.spawnParticle(EnumParticleTypes.SPELL_MOB_AMBIENT,
                    particlePos.getX() + random.nextDouble(),
                    particlePos.getY() + random.nextDouble(),
                    particlePos.getZ() + random.nextDouble(),
                    0.22D, 0.72D, 0.18D);
        }
    }

    @Override public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world,
            BlockPos pos) { return SHAPE; }
    @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState state,
            IBlockAccess world, BlockPos pos) { return NULL_AABB; }
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public int getFlammability(IBlockAccess world, BlockPos pos,
            EnumFacing face) { return 100; }
    @Override public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos,
            EnumFacing face) { return 60; }
    @SideOnly(Side.CLIENT) @Override public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}

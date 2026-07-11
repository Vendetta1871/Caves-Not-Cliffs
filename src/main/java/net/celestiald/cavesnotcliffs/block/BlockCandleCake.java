package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.content.CandleMechanics;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/** Hidden block-state companion representing a full cake topped with one candle. */
public final class BlockCandleCake extends Block implements CandleLightable {
    public static final PropertyBool LIT = PropertyBool.create("lit");
    public static final AxisAlignedBB CAKE_SHAPE = new AxisAlignedBB(
            1.0D / 16.0D, 0.0D, 1.0D / 16.0D,
            15.0D / 16.0D, 8.0D / 16.0D, 15.0D / 16.0D);
    public static final AxisAlignedBB CANDLE_SHAPE = new AxisAlignedBB(
            7.0D / 16.0D, 8.0D / 16.0D, 7.0D / 16.0D,
            9.0D / 16.0D, 14.0D / 16.0D, 9.0D / 16.0D);
    public static final AxisAlignedBB OUTLINE_SHAPE = new AxisAlignedBB(
            1.0D / 16.0D, 0.0D, 1.0D / 16.0D,
            15.0D / 16.0D, 14.0D / 16.0D, 15.0D / 16.0D);
    private static final List<CandleMechanics.ParticleOffset> PARTICLE_OFFSET =
            java.util.Collections.singletonList(
                    newCakeOffset());

    private final CandleMechanics.Color color;

    public BlockCandleCake(CandleMechanics.Color color) {
        super(Material.CAKE);
        this.color = color;
        setUnlocalizedName(color.getCandleCakePath());
        setHardness(0.5F);
        setResistance(0.5F);
        setSoundType(SoundType.CLOTH);
        setLightOpacity(0);
        setDefaultState(blockState.getBaseState().withProperty(LIT, false));
    }

    public CandleMechanics.Color getColor() {
        return color;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, LIT);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(LIT, (meta & 1) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(LIT) ? 1 : 0;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world,
            BlockPos pos) {
        return OUTLINE_SHAPE;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world,
            BlockPos pos) {
        return OUTLINE_SHAPE;
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
            AxisAlignedBB entityBox, List<AxisAlignedBB> boxes,
            @Nullable Entity entity, boolean actualState) {
        addCollisionBoxToList(pos, entityBox, boxes, CAKE_SHAPE);
        addCollisionBoxToList(pos, entityBox, boxes, CANDLE_SHAPE);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state,
            BlockPos pos, EnumFacing side) {
        return BlockFaceShape.UNDEFINED;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public int getLightValue(IBlockState state) {
        return state.getValue(LIT) ? CandleMechanics.LIGHT_PER_CANDLE : 0;
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return getLightValue(state);
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return world.getBlockState(pos.down()).getMaterial().isSolid();
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos,
            Block changed, BlockPos changedPos) {
        if (!world.isRemote && !canPlaceBlockAt(world, pos)) {
            world.setBlockToAir(pos);
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
            EntityPlayer player, EnumHand hand, EnumFacing side,
            float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (held.getItem() == Items.FLINT_AND_STEEL
                || held.getItem() == Items.FIRE_CHARGE) {
            return false;
        }
        if (CandleMechanics.extinguishesCandleCake(held.isEmpty(),
                state.getValue(LIT), hitY)) {
            if (!world.isRemote) {
                CandleEffects.extinguish(player, world, pos, state);
            }
            return true;
        }
        if (!player.canEat(false)) {
            return false;
        }
        if (!world.isRemote) {
            player.addStat(StatList.CAKE_SLICES_EATEN);
            player.getFoodStats().addStats(2, 0.1F);
            world.setBlockState(pos, Blocks.CAKE.getDefaultState()
                    .withProperty(net.minecraft.block.BlockCake.BITES, 1), 3);
            Item candle = candleItem();
            if (candle != null && candle != Items.AIR) {
                spawnAsEntity(world, pos, new ItemStack(candle));
            }
        }
        return true;
    }

    @Override
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos,
            Random random) {
        CandleEffects.animate(state, world, pos, random);
    }

    @Override
    public boolean canLight(IBlockState state) {
        return !state.getValue(LIT);
    }

    @Override
    public IBlockState withLit(IBlockState state, boolean lit) {
        return state.withProperty(LIT, lit);
    }

    @Override
    public boolean isLit(IBlockState state) {
        return state.getValue(LIT);
    }

    @Override
    public Iterable<CandleMechanics.ParticleOffset> particleOffsets(IBlockState state) {
        return PARTICLE_OFFSET;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos,
            IBlockState state, int fortune) {
        Item candle = candleItem();
        if (candle != null && candle != Items.AIR) {
            drops.add(new ItemStack(candle));
        }
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world,
            BlockPos pos, EntityPlayer player) {
        return new ItemStack(Items.CAKE);
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        return CandleMechanics.FULL_CAKE_COMPARATOR_SIGNAL;
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    private Item candleItem() {
        Block block = ForgeRegistries.BLOCKS.getValue(
                net.celestiald.cavesnotcliffs.registry.CncRegistryIds.id(
                        color.getCandlePath()));
        return block == null ? Items.AIR : Item.getItemFromBlock(block);
    }

    private static CandleMechanics.ParticleOffset newCakeOffset() {
        return CandleMechanics.cakeParticleOffset();
    }
}

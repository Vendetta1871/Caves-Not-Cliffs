package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.content.CandleMechanics;
import net.celestiald.cavesnotcliffs.content.CandleSoundEvents;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.Random;

/** All sixteen Java 1.18.2 states for one canonical candle color. */
public final class BlockCandle extends LushWaterloggedBlock implements CandleLightable {
    public static final PropertyInteger CANDLES = PropertyInteger.create("candles", 1, 4);
    public static final PropertyBool LIT = PropertyBool.create("lit");
    public static final PropertyBool WATERLOGGED = PropertyBool.create("waterlogged");

    private static final AxisAlignedBB[] SHAPES = {
            new AxisAlignedBB(7.0D / 16.0D, 0.0D, 7.0D / 16.0D,
                    9.0D / 16.0D, 6.0D / 16.0D, 9.0D / 16.0D),
            new AxisAlignedBB(5.0D / 16.0D, 0.0D, 6.0D / 16.0D,
                    11.0D / 16.0D, 6.0D / 16.0D, 9.0D / 16.0D),
            new AxisAlignedBB(5.0D / 16.0D, 0.0D, 6.0D / 16.0D,
                    10.0D / 16.0D, 6.0D / 16.0D, 11.0D / 16.0D),
            new AxisAlignedBB(5.0D / 16.0D, 0.0D, 5.0D / 16.0D,
                    11.0D / 16.0D, 6.0D / 16.0D, 10.0D / 16.0D)
    };
    public static final SoundType CANDLE_SOUND = new SoundType(1.0F, 1.0F,
            CandleSoundEvents.CANDLE_BREAK, CandleSoundEvents.CANDLE_STEP,
            CandleSoundEvents.CANDLE_PLACE, CandleSoundEvents.CANDLE_HIT,
            CandleSoundEvents.CANDLE_FALL);

    private final CandleMechanics.Color color;

    public BlockCandle(CandleMechanics.Color color) {
        super(Material.CIRCUITS);
        this.color = color;
        setUnlocalizedName(color.getCandlePath());
        setCreativeTab(CreativeTabs.DECORATIONS);
        setHardness(0.1F);
        setResistance(0.1F);
        setSoundType(CANDLE_SOUND);
        setLightOpacity(0);
        setDefaultState(blockState.getBaseState()
                .withProperty(CANDLES, 1)
                .withProperty(LIT, false)
                .withProperty(WATERLOGGED, false));
    }

    public CandleMechanics.Color getColor() {
        return color;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, CANDLES, LIT, WATERLOGGED);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        CandleMechanics.State state = CandleMechanics.stateFromMetadata(meta & 15);
        return getDefaultState().withProperty(CANDLES, state.getCandles())
                .withProperty(LIT, state.isLit())
                .withProperty(WATERLOGGED, state.isWaterlogged());
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return CandleMechanics.metadata(state.getValue(CANDLES),
                state.getValue(LIT), state.getValue(WATERLOGGED));
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
            float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer,
            EnumHand hand) {
        return getDefaultState().withProperty(WATERLOGGED,
                world.getBlockState(pos).getMaterial() == Material.WATER);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source,
            BlockPos pos) {
        return SHAPES[state.getValue(CANDLES) - 1];
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world,
            BlockPos pos) {
        return getBoundingBox(state, world, pos);
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
        return CandleMechanics.lightLevel(state.getValue(CANDLES),
                state.getValue(LIT), state.getValue(WATERLOGGED));
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return getLightValue(state);
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        BlockPos support = pos.down();
        IBlockState state = world.getBlockState(support);
        return state.getBlock().canPlaceTorchOnTop(state, world, support);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos,
            Block changed, BlockPos changedPos) {
        scheduleRetainedWater(world, pos, state);
        if (!world.isRemote && !canPlaceBlockAt(world, pos)) {
            dropBlockAsItem(world, pos, state, 0);
            world.setBlockState(pos, state.getValue(WATERLOGGED)
                    ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState(), 3);
        }
    }

    @Override
    protected boolean hasRetainedWater(IBlockState state) {
        return state.getValue(WATERLOGGED);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
            EntityPlayer player, EnumHand hand, EnumFacing side,
            float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (player.capabilities.allowEdit && held.isEmpty() && state.getValue(LIT)) {
            if (!world.isRemote) {
                CandleEffects.extinguish(player, world, pos, state);
            }
            return true;
        }
        if (!player.capabilities.allowEdit || held.isEmpty()) {
            return false;
        }
        boolean fill = held.getItem() == Items.WATER_BUCKET
                && !state.getValue(WATERLOGGED);
        boolean drain = held.getItem() == Items.BUCKET
                && state.getValue(WATERLOGGED);
        if (!fill && !drain) {
            return false;
        }
        if (!world.isRemote) {
            Item used = held.getItem();
            if (fill && state.getValue(LIT)) {
                CandleEffects.extinguish(null, world, pos, state);
                state = world.getBlockState(pos);
            }
            world.setBlockState(pos, state.withProperty(WATERLOGGED, fill)
                    .withProperty(LIT, false), 3);
            replaceContainer(player, hand, held,
                    new ItemStack(fill ? Items.BUCKET : Items.WATER_BUCKET));
            StatBase stat = StatList.getObjectUseStats(used);
            if (stat != null) {
                player.addStat(stat);
            }
            world.playSound(null, pos,
                    fill ? SoundEvents.ITEM_BUCKET_EMPTY : SoundEvents.ITEM_BUCKET_FILL,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);
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
        return CandleMechanics.canLight(state.getValue(LIT),
                state.getValue(WATERLOGGED));
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
        return CandleMechanics.particleOffsets(state.getValue(CANDLES));
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos,
            IBlockState state, int fortune) {
        Item item = Item.getItemFromBlock(this);
        if (item != Items.AIR) {
            drops.add(new ItemStack(item, state.getValue(CANDLES)));
        }
    }

    private static void replaceContainer(EntityPlayer player, EnumHand hand,
            ItemStack held, ItemStack replacement) {
        if (player.capabilities.isCreativeMode) {
            return;
        }
        held.shrink(1);
        if (held.isEmpty()) {
            player.setHeldItem(hand, replacement);
        } else if (!player.inventory.addItemStackToInventory(replacement)) {
            player.dropItem(replacement, false);
        }
    }
}

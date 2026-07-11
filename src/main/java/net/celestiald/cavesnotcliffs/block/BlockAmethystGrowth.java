package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.content.AmethystSoundEvents;
import net.celestiald.cavesnotcliffs.content.CncMaterialContent;
import net.celestiald.cavesnotcliffs.content.OreDropLogic;
import net.minecraft.block.Block;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/** One canonical implementation for all four Java 1.18.2 amethyst growth stages. */
public class BlockAmethystGrowth extends Block implements AmethystChimeSource {
    public static final PropertyBool WATERLOGGED = PropertyBool.create("waterlogged");
    public static final PropertyDirection FACING = PropertyDirection.create("facing");

    private final AxisAlignedBB northAabb;
    private final AxisAlignedBB southAabb;
    private final AxisAlignedBB eastAabb;
    private final AxisAlignedBB westAabb;
    private final AxisAlignedBB upAabb;
    private final AxisAlignedBB downAabb;
    private final int lightLevel;
    private final boolean cluster;

    public BlockAmethystGrowth(String name, int height, int offset, int lightLevel,
            boolean cluster) {
        super(Material.ROCK, MapColor.PURPLE);
        this.lightLevel = lightLevel;
        this.cluster = cluster;

        double h = height / 16.0D;
        double o = offset / 16.0D;
        double opposite = 1.0D - o;
        northAabb = new AxisAlignedBB(o, o, 1.0D - h, opposite, opposite, 1.0D);
        southAabb = new AxisAlignedBB(o, o, 0.0D, opposite, opposite, h);
        eastAabb = new AxisAlignedBB(0.0D, o, o, h, opposite, opposite);
        westAabb = new AxisAlignedBB(1.0D - h, o, o, 1.0D, opposite, opposite);
        upAabb = new AxisAlignedBB(o, 0.0D, o, opposite, h, opposite);
        downAabb = new AxisAlignedBB(o, 1.0D - h, o, opposite, 1.0D, opposite);

        setUnlocalizedName(name);
        setSoundType(AmethystSoundEvents.forGrowthStage(name));
        setHardness(1.5F);
        setResistance(2.5F);
        setDefaultState(blockState.getBaseState()
                .withProperty(WATERLOGGED, false)
                .withProperty(FACING, EnumFacing.UP));
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
    public int getLightValue(IBlockState state) {
        return lightLevel;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        switch (state.getValue(FACING)) {
            case NORTH:
                return northAabb;
            case SOUTH:
                return southAabb;
            case EAST:
                return eastAabb;
            case WEST:
                return westAabb;
            case DOWN:
                return downAabb;
            case UP:
            default:
                return upAabb;
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side) {
        return hasSupport(world, pos, side);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
            float hitX, float hitY, float hitZ, int meta,
            net.minecraft.entity.EntityLivingBase placer) {
        boolean waterlogged = world.getBlockState(pos).getMaterial() == Material.WATER;
        return getDefaultState()
                .withProperty(WATERLOGGED, waterlogged)
                .withProperty(FACING, facing);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos,
            Block changedBlock, BlockPos changedPos) {
        EnumFacing facing = state.getValue(FACING);
        BlockPos support = pos.offset(facing.getOpposite());
        if (!world.isRemote && changedPos.equals(support) && !hasSupport(world, pos, facing)) {
            world.destroyBlock(pos, true);
        }
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        super.breakBlock(world, pos, state);
        if (state.getValue(WATERLOGGED) && world.isAirBlock(pos)) {
            world.setBlockState(pos, Blocks.WATER.getDefaultState(), 3);
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
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos,
            IBlockState state, int fortune) {
        if (cluster) {
            Item shard = CncMaterialContent.item("amethyst_shard");
            if (shard != null) {
                net.minecraft.entity.player.EntityPlayer player = harvesters.get();
                ItemStack tool = player == null ? ItemStack.EMPTY : player.getHeldItemMainhand();
                int count = 2;
                if (!tool.isEmpty() && tool.getItem().getToolClasses(tool).contains("pickaxe")) {
                    count = OreDropLogic.applyOreBonus(4, fortune,
                            world instanceof World ? ((World) world).rand : new Random(0L));
                }
                drops.add(new ItemStack(shard, count));
            }
        }
    }

    @Override
    protected boolean canSilkHarvest() {
        return true;
    }

    @Override
    public EnumPushReaction getMobilityFlag(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, WATERLOGGED, FACING);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        int facingIndex = meta & 7;
        EnumFacing facing = facingIndex < EnumFacing.values().length
                ? EnumFacing.getFront(facingIndex) : EnumFacing.UP;
        return getDefaultState()
                .withProperty(WATERLOGGED, (meta & 8) != 0)
                .withProperty(FACING, facing);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex()
                | (state.getValue(WATERLOGGED) ? 8 : 0);
    }

    @SideOnly(Side.CLIENT)
    public static void registerStateMapper(Block block) {
        ModelLoader.setCustomStateMapper(block,
                new StateMap.Builder().ignore(WATERLOGGED).build());
    }

    @SideOnly(Side.CLIENT)
    public static void registerItemModel(Block block, ModelResourceLocation model) {
        registerStateMapper(block);
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0, model);
    }

    private static boolean hasSupport(World world, BlockPos pos, EnumFacing facing) {
        BlockPos support = pos.offset(facing.getOpposite());
        return world.getBlockState(support).isSideSolid(world, support, facing);
    }
}

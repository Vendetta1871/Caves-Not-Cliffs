package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.content.AzaleaTreeFeature;
import net.celestiald.cavesnotcliffs.content.LushCaveContent;
import net.celestiald.cavesnotcliffs.content.LushCaveMechanics;
import net.celestiald.cavesnotcliffs.content.LushCaveSounds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Runtime implementations for azaleas, their leaves and root blocks. */
public final class LushAzaleaBlocks {
    private LushAzaleaBlocks() {
    }

    public static final class AzaleaBush extends BlockBush implements IGrowable {
        private static final AxisAlignedBB STEM =
                new AxisAlignedBB(6.0D / 16.0D, 0.0D, 6.0D / 16.0D,
                        10.0D / 16.0D, 0.5D, 10.0D / 16.0D);
        private static final AxisAlignedBB CANOPY =
                new AxisAlignedBB(0.0D, 0.5D, 0.0D, 1.0D, 1.0D, 1.0D);

        public AzaleaBush(boolean flowering) {
            super(Material.PLANTS);
            setUnlocalizedName(flowering ? "flowering_azalea" : "azalea");
            setSoundType(flowering ? LushCaveSounds.FLOWERING_AZALEA
                    : LushCaveSounds.AZALEA);
            setHardness(0.0F);
            setCreativeTab(net.minecraft.creativetab.CreativeTabs.DECORATIONS);
        }

        @Override
        protected boolean canSustainBush(IBlockState state) {
            Block block = state.getBlock();
            return block == Blocks.CLAY || block == LushCaveContent.MOSS_BLOCK
                    || block == LushCaveContent.ROOTED_DIRT
                    || state.getMaterial() == Material.GROUND
                    || state.getMaterial() == Material.GRASS;
        }

        @Override
        public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
                AxisAlignedBB entityBox, List<AxisAlignedBB> boxes,
                @Nullable Entity entity, boolean actualState) {
            addCollisionBoxToList(pos, entityBox, boxes, STEM);
            addCollisionBoxToList(pos, entityBox, boxes, CANOPY);
        }

        @Override
        public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world,
                BlockPos pos) {
            return FULL_BLOCK_AABB;
        }

        @Override
        public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world,
                BlockPos pos) {
            return FULL_BLOCK_AABB;
        }

        @Override
        public boolean canGrow(World world, BlockPos pos, IBlockState state,
                boolean isClient) {
            return world.isAirBlock(pos.up());
        }

        @Override
        public boolean canUseBonemeal(World world, Random random, BlockPos pos,
                IBlockState state) {
            return random.nextFloat()
                    < LushCaveMechanics.AZALEA_BONEMEAL_SUCCESS_CHANCE;
        }

        @Override
        public void grow(World world, Random random, BlockPos pos, IBlockState state) {
            AzaleaTreeFeature.grow(world, random, pos,
                    LushCaveContent.ROOTED_DIRT,
                    LushCaveContent.AZALEA_LEAVES,
                    LushCaveContent.FLOWERING_AZALEA_LEAVES,
                    AzaleaLeaves.DISTANCE,
                    AzaleaLeaves.PERSISTENT);
        }

        @SideOnly(Side.CLIENT)
        @Override
        public BlockRenderLayer getBlockLayer() {
            return BlockRenderLayer.CUTOUT;
        }
    }

    public static final class AzaleaLeaves extends Block implements IShearable {
        public static final PropertyInteger DISTANCE = PropertyInteger.create("distance", 1, 7);
        public static final PropertyBool PERSISTENT = PropertyBool.create("persistent");

        private static final float[] SAPLING_CHANCES =
                {0.05F, 0.0625F, 0.083333336F, 0.1F};
        private static final float[] STICK_CHANCES =
                {0.02F, 0.022222223F, 0.025F, 0.033333335F, 0.1F};

        private final boolean flowering;

        public AzaleaLeaves(boolean flowering) {
            super(Material.LEAVES, MapColor.FOLIAGE);
            this.flowering = flowering;
            setUnlocalizedName(flowering ? "flowering_azalea_leaves" : "azalea_leaves");
            setSoundType(LushCaveSounds.AZALEA_LEAVES);
            setHardness(0.2F);
            setLightOpacity(1);
            setTickRandomly(true);
            setCreativeTab(net.minecraft.creativetab.CreativeTabs.DECORATIONS);
            setDefaultState(blockState.getBaseState()
                    .withProperty(DISTANCE, 7)
                    .withProperty(PERSISTENT, false));
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, DISTANCE, PERSISTENT);
        }

        @Override
        public IBlockState getStateFromMeta(int meta) {
            return getDefaultState()
                    .withProperty(DISTANCE, Math.min(7, (meta & 7) + 1))
                    .withProperty(PERSISTENT, (meta & 8) != 0);
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return state.getValue(DISTANCE) - 1
                    | (state.getValue(PERSISTENT) ? 8 : 0);
        }

        @Override
        public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer,
                EnumHand hand) {
            return updateDistance(world, pos, getDefaultState()
                    .withProperty(PERSISTENT, true));
        }

        @Override
        public void neighborChanged(IBlockState state, World world, BlockPos pos,
                Block changed, BlockPos changedPos) {
            IBlockState updated = updateDistance(world, pos, state);
            if (updated != state) {
                world.setBlockState(pos, updated, 3);
            }
        }

        @Override
        public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
            if (!state.getValue(PERSISTENT) && state.getValue(DISTANCE) == 7) {
                dropBlockAsItem(world, pos, state, 0);
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
            }
        }

        private IBlockState updateDistance(IBlockAccess world, BlockPos pos,
                IBlockState state) {
            int distance = 7;
            for (EnumFacing facing : EnumFacing.values()) {
                IBlockState neighbor = world.getBlockState(pos.offset(facing));
                int neighborDistance = isLog(neighbor, world, pos.offset(facing)) ? 0
                        : neighbor.getBlock() instanceof AzaleaLeaves
                        ? neighbor.getValue(DISTANCE) : 7;
                distance = Math.min(distance, neighborDistance + 1);
                if (distance == 1) {
                    break;
                }
            }
            return state.withProperty(DISTANCE, distance);
        }

        private static boolean isLog(IBlockState state, IBlockAccess world, BlockPos pos) {
            return state.getBlock().isWood(world, pos);
        }

        @Override
        public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos,
                IBlockState state, int fortune) {
            int saplingIndex = Math.min(fortune, SAPLING_CHANCES.length - 1);
            if (RANDOM.nextFloat() < SAPLING_CHANCES[saplingIndex]) {
                Block sapling = flowering ? LushCaveContent.FLOWERING_AZALEA
                        : LushCaveContent.AZALEA;
                drops.add(new ItemStack(Item.getItemFromBlock(sapling)));
            }
            int stickIndex = Math.min(fortune, STICK_CHANCES.length - 1);
            if (RANDOM.nextFloat() < STICK_CHANCES[stickIndex]) {
                drops.add(new ItemStack(Items.STICK, 1 + RANDOM.nextInt(2)));
            }
        }

        @Override
        protected boolean canSilkHarvest() {
            return true;
        }

        @Override
        public boolean isShearable(ItemStack item, IBlockAccess world, BlockPos pos) {
            return true;
        }

        @Override
        public List<ItemStack> onSheared(ItemStack item, IBlockAccess world, BlockPos pos,
                int fortune) {
            List<ItemStack> drops = new ArrayList<>();
            drops.add(new ItemStack(this));
            return drops;
        }

        @Override
        public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
            return 60;
        }

        @Override
        public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
            return 30;
        }

        @Override public boolean isOpaqueCube(IBlockState state) { return false; }
        @SideOnly(Side.CLIENT) @Override public BlockRenderLayer getBlockLayer() {
            return BlockRenderLayer.CUTOUT_MIPPED;
        }
    }

    public static final class RootedDirt extends Block implements IGrowable {
        public RootedDirt() {
            super(Material.GROUND, MapColor.DIRT);
            setUnlocalizedName("rooted_dirt");
            setHardness(0.5F);
            setSoundType(LushCaveSounds.ROOTED_DIRT);
            setCreativeTab(net.minecraft.creativetab.CreativeTabs.BUILDING_BLOCKS);
        }

        @Override
        public boolean canGrow(World world, BlockPos pos, IBlockState state,
                boolean isClient) {
            return world.isAirBlock(pos.down());
        }

        @Override
        public boolean canUseBonemeal(World world, Random random, BlockPos pos,
                IBlockState state) {
            return true;
        }

        @Override
        public void grow(World world, Random random, BlockPos pos, IBlockState state) {
            world.setBlockState(pos.down(),
                    LushCaveContent.HANGING_ROOTS.getDefaultState(), 3);
        }
    }

    public static final class HangingRoots extends Block implements IShearable {
        private static final AxisAlignedBB SHAPE =
                new AxisAlignedBB(2.0D / 16.0D, 10.0D / 16.0D, 2.0D / 16.0D,
                        14.0D / 16.0D, 1.0D, 14.0D / 16.0D);
        private final boolean waterlogged;

        public HangingRoots(boolean waterlogged) {
            super(Material.PLANTS, MapColor.DIRT);
            this.waterlogged = waterlogged;
            setUnlocalizedName("hanging_roots");
            setHardness(0.0F);
            setSoundType(LushCaveSounds.HANGING_ROOTS);
            setCreativeTab(waterlogged ? null
                    : net.minecraft.creativetab.CreativeTabs.DECORATIONS);
        }

        @Override
        public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer,
                EnumHand hand) {
            return world.getBlockState(pos).getMaterial() == Material.WATER
                    ? LushCaveContent.HANGING_ROOTS_WATERLOGGED.getDefaultState()
                    : getDefaultState();
        }

        @Override
        public boolean canPlaceBlockAt(World world, BlockPos pos) {
            return world.getBlockState(pos.up()).isSideSolid(world, pos.up(),
                    EnumFacing.DOWN);
        }

        @Override
        public void neighborChanged(IBlockState state, World world, BlockPos pos,
                Block changed, BlockPos changedPos) {
            if (!canPlaceBlockAt(world, pos)) {
                dropBlockAsItem(world, pos, state, 0);
                world.setBlockState(pos, waterlogged ? Blocks.WATER.getDefaultState()
                        : Blocks.AIR.getDefaultState(), 3);
            }
        }

        @Override
        public boolean removedByPlayer(IBlockState state, World world, BlockPos pos,
                EntityPlayer player, boolean willHarvest) {
            onBlockHarvested(world, pos, state, player);
            return world.setBlockState(pos, waterlogged ? Blocks.WATER.getDefaultState()
                    : Blocks.AIR.getDefaultState(), world.isRemote ? 11 : 3);
        }

        @Override
        public Item getItemDropped(IBlockState state, Random random, int fortune) {
            return Items.AIR;
        }

        @Override
        public boolean isShearable(ItemStack item, IBlockAccess world, BlockPos pos) {
            return true;
        }

        @Override
        public List<ItemStack> onSheared(ItemStack item, IBlockAccess world, BlockPos pos,
                int fortune) {
            return java.util.Collections.singletonList(
                    new ItemStack(Item.getItemFromBlock(LushCaveContent.HANGING_ROOTS)));
        }

        @Override
        public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world,
                BlockPos pos) {
            return SHAPE;
        }

        @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState state,
                IBlockAccess world, BlockPos pos) { return NULL_AABB; }
        @Override public boolean isOpaqueCube(IBlockState state) { return false; }
        @Override public boolean isFullCube(IBlockState state) { return false; }
        @Override public EnumOffsetType getOffsetType() { return EnumOffsetType.XZ; }
        @SideOnly(Side.CLIENT) @Override public BlockRenderLayer getBlockLayer() {
            return BlockRenderLayer.CUTOUT;
        }
    }

    public static final class PottedAzalea extends Block {
        private static final AxisAlignedBB SHAPE =
                new AxisAlignedBB(5.0D / 16.0D, 0.0D, 5.0D / 16.0D,
                        11.0D / 16.0D, 6.0D / 16.0D, 11.0D / 16.0D);
        private final boolean flowering;

        public PottedAzalea(boolean flowering) {
            super(Material.CIRCUITS);
            this.flowering = flowering;
            setUnlocalizedName(flowering ? "potted_flowering_azalea_bush"
                    : "potted_azalea_bush");
            setHardness(0.0F);
        }

        public Block content() {
            return flowering ? LushCaveContent.FLOWERING_AZALEA
                    : LushCaveContent.AZALEA;
        }

        @Override
        public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX,
                float hitY, float hitZ) {
            Block heldBlock = Block.getBlockFromItem(player.getHeldItem(hand).getItem());
            if (heldBlock == LushCaveContent.AZALEA
                    || heldBlock == LushCaveContent.FLOWERING_AZALEA) {
                return true;
            }
            if (!world.isRemote) {
                ItemStack content = new ItemStack(Item.getItemFromBlock(content()));
                if (player.getHeldItem(hand).isEmpty()) {
                    player.setHeldItem(hand, content);
                } else if (!player.addItemStackToInventory(content)) {
                    player.dropItem(content, false);
                }
                world.setBlockState(pos, Blocks.FLOWER_POT.getDefaultState(), 3);
            }
            return true;
        }

        @Override
        public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos,
                IBlockState state, int fortune) {
            drops.add(new ItemStack(Items.FLOWER_POT));
            drops.add(new ItemStack(Item.getItemFromBlock(content())));
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
                world.setBlockToAir(pos);
            }
        }

        @Override public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world,
                BlockPos pos) { return SHAPE; }
        @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState state,
                IBlockAccess world, BlockPos pos) { return SHAPE; }
        @Override public boolean isOpaqueCube(IBlockState state) { return false; }
        @Override public boolean isFullCube(IBlockState state) { return false; }
        @SideOnly(Side.CLIENT) @Override public BlockRenderLayer getBlockLayer() {
            return BlockRenderLayer.CUTOUT;
        }
    }
}

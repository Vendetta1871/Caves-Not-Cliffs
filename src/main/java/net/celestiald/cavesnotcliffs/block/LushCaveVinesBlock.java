package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.content.LushCaveMechanics;
import net.celestiald.cavesnotcliffs.content.LushCaveSounds;
import net.celestiald.cavesnotcliffs.item.ItemGlowBerries;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/** Hidden cave-vine state blocks; glow berries are the sole obtainable planting item. */
public final class LushCaveVinesBlock {
    public static final PropertyBool BERRIES = PropertyBool.create("berries");
    public static final PropertyInteger LOCAL_AGE = PropertyInteger.create("age", 0, 7);
    private static final AxisAlignedBB SHAPE =
            new AxisAlignedBB(1.0D / 16.0D, 0.0D, 1.0D / 16.0D,
                    15.0D / 16.0D, 1.0D, 15.0D / 16.0D);

    private LushCaveVinesBlock() {
    }

    public static final class Head extends Block implements IGrowable {
        private final int baseAge;
        private final int maxLocalAge;

        public Head(int baseAge, int maxLocalAge) {
            super(Material.VINE);
            this.baseAge = baseAge;
            this.maxLocalAge = maxLocalAge;
            setUnlocalizedName("cave_vines");
            setSoundType(LushCaveSounds.CAVE_VINES);
            setHardness(0.0F);
            setTickRandomly(true);
            setLightOpacity(0);
            setDefaultState(blockState.getBaseState()
                    .withProperty(LOCAL_AGE, 0)
                    .withProperty(BERRIES, false));
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, LOCAL_AGE, BERRIES);
        }

        @Override
        public IBlockState getStateFromMeta(int meta) {
            return getDefaultState()
                    .withProperty(LOCAL_AGE, Math.min(meta & 7, maxLocalAge))
                    .withProperty(BERRIES, (meta & 8) != 0);
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return state.getValue(LOCAL_AGE) | (state.getValue(BERRIES) ? 8 : 0);
        }

        public int age(IBlockState state) {
            return baseAge + state.getValue(LOCAL_AGE);
        }

        @Override
        public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
            int age = age(state);
            if (age < LushCaveMechanics.CAVE_VINE_MAX_AGE
                    && random.nextDouble() < LushCaveMechanics.CAVE_VINE_GROWTH_CHANCE
                    && world.isAirBlock(pos.down())) {
                IBlockState next = headState(age + 1,
                        random.nextFloat()
                                < LushCaveMechanics.CAVE_VINE_BERRY_GROWTH_CHANCE);
                world.setBlockState(pos.down(), next, 3);
                world.setBlockState(pos, bodyState(state.getValue(BERRIES)), 3);
            }
        }

        @Override
        public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX,
                float hitY, float hitZ) {
            return pickBerries(world, pos, state, state.getValue(BERRIES));
        }

        @Override
        public boolean canGrow(World world, BlockPos pos, IBlockState state,
                boolean isClient) {
            return !state.getValue(BERRIES);
        }

        @Override
        public boolean canUseBonemeal(World world, Random random, BlockPos pos,
                IBlockState state) {
            return true;
        }

        @Override
        public void grow(World world, Random random, BlockPos pos, IBlockState state) {
            world.setBlockState(pos, state.withProperty(BERRIES, true), 2);
        }

        @Override
        public void neighborChanged(IBlockState state, World world, BlockPos pos,
                Block changed, BlockPos changedPos) {
            if (!canHangFrom(world, pos.up())) {
                breakUnsupported(world, pos, state);
            } else if (isVine(world.getBlockState(pos.down()).getBlock())) {
                world.setBlockState(pos, bodyState(state.getValue(BERRIES)), 2);
            }
        }

        @Override
        public boolean canPlaceBlockAt(World world, BlockPos pos) {
            return canHangFrom(world, pos.up());
        }

        @Override
        public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
            return state.getValue(BERRIES) ? LushCaveMechanics.CAVE_VINE_LIGHT : 0;
        }

        @Override
        public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos,
                IBlockState state, int fortune) {
            addBerryDrop(drops, state.getValue(BERRIES));
        }

        @Override public boolean isOpaqueCube(IBlockState state) { return false; }
        @Override public boolean isFullCube(IBlockState state) { return false; }
        @Override public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world,
                BlockPos pos) { return SHAPE; }
        @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState state,
                IBlockAccess world, BlockPos pos) { return NULL_AABB; }
        @SideOnly(Side.CLIENT) @Override public BlockRenderLayer getBlockLayer() {
            return BlockRenderLayer.CUTOUT;
        }
    }

    public static final class Body extends Block implements IGrowable {
        public Body() {
            super(Material.VINE);
            setUnlocalizedName("cave_vines_plant");
            setSoundType(LushCaveSounds.CAVE_VINES);
            setHardness(0.0F);
            setLightOpacity(0);
            setDefaultState(blockState.getBaseState().withProperty(BERRIES, false));
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, BERRIES);
        }

        @Override
        public IBlockState getStateFromMeta(int meta) {
            return getDefaultState().withProperty(BERRIES, (meta & 1) != 0);
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return state.getValue(BERRIES) ? 1 : 0;
        }

        @Override
        public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX,
                float hitY, float hitZ) {
            return pickBerries(world, pos, state, state.getValue(BERRIES));
        }

        @Override
        public boolean canGrow(World world, BlockPos pos, IBlockState state,
                boolean isClient) {
            return !state.getValue(BERRIES);
        }

        @Override
        public boolean canUseBonemeal(World world, Random random, BlockPos pos,
                IBlockState state) {
            return true;
        }

        @Override
        public void grow(World world, Random random, BlockPos pos, IBlockState state) {
            world.setBlockState(pos, state.withProperty(BERRIES, true), 2);
        }

        @Override
        public void neighborChanged(IBlockState state, World world, BlockPos pos,
                Block changed, BlockPos changedPos) {
            if (!canHangFrom(world, pos.up())) {
                breakUnsupported(world, pos, state);
                return;
            }
            if (!isVine(world.getBlockState(pos.down()).getBlock())) {
                world.setBlockState(pos,
                        headState(world.rand.nextInt(25), state.getValue(BERRIES)), 2);
            }
        }

        @Override
        public boolean canPlaceBlockAt(World world, BlockPos pos) {
            return canHangFrom(world, pos.up());
        }

        @Override
        public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
            return state.getValue(BERRIES) ? LushCaveMechanics.CAVE_VINE_LIGHT : 0;
        }

        @Override
        public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos,
                IBlockState state, int fortune) {
            addBerryDrop(drops, state.getValue(BERRIES));
        }

        @Override public boolean isOpaqueCube(IBlockState state) { return false; }
        @Override public boolean isFullCube(IBlockState state) { return false; }
        @Override public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world,
                BlockPos pos) { return SHAPE; }
        @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState state,
                IBlockAccess world, BlockPos pos) { return NULL_AABB; }
        @SideOnly(Side.CLIENT) @Override public BlockRenderLayer getBlockLayer() {
            return BlockRenderLayer.CUTOUT;
        }
    }

    public static IBlockState headState(int age, boolean berries) {
        String path = LushCaveMechanics.caveVineHeadPath(age);
        Block block = ForgeRegistries.BLOCKS.getValue(CncRegistryIds.id(path));
        if (!(block instanceof Head)) {
            throw new IllegalStateException("Missing cave-vine age band " + path);
        }
        Head head = (Head) block;
        return head.getStateFromMeta(LushCaveMechanics.caveVineHeadMeta(age, berries));
    }

    public static IBlockState bodyState(boolean berries) {
        Block block = ForgeRegistries.BLOCKS.getValue(CncRegistryIds.CAVE_VINES_PLANT);
        if (!(block instanceof Body)) {
            throw new IllegalStateException("Missing cave-vine body block");
        }
        return block.getDefaultState().withProperty(BERRIES, berries);
    }

    public static boolean isVine(Block block) {
        if (block == null || block.getRegistryName() == null) {
            return false;
        }
        return LushCaveMechanics.isCaveVine(block.getRegistryName().getResourcePath());
    }

    private static boolean canHangFrom(World world, BlockPos support) {
        IBlockState state = world.getBlockState(support);
        return isVine(state.getBlock())
                || state.isSideSolid(world, support, EnumFacing.DOWN);
    }

    private static boolean pickBerries(World world, BlockPos pos, IBlockState state,
            boolean berries) {
        if (!berries) {
            return false;
        }
        if (!world.isRemote) {
            ItemStack berryStack = new ItemStack(ItemGlowBerries.item);
            EntityItem dropped = new EntityItem(world,
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    berryStack);
            dropped.setDefaultPickupDelay();
            world.spawnEntity(dropped);
            world.setBlockState(pos, state.withProperty(BERRIES, false), 2);
        }
        world.playSound(null, pos, LushCaveSounds.CAVE_VINES_PICK_BERRIES,
                SoundCategory.BLOCKS, 1.0F, 0.8F + world.rand.nextFloat() * 0.4F);
        return true;
    }

    private static void addBerryDrop(NonNullList<ItemStack> drops, boolean berries) {
        if (berries && ItemGlowBerries.item != null) {
            drops.add(new ItemStack(ItemGlowBerries.item));
        }
    }

    private static void breakUnsupported(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            state.getBlock().dropBlockAsItem(world, pos, state, 0);
        }
        world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
    }
}

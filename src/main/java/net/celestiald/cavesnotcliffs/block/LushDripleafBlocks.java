package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.content.LushCaveContent;
import net.celestiald.cavesnotcliffs.content.LushCaveMechanics;
import net.celestiald.cavesnotcliffs.content.LushCaveSounds;
import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Small/big dripleaf placement, growth, support, water storage and tilt state machines. */
public final class LushDripleafBlocks {
    public static final PropertyDirection FACING =
            PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyBool WATERLOGGED = PropertyBool.create("waterlogged");
    public static final PropertyEnum<Half> HALF = PropertyEnum.create("half", Half.class);
    public static final PropertyEnum<Tilt> TILT = PropertyEnum.create("tilt", Tilt.class);

    private LushDripleafBlocks() {
    }

    public enum Half implements IStringSerializable {
        LOWER("lower"), UPPER("upper");
        private final String name;
        Half(String name) { this.name = name; }
        @Override public String getName() { return name; }
    }

    public enum Tilt implements IStringSerializable {
        NONE(LushCaveMechanics.Tilt.NONE),
        UNSTABLE(LushCaveMechanics.Tilt.UNSTABLE),
        PARTIAL(LushCaveMechanics.Tilt.PARTIAL),
        FULL(LushCaveMechanics.Tilt.FULL);

        private final LushCaveMechanics.Tilt mechanics;
        Tilt(LushCaveMechanics.Tilt mechanics) { this.mechanics = mechanics; }
        @Override public String getName() { return mechanics.getName(); }
        public int delay() { return mechanics.getDelay(); }
        public Tilt next() { return values()[mechanics.next().ordinal()]; }
    }

    public static final class Small extends LushWaterloggedBlock
            implements IGrowable, IShearable {
        private static final AxisAlignedBB SHAPE =
                new AxisAlignedBB(2.0D / 16.0D, 0.0D, 2.0D / 16.0D,
                        14.0D / 16.0D, 13.0D / 16.0D, 14.0D / 16.0D);

        public Small() {
            super(Material.PLANTS);
            setUnlocalizedName("small_dripleaf");
            setHardness(0.0F);
            setSoundType(LushCaveSounds.SMALL_DRIPLEAF);
            setCreativeTab(net.minecraft.creativetab.CreativeTabs.DECORATIONS);
            setDefaultState(blockState.getBaseState()
                    .withProperty(FACING, EnumFacing.NORTH)
                    .withProperty(HALF, Half.LOWER)
                    .withProperty(WATERLOGGED, false));
        }

        @Override protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, FACING, HALF, WATERLOGGED);
        }

        @Override public IBlockState getStateFromMeta(int meta) {
            return getDefaultState()
                    .withProperty(FACING, EnumFacing.getHorizontal(meta & 3))
                    .withProperty(HALF, (meta & 4) != 0 ? Half.UPPER : Half.LOWER)
                    .withProperty(WATERLOGGED, (meta & 8) != 0);
        }

        @Override public int getMetaFromState(IBlockState state) {
            return LushCaveMechanics.smallDripleafMeta(
                    state.getValue(FACING).getHorizontalIndex(),
                    state.getValue(HALF) == Half.UPPER,
                    state.getValue(WATERLOGGED));
        }

        @Override
        public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side,
                float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer,
                EnumHand hand) {
            return getDefaultState()
                    .withProperty(FACING, placer.getHorizontalFacing().getOpposite())
                    .withProperty(HALF, Half.LOWER)
                    .withProperty(WATERLOGGED, isWater(world.getBlockState(pos)));
        }

        @Override
        public boolean canPlaceBlockAt(World world, BlockPos pos) {
            boolean water = isWater(world.getBlockState(pos));
            return canReplace(world, pos) && canReplace(world, pos.up())
                    && canSmallSurviveOn(world.getBlockState(pos.down()), water);
        }

        @Override
        public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
                EntityLivingBase placer, ItemStack stack) {
            IBlockState upperOriginal = world.getBlockState(pos.up());
            world.setBlockState(pos.up(), getDefaultState()
                    .withProperty(FACING, state.getValue(FACING))
                    .withProperty(HALF, Half.UPPER)
                    .withProperty(WATERLOGGED, isWater(upperOriginal)), 3);
        }

        @Override
        public void neighborChanged(IBlockState state, World world, BlockPos pos,
                Block changed, BlockPos changedPos) {
            scheduleRetainedWater(world, pos, state);
            if (!canSurvive(world, pos, state)) {
                breakHalf(world, pos, state, false);
            }
        }

        @Override
        protected boolean hasRetainedWater(IBlockState state) {
            return state.getValue(WATERLOGGED);
        }

        private boolean canSurvive(World world, BlockPos pos, IBlockState state) {
            if (state.getValue(HALF) == Half.UPPER) {
                IBlockState lower = world.getBlockState(pos.down());
                return lower.getBlock() == this && lower.getValue(HALF) == Half.LOWER;
            }
            IBlockState upper = world.getBlockState(pos.up());
            return upper.getBlock() == this && upper.getValue(HALF) == Half.UPPER
                    && canSmallSurviveOn(world.getBlockState(pos.down()),
                    state.getValue(WATERLOGGED));
        }

        @Override
        public void onBlockHarvested(World world, BlockPos pos, IBlockState state,
                EntityPlayer player) {
            BlockPos otherPos = state.getValue(HALF) == Half.LOWER ? pos.up() : pos.down();
            IBlockState other = world.getBlockState(otherPos);
            if (other.getBlock() == this) {
                world.setBlockState(otherPos, other.getValue(WATERLOGGED)
                        ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState(), 3);
            }
        }

        @Override
        public boolean removedByPlayer(IBlockState state, World world, BlockPos pos,
                EntityPlayer player, boolean willHarvest) {
            onBlockHarvested(world, pos, state, player);
            return world.setBlockState(pos, state.getValue(WATERLOGGED)
                    ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState(),
                    world.isRemote ? 11 : 3);
        }

        private void breakHalf(World world, BlockPos pos, IBlockState state,
                boolean drop) {
            if (drop) {
                dropBlockAsItem(world, pos, state, 0);
            }
            world.setBlockState(pos, state.getValue(WATERLOGGED)
                    ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState(), 3);
        }

        @Override public boolean canGrow(World world, BlockPos pos, IBlockState state,
                boolean isClient) { return true; }
        @Override public boolean canUseBonemeal(World world, Random random, BlockPos pos,
                IBlockState state) { return true; }

        @Override
        public void grow(World world, Random random, BlockPos pos, IBlockState state) {
            BlockPos lower = state.getValue(HALF) == Half.LOWER ? pos : pos.down();
            placeWithRandomHeight(world, random, lower, state.getValue(FACING));
        }

        @Override public Item getItemDropped(IBlockState state, Random random, int fortune) {
            return Items.AIR;
        }
        @Override public boolean isShearable(ItemStack item, IBlockAccess world, BlockPos pos) {
            return true;
        }
        @Override public List<ItemStack> onSheared(ItemStack item, IBlockAccess world,
                BlockPos pos, int fortune) {
            return Collections.singletonList(
                    new ItemStack(Item.getItemFromBlock(LushCaveContent.SMALL_DRIPLEAF)));
        }

        @Override
        public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world,
                BlockPos pos, EntityPlayer player) {
            return canonicalSmallDripleaf();
        }

        @Override public int getFlammability(IBlockAccess world, BlockPos pos,
                EnumFacing face) { return 100; }
        @Override public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos,
                EnumFacing face) { return 60; }

        @Override public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world,
                BlockPos pos) { return SHAPE; }
        @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState state,
                IBlockAccess world, BlockPos pos) { return NULL_AABB; }
        @Override public boolean isOpaqueCube(IBlockState state) { return false; }
        @Override public boolean isFullCube(IBlockState state) { return false; }
        @Override public EnumOffsetType getOffsetType() { return EnumOffsetType.XYZ; }
        @SideOnly(Side.CLIENT) @Override public BlockRenderLayer getBlockLayer() {
            return BlockRenderLayer.CUTOUT;
        }

        @Override
        public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
            return layer == (state.getValue(WATERLOGGED)
                    ? BlockRenderLayer.TRANSLUCENT : BlockRenderLayer.CUTOUT);
        }
    }

    public static final class Head extends LushWaterloggedBlock implements IGrowable {
        private static final AxisAlignedBB STABLE =
                new AxisAlignedBB(0.0D, 11.0D / 16.0D, 0.0D,
                        1.0D, 15.0D / 16.0D, 1.0D);
        private static final AxisAlignedBB PARTIAL =
                new AxisAlignedBB(0.0D, 11.0D / 16.0D, 0.0D,
                        1.0D, 13.0D / 16.0D, 1.0D);
        private static final AxisAlignedBB NORTH_STEM = new AxisAlignedBB(
                5.0D / 16.0D, 0.0D, 9.0D / 16.0D,
                11.0D / 16.0D, 13.0D / 16.0D, 15.0D / 16.0D);
        private static final AxisAlignedBB SOUTH_STEM = new AxisAlignedBB(
                5.0D / 16.0D, 0.0D, 1.0D / 16.0D,
                11.0D / 16.0D, 13.0D / 16.0D, 7.0D / 16.0D);
        private static final AxisAlignedBB EAST_STEM = new AxisAlignedBB(
                1.0D / 16.0D, 0.0D, 5.0D / 16.0D,
                7.0D / 16.0D, 13.0D / 16.0D, 11.0D / 16.0D);
        private static final AxisAlignedBB WEST_STEM = new AxisAlignedBB(
                9.0D / 16.0D, 0.0D, 5.0D / 16.0D,
                15.0D / 16.0D, 13.0D / 16.0D, 11.0D / 16.0D);
        private final boolean waterlogged;

        public Head(boolean waterlogged) {
            super(Material.PLANTS);
            this.waterlogged = waterlogged;
            setUnlocalizedName("big_dripleaf");
            setHardness(0.1F);
            setSoundType(LushCaveSounds.BIG_DRIPLEAF);
            if (!waterlogged) {
                setCreativeTab(net.minecraft.creativetab.CreativeTabs.DECORATIONS);
            }
            setDefaultState(blockState.getBaseState()
                    .withProperty(FACING, EnumFacing.NORTH)
                    .withProperty(TILT, Tilt.NONE));
        }

        @Override protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, FACING, TILT);
        }
        @Override public IBlockState getStateFromMeta(int meta) {
            return getDefaultState()
                    .withProperty(FACING, EnumFacing.getHorizontal(meta & 3))
                    .withProperty(TILT, Tilt.values()[meta >> 2 & 3]);
        }
        @Override public int getMetaFromState(IBlockState state) {
            return LushCaveMechanics.bigDripleafMeta(
                    state.getValue(FACING).getHorizontalIndex(),
                    state.getValue(TILT).mechanics);
        }

        @Override
        public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side,
                float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer,
                EnumHand hand) {
            IBlockState below = world.getBlockState(pos.down());
            EnumFacing facing = isHead(below.getBlock()) || below.getBlock() == LushCaveContent.BIG_DRIPLEAF_STEM
                    ? below.getValue(FACING) : placer.getHorizontalFacing().getOpposite();
            Block target = isWater(world.getBlockState(pos))
                    ? LushCaveContent.BIG_DRIPLEAF_WATERLOGGED : LushCaveContent.BIG_DRIPLEAF;
            return target.getDefaultState().withProperty(FACING, facing)
                    .withProperty(TILT, Tilt.NONE);
        }

        @Override public boolean canPlaceBlockAt(World world, BlockPos pos) {
            return canReplace(world, pos) && canBigSurviveOn(world.getBlockState(pos.down()));
        }

        @Override
        public void neighborChanged(IBlockState state, World world, BlockPos pos,
                Block changed, BlockPos changedPos) {
            scheduleRetainedWater(world, pos, state);
            if (world.isBlockPowered(pos)) {
                resetTilt(world, pos, state);
            }
            if (!canBigSurviveOn(world.getBlockState(pos.down()))) {
                breakHead(world, pos, state);
                return;
            }
            if (isHead(world.getBlockState(pos.up()).getBlock())) {
                world.setBlockState(pos, stemState(state.getValue(FACING), waterlogged), 3);
            }
        }

        @Override
        protected boolean hasRetainedWater(IBlockState state) {
            return waterlogged;
        }

        public boolean isWaterloggedStorage() {
            return waterlogged;
        }

        @Override
        public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state,
                Entity entity) {
            if (world.isRemote) {
                return;
            }
            attemptEntityTilt(world, pos, state, entity);
        }

        public void projectileHit(World world, BlockPos pos, IBlockState state) {
            if (!world.isRemote && state.getBlock() == this) {
                setTiltAndSchedule(world, pos, state, Tilt.FULL, true);
            }
        }

        @Override
        public void onEntityWalk(World world, BlockPos pos, Entity entity) {
            if (!world.isRemote) {
                attemptEntityTilt(world, pos, world.getBlockState(pos), entity);
            }
        }

        private void attemptEntityTilt(World world, BlockPos pos, IBlockState state,
                Entity entity) {
            if (state.getBlock() == this && state.getValue(TILT) == Tilt.NONE
                    && entity.onGround && entity.posY > pos.getY() + 0.6875D
                    && !world.isBlockPowered(pos)) {
                setTiltAndSchedule(world, pos, state, Tilt.UNSTABLE, false);
            }
        }

        @Override
        public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
            if (world.isBlockPowered(pos)) {
                resetTilt(world, pos, state);
                return;
            }
            Tilt tilt = state.getValue(TILT);
            if (tilt == Tilt.FULL) {
                resetTilt(world, pos, state);
            } else if (tilt == Tilt.UNSTABLE || tilt == Tilt.PARTIAL) {
                setTiltAndSchedule(world, pos, state, tilt.next(), true);
            }
        }

        private void setTiltAndSchedule(World world, BlockPos pos, IBlockState state,
                Tilt tilt, boolean sound) {
            world.setBlockState(pos, state.withProperty(TILT, tilt), 2);
            if (sound) {
                world.playSound(null, pos, LushCaveSounds.BIG_DRIPLEAF_TILT_DOWN,
                        SoundCategory.BLOCKS, 1.0F, 0.8F + world.rand.nextFloat() * 0.4F);
            }
            if (tilt.delay() >= 0) {
                world.scheduleUpdate(pos, this, tilt.delay());
            }
        }

        private void resetTilt(World world, BlockPos pos, IBlockState state) {
            if (state.getValue(TILT) != Tilt.NONE) {
                world.setBlockState(pos, state.withProperty(TILT, Tilt.NONE), 2);
                world.playSound(null, pos, LushCaveSounds.BIG_DRIPLEAF_TILT_UP,
                        SoundCategory.BLOCKS, 1.0F, 0.8F + world.rand.nextFloat() * 0.4F);
            }
        }

        @Override public boolean canGrow(World world, BlockPos pos, IBlockState state,
                boolean isClient) { return canReplace(world, pos.up()); }
        @Override public boolean canUseBonemeal(World world, Random random, BlockPos pos,
                IBlockState state) { return true; }

        @Override
        public void grow(World world, Random random, BlockPos pos, IBlockState state) {
            BlockPos above = pos.up();
            if (!canReplace(world, above)) {
                return;
            }
            EnumFacing facing = state.getValue(FACING);
            world.setBlockState(pos, stemState(facing, waterlogged), 3);
            world.setBlockState(above, headState(facing,
                    isWater(world.getBlockState(above)), Tilt.NONE), 3);
        }

        @Override public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world,
                BlockPos pos, IBlockState state, int fortune) {
            drops.add(new ItemStack(Item.getItemFromBlock(LushCaveContent.BIG_DRIPLEAF)));
        }

        @Override
        public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world,
                BlockPos pos, EntityPlayer player) {
            return canonicalBigDripleaf();
        }

        @Override public int getFlammability(IBlockAccess world, BlockPos pos,
                EnumFacing face) { return 100; }
        @Override public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos,
                EnumFacing face) { return 60; }

        @Override
        public boolean removedByPlayer(IBlockState state, World world, BlockPos pos,
                EntityPlayer player, boolean willHarvest) {
            onBlockHarvested(world, pos, state, player);
            return world.setBlockState(pos, waterlogged ? Blocks.WATER.getDefaultState()
                    : Blocks.AIR.getDefaultState(), world.isRemote ? 11 : 3);
        }

        private void breakHead(World world, BlockPos pos, IBlockState state) {
            dropBlockAsItem(world, pos, state, 0);
            world.setBlockState(pos, waterlogged ? Blocks.WATER.getDefaultState()
                    : Blocks.AIR.getDefaultState(), 3);
        }

        @Override public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world,
                BlockPos pos) { return selectionShape(state); }
        @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState state,
                IBlockAccess world, BlockPos pos) {
            return collisionShape(state);
        }
        @Override public boolean isOpaqueCube(IBlockState state) { return false; }
        @Override public boolean isFullCube(IBlockState state) { return false; }
        @SideOnly(Side.CLIENT) @Override public BlockRenderLayer getBlockLayer() {
            return waterlogged ? BlockRenderLayer.TRANSLUCENT : BlockRenderLayer.CUTOUT;
        }

        @Override
        public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
            return layer == (waterlogged
                    ? BlockRenderLayer.TRANSLUCENT : BlockRenderLayer.CUTOUT);
        }

        /** Envelope of Java 1.18's directional stem plus its tilt-dependent leaf. */
        public static AxisAlignedBB selectionShape(IBlockState state) {
            AxisAlignedBB stem = stemShape(state.getValue(FACING));
            AxisAlignedBB leaf = leafShape(state.getValue(TILT));
            return leaf == null ? stem : stem.union(leaf);
        }

        /** Java 1.18 collides with the leaf only; its stem is selection geometry. */
        public static AxisAlignedBB collisionShape(IBlockState state) {
            return leafShape(state.getValue(TILT));
        }

        private static AxisAlignedBB leafShape(Tilt tilt) {
            switch (tilt) {
                case FULL: return null;
                case PARTIAL: return PARTIAL;
                default: return STABLE;
            }
        }

        private static AxisAlignedBB stemShape(EnumFacing facing) {
            switch (facing) {
                case SOUTH: return SOUTH_STEM;
                case EAST: return EAST_STEM;
                case WEST: return WEST_STEM;
                default: return NORTH_STEM;
            }
        }
    }

    public static final class Stem extends LushWaterloggedBlock implements IGrowable {
        private static final AxisAlignedBB NORTH = new AxisAlignedBB(
                5.0D / 16.0D, 0.0D, 9.0D / 16.0D,
                11.0D / 16.0D, 1.0D, 15.0D / 16.0D);
        private static final AxisAlignedBB SOUTH = new AxisAlignedBB(
                5.0D / 16.0D, 0.0D, 1.0D / 16.0D,
                11.0D / 16.0D, 1.0D, 7.0D / 16.0D);
        private static final AxisAlignedBB EAST = new AxisAlignedBB(
                1.0D / 16.0D, 0.0D, 5.0D / 16.0D,
                7.0D / 16.0D, 1.0D, 11.0D / 16.0D);
        private static final AxisAlignedBB WEST = new AxisAlignedBB(
                9.0D / 16.0D, 0.0D, 5.0D / 16.0D,
                15.0D / 16.0D, 1.0D, 11.0D / 16.0D);

        public Stem() {
            super(Material.PLANTS);
            setUnlocalizedName("big_dripleaf_stem");
            setHardness(0.1F);
            setSoundType(LushCaveSounds.BIG_DRIPLEAF);
            setDefaultState(blockState.getBaseState()
                    .withProperty(FACING, EnumFacing.NORTH)
                    .withProperty(WATERLOGGED, false));
        }

        @Override protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, FACING, WATERLOGGED);
        }
        @Override public IBlockState getStateFromMeta(int meta) {
            return getDefaultState()
                    .withProperty(FACING, EnumFacing.getHorizontal(meta & 3))
                    .withProperty(WATERLOGGED, (meta & 4) != 0);
        }
        @Override public int getMetaFromState(IBlockState state) {
            return LushCaveMechanics.bigDripleafStemMeta(
                    state.getValue(FACING).getHorizontalIndex(),
                    state.getValue(WATERLOGGED));
        }

        @Override
        public void neighborChanged(IBlockState state, World world, BlockPos pos,
                Block changed, BlockPos changedPos) {
            scheduleRetainedWater(world, pos, state);
            if (!canSurvive(world, pos)) {
                world.scheduleUpdate(pos, this, 1);
            }
        }

        @Override
        protected boolean hasRetainedWater(IBlockState state) {
            return state.getValue(WATERLOGGED);
        }

        @Override
        public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
            if (!canSurvive(world, pos)) {
                dropBlockAsItem(world, pos, state, 0);
                world.setBlockState(pos, state.getValue(WATERLOGGED)
                        ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState(), 3);
            }
        }

        private boolean canSurvive(World world, BlockPos pos) {
            IBlockState below = world.getBlockState(pos.down());
            IBlockState above = world.getBlockState(pos.up());
            return (below.getBlock() == this || canBigSurviveOn(below))
                    && (above.getBlock() == this || isHead(above.getBlock()));
        }

        @Override public boolean canGrow(World world, BlockPos pos, IBlockState state,
                boolean isClient) { return findHead(world, pos) != null; }
        @Override public boolean canUseBonemeal(World world, Random random, BlockPos pos,
                IBlockState state) { return true; }

        @Override
        public void grow(World world, Random random, BlockPos pos, IBlockState state) {
            BlockPos headPos = findHead(world, pos);
            if (headPos == null || !canReplace(world, headPos.up())) {
                return;
            }
            IBlockState head = world.getBlockState(headPos);
            Head headBlock = (Head) head.getBlock();
            EnumFacing facing = state.getValue(FACING);
            world.setBlockState(headPos, stemState(facing, headBlock.waterlogged), 3);
            BlockPos above = headPos.up();
            world.setBlockState(above,
                    headState(facing, isWater(world.getBlockState(above)), Tilt.NONE), 3);
        }

        private BlockPos findHead(World world, BlockPos start) {
            BlockPos cursor = start;
            while (cursor.getY() < net.celestiald.cavesnotcliffs.world.CavesNotCliffsWorldType.MAX_HEIGHT) {
                IBlockState state = world.getBlockState(cursor);
                if (isHead(state.getBlock())) {
                    return canReplace(world, cursor.up()) ? cursor : null;
                }
                if (state.getBlock() != this) {
                    return null;
                }
                cursor = cursor.up();
            }
            return null;
        }

        @Override public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world,
                BlockPos pos, IBlockState state, int fortune) {
            drops.add(new ItemStack(Item.getItemFromBlock(LushCaveContent.BIG_DRIPLEAF)));
        }

        @Override
        public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world,
                BlockPos pos, EntityPlayer player) {
            return canonicalBigDripleaf();
        }

        @Override public int getFlammability(IBlockAccess world, BlockPos pos,
                EnumFacing face) { return 100; }
        @Override public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos,
                EnumFacing face) { return 60; }

        @Override
        public boolean removedByPlayer(IBlockState state, World world, BlockPos pos,
                EntityPlayer player, boolean willHarvest) {
            onBlockHarvested(world, pos, state, player);
            return world.setBlockState(pos, state.getValue(WATERLOGGED)
                    ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState(),
                    world.isRemote ? 11 : 3);
        }

        @Override public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world,
                BlockPos pos) {
            switch (state.getValue(FACING)) {
                case SOUTH: return SOUTH;
                case EAST: return EAST;
                case WEST: return WEST;
                default: return NORTH;
            }
        }
        @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState state,
                IBlockAccess world, BlockPos pos) { return NULL_AABB; }
        @Override public boolean isOpaqueCube(IBlockState state) { return false; }
        @Override public boolean isFullCube(IBlockState state) { return false; }
        @SideOnly(Side.CLIENT) @Override public BlockRenderLayer getBlockLayer() {
            return BlockRenderLayer.CUTOUT;
        }

        @Override
        public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
            return layer == (state.getValue(WATERLOGGED)
                    ? BlockRenderLayer.TRANSLUCENT : BlockRenderLayer.CUTOUT);
        }
    }

    public static void placeWithRandomHeight(World world, Random random, BlockPos origin,
            EnumFacing facing) {
        int requested = LushCaveMechanics.generatedBigDripleafHeight(random);
        int available = 0;
        BlockPos cursor = origin;
        while (available < requested && canReplace(world, cursor)) {
            available++;
            cursor = cursor.up();
        }
        if (available <= 0) {
            return;
        }
        for (int offset = 0; offset < available - 1; offset++) {
            BlockPos stemPos = origin.up(offset);
            boolean water = isWater(world.getBlockState(stemPos))
                    || isDripleafWaterlogged(world.getBlockState(stemPos));
            world.setBlockState(stemPos, stemState(facing, water), 3);
        }
        BlockPos headPos = origin.up(available - 1);
        boolean water = isWater(world.getBlockState(headPos))
                || isDripleafWaterlogged(world.getBlockState(headPos));
        world.setBlockState(headPos, headState(facing, water, Tilt.NONE), 3);
    }

    public static IBlockState headState(EnumFacing facing, boolean waterlogged, Tilt tilt) {
        Block block = waterlogged ? LushCaveContent.BIG_DRIPLEAF_WATERLOGGED
                : LushCaveContent.BIG_DRIPLEAF;
        return block.getDefaultState().withProperty(FACING, facing).withProperty(TILT, tilt);
    }

    public static IBlockState stemState(EnumFacing facing, boolean waterlogged) {
        return LushCaveContent.BIG_DRIPLEAF_STEM.getDefaultState()
                .withProperty(FACING, facing).withProperty(WATERLOGGED, waterlogged);
    }

    public static boolean isHead(Block block) {
        return block == LushCaveContent.BIG_DRIPLEAF
                || block == LushCaveContent.BIG_DRIPLEAF_WATERLOGGED;
    }

    private static boolean canReplace(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return world.isAirBlock(pos) || isWater(state)
                || state.getBlock() == LushCaveContent.SMALL_DRIPLEAF;
    }

    private static boolean canSmallSurviveOn(IBlockState support, boolean waterlogged) {
        return support.getBlock() == Blocks.CLAY
                || support.getBlock() == LushCaveContent.MOSS_BLOCK
                || waterlogged && isDirtLike(support);
    }

    private static boolean canBigSurviveOn(IBlockState support) {
        return support.getBlock() == Blocks.CLAY
                || support.getBlock() == LushCaveContent.MOSS_BLOCK
                || support.getBlock() == Blocks.FARMLAND
                || isDirtLike(support)
                || support.getBlock() == LushCaveContent.BIG_DRIPLEAF_STEM
                || isHead(support.getBlock());
    }

    private static boolean isDirtLike(IBlockState state) {
        return state.getMaterial() == Material.GROUND
                || state.getMaterial() == Material.GRASS
                || state.getBlock() == LushCaveContent.ROOTED_DIRT;
    }

    private static boolean isWater(IBlockState state) {
        return state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.FLOWING_WATER;
    }

    private static boolean isDripleafWaterlogged(IBlockState state) {
        if (state.getBlock() instanceof Small) {
            return state.getValue(WATERLOGGED);
        }
        if (state.getBlock() instanceof Stem) {
            return state.getValue(WATERLOGGED);
        }
        return state.getBlock() == LushCaveContent.BIG_DRIPLEAF_WATERLOGGED;
    }

    private static ItemStack canonicalSmallDripleaf() {
        Item item = Item.getItemFromBlock(LushCaveContent.SMALL_DRIPLEAF);
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static ItemStack canonicalBigDripleaf() {
        Item item = Item.getItemFromBlock(LushCaveContent.BIG_DRIPLEAF);
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
}

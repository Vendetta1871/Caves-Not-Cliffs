package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.content.CncBlockProperties;
import net.celestiald.cavesnotcliffs.content.DripstoneSoundEvents;
import net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics;
import net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics.Neighbor;
import net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics.Thickness;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
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
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * One canonical implementation for every Java 1.18.2 pointed-dripstone direction and thickness.
 * A second block-only identity stores the same ten states when source-waterlogged.
 */
public final class BlockPointedDripstone extends Block {
    public static final PropertyDirection TIP_DIRECTION = PropertyDirection.create(
            "vertical_direction", EnumFacing.Plane.VERTICAL);
    public static final PropertyEnum<Thickness> THICKNESS =
            PropertyEnum.create("thickness", Thickness.class);

    private static final AxisAlignedBB TIP_MERGE_SHAPE =
            new AxisAlignedBB(5.0D / 16.0D, 0.0D, 5.0D / 16.0D,
                    11.0D / 16.0D, 1.0D, 11.0D / 16.0D);
    private static final AxisAlignedBB TIP_UP_SHAPE =
            new AxisAlignedBB(5.0D / 16.0D, 0.0D, 5.0D / 16.0D,
                    11.0D / 16.0D, 11.0D / 16.0D, 11.0D / 16.0D);
    private static final AxisAlignedBB TIP_DOWN_SHAPE =
            new AxisAlignedBB(5.0D / 16.0D, 5.0D / 16.0D, 5.0D / 16.0D,
                    11.0D / 16.0D, 1.0D, 11.0D / 16.0D);
    private static final AxisAlignedBB FRUSTUM_SHAPE =
            new AxisAlignedBB(4.0D / 16.0D, 0.0D, 4.0D / 16.0D,
                    12.0D / 16.0D, 1.0D, 12.0D / 16.0D);
    private static final AxisAlignedBB MIDDLE_SHAPE =
            new AxisAlignedBB(3.0D / 16.0D, 0.0D, 3.0D / 16.0D,
                    13.0D / 16.0D, 1.0D, 13.0D / 16.0D);
    private static final AxisAlignedBB BASE_SHAPE =
            new AxisAlignedBB(2.0D / 16.0D, 0.0D, 2.0D / 16.0D,
                    14.0D / 16.0D, 1.0D, 14.0D / 16.0D);

    private final boolean waterloggedStorage;

    public BlockPointedDripstone(boolean waterloggedStorage) {
        super(Material.ROCK, MapColor.ADOBE);
        this.waterloggedStorage = waterloggedStorage;
        setUnlocalizedName("pointed_dripstone");
        setCreativeTab(waterloggedStorage ? null : CreativeTabs.BUILDING_BLOCKS);
        setSoundType(DripstoneSoundEvents.POINTED_DRIPSTONE);
        setHardness(1.5F);
        setResistance(CncBlockProperties.legacyResistance(3.0F));
        setTickRandomly(true);
        setDefaultState(blockState.getBaseState()
                .withProperty(TIP_DIRECTION, EnumFacing.UP)
                .withProperty(THICKNESS, Thickness.TIP));
    }

    public boolean isWaterloggedStorage() {
        return waterloggedStorage;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, TIP_DIRECTION, THICKNESS);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState()
                .withProperty(TIP_DIRECTION,
                        PointedDripstoneMechanics.tipUpFromMetadata(meta)
                                ? EnumFacing.UP : EnumFacing.DOWN)
                .withProperty(THICKNESS,
                        PointedDripstoneMechanics.thicknessFromMetadata(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return PointedDripstoneMechanics.metadata(
                state.getValue(TIP_DIRECTION) == EnumFacing.UP,
                state.getValue(THICKNESS));
    }

    @Override
    public int damageDropped(IBlockState state) {
        return 0;
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side,
            float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer,
            EnumHand hand) {
        EnumFacing preferred = placer.getLookVec().y > 0.0D
                ? EnumFacing.DOWN : EnumFacing.UP;
        EnumFacing direction = calculateTipDirection(world, pos, preferred);
        if (direction == null) {
            return getDefaultState();
        }
        boolean merge = !(placer instanceof EntityPlayer)
                || !((EntityPlayer) placer).isSneaking();
        Thickness thickness = calculateThickness(world, pos, direction, merge);
        BlockPointedDripstone target = sourceWater(world.getBlockState(pos))
                ? waterloggedBlock() : dryBlock();
        if (target == null) {
            target = this;
        }
        return target.getDefaultState()
                .withProperty(TIP_DIRECTION, direction)
                .withProperty(THICKNESS, thickness);
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return validPlacement(world, pos, EnumFacing.UP)
                || validPlacement(world, pos, EnumFacing.DOWN);
    }

    @Override
    public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side) {
        return canPlaceBlockAt(world, pos);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos,
            Block changedBlock, BlockPos changedPos) {
        EnumFacing direction = state.getValue(TIP_DIRECTION);
        BlockPos support = pos.offset(direction.getOpposite());
        if (!world.isRemote && changedPos.equals(support)
                && !validPlacement(world, pos, direction)) {
            world.scheduleUpdate(pos, this,
                    direction == EnumFacing.DOWN
                            ? PointedDripstoneMechanics.FALLING_DELAY : 1);
            return;
        }
        if (!world.isRemote && (changedPos.equals(pos.up()) || changedPos.equals(pos.down()))) {
            Thickness next = calculateThickness(world, pos, direction,
                    state.getValue(THICKNESS) == Thickness.TIP_MERGE);
            if (next != state.getValue(THICKNESS)) {
                world.setBlockState(pos, state.withProperty(THICKNESS, next), 2);
            }
        }
        if (!world.isRemote && waterloggedStorage) {
            emitWater(world, pos);
        }
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote && waterloggedStorage) {
            emitWater(world, pos);
        }
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos,
            EntityPlayer player, boolean willHarvest) {
        boolean removed = super.removedByPlayer(state, world, pos, player, willHarvest);
        if (removed && waterloggedStorage && world.isAirBlock(pos)) {
            world.setBlockState(pos, Blocks.WATER.getDefaultState(), 3);
        }
        return removed;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
            EntityPlayer player, EnumHand hand, EnumFacing side,
            float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        boolean fill = !waterloggedStorage && held.getItem() == Items.WATER_BUCKET;
        boolean drain = waterloggedStorage && held.getItem() == Items.BUCKET;
        if ((!fill && !drain) || !world.isBlockModifiable(player, pos)
                || !player.canPlayerEdit(pos, side, held)) {
            return false;
        }
        if (world.isRemote) {
            return true;
        }
        BlockPointedDripstone target = fill ? waterloggedBlock() : dryBlock();
        if (target == null) {
            return false;
        }
        world.setBlockState(pos, target.getDefaultState()
                .withProperty(TIP_DIRECTION, state.getValue(TIP_DIRECTION))
                .withProperty(THICKNESS, state.getValue(THICKNESS)), 3);
        Item original = held.getItem();
        replaceContainer(player, hand, held,
                new ItemStack(fill ? Items.BUCKET : Items.WATER_BUCKET));
        StatBase use = StatList.getObjectUseStats(original);
        if (use != null) {
            player.addStat(use);
        }
        world.playSound(null, pos,
                fill ? SoundEvents.ITEM_BUCKET_EMPTY : SoundEvents.ITEM_BUCKET_FILL,
                SoundCategory.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    private static void replaceContainer(EntityPlayer player, EnumHand hand,
            ItemStack held, ItemStack result) {
        if (player.capabilities.isCreativeMode) {
            if (!player.inventory.hasItemStack(result)) {
                player.inventory.addItemStackToInventory(result);
            }
            return;
        }
        held.shrink(1);
        if (held.isEmpty()) {
            player.setHeldItem(hand, result);
        } else if (!player.inventory.addItemStackToInventory(result)) {
            player.dropItem(result, false);
        }
    }

    private void emitWater(World world, BlockPos pos) {
        world.scheduleUpdate(pos, this, tickRate(world));
        for (EnumFacing side : EnumFacing.values()) {
            BlockPos neighbor = pos.offset(side);
            if (world.isAirBlock(neighbor)) {
                world.setBlockState(neighbor, Blocks.FLOWING_WATER.getDefaultState(), 2);
            }
        }
    }

    @Override
    public int tickRate(World world) {
        return 5;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world,
            BlockPos pos) {
        AxisAlignedBB shape = baseShape(state);
        Vec3d offset = getOffset(state, world, pos);
        return shape.offset(offset.x, 0.0D, offset.z);
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
            AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes,
            @Nullable Entity entity, boolean actualState) {
        addCollisionBoxToList(pos, entityBox, collidingBoxes,
                getBoundingBox(state, world, pos));
    }

    private static AxisAlignedBB baseShape(IBlockState state) {
        switch (state.getValue(THICKNESS)) {
            case TIP_MERGE:
                return TIP_MERGE_SHAPE;
            case TIP:
                return state.getValue(TIP_DIRECTION) == EnumFacing.DOWN
                        ? TIP_DOWN_SHAPE : TIP_UP_SHAPE;
            case FRUSTUM:
                return FRUSTUM_SHAPE;
            case MIDDLE:
                return MIDDLE_SHAPE;
            case BASE:
            default:
                return BASE_SHAPE;
        }
    }

    @Override
    public EnumOffsetType getOffsetType() {
        return EnumOffsetType.XZ;
    }

    @Override
    public Vec3d getOffset(IBlockState state, IBlockAccess world, BlockPos pos) {
        long seed = MathHelper.getCoordinateRandom(pos.getX(), 0, pos.getZ());
        double x = MathHelper.clamp(((double) ((float) (seed & 15L) / 15.0F) - 0.5D)
                * 0.5D, -0.125D, 0.125D);
        double z = MathHelper.clamp(((double) ((float) (seed >> 8 & 15L) / 15.0F) - 0.5D)
                * 0.5D, -0.125D, 0.125D);
        return new Vec3d(x, 0.0D, z);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getBlockLayer() {
        return waterloggedStorage ? BlockRenderLayer.TRANSLUCENT : BlockRenderLayer.CUTOUT;
    }

    @Override
    public EnumPushReaction getMobilityFlag(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        BlockPointedDripstone dry = dryBlock();
        return dry == null ? Items.AIR : Item.getItemFromBlock(dry);
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos,
            IBlockState state, int fortune) {
        Item item = getItemDropped(state, new Random(0L), fortune);
        if (item != null && item != Items.AIR) {
            drops.add(new ItemStack(item));
        }
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world,
            BlockPos pos, EntityPlayer player) {
        Item item = getItemDropped(state, world.rand, 0);
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public void onFallenUpon(World world, BlockPos pos, Entity entity, float fallDistance) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof BlockPointedDripstone
                && state.getValue(TIP_DIRECTION) == EnumFacing.UP
                && state.getValue(THICKNESS) == Thickness.TIP) {
            entity.fall(fallDistance + 2.0F, 2.0F);
        } else {
            super.onFallenUpon(world, pos, entity, fallDistance);
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos,
            Random random) {
        if (state.getValue(TIP_DIRECTION) != EnumFacing.DOWN
                || state.getValue(THICKNESS) != Thickness.TIP || waterloggedStorage) {
            return;
        }
        float roll = random.nextFloat();
        if (roll > 0.12F) {
            return;
        }
        Material fluid = fluidAboveStalactite(world, pos, state);
        if (roll < 0.02F || fluid == Material.WATER || fluid == Material.LAVA) {
            Vec3d offset = getOffset(state, world, pos);
            world.spawnParticle(fluid == Material.LAVA
                            ? EnumParticleTypes.DRIP_LAVA : EnumParticleTypes.DRIP_WATER,
                    pos.getX() + 0.5D + offset.x,
                    pos.getY() + 0.25D,
                    pos.getZ() + 0.5D + offset.z,
                    0.0D, 0.0D, 0.0D);
        }
    }

    private static EnumFacing calculateTipDirection(World world, BlockPos pos,
            EnumFacing preferred) {
        if (validPlacement(world, pos, preferred)) {
            return preferred;
        }
        EnumFacing opposite = preferred.getOpposite();
        return validPlacement(world, pos, opposite) ? opposite : null;
    }

    public static boolean validPlacement(World world, BlockPos pos, EnumFacing direction) {
        BlockPos support = pos.offset(direction.getOpposite());
        IBlockState supportState = world.getBlockState(support);
        return supportState.isSideSolid(world, support, direction)
                || pointedInDirection(supportState, direction);
    }

    public static Thickness calculateThickness(IBlockAccess world, BlockPos pos,
            EnumFacing direction, boolean mergeOpposingTips) {
        Neighbor forward = neighbor(world.getBlockState(pos.offset(direction)));
        Neighbor backward = neighbor(world.getBlockState(pos.offset(direction.getOpposite())));
        return PointedDripstoneMechanics.calculateThickness(direction == EnumFacing.UP,
                forward, backward, mergeOpposingTips);
    }

    private static Neighbor neighbor(IBlockState state) {
        if (!(state.getBlock() instanceof BlockPointedDripstone)) {
            return Neighbor.OTHER;
        }
        return Neighbor.pointed(state.getValue(TIP_DIRECTION) == EnumFacing.UP,
                state.getValue(THICKNESS));
    }

    public static boolean pointedInDirection(IBlockState state, EnumFacing direction) {
        return state.getBlock() instanceof BlockPointedDripstone
                && state.getValue(TIP_DIRECTION) == direction;
    }

    public static boolean canDrip(IBlockState state) {
        return state.getBlock() instanceof BlockPointedDripstone
                && state.getValue(TIP_DIRECTION) == EnumFacing.DOWN
                && state.getValue(THICKNESS) == Thickness.TIP
                && !((BlockPointedDripstone) state.getBlock()).waterloggedStorage;
    }

    private static Material fluidAboveStalactite(World world, BlockPos start,
            IBlockState state) {
        if (!pointedInDirection(state, EnumFacing.DOWN)) {
            return Material.AIR;
        }
        BlockPos cursor = start;
        for (int distance = 1;
                distance < PointedDripstoneMechanics.MAX_DRIP_TYPE_SEARCH; ++distance) {
            cursor = cursor.up();
            IBlockState candidate = world.getBlockState(cursor);
            if (!pointedInDirection(candidate, EnumFacing.DOWN)) {
                return world.getBlockState(cursor.up()).getMaterial();
            }
        }
        return Material.AIR;
    }

    private static boolean sourceWater(IBlockState state) {
        return state.getBlock() == Blocks.WATER
                && state.getValue(BlockLiquid.LEVEL) == 0;
    }

    private static BlockPointedDripstone dryBlock() {
        return BlockStalactite.block instanceof BlockPointedDripstone
                ? (BlockPointedDripstone) BlockStalactite.block : null;
    }

    private static BlockPointedDripstone waterloggedBlock() {
        return BlockPointedDripstoneWaterlogged.block instanceof BlockPointedDripstone
                ? (BlockPointedDripstone) BlockPointedDripstoneWaterlogged.block : null;
    }
}

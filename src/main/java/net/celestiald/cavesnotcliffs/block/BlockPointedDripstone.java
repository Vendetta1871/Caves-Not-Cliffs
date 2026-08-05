package net.celestiald.cavesnotcliffs.block;

import git.jbredwards.fluidlogged_api.api.block.IFluidloggable;
import net.celestiald.cavesnotcliffs.content.CncBlockProperties;
import net.celestiald.cavesnotcliffs.content.DripstoneSoundEvents;
import net.celestiald.cavesnotcliffs.client.ParticleDripstone;
import net.celestiald.cavesnotcliffs.compat.FluidloggedCompat;
import net.celestiald.cavesnotcliffs.dripstone.CauldronMechanics;
import net.celestiald.cavesnotcliffs.dripstone.CauldronMechanics.DripFluid;
import net.celestiald.cavesnotcliffs.dripstone.VanillaCauldronMeta;
import net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics;
import net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics.Neighbor;
import net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics.Thickness;
import net.celestiald.cavesnotcliffs.entity.EntityFallingPointedDripstone;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
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
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/** One canonical implementation for every Java 1.18.2 pointed-dripstone state. */
@Optional.Interface(iface = "git.jbredwards.fluidlogged_api.api.block.IFluidloggable",
        modid = "fluidlogged_api")
public final class BlockPointedDripstone extends Block implements IFluidloggable {
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

    public BlockPointedDripstone() {
        super(Material.ROCK, MapColor.ADOBE);
        setUnlocalizedName("pointed_dripstone");
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
        setSoundType(DripstoneSoundEvents.POINTED_DRIPSTONE);
        setHardness(1.5F);
        setResistance(CncBlockProperties.legacyResistance(3.0F));
        setTickRandomly(true);
        setDefaultState(blockState.getBaseState()
                .withProperty(TIP_DIRECTION, EnumFacing.UP)
                .withProperty(THICKNESS, Thickness.TIP));
    }

    @Override
    public boolean overrideApplyDefaultsSetting() {
        return true;
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
        return getDefaultState()
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
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        super.onBlockAdded(world, pos, state);
        if (FluidloggedCompat.hasFluid(world, pos)) {
            FluidloggedCompat.notifyFluids(world, pos, true);
        }
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
                FluidloggedCompat.notifyFluids(world, pos, false,
                        EnumFacing.UP, EnumFacing.DOWN);
            }
        }
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (world.isRemote || world.getBlockState(pos).getBlock() != this) {
            return;
        }
        if (state.getValue(TIP_DIRECTION) == EnumFacing.UP) {
            if (!validPlacement(world, pos, EnumFacing.UP)) {
                world.destroyBlock(pos, true);
            }
            return;
        }
        if (!validPlacement(world, pos, EnumFacing.DOWN)) {
            spawnFallingStalactite(world, pos, state);
        }
    }

    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
        maybeFillCauldron(state, world, pos, random.nextFloat());
        if (random.nextFloat() < PointedDripstoneMechanics.GROWTH_CHANCE
                && isStalactiteStart(state, world, pos)) {
            growStalactiteOrStalagmiteIfPossible(state, world, pos, random);
        }
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
        return xzOffset(pos);
    }

    private static Vec3d xzOffset(BlockPos pos) {
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

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state,
            BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public EnumPushReaction getMobilityFlag(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        Item item = Item.getItemFromBlock(this);
        return item == null ? Items.AIR : item;
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
                || state.getValue(THICKNESS) != Thickness.TIP
                || FluidloggedCompat.hasFluid(world, pos)) {
            return;
        }
        float roll = random.nextFloat();
        if (roll > 0.12F) {
            return;
        }
        DripFluid fluid = cauldronFillFluid(world, pos);
        if (roll < 0.02F || fluid != null) {
            Vec3d offset = getOffset(state, world, pos);
            boolean lavaParticle = fluid == DripFluid.LAVA
                    || fluid == null && world.provider.doesWaterVaporize();
            ParticleDripstone.spawn(world, pos.getX() + 0.5D + offset.x,
                    pos.getY() + 0.25D,
                    pos.getZ() + 0.5D + offset.z, lavaParticle);
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

    public static boolean canDrip(IBlockAccess world, BlockPos pos, IBlockState state) {
        return state.getBlock() instanceof BlockPointedDripstone
                && state.getValue(TIP_DIRECTION) == EnumFacing.DOWN
                && state.getValue(THICKNESS) == Thickness.TIP
                && !FluidloggedCompat.hasFluid(world, pos);
    }

    private static void spawnFallingStalactite(World world, BlockPos root,
            IBlockState rootState) {
        BlockPos cursor = root;
        IBlockState state = rootState;
        while (pointedInDirection(state, EnumFacing.DOWN)) {
            float damageFactor = isTip(state, true)
                    ? PointedDripstoneMechanics.fallingDamagePerDistance(
                            root.getY(), cursor.getY()) : 0.0F;
            EntityFallingPointedDripstone.EntityCustom falling =
                    new EntityFallingPointedDripstone.EntityCustom(world,
                            cursor.getX() + 0.5D, cursor.getY(), cursor.getZ() + 0.5D,
                            state, damageFactor);
            replaceSourceForFall(world, cursor);
            falling.fallTime = 1;
            world.spawnEntity(falling);
            if (isTip(state, true)) {
                break;
            }
            cursor = cursor.down();
            state = world.getBlockState(cursor);
        }
    }

    private static void replaceSourceForFall(World world, BlockPos pos) {
        if (!FluidloggedCompat.restoreFluid(world, pos, 3)) {
            world.setBlockToAir(pos);
        }
    }

    public static void maybeFillCauldron(IBlockState state, World world, BlockPos pos,
            float randomValue) {
        if (randomValue > PointedDripstoneMechanics.WATER_CAULDRON_FILL_CHANCE
                && randomValue > PointedDripstoneMechanics.LAVA_CAULDRON_FILL_CHANCE) {
            return;
        }
        if (!isStalactiteStart(state, world, pos)) {
            return;
        }
        DripFluid fluid = cauldronFillFluid(world, pos);
        if (fluid == null || !PointedDripstoneMechanics.shouldAttemptCauldronFill(
                fluid == DripFluid.LAVA, randomValue)) {
            return;
        }
        BlockPos tip = findTip(state, world, pos,
                PointedDripstoneMechanics.MAX_DRIP_TYPE_SEARCH, false);
        if (tip == null) {
            return;
        }
        BlockPos cauldron = findFillableCauldronBelowTip(world, tip, fluid);
        if (cauldron == null) {
            return;
        }
        IBlockState cauldronState = world.getBlockState(cauldron);
        // 1.18 dedups pending block ticks per position; 1.12 does not, so repeated schedules
        // would burst-fire every fill stage at once once the first delay elapses.
        if (world.isUpdateScheduled(cauldron, cauldronState.getBlock())) {
            return;
        }
        // Show the drip that is on its way: it detaches from the tip now and lands roughly
        // when the scheduled fill applies cauldronDelay ticks later.
        spawnFallingDrip(world, tip, fluid);
        world.scheduleUpdate(cauldron, cauldronState.getBlock(),
                PointedDripstoneMechanics.cauldronDelay(tip.getY(), cauldron.getY()));
    }

    private static void spawnFallingDrip(World world, BlockPos tip, DripFluid fluid) {
        if (!(world instanceof WorldServer)) {
            return;
        }
        Vec3d offset = xzOffset(tip);
        ((WorldServer) world).spawnParticle(
                fluid == DripFluid.LAVA
                        ? EnumParticleTypes.DRIP_LAVA : EnumParticleTypes.DRIP_WATER,
                tip.getX() + 0.5D + offset.x, tip.getY() + 0.25D,
                tip.getZ() + 0.5D + offset.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    @Nullable
    public static BlockPos findStalactiteTipAboveCauldron(World world, BlockPos cauldron) {
        BlockPos cursor = cauldron;
        for (int distance = 1;
                distance < PointedDripstoneMechanics.MAX_CAULDRON_SEARCH; ++distance) {
            cursor = cursor.up();
            IBlockState state = world.getBlockState(cursor);
            if (canDrip(world, cursor, state)) {
                return cursor;
            }
            if (!canDripThrough(world, cursor, state)) {
                return null;
            }
        }
        return null;
    }

    @Nullable
    public static DripFluid cauldronFillFluid(World world, BlockPos pointedPos) {
        IBlockState pointed = world.getBlockState(pointedPos);
        if (!pointedInDirection(pointed, EnumFacing.DOWN)) {
            return null;
        }
        BlockPos cursor = pointedPos;
        for (int distance = 1;
                distance < PointedDripstoneMechanics.MAX_DRIP_TYPE_SEARCH; ++distance) {
            cursor = cursor.up();
            IBlockState state = world.getBlockState(cursor);
            if (pointedInDirection(state, EnumFacing.DOWN)) {
                continue;
            }
            Material material = world.getBlockState(cursor.up()).getMaterial();
            if (material == Material.WATER) {
                return DripFluid.WATER;
            }
            if (material == Material.LAVA) {
                return DripFluid.LAVA;
            }
            return null;
        }
        return null;
    }

    @Nullable
    private static BlockPos findFillableCauldronBelowTip(World world, BlockPos tip,
            DripFluid fluid) {
        BlockPos cursor = tip;
        for (int distance = 1;
                distance < PointedDripstoneMechanics.MAX_CAULDRON_SEARCH; ++distance) {
            cursor = cursor.down();
            IBlockState state = world.getBlockState(cursor);
            if (state.getBlock() instanceof net.minecraft.block.BlockCauldron) {
                // The vanilla cauldron stores every 1.18.2 content directly (CauldronMixin);
                // legacy hidden storage blocks are migrated back to vanilla on chunk load and
                // simply stop the drip here in the meantime.
                if (state.getBlock() != Blocks.CAULDRON) {
                    return null;
                }
                return CauldronMechanics.canReceiveDrip(VanillaCauldronMeta.fromMeta(
                        state.getBlock().getMetaFromState(state)), fluid) ? cursor : null;
            }
            if (!canDripThrough(world, cursor, state)) {
                return null;
            }
        }
        return null;
    }

    private static boolean canDripThrough(IBlockAccess world, BlockPos pos,
            IBlockState state) {
        if (state.getMaterial() == Material.AIR) {
            return true;
        }
        if (state.isOpaqueCube() || state.getMaterial().isLiquid()) {
            return false;
        }
        AxisAlignedBB collision = state.getCollisionBoundingBox(world, pos);
        if (collision == null) {
            return true;
        }
        AxisAlignedBB required = new AxisAlignedBB(6.0D / 16.0D, 0.0D,
                6.0D / 16.0D, 10.0D / 16.0D, 1.0D, 10.0D / 16.0D);
        return !collision.intersects(required);
    }

    private static boolean isStalactiteStart(IBlockState state, IBlockAccess world,
            BlockPos pos) {
        return pointedInDirection(state, EnumFacing.DOWN)
                && !(world.getBlockState(pos.up()).getBlock() instanceof BlockPointedDripstone);
    }

    @Nullable
    private static BlockPos findTip(IBlockState state, IBlockAccess world, BlockPos pos,
            int maxLength, boolean allowMerged) {
        if (isTip(state, allowMerged)) {
            return pos;
        }
        EnumFacing direction = state.getValue(TIP_DIRECTION);
        BlockPos cursor = pos;
        for (int distance = 1; distance < maxLength; ++distance) {
            cursor = cursor.offset(direction);
            IBlockState candidate = world.getBlockState(cursor);
            if (!pointedInDirection(candidate, direction)) {
                return null;
            }
            if (isTip(candidate, allowMerged)) {
                return cursor;
            }
        }
        return null;
    }

    private static boolean isTip(IBlockState state, boolean allowMerged) {
        if (!(state.getBlock() instanceof BlockPointedDripstone)) {
            return false;
        }
        Thickness thickness = state.getValue(THICKNESS);
        return thickness == Thickness.TIP
                || allowMerged && thickness == Thickness.TIP_MERGE;
    }

    public static void growStalactiteOrStalagmiteIfPossible(IBlockState state,
            World world, BlockPos pos, Random random) {
        if (world.getBlockState(pos.up()).getBlock() != BlockDripstone.block
                || !sourceWater(world.getBlockState(pos.up(2)))) {
            return;
        }
        BlockPos tip = findTip(state, world, pos,
                PointedDripstoneMechanics.MAX_GROWTH_LENGTH, false);
        if (tip == null) {
            return;
        }
        IBlockState tipState = world.getBlockState(tip);
        if (!canDrip(world, tip, tipState) || !canTipGrow(tipState, world, tip)) {
            return;
        }
        if (random.nextBoolean()) {
            grow(world, tip, EnumFacing.DOWN);
        } else {
            growStalagmiteBelow(world, tip);
        }
    }

    private static boolean canTipGrow(IBlockState state, World world, BlockPos pos) {
        EnumFacing direction = state.getValue(TIP_DIRECTION);
        BlockPos forwardPos = pos.offset(direction);
        IBlockState forward = world.getBlockState(forwardPos);
        if (forward.getMaterial().isLiquid()
                || FluidloggedCompat.hasFluid(world, forwardPos)) {
            return false;
        }
        return forward.getMaterial() == Material.AIR
                || isTip(forward, false)
                && forward.getValue(TIP_DIRECTION) == direction.getOpposite();
    }

    private static void growStalagmiteBelow(World world, BlockPos tip) {
        BlockPos cursor = tip;
        for (int distance = 0;
                distance < PointedDripstoneMechanics.MAX_STALAGMITE_GROWTH_SEARCH;
                ++distance) {
            cursor = cursor.down();
            IBlockState state = world.getBlockState(cursor);
            if (state.getMaterial().isLiquid()
                    || FluidloggedCompat.hasFluid(world, cursor)) {
                return;
            }
            if (isTip(state, false) && state.getValue(TIP_DIRECTION) == EnumFacing.UP
                    && canTipGrow(state, world, cursor)) {
                grow(world, cursor, EnumFacing.UP);
                return;
            }
            if (validPlacement(world, cursor, EnumFacing.UP)
                    && world.getBlockState(cursor.down()).getMaterial() != Material.WATER) {
                grow(world, cursor.down(), EnumFacing.UP);
                return;
            }
            if (!canDripThrough(world, cursor, state)) {
                return;
            }
        }
    }

    private static void grow(World world, BlockPos tip, EnumFacing direction) {
        BlockPos target = tip.offset(direction);
        IBlockState targetState = world.getBlockState(target);
        if (isTip(targetState, false)
                && targetState.getValue(TIP_DIRECTION) == direction.getOpposite()) {
            createMergedTips(world, target, targetState);
        } else if (targetState.getMaterial() == Material.AIR
                || targetState.getMaterial() == Material.WATER) {
            createDripstone(world, target, direction, Thickness.TIP);
        }
    }

    private static void createMergedTips(World world, BlockPos target,
            IBlockState existing) {
        BlockPos down;
        BlockPos up;
        if (existing.getValue(TIP_DIRECTION) == EnumFacing.UP) {
            down = target;
            up = target.up();
        } else {
            down = target.down();
            up = target;
        }
        createDripstone(world, down, EnumFacing.DOWN, Thickness.TIP_MERGE);
        createDripstone(world, up, EnumFacing.UP, Thickness.TIP_MERGE);
    }

    public static void createDripstone(World world, BlockPos pos, EnumFacing direction,
            Thickness thickness) {
        IBlockState existing = world.getBlockState(pos);
        boolean water = existing.getMaterial() == Material.WATER
                || FluidloggedCompat.hasWater(world, pos);
        BlockPointedDripstone target = dryBlock();
        if (target != null) {
            IBlockState pointed = target.getDefaultState()
                    .withProperty(TIP_DIRECTION, direction)
                    .withProperty(THICKNESS, thickness);
            if (world.setBlockState(pos, pointed, 3) && water) {
                FluidloggedCompat.storeWater(world, pos, pointed, existing, 3);
            }
        }
    }

    private static boolean sourceWater(IBlockState state) {
        return (state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.FLOWING_WATER)
                && state.getValue(BlockLiquid.LEVEL) == 0;
    }

    private static BlockPointedDripstone dryBlock() {
        return BlockStalactite.block instanceof BlockPointedDripstone
                ? (BlockPointedDripstone) BlockStalactite.block : null;
    }
}

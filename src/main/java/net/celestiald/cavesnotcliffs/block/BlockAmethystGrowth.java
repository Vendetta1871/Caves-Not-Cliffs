package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.content.AmethystSoundEvents;
import net.celestiald.cavesnotcliffs.content.CncMaterialContent;
import net.celestiald.cavesnotcliffs.content.OreDropLogic;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

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
    private final boolean waterloggedStorage;
    private final String publicStagePath;
    private final ThreadLocal<ItemStack> harvestingTool = new ThreadLocal<>();

    public BlockAmethystGrowth(String name, int height, int offset, int lightLevel,
            boolean cluster) {
        this(name, height, offset, lightLevel, cluster, false);
    }

    private BlockAmethystGrowth(String name, int height, int offset, int lightLevel,
            boolean cluster, boolean waterloggedStorage) {
        super(Material.ROCK, MapColor.PURPLE);
        this.lightLevel = lightLevel;
        this.cluster = cluster;
        this.waterloggedStorage = waterloggedStorage;
        this.publicStagePath = AmethystWaterlogging.isCompanionPath(name)
                ? AmethystWaterlogging.publicPath(name) : name;

        double h = height / 16.0D;
        double o = offset / 16.0D;
        double opposite = 1.0D - o;
        northAabb = new AxisAlignedBB(o, o, 1.0D - h, opposite, opposite, 1.0D);
        southAabb = new AxisAlignedBB(o, o, 0.0D, opposite, opposite, h);
        eastAabb = new AxisAlignedBB(0.0D, o, o, h, opposite, opposite);
        westAabb = new AxisAlignedBB(1.0D - h, o, o, 1.0D, opposite, opposite);
        upAabb = new AxisAlignedBB(o, 0.0D, o, opposite, h, opposite);
        downAabb = new AxisAlignedBB(o, 1.0D - h, o, opposite, 1.0D, opposite);

        setUnlocalizedName(publicStagePath);
        setSoundType(AmethystSoundEvents.forGrowthStage(publicStagePath));
        setHardness(1.5F);
        setResistance(2.5F);
        setDefaultState(blockState.getBaseState()
                .withProperty(WATERLOGGED, waterloggedStorage)
                .withProperty(FACING, EnumFacing.UP));
    }

    /** Creates a hidden block-only storage identity for a waterlogged public stage. */
    public static BlockAmethystGrowth waterloggedCompanion(String publicStagePath,
            int height, int offset, int lightLevel, boolean cluster) {
        return new BlockAmethystGrowth(AmethystWaterlogging.companionPath(publicStagePath),
                height, offset, lightLevel, cluster, true);
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
        return waterloggedStorage ? BlockRenderLayer.TRANSLUCENT : BlockRenderLayer.CUTOUT;
    }

    @Override
    public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side) {
        return hasSupport(world, pos, side);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
            float hitX, float hitY, float hitZ, int meta,
            net.minecraft.entity.EntityLivingBase placer) {
        boolean waterlogged = isSourceWater(world.getBlockState(pos));
        BlockAmethystGrowth storage = waterlogged ? waterloggedCompanion() : this;
        if (storage == null) {
            storage = this;
        }
        return storage.getDefaultState()
                .withProperty(WATERLOGGED, waterlogged || storage.waterloggedStorage)
                .withProperty(FACING, facing);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos,
            Block changedBlock, BlockPos changedPos) {
        EnumFacing facing = state.getValue(FACING);
        BlockPos support = pos.offset(facing.getOpposite());
        if (!world.isRemote && changedPos.equals(support) && !hasSupport(world, pos, facing)) {
            world.destroyBlock(pos, true);
            return;
        }
        if (!world.isRemote && isWaterlogged(state)) {
            emitWater(world, pos);
        }
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote && isWaterlogged(state)) {
            emitWater(world, pos);
        }
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (!world.isRemote && isWaterlogged(state)) {
            emitWater(world, pos);
        }
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        super.breakBlock(world, pos, state);
        if (isWaterlogged(state) && world.isAirBlock(pos)) {
            world.setBlockState(pos, Blocks.WATER.getDefaultState(), 3);
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
            EntityPlayer player, EnumHand hand, EnumFacing facing,
            float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        boolean waterlogged = isWaterlogged(state);
        if (held.isEmpty()) {
            return false;
        }

        if (!waterlogged && held.getItem() == Items.WATER_BUCKET) {
            if (!world.isRemote) {
                BlockAmethystGrowth companion = waterloggedCompanion();
                if (companion == null) {
                    return false;
                }
                world.setBlockState(pos, companion.getDefaultState()
                        .withProperty(FACING, state.getValue(FACING))
                        .withProperty(WATERLOGGED, true), 3);
                replaceBucket(player, hand, held, new ItemStack(Items.BUCKET));
                addUseStat(player, Items.WATER_BUCKET);
                world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY,
                        SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
            return true;
        }

        if (waterlogged && held.getItem() == Items.BUCKET) {
            if (!world.isRemote) {
                BlockAmethystGrowth dry = publicStageBlock();
                if (dry == null) {
                    return false;
                }
                world.setBlockState(pos, dry.getDefaultState()
                        .withProperty(FACING, state.getValue(FACING))
                        .withProperty(WATERLOGGED, false), 3);
                replaceBucket(player, hand, held, new ItemStack(Items.WATER_BUCKET));
                addUseStat(player, Items.BUCKET);
                world.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL,
                        SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
            return true;
        }
        return false;
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
                ItemStack tool = harvestingTool.get();
                if (tool == null) {
                    EntityPlayer player = harvesters.get();
                    tool = player == null ? ItemStack.EMPTY : player.getHeldItemMainhand();
                }
                int count = clusterDropCount(tool, fortune,
                        world instanceof World ? ((World) world).rand : new Random(0L));
                drops.add(new ItemStack(shard, count));
            }
        }
    }

    /** Growth blocks intentionally allow harvesting by every tool; their loot table decides drops. */
    @Override
    public boolean canHarvestBlock(IBlockAccess world, BlockPos pos, EntityPlayer player) {
        return true;
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state,
            TileEntity tileEntity, ItemStack tool) {
        harvestingTool.set(tool == null ? ItemStack.EMPTY : tool);
        try {
            // Retain vanilla/Forge Silk Touch handling and HarvestDropsEvent dispatch.
            super.harvestBlock(world, player, pos, state, tileEntity, tool);
        } finally {
            harvestingTool.remove();
        }
    }

    static int clusterDropCount(ItemStack tool, int fortune, Random random) {
        if (tool != null && !tool.isEmpty()
                && tool.getItem().getToolClasses(tool).contains("pickaxe")) {
            return OreDropLogic.applyOreBonus(4, fortune, random);
        }
        return 2;
    }

    @Override
    protected boolean canSilkHarvest() {
        return true;
    }

    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state) {
        BlockAmethystGrowth publicBlock = publicStageBlock();
        Item item = publicBlock == null ? Items.AIR : Item.getItemFromBlock(publicBlock);
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world,
            BlockPos pos, EntityPlayer player) {
        return getSilkTouchDrop(state);
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
        boolean metadataWaterlogged = (meta & 8) != 0;
        BlockAmethystGrowth decoded = this;
        if (metadataWaterlogged && !waterloggedStorage) {
            BlockAmethystGrowth companion = waterloggedCompanion();
            if (companion != null) {
                decoded = companion;
            }
        }
        return decoded.getDefaultState()
                .withProperty(WATERLOGGED, decoded.waterloggedStorage || metadataWaterlogged)
                .withProperty(FACING, facing);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex()
                | (state.getValue(WATERLOGGED) ? 8 : 0);
    }

    @Override
    public Boolean isEntityInsideMaterial(IBlockAccess world, BlockPos pos, IBlockState state,
            Entity entity, double yToTest, Material material, boolean testingHead) {
        if (material != Material.WATER || !isWaterlogged(state)) {
            return null;
        }
        double surface = pos.getY() + 1.0D;
        if (testingHead) {
            return yToTest < surface;
        }
        AxisAlignedBB bounds = entity.getEntityBoundingBox();
        return bounds.maxY > pos.getY() && bounds.minY < surface;
    }

    @Override
    public Boolean isAABBInsideMaterial(World world, BlockPos pos, AxisAlignedBB bounds,
            Material material) {
        if (material != Material.WATER) {
            return null;
        }
        IBlockState state = world.getBlockState(pos);
        return isWaterlogged(state) && intersectsWater(pos, bounds);
    }

    @Override
    public Boolean isAABBInsideLiquid(World world, BlockPos pos, AxisAlignedBB bounds) {
        IBlockState state = world.getBlockState(pos);
        return isWaterlogged(state) && intersectsWater(pos, bounds);
    }

    @Override
    public float getBlockLiquidHeight(World world, BlockPos pos, IBlockState state,
            Material material) {
        return material == Material.WATER && isWaterlogged(state) ? 1.0F : 0.0F;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean shouldSideBeRendered(IBlockState state, IBlockAccess source, BlockPos pos,
            EnumFacing side) {
        if (isWaterlogged(state)) {
            IBlockState neighbor = source.getBlockState(pos.offset(side));
            if (neighbor.getMaterial() == Material.WATER
                    || neighbor.getBlock() instanceof BlockAmethystGrowth
                    && ((BlockAmethystGrowth) neighbor.getBlock()).isWaterlogged(neighbor)) {
                return false;
            }
        }
        return super.shouldSideBeRendered(state, source, pos, side);
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

    public boolean isWaterloggedStorage() {
        return waterloggedStorage;
    }

    public BlockAmethystGrowth getWaterloggedCompanion() {
        return waterloggedCompanion();
    }

    public String getPublicStagePath() {
        return publicStagePath;
    }

    public boolean isWaterlogged(IBlockState state) {
        return waterloggedStorage || state.getValue(WATERLOGGED);
    }

    private BlockAmethystGrowth waterloggedCompanion() {
        if (waterloggedStorage) {
            return this;
        }
        if (!AmethystWaterlogging.PUBLIC_STAGES.contains(publicStagePath)) {
            return null;
        }
        Block block = ForgeRegistries.BLOCKS.getValue(new net.minecraft.util.ResourceLocation(
                CavesNotCliffs.MODID, AmethystWaterlogging.companionPath(publicStagePath)));
        return block instanceof BlockAmethystGrowth ? (BlockAmethystGrowth) block : null;
    }

    private BlockAmethystGrowth publicStageBlock() {
        if (!waterloggedStorage && publicStagePath.equals(registryPath(this))) {
            return this;
        }
        Block block = ForgeRegistries.BLOCKS.getValue(new net.minecraft.util.ResourceLocation(
                CavesNotCliffs.MODID, publicStagePath));
        return block instanceof BlockAmethystGrowth ? (BlockAmethystGrowth) block : null;
    }

    private static String registryPath(Block block) {
        return block.getRegistryName() == null ? null : block.getRegistryName().getResourcePath();
    }

    private static boolean isSourceWater(IBlockState state) {
        return state.getBlock() == Blocks.WATER
                && state.getValue(BlockLiquid.LEVEL) == 0;
    }

    private static boolean intersectsWater(BlockPos pos, AxisAlignedBB bounds) {
        return bounds.maxX > pos.getX() && bounds.minX < pos.getX() + 1.0D
                && bounds.maxY > pos.getY() && bounds.minY < pos.getY() + 1.0D
                && bounds.maxZ > pos.getZ() && bounds.minZ < pos.getZ() + 1.0D;
    }

    private void emitWater(World world, BlockPos pos) {
        flowInto(world, pos, EnumFacing.DOWN, 8);
        flowInto(world, pos, EnumFacing.NORTH, 1);
        flowInto(world, pos, EnumFacing.SOUTH, 1);
        flowInto(world, pos, EnumFacing.WEST, 1);
        flowInto(world, pos, EnumFacing.EAST, 1);
    }

    private static void flowInto(World world, BlockPos origin, EnumFacing direction, int level) {
        BlockPos target = origin.offset(direction);
        IBlockState existing = world.getBlockState(target);
        Material material = existing.getMaterial();
        if (material == Material.WATER || material == Material.LAVA
                || !existing.getBlock().isAir(existing, world, target) && !material.isReplaceable()) {
            return;
        }
        if (!existing.getBlock().isAir(existing, world, target)) {
            existing.getBlock().dropBlockAsItem(world, target, existing, 0);
        }
        world.setBlockState(target, Blocks.FLOWING_WATER.getDefaultState()
                .withProperty(BlockLiquid.LEVEL, level), 3);
    }

    private static void replaceBucket(EntityPlayer player, EnumHand hand, ItemStack held,
            ItemStack replacement) {
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

    private static void addUseStat(EntityPlayer player, Item item) {
        StatBase stat = StatList.getObjectUseStats(item);
        if (stat != null) {
            player.addStat(stat);
        }
    }

    private static boolean hasSupport(World world, BlockPos pos, EnumFacing facing) {
        BlockPos support = pos.offset(facing.getOpposite());
        return world.getBlockState(support).isSideSolid(world, support, facing);
    }
}

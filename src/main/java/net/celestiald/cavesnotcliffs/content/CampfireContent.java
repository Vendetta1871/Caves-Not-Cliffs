package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.block.LushWaterlogging;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.celestiald.cavesnotcliffs.handler.CampfireProjectileHandler;
import net.celestiald.cavesnotcliffs.tile.TileEntityCampfire;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.Mirror;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.Explosion;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/** Ordinary/soul campfires and soul soil with Java 1.18.2 behavior. */
@ElementsCavesNotCliffs.ModElement.Tag
public final class CampfireContent extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:campfire")
    public static final Block campfire = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:soul_campfire")
    public static final Block soulCampfire = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:soul_soil")
    public static final Block soulSoil = null;

    public CampfireContent(ElementsCavesNotCliffs elements) {
        super(elements, 360);
    }

    @Override
    public void initElements() {
        CampfireSoundEvents.registerAll();
        elements.blocks.add(() -> new BlockCustom(false)
            .setRegistryName(CncRegistryIds.CAMPFIRE));
        elements.blocks.add(() -> new BlockCustom(true)
            .setRegistryName(CncRegistryIds.SOUL_CAMPFIRE));
        elements.blocks.add(() -> new SoulSoilBlock()
            .setRegistryName(CncRegistryIds.SOUL_SOIL));
        elements.items.add(() -> new ItemBlock(campfire)
            .setRegistryName(CncRegistryIds.CAMPFIRE));
        elements.items.add(() -> new ItemBlock(soulCampfire)
            .setRegistryName(CncRegistryIds.SOUL_CAMPFIRE));
        elements.items.add(() -> new ItemBlock(soulSoil)
            .setRegistryName(CncRegistryIds.SOUL_SOIL));
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        GameRegistry.registerTileEntity(TileEntityCampfire.class, CncRegistryIds.CAMPFIRE);
        BeehiveSmokeHooks.register(CampfireContent::isSmokeyPos);
        MinecraftForge.EVENT_BUS.register(CampfireProjectileHandler.INSTANCE);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        registerModel(Item.getItemFromBlock(campfire), CncRegistryIds.CAMPFIRE);
        registerModel(Item.getItemFromBlock(soulCampfire), CncRegistryIds.SOUL_CAMPFIRE);
        registerModel(Item.getItemFromBlock(soulSoil), CncRegistryIds.SOUL_SOIL);
    }

    @SideOnly(Side.CLIENT)
    private static void registerModel(Item item, net.minecraft.util.ResourceLocation id) {
        ModelLoader.setCustomModelResourceLocation(item, 0,
            new ModelResourceLocation(id, "inventory"));
    }

    public static boolean isLitCampfire(IBlockState state) {
        return state != null && state.getBlock() instanceof BlockCustom
            && state.getValue(BlockCustom.LIT);
    }

    public static boolean isSignalFire(World world, BlockPos pos) {
        return world != null && world.getBlockState(pos.down()).getBlock() == Blocks.HAY_BLOCK;
    }

    public static boolean isSmokeyPos(World world, BlockPos hivePos) {
        for (int distance = 1; distance <= CampfireMechanics.SMOKE_DISTANCE; ++distance) {
            BlockPos candidate = hivePos.down(distance);
            if (isLitCampfire(world.getBlockState(candidate))) {
                return true;
            }
            if (blocksSmokeColumn(world, candidate)) {
                return isLitCampfire(world.getBlockState(candidate.down()));
            }
        }
        return false;
    }

    static boolean blocksSmokeColumn(IBlockAccess world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        AxisAlignedBB collision = state.getCollisionBoundingBox(world, pos);
        if (collision == null || collision == Block.NULL_AABB) {
            return false;
        }
        AxisAlignedBB center = new AxisAlignedBB(6.0D / 16.0D, 0.0D,
            6.0D / 16.0D, 10.0D / 16.0D, 1.0D, 10.0D / 16.0D);
        return collision.intersects(center);
    }

    @SideOnly(Side.CLIENT)
    public static void clientParticleTick(World world, BlockPos pos, boolean signal,
            NonNullList<ItemStack> items) {
        Random random = world.rand;
        if (random.nextFloat() < 0.11F) {
            int count = random.nextInt(2) + 2;
            for (int index = 0; index < count; ++index) {
                makeParticles(world, pos, signal, false);
            }
        }
        int facing = world.getBlockState(pos).getValue(BlockCustom.FACING)
            .getHorizontalIndex();
        for (int slot = 0; slot < items.size(); ++slot) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty() && random.nextFloat() < 0.2F) {
                EnumFacing direction = EnumFacing.getHorizontal(
                    Math.floorMod(slot + facing, 4));
                EnumFacing clockwise = direction.rotateY();
                double x = pos.getX() + 0.5D
                    - direction.getFrontOffsetX() * 0.3125D
                    + clockwise.getFrontOffsetX() * 0.3125D;
                double z = pos.getZ() + 0.5D
                    - direction.getFrontOffsetZ() * 0.3125D
                    + clockwise.getFrontOffsetZ() * 0.3125D;
                for (int particle = 0; particle < 4; ++particle) {
                    world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                        x, pos.getY() + 0.5D, z, 0.0D, 5.0E-4D, 0.0D);
                }
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public static void makeParticles(World world, BlockPos pos, boolean signal,
            boolean extraSmoke) {
        Random random = world.rand;
        double x = pos.getX() + 0.5D
            + random.nextDouble() / 3.0D * (random.nextBoolean() ? 1 : -1);
        double y = pos.getY() + random.nextDouble() + random.nextDouble();
        double z = pos.getZ() + 0.5D
            + random.nextDouble() / 3.0D * (random.nextBoolean() ? 1 : -1);
        world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, x, y, z,
            0.0D, signal ? 0.14D : 0.07D, 0.0D);
        if (extraSmoke) {
            world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                pos.getX() + 0.5D, pos.getY() + 0.4D,
                pos.getZ() + 0.5D, 0.0D, 0.005D, 0.0D);
        }
    }

    public static final class BlockCustom extends BlockContainer {
        public static final PropertyDirection FACING = BlockHorizontal.FACING;
        public static final PropertyBool LIT = PropertyBool.create("lit");
        public static final PropertyBool WATERLOGGED = PropertyBool.create("waterlogged");
        private static final AxisAlignedBB SHAPE = new AxisAlignedBB(
            0.0D, 0.0D, 0.0D, 1.0D, 7.0D / 16.0D, 1.0D);
        // valueOf avoids a production MCP-field reference rejected by verifyReleaseJar.
        private static final EnumParticleTypes LAVA_PARTICLE =
            EnumParticleTypes.valueOf("LAVA");

        private final boolean soul;

        BlockCustom(boolean soul) {
            super(Material.WOOD);
            this.soul = soul;
            setUnlocalizedName(soul ? "soul_campfire" : "campfire");
            setCreativeTab(CreativeTabs.DECORATIONS);
            setHardness(2.0F);
            setResistance(CncBlockProperties.legacyResistance(2.0F));
            setSoundType(net.minecraft.block.SoundType.WOOD);
            setDefaultState(blockState.getBaseState()
                .withProperty(FACING, EnumFacing.NORTH)
                .withProperty(LIT, true)
                .withProperty(WATERLOGGED, false));
        }

        public boolean isSoul() { return soul; }

        /** Places retained source water without applying bucket inventory bookkeeping. */
        public boolean placeSourceWater(@Nullable Entity source, World world, BlockPos pos,
                IBlockState state) {
            if (state.getBlock() != this || state.getValue(WATERLOGGED)) {
                return false;
            }
            if (!world.isRemote && state.getValue(LIT)) {
                world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);
                douse(source, world, pos, state);
            }
            IBlockState wet = state.withProperty(WATERLOGGED, true)
                .withProperty(LIT, false);
            if (!world.setBlockState(pos, wet, 3)) {
                return false;
            }
            scheduleWater(world, pos, wet);
            LushWaterlogging.schedule(world, pos, true);
            return true;
        }

        @Override protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, FACING, LIT, WATERLOGGED);
        }

        @Override public IBlockState getStateFromMeta(int meta) {
            return getDefaultState()
                .withProperty(FACING, EnumFacing.getHorizontal(
                    CampfireMechanics.horizontalFacing(meta)))
                .withProperty(LIT, CampfireMechanics.lit(meta))
                .withProperty(WATERLOGGED, CampfireMechanics.waterlogged(meta));
        }

        @Override public int getMetaFromState(IBlockState state) {
            return CampfireMechanics.metadata(state.getValue(FACING).getHorizontalIndex(),
                state.getValue(LIT), state.getValue(WATERLOGGED));
        }

        @Override public IBlockState getStateForPlacement(World world, BlockPos pos,
                EnumFacing side, float hitX, float hitY, float hitZ, int meta,
                EntityLivingBase placer, EnumHand hand) {
            Block existing = world.getBlockState(pos).getBlock();
            boolean waterlogged = existing == Blocks.WATER || existing == Blocks.FLOWING_WATER;
            return getDefaultState()
                .withProperty(FACING, placer.getHorizontalFacing())
                .withProperty(WATERLOGGED, waterlogged)
                .withProperty(LIT, CampfireMechanics.placementLit(waterlogged));
        }

        @Override public TileEntity createNewTileEntity(World world, int meta) {
            return new TileEntityCampfire();
        }

        @Override public boolean onBlockActivated(World world, BlockPos pos,
                IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side,
                float hitX, float hitY, float hitZ) {
            ItemStack held = player.getHeldItem(hand);
            if (tryStateInteraction(world, pos, state, player, hand, held)) {
                return true;
            }
            TileEntity tile = world.getTileEntity(pos);
            if (!(tile instanceof TileEntityCampfire)) {
                return false;
            }
            TileEntityCampfire campfire = (TileEntityCampfire) tile;
            CampfireCooking.Recipe recipe = campfire.getCookableRecipe(held);
            if (recipe == null) {
                return false;
            }
            if (!world.isRemote) {
                ItemStack source = player.capabilities.isCreativeMode ? held.copy() : held;
                if (campfire.placeFood(source, recipe)) {
                    StatBase stat = StatList.getObjectUseStats(held.getItem());
                    if (stat != null) player.addStat(stat);
                }
            }
            return true;
        }

        private boolean tryStateInteraction(World world, BlockPos pos, IBlockState state,
                EntityPlayer player, EnumHand hand, ItemStack held) {
            if (held.isEmpty()) return false;
            if ((held.getItem() == Items.FLINT_AND_STEEL
                    || held.getItem() == Items.FIRE_CHARGE)
                    && CampfireMechanics.canLight(state.getValue(LIT),
                        state.getValue(WATERLOGGED))) {
                if (!world.isRemote) {
                    Item used = held.getItem();
                    boolean flint = used == Items.FLINT_AND_STEEL;
                    world.setBlockState(pos, state.withProperty(LIT, true), 11);
                    world.playSound(null, pos,
                        flint ? SoundEvents.ITEM_FLINTANDSTEEL_USE
                            : SoundEvents.ITEM_FIRECHARGE_USE,
                        SoundCategory.BLOCKS, 1.0F,
                        flint ? world.rand.nextFloat() * 0.4F + 0.8F
                            : (world.rand.nextFloat() - world.rand.nextFloat())
                                * 0.2F + 1.0F);
                    if (!player.capabilities.isCreativeMode) {
                        if (flint) held.damageItem(1, player);
                        else held.shrink(1);
                    }
                    awardUseStat(player, used);
                }
                return true;
            }
            if (held.getItem() instanceof ItemSpade && state.getValue(LIT)) {
                if (!world.isRemote) {
                    world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH,
                        SoundCategory.BLOCKS, 1.0F, 1.0F);
                    held.damageItem(1, player);
                    douse(player, world, pos, state);
                    world.setBlockState(pos, state.withProperty(LIT, false), 11);
                }
                return true;
            }
            return false;
        }

        private static void awardUseStat(EntityPlayer player, Item item) {
            StatBase stat = StatList.getObjectUseStats(item);
            if (stat != null) {
                player.addStat(stat);
            }
        }

        private static void douse(@Nullable Entity entity, World world, BlockPos pos,
                IBlockState state) {
            if (!world.isRemote) {
                world.addBlockEvent(pos, state.getBlock(), 0,
                    isSignalFire(world, pos) ? 1 : 0);
            }
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileEntityCampfire) ((TileEntityCampfire) tile).douse();
        }

        @Override public boolean eventReceived(IBlockState state, World world,
                BlockPos pos, int id, int param) {
            if (id == 0) {
                if (world.isRemote) {
                    for (int particle = 0; particle < 20; ++particle) {
                        makeParticles(world, pos, param != 0, true);
                    }
                }
                return true;
            }
            return super.eventReceived(state, world, pos, id, param);
        }

        @Override public void onEntityCollidedWithBlock(World world, BlockPos pos,
                IBlockState state, Entity entity) {
            if (state.getValue(LIT) && entity instanceof EntityLivingBase
                    && !entity.isImmuneToFire()
                    && !hasFrostWalker((EntityLivingBase) entity)) {
                entity.attackEntityFrom(DamageSource.IN_FIRE,
                    CampfireMechanics.fireDamage(soul));
            }
            if (!world.isRemote && entity instanceof EntityArrow && entity.isBurning()
                    && CampfireMechanics.canLight(state.getValue(LIT),
                        state.getValue(WATERLOGGED))) {
                world.setBlockState(pos, state.withProperty(LIT, true), 11);
            }
        }

        private static boolean hasFrostWalker(EntityLivingBase entity) {
            return EnchantmentHelper.getEnchantmentLevel(Enchantments.FROST_WALKER,
                entity.getItemStackFromSlot(EntityEquipmentSlot.FEET)) > 0;
        }

        @SideOnly(Side.CLIENT)
        @Override public void randomDisplayTick(IBlockState state, World world,
                BlockPos pos, Random random) {
            if (!state.getValue(LIT)) return;
            if (random.nextInt(10) == 0) {
                world.playSound(pos.getX() + 0.5D, pos.getY() + 0.5D,
                    pos.getZ() + 0.5D, CampfireSoundEvents.CAMPFIRE_CRACKLE,
                    SoundCategory.BLOCKS, 0.5F + random.nextFloat(),
                    random.nextFloat() * 0.7F + 0.6F, false);
            }
            if (!soul && random.nextInt(5) == 0) {
                world.spawnParticle(LAVA_PARTICLE,
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    random.nextFloat() / 2.0F, 5.0E-5D, random.nextFloat() / 2.0F);
            }
        }

        @Override public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
            super.onBlockAdded(world, pos, state);
            scheduleWater(world, pos, state);
        }

        @Override public void neighborChanged(IBlockState state, World world, BlockPos pos,
                Block block, BlockPos fromPos) {
            scheduleWater(world, pos, state);
            super.neighborChanged(state, world, pos, block, fromPos);
        }

        private void scheduleWater(World world, BlockPos pos, IBlockState state) {
            if (!world.isRemote && state.getValue(WATERLOGGED)) {
                world.scheduleUpdate(pos, this, CampfireMechanics.WATER_TICK_DELAY);
            }
        }

        @Override public void updateTick(World world, BlockPos pos, IBlockState state,
                Random random) {
            if (!world.isRemote && state.getValue(WATERLOGGED)) {
                LushWaterlogging.emitSourceWater(world, pos);
            }
        }

        @Override public void breakBlock(World world, BlockPos pos, IBlockState state) {
            boolean waterlogged = state.getValue(WATERLOGGED);
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileEntityCampfire) {
                InventoryHelper.dropInventoryItems(world, pos, (TileEntityCampfire) tile);
            }
            super.breakBlock(world, pos, state);
            LushWaterlogging.restoreAfterRemoval(world, pos, waterlogged);
        }

        @Override public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
            IBlockState state = world.getBlockState(pos);
            super.onBlockExploded(world, pos, explosion);
            LushWaterlogging.restoreAfterRemoval(world, pos,
                state.getBlock() == this && state.getValue(WATERLOGGED));
        }

        @Override public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world,
                BlockPos pos, IBlockState state, int fortune) {
            drops.add(soul ? new ItemStack(soulSoil) : new ItemStack(Items.COAL, 2, 1));
        }

        @Override protected boolean canSilkHarvest() { return true; }
        @Override protected ItemStack getSilkTouchDrop(IBlockState state) {
            return new ItemStack(soul ? soulCampfire : campfire);
        }

        @Override public ItemStack getPickBlock(IBlockState state, RayTraceResult target,
                World world, BlockPos pos, EntityPlayer player) {
            return getSilkTouchDrop(state);
        }

        @Override public AxisAlignedBB getBoundingBox(IBlockState state,
                IBlockAccess world, BlockPos pos) { return SHAPE; }
        @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState state,
                IBlockAccess world, BlockPos pos) { return SHAPE; }
        @Override public boolean isOpaqueCube(IBlockState state) { return false; }
        @Override public boolean isFullCube(IBlockState state) { return false; }
        @Override public boolean isPassable(IBlockAccess world, BlockPos pos) { return false; }
        @SideOnly(Side.CLIENT)
        @Override public BlockRenderLayer getBlockLayer() { return BlockRenderLayer.CUTOUT; }

        @Override public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
            return state.getValue(LIT) ? (soul ? 10 : 15) : 0;
        }

        @Override public IBlockState withRotation(IBlockState state, Rotation rotation) {
            return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
        }

        @Override public IBlockState withMirror(IBlockState state, Mirror mirror) {
            return state.withRotation(mirror.toRotation(state.getValue(FACING)));
        }

        @Override public Boolean isEntityInsideMaterial(IBlockAccess world, BlockPos pos,
                IBlockState state, Entity entity, double yToTest, Material material,
                boolean testingHead) {
            return LushWaterlogging.isEntityInsideMaterial(
                state.getValue(WATERLOGGED), material);
        }

        @Override public Boolean isAABBInsideMaterial(World world, BlockPos pos,
                AxisAlignedBB bounds, Material material) {
            return LushWaterlogging.isAabbInsideMaterial(world.getBlockState(pos)
                .getValue(WATERLOGGED), material, bounds, pos);
        }

        @Override public Boolean isAABBInsideLiquid(World world, BlockPos pos,
                AxisAlignedBB bounds) {
            return LushWaterlogging.isAabbInsideLiquid(world.getBlockState(pos)
                .getValue(WATERLOGGED), bounds, pos);
        }

        @Override public float getBlockLiquidHeight(World world, BlockPos pos,
                IBlockState state, Material material) {
            return state.getValue(WATERLOGGED) && material == Material.WATER ? 1.0F : 0.0F;
        }

        @Override public IBlockState getStateAtViewpoint(IBlockState state,
                IBlockAccess world, BlockPos pos, Vec3d viewpoint) {
            return state.getValue(WATERLOGGED) && viewpoint.y < pos.getY() + 1.0D
                ? Blocks.WATER.getDefaultState() : state;
        }

        @Override public PathNodeType getAiPathNodeType(IBlockState state,
                IBlockAccess world, BlockPos pos) {
            return state.getValue(WATERLOGGED) ? LushWaterlogging.waterPathNodeType()
                : state.getValue(LIT) ? PathNodeType.DAMAGE_FIRE : PathNodeType.BLOCKED;
        }
    }

    public static final class SoulSoilBlock extends Block {
        SoulSoilBlock() {
            super(Material.GROUND, MapColor.BROWN);
            setUnlocalizedName("soul_soil");
            setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
            setHardness(0.5F);
            setResistance(CncBlockProperties.legacyResistance(0.5F));
            setSoundType(CampfireSoundEvents.SOUL_SOIL);
            setHarvestLevel("shovel", 0);
        }
    }
}

package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.block.LushWaterlogging;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/** Directional, powered lightning rods plus the hidden Java 1.12 water storage companion. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class LightningRodContent {
    public static final String PUBLIC_PATH = "lightning_rod";
    public static final String WATERLOGGED_PATH = "lightning_rod_waterlogged";
    public static final int RANGE = 128;
    public static final int ACTIVATION_TICKS = 8;

    private static LightningRodBlock publicRod;
    private static LightningRodBlock waterloggedRod;

    private LightningRodContent() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        publicRod = new LightningRodBlock(false);
        publicRod.setRegistryName(id(PUBLIC_PATH));
        waterloggedRod = new LightningRodBlock(true);
        waterloggedRod.setRegistryName(id(WATERLOGGED_PATH));
        event.getRegistry().registerAll(publicRod, waterloggedRod);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new LightningRodItem(requiredPublicRod())
                .setRegistryName(id(PUBLIC_PATH)));
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void registerModels(ModelRegistryEvent event) {
        Item item = ForgeRegistries.ITEMS.getValue(id(PUBLIC_PATH));
        if (item != null) {
            ModelLoader.setCustomModelResourceLocation(item, 0,
                    new ModelResourceLocation(id(PUBLIC_PATH), "inventory"));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void redirectAndHandleLightning(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !(event.getEntity() instanceof EntityLightningBolt)
                || event.getEntity().getEntityData().getBoolean("CncCopperHandled")) {
            return;
        }

        EntityLightningBolt bolt = (EntityLightningBolt) event.getEntity();
        World world = event.getWorld();
        BlockPos originalStrike = new BlockPos(bolt.posX, bolt.posY - 1.0E-6D, bolt.posZ);
        BlockPos rod = findClosestExposedRod(world, originalStrike);
        BlockPos strike = originalStrike;
        if (rod != null) {
            bolt.setPosition(rod.getX() + 0.5D, rod.getY() + 1.0D, rod.getZ() + 0.5D);
            strike = rod;
            IBlockState rodState = world.getBlockState(rod);
            ((LightningRodBlock) rodState.getBlock()).onLightningStrike(world, rod, rodState);
        }
        cleanCopper(world, strike);
        bolt.getEntityData().setBoolean("CncCopperHandled", true);
    }

    public static boolean isLightningRod(Block block) {
        return block instanceof LightningRodBlock;
    }

    public static int encodeMetadata(EnumFacing facing, boolean powered) {
        return facing.getIndex() | (powered ? 8 : 0);
    }

    public static EnumFacing facingFromMetadata(int meta) {
        int facingIndex = meta & 7;
        return facingIndex < EnumFacing.values().length
                ? EnumFacing.getFront(facingIndex) : EnumFacing.UP;
    }

    public static boolean poweredFromMetadata(int meta) {
        return (meta & 8) != 0;
    }

    public static void onLightningStrike(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof LightningRodBlock) {
            ((LightningRodBlock) state.getBlock()).onLightningStrike(world, pos, state);
            cleanCopper(world, pos);
        }
    }

    @Nullable
    static BlockPos findClosestExposedRod(World world, BlockPos strike) {
        BlockPos closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (int x = strike.getX() - RANGE; x <= strike.getX() + RANGE; x++) {
            for (int z = strike.getZ() - RANGE; z <= strike.getZ() + RANGE; z++) {
                int dx = x - strike.getX();
                int dz = z - strike.getZ();
                if (dx * dx + dz * dz > RANGE * RANGE) {
                    continue;
                }
                // A lightning event must never force-generate a 257x257 chunk area. Weather
                // strikes originate at the loaded surface, so probing that Y is also compatible
                // with the finite world's vertical load checks.
                if (!world.isBlockLoaded(new BlockPos(x, strike.getY(), z), false)) {
                    continue;
                }
                BlockPos top = world.getHeight(new BlockPos(x, 0, z)).down();
                if (!isLightningRod(world.getBlockState(top).getBlock())
                        || !world.canSeeSky(top.up())) {
                    continue;
                }
                double distance = top.distanceSq(strike);
                if (!isWithinAttractionRange(top, strike)) {
                    continue;
                }
                if (distance < closestDistance) {
                    closest = top;
                    closestDistance = distance;
                }
            }
        }
        return closest;
    }

    static boolean isWithinAttractionRange(BlockPos rod, BlockPos strike) {
        return rod.distanceSq(strike) <= (double) RANGE * (double) RANGE;
    }

    private static void cleanCopper(final World world, BlockPos strike) {
        IBlockState struck = world.getBlockState(strike);
        BlockPos target = strike;
        if (struck.getBlock() instanceof LightningRodBlock) {
            target = strike.offset(struck.getValue(BlockDirectional.FACING).getOpposite());
        }
        final BlockPos origin = target;
        CopperLightning.clean(new CopperLightning.Access() {
            @Override
            public String blockPathAt(int x, int y, int z) {
                ResourceLocation name = world.getBlockState(new BlockPos(x, y, z))
                        .getBlock().getRegistryName();
                return name != null && CavesNotCliffs.MODID.equals(name.getResourceDomain())
                        ? name.getResourcePath() : null;
            }

            @Override
            public void replace(int x, int y, int z, String path) {
                BlockPos pos = new BlockPos(x, y, z);
                IBlockState oldState = world.getBlockState(pos);
                Block targetBlock = CopperContent.block(path);
                if (targetBlock != null) {
                    IBlockState targetState = stateForVariant(oldState, targetBlock);
                    world.setBlockState(pos, targetState, 3);
                }
            }

            @Override
            public void emitCopperParticles(int x, int y, int z) {
                if (world instanceof WorldServer) {
                    ((WorldServer) world).spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                            x + 0.5D, y + 0.5D, z + 0.5D, 8,
                            0.35D, 0.35D, 0.35D, 0.02D,
                            Block.getStateId(world.getBlockState(new BlockPos(x, y, z))));
                }
            }
        }, origin.getX(), origin.getY(), origin.getZ(), world.rand);
    }

    private static IBlockState stateForVariant(IBlockState source, Block target) {
        CopperWeathering.Variant sourceVariant = CopperContent.variant(source.getBlock());
        CopperWeathering.Variant targetVariant = CopperContent.variant(target);
        if (sourceVariant == null || targetVariant == null) {
            return target.getDefaultState();
        }
        if (targetVariant.getStage().ordinal() < sourceVariant.getStage().ordinal()) {
            IBlockState cursor = source;
            while (CopperContent.variant(cursor.getBlock()).getStage()
                    != targetVariant.getStage()) {
                cursor = CopperContent.previous(cursor);
            }
            return cursor;
        }
        return target.getDefaultState();
    }

    private static LightningRodBlock requiredPublicRod() {
        if (publicRod == null) {
            throw new IllegalStateException("Lightning rod block was not registered");
        }
        return publicRod;
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(CavesNotCliffs.MODID, path);
    }

    public static final class LightningRodBlock extends BlockDirectional {
        public static final PropertyBool POWERED = PropertyBool.create("powered");
        private static final AxisAlignedBB Y_AXIS =
                new AxisAlignedBB(0.375D, 0.0D, 0.375D, 0.625D, 1.0D, 0.625D);
        private static final AxisAlignedBB Z_AXIS =
                new AxisAlignedBB(0.375D, 0.375D, 0.0D, 0.625D, 0.625D, 1.0D);
        private static final AxisAlignedBB X_AXIS =
                new AxisAlignedBB(0.0D, 0.375D, 0.375D, 1.0D, 0.625D, 0.625D);

        private final boolean waterlogged;

        LightningRodBlock(boolean waterlogged) {
            super(Material.IRON);
            this.waterlogged = waterlogged;
            setUnlocalizedName(waterlogged ? WATERLOGGED_PATH : PUBLIC_PATH);
            setCreativeTab(waterlogged ? null : CreativeTabs.REDSTONE);
            setSoundType(CopperSoundEvents.COPPER);
            setHardness(3.0F);
            setResistance(CncBlockProperties.legacyResistance(6.0F));
            setHarvestLevel("pickaxe", 1);
            setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.UP)
                    .withProperty(POWERED, false));
        }

        public boolean isWaterloggedStorage() {
            return waterlogged;
        }

        /** Places retained source water without applying bucket inventory bookkeeping. */
        public boolean placeSourceWater(World world, BlockPos pos, IBlockState state) {
            if (state.getBlock() != this || waterlogged) {
                return false;
            }
            LightningRodBlock target = waterloggedRod;
            if (target == null) {
                throw new IllegalStateException("Lightning rod companion was not registered");
            }
            boolean powered = state.getValue(POWERED);
            if (powered) {
                world.scheduleUpdate(pos, target, ACTIVATION_TICKS);
            }
            boolean placed = world.setBlockState(pos, target.getDefaultState()
                .withProperty(FACING, state.getValue(FACING))
                .withProperty(POWERED, powered), 3);
            if (placed) {
                LushWaterlogging.schedule(world, pos, true);
            }
            return placed;
        }

        @Override
        public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
                float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer,
                EnumHand hand) {
            return getDefaultState().withProperty(FACING, facing).withProperty(POWERED, false);
        }

        @Override
        public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
            switch (state.getValue(FACING).getAxis()) {
                case X:
                    return X_AXIS;
                case Z:
                    return Z_AXIS;
                case Y:
                default:
                    return Y_AXIS;
            }
        }

        @Override
        public boolean isFullCube(IBlockState state) {
            return false;
        }

        @Override
        public boolean isOpaqueCube(IBlockState state) {
            return false;
        }

        @Override
        public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
            return MapColor.ADOBE;
        }

        @Override
        public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                EntityPlayer player, EnumHand hand, EnumFacing side,
                float hitX, float hitY, float hitZ) {
            ItemStack held = player.getHeldItem(hand);
            boolean fill = !waterlogged && held.getItem() == Items.WATER_BUCKET;
            boolean drain = waterlogged && held.getItem() == Items.BUCKET;
            if ((!fill && !drain) || !world.isBlockModifiable(player, pos)
                    || !player.canPlayerEdit(pos, side, held)) {
                return false;
            }
            if (world.isRemote) {
                return true;
            }

            LightningRodBlock target = fill ? waterloggedRod : publicRod;
            if (target == null) {
                throw new IllegalStateException("Lightning rod companion was not registered");
            }
            boolean powered = state.getValue(POWERED);
            if (powered) {
                // The hidden 1.12 water-storage companion is a different block identity, so the
                // old block's scheduled unpower tick cannot follow the state transition.
                world.scheduleUpdate(pos, target, ACTIVATION_TICKS);
            }
            world.setBlockState(pos, target.getDefaultState()
                    .withProperty(FACING, state.getValue(FACING))
                    .withProperty(POWERED, powered), 3);
            Item original = held.getItem();
            replaceBucket(player, hand, held,
                    new ItemStack(fill ? Items.BUCKET : Items.WATER_BUCKET));
            addUseStat(player, original);
            world.playSound(null, pos,
                    fill ? SoundEvents.ITEM_BUCKET_EMPTY : SoundEvents.ITEM_BUCKET_FILL,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);
            return true;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void randomDisplayTick(IBlockState state, World world, BlockPos pos,
                Random random) {
            if (!world.isThundering()
                    || (long) world.rand.nextInt(200) > world.getTotalWorldTime() % 200L
                    || pos.getY() != world.getHeight(pos).getY() - 1) {
                return;
            }

            EnumFacing.Axis axis = state.getValue(FACING).getAxis();
            int count = world.rand.nextInt(2) + 1;
            for (int index = 0; index < count; index++) {
                boolean xAxis = axis == EnumFacing.Axis.X;
                boolean yAxis = axis == EnumFacing.Axis.Y;
                boolean zAxis = axis == EnumFacing.Axis.Z;
                double x = pos.getX() + 0.5D + signedDouble(world.rand)
                        * (xAxis ? 0.5D : 0.125D);
                double y = pos.getY() + 0.5D + signedDouble(world.rand)
                        * (yAxis ? 0.5D : 0.125D);
                double z = pos.getZ() + 0.5D + signedDouble(world.rand)
                        * (zAxis ? 0.5D : 0.125D);
                world.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, x, y, z,
                        xAxis ? signedDouble(world.rand) : 0.0D,
                        yAxis ? signedDouble(world.rand) : 0.0D,
                        zAxis ? signedDouble(world.rand) : 0.0D);
            }
        }

        @Override
        public boolean canProvidePower(IBlockState state) {
            return true;
        }

        @Override
        public int getWeakPower(IBlockState state, IBlockAccess world, BlockPos pos,
                EnumFacing side) {
            return state.getValue(POWERED) ? 15 : 0;
        }

        @Override
        public int getStrongPower(IBlockState state, IBlockAccess world, BlockPos pos,
                EnumFacing side) {
            return state.getValue(POWERED) && state.getValue(FACING) == side ? 15 : 0;
        }

        void onLightningStrike(World world, BlockPos pos, IBlockState state) {
            world.setBlockState(pos, state.withProperty(POWERED, true), 3);
            updateNeighbors(world, pos, state);
            world.scheduleUpdate(pos, this, ACTIVATION_TICKS);
            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.FIREWORKS_SPARK,
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        12, 0.25D, 0.25D, 0.25D, 0.02D);
            }
        }

        @Override
        public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
            if (state.getValue(POWERED)) {
                world.setBlockState(pos, state.withProperty(POWERED, false), 3);
                updateNeighbors(world, pos, state);
            }
        }

        @Override
        public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
            if (state.getValue(POWERED) && !world.isUpdateScheduled(pos, this)) {
                world.setBlockState(pos, state.withProperty(POWERED, false), 18);
            }
        }

        @Override
        public void breakBlock(World world, BlockPos pos, IBlockState state) {
            if (state.getValue(POWERED)) {
                updateNeighbors(world, pos, state);
            }
            super.breakBlock(world, pos, state);
            if (waterlogged && world.isAirBlock(pos)) {
                world.setBlockState(pos, Blocks.WATER.getDefaultState(), 3);
            }
        }

        @Override
        public Item getItemDropped(IBlockState state, Random random, int fortune) {
            return Item.getItemFromBlock(requiredPublicRod());
        }

        @Override
        public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
            return new ItemStack(requiredPublicRod());
        }

        private void updateNeighbors(World world, BlockPos pos, IBlockState state) {
            world.notifyNeighborsOfStateChange(pos.offset(
                    state.getValue(FACING).getOpposite()), this, false);
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
        public IBlockState getStateFromMeta(int meta) {
            return getDefaultState().withProperty(FACING, facingFromMetadata(meta))
                    .withProperty(POWERED, poweredFromMetadata(meta));
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return encodeMetadata(state.getValue(FACING), state.getValue(POWERED));
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, FACING, POWERED);
        }
    }

    private static double signedDouble(Random random) {
        return random.nextDouble() * 2.0D - 1.0D;
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

    private static final class LightningRodItem extends ItemBlock {
        LightningRodItem(Block block) {
            super(block);
        }

        @Override
        public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world,
                BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ,
                IBlockState newState) {
            IBlockState state = newState;
            if (world.getBlockState(pos).getMaterial() == Material.WATER) {
                state = waterloggedRod.getDefaultState()
                        .withProperty(BlockDirectional.FACING, newState.getValue(BlockDirectional.FACING))
                        .withProperty(LightningRodBlock.POWERED, false);
            }
            return super.placeBlockAt(stack, player, world, pos, side,
                    hitX, hitY, hitZ, state);
        }
    }
}

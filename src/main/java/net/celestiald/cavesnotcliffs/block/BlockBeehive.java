package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.content.BeeMechanics;
import net.celestiald.cavesnotcliffs.content.BeeSoundEvents;
import net.celestiald.cavesnotcliffs.content.BeehiveHarvestHooks;
import net.celestiald.cavesnotcliffs.content.BeehiveDispenserBehavior;
import net.celestiald.cavesnotcliffs.entity.EntityBee;
import net.celestiald.cavesnotcliffs.item.ItemBlockBeehive;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.celestiald.cavesnotcliffs.tile.TileEntityBeehive;
import net.celestiald.cavesnotcliffs.world.BeeSaplingNestHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/** Bee nest, crafted beehive, and their hidden full-honey visual companions. */
@ElementsCavesNotCliffs.ModElement.Tag
public final class BlockBeehive extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:bee_nest")
    public static final Block beeNest = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:beehive")
    public static final Block beehive = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:bee_nest_honey")
    public static final Block beeNestHoney = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:beehive_honey")
    public static final Block beehiveHoney = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:honeycomb")
    public static final Item honeycomb = null;

    public BlockBeehive(ElementsCavesNotCliffs elements) {
        super(elements, 340);
    }

    @Override
    public void initElements() {
        elements.blocks.add(() -> new BlockCustom(true, false)
                .setRegistryName(CncRegistryIds.BEE_NEST));
        elements.blocks.add(() -> new BlockCustom(false, false)
                .setRegistryName(CncRegistryIds.BEEHIVE));
        elements.blocks.add(() -> new BlockCustom(true, true)
                .setRegistryName(CncRegistryIds.BEE_NEST_HONEY));
        elements.blocks.add(() -> new BlockCustom(false, true)
                .setRegistryName(CncRegistryIds.BEEHIVE_HONEY));
        elements.items.add(() -> new ItemBlockBeehive(beeNest)
                .setRegistryName(CncRegistryIds.BEE_NEST));
        elements.items.add(() -> new ItemBlockBeehive(beehive)
                .setRegistryName(CncRegistryIds.BEEHIVE));
        elements.items.add(() -> new Item()
                .setRegistryName(CncRegistryIds.HONEYCOMB)
                .setUnlocalizedName("honeycomb")
                .setCreativeTab(CreativeTabs.MATERIALS));
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        GameRegistry.registerTileEntity(TileEntityBeehive.class,
                CncRegistryIds.BEEHIVE);
        net.minecraftforge.common.MinecraftForge.TERRAIN_GEN_BUS.register(
                BeeSaplingNestHandler.INSTANCE);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(
                BeeSaplingNestHandler.INSTANCE);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        net.minecraft.block.BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(
                Items.SHEARS, new BeehiveDispenserBehavior());
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        registerModel(Item.getItemFromBlock(beeNest), CncRegistryIds.BEE_NEST);
        registerModel(Item.getItemFromBlock(beehive), CncRegistryIds.BEEHIVE);
        registerModel(honeycomb, CncRegistryIds.HONEYCOMB);
    }

    @SideOnly(Side.CLIENT)
    private static void registerModel(Item item, net.minecraft.util.ResourceLocation id) {
        ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(id, "inventory"));
    }

    public static Block blockFor(boolean nest, boolean fullHoney) {
        if (nest) {
            return fullHoney ? beeNestHoney : beeNest;
        }
        return fullHoney ? beehiveHoney : beehive;
    }

    public static EnumFacing facing(IBlockState state) {
        return state.getBlock() instanceof BlockCustom
                ? state.getValue(BlockCustom.FACING) : EnumFacing.NORTH;
    }

    public static final class BlockCustom extends BlockContainer {
        public static final PropertyDirection FACING = BlockHorizontal.FACING;

        private final boolean nest;
        private final boolean fullHoneyVisual;

        BlockCustom(boolean nest, boolean fullHoneyVisual) {
            super(Material.WOOD);
            this.nest = nest;
            this.fullHoneyVisual = fullHoneyVisual;
            setUnlocalizedName(nest ? "bee_nest" : "beehive");
            setHardness(nest ? 0.3F : 0.6F);
            setSoundType(SoundType.WOOD);
            setCreativeTab(fullHoneyVisual ? null : CreativeTabs.DECORATIONS);
            setDefaultState(blockState.getBaseState().withProperty(FACING,
                    EnumFacing.NORTH));
        }

        public boolean isNest() {
            return nest;
        }

        public boolean isFullHoneyVisual() {
            return fullHoneyVisual;
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, FACING);
        }

        @Override
        public IBlockState getStateFromMeta(int meta) {
            return getDefaultState().withProperty(FACING,
                    EnumFacing.getHorizontal(meta & 3));
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return state.getValue(FACING).getHorizontalIndex();
        }

        @Override
        public IBlockState getStateForPlacement(World world, BlockPos pos,
                EnumFacing side, float hitX, float hitY, float hitZ, int meta,
                EntityLivingBase placer) {
            return getDefaultState().withProperty(FACING,
                    placer.getHorizontalFacing().getOpposite());
        }

        @Nullable
        @Override
        public TileEntity createNewTileEntity(World world, int meta) {
            return new TileEntityBeehive();
        }

        @Override
        public boolean hasComparatorInputOverride(IBlockState state) {
            return true;
        }

        @Override
        public int getComparatorInputOverride(IBlockState state, World world,
                BlockPos pos) {
            TileEntity tile = world.getTileEntity(pos);
            return tile instanceof TileEntityBeehive
                    ? ((TileEntityBeehive) tile).getHoneyLevel() : 0;
        }

        @Override
        public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                EntityPlayer player, EnumHand hand, EnumFacing side, float hitX,
                float hitY, float hitZ) {
            TileEntity tile = world.getTileEntity(pos);
            if (!(tile instanceof TileEntityBeehive)) {
                return false;
            }
            TileEntityBeehive hive = (TileEntityBeehive) tile;
            if (hive.getHoneyLevel() < BeeMechanics.MAX_HONEY_LEVEL) {
                return false;
            }
            ItemStack held = player.getHeldItem(hand);
            boolean harvested = false;
            if (held.getItem() == Items.SHEARS) {
                harvested = true;
                if (!world.isRemote) {
                    world.playSound(null, pos, BeeSoundEvents.BEEHIVE_SHEAR,
                            SoundCategory.NEUTRAL, 1.0F, 1.0F);
                    spawnAsEntity(world, pos, new ItemStack(honeycomb, 3));
                    held.damageItem(1, player);
                }
            } else if (BeehiveHarvestHooks.tryBottleHarvest(world, pos, state,
                    player, hand, held)) {
                harvested = true;
            }
            if (!harvested) {
                return false;
            }
            if (!world.isRemote) {
                if (!hive.isSedated()) {
                    hive.emptyAllLivingFromHive(player,
                            TileEntityBeehive.BeeReleaseStatus.EMERGENCY);
                    angerNearbyBees(world, pos);
                }
                hive.setHoneyLevel(0);
            }
            return true;
        }

        @Override
        public void harvestBlock(World world, EntityPlayer player, BlockPos pos,
                IBlockState state, @Nullable TileEntity tile, ItemStack tool) {
            if (world.isRemote || !(tile instanceof TileEntityBeehive)) {
                return;
            }
            TileEntityBeehive hive = (TileEntityBeehive) tile;
            boolean silk = EnchantmentHelper.getEnchantmentLevel(
                    Enchantments.SILK_TOUCH, tool) > 0;
            if (silk) {
                spawnAsEntity(world, pos, preservedStack(hive));
            } else {
                if (!nest) {
                    spawnAsEntity(world, pos, new ItemStack(beehive));
                }
            }
        }

        @Override
        public void onBlockHarvested(World world, BlockPos pos, IBlockState state,
                EntityPlayer player) {
            if (!world.isRemote) {
                TileEntity tile = world.getTileEntity(pos);
                if (tile instanceof TileEntityBeehive) {
                    TileEntityBeehive hive = (TileEntityBeehive) tile;
                    if (player.capabilities.isCreativeMode) {
                        if (!hive.isEmpty() || hive.getHoneyLevel() > 0) {
                            spawnAsEntity(world, pos, preservedStack(hive));
                        }
                    } else {
                        boolean silk = EnchantmentHelper.getEnchantmentLevel(
                                Enchantments.SILK_TOUCH,
                                player.getHeldItemMainhand()) > 0;
                        if (!silk) {
                            hive.emptyAllLivingFromHive(player,
                                    TileEntityBeehive.BeeReleaseStatus.EMERGENCY);
                            angerNearbyBees(world, pos);
                        }
                    }
                    hive.setBreakHandled(true);
                }
            }
            super.onBlockHarvested(world, pos, state, player);
        }

        private ItemStack preservedStack(TileEntityBeehive hive) {
            ItemStack stack = new ItemStack(nest ? beeNest : beehive);
            NBTTagCompound root = new NBTTagCompound();
            if (!hive.isEmpty()) {
                NBTTagCompound blockEntity = new NBTTagCompound();
                blockEntity.setTag(TileEntityBeehive.TAG_BEES, hive.writeBees());
                root.setTag("BlockEntityTag", blockEntity);
            }
            NBTTagCompound blockState = new NBTTagCompound();
            // BlockItem's 1.18 BlockStateTag codec writes serialized property strings.
            blockState.setString("honey_level",
                    Integer.toString(hive.getHoneyLevel()));
            root.setTag("BlockStateTag", blockState);
            stack.setTagCompound(root);
            return stack;
        }

        @Override
        public void breakBlock(World world, BlockPos pos, IBlockState state) {
            TileEntity tile = world.getTileEntity(pos);
            if (!world.isRemote && tile instanceof TileEntityBeehive) {
                TileEntityBeehive hive = (TileEntityBeehive) tile;
                if (hive.isVisualTransition()) {
                    return;
                }
                if (!hive.isBreakHandled()) {
                    hive.emptyAllLivingFromHive(null,
                            TileEntityBeehive.BeeReleaseStatus.EMERGENCY);
                }
                world.updateComparatorOutputLevel(pos, this);
            }
            super.breakBlock(world, pos, state);
        }

        @Override
        public void neighborChanged(IBlockState state, World world, BlockPos pos,
                Block neighbor, BlockPos fromPos) {
            if (!world.isRemote && neighbor instanceof BlockFire) {
                TileEntity tile = world.getTileEntity(pos);
                if (tile instanceof TileEntityBeehive
                        && ((TileEntityBeehive) tile).isFireNearby()) {
                    ((TileEntityBeehive) tile).emptyAllLivingFromHive(null,
                            TileEntityBeehive.BeeReleaseStatus.EMERGENCY);
                }
            }
            super.neighborChanged(state, world, pos, neighbor, fromPos);
        }

        @SideOnly(Side.CLIENT)
        @Override
        public void randomDisplayTick(IBlockState state, World world, BlockPos pos,
                Random random) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileEntityBeehive
                    && ((TileEntityBeehive) tile).getHoneyLevel()
                    >= BeeMechanics.MAX_HONEY_LEVEL && random.nextFloat() >= 0.3F) {
                world.spawnParticle(EnumParticleTypes.DRIP_WATER,
                        pos.getX() + random.nextDouble(), pos.getY() - 0.05D,
                        pos.getZ() + random.nextDouble(), 0.0D, 0.0D, 0.0D);
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
        public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world,
                BlockPos pos, IBlockState state, int fortune) {
            // Player harvesting is manual so it can preserve tile NBT. This path serves
            // explosions and other non-player destruction, where only crafted hives drop.
            if (!nest && beehive != null) {
                drops.add(new ItemStack(beehive));
            }
        }

        @Override
        public ItemStack getPickBlock(IBlockState state, RayTraceResult target,
                World world, BlockPos pos, EntityPlayer player) {
            return new ItemStack(nest ? beeNest : beehive);
        }

        @Override
        public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> items) {
            if (!fullHoneyVisual && (getCreativeTabToDisplayOn() == tab
                    || tab == CreativeTabs.SEARCH)) {
                items.add(new ItemStack(this));
            }
        }

        @Override
        public int getFlammability(IBlockAccess world, BlockPos pos,
                EnumFacing face) {
            return 20;
        }

        @Override
        public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos,
                EnumFacing face) {
            return 5;
        }

        private static void angerNearbyBees(World world, BlockPos pos) {
            AxisAlignedBB beeArea = new AxisAlignedBB(pos).grow(8.0D, 6.0D, 8.0D);
            List<EntityBee.EntityCustom> bees = world.getEntitiesWithinAABB(
                    EntityBee.EntityCustom.class, beeArea);
            List<EntityPlayer> players = world.getEntitiesWithinAABB(
                    EntityPlayer.class, beeArea);
            if (players.isEmpty()) {
                return;
            }
            for (EntityBee.EntityCustom bee : bees) {
                if (bee.getAttackTarget() == null) {
                    bee.becomeAngryAt(players.get(world.rand.nextInt(players.size())));
                }
            }
        }
    }
}

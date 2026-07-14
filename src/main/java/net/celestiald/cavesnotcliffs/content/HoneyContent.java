package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.handler.HoneyPhysicsHandler;
import net.celestiald.cavesnotcliffs.item.ItemHoneyBottle;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.celestiald.cavesnotcliffs.tile.TileEntityBeehive;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Honey bottle, honey block, and honeycomb block Java 1.18.2 backport. */
@ElementsCavesNotCliffs.ModElement.Tag
public final class HoneyContent extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:honey_block")
    public static final Block honeyBlock = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:honeycomb_block")
    public static final Block honeycombBlock = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:honey_bottle")
    public static final Item honeyBottle = null;

    private static final SoundType HONEY_SOUND = new SoundType(1.0F, 1.0F,
            HoneySoundEvents.HONEY_BREAK, HoneySoundEvents.HONEY_STEP,
            HoneySoundEvents.HONEY_PLACE, HoneySoundEvents.HONEY_HIT,
            HoneySoundEvents.HONEY_FALL);
    private static final SoundType CORAL_SOUND = new SoundType(1.0F, 1.0F,
            HoneySoundEvents.CORAL_BREAK, HoneySoundEvents.CORAL_STEP,
            HoneySoundEvents.CORAL_PLACE, HoneySoundEvents.CORAL_HIT,
            HoneySoundEvents.CORAL_FALL);

    public HoneyContent(ElementsCavesNotCliffs elements) {
        super(elements, 350);
    }

    @Override
    public void initElements() {
        HoneySoundEvents.registerAll();
        elements.blocks.add(() -> new HoneyBlockCustom()
                .setRegistryName(CncRegistryIds.HONEY_BLOCK));
        elements.blocks.add(() -> new HoneycombBlockCustom()
                .setRegistryName(CncRegistryIds.HONEYCOMB_BLOCK));
        elements.items.add(() -> new ItemBlock(honeyBlock)
                .setRegistryName(CncRegistryIds.HONEY_BLOCK));
        elements.items.add(() -> new ItemBlock(honeycombBlock)
                .setRegistryName(CncRegistryIds.HONEYCOMB_BLOCK));
        elements.items.add(() -> new ItemHoneyBottle()
                .setRegistryName(CncRegistryIds.HONEY_BOTTLE)
                .setUnlocalizedName("honey_bottle")
                .setCreativeTab(CreativeTabs.FOOD));
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(HoneyPhysicsHandler.INSTANCE);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        BeehiveHarvestHooks.registerBottleHarvester(HoneyContent::harvestBottle);
        HoneyBottleDispenserBehavior.register();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        registerModel(Item.getItemFromBlock(honeyBlock), CncRegistryIds.HONEY_BLOCK);
        registerModel(Item.getItemFromBlock(honeycombBlock),
                CncRegistryIds.HONEYCOMB_BLOCK);
        registerModel(honeyBottle, CncRegistryIds.HONEY_BOTTLE);
    }

    @SideOnly(Side.CLIENT)
    private static void registerModel(Item item, net.minecraft.util.ResourceLocation id) {
        ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(id, "inventory"));
    }

    private static boolean harvestBottle(World world, BlockPos pos, IBlockState state,
            EntityPlayer player, EnumHand hand, ItemStack stack) {
        if (stack.getItem() != Items.GLASS_BOTTLE || honeyBottle == null) {
            return false;
        }
        stack.shrink(1);
        world.playSound(player, player.posX, player.posY, player.posZ,
                SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.NEUTRAL, 1.0F, 1.0F);
        ItemStack filled = new ItemStack(honeyBottle);
        if (stack.isEmpty()) {
            player.setHeldItem(hand, filled);
        } else if (!player.inventory.addItemStackToInventory(filled)) {
            player.dropItem(filled, false);
        }
        if (!world.isRemote) {
            StatBase stat = StatList.getObjectUseStats(Items.GLASS_BOTTLE);
            if (stat != null) {
                player.addStat(stat);
            }
        }
        return true;
    }

    public static final class HoneyBlockCustom extends Block {
        private static final AxisAlignedBB COLLISION = new AxisAlignedBB(
                1.0D / 16.0D, 0.0D, 1.0D / 16.0D,
                15.0D / 16.0D, 15.0D / 16.0D, 15.0D / 16.0D);

        public HoneyBlockCustom() {
            super(Material.CLAY);
            setUnlocalizedName("honey_block");
            setCreativeTab(CreativeTabs.REDSTONE);
            setHardness(0.0F);
            setResistance(0.0F);
            setSoundType(HONEY_SOUND);
            setLightOpacity(0);
        }

        @Override
        public boolean isStickyBlock(IBlockState state) {
            return true;
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
            return BlockRenderLayer.TRANSLUCENT;
        }

        @Override
        public AxisAlignedBB getCollisionBoundingBox(IBlockState state,
                IBlockAccess world, BlockPos pos) {
            return COLLISION;
        }

        @Override
        public void onFallenUpon(World world, BlockPos pos, Entity entity,
                float fallDistance) {
            entity.playSound(HoneySoundEvents.HONEY_SLIDE, 1.0F, 1.0F);
            entity.fall(fallDistance, HoneyBlockMechanics.FALL_DAMAGE_MULTIPLIER);
            spawnSlideParticles(world, entity, 10);
        }

        @Override
        public void onEntityCollidedWithBlock(World world, BlockPos pos,
                IBlockState state, Entity entity) {
            double deltaX = pos.getX() + 0.5D - entity.posX;
            double deltaZ = pos.getZ() + 0.5D - entity.posZ;
            if (!HoneyBlockMechanics.isSliding(entity.onGround, entity.posY,
                    pos.getY(), entity.motionY, deltaX, deltaZ, entity.width)) {
                return;
            }
            HoneyBlockMechanics.Velocity velocity = HoneyBlockMechanics.slide(
                    entity.motionX, entity.motionY, entity.motionZ);
            entity.motionX = velocity.x;
            entity.motionY = velocity.y;
            entity.motionZ = velocity.z;
            entity.fallDistance = 0.0F;
            if (hasSlideEffects(entity)) {
                if (world.rand.nextInt(5) == 0) {
                    entity.playSound(HoneySoundEvents.HONEY_SLIDE, 1.0F, 1.0F);
                }
                if (world.rand.nextInt(5) == 0) {
                    spawnSlideParticles(world, entity, 5);
                }
            }
        }

        @Override
        public void onEntityWalk(World world, BlockPos pos, Entity entity) {
            entity.motionX *= HoneyBlockMechanics.SPEED_FACTOR;
            entity.motionZ *= HoneyBlockMechanics.SPEED_FACTOR;
            super.onEntityWalk(world, pos, entity);
        }

        private static boolean hasSlideEffects(Entity entity) {
            return entity instanceof EntityLivingBase
                    || entity instanceof EntityMinecart
                    || entity instanceof EntityTNTPrimed
                    || entity instanceof EntityBoat;
        }

        private static void spawnSlideParticles(World world, Entity entity, int count) {
            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                        entity.posX, entity.posY, entity.posZ, count,
                        0.0D, 0.0D, 0.0D, 0.0D,
                        Block.getStateId(honeyBlock.getDefaultState()));
            }
        }
    }

    public static final class HoneycombBlockCustom extends Block {
        public HoneycombBlockCustom() {
            super(Material.CLAY);
            setUnlocalizedName("honeycomb_block");
            setCreativeTab(CreativeTabs.DECORATIONS);
            setHardness(0.6F);
            setResistance(CncBlockProperties.legacyResistance(0.6F));
            setSoundType(CORAL_SOUND);
        }
    }
}

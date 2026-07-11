package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.handler.PowderSnowHandler;
import net.celestiald.cavesnotcliffs.item.ItemPowderSnowBucket;
import net.celestiald.cavesnotcliffs.powdersnow.PowderSnowDispenserBehavior;
import net.celestiald.cavesnotcliffs.powdersnow.PowderSnowMechanics;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.monster.EntityEndermite;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.passive.EntityRabbit;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/** Canonical Java 1.18.2 powder snow block and its solid-bucket item. */
@ElementsCavesNotCliffs.ModElement.Tag
public final class BlockPowderSnow extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:powder_snow")
    public static final Block block = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:powder_snow_bucket")
    public static final Item bucket = null;

    public static final SoundEvent BREAK_SOUND = sound("block.powder_snow.break");
    public static final SoundEvent FALL_SOUND = sound("block.powder_snow.fall");
    public static final SoundEvent HIT_SOUND = sound("block.powder_snow.hit");
    public static final SoundEvent PLACE_SOUND = sound("block.powder_snow.place");
    public static final SoundEvent STEP_SOUND = sound("block.powder_snow.step");
    public static final SoundEvent BUCKET_EMPTY_SOUND =
        sound("item.bucket.empty_powder_snow");
    public static final SoundEvent BUCKET_FILL_SOUND =
        sound("item.bucket.fill_powder_snow");

    private static final SoundType POWDER_SNOW_SOUND = new SoundType(1.0F, 1.0F,
        BREAK_SOUND, STEP_SOUND, PLACE_SOUND, HIT_SOUND, FALL_SOUND);

    public BlockPowderSnow(ElementsCavesNotCliffs elements) {
        super(elements, 320);
    }

    @Override
    public void initElements() {
        registerSound(BREAK_SOUND);
        registerSound(FALL_SOUND);
        registerSound(HIT_SOUND);
        registerSound(PLACE_SOUND);
        registerSound(STEP_SOUND);
        registerSound(BUCKET_EMPTY_SOUND);
        registerSound(BUCKET_FILL_SOUND);
        elements.blocks.add(() -> new BlockCustom().setRegistryName(CncRegistryIds.POWDER_SNOW));
        elements.items.add(() -> new ItemPowderSnowBucket()
            .setRegistryName(CncRegistryIds.POWDER_SNOW_BUCKET));
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(PowderSnowHandler.INSTANCE);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        PowderSnowDispenserBehavior.register();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(bucket, 0,
            new ModelResourceLocation(CncRegistryIds.POWDER_SNOW_BUCKET, "inventory"));
    }

    private static SoundEvent sound(String path) {
        return new SoundEvent(CncRegistryIds.id(path));
    }

    private static void registerSound(SoundEvent sound) {
        ElementsCavesNotCliffs.sounds.put(sound.getSoundName(), sound);
    }

    public static final class BlockCustom extends Block {
        private static final AxisAlignedBB FALLING_COLLISION =
            new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D,
                PowderSnowMechanics.FALLING_COLLISION_HEIGHT, 1.0D);

        public BlockCustom() {
            super(Material.CRAFTED_SNOW);
            setUnlocalizedName("powder_snow");
            setHardness(0.25F);
            setSoundType(POWDER_SNOW_SOUND);
            setLightOpacity(0);
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
        public boolean isPassable(IBlockAccess world, BlockPos pos) {
            return true;
        }

        @Nullable
        @Override
        public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world,
                BlockPos pos) {
            return NULL_AABB;
        }

        @Override
        public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
                AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes,
                @Nullable Entity entity, boolean actualState) {
            if (entity == null) {
                return;
            }
            boolean above = entity.getEntityBoundingBox().minY
                > pos.getY() + 1.0D - 1.0E-5D;
            PowderSnowMechanics.CollisionShape shape = PowderSnowMechanics.collisionShape(
                entity.fallDistance,
                entity instanceof EntityFallingBlock,
                canWalkOnPowderSnow(entity),
                above,
                entity.isSneaking());
            if (shape == PowderSnowMechanics.CollisionShape.FULL) {
                addCollisionBoxToList(pos, entityBox, collidingBoxes, FULL_BLOCK_AABB);
            } else if (shape == PowderSnowMechanics.CollisionShape.FALLING) {
                addCollisionBoxToList(pos, entityBox, collidingBoxes, FALLING_COLLISION);
            }
        }

        @Override
        public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state,
                Entity entity) {
            if (!(entity instanceof EntityLivingBase) || feetAreInPowderSnow(world, entity)) {
                entity.motionX *= PowderSnowMechanics.HORIZONTAL_STUCK_MULTIPLIER;
                entity.motionY *= PowderSnowMechanics.VERTICAL_STUCK_MULTIPLIER;
                entity.motionZ *= PowderSnowMechanics.HORIZONTAL_STUCK_MULTIPLIER;
                if (world.isRemote && (entity.prevPosX != entity.posX
                        || entity.prevPosZ != entity.posZ) && world.rand.nextBoolean()) {
                    world.spawnParticle(EnumParticleTypes.SNOW_SHOVEL,
                        entity.posX, pos.getY() + 1.0D, entity.posZ,
                        (world.rand.nextFloat() * 2.0F - 1.0F) / 12.0F,
                        0.05D,
                        (world.rand.nextFloat() * 2.0F - 1.0F) / 12.0F);
                }
            }

            if (!world.isRemote && entity.isBurning()) {
                boolean mayDestroy = entity instanceof EntityPlayer
                    ? world.isBlockModifiable((EntityPlayer) entity, pos)
                    : ForgeEventFactory.getMobGriefingEvent(world, entity);
                if (mayDestroy) {
                    world.setBlockToAir(pos);
                }
                entity.extinguish();
            }
        }

        @Override
        public void onFallenUpon(World world, BlockPos pos, Entity entity,
                float fallDistance) {
            boolean living = entity instanceof EntityLivingBase;
            if (!PowderSnowMechanics.shouldPlayFallSound(fallDistance, living)) {
                return;
            }
            // 1.12 keeps LivingEntity#getFallSound protected, so player-specific sounds are used
            // where available and the vanilla generic peer is the closest safe mob fallback.
            boolean big = PowderSnowMechanics.shouldPlayBigFallSound(fallDistance);
            SoundEvent sound = entity instanceof EntityPlayer
                ? big ? SoundEvents.ENTITY_PLAYER_BIG_FALL : SoundEvents.ENTITY_PLAYER_SMALL_FALL
                : big ? SoundEvents.ENTITY_GENERIC_BIG_FALL : SoundEvents.ENTITY_GENERIC_SMALL_FALL;
            world.playSound(null, entity.posX, entity.posY, entity.posZ,
                sound, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }

        @Override
        public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world,
                BlockPos pos, EnumFacing side) {
            return world.getBlockState(pos.offset(side)).getBlock() != this
                && super.shouldSideBeRendered(state, world, pos, side);
        }

        @Override
        public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state,
                BlockPos pos, EnumFacing side) {
            return BlockFaceShape.UNDEFINED;
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
        public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world,
                BlockPos pos, EntityPlayer player) {
            return bucket == null ? ItemStack.EMPTY : new ItemStack(bucket);
        }

        private boolean feetAreInPowderSnow(World world, Entity entity) {
            BlockPos feet = new BlockPos(entity.posX,
                entity.getEntityBoundingBox().minY + 0.01D, entity.posZ);
            return world.getBlockState(feet).getBlock() == this;
        }

        private boolean canWalkOnPowderSnow(Entity entity) {
            if (entity instanceof EntityRabbit || entity instanceof EntityEndermite
                    || entity instanceof EntitySilverfish) {
                return true;
            }
            if (entity instanceof EntityLivingBase) {
                ItemStack boots = ((EntityLivingBase) entity)
                    .getItemStackFromSlot(net.minecraft.inventory.EntityEquipmentSlot.FEET);
                return !boots.isEmpty() && boots.getItem() == Items.LEATHER_BOOTS;
            }
            return false;
        }
    }
}

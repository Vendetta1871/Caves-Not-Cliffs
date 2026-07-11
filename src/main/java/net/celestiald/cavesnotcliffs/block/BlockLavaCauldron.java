package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.powdersnow.PowderSnowMechanics;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBanner;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtils;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntityBanner;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.Random;

/**
 * A Forge-native cauldron with water/lava state. Newly placed vanilla cauldrons are swapped to
 * this block, so passive pointed-dripstone filling works without modifying BlockCauldron itself.
 */
@ElementsCavesNotCliffs.ModElement.Tag
public final class BlockLavaCauldron extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:lava_cauldron")
    public static final Block block = null;

    public BlockLavaCauldron(ElementsCavesNotCliffs instance) {
        super(instance, 201);
    }

    @Override
    public void initElements() {
        // The vanilla cauldron item places this block through LavaCauldronHandler.
        elements.blocks.add(() -> new BlockCustom().setRegistryName("lava_cauldron"));
    }

    public static final class BlockCustom extends BlockCauldron {
        public static final PropertyBool IS_LAVA = PropertyBool.create("is_lava");

        public BlockCustom() {
            setUnlocalizedName("lava_cauldron");
            setDefaultState(blockState.getBaseState()
                    .withProperty(LEVEL, 0).withProperty(IS_LAVA, false));
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, new IProperty<?>[]{LEVEL, IS_LAVA});
        }

        @Override
        public IBlockState getStateFromMeta(int meta) {
            boolean lava = meta >= 4;
            int level = MathHelper.clamp(lava ? meta - 4 : meta, 0, 3);
            return getDefaultState().withProperty(IS_LAVA, lava).withProperty(LEVEL, level);
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            int level = state.getValue(LEVEL);
            return state.getValue(IS_LAVA) ? 4 + level : level;
        }

        @Override
        public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
            scheduleIfFillable(world, pos, state);
        }

        @Override
        public int tickRate(World world) {
            return 100;
        }

        private void scheduleIfFillable(World world, BlockPos pos, IBlockState state) {
            if (!world.isRemote && state.getValue(LEVEL) < 3) {
                world.scheduleUpdate(pos, this, tickRate(world));
            }
        }

        @Override
        public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                EntityPlayer player, EnumHand hand, EnumFacing facing,
                float hitX, float hitY, float hitZ) {
            ItemStack held = player.getHeldItem(hand);
            if (held.isEmpty()) {
                return false;
            }

            int level = state.getValue(LEVEL);
            boolean lava = state.getValue(IS_LAVA);
            Item item = held.getItem();

            if (item == Items.WATER_BUCKET) {
                if (!lava && level < 3 && !world.isRemote) {
                    replaceHeldBucket(player, hand, Items.BUCKET);
                    player.addStat(StatList.CAULDRON_FILLED);
                    setWaterLevel(world, pos, state, 3);
                    play(world, pos, SoundEvents.ITEM_BUCKET_EMPTY);
                }
                return true;
            }

            if (item == Items.LAVA_BUCKET) {
                if ((lava && level < 3 || !lava && level == 0) && !world.isRemote) {
                    replaceHeldBucket(player, hand, Items.BUCKET);
                    player.addStat(StatList.CAULDRON_FILLED);
                    setLavaLevel(world, pos, state, 3);
                    play(world, pos, SoundEvents.ITEM_BUCKET_EMPTY);
                }
                return true;
            }

            if (item == Items.BUCKET) {
                if (level == 3 && !world.isRemote) {
                    Item filled = lava ? Items.LAVA_BUCKET : Items.WATER_BUCKET;
                    giveFilledContainer(player, hand, held, new ItemStack(filled));
                    player.addStat(StatList.CAULDRON_USED);
                    setWaterLevel(world, pos, state, 0);
                    play(world, pos, SoundEvents.ITEM_BUCKET_FILL);
                }
                return true;
            }

            if (item == Items.GLASS_BOTTLE) {
                if (!lava && level > 0 && !world.isRemote) {
                    ItemStack bottle = PotionUtils.addPotionToItemStack(
                            new ItemStack(Items.POTIONITEM), PotionTypes.WATER);
                    giveFilledContainer(player, hand, held, bottle);
                    player.addStat(StatList.CAULDRON_USED);
                    setWaterLevel(world, pos, state, level - 1);
                    play(world, pos, SoundEvents.ITEM_BOTTLE_FILL);
                    syncInventory(player);
                }
                return true;
            }

            if (item == Items.POTIONITEM && PotionUtils.getPotionFromItem(held) == PotionTypes.WATER) {
                if (!lava && level < 3 && !world.isRemote) {
                    if (!player.capabilities.isCreativeMode) {
                        player.setHeldItem(hand, new ItemStack(Items.GLASS_BOTTLE));
                    }
                    player.addStat(StatList.CAULDRON_USED);
                    setWaterLevel(world, pos, state, level + 1);
                    play(world, pos, SoundEvents.ITEM_BOTTLE_EMPTY);
                    syncInventory(player);
                }
                return true;
            }

            if (!lava && level > 0 && item instanceof ItemArmor) {
                ItemArmor armor = (ItemArmor) item;
                if (armor.getArmorMaterial() == ItemArmor.ArmorMaterial.LEATHER
                        && armor.hasColor(held) && !world.isRemote) {
                    armor.removeColor(held);
                    setWaterLevel(world, pos, state, level - 1);
                    player.addStat(StatList.ARMOR_CLEANED);
                    return true;
                }
            }

            if (!lava && level > 0 && item instanceof ItemBanner
                    && TileEntityBanner.getPatterns(held) > 0 && !world.isRemote) {
                ItemStack clean = held.copy();
                clean.setCount(1);
                TileEntityBanner.removeBannerData(clean);
                player.addStat(StatList.BANNER_CLEANED);
                if (!player.capabilities.isCreativeMode) {
                    held.shrink(1);
                    setWaterLevel(world, pos, state, level - 1);
                }
                if (held.isEmpty()) {
                    player.setHeldItem(hand, clean);
                } else if (!player.inventory.addItemStackToInventory(clean)) {
                    player.dropItem(clean, false);
                }
                syncInventory(player);
                return true;
            }
            return false;
        }

        private void replaceHeldBucket(EntityPlayer player, EnumHand hand, Item replacement) {
            if (!player.capabilities.isCreativeMode) {
                player.setHeldItem(hand, new ItemStack(replacement));
            }
        }

        private void giveFilledContainer(EntityPlayer player, EnumHand hand,
                ItemStack held, ItemStack filled) {
            if (player.capabilities.isCreativeMode) {
                return;
            }
            held.shrink(1);
            if (held.isEmpty()) {
                player.setHeldItem(hand, filled);
            } else if (!player.inventory.addItemStackToInventory(filled)) {
                player.dropItem(filled, false);
            }
        }

        private void syncInventory(EntityPlayer player) {
            if (player instanceof EntityPlayerMP) {
                ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
            }
        }

        private void play(World world, BlockPos pos, net.minecraft.util.SoundEvent sound) {
            world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }

        @Override
        public void setWaterLevel(World world, BlockPos pos, IBlockState state, int level) {
            int clamped = MathHelper.clamp(level, 0, 3);
            IBlockState next = state.withProperty(IS_LAVA, false).withProperty(LEVEL, clamped);
            world.setBlockState(pos, next, 2);
            world.updateComparatorOutputLevel(pos, this);
            scheduleIfFillable(world, pos, next);
        }

        private void setLavaLevel(World world, BlockPos pos, IBlockState state, int level) {
            int clamped = MathHelper.clamp(level, 0, 3);
            IBlockState next = state.withProperty(IS_LAVA, clamped > 0).withProperty(LEVEL, clamped);
            world.setBlockState(pos, next, 2);
            world.updateComparatorOutputLevel(pos, this);
            scheduleIfFillable(world, pos, next);
        }

        @Override
        public void onEntityCollidedWithBlock(World world, BlockPos pos,
                IBlockState state, Entity entity) {
            int level = state.getValue(LEVEL);
            boolean lava = state.getValue(IS_LAVA);
            float liquidTop = pos.getY() + (6.0F + 3.0F * level) / 16.0F;
            if (!world.isRemote && level > 0
                    && entity.getEntityBoundingBox().minY <= liquidTop) {
                if (lava) {
                    entity.setFire(5);
                } else if (entity.isBurning()) {
                    entity.extinguish();
                    setWaterLevel(world, pos, state, level - 1);
                }
            }
        }

        @Override
        public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
            if (world.isRemote || world.getBlockState(pos).getBlock() != this) {
                return;
            }

            int level = state.getValue(LEVEL);
            boolean lava = state.getValue(IS_LAVA);
            Material source = findDripSource(world, pos);
            if (level < 3 && source == Material.LAVA && (lava || level == 0)) {
                setLavaLevel(world, pos, state, level + 1);
                return;
            }
            if (level < 3 && source == Material.WATER && !lava) {
                setWaterLevel(world, pos, state, level + 1);
                return;
            }
            scheduleIfFillable(world, pos, state);
        }

        private Material findDripSource(World world, BlockPos cauldronPos) {
            int maxY = world.getActualHeight();
            BlockPos cursor = cauldronPos.up();
            while (cursor.getY() < maxY && world.isAirBlock(cursor)) {
                cursor = cursor.up();
            }
            if (world.getBlockState(cursor).getBlock() != BlockTopStalactite.block) {
                return null;
            }

            cursor = cursor.up();
            while (cursor.getY() < maxY
                    && world.getBlockState(cursor).getBlock() == BlockMiddleStalactite.block) {
                cursor = cursor.up();
            }
            BlockPos support;
            if (world.getBlockState(cursor).getBlock() == BlockBottomStalactite.block) {
                support = cursor.up();
            } else if (world.getBlockState(cursor).isSideSolid(world, cursor, EnumFacing.DOWN)) {
                // A single tip can hang directly from its support without base/middle segments.
                support = cursor;
            } else {
                return null;
            }
            if (!world.getBlockState(support).isSideSolid(world, support, EnumFacing.DOWN)) {
                return null;
            }
            IBlockState sourceState = world.getBlockState(support.up());
            Material sourceMaterial = sourceState.getMaterial();
            if ((sourceMaterial == Material.LAVA || sourceMaterial == Material.WATER)
                    && sourceState.getBlock() instanceof BlockLiquid
                    && sourceState.getValue(BlockLiquid.LEVEL) == 0) {
                return sourceMaterial;
            }
            return null;
        }

        @Override
        public void fillWithRain(World world, BlockPos pos) {
            IBlockState state = world.getBlockState(pos);
            int level = state.getValue(LEVEL);
            if (state.getValue(IS_LAVA) || level >= 3) {
                return;
            }
            float temperature = world.getBiome(pos).getTemperature(pos);
            float adjusted = world.getBiomeProvider()
                    .getTemperatureAtHeight(temperature, pos.getY());
            float precipitationRoll = world.rand.nextFloat();
            if (adjusted < 0.15F) {
                if (level == 0 && BlockPowderSnowCauldron.block != null
                        && PowderSnowMechanics.shouldFillFromSnow(precipitationRoll)) {
                    world.setBlockState(pos, BlockPowderSnowCauldron.block.getDefaultState()
                            .withProperty(LEVEL, 1), 2);
                    world.updateComparatorOutputLevel(pos, BlockPowderSnowCauldron.block);
                }
            } else if (precipitationRoll < 0.05F) {
                    setWaterLevel(world, pos, state, level + 1);
            }
        }

        @Override
        public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
            return state.getValue(IS_LAVA) && state.getValue(LEVEL) > 0 ? 15 : 0;
        }

        @Override
        public Item getItemDropped(IBlockState state, Random random, int fortune) {
            return Items.CAULDRON;
        }

        @Override
        public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
            return new ItemStack(Items.CAULDRON);
        }
    }
}

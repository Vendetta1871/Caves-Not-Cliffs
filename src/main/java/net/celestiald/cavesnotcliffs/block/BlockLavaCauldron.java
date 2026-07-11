package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.content.DripstoneSoundEvents;
import net.celestiald.cavesnotcliffs.dripstone.CauldronMechanics;
import net.celestiald.cavesnotcliffs.dripstone.CauldronMechanics.Content;
import net.celestiald.cavesnotcliffs.dripstone.CauldronMechanics.DripFluid;
import net.celestiald.cavesnotcliffs.dripstone.CauldronMechanics.State;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBanner;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtils;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntityBanner;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.Random;

/**
 * Hidden 1.12 storage for Java 1.18.2 empty, layered-water, and full-lava cauldrons.
 * The vanilla cauldron remains the only obtainable item.
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
        BlockCustom storage = (BlockCustom) new BlockCustom()
                .setRegistryName(CncRegistryIds.LAVA_CAULDRON);
        elements.blocks.add(() -> storage);
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
            if (meta >= 4) {
                return getDefaultState().withProperty(IS_LAVA, true)
                        .withProperty(LEVEL, CauldronMechanics.MAX_LEVEL);
            }
            return getDefaultState().withProperty(IS_LAVA, false)
                    .withProperty(LEVEL, MathHelper.clamp(meta, 0, 3));
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return state.getValue(IS_LAVA) ? 7 : state.getValue(LEVEL);
        }

        public State mechanicsState(IBlockState state) {
            if (state.getValue(IS_LAVA)) {
                return CauldronMechanics.lava();
            }
            int level = state.getValue(LEVEL);
            return level == 0 ? CauldronMechanics.empty() : CauldronMechanics.water(level);
        }

        public IBlockState blockState(State state) {
            if (state.content == Content.LAVA) {
                return getDefaultState().withProperty(IS_LAVA, true)
                        .withProperty(LEVEL, 3);
            }
            if (state.content == Content.EMPTY) {
                return getDefaultState().withProperty(IS_LAVA, false)
                        .withProperty(LEVEL, 0);
            }
            if (state.content != Content.WATER) {
                throw new IllegalArgumentException("Powder snow uses its block-only companion");
            }
            return getDefaultState().withProperty(IS_LAVA, false)
                    .withProperty(LEVEL, state.level);
        }

        @Override
        public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                EntityPlayer player, EnumHand hand, EnumFacing facing,
                float hitX, float hitY, float hitZ) {
            ItemStack held = player.getHeldItem(hand);
            if (held.isEmpty()) {
                return false;
            }

            State contents = mechanicsState(state);
            Item item = held.getItem();

            if (item == Items.WATER_BUCKET) {
                if (!world.isRemote) {
                    consumeFilledContainer(player, hand, held);
                    fill(world, pos, CauldronMechanics.water(3));
                    used(player, item, StatList.CAULDRON_FILLED);
                    play(world, pos, SoundEvents.ITEM_BUCKET_EMPTY);
                }
                return true;
            }

            if (item == Items.LAVA_BUCKET) {
                if (!world.isRemote) {
                    consumeFilledContainer(player, hand, held);
                    fill(world, pos, CauldronMechanics.lava());
                    used(player, item, StatList.CAULDRON_FILLED);
                    play(world, pos, SoundEvents.ITEM_BUCKET_EMPTY_LAVA);
                }
                return true;
            }

            if (BlockPowderSnow.bucket != null && item == BlockPowderSnow.bucket) {
                if (!world.isRemote && BlockPowderSnowCauldron.block != null) {
                    consumeFilledContainer(player, hand, held);
                    world.setBlockState(pos, BlockPowderSnowCauldron.block.getDefaultState()
                            .withProperty(LEVEL, 3), 3);
                    world.updateComparatorOutputLevel(pos, BlockPowderSnowCauldron.block);
                    used(player, item, StatList.CAULDRON_FILLED);
                    play(world, pos, BlockPowderSnow.BUCKET_EMPTY_SOUND);
                }
                return true;
            }

            if (item == Items.BUCKET) {
                if (!CauldronMechanics.canFillBucket(contents)) {
                    return false;
                }
                if (!world.isRemote) {
                    Item filled = contents.content == Content.LAVA
                            ? Items.LAVA_BUCKET : Items.WATER_BUCKET;
                    giveFilledResult(player, hand, held, new ItemStack(filled));
                    fill(world, pos, CauldronMechanics.empty());
                    used(player, item, StatList.CAULDRON_USED);
                    play(world, pos, contents.content == Content.LAVA
                            ? SoundEvents.ITEM_BUCKET_FILL_LAVA : SoundEvents.ITEM_BUCKET_FILL);
                }
                return true;
            }

            if (item == Items.GLASS_BOTTLE) {
                if (contents.content != Content.WATER) {
                    return false;
                }
                if (!world.isRemote) {
                    ItemStack waterBottle = PotionUtils.addPotionToItemStack(
                            new ItemStack(Items.POTIONITEM), PotionTypes.WATER);
                    giveFilledResult(player, hand, held, waterBottle);
                    fill(world, pos, CauldronMechanics.lowerLayer(contents));
                    used(player, item, StatList.CAULDRON_USED);
                    play(world, pos, SoundEvents.ITEM_BOTTLE_FILL);
                }
                return true;
            }

            if (item == Items.POTIONITEM
                    && PotionUtils.getPotionFromItem(held) == PotionTypes.WATER) {
                if (contents.content != Content.EMPTY && contents.content != Content.WATER
                        || contents.content == Content.WATER && contents.level == 3) {
                    return false;
                }
                if (!world.isRemote) {
                    giveFilledResult(player, hand, held, new ItemStack(Items.GLASS_BOTTLE));
                    fill(world, pos, CauldronMechanics.water(
                            contents.content == Content.EMPTY ? 1 : contents.level + 1));
                    used(player, item, StatList.CAULDRON_USED);
                    play(world, pos, SoundEvents.ITEM_BOTTLE_EMPTY);
                }
                return true;
            }

            if (contents.content == Content.WATER && item instanceof ItemArmor) {
                ItemArmor armor = (ItemArmor) item;
                if (armor.getArmorMaterial() == ItemArmor.ArmorMaterial.LEATHER
                        && armor.hasColor(held)) {
                    if (!world.isRemote) {
                        armor.removeColor(held);
                        fill(world, pos, CauldronMechanics.lowerLayer(contents));
                        player.addStat(StatList.ARMOR_CLEANED);
                    }
                    return true;
                }
            }

            if (contents.content == Content.WATER && item instanceof ItemBanner
                    && TileEntityBanner.getPatterns(held) > 0) {
                if (!world.isRemote) {
                    ItemStack clean = held.copy();
                    clean.setCount(1);
                    TileEntityBanner.removeBannerData(clean);
                    if (!player.capabilities.isCreativeMode) {
                        held.shrink(1);
                    }
                    deliverResult(player, hand, held, clean);
                    fill(world, pos, CauldronMechanics.lowerLayer(contents));
                    player.addStat(StatList.BANNER_CLEANED);
                }
                return true;
            }

            Block heldBlock = Block.getBlockFromItem(item);
            if (contents.content == Content.WATER && heldBlock instanceof BlockShulkerBox) {
                if (!world.isRemote) {
                    ItemStack clean = new ItemStack(Blocks.PURPLE_SHULKER_BOX);
                    if (held.hasTagCompound()) {
                        clean.setTagCompound(held.getTagCompound().copy());
                    }
                    player.setHeldItem(hand, clean);
                    fill(world, pos, CauldronMechanics.lowerLayer(contents));
                    if (player instanceof EntityPlayerMP) {
                        ((EntityPlayerMP) player)
                                .sendContainerToPlayer(player.inventoryContainer);
                    }
                }
                return true;
            }
            return false;
        }

        @Override
        public void onEntityCollidedWithBlock(World world, BlockPos pos,
                IBlockState state, Entity entity) {
            State contents = mechanicsState(state);
            if (world.isRemote || contents.content == Content.EMPTY
                    || !insideContents(contents, pos, entity)) {
                return;
            }
            if (contents.content == Content.LAVA) {
                entity.setFire(15);
                entity.attackEntityFrom(DamageSource.LAVA, 4.0F);
            } else if (entity.isBurning()) {
                entity.extinguish();
                if (!(entity instanceof EntityPlayer)
                        || world.isBlockModifiable((EntityPlayer) entity, pos)) {
                    fill(world, pos, CauldronMechanics.lowerLayer(contents));
                }
            }
        }

        private boolean insideContents(State contents, BlockPos pos, Entity entity) {
            return entity.posY < pos.getY() + CauldronMechanics.contentHeight(contents)
                    && entity.getEntityBoundingBox().maxY > pos.getY() + 0.25D;
        }

        @Override
        public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
            if (world.isRemote || world.getBlockState(pos).getBlock() != this) {
                return;
            }
            BlockPos tip = BlockPointedDripstone.findStalactiteTipAboveCauldron(world, pos);
            if (tip == null) {
                return;
            }
            DripFluid fluid = BlockPointedDripstone.cauldronFillFluid(world, tip);
            if (fluid != null && CauldronMechanics.canReceiveDrip(mechanicsState(state), fluid)) {
                receiveStalactiteDrip(world, pos, state, fluid);
            }
        }

        public boolean canReceiveStalactiteDrip(IBlockState state, DripFluid fluid) {
            return CauldronMechanics.canReceiveDrip(mechanicsState(state), fluid);
        }

        public void receiveStalactiteDrip(World world, BlockPos pos, IBlockState state,
                DripFluid fluid) {
            State next = CauldronMechanics.receiveDrip(mechanicsState(state), fluid);
            fill(world, pos, next);
            world.playSound(null, pos, fluid == DripFluid.LAVA
                            ? DripstoneSoundEvents.DRIP_LAVA_CAULDRON
                            : DripstoneSoundEvents.DRIP_WATER_CAULDRON,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);
        }

        @Override
        public void fillWithRain(World world, BlockPos pos) {
            IBlockState current = world.getBlockState(pos);
            if (current.getBlock() != this) {
                return;
            }
            State contents = mechanicsState(current);
            if (contents.content == Content.LAVA) {
                return;
            }
            float temperature = world.getBiome(pos).getTemperature(pos);
            boolean snow = world.getBiomeProvider()
                    .getTemperatureAtHeight(temperature, pos.getY()) < 0.15F;
            State next = CauldronMechanics.precipitation(contents, snow,
                    world.rand.nextFloat());
            if (next.equals(contents)) {
                return;
            }
            if (next.content == Content.POWDER_SNOW) {
                if (BlockPowderSnowCauldron.block != null) {
                    world.setBlockState(pos, BlockPowderSnowCauldron.block.getDefaultState()
                            .withProperty(LEVEL, next.level), 2);
                    world.updateComparatorOutputLevel(pos, BlockPowderSnowCauldron.block);
                }
            } else {
                fill(world, pos, next);
            }
        }

        private void fill(World world, BlockPos pos, State state) {
            IBlockState next = blockState(state);
            world.setBlockState(pos, next, 2);
            world.updateComparatorOutputLevel(pos, this);
        }

        @Override
        public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
            return CauldronMechanics.comparatorSignal(mechanicsState(state));
        }

        @Override
        public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
            return state.getValue(IS_LAVA) ? 15 : 0;
        }

        @Override
        public Item getItemDropped(IBlockState state, Random random, int fortune) {
            return Items.CAULDRON;
        }

        @Override
        public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
            return new ItemStack(Items.CAULDRON);
        }

        private static void consumeFilledContainer(EntityPlayer player, EnumHand hand,
                ItemStack filled) {
            giveFilledResult(player, hand, filled, new ItemStack(Items.BUCKET));
        }

        private static void giveFilledResult(EntityPlayer player, EnumHand hand,
                ItemStack original, ItemStack result) {
            if (player.capabilities.isCreativeMode) {
                if (!player.inventory.hasItemStack(result)) {
                    player.inventory.addItemStackToInventory(result);
                }
                return;
            }
            original.shrink(1);
            deliverResult(player, hand, original, result);
        }

        private static void deliverResult(EntityPlayer player, EnumHand hand,
                ItemStack original, ItemStack result) {
            if (original.isEmpty()) {
                player.setHeldItem(hand, result);
            } else if (!player.inventory.addItemStackToInventory(result)) {
                player.dropItem(result, false);
            }
            if (player instanceof EntityPlayerMP) {
                ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
            }
        }

        private static void used(EntityPlayer player, Item item, StatBase cauldronStat) {
            player.addStat(cauldronStat);
            StatBase itemUse = StatList.getObjectUseStats(item);
            if (itemUse != null) {
                player.addStat(itemUse);
            }
        }

        private static void play(World world, BlockPos pos, SoundEvent sound) {
            world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }
}

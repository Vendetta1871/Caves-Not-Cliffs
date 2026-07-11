package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.dripstone.CauldronMechanics;
import net.celestiald.cavesnotcliffs.dripstone.CauldronMechanics.Interaction;
import net.celestiald.cavesnotcliffs.dripstone.CauldronMechanics.State;
import net.celestiald.cavesnotcliffs.dripstone.CauldronStateBridge;
import net.celestiald.cavesnotcliffs.powdersnow.PowderSnowMechanics;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.Random;

/** Hidden 1-3 level state companion for powder snow stored in a vanilla cauldron item. */
@ElementsCavesNotCliffs.ModElement.Tag
public final class BlockPowderSnowCauldron extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:powder_snow_cauldron")
    public static final Block block = null;

    public BlockPowderSnowCauldron(ElementsCavesNotCliffs elements) {
        super(elements, 321);
    }

    @Override
    public void initElements() {
        // Deliberately no ItemBlock: breaking or picking always returns the vanilla cauldron item.
        elements.blocks.add(() -> new BlockCustom()
            .setRegistryName(CncRegistryIds.POWDER_SNOW_CAULDRON));
    }

    public static final class BlockCustom extends BlockCauldron {
        public BlockCustom() {
            setUnlocalizedName("powder_snow_cauldron");
            setDefaultState(blockState.getBaseState().withProperty(LEVEL, 1));
        }

        @Override
        public IBlockState getStateFromMeta(int meta) {
            return getDefaultState().withProperty(LEVEL,
                PowderSnowMechanics.requireCauldronLevel(Math.max(1, Math.min(3, meta))));
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            // BlockCauldron's inherited state container includes its level-zero sentinel.
            // Forge enumerates every valid state while assigning block-state IDs at registry
            // time, even though this hidden companion exposes only levels one through three.
            // Canonicalize that unreachable sentinel to level one instead of crashing startup.
            return Math.max(1, Math.min(3, state.getValue(LEVEL)));
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
            State contents = CauldronMechanics.powderSnow(level);
            Item item = held.getItem();

            if (item == Items.BUCKET) {
                if (!CauldronMechanics.canInteract(contents, Interaction.TAKE_BUCKET)) {
                    return false;
                }
                if (!world.isRemote) {
                    giveFilledContainer(player, hand, held,
                        new ItemStack(BlockPowderSnow.bucket));
                    used(player, item, StatList.CAULDRON_USED);
                    setContents(world, pos, CauldronMechanics.interact(
                        contents, Interaction.TAKE_BUCKET));
                    play(world, pos, BlockPowderSnow.BUCKET_FILL_SOUND);
                    syncInventory(player);
                }
                return true;
            }

            if (item == Items.WATER_BUCKET || item == Items.LAVA_BUCKET) {
                if (!world.isRemote) {
                    replaceFilledBucket(player, hand, held);
                    boolean lava = item == Items.LAVA_BUCKET;
                    used(player, item, StatList.CAULDRON_FILLED);
                    setContents(world, pos, CauldronMechanics.interact(contents,
                        lava ? Interaction.FILL_LAVA : Interaction.FILL_WATER));
                    play(world, pos, lava
                        ? SoundEvents.ITEM_BUCKET_EMPTY_LAVA : SoundEvents.ITEM_BUCKET_EMPTY);
                    syncInventory(player);
                }
                return true;
            }

            if (item == BlockPowderSnow.bucket) {
                if (!world.isRemote) {
                    replaceFilledBucket(player, hand, held);
                    used(player, item, StatList.CAULDRON_FILLED);
                    setContents(world, pos, CauldronMechanics.interact(
                        contents, Interaction.FILL_POWDER_SNOW));
                    play(world, pos, BlockPowderSnow.BUCKET_EMPTY_SOUND);
                    syncInventory(player);
                }
                return true;
            }
            return false;
        }

        @Override
        public void onEntityCollidedWithBlock(World world, BlockPos pos,
                IBlockState state, Entity entity) {
            int level = state.getValue(LEVEL);
            double contentTop = pos.getY()
                + PowderSnowMechanics.cauldronContentHeight(level);
            if (world.isRemote || !entity.isBurning()
                    || entity.posY >= contentTop
                    || entity.getEntityBoundingBox().maxY <= pos.getY() + 0.25D) {
                return;
            }
            entity.extinguish();
            if (entity instanceof EntityPlayer
                    && !world.isBlockModifiable((EntityPlayer) entity, pos)) {
                return;
            }
            // 1.18 melts one powder-snow layer into water, then consumes that water layer while
            // extinguishing: level N powder snow becomes level N-1 water.
            setContents(world, pos,
                CauldronMechanics.extinguishInPowderSnow(
                    CauldronMechanics.powderSnow(level)));
        }

        @Override
        public void fillWithRain(World world, BlockPos pos) {
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() != this) {
                return;
            }
            int level = state.getValue(LEVEL);
            float temperature = world.getBiome(pos).getTemperature(pos);
            boolean snowing = world.getBiomeProvider()
                .getTemperatureAtHeight(temperature, pos.getY()) < 0.15F;
            State contents = CauldronMechanics.powderSnow(level);
            State next = CauldronMechanics.precipitation(
                contents, snowing, world.rand.nextFloat());
            if (!next.equals(contents)) {
                setContents(world, pos, next);
            }
        }

        @Override
        public Item getItemDropped(IBlockState state, Random random, int fortune) {
            return Items.CAULDRON;
        }

        @Override
        public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
            return new ItemStack(Items.CAULDRON);
        }

        private void setContents(World world, BlockPos pos, State contents) {
            CauldronStateBridge.setContents(world, pos, contents);
        }

        private void replaceFilledBucket(EntityPlayer player, EnumHand hand,
                ItemStack filledBucket) {
            if (player.capabilities.isCreativeMode) {
                ItemStack empty = new ItemStack(Items.BUCKET);
                if (!player.inventory.hasItemStack(empty)) {
                    player.inventory.addItemStackToInventory(empty);
                }
                return;
            }
            filledBucket.shrink(1);
            ItemStack empty = new ItemStack(Items.BUCKET);
            if (filledBucket.isEmpty()) {
                player.setHeldItem(hand, empty);
            } else if (!player.inventory.addItemStackToInventory(empty)) {
                player.dropItem(empty, false);
            }
        }

        private void giveFilledContainer(EntityPlayer player, EnumHand hand,
                ItemStack emptyBuckets, ItemStack filled) {
            if (player.capabilities.isCreativeMode) {
                if (!player.inventory.hasItemStack(filled)) {
                    player.inventory.addItemStackToInventory(filled);
                }
                return;
            }
            emptyBuckets.shrink(1);
            if (emptyBuckets.isEmpty()) {
                player.setHeldItem(hand, filled);
            } else if (!player.inventory.addItemStackToInventory(filled)) {
                player.dropItem(filled, false);
            }
        }

        private void play(World world, BlockPos pos, SoundEvent sound) {
            world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
        }

        private void syncInventory(EntityPlayer player) {
            if (player instanceof EntityPlayerMP) {
                ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
            }
        }

        private void used(EntityPlayer player, Item item, StatBase cauldronStat) {
            player.addStat(cauldronStat);
            StatBase itemUse = StatList.getObjectUseStats(item);
            if (itemUse != null) {
                player.addStat(itemUse);
            }
        }
    }
}

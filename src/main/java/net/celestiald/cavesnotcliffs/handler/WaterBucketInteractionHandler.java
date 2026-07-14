package net.celestiald.cavesnotcliffs.handler;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.block.CncFluidState;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Ordinary water-bucket bridge for first-party Java 1.18.2 liquid containers. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class WaterBucketInteractionHandler {
    private WaterBucketInteractionHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getUseItem()
                == net.minecraftforge.fml.common.eventhandler.Event.Result.DENY) {
            return;
        }
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        EntityPlayer player = event.getEntityPlayer();
        EnumHand hand = event.getHand();
        ItemStack held = event.getItemStack();
        if (held.isEmpty() || hand == null) {
            return;
        }

        IBlockState state = world.getBlockState(pos);
        boolean fill = held.getItem() == Items.WATER_BUCKET
            && CncFluidState.canPlaceSourceWater(state);
        boolean drain = held.getItem() == Items.BUCKET
            && CncFluidState.canPickupSourceWater(state);
        if (!fill && !drain) {
            return;
        }

        EnumFacing face = event.getFace();
        if (face == null || !world.isBlockModifiable(player, pos)
                || !player.canPlayerEdit(pos, face, held)) {
            finish(event, EnumActionResult.FAIL);
            return;
        }

        boolean changed = fill
            ? CncFluidState.placeSourceWater(player, world, pos, state)
            : CncFluidState.pickupSourceWater(world, pos, state);
        if (!changed) {
            finish(event, EnumActionResult.FAIL);
            return;
        }
        finish(event, EnumActionResult.SUCCESS);
        if (world.isRemote) {
            return;
        }

        if (fill) {
            world.playSound(null, pos, net.minecraft.init.SoundEvents.ITEM_BUCKET_EMPTY,
                SoundCategory.BLOCKS, 1.0F, 1.0F);
            // Java 1.12 has no FLUID_PLACE game event; do not substitute another event.
            if (player instanceof EntityPlayerMP) {
                CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, pos, held);
            }
            awardUseStat(player, Items.WATER_BUCKET);
            applyFillResult(player, hand, held);
            return;
        }

        awardUseStat(player, Items.BUCKET);
        world.playSound(null, pos, net.minecraft.init.SoundEvents.ITEM_BUCKET_FILL,
            SoundCategory.BLOCKS, 1.0F, 1.0F);
        // Java 1.12 has no FLUID_PICKUP game event or FILLED_BUCKET criterion;
        // do not substitute an unrelated event or advancement.
        applyDrainResult(player, hand, held);
    }

    private static void applyFillResult(EntityPlayer player, EnumHand hand, ItemStack held) {
        if (player.capabilities.isCreativeMode) {
            return;
        }
        replaceOne(player, hand, held, new ItemStack(Items.BUCKET));
    }

    private static void applyDrainResult(EntityPlayer player, EnumHand hand, ItemStack held) {
        ItemStack waterBucket = new ItemStack(Items.WATER_BUCKET);
        if (player.capabilities.isCreativeMode) {
            if (!player.inventory.hasItemStack(waterBucket)) {
                player.inventory.addItemStackToInventory(waterBucket);
            }
            return;
        }
        replaceOne(player, hand, held, waterBucket);
    }

    private static void replaceOne(EntityPlayer player, EnumHand hand,
            ItemStack held, ItemStack replacement) {
        held.shrink(1);
        if (held.isEmpty()) {
            player.setHeldItem(hand, replacement);
        } else if (!player.inventory.addItemStackToInventory(replacement)) {
            player.dropItem(replacement, false);
        }
    }

    private static void awardUseStat(EntityPlayer player, Item item) {
        StatBase stat = StatList.getObjectUseStats(item);
        if (stat != null) {
            player.addStat(stat);
        }
    }

    private static void finish(PlayerInteractEvent.RightClickBlock event,
            EnumActionResult result) {
        event.setCancellationResult(result);
        event.setCanceled(true);
    }
}

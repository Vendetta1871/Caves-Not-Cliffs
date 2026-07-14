package net.celestiald.cavesnotcliffs.item;

import net.celestiald.cavesnotcliffs.content.AmethystSoundEvents;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

/** Functional Java 1.18.2 spyglass use contract; rendering is client-only. */
public final class ItemSpyglass extends Item {
    public static final int USE_DURATION = 1200;
    public static final float ZOOM_FOV_MODIFIER = 0.1F;

    public ItemSpyglass() {
        setUnlocalizedName("spyglass");
        setMaxStackSize(1);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return USE_DURATION;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        // 1.12 has no SPYGLASS use animation. The dedicated renderer handles the pose/overlay.
        return EnumAction.NONE;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player,
            EnumHand hand) {
        player.playSound(AmethystSoundEvents.SPYGLASS_USE, 1.0F, 1.0F);
        StatBase used = StatList.getObjectUseStats(this);
        if (used != null) {
            player.addStat(used);
        }
        player.setActiveHand(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, EntityLivingBase entity) {
        stopUsing(entity);
        return stack;
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityLivingBase entity,
            int timeLeft) {
        stopUsing(entity);
    }

    private static void stopUsing(EntityLivingBase entity) {
        entity.playSound(AmethystSoundEvents.SPYGLASS_STOP, 1.0F, 1.0F);
    }
}

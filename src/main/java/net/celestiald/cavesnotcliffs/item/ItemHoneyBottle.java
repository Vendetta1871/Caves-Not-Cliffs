package net.celestiald.cavesnotcliffs.item;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

/** Java 1.18.2 honey bottle food, poison cure, and glass-container contract. */
public final class ItemHoneyBottle extends ItemFood {
    public static final int NUTRITION = 6;
    public static final float SATURATION_MODIFIER = 0.1F;
    public static final int DRINK_DURATION = 40;
    public static final int MAX_STACK_SIZE = 16;

    public ItemHoneyBottle() {
        super(NUTRITION, SATURATION_MODIFIER, false);
        setMaxStackSize(MAX_STACK_SIZE);
        setContainerItem(Items.GLASS_BOTTLE);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return DRINK_DURATION;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.DRINK;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player,
            EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        player.setActiveHand(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world,
            EntityLivingBase entity) {
        ItemStack remaining = super.onItemUseFinish(stack, world, entity);
        if (!world.isRemote) {
            entity.removePotionEffect(MobEffects.POISON);
        }
        if (remaining.isEmpty()) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }
        if (entity instanceof EntityPlayer
                && !((EntityPlayer) entity).capabilities.isCreativeMode) {
            EntityPlayer player = (EntityPlayer) entity;
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (!player.inventory.addItemStackToInventory(bottle)) {
                player.dropItem(bottle, false);
            }
        }
        return remaining;
    }
}

package net.celestiald.cavesnotcliffs.item;

import net.celestiald.cavesnotcliffs.content.HoneySoundEvents;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
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
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            player.getFoodStats().addStats(this, stack);
            player.addStat(StatList.getObjectUseStats(this));
            world.playSound(null, player.posX, player.posY, player.posZ,
                    SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS,
                    0.5F, world.rand.nextFloat() * 0.1F + 0.9F);
            if (player instanceof EntityPlayerMP) {
                CriteriaTriggers.CONSUME_ITEM.trigger((EntityPlayerMP) player, stack);
            }
        }

        world.playSound(null, entity.posX, entity.posY, entity.posZ,
                HoneySoundEvents.HONEY_DRINK, SoundCategory.NEUTRAL, 1.0F,
                1.0F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.4F);
        if (!(entity instanceof EntityPlayer)
                || !((EntityPlayer) entity).capabilities.isCreativeMode) {
            stack.shrink(1);
        }

        if (entity instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) entity;
            CriteriaTriggers.CONSUME_ITEM.trigger(player, stack);
            player.addStat(StatList.getObjectUseStats(this));
        }
        if (!world.isRemote) {
            entity.removePotionEffect(MobEffects.POISON);
        }
        if (stack.isEmpty()) {
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
        return stack;
    }
}

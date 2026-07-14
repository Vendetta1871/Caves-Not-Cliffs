package net.celestiald.cavesnotcliffs.item;

import net.celestiald.cavesnotcliffs.block.CncFluidState;
import net.celestiald.cavesnotcliffs.content.AxolotlBucketRelease;
import net.celestiald.cavesnotcliffs.entity.EntityAxolotl;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;

/** Filled-mob bucket bridge with Java 1.18.2 axolotl NBT round-tripping. */
public final class ItemAxolotlBucket extends Item {
    public ItemAxolotlBucket() {
        setMaxStackSize(1);
        setUnlocalizedName("axolotl_bucket");
        setCreativeTab(net.minecraft.creativetab.CreativeTabs.MISC);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player,
            EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        RayTraceResult hit = rayTrace(world, player, false);
        ActionResult<ItemStack> forgeResult =
            ForgeEventFactory.onBucketUse(player, world, held, hit);
        if (forgeResult != null) {
            return forgeResult;
        }
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
            return new ActionResult<ItemStack>(EnumActionResult.PASS, held);
        }

        BlockPos clicked = hit.getBlockPos();
        EnumFacing side = hit.sideHit;
        BlockPos adjacent = clicked.offset(side);
        if (!world.isBlockModifiable(player, clicked)
                || !player.canPlayerEdit(adjacent, side, held)) {
            return new ActionResult<ItemStack>(EnumActionResult.FAIL, held);
        }

        IBlockState clickedState = world.getBlockState(clicked);
        BlockPos target = CncFluidState.isKnownWaterContainer(clickedState)
            ? clicked : adjacent;
        if (!AxolotlBucketRelease.emptyWater(player, world, target, hit)) {
            return new ActionResult<ItemStack>(EnumActionResult.FAIL, held);
        }

        if (!world.isRemote) {
            AxolotlBucketRelease.spawnAxolotl(player, world, target, held);
            if (player instanceof EntityPlayerMP) {
                CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, target, held);
            }
            StatBase use = StatList.getObjectUseStats(this);
            if (use != null) {
                player.addStat(use);
            }
        }

        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS,
                player.capabilities.isCreativeMode ? held : new ItemStack(Items.BUCKET));
    }

    public static ItemStack capture(EntityAxolotl.EntityCustom axolotl, Item bucketItem) {
        ItemStack result = new ItemStack(bucketItem);
        NBTTagCompound tag = new NBTTagCompound();
        axolotl.saveToBucket(tag);
        result.setTagCompound(tag);
        if (axolotl.hasCustomName()) {
            result.setStackDisplayName(axolotl.getCustomNameTag());
        }
        return result;
    }

    /** Java 1.18.2 ItemUtils#createFilledResult(..., false). */
    public static ItemStack createFilledResult(ItemStack empty, EntityPlayer player,
            ItemStack filled) {
        if (!player.capabilities.isCreativeMode) {
            empty.shrink(1);
        }
        if (empty.isEmpty()) {
            return filled;
        }
        if (!player.inventory.addItemStackToInventory(filled)) {
            player.dropItem(filled, false);
        }
        return empty;
    }
}

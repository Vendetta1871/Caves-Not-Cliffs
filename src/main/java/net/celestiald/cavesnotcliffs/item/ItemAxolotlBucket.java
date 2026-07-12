package net.celestiald.cavesnotcliffs.item;

import net.celestiald.cavesnotcliffs.content.AxolotlSoundEvents;
import net.celestiald.cavesnotcliffs.entity.EntityAxolotl;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

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
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
            return new ActionResult<ItemStack>(EnumActionResult.PASS, held);
        }

        BlockPos clicked = hit.getBlockPos();
        EnumFacing side = hit.sideHit;
        if (!world.isBlockModifiable(player, clicked)
                || !player.canPlayerEdit(clicked.offset(side), side, held)) {
            return new ActionResult<ItemStack>(EnumActionResult.FAIL, held);
        }

        BlockPos spawnPos = clicked;
        if (world.getBlockState(clicked).getMaterial() != Material.WATER) {
            spawnPos = clicked.offset(side);
            if (!world.isAirBlock(spawnPos)
                    && !world.getBlockState(spawnPos).getBlock()
                        .isReplaceable(world, spawnPos)) {
                return new ActionResult<ItemStack>(EnumActionResult.FAIL, held);
            }
        }

        if (!world.isRemote) {
            if (world.getBlockState(spawnPos).getMaterial() != Material.WATER) {
                world.setBlockState(spawnPos, Blocks.FLOWING_WATER.getDefaultState(), 11);
            }
            EntityAxolotl.EntityCustom axolotl = new EntityAxolotl.EntityCustom(world);
            axolotl.setLocationAndAngles(spawnPos.getX() + 0.5D,
                    spawnPos.getY() + 0.5D, spawnPos.getZ() + 0.5D,
                    player.rotationYaw, 0.0F);
            axolotl.loadFromBucket(held);
            world.spawnEntity(axolotl);
            world.playSound(null, spawnPos, AxolotlSoundEvents.BUCKET_EMPTY,
                    SoundCategory.NEUTRAL, 1.0F, 1.0F);
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
}

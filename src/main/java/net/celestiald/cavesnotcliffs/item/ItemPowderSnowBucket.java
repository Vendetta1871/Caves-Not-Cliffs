package net.celestiald.cavesnotcliffs.item;

import net.celestiald.cavesnotcliffs.block.BlockPowderSnow;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Java 1.18.2 solid-bucket placement semantics without exposing an ordinary ItemBlock. */
public final class ItemPowderSnowBucket extends Item {
    public ItemPowderSnowBucket() {
        setUnlocalizedName("powder_snow_bucket");
        setCreativeTab(CreativeTabs.MISC);
        setMaxStackSize(1);
        setContainerItem(Items.BUCKET);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos clickedPos,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (BlockPowderSnow.block == null) {
            return EnumActionResult.FAIL;
        }
        ItemStack held = player.getHeldItem(hand);
        IBlockState clicked = world.getBlockState(clickedPos);
        boolean replaceClicked = clicked.getBlock().isReplaceable(world, clickedPos);
        BlockPos placePos = replaceClicked ? clickedPos : clickedPos.offset(facing);
        if (!player.canPlayerEdit(placePos, facing, held)
                || !world.mayPlace(BlockPowderSnow.block, placePos, false, facing, player)) {
            return EnumActionResult.FAIL;
        }

        if (!world.isRemote) {
            IBlockState placed = BlockPowderSnow.block.getDefaultState();
            if (!world.setBlockState(placePos, placed, 3)) {
                return EnumActionResult.FAIL;
            }
            BlockPowderSnow.block.onBlockPlacedBy(world, placePos, placed, player, held);
            if (!player.capabilities.isCreativeMode) {
                player.setHeldItem(hand, new ItemStack(Items.BUCKET));
            }
            player.addStat(StatList.getObjectUseStats(this));
        }
        world.playSound(player, placePos, BlockPowderSnow.BUCKET_EMPTY_SOUND,
            SoundCategory.BLOCKS, 1.0F, 1.0F);
        return EnumActionResult.SUCCESS;
    }

    @Override
    public boolean hasContainerItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getContainerItem(ItemStack stack) {
        return new ItemStack(Items.BUCKET);
    }
}

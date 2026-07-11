package net.celestiald.cavesnotcliffs.item;

import net.celestiald.cavesnotcliffs.content.CopperContent;
import net.celestiald.cavesnotcliffs.content.HoneyWaxingEffects;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Canonical honeycomb item with Java 1.18.2 copper waxing behavior. */
public final class ItemHoneycomb extends Item {
    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        IBlockState waxed = CopperContent.waxed(world.getBlockState(pos));
        if (waxed == null) {
            return EnumActionResult.PASS;
        }
        stack.shrink(1);
        world.setBlockState(pos, waxed, 11);
        HoneyWaxingEffects.play(world, pos);
        return EnumActionResult.SUCCESS;
    }
}

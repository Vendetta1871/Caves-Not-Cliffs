package net.celestiald.cavesnotcliffs.item;

import net.celestiald.cavesnotcliffs.block.BlockCandle;
import net.celestiald.cavesnotcliffs.content.CandleMechanics;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Adds same-color, non-sneaking 1..4 stacking before normal ItemBlock placement. */
public final class ItemBlockCandle extends ItemBlock {
    public ItemBlockCandle(BlockCandle block) {
        super(block);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() == block
                && CandleMechanics.canStack(true, player.isSneaking(),
                        state.getValue(BlockCandle.CANDLES))) {
            if (!player.canPlayerEdit(pos, facing, held)) {
                return EnumActionResult.FAIL;
            }
            if (!world.isRemote) {
                world.setBlockState(pos, state.withProperty(BlockCandle.CANDLES,
                        CandleMechanics.stackedCount(
                                state.getValue(BlockCandle.CANDLES))), 3);
                world.playSound(null, pos, block.getSoundType(state, world, pos, player)
                                .getPlaceSound(), SoundCategory.BLOCKS,
                        (block.getSoundType().getVolume() + 1.0F) / 2.0F,
                        block.getSoundType().getPitch() * 0.8F);
                held.shrink(1);
            }
            return EnumActionResult.SUCCESS;
        }
        return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
    }
}

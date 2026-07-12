package net.celestiald.cavesnotcliffs.item;

import net.celestiald.cavesnotcliffs.block.BlockCandle;
import net.celestiald.cavesnotcliffs.content.CandleMechanics;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
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
            if (held.isEmpty() || !player.canPlayerEdit(pos, facing, held)) {
                return EnumActionResult.FAIL;
            }

            IBlockState placed = state.withProperty(BlockCandle.CANDLES,
                    CandleMechanics.stackedCount(state.getValue(BlockCandle.CANDLES)));
            AxisAlignedBB collision = placed.getCollisionBoundingBox(world, pos);
            if (!block.canPlaceBlockAt(world, pos)
                    || (collision != null
                    && !world.checkNoEntityCollision(collision.offset(pos)))) {
                return EnumActionResult.FAIL;
            }
            if (!world.setBlockState(pos, placed, 11)) {
                return EnumActionResult.FAIL;
            }

            IBlockState actual = world.getBlockState(pos);
            if (actual.getBlock() == placed.getBlock()) {
                actual.getBlock().onBlockPlacedBy(world, pos, actual, player, held);
                if (player instanceof EntityPlayerMP) {
                    CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player,
                            pos, held);
                }
            }
            SoundType sound = actual.getBlock().getSoundType(
                    actual, world, pos, player);
            world.playSound(player, pos, sound.getPlaceSound(), SoundCategory.BLOCKS,
                    (sound.getVolume() + 1.0F) / 2.0F,
                    sound.getPitch() * 0.8F);
            if (!player.capabilities.isCreativeMode) {
                held.shrink(1);
            }
            return EnumActionResult.SUCCESS;
        }
        return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
    }
}

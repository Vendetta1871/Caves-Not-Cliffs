package net.celestiald.cavesnotcliffs.item;

import net.celestiald.cavesnotcliffs.tile.TileEntityBeehive;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Applies the 1.18 BlockStateTag honey level after 1.12's ItemBlock placement. */
public final class ItemBlockBeehive extends ItemBlock {
    public ItemBlockBeehive(Block block) {
        super(block);
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world,
            BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ,
            IBlockState newState) {
        if (!super.placeBlockAt(stack, player, world, pos, side,
                hitX, hitY, hitZ, newState)) {
            return false;
        }
        TileEntity tile = world.getTileEntity(pos);
        NBTTagCompound root = stack.getTagCompound();
        if (tile instanceof TileEntityBeehive && root != null
                && root.hasKey("BlockStateTag", 10)) {
            NBTTagCompound state = root.getCompoundTag("BlockStateTag");
            int level = state.getInteger("honey_level");
            ((TileEntityBeehive) tile).setHoneyLevel(Math.max(0, Math.min(5, level)));
        }
        return true;
    }
}

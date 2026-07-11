package net.celestiald.cavesnotcliffs.tile;

import net.celestiald.cavesnotcliffs.block.BlockComposter;
import net.celestiald.cavesnotcliffs.content.ComposterCompostables;
import net.celestiald.cavesnotcliffs.content.ComposterMechanics;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

/** Stateless inventory facade that exposes the 1.18 composter contract to 1.12 hoppers. */
public final class TileEntityComposter extends TileEntity implements ISidedInventory {
    private static final int[] SLOT = {0};
    private static final int[] NO_SLOTS = new int[0];

    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return level() != ComposterMechanics.READY_LEVEL;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return index == 0 && level() == ComposterMechanics.READY_LEVEL
            ? new ItemStack(Items.DYE, 1, 15) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        return index == 0 && count > 0 ? takeReadyBoneMeal() : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        return index == 0 ? takeReadyBoneMeal() : ItemStack.EMPTY;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index != 0 || stack.isEmpty() || world == null || world.isRemote) {
            return;
        }
        IBlockState state = state();
        // TileEntityHopper restores the source slot with its pre-extraction copy when the
        // destination rejects an item. Reconstitute the ready level so a failed pull cannot
        // delete the bone meal or empty the composter.
        if (isBoneMeal(stack) && state.getBlock() instanceof BlockComposter.BlockCustom
                && state.getValue(BlockComposter.BlockCustom.LEVEL) == 0) {
            world.setBlockState(pos, state.withProperty(BlockComposter.BlockCustom.LEVEL,
                ComposterMechanics.READY_LEVEL), 3);
            return;
        }
        float chance = ComposterCompostables.chance(stack);
        if (state.getBlock() instanceof BlockComposter.BlockCustom
                && ComposterMechanics.acceptsInput(state.getValue(BlockComposter.BlockCustom.LEVEL),
                    chance)) {
            boolean success = BlockComposter.BlockCustom.addItem(world, pos, state, chance);
            BlockComposter.BlockCustom.playFillFeedback(world, pos, success);
        }
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return world != null && world.getTileEntity(pos) == this
            && player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void openInventory(EntityPlayer player) {
    }

    @Override
    public void closeInventory(EntityPlayer player) {
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return index == 0 && level() < ComposterMechanics.MAX_FILL_LEVEL
            && ComposterCompostables.contains(stack);
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        int level = level();
        if (side == EnumFacing.UP && level < ComposterMechanics.MAX_FILL_LEVEL) {
            return SLOT;
        }
        if (side == EnumFacing.DOWN && level == ComposterMechanics.READY_LEVEL) {
            return SLOT;
        }
        return NO_SLOTS;
    }

    @Override
    public boolean canInsertItem(int index, ItemStack stack, EnumFacing direction) {
        return direction == EnumFacing.UP && isItemValidForSlot(index, stack);
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
        return index == 0 && direction == EnumFacing.DOWN
            && level() == ComposterMechanics.READY_LEVEL
            && isBoneMeal(stack);
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        takeReadyBoneMeal();
    }

    @Override
    public String getName() {
        return "container.cavesnotcliffs.composter";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentTranslation(getName());
    }

    private ItemStack takeReadyBoneMeal() {
        if (world == null || world.isRemote) {
            return ItemStack.EMPTY;
        }
        IBlockState state = state();
        if (!(state.getBlock() instanceof BlockComposter.BlockCustom)) {
            return ItemStack.EMPTY;
        }
        return BlockComposter.BlockCustom.extractProduce(world, pos, state, false);
    }

    private int level() {
        IBlockState state = state();
        return state.getBlock() instanceof BlockComposter.BlockCustom
            ? state.getValue(BlockComposter.BlockCustom.LEVEL) : 0;
    }

    private IBlockState state() {
        return world == null ? net.minecraft.init.Blocks.AIR.getDefaultState()
            : world.getBlockState(pos);
    }

    private static boolean isBoneMeal(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.DYE
            && stack.getMetadata() == 15;
    }
}

package net.celestiald.cavesnotcliffs.tile;

import net.celestiald.cavesnotcliffs.content.CampfireCooking;
import net.celestiald.cavesnotcliffs.content.CampfireMechanics;
import net.celestiald.cavesnotcliffs.content.CampfireContent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nullable;

/** Four-slot Java 1.18.2 campfire cooking ticker and save contract. */
public final class TileEntityCampfire extends TileEntity implements ITickable, IInventory {
    private final NonNullList<ItemStack> items = NonNullList.withSize(
        CampfireMechanics.SLOT_COUNT, ItemStack.EMPTY);
    private final int[] cookingProgress = new int[CampfireMechanics.SLOT_COUNT];
    private final int[] cookingTime = new int[CampfireMechanics.SLOT_COUNT];

    @Override
    public void update() {
        if (world == null) {
            return;
        }
        boolean lit = CampfireContent.isLitCampfire(world.getBlockState(pos));
        if (world.isRemote) {
            if (lit) {
                CampfireContent.clientParticleTick(world, pos,
                    CampfireContent.isSignalFire(world, pos), items);
            }
            return;
        }
        boolean changed = lit ? cookTick() : cooldownTick();
        if (changed) {
            markDirty();
        }
    }

    private boolean cookTick() {
        boolean active = false;
        for (int slot = 0; slot < items.size(); ++slot) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            active = true;
            ++cookingProgress[slot];
            if (cookingProgress[slot] < cookingTime[slot]) {
                continue;
            }
            CampfireCooking.Recipe recipe = CampfireCooking.find(stack);
            ItemStack output = recipe == null ? stack.copy() : recipe.output();
            InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), output);
            items.set(slot, ItemStack.EMPTY);
            cookingProgress[slot] = 0;
            cookingTime[slot] = 0;
            world.notifyBlockUpdate(pos, world.getBlockState(pos),
                world.getBlockState(pos), 3);
        }
        return active;
    }

    private boolean cooldownTick() {
        boolean changed = false;
        for (int slot = 0; slot < cookingProgress.length; ++slot) {
            int cooled = CampfireMechanics.coolProgress(cookingProgress[slot],
                cookingTime[slot]);
            if (cooled != cookingProgress[slot]) {
                cookingProgress[slot] = cooled;
                changed = true;
            }
        }
        return changed;
    }

    @Nullable
    public CampfireCooking.Recipe getCookableRecipe(ItemStack stack) {
        return firstEmptySlot() < 0 ? null : CampfireCooking.find(stack);
    }

    public boolean placeFood(ItemStack stack, CampfireCooking.Recipe recipe) {
        if (stack == null || stack.isEmpty() || recipe == null || !recipe.matches(stack)) {
            return false;
        }
        int slot = firstEmptySlot();
        if (slot < 0) {
            return false;
        }
        cookingTime[slot] = recipe.cookingTime();
        cookingProgress[slot] = 0;
        items.set(slot, stack.splitStack(1));
        markUpdated();
        return true;
    }

    private int firstEmptySlot() {
        for (int slot = 0; slot < items.size(); ++slot) {
            if (items.get(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    public int cookingProgress(int slot) {
        return cookingProgress[slot];
    }

    public int cookingTime(int slot) {
        return cookingTime[slot];
    }

    public void douse() {
        if (world != null) {
            markUpdated();
        }
    }

    private void markUpdated() {
        markDirty();
        if (world != null) {
            world.notifyBlockUpdate(pos, world.getBlockState(pos),
                world.getBlockState(pos), 3);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        ItemStackHelper.saveAllItems(compound, items, true);
        compound.setIntArray("CookingTimes", cookingProgress);
        compound.setIntArray("CookingTotalTimes", cookingTime);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        items.clear();
        ItemStackHelper.loadAllItems(compound, items);
        copyArray(compound.getIntArray("CookingTimes"), cookingProgress);
        copyArray(compound.getIntArray("CookingTotalTimes"), cookingTime);
    }

    private static void copyArray(int[] source, int[] destination) {
        java.util.Arrays.fill(destination, 0);
        System.arraycopy(source, 0, destination, 0,
            Math.min(source.length, destination.length));
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound compound = new NBTTagCompound();
        ItemStackHelper.saveAllItems(compound, items, true);
        return compound;
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager networkManager, SPacketUpdateTileEntity packet) {
        items.clear();
        ItemStackHelper.loadAllItems(packet.getNbtCompound(), items);
    }

    @Override public int getSizeInventory() { return items.size(); }
    @Override public boolean isEmpty() {
        for (ItemStack stack : items) if (!stack.isEmpty()) return false;
        return true;
    }
    @Override public ItemStack getStackInSlot(int index) { return items.get(index); }
    @Override public ItemStack decrStackSize(int index, int count) {
        ItemStack removed = ItemStackHelper.getAndSplit(items, index, count);
        if (!removed.isEmpty()) markUpdated();
        return removed;
    }
    @Override public ItemStack removeStackFromSlot(int index) {
        ItemStack removed = ItemStackHelper.getAndRemove(items, index);
        if (!removed.isEmpty()) markUpdated();
        return removed;
    }
    @Override public void setInventorySlotContents(int index, ItemStack stack) {
        items.set(index, stack);
        if (!stack.isEmpty() && stack.getCount() > 1) stack.setCount(1);
        markUpdated();
    }
    @Override public int getInventoryStackLimit() { return 1; }
    @Override public boolean isUsableByPlayer(EntityPlayer player) {
        return world != null && world.getTileEntity(pos) == this
            && player.getDistanceSq(pos) <= 64.0D;
    }
    @Override public void openInventory(EntityPlayer player) { }
    @Override public void closeInventory(EntityPlayer player) { }
    @Override public boolean isItemValidForSlot(int index, ItemStack stack) { return false; }
    @Override public int getField(int id) { return 0; }
    @Override public void setField(int id, int value) { }
    @Override public int getFieldCount() { return 0; }
    @Override public void clear() {
        items.clear();
        java.util.Arrays.fill(cookingProgress, 0);
        java.util.Arrays.fill(cookingTime, 0);
        markUpdated();
    }
    @Override public String getName() { return "container.cavesnotcliffs.campfire"; }
    @Override public boolean hasCustomName() { return false; }
    @Override public ITextComponent getDisplayName() {
        return new TextComponentTranslation(getName());
    }
}

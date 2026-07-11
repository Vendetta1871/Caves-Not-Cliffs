package net.celestiald.cavesnotcliffs.stonecutter;

import net.celestiald.cavesnotcliffs.block.BlockStonecutter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.List;

/** Server-authoritative Java 1.18.2 stonecutter menu adapted to 1.12 slots. */
public final class ContainerStonecutter extends Container {
    private final World world;
    private final BlockPos pos;
    private final InventoryBasic inputInventory = new InventoryBasic("", false, 1);
    private final InventoryBasic resultInventory = new InventoryBasic("", false, 1);
    private final Slot inputSlot;
    private final Slot resultSlot;
    private List<StonecutterRecipeCatalog.ResolvedRecipe> recipes = Collections.emptyList();
    private ItemStack inputSnapshot = ItemStack.EMPTY;
    private int selectedRecipe = -1;
    private int lastSelectedRecipe = -1;
    private long lastSoundTime;
    private Runnable updateListener = () -> { };

    public ContainerStonecutter(InventoryPlayer playerInventory, World world, BlockPos pos) {
        this.world = world;
        this.pos = pos.toImmutable();
        inputInventory.addInventoryChangeListener(this::onInputChanged);

        inputSlot = addSlotToContainer(new Slot(inputInventory, 0, 20, 33));
        resultSlot = addSlotToContainer(new Slot(resultInventory, 0, 143, 33) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false;
            }

            @Override
            public ItemStack onTake(EntityPlayer player, ItemStack stack) {
                stack.onCrafting(player.world, player, stack.getCount());
                ItemStack removed = inputSlot.decrStackSize(
                        StonecutterMenuLogic.INPUT_CONSUMED_PER_TAKE);
                if (!removed.isEmpty()) {
                    setupResultSlot();
                }
                playTakeResultSound();
                return super.onTake(player, stack);
            }
        });

        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                addSlotToContainer(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; ++column) {
            addSlotToContainer(new Slot(playerInventory, column,
                    8 + column * 18, 142));
        }
    }

    public int getSelectedRecipeIndex() {
        return selectedRecipe;
    }

    public List<StonecutterRecipeCatalog.ResolvedRecipe> getRecipes() {
        return recipes;
    }

    public int getNumRecipes() {
        return recipes.size();
    }

    public boolean hasInputItem() {
        return inputSlot.getHasStack() && !recipes.isEmpty();
    }

    public void registerUpdateListener(Runnable listener) {
        updateListener = listener == null ? () -> { } : listener;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return world.getBlockState(pos).getBlock() == BlockStonecutter.block
                && player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D,
                        pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public boolean enchantItem(EntityPlayer player, int id) {
        if (StonecutterMenuLogic.isValidRecipeIndex(id, recipes.size())) {
            selectedRecipe = StonecutterMenuLogic.selectionAfterClick(
                    selectedRecipe, id, recipes.size());
            setupResultSlot();
        }
        return true;
    }

    private void onInputChanged(net.minecraft.inventory.IInventory ignored) {
        ItemStack current = inputSlot == null ? ItemStack.EMPTY : inputSlot.getStack();
        if (!ItemStack.areItemsEqual(current, inputSnapshot)) {
            inputSnapshot = current.copy();
            setupRecipeList(current);
        }
        updateListener.run();
    }

    private void setupRecipeList(ItemStack input) {
        recipes = Collections.emptyList();
        selectedRecipe = -1;
        resultInventory.setInventorySlotContents(0, ItemStack.EMPTY);
        if (!input.isEmpty()) {
            recipes = StonecutterRecipeCatalog.recipesFor(input);
        }
    }

    private void setupResultSlot() {
        if (StonecutterMenuLogic.isValidRecipeIndex(selectedRecipe, recipes.size())) {
            resultInventory.setInventorySlotContents(0,
                    recipes.get(selectedRecipe).result());
        } else {
            resultInventory.setInventorySlotContents(0, ItemStack.EMPTY);
        }
        detectAndSendChanges();
    }

    private void playTakeResultSound() {
        if (world.isRemote) {
            return;
        }
        long tick = world.getTotalWorldTime();
        if (StonecutterMenuLogic.shouldPlayTakeSound(lastSoundTime, tick)) {
            world.playSound(null, pos, BlockStonecutter.TAKE_RESULT_SOUND,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);
            lastSoundTime = tick;
        }
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        listener.sendWindowProperty(this, 0, selectedRecipe);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (lastSelectedRecipe != selectedRecipe) {
            for (IContainerListener listener : listeners) {
                listener.sendWindowProperty(this, 0, selectedRecipe);
            }
            lastSelectedRecipe = selectedRecipe;
        }
    }

    @Override
    public void updateProgressBar(int id, int data) {
        if (id == 0) {
            selectedRecipe = data;
            setupResultSlot();
        }
    }

    @Override
    public boolean canMergeSlot(ItemStack stack, Slot slot) {
        return slot != resultSlot && super.canMergeSlot(stack, slot);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (index < 0 || index >= inventorySlots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        boolean hasRecipe = StonecutterRecipeCatalog.hasRecipe(stack);
        StonecutterMenuLogic.QuickMoveRoute route =
                StonecutterMenuLogic.quickMoveRoute(index, hasRecipe);
        switch (route) {
            case RESULT_TO_PLAYER:
                // StonecutterMenu.quickMoveStack calls the item's crafted hook
                // before moving the result, but leaves crafted-stat accounting
                // to the result slot's onTake callback after the move.
                stack.getItem().onCreated(stack, player.world, player);
                if (!mergeItemStack(stack, StonecutterMenuLogic.PLAYER_MAIN_START,
                        StonecutterMenuLogic.HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onSlotChange(stack, original);
                break;
            case INPUT_TO_PLAYER:
                if (!mergeItemStack(stack, StonecutterMenuLogic.PLAYER_MAIN_START,
                        StonecutterMenuLogic.HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
                break;
            case PLAYER_TO_INPUT:
                if (!mergeItemStack(stack, StonecutterMenuLogic.INPUT_SLOT,
                        StonecutterMenuLogic.RESULT_SLOT, false)) {
                    return ItemStack.EMPTY;
                }
                break;
            case MAIN_TO_HOTBAR:
                if (!mergeItemStack(stack, StonecutterMenuLogic.HOTBAR_START,
                        StonecutterMenuLogic.HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
                break;
            case HOTBAR_TO_MAIN:
                if (!mergeItemStack(stack, StonecutterMenuLogic.PLAYER_MAIN_START,
                        StonecutterMenuLogic.PLAYER_MAIN_END, false)) {
                    return ItemStack.EMPTY;
                }
                break;
            default:
                return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        detectAndSendChanges();
        return original;
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        resultInventory.removeStackFromSlot(0);
        if (!world.isRemote) {
            clearContainer(player, world, inputInventory);
        }
    }
}

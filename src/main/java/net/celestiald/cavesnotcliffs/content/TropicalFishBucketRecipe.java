package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistryEntry;

/**
 * Narrow 1.12 clownfish bridge for the 1.18 tropical-fish bucket.
 *
 * <p>A normal shapeless recipe would invoke the water bucket's container remainder and duplicate
 * an empty bucket even though the water is retained by the result. This explicit recipe consumes
 * both inputs and returns no crafting remainder.</p>
 */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class TropicalFishBucketRecipe extends IForgeRegistryEntry.Impl<IRecipe>
        implements IRecipe {
    private final Item fixedOutput;

    public TropicalFishBucketRecipe() {
        this(null);
    }

    TropicalFishBucketRecipe(Item fixedOutput) {
        this.fixedOutput = fixedOutput;
        setRegistryName(CncRegistryIds.id("tropical_fish_bucket_from_clownfish"));
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<IRecipe> event) {
        event.getRegistry().register(new TropicalFishBucketRecipe());
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        boolean waterBucket = false;
        boolean clownfish = false;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (!waterBucket && stack.getItem() == Items.WATER_BUCKET) {
                waterBucket = true;
            } else if (!clownfish && stack.getItem() == Items.FISH
                    && stack.getMetadata() == 2) {
                clownfish = true;
            } else {
                return false;
            }
        }
        return waterBucket && clownfish && output() != null;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventory) {
        Item item = output();
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        Item item = output();
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inventory) {
        return NonNullList.withSize(inventory.getSizeInventory(), ItemStack.EMPTY);
    }

    private Item output() {
        return fixedOutput != null ? fixedOutput
                : ForgeRegistries.ITEMS.getValue(CncRegistryIds.TROPICAL_FISH_BUCKET);
    }
}

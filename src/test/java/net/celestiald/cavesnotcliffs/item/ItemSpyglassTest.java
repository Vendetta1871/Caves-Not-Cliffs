package net.celestiald.cavesnotcliffs.item;

import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ItemSpyglassTest {
    @BeforeClass
    public static void bootstrapMinecraftRegistries() {
        Bootstrap.register();
    }

    @Test
    public void exposesTheOfficialUseDurationAndSingleItemStackLimit() {
        ItemSpyglass spyglass = new ItemSpyglass();
        ItemStack stack = new ItemStack(spyglass);
        assertEquals(1200, spyglass.getMaxItemUseDuration(stack));
        assertEquals(1, spyglass.getItemStackLimit(stack));
        assertEquals(EnumAction.NONE, spyglass.getItemUseAction(stack));
        assertEquals(0.1F, ItemSpyglass.ZOOM_FOV_MODIFIER, 0.0F);
    }
}

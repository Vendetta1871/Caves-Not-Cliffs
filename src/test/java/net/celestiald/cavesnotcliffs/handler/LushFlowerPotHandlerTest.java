package net.celestiald.cavesnotcliffs.handler;

import net.minecraft.item.Item;
import net.minecraft.init.Bootstrap;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityFlowerPot;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LushFlowerPotHandlerTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void acceptsOnlyAnEmptyVanillaFlowerPotTile() {
        assertTrue(LushFlowerPotHandler.isEmptyFlowerPot(new TileEntityFlowerPot()));
        assertFalse(LushFlowerPotHandler.isEmptyFlowerPot(
                new TileEntityFlowerPot(new Item(), 0)));
    }

    @Test
    public void failsClosedWhenFlowerPotTileDataIsMissingOrWrong() {
        assertFalse(LushFlowerPotHandler.isEmptyFlowerPot(null));
        assertFalse(LushFlowerPotHandler.isEmptyFlowerPot(new TileEntityChest()));
    }
}

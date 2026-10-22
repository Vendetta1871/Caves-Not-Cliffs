package net.celestiald.cavesnotcliffs.client;

import net.celestiald.cavesnotcliffs.compat.NoiseThreaderConflict;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraftforge.fml.client.CustomModLoadingErrorDisplayException;

/**
 * The startup screen FML paints when {@link NoiseThreaderConflict} refuses a pack. FML keeps
 * the screen up and stops the game; the player's only way past it is removing NoiseThreader.
 */
public class NoiseThreaderErrorScreen extends CustomModLoadingErrorDisplayException {
    public NoiseThreaderErrorScreen() {
        super(NoiseThreaderConflict.TITLE, null);
    }

    @Override
    public void initGui(GuiErrorScreen errorScreen, FontRenderer fontRenderer) {
        // The whole refusal is static text; nothing to set up.
    }

    @Override
    public void drawScreen(GuiErrorScreen errorScreen, FontRenderer fontRenderer,
            int mouseRelX, int mouseRelY, float tickTime) {
        int center = errorScreen.width / 2;
        drawCentered(fontRenderer, NoiseThreaderConflict.TITLE, center, 30, 0xFF5555);
        int line = 60;
        for (String text : NoiseThreaderConflict.messageLines()) {
            drawCentered(fontRenderer, text, center, line, 0xFFFFFF);
            line += 12;
        }
    }

    private static void drawCentered(FontRenderer fontRenderer, String text, int center,
            int y, int color) {
        fontRenderer.drawString(text, center - fontRenderer.getStringWidth(text) / 2, y, color);
    }
}

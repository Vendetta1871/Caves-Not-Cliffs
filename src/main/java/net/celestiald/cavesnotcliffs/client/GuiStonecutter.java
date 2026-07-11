package net.celestiald.cavesnotcliffs.client;

import net.celestiald.cavesnotcliffs.block.BlockStonecutter;
import net.celestiald.cavesnotcliffs.stonecutter.ContainerStonecutter;
import net.celestiald.cavesnotcliffs.stonecutter.StonecutterMenuLogic;
import net.celestiald.cavesnotcliffs.stonecutter.StonecutterRecipeCatalog;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;

/** Java 1.12 rendering adapter for the Java 1.18.2 stonecutter screen. */
public final class GuiStonecutter extends GuiContainer {
    private static final ResourceLocation BACKGROUND = new ResourceLocation(
            "cavesnotcliffs", "textures/gui/container/stonecutter.png");
    private static final int RECIPE_X = 52;
    private static final int RECIPE_Y = 14;
    private static final int RECIPE_WIDTH = 16;
    private static final int RECIPE_HEIGHT = 18;
    private static final int SCROLLER_X = 119;
    private static final int SCROLLER_Y = 15;
    private static final int SCROLLER_WIDTH = 12;
    private static final int SCROLLER_HEIGHT = 15;
    private static final int SCROLL_DISTANCE = 41;

    private final ContainerStonecutter container;
    private final InventoryPlayer playerInventory;
    private float scrollOffset;
    private boolean scrolling;
    private int startIndex;
    private boolean displayRecipes;

    private GuiStonecutter(ContainerStonecutter container,
            InventoryPlayer playerInventory) {
        super(container);
        this.container = container;
        this.playerInventory = playerInventory;
        xSize = 176;
        ySize = 166;
        container.registerUpdateListener(this::containerChanged);
    }

    /** Reflection target used by the common handler without client class linkage. */
    public static Object create(EntityPlayer player, World world, BlockPos pos) {
        return new GuiStonecutter(
                new ContainerStonecutter(player.inventory, world, pos), player.inventory);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (!displayRecipes) {
            return;
        }
        int recipesX = guiLeft + RECIPE_X;
        int recipesY = guiTop + RECIPE_Y;
        int limit = Math.min(startIndex + StonecutterMenuLogic.VISIBLE_RECIPES,
                container.getNumRecipes());
        List<StonecutterRecipeCatalog.ResolvedRecipe> recipes = container.getRecipes();
        for (int index = startIndex; index < limit; ++index) {
            int relative = index - startIndex;
            int x = recipesX + relative % StonecutterMenuLogic.VISIBLE_COLUMNS
                    * RECIPE_WIDTH;
            int y = recipesY + relative / StonecutterMenuLogic.VISIBLE_COLUMNS
                    * RECIPE_HEIGHT + 2;
            if (mouseX >= x && mouseX < x + RECIPE_WIDTH
                    && mouseY >= y && mouseY < y + RECIPE_HEIGHT) {
                renderToolTip(recipes.get(index).result(), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("container.stonecutter"), 8, 5, 4210752);
        fontRenderer.drawString(playerInventory.getDisplayName().getUnformattedText(),
                8, 72, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX,
            int mouseY) {
        mc.getTextureManager().bindTexture(BACKGROUND);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);

        int scrollPixels = (int) (SCROLL_DISTANCE * scrollOffset);
        drawTexturedModalRect(guiLeft + SCROLLER_X,
                guiTop + SCROLLER_Y + scrollPixels,
                176 + (canScroll() ? 0 : SCROLLER_WIDTH), 0,
                SCROLLER_WIDTH, SCROLLER_HEIGHT);

        if (!displayRecipes) {
            return;
        }
        int recipesX = guiLeft + RECIPE_X;
        int recipesY = guiTop + RECIPE_Y;
        int limit = Math.min(startIndex + StonecutterMenuLogic.VISIBLE_RECIPES,
                container.getNumRecipes());
        drawRecipeButtons(mouseX, mouseY, recipesX, recipesY, limit);
        drawRecipeItems(recipesX, recipesY, limit);
    }

    private void drawRecipeButtons(int mouseX, int mouseY, int recipesX,
            int recipesY, int limit) {
        for (int index = startIndex; index < limit; ++index) {
            int relative = index - startIndex;
            int x = recipesX + relative % StonecutterMenuLogic.VISIBLE_COLUMNS
                    * RECIPE_WIDTH;
            int y = recipesY + relative / StonecutterMenuLogic.VISIBLE_COLUMNS
                    * RECIPE_HEIGHT + 2;
            int textureY = ySize;
            if (index == container.getSelectedRecipeIndex()) {
                textureY += RECIPE_HEIGHT;
            } else if (mouseX >= x && mouseY >= y
                    && mouseX < x + RECIPE_WIDTH && mouseY < y + RECIPE_HEIGHT) {
                textureY += RECIPE_HEIGHT * 2;
            }
            drawTexturedModalRect(x, y - 1, 0, textureY,
                    RECIPE_WIDTH, RECIPE_HEIGHT);
        }
    }

    private void drawRecipeItems(int recipesX, int recipesY, int limit) {
        List<StonecutterRecipeCatalog.ResolvedRecipe> recipes = container.getRecipes();
        for (int index = startIndex; index < limit; ++index) {
            int relative = index - startIndex;
            int x = recipesX + relative % StonecutterMenuLogic.VISIBLE_COLUMNS
                    * RECIPE_WIDTH;
            int y = recipesY + relative / StonecutterMenuLogic.VISIBLE_COLUMNS
                    * RECIPE_HEIGHT + 2;
            ItemStack result = recipes.get(index).result();
            itemRender.renderItemAndEffectIntoGUI(result, x, y);
            itemRender.renderItemOverlayIntoGUI(fontRenderer, result, x, y, null);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
            throws IOException {
        scrolling = false;
        if (displayRecipes) {
            int recipesX = guiLeft + RECIPE_X;
            int recipesY = guiTop + RECIPE_Y;
            int limit = Math.min(startIndex + StonecutterMenuLogic.VISIBLE_RECIPES,
                    container.getNumRecipes());
            for (int index = startIndex; index < limit; ++index) {
                int relative = index - startIndex;
                double x = mouseX - (recipesX
                        + relative % StonecutterMenuLogic.VISIBLE_COLUMNS * RECIPE_WIDTH);
                double y = mouseY - (recipesY
                        + relative / StonecutterMenuLogic.VISIBLE_COLUMNS * RECIPE_HEIGHT);
                if (x >= 0.0D && y >= 0.0D && x < RECIPE_WIDTH
                        && y < RECIPE_HEIGHT
                        && container.enchantItem(mc.player, index)) {
                    mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(
                            BlockStonecutter.SELECT_RECIPE_SOUND, 1.0F));
                    mc.playerController.sendEnchantPacket(container.windowId, index);
                    return;
                }
            }
            int scrollX = guiLeft + SCROLLER_X;
            int scrollY = guiTop + 9;
            if (mouseX >= scrollX && mouseX < scrollX + SCROLLER_WIDTH
                    && mouseY >= scrollY && mouseY < scrollY + 54) {
                scrolling = true;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton,
            long timeSinceLastClick) {
        if (scrolling && canScroll()) {
            scrollOffset = StonecutterMenuLogic.scrollDrag(mouseY, guiTop + RECIPE_Y);
            startIndex = StonecutterMenuLogic.startIndex(scrollOffset,
                    container.getNumRecipes());
            return;
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        scrolling = false;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && canScroll()) {
            int direction = wheel > 0 ? 1 : -1;
            scrollOffset = StonecutterMenuLogic.scrollWheel(scrollOffset, direction,
                    container.getNumRecipes());
            startIndex = StonecutterMenuLogic.startIndex(scrollOffset,
                    container.getNumRecipes());
        }
    }

    private boolean canScroll() {
        return displayRecipes
                && container.getNumRecipes() > StonecutterMenuLogic.VISIBLE_RECIPES;
    }

    private void containerChanged() {
        displayRecipes = container.hasInputItem();
        if (!displayRecipes) {
            scrollOffset = 0.0F;
            startIndex = 0;
        }
    }
}

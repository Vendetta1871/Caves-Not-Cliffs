package net.celestiald.cavesnotcliffs.stonecutter;

/** Pure menu math shared by the container, client screen, and exhaustive tests. */
public final class StonecutterMenuLogic {
    public static final int VISIBLE_COLUMNS = 4;
    public static final int VISIBLE_ROWS = 3;
    public static final int VISIBLE_RECIPES = VISIBLE_COLUMNS * VISIBLE_ROWS;
    public static final int INPUT_SLOT = 0;
    public static final int RESULT_SLOT = 1;
    public static final int PLAYER_MAIN_START = 2;
    public static final int PLAYER_MAIN_END = 29;
    public static final int HOTBAR_START = 29;
    public static final int HOTBAR_END = 38;
    public static final int INPUT_CONSUMED_PER_TAKE = 1;

    public enum QuickMoveRoute {
        RESULT_TO_PLAYER,
        INPUT_TO_PLAYER,
        PLAYER_TO_INPUT,
        MAIN_TO_HOTBAR,
        HOTBAR_TO_MAIN,
        INVALID
    }

    private StonecutterMenuLogic() {
    }

    public static boolean isValidRecipeIndex(int index, int recipeCount) {
        return index >= 0 && index < recipeCount;
    }

    public static int selectionAfterClick(int current, int clicked, int recipeCount) {
        return isValidRecipeIndex(clicked, recipeCount) ? clicked : current;
    }

    public static int recipeRows(int recipeCount) {
        if (recipeCount < 0) {
            throw new IllegalArgumentException("Recipe count cannot be negative");
        }
        return (recipeCount + VISIBLE_COLUMNS - 1) / VISIBLE_COLUMNS;
    }

    public static int offscreenRows(int recipeCount) {
        return Math.max(0, recipeRows(recipeCount) - VISIBLE_ROWS);
    }

    public static int startIndex(float scrollOffset, int recipeCount) {
        float clamped = clamp(scrollOffset);
        return (int) (clamped * offscreenRows(recipeCount) + 0.5F) * VISIBLE_COLUMNS;
    }

    public static float scrollWheel(float current, int wheelDirection, int recipeCount) {
        int rows = offscreenRows(recipeCount);
        if (rows == 0 || wheelDirection == 0) {
            return clamp(current);
        }
        return clamp(current - (float) wheelDirection / (float) rows);
    }

    public static float scrollDrag(float mouseY, float recipesTop) {
        float recipesBottom = recipesTop + 54.0F;
        return clamp((mouseY - recipesTop - 7.5F)
                / (recipesBottom - recipesTop - 15.0F));
    }

    public static boolean shouldPlayTakeSound(long lastSoundTick, long currentTick) {
        return lastSoundTick != currentTick;
    }

    public static QuickMoveRoute quickMoveRoute(int slotIndex, boolean hasRecipe) {
        if (slotIndex == RESULT_SLOT) {
            return QuickMoveRoute.RESULT_TO_PLAYER;
        }
        if (slotIndex == INPUT_SLOT) {
            return QuickMoveRoute.INPUT_TO_PLAYER;
        }
        if (slotIndex >= PLAYER_MAIN_START && slotIndex < PLAYER_MAIN_END) {
            return hasRecipe ? QuickMoveRoute.PLAYER_TO_INPUT
                    : QuickMoveRoute.MAIN_TO_HOTBAR;
        }
        if (slotIndex >= HOTBAR_START && slotIndex < HOTBAR_END) {
            return hasRecipe ? QuickMoveRoute.PLAYER_TO_INPUT
                    : QuickMoveRoute.HOTBAR_TO_MAIN;
        }
        return QuickMoveRoute.INVALID;
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}

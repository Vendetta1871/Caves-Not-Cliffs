package net.celestiald.cavesnotcliffs.stonecutter;

import org.junit.Test;

import static net.celestiald.cavesnotcliffs.stonecutter.StonecutterMenuLogic.QuickMoveRoute.HOTBAR_TO_MAIN;
import static net.celestiald.cavesnotcliffs.stonecutter.StonecutterMenuLogic.QuickMoveRoute.INPUT_TO_PLAYER;
import static net.celestiald.cavesnotcliffs.stonecutter.StonecutterMenuLogic.QuickMoveRoute.INVALID;
import static net.celestiald.cavesnotcliffs.stonecutter.StonecutterMenuLogic.QuickMoveRoute.MAIN_TO_HOTBAR;
import static net.celestiald.cavesnotcliffs.stonecutter.StonecutterMenuLogic.QuickMoveRoute.PLAYER_TO_INPUT;
import static net.celestiald.cavesnotcliffs.stonecutter.StonecutterMenuLogic.QuickMoveRoute.RESULT_TO_PLAYER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StonecutterMenuLogicTest {
    @Test
    public void everySelectionBoundaryMatchesThe1182Menu() {
        for (int count = 0; count <= 128; ++count) {
            for (int clicked = -2; clicked <= count + 2; ++clicked) {
                boolean valid = clicked >= 0 && clicked < count;
                assertEquals(valid,
                        StonecutterMenuLogic.isValidRecipeIndex(clicked, count));
                assertEquals(valid ? clicked : 7,
                        StonecutterMenuLogic.selectionAfterClick(7, clicked, count));
            }
        }
        assertEquals(1, StonecutterMenuLogic.INPUT_CONSUMED_PER_TAKE);
    }

    @Test
    public void viewportRowsScrollWheelAndDragMatchOfficialConstants() {
        for (int count = 0; count <= 128; ++count) {
            int rows = (count + 3) / 4;
            int offscreen = Math.max(0, rows - 3);
            assertEquals(rows, StonecutterMenuLogic.recipeRows(count));
            assertEquals(offscreen, StonecutterMenuLogic.offscreenRows(count));
            assertEquals(0, StonecutterMenuLogic.startIndex(-1.0F, count));
            assertEquals(offscreen * 4,
                    StonecutterMenuLogic.startIndex(2.0F, count));
        }

        assertEquals(0.0F, StonecutterMenuLogic.scrollDrag(6.5F, 14.0F), 0.0F);
        assertEquals(0.5F, StonecutterMenuLogic.scrollDrag(41.0F, 14.0F), 0.0F);
        assertEquals(1.0F, StonecutterMenuLogic.scrollDrag(75.5F, 14.0F), 0.0F);
        assertEquals(0.75F, StonecutterMenuLogic.scrollWheel(1.0F, 1, 25), 0.0F);
        assertEquals(1.0F, StonecutterMenuLogic.scrollWheel(0.75F, -1, 25), 0.0F);
        assertEquals(0.5F, StonecutterMenuLogic.scrollWheel(0.5F, 1, 12), 0.0F);
    }

    @Test
    public void everyContainerSlotUsesTheFaithfulQuickMoveRoute() {
        for (int index = -3; index <= 41; ++index) {
            if (index == 0) {
                assertEquals(INPUT_TO_PLAYER,
                        StonecutterMenuLogic.quickMoveRoute(index, false));
                assertEquals(INPUT_TO_PLAYER,
                        StonecutterMenuLogic.quickMoveRoute(index, true));
            } else if (index == 1) {
                assertEquals(RESULT_TO_PLAYER,
                        StonecutterMenuLogic.quickMoveRoute(index, false));
                assertEquals(RESULT_TO_PLAYER,
                        StonecutterMenuLogic.quickMoveRoute(index, true));
            } else if (index >= 2 && index < 29) {
                assertEquals(MAIN_TO_HOTBAR,
                        StonecutterMenuLogic.quickMoveRoute(index, false));
                assertEquals(PLAYER_TO_INPUT,
                        StonecutterMenuLogic.quickMoveRoute(index, true));
            } else if (index >= 29 && index < 38) {
                assertEquals(HOTBAR_TO_MAIN,
                        StonecutterMenuLogic.quickMoveRoute(index, false));
                assertEquals(PLAYER_TO_INPUT,
                        StonecutterMenuLogic.quickMoveRoute(index, true));
            } else {
                assertEquals(INVALID,
                        StonecutterMenuLogic.quickMoveRoute(index, false));
                assertEquals(INVALID,
                        StonecutterMenuLogic.quickMoveRoute(index, true));
            }
        }
    }

    @Test
    public void resultSoundPlaysAtMostOncePerWorldTick() {
        assertFalse(StonecutterMenuLogic.shouldPlayTakeSound(0L, 0L));
        assertFalse(StonecutterMenuLogic.shouldPlayTakeSound(Long.MIN_VALUE,
                Long.MIN_VALUE));
        assertTrue(StonecutterMenuLogic.shouldPlayTakeSound(0L, 1L));
        assertTrue(StonecutterMenuLogic.shouldPlayTakeSound(Long.MAX_VALUE,
                Long.MIN_VALUE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeRecipeCountsAreRejected() {
        StonecutterMenuLogic.recipeRows(-1);
    }
}

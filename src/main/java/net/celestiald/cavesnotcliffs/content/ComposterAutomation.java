package net.celestiald.cavesnotcliffs.content;

import net.minecraft.util.EnumFacing;

/** Pure sided-inventory policy used by the 1.12 hopper bridge. */
public final class ComposterAutomation {
    private ComposterAutomation() {
    }

    public static SlotMode slots(int level, EnumFacing face) {
        ComposterMechanics.requireLevel(level);
        if (face == EnumFacing.UP && level < ComposterMechanics.MAX_FILL_LEVEL) {
            return SlotMode.INPUT;
        }
        if (face == EnumFacing.DOWN && level == ComposterMechanics.READY_LEVEL) {
            return SlotMode.OUTPUT;
        }
        return SlotMode.NONE;
    }

    public static boolean canInsert(int level, float chance, EnumFacing face) {
        return slots(level, face) == SlotMode.INPUT
            && ComposterMechanics.acceptsInput(level, chance);
    }

    public static boolean canExtract(int level, boolean boneMeal, EnumFacing face) {
        return boneMeal && slots(level, face) == SlotMode.OUTPUT;
    }

    /** A failed 1.12 hopper transfer restores its source stack through this same inventory slot. */
    public static boolean restoresReadyOutput(int level, boolean boneMeal) {
        ComposterMechanics.requireLevel(level);
        return level == ComposterMechanics.EMPTY_LEVEL && boneMeal;
    }

    public enum SlotMode {
        NONE,
        INPUT,
        OUTPUT
    }
}

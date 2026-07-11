package net.celestiald.cavesnotcliffs.content;

import net.minecraft.util.EnumFacing;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ComposterAutomationTest {
    @Test
    public void exhaustivelyMatchesAllNineLevelsAndSixFaces() {
        for (int level = 0; level <= 8; ++level) {
            for (EnumFacing face : EnumFacing.values()) {
                ComposterAutomation.SlotMode expected =
                    face == EnumFacing.UP && level < 7
                        ? ComposterAutomation.SlotMode.INPUT
                        : face == EnumFacing.DOWN && level == 8
                            ? ComposterAutomation.SlotMode.OUTPUT
                            : ComposterAutomation.SlotMode.NONE;
                assertEquals("level=" + level + " face=" + face, expected,
                    ComposterAutomation.slots(level, face));
                assertEquals(expected == ComposterAutomation.SlotMode.INPUT,
                    ComposterAutomation.canInsert(level, 0.30F, face));
                assertEquals(expected == ComposterAutomation.SlotMode.OUTPUT,
                    ComposterAutomation.canExtract(level, true, face));
                assertFalse(ComposterAutomation.canExtract(level, false, face));
            }
        }
    }

    @Test
    public void failedHopperPullCanRestoreOnlyReadyBoneMeal() {
        assertTrue(ComposterAutomation.restoresReadyOutput(0, true));
        assertFalse(ComposterAutomation.restoresReadyOutput(0, false));
        for (int level = 1; level <= 8; ++level) {
            assertFalse(ComposterAutomation.restoresReadyOutput(level, true));
        }
    }
}

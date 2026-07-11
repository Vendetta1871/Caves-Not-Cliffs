package net.celestiald.cavesnotcliffs.content;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BeeHiveRoutingTest {
    @Test
    public void burningSelectedHiveIsDroppedWithoutALocateCooldown() {
        assertTrue(BeeHiveRouting.shouldAbandonSelectedHive(true, true));
        assertFalse(BeeHiveRouting.shouldAbandonSelectedHive(true, false));
        assertFalse(BeeHiveRouting.shouldAbandonSelectedHive(false, true));
        assertEquals(0, BeeHiveRouting.RETRY_COOLDOWN_AFTER_FIRE);
    }

    @Test
    public void locateCandidatesExcludeFullAndBurningHives() {
        assertTrue(BeeHiveRouting.canSelectHive(false, false));
        assertFalse(BeeHiveRouting.canSelectHive(true, false));
        assertFalse(BeeHiveRouting.canSelectHive(false, true));
        assertFalse(BeeHiveRouting.canSelectHive(true, true));
    }
}

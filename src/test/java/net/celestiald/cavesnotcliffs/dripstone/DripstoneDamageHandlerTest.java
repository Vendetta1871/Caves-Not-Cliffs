package net.celestiald.cavesnotcliffs.dripstone;

import net.celestiald.cavesnotcliffs.handler.DripstoneDamageHandler;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DripstoneDamageHandlerTest {
    @Test
    public void dedicatedSourcesMatchThe1182ArmorContracts() {
        assertEquals("fallingStalactite",
                DripstoneDamageHandler.FALLING_STALACTITE.getDamageType());
        assertFalse(DripstoneDamageHandler.FALLING_STALACTITE.isUnblockable());
        assertEquals("stalagmite", DripstoneDamageHandler.STALAGMITE.getDamageType());
        assertTrue(DripstoneDamageHandler.STALAGMITE.isUnblockable());
    }
}

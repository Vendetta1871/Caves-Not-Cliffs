package net.celestiald.cavesnotcliffs.dripstone;

import net.celestiald.cavesnotcliffs.entity.EntityFallingPointedDripstone;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FallingPointedDripstoneEntityTest {
    @Test
    public void internalEntityHasStableNamespacedIdentity() {
        assertEquals("cavesnotcliffs:falling_pointed_dripstone",
                EntityFallingPointedDripstone.CncEntity.ID.toString());
    }
}

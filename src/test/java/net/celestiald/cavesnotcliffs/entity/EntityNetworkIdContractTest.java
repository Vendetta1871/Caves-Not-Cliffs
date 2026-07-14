package net.celestiald.cavesnotcliffs.entity;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

/** Prevents Forge's per-mod numeric entity IDs from silently aliasing spawn packets. */
public class EntityNetworkIdContractTest {
    @Test
    public void everyCavesNotCliffsEntityHasAUniqueNumericId() {
        assertEquals(0, EntityAxolotl.NETWORK_ID);
        assertEquals(1, EntityFallingPointedDripstone.NETWORK_ID);
        assertEquals(2, EntityBee.NETWORK_ID);
        assertEquals(3, new HashSet<Integer>(Arrays.asList(
                EntityAxolotl.NETWORK_ID,
                EntityFallingPointedDripstone.NETWORK_ID,
                EntityBee.NETWORK_ID)).size());
    }
}

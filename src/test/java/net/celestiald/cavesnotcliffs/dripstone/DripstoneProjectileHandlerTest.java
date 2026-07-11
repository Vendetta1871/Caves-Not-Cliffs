package net.celestiald.cavesnotcliffs.dripstone;

import net.celestiald.cavesnotcliffs.handler.DripstoneProjectileHandler;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DripstoneProjectileHandlerTest {
    @Test
    public void recognizesVanillaAndBridgeTridentIdentitiesOnly() {
        assertTrue(DripstoneProjectileHandler.isTridentIdentity(
                "trident", "ThrownTrident"));
        assertTrue(DripstoneProjectileHandler.isTridentIdentity(
                "future_mod:thrown_trident", "EntityArrow"));
        assertTrue(DripstoneProjectileHandler.isTridentIdentity(
                null, "EntityTrident"));
        assertFalse(DripstoneProjectileHandler.isTridentIdentity(
                "arrow", "EntityTippedArrow"));
    }
}

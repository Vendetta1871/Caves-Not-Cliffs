package net.celestiald.cavesnotcliffs.world;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class WorldTypeNamingTest {
    @Test
    public void moddedNamesAreStableAndFitTheVanillaLimit() {
        String name = WorldTypeNaming.moddedWrapperName(
                "skylands", "com.example.SkylandsWorldType");
        assertEquals("cnc_m_5f54a84653", name);
        assertTrue(name.length() <= WorldTypeNaming.MAX_WORLD_TYPE_NAME_LENGTH);
        assertEquals(name, WorldTypeNaming.moddedWrapperName(
                "skylands", "com.example.SkylandsWorldType"));
    }

    @Test
    public void classNameParticipatesInTheStableIdentity() {
        assertNotEquals(
                WorldTypeNaming.moddedWrapperName("skylands", "com.example.First"),
                WorldTypeNaming.moddedWrapperName("skylands", "com.example.Second"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyBaseNamesAreRejected() {
        WorldTypeNaming.moddedWrapperName("", "com.example.WorldType");
    }
}

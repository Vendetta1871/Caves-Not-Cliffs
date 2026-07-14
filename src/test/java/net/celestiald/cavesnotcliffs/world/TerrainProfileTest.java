package net.celestiald.cavesnotcliffs.world;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TerrainProfileTest {
    @Test
    public void nativeVanillaProfilesAreMappedExplicitly() {
        assertEquals(TerrainProfile.DEFAULT, TerrainProfile.forVanillaTypeName("default"));
        assertEquals(TerrainProfile.LARGE_BIOMES,
                TerrainProfile.forVanillaTypeName("largeBiomes"));
        assertEquals(TerrainProfile.AMPLIFIED,
                TerrainProfile.forVanillaTypeName("amplified"));
    }

    @Test
    public void flatCustomizedLegacyDebugAndModdedProfilesDelegate() {
        String[] delegated = {"flat", "customized", "default_1_1",
                "debug_all_block_states", "biomesoplenty"};
        for (String name : delegated) {
            assertEquals(name, TerrainProfile.DELEGATED,
                    TerrainProfile.forVanillaTypeName(name));
        }
    }

    @Test
    public void serializedNamesRoundTripAndUnknownValuesFailSafeToDelegated() {
        for (TerrainProfile profile : TerrainProfile.values()) {
            assertEquals(profile, TerrainProfile.bySerializedName(profile.getSerializedName()));
        }
        assertEquals(TerrainProfile.DELEGATED, TerrainProfile.bySerializedName("future_profile"));
    }
}

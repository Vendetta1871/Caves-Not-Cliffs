package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118NoiseRouterData;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class V118CubicChunksGeneratorTest {
    @Test
    public void mapsOnlyThreeNativeProfilesToJava118Routers() {
        assertTrue(V118CubicChunksGenerator.isNativeProfile(TerrainProfile.DEFAULT));
        assertTrue(V118CubicChunksGenerator.isNativeProfile(TerrainProfile.LARGE_BIOMES));
        assertTrue(V118CubicChunksGenerator.isNativeProfile(TerrainProfile.AMPLIFIED));
        assertFalse(V118CubicChunksGenerator.isNativeProfile(TerrainProfile.DELEGATED));
        assertFalse(V118CubicChunksGenerator.isNativeProfile(null));

        assertEquals(V118NoiseRouterData.Profile.DEFAULT,
            V118CubicChunksGenerator.nativeProfileFor(TerrainProfile.DEFAULT));
        assertEquals(V118NoiseRouterData.Profile.LARGE_BIOMES,
            V118CubicChunksGenerator.nativeProfileFor(TerrainProfile.LARGE_BIOMES));
        assertEquals(V118NoiseRouterData.Profile.AMPLIFIED,
            V118CubicChunksGenerator.nativeProfileFor(TerrainProfile.AMPLIFIED));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDelegatedProfileFromNativeGenerator() {
        V118CubicChunksGenerator.nativeProfileFor(TerrainProfile.DELEGATED);
    }
}

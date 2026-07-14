package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavebiomes.api.IExtendedPopulationGenerator;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118NoiseRouterData;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class V118ChunkGeneratorTest {
    @Test
    public void mapsOnlyThreeNativeProfilesToJava118Routers() {
        assertTrue(V118ChunkGenerator.isNativeProfile(TerrainProfile.DEFAULT));
        assertTrue(V118ChunkGenerator.isNativeProfile(TerrainProfile.LARGE_BIOMES));
        assertTrue(V118ChunkGenerator.isNativeProfile(TerrainProfile.AMPLIFIED));
        assertFalse(V118ChunkGenerator.isNativeProfile(TerrainProfile.DELEGATED));
        assertFalse(V118ChunkGenerator.isNativeProfile(null));

        assertEquals(V118NoiseRouterData.Profile.DEFAULT,
            V118ChunkGenerator.nativeProfileFor(TerrainProfile.DEFAULT));
        assertEquals(V118NoiseRouterData.Profile.LARGE_BIOMES,
            V118ChunkGenerator.nativeProfileFor(TerrainProfile.LARGE_BIOMES));
        assertEquals(V118NoiseRouterData.Profile.AMPLIFIED,
            V118ChunkGenerator.nativeProfileFor(TerrainProfile.AMPLIFIED));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsDelegatedProfileFromNativeGenerator() {
        V118ChunkGenerator.nativeProfileFor(TerrainProfile.DELEGATED);
    }

    @Test
    public void nativeFeaturesOptIntoTheSymmetricPopulationGate() {
        assertTrue(IExtendedPopulationGenerator.class
            .isAssignableFrom(V118ChunkGenerator.class));
    }

    @Test
    public void virtualBiomeQueriesCoverExactlyTheFiniteGeneratedHeight() {
        assertFalse(V118ChunkGenerator.hasVirtualBiomeY(-65));
        assertTrue(V118ChunkGenerator.hasVirtualBiomeY(-64));
        assertTrue(V118ChunkGenerator.hasVirtualBiomeY(319));
        assertFalse(V118ChunkGenerator.hasVirtualBiomeY(320));

        assertFalse(V118ChunkGenerator.hasDecorationBiomeY(-65));
        assertTrue(V118ChunkGenerator.hasDecorationBiomeY(-64));
        assertTrue(V118ChunkGenerator.hasDecorationBiomeY(319));
        assertTrue(V118ChunkGenerator.hasDecorationBiomeY(320));
        assertFalse(V118ChunkGenerator.hasDecorationBiomeY(321));
    }
}

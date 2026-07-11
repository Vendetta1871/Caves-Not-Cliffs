package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118NoiseRouterData;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
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

    @Test
    public void everyFiniteTargetRequiresTheNineFeatureSourceColumnsAtCubeZero() {
        PopulationBox full = V118CubicChunksGenerator.fullPopulationRequirements(5);
        assertEquals(new PopulationBox(-1, -5, -1, 1, -5, 1), full);

        PopulationBox bottom = V118CubicChunksGenerator.fullPopulationRequirements(-4);
        assertEquals(4, bottom.minY);
        assertEquals(4, bottom.maxY);
        PopulationBox top = V118CubicChunksGenerator.fullPopulationRequirements(19);
        assertEquals(-19, top.minY);
        assertEquals(-19, top.maxY);

        assertSame(V118CubicChunksGenerator.fullPopulationRequirements(-5),
            V118CubicChunksGenerator.fullPopulationRequirements(20));
    }

    @Test
    public void retainedStructuresPregenerateTheWholeLegacyVerticalColumn() {
        PopulationBox pregeneration =
            V118CubicChunksGenerator.populationPregenerationRequirements(5);
        assertEquals(new PopulationBox(-1, -5, -1, 1, 10, 1), pregeneration);

        assertSame(V118CubicChunksGenerator.populationPregenerationRequirements(-1),
            V118CubicChunksGenerator.populationPregenerationRequirements(16));
    }

    @Test
    public void featurePopulationLoadsTheCompleteFiniteThreeByThreeRegion() {
        PopulationBox full = V118CubicChunksGenerator.fullPopulationRequirements(0);
        assertEquals(new PopulationBox(-1, 0, -1, 1, 0, 1), full);

        PopulationBox pregeneration =
            V118CubicChunksGenerator.populationPregenerationRequirements(0);
        assertEquals(new PopulationBox(-1, -4, -1, 1, 19, 1), pregeneration);
    }

    @Test
    public void virtualBiomeQueriesCoverExactlyTheFiniteGeneratedHeight() {
        assertFalse(V118CubicChunksGenerator.hasVirtualBiomeY(-65));
        assertTrue(V118CubicChunksGenerator.hasVirtualBiomeY(-64));
        assertTrue(V118CubicChunksGenerator.hasVirtualBiomeY(319));
        assertFalse(V118CubicChunksGenerator.hasVirtualBiomeY(320));
    }
}

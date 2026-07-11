package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118NoiseRouterData;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
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
    public void everyFiniteTargetRequiresTheNineFeatureSourceColumnsAtCubeZero() {
        PopulationBox full = V118ChunkGenerator.fullPopulationRequirements(5);
        assertEquals(new PopulationBox(-1, -5, -1, 1, -5, 1), full);

        PopulationBox bottom = V118ChunkGenerator.fullPopulationRequirements(-4);
        assertEquals(4, bottom.minY);
        assertEquals(4, bottom.maxY);
        PopulationBox top = V118ChunkGenerator.fullPopulationRequirements(19);
        assertEquals(-19, top.minY);
        assertEquals(-19, top.maxY);

        assertSame(V118ChunkGenerator.fullPopulationRequirements(-5),
            V118ChunkGenerator.fullPopulationRequirements(20));
    }

    @Test
    public void retainedStructuresPregenerateTheWholeLegacyVerticalColumn() {
        PopulationBox pregeneration =
            V118ChunkGenerator.populationPregenerationRequirements(5);
        assertEquals(new PopulationBox(-1, -5, -1, 1, 10, 1), pregeneration);

        assertSame(V118ChunkGenerator.populationPregenerationRequirements(-1),
            V118ChunkGenerator.populationPregenerationRequirements(16));
    }

    @Test
    public void featurePopulationLoadsTheCompleteFiniteThreeByThreeRegion() {
        PopulationBox full = V118ChunkGenerator.fullPopulationRequirements(0);
        assertEquals(new PopulationBox(-1, 0, -1, 1, 0, 1), full);

        PopulationBox pregeneration =
            V118ChunkGenerator.populationPregenerationRequirements(0);
        assertEquals(new PopulationBox(-1, -4, -1, 1, 19, 1), pregeneration);
    }

    @Test
    public void virtualBiomeQueriesCoverExactlyTheFiniteGeneratedHeight() {
        assertFalse(V118ChunkGenerator.hasVirtualBiomeY(-65));
        assertTrue(V118ChunkGenerator.hasVirtualBiomeY(-64));
        assertTrue(V118ChunkGenerator.hasVirtualBiomeY(319));
        assertFalse(V118ChunkGenerator.hasVirtualBiomeY(320));
    }
}

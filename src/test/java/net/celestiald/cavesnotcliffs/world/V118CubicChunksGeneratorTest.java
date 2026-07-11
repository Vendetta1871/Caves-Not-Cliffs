package net.celestiald.cavesnotcliffs.world;

import io.github.opencubicchunks.cubicchunks.api.util.Box;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118NoiseRouterData;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

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
    public void retainedStructuresLoadTheWholeLegacyVerticalColumnWithoutOldDecorators() {
        Box full = V118CubicChunksGenerator.fullPopulationRequirements(5);
        AtomicInteger fullCount = new AtomicInteger();
        full.forEachPoint((x, y, z) -> fullCount.incrementAndGet());
        assertEquals(64, fullCount.get());
        assertTrue(full.allMatch((x, y, z) -> x >= -1 && x <= 0
            && y >= -5 && y <= 10 && z >= -1 && z <= 0));

        Box pregeneration = V118CubicChunksGenerator.populationPregenerationRequirements(5);
        AtomicInteger pregenerationCount = new AtomicInteger();
        pregeneration.forEachPoint((x, y, z) -> pregenerationCount.incrementAndGet());
        assertEquals(144, pregenerationCount.get());
        assertTrue(pregeneration.allMatch((x, y, z) -> x >= -1 && x <= 1
            && y >= -5 && y <= 10 && z >= -1 && z <= 1));

        assertSame(V118CubicChunksGenerator.fullPopulationRequirements(-1),
            V118CubicChunksGenerator.fullPopulationRequirements(16));
        assertSame(V118CubicChunksGenerator.populationPregenerationRequirements(-1),
            V118CubicChunksGenerator.populationPregenerationRequirements(16));
    }
}

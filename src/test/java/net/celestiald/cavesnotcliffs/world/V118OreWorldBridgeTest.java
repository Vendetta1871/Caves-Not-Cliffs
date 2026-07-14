package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.worldgen.v118.TerrainColumn;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class V118OreWorldBridgeTest {
    @Test
    public void clampsDecorationBiomeQueriesToTheFiniteResolver() {
        assertEquals(TerrainColumn.MIN_Y, V118OreWorldBridge.clampBiomeY(Integer.MIN_VALUE));
        assertEquals(TerrainColumn.MIN_Y, V118OreWorldBridge.clampBiomeY(-65));
        assertEquals(TerrainColumn.MIN_Y, V118OreWorldBridge.clampBiomeY(-64));
        assertEquals(0, V118OreWorldBridge.clampBiomeY(0));
        assertEquals(TerrainColumn.MAX_Y, V118OreWorldBridge.clampBiomeY(319));
        assertEquals(TerrainColumn.MAX_Y, V118OreWorldBridge.clampBiomeY(320));
        assertEquals(TerrainColumn.MAX_Y,
            V118OreWorldBridge.clampBiomeY(Integer.MAX_VALUE));
    }

    @Test
    public void oceanFloorMatchesChunkAccessHighestTakenConvention() {
        Set<Integer> motionBlocking = new HashSet<>();
        motionBlocking.add(-64);
        motionBlocking.add(17);
        motionBlocking.add(96);
        assertEquals(96, V118OreWorldBridge.highestMotionBlockingY(-64, 319,
            motionBlocking::contains));
        assertEquals(-65, V118OreWorldBridge.highestMotionBlockingY(-64, 319,
            ignored -> false));
    }
}

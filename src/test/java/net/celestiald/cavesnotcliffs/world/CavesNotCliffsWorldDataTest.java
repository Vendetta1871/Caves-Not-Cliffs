package net.celestiald.cavesnotcliffs.world;

import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.WorldInfo;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CavesNotCliffsWorldDataTest {
    @Test
    public void schemaTwoContractRoundTripsThroughLevelDimensionData() {
        WorldInfo info = worldInfo(WorldType.LARGE_BIOMES, "{\"seaLevel\":70}");
        CavesNotCliffsWorldData.writeCurrent(
                info, WorldType.LARGE_BIOMES, TerrainProfile.LARGE_BIOMES);

        CavesNotCliffsWorldData loaded = CavesNotCliffsWorldData.read(info);
        assertEquals(CavesNotCliffsWorldData.CURRENT_SCHEMA, loaded.getTerrainSchema());
        assertEquals("largeBiomes", loaded.getBaseTypeName());
        assertEquals(WorldType.LARGE_BIOMES.getClass().getName(), loaded.getBaseTypeClass());
        assertEquals("{\"seaLevel\":70}", loaded.getGeneratorOptions());
        assertEquals(TerrainProfile.LARGE_BIOMES, loaded.getTerrainProfile());
    }

    @Test
    public void legacyMarkerPinsTheOldDefaultSurfaceContract() {
        WorldInfo info = worldInfo(WorldType.DEFAULT, "");
        CavesNotCliffsWorldData.writeLegacy(info);

        CavesNotCliffsWorldData loaded = CavesNotCliffsWorldData.read(info);
        assertEquals(CavesNotCliffsWorldData.LEGACY_SCHEMA, loaded.getTerrainSchema());
        assertEquals("default", loaded.getBaseTypeName());
        assertEquals(TerrainProfile.DEFAULT, loaded.getTerrainProfile());
    }

    private static WorldInfo worldInfo(WorldType type, String options) {
        WorldSettings settings = new WorldSettings(1234L, GameType.SURVIVAL,
                true, false, type).setGeneratorOptions(options);
        return new WorldInfo(settings, "test");
    }
}

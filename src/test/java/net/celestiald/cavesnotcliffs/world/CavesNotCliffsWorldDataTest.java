package net.celestiald.cavesnotcliffs.world;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.WorldInfo;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

    @Test
    public void generatorContractRejectsSchemaBaseClassAndProfileDrift() {
        WorldInfo info = worldInfo(WorldType.DEFAULT, "");

        CavesNotCliffsWorldData current = CavesNotCliffsWorldData.writeCurrent(
                info, WorldType.DEFAULT, TerrainProfile.DEFAULT);
        expectMismatch(current, CavesNotCliffsWorldData.LEGACY_SCHEMA,
                WorldType.DEFAULT, TerrainProfile.DEFAULT, "terrain schema");
        expectMismatch(current, CavesNotCliffsWorldData.CURRENT_SCHEMA,
                WorldType.LARGE_BIOMES, TerrainProfile.DEFAULT, "base world type");

        NBTTagCompound dimensionData = info.getDimensionData(0);
        dimensionData.getCompoundTag("cavesnotcliffs")
                .setString("baseTypeClass", "missing.replaced.WorldType");
        info.setDimensionData(0, dimensionData);
        CavesNotCliffsWorldData wrongClass = CavesNotCliffsWorldData.read(info);
        expectMismatch(wrongClass, CavesNotCliffsWorldData.CURRENT_SCHEMA,
                WorldType.DEFAULT, TerrainProfile.DEFAULT, "base world type class");

        CavesNotCliffsWorldData wrongProfile = CavesNotCliffsWorldData.writeCurrent(
                info, WorldType.DEFAULT, TerrainProfile.AMPLIFIED);
        expectMismatch(wrongProfile, CavesNotCliffsWorldData.CURRENT_SCHEMA,
                WorldType.DEFAULT, TerrainProfile.DEFAULT, "terrain profile");
    }

    private static void expectMismatch(CavesNotCliffsWorldData data, int schema,
            WorldType baseType, TerrainProfile profile, String messagePart) {
        try {
            data.validateGeneratorContract(schema, baseType, profile);
            fail("Expected the persisted generator contract mismatch to be rejected");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains(messagePart));
        }
    }

    private static WorldInfo worldInfo(WorldType type, String options) {
        WorldSettings settings = new WorldSettings(1234L, GameType.SURVIVAL,
                true, false, type).setGeneratorOptions(options);
        return new WorldInfo(settings, "test");
    }
}

package net.celestiald.cavesnotcliffs.world;

import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.WorldInfo;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class WorldHeightBootstrapTest {
    @BeforeClass
    public static void registerWorldTypes() {
        CavesNotCliffsWorldTypes.registerWrappers();
    }

    @Test
    public void enabledNewDefaultIsWrappedBeforeProviderConstruction() {
        WorldInfo info = worldInfo(WorldType.DEFAULT);

        WorldHeightBootstrap.applyWorldFormatDecision(info, false, true);

        CavesNotCliffsWorldTypeWrapper wrapper =
                CavesNotCliffsWorldTypes.wrapperForBase(WorldType.DEFAULT);
        assertSame(wrapper, info.getTerrainType());
        CavesNotCliffsWorldData data = CavesNotCliffsWorldData.read(info);
        assertEquals(CavesNotCliffsWorldData.CURRENT_SCHEMA, data.getTerrainSchema());
        assertEquals("default", data.getBaseTypeName());
        assertEquals(TerrainProfile.DEFAULT, data.getTerrainProfile());
    }

    @Test
    public void unmarkedExistingWorldIgnoresTheCreationToggle() {
        WorldInfo info = worldInfo(WorldType.DEFAULT);

        WorldHeightBootstrap.applyWorldFormatDecision(info, true, true);

        assertSame(WorldType.DEFAULT, info.getTerrainType());
        assertNull(CavesNotCliffsWorldData.read(info));
    }

    @Test
    public void existingSchemaTwoContractRestoresItsWrapperBeforeConstruction() {
        WorldInfo info = worldInfo(WorldType.DEFAULT);
        CavesNotCliffsWorldData.writeCurrent(
                info, WorldType.DEFAULT, TerrainProfile.DEFAULT);
        info.setTerrainType(WorldType.DEFAULT);

        WorldHeightBootstrap.applyWorldFormatDecision(info, true, false);

        assertSame(CavesNotCliffsWorldTypes.wrapperForBase(WorldType.DEFAULT),
                info.getTerrainType());
    }

    private static WorldInfo worldInfo(WorldType type) {
        WorldSettings settings = new WorldSettings(123456789L, GameType.SURVIVAL,
                true, false, type);
        return new WorldInfo(settings, "test");
    }
}

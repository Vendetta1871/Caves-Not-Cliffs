package net.celestiald.cavesnotcliffs.world;

import net.minecraft.world.WorldType;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class CavesNotCliffsWorldTypesTest {
    @Test
    public void registersVanillaThenModdedWrappersWithStableHiddenContracts() {
        WorldType modded = new TestWorldType("test_twod");
        CavesNotCliffsWorldTypes.registerWrappers();

        List<WorldType> bases = Arrays.asList(
                WorldType.DEFAULT,
                WorldType.FLAT,
                WorldType.LARGE_BIOMES,
                WorldType.AMPLIFIED,
                WorldType.CUSTOMIZED,
                WorldType.DEBUG_ALL_BLOCK_STATES,
                WorldType.DEFAULT_1_1,
                modded);
        Set<String> names = new HashSet<>();
        int previousId = -1;
        for (WorldType base : bases) {
            CavesNotCliffsWorldTypeWrapper wrapper =
                    CavesNotCliffsWorldTypes.wrapperForBase(base);
            assertNotNull(wrapper);
            assertSame(base, wrapper.getBaseType());
            assertFalse(wrapper.canBeCreated());
            assertTrue(wrapper.getName().length() <= WorldTypeNaming.MAX_WORLD_TYPE_NAME_LENGTH);
            assertTrue("wrapper names must be unique", names.add(wrapper.getName()));
            assertTrue("wrappers must follow the stable registration order",
                    wrapper.getId() > previousId);
            previousId = wrapper.getId();
        }

        CavesNotCliffsWorldTypeWrapper moddedWrapper =
                CavesNotCliffsWorldTypes.wrapperForBase(modded);
        assertEquals(WorldTypeNaming.moddedWrapperName(
                modded.getName(), modded.getClass().getName()), moddedWrapper.getName());
        assertEquals(TerrainProfile.DELEGATED, moddedWrapper.getTerrainProfile());

        WorldType late = new TestWorldType("test_late_twod");
        CavesNotCliffsWorldTypes.registerWrappers();
        CavesNotCliffsWorldTypeWrapper lateWrapper =
                CavesNotCliffsWorldTypes.wrapperForBase(late);
        assertNotNull("a later lifecycle scan must discover newly registered types", lateWrapper);
        assertSame(late, lateWrapper.getBaseType());
        assertEquals(WorldTypeNaming.moddedWrapperName(
                late.getName(), late.getClass().getName()), lateWrapper.getName());
        CavesNotCliffsWorldTypes.registerWrappers();
        assertSame("refreshing an already covered type must be idempotent", lateWrapper,
                CavesNotCliffsWorldTypes.wrapperForBase(late));
    }

    private static final class TestWorldType extends WorldType {
        private TestWorldType(String name) {
            super(name);
        }
    }
}

package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavebiomes.api.IExtendedHeightWorldType;
import net.minecraft.world.WorldType;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class CavesNotCliffsWorldTypesTest {
    @Test
    public void finiteWorldTypeLayerHasNoStaticCubicChunksApiLinks() throws IOException {
        // The only static interface link is the required CaveBiomesAPI marker;
        // CubicChunks classes must stay out of the constant pools below.
        assertArrayEquals(new Class<?>[]{IExtendedHeightWorldType.class},
                CavesNotCliffsFiniteWorldType.class.getInterfaces());
        assertTrue(CavesNotCliffsFiniteWorldType.class.isAssignableFrom(
                CavesNotCliffsWorldType.class));
        assertTrue(CavesNotCliffsFiniteWorldType.class.isAssignableFrom(
                CavesNotCliffsWorldTypeWrapper.class));

        for (Class<?> type : Arrays.asList(
                CavesNotCliffsFiniteWorldType.class,
                CavesNotCliffsWorldType.class,
                CavesNotCliffsWorldTypeWrapper.class,
                CavesNotCliffsWorldTypes.class,
                WorldHeightBootstrap.class)) {
            String resource = "/" + type.getName().replace('.', '/') + ".class";
            try (InputStream input = type.getResourceAsStream(resource)) {
                assertNotNull(resource, input);
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    bytes.write(buffer, 0, read);
                }
                String constants = new String(bytes.toByteArray(), StandardCharsets.ISO_8859_1);
                assertFalse(type.getName() + " has a static CubicChunks class reference",
                        constants.contains("io/github/opencubicchunks"));
            }
        }
    }

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
            assertTrue(wrapper instanceof CavesNotCliffsFiniteWorldType);
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

        assertEquals(TerrainProfile.DEFAULT,
                CavesNotCliffsWorldTypes.wrapperForBase(WorldType.DEFAULT).getTerrainProfile());
        assertEquals(TerrainProfile.LARGE_BIOMES,
                CavesNotCliffsWorldTypes.wrapperForBase(WorldType.LARGE_BIOMES)
                        .getTerrainProfile());
        assertEquals(TerrainProfile.AMPLIFIED,
                CavesNotCliffsWorldTypes.wrapperForBase(WorldType.AMPLIFIED)
                        .getTerrainProfile());
        for (WorldType delegated : Arrays.asList(WorldType.FLAT, WorldType.CUSTOMIZED,
                WorldType.DEBUG_ALL_BLOCK_STATES, WorldType.DEFAULT_1_1, modded)) {
            assertEquals("2D and third-party wrappers must remain delegated", TerrainProfile.DELEGATED,
                    CavesNotCliffsWorldTypes.wrapperForBase(delegated).getTerrainProfile());
        }

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
        assertNull("finite wrappers must never be wrapped recursively",
                CavesNotCliffsWorldTypes.wrapperForBase(lateWrapper));
        assertFalse("an ordinary 2D type must not be mistaken for an optional CubicChunks type",
                CavesNotCliffsWorldTypes.isExternalCubicWorldType(modded));
    }

    private static final class TestWorldType extends WorldType {
        private TestWorldType(String name) {
            super(name);
        }
    }
}

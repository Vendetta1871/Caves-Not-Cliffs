package net.celestiald.cavesnotcliffs.registry;

import net.minecraft.util.ResourceLocation;
import org.junit.Test;

import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LegacyContentMappingsTest {
    @Test
    public void mapsReleasedPlaceholderNamesToCanonicalNames() {
        assertEquals("deepslate", LegacyContentMappings.canonicalPath("unnamed_stone"));
        assertEquals("tuff", LegacyContentMappings.canonicalPath("dark_stone"));
        assertEquals("calcite", LegacyContentMappings.canonicalPath("unknown_stone"));
        assertEquals("oxidized_cut_copper_stairs",
                LegacyContentMappings.canonicalPath("copper_stairs_stage3"));
        assertEquals("unrelated", LegacyContentMappings.canonicalPath("unrelated"));
    }

    @Test
    public void preservesLegacyGeodeBlockMarkerButCanonicalizesItsItem() {
        assertEquals("amethyst_geode",
                LegacyContentMappings.canonicalBlockPath("amethyst_geode"));
        assertEquals("amethyst_block",
                LegacyContentMappings.canonicalItemPath("amethyst_geode"));
    }

    @Test
    public void doesNotRewriteAnotherNamespace() {
        ResourceLocation other = new ResourceLocation("example", "dark_stone");
        assertEquals(other, LegacyContentMappings.canonicalLocation(other));
    }

    @Test
    public void canonicalTargetsAreOneToOne() {
        assertEquals(LegacyContentMappings.paths().size(),
                new HashSet<>(LegacyContentMappings.paths().values()).size());
    }

    @Test
    public void everyCanonicalBlockIdHasABlockstateResource() {
        ClassLoader loader = LegacyContentMappingsTest.class.getClassLoader();
        for (Map.Entry<String, String> mapping : LegacyContentMappings.paths().entrySet()) {
            String legacyResource = "assets/cavesnotcliffs/blockstates/"
                    + mapping.getKey() + ".json";
            if (loader.getResource(legacyResource) == null) {
                continue;
            }
            String canonicalResource = "assets/cavesnotcliffs/blockstates/"
                    + mapping.getValue() + ".json";
            assertNotNull("Missing canonical blockstate for " + mapping,
                    loader.getResource(canonicalResource));
        }
    }

    @Test
    public void retainedGeodeGeneratorUsesOnlyCanonicalRuntimeIds() {
        for (ResourceLocation id : CncRegistryIds.GEODE_GENERATION_BLOCKS) {
            assertEquals("cavesnotcliffs", id.getResourceDomain());
            assertEquals(id.getResourcePath(),
                    LegacyContentMappings.canonicalPath(id.getResourcePath()));
            assertNotNull("Missing blockstate for runtime geode lookup " + id,
                    getClass().getClassLoader().getResource(
                            "assets/cavesnotcliffs/blockstates/"
                                    + id.getResourcePath() + ".json"));
        }
    }
}

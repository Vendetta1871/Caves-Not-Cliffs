package net.celestiald.cavesnotcliffs.registry;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Canonical v2 registry paths and their released 1.x/prerelease aliases.
 *
 * <p>The mapping is deliberately limited to IDs whose block-state metadata is directly
 * compatible. Split-state plants and pointed-dripstone segments stay registered under their old
 * internal IDs until the chunk converter can preserve their semantic state.</p>
 */
public final class LegacyContentMappings {
    private static final Map<String, String> PATHS;

    static {
        Map<String, String> paths = new LinkedHashMap<>();
        map(paths, "unnamed_stone", "deepslate");
        map(paths, "dark_stone", "tuff");
        map(paths, "dark_stone_slab", "tuff_slab");
        map(paths, "dark_stone_slab_double", "tuff_slab_double");
        map(paths, "dark_stone_stairs", "tuff_stairs");
        map(paths, "dark_stone_walls", "tuff_wall");
        map(paths, "unknown_stone", "calcite");
        map(paths, "unknown_stone_slab", "calcite_slab");
        map(paths, "unknown_stone_slab_double", "calcite_slab_double");
        map(paths, "unknown_stone_stairs", "calcite_stairs");
        map(paths, "unknown_stone_wall", "calcite_wall");
        map(paths, "moss", "moss_block");
        map(paths, "moss_layer", "moss_carpet");
        map(paths, "dripstone", "dripstone_block");
        map(paths, "stalactite", "pointed_dripstone");
        map(paths, "amethyst_geode", "amethyst_block");
        map(paths, "geode_casing", "smooth_basalt");
        map(paths, "amethyst_crystal_stage_1", "small_amethyst_bud");
        map(paths, "amethyst_crystal_stage_2", "medium_amethyst_bud");
        map(paths, "amethyst_crystal", "amethyst_cluster");
        map(paths, "baby_azalea_tree", "azalea");
        map(paths, "blooming_baby_azalea_tree", "flowering_azalea");
        map(paths, "baby_dripleaf", "small_dripleaf");
        map(paths, "copper_block_stage1", "exposed_copper");
        map(paths, "copper_block_stage2", "weathered_copper");
        map(paths, "copper_block_stage3", "oxidized_copper");
        map(paths, "copper_stairs", "cut_copper_stairs");
        map(paths, "copper_stairs_stage1", "exposed_cut_copper_stairs");
        map(paths, "copper_stairs_stage2", "weathered_cut_copper_stairs");
        map(paths, "copper_stairs_stage3", "oxidized_cut_copper_stairs");
        map(paths, "copper_slab", "cut_copper_slab");
        map(paths, "copper_slab_double", "cut_copper_slab_double");
        map(paths, "copper_slab_stage1", "exposed_cut_copper_slab");
        map(paths, "copper_slab_stage1_double", "exposed_cut_copper_slab_double");
        map(paths, "copper_slab_stage2", "weathered_cut_copper_slab");
        map(paths, "copper_slab_stage2_double", "weathered_cut_copper_slab_double");
        map(paths, "copper_slab_stage3", "oxidized_cut_copper_slab");
        map(paths, "copper_slab_stage3_double", "oxidized_cut_copper_slab_double");
        PATHS = Collections.unmodifiableMap(paths);
    }

    private LegacyContentMappings() {
    }

    private static void map(Map<String, String> paths, String legacy, String canonical) {
        if (paths.put(legacy, canonical) != null) {
            throw new IllegalStateException("Duplicate legacy content path: " + legacy);
        }
    }

    public static String canonicalPath(String path) {
        String canonical = PATHS.get(path);
        return canonical == null ? path : canonical;
    }

    public static ResourceLocation canonicalLocation(ResourceLocation location) {
        if (!CavesNotCliffs.MODID.equals(location.getResourceDomain())) {
            return location;
        }
        return new ResourceLocation(CavesNotCliffs.MODID,
                canonicalPath(location.getResourcePath()));
    }

    public static Map<String, String> paths() {
        return PATHS;
    }
}

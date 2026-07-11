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
    private static final Map<String, String> BLOCK_PATHS;
    private static final Map<String, String> ITEM_PATHS;

    static {
        Map<String, String> blocks = new LinkedHashMap<>();
        map(blocks, "unnamed_stone", "deepslate");
        map(blocks, "dark_stone", "tuff");
        map(blocks, "dark_stone_slab", "tuff_slab");
        map(blocks, "dark_stone_slab_double", "tuff_slab_double");
        map(blocks, "dark_stone_stairs", "tuff_stairs");
        map(blocks, "dark_stone_walls", "tuff_wall");
        map(blocks, "unknown_stone", "calcite");
        map(blocks, "unknown_stone_slab", "calcite_slab");
        map(blocks, "unknown_stone_slab_double", "calcite_slab_double");
        map(blocks, "unknown_stone_stairs", "calcite_stairs");
        map(blocks, "unknown_stone_wall", "calcite_wall");
        map(blocks, "moss", "moss_block");
        map(blocks, "moss_layer", "moss_carpet");
        map(blocks, "dripstone", "dripstone_block");
        map(blocks, "stalactite", "pointed_dripstone");
        // amethyst_geode remains registered as a hidden marker until its chunk conversion runs.
        map(blocks, "geode_casing", "smooth_basalt");
        map(blocks, "amethyst_crystal_stage_1", "small_amethyst_bud");
        map(blocks, "amethyst_crystal_stage_2", "medium_amethyst_bud");
        map(blocks, "amethyst_crystal", "amethyst_cluster");
        map(blocks, "baby_azalea_tree", "azalea");
        map(blocks, "blooming_baby_azalea_tree", "flowering_azalea");
        map(blocks, "baby_dripleaf", "small_dripleaf");
        map(blocks, "dripleaf_plant", "big_dripleaf");
        map(blocks, "copper_block_stage1", "exposed_copper");
        map(blocks, "copper_block_stage2", "weathered_copper");
        map(blocks, "copper_block_stage3", "oxidized_copper");
        map(blocks, "copper_stairs", "cut_copper_stairs");
        map(blocks, "copper_stairs_stage1", "exposed_cut_copper_stairs");
        map(blocks, "copper_stairs_stage2", "weathered_cut_copper_stairs");
        map(blocks, "copper_stairs_stage3", "oxidized_cut_copper_stairs");
        map(blocks, "copper_slab", "cut_copper_slab");
        map(blocks, "copper_slab_double", "cut_copper_slab_double");
        map(blocks, "copper_slab_stage1", "exposed_cut_copper_slab");
        map(blocks, "copper_slab_stage1_double", "exposed_cut_copper_slab_double");
        map(blocks, "copper_slab_stage2", "weathered_cut_copper_slab");
        map(blocks, "copper_slab_stage2_double", "weathered_cut_copper_slab_double");
        map(blocks, "copper_slab_stage3", "oxidized_cut_copper_slab");
        map(blocks, "copper_slab_stage3_double", "oxidized_cut_copper_slab_double");
        BLOCK_PATHS = Collections.unmodifiableMap(blocks);

        Map<String, String> items = new LinkedHashMap<>(blocks);
        // Double slabs are hidden block-only storage in v2. Any prerelease stack carrying one of
        // the old state-split IDs must become the obtainable single slab rather than mapping to a
        // registry path which intentionally has no ItemBlock.
        replaceItem(items, "copper_slab_double", "cut_copper_slab");
        replaceItem(items, "copper_slab_stage1_double", "exposed_cut_copper_slab");
        replaceItem(items, "copper_slab_stage2_double", "weathered_cut_copper_slab");
        replaceItem(items, "copper_slab_stage3_double", "oxidized_cut_copper_slab");
        // The old block identity is retained for chunk recognition, but its obtainable item is not.
        map(items, "amethyst_geode", "amethyst_block");
        // State-split companion blocks remain registered, but their old ItemBlocks do not.
        map(items, "glow_berry_vines", "glow_berries");
        map(items, "glow_berry_middle_fill", "glow_berries");
        map(items, "dripleafplant_1", "big_dripleaf");
        map(items, "dripleaf_plant_2", "big_dripleaf");
        map(items, "dripleaf_stem", "big_dripleaf");
        map(items, "stalagmite", "pointed_dripstone");
        map(items, "bottom_stalactite", "pointed_dripstone");
        map(items, "middle_stalactite", "pointed_dripstone");
        map(items, "top_stalactite", "pointed_dripstone");
        map(items, "bottom_stalagmite", "pointed_dripstone");
        map(items, "middle_stalagmite", "pointed_dripstone");
        map(items, "top_stalagmite", "pointed_dripstone");
        ITEM_PATHS = Collections.unmodifiableMap(items);
    }

    private LegacyContentMappings() {
    }

    private static void map(Map<String, String> paths, String legacy, String canonical) {
        if (paths.put(legacy, canonical) != null) {
            throw new IllegalStateException("Duplicate legacy content path: " + legacy);
        }
    }

    private static void replaceItem(Map<String, String> paths, String legacy, String canonical) {
        if (!paths.containsKey(legacy)) {
            throw new IllegalStateException("Missing inherited legacy item path: " + legacy);
        }
        paths.put(legacy, canonical);
    }

    public static String canonicalPath(String path) {
        return canonicalBlockPath(path);
    }

    public static String canonicalBlockPath(String path) {
        String canonical = BLOCK_PATHS.get(path);
        return canonical == null ? path : canonical;
    }

    public static String canonicalItemPath(String path) {
        String canonical = ITEM_PATHS.get(path);
        return canonical == null ? path : canonical;
    }

    public static ResourceLocation canonicalLocation(ResourceLocation location) {
        return canonicalBlockLocation(location);
    }

    public static ResourceLocation canonicalBlockLocation(ResourceLocation location) {
        if (!CavesNotCliffs.MODID.equals(location.getResourceDomain())) {
            return location;
        }
        return new ResourceLocation(CavesNotCliffs.MODID,
                canonicalBlockPath(location.getResourcePath()));
    }

    public static ResourceLocation canonicalItemLocation(ResourceLocation location) {
        if (!CavesNotCliffs.MODID.equals(location.getResourceDomain())) {
            return location;
        }
        return new ResourceLocation(CavesNotCliffs.MODID,
                canonicalItemPath(location.getResourcePath()));
    }

    public static Map<String, String> paths() {
        return BLOCK_PATHS;
    }

    public static Map<String, String> blockPaths() {
        return BLOCK_PATHS;
    }

    public static Map<String, String> itemPaths() {
        return ITEM_PATHS;
    }
}

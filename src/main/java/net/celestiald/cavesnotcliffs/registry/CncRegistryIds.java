package net.celestiald.cavesnotcliffs.registry;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.minecraft.util.ResourceLocation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Canonical registry identities shared by runtime content lookups and migrations. */
public final class CncRegistryIds {
    public static final ResourceLocation AMETHYST_BLOCK = id("amethyst_block");
    public static final ResourceLocation BUDDING_AMETHYST = id("budding_amethyst");
    public static final ResourceLocation SMOOTH_BASALT = id("smooth_basalt");
    public static final ResourceLocation CALCITE = id("calcite");
    public static final ResourceLocation SMALL_AMETHYST_BUD = id("small_amethyst_bud");
    public static final ResourceLocation MEDIUM_AMETHYST_BUD = id("medium_amethyst_bud");
    public static final ResourceLocation LARGE_AMETHYST_BUD = id("large_amethyst_bud");
    public static final ResourceLocation AMETHYST_CLUSTER = id("amethyst_cluster");
    /** Hidden storage identities used only when a generated growth occupies source water. */
    public static final ResourceLocation SMALL_AMETHYST_BUD_WATERLOGGED =
            id("small_amethyst_bud_waterlogged");
    public static final ResourceLocation MEDIUM_AMETHYST_BUD_WATERLOGGED =
            id("medium_amethyst_bud_waterlogged");
    public static final ResourceLocation LARGE_AMETHYST_BUD_WATERLOGGED =
            id("large_amethyst_bud_waterlogged");
    public static final ResourceLocation AMETHYST_CLUSTER_WATERLOGGED =
            id("amethyst_cluster_waterlogged");
    public static final ResourceLocation GLOW_BERRIES = id("glow_berries");
    /** Hidden cave-vine head/body storage; glow berries are their only obtainable item. */
    public static final ResourceLocation CAVE_VINES = id("cave_vines");
    public static final ResourceLocation CAVE_VINES_AGE_8_15 = id("cave_vines_age_8_15");
    public static final ResourceLocation CAVE_VINES_AGE_16_23 = id("cave_vines_age_16_23");
    public static final ResourceLocation CAVE_VINES_AGE_24_25 = id("cave_vines_age_24_25");
    public static final ResourceLocation CAVE_VINES_PLANT = id("cave_vines_plant");
    public static final ResourceLocation AZALEA = id("azalea");
    public static final ResourceLocation FLOWERING_AZALEA = id("flowering_azalea");
    public static final ResourceLocation AZALEA_LEAVES = id("azalea_leaves");
    public static final ResourceLocation FLOWERING_AZALEA_LEAVES = id("flowering_azalea_leaves");
    public static final ResourceLocation ROOTED_DIRT = id("rooted_dirt");
    public static final ResourceLocation HANGING_ROOTS = id("hanging_roots");
    public static final ResourceLocation HANGING_ROOTS_WATERLOGGED =
            id("hanging_roots_waterlogged");
    public static final ResourceLocation MOSS_BLOCK = id("moss_block");
    public static final ResourceLocation MOSS_CARPET = id("moss_carpet");
    public static final ResourceLocation SMALL_DRIPLEAF = id("small_dripleaf");
    public static final ResourceLocation BIG_DRIPLEAF = id("big_dripleaf");
    public static final ResourceLocation BIG_DRIPLEAF_WATERLOGGED =
            id("big_dripleaf_waterlogged");
    public static final ResourceLocation BIG_DRIPLEAF_STEM = id("big_dripleaf_stem");
    public static final ResourceLocation SPORE_BLOSSOM = id("spore_blossom");
    public static final ResourceLocation POTTED_AZALEA = id("potted_azalea_bush");
    public static final ResourceLocation POTTED_FLOWERING_AZALEA =
            id("potted_flowering_azalea_bush");
    public static final ResourceLocation DRIPSTONE_BLOCK = id("dripstone_block");
    public static final ResourceLocation POINTED_DRIPSTONE = id("pointed_dripstone");
    /** Hidden storage identity for source-water pointed-dripstone states. */
    public static final ResourceLocation POINTED_DRIPSTONE_WATERLOGGED =
            id("pointed_dripstone_waterlogged");
    /** Hidden storage for empty, layered-water, and full-lava cauldron states. */
    public static final ResourceLocation LAVA_CAULDRON = id("lava_cauldron");
    public static final ResourceLocation POWDER_SNOW = id("powder_snow");
    public static final ResourceLocation POWDER_SNOW_BUCKET = id("powder_snow_bucket");
    /** Hidden state companion; the vanilla cauldron remains the only obtainable item. */
    public static final ResourceLocation POWDER_SNOW_CAULDRON = id("powder_snow_cauldron");
    public static final ResourceLocation COMPOSTER = id("composter");
    public static final ResourceLocation INFESTED_DEEPSLATE = id("infested_deepslate");
    public static final ResourceLocation STONECUTTER = id("stonecutter");
    public static final ResourceLocation AXOLOTL = id("axolotl");
    public static final ResourceLocation AXOLOTL_BUCKET = id("axolotl_bucket");
    public static final ResourceLocation TROPICAL_FISH_BUCKET = id("tropical_fish_bucket");
    public static final ResourceLocation BEE = id("bee");
    public static final ResourceLocation BEE_NEST = id("bee_nest");
    public static final ResourceLocation BEEHIVE = id("beehive");
    public static final ResourceLocation HONEYCOMB = id("honeycomb");
    public static final ResourceLocation HONEY_BOTTLE = id("honey_bottle");
    public static final ResourceLocation HONEY_BLOCK = id("honey_block");
    public static final ResourceLocation HONEYCOMB_BLOCK = id("honeycomb_block");
    public static final ResourceLocation CAMPFIRE = id("campfire");
    public static final ResourceLocation SOUL_CAMPFIRE = id("soul_campfire");
    public static final ResourceLocation SOUL_SOIL = id("soul_soil");
    public static final List<ResourceLocation> CANDLES = paths(
            "candle", "white_candle", "orange_candle", "magenta_candle",
            "light_blue_candle", "yellow_candle", "lime_candle", "pink_candle",
            "gray_candle", "light_gray_candle", "cyan_candle", "purple_candle",
            "blue_candle", "brown_candle", "green_candle", "red_candle",
            "black_candle");
    /** Hidden block-state companions created only by inserting a candle into a full cake. */
    public static final List<ResourceLocation> CANDLE_CAKES = paths(
            "candle_cake", "white_candle_cake", "orange_candle_cake",
            "magenta_candle_cake", "light_blue_candle_cake", "yellow_candle_cake",
            "lime_candle_cake", "pink_candle_cake", "gray_candle_cake",
            "light_gray_candle_cake", "cyan_candle_cake", "purple_candle_cake",
            "blue_candle_cake", "brown_candle_cake", "green_candle_cake",
            "red_candle_cake", "black_candle_cake");
    /** Hidden model/storage states used when the tile's honey level reaches five. */
    public static final ResourceLocation BEE_NEST_HONEY = id("bee_nest_honey");
    public static final ResourceLocation BEEHIVE_HONEY = id("beehive_honey");

    /**
     * The released pre-v2 geode-shell identity is intentionally retained as an internal marker.
     * It must not be used for newly generated content or exposed through an ItemBlock.
     */
    public static final ResourceLocation LEGACY_AMETHYST_GEODE = id("amethyst_geode");

    /** Every registry entry the retained geode feature must resolve before it can generate. */
    public static final List<ResourceLocation> GEODE_GENERATION_BLOCKS =
            Collections.unmodifiableList(Arrays.asList(
                    SMOOTH_BASALT,
                    AMETHYST_BLOCK,
                    BUDDING_AMETHYST,
                    CALCITE,
                    SMALL_AMETHYST_BUD,
                    MEDIUM_AMETHYST_BUD,
                    LARGE_AMETHYST_BUD,
                    AMETHYST_CLUSTER,
                    SMALL_AMETHYST_BUD_WATERLOGGED,
                    MEDIUM_AMETHYST_BUD_WATERLOGGED,
                    LARGE_AMETHYST_BUD_WATERLOGGED,
                    AMETHYST_CLUSTER_WATERLOGGED));

    private CncRegistryIds() {
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(CavesNotCliffs.MODID, path);
    }

    private static List<ResourceLocation> paths(String... paths) {
        ResourceLocation[] ids = new ResourceLocation[paths.length];
        for (int i = 0; i < paths.length; i++) {
            ids[i] = id(paths[i]);
        }
        return Collections.unmodifiableList(Arrays.asList(ids));
    }
}

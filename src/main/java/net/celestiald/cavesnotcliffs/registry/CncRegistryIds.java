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
    public static final ResourceLocation AMETHYST_CLUSTER = id("amethyst_cluster");
    public static final ResourceLocation GLOW_BERRIES = id("glow_berries");
    public static final ResourceLocation BIG_DRIPLEAF = id("big_dripleaf");
    public static final ResourceLocation POINTED_DRIPSTONE = id("pointed_dripstone");

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
                    CALCITE,
                    SMALL_AMETHYST_BUD,
                    MEDIUM_AMETHYST_BUD,
                    AMETHYST_CLUSTER));

    private CncRegistryIds() {
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(CavesNotCliffs.MODID, path);
    }
}

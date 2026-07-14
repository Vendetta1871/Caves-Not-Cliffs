package net.celestiald.cavesnotcliffs.block;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Stable hidden registry identities used to represent 1.18 waterlogged amethyst growth. */
public final class AmethystWaterlogging {
    public static final String SUFFIX = "_waterlogged";

    public static final List<String> PUBLIC_STAGES = Collections.unmodifiableList(Arrays.asList(
            "small_amethyst_bud",
            "medium_amethyst_bud",
            "large_amethyst_bud",
            "amethyst_cluster"));

    private AmethystWaterlogging() {
    }

    public static String companionPath(String publicPath) {
        if (!PUBLIC_STAGES.contains(publicPath)) {
            throw new IllegalArgumentException("Not an amethyst growth stage: " + publicPath);
        }
        return publicPath + SUFFIX;
    }

    public static boolean isCompanionPath(String path) {
        return path != null && path.endsWith(SUFFIX)
                && PUBLIC_STAGES.contains(path.substring(0, path.length() - SUFFIX.length()));
    }

    public static String publicPath(String path) {
        if (isCompanionPath(path)) {
            return path.substring(0, path.length() - SUFFIX.length());
        }
        if (!PUBLIC_STAGES.contains(path)) {
            throw new IllegalArgumentException("Not an amethyst growth identity: " + path);
        }
        return path;
    }

    /** Draft-v2 encoded waterlogging in metadata bit 3; resolve it to the hidden storage ID. */
    public static String storagePathForMetadata(String publicPath, int metadata) {
        return (metadata & 8) == 0 ? publicPath : companionPath(publicPath);
    }
}

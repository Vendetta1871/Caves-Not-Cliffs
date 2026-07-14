package net.celestiald.cavesnotcliffs.content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Version-independent copper catalog and the numeric core of Java 1.18.2 weathering.
 *
 * <p>The runtime blocks delegate to this class so waxing, axes, dispensers, lightning, recipes,
 * and save remapping all share one stable set of registry paths. Double slabs are deliberately
 * present in the catalog but marked hidden: 1.12 needs a companion block to represent the modern
 * slab {@code type=double} state, while only the single slab owns an item.</p>
 */
public final class CopperWeathering {
    /** Exact random-tick gate from {@code ChangeOverTimeBlock#onRandomTick}. */
    public static final float RANDOM_TICK_CHANCE = 0.05688889F;
    public static final int SCAN_DISTANCE = 4;

    public enum Stage {
        UNAFFECTED,
        EXPOSED,
        WEATHERED,
        OXIDIZED;

        public Stage previous() {
            return ordinal() == 0 ? null : values()[ordinal() - 1];
        }

        public Stage next() {
            return ordinal() == values().length - 1 ? null : values()[ordinal() + 1];
        }
    }

    public enum Shape {
        BLOCK,
        CUT,
        STAIRS,
        SLAB,
        DOUBLE_SLAB
    }

    public enum AxeAction {
        SCRAPE,
        UNWAX,
        PASS
    }

    public static final class Variant {
        private final String path;
        private final Stage stage;
        private final Shape shape;
        private final boolean waxed;
        private final boolean publicItem;

        private Variant(String path, Stage stage, Shape shape, boolean waxed,
                boolean publicItem) {
            this.path = path;
            this.stage = stage;
            this.shape = shape;
            this.waxed = waxed;
            this.publicItem = publicItem;
        }

        public String getPath() {
            return path;
        }

        public Stage getStage() {
            return stage;
        }

        public Shape getShape() {
            return shape;
        }

        public boolean isWaxed() {
            return waxed;
        }

        public boolean hasPublicItem() {
            return publicItem;
        }

        @Override
        public String toString() {
            return path;
        }
    }

    private static final Map<String, Variant> BY_PATH;
    private static final List<Variant> VARIANTS;

    static {
        Map<String, Variant> byPath = new LinkedHashMap<>();
        for (boolean waxed : new boolean[]{false, true}) {
            for (Stage stage : Stage.values()) {
                add(byPath, stage, Shape.BLOCK, waxed, true);
                add(byPath, stage, Shape.CUT, waxed, true);
                add(byPath, stage, Shape.STAIRS, waxed, true);
                add(byPath, stage, Shape.SLAB, waxed, true);
                add(byPath, stage, Shape.DOUBLE_SLAB, waxed, false);
            }
        }
        BY_PATH = Collections.unmodifiableMap(byPath);
        VARIANTS = Collections.unmodifiableList(new ArrayList<>(byPath.values()));
    }

    private CopperWeathering() {
    }

    private static void add(Map<String, Variant> variants, Stage stage, Shape shape,
            boolean waxed, boolean publicItem) {
        Variant variant = new Variant(path(stage, shape, waxed), stage, shape, waxed, publicItem);
        if (variants.put(variant.path, variant) != null) {
            throw new IllegalStateException("Duplicate copper registry path: " + variant.path);
        }
    }

    /** Returns the canonical Java 1.18.2 path, including the hidden 1.12 double-slab suffix. */
    public static String path(Stage stage, Shape shape, boolean waxed) {
        String age;
        switch (stage) {
            case UNAFFECTED:
                age = "";
                break;
            case EXPOSED:
                age = "exposed_";
                break;
            case WEATHERED:
                age = "weathered_";
                break;
            case OXIDIZED:
                age = "oxidized_";
                break;
            default:
                throw new IllegalArgumentException("Unknown copper stage: " + stage);
        }

        String base;
        switch (shape) {
            case BLOCK:
                base = stage == Stage.UNAFFECTED ? "copper_block" : age + "copper";
                age = "";
                break;
            case CUT:
                base = age + "cut_copper";
                age = "";
                break;
            case STAIRS:
                base = age + "cut_copper_stairs";
                age = "";
                break;
            case SLAB:
                base = age + "cut_copper_slab";
                age = "";
                break;
            case DOUBLE_SLAB:
                base = age + "cut_copper_slab_double";
                age = "";
                break;
            default:
                throw new IllegalArgumentException("Unknown copper shape: " + shape);
        }
        return waxed ? "waxed_" + base : base;
    }

    public static List<Variant> variants() {
        return VARIANTS;
    }

    public static Map<String, Variant> variantsByPath() {
        return BY_PATH;
    }

    public static Variant variant(String path) {
        return BY_PATH.get(path);
    }

    public static Variant counterpart(Variant source, Stage stage, boolean waxed) {
        return variant(path(stage, source.shape, waxed));
    }

    public static Variant next(Variant source) {
        Stage next = source.stage.next();
        return next == null || source.waxed ? null : counterpart(source, next, false);
    }

    public static Variant previous(Variant source) {
        Stage previous = source.stage.previous();
        return previous == null || source.waxed ? null : counterpart(source, previous, false);
    }

    public static Variant waxed(Variant source) {
        return source.waxed ? null : counterpart(source, source.stage, true);
    }

    public static Variant unwaxed(Variant source) {
        return source.waxed ? counterpart(source, source.stage, false) : null;
    }

    /** Mirrors AxeItem's scrape-before-unwax precedence. */
    public static AxeAction axeAction(Variant source) {
        if (previous(source) != null) {
            return AxeAction.SCRAPE;
        }
        return unwaxed(source) != null ? AxeAction.UNWAX : AxeAction.PASS;
    }

    public static Variant axeResult(Variant source) {
        AxeAction action = axeAction(source);
        if (action == AxeAction.SCRAPE) {
            return previous(source);
        }
        return action == AxeAction.UNWAX ? unwaxed(source) : null;
    }

    /** The unaffected stage uses the exact 0.75 modifier; all later stages use 1.0. */
    public static float chanceModifier(Stage stage) {
        return stage == Stage.UNAFFECTED ? 0.75F : 1.0F;
    }

    /**
     * Computes the second-roll threshold after the radius-four scan.
     *
     * @return {@code -1} when a younger neighbor vetoes weathering; otherwise the exact squared
     *     ratio used by Java 1.18.2.
     */
    public static float transitionChance(Stage current, Iterable<Stage> neighbors) {
        int same = 0;
        int older = 0;
        for (Stage neighbor : neighbors) {
            if (neighbor.ordinal() < current.ordinal()) {
                return -1.0F;
            }
            if (neighbor.ordinal() > current.ordinal()) {
                older++;
            } else {
                same++;
            }
        }
        float ratio = (float) (older + 1) / (float) (older + same + 1);
        return ratio * ratio * chanceModifier(current);
    }

    /** Applies both official random rolls without consuming any extra randomness. */
    public static boolean shouldAdvance(Stage current, Iterable<Stage> neighbors,
            float tickRoll, float transitionRoll) {
        if (!(tickRoll < RANDOM_TICK_CHANCE)) {
            return false;
        }
        float chance = transitionChance(current, neighbors);
        return chance >= 0.0F && transitionRoll < chance;
    }

    /** Groups paths by stage for deterministic oracle and migration matrices. */
    public static Map<Stage, List<Variant>> variantsByStage(boolean waxed) {
        Map<Stage, List<Variant>> result = new EnumMap<>(Stage.class);
        for (Stage stage : Stage.values()) {
            result.put(stage, new ArrayList<Variant>());
        }
        for (Variant variant : VARIANTS) {
            if (variant.waxed == waxed) {
                result.get(variant.stage).add(variant);
            }
        }
        for (Map.Entry<Stage, List<Variant>> entry : result.entrySet()) {
            entry.setValue(Collections.unmodifiableList(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }
}

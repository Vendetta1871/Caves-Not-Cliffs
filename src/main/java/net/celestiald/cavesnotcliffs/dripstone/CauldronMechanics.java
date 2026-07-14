package net.celestiald.cavesnotcliffs.dripstone;

/** Exact Java 1.18.2 cauldron state matrix expressed independently of 1.12 blocks. */
public final class CauldronMechanics {
    public static final int MAX_LEVEL = 3;
    public static final float RAIN_FILL_CHANCE = 0.05F;
    public static final float SNOW_FILL_CHANCE = 0.1F;
    public static final double LAVA_CONTENT_HEIGHT = 15.0D / 16.0D;

    private CauldronMechanics() {
    }

    public enum Content {
        EMPTY,
        WATER,
        LAVA,
        POWDER_SNOW
    }

    public enum DripFluid {
        WATER,
        LAVA
    }

    /** The seven state-changing entries shared by the four Java 1.18.2 interaction maps. */
    public enum Interaction {
        FILL_WATER,
        FILL_LAVA,
        FILL_POWDER_SNOW,
        TAKE_BUCKET,
        TAKE_BOTTLE,
        POUR_WATER_BOTTLE,
        CLEAN
    }

    public static final class State {
        public final Content content;
        public final int level;

        public State(Content content, int level) {
            requireValid(content, level);
            this.content = content;
            this.level = level;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof State
                    && ((State) other).content == content
                    && ((State) other).level == level;
        }

        @Override
        public int hashCode() {
            return content.hashCode() * 31 + level;
        }

        @Override
        public String toString() {
            return content + "@" + level;
        }
    }

    public static State empty() {
        return new State(Content.EMPTY, 0);
    }

    public static State water(int level) {
        return new State(Content.WATER, level);
    }

    public static State lava() {
        return new State(Content.LAVA, MAX_LEVEL);
    }

    public static State powderSnow(int level) {
        return new State(Content.POWDER_SNOW, level);
    }

    public static boolean canReceiveDrip(State state, DripFluid fluid) {
        if (state.content == Content.EMPTY) {
            return true;
        }
        return state.content == Content.WATER && state.level < MAX_LEVEL
                && fluid == DripFluid.WATER;
    }

    public static State receiveDrip(State state, DripFluid fluid) {
        if (!canReceiveDrip(state, fluid)) {
            return state;
        }
        if (fluid == DripFluid.LAVA) {
            return lava();
        }
        return water(state.content == Content.WATER ? state.level + 1 : 1);
    }

    public static boolean canFillBucket(State state) {
        return state.content == Content.LAVA
                || (state.content == Content.WATER || state.content == Content.POWDER_SNOW)
                && state.level == MAX_LEVEL;
    }

    public static int comparatorSignal(State state) {
        return state.content == Content.EMPTY ? 0
                : state.content == Content.LAVA ? MAX_LEVEL : state.level;
    }

    public static double contentHeight(State state) {
        if (state.content == Content.EMPTY) {
            return 0.0D;
        }
        if (state.content == Content.LAVA) {
            return LAVA_CONTENT_HEIGHT;
        }
        return (6.0D + state.level * 3.0D) / 16.0D;
    }

    public static State lowerLayer(State state) {
        if (state.content != Content.WATER && state.content != Content.POWDER_SNOW) {
            return state;
        }
        return state.level == 1 ? empty() : new State(state.content, state.level - 1);
    }

    /** Returns whether the selected 1.18.2 cauldron interaction map accepts the item. */
    public static boolean canInteract(State state, Interaction interaction) {
        requireStateAndInteraction(state, interaction);
        switch (interaction) {
            case FILL_WATER:
            case FILL_LAVA:
            case FILL_POWDER_SNOW:
                // addDefaultInteractions installs all three filled buckets in every map.
                return true;
            case TAKE_BUCKET:
                return canFillBucket(state);
            case TAKE_BOTTLE:
            case CLEAN:
                return state.content == Content.WATER;
            case POUR_WATER_BOTTLE:
                return state.content == Content.EMPTY
                        || state.content == Content.WATER && state.level < MAX_LEVEL;
            default:
                throw new AssertionError(interaction);
        }
    }

    /** Applies an accepted interaction, or returns the unchanged state for a map miss. */
    public static State interact(State state, Interaction interaction) {
        if (!canInteract(state, interaction)) {
            return state;
        }
        switch (interaction) {
            case FILL_WATER:
                return water(MAX_LEVEL);
            case FILL_LAVA:
                return lava();
            case FILL_POWDER_SNOW:
                return powderSnow(MAX_LEVEL);
            case TAKE_BUCKET:
                return empty();
            case TAKE_BOTTLE:
            case CLEAN:
                return lowerLayer(state);
            case POUR_WATER_BOTTLE:
                return water(state.content == Content.EMPTY ? 1 : state.level + 1);
            default:
                throw new AssertionError(interaction);
        }
    }

    /** Burning entities turn N layers of powder snow into N-1 layers of water. */
    public static State extinguishInPowderSnow(State state) {
        if (state.content != Content.POWDER_SNOW) {
            return state;
        }
        return state.level == 1 ? empty() : water(state.level - 1);
    }

    public static State precipitation(State state, boolean snow, float randomValue) {
        float chance = snow ? SNOW_FILL_CHANCE : RAIN_FILL_CHANCE;
        if (randomValue >= chance) {
            return state;
        }
        if (state.content == Content.EMPTY) {
            return snow ? powderSnow(1) : water(1);
        }
        if (!snow && state.content == Content.WATER && state.level < MAX_LEVEL) {
            return water(state.level + 1);
        }
        if (snow && state.content == Content.POWDER_SNOW && state.level < MAX_LEVEL) {
            return powderSnow(state.level + 1);
        }
        return state;
    }

    private static void requireValid(Content content, int level) {
        if (content == null) {
            throw new IllegalArgumentException("Cauldron content cannot be null");
        }
        boolean valid = content == Content.EMPTY && level == 0
                || content == Content.LAVA && level == MAX_LEVEL
                || (content == Content.WATER || content == Content.POWDER_SNOW)
                && level >= 1 && level <= MAX_LEVEL;
        if (!valid) {
            throw new IllegalArgumentException("Invalid cauldron state " + content + "@" + level);
        }
    }

    private static void requireStateAndInteraction(State state, Interaction interaction) {
        if (state == null || interaction == null) {
            throw new IllegalArgumentException("Cauldron state and interaction are required");
        }
    }
}

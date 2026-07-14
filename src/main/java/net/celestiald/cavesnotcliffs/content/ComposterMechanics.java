package net.celestiald.cavesnotcliffs.content;

/** Exact Java 1.18.2 composter level transitions, isolated from the 1.12 block API. */
public final class ComposterMechanics {
    public static final int EMPTY_LEVEL = 0;
    public static final int MAX_FILL_LEVEL = 7;
    public static final int READY_LEVEL = 8;
    public static final int READY_DELAY_TICKS = 20;

    private ComposterMechanics() {
    }

    /**
     * Returns the level after consuming one compostable item.
     *
     * <p>The first valid item always raises an empty composter. Later items use the configured
     * chance exactly as 1.18.2 does. A level-seven or ready composter does not accept input.</p>
     */
    public static int addItem(int level, float chance, double randomUnit) {
        requireLevel(level);
        requireChance(chance);
        requireRandomUnit(randomUnit);
        if (level >= MAX_FILL_LEVEL || chance < 0.0F) {
            return level;
        }
        return level == EMPTY_LEVEL && chance > 0.0F || randomUnit < chance
            ? level + 1 : level;
    }

    public static boolean acceptsInput(int level, float chance) {
        requireLevel(level);
        requireChance(chance);
        return level < MAX_FILL_LEVEL && chance >= 0.0F;
    }

    public static int mature(int level) {
        requireLevel(level);
        return level == MAX_FILL_LEVEL ? READY_LEVEL : level;
    }

    public static int comparatorOutput(int level) {
        return requireLevel(level);
    }

    public static int requireLevel(int level) {
        if (level < EMPTY_LEVEL || level > READY_LEVEL) {
            throw new IllegalArgumentException("Composter level must be 0..8: " + level);
        }
        return level;
    }

    private static void requireChance(float chance) {
        if (Float.isNaN(chance) || chance < -1.0F || chance > 1.0F) {
            throw new IllegalArgumentException("Compost chance must be -1 or in [0, 1]: "
                + chance);
        }
    }

    private static void requireRandomUnit(double randomUnit) {
        if (Double.isNaN(randomUnit) || randomUnit < 0.0D || randomUnit >= 1.0D) {
            throw new IllegalArgumentException("randomUnit must be in [0, 1): " + randomUnit);
        }
    }
}

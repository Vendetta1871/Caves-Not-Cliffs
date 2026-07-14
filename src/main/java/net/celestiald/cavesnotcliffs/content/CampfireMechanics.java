package net.celestiald.cavesnotcliffs.content;

/** Dependency-free Java 1.18.2 campfire state and ticker constants. */
public final class CampfireMechanics {
    public static final int SLOT_COUNT = 4;
    public static final int DEFAULT_COOKING_TIME = 600;
    public static final int BURN_COOL_SPEED = 2;
    public static final int NORMAL_FIRE_DAMAGE = 1;
    public static final int SOUL_FIRE_DAMAGE = 2;
    public static final int SMOKE_DISTANCE = 5;
    public static final int WATER_TICK_DELAY = 5;

    private static final int HORIZONTAL_MASK = 3;
    private static final int LIT_BIT = 4;
    private static final int WATERLOGGED_BIT = 8;

    private CampfireMechanics() {
    }

    /** Packs four facings, lit, and waterlogged into all sixteen legacy metadata values. */
    public static int metadata(int horizontalFacing, boolean lit, boolean waterlogged) {
        if (horizontalFacing < 0 || horizontalFacing > 3) {
            throw new IllegalArgumentException("horizontalFacing must be 0..3: "
                + horizontalFacing);
        }
        return horizontalFacing | (lit ? LIT_BIT : 0) | (waterlogged ? WATERLOGGED_BIT : 0);
    }

    public static int horizontalFacing(int metadata) {
        return metadata & HORIZONTAL_MASK;
    }

    public static boolean lit(int metadata) {
        return (metadata & LIT_BIT) != 0;
    }

    public static boolean waterlogged(int metadata) {
        return (metadata & WATERLOGGED_BIT) != 0;
    }

    public static boolean canLight(boolean lit, boolean waterlogged) {
        return !lit && !waterlogged;
    }

    public static boolean placementLit(boolean waterlogged) {
        return !waterlogged;
    }

    /** Unlit campfires cool twice per tick without dropping below zero. */
    public static int coolProgress(int progress) {
        return coolProgress(progress, Integer.MAX_VALUE);
    }

    /** Mirrors 1.18.2's clamp so malformed or upgraded NBT cannot exceed total cook time. */
    public static int coolProgress(int progress, int totalCookingTime) {
        return Math.max(0, Math.min(totalCookingTime, progress - BURN_COOL_SPEED));
    }

    public static int fireDamage(boolean soul) {
        return soul ? SOUL_FIRE_DAMAGE : NORMAL_FIRE_DAMAGE;
    }
}

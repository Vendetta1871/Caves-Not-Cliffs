package net.celestiald.cavesnotcliffs.content;

/** Fire-aware hive selection rules that prevent a selected-hive retry deadlock. */
public final class BeeHiveRouting {
    public static final int RETRY_COOLDOWN_AFTER_FIRE = 0;

    private BeeHiveRouting() {
    }

    public static boolean shouldAbandonSelectedHive(boolean hasSelectedHive,
            boolean fireNearby) {
        return hasSelectedHive && fireNearby;
    }

    public static boolean canSelectHive(boolean full, boolean fireNearby) {
        return !full && !fireNearby;
    }
}

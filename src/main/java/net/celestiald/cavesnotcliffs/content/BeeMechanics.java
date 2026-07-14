package net.celestiald.cavesnotcliffs.content;

import net.minecraft.world.EnumDifficulty;

import java.util.Random;

/**
 * Dependency-light Java 1.18.2 bee and hive constants.
 *
 * <p>The runtime adaptation deliberately routes every numeric decision through this class. That
 * keeps 1.12 AI/API compromises from changing the behavioral oracle.</p>
 */
public final class BeeMechanics {
    public static final int MAX_HIVE_OCCUPANTS = 3;
    public static final int MIN_OCCUPATION_TICKS_NECTARLESS = 600;
    public static final int MIN_OCCUPATION_TICKS_NECTAR = 2400;
    public static final int MIN_TICKS_BEFORE_REENTERING_HIVE = 400;
    public static final int STING_DEATH_COUNTDOWN = 1200;
    public static final int TICKS_BEFORE_GOING_TO_KNOWN_FLOWER = 2400;
    public static final int TICKS_WITHOUT_NECTAR_BEFORE_GOING_HOME = 3600;
    public static final int MIN_POLLINATION_TICKS = 400;
    public static final int MAX_POLLINATION_TICKS = 600;
    public static final int LOCATE_HIVE_COOLDOWN = 200;
    public static final int LOCATE_FLOWER_COOLDOWN = 200;
    public static final int HIVE_SEARCH_DISTANCE = 20;
    public static final int TOO_FAR_DISTANCE = 32;
    public static final int HIVE_CLOSE_ENOUGH_DISTANCE = 2;
    public static final int MAX_CROPS_GROWN = 10;
    public static final int MAX_HONEY_LEVEL = 5;

    private BeeMechanics() {
    }

    public static int minimumOccupationTicks(boolean hasNectar) {
        return hasNectar ? MIN_OCCUPATION_TICKS_NECTAR
                : MIN_OCCUPATION_TICKS_NECTARLESS;
    }

    /** The official ticker releases only after, not at, the minimum. */
    public static boolean occupationComplete(int ticksInHive, int minimumTicks) {
        return ticksInHive > minimumTicks;
    }

    /** Returns the honey increment, including the exact one-percent double delivery. */
    public static int honeyIncrement(int currentLevel, Random random) {
        requireHoneyLevel(currentLevel);
        if (currentLevel >= MAX_HONEY_LEVEL) {
            return 0;
        }
        int amount = random.nextInt(100) == 0 ? 2 : 1;
        return Math.min(amount, MAX_HONEY_LEVEL - currentLevel);
    }

    public static int nextAngerTime(Random random) {
        return (20 + random.nextInt(20)) * 20;
    }

    public static int poisonDurationTicks(EnumDifficulty difficulty) {
        if (difficulty == EnumDifficulty.NORMAL) {
            return 10 * 20;
        }
        if (difficulty == EnumDifficulty.HARD) {
            return 18 * 20;
        }
        return 0;
    }

    /** Called on every fifth tick after a sting, matching Bee#customServerAiStep. */
    public static boolean diesAfterSting(int timeSinceSting, Random random) {
        if (timeSinceSting <= 0 || timeSinceSting % 5 != 0) {
            return false;
        }
        int bound = clamp(STING_DEATH_COUNTDOWN - timeSinceSting,
                1, STING_DEATH_COUNTDOWN);
        return random.nextInt(bound) == 0;
    }

    public static boolean pollinatedLongEnough(int successfulTicks) {
        return successfulTicks > MIN_POLLINATION_TICKS;
    }

    public static boolean pollinationTimedOut(int totalTicks) {
        return totalTicks > MAX_POLLINATION_TICKS;
    }

    public static boolean wantsToEnterHive(int stayOutTicks, boolean pollinating,
            boolean hasStung, boolean hasTarget, int ticksWithoutNectar,
            boolean raining, boolean night, boolean hasNectar, boolean hiveNearFire) {
        if (stayOutTicks > 0 || pollinating || hasStung || hasTarget) {
            return false;
        }
        boolean wantsShelter = ticksWithoutNectar > TICKS_WITHOUT_NECTAR_BEFORE_GOING_HOME
                || raining || night || hasNectar;
        return wantsShelter && !hiveNearFire;
    }

    public static boolean canRelease(boolean emergency, boolean night,
            boolean raining, boolean exitBlocked) {
        if (!emergency && (night || raining)) {
            return false;
        }
        return emergency || !exitBlocked;
    }

    public static boolean canGrowCrop(int cropsGrown, float randomUnit,
            boolean hasNectar, boolean hiveValid) {
        return cropsGrown < MAX_CROPS_GROWN && randomUnit >= 0.3F
                && hasNectar && hiveValid;
    }

    public static int requireHoneyLevel(int level) {
        if (level < 0 || level > MAX_HONEY_LEVEL) {
            throw new IllegalArgumentException("Honey level must be 0..5: " + level);
        }
        return level;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

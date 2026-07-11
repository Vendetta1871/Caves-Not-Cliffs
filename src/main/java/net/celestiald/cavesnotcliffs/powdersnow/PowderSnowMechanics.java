package net.celestiald.cavesnotcliffs.powdersnow;

/**
 * Pure Java 1.18.2 powder-snow thresholds shared by runtime hooks and exhaustive tests.
 *
 * <p>Minecraft 1.12 has no entity-aware voxel-shape context and no synchronized frozen-tick
 * field. The Forge bridge supplies equivalent inputs, while all numeric decisions stay isolated
 * here so those unavoidable API seams cannot alter the canonical rules.</p>
 */
public final class PowderSnowMechanics {
    public static final int TICKS_REQUIRED_TO_FREEZE = 140;
    public static final int THAW_TICKS_PER_TICK = 2;
    public static final int FROZEN_DAMAGE_INTERVAL = 40;
    public static final int NORMAL_FROZEN_DAMAGE = 1;
    public static final int EXTRA_FROZEN_DAMAGE = 5;

    public static final float FALLING_COLLISION_THRESHOLD = 2.5F;
    public static final float FALL_SOUND_THRESHOLD = 4.0F;
    public static final float BIG_FALL_SOUND_THRESHOLD = 7.0F;
    public static final double FALLING_COLLISION_HEIGHT = 0.9D;

    public static final double HORIZONTAL_STUCK_MULTIPLIER = 0.9D;
    public static final double VERTICAL_STUCK_MULTIPLIER = 1.5D;
    public static final double SNOW_CAULDRON_FILL_CHANCE = 0.1D;

    private PowderSnowMechanics() {
    }

    public static int nextFrozenTicks(int currentTicks, boolean inPowderSnow,
            boolean canFreeze) {
        int current = clamp(currentTicks, 0, TICKS_REQUIRED_TO_FREEZE);
        if (inPowderSnow && canFreeze) {
            return Math.min(TICKS_REQUIRED_TO_FREEZE, current + 1);
        }
        return Math.max(0, current - THAW_TICKS_PER_TICK);
    }

    public static boolean isFullyFrozen(int frozenTicks) {
        return frozenTicks >= TICKS_REQUIRED_TO_FREEZE;
    }

    public static boolean shouldApplyFrozenDamage(int entityTick, int frozenTicks,
            boolean canFreeze) {
        return canFreeze && entityTick % FROZEN_DAMAGE_INTERVAL == 0
            && isFullyFrozen(frozenTicks);
    }

    public static int frozenDamage(boolean hurtsExtra) {
        return hurtsExtra ? EXTRA_FROZEN_DAMAGE : NORMAL_FROZEN_DAMAGE;
    }

    public static float frozenPercent(int frozenTicks) {
        return clamp(frozenTicks, 0, TICKS_REQUIRED_TO_FREEZE)
            / (float) TICKS_REQUIRED_TO_FREEZE;
    }

    public static double movementSpeedModifier(int frozenTicks) {
        return -0.05D * frozenPercent(frozenTicks);
    }

    public static CollisionShape collisionShape(float fallDistance, boolean fallingBlock,
            boolean canWalkOnPowderSnow, boolean entityBottomAboveBlock,
            boolean descending) {
        if (fallDistance > FALLING_COLLISION_THRESHOLD) {
            return CollisionShape.FALLING;
        }
        if (fallingBlock || canWalkOnPowderSnow && entityBottomAboveBlock && !descending) {
            return CollisionShape.FULL;
        }
        return CollisionShape.NONE;
    }

    public static boolean shouldPlayFallSound(float fallDistance, boolean livingEntity) {
        return livingEntity && fallDistance >= FALL_SOUND_THRESHOLD;
    }

    public static boolean shouldPlayBigFallSound(float fallDistance) {
        return fallDistance >= BIG_FALL_SOUND_THRESHOLD;
    }

    public static boolean shouldFillFromSnow(double randomUnit) {
        if (randomUnit < 0.0D || randomUnit >= 1.0D) {
            throw new IllegalArgumentException("randomUnit must be in [0, 1): " + randomUnit);
        }
        return randomUnit < SNOW_CAULDRON_FILL_CHANCE;
    }

    public static int nextPowderSnowCauldronLevel(int level, boolean snowing,
            double randomUnit) {
        int checkedLevel = requireCauldronLevel(level);
        return snowing && checkedLevel < 3 && shouldFillFromSnow(randomUnit)
            ? checkedLevel + 1 : checkedLevel;
    }

    public static int waterLevelAfterExtinguishing(int powderSnowLevel) {
        return requireCauldronLevel(powderSnowLevel) - 1;
    }

    public static double cauldronContentHeight(int level) {
        return (6.0D + requireCauldronLevel(level) * 3.0D) / 16.0D;
    }

    public static int requireCauldronLevel(int level) {
        if (level < 1 || level > 3) {
            throw new IllegalArgumentException("Powder-snow cauldron level must be 1..3: "
                + level);
        }
        return level;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum CollisionShape {
        NONE,
        FALLING,
        FULL
    }
}

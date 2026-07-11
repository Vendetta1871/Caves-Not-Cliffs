package net.celestiald.cavesnotcliffs.content;

/** Dependency-free Java 1.18.2 honey-block physics oracle. */
public final class HoneyBlockMechanics {
    public static final double SLIDE_START_SPEED = -0.08D;
    public static final double FAST_SLIDE_SPEED = -0.13D;
    public static final double THROTTLED_SLIDE_SPEED = -0.05D;
    public static final double COLLISION_TOP = 0.9375D;
    public static final double COLLISION_HALF_WIDTH = 0.4375D;
    public static final float FALL_DAMAGE_MULTIPLIER = 0.2F;
    public static final double SPEED_FACTOR = 0.4D;
    public static final double JUMP_FACTOR = 0.5D;

    private HoneyBlockMechanics() {
    }

    public static boolean isSliding(boolean onGround, double entityY, int blockY,
            double motionY, double deltaX, double deltaZ, float entityWidth) {
        if (onGround || entityY > blockY + COLLISION_TOP - 1.0E-7D
                || motionY >= SLIDE_START_SPEED) {
            return false;
        }
        double threshold = COLLISION_HALF_WIDTH + entityWidth / 2.0D;
        return Math.abs(deltaX) + 1.0E-7D > threshold
                || Math.abs(deltaZ) + 1.0E-7D > threshold;
    }

    public static Velocity slide(double motionX, double motionY, double motionZ) {
        if (motionY < FAST_SLIDE_SPEED) {
            double scale = THROTTLED_SLIDE_SPEED / motionY;
            return new Velocity(motionX * scale, THROTTLED_SLIDE_SPEED,
                    motionZ * scale);
        }
        return new Velocity(motionX, THROTTLED_SLIDE_SPEED, motionZ);
    }

    public static final class Velocity {
        public final double x;
        public final double y;
        public final double z;

        Velocity(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}

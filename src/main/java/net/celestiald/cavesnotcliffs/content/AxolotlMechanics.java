package net.celestiald.cavesnotcliffs.content;

import java.util.Random;

/** Dependency-free Java 1.18.2 axolotl constants and state transitions. */
public final class AxolotlMechanics {
    public static final int TOTAL_PLAY_DEAD_TIME = 200;
    public static final int RARE_VARIANT_CHANCE = 1200;
    public static final int MAX_AIR_SUPPLY = 6000;
    public static final int REHYDRATE_AIR_SUPPLY = 1800;
    public static final int REGENERATION_BASE_DURATION = 100;
    public static final int REGENERATION_MAX_DURATION = 2400;
    public static final int HUNTING_COOLDOWN = 2400;
    public static final double SUPPORT_RANGE = 20.0D;
    public static final double TARGET_RANGE = 8.0D;

    private AxolotlMechanics() {
    }

    public enum Variant {
        LUCY(0, "lucy", true),
        WILD(1, "wild", true),
        GOLD(2, "gold", true),
        CYAN(3, "cyan", true),
        BLUE(4, "blue", false);

        private static final Variant[] BY_ID = values();
        private static final Variant[] COMMON = {LUCY, WILD, GOLD, CYAN};
        private final int id;
        private final String name;
        private final boolean common;

        Variant(int id, String name, boolean common) {
            this.id = id;
            this.name = name;
            this.common = common;
        }

        public int id() {
            return id;
        }

        public String serializedName() {
            return name;
        }

        public boolean isCommon() {
            return common;
        }

        public static Variant byId(int id) {
            return id >= 0 && id < BY_ID.length ? BY_ID[id] : LUCY;
        }

        public static Variant common(Random random) {
            return COMMON[random.nextInt(COMMON.length)];
        }
    }

    /** Java 1.18.2 breeding order: the rare roll precedes the parent-selection roll. */
    public static Variant childVariant(Variant firstParent, Variant secondParent, Random random) {
        if (random.nextInt(RARE_VARIANT_CHANCE) == 0) {
            return Variant.BLUE;
        }
        return random.nextBoolean() ? firstParent : secondParent;
    }

    /** Exact play-dead eligibility and RNG consumption from Axolotl#hurt. */
    public static boolean shouldPlayDead(Random random, float damage, float health,
            float maxHealth, boolean inWater, boolean hasAttacker, boolean alreadyPlayingDead) {
        return random.nextInt(3) == 0
                && (random.nextInt(3) < damage || health / maxHealth < 0.5F)
                && damage < health
                && inWater
                && hasAttacker
                && !alreadyPlayingDead;
    }

    public static int rehydratedAir(int currentAir) {
        return Math.min(MAX_AIR_SUPPLY, currentAir + REHYDRATE_AIR_SUPPLY);
    }

    public static int regenerationDuration(int currentDuration) {
        return Math.min(REGENERATION_MAX_DURATION,
                Math.max(0, currentDuration) + REGENERATION_BASE_DURATION);
    }

    /** The 1.18.2 axolotl spawn tag contains exactly clay. */
    public static boolean isSpawnableSurface(String registryName) {
        return "minecraft:clay".equals(registryName);
    }
}

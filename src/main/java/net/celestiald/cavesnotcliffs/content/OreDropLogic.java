package net.celestiald.cavesnotcliffs.content;

import java.util.Random;

/** Java 1.18.2 loot-function equivalents used by ore blocks on the 1.12 loot API. */
public final class OreDropLogic {
    private OreDropLogic() {
    }

    /** Equivalent to the 1.18.2 {@code minecraft:ore_drops} Fortune formula. */
    public static int applyOreBonus(int baseCount, int fortune, Random random) {
        if (fortune <= 0) {
            return baseCount;
        }
        int multiplier = random.nextInt(fortune + 2) - 1;
        return baseCount * (Math.max(0, multiplier) + 1);
    }

    /** Equivalent to {@code uniform_bonus_count} with a bonus multiplier of one. */
    public static int applyUniformBonus(int baseCount, int fortune, Random random) {
        return fortune <= 0 ? baseCount : baseCount + random.nextInt(fortune + 1);
    }
}

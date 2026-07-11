package net.celestiald.cavesnotcliffs.content;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Numeric Java 1.18.2 lush-cave contracts shared by the runtime blocks and oracle tests.
 *
 * <p>Java 1.12 stores at most sixteen metadata values per block. Cave-vine heads therefore use
 * four hidden age bands while retaining the exact public 0..25 age and berries state. Big
 * dripleaf heads use a second hidden block for waterlogged storage; their dry metadata remains
 * the complete four-facing by four-tilt matrix.</p>
 */
public final class LushCaveMechanics {
    public static final int CAVE_VINE_MAX_AGE = 25;
    public static final double CAVE_VINE_GROWTH_CHANCE = 0.10D;
    public static final float CAVE_VINE_BERRY_GROWTH_CHANCE = 0.11F;
    public static final int CAVE_VINE_LIGHT = 14;

    public static final float AZALEA_BONEMEAL_SUCCESS_CHANCE = 0.45F;
    public static final int BIG_DRIPLEAF_MIN_GENERATED_HEIGHT = 2;
    public static final int BIG_DRIPLEAF_MAX_GENERATED_HEIGHT = 5;

    public static final int MOSS_BONEMEAL_MIN_RADIUS = 1;
    public static final int MOSS_BONEMEAL_MAX_RADIUS = 2;
    public static final int MOSS_BONEMEAL_VERTICAL_RANGE = 5;
    public static final float MOSS_BONEMEAL_EDGE_CHANCE = 0.75F;
    public static final float MOSS_BONEMEAL_VEGETATION_CHANCE = 0.60F;

    public static final String CAVE_VINES = "cave_vines";
    public static final String CAVE_VINES_AGE_8_15 = "cave_vines_age_8_15";
    public static final String CAVE_VINES_AGE_16_23 = "cave_vines_age_16_23";
    public static final String CAVE_VINES_AGE_24_25 = "cave_vines_age_24_25";
    public static final String CAVE_VINES_PLANT = "cave_vines_plant";

    private static final List<String> HEAD_PATHS = Collections.unmodifiableList(Arrays.asList(
            CAVE_VINES,
            CAVE_VINES_AGE_8_15,
            CAVE_VINES_AGE_16_23,
            CAVE_VINES_AGE_24_25));

    private LushCaveMechanics() {
    }

    public enum Tilt {
        NONE("none", -1),
        UNSTABLE("unstable", 10),
        PARTIAL("partial", 10),
        FULL("full", 100);

        private final String name;
        private final int delay;

        Tilt(String name, int delay) {
            this.name = name;
            this.delay = delay;
        }

        public String getName() {
            return name;
        }

        public int getDelay() {
            return delay;
        }

        public Tilt next() {
            switch (this) {
                case NONE:
                    return UNSTABLE;
                case UNSTABLE:
                    return PARTIAL;
                case PARTIAL:
                    return FULL;
                case FULL:
                default:
                    return NONE;
            }
        }
    }

    public static List<String> caveVineHeadPaths() {
        return HEAD_PATHS;
    }

    public static boolean isCaveVineHead(String path) {
        return HEAD_PATHS.contains(path);
    }

    public static boolean isCaveVine(String path) {
        return CAVE_VINES_PLANT.equals(path) || isCaveVineHead(path);
    }

    public static String caveVineHeadPath(int age) {
        requireAge(age);
        if (age < 8) {
            return CAVE_VINES;
        }
        if (age < 16) {
            return CAVE_VINES_AGE_8_15;
        }
        if (age < 24) {
            return CAVE_VINES_AGE_16_23;
        }
        return CAVE_VINES_AGE_24_25;
    }

    public static int caveVineHeadMeta(int age, boolean berries) {
        requireAge(age);
        int localAge = age < 24 ? age & 7 : age - 24;
        return localAge | (berries ? 8 : 0);
    }

    public static int caveVineHeadAge(String path, int metadata) {
        int local = metadata & 7;
        if (CAVE_VINES.equals(path)) {
            return local;
        }
        if (CAVE_VINES_AGE_8_15.equals(path)) {
            return 8 + local;
        }
        if (CAVE_VINES_AGE_16_23.equals(path)) {
            return 16 + local;
        }
        if (CAVE_VINES_AGE_24_25.equals(path) && local <= 1) {
            return 24 + local;
        }
        throw new IllegalArgumentException("Invalid cave-vine head state: " + path
                + '#' + metadata);
    }

    public static boolean caveVineHasBerries(int metadata) {
        return (metadata & 8) != 0;
    }

    public static int caveVinePlantMeta(boolean berries) {
        return berries ? 1 : 0;
    }

    public static boolean caveVinePlantHasBerries(int metadata) {
        return (metadata & 1) != 0;
    }

    public static int bigDripleafMeta(int horizontalFacing, Tilt tilt) {
        requireFacing(horizontalFacing);
        if (tilt == null) {
            throw new IllegalArgumentException("Big-dripleaf tilt is required");
        }
        return horizontalFacing | tilt.ordinal() << 2;
    }

    public static int bigDripleafFacing(int metadata) {
        return metadata & 3;
    }

    public static Tilt bigDripleafTilt(int metadata) {
        return Tilt.values()[metadata >> 2 & 3];
    }

    public static int bigDripleafStemMeta(int horizontalFacing, boolean waterlogged) {
        requireFacing(horizontalFacing);
        return horizontalFacing | (waterlogged ? 4 : 0);
    }

    public static int smallDripleafMeta(int horizontalFacing, boolean upper,
            boolean waterlogged) {
        requireFacing(horizontalFacing);
        return horizontalFacing | (upper ? 4 : 0) | (waterlogged ? 8 : 0);
    }

    public static int dripleafFacing(int metadata) {
        return metadata & 3;
    }

    public static boolean dripleafUpper(int metadata) {
        return (metadata & 4) != 0;
    }

    public static boolean dripleafWaterlogged(int metadata) {
        return (metadata & 8) != 0;
    }

    public static int generatedBigDripleafHeight(java.util.Random random) {
        return BIG_DRIPLEAF_MIN_GENERATED_HEIGHT
                + random.nextInt(BIG_DRIPLEAF_MAX_GENERATED_HEIGHT
                - BIG_DRIPLEAF_MIN_GENERATED_HEIGHT + 1);
    }

    private static void requireAge(int age) {
        if (age < 0 || age > CAVE_VINE_MAX_AGE) {
            throw new IllegalArgumentException("Cave-vine age must be 0..25: " + age);
        }
    }

    private static void requireFacing(int horizontalFacing) {
        if (horizontalFacing < 0 || horizontalFacing > 3) {
            throw new IllegalArgumentException("Horizontal facing must be 0..3: "
                    + horizontalFacing);
        }
    }
}

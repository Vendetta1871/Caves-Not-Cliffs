package net.celestiald.cavesnotcliffs.content;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Dependency-light Java 1.18.2 candle state and content oracle.
 *
 * <p>A 1.12 block has exactly sixteen metadata values, which is enough to retain every public
 * candle state: four candle counts, lit/unlit, and waterlogged/dry. Keeping the packing here makes
 * placement, lighting, drops, models, and save data agree on one stable representation.</p>
 */
public final class CandleMechanics {
    public static final int MIN_CANDLES = 1;
    public static final int MAX_CANDLES = 4;
    public static final int LIGHT_PER_CANDLE = 3;
    public static final int FULL_CAKE_COMPARATOR_SIGNAL = 14;

    /** Canonical public candle families in vanilla dye order. */
    public enum Color {
        UNCOLORED("candle", -1),
        WHITE("white_candle", 0),
        ORANGE("orange_candle", 1),
        MAGENTA("magenta_candle", 2),
        LIGHT_BLUE("light_blue_candle", 3),
        YELLOW("yellow_candle", 4),
        LIME("lime_candle", 5),
        PINK("pink_candle", 6),
        GRAY("gray_candle", 7),
        LIGHT_GRAY("light_gray_candle", 8),
        CYAN("cyan_candle", 9),
        PURPLE("purple_candle", 10),
        BLUE("blue_candle", 11),
        BROWN("brown_candle", 12),
        GREEN("green_candle", 13),
        RED("red_candle", 14),
        BLACK("black_candle", 15);

        private final String candlePath;
        private final int dyeMetadata;

        Color(String candlePath, int dyeMetadata) {
            this.candlePath = candlePath;
            this.dyeMetadata = dyeMetadata;
        }

        public String getCandlePath() {
            return candlePath;
        }

        public String getCandleCakePath() {
            return candlePath + "_cake";
        }

        /** 1.12 {@code EnumDyeColor#getMetadata}; uncolored candles return {@code -1}. */
        public int getDyeMetadata() {
            return dyeMetadata;
        }

        public boolean isDyed() {
            return dyeMetadata >= 0;
        }
    }

    /** Immutable decoded state used by both runtime and tests. */
    public static final class State {
        private final int candles;
        private final boolean lit;
        private final boolean waterlogged;

        private State(int candles, boolean lit, boolean waterlogged) {
            this.candles = candles;
            this.lit = lit;
            this.waterlogged = waterlogged;
        }

        public int getCandles() {
            return candles;
        }

        public boolean isLit() {
            return lit;
        }

        public boolean isWaterlogged() {
            return waterlogged;
        }
    }

    /** Normalized block-space flame location copied from {@code CandleBlock}. */
    public static final class ParticleOffset {
        public final double x;
        public final double y;
        public final double z;

        private ParticleOffset(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static final List<Color> COLORS = Collections.unmodifiableList(
            Arrays.asList(Color.values()));
    private static final List<List<ParticleOffset>> PARTICLE_OFFSETS =
            Collections.unmodifiableList(Arrays.asList(
                    Collections.<ParticleOffset>emptyList(),
                    immutableOffsets(offset(0.5D, 0.5D, 0.5D)),
                    immutableOffsets(offset(0.375D, 0.44D, 0.5D),
                            offset(0.625D, 0.5D, 0.44D)),
                    immutableOffsets(offset(0.5D, 0.313D, 0.625D),
                            offset(0.375D, 0.44D, 0.5D),
                            offset(0.56D, 0.5D, 0.44D)),
                    immutableOffsets(offset(0.44D, 0.313D, 0.56D),
                            offset(0.625D, 0.44D, 0.56D),
                            offset(0.375D, 0.44D, 0.375D),
                            offset(0.56D, 0.5D, 0.375D))));

    private CandleMechanics() {
    }

    public static List<Color> colors() {
        return COLORS;
    }

    /** Packs count in bits 0..1, lit in bit 2, and waterlogged in bit 3. */
    public static int metadata(int candles, boolean lit, boolean waterlogged) {
        requireCandleCount(candles);
        return candles - 1 | (lit ? 4 : 0) | (waterlogged ? 8 : 0);
    }

    public static State stateFromMetadata(int metadata) {
        if (metadata < 0 || metadata > 15) {
            throw new IllegalArgumentException("Candle metadata must be 0..15: " + metadata);
        }
        return new State((metadata & 3) + 1, (metadata & 4) != 0,
                (metadata & 8) != 0);
    }

    /** Water always extinguishes a candle before the new state is stored. */
    public static int waterlog(int metadata) {
        State state = stateFromMetadata(metadata);
        return metadata(state.candles, false, true);
    }

    public static int lightLevel(int candles, boolean lit, boolean waterlogged) {
        requireCandleCount(candles);
        return lit && !waterlogged ? LIGHT_PER_CANDLE * candles : 0;
    }

    public static boolean canLight(boolean lit, boolean waterlogged) {
        return !lit && !waterlogged;
    }

    public static boolean canStack(boolean sameCandleItem, boolean secondaryUseActive,
            int candles) {
        requireCandleCount(candles);
        return sameCandleItem && !secondaryUseActive && candles < MAX_CANDLES;
    }

    public static int stackedCount(int candles) {
        requireCandleCount(candles);
        if (candles == MAX_CANDLES) {
            throw new IllegalStateException("A candle stack cannot exceed four");
        }
        return candles + 1;
    }

    public static List<ParticleOffset> particleOffsets(int candles) {
        requireCandleCount(candles);
        return PARTICLE_OFFSETS.get(candles);
    }

    public static ParticleOffset cakeParticleOffset() {
        return offset(0.5D, 1.0D, 0.5D);
    }

    /** Candle cakes extinguish only when the empty-hand hit lands above the cake body. */
    public static boolean extinguishesCandleCake(boolean emptyHand, boolean lit,
            double hitY) {
        return emptyHand && lit && hitY > 0.5D;
    }

    /** A candle can be inserted only into the untouched, zero-bite cake state. */
    public static boolean canInsertIntoCake(int bites) {
        return bites == 0;
    }

    private static int requireCandleCount(int candles) {
        if (candles < MIN_CANDLES || candles > MAX_CANDLES) {
            throw new IllegalArgumentException("Candle count must be 1..4: " + candles);
        }
        return candles;
    }

    private static ParticleOffset offset(double x, double y, double z) {
        return new ParticleOffset(x, y, z);
    }

    private static List<ParticleOffset> immutableOffsets(ParticleOffset... offsets) {
        return Collections.unmodifiableList(Arrays.asList(offsets));
    }
}

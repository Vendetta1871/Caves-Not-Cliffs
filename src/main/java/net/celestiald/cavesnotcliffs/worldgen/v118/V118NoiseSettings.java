package net.celestiald.cavesnotcliffs.worldgen.v118;

/** Immutable Java 1.18.2 Overworld density settings for normal and amplified profiles. */
public final class V118NoiseSettings {
    public static final int OVERWORLD_MIN_Y = -64;
    public static final int OVERWORLD_HEIGHT = 384;

    private final int minY;
    private final int height;
    private final NoiseSamplingSettings sampling;
    private final NoiseSlider topSlide;
    private final NoiseSlider bottomSlide;
    private final int noiseSizeHorizontal;
    private final int noiseSizeVertical;
    private final boolean amplified;

    private V118NoiseSettings(int minY, int height, NoiseSamplingSettings sampling,
            NoiseSlider topSlide, NoiseSlider bottomSlide, int noiseSizeHorizontal,
            int noiseSizeVertical, boolean amplified) {
        if (height < 0 || height % 16 != 0) {
            throw new IllegalArgumentException("height must be a non-negative multiple of 16");
        }
        if (minY % 16 != 0) {
            throw new IllegalArgumentException("minY must be a multiple of 16");
        }
        if (noiseSizeHorizontal < 1 || noiseSizeHorizontal > 4
                || noiseSizeVertical < 1 || noiseSizeVertical > 4) {
            throw new IllegalArgumentException("noise sizes must be in the range 1..4");
        }
        this.minY = minY;
        this.height = height;
        this.sampling = sampling;
        this.topSlide = topSlide;
        this.bottomSlide = bottomSlide;
        this.noiseSizeHorizontal = noiseSizeHorizontal;
        this.noiseSizeVertical = noiseSizeVertical;
        this.amplified = amplified;
    }

    public static V118NoiseSettings overworld(boolean amplified) {
        return new V118NoiseSettings(OVERWORLD_MIN_Y, OVERWORLD_HEIGHT,
            new NoiseSamplingSettings(1.0D, 1.0D, 80.0D, 160.0D),
            new NoiseSlider(-0.078125D, 2, amplified ? 0 : 8),
            new NoiseSlider(amplified ? 0.4D : 0.1171875D, 3, 0),
            1, 2, amplified);
    }

    public double applySlide(double value, double blockY) {
        double cellY = (int) blockY / getCellHeight() - getMinCellY();
        value = topSlide.applySlide(value, getCellCountY() - cellY);
        return bottomSlide.applySlide(value, cellY);
    }

    public int minY() {
        return minY;
    }

    public int height() {
        return height;
    }

    public int maxYExclusive() {
        return minY + height;
    }

    public NoiseSamplingSettings sampling() {
        return sampling;
    }

    public NoiseSlider topSlide() {
        return topSlide;
    }

    public NoiseSlider bottomSlide() {
        return bottomSlide;
    }

    public int noiseSizeHorizontal() {
        return noiseSizeHorizontal;
    }

    public int noiseSizeVertical() {
        return noiseSizeVertical;
    }

    public boolean amplified() {
        return amplified;
    }

    public int getCellHeight() {
        return noiseSizeVertical * 4;
    }

    public int getCellWidth() {
        return noiseSizeHorizontal * 4;
    }

    public int getCellCountY() {
        return height / getCellHeight();
    }

    public int getMinCellY() {
        return Math.floorDiv(minY, getCellHeight());
    }
}

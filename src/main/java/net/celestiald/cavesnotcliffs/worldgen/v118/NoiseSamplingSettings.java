package net.celestiald.cavesnotcliffs.worldgen.v118;

/** Minecraft 1.18.2's four base-density sampling scales. */
public final class NoiseSamplingSettings {
    private final double xzScale;
    private final double yScale;
    private final double xzFactor;
    private final double yFactor;

    public NoiseSamplingSettings(double xzScale, double yScale,
            double xzFactor, double yFactor) {
        this.xzScale = xzScale;
        this.yScale = yScale;
        this.xzFactor = xzFactor;
        this.yFactor = yFactor;
    }

    public double xzScale() {
        return xzScale;
    }

    public double yScale() {
        return yScale;
    }

    public double xzFactor() {
        return xzFactor;
    }

    public double yFactor() {
        return yFactor;
    }
}

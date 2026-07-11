package net.celestiald.cavesnotcliffs.worldgen.v118;

/** Exact Java 1.18.2 top/bottom density slide. */
public final class NoiseSlider {
    private final double target;
    private final int size;
    private final int offset;

    public NoiseSlider(double target, int size, int offset) {
        this.target = target;
        this.size = size;
        this.offset = offset;
    }

    public double applySlide(double value, double position) {
        if (size <= 0) {
            return value;
        }
        double delta = (position - offset) / size;
        return clampedLerp(target, value, delta);
    }

    public double target() {
        return target;
    }

    public int size() {
        return size;
    }

    public int offset() {
        return offset;
    }

    private static double clampedLerp(double start, double end, double delta) {
        if (delta < 0.0D) {
            return start;
        }
        if (delta > 1.0D) {
            return end;
        }
        return WorldgenMath.lerp(delta, start, end);
    }
}

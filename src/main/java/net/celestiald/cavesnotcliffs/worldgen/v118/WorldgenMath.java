package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.function.IntPredicate;

/** Small, dependency-free subset of 1.18.2 Mth used by the world-generation primitives. */
public final class WorldgenMath {
    private WorldgenMath() {
    }

    public static int floor(double value) {
        int truncated = (int) value;
        return value < truncated ? truncated - 1 : truncated;
    }

    public static long lfloor(double value) {
        long truncated = (long) value;
        return value < truncated ? truncated - 1L : truncated;
    }

    public static long getSeed(int x, int y, int z) {
        long seed = (long) (x * 3129871) ^ (long) z * 116129781L ^ (long) y;
        seed = seed * seed * 42317861L + seed * 11L;
        return seed >> 16;
    }

    public static int binarySearch(int min, int max, IntPredicate predicate) {
        int length = max - min;
        while (length > 0) {
            int half = length / 2;
            int middle = min + half;
            if (predicate.test(middle)) {
                length = half;
            } else {
                min = middle + 1;
                length -= half + 1;
            }
        }
        return min;
    }

    public static float lerp(float delta, float start, float end) {
        return start + delta * (end - start);
    }

    public static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }

    public static double clampedLerp(double start, double end, double delta) {
        if (delta < 0.0D) {
            return start;
        }
        if (delta > 1.0D) {
            return end;
        }
        return lerp(delta, start, end);
    }

    public static double lerp2(double deltaX, double deltaY,
            double x0y0, double x1y0, double x0y1, double x1y1) {
        return lerp(deltaY, lerp(deltaX, x0y0, x1y0), lerp(deltaX, x0y1, x1y1));
    }

    public static double lerp3(double deltaX, double deltaY, double deltaZ,
            double x0y0z0, double x1y0z0, double x0y1z0, double x1y1z0,
            double x0y0z1, double x1y0z1, double x0y1z1, double x1y1z1) {
        return lerp(deltaZ,
            lerp2(deltaX, deltaY, x0y0z0, x1y0z0, x0y1z0, x1y1z0),
            lerp2(deltaX, deltaY, x0y0z1, x1y0z1, x0y1z1, x1y1z1));
    }

    public static double smoothstep(double value) {
        return value * value * value * (value * (value * 6.0D - 15.0D) + 10.0D);
    }

    public static double smoothstepDerivative(double value) {
        return 30.0D * value * value * (value - 1.0D) * (value - 1.0D);
    }

    public static double square(double value) {
        return value * value;
    }

    public static long square(long value) {
        return value * value;
    }

    public static int clamp(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }

    public static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }

    public static double clamp(double value, double min, double max) {
        return value < min ? min : Math.min(value, max);
    }
}

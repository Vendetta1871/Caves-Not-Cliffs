package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.Random;

/** Shared, dependency-free subset of Java 1.18.2's configured-carver contract. */
public abstract class V118CarverConfiguration {
    private final float probability;
    private final int minimumY;
    private final int maximumY;
    private final float minimumYScale;
    private final float maximumYScale;
    private final int lavaLevel;

    V118CarverConfiguration(float probability, int minimumY, int maximumY,
            float minimumYScale, float maximumYScale, int lavaLevel) {
        if (probability < 0.0F || probability > 1.0F) {
            throw new IllegalArgumentException("probability outside [0, 1]: " + probability);
        }
        if (minimumY > maximumY) {
            throw new IllegalArgumentException("empty carver height range: " + minimumY
                + ".." + maximumY);
        }
        if (maximumYScale < minimumYScale) {
            throw new IllegalArgumentException("empty Y-scale range: " + minimumYScale
                + ".." + maximumYScale);
        }
        this.probability = probability;
        this.minimumY = minimumY;
        this.maximumY = maximumY;
        this.minimumYScale = minimumYScale;
        this.maximumYScale = maximumYScale;
        this.lavaLevel = lavaLevel;
    }

    public final float probability() {
        return probability;
    }

    public final int minimumY() {
        return minimumY;
    }

    public final int maximumY() {
        return maximumY;
    }

    public final float minimumYScale() {
        return minimumYScale;
    }

    public final float maximumYScale() {
        return maximumYScale;
    }

    public final int lavaLevel() {
        return lavaLevel;
    }

    final int sampleY(Random random) {
        return random.nextInt(maximumY - minimumY + 1) + minimumY;
    }

    final float sampleYScale(Random random) {
        return uniform(random, minimumYScale, maximumYScale);
    }

    static float uniform(Random random, float minimum, float maximum) {
        if (minimum == maximum) {
            return minimum;
        }
        return random.nextFloat() * (maximum - minimum) + minimum;
    }

    static float trapezoid(Random random, float minimum, float maximum, float plateau) {
        float span = maximum - minimum;
        float slope = (span - plateau) / 2.0F;
        float upper = span - slope;
        return minimum + random.nextFloat() * upper + random.nextFloat() * slope;
    }
}

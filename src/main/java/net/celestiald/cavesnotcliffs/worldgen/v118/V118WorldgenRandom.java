package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.Random;

/**
 * Java 1.18.2 {@code WorldgenRandom} using its xoroshiro source and legacy decoration seeding.
 *
 * <p>Feature code consumes the inherited {@link Random} methods. Those methods intentionally use
 * Java's bounded-int rejection algorithm over {@link #next(int)}, not xoroshiro's direct bounded
 * sampler.</p>
 */
public final class V118WorldgenRandom extends Random {
    private static final long serialVersionUID = 1L;

    private XoroshiroRandomSource source;

    public V118WorldgenRandom(long seed) {
        super(0L);
        source = new XoroshiroRandomSource(seed);
    }

    @Override
    protected int next(int bits) {
        return (int) (source.nextLong() >>> 64 - bits);
    }

    @Override
    public synchronized void setSeed(long seed) {
        if (source != null) {
            source.setSeed(seed);
        }
    }

    public long setDecorationSeed(long worldSeed, int blockX, int blockZ) {
        setSeed(worldSeed);
        long xMultiplier = nextLong() | 1L;
        long zMultiplier = nextLong() | 1L;
        long decorationSeed = (long) blockX * xMultiplier + (long) blockZ * zMultiplier
            ^ worldSeed;
        setSeed(decorationSeed);
        return decorationSeed;
    }

    public void setFeatureSeed(long decorationSeed, int featureIndex, int decorationStep) {
        setSeed(decorationSeed + featureIndex + 10000L * decorationStep);
    }
}

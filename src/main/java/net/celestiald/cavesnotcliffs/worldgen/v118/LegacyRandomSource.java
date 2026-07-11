package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.Random;

/** Java 1.18.2's 48-bit legacy random source and positional factory. */
public final class LegacyRandomSource implements RandomSource {
    private final Random random;

    public LegacyRandomSource(long seed) {
        random = new Random(seed);
    }

    @Override
    public RandomSource fork() {
        return new LegacyRandomSource(nextLong());
    }

    @Override
    public PositionalRandomFactory forkPositional() {
        return new LegacyPositionalRandomFactory(nextLong());
    }

    @Override
    public void setSeed(long seed) {
        random.setSeed(seed);
    }

    @Override
    public int nextInt() {
        return random.nextInt();
    }

    @Override
    public int nextInt(int bound) {
        return random.nextInt(bound);
    }

    @Override
    public long nextLong() {
        return random.nextLong();
    }

    @Override
    public boolean nextBoolean() {
        return random.nextBoolean();
    }

    @Override
    public float nextFloat() {
        return random.nextFloat();
    }

    @Override
    public double nextDouble() {
        return random.nextDouble();
    }

    @Override
    public double nextGaussian() {
        return random.nextGaussian();
    }

    private static final class LegacyPositionalRandomFactory
            implements PositionalRandomFactory {
        private final long seed;

        private LegacyPositionalRandomFactory(long seed) {
            this.seed = seed;
        }

        @Override
        public RandomSource at(int x, int y, int z) {
            return new LegacyRandomSource(WorldgenMath.getSeed(x, y, z) ^ seed);
        }

        @Override
        public RandomSource fromHashOf(String name) {
            if (name == null) {
                throw new NullPointerException("name");
            }
            return new LegacyRandomSource((long) name.hashCode() ^ seed);
        }

        @Override
        public void appendParityConfigString(StringBuilder builder) {
            builder.append("LegacyPositionalRandomFactory{").append(seed).append('}');
        }
    }
}

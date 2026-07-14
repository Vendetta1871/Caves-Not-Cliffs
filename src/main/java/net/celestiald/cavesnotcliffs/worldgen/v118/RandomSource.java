package net.celestiald.cavesnotcliffs.worldgen.v118;

/** Dependency-free equivalent of Minecraft 1.18.2's world-generation random contract. */
public interface RandomSource {
    RandomSource fork();

    PositionalRandomFactory forkPositional();

    void setSeed(long seed);

    int nextInt();

    int nextInt(int bound);

    default int nextIntBetweenInclusive(int min, int max) {
        return nextInt(max - min + 1) + min;
    }

    long nextLong();

    boolean nextBoolean();

    float nextFloat();

    double nextDouble();

    double nextGaussian();

    default void consumeCount(int count) {
        for (int i = 0; i < count; ++i) {
            nextInt();
        }
    }
}

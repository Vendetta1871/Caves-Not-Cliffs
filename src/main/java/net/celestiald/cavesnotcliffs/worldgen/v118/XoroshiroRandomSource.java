package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Minecraft 1.18.2's xoroshiro-backed random source and positional factory. */
public final class XoroshiroRandomSource implements RandomSource {
    private static final float FLOAT_UNIT = 0x1.0p-24F;
    private static final double DOUBLE_UNIT = 0x1.0p-53D;

    private Xoroshiro128PlusPlus randomNumberGenerator;
    private final MarsagliaPolarGaussian gaussianSource = new MarsagliaPolarGaussian(this);

    public XoroshiroRandomSource(long seed) {
        this(RandomSupport.upgradeSeedTo128bit(seed));
    }

    public XoroshiroRandomSource(RandomSupport.Seed128 seed) {
        this(seed.seedLo(), seed.seedHi());
    }

    public XoroshiroRandomSource(long seedLo, long seedHi) {
        randomNumberGenerator = new Xoroshiro128PlusPlus(seedLo, seedHi);
    }

    @Override
    public RandomSource fork() {
        return new XoroshiroRandomSource(nextLong(), nextLong());
    }

    @Override
    public PositionalRandomFactory forkPositional() {
        return new XoroshiroPositionalRandomFactory(nextLong(), nextLong());
    }

    @Override
    public void setSeed(long seed) {
        randomNumberGenerator = new Xoroshiro128PlusPlus(RandomSupport.upgradeSeedTo128bit(seed));
        gaussianSource.reset();
    }

    @Override
    public int nextInt() {
        return (int) nextLong();
    }

    @Override
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("Bound must be positive");
        }

        long unsigned = Integer.toUnsignedLong(nextInt());
        long product = unsigned * (long) bound;
        long low = product & 0xFFFFFFFFL;
        if (low < bound) {
            int threshold = Integer.remainderUnsigned(~bound + 1, bound);
            while (low < threshold) {
                unsigned = Integer.toUnsignedLong(nextInt());
                product = unsigned * (long) bound;
                low = product & 0xFFFFFFFFL;
            }
        }
        return (int) (product >>> 32);
    }

    @Override
    public long nextLong() {
        return randomNumberGenerator.nextLong();
    }

    @Override
    public boolean nextBoolean() {
        return (nextLong() & 1L) != 0L;
    }

    @Override
    public float nextFloat() {
        return (float) nextBits(24) * FLOAT_UNIT;
    }

    @Override
    public double nextDouble() {
        return (double) nextBits(53) * DOUBLE_UNIT;
    }

    @Override
    public double nextGaussian() {
        return gaussianSource.nextGaussian();
    }

    @Override
    public void consumeCount(int count) {
        for (int i = 0; i < count; ++i) {
            nextLong();
        }
    }

    private long nextBits(int bits) {
        return nextLong() >>> 64 - bits;
    }

    private static final class MarsagliaPolarGaussian {
        private final RandomSource randomSource;
        private double nextNextGaussian;
        private boolean haveNextNextGaussian;

        private MarsagliaPolarGaussian(RandomSource randomSource) {
            this.randomSource = randomSource;
        }

        private void reset() {
            haveNextNextGaussian = false;
        }

        private double nextGaussian() {
            if (haveNextNextGaussian) {
                haveNextNextGaussian = false;
                return nextNextGaussian;
            }

            double x;
            double y;
            double radiusSquared;
            do {
                x = 2.0D * randomSource.nextDouble() - 1.0D;
                y = 2.0D * randomSource.nextDouble() - 1.0D;
                radiusSquared = WorldgenMath.square(x) + WorldgenMath.square(y);
            } while (radiusSquared >= 1.0D || radiusSquared == 0.0D);

            double multiplier = Math.sqrt(-2.0D * Math.log(radiusSquared) / radiusSquared);
            nextNextGaussian = y * multiplier;
            haveNextNextGaussian = true;
            return x * multiplier;
        }
    }

    public static final class XoroshiroPositionalRandomFactory implements PositionalRandomFactory {
        private final long seedLo;
        private final long seedHi;

        public XoroshiroPositionalRandomFactory(long seedLo, long seedHi) {
            this.seedLo = seedLo;
            this.seedHi = seedHi;
        }

        @Override
        public RandomSource at(int x, int y, int z) {
            long positionalSeed = WorldgenMath.getSeed(x, y, z);
            return new XoroshiroRandomSource(positionalSeed ^ seedLo, seedHi);
        }

        @Override
        public RandomSource fromHashOf(String name) {
            byte[] hash;
            try {
                hash = MessageDigest.getInstance("MD5").digest(name.getBytes(StandardCharsets.UTF_8));
            } catch (NoSuchAlgorithmException exception) {
                throw new IllegalStateException("MD5 must be available", exception);
            }
            long hashLo = longFromBigEndian(hash, 0);
            long hashHi = longFromBigEndian(hash, 8);
            return new XoroshiroRandomSource(hashLo ^ seedLo, hashHi ^ seedHi);
        }

        @Override
        public void appendParityConfigString(StringBuilder builder) {
            builder.append("seedLo: ").append(seedLo).append(", seedHi: ").append(seedHi);
        }

        private static long longFromBigEndian(byte[] bytes, int offset) {
            return (bytes[offset] & 0xFFL) << 56
                | (bytes[offset + 1] & 0xFFL) << 48
                | (bytes[offset + 2] & 0xFFL) << 40
                | (bytes[offset + 3] & 0xFFL) << 32
                | (bytes[offset + 4] & 0xFFL) << 24
                | (bytes[offset + 5] & 0xFFL) << 16
                | (bytes[offset + 6] & 0xFFL) << 8
                | bytes[offset + 7] & 0xFFL;
        }
    }
}

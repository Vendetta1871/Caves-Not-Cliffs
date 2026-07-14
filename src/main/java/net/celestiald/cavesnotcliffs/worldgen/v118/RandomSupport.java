package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.concurrent.atomic.AtomicLong;

/** Seed mixing constants and the 64-to-128-bit seed upgrade used by Minecraft 1.18.2. */
public final class RandomSupport {
    public static final long GOLDEN_RATIO_64 = -7046029254386353131L;
    public static final long SILVER_RATIO_64 = 7640891576956012809L;

    private static final AtomicLong SEED_UNIQUIFIER = new AtomicLong(8682522807148012L);

    private RandomSupport() {
    }

    public static long mixStafford13(long seed) {
        seed = (seed ^ seed >>> 30) * -4658895280553007687L;
        seed = (seed ^ seed >>> 27) * -7723592293110705685L;
        return seed ^ seed >>> 31;
    }

    public static Seed128 upgradeSeedTo128bit(long seed) {
        long loBase = seed ^ SILVER_RATIO_64;
        long hiBase = loBase + GOLDEN_RATIO_64;
        return new Seed128(mixStafford13(loBase), mixStafford13(hiBase));
    }

    public static long seedUniquifier() {
        return SEED_UNIQUIFIER.updateAndGet(value -> value * 1181783497276652981L)
            ^ System.nanoTime();
    }

    public static final class Seed128 {
        private final long seedLo;
        private final long seedHi;

        public Seed128(long seedLo, long seedHi) {
            this.seedLo = seedLo;
            this.seedHi = seedHi;
        }

        public long seedLo() {
            return seedLo;
        }

        public long seedHi() {
            return seedHi;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof Seed128)) {
                return false;
            }
            Seed128 other = (Seed128) object;
            return seedLo == other.seedLo && seedHi == other.seedHi;
        }

        @Override
        public int hashCode() {
            int result = (int) (seedLo ^ seedLo >>> 32);
            return 31 * result + (int) (seedHi ^ seedHi >>> 32);
        }

        @Override
        public String toString() {
            return "Seed128{seedLo=" + seedLo + ", seedHi=" + seedHi + '}';
        }
    }
}

package net.celestiald.cavesnotcliffs.worldgen.v118;

/** xoroshiro128++ 1.0 with Minecraft's all-zero-state substitution. */
public final class Xoroshiro128PlusPlus {
    private long seedLo;
    private long seedHi;

    public Xoroshiro128PlusPlus(RandomSupport.Seed128 seed) {
        this(seed.seedLo(), seed.seedHi());
    }

    public Xoroshiro128PlusPlus(long seedLo, long seedHi) {
        this.seedLo = seedLo;
        this.seedHi = seedHi;
        if ((this.seedLo | this.seedHi) == 0L) {
            this.seedLo = RandomSupport.GOLDEN_RATIO_64;
            this.seedHi = RandomSupport.SILVER_RATIO_64;
        }
    }

    public long nextLong() {
        long lo = seedLo;
        long hi = seedHi;
        long result = Long.rotateLeft(lo + hi, 17) + lo;
        hi ^= lo;
        seedLo = Long.rotateLeft(lo, 49) ^ hi ^ hi << 21;
        seedHi = Long.rotateLeft(hi, 28);
        return result;
    }
}

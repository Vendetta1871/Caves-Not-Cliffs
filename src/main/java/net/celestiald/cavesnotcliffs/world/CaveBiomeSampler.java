package net.celestiald.cavesnotcliffs.world;

/**
 * Deterministic three-dimensional cave-region selector.
 *
 * Minecraft 1.12 stores one biome per X/Z column, so cave biomes cannot be represented by the
 * vanilla biome array.  v2 keeps a separate 3D identity instead: every underground position is
 * assigned to a coherent NORMAL, LUSH, or DRIPSTONE region and all cave decoration consults this
 * class.  The sampler is deliberately stateless so generation order cannot change a world.
 */
public final class CaveBiomeSampler {
    public enum Type {
        NORMAL("Normal caves"),
        LUSH("Lush caves"),
        DRIPSTONE("Dripstone caves");

        private final String displayName;

        Type(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private static final long LARGE_SCALE_SALT = 0x4f9939f508L;
    private static final long DETAIL_SALT = 0x1ef1565bd5L;

    private CaveBiomeSampler() {
    }

    public static Type sample(long worldSeed, int x, int y, int z) {
        if (y < CavesNotCliffsWorldType.MIN_HEIGHT || y >= 64) {
            return Type.NORMAL;
        }

        // Broad 96x48x96 regions make a cave feel like one biome instead of a block-by-block mix.
        double broad = valueNoise(worldSeed ^ LARGE_SCALE_SALT, x / 96.0, y / 48.0, z / 96.0);
        double detail = valueNoise(worldSeed ^ DETAIL_SALT, x / 38.0, y / 30.0, z / 38.0);
        double selector = broad * 0.78 + detail * 0.22;

        if (selector >= 0.20) {
            return Type.LUSH;
        }
        if (selector <= -0.20) {
            return Type.DRIPSTONE;
        }
        return Type.NORMAL;
    }

    static double valueNoise(long seed, double x, double y, double z) {
        int x0 = fastFloor(x);
        int y0 = fastFloor(y);
        int z0 = fastFloor(z);
        double tx = fade(x - x0);
        double ty = fade(y - y0);
        double tz = fade(z - z0);

        double x00 = lerp(lattice(seed, x0, y0, z0), lattice(seed, x0 + 1, y0, z0), tx);
        double x10 = lerp(lattice(seed, x0, y0 + 1, z0), lattice(seed, x0 + 1, y0 + 1, z0), tx);
        double x01 = lerp(lattice(seed, x0, y0, z0 + 1), lattice(seed, x0 + 1, y0, z0 + 1), tx);
        double x11 = lerp(lattice(seed, x0, y0 + 1, z0 + 1), lattice(seed, x0 + 1, y0 + 1, z0 + 1), tx);
        return lerp(lerp(x00, x10, ty), lerp(x01, x11, ty), tz);
    }

    private static int fastFloor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    private static double fade(double value) {
        return value * value * value * (value * (value * 6.0 - 15.0) + 10.0);
    }

    private static double lerp(double a, double b, double amount) {
        return a + (b - a) * amount;
    }

    private static double lattice(long seed, int x, int y, int z) {
        long value = seed;
        value ^= (long) x * 0x632BE59BD9B4E019L;
        value ^= (long) y * 0x9E3779B97F4A7C15L;
        value ^= (long) z * 0x85157AF5D66D3E2BL;
        value = mix64(value);
        return ((value >>> 11) * 0x1.0p-53) * 2.0 - 1.0;
    }

    static long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }
}

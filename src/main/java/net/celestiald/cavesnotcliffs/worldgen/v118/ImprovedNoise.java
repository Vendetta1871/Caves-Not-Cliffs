package net.celestiald.cavesnotcliffs.worldgen.v118;

/** Ken Perlin's improved noise as initialized and sampled by Minecraft 1.18.2. */
public final class ImprovedNoise {
    private static final float SHIFT_UP_EPSILON = 1.0E-7F;
    private static final int[][] GRADIENT = {
        {1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0},
        {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
        {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1},
        {1, 1, 0}, {0, -1, 1}, {-1, 1, 0}, {0, -1, -1}
    };

    private final byte[] permutation = new byte[256];
    public final double xo;
    public final double yo;
    public final double zo;

    public ImprovedNoise(RandomSource random) {
        xo = random.nextDouble() * 256.0D;
        yo = random.nextDouble() * 256.0D;
        zo = random.nextDouble() * 256.0D;

        for (int i = 0; i < 256; ++i) {
            permutation[i] = (byte) i;
        }
        for (int i = 0; i < 256; ++i) {
            int offset = random.nextInt(256 - i);
            byte current = permutation[i];
            permutation[i] = permutation[i + offset];
            permutation[i + offset] = current;
        }
    }

    public double noise(double x, double y, double z) {
        return noise(x, y, z, 0.0D, 0.0D);
    }

    /** The legacy y-smearing overload retained by 1.18.2's blended noise. */
    public double noise(double x, double y, double z, double yScale, double yMax) {
        double shiftedX = x + xo;
        double shiftedY = y + yo;
        double shiftedZ = z + zo;
        int floorX = WorldgenMath.floor(shiftedX);
        int floorY = WorldgenMath.floor(shiftedY);
        int floorZ = WorldgenMath.floor(shiftedZ);
        double localX = shiftedX - floorX;
        double localY = shiftedY - floorY;
        double localZ = shiftedZ - floorZ;

        double yOffset;
        if (yScale != 0.0D) {
            double limitedY = yMax >= 0.0D && yMax < localY ? yMax : localY;
            yOffset = WorldgenMath.floor(limitedY / yScale + SHIFT_UP_EPSILON) * yScale;
        } else {
            yOffset = 0.0D;
        }
        return sampleAndLerp(floorX, floorY, floorZ, localX, localY - yOffset, localZ, localY);
    }

    public double noiseWithDerivative(double x, double y, double z, double[] derivative) {
        if (derivative.length < 3) {
            throw new IllegalArgumentException("Derivative array must have at least three elements");
        }
        double shiftedX = x + xo;
        double shiftedY = y + yo;
        double shiftedZ = z + zo;
        int floorX = WorldgenMath.floor(shiftedX);
        int floorY = WorldgenMath.floor(shiftedY);
        int floorZ = WorldgenMath.floor(shiftedZ);
        return sampleWithDerivative(floorX, floorY, floorZ,
            shiftedX - floorX, shiftedY - floorY, shiftedZ - floorZ, derivative);
    }

    private static double gradDot(int hash, double x, double y, double z) {
        int[] gradient = GRADIENT[hash & 15];
        return gradient[0] * x + gradient[1] * y + gradient[2] * z;
    }

    private int p(int value) {
        return permutation[value & 255] & 255;
    }

    private double sampleAndLerp(int floorX, int floorY, int floorZ,
            double localX, double localY, double localZ, double fadeYInput) {
        int px0 = p(floorX);
        int px1 = p(floorX + 1);
        int pxy00 = p(px0 + floorY);
        int pxy01 = p(px0 + floorY + 1);
        int pxy10 = p(px1 + floorY);
        int pxy11 = p(px1 + floorY + 1);

        double x0y0z0 = gradDot(p(pxy00 + floorZ), localX, localY, localZ);
        double x1y0z0 = gradDot(p(pxy10 + floorZ), localX - 1.0D, localY, localZ);
        double x0y1z0 = gradDot(p(pxy01 + floorZ), localX, localY - 1.0D, localZ);
        double x1y1z0 = gradDot(p(pxy11 + floorZ), localX - 1.0D, localY - 1.0D, localZ);
        double x0y0z1 = gradDot(p(pxy00 + floorZ + 1), localX, localY, localZ - 1.0D);
        double x1y0z1 = gradDot(p(pxy10 + floorZ + 1), localX - 1.0D, localY, localZ - 1.0D);
        double x0y1z1 = gradDot(p(pxy01 + floorZ + 1), localX, localY - 1.0D, localZ - 1.0D);
        double x1y1z1 = gradDot(p(pxy11 + floorZ + 1), localX - 1.0D, localY - 1.0D, localZ - 1.0D);

        return WorldgenMath.lerp3(
            WorldgenMath.smoothstep(localX),
            WorldgenMath.smoothstep(fadeYInput),
            WorldgenMath.smoothstep(localZ),
            x0y0z0, x1y0z0, x0y1z0, x1y1z0,
            x0y0z1, x1y0z1, x0y1z1, x1y1z1);
    }

    private double sampleWithDerivative(int floorX, int floorY, int floorZ,
            double localX, double localY, double localZ, double[] derivative) {
        int px0 = p(floorX);
        int px1 = p(floorX + 1);
        int pxy00 = p(px0 + floorY);
        int pxy01 = p(px0 + floorY + 1);
        int pxy10 = p(px1 + floorY);
        int pxy11 = p(px1 + floorY + 1);

        int[] g000 = GRADIENT[p(pxy00 + floorZ) & 15];
        int[] g100 = GRADIENT[p(pxy10 + floorZ) & 15];
        int[] g010 = GRADIENT[p(pxy01 + floorZ) & 15];
        int[] g110 = GRADIENT[p(pxy11 + floorZ) & 15];
        int[] g001 = GRADIENT[p(pxy00 + floorZ + 1) & 15];
        int[] g101 = GRADIENT[p(pxy10 + floorZ + 1) & 15];
        int[] g011 = GRADIENT[p(pxy01 + floorZ + 1) & 15];
        int[] g111 = GRADIENT[p(pxy11 + floorZ + 1) & 15];

        double n000 = dot(g000, localX, localY, localZ);
        double n100 = dot(g100, localX - 1.0D, localY, localZ);
        double n010 = dot(g010, localX, localY - 1.0D, localZ);
        double n110 = dot(g110, localX - 1.0D, localY - 1.0D, localZ);
        double n001 = dot(g001, localX, localY, localZ - 1.0D);
        double n101 = dot(g101, localX - 1.0D, localY, localZ - 1.0D);
        double n011 = dot(g011, localX, localY - 1.0D, localZ - 1.0D);
        double n111 = dot(g111, localX - 1.0D, localY - 1.0D, localZ - 1.0D);

        double fadeX = WorldgenMath.smoothstep(localX);
        double fadeY = WorldgenMath.smoothstep(localY);
        double fadeZ = WorldgenMath.smoothstep(localZ);

        double gradientX = WorldgenMath.lerp3(fadeX, fadeY, fadeZ,
            g000[0], g100[0], g010[0], g110[0], g001[0], g101[0], g011[0], g111[0]);
        double gradientY = WorldgenMath.lerp3(fadeX, fadeY, fadeZ,
            g000[1], g100[1], g010[1], g110[1], g001[1], g101[1], g011[1], g111[1]);
        double gradientZ = WorldgenMath.lerp3(fadeX, fadeY, fadeZ,
            g000[2], g100[2], g010[2], g110[2], g001[2], g101[2], g011[2], g111[2]);

        double xBlend = WorldgenMath.lerp2(fadeY, fadeZ,
            n100 - n000, n110 - n010, n101 - n001, n111 - n011);
        double yBlend = WorldgenMath.lerp2(fadeZ, fadeX,
            n010 - n000, n011 - n001, n110 - n100, n111 - n101);
        double zBlend = WorldgenMath.lerp2(fadeX, fadeY,
            n001 - n000, n101 - n100, n011 - n010, n111 - n110);

        derivative[0] += gradientX + WorldgenMath.smoothstepDerivative(localX) * xBlend;
        derivative[1] += gradientY + WorldgenMath.smoothstepDerivative(localY) * yBlend;
        derivative[2] += gradientZ + WorldgenMath.smoothstepDerivative(localZ) * zBlend;

        return WorldgenMath.lerp3(fadeX, fadeY, fadeZ,
            n000, n100, n010, n110, n001, n101, n011, n111);
    }

    private static double dot(int[] gradient, double x, double y, double z) {
        return gradient[0] * x + gradient[1] * y + gradient[2] * z;
    }
}

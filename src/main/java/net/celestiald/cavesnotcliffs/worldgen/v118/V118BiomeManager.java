package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Java 8 port of 1.18.2's quart-biome Voronoi zoom used by surface evaluation. */
public final class V118BiomeManager {
    private final NoiseBiomeSource source;
    private final long biomeZoomSeed;

    public V118BiomeManager(NoiseBiomeSource source, long worldSeed) {
        if (source == null) {
            throw new NullPointerException("source");
        }
        this.source = source;
        biomeZoomSeed = obfuscateSeed(worldSeed);
    }

    public V118Biome getBiome(int blockX, int blockY, int blockZ) {
        int shiftedX = blockX - 2;
        int shiftedY = blockY - 2;
        int shiftedZ = blockZ - 2;
        int quartX = shiftedX >> 2;
        int quartY = shiftedY >> 2;
        int quartZ = shiftedZ >> 2;
        double fractionX = (shiftedX & 3) / 4.0D;
        double fractionY = (shiftedY & 3) / 4.0D;
        double fractionZ = (shiftedZ & 3) / 4.0D;
        int nearest = 0;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (int corner = 0; corner < 8; ++corner) {
            boolean lowerX = (corner & 4) == 0;
            boolean lowerY = (corner & 2) == 0;
            boolean lowerZ = (corner & 1) == 0;
            int sampleX = lowerX ? quartX : quartX + 1;
            int sampleY = lowerY ? quartY : quartY + 1;
            int sampleZ = lowerZ ? quartZ : quartZ + 1;
            double distance = fiddledDistance(biomeZoomSeed, sampleX, sampleY, sampleZ,
                lowerX ? fractionX : fractionX - 1.0D,
                lowerY ? fractionY : fractionY - 1.0D,
                lowerZ ? fractionZ : fractionZ - 1.0D);
            if (nearestDistance > distance) {
                nearest = corner;
                nearestDistance = distance;
            }
        }
        return source.getNoiseBiome((nearest & 4) == 0 ? quartX : quartX + 1,
            (nearest & 2) == 0 ? quartY : quartY + 1,
            (nearest & 1) == 0 ? quartZ : quartZ + 1);
    }

    public static long obfuscateSeed(long seed) {
        byte[] input = new byte[8];
        for (int index = 0; index < input.length; ++index) {
            input[index] = (byte) (seed >>> index * 8);
        }
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
        long result = 0L;
        for (int index = 0; index < 8; ++index) {
            result |= (hash[index] & 255L) << index * 8;
        }
        return result;
    }

    private static double fiddledDistance(long seed, int quartX, int quartY, int quartZ,
            double fractionX, double fractionY, double fractionZ) {
        long mixed = seed;
        mixed = next(mixed, quartX);
        mixed = next(mixed, quartY);
        mixed = next(mixed, quartZ);
        mixed = next(mixed, quartX);
        mixed = next(mixed, quartY);
        mixed = next(mixed, quartZ);
        double offsetX = fiddle(mixed);
        mixed = next(mixed, seed);
        double offsetY = fiddle(mixed);
        mixed = next(mixed, seed);
        double offsetZ = fiddle(mixed);
        return square(fractionZ + offsetZ) + square(fractionY + offsetY)
            + square(fractionX + offsetX);
    }

    private static long next(long seed, long salt) {
        return seed * (seed * 6364136223846793005L + 1442695040888963407L) + salt;
    }

    private static double fiddle(long value) {
        double unit = Math.floorMod(value >> 24, 1024) / 1024.0D;
        return (unit - 0.5D) * 0.9D;
    }

    private static double square(double value) {
        return value * value;
    }

    public interface NoiseBiomeSource {
        V118Biome getNoiseBiome(int quartX, int quartY, int quartZ);
    }
}

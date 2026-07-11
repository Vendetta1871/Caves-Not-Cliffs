package net.celestiald.cavesnotcliffs.world;

import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

import java.util.Random;

/**
 * A deterministic worm-style carver for the new negative-Y layer.  v3 owns the planned noise-cave
 * rewrite; v2 deliberately extends the familiar 1.12 cave shape instead.
 */
final class LegacyCaveCarver {
    // A 52-block tunnel plus its radius can reach four cubes away from its origin.
    private static final int ORIGIN_RADIUS = 4;

    private LegacyCaveCarver() {
    }

    static void carve(CubePrimer primer, long worldSeed, int cubeX, int cubeY, int cubeZ) {
        if (cubeY < -4 || cubeY >= 0) {
            return;
        }

        for (int originX = cubeX - ORIGIN_RADIUS; originX <= cubeX + ORIGIN_RADIUS; originX++) {
            for (int originY = Math.max(-4, cubeY - ORIGIN_RADIUS);
                    originY <= Math.min(-1, cubeY + ORIGIN_RADIUS); originY++) {
                for (int originZ = cubeZ - ORIGIN_RADIUS; originZ <= cubeZ + ORIGIN_RADIUS; originZ++) {
                    Random random = new Random(seed(worldSeed, originX, originY, originZ));
                    if (random.nextInt(13) != 0) {
                        continue;
                    }
                    int tunnels = 1 + (random.nextInt(5) == 0 ? 1 : 0);
                    for (int tunnel = 0; tunnel < tunnels; tunnel++) {
                        carveTunnel(primer, random, cubeX, cubeY, cubeZ,
                                originX * 16 + random.nextInt(16),
                                originY * 16 + random.nextInt(16),
                                originZ * 16 + random.nextInt(16));
                    }
                }
            }
        }
    }

    private static void carveTunnel(CubePrimer primer, Random random,
            int cubeX, int cubeY, int cubeZ, double x, double y, double z) {
        float yaw = random.nextFloat() * ((float) Math.PI * 2.0F);
        float pitch = (random.nextFloat() - 0.5F) * 0.28F;
        float yawDrift = 0.0F;
        float pitchDrift = 0.0F;
        int length = 28 + random.nextInt(25);
        double baseRadius = 1.35 + random.nextDouble() * 1.65;

        for (int step = 0; step < length; step++) {
            double horizontal = Math.cos(pitch);
            x += Math.cos(yaw) * horizontal;
            y += Math.sin(pitch);
            z += Math.sin(yaw) * horizontal;

            pitch *= 0.72F;
            pitch += pitchDrift * 0.10F;
            yaw += yawDrift * 0.10F;
            pitchDrift *= 0.80F;
            yawDrift *= 0.75F;
            pitchDrift += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 0.18F;
            yawDrift += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 0.34F;

            double taper = Math.sin(Math.PI * step / length);
            double radius = baseRadius * (0.65 + taper * 0.55);
            carveEllipsoid(primer, cubeX, cubeY, cubeZ, x, y, z, radius, radius * 0.72);
        }
    }

    private static void carveEllipsoid(CubePrimer primer, int cubeX, int cubeY, int cubeZ,
            double centerX, double centerY, double centerZ, double radiusXZ, double radiusY) {
        int minWorldX = cubeX * 16;
        int minWorldY = cubeY * 16;
        int minWorldZ = cubeZ * 16;
        int minX = Math.max(0, (int) Math.floor(centerX - radiusXZ) - minWorldX);
        int maxX = Math.min(15, (int) Math.floor(centerX + radiusXZ) - minWorldX);
        int minY = Math.max(0, (int) Math.floor(centerY - radiusY) - minWorldY);
        int maxY = Math.min(15, (int) Math.floor(centerY + radiusY) - minWorldY);
        int minZ = Math.max(0, (int) Math.floor(centerZ - radiusXZ) - minWorldZ);
        int maxZ = Math.min(15, (int) Math.floor(centerZ + radiusXZ) - minWorldZ);

        for (int localX = minX; localX <= maxX; localX++) {
            double dx = (minWorldX + localX + 0.5 - centerX) / radiusXZ;
            for (int localY = minY; localY <= maxY; localY++) {
                int worldY = minWorldY + localY;
                if (worldY <= CavesNotCliffsWorldType.MIN_HEIGHT + 4 || worldY >= 0) {
                    continue;
                }
                double dy = (worldY + 0.5 - centerY) / radiusY;
                for (int localZ = minZ; localZ <= maxZ; localZ++) {
                    double dz = (minWorldZ + localZ + 0.5 - centerZ) / radiusXZ;
                    if (dx * dx + dy * dy + dz * dz >= 1.0) {
                        continue;
                    }
                    IBlockState state = primer.getBlockState(localX, localY, localZ);
                    Block block = state.getBlock();
                    if (block == Blocks.BEDROCK) {
                        continue;
                    }
                    primer.setBlockState(localX, localY, localZ,
                            worldY < -54 ? Blocks.LAVA.getDefaultState() : Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    private static long seed(long worldSeed, int x, int y, int z) {
        long value = worldSeed;
        value ^= (long) x * 341873128712L;
        value ^= (long) y * 132897987541L;
        value ^= (long) z * 42317861L;
        return CaveBiomeSampler.mix64(value);
    }
}

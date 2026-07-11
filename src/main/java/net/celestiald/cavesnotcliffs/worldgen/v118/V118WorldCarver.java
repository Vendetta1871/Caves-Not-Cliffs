package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.Random;

/** Dependency-free Java 8 port of Java 1.18.2's base {@code WorldCarver}. */
public abstract class V118WorldCarver<C extends V118CarverConfiguration> {
    static final int RANGE = 4;
    private static final int NON_UPGRADING_TOP_MARGIN = 7;
    private static final float[] SIN = createSinTable();

    public final boolean isStartChunk(C configuration, Random random) {
        return random.nextFloat() <= configuration.probability();
    }

    public abstract boolean carve(C configuration, WorldAccess world, Random random,
        int sourceChunkX, int sourceChunkZ, V118CarvingMask mask);

    final boolean carveEllipsoid(C configuration, WorldAccess world,
            double centerX, double centerY, double centerZ, double horizontalRadius,
            double verticalRadius, V118CarvingMask mask, CarveSkipChecker skipChecker) {
        int targetMinX = world.targetChunkX() << 4;
        int targetMinZ = world.targetChunkZ() << 4;
        double targetMiddleX = targetMinX + 8;
        double targetMiddleZ = targetMinZ + 8;
        double reach = 16.0D + horizontalRadius * 2.0D;
        if (Math.abs(centerX - targetMiddleX) > reach
                || Math.abs(centerZ - targetMiddleZ) > reach) {
            return false;
        }

        int minimumLocalX = Math.max(WorldgenMath.floor(centerX - horizontalRadius)
            - targetMinX - 1, 0);
        int maximumLocalX = Math.min(WorldgenMath.floor(centerX + horizontalRadius)
            - targetMinX, 15);
        int minimumY = Math.max(WorldgenMath.floor(centerY - verticalRadius) - 1,
            world.minBuildHeight() + 1);
        int maximumY = Math.min(WorldgenMath.floor(centerY + verticalRadius) + 1,
            world.maxBuildHeight() - 1 - NON_UPGRADING_TOP_MARGIN);
        int minimumLocalZ = Math.max(WorldgenMath.floor(centerZ - horizontalRadius)
            - targetMinZ - 1, 0);
        int maximumLocalZ = Math.min(WorldgenMath.floor(centerZ + horizontalRadius)
            - targetMinZ, 15);
        boolean carved = false;

        for (int localX = minimumLocalX; localX <= maximumLocalX; ++localX) {
            int blockX = targetMinX + localX;
            double normalizedX = ((double) blockX + 0.5D - centerX) / horizontalRadius;
            for (int localZ = minimumLocalZ; localZ <= maximumLocalZ; ++localZ) {
                int blockZ = targetMinZ + localZ;
                double normalizedZ = ((double) blockZ + 0.5D - centerZ)
                    / horizontalRadius;
                if (normalizedX * normalizedX + normalizedZ * normalizedZ >= 1.0D) {
                    continue;
                }
                boolean[] foundSurface = {false};
                for (int blockY = maximumY; blockY > minimumY; --blockY) {
                    double normalizedY = ((double) blockY - 0.5D - centerY)
                        / verticalRadius;
                    if (skipChecker.shouldSkip(normalizedX, normalizedY, normalizedZ, blockY)
                            || mask.get(localX, blockY, localZ)) {
                        continue;
                    }
                    mask.set(localX, blockY, localZ);
                    carved |= carveBlock(configuration, world, blockX, blockY, blockZ,
                        foundSurface);
                }
            }
        }
        return carved;
    }

    private boolean carveBlock(C configuration, WorldAccess world, int blockX, int blockY,
            int blockZ, boolean[] foundSurface) {
        V118Material current = world.getMaterial(blockX, blockY, blockZ);
        if (current == V118Material.GRASS_BLOCK || current == V118Material.MYCELIUM) {
            foundSurface[0] = true;
        }
        if (!canReplaceBlock(current)) {
            return false;
        }

        V118Material replacement;
        if (blockY <= configuration.lavaLevel()) {
            replacement = V118Material.LAVA;
        } else {
            replacement = world.computeAquiferMaterial(blockX, blockY, blockZ);
            if (replacement == null) {
                return false;
            }
        }
        boolean scheduleFluid = isFluid(replacement)
            && world.shouldScheduleAquiferFluidUpdate();
        world.setMaterial(blockX, blockY, blockZ, replacement, scheduleFluid);

        if (foundSurface[0]) {
            int belowY = blockY - 1;
            if (world.getMaterial(blockX, belowY, blockZ) == V118Material.DIRT) {
                V118Material top = world.topMaterial(blockX, belowY, blockZ,
                    isFluid(replacement));
                if (top != null) {
                    world.setMaterial(blockX, belowY, blockZ, top, isFluid(top));
                }
            }
        }
        return true;
    }

    static boolean canReach(int targetChunkX, int targetChunkZ, double x, double z,
            int step, int length, float thickness) {
        double middleX = (targetChunkX << 4) + 8;
        double middleZ = (targetChunkZ << 4) + 8;
        double deltaX = x - middleX;
        double deltaZ = z - middleZ;
        double remaining = length - step;
        double maximumReach = thickness + 2.0F + 16.0F;
        return deltaX * deltaX + deltaZ * deltaZ - remaining * remaining
            <= maximumReach * maximumReach;
    }

    static float sin(float value) {
        return SIN[(int) (value * 10430.378F) & 65535];
    }

    static float cos(float value) {
        return SIN[(int) (value * 10430.378F + 16384.0F) & 65535];
    }

    private static boolean canReplaceBlock(V118Material material) {
        switch (material) {
            case WATER:
            case STONE:
            case GRANITE:
            case DIRT:
            case COARSE_DIRT:
            case PODZOL:
            case GRASS_BLOCK:
            case TERRACOTTA:
            case WHITE_TERRACOTTA:
            case ORANGE_TERRACOTTA:
            case YELLOW_TERRACOTTA:
            case BROWN_TERRACOTTA:
            case RED_TERRACOTTA:
            case LIGHT_GRAY_TERRACOTTA:
            case SANDSTONE:
            case RED_SANDSTONE:
            case MYCELIUM:
            case PACKED_ICE:
            case DEEPSLATE:
            case CALCITE:
            case SAND:
            case RED_SAND:
            case GRAVEL:
            case TUFF:
            case COPPER_ORE:
            case RAW_COPPER_BLOCK:
            case DEEPSLATE_IRON_ORE:
            case RAW_IRON_BLOCK:
                return true;
            default:
                return false;
        }
    }

    static boolean isFluid(V118Material material) {
        return material == V118Material.WATER || material == V118Material.LAVA;
    }

    private static float[] createSinTable() {
        float[] values = new float[65536];
        for (int index = 0; index < values.length; ++index) {
            values[index] = (float) Math.sin(index * Math.PI * 2.0D / 65536.0D);
        }
        return values;
    }

    interface CarveSkipChecker {
        boolean shouldSkip(double normalizedX, double normalizedY, double normalizedZ,
            int blockY);
    }

    /** Mutable target-column and aquifer bridge used by the isolated carvers. */
    public interface WorldAccess {
        int targetChunkX();

        int targetChunkZ();

        int minBuildHeight();

        int maxBuildHeight();

        V118Material getMaterial(int blockX, int blockY, int blockZ);

        void setMaterial(int blockX, int blockY, int blockZ, V118Material material,
            boolean scheduleFluidUpdate);

        /** Returns null when the aquifer barrier keeps this location solid. */
        V118Material computeAquiferMaterial(int blockX, int blockY, int blockZ);

        /** Mirrors Aquifer's stateful flag, including forced-lava calls that do not recompute. */
        boolean shouldScheduleAquiferFluidUpdate();

        /** Evaluates the Overworld surface rule in the exact one-above/one-below repair context. */
        V118Material topMaterial(int blockX, int blockY, int blockZ, boolean hasFluidAbove);
    }
}

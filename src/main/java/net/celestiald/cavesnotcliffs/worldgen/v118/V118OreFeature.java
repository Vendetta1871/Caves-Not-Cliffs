package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Dependency-free Java 8 port of 1.18.2's ordinary ellipsoid {@code OreFeature}. */
public final class V118OreFeature {
    private static final float[] SIN = createSinTable();
    private static final int[][] NEIGHBORS = {
        {0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}
    };

    private V118OreFeature() {
    }

    public static boolean place(WorldAccess world, Random random, Configuration configuration,
            int originX, int originY, int originZ) {
        float angle = random.nextFloat() * (float) Math.PI;
        float reach = (float) configuration.size / 8.0F;
        int padding = ceil(((float) configuration.size / 16.0F * 2.0F + 1.0F) / 2.0F);
        double startX = originX + Math.sin(angle) * reach;
        double endX = originX - Math.sin(angle) * reach;
        double startZ = originZ + Math.cos(angle) * reach;
        double endZ = originZ - Math.cos(angle) * reach;
        double startY = originY + random.nextInt(3) - 2;
        double endY = originY + random.nextInt(3) - 2;
        int boxMinX = originX - ceil(reach) - padding;
        int boxMinY = originY - 2 - padding;
        int boxMinZ = originZ - ceil(reach) - padding;
        int boxWidth = 2 * (ceil(reach) + padding);
        int boxHeight = 2 * (2 + padding);

        for (int x = boxMinX; x <= boxMinX + boxWidth; ++x) {
            for (int z = boxMinZ; z <= boxMinZ + boxWidth; ++z) {
                if (boxMinY > world.oceanFloorHeight(x, z)) {
                    continue;
                }
                return doPlace(world, random, configuration, startX, endX, startZ, endZ,
                    startY, endY, boxMinX, boxMinY, boxMinZ, boxWidth, boxHeight);
            }
        }
        return false;
    }

    private static boolean doPlace(WorldAccess world, Random random, Configuration configuration,
            double startX, double endX, double startZ, double endZ, double startY, double endY,
            int boxMinX, int boxMinY, int boxMinZ, int boxWidth, int boxHeight) {
        int placed = 0;
        BitSet visited = new BitSet(boxWidth * boxHeight * boxWidth);
        int size = configuration.size;
        double[] ellipsoids = new double[size * 4];
        for (int index = 0; index < size; ++index) {
            float progress = (float) index / (float) size;
            double centerX = WorldgenMath.lerp(progress, startX, endX);
            double centerY = WorldgenMath.lerp(progress, startY, endY);
            double centerZ = WorldgenMath.lerp(progress, startZ, endZ);
            double randomRadius = random.nextDouble() * size / 16.0D;
            double radius = ((sin((float) Math.PI * progress) + 1.0F) * randomRadius + 1.0D)
                / 2.0D;
            ellipsoids[index * 4] = centerX;
            ellipsoids[index * 4 + 1] = centerY;
            ellipsoids[index * 4 + 2] = centerZ;
            ellipsoids[index * 4 + 3] = radius;
        }

        for (int first = 0; first < size - 1; ++first) {
            if (ellipsoids[first * 4 + 3] <= 0.0D) {
                continue;
            }
            for (int second = first + 1; second < size; ++second) {
                if (ellipsoids[second * 4 + 3] <= 0.0D) {
                    continue;
                }
                double radiusDifference = ellipsoids[first * 4 + 3]
                    - ellipsoids[second * 4 + 3];
                double xDifference = ellipsoids[first * 4] - ellipsoids[second * 4];
                double yDifference = ellipsoids[first * 4 + 1] - ellipsoids[second * 4 + 1];
                double zDifference = ellipsoids[first * 4 + 2] - ellipsoids[second * 4 + 2];
                if (radiusDifference * radiusDifference <= xDifference * xDifference
                        + yDifference * yDifference + zDifference * zDifference) {
                    continue;
                }
                if (radiusDifference > 0.0D) {
                    ellipsoids[second * 4 + 3] = -1.0D;
                } else {
                    ellipsoids[first * 4 + 3] = -1.0D;
                }
            }
        }

        for (int index = 0; index < size; ++index) {
            double radius = ellipsoids[index * 4 + 3];
            if (radius < 0.0D) {
                continue;
            }
            double centerX = ellipsoids[index * 4];
            double centerY = ellipsoids[index * 4 + 1];
            double centerZ = ellipsoids[index * 4 + 2];
            int minX = Math.max(WorldgenMath.floor(centerX - radius), boxMinX);
            int minY = Math.max(WorldgenMath.floor(centerY - radius), boxMinY);
            int minZ = Math.max(WorldgenMath.floor(centerZ - radius), boxMinZ);
            int maxX = Math.max(WorldgenMath.floor(centerX + radius), minX);
            int maxY = Math.max(WorldgenMath.floor(centerY + radius), minY);
            int maxZ = Math.max(WorldgenMath.floor(centerZ + radius), minZ);
            for (int x = minX; x <= maxX; ++x) {
                double normalizedX = (x + 0.5D - centerX) / radius;
                if (normalizedX * normalizedX >= 1.0D) {
                    continue;
                }
                for (int y = minY; y <= maxY; ++y) {
                    double normalizedY = (y + 0.5D - centerY) / radius;
                    if (normalizedX * normalizedX + normalizedY * normalizedY >= 1.0D) {
                        continue;
                    }
                    for (int z = minZ; z <= maxZ; ++z) {
                        double normalizedZ = (z + 0.5D - centerZ) / radius;
                        int visitedIndex = x - boxMinX + (y - boxMinY) * boxWidth
                            + (z - boxMinZ) * boxWidth * boxHeight;
                        if (normalizedX * normalizedX + normalizedY * normalizedY
                                + normalizedZ * normalizedZ >= 1.0D
                                || world.isOutsideBuildHeight(y) || visited.get(visitedIndex)) {
                            continue;
                        }
                        visited.set(visitedIndex);
                        if (!world.ensureCanWrite(x, y, z)) {
                            continue;
                        }
                        V118OreMaterial current = world.getMaterial(x, y, z);
                        for (Target target : configuration.targets) {
                            if (!target.rule.matches(current)
                                    || !canPlaceOre(world, random, configuration, x, y, z)) {
                                continue;
                            }
                            world.setMaterial(x, y, z, target.result);
                            ++placed;
                            break;
                        }
                    }
                }
            }
        }
        return placed > 0;
    }

    private static boolean canPlaceOre(WorldAccess world, Random random,
            Configuration configuration, int x, int y, int z) {
        float discardChance = configuration.discardChanceOnAirExposure;
        if (discardChance <= 0.0F) {
            return true;
        }
        if (discardChance < 1.0F && random.nextFloat() >= discardChance) {
            return true;
        }
        for (int[] neighbor : NEIGHBORS) {
            if (world.getMaterial(x + neighbor[0], y + neighbor[1], z + neighbor[2])
                    == V118OreMaterial.AIR) {
                return false;
            }
        }
        return true;
    }

    public enum TargetRule {
        NATURAL_STONE {
            @Override
            boolean matches(V118OreMaterial material) {
                return material == V118OreMaterial.STONE || material == V118OreMaterial.GRANITE
                    || material == V118OreMaterial.DIORITE || material == V118OreMaterial.ANDESITE
                    || material == V118OreMaterial.TUFF || material == V118OreMaterial.DEEPSLATE;
            }
        },
        STONE_ORE_REPLACEABLES {
            @Override
            boolean matches(V118OreMaterial material) {
                return material == V118OreMaterial.STONE || material == V118OreMaterial.GRANITE
                    || material == V118OreMaterial.DIORITE || material == V118OreMaterial.ANDESITE;
            }
        },
        DEEPSLATE_ORE_REPLACEABLES {
            @Override
            boolean matches(V118OreMaterial material) {
                return material == V118OreMaterial.DEEPSLATE || material == V118OreMaterial.TUFF;
            }
        };

        abstract boolean matches(V118OreMaterial material);
    }

    public static final class Target {
        private final TargetRule rule;
        private final V118OreMaterial result;

        public Target(TargetRule rule, V118OreMaterial result) {
            if (rule == null || result == null) {
                throw new NullPointerException("target rule and result are required");
            }
            this.rule = rule;
            this.result = result;
        }

        public TargetRule rule() {
            return rule;
        }

        public V118OreMaterial result() {
            return result;
        }
    }

    public static final class Configuration {
        private final List<Target> targets;
        private final int size;
        private final float discardChanceOnAirExposure;

        public Configuration(List<Target> targets, int size,
                float discardChanceOnAirExposure) {
            if (targets == null || targets.isEmpty()) {
                throw new IllegalArgumentException("at least one target is required");
            }
            if (size < 0 || size > 64) {
                throw new IllegalArgumentException("size must be in [0, 64]");
            }
            if (discardChanceOnAirExposure < 0.0F || discardChanceOnAirExposure > 1.0F) {
                throw new IllegalArgumentException("discard chance must be in [0, 1]");
            }
            this.targets = Collections.unmodifiableList(new ArrayList<Target>(targets));
            this.size = size;
            this.discardChanceOnAirExposure = discardChanceOnAirExposure;
        }

        public List<Target> targets() {
            return targets;
        }

        public int size() {
            return size;
        }

        public float discardChanceOnAirExposure() {
            return discardChanceOnAirExposure;
        }
    }

    public interface WorldAccess {
        int minBuildHeight();

        int maxBuildHeight();

        V118OreMaterial getMaterial(int blockX, int blockY, int blockZ);

        void setMaterial(int blockX, int blockY, int blockZ, V118OreMaterial material);

        int oceanFloorHeight(int blockX, int blockZ);

        default boolean ensureCanWrite(int blockX, int blockY, int blockZ) {
            return true;
        }

        default boolean isOutsideBuildHeight(int blockY) {
            return blockY < minBuildHeight() || blockY >= maxBuildHeight();
        }
    }

    private static float sin(float value) {
        return SIN[(int) (value * 10430.378F) & 65535];
    }

    private static int ceil(float value) {
        int truncated = (int) value;
        return value > truncated ? truncated + 1 : truncated;
    }

    private static float[] createSinTable() {
        float[] values = new float[65536];
        for (int index = 0; index < values.length; ++index) {
            values[index] = (float) Math.sin(index * Math.PI * 2.0D / 65536.0D);
        }
        return values;
    }
}

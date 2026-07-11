package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Dependency-free Java 8 port of Java 1.18.2's {@code BaseDiskFeature}. */
public final class V118DiskFeature {
    private V118DiskFeature() {
    }

    public static boolean place(WorldAccess world, Random random, Configuration configuration,
            int originX, int originY, int originZ) {
        // Feature.DISK is DiskReplaceFeature in 1.18.2 and rejects non-water origins.
        if (world.getMaterial(originX, originY, originZ) != V118OreMaterial.WATER) {
            return false;
        }
        boolean placed = false;
        int upperY = originY + configuration.halfHeight;
        int lowerY = originY - configuration.halfHeight - 1;
        boolean falling = configuration.state == V118OreMaterial.SAND
                || configuration.state == V118OreMaterial.GRAVEL;
        int radius = configuration.minimumRadius
                + random.nextInt(configuration.maximumRadius
                    - configuration.minimumRadius + 1);

        for (int x = originX - radius; x <= originX + radius; ++x) {
            for (int z = originZ - radius; z <= originZ + radius; ++z) {
                int offsetX = x - originX;
                int offsetZ = z - originZ;
                if (offsetX * offsetX + offsetZ * offsetZ > radius * radius) {
                    continue;
                }
                boolean replacedAbove = false;
                for (int y = upperY; y >= lowerY; --y) {
                    V118OreMaterial current = world.getMaterial(x, y, z);
                    boolean replaced = false;
                    if (y > lowerY && configuration.targets.contains(current)) {
                        world.setMaterial(x, y, z, configuration.state);
                        placed = true;
                        replaced = true;
                    }
                    if (falling && replacedAbove && current == V118OreMaterial.AIR) {
                        // The only red-sand branch is not used by the scoped Overworld catalog.
                        world.setMaterial(x, y + 1, z, V118OreMaterial.SANDSTONE);
                    }
                    replacedAbove = replaced;
                }
            }
        }
        return placed;
    }

    public static final class Configuration {
        private final V118OreMaterial state;
        private final int minimumRadius;
        private final int maximumRadius;
        private final int halfHeight;
        private final List<V118OreMaterial> targets;

        public Configuration(V118OreMaterial state, int minimumRadius, int maximumRadius,
                int halfHeight, V118OreMaterial... targets) {
            if (state == null || targets == null || targets.length == 0) {
                throw new IllegalArgumentException("state and targets are required");
            }
            if (minimumRadius < 0 || maximumRadius < minimumRadius || maximumRadius > 8
                    || halfHeight < 0 || halfHeight > 4) {
                throw new IllegalArgumentException("invalid Java 1.18.2 disk bounds");
            }
            this.state = state;
            this.minimumRadius = minimumRadius;
            this.maximumRadius = maximumRadius;
            this.halfHeight = halfHeight;
            this.targets = Collections.unmodifiableList(Arrays.asList(targets.clone()));
        }

        public V118OreMaterial state() {
            return state;
        }

        public int minimumRadius() {
            return minimumRadius;
        }

        public int maximumRadius() {
            return maximumRadius;
        }

        public int halfHeight() {
            return halfHeight;
        }

        public List<V118OreMaterial> targets() {
            return targets;
        }
    }

    public interface WorldAccess {
        V118OreMaterial getMaterial(int blockX, int blockY, int blockZ);

        void setMaterial(int blockX, int blockY, int blockZ, V118OreMaterial material);
    }
}

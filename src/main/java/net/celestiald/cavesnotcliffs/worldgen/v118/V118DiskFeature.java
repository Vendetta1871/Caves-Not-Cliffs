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
        return placeBase(new MaterialDiskAccess(world, configuration), random,
            configuration.minimumRadius, configuration.maximumRadius,
            configuration.halfHeight, originX, originY, originZ);
    }

    /** Shared body used by both DiskReplaceFeature and IcePatchFeature. */
    static <S> boolean placeBase(BaseDiskAccess<S> world, Random random,
            int minimumRadius, int maximumRadius, int halfHeight,
            int originX, int originY, int originZ) {
        boolean placed = false;
        int upperY = originY + halfHeight;
        int lowerY = originY - halfHeight - 1;
        boolean falling = world.outputFalls();
        int radius = minimumRadius + random.nextInt(maximumRadius - minimumRadius + 1);

        for (int x = originX - radius; x <= originX + radius; ++x) {
            for (int z = originZ - radius; z <= originZ + radius; ++z) {
                int offsetX = x - originX;
                int offsetZ = z - originZ;
                if (offsetX * offsetX + offsetZ * offsetZ > radius * radius) {
                    continue;
                }
                boolean replacedAbove = false;
                for (int y = upperY; y >= lowerY; --y) {
                    S current = world.getState(x, y, z);
                    boolean replaced = false;
                    if (y > lowerY && world.isTarget(current)) {
                        world.setOutput(x, y, z);
                        world.markAboveForPostProcessing(x, y, z);
                        placed = true;
                        replaced = true;
                    }
                    if (falling && replacedAbove && world.isAir(current)) {
                        world.setFallingSupport(x, y + 1, z);
                    }
                    replacedAbove = replaced;
                }
            }
        }
        return placed;
    }

    interface BaseDiskAccess<S> {
        S getState(int blockX, int blockY, int blockZ);

        boolean isTarget(S state);

        boolean isAir(S state);

        boolean outputFalls();

        void setOutput(int blockX, int blockY, int blockZ);

        void setFallingSupport(int blockX, int blockY, int blockZ);

        void markAboveForPostProcessing(int blockX, int blockY, int blockZ);
    }

    private static final class MaterialDiskAccess
            implements BaseDiskAccess<V118OreMaterial> {
        private final WorldAccess world;
        private final Configuration configuration;

        private MaterialDiskAccess(WorldAccess world, Configuration configuration) {
            this.world = world;
            this.configuration = configuration;
        }

        @Override
        public V118OreMaterial getState(int blockX, int blockY, int blockZ) {
            return world.getMaterial(blockX, blockY, blockZ);
        }

        @Override
        public boolean isTarget(V118OreMaterial state) {
            return configuration.targets.contains(state);
        }

        @Override
        public boolean isAir(V118OreMaterial state) {
            return state == V118OreMaterial.AIR;
        }

        @Override
        public boolean outputFalls() {
            return configuration.state == V118OreMaterial.SAND
                || configuration.state == V118OreMaterial.GRAVEL;
        }

        @Override
        public void setOutput(int blockX, int blockY, int blockZ) {
            world.setMaterial(blockX, blockY, blockZ, configuration.state);
        }

        @Override
        public void setFallingSupport(int blockX, int blockY, int blockZ) {
            // The only red-sand branch is not used by the scoped Overworld catalog.
            world.setMaterial(blockX, blockY, blockZ, V118OreMaterial.SANDSTONE);
        }

        @Override
        public void markAboveForPostProcessing(int blockX, int blockY, int blockZ) {
            world.markAboveForPostProcessing(blockX, blockY, blockZ);
        }
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

        default void markAboveForPostProcessing(int blockX, int blockY, int blockZ) {
        }
    }
}

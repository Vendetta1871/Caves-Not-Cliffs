package net.celestiald.cavesnotcliffs.worldgen.v118;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.Random;
import java.util.Set;

/** Exact Java 1.18.2 ice-spike and ice-patch surface-structure pair. */
public final class V118IceSurfacePlacements {
    public static final int SURFACE_STRUCTURES_STEP = 4;
    public static final int ICE_SPIKE_INDEX = 0;
    public static final int ICE_PATCH_INDEX = 1;

    private V118IceSurfacePlacements() {
    }

    public static void decorate(WorldAccess world, long worldSeed, int chunkX, int chunkZ,
            Set<V118Biome> regionBiomes) {
        if (world == null || regionBiomes == null) {
            throw new NullPointerException("world and regionBiomes are required");
        }
        if (!regionBiomes.contains(V118Biome.ICE_SPIKES)) {
            return;
        }

        V118WorldgenRandom random = new V118WorldgenRandom(0L);
        long decorationSeed = random.setDecorationSeed(worldSeed, chunkX << 4, chunkZ << 4);
        random.setFeatureSeed(decorationSeed, ICE_SPIKE_INDEX, SURFACE_STRUCTURES_STEP);
        placeAttempts(world, random, chunkX, chunkZ, 3, true);
        random.setFeatureSeed(decorationSeed, ICE_PATCH_INDEX, SURFACE_STRUCTURES_STEP);
        placeAttempts(world, random, chunkX, chunkZ, 2, false);
    }

    private static void placeAttempts(WorldAccess world, Random random, int chunkX, int chunkZ,
            int attempts, boolean spike) {
        int chunkOriginX = chunkX << 4;
        int chunkOriginZ = chunkZ << 4;
        for (int attempt = 0; attempt < attempts; ++attempt) {
            int x = chunkOriginX + random.nextInt(16);
            int z = chunkOriginZ + random.nextInt(16);
            int y = world.motionBlockingHeight(x, z);
            if (y <= world.minBuildHeight()) {
                continue;
            }
            BlockPos origin = new BlockPos(x, y, z);
            if (world.biomeAt(origin) != V118Biome.ICE_SPIKES) {
                continue;
            }
            if (spike) {
                placeIceSpike(world, random, origin);
            } else {
                placeIcePatch(world, random, origin);
            }
        }
    }

    private static boolean placeIceSpike(WorldAccess world, Random random, BlockPos origin) {
        BlockPos base = origin;
        while (world.stateAt(base) == State.AIR
                && base.getY() > world.minBuildHeight() + 2) {
            base = base.down();
        }
        if (world.stateAt(base) != State.SNOW_BLOCK) {
            return false;
        }

        base = base.up(random.nextInt(4));
        int height = random.nextInt(4) + 7;
        int baseRadius = height / 4 + random.nextInt(2);
        if (baseRadius > 1 && random.nextInt(60) == 0) {
            base = base.up(10 + random.nextInt(30));
        }

        for (int layer = 0; layer < height; ++layer) {
            float radius = (1.0F - (float) layer / (float) height) * (float) baseRadius;
            int ceilingRadius = MathHelper.ceil(radius);
            for (int offsetX = -ceilingRadius; offsetX <= ceilingRadius; ++offsetX) {
                float xDistance = (float) Math.abs(offsetX) - 0.25F;
                for (int offsetZ = -ceilingRadius; offsetZ <= ceilingRadius; ++offsetZ) {
                    float zDistance = (float) Math.abs(offsetZ) - 0.25F;
                    boolean outsideCircle = (offsetX != 0 || offsetZ != 0)
                        && xDistance * xDistance + zDistance * zDistance > radius * radius;
                    boolean perimeter = offsetX == -ceilingRadius
                        || offsetX == ceilingRadius || offsetZ == -ceilingRadius
                        || offsetZ == ceilingRadius;
                    if (outsideCircle || perimeter && random.nextFloat() > 0.75F) {
                        continue;
                    }

                    BlockPos upper = base.add(offsetX, layer, offsetZ);
                    if (isSpikeReplaceable(world.stateAt(upper))) {
                        world.setPackedIce(upper, 3);
                    }
                    if (layer != 0 && ceilingRadius > 1) {
                        BlockPos lower = base.add(offsetX, -layer, offsetZ);
                        if (isSpikeReplaceable(world.stateAt(lower))) {
                            world.setPackedIce(lower, 3);
                        }
                    }
                }
            }
        }

        int foundationRadius = Math.max(0, Math.min(1, baseRadius - 1));
        for (int offsetX = -foundationRadius; offsetX <= foundationRadius; ++offsetX) {
            for (int offsetZ = -foundationRadius; offsetZ <= foundationRadius; ++offsetZ) {
                BlockPos cursor = base.add(offsetX, -1, offsetZ);
                int run = 50;
                if (Math.abs(offsetX) == 1 && Math.abs(offsetZ) == 1) {
                    run = random.nextInt(5);
                }
                while (cursor.getY() > 50 && isFoundationReplaceable(world.stateAt(cursor))) {
                    world.setPackedIce(cursor, 3);
                    cursor = cursor.down();
                    if (--run > 0) {
                        continue;
                    }
                    cursor = cursor.down(random.nextInt(5) + 1);
                    run = random.nextInt(5);
                }
            }
        }
        return true;
    }

    private static boolean placeIcePatch(WorldAccess world, Random random, BlockPos origin) {
        BlockPos base = origin;
        while (world.stateAt(base) == State.AIR
                && base.getY() > world.minBuildHeight() + 2) {
            base = base.down();
        }
        if (world.stateAt(base) != State.SNOW_BLOCK) {
            return false;
        }
        return V118DiskFeature.placeBase(new IcePatchDiskAccess(world), random,
            2, 3, 1, base.getX(), base.getY(), base.getZ());
    }

    private static boolean isSpikeReplaceable(State state) {
        return state == State.AIR || state == State.PATCH_DIRT || state == State.DIRT
            || state == State.SNOW_BLOCK || state == State.ICE;
    }

    private static boolean isFoundationReplaceable(State state) {
        return isSpikeReplaceable(state) || state == State.PACKED_ICE;
    }

    private static final class IcePatchDiskAccess
            implements V118DiskFeature.BaseDiskAccess<State> {
        private final WorldAccess world;

        private IcePatchDiskAccess(WorldAccess world) {
            this.world = world;
        }

        @Override
        public State getState(int blockX, int blockY, int blockZ) {
            return world.stateAt(new BlockPos(blockX, blockY, blockZ));
        }

        @Override
        public boolean isTarget(State state) {
            return state == State.PATCH_DIRT || state == State.SNOW_BLOCK
                || state == State.ICE;
        }

        @Override
        public boolean isAir(State state) {
            return state == State.AIR;
        }

        @Override
        public boolean outputFalls() {
            return false;
        }

        @Override
        public void setOutput(int blockX, int blockY, int blockZ) {
            world.setPackedIce(new BlockPos(blockX, blockY, blockZ), 2);
        }

        @Override
        public void setFallingSupport(int blockX, int blockY, int blockZ) {
            throw new AssertionError("Packed ice is not a falling block");
        }

        @Override
        public void markAboveForPostProcessing(int blockX, int blockY, int blockZ) {
            world.markAboveForPostProcessing(new BlockPos(blockX, blockY, blockZ));
        }
    }

    public enum State {
        AIR,
        /** Dirt, grass, podzol, coarse dirt, or mycelium; also an ice-patch target. */
        PATCH_DIRT,
        /** Rooted dirt or moss; accepted only by IceSpikeFeature's dirt tag. */
        DIRT,
        SNOW_BLOCK,
        ICE,
        PACKED_ICE,
        OTHER
    }

    public interface WorldAccess {
        int minBuildHeight();

        int motionBlockingHeight(int blockX, int blockZ);

        V118Biome biomeAt(BlockPos pos);

        State stateAt(BlockPos pos);

        void setPackedIce(BlockPos pos, int flags);

        default void markAboveForPostProcessing(BlockPos pos) {
        }
    }
}

package net.celestiald.cavesnotcliffs.worldgen.v118;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.Set;

/** Exact Java 1.18.2 desert-well surface-structure placement and feature body. */
public final class V118DesertWellPlacements {
    public static final int SURFACE_STRUCTURES_STEP = 4;
    public static final int DESERT_WELL_INDEX = 2;

    private V118DesertWellPlacements() {
    }

    public static boolean decorate(WorldAccess world, long worldSeed, int chunkX, int chunkZ,
            Set<V118Biome> regionBiomes) {
        if (world == null || regionBiomes == null) {
            throw new NullPointerException("world and regionBiomes are required");
        }
        if (!regionBiomes.contains(V118Biome.DESERT)) {
            return false;
        }

        V118WorldgenRandom random = new V118WorldgenRandom(0L);
        long decorationSeed = random.setDecorationSeed(worldSeed, chunkX << 4, chunkZ << 4);
        random.setFeatureSeed(decorationSeed, DESERT_WELL_INDEX, SURFACE_STRUCTURES_STEP);
        if (random.nextFloat() >= 1.0F / 1000.0F) {
            return false;
        }

        int x = (chunkX << 4) + random.nextInt(16);
        int z = (chunkZ << 4) + random.nextInt(16);
        int y = world.motionBlockingHeight(x, z);
        if (y <= world.minBuildHeight()) {
            return false;
        }
        BlockPos origin = new BlockPos(x, y, z);
        return world.biomeAt(origin) == V118Biome.DESERT && place(world, origin);
    }

    private static boolean place(WorldAccess world, BlockPos origin) {
        BlockPos base = origin.up();
        while (world.isEmptyBlock(base) && base.getY() > world.minBuildHeight() + 2) {
            base = base.down();
        }
        if (!world.isOrdinarySand(base)) {
            return false;
        }

        for (int offsetX = -2; offsetX <= 2; ++offsetX) {
            for (int offsetZ = -2; offsetZ <= 2; ++offsetZ) {
                if (world.isEmptyBlock(base.add(offsetX, -1, offsetZ))
                        && world.isEmptyBlock(base.add(offsetX, -2, offsetZ))) {
                    return false;
                }
            }
        }

        for (int offsetY = -1; offsetY <= 0; ++offsetY) {
            for (int offsetX = -2; offsetX <= 2; ++offsetX) {
                for (int offsetZ = -2; offsetZ <= 2; ++offsetZ) {
                    world.setSandstone(base.add(offsetX, offsetY, offsetZ));
                }
            }
        }
        world.setSourceWater(base);
        for (EnumFacing direction : EnumFacing.Plane.HORIZONTAL) {
            world.setSourceWater(base.offset(direction));
        }
        for (int offsetX = -2; offsetX <= 2; ++offsetX) {
            for (int offsetZ = -2; offsetZ <= 2; ++offsetZ) {
                if (offsetX == -2 || offsetX == 2 || offsetZ == -2 || offsetZ == 2) {
                    world.setSandstone(base.add(offsetX, 1, offsetZ));
                }
            }
        }
        world.setBottomSandstoneSlab(base.add(2, 1, 0));
        world.setBottomSandstoneSlab(base.add(-2, 1, 0));
        world.setBottomSandstoneSlab(base.add(0, 1, 2));
        world.setBottomSandstoneSlab(base.add(0, 1, -2));
        for (int offsetX = -1; offsetX <= 1; ++offsetX) {
            for (int offsetZ = -1; offsetZ <= 1; ++offsetZ) {
                BlockPos roof = base.add(offsetX, 4, offsetZ);
                if (offsetX == 0 && offsetZ == 0) {
                    world.setSandstone(roof);
                } else {
                    world.setBottomSandstoneSlab(roof);
                }
            }
        }
        for (int offsetY = 1; offsetY <= 3; ++offsetY) {
            world.setSandstone(base.add(-1, offsetY, -1));
            world.setSandstone(base.add(-1, offsetY, 1));
            world.setSandstone(base.add(1, offsetY, -1));
            world.setSandstone(base.add(1, offsetY, 1));
        }
        return true;
    }

    public interface WorldAccess {
        int minBuildHeight();

        int motionBlockingHeight(int blockX, int blockZ);

        V118Biome biomeAt(BlockPos pos);

        boolean isEmptyBlock(BlockPos pos);

        boolean isOrdinarySand(BlockPos pos);

        void setSandstone(BlockPos pos);

        void setBottomSandstoneSlab(BlockPos pos);

        void setSourceWater(BlockPos pos);
    }
}

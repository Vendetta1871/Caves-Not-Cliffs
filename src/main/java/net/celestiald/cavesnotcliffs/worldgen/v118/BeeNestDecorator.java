package net.celestiald.cavesnotcliffs.worldgen.v118;

import net.celestiald.cavesnotcliffs.block.BlockBeehive;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.celestiald.cavesnotcliffs.tile.TileEntityBeehive;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Exact Java 1.18.2 BeehiveDecorator algorithm and a narrow 1.12 world bridge. */
public final class BeeNestDecorator {
    public static final float PROBABILITY_0002 = 0.002F;
    public static final float PROBABILITY_002 = 0.02F;
    public static final float PROBABILITY_005 = 0.05F;
    public static final float PROBABILITY_ALWAYS = 1.0F;
    public static final EnumFacing WORLDGEN_FACING = EnumFacing.SOUTH;
    private static final EnumFacing[] SPAWN_DIRECTIONS = {
        EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST
    };

    private BeeNestDecorator() {
    }

    public static PlacementResult place(WorldAccess world, Random random,
            float probability, List<BlockPos> trunkPositions,
            List<BlockPos> foliagePositions) {
        if (world == null || random == null || trunkPositions == null
                || foliagePositions == null) {
            throw new NullPointerException("Bee nest decorator arguments");
        }
        if (probability < 0.0F || probability > 1.0F || Float.isNaN(probability)) {
            throw new IllegalArgumentException("probability must be in [0,1]");
        }
        if (trunkPositions.isEmpty() || random.nextFloat() >= probability) {
            return PlacementResult.notPlaced();
        }
        int targetY;
        if (!foliagePositions.isEmpty()) {
            targetY = Math.max(foliagePositions.get(0).getY() - 1,
                    trunkPositions.get(0).getY() + 1);
        } else {
            targetY = Math.min(trunkPositions.get(0).getY() + 1
                    + random.nextInt(3),
                    trunkPositions.get(trunkPositions.size() - 1).getY());
        }
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos trunk : trunkPositions) {
            if (trunk.getY() != targetY) {
                continue;
            }
            for (EnumFacing direction : SPAWN_DIRECTIONS) {
                candidates.add(trunk.offset(direction));
            }
        }
        if (candidates.isEmpty()) {
            return PlacementResult.notPlaced();
        }
        // This apparently unusual no-Random overload is what the 1.18.2 bytecode calls.
        Collections.shuffle(candidates);
        BlockPos nest = null;
        for (BlockPos candidate : candidates) {
            if (world.isAir(candidate)
                    && world.isAir(candidate.offset(WORLDGEN_FACING))) {
                nest = candidate;
                break;
            }
        }
        if (nest == null || !world.placeNest(nest, WORLDGEN_FACING)) {
            return PlacementResult.notPlaced();
        }
        int bees = 2 + random.nextInt(2);
        for (int index = 0; index < bees; index++) {
            NBTTagCompound data = new NBTTagCompound();
            data.setString("id", CncRegistryIds.BEE.toString());
            world.storeBee(nest, data, random.nextInt(599), false);
        }
        return new PlacementResult(true, nest, bees);
    }

    public static PlacementResult placeInWorld(World world, Random random,
            float probability, List<BlockPos> trunkPositions,
            List<BlockPos> foliagePositions) {
        return place(new MinecraftWorldAccess(world), random, probability,
                trunkPositions, foliagePositions);
    }

    public interface WorldAccess {
        boolean isAir(BlockPos pos);

        boolean placeNest(BlockPos pos, EnumFacing facing);

        void storeBee(BlockPos pos, NBTTagCompound entityData, int ticksInHive,
                boolean hasNectar);
    }

    public static final class PlacementResult {
        private static final PlacementResult NOT_PLACED =
                new PlacementResult(false, null, 0);
        private final boolean placed;
        private final BlockPos pos;
        private final int occupants;

        private PlacementResult(boolean placed, BlockPos pos, int occupants) {
            this.placed = placed;
            this.pos = pos;
            this.occupants = occupants;
        }

        static PlacementResult notPlaced() {
            return NOT_PLACED;
        }

        public boolean placed() {
            return placed;
        }

        public BlockPos pos() {
            return pos;
        }

        public int occupants() {
            return occupants;
        }
    }

    private static final class MinecraftWorldAccess implements WorldAccess {
        private final World world;

        MinecraftWorldAccess(World world) {
            this.world = world;
        }

        @Override
        public boolean isAir(BlockPos pos) {
            return world.isAirBlock(pos);
        }

        @Override
        public boolean placeNest(BlockPos pos, EnumFacing facing) {
            if (BlockBeehive.beeNest == null) {
                return false;
            }
            IBlockState state = BlockBeehive.beeNest.getDefaultState()
                    .withProperty(BlockBeehive.BlockCustom.FACING, facing);
            return world.setBlockState(pos, state, 3);
        }

        @Override
        public void storeBee(BlockPos pos, NBTTagCompound entityData,
                int ticksInHive, boolean hasNectar) {
            TileEntity tile = world.getTileEntity(pos);
            if (!(tile instanceof TileEntityBeehive)) {
                throw new IllegalStateException("Bee nest at " + pos
                        + " has no beehive tile entity");
            }
            ((TileEntityBeehive) tile).storeBee(entityData, ticksInHive, hasNectar);
        }
    }
}

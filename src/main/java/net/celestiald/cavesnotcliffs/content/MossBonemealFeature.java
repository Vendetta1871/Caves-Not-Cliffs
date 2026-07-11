package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/** Exact MOSS_PATCH_BONEMEAL vegetation-patch configuration from Java 1.18.2. */
public final class MossBonemealFeature {
    private MossBonemealFeature() {
    }

    public static boolean place(World world, Random random, BlockPos origin, Block moss,
            Block mossCarpet, Block azalea, Block floweringAzalea) {
        int radiusX = 1 + uniform(random, 1, 2);
        int radiusZ = 1 + uniform(random, 1, 2);
        Set<BlockPos> ground = new HashSet<>();

        for (int dx = -radiusX; dx <= radiusX; dx++) {
            boolean xEdge = dx == -radiusX || dx == radiusX;
            for (int dz = -radiusZ; dz <= radiusZ; dz++) {
                boolean zEdge = dz == -radiusZ || dz == radiusZ;
                boolean edge = xEdge || zEdge;
                boolean corner = xEdge && zEdge;
                if (corner || edge && random.nextFloat()
                        > LushCaveMechanics.MOSS_BONEMEAL_EDGE_CHANCE) {
                    continue;
                }

                BlockPos air = origin.add(dx, 0, dz);
                int moved = 0;
                while (world.isAirBlock(air)
                        && moved < LushCaveMechanics.MOSS_BONEMEAL_VERTICAL_RANGE) {
                    air = air.down();
                    moved++;
                }
                moved = 0;
                while (!world.isAirBlock(air)
                        && moved < LushCaveMechanics.MOSS_BONEMEAL_VERTICAL_RANGE) {
                    air = air.up();
                    moved++;
                }

                BlockPos support = air.down();
                IBlockState supportState = world.getBlockState(support);
                if (!world.isAirBlock(air)
                        || !supportState.isSideSolid(world, support, EnumFacing.UP)
                        || !isMossReplaceable(supportState)) {
                    continue;
                }
                if (supportState.getBlock() != moss) {
                    world.setBlockState(support, moss.getDefaultState(), 2);
                }
                ground.add(support);
            }
        }

        for (BlockPos support : ground) {
            if (random.nextFloat()
                    >= LushCaveMechanics.MOSS_BONEMEAL_VEGETATION_CHANCE) {
                continue;
            }
            placeVegetation(world, random, support.up(), mossCarpet, azalea,
                    floweringAzalea);
        }
        return !ground.isEmpty();
    }

    private static void placeVegetation(World world, Random random, BlockPos position,
            Block mossCarpet, Block azalea, Block floweringAzalea) {
        int value = random.nextInt(96);
        IBlockState state;
        if (value < 4) {
            state = floweringAzalea.getDefaultState();
        } else if (value < 11) {
            state = azalea.getDefaultState();
        } else if (value < 36) {
            state = mossCarpet.getDefaultState();
        } else if (value < 86) {
            state = Blocks.TALLGRASS.getStateFromMeta(1);
        } else {
            if (!world.isAirBlock(position.up())) {
                return;
            }
            Blocks.DOUBLE_PLANT.placeAt(world, position,
                    BlockDoublePlant.EnumPlantType.GRASS, 2);
            return;
        }
        if (world.isAirBlock(position) && state.getBlock().canPlaceBlockAt(world, position)) {
            world.setBlockState(position, state, 2);
        }
    }

    public static boolean isMossReplaceable(IBlockState state) {
        ResourceLocation id = state.getBlock().getRegistryName();
        if (id == null) {
            return false;
        }
        String domain = id.getResourceDomain();
        String path = id.getResourcePath();
        if ("minecraft".equals(domain)) {
            return "stone".equals(path) || "dirt".equals(path)
                    || "grass".equals(path) || "mycelium".equals(path);
        }
        if (!CavesNotCliffs.MODID.equals(domain)) {
            return false;
        }
        return "deepslate".equals(path) || "tuff".equals(path)
                || "rooted_dirt".equals(path) || "moss_block".equals(path)
                || LushCaveMechanics.isCaveVine(path);
    }

    private static int uniform(Random random, int min, int max) {
        return min + random.nextInt(max - min + 1);
    }
}

package net.celestiald.cavesnotcliffs.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import net.celestiald.cavesnotcliffs.block.BlockBabyAzaleaTree;
import net.celestiald.cavesnotcliffs.block.BlockBabyDripleaf;
import net.celestiald.cavesnotcliffs.block.BlockBloomingBabyAzaleaTree;
import net.celestiald.cavesnotcliffs.block.BlockDarkStone;
import net.celestiald.cavesnotcliffs.block.BlockDripleafPlant;
import net.celestiald.cavesnotcliffs.block.BlockDripstone;
import net.celestiald.cavesnotcliffs.block.BlockPointedDripstone;
import net.celestiald.cavesnotcliffs.block.BlockGlowBerryMiddleFill;
import net.celestiald.cavesnotcliffs.block.BlockGlowBerryVines;
import net.celestiald.cavesnotcliffs.block.BlockMoss;
import net.celestiald.cavesnotcliffs.block.BlockMossLayer;
import net.celestiald.cavesnotcliffs.block.BlockSporeBlossom;
import net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics.Thickness;
import net.celestiald.cavesnotcliffs.block.BlockUnnamedStone;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

/** Applies one coherent cave-biome identity to all exposed surfaces in a populated cube. */
final class CaveBiomeDecorator {
    private CaveBiomeDecorator() {
    }

    static void decorate(World world, Random random, CubePos cube) {
        int minY = Math.max(CavesNotCliffsWorldType.MIN_HEIGHT + 1, cube.getMinBlockY());
        int maxY = Math.min(63, cube.getMaxBlockY());
        if (minY > maxY) {
            return;
        }

        int minX = cube.getMinBlockX();
        int maxX = cube.getMaxBlockX();
        int minZ = cube.getMinBlockZ();
        int maxZ = cube.getMaxBlockZ();

        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    BlockPos position = new BlockPos(x, y, z);
                    IBlockState state = world.getBlockState(position);
                    if (!isCaveRock(state.getBlock())) {
                        continue;
                    }

                    boolean airAbove = world.isAirBlock(position.up());
                    boolean airBelow = world.isAirBlock(position.down());
                    boolean exposedSide = world.isAirBlock(position.north())
                            || world.isAirBlock(position.south())
                            || world.isAirBlock(position.east())
                            || world.isAirBlock(position.west());
                    if (!airAbove && !airBelow && !exposedSide) {
                        continue;
                    }

                    CaveBiomeSampler.Type type = CaveBiomeSampler.sample(world.getSeed(), x, y, z);
                    if (type == CaveBiomeSampler.Type.LUSH) {
                        decorateLush(world, random, cube, position, airAbove, airBelow, exposedSide);
                    } else if (type == CaveBiomeSampler.Type.DRIPSTONE) {
                        decorateDripstone(world, random, cube, position, airAbove, airBelow, exposedSide);
                    }
                }
            }
        }
    }

    private static boolean isCaveRock(Block block) {
        return block == Blocks.STONE || block == BlockUnnamedStone.block || block == BlockDarkStone.block;
    }

    private static void decorateLush(World world, Random random, CubePos cube, BlockPos rock,
            boolean airAbove, boolean airBelow, boolean exposedSide) {
        if (BlockMoss.block != null) {
            double mossChance = airAbove || airBelow ? 0.88 : (exposedSide ? 0.62 : 0.0);
            if (random.nextDouble() < mossChance) {
                world.setBlockState(rock, BlockMoss.block.getDefaultState(), 2);
            }
        }

        if (airAbove && cube.containsBlock(rock.up())) {
            BlockPos plant = rock.up();
            double roll = random.nextDouble();
            if (roll < 0.018 && BlockBabyAzaleaTree.block != null) {
                world.setBlockState(plant, BlockBabyAzaleaTree.block.getDefaultState(), 2);
            } else if (roll < 0.028 && BlockBloomingBabyAzaleaTree.block != null) {
                world.setBlockState(plant, BlockBloomingBabyAzaleaTree.block.getDefaultState(), 2);
            } else if (roll < 0.050 && BlockBabyDripleaf.block != null) {
                world.setBlockState(plant, BlockBabyDripleaf.block.getDefaultState(), 2);
            } else if (roll < 0.062 && BlockDripleafPlant.block != null) {
                world.setBlockState(plant, BlockDripleafPlant.block.getDefaultState(), 2);
            } else if (roll < 0.20 && BlockMossLayer.block != null) {
                world.setBlockState(plant, BlockMossLayer.block.getDefaultState(), 2);
            }
        }

        if (airBelow && cube.containsBlock(rock.down())) {
            BlockPos hanging = rock.down();
            double roll = random.nextDouble();
            if (roll < 0.018 && BlockSporeBlossom.block != null) {
                world.setBlockState(hanging, BlockSporeBlossom.block.getDefaultState(), 2);
            } else if (roll < 0.11) {
                generateGlowBerries(world, random, cube, hanging, 1 + random.nextInt(4));
            }
        }
    }

    private static void generateGlowBerries(World world, Random random, CubePos cube,
            BlockPos start, int length) {
        if (BlockGlowBerryVines.block == null || BlockGlowBerryMiddleFill.block == null) {
            return;
        }
        BlockPos current = start;
        for (int index = 0; index < length && cube.containsBlock(current)
                && world.isAirBlock(current); index++) {
            Block block = index == length - 1 || random.nextDouble() < 0.35
                    ? BlockGlowBerryVines.block : BlockGlowBerryMiddleFill.block;
            world.setBlockState(current, block.getDefaultState(), 2);
            current = current.down();
        }
    }

    private static void decorateDripstone(World world, Random random, CubePos cube, BlockPos rock,
            boolean airAbove, boolean airBelow, boolean exposedSide) {
        if (BlockDripstone.block != null) {
            double chance = airAbove || airBelow ? 0.92 : (exposedSide ? 0.68 : 0.0);
            if (random.nextDouble() < chance) {
                world.setBlockState(rock, BlockDripstone.block.getDefaultState(), 2);
            }
        }
        if (airBelow && cube.containsBlock(rock.down()) && random.nextDouble() < 0.24) {
            placeStalactite(world, cube, rock.down(), 1 + random.nextInt(3));
        }
        if (airAbove && cube.containsBlock(rock.up()) && random.nextDouble() < 0.22) {
            placeStalagmite(world, cube, rock.up(), 1 + random.nextInt(3));
        }
    }

    private static void placeStalactite(World world, CubePos cube, BlockPos start, int requestedLength) {
        int length = availableAir(world, cube, start, requestedLength, -1);
        if (length <= 0) {
            return;
        }
        for (int index = 0; index < length; ++index) {
            Thickness thickness = segmentThickness(index, length);
            BlockPointedDripstone.createDripstone(world, start.down(index),
                    EnumFacing.DOWN, thickness);
        }
    }

    private static void placeStalagmite(World world, CubePos cube, BlockPos start, int requestedLength) {
        int length = availableAir(world, cube, start, requestedLength, 1);
        if (length <= 0) {
            return;
        }
        for (int index = 0; index < length; ++index) {
            Thickness thickness = segmentThickness(index, length);
            BlockPointedDripstone.createDripstone(world, start.up(index),
                    EnumFacing.UP, thickness);
        }
    }

    private static Thickness segmentThickness(int index, int length) {
        if (index == length - 1) {
            return Thickness.TIP;
        }
        if (index == 0) {
            return length == 2 ? Thickness.FRUSTUM : Thickness.BASE;
        }
        return Thickness.MIDDLE;
    }

    private static int availableAir(World world, CubePos cube, BlockPos start,
            int requestedLength, int direction) {
        int available = 0;
        BlockPos current = start;
        while (available < requestedLength && cube.containsBlock(current)
                && world.isAirBlock(current)) {
            available++;
            current = direction < 0 ? current.down() : current.up();
        }
        return available;
    }
}

package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.block.BlockDarkStone;
import net.celestiald.cavesnotcliffs.block.LushCaveVinesBlock;
import net.celestiald.cavesnotcliffs.block.LushDripleafBlocks;
import net.celestiald.cavesnotcliffs.content.LushCaveContent;
import net.celestiald.cavesnotcliffs.block.BlockDripstone;
import net.celestiald.cavesnotcliffs.block.BlockPointedDripstone;
import net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics.Thickness;
import net.celestiald.cavesnotcliffs.block.BlockUnnamedStone;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Applies one coherent cave-biome identity to all exposed surfaces in a populated section. */
final class CaveBiomeDecorator {
    private CaveBiomeDecorator() {
    }

    static void decorate(World world, Random random, FiniteSectionPos cube) {
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

    private static void decorateLush(World world, Random random, FiniteSectionPos cube, BlockPos rock,
            boolean airAbove, boolean airBelow, boolean exposedSide) {
        if (LushCaveContent.MOSS_BLOCK != null) {
            double mossChance = airAbove || airBelow ? 0.88 : (exposedSide ? 0.62 : 0.0);
            if (random.nextDouble() < mossChance) {
                world.setBlockState(rock, LushCaveContent.MOSS_BLOCK.getDefaultState(), 2);
            }
        }

        if (airAbove && cube.containsBlock(rock.up())) {
            BlockPos plant = rock.up();
            double roll = random.nextDouble();
            if (roll < 0.018 && LushCaveContent.AZALEA != null) {
                world.setBlockState(plant, LushCaveContent.AZALEA.getDefaultState(), 2);
            } else if (roll < 0.028 && LushCaveContent.FLOWERING_AZALEA != null) {
                world.setBlockState(plant, LushCaveContent.FLOWERING_AZALEA.getDefaultState(), 2);
            } else if (roll < 0.050 && LushCaveContent.SMALL_DRIPLEAF != null) {
                placeSmallDripleaf(world, cube, plant);
            } else if (roll < 0.062 && LushCaveContent.BIG_DRIPLEAF != null) {
                placeBigDripleaf(world, plant);
            } else if (roll < 0.20 && LushCaveContent.MOSS_CARPET != null) {
                world.setBlockState(plant, LushCaveContent.MOSS_CARPET.getDefaultState(), 2);
            }
        }

        if (airBelow && cube.containsBlock(rock.down())) {
            BlockPos hanging = rock.down();
            double roll = random.nextDouble();
            if (roll < 0.018 && LushCaveContent.SPORE_BLOSSOM != null) {
                world.setBlockState(hanging, LushCaveContent.SPORE_BLOSSOM.getDefaultState(), 2);
            } else if (roll < 0.11) {
                generateGlowBerries(world, random, cube, hanging, 1 + random.nextInt(4));
            }
        }
    }

    private static void placeSmallDripleaf(World world, FiniteSectionPos cube, BlockPos lower) {
        BlockPos upper = lower.up();
        if (!cube.containsBlock(upper) || !world.isAirBlock(lower)
                || !world.isAirBlock(upper)
                || !LushCaveContent.SMALL_DRIPLEAF.canPlaceBlockAt(world, lower)) {
            return;
        }
        IBlockState base = LushCaveContent.SMALL_DRIPLEAF.getDefaultState()
                .withProperty(LushDripleafBlocks.FACING, EnumFacing.NORTH)
                .withProperty(LushDripleafBlocks.WATERLOGGED, false);
        world.setBlockState(lower,
                base.withProperty(LushDripleafBlocks.HALF,
                        LushDripleafBlocks.Half.LOWER), 2);
        world.setBlockState(upper,
                base.withProperty(LushDripleafBlocks.HALF,
                        LushDripleafBlocks.Half.UPPER), 2);
    }

    private static void placeBigDripleaf(World world, BlockPos position) {
        if (!LushCaveContent.BIG_DRIPLEAF.canPlaceBlockAt(world, position)) {
            return;
        }
        world.setBlockState(position, LushDripleafBlocks.headState(
                EnumFacing.NORTH, false, LushDripleafBlocks.Tilt.NONE), 2);
    }

    private static void generateGlowBerries(World world, Random random, FiniteSectionPos cube,
            BlockPos start, int length) {
        if (LushCaveContent.CAVE_VINES_PLANT == null
                || LushCaveContent.CAVE_VINES_AGE_24_25 == null) {
            return;
        }
        List<BlockPos> positions = new ArrayList<BlockPos>();
        List<Boolean> berries = new ArrayList<Boolean>();
        BlockPos current = start;
        for (int index = 0; index < length && cube.containsBlock(current)
                && world.isAirBlock(current); index++) {
            // Preserve the draft-v2 RNG schedule exactly. The old head identity represented a
            // berry-bearing segment; canonical topology instead reserves Head for the bottom.
            boolean berrySegment = legacyVineBerryRoll(random, index, length);
            positions.add(current);
            berries.add(berrySegment);
            current = current.down();
        }
        for (int index = 0; index < positions.size(); index++) {
            boolean head = index == positions.size() - 1;
            IBlockState state = head
                    ? LushCaveVinesBlock.headState(25, berries.get(index))
                    : LushCaveVinesBlock.bodyState(berries.get(index));
            world.setBlockState(positions.get(index), state, 2);
        }
    }

    static boolean legacyVineBerryRoll(Random random, int index, int length) {
        return index == length - 1 || random.nextDouble() < 0.35D;
    }

    private static void decorateDripstone(World world, Random random, FiniteSectionPos cube, BlockPos rock,
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

    private static void placeStalactite(World world, FiniteSectionPos cube, BlockPos start, int requestedLength) {
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

    private static void placeStalagmite(World world, FiniteSectionPos cube, BlockPos start, int requestedLength) {
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

    private static int availableAir(World world, FiniteSectionPos cube, BlockPos start,
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

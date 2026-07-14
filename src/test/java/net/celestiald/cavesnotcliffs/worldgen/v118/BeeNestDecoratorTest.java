package net.celestiald.cavesnotcliffs.worldgen.v118;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BeeNestDecoratorTest {
    @Test
    public void probabilitiesAndFacingMatchTreeFeaturesOracle() {
        assertEquals(0.002F, BeeNestDecorator.PROBABILITY_0002, 0.0F);
        assertEquals(0.02F, BeeNestDecorator.PROBABILITY_002, 0.0F);
        assertEquals(0.05F, BeeNestDecorator.PROBABILITY_005, 0.0F);
        assertEquals(1.0F, BeeNestDecorator.PROBABILITY_ALWAYS, 0.0F);
        assertEquals(EnumFacing.SOUTH, BeeNestDecorator.WORLDGEN_FACING);
    }

    @Test
    public void failedProbabilityConsumesOnlyTheProbabilityRoll() {
        FakeWorld world = new FakeWorld();
        FixedRandom random = new FixedRandom(0.5F);
        BeeNestDecorator.PlacementResult result = BeeNestDecorator.place(world,
                random, 0.05F, Collections.singletonList(new BlockPos(0, 1, 0)),
                Collections.<BlockPos>emptyList());
        assertFalse(result.placed());
        assertEquals(1, random.floatCalls);
        assertEquals(0, random.intCalls);
        assertTrue(world.bees.isEmpty());
    }

    @Test
    public void foliageHeightAirGateAndPreloadedBeeNbtAreExact() {
        BlockPos trunk = new BlockPos(0, 2, 0);
        BlockPos nest = trunk.east();
        FakeWorld world = new FakeWorld();
        world.air.add(nest);
        world.air.add(nest.south());
        FixedRandom random = new FixedRandom(0.0F, 1, 10, 20, 30);

        BeeNestDecorator.PlacementResult result = BeeNestDecorator.place(world,
                random, 1.0F,
                Arrays.asList(new BlockPos(0, 1, 0), trunk,
                        new BlockPos(0, 3, 0), new BlockPos(0, 4, 0)),
                Collections.singletonList(new BlockPos(0, 3, 0)));

        assertTrue(result.placed());
        assertEquals(nest, result.pos());
        assertEquals(3, result.occupants());
        assertEquals(EnumFacing.SOUTH, world.facing);
        assertEquals(Arrays.asList(10, 20, 30), world.ticks);
        assertEquals(3, world.bees.size());
        for (NBTTagCompound bee : world.bees) {
            assertEquals("cavesnotcliffs:bee", bee.getString("id"));
            assertFalse(bee.getBoolean("HasNectar"));
        }
    }

    @Test
    public void noFoliageBranchUsesOneToThreeBlocksAboveFirstTrunkAndCapsAtTop() {
        FakeWorld world = new FakeWorld();
        BlockPos nest = new BlockPos(0, 2, 0).east();
        world.air.add(nest);
        world.air.add(nest.south());
        FixedRandom random = new FixedRandom(0.0F, 2, 0, 0, 0);
        BeeNestDecorator.PlacementResult result = BeeNestDecorator.place(world,
                random, 1.0F, Arrays.asList(new BlockPos(0, 1, 0),
                        new BlockPos(0, 2, 0)), Collections.<BlockPos>emptyList());
        assertTrue(result.placed());
        assertEquals(nest, result.pos());
        assertEquals(2, result.occupants());
    }

    @Test
    public void candidateShuffleIsStableAcrossInputOrderAndUnrelatedJvmShuffles() {
        List<BlockPos> trunks = Arrays.asList(new BlockPos(0, 1, 0),
                new BlockPos(0, 2, 0), new BlockPos(1, 2, 0));
        List<BlockPos> foliage = Collections.singletonList(new BlockPos(0, 3, 0));
        FakeWorld first = allCandidateSidesAir(trunks, 2);
        BeeNestDecorator.PlacementResult firstResult = BeeNestDecorator.place(
                first, new Random(42L), 1.0F, trunks, foliage);

        List<Integer> unrelated = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        Collections.shuffle(unrelated);
        List<BlockPos> reversed = Arrays.asList(trunks.get(0), trunks.get(2),
                trunks.get(1));
        FakeWorld second = allCandidateSidesAir(reversed, 2);
        BeeNestDecorator.PlacementResult secondResult = BeeNestDecorator.place(
                second, new Random(42L), 1.0F, reversed, foliage);

        assertTrue(firstResult.placed());
        assertTrue(secondResult.placed());
        assertEquals(firstResult.pos(), secondResult.pos());
        assertEquals(firstResult.occupants(), secondResult.occupants());
    }

    private static FakeWorld allCandidateSidesAir(List<BlockPos> trunks, int targetY) {
        FakeWorld world = new FakeWorld();
        for (BlockPos trunk : trunks) {
            if (trunk.getY() != targetY) {
                continue;
            }
            for (EnumFacing facing : Arrays.asList(EnumFacing.EAST,
                    EnumFacing.SOUTH, EnumFacing.WEST)) {
                BlockPos candidate = trunk.offset(facing);
                world.air.add(candidate);
                world.air.add(candidate.south());
            }
        }
        return world;
    }

    private static final class FakeWorld implements BeeNestDecorator.WorldAccess {
        final Set<BlockPos> air = new HashSet<>();
        final List<NBTTagCompound> bees = new ArrayList<>();
        final List<Integer> ticks = new ArrayList<>();
        EnumFacing facing;

        @Override
        public boolean isAir(BlockPos pos) {
            return air.contains(pos);
        }

        @Override
        public boolean placeNest(BlockPos pos, EnumFacing facing) {
            this.facing = facing;
            return true;
        }

        @Override
        public void storeBee(BlockPos pos, NBTTagCompound entityData,
                int ticksInHive, boolean hasNectar) {
            NBTTagCompound copy = entityData.copy();
            copy.setBoolean("HasNectar", hasNectar);
            bees.add(copy);
            ticks.add(ticksInHive);
        }
    }

    private static final class FixedRandom extends Random {
        final float value;
        final int[] ints;
        int floatCalls;
        int intCalls;

        FixedRandom(float value, int... ints) {
            this.value = value;
            this.ints = ints;
        }

        @Override
        public float nextFloat() {
            floatCalls++;
            return value;
        }

        @Override
        public int nextInt(int bound) {
            if (intCalls >= ints.length) {
                throw new AssertionError("unexpected nextInt(" + bound + ")");
            }
            return Math.floorMod(ints[intCalls++], bound);
        }
    }
}

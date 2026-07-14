package net.celestiald.cavesnotcliffs.worldgen.v118;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118DefaultSpringPlacements.DecorationResult;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118DefaultSpringPlacements.SpringFluid;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class V118DefaultSpringPlacementsTest {
    @Test
    public void catalogPinsTheCompleteOfficialFluidSpringPrefix() {
        assertEquals(8, V118DefaultSpringPlacements.FLUID_SPRINGS_STEP);
        assertEquals(0, V118DefaultSpringPlacements.SPRING_WATER_INDEX);
        assertEquals(1, V118DefaultSpringPlacements.SPRING_LAVA_INDEX);
        assertEquals(2, V118MountainSurfacePlacements.SPRING_LAVA_FROZEN_INDEX);
        assertEquals(25, V118DefaultSpringPlacements.WATER_ATTEMPTS);
        assertEquals(20, V118DefaultSpringPlacements.LAVA_ATTEMPTS);
        assertEquals(192, V118DefaultSpringPlacements.WATER_MAX_Y);
        assertEquals(50, V118Biome.values().length);
        for (V118Biome biome : V118Biome.values()) {
            assertTrue(biome.id(),
                V118DefaultSpringPlacements.supportsDefaultSpring(biome));
        }
    }

    @Test
    public void committedOfficialRuntimeOracleMatchesEveryCandidate() throws IOException {
        InputStream stream = getClass().getResourceAsStream(
            "/net/celestiald/cavesnotcliffs/worldgen/v118/"
                + "default-spring-oracle-1.18.2.tsv");
        assertTrue(stream != null);
        BufferedReader reader = new BufferedReader(new InputStreamReader(
            stream, StandardCharsets.UTF_8));
        RecordingWorld world = new RecordingWorld();
        String line;
        int cases = 0;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }
            String[] fields = line.split("\t");
            if ("feature".equals(fields[0])) {
                assertFeatureSlot(fields);
            } else if ("biomes".equals(fields[0])) {
                assertEquals(V118Biome.values().length, Integer.parseInt(fields[1]));
                assertEquals("spring_water,spring_lava", fields[2]);
            } else if ("case".equals(fields[0])) {
                long seed = Long.parseLong(fields[1]);
                int chunkX = Integer.parseInt(fields[2]);
                int chunkZ = Integer.parseInt(fields[3]);
                SpringFluid fluid = oracleFluid(fields[4]);
                List<BlockPos> expected = positions(fields[6]);
                assertEquals(Integer.parseInt(fields[5]), expected.size());
                assertEquals(expected, V118DefaultSpringPlacements.candidates(
                    world, seed, chunkX, chunkZ, fluid));
                cases++;
            } else {
                throw new AssertionError("Unknown spring oracle row: " + fields[0]);
            }
        }
        reader.close();
        assertEquals(12, cases);
    }

    @Test
    public void configuredFeatureWritesAndSchedulesExactSourceFluid() {
        BlockPos origin = new BlockPos(5, 20, 9);
        RecordingWorld water = successfulWorld(origin, Cell.WATER_VALID);
        assertTrue(V118DefaultSpringPlacements.placeSpring(
            water, origin, SpringFluid.WATER));
        assertEquals(Cell.WATER, water.cell(origin));
        assertEquals(Collections.singletonList(new Scheduled(origin, SpringFluid.WATER)),
            water.scheduled);

        RecordingWorld lava = successfulWorld(origin, Cell.BASE_VALID);
        assertTrue(V118DefaultSpringPlacements.placeSpring(
            lava, origin, SpringFluid.LAVA));
        assertEquals(Cell.LAVA, lava.cell(origin));
        assertEquals(Collections.singletonList(new Scheduled(origin, SpringFluid.LAVA)),
            lava.scheduled);

        RecordingWorld airOrigin = successfulWorld(origin, Cell.BASE_VALID);
        airOrigin.base(origin, Cell.AIR);
        assertTrue(V118DefaultSpringPlacements.placeSpring(
            airOrigin, origin, SpringFluid.WATER));
        assertEquals(Cell.WATER, airOrigin.cell(origin));
    }

    @Test
    public void configuredFeatureRejectsEveryInvalidShape() {
        BlockPos origin = new BlockPos(5, 20, 9);

        RecordingWorld invalidAbove = successfulWorld(origin, Cell.BASE_VALID);
        invalidAbove.base(origin.up(), Cell.INVALID);
        assertRejected(invalidAbove, origin);

        RecordingWorld invalidBelow = successfulWorld(origin, Cell.BASE_VALID);
        invalidBelow.base(origin.down(), Cell.INVALID);
        assertRejected(invalidBelow, origin);

        RecordingWorld invalidOrigin = successfulWorld(origin, Cell.BASE_VALID);
        invalidOrigin.base(origin, Cell.INVALID);
        assertRejected(invalidOrigin, origin);

        RecordingWorld tooFewRocks = successfulWorld(origin, Cell.BASE_VALID);
        tooFewRocks.base(origin.east(), Cell.INVALID);
        assertRejected(tooFewRocks, origin);

        RecordingWorld noHole = successfulWorld(origin, Cell.BASE_VALID);
        noHole.base(origin.west(), Cell.BASE_VALID);
        assertRejected(noHole, origin);

        RecordingWorld twoHoles = successfulWorld(origin, Cell.BASE_VALID);
        twoHoles.base(origin.east(), Cell.AIR);
        assertRejected(twoHoles, origin);
    }

    @Test
    public void waterOnlyValidBlocksAreNeverAcceptedByLava() {
        BlockPos origin = new BlockPos(5, 20, 9);
        RecordingWorld world = successfulWorld(origin, Cell.WATER_VALID);
        assertFalse(V118DefaultSpringPlacements.placeSpring(
            world, origin, SpringFluid.LAVA));
        assertTrue(world.writes.isEmpty());
        assertTrue(world.scheduled.isEmpty());
    }

    @Test
    public void decorationRunsAllWaterCandidatesBeforeAnyLavaCandidate() {
        RecordingWorld world = new RecordingWorld();
        BlockPos water = V118DefaultSpringPlacements.candidates(
            world, 0L, 0, 0, SpringFluid.WATER).get(0);
        BlockPos lava = V118DefaultSpringPlacements.candidates(
            world, 0L, 0, 0, SpringFluid.LAVA).get(0);
        arrangeSuccess(world, water, Cell.BASE_VALID);
        arrangeSuccess(world, lava, Cell.BASE_VALID);

        DecorationResult result = V118DefaultSpringPlacements.decorate(
            world, 0L, 0, 0, EnumSet.of(V118Biome.PLAINS));

        assertEquals(25, result.waterAttempts());
        assertEquals(20, result.lavaAttempts());
        assertEquals(1, result.waterSpringsPlaced());
        assertEquals(1, result.lavaSpringsPlaced());
        assertEquals(java.util.Arrays.asList(SpringFluid.WATER, SpringFluid.LAVA),
            world.writeOrder);
        assertEquals(java.util.Arrays.asList(
            new Scheduled(water, SpringFluid.WATER),
            new Scheduled(lava, SpringFluid.LAVA)), world.scheduled);
        List<BlockPos> expectedQueries = new ArrayList<BlockPos>(
            V118DefaultSpringPlacements.candidates(
                world, 0L, 0, 0, SpringFluid.WATER));
        expectedQueries.addAll(V118DefaultSpringPlacements.candidates(
            world, 0L, 0, 0, SpringFluid.LAVA));
        assertEquals(expectedQueries, world.biomeQueries);
    }

    @Test
    public void absentRegionBiomeSkipsBothGlobalFeatures() {
        RecordingWorld world = new RecordingWorld();
        DecorationResult result = V118DefaultSpringPlacements.decorate(
            world, 0L, 0, 0, Collections.<V118Biome>emptySet());
        assertEquals(0, result.waterAttempts());
        assertEquals(0, result.lavaAttempts());
        assertTrue(world.biomeQueries.isEmpty());
        assertTrue(world.writes.isEmpty());
    }

    private static void assertFeatureSlot(String[] fields) {
        int step = Integer.parseInt(fields[2]);
        int index = Integer.parseInt(fields[3]);
        assertEquals(8, step);
        if ("spring_water".equals(fields[1])) {
            assertEquals(V118DefaultSpringPlacements.SPRING_WATER_INDEX, index);
        } else if ("spring_lava".equals(fields[1])) {
            assertEquals(V118DefaultSpringPlacements.SPRING_LAVA_INDEX, index);
        } else if ("spring_lava_frozen".equals(fields[1])) {
            assertEquals(V118MountainSurfacePlacements.SPRING_LAVA_FROZEN_INDEX, index);
        } else {
            throw new AssertionError("Unknown spring oracle feature: " + fields[1]);
        }
    }

    private static SpringFluid oracleFluid(String id) {
        if ("spring_water".equals(id)) {
            return SpringFluid.WATER;
        }
        if ("spring_lava".equals(id)) {
            return SpringFluid.LAVA;
        }
        throw new AssertionError("Unknown spring fluid: " + id);
    }

    private static List<BlockPos> positions(String encoded) {
        List<BlockPos> result = new ArrayList<BlockPos>();
        for (String position : encoded.split(";")) {
            String[] values = position.split(",");
            result.add(new BlockPos(Integer.parseInt(values[0]),
                Integer.parseInt(values[1]), Integer.parseInt(values[2])));
        }
        return result;
    }

    private static RecordingWorld successfulWorld(BlockPos origin, Cell valid) {
        RecordingWorld world = new RecordingWorld();
        arrangeSuccess(world, origin, valid);
        return world;
    }

    private static void arrangeSuccess(RecordingWorld world, BlockPos origin, Cell valid) {
        world.base(origin, valid);
        world.base(origin.up(), valid);
        world.base(origin.down(), valid);
        world.base(origin.east(), valid);
        world.base(origin.north(), valid);
        world.base(origin.south(), valid);
        world.base(origin.west(), Cell.AIR);
    }

    private static void assertRejected(RecordingWorld world, BlockPos origin) {
        assertFalse(V118DefaultSpringPlacements.placeSpring(
            world, origin, SpringFluid.LAVA));
        assertTrue(world.writes.isEmpty());
        assertTrue(world.scheduled.isEmpty());
    }

    private enum Cell {
        AIR,
        INVALID,
        BASE_VALID,
        WATER_VALID,
        WATER,
        LAVA
    }

    private static final class Scheduled {
        private final BlockPos position;
        private final SpringFluid fluid;

        private Scheduled(BlockPos position, SpringFluid fluid) {
            this.position = position.toImmutable();
            this.fluid = fluid;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Scheduled)) {
                return false;
            }
            Scheduled that = (Scheduled) other;
            return position.equals(that.position) && fluid == that.fluid;
        }

        @Override
        public int hashCode() {
            return 31 * position.hashCode() + fluid.hashCode();
        }

        @Override
        public String toString() {
            return fluid + "@" + position;
        }
    }

    private static final class RecordingWorld
            implements V118DefaultSpringPlacements.WorldAccess {
        private final Map<BlockPos, Cell> base = new HashMap<BlockPos, Cell>();
        private final Map<BlockPos, Cell> writes = new HashMap<BlockPos, Cell>();
        private final List<SpringFluid> writeOrder = new ArrayList<SpringFluid>();
        private final List<Scheduled> scheduled = new ArrayList<Scheduled>();
        private final List<BlockPos> biomeQueries = new ArrayList<BlockPos>();

        void base(BlockPos pos, Cell cell) {
            base.put(pos.toImmutable(), cell);
        }

        Cell cell(BlockPos pos) {
            Cell written = writes.get(pos);
            if (written != null) {
                return written;
            }
            Cell value = base.get(pos);
            return value == null ? Cell.AIR : value;
        }

        @Override
        public int minBuildHeight() {
            return -64;
        }

        @Override
        public int maxBuildHeight() {
            return 320;
        }

        @Override
        public V118Biome biomeAt(BlockPos pos) {
            biomeQueries.add(pos.toImmutable());
            return V118Biome.PLAINS;
        }

        @Override
        public boolean isAir(BlockPos pos) {
            return cell(pos) == Cell.AIR;
        }

        @Override
        public boolean isSpringValid(BlockPos pos, SpringFluid fluid) {
            if (pos.getY() < minBuildHeight() || pos.getY() >= maxBuildHeight()) {
                return false;
            }
            Cell cell = cell(pos);
            return cell == Cell.BASE_VALID
                || fluid == SpringFluid.WATER && cell == Cell.WATER_VALID;
        }

        @Override
        public void setSpring(BlockPos pos, SpringFluid fluid) {
            writes.put(pos.toImmutable(), fluid == SpringFluid.WATER
                ? Cell.WATER : Cell.LAVA);
            writeOrder.add(fluid);
        }

        @Override
        public void scheduleSpringTick(BlockPos pos, SpringFluid fluid) {
            scheduled.add(new Scheduled(pos, fluid));
        }
    }
}

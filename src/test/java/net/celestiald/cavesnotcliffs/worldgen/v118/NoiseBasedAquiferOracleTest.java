package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NoiseBasedAquiferOracleTest {
    private static final String FIXTURE =
            "/net/celestiald/cavesnotcliffs/worldgen/v118/aquifer-oracle-1.18.2.tsv";

    @Test
    public void matchesOfficialPureHelpersAndEndToEndMatrices() throws Exception {
        BufferedReader reader = fixtureReader();
        String line;
        int records = 0;
        int offsets = 0;
        int grids = 0;
        int similarities = 0;
        int statuses = 0;
        int centers = 0;
        int pressures = 0;
        int samples = 0;
        int scheduled = 0;
        int water = 0;
        int lava = 0;
        int air = 0;
        int solid = 0;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }
            String[] fields = line.split("\\t", -1);
            if ("count".equals(fields[0])) {
                assertEquals("oracle record count", Integer.parseInt(fields[1]), records);
                continue;
            }
            ++records;
            if ("constant".equals(fields[0])) {
                assertEquals("flowing similarity", "flowing_similarity", fields[1]);
                assertBits(line, fields[2], NoiseBasedAquifer.similarity(100, 144));
            } else if ("offset".equals(fields[0])) {
                int index = Integer.parseInt(fields[1]);
                int[][] actual = NoiseBasedAquifer.surfaceSamplingOffsetsInChunks();
                assertEquals(line, Integer.parseInt(fields[2]), actual[index][0]);
                assertEquals(line, Integer.parseInt(fields[3]), actual[index][1]);
                ++offsets;
            } else if ("grid".equals(fields[0])) {
                int coordinate = Integer.parseInt(fields[1]);
                assertEquals(line, Integer.parseInt(fields[2]),
                        NoiseBasedAquifer.gridX(coordinate));
                assertEquals(line, Integer.parseInt(fields[3]),
                        NoiseBasedAquifer.gridY(coordinate));
                assertEquals(line, Integer.parseInt(fields[4]),
                        NoiseBasedAquifer.gridZ(coordinate));
                ++grids;
            } else if ("similarity".equals(fields[0])) {
                assertBits(line, fields[3], NoiseBasedAquifer.similarity(
                        Integer.parseInt(fields[1]), Integer.parseInt(fields[2])));
                ++similarities;
            } else if ("status".equals(fields[0])) {
                NoiseBasedAquifer.FluidStatus status = new NoiseBasedAquifer.FluidStatus(
                        Integer.parseInt(fields[1]), material(fields[2]));
                assertEquals(line, material(fields[4]), status.at(Integer.parseInt(fields[3])));
                ++statuses;
            } else if ("center".equals(fields[0])) {
                PositionalRandomFactory factory = aquiferFactory(Long.parseLong(fields[1]));
                NoiseBasedAquifer.AquiferCenter center = NoiseBasedAquifer.centerForGrid(factory,
                        Integer.parseInt(fields[2]), Integer.parseInt(fields[3]),
                        Integer.parseInt(fields[4]));
                assertEquals(line, Integer.parseInt(fields[5]), center.blockX());
                assertEquals(line, Integer.parseInt(fields[6]), center.blockY());
                assertEquals(line, Integer.parseInt(fields[7]), center.blockZ());
                ++centers;
            } else if ("pressure".equals(fields[0])) {
                int y = Integer.parseInt(fields[1]);
                NoiseBasedAquifer.FluidStatus first = new NoiseBasedAquifer.FluidStatus(
                        Integer.parseInt(fields[2]), material(fields[3]));
                NoiseBasedAquifer.FluidStatus second = new NoiseBasedAquifer.FluidStatus(
                        Integer.parseInt(fields[4]), material(fields[5]));
                double barrier = fromBits(fields[6]);
                assertBits(line, fields[7],
                        NoiseBasedAquifer.pressure(y, first, second, barrier));
                ++pressures;
            } else if ("sample".equals(fields[0])) {
                long seed = Long.parseLong(fields[1]);
                int x = Integer.parseInt(fields[2]);
                int y = Integer.parseInt(fields[3]);
                int z = Integer.parseInt(fields[4]);
                NoiseBasedAquifer aquifer = aquifer(seed, Math.floorDiv(x, 16),
                        Math.floorDiv(z, 16), new Probe(ProbeKind.BARRIER, 0.0D),
                        NoiseBasedAquiferOracleTest::surfaceLevel);
                NoiseBasedAquifer.Result result = aquifer.compute(x, y, z, fromBits(fields[5]));
                assertEquals(line, fields[6], resultName(result));
                assertEquals(line, Boolean.parseBoolean(fields[7]),
                        result.shouldScheduleFluidUpdate());
                if (result.shouldScheduleFluidUpdate()) {
                    ++scheduled;
                }
                if ("WATER".equals(fields[6])) {
                    ++water;
                } else if ("LAVA".equals(fields[6])) {
                    ++lava;
                } else if ("AIR".equals(fields[6])) {
                    ++air;
                } else if ("SOLID".equals(fields[6])) {
                    ++solid;
                }
                ++samples;
            } else {
                fail("Unknown aquifer oracle record: " + line);
            }
        }
        reader.close();

        assertEquals("official record count", 884, records);
        assertEquals("surface offsets", 13, offsets);
        assertEquals("grid boundaries", 27, grids);
        assertEquals("similarities", 11, similarities);
        assertEquals("fluid statuses", 150, statuses);
        assertEquals("seeded centers", 42, centers);
        assertEquals("pressure cases", 16, pressures);
        assertEquals("end-to-end samples", 624, samples);
        assertEquals("scheduled samples", 134, scheduled);
        assertEquals("water samples", 30, water);
        assertEquals("lava samples", 90, lava);
        assertEquals("air samples", 308, air);
        assertEquals("solid samples", 196, solid);
    }

    @Test
    public void requestOrderDoesNotChangeResultsOrBarrierSampling() {
        List<Query> queries = new ArrayList<Query>();
        for (int x : new int[] {0, 3, 7, 11, 15}) {
            for (int z : new int[] {0, 5, 10, 15}) {
                for (int y : new int[] {-53, -48, -40, -32, -24, -16, -8, 0, 8,
                        16, 24, 32, 40, 48, 56, 62}) {
                    queries.add(new Query(x, y, z, (x + y + z) % 3 == 0 ? 0.0D : -0.35D));
                }
            }
        }

        Probe forwardBarrier = new Probe(ProbeKind.BARRIER, 0.0D);
        NoiseBasedAquifer forward = aquifer(123456789L, 0, 0, forwardBarrier,
                NoiseBasedAquiferOracleTest::surfaceLevel);
        Map<Query, String> expected = evaluate(forward, queries);

        List<Query> reversed = new ArrayList<Query>(queries);
        Collections.reverse(reversed);
        Probe reverseBarrier = new Probe(ProbeKind.BARRIER, 0.0D);
        NoiseBasedAquifer reverse = aquifer(123456789L, 0, 0, reverseBarrier,
                NoiseBasedAquiferOracleTest::surfaceLevel);
        Map<Query, String> actual = evaluate(reverse, reversed);

        assertEquals(expected, actual);
        assertTrue("barrier must be exercised", forwardBarrier.computeCalls > 0);
        assertTrue("one query evaluates barrier at most once",
                forwardBarrier.computeCalls <= queries.size());
        assertTrue("one query evaluates barrier at most once",
                reverseBarrier.computeCalls <= queries.size());
    }

    @Test
    public void surfaceLookupReceivesOfficialQuartAnchors() {
        final int[] calls = {0};
        NoiseBasedAquifer.PreliminarySurfaceLookup lookup = (x, z) -> {
            assertEquals("quart-aligned X", 0, Math.floorMod(x, 4));
            assertEquals("quart-aligned Z", 0, Math.floorMod(z, 4));
            ++calls[0];
            return surfaceLevel(x, z);
        };
        NoiseBasedAquifer aquifer = aquifer(-1L, -1, -1,
                new Probe(ProbeKind.BARRIER, 0.0D), lookup);
        NoiseBasedAquifer.Result result = aquifer.compute(-16, -54, -16, -0.5D);
        assertNotNull(result);
        assertTrue("surface scan was exercised", calls[0] > 0);
    }

    @Test
    public void repeatedQueryReusesLocationAndFluidStatusCaches() {
        CountingPositionalFactory factory = new CountingPositionalFactory(
                aquiferFactory(123456789L));
        final int[] surfaceCalls = {0};
        NoiseBasedAquifer.PreliminarySurfaceLookup lookup = (x, z) -> {
            ++surfaceCalls[0];
            return surfaceLevel(x, z);
        };
        Probe floodedness = new Probe(ProbeKind.FLOODEDNESS, 0.0D);
        NoiseBasedAquifer aquifer = new NoiseBasedAquifer(0, 0, -64, 384,
                new Probe(ProbeKind.BARRIER, 0.0D), floodedness,
                new Probe(ProbeKind.SPREAD, 0.0D), new Probe(ProbeKind.LAVA, 0.0D),
                factory, lookup, NoiseBasedAquiferOracleTest::globalFluid);

        NoiseBasedAquifer.Result first = aquifer.compute(7, 0, 9, -0.5D);
        int locationCalls = factory.atCalls;
        int firstSurfaceCalls = surfaceCalls[0];
        int firstFloodednessCalls = floodedness.computeCalls;
        NoiseBasedAquifer.Result second = aquifer.compute(7, 0, 9, -0.5D);

        assertEquals("twelve neighboring grid centers", 12, locationCalls);
        assertTrue("fluid status was computed", firstSurfaceCalls > 0);
        assertTrue("floodedness was computed", firstFloodednessCalls > 0);
        assertEquals("locations cached", locationCalls, factory.atCalls);
        assertEquals("surface-derived status cached", firstSurfaceCalls, surfaceCalls[0]);
        assertEquals("floodedness-derived status cached", firstFloodednessCalls,
                floodedness.computeCalls);
        assertEquals(resultName(first), resultName(second));
        assertEquals(first.shouldScheduleFluidUpdate(), second.shouldScheduleFluidUpdate());
    }

    @Test
    public void resultDistinguishesSolidFromAir() {
        NoiseBasedAquifer.Result solid = NoiseBasedAquifer.Result.solid();
        assertTrue(solid.isSolid());
        assertFalse(solid.shouldScheduleFluidUpdate());
        try {
            solid.material();
            fail("solid result must not expose a material");
        } catch (IllegalStateException expected) {
            assertEquals("Solid aquifer result has no replacement material",
                    expected.getMessage());
        }
        NoiseBasedAquifer.Result air = NoiseBasedAquifer.Result.material(
                NoiseBasedAquifer.Material.AIR, true);
        assertFalse(air.isSolid());
        assertEquals(NoiseBasedAquifer.Material.AIR, air.material());
        assertTrue(air.shouldScheduleFluidUpdate());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonPositiveHeight() {
        aquifer(0L, 0, 0, new Probe(ProbeKind.BARRIER, 0.0D),
                NoiseBasedAquiferOracleTest::surfaceLevel, 0);
    }

    private static Map<Query, String> evaluate(NoiseBasedAquifer aquifer, List<Query> queries) {
        Map<Query, String> results = new LinkedHashMap<Query, String>();
        for (Query query : queries) {
            NoiseBasedAquifer.Result result = aquifer.compute(query.x, query.y, query.z,
                    query.density);
            results.put(query, resultName(result) + '/' + result.shouldScheduleFluidUpdate());
        }
        return results;
    }

    private static NoiseBasedAquifer aquifer(long seed, int chunkX, int chunkZ, Probe barrier,
            NoiseBasedAquifer.PreliminarySurfaceLookup surfaceLookup) {
        return aquifer(seed, chunkX, chunkZ, barrier, surfaceLookup, 384);
    }

    private static NoiseBasedAquifer aquifer(long seed, int chunkX, int chunkZ, Probe barrier,
            NoiseBasedAquifer.PreliminarySurfaceLookup surfaceLookup, int height) {
        return new NoiseBasedAquifer(chunkX, chunkZ, -64, height, barrier,
                new Probe(ProbeKind.FLOODEDNESS, 0.0D),
                new Probe(ProbeKind.SPREAD, 0.0D), new Probe(ProbeKind.LAVA, 0.0D),
                aquiferFactory(seed), surfaceLookup, NoiseBasedAquiferOracleTest::globalFluid);
    }

    private static PositionalRandomFactory aquiferFactory(long seed) {
        return new XoroshiroRandomSource(seed).forkPositional()
                .fromHashOf("minecraft:aquifer").forkPositional();
    }

    private static NoiseBasedAquifer.FluidStatus globalFluid(int x, int y, int z) {
        return y < -54
                ? new NoiseBasedAquifer.FluidStatus(-54, NoiseBasedAquifer.Material.LAVA)
                : new NoiseBasedAquifer.FluidStatus(63, NoiseBasedAquifer.Material.WATER);
    }

    private static int surfaceLevel(int blockX, int blockZ) {
        int quartX = Math.floorDiv(blockX, 4);
        int quartZ = Math.floorDiv(blockZ, 4);
        return 40 + (int) Math.floorMod(quartX * 31L + quartZ * 17L, 49L);
    }

    private static double probe(ProbeKind kind, DensityFunction.FunctionContext context,
            double constant) {
        if (kind == ProbeKind.CONSTANT) {
            return constant;
        }
        long salt;
        double divisor;
        if (kind == ProbeKind.BARRIER) {
            salt = 0x13579BDFL;
            divisor = 2000.0D;
        } else if (kind == ProbeKind.FLOODEDNESS) {
            salt = 0x2468ACE0L;
            divisor = 1000.0D;
        } else if (kind == ProbeKind.SPREAD) {
            salt = 0x55AA55AAL;
            divisor = 1000.0D;
        } else {
            salt = 0x10203040L;
            divisor = 1000.0D;
        }
        long mixed = context.blockX() * 341873128712L
                + context.blockY() * 132897987541L
                + context.blockZ() * 42317861L + salt;
        return (Math.floorMod(mixed, 2001L) - 1000L) / divisor;
    }

    private static String resultName(NoiseBasedAquifer.Result result) {
        return result.isSolid() ? "SOLID" : result.material().name();
    }

    private static NoiseBasedAquifer.Material material(String name) {
        return NoiseBasedAquifer.Material.valueOf(name);
    }

    private static BufferedReader fixtureReader() {
        InputStream input = NoiseBasedAquiferOracleTest.class.getResourceAsStream(FIXTURE);
        assertNotNull("Missing aquifer oracle fixture", input);
        return new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
    }

    private static double fromBits(String unsignedBits) {
        return Double.longBitsToDouble(Long.parseUnsignedLong(unsignedBits));
    }

    private static void assertBits(String message, String expectedUnsignedBits, double actual) {
        assertEquals(message, Long.parseUnsignedLong(expectedUnsignedBits),
                Double.doubleToRawLongBits(actual));
    }

    private enum ProbeKind {
        CONSTANT,
        BARRIER,
        FLOODEDNESS,
        SPREAD,
        LAVA
    }

    private static final class Probe implements DensityFunction.SimpleFunction {
        private final ProbeKind kind;
        private final double constant;
        private int computeCalls;

        private Probe(ProbeKind kind, double constant) {
            this.kind = kind;
            this.constant = constant;
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            ++computeCalls;
            return probe(kind, context, constant);
        }

        @Override
        public double minValue() {
            return -2.0D;
        }

        @Override
        public double maxValue() {
            return 2.0D;
        }
    }

    private static final class CountingPositionalFactory implements PositionalRandomFactory {
        private final PositionalRandomFactory delegate;
        private int atCalls;

        private CountingPositionalFactory(PositionalRandomFactory delegate) {
            this.delegate = delegate;
        }

        @Override
        public RandomSource at(int x, int y, int z) {
            ++atCalls;
            return delegate.at(x, y, z);
        }

        @Override
        public RandomSource fromHashOf(String name) {
            return delegate.fromHashOf(name);
        }

        @Override
        public void appendParityConfigString(StringBuilder builder) {
            delegate.appendParityConfigString(builder);
        }
    }

    private static final class Query {
        private final int x;
        private final int y;
        private final int z;
        private final double density;

        private Query(int x, int y, int z, double density) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.density = density;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof Query)) {
                return false;
            }
            Query other = (Query) object;
            return x == other.x && y == other.y && z == other.z
                    && Double.doubleToRawLongBits(density)
                            == Double.doubleToRawLongBits(other.density);
        }

        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            result = 31 * result + z;
            long bits = Double.doubleToRawLongBits(density);
            return 31 * result + (int) (bits ^ bits >>> 32);
        }
    }
}

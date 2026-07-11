package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DensityFunctionsOracleTest {
    private static final String FIXTURE =
        "/net/celestiald/cavesnotcliffs/worldgen/v118/density-functions-oracle-1.18.2.tsv";

    @Test
    public void matchesOfficialDensityFunctionsBitForBit() throws IOException {
        int records = 0;
        int expectedCount = -1;
        InputStream input = DensityFunctionsOracleTest.class.getResourceAsStream(FIXTURE);
        assertNotNull("missing fixture " + FIXTURE, input);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] fields = line.split("\\t", -1);
                switch (fields[0]) {
                    case "mapped":
                        verifyMapped(line, fields);
                        break;
                    case "gradient":
                        verifyGradient(line, fields);
                        break;
                    case "binary":
                        verifyBinary(line, fields);
                        break;
                    case "constant_binary":
                        verifyConstantBinary(line, fields);
                        break;
                    case "range":
                        verifyRange(line, fields);
                        break;
                    case "rarity":
                        verifyRarity(line, fields);
                        break;
                    case "slide":
                        verifySlide(line, fields);
                        break;
                    case "spline":
                        verifySpline(line, fields);
                        break;
                    case "terrain_spline":
                        verifyTerrainSpline(line, fields);
                        break;
                    case "noise":
                        verifyNoise(line, fields);
                        break;
                    case "count":
                        expectedCount = Integer.parseInt(fields[1]);
                        continue;
                    default:
                        fail("Unknown fixture record: " + line);
                }
                ++records;
            }
        }
        assertEquals("oracle fixture count", expectedCount, records);
        assertEquals("density fixture coverage changed", 464, records);
    }

    @Test
    public void bulkEvaluationMatchesScalarEvaluationAndOfficialShortCircuiting() {
        final DensityFunction.FunctionContext[] contexts = {
            context(-17, -64, 31), context(0, -1, 0), context(1, 0, -1),
            context(32, 319, -33)
        };
        DensityFunction.ContextProvider provider = provider(contexts);
        DensityFunction expression = DensityFunctions.rangeChoice(
            DensityFunctions.yClampedGradient(-64, 320, -2.0D, 2.0D),
            -0.75D, 0.75D,
            DensityFunctions.add(DensityFunctions.constant(0.125D),
                DensityFunctions.yClampedGradient(-64, 320, 1.5D, -1.5D).square()),
            DensityFunctions.constant(-0.25D));
        double[] values = new double[contexts.length];
        expression.fillArray(values, provider);
        for (int index = 0; index < values.length; ++index) {
            assertRawBits("bulk value " + index, expression.compute(contexts[index]),
                values[index]);
        }

        Probe skippedNaN = new Probe(Double.NaN, -4.0D, 5.0D);
        DensityFunction zeroProduct = DensityFunctions.mul(
            new Probe(-0.0D, -2.0D, 3.0D), skippedNaN);
        assertRawBits("multiply zero short circuit", 0.0D,
            zeroProduct.compute(context(0, 0, 0)));
        assertEquals("second operand skipped", 0, skippedNaN.computeCalls);

        Probe skippedMin = new Probe(Double.NaN, -4.0D, 5.0D);
        DensityFunction minimum = DensityFunctions.min(
            new Probe(-10.0D, -10.0D, -10.0D), skippedMin);
        assertRawBits("minimum bound short circuit", -10.0D,
            minimum.compute(context(0, 0, 0)));
        assertEquals("minimum second operand skipped", 0, skippedMin.computeCalls);

        Probe skippedMax = new Probe(Double.NaN, -4.0D, 5.0D);
        DensityFunction maximum = DensityFunctions.max(
            new Probe(10.0D, 10.0D, 10.0D), skippedMax);
        assertRawBits("maximum bound short circuit", 10.0D,
            maximum.compute(context(0, 0, 0)));
        assertEquals("maximum second operand skipped", 0, skippedMax.computeCalls);
    }

    @Test
    public void visitorOrderAndUnseededBoundsMatchOfficialContracts() {
        NormalNoise.NoiseParameters parameters = parameters();
        DensityFunctions.Noise noise = DensityFunctions.noise(parameters, 0.25D, 0.5D);
        assertRawBits("unseeded noise", 0.0D, noise.compute(context(4, -8, 16)));
        assertRawBits("unseeded noise minimum", -2.0D, noise.minValue());
        assertRawBits("unseeded noise maximum", 2.0D, noise.maxValue());

        DensityFunctions.Shift shift = DensityFunctions.shift(parameters);
        assertRawBits("unseeded shift", 0.0D, shift.compute(context(4, -8, 16)));
        assertRawBits("unseeded shift minimum", -8.0D, shift.minValue());
        assertRawBits("unseeded shift maximum", 8.0D, shift.maxValue());

        DensityFunction blended = DensityFunctions.blendDensity(DensityFunctions.constant(3.0D));
        DensityFunction.FunctionContext blendContext = new DensityFunction.FunctionContext() {
            @Override
            public int blockX() {
                return 0;
            }

            @Override
            public int blockY() {
                return 0;
            }

            @Override
            public int blockZ() {
                return 0;
            }

            @Override
            public double blendDensity(double density) {
                return density * -0.25D;
            }
        };
        assertRawBits("blender context hook", -0.75D, blended.compute(blendContext));
        assertRawBits("blend minimum", Double.NEGATIVE_INFINITY, blended.minValue());
        assertRawBits("blend maximum", Double.POSITIVE_INFINITY, blended.maxValue());

        final List<DensityFunction> visits = new ArrayList<DensityFunction>();
        DensityFunction wrapped = DensityFunctions.weirdScaledSampler(
            DensityFunctions.constant(0.25D), parameters, DensityFunctions.RarityValueMapper.TYPE1);
        DensityFunction mapped = wrapped.mapAll(new DensityFunction.Visitor() {
            @Override
            public DensityFunction apply(DensityFunction function) {
                visits.add(function);
                return function;
            }
        });
        assertTrue(mapped instanceof DensityFunctions.WeirdScaledSampler);
        assertEquals("1.18.2 deliberately walks weird-sampler input twice", 3, visits.size());
        assertTrue(visits.get(0) instanceof DensityFunctions.Constant);
        assertTrue(visits.get(1) instanceof DensityFunctions.Constant);
        assertTrue(visits.get(2) instanceof DensityFunctions.WeirdScaledSampler);

        DensityFunction marker = DensityFunctions.cacheOnce(DensityFunctions.constant(3.0D));
        assertSame(marker, marker.mapAll(new DensityFunction.Visitor() {
            @Override
            public DensityFunction apply(DensityFunction function) {
                return function instanceof DensityFunctions.Marker ? marker : function;
            }
        }));
    }

    private static void verifyMapped(String line, String[] fields) {
        DensityFunction function = DensityFunctions.map(
            new Probe(fromBits(fields[2]), -2.0D, 3.0D),
            DensityFunctions.MappedType.valueOf(fields[1]));
        assertBits(line, 3, fields[3], function.compute(context(0, 0, 0)));
        assertBits(line, 4, fields[4], function.minValue());
        assertBits(line, 5, fields[5], function.maxValue());
    }

    private static void verifyGradient(String line, String[] fields) {
        DensityFunction function = "overworld".equals(fields[1])
            ? DensityFunctions.yClampedGradient(-64, 320, 1.5D, -1.5D)
            : DensityFunctions.yClampedGradient(-8, 8, -2.0D, 6.0D);
        int y = Integer.parseInt(fields[2]);
        assertBits(line, 3, fields[3], function.compute(context(0, y, 0)));
        assertBits(line, 4, fields[4], function.minValue());
        assertBits(line, 5, fields[5], function.maxValue());
    }

    private static void verifyBinary(String line, String[] fields) {
        Probe first = new Probe(fromBits(fields[2]), fromBits(fields[3]), fromBits(fields[4]));
        Probe second = new Probe(fromBits(fields[5]), fromBits(fields[6]), fromBits(fields[7]));
        DensityFunction function = binary(fields[1], first, second);
        assertBits(line, 8, fields[8], function.compute(context(0, 0, 0)));
        assertBits(line, 9, fields[9], function.minValue());
        assertBits(line, 10, fields[10], function.maxValue());
        assertEquals("second compute count in " + line, Integer.parseInt(fields[11]),
            second.computeCalls);
    }

    private static void verifyConstantBinary(String line, String[] fields) {
        DensityFunction input = new Probe(-2.5D, -4.0D, 3.0D);
        DensityFunction function = binary(fields[1], DensityFunctions.constant(fromBits(fields[2])),
            input);
        assertBits(line, 3, fields[3], function.compute(context(0, 0, 0)));
        assertBits(line, 4, fields[4], function.minValue());
        assertBits(line, 5, fields[5], function.maxValue());
    }

    private static DensityFunction binary(String type, DensityFunction first,
            DensityFunction second) {
        switch (type) {
            case "ADD":
                return DensityFunctions.add(first, second);
            case "MUL":
                return DensityFunctions.mul(first, second);
            case "MIN":
                return DensityFunctions.min(first, second);
            case "MAX":
                return DensityFunctions.max(first, second);
            default:
                throw new AssertionError(type);
        }
    }

    private static void verifyRange(String line, String[] fields) {
        DensityFunction function = DensityFunctions.rangeChoice(
            new Probe(fromBits(fields[1]), -2.0D, 2.0D), -0.5D, 0.5D,
            DensityFunctions.constant(11.0D), DensityFunctions.constant(-13.0D));
        assertBits(line, 2, fields[2], function.compute(context(0, 0, 0)));
        assertBits(line, 3, fields[3], function.minValue());
        assertBits(line, 4, fields[4], function.maxValue());
    }

    private static void verifyRarity(String line, String[] fields) {
        DensityFunctions.RarityValueMapper mapper =
            DensityFunctions.RarityValueMapper.valueOf(fields[1].replace("TYPE_", "TYPE"));
        assertBits(line, 3, fields[3], mapper.map(fromBits(fields[2])));
    }

    private static void verifySlide(String line, String[] fields) {
        boolean amplified = Boolean.parseBoolean(fields[1]);
        DensityFunction function = DensityFunctions.slide(new OracleSlideSettings(amplified),
            new Probe(0.625D, -2.0D, 3.0D));
        int y = Integer.parseInt(fields[2]);
        assertBits(line, 3, fields[3], function.compute(context(0, y, 0)));
        assertBits(line, 4, fields[4], function.minValue());
        assertBits(line, 5, fields[5], function.maxValue());
    }

    private static void verifySpline(String line, String[] fields) {
        DensityFunction coordinateFunction = DensityFunctions.yClampedGradient(-64, 320,
            -1.25D, 1.75D);
        CubicSpline<DensityFunction.FunctionContext> spline = CubicSpline.builder(
                DensityFunctions.splineCoordinate(coordinateFunction))
            .addPoint(-1.0F, -0.75F, 0.25F)
            .addPoint(-0.2F, 0.5F, -0.4F)
            .addPoint(0.6F, -0.1F, 0.8F)
            .addPoint(1.25F, 1.5F, -0.2F)
            .build();
        DensityFunction function = DensityFunctions.spline(spline, -0.5D, 1.0D);
        int y = Integer.parseInt(fields[1]);
        assertBits(line, 2, fields[2], function.compute(context(0, y, 0)));
        assertBits(line, 3, fields[3], function.minValue());
        assertBits(line, 4, fields[4], function.maxValue());
    }

    private static void verifyTerrainSpline(String line, String[] fields) {
        DensityFunction function = DensityFunctions.terrainShaperSpline(
            new Probe(-0.125D, -1.0D, 1.0D), new Probe(0.375D, -1.0D, 1.0D),
            new Probe(-0.69D, -1.0D, 1.0D),
            TerrainShaper.overworld(TerrainShaper.Profile.NORMAL),
            DensityFunctions.TerrainShaperSplineType.valueOf(fields[1]), -0.81D, 8.0D);
        assertBits(line, 2, fields[2], function.compute(context(0, 0, 0)));
        assertBits(line, 3, fields[3], function.minValue());
        assertBits(line, 4, fields[4], function.maxValue());
    }

    private static void verifyNoise(String line, String[] fields) {
        String kind = fields[1];
        long seed = Long.parseLong(fields[2]);
        int x = Integer.parseInt(fields[3]);
        int y = Integer.parseInt(fields[4]);
        int z = Integer.parseInt(fields[5]);
        NormalNoise normal = NormalNoise.create(new XoroshiroRandomSource(seed), parameters());
        DensityFunction function;
        switch (kind) {
            case "noise":
                function = DensityFunctions.noise(normal, 0.25D, 0.5D);
                break;
            case "shift_a":
                function = DensityFunctions.shiftA(normal);
                break;
            case "shift_b":
                function = DensityFunctions.shiftB(normal);
                break;
            case "shift":
                function = DensityFunctions.shift(normal);
                break;
            case "shifted":
                function = new DensityFunctions.ShiftedNoise(
                    DensityFunctions.constant(0.25D), DensityFunctions.constant(-0.5D),
                    DensityFunctions.constant(0.75D), 0.25D, 0.5D,
                    normal.parameters(), normal);
                break;
            case "weird_type1":
                function = DensityFunctions.weirdScaledSampler(DensityFunctions.constant(-0.51D),
                    normal, DensityFunctions.RarityValueMapper.TYPE1);
                break;
            case "weird_type2":
                function = DensityFunctions.weirdScaledSampler(DensityFunctions.constant(0.75D),
                    normal, DensityFunctions.RarityValueMapper.TYPE2);
                break;
            default:
                throw new AssertionError(kind);
        }
        assertBits(line, 6, fields[6], function.compute(context(x, y, z)));
        assertBits(line, 7, fields[7], function.minValue());
        assertBits(line, 8, fields[8], function.maxValue());
    }

    private static NormalNoise.NoiseParameters parameters() {
        return new NormalNoise.NoiseParameters(-3, 1.0D, 0.5D, 0.0D, 1.0D);
    }

    private static DensityFunction.FunctionContext context(int x, int y, int z) {
        return new DensityFunction.SinglePointContext(x, y, z);
    }

    private static DensityFunction.ContextProvider provider(
            final DensityFunction.FunctionContext[] contexts) {
        return new DensityFunction.ContextProvider() {
            @Override
            public DensityFunction.FunctionContext forIndex(int index) {
                return contexts[index];
            }

            @Override
            public void fillAllDirectly(double[] values, DensityFunction function) {
                for (int index = 0; index < values.length; ++index) {
                    values[index] = function.compute(contexts[index]);
                }
            }
        };
    }

    private static double fromBits(String unsignedBits) {
        return Double.longBitsToDouble(Long.parseUnsignedLong(unsignedBits));
    }

    private static void assertBits(String line, int field, String expectedBits, double actual) {
        assertEquals("oracle mismatch at field " + field + " in " + line,
            Long.parseUnsignedLong(expectedBits), Double.doubleToRawLongBits(actual));
    }

    private static void assertRawBits(String message, double expected, double actual) {
        assertEquals(message, Double.doubleToRawLongBits(expected),
            Double.doubleToRawLongBits(actual));
    }

    private static final class Probe implements DensityFunction.SimpleFunction {
        private final double value;
        private final double min;
        private final double max;
        private int computeCalls;

        Probe(double value, double min, double max) {
            this.value = value;
            this.min = min;
            this.max = max;
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            ++computeCalls;
            return value;
        }

        @Override
        public double minValue() {
            return min;
        }

        @Override
        public double maxValue() {
            return max;
        }
    }

    private static final class OracleSlideSettings implements DensityFunctions.SlideSettings {
        private final TestNoiseSlider top;
        private final TestNoiseSlider bottom;

        OracleSlideSettings(boolean amplified) {
            top = new TestNoiseSlider(-0.078125D, 2, amplified ? 0 : 8);
            bottom = new TestNoiseSlider(amplified ? 0.4D : 0.1171875D, 3, 0);
        }

        @Override
        public double applySlide(double value, int blockY) {
            double cellY = blockY / 8 - Math.floorDiv(-64, 8);
            value = top.apply(value, 48.0D - cellY);
            return bottom.apply(value, cellY);
        }

        @Override
        public double topSlideTarget() {
            return top.target;
        }

        @Override
        public double bottomSlideTarget() {
            return bottom.target;
        }
    }

    private static final class TestNoiseSlider {
        private final double target;
        private final int size;
        private final int offset;

        TestNoiseSlider(double target, int size, int offset) {
            this.target = target;
            this.size = size;
            this.offset = offset;
        }

        double apply(double value, double position) {
            if (size <= 0) {
                return value;
            }
            return WorldgenMath.clampedLerp(target, value, (position - offset) / size);
        }
    }
}

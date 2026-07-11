package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.Arrays;

/** Java 8 port of the pure Minecraft 1.18.2 density-function expression nodes. */
public final class DensityFunctions {
    private static final Constant ZERO = new Constant(0.0D);
    private static final Fixed BLEND_ALPHA = new Fixed(1.0D);
    private static final Fixed BLEND_OFFSET = new Fixed(0.0D);

    private DensityFunctions() {
    }

    public static DensityFunction interpolated(DensityFunction function) {
        return new Marker(MarkerType.INTERPOLATED, function);
    }

    public static DensityFunction flatCache(DensityFunction function) {
        return new Marker(MarkerType.FLAT_CACHE, function);
    }

    public static DensityFunction cache2d(DensityFunction function) {
        return new Marker(MarkerType.CACHE_2D, function);
    }

    public static DensityFunction cacheOnce(DensityFunction function) {
        return new Marker(MarkerType.CACHE_ONCE, function);
    }

    public static DensityFunction cacheAllInCell(DensityFunction function) {
        return new Marker(MarkerType.CACHE_ALL_IN_CELL, function);
    }

    public static DensityFunction mappedNoise(NormalNoise.NoiseParameters noiseData,
            double xzScale, double yScale, double min, double max) {
        return mapFromUnitTo(noise(noiseData, xzScale, yScale), min, max);
    }

    public static DensityFunction mappedNoise(NormalNoise noise, double xzScale, double yScale,
            double min, double max) {
        return mapFromUnitTo(noise(noise, xzScale, yScale), min, max);
    }

    public static DensityFunction mappedNoise(NormalNoise.NoiseParameters noiseData,
            double yScale, double min, double max) {
        return mappedNoise(noiseData, 1.0D, yScale, min, max);
    }

    public static DensityFunction mappedNoise(NormalNoise.NoiseParameters noiseData,
            double min, double max) {
        return mappedNoise(noiseData, 1.0D, 1.0D, min, max);
    }

    public static Noise noise(NormalNoise.NoiseParameters noiseData) {
        return noise(noiseData, 1.0D, 1.0D);
    }

    public static Noise noise(NormalNoise.NoiseParameters noiseData, double yScale) {
        return noise(noiseData, 1.0D, yScale);
    }

    public static Noise noise(NormalNoise.NoiseParameters noiseData, double xzScale,
            double yScale) {
        return new Noise(noiseData, null, xzScale, yScale);
    }

    public static Noise noise(NormalNoise noise) {
        return noise(noise, 1.0D, 1.0D);
    }

    public static Noise noise(NormalNoise noise, double yScale) {
        return noise(noise, 1.0D, yScale);
    }

    public static Noise noise(NormalNoise noise, double xzScale, double yScale) {
        return new Noise(noise.parameters(), noise, xzScale, yScale);
    }

    public static ShiftedNoise shiftedNoise2d(DensityFunction shiftX, DensityFunction shiftZ,
            double xzScale, NormalNoise.NoiseParameters noiseData) {
        return new ShiftedNoise(shiftX, zero(), shiftZ, xzScale, 0.0D, noiseData, null);
    }

    public static ShiftedNoise shiftedNoise2d(DensityFunction shiftX, DensityFunction shiftZ,
            double xzScale, NormalNoise noise) {
        return new ShiftedNoise(shiftX, zero(), shiftZ, xzScale, 0.0D,
            noise.parameters(), noise);
    }

    public static RangeChoice rangeChoice(DensityFunction input, double minInclusive,
            double maxExclusive, DensityFunction whenInRange, DensityFunction whenOutOfRange) {
        return new RangeChoice(input, minInclusive, maxExclusive, whenInRange, whenOutOfRange);
    }

    public static ShiftA shiftA(NormalNoise.NoiseParameters noiseData) {
        return new ShiftA(noiseData, null);
    }

    public static ShiftA shiftA(NormalNoise noise) {
        return new ShiftA(noise.parameters(), noise);
    }

    public static ShiftB shiftB(NormalNoise.NoiseParameters noiseData) {
        return new ShiftB(noiseData, null);
    }

    public static ShiftB shiftB(NormalNoise noise) {
        return new ShiftB(noise.parameters(), noise);
    }

    public static Shift shift(NormalNoise.NoiseParameters noiseData) {
        return new Shift(noiseData, null);
    }

    public static Shift shift(NormalNoise noise) {
        return new Shift(noise.parameters(), noise);
    }

    public static WeirdScaledSampler weirdScaledSampler(DensityFunction input,
            NormalNoise.NoiseParameters noiseData, RarityValueMapper rarityValueMapper) {
        return new WeirdScaledSampler(input, noiseData, null, rarityValueMapper);
    }

    public static WeirdScaledSampler weirdScaledSampler(DensityFunction input,
            NormalNoise noise, RarityValueMapper rarityValueMapper) {
        return new WeirdScaledSampler(input, noise.parameters(), noise, rarityValueMapper);
    }

    public static BlendDensity blendDensity(DensityFunction input) {
        return new BlendDensity(input);
    }

    public static Slide slide(SlideSettings settings, DensityFunction input) {
        return new Slide(settings, input);
    }

    public static DensityFunction add(DensityFunction first, DensityFunction second) {
        return twoArgument(TwoArgumentType.ADD, first, second);
    }

    public static DensityFunction mul(DensityFunction first, DensityFunction second) {
        return twoArgument(TwoArgumentType.MUL, first, second);
    }

    public static DensityFunction min(DensityFunction first, DensityFunction second) {
        return twoArgument(TwoArgumentType.MIN, first, second);
    }

    public static DensityFunction max(DensityFunction first, DensityFunction second) {
        return twoArgument(TwoArgumentType.MAX, first, second);
    }

    public static Spline spline(CubicSpline<DensityFunction.FunctionContext> spline,
            double minValue, double maxValue) {
        return new Spline(spline, minValue, maxValue);
    }

    public static DensityCoordinate splineCoordinate(DensityFunction function) {
        return new DensityCoordinate(function);
    }

    public static TerrainShaperSpline terrainShaperSpline(DensityFunction continentalness,
            DensityFunction erosion, DensityFunction weirdness,
            TerrainShaperSplineType splineType, double minValue, double maxValue) {
        return new TerrainShaperSpline(continentalness, erosion, weirdness, null, splineType,
            minValue, maxValue);
    }

    public static TerrainShaperSpline terrainShaperSpline(DensityFunction continentalness,
            DensityFunction erosion, DensityFunction weirdness, TerrainShaper terrainShaper,
            TerrainShaperSplineType splineType, double minValue, double maxValue) {
        return new TerrainShaperSpline(continentalness, erosion, weirdness, terrainShaper,
            splineType, minValue, maxValue);
    }

    public static DensityFunction zero() {
        return ZERO;
    }

    public static Constant constant(double value) {
        return new Constant(value);
    }

    public static YClampedGradient yClampedGradient(int fromY, int toY, double fromValue,
            double toValue) {
        return new YClampedGradient(fromY, toY, fromValue, toValue);
    }

    public static Clamp clamp(DensityFunction input, double minValue, double maxValue) {
        return new Clamp(input, minValue, maxValue);
    }

    public static Mapped map(DensityFunction input, MappedType type) {
        return Mapped.create(type, input);
    }

    public static DensityFunction blendAlpha() {
        return BLEND_ALPHA;
    }

    public static DensityFunction blendOffset() {
        return BLEND_OFFSET;
    }

    public static DensityFunction lerp(DensityFunction delta, DensityFunction start,
            DensityFunction end) {
        DensityFunction cachedDelta = cacheOnce(delta);
        DensityFunction inverseDelta = add(mul(cachedDelta, constant(-1.0D)), constant(1.0D));
        return add(mul(start, inverseDelta), mul(end, cachedDelta));
    }

    private static DensityFunction mapFromUnitTo(DensityFunction input, double min, double max) {
        double midpoint = (min + max) * 0.5D;
        double scale = (max - min) * 0.5D;
        return add(constant(midpoint), mul(constant(scale), input));
    }

    private static DensityFunction twoArgument(TwoArgumentType type, DensityFunction first,
            DensityFunction second) {
        double firstMin = first.minValue();
        double secondMin = second.minValue();
        double firstMax = first.maxValue();
        double secondMax = second.maxValue();
        double min;
        double max;

        switch (type) {
            case ADD:
                min = firstMin + secondMin;
                max = firstMax + secondMax;
                break;
            case MIN:
                min = Math.min(firstMin, secondMin);
                max = Math.min(firstMax, secondMax);
                break;
            case MAX:
                min = Math.max(firstMin, secondMin);
                max = Math.max(firstMax, secondMax);
                break;
            case MUL:
                if (firstMin > 0.0D && secondMin > 0.0D) {
                    min = firstMin * secondMin;
                    max = firstMax * secondMax;
                } else if (firstMax < 0.0D && secondMax < 0.0D) {
                    min = firstMax * secondMax;
                    max = firstMin * secondMin;
                } else {
                    min = Math.min(firstMin * secondMax, firstMax * secondMin);
                    max = Math.max(firstMin * secondMin, firstMax * secondMax);
                }
                break;
            default:
                throw new AssertionError(type);
        }

        if (type == TwoArgumentType.ADD || type == TwoArgumentType.MUL) {
            if (first instanceof Constant) {
                return new MulOrAdd(type, second, min, max, ((Constant) first).value());
            }
            if (second instanceof Constant) {
                return new MulOrAdd(type, first, min, max, ((Constant) second).value());
            }
        }
        return new Ap2(type, first, second, min, max);
    }

    private abstract static class PureTransformer implements DensityFunction {
        private final DensityFunction input;

        PureTransformer(DensityFunction input) {
            this.input = input;
        }

        public final DensityFunction input() {
            return input;
        }

        @Override
        public final double compute(DensityFunction.FunctionContext context) {
            return transform(input.compute(context));
        }

        @Override
        public final void fillArray(double[] values, DensityFunction.ContextProvider provider) {
            input.fillArray(values, provider);
            for (int index = 0; index < values.length; ++index) {
                values[index] = transform(values[index]);
            }
        }

        abstract double transform(double value);
    }

    private abstract static class TransformerWithContext implements DensityFunction {
        private final DensityFunction input;

        TransformerWithContext(DensityFunction input) {
            this.input = input;
        }

        public final DensityFunction input() {
            return input;
        }

        @Override
        public final double compute(DensityFunction.FunctionContext context) {
            return transform(context, input.compute(context));
        }

        @Override
        public final void fillArray(double[] values, DensityFunction.ContextProvider provider) {
            input.fillArray(values, provider);
            for (int index = 0; index < values.length; ++index) {
                values[index] = transform(provider.forIndex(index), values[index]);
            }
        }

        abstract double transform(DensityFunction.FunctionContext context, double value);
    }

    public enum MarkerType {
        INTERPOLATED,
        FLAT_CACHE,
        CACHE_2D,
        CACHE_ONCE,
        CACHE_ALL_IN_CELL
    }

    public static final class Marker implements DensityFunction {
        private final MarkerType type;
        private final DensityFunction wrapped;

        public Marker(MarkerType type, DensityFunction wrapped) {
            this.type = type;
            this.wrapped = wrapped;
        }

        public MarkerType type() {
            return type;
        }

        public DensityFunction wrapped() {
            return wrapped;
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return wrapped.compute(context);
        }

        @Override
        public void fillArray(double[] values, DensityFunction.ContextProvider provider) {
            wrapped.fillArray(values, provider);
        }

        @Override
        public DensityFunction mapAll(DensityFunction.Visitor visitor) {
            return visitor.apply(new Marker(type, wrapped.mapAll(visitor)));
        }

        @Override
        public double minValue() {
            return wrapped.minValue();
        }

        @Override
        public double maxValue() {
            return wrapped.maxValue();
        }
    }

    private static final class Fixed implements DensityFunction.SimpleFunction {
        private final double value;

        Fixed(double value) {
            this.value = value;
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return value;
        }

        @Override
        public void fillArray(double[] values, DensityFunction.ContextProvider provider) {
            Arrays.fill(values, value);
        }

        @Override
        public double minValue() {
            return value;
        }

        @Override
        public double maxValue() {
            return value;
        }
    }

    public static final class Noise implements DensityFunction.SimpleFunction {
        private final NormalNoise.NoiseParameters noiseData;
        private final NormalNoise noise;
        private final double xzScale;
        private final double yScale;

        public Noise(NormalNoise.NoiseParameters noiseData, NormalNoise noise, double xzScale,
                double yScale) {
            this.noiseData = noiseData;
            this.noise = noise;
            this.xzScale = xzScale;
            this.yScale = yScale;
        }

        public NormalNoise.NoiseParameters noiseData() {
            return noiseData;
        }

        public NormalNoise noise() {
            return noise;
        }

        public double xzScale() {
            return xzScale;
        }

        public double yScale() {
            return yScale;
        }

        public Noise withNoise(NormalNoise seededNoise) {
            return new Noise(noiseData, seededNoise, xzScale, yScale);
        }

        public static Noise createUnseeded(NormalNoise.NoiseParameters noiseData,
                double xzScale, double yScale) {
            return new Noise(noiseData, null, xzScale, yScale);
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            if (noise == null) {
                return 0.0D;
            }
            return noise.getValue(context.blockX() * xzScale, context.blockY() * yScale,
                context.blockZ() * xzScale);
        }

        @Override
        public double minValue() {
            return -maxValue();
        }

        @Override
        public double maxValue() {
            return noise == null ? 2.0D : noise.maxValue();
        }
    }

    public enum RarityValueMapper {
        TYPE1(2.0D) {
            @Override
            public double map(double value) {
                if (value < -0.5D) {
                    return 0.75D;
                }
                if (value < 0.0D) {
                    return 1.0D;
                }
                if (value < 0.5D) {
                    return 1.5D;
                }
                return 2.0D;
            }
        },
        TYPE2(3.0D) {
            @Override
            public double map(double value) {
                if (value < -0.75D) {
                    return 0.5D;
                }
                if (value < -0.5D) {
                    return 0.75D;
                }
                if (value < 0.5D) {
                    return 1.0D;
                }
                if (value < 0.75D) {
                    return 2.0D;
                }
                return 3.0D;
            }
        };

        private final double maxRarity;

        RarityValueMapper(double maxRarity) {
            this.maxRarity = maxRarity;
        }

        public abstract double map(double value);

        public double maxRarity() {
            return maxRarity;
        }
    }

    public static final class WeirdScaledSampler extends TransformerWithContext {
        private final NormalNoise.NoiseParameters noiseData;
        private final NormalNoise noise;
        private final RarityValueMapper rarityValueMapper;

        public WeirdScaledSampler(DensityFunction input,
                NormalNoise.NoiseParameters noiseData, NormalNoise noise,
                RarityValueMapper rarityValueMapper) {
            super(input);
            this.noiseData = noiseData;
            this.noise = noise;
            this.rarityValueMapper = rarityValueMapper;
        }

        public NormalNoise.NoiseParameters noiseData() {
            return noiseData;
        }

        public NormalNoise noise() {
            return noise;
        }

        public RarityValueMapper rarityValueMapper() {
            return rarityValueMapper;
        }

        public WeirdScaledSampler withNoise(NormalNoise seededNoise) {
            return new WeirdScaledSampler(input(), noiseData, seededNoise, rarityValueMapper);
        }

        public static WeirdScaledSampler createUnseeded(DensityFunction input,
                NormalNoise.NoiseParameters noiseData, RarityValueMapper rarityValueMapper) {
            return new WeirdScaledSampler(input, noiseData, null, rarityValueMapper);
        }

        @Override
        double transform(DensityFunction.FunctionContext context, double value) {
            if (noise == null) {
                return 0.0D;
            }
            double rarity = rarityValueMapper.map(value);
            return rarity * Math.abs(noise.getValue(context.blockX() / rarity,
                context.blockY() / rarity, context.blockZ() / rarity));
        }

        @Override
        public DensityFunction mapAll(DensityFunction.Visitor visitor) {
            // The first discarded walk is present in the 1.18.2 bytecode and is kept for visitor
            // side-effect parity.
            input().mapAll(visitor);
            return visitor.apply(new WeirdScaledSampler(input().mapAll(visitor), noiseData, noise,
                rarityValueMapper));
        }

        @Override
        public double minValue() {
            return 0.0D;
        }

        @Override
        public double maxValue() {
            return rarityValueMapper.maxRarity() * (noise == null ? 2.0D : noise.maxValue());
        }
    }

    public static final class ShiftedNoise implements DensityFunction {
        private final DensityFunction shiftX;
        private final DensityFunction shiftY;
        private final DensityFunction shiftZ;
        private final double xzScale;
        private final double yScale;
        private final NormalNoise.NoiseParameters noiseData;
        private final NormalNoise noise;

        public ShiftedNoise(DensityFunction shiftX, DensityFunction shiftY,
                DensityFunction shiftZ, double xzScale, double yScale,
                NormalNoise.NoiseParameters noiseData, NormalNoise noise) {
            this.shiftX = shiftX;
            this.shiftY = shiftY;
            this.shiftZ = shiftZ;
            this.xzScale = xzScale;
            this.yScale = yScale;
            this.noiseData = noiseData;
            this.noise = noise;
        }

        public DensityFunction shiftX() {
            return shiftX;
        }

        public DensityFunction shiftY() {
            return shiftY;
        }

        public DensityFunction shiftZ() {
            return shiftZ;
        }

        public double xzScale() {
            return xzScale;
        }

        public double yScale() {
            return yScale;
        }

        public NormalNoise.NoiseParameters noiseData() {
            return noiseData;
        }

        public NormalNoise noise() {
            return noise;
        }

        public ShiftedNoise withNoise(NormalNoise seededNoise) {
            return new ShiftedNoise(shiftX, shiftY, shiftZ, xzScale, yScale, noiseData,
                seededNoise);
        }

        public static ShiftedNoise createUnseeded(DensityFunction shiftX,
                DensityFunction shiftY, DensityFunction shiftZ, double xzScale,
                double yScale, NormalNoise.NoiseParameters noiseData) {
            return new ShiftedNoise(shiftX, shiftY, shiftZ, xzScale, yScale, noiseData, null);
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            if (noise == null) {
                return 0.0D;
            }
            double x = context.blockX() * xzScale + shiftX.compute(context);
            double y = context.blockY() * yScale + shiftY.compute(context);
            double z = context.blockZ() * xzScale + shiftZ.compute(context);
            return noise.getValue(x, y, z);
        }

        @Override
        public void fillArray(double[] values, DensityFunction.ContextProvider provider) {
            provider.fillAllDirectly(values, this);
        }

        @Override
        public DensityFunction mapAll(DensityFunction.Visitor visitor) {
            return visitor.apply(new ShiftedNoise(shiftX.mapAll(visitor), shiftY.mapAll(visitor),
                shiftZ.mapAll(visitor), xzScale, yScale, noiseData, noise));
        }

        @Override
        public double minValue() {
            return -maxValue();
        }

        @Override
        public double maxValue() {
            return noise == null ? 2.0D : noise.maxValue();
        }
    }

    public static final class RangeChoice implements DensityFunction {
        private final DensityFunction input;
        private final double minInclusive;
        private final double maxExclusive;
        private final DensityFunction whenInRange;
        private final DensityFunction whenOutOfRange;

        public RangeChoice(DensityFunction input, double minInclusive, double maxExclusive,
                DensityFunction whenInRange, DensityFunction whenOutOfRange) {
            this.input = input;
            this.minInclusive = minInclusive;
            this.maxExclusive = maxExclusive;
            this.whenInRange = whenInRange;
            this.whenOutOfRange = whenOutOfRange;
        }

        public DensityFunction input() {
            return input;
        }

        public double minInclusive() {
            return minInclusive;
        }

        public double maxExclusive() {
            return maxExclusive;
        }

        public DensityFunction whenInRange() {
            return whenInRange;
        }

        public DensityFunction whenOutOfRange() {
            return whenOutOfRange;
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            double value = input.compute(context);
            if (value >= minInclusive && value < maxExclusive) {
                return whenInRange.compute(context);
            }
            return whenOutOfRange.compute(context);
        }

        @Override
        public void fillArray(double[] values, DensityFunction.ContextProvider provider) {
            input.fillArray(values, provider);
            for (int index = 0; index < values.length; ++index) {
                double value = values[index];
                values[index] = value >= minInclusive && value < maxExclusive
                    ? whenInRange.compute(provider.forIndex(index))
                    : whenOutOfRange.compute(provider.forIndex(index));
            }
        }

        @Override
        public DensityFunction mapAll(DensityFunction.Visitor visitor) {
            return visitor.apply(new RangeChoice(input.mapAll(visitor), minInclusive,
                maxExclusive, whenInRange.mapAll(visitor), whenOutOfRange.mapAll(visitor)));
        }

        @Override
        public double minValue() {
            return Math.min(whenInRange.minValue(), whenOutOfRange.minValue());
        }

        @Override
        public double maxValue() {
            return Math.max(whenInRange.maxValue(), whenOutOfRange.maxValue());
        }
    }

    public interface ShiftNoise extends DensityFunction.SimpleFunction {
        NormalNoise.NoiseParameters noiseData();

        NormalNoise offsetNoise();

        ShiftNoise withNewNoise(NormalNoise noise);

        default ShiftNoise withNoise(NormalNoise noise) {
            return withNewNoise(noise);
        }

        @Override
        default double minValue() {
            return -maxValue();
        }

        @Override
        default double maxValue() {
            NormalNoise noise = offsetNoise();
            return (noise == null ? 2.0D : noise.maxValue()) * 4.0D;
        }

        default double computeShift(double x, double y, double z) {
            NormalNoise noise = offsetNoise();
            return noise == null ? 0.0D
                : noise.getValue(x * 0.25D, y * 0.25D, z * 0.25D) * 4.0D;
        }
    }

    public static final class ShiftA implements ShiftNoise {
        private final NormalNoise.NoiseParameters noiseData;
        private final NormalNoise offsetNoise;

        public ShiftA(NormalNoise.NoiseParameters noiseData, NormalNoise offsetNoise) {
            this.noiseData = noiseData;
            this.offsetNoise = offsetNoise;
        }

        @Override
        public NormalNoise.NoiseParameters noiseData() {
            return noiseData;
        }

        @Override
        public NormalNoise offsetNoise() {
            return offsetNoise;
        }

        @Override
        public ShiftA withNewNoise(NormalNoise noise) {
            return new ShiftA(noiseData, noise);
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return computeShift(context.blockX(), 0.0D, context.blockZ());
        }
    }

    public static final class ShiftB implements ShiftNoise {
        private final NormalNoise.NoiseParameters noiseData;
        private final NormalNoise offsetNoise;

        public ShiftB(NormalNoise.NoiseParameters noiseData, NormalNoise offsetNoise) {
            this.noiseData = noiseData;
            this.offsetNoise = offsetNoise;
        }

        @Override
        public NormalNoise.NoiseParameters noiseData() {
            return noiseData;
        }

        @Override
        public NormalNoise offsetNoise() {
            return offsetNoise;
        }

        @Override
        public ShiftB withNewNoise(NormalNoise noise) {
            return new ShiftB(noiseData, noise);
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return computeShift(context.blockZ(), context.blockX(), 0.0D);
        }
    }

    public static final class Shift implements ShiftNoise {
        private final NormalNoise.NoiseParameters noiseData;
        private final NormalNoise offsetNoise;

        public Shift(NormalNoise.NoiseParameters noiseData, NormalNoise offsetNoise) {
            this.noiseData = noiseData;
            this.offsetNoise = offsetNoise;
        }

        @Override
        public NormalNoise.NoiseParameters noiseData() {
            return noiseData;
        }

        @Override
        public NormalNoise offsetNoise() {
            return offsetNoise;
        }

        @Override
        public Shift withNewNoise(NormalNoise noise) {
            return new Shift(noiseData, noise);
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return computeShift(context.blockX(), context.blockY(), context.blockZ());
        }
    }

    public static final class BlendDensity extends TransformerWithContext {
        public BlendDensity(DensityFunction input) {
            super(input);
        }

        @Override
        double transform(DensityFunction.FunctionContext context, double value) {
            return context.blendDensity(value);
        }

        @Override
        public DensityFunction mapAll(DensityFunction.Visitor visitor) {
            return visitor.apply(new BlendDensity(input().mapAll(visitor)));
        }

        @Override
        public double minValue() {
            return Double.NEGATIVE_INFINITY;
        }

        @Override
        public double maxValue() {
            return Double.POSITIVE_INFINITY;
        }
    }

    public static final class Clamp extends PureTransformer {
        private final double minValue;
        private final double maxValue;

        public Clamp(DensityFunction input, double minValue, double maxValue) {
            super(input);
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        @Override
        double transform(double value) {
            return WorldgenMath.clamp(value, minValue, maxValue);
        }

        @Override
        public DensityFunction mapAll(DensityFunction.Visitor visitor) {
            return new Clamp(input().mapAll(visitor), minValue, maxValue);
        }

        @Override
        public double minValue() {
            return minValue;
        }

        @Override
        public double maxValue() {
            return maxValue;
        }
    }

    public enum MappedType {
        ABS,
        SQUARE,
        CUBE,
        HALF_NEGATIVE,
        QUARTER_NEGATIVE,
        SQUEEZE
    }

    public static final class Mapped extends PureTransformer {
        private final MappedType type;
        private final double minValue;
        private final double maxValue;

        private Mapped(MappedType type, DensityFunction input, double minValue,
                double maxValue) {
            super(input);
            this.type = type;
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        static Mapped create(MappedType type, DensityFunction input) {
            double inputMin = input.minValue();
            double transformedMin = transform(type, inputMin);
            double transformedMax = transform(type, input.maxValue());
            if (type == MappedType.ABS || type == MappedType.SQUARE) {
                return new Mapped(type, input, Math.max(0.0D, inputMin),
                    Math.max(transformedMin, transformedMax));
            }
            return new Mapped(type, input, transformedMin, transformedMax);
        }

        public MappedType type() {
            return type;
        }

        private static double transform(MappedType type, double value) {
            switch (type) {
                case ABS:
                    return Math.abs(value);
                case SQUARE:
                    return value * value;
                case CUBE:
                    return value * value * value;
                case HALF_NEGATIVE:
                    return value > 0.0D ? value : value * 0.5D;
                case QUARTER_NEGATIVE:
                    return value > 0.0D ? value : value * 0.25D;
                case SQUEEZE:
                    double clamped = WorldgenMath.clamp(value, -1.0D, 1.0D);
                    return clamped / 2.0D - clamped * clamped * clamped / 24.0D;
                default:
                    throw new AssertionError(type);
            }
        }

        @Override
        double transform(double value) {
            return transform(type, value);
        }

        @Override
        public DensityFunction mapAll(DensityFunction.Visitor visitor) {
            return create(type, input().mapAll(visitor));
        }

        @Override
        public double minValue() {
            return minValue;
        }

        @Override
        public double maxValue() {
            return maxValue;
        }
    }

    /** Minimal bridge implemented by the terrain-profile settings object. */
    public interface SlideSettings {
        double applySlide(double value, int blockY);

        double topSlideTarget();

        double bottomSlideTarget();
    }

    public static final class Slide extends TransformerWithContext {
        private final SlideSettings settings;

        public Slide(SlideSettings settings, DensityFunction input) {
            super(input);
            this.settings = settings;
        }

        public SlideSettings settings() {
            return settings;
        }

        @Override
        double transform(DensityFunction.FunctionContext context, double value) {
            return settings == null ? value : settings.applySlide(value, context.blockY());
        }

        @Override
        public DensityFunction mapAll(DensityFunction.Visitor visitor) {
            return visitor.apply(new Slide(settings, input().mapAll(visitor)));
        }

        @Override
        public double minValue() {
            if (settings == null) {
                return input().minValue();
            }
            return Math.min(input().minValue(), Math.min(settings.bottomSlideTarget(),
                settings.topSlideTarget()));
        }

        @Override
        public double maxValue() {
            if (settings == null) {
                return input().maxValue();
            }
            return Math.max(input().maxValue(), Math.max(settings.bottomSlideTarget(),
                settings.topSlideTarget()));
        }
    }

    public enum TwoArgumentType {
        ADD,
        MUL,
        MIN,
        MAX
    }

    public interface TwoArgumentFunction extends DensityFunction {
        TwoArgumentType type();

        DensityFunction argument1();

        DensityFunction argument2();
    }

    private static final class Ap2 implements TwoArgumentFunction {
        private final TwoArgumentType type;
        private final DensityFunction argument1;
        private final DensityFunction argument2;
        private final double minValue;
        private final double maxValue;

        Ap2(TwoArgumentType type, DensityFunction argument1, DensityFunction argument2,
                double minValue, double maxValue) {
            this.type = type;
            this.argument1 = argument1;
            this.argument2 = argument2;
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        @Override
        public TwoArgumentType type() {
            return type;
        }

        @Override
        public DensityFunction argument1() {
            return argument1;
        }

        @Override
        public DensityFunction argument2() {
            return argument2;
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            double first = argument1.compute(context);
            switch (type) {
                case ADD:
                    return first + argument2.compute(context);
                case MUL:
                    return first == 0.0D ? 0.0D : first * argument2.compute(context);
                case MIN:
                    return first < argument2.minValue()
                        ? first : Math.min(first, argument2.compute(context));
                case MAX:
                    return first > argument2.maxValue()
                        ? first : Math.max(first, argument2.compute(context));
                default:
                    throw new AssertionError(type);
            }
        }

        @Override
        public void fillArray(double[] values, DensityFunction.ContextProvider provider) {
            argument1.fillArray(values, provider);
            switch (type) {
                case ADD:
                    double[] second = new double[values.length];
                    argument2.fillArray(second, provider);
                    for (int index = 0; index < values.length; ++index) {
                        values[index] += second[index];
                    }
                    break;
                case MUL:
                    for (int index = 0; index < values.length; ++index) {
                        double first = values[index];
                        values[index] = first == 0.0D ? 0.0D
                            : first * argument2.compute(provider.forIndex(index));
                    }
                    break;
                case MIN:
                    double secondMin = argument2.minValue();
                    for (int index = 0; index < values.length; ++index) {
                        double first = values[index];
                        values[index] = first < secondMin ? first
                            : Math.min(first, argument2.compute(provider.forIndex(index)));
                    }
                    break;
                case MAX:
                    double secondMax = argument2.maxValue();
                    for (int index = 0; index < values.length; ++index) {
                        double first = values[index];
                        values[index] = first > secondMax ? first
                            : Math.max(first, argument2.compute(provider.forIndex(index)));
                    }
                    break;
                default:
                    throw new AssertionError(type);
            }
        }

        @Override
        public DensityFunction mapAll(DensityFunction.Visitor visitor) {
            return visitor.apply(twoArgument(type, argument1.mapAll(visitor),
                argument2.mapAll(visitor)));
        }

        @Override
        public double minValue() {
            return minValue;
        }

        @Override
        public double maxValue() {
            return maxValue;
        }
    }

    private static final class MulOrAdd extends PureTransformer implements TwoArgumentFunction {
        private final TwoArgumentType type;
        private final double minValue;
        private final double maxValue;
        private final double argument;

        MulOrAdd(TwoArgumentType type, DensityFunction input, double minValue, double maxValue,
                double argument) {
            super(input);
            this.type = type;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.argument = argument;
        }

        @Override
        public TwoArgumentType type() {
            return type;
        }

        @Override
        public DensityFunction argument1() {
            return constant(argument);
        }

        @Override
        public DensityFunction argument2() {
            return input();
        }

        public double argument() {
            return argument;
        }

        @Override
        double transform(double value) {
            return type == TwoArgumentType.MUL ? value * argument : value + argument;
        }

        @Override
        public DensityFunction mapAll(DensityFunction.Visitor visitor) {
            DensityFunction mappedInput = input().mapAll(visitor);
            double mappedMin;
            double mappedMax;
            if (type == TwoArgumentType.ADD) {
                mappedMin = mappedInput.minValue() + argument;
                mappedMax = mappedInput.maxValue() + argument;
            } else if (argument >= 0.0D) {
                mappedMin = mappedInput.minValue() * argument;
                mappedMax = mappedInput.maxValue() * argument;
            } else {
                mappedMin = mappedInput.maxValue() * argument;
                mappedMax = mappedInput.minValue() * argument;
            }
            return new MulOrAdd(type, mappedInput, mappedMin, mappedMax, argument);
        }

        @Override
        public double minValue() {
            return minValue;
        }

        @Override
        public double maxValue() {
            return maxValue;
        }
    }

    public static final class DensityCoordinate
            implements ToFloatFunction<DensityFunction.FunctionContext> {
        private final DensityFunction function;

        public DensityCoordinate(DensityFunction function) {
            this.function = function;
        }

        public DensityFunction function() {
            return function;
        }

        @Override
        public float apply(DensityFunction.FunctionContext context) {
            return (float) function.compute(context);
        }

        public DensityCoordinate mapAll(DensityFunction.Visitor visitor) {
            return new DensityCoordinate(function.mapAll(visitor));
        }

        @Override
        public String toString() {
            return "DensityCoordinate{" + function + '}';
        }
    }

    public static final class Spline implements DensityFunction {
        private final CubicSpline<DensityFunction.FunctionContext> spline;
        private final double minValue;
        private final double maxValue;

        public Spline(CubicSpline<DensityFunction.FunctionContext> spline, double minValue,
                double maxValue) {
            this.spline = spline;
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        public CubicSpline<DensityFunction.FunctionContext> spline() {
            return spline;
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return WorldgenMath.clamp((double) spline.apply(context), minValue, maxValue);
        }

        @Override
        public void fillArray(double[] values, DensityFunction.ContextProvider provider) {
            provider.fillAllDirectly(values, this);
        }

        @Override
        public DensityFunction mapAll(final DensityFunction.Visitor visitor) {
            CubicSpline<DensityFunction.FunctionContext> mapped = spline.mapAll(
                new CubicSpline.CoordinateVisitor<DensityFunction.FunctionContext>() {
                    @Override
                    public ToFloatFunction<DensityFunction.FunctionContext> visit(
                            ToFloatFunction<DensityFunction.FunctionContext> coordinate) {
                        if (coordinate instanceof DensityCoordinate) {
                            return ((DensityCoordinate) coordinate).mapAll(visitor);
                        }
                        return coordinate;
                    }
                });
            return visitor.apply(new Spline(mapped, minValue, maxValue));
        }

        @Override
        public double minValue() {
            return minValue;
        }

        @Override
        public double maxValue() {
            return maxValue;
        }
    }

    public enum TerrainShaperSplineType {
        OFFSET {
            @Override
            float sample(TerrainShaper shaper, TerrainShaper.Point point) {
                return shaper.offset(point);
            }
        },
        FACTOR {
            @Override
            float sample(TerrainShaper shaper, TerrainShaper.Point point) {
                return shaper.factor(point);
            }
        },
        JAGGEDNESS {
            @Override
            float sample(TerrainShaper shaper, TerrainShaper.Point point) {
                return shaper.jaggedness(point);
            }
        };

        abstract float sample(TerrainShaper shaper, TerrainShaper.Point point);
    }

    public static final class TerrainShaperSpline implements DensityFunction {
        private final DensityFunction continentalness;
        private final DensityFunction erosion;
        private final DensityFunction weirdness;
        private final TerrainShaper shaper;
        private final TerrainShaperSplineType splineType;
        private final double minValue;
        private final double maxValue;

        public TerrainShaperSpline(DensityFunction continentalness, DensityFunction erosion,
                DensityFunction weirdness, TerrainShaper shaper,
                TerrainShaperSplineType splineType, double minValue, double maxValue) {
            this.continentalness = continentalness;
            this.erosion = erosion;
            this.weirdness = weirdness;
            this.shaper = shaper;
            this.splineType = splineType;
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        public DensityFunction continentalness() {
            return continentalness;
        }

        public DensityFunction erosion() {
            return erosion;
        }

        public DensityFunction weirdness() {
            return weirdness;
        }

        public TerrainShaper shaper() {
            return shaper;
        }

        public TerrainShaperSplineType splineType() {
            return splineType;
        }

        public TerrainShaperSplineType spline() {
            return splineType;
        }

        public TerrainShaperSpline withShaper(TerrainShaper terrainShaper) {
            return new TerrainShaperSpline(continentalness, erosion, weirdness, terrainShaper,
                splineType, minValue, maxValue);
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            if (shaper == null) {
                return 0.0D;
            }
            TerrainShaper.Point point = TerrainShaper.makePoint(
                (float) continentalness.compute(context),
                (float) erosion.compute(context),
                (float) weirdness.compute(context));
            return WorldgenMath.clamp((double) splineType.sample(shaper, point), minValue,
                maxValue);
        }

        @Override
        public void fillArray(double[] values, DensityFunction.ContextProvider provider) {
            for (int index = 0; index < values.length; ++index) {
                values[index] = compute(provider.forIndex(index));
            }
        }

        @Override
        public DensityFunction mapAll(DensityFunction.Visitor visitor) {
            return visitor.apply(new TerrainShaperSpline(continentalness.mapAll(visitor),
                erosion.mapAll(visitor), weirdness.mapAll(visitor), shaper, splineType,
                minValue, maxValue));
        }

        @Override
        public double minValue() {
            return minValue;
        }

        @Override
        public double maxValue() {
            return maxValue;
        }
    }

    public static final class Constant implements DensityFunction.SimpleFunction {
        private final double value;

        public Constant(double value) {
            this.value = value;
        }

        public double value() {
            return value;
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            return value;
        }

        @Override
        public void fillArray(double[] values, DensityFunction.ContextProvider provider) {
            Arrays.fill(values, value);
        }

        @Override
        public double minValue() {
            return value;
        }

        @Override
        public double maxValue() {
            return value;
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof Constant
                && Double.doubleToLongBits(value)
                    == Double.doubleToLongBits(((Constant) object).value);
        }

        @Override
        public int hashCode() {
            long bits = Double.doubleToLongBits(value);
            return (int) (bits ^ bits >>> 32);
        }

        @Override
        public String toString() {
            return "Constant{" + value + '}';
        }
    }

    public static final class YClampedGradient implements DensityFunction.SimpleFunction {
        private final int fromY;
        private final int toY;
        private final double fromValue;
        private final double toValue;

        public YClampedGradient(int fromY, int toY, double fromValue, double toValue) {
            this.fromY = fromY;
            this.toY = toY;
            this.fromValue = fromValue;
            this.toValue = toValue;
        }

        public int fromY() {
            return fromY;
        }

        public int toY() {
            return toY;
        }

        public double fromValue() {
            return fromValue;
        }

        public double toValue() {
            return toValue;
        }

        @Override
        public double compute(DensityFunction.FunctionContext context) {
            double delta = inverseLerp(context.blockY(), fromY, toY);
            return WorldgenMath.clampedLerp(fromValue, toValue, delta);
        }

        private static double inverseLerp(double value, double start, double end) {
            return (value - start) / (end - start);
        }

        @Override
        public double minValue() {
            return Math.min(fromValue, toValue);
        }

        @Override
        public double maxValue() {
            return Math.max(fromValue, toValue);
        }
    }
}

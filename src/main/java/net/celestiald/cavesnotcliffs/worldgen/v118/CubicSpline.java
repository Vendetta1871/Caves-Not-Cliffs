package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Generic Hermite spline primitive matching Minecraft 1.18.2. */
public interface CubicSpline<C> extends ToFloatFunction<C> {
    String parityString();

    float min();

    float max();

    CubicSpline<C> mapAll(CoordinateVisitor<C> visitor);

    static <C> CubicSpline<C> constant(float value) {
        return new Constant<C>(value);
    }

    static <C> Builder<C> builder(ToFloatFunction<C> coordinate) {
        return new Builder<C>(coordinate, value -> value);
    }

    static <C> Builder<C> builder(ToFloatFunction<C> coordinate,
            ToFloatFunction<Float> valueTransformer) {
        return new Builder<C>(coordinate, valueTransformer);
    }

    interface CoordinateVisitor<C> {
        ToFloatFunction<C> visit(ToFloatFunction<C> coordinate);
    }

    final class Constant<C> implements CubicSpline<C> {
        private final float value;

        private Constant(float value) {
            this.value = value;
        }

        @Override
        public float apply(C context) {
            return value;
        }

        @Override
        public String parityString() {
            return String.format(Locale.ROOT, "k=%.3f", value);
        }

        @Override
        public float min() {
            return value;
        }

        @Override
        public float max() {
            return value;
        }

        @Override
        public CubicSpline<C> mapAll(CoordinateVisitor<C> visitor) {
            return this;
        }

        public float value() {
            return value;
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof Constant
                && Float.floatToIntBits(value) == Float.floatToIntBits(((Constant<?>) object).value);
        }

        @Override
        public int hashCode() {
            return Float.floatToIntBits(value);
        }

        @Override
        public String toString() {
            return "Constant{" + value + '}';
        }
    }

    final class Builder<C> {
        private final ToFloatFunction<C> coordinate;
        private final ToFloatFunction<Float> valueTransformer;
        private final List<Float> locations = new ArrayList<Float>();
        private final List<CubicSpline<C>> values = new ArrayList<CubicSpline<C>>();
        private final List<Float> derivatives = new ArrayList<Float>();

        private Builder(ToFloatFunction<C> coordinate, ToFloatFunction<Float> valueTransformer) {
            this.coordinate = coordinate;
            this.valueTransformer = valueTransformer;
        }

        public Builder<C> addPoint(float location, float value, float derivative) {
            return addPoint(location, CubicSpline.<C>constant(valueTransformer.apply(value)), derivative);
        }

        public Builder<C> addPoint(float location, CubicSpline<C> value, float derivative) {
            if (!locations.isEmpty() && location <= locations.get(locations.size() - 1)) {
                throw new IllegalArgumentException("Please register points in ascending order");
            }
            locations.add(location);
            values.add(value);
            derivatives.add(derivative);
            return this;
        }

        public CubicSpline<C> build() {
            if (locations.isEmpty()) {
                throw new IllegalStateException("No elements added");
            }
            float[] locationArray = new float[locations.size()];
            float[] derivativeArray = new float[derivatives.size()];
            for (int i = 0; i < locations.size(); ++i) {
                locationArray[i] = locations.get(i);
                derivativeArray[i] = derivatives.get(i);
            }
            return new Multipoint<C>(coordinate, locationArray,
                Collections.unmodifiableList(new ArrayList<CubicSpline<C>>(values)), derivativeArray);
        }
    }

    final class Multipoint<C> implements CubicSpline<C> {
        private final ToFloatFunction<C> coordinate;
        private final float[] locations;
        private final List<CubicSpline<C>> values;
        private final float[] derivatives;

        public Multipoint(ToFloatFunction<C> coordinate, float[] locations,
                List<CubicSpline<C>> values, float[] derivatives) {
            if (locations.length != values.size() || locations.length != derivatives.length) {
                throw new IllegalArgumentException("All lengths must be equal, got: "
                    + locations.length + " " + values.size() + " " + derivatives.length);
            }
            this.coordinate = coordinate;
            this.locations = locations.clone();
            this.values = Collections.unmodifiableList(new ArrayList<CubicSpline<C>>(values));
            this.derivatives = derivatives.clone();
        }

        @Override
        public float apply(C context) {
            float coordinateValue = coordinate.apply(context);
            int lowerIndex = WorldgenMath.binarySearch(0, locations.length,
                index -> coordinateValue < locations[index]) - 1;
            int lastIndex = locations.length - 1;
            if (lowerIndex < 0) {
                return values.get(0).apply(context)
                    + derivatives[0] * (coordinateValue - locations[0]);
            }
            if (lowerIndex == lastIndex) {
                return values.get(lastIndex).apply(context)
                    + derivatives[lastIndex] * (coordinateValue - locations[lastIndex]);
            }

            float lowerLocation = locations[lowerIndex];
            float upperLocation = locations[lowerIndex + 1];
            float delta = (coordinateValue - lowerLocation) / (upperLocation - lowerLocation);
            CubicSpline<C> lowerSpline = values.get(lowerIndex);
            CubicSpline<C> upperSpline = values.get(lowerIndex + 1);
            float lowerValue = lowerSpline.apply(context);
            float upperValue = upperSpline.apply(context);
            float lowerSlope = derivatives[lowerIndex];
            float upperSlope = derivatives[lowerIndex + 1];
            float lowerHermite = lowerSlope * (upperLocation - lowerLocation)
                - (upperValue - lowerValue);
            float upperHermite = -upperSlope * (upperLocation - lowerLocation)
                + (upperValue - lowerValue);
            return WorldgenMath.lerp(delta, lowerValue, upperValue)
                + delta * (1.0F - delta) * WorldgenMath.lerp(delta, lowerHermite, upperHermite);
        }

        @Override
        public String parityString() {
            StringBuilder builder = new StringBuilder("Spline{coordinate=")
                .append(coordinate)
                .append(", locations=").append(format(locations))
                .append(", derivatives=").append(format(derivatives))
                .append(", values=[");
            for (int i = 0; i < values.size(); ++i) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(values.get(i).parityString());
            }
            return builder.append("]}").toString();
        }

        private static String format(float[] values) {
            StringBuilder builder = new StringBuilder("[");
            for (int i = 0; i < values.length; ++i) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(String.format(Locale.ROOT, "%.3f", values[i]));
            }
            return builder.append(']').toString();
        }

        @Override
        public float min() {
            float min = Float.POSITIVE_INFINITY;
            for (CubicSpline<C> value : values) {
                min = Math.min(min, value.min());
            }
            return min;
        }

        @Override
        public float max() {
            float max = Float.NEGATIVE_INFINITY;
            for (CubicSpline<C> value : values) {
                max = Math.max(max, value.max());
            }
            return max;
        }

        @Override
        public CubicSpline<C> mapAll(CoordinateVisitor<C> visitor) {
            List<CubicSpline<C>> mapped = new ArrayList<CubicSpline<C>>(values.size());
            for (CubicSpline<C> value : values) {
                mapped.add(value.mapAll(visitor));
            }
            return new Multipoint<C>(visitor.visit(coordinate), locations, mapped, derivatives);
        }

        public ToFloatFunction<C> coordinate() {
            return coordinate;
        }

        public float[] locations() {
            return locations.clone();
        }

        public List<CubicSpline<C>> values() {
            return values;
        }

        public float[] derivatives() {
            return derivatives.clone();
        }
    }
}

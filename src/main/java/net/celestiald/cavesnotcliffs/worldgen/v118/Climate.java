package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Quantized six-axis climate parameter types and nearest-neighbor index from 1.18.2. */
public final class Climate {
    public static final int PARAMETER_COUNT = 7;
    private static final float QUANTIZATION_FACTOR = 10000.0F;

    private Climate() {
    }

    public static TargetPoint target(float temperature, float humidity, float continentalness,
            float erosion, float depth, float weirdness) {
        return new TargetPoint(quantizeCoord(temperature), quantizeCoord(humidity),
            quantizeCoord(continentalness), quantizeCoord(erosion), quantizeCoord(depth),
            quantizeCoord(weirdness));
    }

    public static ParameterPoint parameters(float temperature, float humidity, float continentalness,
            float erosion, float depth, float weirdness, float offset) {
        return parameters(Parameter.point(temperature), Parameter.point(humidity),
            Parameter.point(continentalness), Parameter.point(erosion), Parameter.point(depth),
            Parameter.point(weirdness), offset);
    }

    public static ParameterPoint parameters(Parameter temperature, Parameter humidity,
            Parameter continentalness, Parameter erosion, Parameter depth, Parameter weirdness,
            float offset) {
        return new ParameterPoint(temperature, humidity, continentalness, erosion, depth, weirdness,
            quantizeCoord(offset));
    }

    public static long quantizeCoord(float value) {
        return (long) (value * QUANTIZATION_FACTOR);
    }

    public static float unquantizeCoord(long value) {
        return (float) value / QUANTIZATION_FACTOR;
    }

    public static final class TargetPoint {
        private final long temperature;
        private final long humidity;
        private final long continentalness;
        private final long erosion;
        private final long depth;
        private final long weirdness;

        public TargetPoint(long temperature, long humidity, long continentalness,
                long erosion, long depth, long weirdness) {
            this.temperature = temperature;
            this.humidity = humidity;
            this.continentalness = continentalness;
            this.erosion = erosion;
            this.depth = depth;
            this.weirdness = weirdness;
        }

        public long[] toParameterArray() {
            return new long[] {
                temperature, humidity, continentalness, erosion, depth, weirdness, 0L
            };
        }

        public long temperature() {
            return temperature;
        }

        public long humidity() {
            return humidity;
        }

        public long continentalness() {
            return continentalness;
        }

        public long erosion() {
            return erosion;
        }

        public long depth() {
            return depth;
        }

        public long weirdness() {
            return weirdness;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof TargetPoint)) {
                return false;
            }
            TargetPoint other = (TargetPoint) object;
            return temperature == other.temperature && humidity == other.humidity
                && continentalness == other.continentalness && erosion == other.erosion
                && depth == other.depth && weirdness == other.weirdness;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(toParameterArray());
        }
    }

    public static final class Parameter {
        private final long min;
        private final long max;

        public Parameter(long min, long max) {
            this.min = min;
            this.max = max;
        }

        public static Parameter point(float value) {
            return span(value, value);
        }

        public static Parameter span(float min, float max) {
            if (min > max) {
                throw new IllegalArgumentException("min > max: " + min + " " + max);
            }
            return new Parameter(quantizeCoord(min), quantizeCoord(max));
        }

        public static Parameter span(Parameter min, Parameter max) {
            if (min.min() > max.max()) {
                throw new IllegalArgumentException("min > max: " + min + " " + max);
            }
            return new Parameter(min.min(), max.max());
        }

        public long distance(long value) {
            long above = value - max;
            long below = min - value;
            if (above > 0L) {
                return above;
            }
            return Math.max(below, 0L);
        }

        public long distance(Parameter other) {
            long above = other.min() - max;
            long below = min - other.max();
            if (above > 0L) {
                return above;
            }
            return Math.max(below, 0L);
        }

        private Parameter spanNullable(Parameter other) {
            return other == null ? this
                : new Parameter(Math.min(min, other.min()), Math.max(max, other.max()));
        }

        public long min() {
            return min;
        }

        public long max() {
            return max;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof Parameter)) {
                return false;
            }
            Parameter other = (Parameter) object;
            return min == other.min && max == other.max;
        }

        @Override
        public int hashCode() {
            return 31 * (int) (min ^ min >>> 32) + (int) (max ^ max >>> 32);
        }

        @Override
        public String toString() {
            return min == max ? String.format("%d", min) : String.format("[%d-%d]", min, max);
        }
    }

    public static final class ParameterPoint {
        private final Parameter temperature;
        private final Parameter humidity;
        private final Parameter continentalness;
        private final Parameter erosion;
        private final Parameter depth;
        private final Parameter weirdness;
        private final long offset;

        public ParameterPoint(Parameter temperature, Parameter humidity, Parameter continentalness,
                Parameter erosion, Parameter depth, Parameter weirdness, long offset) {
            this.temperature = temperature;
            this.humidity = humidity;
            this.continentalness = continentalness;
            this.erosion = erosion;
            this.depth = depth;
            this.weirdness = weirdness;
            this.offset = offset;
        }

        public long fitness(TargetPoint target) {
            return WorldgenMath.square(temperature.distance(target.temperature()))
                + WorldgenMath.square(humidity.distance(target.humidity()))
                + WorldgenMath.square(continentalness.distance(target.continentalness()))
                + WorldgenMath.square(erosion.distance(target.erosion()))
                + WorldgenMath.square(depth.distance(target.depth()))
                + WorldgenMath.square(weirdness.distance(target.weirdness()))
                + WorldgenMath.square(offset);
        }

        public List<Parameter> parameterSpace() {
            return Collections.unmodifiableList(Arrays.asList(temperature, humidity, continentalness,
                erosion, depth, weirdness, new Parameter(offset, offset)));
        }

        public Parameter temperature() {
            return temperature;
        }

        public Parameter humidity() {
            return humidity;
        }

        public Parameter continentalness() {
            return continentalness;
        }

        public Parameter erosion() {
            return erosion;
        }

        public Parameter depth() {
            return depth;
        }

        public Parameter weirdness() {
            return weirdness;
        }

        public long offset() {
            return offset;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof ParameterPoint)) {
                return false;
            }
            ParameterPoint other = (ParameterPoint) object;
            return offset == other.offset
                && temperature.equals(other.temperature)
                && humidity.equals(other.humidity)
                && continentalness.equals(other.continentalness)
                && erosion.equals(other.erosion)
                && depth.equals(other.depth)
                && weirdness.equals(other.weirdness);
        }

        @Override
        public int hashCode() {
            int result = temperature.hashCode();
            result = 31 * result + humidity.hashCode();
            result = 31 * result + continentalness.hashCode();
            result = 31 * result + erosion.hashCode();
            result = 31 * result + depth.hashCode();
            result = 31 * result + weirdness.hashCode();
            return 31 * result + (int) (offset ^ offset >>> 32);
        }
    }

    public static final class Entry<T> {
        private final ParameterPoint parameters;
        private final T value;

        public Entry(ParameterPoint parameters, T value) {
            this.parameters = parameters;
            this.value = value;
        }

        public ParameterPoint parameters() {
            return parameters;
        }

        public T value() {
            return value;
        }
    }

    public static final class ParameterList<T> {
        private final List<Entry<T>> values;
        private final RTree<T> index;

        public ParameterList(List<Entry<T>> values) {
            this.values = Collections.unmodifiableList(new ArrayList<Entry<T>>(values));
            index = RTree.create(this.values);
        }

        public List<Entry<T>> values() {
            return values;
        }

        public T findValue(TargetPoint target) {
            return index.search(target);
        }

        public T findValueBruteForce(TargetPoint target) {
            if (values.isEmpty()) {
                throw new IllegalStateException("Need at least one value");
            }
            Entry<T> best = values.get(0);
            long bestFitness = best.parameters().fitness(target);
            for (int i = 1; i < values.size(); ++i) {
                Entry<T> candidate = values.get(i);
                long fitness = candidate.parameters().fitness(target);
                if (fitness < bestFitness) {
                    bestFitness = fitness;
                    best = candidate;
                }
            }
            return best.value();
        }
    }

    private static final class RTree<T> {
        private static final int CHILDREN_PER_NODE = 10;

        private final Node<T> root;
        private final ThreadLocal<Leaf<T>> lastResult = new ThreadLocal<Leaf<T>>();

        private RTree(Node<T> root) {
            this.root = root;
        }

        private static <T> RTree<T> create(List<Entry<T>> entries) {
            if (entries.isEmpty()) {
                throw new IllegalArgumentException("Need at least one value to build the search tree.");
            }
            int dimensions = entries.get(0).parameters().parameterSpace().size();
            if (dimensions != PARAMETER_COUNT) {
                throw new IllegalStateException("Expecting parameter space to be " + PARAMETER_COUNT
                    + ", got " + dimensions);
            }
            List<Node<T>> leaves = new ArrayList<Node<T>>(entries.size());
            for (Entry<T> entry : entries) {
                leaves.add(new Leaf<T>(entry.parameters(), entry.value()));
            }
            return new RTree<T>(build(dimensions, leaves));
        }

        private static <T> Node<T> build(int dimensions, List<? extends Node<T>> input) {
            if (input.isEmpty()) {
                throw new IllegalStateException("Need at least one child to build a node");
            }
            List<Node<T>> nodes = new ArrayList<Node<T>>(input);
            if (nodes.size() == 1) {
                return nodes.get(0);
            }
            if (nodes.size() <= CHILDREN_PER_NODE) {
                Collections.sort(nodes, Comparator.comparingLong(RTree::absoluteCenterCost));
                return new SubTree<T>(nodes);
            }

            long bestCost = Long.MAX_VALUE;
            int bestDimension = -1;
            List<SubTree<T>> bestBuckets = null;
            for (int dimension = 0; dimension < dimensions; ++dimension) {
                sort(nodes, dimensions, dimension, false);
                List<SubTree<T>> buckets = bucketize(nodes);
                long cost = 0L;
                for (SubTree<T> bucket : buckets) {
                    cost += cost(bucket.parameterSpace);
                }
                if (cost < bestCost) {
                    bestCost = cost;
                    bestDimension = dimension;
                    bestBuckets = buckets;
                }
            }

            List<Node<T>> bucketNodes = new ArrayList<Node<T>>(bestBuckets);
            sort(bucketNodes, dimensions, bestDimension, true);
            List<Node<T>> children = new ArrayList<Node<T>>(bucketNodes.size());
            for (Node<T> bucket : bucketNodes) {
                SubTree<T> subTree = (SubTree<T>) bucket;
                children.add(build(dimensions, Arrays.asList(subTree.children)));
            }
            return new SubTree<T>(children);
        }

        private static long absoluteCenterCost(Node<?> node) {
            long cost = 0L;
            for (Parameter parameter : node.parameterSpace) {
                cost += Math.abs((parameter.min() + parameter.max()) / 2L);
            }
            return cost;
        }

        private static <T> void sort(List<Node<T>> nodes, int dimensions,
                int firstDimension, boolean absolute) {
            Comparator<Node<T>> comparator = comparator(firstDimension, absolute);
            for (int offset = 1; offset < dimensions; ++offset) {
                comparator = comparator.thenComparing(comparator(
                    (firstDimension + offset) % dimensions, absolute));
            }
            Collections.sort(nodes, comparator);
        }

        private static <T> Comparator<Node<T>> comparator(int dimension, boolean absolute) {
            return Comparator.comparingLong(node -> {
                Parameter parameter = node.parameterSpace[dimension];
                long center = (parameter.min() + parameter.max()) / 2L;
                return absolute ? Math.abs(center) : center;
            });
        }

        private static <T> List<SubTree<T>> bucketize(List<Node<T>> nodes) {
            List<SubTree<T>> result = new ArrayList<SubTree<T>>();
            List<Node<T>> bucket = new ArrayList<Node<T>>();
            int bucketSize = (int) Math.pow(CHILDREN_PER_NODE,
                Math.floor(Math.log(nodes.size() - 0.01D) / Math.log(CHILDREN_PER_NODE)));
            for (Node<T> node : nodes) {
                bucket.add(node);
                if (bucket.size() >= bucketSize) {
                    result.add(new SubTree<T>(bucket));
                    bucket = new ArrayList<Node<T>>();
                }
            }
            if (!bucket.isEmpty()) {
                result.add(new SubTree<T>(bucket));
            }
            return result;
        }

        private static long cost(Parameter[] parameterSpace) {
            long result = 0L;
            for (Parameter parameter : parameterSpace) {
                result += Math.abs(parameter.max() - parameter.min());
            }
            return result;
        }

        private T search(TargetPoint target) {
            long[] parameters = target.toParameterArray();
            Leaf<T> result = root.search(parameters, lastResult.get());
            lastResult.set(result);
            return result.value;
        }

        private abstract static class Node<T> {
            protected final Parameter[] parameterSpace;

            private Node(List<Parameter> parameterSpace) {
                this.parameterSpace = parameterSpace.toArray(new Parameter[0]);
            }

            protected abstract Leaf<T> search(long[] target, Leaf<T> currentBest);

            protected long distance(long[] target) {
                long result = 0L;
                for (int i = 0; i < PARAMETER_COUNT; ++i) {
                    result += WorldgenMath.square(parameterSpace[i].distance(target[i]));
                }
                return result;
            }
        }

        private static final class SubTree<T> extends Node<T> {
            private final Node<T>[] children;

            @SuppressWarnings("unchecked")
            private SubTree(List<? extends Node<T>> children) {
                super(combineParameterSpace(children));
                this.children = children.toArray(new Node[0]);
            }

            @Override
            protected Leaf<T> search(long[] target, Leaf<T> currentBest) {
                long bestDistance = currentBest == null ? Long.MAX_VALUE : currentBest.distance(target);
                Leaf<T> best = currentBest;
                for (Node<T> child : children) {
                    long childDistance = child.distance(target);
                    if (bestDistance <= childDistance) {
                        continue;
                    }
                    Leaf<T> candidate = child.search(target, best);
                    long candidateDistance = child == candidate
                        ? childDistance : candidate.distance(target);
                    if (bestDistance <= candidateDistance) {
                        continue;
                    }
                    bestDistance = candidateDistance;
                    best = candidate;
                }
                return best;
            }
        }

        private static final class Leaf<T> extends Node<T> {
            private final T value;

            private Leaf(ParameterPoint parameters, T value) {
                super(parameters.parameterSpace());
                this.value = value;
            }

            @Override
            protected Leaf<T> search(long[] target, Leaf<T> currentBest) {
                return this;
            }
        }

        private static <T> List<Parameter> combineParameterSpace(
                List<? extends Node<T>> children) {
            if (children.isEmpty()) {
                throw new IllegalArgumentException("SubTree needs at least one child");
            }
            List<Parameter> result = new ArrayList<Parameter>(
                Collections.nCopies(PARAMETER_COUNT, (Parameter) null));
            for (Node<T> child : children) {
                for (int dimension = 0; dimension < PARAMETER_COUNT; ++dimension) {
                    result.set(dimension,
                        child.parameterSpace[dimension].spanNullable(result.get(dimension)));
                }
            }
            return result;
        }
    }
}

package net.celestiald.cavesnotcliffs.worldgen.v118;

/**
 * Dependency-free Java 8 form of Minecraft 1.18.2's density-function contract.
 *
 * <p>Implementations are immutable expression-tree nodes. A context provider is deliberately
 * separate from a single point so later terrain generation can supply cache-aware bulk contexts
 * without changing the numeric expression tree.</p>
 */
public interface DensityFunction {
    double compute(FunctionContext context);

    void fillArray(double[] values, ContextProvider contextProvider);

    DensityFunction mapAll(Visitor visitor);

    double minValue();

    double maxValue();

    default DensityFunction clamp(double min, double max) {
        return DensityFunctions.clamp(this, min, max);
    }

    default DensityFunction abs() {
        return DensityFunctions.map(this, DensityFunctions.MappedType.ABS);
    }

    default DensityFunction square() {
        return DensityFunctions.map(this, DensityFunctions.MappedType.SQUARE);
    }

    default DensityFunction cube() {
        return DensityFunctions.map(this, DensityFunctions.MappedType.CUBE);
    }

    default DensityFunction halfNegative() {
        return DensityFunctions.map(this, DensityFunctions.MappedType.HALF_NEGATIVE);
    }

    default DensityFunction quarterNegative() {
        return DensityFunctions.map(this, DensityFunctions.MappedType.QUARTER_NEGATIVE);
    }

    default DensityFunction squeeze() {
        return DensityFunctions.map(this, DensityFunctions.MappedType.SQUEEZE);
    }

    interface FunctionContext {
        int blockX();

        int blockY();

        int blockZ();

        /** Empty-blender behavior; a future chunk-upgrade context may override this hook. */
        default double blendDensity(double density) {
            return density;
        }
    }

    final class SinglePointContext implements FunctionContext {
        private final int blockX;
        private final int blockY;
        private final int blockZ;

        public SinglePointContext(int blockX, int blockY, int blockZ) {
            this.blockX = blockX;
            this.blockY = blockY;
            this.blockZ = blockZ;
        }

        @Override
        public int blockX() {
            return blockX;
        }

        @Override
        public int blockY() {
            return blockY;
        }

        @Override
        public int blockZ() {
            return blockZ;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof SinglePointContext)) {
                return false;
            }
            SinglePointContext other = (SinglePointContext) object;
            return blockX == other.blockX && blockY == other.blockY && blockZ == other.blockZ;
        }

        @Override
        public int hashCode() {
            int result = blockX;
            result = 31 * result + blockY;
            return 31 * result + blockZ;
        }

        @Override
        public String toString() {
            return "SinglePointContext{" + blockX + ", " + blockY + ", " + blockZ + '}';
        }
    }

    interface ContextProvider {
        FunctionContext forIndex(int index);

        void fillAllDirectly(double[] values, DensityFunction function);
    }

    interface Visitor {
        DensityFunction apply(DensityFunction function);
    }

    interface SimpleFunction extends DensityFunction {
        @Override
        default void fillArray(double[] values, ContextProvider contextProvider) {
            contextProvider.fillAllDirectly(values, this);
        }

        @Override
        default DensityFunction mapAll(Visitor visitor) {
            return visitor.apply(this);
        }
    }
}

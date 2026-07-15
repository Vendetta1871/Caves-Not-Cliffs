package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.Arrays;

/**
 * Dependency-free Java 8 port of Minecraft 1.18.2's noise-based Overworld aquifer.
 *
 * <p>The instance owns exactly the cache halo used by one 16x16 chunk. The caller supplies the
 * four already-seeded density functions, the {@code minecraft:aquifer} positional stream, a
 * preliminary-surface callback, and the dimension's global fluid picker. This keeps the aquifer
 * independent of both Forge block states and the later noise-router/column implementation. Like
 * the official chunk-local aquifer, an instance is deliberately not thread-safe.</p>
 */
public final class NoiseBasedAquifer {
    public static final int X_SPACING = 16;
    public static final int Y_SPACING = 12;
    public static final int Z_SPACING = 16;
    public static final int X_RANGE = 10;
    public static final int Y_RANGE = 9;
    public static final int Z_RANGE = 10;
    public static final int WAY_BELOW_MIN_Y = -32512;

    private static final int[][] SURFACE_SAMPLING_OFFSETS_IN_CHUNKS = {
        {-2, -1}, {-1, -1}, {0, -1}, {1, -1},
        {-3, 0}, {-2, 0}, {-1, 0}, {0, 0}, {1, 0},
        {-2, 1}, {-1, 1}, {0, 1}, {1, 1}
    };
    private static final long EMPTY_LOCATION = Long.MAX_VALUE;
    private static final double FLOWING_UPDATE_SIMILARITY = similarity(100, 144);

    private final DensityFunction barrierNoise;
    private final DensityFunction fluidLevelFloodednessNoise;
    private final DensityFunction fluidLevelSpreadNoise;
    private final DensityFunction lavaNoise;
    private final PositionalRandomFactory positionalRandomFactory;
    private final PreliminarySurfaceLookup preliminarySurfaceLookup;
    private final FluidPicker globalFluidPicker;
    private final FluidStatus[] aquiferCache;
    private final long[] aquiferLocationCache;
    private final int minGridX;
    private final int minGridY;
    private final int minGridZ;
    private final int gridSizeX;
    private final int gridSizeY;
    private final int gridSizeZ;
    private final BarrierSample barrierSample = new BarrierSample();

    /**
     * Creates the aquifer cache for one chunk.
     *
     * @param chunkX chunk coordinate, not a block coordinate
     * @param chunkZ chunk coordinate, not a block coordinate
     * @param minY minimum generated block Y (normally {@code -64})
     * @param height generated height (normally {@code 384})
     */
    public NoiseBasedAquifer(int chunkX, int chunkZ, int minY, int height,
            DensityFunction barrierNoise, DensityFunction fluidLevelFloodednessNoise,
            DensityFunction fluidLevelSpreadNoise, DensityFunction lavaNoise,
            PositionalRandomFactory positionalRandomFactory,
            PreliminarySurfaceLookup preliminarySurfaceLookup, FluidPicker globalFluidPicker) {
        if (height <= 0) {
            throw new IllegalArgumentException("Aquifer height must be positive: " + height);
        }
        this.barrierNoise = requireNonNull(barrierNoise, "barrierNoise");
        this.fluidLevelFloodednessNoise = requireNonNull(fluidLevelFloodednessNoise,
                "fluidLevelFloodednessNoise");
        this.fluidLevelSpreadNoise = requireNonNull(fluidLevelSpreadNoise,
                "fluidLevelSpreadNoise");
        this.lavaNoise = requireNonNull(lavaNoise, "lavaNoise");
        this.positionalRandomFactory = requireNonNull(positionalRandomFactory,
                "positionalRandomFactory");
        this.preliminarySurfaceLookup = requireNonNull(preliminarySurfaceLookup,
                "preliminarySurfaceLookup");
        this.globalFluidPicker = requireNonNull(globalFluidPicker, "globalFluidPicker");

        int minBlockX = chunkX * 16;
        int minBlockZ = chunkZ * 16;
        this.minGridX = gridX(minBlockX) - 1;
        int maxGridX = gridX(minBlockX + 15) + 1;
        this.gridSizeX = maxGridX - minGridX + 1;
        this.minGridY = gridY(minY) - 1;
        int maxGridY = gridY(minY + height) + 1;
        this.gridSizeY = maxGridY - minGridY + 1;
        this.minGridZ = gridZ(minBlockZ) - 1;
        int maxGridZ = gridZ(minBlockZ + 15) + 1;
        this.gridSizeZ = maxGridZ - minGridZ + 1;

        int cacheSize = gridSizeX * gridSizeY * gridSizeZ;
        this.aquiferCache = new FluidStatus[cacheSize];
        this.aquiferLocationCache = new long[cacheSize];
        Arrays.fill(aquiferLocationCache, EMPTY_LOCATION);
    }

    public Result compute(int blockX, int blockY, int blockZ, double density) {
        return compute(new DensityFunction.SinglePointContext(blockX, blockY, blockZ), density);
    }

    /** Returns the material decision and the fluid-tick flag for one density sample. */
    public Result compute(DensityFunction.FunctionContext context, double density) {
        int blockX = context.blockX();
        int blockY = context.blockY();
        int blockZ = context.blockZ();
        if (density > 0.0D) {
            return Result.solid();
        }

        FluidStatus globalStatus = globalFluidPicker.computeFluid(blockX, blockY, blockZ);
        if (globalStatus.at(blockY) == Material.LAVA) {
            return Result.material(Material.LAVA, false);
        }

        int baseGridX = Math.floorDiv(blockX - 5, X_SPACING);
        int baseGridY = Math.floorDiv(blockY + 1, Y_SPACING);
        int baseGridZ = Math.floorDiv(blockZ - 5, Z_SPACING);
        int firstDistance = Integer.MAX_VALUE;
        int secondDistance = Integer.MAX_VALUE;
        int thirdDistance = Integer.MAX_VALUE;
        long firstLocation = 0L;
        long secondLocation = 0L;
        long thirdLocation = 0L;

        for (int gridOffsetX = 0; gridOffsetX <= 1; ++gridOffsetX) {
            for (int gridOffsetY = -1; gridOffsetY <= 1; ++gridOffsetY) {
                for (int gridOffsetZ = 0; gridOffsetZ <= 1; ++gridOffsetZ) {
                    int candidateGridX = baseGridX + gridOffsetX;
                    int candidateGridY = baseGridY + gridOffsetY;
                    int candidateGridZ = baseGridZ + gridOffsetZ;
                    int index = getIndex(candidateGridX, candidateGridY, candidateGridZ);
                    long candidateLocation = aquiferLocationCache[index];
                    if (candidateLocation == EMPTY_LOCATION) {
                        RandomSource random = positionalRandomFactory.at(candidateGridX,
                                candidateGridY, candidateGridZ);
                        candidateLocation = packBlockPosition(
                                candidateGridX * X_SPACING + random.nextInt(X_RANGE),
                                candidateGridY * Y_SPACING + random.nextInt(Y_RANGE),
                                candidateGridZ * Z_SPACING + random.nextInt(Z_RANGE));
                        aquiferLocationCache[index] = candidateLocation;
                    }

                    int deltaX = unpackBlockX(candidateLocation) - blockX;
                    int deltaY = unpackBlockY(candidateLocation) - blockY;
                    int deltaZ = unpackBlockZ(candidateLocation) - blockZ;
                    int distance = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
                    if (distance <= firstDistance) {
                        thirdLocation = secondLocation;
                        secondLocation = firstLocation;
                        firstLocation = candidateLocation;
                        thirdDistance = secondDistance;
                        secondDistance = firstDistance;
                        firstDistance = distance;
                    } else if (distance <= secondDistance) {
                        thirdLocation = secondLocation;
                        secondLocation = candidateLocation;
                        thirdDistance = secondDistance;
                        secondDistance = distance;
                    } else if (distance <= thirdDistance) {
                        thirdLocation = candidateLocation;
                        thirdDistance = distance;
                    }
                }
            }
        }

        FluidStatus firstStatus = getAquiferStatus(firstLocation);
        double firstSimilarity = similarity(firstDistance, secondDistance);
        Material firstMaterial = firstStatus.at(blockY);
        if (firstSimilarity <= 0.0D) {
            return Result.material(firstMaterial,
                    firstSimilarity >= FLOWING_UPDATE_SIMILARITY);
        }

        if (firstMaterial == Material.WATER
                && globalFluidPicker.computeFluid(blockX, blockY - 1, blockZ)
                        .at(blockY - 1) == Material.LAVA) {
            return Result.material(firstMaterial, true);
        }

        barrierSample.value = Double.NaN;
        FluidStatus secondStatus = getAquiferStatus(secondLocation);
        double firstPressure = firstSimilarity
                * calculatePressure(context, barrierSample, firstStatus, secondStatus);
        if (density + firstPressure > 0.0D) {
            return Result.solid();
        }

        FluidStatus thirdStatus = getAquiferStatus(thirdLocation);
        double secondSimilarity = similarity(firstDistance, thirdDistance);
        if (secondSimilarity > 0.0D) {
            double secondPressure = firstSimilarity * secondSimilarity
                    * calculatePressure(context, barrierSample, firstStatus, thirdStatus);
            if (density + secondPressure > 0.0D) {
                return Result.solid();
            }
        }

        double thirdSimilarity = similarity(secondDistance, thirdDistance);
        if (thirdSimilarity > 0.0D) {
            double thirdPressure = firstSimilarity * thirdSimilarity
                    * calculatePressure(context, barrierSample, secondStatus, thirdStatus);
            if (density + thirdPressure > 0.0D) {
                return Result.solid();
            }
        }
        return Result.material(firstMaterial, true);
    }

    private double calculatePressure(DensityFunction.FunctionContext context,
            BarrierSample barrierSample, FluidStatus first, FluidStatus second) {
        int blockY = context.blockY();
        if (isWaterLavaBoundary(first.at(blockY), second.at(blockY))) {
            return PRESSURE_LAVA_WATER;
        }
        if (first.fluidLevel() == second.fluidLevel()) {
            return 0.0D;
        }
        double pressureShape = pressureShape(blockY, first, second);
        double barrier = 0.0D;
        if (pressureShape >= -2.0D && pressureShape <= 2.0D) {
            if (Double.isNaN(barrierSample.value)) {
                barrierSample.value = barrierNoise.compute(context);
            }
            barrier = barrierSample.value;
        }
        return 2.0D * (barrier + pressureShape);
    }

    private FluidStatus getAquiferStatus(long packedLocation) {
        int blockX = unpackBlockX(packedLocation);
        int blockY = unpackBlockY(packedLocation);
        int blockZ = unpackBlockZ(packedLocation);
        int index = getIndex(gridX(blockX), gridY(blockY), gridZ(blockZ));
        FluidStatus cached = aquiferCache[index];
        if (cached != null) {
            return cached;
        }
        FluidStatus computed = computeFluid(blockX, blockY, blockZ);
        aquiferCache[index] = computed;
        return computed;
    }

    private FluidStatus computeFluid(int blockX, int blockY, int blockZ) {
        FluidStatus globalStatus = globalFluidPicker.computeFluid(blockX, blockY, blockZ);
        int minimumSurfaceLevel = Integer.MAX_VALUE;
        int upperSampleY = blockY + 12;
        int lowerSampleY = blockY - 12;
        boolean hasFluidAtDirectSurface = false;

        for (int[] offset : SURFACE_SAMPLING_OFFSETS_IN_CHUNKS) {
            int sampleX = blockX + offset[0] * 16;
            int sampleZ = blockZ + offset[1] * 16;
            int surfaceLevel = preliminarySurfaceLevel(sampleX, sampleZ);
            int surfaceFluidY = surfaceLevel + 8;
            boolean directSample = offset[0] == 0 && offset[1] == 0;
            if (directSample && lowerSampleY > surfaceFluidY) {
                return globalStatus;
            }

            boolean sampleAboveSurface = upperSampleY > surfaceFluidY;
            if (sampleAboveSurface || directSample) {
                FluidStatus surfaceStatus = globalFluidPicker.computeFluid(sampleX,
                        surfaceFluidY, sampleZ);
                if (surfaceStatus.at(surfaceFluidY) != Material.AIR) {
                    if (directSample) {
                        hasFluidAtDirectSurface = true;
                    }
                    if (sampleAboveSurface) {
                        return surfaceStatus;
                    }
                }
            }
            minimumSurfaceLevel = Math.min(minimumSurfaceLevel, surfaceLevel);
        }

        int distanceToSurface = minimumSurfaceLevel + 8 - blockY;
        double surfacePressure = hasFluidAtDirectSurface
                ? clampedMap(distanceToSurface, 0.0D, 64.0D, 1.0D, 0.0D)
                : 0.0D;
        DensityFunction.FunctionContext context =
                new DensityFunction.SinglePointContext(blockX, blockY, blockZ);
        double floodedness = WorldgenMath.clamp(fluidLevelFloodednessNoise.compute(context),
                -1.0D, 1.0D);
        double floodedThreshold = map(surfacePressure, 1.0D, 0.0D, -0.3D, 0.8D);
        if (floodedness > floodedThreshold) {
            return globalStatus;
        }

        double dryThreshold = map(surfacePressure, 1.0D, 0.0D, -0.8D, 0.4D);
        if (floodedness <= dryThreshold) {
            return new FluidStatus(WAY_BELOW_MIN_Y, globalStatus.fluidType());
        }

        int spreadGridX = Math.floorDiv(blockX, 16);
        int spreadGridY = Math.floorDiv(blockY, 40);
        int spreadGridZ = Math.floorDiv(blockZ, 16);
        int middleY = spreadGridY * 40 + 20;
        double spread = fluidLevelSpreadNoise.compute(new DensityFunction.SinglePointContext(
                spreadGridX, spreadGridY, spreadGridZ)) * 10.0D;
        int quantizedSpread = quantize(spread, 3);
        int candidateFluidLevel = middleY + quantizedSpread;
        int fluidLevel = Math.min(minimumSurfaceLevel, candidateFluidLevel);
        if (candidateFluidLevel <= -10) {
            int lavaGridX = Math.floorDiv(blockX, 64);
            int lavaGridY = Math.floorDiv(blockY, 40);
            int lavaGridZ = Math.floorDiv(blockZ, 64);
            double lava = lavaNoise.compute(new DensityFunction.SinglePointContext(
                    lavaGridX, lavaGridY, lavaGridZ));
            if (Math.abs(lava) > 0.3D) {
                return new FluidStatus(fluidLevel, Material.LAVA);
            }
        }
        return new FluidStatus(fluidLevel, globalStatus.fluidType());
    }

    private int preliminarySurfaceLevel(int blockX, int blockZ) {
        // NoiseChunk keys this cache in quart coordinates and evaluates at the quart origin.
        int anchoredX = Math.floorDiv(blockX, 4) * 4;
        int anchoredZ = Math.floorDiv(blockZ, 4) * 4;
        return preliminarySurfaceLookup.preliminarySurfaceLevel(anchoredX, anchoredZ);
    }

    private int getIndex(int gridX, int gridY, int gridZ) {
        int localX = gridX - minGridX;
        int localY = gridY - minGridY;
        int localZ = gridZ - minGridZ;
        if (localX < 0 || localX >= gridSizeX || localY < 0 || localY >= gridSizeY
                || localZ < 0 || localZ >= gridSizeZ) {
            throw new IllegalArgumentException("Aquifer grid outside chunk cache: "
                    + gridX + "," + gridY + "," + gridZ);
        }
        return (localY * gridSizeZ + localZ) * gridSizeX + localX;
    }

    /** Exact 1.18.2 X/Z aquifer grid coordinate. */
    public static int gridX(int blockX) {
        return Math.floorDiv(blockX, X_SPACING);
    }

    /** Exact 1.18.2 Y aquifer grid coordinate. */
    public static int gridY(int blockY) {
        return Math.floorDiv(blockY, Y_SPACING);
    }

    /** Exact 1.18.2 X/Z aquifer grid coordinate. */
    public static int gridZ(int blockZ) {
        return Math.floorDiv(blockZ, Z_SPACING);
    }

    /** Voronoi-center similarity used for barrier blending and fluid tick scheduling. */
    public static double similarity(int firstDistanceSquared, int secondDistanceSquared) {
        return 1.0D - Math.abs(secondDistanceSquared - firstDistanceSquared) / 25.0D;
    }

    /**
     * Pure pressure-shape term before barrier noise is added and the result is doubled.
     * A water/lava boundary returns the sentinel value {@code 2.0}, which is also the final
     * official pressure for that special case.
     */
    public static double pressureShape(int blockY, FluidStatus first, FluidStatus second) {
        Material firstMaterial = first.at(blockY);
        Material secondMaterial = second.at(blockY);
        if (firstMaterial == Material.LAVA && secondMaterial == Material.WATER
                || firstMaterial == Material.WATER && secondMaterial == Material.LAVA) {
            return PRESSURE_LAVA_WATER;
        }
        int levelDifference = Math.abs(first.fluidLevel() - second.fluidLevel());
        if (levelDifference == 0) {
            return 0.0D;
        }
        double middleLevel = 0.5D * (first.fluidLevel() + second.fluidLevel());
        double distanceFromMiddle = blockY + 0.5D - middleLevel;
        double halfDifference = levelDifference / 2.0D;
        double distanceInsideLevels = halfDifference - Math.abs(distanceFromMiddle);
        double pressure;
        if (distanceFromMiddle > 0.0D) {
            pressure = distanceInsideLevels;
            pressure /= pressure > 0.0D ? 1.5D : 2.5D;
        } else {
            pressure = 3.0D + distanceInsideLevels;
            pressure /= pressure > 0.0D ? 3.0D : 10.0D;
        }
        return pressure;
    }

    /** Pure version of the ordinary pressure path for oracle and router integration tests. */
    public static double pressure(int blockY, FluidStatus first, FluidStatus second,
            double barrierNoise) {
        if (isWaterLavaBoundary(first.at(blockY), second.at(blockY))) {
            return PRESSURE_LAVA_WATER;
        }
        if (first.fluidLevel() == second.fluidLevel()) {
            return 0.0D;
        }
        double shape = pressureShape(blockY, first, second);
        double barrier = shape < -2.0D || shape > 2.0D ? 0.0D : barrierNoise;
        return 2.0D * (barrier + shape);
    }

    /** Deterministic block center selected for one aquifer grid cell. */
    public static AquiferCenter centerForGrid(PositionalRandomFactory factory,
            int gridX, int gridY, int gridZ) {
        requireNonNull(factory, "factory");
        RandomSource random = factory.at(gridX, gridY, gridZ);
        return new AquiferCenter(gridX * X_SPACING + random.nextInt(X_RANGE),
                gridY * Y_SPACING + random.nextInt(Y_RANGE),
                gridZ * Z_SPACING + random.nextInt(Z_RANGE));
    }

    /** Returns a defensive copy in the exact 1.18.2 iteration order. */
    public static int[][] surfaceSamplingOffsetsInChunks() {
        int[][] copy = new int[SURFACE_SAMPLING_OFFSETS_IN_CHUNKS.length][];
        for (int index = 0; index < copy.length; ++index) {
            copy[index] = SURFACE_SAMPLING_OFFSETS_IN_CHUNKS[index].clone();
        }
        return copy;
    }

    private static final double PRESSURE_LAVA_WATER = 2.0D;

    private static boolean isWaterLavaBoundary(Material first, Material second) {
        return first == Material.LAVA && second == Material.WATER
                || first == Material.WATER && second == Material.LAVA;
    }

    private static double clampedMap(double value, double fromLow, double fromHigh,
            double toLow, double toHigh) {
        double delta = (value - fromLow) / (fromHigh - fromLow);
        return WorldgenMath.clampedLerp(toLow, toHigh, delta);
    }

    private static double map(double value, double fromLow, double fromHigh,
            double toLow, double toHigh) {
        return WorldgenMath.lerp((value - fromLow) / (fromHigh - fromLow), toLow, toHigh);
    }

    private static int quantize(double value, int quantum) {
        return WorldgenMath.floor(value / quantum) * quantum;
    }

    private static long packBlockPosition(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38
                | ((long) z & 0x3FFFFFFL) << 12
                | ((long) y & 0xFFFL);
    }

    private static int unpackBlockX(long packed) {
        return (int) (packed >> 38);
    }

    private static int unpackBlockY(long packed) {
        return (int) (packed << 52 >> 52);
    }

    private static int unpackBlockZ(long packed) {
        return (int) (packed << 26 >> 38);
    }

    private static <T> T requireNonNull(T value, String name) {
        if (value == null) {
            throw new NullPointerException(name);
        }
        return value;
    }

    /** Material returned by the aquifer; solid is represented by {@link Result#isSolid()}. */
    public enum Material {
        AIR,
        WATER,
        LAVA
    }

    /** Global dimension fluid-level policy, normally lava below -54 and water below sea level. */
    public interface FluidPicker {
        FluidStatus computeFluid(int blockX, int blockY, int blockZ);
    }

    /** Callback for the router's preliminary surface scan, evaluated at four-block anchors. */
    public interface PreliminarySurfaceLookup {
        int preliminarySurfaceLevel(int blockX, int blockZ);
    }

    /** Immutable fluid surface and type. The fluid occupies every block strictly below the level. */
    public static final class FluidStatus {
        private final int fluidLevel;
        private final Material fluidType;

        public FluidStatus(int fluidLevel, Material fluidType) {
            this.fluidLevel = fluidLevel;
            this.fluidType = requireNonNull(fluidType, "fluidType");
        }

        public int fluidLevel() {
            return fluidLevel;
        }

        public Material fluidType() {
            return fluidType;
        }

        public Material at(int blockY) {
            return blockY < fluidLevel ? fluidType : Material.AIR;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof FluidStatus)) {
                return false;
            }
            FluidStatus other = (FluidStatus) object;
            return fluidLevel == other.fluidLevel && fluidType == other.fluidType;
        }

        @Override
        public int hashCode() {
            return 31 * fluidLevel + fluidType.hashCode();
        }

        @Override
        public String toString() {
            return "FluidStatus{" + fluidLevel + ", " + fluidType + '}';
        }
    }

    /** Per-block aquifer decision. */
    public static final class Result {
        private static final Result SOLID = new Result(null, false);
        private static final Result AIR_STABLE = new Result(Material.AIR, false);
        private static final Result AIR_SCHEDULED = new Result(Material.AIR, true);
        private static final Result WATER_STABLE = new Result(Material.WATER, false);
        private static final Result WATER_SCHEDULED = new Result(Material.WATER, true);
        private static final Result LAVA_STABLE = new Result(Material.LAVA, false);
        private static final Result LAVA_SCHEDULED = new Result(Material.LAVA, true);

        private final Material material;
        private final boolean scheduleFluidUpdate;

        private Result(Material material, boolean scheduleFluidUpdate) {
            this.material = material;
            this.scheduleFluidUpdate = scheduleFluidUpdate;
        }

        public static Result solid() {
            return SOLID;
        }

        public static Result material(Material material, boolean scheduleFluidUpdate) {
            switch (requireNonNull(material, "material")) {
                case AIR:
                    return scheduleFluidUpdate ? AIR_SCHEDULED : AIR_STABLE;
                case WATER:
                    return scheduleFluidUpdate ? WATER_SCHEDULED : WATER_STABLE;
                case LAVA:
                    return scheduleFluidUpdate ? LAVA_SCHEDULED : LAVA_STABLE;
                default:
                    throw new AssertionError(material);
            }
        }

        public boolean isSolid() {
            return material == null;
        }

        public Material material() {
            if (material == null) {
                throw new IllegalStateException("Solid aquifer result has no replacement material");
            }
            return material;
        }

        public boolean shouldScheduleFluidUpdate() {
            return scheduleFluidUpdate;
        }

        @Override
        public String toString() {
            return isSolid() ? "Result{SOLID}" : "Result{" + material + ", schedule="
                    + scheduleFluidUpdate + '}';
        }
    }

    /** Immutable public view of a seeded aquifer center. */
    public static final class AquiferCenter {
        private final int blockX;
        private final int blockY;
        private final int blockZ;

        private AquiferCenter(int blockX, int blockY, int blockZ) {
            this.blockX = blockX;
            this.blockY = blockY;
            this.blockZ = blockZ;
        }

        public int blockX() {
            return blockX;
        }

        public int blockY() {
            return blockY;
        }

        public int blockZ() {
            return blockZ;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof AquiferCenter)) {
                return false;
            }
            AquiferCenter other = (AquiferCenter) object;
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
            return "AquiferCenter{" + blockX + ", " + blockY + ", " + blockZ + '}';
        }
    }

    private static final class BarrierSample {
        private double value = Double.NaN;
    }
}

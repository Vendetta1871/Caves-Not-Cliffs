package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

/** Generates and caches complete Java 1.18.2 Overworld density columns. */
public final class V118TerrainColumnGenerator {
    public static final int SEA_LEVEL = 63;
    public static final int LAVA_LEVEL = -54;
    /** System property selecting how many worker threads fill one column's density cells. */
    public static final String THREADS_PROPERTY = "cavesnotcliffs.terrainThreads";
    private static final int MAX_PARALLELISM = 4;
    private static final int CONFIGURED_PARALLELISM = Math.max(1, Math.min(MAX_PARALLELISM,
        Integer.getInteger(THREADS_PROPERTY,
            Math.max(1, Runtime.getRuntime().availableProcessors() / 2))));
    private static final ForkJoinPool CELL_POOL = CONFIGURED_PARALLELISM > 1
        ? new ForkJoinPool(CONFIGURED_PARALLELISM, new DaemonThreadFactory(), null, false)
        : null;
    private static final NoiseBasedAquifer.FluidStatus GLOBAL_LAVA =
        new NoiseBasedAquifer.FluidStatus(LAVA_LEVEL, NoiseBasedAquifer.Material.LAVA);
    private static final NoiseBasedAquifer.FluidStatus GLOBAL_WATER =
        new NoiseBasedAquifer.FluidStatus(SEA_LEVEL, NoiseBasedAquifer.Material.WATER);

    private final long seed;
    private final V118NoiseSettings settings;
    private final V118NoiseRouter router;
    private final DensityFunction preliminarySurfaceDensity;
    private final V118ClimateSampler climateSampler;
    private final V118BiomeManager biomeManager;
    private final OverworldBiomeBuilder biomeTable;
    private final V118SurfaceSystem surfaceSystem;
    private final boolean applySurfaceRules;
    private final boolean applyCarvers;
    private final TerrainColumnCache cache;
    private final int parallelism;
    private final MutableDensityContext blockContext = new MutableDensityContext();

    public V118TerrainColumnGenerator(long seed, V118NoiseRouterData.Profile profile) {
        this(seed, profile, true, true);
    }

    V118TerrainColumnGenerator(long seed, V118NoiseRouterData.Profile profile,
            boolean applySurfaceRules) {
        this(seed, profile, applySurfaceRules, false);
    }

    V118TerrainColumnGenerator(long seed, V118NoiseRouterData.Profile profile,
            boolean applySurfaceRules, boolean applyCarvers) {
        this(seed, profile, applySurfaceRules, applyCarvers, CONFIGURED_PARALLELISM);
    }

    /** Test hook: pins the number of cell-fill workers regardless of the system property. */
    V118TerrainColumnGenerator(long seed, V118NoiseRouterData.Profile profile,
            boolean applySurfaceRules, boolean applyCarvers, int parallelism) {
        if (applyCarvers && !applySurfaceRules) {
            throw new IllegalArgumentException("Native carvers require the post-surface seam");
        }
        if (parallelism <= 0) {
            throw new IllegalArgumentException("parallelism must be positive: " + parallelism);
        }
        this.seed = seed;
        this.applySurfaceRules = applySurfaceRules;
        this.applyCarvers = applyCarvers;
        this.parallelism = parallelism;
        settings = V118NoiseSettings.overworld(profile.amplified());
        router = V118NoiseRouterData.create(seed, profile);
        preliminarySurfaceDensity = V118DensityInterpolator.realize(
            router.initialDensityWithoutJaggedness(), settings);
        // Built once per generator: the builder's RTree index is expensive to construct and
        // read-only afterwards, so every parallel lane resolves against this shared instance.
        biomeTable = new OverworldBiomeBuilder();
        climateSampler = new V118ClimateSampler(router, settings, biomeTable);
        biomeManager = new V118BiomeManager(climateSampler::resolveQuart, seed);
        surfaceSystem = new V118SurfaceSystem(seed);
        cache = new TerrainColumnCache(this::generateUncached);
    }

    public TerrainColumn column(int columnX, int columnZ) {
        return cache.get(columnX, columnZ);
    }

    public TerrainColumnCache cache() {
        return cache;
    }

    /** Resolves the exact block-space biome after 1.18.2's Voronoi zoom. */
    public V118Biome biomeAt(int blockX, int blockY, int blockZ) {
        return biomeManager.getBiome(blockX, blockY, blockZ);
    }

    private TerrainColumn generateUncached(int columnX, int columnZ) {
        TerrainColumn.Builder builder = TerrainColumn.builder(columnX, columnZ);
        CellFillGroup[] groups = createFillGroups(columnX, columnZ);

        V118PreliminarySurface preliminarySurface = V118PreliminarySurface.fromRealizedDensity(
            settings, preliminarySurfaceDensity);
        NoiseBasedAquifer aquifer = new NoiseBasedAquifer(columnX, columnZ,
            settings.minY(), settings.height(), router.barrierNoise(),
            router.fluidLevelFloodednessNoise(), router.fluidLevelSpreadNoise(),
            router.lavaNoise(), router.aquiferPositionalRandomFactory(), preliminarySurface,
            V118TerrainColumnGenerator::globalFluid);
        int[] highestNonAir = new int[TerrainColumn.SURFACE_BIOME_COUNT];
        java.util.Arrays.fill(highestNonAir, TerrainColumn.MIN_Y);
        int minBlockX = columnX * TerrainColumn.WIDTH;
        int minBlockZ = columnZ * TerrainColumn.WIDTH;

        int horizontalCellCount = TerrainColumn.WIDTH / settings.getCellWidth();
        int quartsPerGroup = TerrainColumn.QUART_WIDTH / groups.length;
        if (groups.length == 1) {
            fillVirtualBiomes(groups[0], builder, columnX, columnZ, 0,
                TerrainColumn.QUART_WIDTH);
            fillCells(groups[0], builder, highestNonAir, minBlockX, minBlockZ,
                0, horizontalCellCount, 0, horizontalCellCount);
        } else {
            // The caller runs group 0 itself: blocking on join while every worker is busy
            // would leave one hardware thread idle for the whole column.
            int groupAxisX = groups.length >= 4 ? 2 : groups.length;
            int groupAxisZ = groups.length / groupAxisX;
            int cellsPerGroupX = horizontalCellCount / groupAxisX;
            int cellsPerGroupZ = horizontalCellCount / groupAxisZ;
            java.util.concurrent.ForkJoinTask<?> task = CELL_POOL.submit(
                () -> java.util.stream.IntStream.range(1, groups.length)
                    .parallel()
                    .forEach(index -> {
                        fillVirtualBiomes(groups[index], builder, columnX, columnZ,
                            index * quartsPerGroup, (index + 1) * quartsPerGroup);
                        fillCells(groups[index], builder, highestNonAir, minBlockX, minBlockZ,
                            (index % groupAxisX) * cellsPerGroupX,
                            (index % groupAxisX + 1) * cellsPerGroupX,
                            (index / groupAxisX) * cellsPerGroupZ,
                            (index / groupAxisX + 1) * cellsPerGroupZ);
                    }));
            fillVirtualBiomes(groups[0], builder, columnX, columnZ, 0, quartsPerGroup);
            fillCells(groups[0], builder, highestNonAir, minBlockX, minBlockZ,
                0, cellsPerGroupX, 0, cellsPerGroupZ);
            task.join();
        }
        for (CellFillGroup group : groups) {
            builder.orScheduledFluidUpdates(group.fluidBits);
        }
        // Parallel fillers race the max-material bookkeeping; recompute it from the array.
        builder.recomputeMaxMaterialId();

        MutableSurfaceAccess surfaceAccess = null;
        if (applySurfaceRules || applyCarvers) {
            surfaceAccess = new MutableSurfaceAccess(builder, preliminarySurface, aquifer,
                highestNonAir, columnX, columnZ, minBlockX, minBlockZ);
        }
        if (applySurfaceRules) {
            surfaceSystem.buildSurface(surfaceAccess, columnX, columnZ);
        }
        if (applyCarvers) {
            V118OverworldCarvers.carve(seed, columnX, columnZ, surfaceAccess);
        }

        for (int localZ = 0; localZ < TerrainColumn.WIDTH; ++localZ) {
            int blockZ = minBlockZ + localZ;
            for (int localX = 0; localX < TerrainColumn.WIDTH; ++localX) {
                int blockX = minBlockX + localX;
                int surfaceY = highestNonAir[localZ * TerrainColumn.WIDTH + localX];
                builder.setSurfaceBiomeId(localX, localZ,
                    biomeManager.getBiome(blockX, surfaceY, blockZ).ordinal());
            }
        }
        return builder.build();
    }

    /** One cell-fill worker context per parallel lane, or the only context when serial. */
    private CellFillGroup[] createFillGroups(int columnX, int columnZ) {
        int count = CELL_POOL == null ? 1 : Math.min(parallelism, MAX_PARALLELISM);
        CellFillGroup[] groups = new CellFillGroup[count];
        for (int index = 0; index < count; ++index) {
            groups[index] = new CellFillGroup(columnX, columnZ);
        }
        return groups;
    }

    /**
     * Fills a rectangular band of density cells. The per-group density/aquifer/vein state is
     * privately owned: the realized density caches, the aquifer scratch buffers and the mutable
     * function context are all single-threaded by design, so sharing them across lanes would
     * corrupt the terrain. Band-disjoint builder slots and the per-group fluid bitset are the
     * only writes, and both merge back deterministically.
     */
    private void fillCells(CellFillGroup group, TerrainColumn.Builder builder,
            int[] highestNonAir, int minBlockX, int minBlockZ,
            int cellXFrom, int cellXTo, int cellZFrom, int cellZTo) {
        int cellWidth = settings.getCellWidth();
        int cellHeight = settings.getCellHeight();
        int verticalCellCount = settings.getCellCountY();
        // Keep every block in one density cell contiguous, matching NoiseChunk's official
        // traversal. Besides preserving the mutable marker-cache contract, this makes each
        // interpolated node evaluate its eight corners once per 4x8x4 cell instead of once per
        // X row. The resulting material array remains Y-major through Builder's index mapping.
        for (int cellX = cellXFrom; cellX < cellXTo; ++cellX) {
            for (int cellZ = cellZFrom; cellZ < cellZTo; ++cellZ) {
                for (int cellY = verticalCellCount - 1; cellY >= 0; --cellY) {
                    int cellMinY = TerrainColumn.MIN_Y + cellY * cellHeight;
                    for (int offsetY = cellHeight - 1; offsetY >= 0; --offsetY) {
                        int worldY = cellMinY + offsetY;
                        for (int offsetX = 0; offsetX < cellWidth; ++offsetX) {
                            int localX = cellX * cellWidth + offsetX;
                            int blockX = minBlockX + localX;
                            for (int offsetZ = 0; offsetZ < cellWidth; ++offsetZ) {
                                int localZ = cellZ * cellWidth + offsetZ;
                                int blockZ = minBlockZ + localZ;
                                placeMaterial(group, builder, highestNonAir, localX, worldY,
                                    localZ, blockX, blockZ);
                            }
                        }
                    }
                }
            }
        }
    }

    private void placeMaterial(CellFillGroup group, TerrainColumn.Builder builder,
            int[] highestNonAir, int localX, int worldY, int localZ, int blockX, int blockZ) {
        DensityFunction.FunctionContext context = group.blockContext.set(blockX, worldY, blockZ);
        double density = group.finalDensity.compute(context);
        NoiseBasedAquifer.Result aquiferResult =
            group.aquifer.compute(context, density);
        V118Material material;
        if (aquiferResult.isSolid()) {
            V118Material vein = group.oreVeinifier.compute(context);
            material = vein == null ? V118Material.STONE : vein;
        } else {
            material = material(aquiferResult.material());
            if ((material == V118Material.WATER || material == V118Material.LAVA)
                    && aquiferResult.shouldScheduleFluidUpdate()) {
                int index = (worldY - TerrainColumn.MIN_Y)
                    * TerrainColumn.WIDTH * TerrainColumn.WIDTH
                    + localZ * TerrainColumn.WIDTH + localX;
                group.fluidBits[index >>> 6] |= 1L << (index & 63);
            }
        }
        builder.setMaterialId(localX, worldY, localZ, material.storageId());
        if (material != V118Material.AIR) {
            int surfaceIndex = localZ * TerrainColumn.WIDTH + localX;
            highestNonAir[surfaceIndex] = Math.max(highestNonAir[surfaceIndex], worldY);
        }
    }

    private void fillVirtualBiomes(CellFillGroup group, TerrainColumn.Builder builder,
            int columnX, int columnZ, int quartXFrom, int quartXTo) {
        int minQuartX = columnX * TerrainColumn.QUART_WIDTH;
        int minQuartZ = columnZ * TerrainColumn.QUART_WIDTH;
        // Keep each vertical pair in one 4x8x4 density cell before moving horizontally.
        for (int localQuartX = quartXFrom; localQuartX < quartXTo; ++localQuartX) {
            for (int localQuartZ = 0; localQuartZ < TerrainColumn.QUART_WIDTH; ++localQuartZ) {
                for (int quartY = TerrainColumn.MIN_QUART_Y;
                        quartY <= TerrainColumn.MAX_QUART_Y; ++quartY) {
                    V118Biome biome = group.climateSampler.resolveQuart(minQuartX + localQuartX,
                        quartY, minQuartZ + localQuartZ);
                    builder.setVirtualBiomeIdAtQuart(localQuartX, quartY, localQuartZ,
                        biome.ordinal());
                }
            }
        }
    }

    private static NoiseBasedAquifer.FluidStatus globalFluid(int blockX, int blockY,
            int blockZ) {
        if (blockY < Math.min(LAVA_LEVEL, SEA_LEVEL)) {
            return GLOBAL_LAVA;
        }
        return GLOBAL_WATER;
    }

    private static V118Material material(NoiseBasedAquifer.Material material) {
        switch (material) {
            case AIR:
                return V118Material.AIR;
            case WATER:
                return V118Material.WATER;
            case LAVA:
                return V118Material.LAVA;
            default:
                throw new AssertionError(material);
        }
    }

    /**
     * Per-lane realized noise state. Realization wraps only the shared immutable noise graph in
     * fresh cache nodes, so a group's columns are bit-identical to the serial evaluation; the
     * memoized corners/2D values simply get recomputed per lane instead of once per column.
     */
    private final class CellFillGroup {
        private final DensityFunction finalDensity;
        private final NoiseBasedAquifer aquifer;
        private final V118OreVeinifier oreVeinifier;
        private final V118ClimateSampler climateSampler;
        private final MutableDensityContext blockContext = new MutableDensityContext();
        private final long[] fluidBits = new long[(TerrainColumn.BLOCK_COUNT + 63) >>> 6];

        private CellFillGroup(int columnX, int columnZ) {
            finalDensity = V118DensityInterpolator.realizeFinalDensity(router.finalDensity(),
                settings);
            aquifer = new NoiseBasedAquifer(columnX, columnZ,
                settings.minY(), settings.height(), router.barrierNoise(),
                router.fluidLevelFloodednessNoise(), router.fluidLevelSpreadNoise(),
                router.lavaNoise(), router.aquiferPositionalRandomFactory(),
                V118PreliminarySurface.fromRealizedDensity(settings,
                    V118DensityInterpolator.realize(router.initialDensityWithoutJaggedness(),
                        settings)),
                V118TerrainColumnGenerator::globalFluid);
            oreVeinifier = new V118OreVeinifier(
                V118DensityInterpolator.realize(router.veinToggle(), settings),
                V118DensityInterpolator.realize(router.veinRidged(), settings),
                V118DensityInterpolator.realize(router.veinGap(), settings),
                router.oreVeinsPositionalRandomFactory());
            climateSampler = new V118ClimateSampler(router, settings, biomeTable);
        }
    }

    private static final class DaemonThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
        private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

        @Override
        public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory
                .newThread(pool);
            thread.setName("cnc-terrain-cell-" + THREAD_COUNTER.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }

    private final class MutableSurfaceAccess implements V118SurfaceSystem.SurfaceAccess,
            V118WorldCarver.WorldAccess {
        private final TerrainColumn.Builder builder;
        private final V118PreliminarySurface preliminarySurface;
        private final NoiseBasedAquifer aquifer;
        private final int[] highestNonAir;
        private final int columnX;
        private final int columnZ;
        private final int minBlockX;
        private final int minBlockZ;
        private boolean lastAquiferShouldScheduleFluidUpdate;

        private MutableSurfaceAccess(TerrainColumn.Builder builder,
                V118PreliminarySurface preliminarySurface, NoiseBasedAquifer aquifer,
                int[] highestNonAir, int columnX, int columnZ, int minBlockX, int minBlockZ) {
            this.builder = builder;
            this.preliminarySurface = preliminarySurface;
            this.aquifer = aquifer;
            this.highestNonAir = highestNonAir;
            this.columnX = columnX;
            this.columnZ = columnZ;
            this.minBlockX = minBlockX;
            this.minBlockZ = minBlockZ;
        }

        @Override
        public int targetChunkX() {
            return columnX;
        }

        @Override
        public int targetChunkZ() {
            return columnZ;
        }

        @Override
        public int minBuildHeight() {
            return TerrainColumn.MIN_Y;
        }

        @Override
        public int maxBuildHeight() {
            return TerrainColumn.MAX_Y_EXCLUSIVE;
        }

        @Override
        public V118Material getBlock(int blockX, int blockY, int blockZ) {
            int localX = local(blockX, minBlockX, "blockX");
            int localZ = local(blockZ, minBlockZ, "blockZ");
            if (blockY < TerrainColumn.MIN_Y || blockY > TerrainColumn.MAX_Y) {
                return V118Material.AIR;
            }
            return V118Material.fromStorageId(builder.materialId(localX, blockY, localZ));
        }

        @Override
        public V118Material getMaterial(int blockX, int blockY, int blockZ) {
            return getBlock(blockX, blockY, blockZ);
        }

        @Override
        public void setBlock(int blockX, int blockY, int blockZ, V118Material material) {
            if (material == null) {
                throw new NullPointerException("material");
            }
            int localX = local(blockX, minBlockX, "blockX");
            int localZ = local(blockZ, minBlockZ, "blockZ");
            if (blockY < TerrainColumn.MIN_Y || blockY > TerrainColumn.MAX_Y) {
                return;
            }
            V118Material previous = V118Material.fromStorageId(
                builder.materialId(localX, blockY, localZ));
            builder.setMaterialId(localX, blockY, localZ, material.storageId());
            if (material != V118Material.WATER && material != V118Material.LAVA) {
                builder.setScheduledFluidUpdate(localX, blockY, localZ, false);
            }
            int heightIndex = localZ * TerrainColumn.WIDTH + localX;
            if (material != V118Material.AIR) {
                highestNonAir[heightIndex] = Math.max(highestNonAir[heightIndex], blockY);
            } else if (previous != V118Material.AIR
                    && highestNonAir[heightIndex] == blockY) {
                highestNonAir[heightIndex] = findHighestNonAir(localX, localZ, blockY - 1);
            }
        }

        @Override
        public void setMaterial(int blockX, int blockY, int blockZ, V118Material material,
                boolean scheduleFluidUpdate) {
            setBlock(blockX, blockY, blockZ, material);
            int localX = local(blockX, minBlockX, "blockX");
            int localZ = local(blockZ, minBlockZ, "blockZ");
            builder.setScheduledFluidUpdate(localX, blockY, localZ, scheduleFluidUpdate);
        }

        @Override
        public V118Material computeAquiferMaterial(int blockX, int blockY, int blockZ) {
            NoiseBasedAquifer.Result result = aquifer.compute(
                blockContext.set(blockX, blockY, blockZ), 0.0D);
            lastAquiferShouldScheduleFluidUpdate = result.shouldScheduleFluidUpdate();
            return result.isSolid() ? null : material(result.material());
        }

        @Override
        public boolean shouldScheduleAquiferFluidUpdate() {
            return lastAquiferShouldScheduleFluidUpdate;
        }

        @Override
        public V118Material topMaterial(int blockX, int blockY, int blockZ,
                boolean hasFluidAbove) {
            return surfaceSystem.topMaterial(this, blockX, blockY, blockZ, hasFluidAbove);
        }

        @Override
        public int worldSurfaceHeight(int blockX, int blockZ) {
            int localX = local(blockX, minBlockX, "blockX");
            int localZ = local(blockZ, minBlockZ, "blockZ");
            return highestNonAir[localZ * TerrainColumn.WIDTH + localX];
        }

        @Override
        public int preliminarySurfaceLevel(int blockX, int blockZ) {
            return preliminarySurface.preliminarySurfaceLevel(blockX, blockZ);
        }

        @Override
        public V118Biome biomeAt(int blockX, int blockY, int blockZ) {
            return biomeManager.getBiome(blockX, blockY, blockZ);
        }

        @Override
        public boolean coldEnoughToSnow(V118Biome biome, int blockX, int blockY, int blockZ) {
            return V118BiomeTemperature.coldEnoughToSnow(biome, blockX, blockY, blockZ);
        }

        @Override
        public boolean shouldMeltFrozenOceanIcebergSlightly(V118Biome biome, int blockX,
                int blockY, int blockZ) {
            return V118BiomeTemperature.shouldMeltFrozenOceanIcebergSlightly(biome, blockX,
                blockY, blockZ);
        }

        private int findHighestNonAir(int localX, int localZ, int startY) {
            for (int blockY = startY; blockY >= TerrainColumn.MIN_Y; --blockY) {
                V118Material material = V118Material.fromStorageId(
                    builder.materialId(localX, blockY, localZ));
                if (material != V118Material.AIR) {
                    return blockY;
                }
            }
            return TerrainColumn.MIN_Y;
        }

        private int local(int blockCoordinate, int minimum, String name) {
            int coordinate = blockCoordinate - minimum;
            if (coordinate < 0 || coordinate >= TerrainColumn.WIDTH) {
                throw new IllegalArgumentException(name + " is outside generated column: "
                    + blockCoordinate);
            }
            return coordinate;
        }
    }
}

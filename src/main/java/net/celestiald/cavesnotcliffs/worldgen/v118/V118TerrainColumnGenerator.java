package net.celestiald.cavesnotcliffs.worldgen.v118;

/** Generates and caches complete Java 1.18.2 Overworld density columns. */
public final class V118TerrainColumnGenerator {
    public static final int SEA_LEVEL = 63;
    public static final int LAVA_LEVEL = -54;
    private static final NoiseBasedAquifer.FluidStatus GLOBAL_LAVA =
        new NoiseBasedAquifer.FluidStatus(LAVA_LEVEL, NoiseBasedAquifer.Material.LAVA);
    private static final NoiseBasedAquifer.FluidStatus GLOBAL_WATER =
        new NoiseBasedAquifer.FluidStatus(SEA_LEVEL, NoiseBasedAquifer.Material.WATER);

    private final long seed;
    private final V118NoiseSettings settings;
    private final V118NoiseRouter router;
    private final DensityFunction finalDensity;
    private final V118ClimateSampler climateSampler;
    private final V118BiomeManager biomeManager;
    private final V118OreVeinifier oreVeinifier;
    private final V118SurfaceSystem surfaceSystem;
    private final boolean applySurfaceRules;
    private final boolean applyCarvers;
    private final TerrainColumnCache cache;

    public V118TerrainColumnGenerator(long seed, V118NoiseRouterData.Profile profile) {
        this(seed, profile, true, true);
    }

    V118TerrainColumnGenerator(long seed, V118NoiseRouterData.Profile profile,
            boolean applySurfaceRules) {
        this(seed, profile, applySurfaceRules, false);
    }

    V118TerrainColumnGenerator(long seed, V118NoiseRouterData.Profile profile,
            boolean applySurfaceRules, boolean applyCarvers) {
        if (applyCarvers && !applySurfaceRules) {
            throw new IllegalArgumentException("Native carvers require the post-surface seam");
        }
        this.seed = seed;
        this.applySurfaceRules = applySurfaceRules;
        this.applyCarvers = applyCarvers;
        settings = V118NoiseSettings.overworld(profile.amplified());
        router = V118NoiseRouterData.create(seed, profile);
        finalDensity = V118DensityInterpolator.realizeFinalDensity(router.finalDensity(),
            settings);
        OverworldBiomeBuilder biomeTable = new OverworldBiomeBuilder();
        climateSampler = new V118ClimateSampler(router, settings, biomeTable);
        biomeManager = new V118BiomeManager(climateSampler::resolveQuart, seed);
        oreVeinifier = new V118OreVeinifier(
            V118DensityInterpolator.realize(router.veinToggle(), settings),
            V118DensityInterpolator.realize(router.veinRidged(), settings),
            V118DensityInterpolator.realize(router.veinGap(), settings),
            router.oreVeinsPositionalRandomFactory());
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
        fillVirtualBiomes(builder, columnX, columnZ);

        V118PreliminarySurface preliminarySurface = new V118PreliminarySurface(settings,
            router.initialDensityWithoutJaggedness());
        NoiseBasedAquifer aquifer = new NoiseBasedAquifer(columnX, columnZ,
            settings.minY(), settings.height(), router.barrierNoise(),
            router.fluidLevelFloodednessNoise(), router.fluidLevelSpreadNoise(),
            router.lavaNoise(), router.aquiferPositionalRandomFactory(), preliminarySurface,
            V118TerrainColumnGenerator::globalFluid);
        int[] highestNonAir = new int[TerrainColumn.SURFACE_BIOME_COUNT];
        java.util.Arrays.fill(highestNonAir, TerrainColumn.MIN_Y);
        int minBlockX = columnX * TerrainColumn.WIDTH;
        int minBlockZ = columnZ * TerrainColumn.WIDTH;

        // Keep every block in one density cell contiguous, matching NoiseChunk's official
        // traversal. Besides preserving the mutable marker-cache contract, this makes each
        // interpolated node evaluate its eight corners once per 4x8x4 cell instead of once per
        // X row. The resulting material array remains Y-major through Builder's index mapping.
        int cellWidth = settings.getCellWidth();
        int cellHeight = settings.getCellHeight();
        int horizontalCellCount = TerrainColumn.WIDTH / cellWidth;
        int verticalCellCount = settings.getCellCountY();
        for (int cellX = 0; cellX < horizontalCellCount; ++cellX) {
            for (int cellZ = 0; cellZ < horizontalCellCount; ++cellZ) {
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
                                placeMaterial(builder, aquifer, highestNonAir, localX, worldY,
                                    localZ, blockX, blockZ);
                            }
                        }
                    }
                }
            }
        }

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

    private void placeMaterial(TerrainColumn.Builder builder, NoiseBasedAquifer aquifer,
            int[] highestNonAir, int localX, int worldY, int localZ, int blockX, int blockZ) {
        double density = finalDensity.compute(blockX, worldY, blockZ);
        NoiseBasedAquifer.Result aquiferResult =
            aquifer.compute(blockX, worldY, blockZ, density);
        V118Material material;
        if (aquiferResult.isSolid()) {
            V118Material vein = oreVeinifier.compute(blockX, worldY, blockZ);
            material = vein == null ? V118Material.STONE : vein;
        } else {
            material = material(aquiferResult.material());
            if ((material == V118Material.WATER || material == V118Material.LAVA)
                    && aquiferResult.shouldScheduleFluidUpdate()) {
                builder.setScheduledFluidUpdate(localX, worldY, localZ, true);
            }
        }
        builder.setMaterialId(localX, worldY, localZ, material.storageId());
        if (material != V118Material.AIR) {
            int surfaceIndex = localZ * TerrainColumn.WIDTH + localX;
            highestNonAir[surfaceIndex] = Math.max(highestNonAir[surfaceIndex], worldY);
        }
    }

    private void fillVirtualBiomes(TerrainColumn.Builder builder, int columnX, int columnZ) {
        int minQuartX = columnX * TerrainColumn.QUART_WIDTH;
        int minQuartZ = columnZ * TerrainColumn.QUART_WIDTH;
        // Keep each vertical pair in one 4x8x4 density cell before moving horizontally.
        for (int localQuartX = 0; localQuartX < TerrainColumn.QUART_WIDTH; ++localQuartX) {
            for (int localQuartZ = 0; localQuartZ < TerrainColumn.QUART_WIDTH; ++localQuartZ) {
                for (int quartY = TerrainColumn.MIN_QUART_Y;
                        quartY <= TerrainColumn.MAX_QUART_Y; ++quartY) {
                    V118Biome biome = climateSampler.resolveQuart(minQuartX + localQuartX,
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
            NoiseBasedAquifer.Result result = aquifer.compute(blockX, blockY, blockZ, 0.0D);
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

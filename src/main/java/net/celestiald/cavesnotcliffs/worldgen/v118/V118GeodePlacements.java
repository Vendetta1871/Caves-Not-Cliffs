package net.celestiald.cavesnotcliffs.worldgen.v118;

/** Exact Java 1.18.2 placed-feature wrapper for the Overworld amethyst geode. */
public final class V118GeodePlacements {
    public static final int LOCAL_MODIFICATIONS_STEP = 2;
    public static final int GLOBAL_FEATURE_INDEX = 2;
    public static final int AVERAGE_CHUNKS_PER_GEODE = 24;
    public static final int MIN_Y = -58;
    public static final int MAX_Y = 30;

    private V118GeodePlacements() {
    }

    public static PlacementResult decorate(V118GeodeFeature.WorldAccess world, long worldSeed,
            int chunkX, int chunkZ) {
        if (world == null) {
            throw new NullPointerException("world");
        }
        V118WorldgenRandom random = new V118WorldgenRandom(0L);
        long decorationSeed = random.setDecorationSeed(worldSeed, chunkX << 4, chunkZ << 4);
        random.setFeatureSeed(decorationSeed, GLOBAL_FEATURE_INDEX, LOCAL_MODIFICATIONS_STEP);
        if (!(random.nextFloat() < 1.0F / AVERAGE_CHUNKS_PER_GEODE)) {
            return PlacementResult.skipped(decorationSeed);
        }
        int originX = (chunkX << 4) + random.nextInt(16);
        int originZ = (chunkZ << 4) + random.nextInt(16);
        int originY = random.nextInt(MAX_Y - MIN_Y + 1) + MIN_Y;
        boolean placed = V118GeodeFeature.place(world, random, worldSeed,
            originX, originY, originZ);
        return new PlacementResult(decorationSeed, true, placed, originX, originY, originZ);
    }

    public static final class PlacementResult {
        private final long decorationSeed;
        private final boolean attempted;
        private final boolean placed;
        private final int originX;
        private final int originY;
        private final int originZ;

        private PlacementResult(long decorationSeed, boolean attempted, boolean placed,
                int originX, int originY, int originZ) {
            this.decorationSeed = decorationSeed;
            this.attempted = attempted;
            this.placed = placed;
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
        }

        private static PlacementResult skipped(long decorationSeed) {
            return new PlacementResult(decorationSeed, false, false, 0, 0, 0);
        }

        public long decorationSeed() {
            return decorationSeed;
        }

        public boolean attempted() {
            return attempted;
        }

        public boolean placed() {
            return placed;
        }

        public int originX() {
            return originX;
        }

        public int originY() {
            return originY;
        }

        public int originZ() {
            return originZ;
        }
    }
}

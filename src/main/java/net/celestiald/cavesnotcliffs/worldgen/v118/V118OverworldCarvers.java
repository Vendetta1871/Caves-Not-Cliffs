package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Official Java 1.18.2 Overworld carver catalog and source-chunk scheduling loop. */
public final class V118OverworldCarvers {
    public static final int SOURCE_CHUNK_RADIUS = 8;
    public static final int SOURCE_CHUNK_DIAMETER = SOURCE_CHUNK_RADIUS * 2 + 1;
    public static final int SOURCE_CHUNK_COUNT = SOURCE_CHUNK_DIAMETER * SOURCE_CHUNK_DIAMETER;
    public static final int LAVA_LEVEL = TerrainColumn.MIN_Y + 8;

    private static final V118CaveWorldCarver CAVE_CARVER = new V118CaveWorldCarver();
    private static final V118CanyonWorldCarver CANYON_CARVER = new V118CanyonWorldCarver();

    public static final Entry CAVE = new Entry("cave", 0, CAVE_CARVER,
        new V118CaveWorldCarver.Configuration(0.15F, TerrainColumn.MIN_Y + 8, 180,
            0.1F, 0.9F, LAVA_LEVEL, 0.7F, 1.4F, 0.8F, 1.3F, -1.0F, -0.4F));
    public static final Entry CAVE_EXTRA_UNDERGROUND = new Entry("cave_extra_underground", 1,
        CAVE_CARVER,
        new V118CaveWorldCarver.Configuration(0.07F, TerrainColumn.MIN_Y + 8, 47,
            0.1F, 0.9F, LAVA_LEVEL, 0.7F, 1.4F, 0.8F, 1.3F, -1.0F, -0.4F));
    public static final Entry CANYON = new Entry("canyon", 2, CANYON_CARVER,
        new V118CanyonWorldCarver.Configuration(0.01F, 10, 67,
            3.0F, 3.0F, LAVA_LEVEL, -0.125F, 0.125F,
            0.75F, 1.0F, 0.0F, 6.0F, 2.0F, 3,
            0.75F, 1.0F, 1.0F, 0.0F));

    private static final List<Entry> CATALOG;

    static {
        List<Entry> catalog = new ArrayList<Entry>();
        catalog.add(CAVE);
        catalog.add(CAVE_EXTRA_UNDERGROUND);
        catalog.add(CANYON);
        CATALOG = Collections.unmodifiableList(catalog);
    }

    private V118OverworldCarvers() {
    }

    public static List<Entry> catalog() {
        return CATALOG;
    }

    public static Result carve(long worldSeed, int targetChunkX, int targetChunkZ,
            V118WorldCarver.WorldAccess world) {
        if (world == null) {
            throw new NullPointerException("world");
        }
        if (world.targetChunkX() != targetChunkX || world.targetChunkZ() != targetChunkZ) {
            throw new IllegalArgumentException("World access target " + world.targetChunkX()
                + ',' + world.targetChunkZ() + " does not match " + targetChunkX + ','
                + targetChunkZ);
        }
        Random random = new Random(0L);
        V118CarvingMask mask = new V118CarvingMask(
            world.maxBuildHeight() - world.minBuildHeight(), world.minBuildHeight());
        List<Start> starts = new ArrayList<Start>();
        for (int offsetX = -SOURCE_CHUNK_RADIUS; offsetX <= SOURCE_CHUNK_RADIUS; ++offsetX) {
            for (int offsetZ = -SOURCE_CHUNK_RADIUS; offsetZ <= SOURCE_CHUNK_RADIUS; ++offsetZ) {
                int sourceChunkX = targetChunkX + offsetX;
                int sourceChunkZ = targetChunkZ + offsetZ;
                for (Entry entry : CATALOG) {
                    long seed = setLargeFeatureSeed(random, worldSeed + entry.index,
                        sourceChunkX, sourceChunkZ);
                    if (entry.isStartChunk(random)) {
                        starts.add(new Start(entry.id, entry.index, sourceChunkX, sourceChunkZ,
                            seed));
                        entry.carve(world, random, sourceChunkX, sourceChunkZ, mask);
                    }
                }
            }
        }
        return new Result(starts, mask);
    }

    /** Java 1.18.2 {@code WorldgenRandom#setLargeFeatureSeed}. */
    public static long setLargeFeatureSeed(Random random, long worldSeed, int chunkX,
            int chunkZ) {
        random.setSeed(worldSeed);
        long xMultiplier = random.nextLong();
        long zMultiplier = random.nextLong();
        long featureSeed = (long) chunkX * xMultiplier ^ (long) chunkZ * zMultiplier
            ^ worldSeed;
        random.setSeed(featureSeed);
        return featureSeed;
    }

    public static final class Entry {
        private final String id;
        private final int index;
        private final V118WorldCarver<?> carver;
        private final V118CarverConfiguration configuration;

        private Entry(String id, int index, V118WorldCarver<?> carver,
                V118CarverConfiguration configuration) {
            this.id = id;
            this.index = index;
            this.carver = carver;
            this.configuration = configuration;
        }

        public String id() {
            return id;
        }

        public int index() {
            return index;
        }

        public V118CarverConfiguration configuration() {
            return configuration;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private boolean isStartChunk(Random random) {
            return ((V118WorldCarver) carver).isStartChunk(configuration, random);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private void carve(V118WorldCarver.WorldAccess world, Random random,
                int sourceChunkX, int sourceChunkZ, V118CarvingMask mask) {
            ((V118WorldCarver) carver).carve(configuration, world, random,
                sourceChunkX, sourceChunkZ, mask);
        }
    }

    public static final class Start {
        private final String id;
        private final int index;
        private final int sourceChunkX;
        private final int sourceChunkZ;
        private final long featureSeed;

        private Start(String id, int index, int sourceChunkX, int sourceChunkZ,
                long featureSeed) {
            this.id = id;
            this.index = index;
            this.sourceChunkX = sourceChunkX;
            this.sourceChunkZ = sourceChunkZ;
            this.featureSeed = featureSeed;
        }

        public String id() {
            return id;
        }

        public int index() {
            return index;
        }

        public int sourceChunkX() {
            return sourceChunkX;
        }

        public int sourceChunkZ() {
            return sourceChunkZ;
        }

        public long featureSeed() {
            return featureSeed;
        }
    }

    public static final class Result {
        private final List<Start> starts;
        private final int maskCardinality;
        private final long[] mask;

        private Result(List<Start> starts, V118CarvingMask mask) {
            this.starts = Collections.unmodifiableList(new ArrayList<Start>(starts));
            maskCardinality = mask.cardinality();
            this.mask = mask.toLongArray();
        }

        public List<Start> starts() {
            return starts;
        }

        public int maskCardinality() {
            return maskCardinality;
        }

        public long[] mask() {
            return mask.clone();
        }
    }
}

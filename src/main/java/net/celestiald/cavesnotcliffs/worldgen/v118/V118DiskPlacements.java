package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Exact placed-feature wrappers for Java 1.18.2's three default soft disks. */
public final class V118DiskPlacements {
    private V118DiskPlacements() {
    }

    static DecorationResult decorate(WorldAccess world, long decorationSeed, int chunkX,
            int chunkZ, Set<V118Biome> regionBiomes, V118WorldgenRandom random) {
        EnumMap<PlacedDisk, Boolean> results = new EnumMap<PlacedDisk, Boolean>(PlacedDisk.class);
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        for (PlacedDisk feature : PlacedDisk.values()) {
            if (!feature.belongsToAny(regionBiomes)) {
                continue;
            }
            random.setFeatureSeed(decorationSeed, feature.globalFeatureIndex,
                    V118OrePlacements.UNDERGROUND_ORES_STEP);
            results.put(feature, feature.place(world, random, originX, originZ));
        }
        return new DecorationResult(results);
    }

    public enum PlacedDisk {
        DISK_SAND("disk_sand", 28,
                new V118DiskFeature.Configuration(V118OreMaterial.SAND, 2, 6, 2,
                    V118OreMaterial.DIRT, V118OreMaterial.GRASS_BLOCK), 3, false),
        DISK_CLAY("disk_clay", 29,
                new V118DiskFeature.Configuration(V118OreMaterial.CLAY, 2, 3, 1,
                    V118OreMaterial.DIRT, V118OreMaterial.CLAY), 1, true),
        DISK_GRAVEL("disk_gravel", 30,
                new V118DiskFeature.Configuration(V118OreMaterial.GRAVEL, 2, 5, 2,
                    V118OreMaterial.DIRT, V118OreMaterial.GRASS_BLOCK), 1, false);

        private final String id;
        private final int globalFeatureIndex;
        private final V118DiskFeature.Configuration configuration;
        private final int count;
        private final boolean includesSwamp;

        PlacedDisk(String id, int globalFeatureIndex,
                V118DiskFeature.Configuration configuration, int count,
                boolean includesSwamp) {
            this.id = id;
            this.globalFeatureIndex = globalFeatureIndex;
            this.configuration = configuration;
            this.count = count;
            this.includesSwamp = includesSwamp;
        }

        public String id() {
            return id;
        }

        public int globalFeatureIndex() {
            return globalFeatureIndex;
        }

        public V118DiskFeature.Configuration configuration() {
            return configuration;
        }

        public int count() {
            return count;
        }

        public boolean belongsTo(V118Biome biome) {
            return includesSwamp || biome != V118Biome.SWAMP;
        }

        private boolean belongsToAny(Set<V118Biome> biomes) {
            for (V118Biome biome : biomes) {
                if (belongsTo(biome)) {
                    return true;
                }
            }
            return false;
        }

        private boolean place(WorldAccess world, Random random, int originX, int originZ) {
            boolean placed = false;
            for (int attempt = 0; attempt < count; ++attempt) {
                int x = originX + random.nextInt(16);
                int z = originZ + random.nextInt(16);
                int y = world.oceanFloorHeight(x, z) + 1;
                if (!belongsTo(world.biomeAt(x, y, z))) {
                    continue;
                }
                if (V118DiskFeature.place(world, random, configuration, x, y, z)) {
                    placed = true;
                }
            }
            return placed;
        }

        /** Samples only the placement modifiers, before configured-feature RNG is consumed. */
        public List<Position> samplePlacementOrigins(Random random, int originX, int originZ,
                HeightResolver heights) {
            List<Position> result = new ArrayList<Position>(count);
            for (int attempt = 0; attempt < count; ++attempt) {
                int x = originX + random.nextInt(16);
                int z = originZ + random.nextInt(16);
                result.add(new Position(x, heights.firstAvailableY(x, z), z));
            }
            return Collections.unmodifiableList(result);
        }
    }

    public static final class DecorationResult {
        private final Map<PlacedDisk, Boolean> featureResults;

        DecorationResult(Map<PlacedDisk, Boolean> featureResults) {
            this.featureResults = Collections.unmodifiableMap(
                    new EnumMap<PlacedDisk, Boolean>(featureResults));
        }

        public Map<PlacedDisk, Boolean> featureResults() {
            return featureResults;
        }
    }

    public static final class Position {
        private final int x;
        private final int y;
        private final int z;

        Position(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public int x() {
            return x;
        }

        public int y() {
            return y;
        }

        public int z() {
            return z;
        }
    }

    public interface HeightResolver {
        int firstAvailableY(int blockX, int blockZ);
    }

    public interface WorldAccess extends V118DiskFeature.WorldAccess {
        int oceanFloorHeight(int blockX, int blockZ);

        V118Biome biomeAt(int blockX, int blockY, int blockZ);
    }
}

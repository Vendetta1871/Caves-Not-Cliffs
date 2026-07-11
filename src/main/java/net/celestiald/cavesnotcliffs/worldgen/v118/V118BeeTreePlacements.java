package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Java 1.18.2 placement catalog for every Overworld tree feature carrying a beehive decorator.
 *
 * <p>The global indices are the entries exposed by
 * {@code MultiNoiseBiomeSource#featuresPerStep}. Placement count and in-square sampling are kept
 * separate from the configured tree so they can be checked directly against the official runtime
 * oracle.</p>
 */
public final class V118BeeTreePlacements {
    public static final int VEGETAL_DECORATION_STEP = 9;

    private V118BeeTreePlacements() {
    }

    public static V118WorldgenRandom randomFor(long worldSeed, int chunkX, int chunkZ,
            PlacedFeature feature) {
        return randomFor(worldSeed, chunkX, chunkZ, feature.globalIndex);
    }

    public static V118WorldgenRandom randomFor(long worldSeed, int chunkX, int chunkZ,
            int globalFeatureIndex) {
        V118WorldgenRandom random = new V118WorldgenRandom(0L);
        long decorationSeed = random.setDecorationSeed(worldSeed, chunkX << 4, chunkZ << 4);
        random.setFeatureSeed(decorationSeed, globalFeatureIndex,
                VEGETAL_DECORATION_STEP);
        return random;
    }

    /** Samples only Count/Rarity + InSquare, before terrain and biome filters. */
    public static HorizontalSample horizontalSample(long worldSeed, int chunkX, int chunkZ,
            PlacedFeature feature) {
        V118WorldgenRandom random = randomFor(worldSeed, chunkX, chunkZ, feature);
        int count = feature.sampleCount(random);
        List<HorizontalPosition> positions = new ArrayList<>(count);
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        for (int attempt = 0; attempt < count; ++attempt) {
            positions.add(new HorizontalPosition(originX + random.nextInt(16),
                    originZ + random.nextInt(16)));
        }
        return new HorizontalSample(positions, random.nextLong());
    }

    public enum TreeKind {
        OAK,
        FANCY_OAK,
        BIRCH,
        SUPER_BIRCH
    }

    public enum PlacedFeature {
        TREES_FLOWER_FOREST("trees_flower_forest", 15, 6, 10, 0,
                0.02F, EnumSet.of(V118Biome.FLOWER_FOREST)),
        BIRCH_TALL("birch_tall", 18, 10, 10, 0,
                0.002F, EnumSet.of(V118Biome.OLD_GROWTH_BIRCH_FOREST)),
        TREES_BIRCH("trees_birch", 19, 10, 10, 0,
                0.002F, EnumSet.of(V118Biome.BIRCH_FOREST)),
        TREES_BIRCH_AND_OAK("trees_birch_and_oak", 20, 10, 10, 0,
                0.002F, EnumSet.of(V118Biome.FOREST)),
        TREES_PLAINS("trees_plains", 30, 0, 20, 0,
                0.05F, EnumSet.of(V118Biome.PLAINS, V118Biome.SUNFLOWER_PLAINS,
                        V118Biome.DRIPSTONE_CAVES)),
        TREES_MEADOW("trees_meadow", 34, 0, 0, 100,
                1.0F, EnumSet.of(V118Biome.MEADOW));

        private final String id;
        private final int globalIndex;
        private final int baseCount;
        private final int extraDenominator;
        private final int rarityDenominator;
        private final float nestProbability;
        private final Set<V118Biome> biomes;

        PlacedFeature(String id, int globalIndex, int baseCount,
                int extraDenominator, int rarityDenominator, float nestProbability,
                Set<V118Biome> biomes) {
            this.id = id;
            this.globalIndex = globalIndex;
            this.baseCount = baseCount;
            this.extraDenominator = extraDenominator;
            this.rarityDenominator = rarityDenominator;
            this.nestProbability = nestProbability;
            this.biomes = Collections.unmodifiableSet(EnumSet.copyOf(biomes));
        }

        public String id() {
            return id;
        }

        public int globalIndex() {
            return globalIndex;
        }

        public float nestProbability() {
            return nestProbability;
        }

        public Set<V118Biome> biomes() {
            return biomes;
        }

        public boolean supports(V118Biome biome) {
            return biomes.contains(biome);
        }

        public boolean appearsIn(Set<V118Biome> regionBiomes) {
            for (V118Biome biome : biomes) {
                if (regionBiomes.contains(biome)) {
                    return true;
                }
            }
            return false;
        }

        public int sampleCount(Random random) {
            if (rarityDenominator > 0) {
                return random.nextFloat() < 1.0F / rarityDenominator ? 1 : 0;
            }
            // PlacementUtils.countExtra stores base and base+1 with N-1:1 weights.
            return baseCount + (random.nextInt(extraDenominator)
                    == extraDenominator - 1 ? 1 : 0);
        }

        public TreeKind selectTree(Random random) {
            switch (this) {
                case TREES_FLOWER_FOREST:
                    if (random.nextFloat() < 0.2F) {
                        return TreeKind.BIRCH;
                    }
                    return random.nextFloat() < 0.1F
                            ? TreeKind.FANCY_OAK : TreeKind.OAK;
                case BIRCH_TALL:
                    return random.nextFloat() < 0.5F
                            ? TreeKind.SUPER_BIRCH : TreeKind.BIRCH;
                case TREES_BIRCH:
                    return TreeKind.BIRCH;
                case TREES_BIRCH_AND_OAK:
                    if (random.nextFloat() < 0.2F) {
                        return TreeKind.BIRCH;
                    }
                    return random.nextFloat() < 0.1F
                            ? TreeKind.FANCY_OAK : TreeKind.OAK;
                case TREES_PLAINS:
                    return random.nextFloat() < 0.33333334F
                            ? TreeKind.FANCY_OAK : TreeKind.OAK;
                case TREES_MEADOW:
                    return random.nextFloat() < 0.5F
                            ? TreeKind.FANCY_OAK : TreeKind.SUPER_BIRCH;
                default:
                    throw new AssertionError(this);
            }
        }

        public boolean hasOuterSaplingPlacementFilter() {
            return this == TREES_PLAINS || this == TREES_BIRCH;
        }

        public boolean hasInnerSaplingPlacementFilter() {
            return this == TREES_FLOWER_FOREST || this == BIRCH_TALL
                    || this == TREES_BIRCH_AND_OAK || this == TREES_MEADOW;
        }

        public boolean isBeforeLushFeatures() {
            return globalIndex < 22;
        }
    }

    public static final class HorizontalPosition {
        public final int x;
        public final int z;

        HorizontalPosition(int x, int z) {
            this.x = x;
            this.z = z;
        }

        @Override
        public String toString() {
            return x + "," + z;
        }
    }

    public static final class HorizontalSample {
        private final List<HorizontalPosition> positions;
        private final long trailingRandomLong;

        HorizontalSample(List<HorizontalPosition> positions, long trailingRandomLong) {
            this.positions = Collections.unmodifiableList(new ArrayList<>(positions));
            this.trailingRandomLong = trailingRandomLong;
        }

        public List<HorizontalPosition> positions() {
            return positions;
        }

        public long trailingRandomLong() {
            return trailingRandomLong;
        }

        public String encodedPositions() {
            if (positions.isEmpty()) {
                return "-";
            }
            StringBuilder encoded = new StringBuilder();
            for (HorizontalPosition position : positions) {
                if (encoded.length() > 0) {
                    encoded.append(';');
                }
                encoded.append(position);
            }
            return encoded.toString();
        }
    }
}

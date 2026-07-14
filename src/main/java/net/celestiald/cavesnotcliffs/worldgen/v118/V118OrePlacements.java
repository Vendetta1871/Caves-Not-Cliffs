package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Java 1.18.2's ordinary Overworld ore/blob configured and placed-feature catalog.
 *
 * <p>Feature indices are the official global indices from the Overworld biome source. This is
 * intentionally different from ordinal/local-list indexing: omitted neighboring features still
 * occupy their vanilla seed slots.</p>
 */
public final class V118OrePlacements {
    public static final int UNDERGROUND_ORES_STEP = 6;
    public static final int UNDERGROUND_DECORATION_STEP = 7;

    private static final Set<V118Biome> BADLANDS = Collections.unmodifiableSet(EnumSet.of(
        V118Biome.BADLANDS, V118Biome.ERODED_BADLANDS, V118Biome.WOODED_BADLANDS));
    private static final Set<V118Biome> MOUNTAINS = Collections.unmodifiableSet(EnumSet.of(
        V118Biome.WINDSWEPT_HILLS, V118Biome.WINDSWEPT_GRAVELLY_HILLS,
        V118Biome.WINDSWEPT_FOREST, V118Biome.MEADOW, V118Biome.GROVE,
        V118Biome.SNOWY_SLOPES, V118Biome.FROZEN_PEAKS, V118Biome.JAGGED_PEAKS,
        V118Biome.STONY_PEAKS));

    private V118OrePlacements() {
    }

    /**
     * Executes this scoped subset with vanilla decoration/feature seeds and global order.
     *
     * <p>{@code regionBiomes} must be the vanilla decoration union from the surrounding 3x3
     * chunks. The final candidate is filtered again through {@link WorldAccess#biomeAt}. Features
     * outside this ordinary-ore subset retain their global seed slots but are not executed here;
     * integration must run any such target-mutating feature in its proper global position.</p>
     */
    public static DecorationResult decorate(WorldAccess world, long worldSeed, int chunkX,
            int chunkZ, Set<V118Biome> regionBiomes) {
        return decorate(world, worldSeed, chunkX, chunkZ, regionBiomes,
            BetweenDecorationSteps.NONE);
    }

    /**
     * Executes ordinary ores while exposing the exact step-six/step-seven boundary.
     *
     * <p>Dripstone clusters and pointed dripstone occupy global feature indices zero and one in
     * {@code UNDERGROUND_DECORATION}; infested ore is index two. Keeping this seam here prevents
     * independently ported feature families from accidentally running after infested ore or from
     * sharing the wrong feature RNG.</p>
     */
    public static DecorationResult decorate(WorldAccess world, long worldSeed, int chunkX,
            int chunkZ, Set<V118Biome> regionBiomes,
            BetweenDecorationSteps betweenSteps) {
        return decorate(world, worldSeed, chunkX, chunkZ, regionBiomes, betweenSteps,
                FeatureGate.ALLOW_ALL);
    }

    public static DecorationResult decorate(WorldAccess world, long worldSeed, int chunkX,
            int chunkZ, Set<V118Biome> regionBiomes,
            BetweenDecorationSteps betweenSteps, FeatureGate featureGate) {
        if (world == null || regionBiomes == null) {
            throw new NullPointerException("world and regionBiomes are required");
        }
        if (betweenSteps == null || featureGate == null) {
            throw new NullPointerException("betweenSteps and featureGate are required");
        }
        V118WorldgenRandom random = new V118WorldgenRandom(0L);
        int originX = chunkX << 4;
        int originZ = chunkZ << 4;
        long decorationSeed = random.setDecorationSeed(worldSeed, originX, originZ);
        EnumMap<PlacedOre, Boolean> results = new EnumMap<PlacedOre, Boolean>(PlacedOre.class);
        boolean underwaterMagmaDecorated = false;
        boolean softDisksDecorated = false;
        boolean crossedIntoUndergroundDecoration = false;
        for (PlacedOre feature : PlacedOre.values()) {
            if (!crossedIntoUndergroundDecoration
                    && feature.decorationStep == UNDERGROUND_DECORATION_STEP) {
                betweenSteps.decorate(decorationSeed, chunkX, chunkZ, regionBiomes, random);
                crossedIntoUndergroundDecoration = true;
            }
            if (!underwaterMagmaDecorated && feature.decorationStep == UNDERGROUND_ORES_STEP
                    && feature.globalFeatureIndex > 25) {
                V118UnderwaterMagmaPlacements.decorate(world, decorationSeed, chunkX,
                        chunkZ, random);
                underwaterMagmaDecorated = true;
            }
            if (!softDisksDecorated && feature.decorationStep == UNDERGROUND_ORES_STEP
                    && feature.globalFeatureIndex > 30) {
                V118DiskPlacements.decorate(world, decorationSeed, chunkX, chunkZ,
                        regionBiomes, random);
                softDisksDecorated = true;
            }
            if (!feature.belongsToAny(regionBiomes) || !featureGate.allow(feature)) {
                continue;
            }
            random.setFeatureSeed(decorationSeed, feature.globalFeatureIndex,
                feature.decorationStep);
            results.put(feature, feature.place(world, random, originX, world.minBuildHeight(),
                originZ));
        }
        return new DecorationResult(decorationSeed, results);
    }

    public interface FeatureGate {
        FeatureGate ALLOW_ALL = feature -> true;

        boolean allow(PlacedOre feature);
    }

    public interface BetweenDecorationSteps {
        BetweenDecorationSteps NONE = new BetweenDecorationSteps() {
            @Override
            public void decorate(long decorationSeed, int chunkX, int chunkZ,
                    Set<V118Biome> regionBiomes, V118WorldgenRandom random) {
            }
        };

        void decorate(long decorationSeed, int chunkX, int chunkZ,
                Set<V118Biome> regionBiomes, V118WorldgenRandom random);
    }

    public enum PlacedOre {
        ORE_DIRT("ore_dirt", "ore_dirt", 6, 0, natural(V118OreMaterial.DIRT, 33),
            Count.constant(7), Height.uniform(Anchor.absolute(0), Anchor.absolute(160))),
        ORE_GRAVEL("ore_gravel", "ore_gravel", 6, 1, natural(V118OreMaterial.GRAVEL, 33),
            Count.constant(14), Height.uniform(Anchor.bottom(), Anchor.top())),
        ORE_GRANITE_UPPER("ore_granite_upper", "ore_granite", 6, 2,
            natural(V118OreMaterial.GRANITE, 64), Count.rarity(6),
            Height.uniform(Anchor.absolute(64), Anchor.absolute(128))),
        ORE_GRANITE_LOWER("ore_granite_lower", "ore_granite", 6, 3,
            natural(V118OreMaterial.GRANITE, 64), Count.constant(2),
            Height.uniform(Anchor.absolute(0), Anchor.absolute(60))),
        ORE_DIORITE_UPPER("ore_diorite_upper", "ore_diorite", 6, 4,
            natural(V118OreMaterial.DIORITE, 64), Count.rarity(6),
            Height.uniform(Anchor.absolute(64), Anchor.absolute(128))),
        ORE_DIORITE_LOWER("ore_diorite_lower", "ore_diorite", 6, 5,
            natural(V118OreMaterial.DIORITE, 64), Count.constant(2),
            Height.uniform(Anchor.absolute(0), Anchor.absolute(60))),
        ORE_ANDESITE_UPPER("ore_andesite_upper", "ore_andesite", 6, 6,
            natural(V118OreMaterial.ANDESITE, 64), Count.rarity(6),
            Height.uniform(Anchor.absolute(64), Anchor.absolute(128))),
        ORE_ANDESITE_LOWER("ore_andesite_lower", "ore_andesite", 6, 7,
            natural(V118OreMaterial.ANDESITE, 64), Count.constant(2),
            Height.uniform(Anchor.absolute(0), Anchor.absolute(60))),
        ORE_TUFF("ore_tuff", "ore_tuff", 6, 8, natural(V118OreMaterial.TUFF, 64),
            Count.constant(2), Height.uniform(Anchor.bottom(), Anchor.absolute(0))),

        ORE_COAL_UPPER("ore_coal_upper", "ore_coal", 6, 9,
            ore(V118OreMaterial.COAL_ORE, V118OreMaterial.DEEPSLATE_COAL_ORE, 17, 0.0F),
            Count.constant(30), Height.uniform(Anchor.absolute(136), Anchor.top())),
        ORE_COAL_LOWER("ore_coal_lower", "ore_coal_buried", 6, 10,
            ore(V118OreMaterial.COAL_ORE, V118OreMaterial.DEEPSLATE_COAL_ORE, 17, 0.5F),
            Count.constant(20), Height.triangle(Anchor.absolute(0), Anchor.absolute(192))),
        ORE_IRON_UPPER("ore_iron_upper", "ore_iron", 6, 11,
            ore(V118OreMaterial.IRON_ORE, V118OreMaterial.DEEPSLATE_IRON_ORE, 9, 0.0F),
            Count.constant(90), Height.triangle(Anchor.absolute(80), Anchor.absolute(384))),
        ORE_IRON_MIDDLE("ore_iron_middle", "ore_iron", 6, 12,
            ore(V118OreMaterial.IRON_ORE, V118OreMaterial.DEEPSLATE_IRON_ORE, 9, 0.0F),
            Count.constant(10), Height.triangle(Anchor.absolute(-24), Anchor.absolute(56))),
        ORE_IRON_SMALL("ore_iron_small", "ore_iron_small", 6, 13,
            ore(V118OreMaterial.IRON_ORE, V118OreMaterial.DEEPSLATE_IRON_ORE, 4, 0.0F),
            Count.constant(10), Height.uniform(Anchor.bottom(), Anchor.absolute(72))),
        ORE_GOLD("ore_gold", "ore_gold_buried", 6, 14,
            ore(V118OreMaterial.GOLD_ORE, V118OreMaterial.DEEPSLATE_GOLD_ORE, 9, 0.5F),
            Count.constant(4), Height.triangle(Anchor.absolute(-64), Anchor.absolute(32))),
        ORE_GOLD_LOWER("ore_gold_lower", "ore_gold_buried", 6, 15,
            ore(V118OreMaterial.GOLD_ORE, V118OreMaterial.DEEPSLATE_GOLD_ORE, 9, 0.5F),
            Count.uniform(0, 1), Height.uniform(Anchor.absolute(-64), Anchor.absolute(-48))),
        ORE_REDSTONE("ore_redstone", "ore_redstone", 6, 16,
            ore(V118OreMaterial.REDSTONE_ORE, V118OreMaterial.DEEPSLATE_REDSTONE_ORE, 8, 0.0F),
            Count.constant(4), Height.uniform(Anchor.bottom(), Anchor.absolute(15))),
        ORE_REDSTONE_LOWER("ore_redstone_lower", "ore_redstone", 6, 17,
            ore(V118OreMaterial.REDSTONE_ORE, V118OreMaterial.DEEPSLATE_REDSTONE_ORE, 8, 0.0F),
            Count.constant(8), Height.triangle(Anchor.aboveBottom(-32),
                Anchor.aboveBottom(32))),
        ORE_DIAMOND("ore_diamond", "ore_diamond_small", 6, 18,
            ore(V118OreMaterial.DIAMOND_ORE, V118OreMaterial.DEEPSLATE_DIAMOND_ORE, 4, 0.5F),
            Count.constant(7), Height.triangle(Anchor.aboveBottom(-80),
                Anchor.aboveBottom(80))),
        ORE_DIAMOND_LARGE("ore_diamond_large", "ore_diamond_large", 6, 19,
            ore(V118OreMaterial.DIAMOND_ORE, V118OreMaterial.DEEPSLATE_DIAMOND_ORE, 12, 0.7F),
            Count.rarity(9), Height.triangle(Anchor.aboveBottom(-80),
                Anchor.aboveBottom(80))),
        ORE_DIAMOND_BURIED("ore_diamond_buried", "ore_diamond_buried", 6, 20,
            ore(V118OreMaterial.DIAMOND_ORE, V118OreMaterial.DEEPSLATE_DIAMOND_ORE, 8, 1.0F),
            Count.constant(4), Height.triangle(Anchor.aboveBottom(-80),
                Anchor.aboveBottom(80))),
        ORE_LAPIS("ore_lapis", "ore_lapis", 6, 21,
            ore(V118OreMaterial.LAPIS_ORE, V118OreMaterial.DEEPSLATE_LAPIS_ORE, 7, 0.0F),
            Count.constant(2), Height.triangle(Anchor.absolute(-32), Anchor.absolute(32))),
        ORE_LAPIS_BURIED("ore_lapis_buried", "ore_lapis_buried", 6, 22,
            ore(V118OreMaterial.LAPIS_ORE, V118OreMaterial.DEEPSLATE_LAPIS_ORE, 7, 1.0F),
            Count.constant(4), Height.uniform(Anchor.bottom(), Anchor.absolute(64))),
        ORE_COPPER_LARGE("ore_copper_large", "ore_copper_large", 6, 23,
            ore(V118OreMaterial.COPPER_ORE, V118OreMaterial.DEEPSLATE_COPPER_ORE, 20, 0.0F),
            Count.constant(16), Height.triangle(Anchor.absolute(-16), Anchor.absolute(112)),
            Membership.DRIPSTONE_ONLY),
        ORE_COPPER("ore_copper", "ore_copper_small", 6, 24,
            ore(V118OreMaterial.COPPER_ORE, V118OreMaterial.DEEPSLATE_COPPER_ORE, 10, 0.0F),
            Count.constant(16), Height.triangle(Anchor.absolute(-16), Anchor.absolute(112)),
            Membership.NOT_DRIPSTONE),
        ORE_CLAY("ore_clay", "ore_clay", 6, 26,
            natural(V118OreMaterial.CLAY, 33), Count.constant(46),
            Height.uniform(Anchor.bottom(), Anchor.absolute(256)),
            Membership.LUSH_ONLY),
        ORE_GOLD_EXTRA("ore_gold_extra", "ore_gold", 6, 27,
            ore(V118OreMaterial.GOLD_ORE, V118OreMaterial.DEEPSLATE_GOLD_ORE, 9, 0.0F),
            Count.constant(50), Height.uniform(Anchor.absolute(32), Anchor.absolute(256)),
            Membership.BADLANDS_ONLY),
        ORE_EMERALD("ore_emerald", "ore_emerald", 6, 31,
            ore(V118OreMaterial.EMERALD_ORE, V118OreMaterial.DEEPSLATE_EMERALD_ORE, 3, 0.0F),
            Count.constant(100), Height.triangle(Anchor.absolute(-16), Anchor.absolute(480)),
            Membership.MOUNTAINS_ONLY),
        ORE_INFESTED("ore_infested", "ore_infested", 7, 2,
            ore(V118OreMaterial.INFESTED_STONE, V118OreMaterial.INFESTED_DEEPSLATE, 9, 0.0F),
            Count.constant(14), Height.uniform(Anchor.bottom(), Anchor.absolute(63)),
            Membership.MOUNTAINS_ONLY);

        private final String placedId;
        private final String configuredId;
        private final int decorationStep;
        private final int globalFeatureIndex;
        private final V118OreFeature.Configuration configuration;
        private final Count count;
        private final Height height;
        private final Membership membership;

        PlacedOre(String placedId, String configuredId, int decorationStep,
                int globalFeatureIndex, V118OreFeature.Configuration configuration,
                Count count, Height height) {
            this(placedId, configuredId, decorationStep, globalFeatureIndex, configuration,
                count, height, Membership.ALL);
        }

        PlacedOre(String placedId, String configuredId, int decorationStep,
                int globalFeatureIndex, V118OreFeature.Configuration configuration,
                Count count, Height height, Membership membership) {
            this.placedId = placedId;
            this.configuredId = configuredId;
            this.decorationStep = decorationStep;
            this.globalFeatureIndex = globalFeatureIndex;
            this.configuration = configuration;
            this.count = count;
            this.height = height;
            this.membership = membership;
        }

        public String placedId() {
            return placedId;
        }

        public String configuredId() {
            return configuredId;
        }

        public int decorationStep() {
            return decorationStep;
        }

        public int globalFeatureIndex() {
            return globalFeatureIndex;
        }

        public V118OreFeature.Configuration configuration() {
            return configuration;
        }

        public Count count() {
            return count;
        }

        public Height height() {
            return height;
        }

        public boolean belongsTo(V118Biome biome) {
            return membership.includes(biome);
        }

        boolean belongsToAny(Set<V118Biome> biomes) {
            for (V118Biome biome : biomes) {
                if (belongsTo(biome)) {
                    return true;
                }
            }
            return false;
        }

        boolean place(WorldAccess world, Random random, int originX, int originY, int originZ) {
            int attempts = count.sample(random);
            boolean placed = false;
            for (int attempt = 0; attempt < attempts; ++attempt) {
                int x = originX + random.nextInt(16);
                int z = originZ + random.nextInt(16);
                int y = height.sample(random, world.minBuildHeight(),
                    world.maxBuildHeight());
                if (!belongsTo(world.biomeAt(x, y, z))) {
                    continue;
                }
                if (V118OreFeature.place(world, random, configuration, x, y, z)) {
                    placed = true;
                }
            }
            return placed;
        }

        /** Samples the modifier chain with a no-op configured feature, for oracle validation. */
        public List<Position> samplePlacementOrigins(Random random, int originX, int originY,
                int originZ, int minBuildHeight, int maxBuildHeight) {
            int attempts = count.sample(random);
            List<Position> result = new ArrayList<Position>(attempts);
            for (int attempt = 0; attempt < attempts; ++attempt) {
                int x = originX + random.nextInt(16);
                int z = originZ + random.nextInt(16);
                int y = height.sample(random, minBuildHeight, maxBuildHeight);
                result.add(new Position(x, y, z));
            }
            return Collections.unmodifiableList(result);
        }

        public String parityString() {
            StringBuilder result = new StringBuilder();
            result.append(placedId).append('|').append(configuredId).append('|')
                .append(decorationStep).append('|').append(globalFeatureIndex).append('|')
                .append(configuration.size()).append('|')
                .append(Integer.toUnsignedString(
                    Float.floatToRawIntBits(configuration.discardChanceOnAirExposure())))
                .append('|').append(count.parityString()).append('|')
                .append(height.parityString()).append('|').append(membership.name());
            for (V118OreFeature.Target target : configuration.targets()) {
                result.append('|').append(target.rule().name()).append('>')
                    .append(target.result().name());
            }
            return result.toString();
        }
    }

    public static final class Count {
        public enum Kind {
            CONSTANT,
            UNIFORM,
            RARITY
        }

        private final Kind kind;
        private final int minimum;
        private final int maximum;

        private Count(Kind kind, int minimum, int maximum) {
            this.kind = kind;
            this.minimum = minimum;
            this.maximum = maximum;
        }

        public static Count constant(int count) {
            return new Count(Kind.CONSTANT, count, count);
        }

        public static Count uniform(int minimum, int maximum) {
            return new Count(Kind.UNIFORM, minimum, maximum);
        }

        public static Count rarity(int chance) {
            return new Count(Kind.RARITY, chance, chance);
        }

        public Kind kind() {
            return kind;
        }

        public int minimum() {
            return minimum;
        }

        public int maximum() {
            return maximum;
        }

        int sample(Random random) {
            if (kind == Kind.RARITY) {
                return random.nextFloat() < 1.0F / minimum ? 1 : 0;
            }
            if (kind == Kind.UNIFORM) {
                return random.nextInt(maximum - minimum + 1) + minimum;
            }
            return minimum;
        }

        String parityString() {
            return kind.name() + ':' + minimum + ':' + maximum;
        }
    }

    public static final class Height {
        public enum Kind {
            UNIFORM,
            TRAPEZOID
        }

        private final Kind kind;
        private final Anchor minimum;
        private final Anchor maximum;
        private final int plateau;

        private Height(Kind kind, Anchor minimum, Anchor maximum, int plateau) {
            this.kind = kind;
            this.minimum = minimum;
            this.maximum = maximum;
            this.plateau = plateau;
        }

        public static Height uniform(Anchor minimum, Anchor maximum) {
            return new Height(Kind.UNIFORM, minimum, maximum, 0);
        }

        public static Height triangle(Anchor minimum, Anchor maximum) {
            return new Height(Kind.TRAPEZOID, minimum, maximum, 0);
        }

        public Kind kind() {
            return kind;
        }

        public Anchor minimum() {
            return minimum;
        }

        public Anchor maximum() {
            return maximum;
        }

        public int plateau() {
            return plateau;
        }

        int sample(Random random, int minBuildHeight, int maxBuildHeight) {
            int min = minimum.resolve(minBuildHeight, maxBuildHeight);
            int max = maximum.resolve(minBuildHeight, maxBuildHeight);
            if (min > max) {
                return min;
            }
            int range = max - min;
            if (kind == Kind.UNIFORM || plateau >= range) {
                return min + random.nextInt(range + 1);
            }
            int lowerSlope = (range - plateau) / 2;
            int upperSlope = range - lowerSlope;
            return min + random.nextInt(upperSlope + 1) + random.nextInt(lowerSlope + 1);
        }

        String parityString() {
            return kind.name() + ':' + minimum.parityString() + ':' + maximum.parityString()
                + ':' + plateau;
        }
    }

    public static final class Anchor {
        public enum Kind {
            ABSOLUTE,
            ABOVE_BOTTOM,
            BELOW_TOP
        }

        private final Kind kind;
        private final int value;

        private Anchor(Kind kind, int value) {
            this.kind = kind;
            this.value = value;
        }

        public static Anchor absolute(int y) {
            return new Anchor(Kind.ABSOLUTE, y);
        }

        public static Anchor bottom() {
            return aboveBottom(0);
        }

        public static Anchor aboveBottom(int offset) {
            return new Anchor(Kind.ABOVE_BOTTOM, offset);
        }

        public static Anchor top() {
            return belowTop(0);
        }

        public static Anchor belowTop(int offset) {
            return new Anchor(Kind.BELOW_TOP, offset);
        }

        public Kind kind() {
            return kind;
        }

        public int value() {
            return value;
        }

        int resolve(int minBuildHeight, int maxBuildHeight) {
            if (kind == Kind.ABOVE_BOTTOM) {
                return minBuildHeight + value;
            }
            if (kind == Kind.BELOW_TOP) {
                return maxBuildHeight - 1 - value;
            }
            return value;
        }

        String parityString() {
            return kind.name() + ',' + value;
        }
    }

    private enum Membership {
        ALL {
            @Override
            boolean includes(V118Biome biome) {
                return true;
            }
        },
        NOT_DRIPSTONE {
            @Override
            boolean includes(V118Biome biome) {
                return biome != V118Biome.DRIPSTONE_CAVES;
            }
        },
        DRIPSTONE_ONLY {
            @Override
            boolean includes(V118Biome biome) {
                return biome == V118Biome.DRIPSTONE_CAVES;
            }
        },
        LUSH_ONLY {
            @Override
            boolean includes(V118Biome biome) {
                return biome == V118Biome.LUSH_CAVES;
            }
        },
        BADLANDS_ONLY {
            @Override
            boolean includes(V118Biome biome) {
                return BADLANDS.contains(biome);
            }
        },
        MOUNTAINS_ONLY {
            @Override
            boolean includes(V118Biome biome) {
                return MOUNTAINS.contains(biome);
            }
        };

        abstract boolean includes(V118Biome biome);
    }

    public static final class DecorationResult {
        private final long decorationSeed;
        private final Map<PlacedOre, Boolean> featureResults;

        DecorationResult(long decorationSeed, Map<PlacedOre, Boolean> featureResults) {
            this.decorationSeed = decorationSeed;
            this.featureResults = Collections.unmodifiableMap(
                new EnumMap<PlacedOre, Boolean>(featureResults));
        }

        public long decorationSeed() {
            return decorationSeed;
        }

        public Map<PlacedOre, Boolean> featureResults() {
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

    public interface WorldAccess extends V118OreFeature.WorldAccess,
            V118DiskPlacements.WorldAccess, V118UnderwaterMagmaFeature.WorldAccess {
        V118Biome biomeAt(int blockX, int blockY, int blockZ);
    }

    private static V118OreFeature.Configuration natural(V118OreMaterial result, int size) {
        return new V118OreFeature.Configuration(Collections.singletonList(
            new V118OreFeature.Target(V118OreFeature.TargetRule.NATURAL_STONE, result)),
            size, 0.0F);
    }

    private static V118OreFeature.Configuration ore(V118OreMaterial stone,
            V118OreMaterial deepslate, int size, float discardChance) {
        return new V118OreFeature.Configuration(Arrays.asList(
            new V118OreFeature.Target(V118OreFeature.TargetRule.STONE_ORE_REPLACEABLES, stone),
            new V118OreFeature.Target(V118OreFeature.TargetRule.DEEPSLATE_ORE_REPLACEABLES,
                deepslate)), size, discardChance);
    }
}

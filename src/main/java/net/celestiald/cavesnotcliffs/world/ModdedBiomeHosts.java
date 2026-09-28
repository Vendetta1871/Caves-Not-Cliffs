package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Decides which Java 1.18 biomes each modded surface biome may stand in for.
 *
 * <p>Terrain, caves and ores always come from the 1.18 climate map. A modded biome only ever
 * replaces a 1.18 "host" biome whose climate, relief and character it matches: a modded swamp
 * takes over 1.18 swamps, a modded alpine biome takes over peaks and slopes, and a land biome
 * never lands on an ocean. Hosts are described by their 1.18 temperature and downfall plus the
 * 1.12 height and dictionary types of the biome they resemble, so a modded biome is compared
 * with each host on the same scale its own mod described it on.</p>
 */
final class ModdedBiomeHosts {
    /** Hosts per modded biome: the best fit plus near-ties (e.g. forest and flower forest). */
    static final int MAX_HOSTS = 3;
    static final double HOST_SCORE_MARGIN = 0.25D;

    enum Water {
        LAND, OCEAN, RIVER, BEACH
    }

    /** One modded biome with its share of a host. */
    static final class Candidate {
        final Biome biome;
        final double weight;

        Candidate(Biome biome, double weight) {
            this.biome = biome;
            this.weight = weight;
        }
    }

    /** Climate, relief and character of a biome, on 1.12's scales. */
    static final class Traits {
        final float temperature;
        final float downfall;
        final float baseHeight;
        final float heightVariation;
        final Water water;
        final Set<Type> types;

        Traits(float temperature, float downfall, float baseHeight, float heightVariation,
                Water water, Set<Type> types) {
            this.temperature = temperature;
            this.downfall = downfall;
            this.baseHeight = baseHeight;
            this.heightVariation = heightVariation;
            this.water = water;
            this.types = types;
        }

        boolean snowy() {
            // 1.12 snows at sea level below 0.15, which is also where 1.18 turns water to ice.
            return temperature < 0.15F;
        }
    }

    private static final Map<V118Biome, Traits> HOSTS = createHostTraits();
    // Lists, not sets: the penalty is summed in this fixed order so host tables are identical
    // on every start (BiomeDictionary.Type is a class with identity hash codes, not an enum).
    private static final List<Type> MAJOR_TYPES = Arrays.asList(Type.SWAMP, Type.JUNGLE,
            Type.SAVANNA, Type.MESA, Type.SANDY, Type.CONIFEROUS, Type.FOREST, Type.PLAINS,
            Type.MOUNTAIN, Type.SNOWY, Type.MUSHROOM, Type.WASTELAND);
    private static final List<Type> MINOR_TYPES = Arrays.asList(Type.HILLS, Type.SPOOKY,
            Type.DENSE, Type.SPARSE, Type.WET, Type.DRY, Type.HOT, Type.COLD);
    private static final List<Type> NOT_OVERWORLD = Arrays.asList(Type.NETHER, Type.END,
            Type.VOID);

    private final Candidate[][] candidates = new Candidate[V118Biome.values().length][];
    private final double[] candidateWeights = new double[V118Biome.values().length];
    private final double vanillaWeight;

    private ModdedBiomeHosts(double vanillaWeight) {
        this.vanillaWeight = vanillaWeight;
    }

    static ModdedBiomeHosts empty() {
        return new ModdedBiomeHosts(1.0D);
    }

    /**
     * Builds the host table. {@code weights} holds each modded biome's rarity on BiomeManager's
     * scale, where every vanilla biome weighs 10; iteration order must be deterministic.
     * {@code overrides} pins a biome to explicit hosts instead of the scored ones.
     */
    static ModdedBiomeHosts build(Map<Biome, Double> weights,
            Map<Biome, List<V118Biome>> overrides, double vanillaWeight) {
        return build(weights, overrides, vanillaWeight, ModdedBiomeHosts::traitsOf);
    }

    /** Test seam: {@code traits} replaces the BiomeDictionary lookup. */
    static ModdedBiomeHosts build(Map<Biome, Double> weights,
            Map<Biome, List<V118Biome>> overrides, double vanillaWeight,
            Function<Biome, Traits> traits) {
        ModdedBiomeHosts table = new ModdedBiomeHosts(Math.max(0.0D, vanillaWeight));
        List<List<Candidate>> byHost = new ArrayList<List<Candidate>>();
        for (int index = 0; index < V118Biome.values().length; ++index) {
            byHost.add(new ArrayList<Candidate>());
        }
        for (Map.Entry<Biome, Double> entry : weights.entrySet()) {
            Biome biome = entry.getKey();
            double weight = entry.getValue() == null ? 0.0D : entry.getValue();
            if (biome == null || !(weight > 0.0D)) {
                continue;
            }
            List<V118Biome> hosts = overrides.containsKey(biome)
                    ? overrides.get(biome) : hostsFor(traits.apply(biome));
            if (hosts.isEmpty()) {
                continue;
            }
            double share = weight / hosts.size();
            for (V118Biome host : hosts) {
                byHost.get(host.ordinal()).add(new Candidate(biome, share));
            }
        }
        for (V118Biome host : V118Biome.values()) {
            List<Candidate> list = byHost.get(host.ordinal());
            if (list.isEmpty()) {
                continue;
            }
            table.candidates[host.ordinal()] = list.toArray(new Candidate[list.size()]);
            double total = 0.0D;
            for (Candidate candidate : list) {
                total += candidate.weight;
            }
            table.candidateWeights[host.ordinal()] = total;
        }
        return table;
    }

    boolean hasCandidates(V118Biome host) {
        return host != null && candidates[host.ordinal()] != null;
    }

    boolean isEmpty() {
        for (Candidate[] list : candidates) {
            if (list != null) {
                return false;
            }
        }
        return true;
    }

    List<Candidate> candidates(V118Biome host) {
        Candidate[] list = host == null ? null : candidates[host.ordinal()];
        return list == null ? Collections.<Candidate>emptyList()
                : Collections.unmodifiableList(Arrays.asList(list));
    }

    /**
     * The modded biome a region shows in place of {@code host}, or null where the 1.18 biome
     * stays. {@code unit} is the region's uniform roll in [0, 1) for this host.
     */
    Biome pick(V118Biome host, double unit) {
        if (host == null) {
            return null;
        }
        Candidate[] list = candidates[host.ordinal()];
        if (list == null) {
            return null;
        }
        double roll = unit * (vanillaWeight + candidateWeights[host.ordinal()]);
        if (roll < vanillaWeight) {
            return null;
        }
        roll -= vanillaWeight;
        for (Candidate candidate : list) {
            if (roll < candidate.weight) {
                return candidate.biome;
            }
            roll -= candidate.weight;
        }
        return list[list.length - 1].biome;
    }

    /** The 1.18 biomes a modded biome with these traits fits best, best first. */
    static List<V118Biome> hostsFor(Traits traits) {
        if (traits == null || !Collections.disjoint(traits.types, NOT_OVERWORLD)) {
            return Collections.emptyList();
        }
        List<V118Biome> hosts = new ArrayList<V118Biome>();
        List<Double> scores = new ArrayList<Double>();
        for (Map.Entry<V118Biome, Traits> entry : HOSTS.entrySet()) {
            Traits host = entry.getValue();
            if (host.water != traits.water) {
                continue;
            }
            // Mushroom fields are ocean islands with their own surface; they host mushroom
            // biomes only, and mushroom biomes live nowhere else.
            if (traits.types.contains(Type.MUSHROOM) != host.types.contains(Type.MUSHROOM)) {
                continue;
            }
            double score = score(traits, host);
            int position = 0;
            while (position < scores.size() && scores.get(position) <= score) {
                ++position;
            }
            scores.add(position, score);
            hosts.add(position, entry.getKey());
        }
        if (hosts.isEmpty()) {
            return hosts;
        }
        double best = scores.get(0);
        int count = 1;
        while (count < hosts.size() && count < MAX_HOSTS
                && scores.get(count) <= best + HOST_SCORE_MARGIN) {
            ++count;
        }
        return new ArrayList<V118Biome>(hosts.subList(0, count));
    }

    /** Lower is a better fit. */
    static double score(Traits modded, Traits host) {
        double score = 1.5D * Math.abs(clampTemperature(modded.temperature)
                - clampTemperature(host.temperature));
        score += Math.abs(modded.downfall - host.downfall);
        if (modded.water == Water.LAND || modded.water == Water.OCEAN) {
            score += 0.5D * Math.abs(clampHeight(modded.baseHeight) - host.baseHeight);
            score += 0.3D * Math.abs(modded.heightVariation - host.heightVariation);
        }
        if (modded.snowy() != host.snowy()) {
            score += 2.0D;
        }
        score += typePenalty(modded.types, host.types, MAJOR_TYPES, 0.6D, 0.5D);
        score += typePenalty(modded.types, host.types, MINOR_TYPES, 0.2D, 0.1D);
        return score;
    }

    private static double typePenalty(Set<Type> modded, Set<Type> host, List<Type> considered,
            double missingOnHost, double missingOnModded) {
        double penalty = 0.0D;
        for (Type type : considered) {
            boolean inModded = modded.contains(type);
            boolean inHost = host.contains(type);
            if (inModded && !inHost) {
                penalty += missingOnHost;
            } else if (inHost && !inModded) {
                penalty += missingOnModded;
            }
        }
        return penalty;
    }

    private static float clampTemperature(float temperature) {
        return Math.max(-0.5F, Math.min(2.0F, temperature));
    }

    private static float clampHeight(float height) {
        return Math.max(-2.0F, Math.min(2.0F, height));
    }

    static Traits traitsOf(Biome biome) {
        Set<Type> types = new HashSet<Type>(BiomeDictionary.getTypes(biome));
        return traitsOf(biome, types);
    }

    static Traits traitsOf(Biome biome, Set<Type> types) {
        return new Traits(biome.getDefaultTemperature(), biome.getRainfall(),
                biome.getBaseHeight(), biome.getHeightVariation(),
                waterOf(types, biome.getBaseHeight()), types);
    }

    static Water waterOf(Set<Type> types, float baseHeight) {
        if (types.contains(Type.OCEAN)) {
            return Water.OCEAN;
        }
        if (types.contains(Type.RIVER)) {
            return Water.RIVER;
        }
        if (types.contains(Type.BEACH)) {
            return Water.BEACH;
        }
        return baseHeight <= -0.8F ? Water.OCEAN : Water.LAND;
    }

    static Traits hostTraits(V118Biome host) {
        return HOSTS.get(host);
    }

    private static Map<V118Biome, Traits> createHostTraits() {
        Map<V118Biome, Traits> hosts = new EnumMap<V118Biome, Traits>(V118Biome.class);
        land(hosts, V118Biome.BADLANDS, 0.0F, 0.1F, 0.2F,
                Type.MESA, Type.SANDY, Type.HOT, Type.DRY);
        land(hosts, V118Biome.ERODED_BADLANDS, 0.0F, 0.1F, 0.2F,
                Type.MESA, Type.SANDY, Type.HOT, Type.DRY, Type.HILLS);
        land(hosts, V118Biome.WOODED_BADLANDS, 0.0F, 1.5F, 0.025F,
                Type.MESA, Type.SANDY, Type.HOT, Type.DRY, Type.SPARSE);
        land(hosts, V118Biome.BAMBOO_JUNGLE, 0.9F, 0.1F, 0.2F,
                Type.JUNGLE, Type.HOT, Type.WET, Type.DENSE);
        land(hosts, V118Biome.JUNGLE, 0.9F, 0.1F, 0.2F,
                Type.JUNGLE, Type.HOT, Type.WET, Type.DENSE);
        land(hosts, V118Biome.SPARSE_JUNGLE, 0.8F, 0.1F, 0.2F,
                Type.JUNGLE, Type.HOT, Type.WET, Type.SPARSE);
        water(hosts, V118Biome.BEACH, Water.BEACH, 0.4F, 0.0F, 0.025F, Type.BEACH);
        water(hosts, V118Biome.SNOWY_BEACH, Water.BEACH, 0.3F, 0.0F, 0.025F,
                Type.BEACH, Type.COLD, Type.SNOWY);
        water(hosts, V118Biome.STONY_SHORE, Water.BEACH, 0.3F, 0.1F, 0.8F, Type.BEACH);
        land(hosts, V118Biome.BIRCH_FOREST, 0.6F, 0.1F, 0.2F, Type.FOREST);
        land(hosts, V118Biome.OLD_GROWTH_BIRCH_FOREST, 0.6F, 0.2F, 0.4F,
                Type.FOREST, Type.DENSE, Type.HILLS);
        land(hosts, V118Biome.DARK_FOREST, 0.8F, 0.1F, 0.2F,
                Type.FOREST, Type.SPOOKY, Type.DENSE);
        land(hosts, V118Biome.FOREST, 0.8F, 0.1F, 0.2F, Type.FOREST);
        land(hosts, V118Biome.FLOWER_FOREST, 0.8F, 0.1F, 0.4F, Type.FOREST, Type.HILLS);
        land(hosts, V118Biome.DESERT, 0.0F, 0.125F, 0.05F, Type.SANDY, Type.HOT, Type.DRY);
        ocean(hosts, V118Biome.OCEAN, -1.0F);
        ocean(hosts, V118Biome.DEEP_OCEAN, -1.8F);
        ocean(hosts, V118Biome.COLD_OCEAN, -1.0F, Type.COLD);
        ocean(hosts, V118Biome.DEEP_COLD_OCEAN, -1.8F, Type.COLD);
        ocean(hosts, V118Biome.FROZEN_OCEAN, -1.0F, Type.COLD, Type.SNOWY);
        ocean(hosts, V118Biome.DEEP_FROZEN_OCEAN, -1.8F, Type.COLD, Type.SNOWY);
        ocean(hosts, V118Biome.LUKEWARM_OCEAN, -1.0F, Type.HOT);
        ocean(hosts, V118Biome.DEEP_LUKEWARM_OCEAN, -1.8F, Type.HOT);
        ocean(hosts, V118Biome.WARM_OCEAN, -1.0F, Type.HOT);
        land(hosts, V118Biome.FROZEN_PEAKS, 0.9F, 2.0F, 0.5F,
                Type.MOUNTAIN, Type.COLD, Type.SNOWY);
        land(hosts, V118Biome.JAGGED_PEAKS, 0.9F, 2.0F, 0.5F,
                Type.MOUNTAIN, Type.COLD, Type.SNOWY);
        land(hosts, V118Biome.STONY_PEAKS, 0.3F, 2.0F, 0.5F, Type.MOUNTAIN);
        land(hosts, V118Biome.SNOWY_SLOPES, 0.9F, 1.5F, 0.4F,
                Type.MOUNTAIN, Type.COLD, Type.SNOWY);
        land(hosts, V118Biome.GROVE, 0.8F, 1.2F, 0.3F,
                Type.FOREST, Type.CONIFEROUS, Type.MOUNTAIN, Type.COLD, Type.SNOWY);
        land(hosts, V118Biome.MEADOW, 0.8F, 1.0F, 0.2F, Type.PLAINS, Type.MOUNTAIN);
        water(hosts, V118Biome.FROZEN_RIVER, Water.RIVER, 0.5F, -0.5F, 0.0F,
                Type.RIVER, Type.COLD, Type.SNOWY);
        water(hosts, V118Biome.RIVER, Water.RIVER, 0.5F, -0.5F, 0.0F, Type.RIVER);
        land(hosts, V118Biome.ICE_SPIKES, 0.5F, 0.425F, 0.45F,
                Type.COLD, Type.SNOWY, Type.WASTELAND, Type.HILLS);
        land(hosts, V118Biome.SNOWY_PLAINS, 0.5F, 0.125F, 0.05F,
                Type.COLD, Type.SNOWY, Type.WASTELAND, Type.PLAINS);
        land(hosts, V118Biome.MUSHROOM_FIELDS, 1.0F, 0.2F, 0.3F, Type.MUSHROOM);
        land(hosts, V118Biome.OLD_GROWTH_PINE_TAIGA, 0.8F, 0.2F, 0.2F,
                Type.FOREST, Type.CONIFEROUS, Type.COLD, Type.DENSE);
        land(hosts, V118Biome.OLD_GROWTH_SPRUCE_TAIGA, 0.8F, 0.2F, 0.2F,
                Type.FOREST, Type.CONIFEROUS, Type.COLD, Type.DENSE);
        land(hosts, V118Biome.TAIGA, 0.8F, 0.2F, 0.2F,
                Type.FOREST, Type.CONIFEROUS, Type.COLD);
        land(hosts, V118Biome.SNOWY_TAIGA, 0.4F, 0.2F, 0.2F,
                Type.FOREST, Type.CONIFEROUS, Type.COLD, Type.SNOWY);
        land(hosts, V118Biome.PLAINS, 0.4F, 0.125F, 0.05F, Type.PLAINS);
        land(hosts, V118Biome.SUNFLOWER_PLAINS, 0.4F, 0.125F, 0.05F, Type.PLAINS);
        land(hosts, V118Biome.SAVANNA, 0.0F, 0.125F, 0.05F,
                Type.SAVANNA, Type.PLAINS, Type.HOT, Type.DRY, Type.SPARSE);
        land(hosts, V118Biome.SAVANNA_PLATEAU, 0.0F, 1.5F, 0.025F,
                Type.SAVANNA, Type.HOT, Type.DRY, Type.SPARSE);
        land(hosts, V118Biome.WINDSWEPT_SAVANNA, 0.0F, 0.4F, 1.2F,
                Type.SAVANNA, Type.HILLS, Type.HOT, Type.DRY, Type.SPARSE);
        land(hosts, V118Biome.SWAMP, 0.9F, -0.2F, 0.1F, Type.SWAMP, Type.WET);
        land(hosts, V118Biome.WINDSWEPT_FOREST, 0.3F, 1.0F, 0.5F,
                Type.MOUNTAIN, Type.HILLS, Type.FOREST, Type.SPARSE);
        land(hosts, V118Biome.WINDSWEPT_GRAVELLY_HILLS, 0.3F, 1.0F, 0.5F,
                Type.MOUNTAIN, Type.HILLS, Type.SPARSE);
        land(hosts, V118Biome.WINDSWEPT_HILLS, 0.3F, 1.0F, 0.5F, Type.MOUNTAIN, Type.HILLS);
        // Lush and dripstone caves are underground biomes; nothing replaces them.
        return Collections.unmodifiableMap(hosts);
    }

    private static void land(Map<V118Biome, Traits> hosts, V118Biome biome, float downfall,
            float baseHeight, float heightVariation, Type... types) {
        water(hosts, biome, Water.LAND, downfall, baseHeight, heightVariation, types);
    }

    private static void ocean(Map<V118Biome, Traits> hosts, V118Biome biome, float baseHeight,
            Type... types) {
        Type[] withOcean = Arrays.copyOf(types, types.length + 1);
        withOcean[types.length] = Type.OCEAN;
        water(hosts, biome, Water.OCEAN, 0.5F, baseHeight, 0.1F, withOcean);
    }

    private static void water(Map<V118Biome, Traits> hosts, V118Biome biome, Water water,
            float downfall, float baseHeight, float heightVariation, Type... types) {
        Set<Type> typeSet = new HashSet<Type>(Arrays.asList(types));
        // Deep frozen ocean keeps 0.5 but freezes through 1.18's frozen temperature modifier.
        float temperature = biome.hasFrozenTemperatureModifier() ? 0.0F
                : biome.baseTemperature();
        hosts.put(biome, new Traits(temperature, downfall, baseHeight,
                heightVariation, water, Collections.unmodifiableSet(typeSet)));
    }
}

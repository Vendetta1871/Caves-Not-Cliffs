package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Finds the modded surface biomes a native world should contain and how common each is.
 *
 * <p>Most mods add Overworld biomes through {@link BiomeManager} with a weight relative to the
 * vanilla biomes' 10; that weight is kept as the biome's rarity. Biomes that only the base
 * world type's layer chain places (custom GenLayers, sub-biomes) are found by sampling that
 * chain on a sparse grid, and their frequency there is converted to the same weight scale.
 * The config blacklist removes biomes and the host list pins biomes to chosen 1.18 hosts.</p>
 */
final class ModdedBiomeCatalog {
    private static final Logger LOGGER = LogManager.getLogger("CavesNotCliffs/ModdedBiomes");
    static final int SAMPLE_GRID = 48;
    static final int SAMPLE_SPACING_QUARTS = 80;
    private static final double MIN_SAMPLED_WEIGHT = 1.0D;
    private static final double MAX_SAMPLED_WEIGHT = 10.0D;

    private ModdedBiomeCatalog() {}

    /** Parsed config: excluded biomes and explicit host assignments. */
    static final class Settings {
        final Set<String> blacklistedBiomes;
        final Set<String> blacklistedMods;
        final Map<String, List<V118Biome>> hosts;

        Settings(Set<String> blacklistedBiomes, Set<String> blacklistedMods,
                Map<String, List<V118Biome>> hosts) {
            this.blacklistedBiomes = blacklistedBiomes;
            this.blacklistedMods = blacklistedMods;
            this.hosts = hosts;
        }

        static Settings none() {
            return new Settings(Collections.<String>emptySet(), Collections.<String>emptySet(),
                    Collections.<String, List<V118Biome>>emptyMap());
        }

        boolean isBlacklisted(Biome biome) {
            ResourceLocation name = biome.getRegistryName();
            return name == null || blacklistedBiomes.contains(name.toString())
                    || blacklistedMods.contains(name.getResourceDomain());
        }
    }

    /**
     * Parses {@code modid:biome} / {@code modid:*} blacklist entries and
     * {@code modid:biome=host,host} entries whose hosts are 1.18 biome ids (the
     * {@code minecraft:} namespace may be omitted). Invalid entries are logged and skipped.
     */
    static Settings parse(String[] blacklist, String[] hosts) {
        Set<String> biomes = new HashSet<String>();
        Set<String> mods = new HashSet<String>();
        for (String raw : blacklist == null ? new String[0] : blacklist) {
            String entry = raw == null ? "" : raw.trim();
            if (entry.isEmpty()) {
                continue;
            }
            if (entry.endsWith(":*")) {
                mods.add(entry.substring(0, entry.length() - 2));
            } else if (entry.indexOf(':') > 0) {
                biomes.add(new ResourceLocation(entry).toString());
            } else {
                LOGGER.warn("Ignoring modded biome blacklist entry '{}': expected modid:biome"
                        + " or modid:*", entry);
            }
        }
        Map<String, List<V118Biome>> pinned = new HashMap<String, List<V118Biome>>();
        for (String raw : hosts == null ? new String[0] : hosts) {
            String entry = raw == null ? "" : raw.trim();
            if (entry.isEmpty()) {
                continue;
            }
            int separator = entry.indexOf('=');
            if (separator <= 0 || entry.indexOf(':') <= 0 || entry.indexOf(':') > separator) {
                LOGGER.warn("Ignoring modded biome host entry '{}': expected"
                        + " modid:biome=host[,host...]", entry);
                continue;
            }
            List<V118Biome> resolved = new ArrayList<V118Biome>();
            for (String host : entry.substring(separator + 1).split(",")) {
                V118Biome biome = parseHost(host.trim());
                if (biome == null) {
                    LOGGER.warn("Ignoring unknown or underground 1.18 host '{}' in '{}'",
                            host.trim(), entry);
                } else if (!resolved.contains(biome)) {
                    resolved.add(biome);
                }
            }
            if (!resolved.isEmpty()) {
                pinned.put(new ResourceLocation(entry.substring(0, separator).trim()).toString(),
                        Collections.unmodifiableList(resolved));
            }
        }
        return new Settings(biomes, mods, pinned);
    }

    private static V118Biome parseHost(String host) {
        if (host.isEmpty()) {
            return null;
        }
        String id = host.indexOf(':') < 0 ? "minecraft:" + host : host;
        try {
            V118Biome biome = V118Biome.fromId(id);
            return ModdedBiomeHosts.hostTraits(biome) == null ? null : biome;
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }

    /** Every biome's summed weight across BiomeManager's four climate lists. */
    static Map<Biome, Integer> managerWeights() {
        Map<Biome, Integer> weights = new IdentityHashMap<Biome, Integer>();
        for (BiomeManager.BiomeType type : BiomeManager.BiomeType.values()) {
            List<BiomeManager.BiomeEntry> entries = BiomeManager.getBiomes(type);
            if (entries == null) {
                continue;
            }
            for (BiomeManager.BiomeEntry entry : entries) {
                if (entry != null && entry.biome != null && entry.itemWeight > 0) {
                    Integer previous = weights.get(entry.biome);
                    weights.put(entry.biome, (previous == null ? 0 : previous) + entry.itemWeight);
                }
            }
        }
        return weights;
    }

    /** How often each biome appears on a sparse grid of the base world type's layer chain. */
    static Map<Biome, Integer> sampleChain(ModdedBiomeOverlay.Sampler chain) {
        Map<Biome, Integer> counts = new IdentityHashMap<Biome, Integer>();
        int half = SAMPLE_GRID / 2;
        for (int row = 0; row < SAMPLE_GRID; ++row) {
            for (int column = 0; column < SAMPLE_GRID; ++column) {
                Biome[] sample = chain.generationBiomes(
                        (column - half) * SAMPLE_SPACING_QUARTS,
                        (row - half) * SAMPLE_SPACING_QUARTS, 1, 1);
                Biome biome = sample == null || sample.length == 0 ? null : sample[0];
                if (biome != null) {
                    Integer previous = counts.get(biome);
                    counts.put(biome, previous == null ? 1 : previous + 1);
                }
            }
        }
        return counts;
    }

    /**
     * Weights on BiomeManager's scale for every modded, non-blacklisted biome, ordered by
     * registry name so the host table never depends on hash order.
     */
    static Map<Biome, Double> weights(Map<Biome, Integer> managerWeights,
            Map<Biome, Integer> sampledCounts, Settings settings) {
        double sampledTotal = 0.0D;
        double weightTotal = 0.0D;
        for (Map.Entry<Biome, Integer> entry : managerWeights.entrySet()) {
            Integer count = sampledCounts.get(entry.getKey());
            if (count != null && count > 0) {
                sampledTotal += count;
                weightTotal += entry.getValue();
            }
        }
        // Samples per unit of BiomeManager weight, measured on the biomes that have both.
        double samplesPerWeight = weightTotal > 0.0D ? sampledTotal / weightTotal : 0.0D;
        TreeMap<String, Biome> ordered = new TreeMap<String, Biome>();
        collect(ordered, managerWeights.keySet(), settings);
        collect(ordered, sampledCounts.keySet(), settings);
        Map<Biome, Double> weights = new LinkedHashMap<Biome, Double>();
        for (Biome biome : ordered.values()) {
            Integer declared = managerWeights.get(biome);
            double weight;
            if (declared != null && declared > 0) {
                weight = declared;
            } else {
                Integer count = sampledCounts.get(biome);
                weight = samplesPerWeight > 0.0D && count != null
                        ? Math.max(MIN_SAMPLED_WEIGHT,
                                Math.min(MAX_SAMPLED_WEIGHT, count / samplesPerWeight))
                        : MAX_SAMPLED_WEIGHT;
            }
            weights.put(biome, weight);
        }
        return weights;
    }

    private static void collect(TreeMap<String, Biome> ordered, Iterable<Biome> biomes,
            Settings settings) {
        for (Biome biome : biomes) {
            if (ModdedBiomeOverlay.isModded(biome) && !settings.isBlacklisted(biome)) {
                ordered.put(biome.getRegistryName().toString(), biome);
            }
        }
    }

    /** Config host pins resolved against the registry. */
    static Map<Biome, List<V118Biome>> overrides(Settings settings,
            Function<ResourceLocation, Biome> registry) {
        Map<Biome, List<V118Biome>> resolved = new IdentityHashMap<Biome, List<V118Biome>>();
        for (Map.Entry<String, List<V118Biome>> entry : settings.hosts.entrySet()) {
            Biome biome = registry.apply(new ResourceLocation(entry.getKey()));
            if (biome == null) {
                LOGGER.warn("Modded biome host entry names unregistered biome {}",
                        entry.getKey());
            } else {
                resolved.put(biome, entry.getValue());
            }
        }
        return resolved;
    }

    /** Discovers and scores the modded biomes of this game instance. */
    static ModdedBiomeHosts buildHosts(ModdedBiomeOverlay.Sampler chain, Settings settings,
            double vanillaWeight) {
        Map<Biome, Integer> sampled = Collections.emptyMap();
        if (chain != null) {
            try {
                sampled = sampleChain(chain);
            } catch (RuntimeException failure) {
                LOGGER.warn("Could not sample the base world type's biome chain; only"
                        + " BiomeManager biomes are placed", failure);
            }
        }
        Map<Biome, Double> weights = weights(managerWeights(), sampled, settings);
        Map<Biome, List<V118Biome>> pinned = overrides(settings,
                location -> Biome.REGISTRY.getObject(location));
        ModdedBiomeHosts hosts = ModdedBiomeHosts.build(weights, pinned, vanillaWeight);
        logTable(hosts);
        return hosts;
    }

    private static void logTable(ModdedBiomeHosts hosts) {
        if (hosts.isEmpty()) {
            return;
        }
        StringBuilder table = new StringBuilder("Modded surface biomes by 1.18 host:");
        for (V118Biome host : V118Biome.values()) {
            List<ModdedBiomeHosts.Candidate> candidates = hosts.candidates(host);
            if (candidates.isEmpty()) {
                continue;
            }
            table.append("\n  ").append(host.id()).append(" <-");
            for (ModdedBiomeHosts.Candidate candidate : candidates) {
                table.append(' ').append(candidate.biome.getRegistryName())
                        .append(String.format(Locale.ROOT, " (%.1f)", candidate.weight));
            }
        }
        LOGGER.info(table.toString());
    }
}

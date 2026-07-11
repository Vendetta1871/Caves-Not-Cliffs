package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Deterministic projection of Java 1.18 biomes onto the finite 1.12 biome registry. */
public final class V118BiomeMapper {
    private static final Map<V118Biome, String> LEGACY_IDS = createAliases();

    private final Biome[] biomes = new Biome[V118Biome.values().length];

    /** Resolves the complete alias table after vanilla biome bootstrap. */
    public static V118BiomeMapper fromRegisteredBiomes() {
        Biome[] resolved = new Biome[V118Biome.values().length];
        for (V118Biome biome : V118Biome.values()) {
            String legacyId = legacyId(biome);
            Biome legacy = Biome.REGISTRY.getObject(new ResourceLocation(legacyId));
            if (legacy == null) {
                throw new IllegalStateException("Required legacy biome is not registered: "
                    + legacyId + " (for " + biome.id() + ")");
            }
            resolved[biome.ordinal()] = legacy;
        }
        return new V118BiomeMapper(resolved);
    }

    V118BiomeMapper(Biome[] resolved) {
        if (resolved == null || resolved.length != V118Biome.values().length) {
            throw new IllegalArgumentException("Expected one legacy biome per 1.18 biome");
        }
        for (V118Biome biome : V118Biome.values()) {
            Biome legacy = resolved[biome.ordinal()];
            if (legacy == null) {
                throw new IllegalArgumentException("Missing legacy biome for " + biome);
            }
            biomes[biome.ordinal()] = legacy;
        }
    }

    public Biome biomeFor(int biomeId) {
        if (biomeId < 0 || biomeId >= biomes.length) {
            throw new IllegalArgumentException("Unknown 1.18 biome storage id: " + biomeId);
        }
        return biomes[biomeId];
    }

    public Biome biomeFor(V118Biome biome) {
        if (biome == null) {
            throw new NullPointerException("biome");
        }
        return biomes[biome.ordinal()];
    }

    /** Exposes the stable alias contract for tests, diagnostics, and future migrations. */
    public static String legacyId(V118Biome biome) {
        if (biome == null) {
            throw new NullPointerException("biome");
        }
        String id = LEGACY_IDS.get(biome);
        if (id == null) {
            throw new IllegalArgumentException("No legacy biome alias for " + biome);
        }
        return id;
    }

    private static Map<V118Biome, String> createAliases() {
        EnumMap<V118Biome, String> aliases = new EnumMap<V118Biome, String>(V118Biome.class);
        alias(aliases, V118Biome.BADLANDS, "mesa");
        alias(aliases, V118Biome.BAMBOO_JUNGLE, "jungle");
        alias(aliases, V118Biome.BEACH, "beaches");
        alias(aliases, V118Biome.BIRCH_FOREST, "birch_forest");
        alias(aliases, V118Biome.COLD_OCEAN, "ocean");
        alias(aliases, V118Biome.DARK_FOREST, "roofed_forest");
        alias(aliases, V118Biome.DEEP_COLD_OCEAN, "deep_ocean");
        alias(aliases, V118Biome.DEEP_FROZEN_OCEAN, "frozen_ocean");
        alias(aliases, V118Biome.DEEP_LUKEWARM_OCEAN, "deep_ocean");
        alias(aliases, V118Biome.DEEP_OCEAN, "deep_ocean");
        alias(aliases, V118Biome.DESERT, "desert");
        alias(aliases, V118Biome.DRIPSTONE_CAVES, "extreme_hills");
        alias(aliases, V118Biome.ERODED_BADLANDS, "mutated_mesa");
        alias(aliases, V118Biome.FLOWER_FOREST, "mutated_forest");
        alias(aliases, V118Biome.FOREST, "forest");
        alias(aliases, V118Biome.FROZEN_OCEAN, "frozen_ocean");
        alias(aliases, V118Biome.FROZEN_PEAKS, "ice_mountains");
        alias(aliases, V118Biome.FROZEN_RIVER, "frozen_river");
        alias(aliases, V118Biome.GROVE, "taiga_cold");
        alias(aliases, V118Biome.ICE_SPIKES, "mutated_ice_flats");
        alias(aliases, V118Biome.JAGGED_PEAKS, "extreme_hills");
        alias(aliases, V118Biome.JUNGLE, "jungle");
        alias(aliases, V118Biome.LUKEWARM_OCEAN, "ocean");
        alias(aliases, V118Biome.LUSH_CAVES, "forest");
        alias(aliases, V118Biome.MEADOW, "plains");
        alias(aliases, V118Biome.MUSHROOM_FIELDS, "mushroom_island");
        alias(aliases, V118Biome.OCEAN, "ocean");
        alias(aliases, V118Biome.OLD_GROWTH_BIRCH_FOREST, "mutated_birch_forest");
        alias(aliases, V118Biome.OLD_GROWTH_PINE_TAIGA, "redwood_taiga");
        alias(aliases, V118Biome.OLD_GROWTH_SPRUCE_TAIGA, "mutated_redwood_taiga");
        alias(aliases, V118Biome.PLAINS, "plains");
        alias(aliases, V118Biome.RIVER, "river");
        alias(aliases, V118Biome.SAVANNA, "savanna");
        alias(aliases, V118Biome.SAVANNA_PLATEAU, "savanna_rock");
        alias(aliases, V118Biome.SNOWY_BEACH, "cold_beach");
        alias(aliases, V118Biome.SNOWY_PLAINS, "ice_flats");
        alias(aliases, V118Biome.SNOWY_SLOPES, "ice_mountains");
        alias(aliases, V118Biome.SNOWY_TAIGA, "taiga_cold");
        alias(aliases, V118Biome.SPARSE_JUNGLE, "jungle_edge");
        alias(aliases, V118Biome.STONY_PEAKS, "extreme_hills");
        alias(aliases, V118Biome.STONY_SHORE, "stone_beach");
        alias(aliases, V118Biome.SUNFLOWER_PLAINS, "mutated_plains");
        alias(aliases, V118Biome.SWAMP, "swampland");
        alias(aliases, V118Biome.TAIGA, "taiga");
        alias(aliases, V118Biome.WARM_OCEAN, "ocean");
        alias(aliases, V118Biome.WINDSWEPT_FOREST, "extreme_hills_with_trees");
        alias(aliases, V118Biome.WINDSWEPT_GRAVELLY_HILLS, "mutated_extreme_hills");
        alias(aliases, V118Biome.WINDSWEPT_HILLS, "extreme_hills");
        alias(aliases, V118Biome.WINDSWEPT_SAVANNA, "mutated_savanna");
        alias(aliases, V118Biome.WOODED_BADLANDS, "mesa_rock");

        if (aliases.size() != V118Biome.values().length) {
            throw new IllegalStateException("Incomplete 1.18 to 1.12 biome alias table");
        }
        return Collections.unmodifiableMap(aliases);
    }

    private static void alias(Map<V118Biome, String> aliases, V118Biome biome, String path) {
        String previous = aliases.put(biome, "minecraft:" + path);
        if (previous != null) {
            throw new IllegalStateException("Duplicate legacy biome alias for " + biome);
        }
    }
}

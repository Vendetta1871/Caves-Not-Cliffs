package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Exact built-in normal-noise parameter registry bootstrapped by Java 1.18.2. */
public final class V118NoiseParameters {
    private static final Map<String, NormalNoise.NoiseParameters> PARAMETERS = createParameters();

    private V118NoiseParameters() {
    }

    public static NormalNoise.NoiseParameters get(String name) {
        NormalNoise.NoiseParameters parameters = PARAMETERS.get(name);
        if (parameters == null) {
            throw new IllegalArgumentException("Unknown 1.18.2 noise: " + name);
        }
        return parameters;
    }

    public static NormalNoise instantiate(String name, PositionalRandomFactory positional) {
        return NormalNoise.create(positional.fromHashOf("minecraft:" + name), get(name));
    }

    public static Map<String, NormalNoise.NoiseParameters> all() {
        return PARAMETERS;
    }

    private static Map<String, NormalNoise.NoiseParameters> createParameters() {
        Map<String, NormalNoise.NoiseParameters> values = new LinkedHashMap<String, NormalNoise.NoiseParameters>();
        registerBiomeNoises(values, 0, "temperature", "vegetation", "continentalness", "erosion");
        registerBiomeNoises(values, -2, "temperature_large", "vegetation_large",
                "continentalness_large", "erosion_large");
        register(values, "ridge", -7, 1.0D, 2.0D, 1.0D, 0.0D, 0.0D, 0.0D);
        register(values, "offset", -3, 1.0D, 1.0D, 1.0D, 0.0D);
        register(values, "aquifer_barrier", -3, 1.0D);
        register(values, "aquifer_fluid_level_floodedness", -7, 1.0D);
        register(values, "aquifer_lava", -1, 1.0D);
        register(values, "aquifer_fluid_level_spread", -5, 1.0D);
        register(values, "pillar", -7, 1.0D, 1.0D);
        register(values, "pillar_rareness", -8, 1.0D);
        register(values, "pillar_thickness", -8, 1.0D);
        register(values, "spaghetti_2d", -7, 1.0D);
        register(values, "spaghetti_2d_elevation", -8, 1.0D);
        register(values, "spaghetti_2d_modulator", -11, 1.0D);
        register(values, "spaghetti_2d_thickness", -11, 1.0D);
        register(values, "spaghetti_3d_1", -7, 1.0D);
        register(values, "spaghetti_3d_2", -7, 1.0D);
        register(values, "spaghetti_3d_rarity", -11, 1.0D);
        register(values, "spaghetti_3d_thickness", -8, 1.0D);
        register(values, "spaghetti_roughness", -5, 1.0D);
        register(values, "spaghetti_roughness_modulator", -8, 1.0D);
        register(values, "cave_entrance", -7, 0.4D, 0.5D, 1.0D);
        register(values, "cave_layer", -8, 1.0D);
        register(values, "cave_cheese", -8,
                0.5D, 1.0D, 2.0D, 1.0D, 2.0D, 1.0D, 0.0D, 2.0D, 0.0D);
        register(values, "ore_veininess", -8, 1.0D);
        register(values, "ore_vein_a", -7, 1.0D);
        register(values, "ore_vein_b", -7, 1.0D);
        register(values, "ore_gap", -5, 1.0D);
        register(values, "noodle", -8, 1.0D);
        register(values, "noodle_thickness", -8, 1.0D);
        register(values, "noodle_ridge_a", -7, 1.0D);
        register(values, "noodle_ridge_b", -7, 1.0D);
        register(values, "jagged", -16,
                1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D,
                1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D, 1.0D);
        register(values, "surface", -6, 1.0D, 1.0D, 1.0D);
        register(values, "surface_secondary", -6, 1.0D, 1.0D, 0.0D, 1.0D);
        register(values, "clay_bands_offset", -8, 1.0D);
        register(values, "badlands_pillar", -2, 1.0D, 1.0D, 1.0D, 1.0D);
        register(values, "badlands_pillar_roof", -8, 1.0D);
        register(values, "badlands_surface", -6, 1.0D, 1.0D, 1.0D);
        register(values, "iceberg_pillar", -6, 1.0D, 1.0D, 1.0D, 1.0D);
        register(values, "iceberg_pillar_roof", -3, 1.0D);
        register(values, "iceberg_surface", -6, 1.0D, 1.0D, 1.0D);
        register(values, "surface_swamp", -2, 1.0D);
        register(values, "calcite", -9, 1.0D, 1.0D, 1.0D, 1.0D);
        register(values, "gravel", -8, 1.0D, 1.0D, 1.0D, 1.0D);
        register(values, "powder_snow", -6, 1.0D, 1.0D, 1.0D, 1.0D);
        register(values, "packed_ice", -7, 1.0D, 1.0D, 1.0D, 1.0D);
        register(values, "ice", -4, 1.0D, 1.0D, 1.0D, 1.0D);
        register(values, "soul_sand_layer", -8,
                1.0D, 1.0D, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D, 0.0D,
                0.013333333333333334D);
        register(values, "gravel_layer", -8,
                1.0D, 1.0D, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D, 0.0D,
                0.013333333333333334D);
        register(values, "patch", -5, 1.0D, 0.0D, 0.0D, 0.0D, 0.0D,
                0.013333333333333334D);
        register(values, "netherrack", -3, 1.0D, 0.0D, 0.0D, 0.35D);
        register(values, "nether_wart", -3, 1.0D, 0.0D, 0.0D, 0.9D);
        register(values, "nether_state_selector", -4, 1.0D);
        return Collections.unmodifiableMap(values);
    }

    private static void registerBiomeNoises(Map<String, NormalNoise.NoiseParameters> values,
            int offset, String temperature, String vegetation, String continentalness,
            String erosion) {
        register(values, temperature, -10 + offset, 1.5D, 0.0D, 1.0D, 0.0D, 0.0D, 0.0D);
        register(values, vegetation, -8 + offset, 1.0D, 1.0D, 0.0D, 0.0D, 0.0D, 0.0D);
        register(values, continentalness, -9 + offset,
                1.0D, 1.0D, 2.0D, 2.0D, 2.0D, 1.0D, 1.0D, 1.0D, 1.0D);
        register(values, erosion, -9 + offset, 1.0D, 1.0D, 0.0D, 1.0D, 1.0D);
    }

    private static void register(Map<String, NormalNoise.NoiseParameters> values,
            String name, int firstOctave, double firstAmplitude,
            double... remainingAmplitudes) {
        NormalNoise.NoiseParameters previous = values.put(name,
                new NormalNoise.NoiseParameters(firstOctave, firstAmplitude,
                        remainingAmplitudes));
        if (previous != null) {
            throw new IllegalStateException("Duplicate 1.18.2 noise parameters: " + name);
        }
    }
}

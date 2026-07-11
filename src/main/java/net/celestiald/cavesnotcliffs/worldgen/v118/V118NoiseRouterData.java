package net.celestiald.cavesnotcliffs.worldgen.v118;

/** Exact Java 1.18.2 Overworld density-router construction for the three native profiles. */
public final class V118NoiseRouterData {
    private static final double MAX_REASONABLE_NOISE_VALUE = 1000000.0D;
    private static final double SURFACE_DENSITY_THRESHOLD = 1.5625D;

    private V118NoiseRouterData() {
    }

    public enum Profile {
        DEFAULT(false, false),
        LARGE_BIOMES(false, true),
        AMPLIFIED(true, false);

        private final boolean amplified;
        private final boolean largeBiomes;

        Profile(boolean amplified, boolean largeBiomes) {
            this.amplified = amplified;
            this.largeBiomes = largeBiomes;
        }

        public boolean amplified() {
            return amplified;
        }

        public boolean largeBiomes() {
            return largeBiomes;
        }
    }

    public static V118NoiseRouter create(long seed, Profile profile) {
        if (profile == null) {
            throw new NullPointerException("profile");
        }
        return new Factory(seed, profile).create();
    }

    private static final class Factory {
        private final Profile profile;
        private final V118NoiseSettings settings;
        private final PositionalRandomFactory positional;
        private final TerrainShaper terrainShaper;

        private Factory(long seed, Profile profile) {
            this.profile = profile;
            settings = V118NoiseSettings.overworld(profile.amplified());
            positional = new XoroshiroRandomSource(seed).forkPositional();
            terrainShaper = TerrainShaper.overworld(profile.amplified()
                ? TerrainShaper.Profile.AMPLIFIED : TerrainShaper.Profile.NORMAL);
        }

        private V118NoiseRouter create() {
            NormalNoise offsetNoise = normal("offset");
            DensityFunction shiftX = DensityFunctions.flatCache(DensityFunctions.cache2d(
                DensityFunctions.shiftA(offsetNoise)));
            DensityFunction shiftZ = DensityFunctions.flatCache(DensityFunctions.cache2d(
                DensityFunctions.shiftB(offsetNoise)));

            String continentalnessName = profile.largeBiomes()
                ? "continentalness_large" : "continentalness";
            String erosionName = profile.largeBiomes() ? "erosion_large" : "erosion";
            String temperatureName = profile.largeBiomes()
                ? "temperature_large" : "temperature";
            String vegetationName = profile.largeBiomes()
                ? "vegetation_large" : "vegetation";

            DensityFunction continents = DensityFunctions.flatCache(
                DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25D,
                    normal(continentalnessName)));
            DensityFunction erosion = DensityFunctions.flatCache(
                DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25D,
                    normal(erosionName)));
            DensityFunction ridges = DensityFunctions.flatCache(
                DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25D, normal("ridge")));
            DensityFunction jagged = DensityFunctions.noise(normal("jagged"), 1500.0D, 0.0D);
            DensityFunction offset = splineWithBlending(continents, erosion, ridges,
                DensityFunctions.TerrainShaperSplineType.OFFSET, -0.81D, 2.5D,
                DensityFunctions.blendOffset());
            DensityFunction factor = splineWithBlending(continents, erosion, ridges,
                DensityFunctions.TerrainShaperSplineType.FACTOR, 0.0D, 8.0D,
                DensityFunctions.constant(10.0D));
            DensityFunction depth = DensityFunctions.add(
                DensityFunctions.yClampedGradient(-64, 320, 1.5D, -1.5D), offset);

            BlendedNoise base3d = new BlendedNoise(
                positional.fromHashOf("minecraft:terrain"), settings.sampling(),
                settings.getCellWidth(), settings.getCellHeight());
            DensityFunction slopedCheese = slopedCheese(continents, erosion, ridges, factor,
                depth, jagged, base3d);

            DensityFunction spaghettiRoughness = spaghettiRoughnessFunction();
            DensityFunction spaghettiThickness = DensityFunctions.cacheOnce(mappedNoise(
                "spaghetti_2d_thickness", 2.0D, 1.0D, -0.6D, -1.3D));
            DensityFunction spaghetti2d = spaghetti2D(spaghettiThickness);
            DensityFunction entrances = entrances(spaghettiRoughness);
            DensityFunction noodle = noodle();
            DensityFunction pillars = pillars();

            DensityFunction initialDensity = noiseGradientDensity(
                DensityFunctions.cache2d(factor), depth);
            DensityFunction entranceLimited = DensityFunctions.min(slopedCheese,
                DensityFunctions.mul(DensityFunctions.constant(5.0D), entrances));
            DensityFunction caves = DensityFunctions.rangeChoice(slopedCheese,
                -MAX_REASONABLE_NOISE_VALUE, SURFACE_DENSITY_THRESHOLD, entranceLimited,
                underground(slopedCheese, spaghetti2d, spaghettiRoughness, entrances, pillars));
            DensityFunction finalDensity = DensityFunctions.min(postProcess(caves), noodle);

            DensityFunction temperature = DensityFunctions.shiftedNoise2d(shiftX, shiftZ,
                0.25D, normal(temperatureName));
            DensityFunction humidity = DensityFunctions.shiftedNoise2d(shiftX, shiftZ,
                0.25D, normal(vegetationName));

            DensityFunction y = DensityFunctions.yClampedGradient(-4064, 4062,
                -4064.0D, 4062.0D);
            DensityFunction veinToggle = yLimitedInterpolatable(y,
                noise("ore_veininess", 1.5D, 1.5D), -60, 50, 0.0D);
            DensityFunction veinA = yLimitedInterpolatable(y,
                noise("ore_vein_a", 4.0D, 4.0D), -60, 50, 0.0D).abs();
            DensityFunction veinB = yLimitedInterpolatable(y,
                noise("ore_vein_b", 4.0D, 4.0D), -60, 50, 0.0D).abs();
            DensityFunction veinRidged = DensityFunctions.add(
                DensityFunctions.constant((double) -0.08F),
                DensityFunctions.max(veinA, veinB));
            DensityFunction veinGap = noise("ore_gap", 1.0D, 1.0D);

            PositionalRandomFactory aquiferFactory = positional
                .fromHashOf("minecraft:aquifer").forkPositional();
            PositionalRandomFactory oreFactory = positional
                .fromHashOf("minecraft:ore").forkPositional();
            return new V118NoiseRouter(
                noise("aquifer_barrier", 1.0D, 0.5D),
                noise("aquifer_fluid_level_floodedness", 1.0D, 0.67D),
                noise("aquifer_fluid_level_spread", 1.0D,
                    0.7142857142857143D),
                noise("aquifer_lava", 1.0D, 1.0D),
                aquiferFactory, oreFactory, temperature, humidity, continents, erosion, depth,
                ridges, initialDensity, finalDensity, veinToggle, veinRidged, veinGap);
        }

        private DensityFunction slopedCheese(DensityFunction continents,
                DensityFunction erosion, DensityFunction ridges, DensityFunction factor,
                DensityFunction depth, DensityFunction jagged, DensityFunction base3d) {
            DensityFunction jaggedness = splineWithBlending(continents, erosion, ridges,
                DensityFunctions.TerrainShaperSplineType.JAGGEDNESS, 0.0D, 1.28D,
                DensityFunctions.zero());
            DensityFunction jaggedOffset = DensityFunctions.mul(jaggedness,
                jagged.halfNegative());
            DensityFunction gradient = noiseGradientDensity(factor,
                DensityFunctions.add(depth, jaggedOffset));
            return DensityFunctions.add(gradient, base3d);
        }

        private DensityFunction spaghettiRoughnessFunction() {
            DensityFunction roughness = noise("spaghetti_roughness", 1.0D, 1.0D);
            DensityFunction modulator = mappedNoise("spaghetti_roughness_modulator",
                1.0D, 1.0D, 0.0D, -0.1D);
            return DensityFunctions.cacheOnce(DensityFunctions.mul(modulator,
                DensityFunctions.add(roughness.abs(), DensityFunctions.constant(-0.4D))));
        }

        private DensityFunction entrances(DensityFunction spaghettiRoughness) {
            DensityFunction rarity = DensityFunctions.cacheOnce(
                noise("spaghetti_3d_rarity", 2.0D, 1.0D));
            DensityFunction thickness = mappedNoise("spaghetti_3d_thickness",
                1.0D, 1.0D, -0.065D, -0.088D);
            DensityFunction first = DensityFunctions.weirdScaledSampler(rarity,
                normal("spaghetti_3d_1"), DensityFunctions.RarityValueMapper.TYPE1);
            DensityFunction second = DensityFunctions.weirdScaledSampler(rarity,
                normal("spaghetti_3d_2"), DensityFunctions.RarityValueMapper.TYPE1);
            DensityFunction tunnels = DensityFunctions.add(
                DensityFunctions.max(first, second), thickness).clamp(-1.0D, 1.0D);
            DensityFunction entranceNoise = noise("cave_entrance", 0.75D, 0.5D);
            DensityFunction entranceGradient = DensityFunctions.add(
                DensityFunctions.add(entranceNoise, DensityFunctions.constant(0.37D)),
                DensityFunctions.yClampedGradient(-10, 30, 0.3D, 0.0D));
            return DensityFunctions.cacheOnce(DensityFunctions.min(entranceGradient,
                DensityFunctions.add(spaghettiRoughness, tunnels)));
        }

        private DensityFunction noodle() {
            DensityFunction y = DensityFunctions.yClampedGradient(-4064, 4062,
                -4064.0D, 4062.0D);
            DensityFunction noodle = yLimitedInterpolatable(y,
                noise("noodle", 1.0D, 1.0D), -60, 320, -1.0D);
            DensityFunction thickness = yLimitedInterpolatable(y,
                mappedNoise("noodle_thickness", 1.0D, 1.0D, -0.05D, -0.1D),
                -60, 320, 0.0D);
            DensityFunction ridgeA = yLimitedInterpolatable(y,
                noise("noodle_ridge_a", 2.6666666666666665D, 2.6666666666666665D),
                -60, 320, 0.0D);
            DensityFunction ridgeB = yLimitedInterpolatable(y,
                noise("noodle_ridge_b", 2.6666666666666665D, 2.6666666666666665D),
                -60, 320, 0.0D);
            DensityFunction ridges = DensityFunctions.mul(DensityFunctions.constant(1.5D),
                DensityFunctions.max(ridgeA.abs(), ridgeB.abs()));
            return DensityFunctions.rangeChoice(noodle, -MAX_REASONABLE_NOISE_VALUE, 0.0D,
                DensityFunctions.constant(64.0D), DensityFunctions.add(thickness, ridges));
        }

        private DensityFunction pillars() {
            DensityFunction pillar = noise("pillar", 25.0D, 0.3D);
            DensityFunction rareness = mappedNoise("pillar_rareness",
                1.0D, 1.0D, 0.0D, -2.0D);
            DensityFunction thickness = mappedNoise("pillar_thickness",
                1.0D, 1.0D, 0.0D, 1.1D);
            DensityFunction body = DensityFunctions.add(
                DensityFunctions.mul(pillar, DensityFunctions.constant(2.0D)), rareness);
            return DensityFunctions.cacheOnce(DensityFunctions.mul(body, thickness.cube()));
        }

        private DensityFunction spaghetti2D(DensityFunction thickness) {
            DensityFunction modulator = noise("spaghetti_2d_modulator", 2.0D, 1.0D);
            DensityFunction spaghetti = DensityFunctions.weirdScaledSampler(modulator,
                normal("spaghetti_2d"), DensityFunctions.RarityValueMapper.TYPE2);
            DensityFunction elevation = mappedNoise("spaghetti_2d_elevation",
                1.0D, 0.0D, Math.floorDiv(-64, 8), 8.0D);
            DensityFunction vertical = DensityFunctions.add(elevation,
                DensityFunctions.yClampedGradient(-64, 320, 8.0D, -40.0D)).abs();
            DensityFunction verticalTunnel = DensityFunctions.add(vertical, thickness).cube();
            DensityFunction horizontalTunnel = DensityFunctions.add(spaghetti,
                DensityFunctions.mul(DensityFunctions.constant(0.083D), thickness));
            return DensityFunctions.max(horizontalTunnel, verticalTunnel)
                .clamp(-1.0D, 1.0D);
        }

        private DensityFunction underground(DensityFunction slopedCheese,
                DensityFunction spaghetti2d, DensityFunction spaghettiRoughness,
                DensityFunction entrances, DensityFunction pillars) {
            DensityFunction caveLayer = noise("cave_layer", 1.0D, 8.0D);
            DensityFunction layer = DensityFunctions.mul(DensityFunctions.constant(4.0D),
                caveLayer.square());
            DensityFunction cheeseNoise = noise("cave_cheese", 1.0D,
                0.6666666666666666D);
            DensityFunction cheese = DensityFunctions.add(
                DensityFunctions.add(DensityFunctions.constant(0.27D), cheeseNoise)
                    .clamp(-1.0D, 1.0D),
                DensityFunctions.add(DensityFunctions.constant(1.5D),
                    DensityFunctions.mul(DensityFunctions.constant(-0.64D), slopedCheese))
                    .clamp(0.0D, 0.5D));
            DensityFunction cave = DensityFunctions.add(layer, cheese);
            DensityFunction tunnels = DensityFunctions.min(
                DensityFunctions.min(cave, entrances),
                DensityFunctions.add(spaghetti2d, spaghettiRoughness));
            DensityFunction pillarRange = DensityFunctions.rangeChoice(pillars,
                -MAX_REASONABLE_NOISE_VALUE, 0.03D,
                DensityFunctions.constant(-MAX_REASONABLE_NOISE_VALUE), pillars);
            return DensityFunctions.max(tunnels, pillarRange);
        }

        private DensityFunction postProcess(DensityFunction input) {
            DensityFunction slide = DensityFunctions.slide(settings, input);
            DensityFunction blend = DensityFunctions.blendDensity(slide);
            return DensityFunctions.mul(DensityFunctions.interpolated(blend),
                DensityFunctions.constant(0.64D)).squeeze();
        }

        private DensityFunction splineWithBlending(DensityFunction continents,
                DensityFunction erosion, DensityFunction ridges,
                DensityFunctions.TerrainShaperSplineType splineType,
                double minValue, double maxValue, DensityFunction blendValue) {
            DensityFunction spline = DensityFunctions.terrainShaperSpline(continents,
                erosion, ridges, terrainShaper, splineType, minValue, maxValue);
            DensityFunction blended = DensityFunctions.lerp(DensityFunctions.blendAlpha(),
                blendValue, spline);
            return DensityFunctions.flatCache(DensityFunctions.cache2d(blended));
        }

        private DensityFunction noiseGradientDensity(DensityFunction factor,
                DensityFunction depth) {
            DensityFunction product = DensityFunctions.mul(depth, factor);
            return DensityFunctions.mul(DensityFunctions.constant(4.0D),
                product.quarterNegative());
        }

        private DensityFunction yLimitedInterpolatable(DensityFunction y,
                DensityFunction input, int minY, int maxY, double outside) {
            return DensityFunctions.interpolated(DensityFunctions.rangeChoice(y,
                minY, maxY + 1.0D, input, DensityFunctions.constant(outside)));
        }

        private DensityFunction noise(String name, double xzScale, double yScale) {
            return DensityFunctions.noise(normal(name), xzScale, yScale);
        }

        private DensityFunction mappedNoise(String name, double xzScale, double yScale,
                double min, double max) {
            return DensityFunctions.mappedNoise(normal(name), xzScale, yScale, min, max);
        }

        private NormalNoise normal(String name) {
            return V118NoiseParameters.instantiate(name, positional);
        }
    }
}

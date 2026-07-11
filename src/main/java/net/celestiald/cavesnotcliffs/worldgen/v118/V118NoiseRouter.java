package net.celestiald.cavesnotcliffs.worldgen.v118;

/** Seeded Java 1.18.2 Overworld density router. */
public final class V118NoiseRouter {
    private final DensityFunction barrierNoise;
    private final DensityFunction fluidLevelFloodednessNoise;
    private final DensityFunction fluidLevelSpreadNoise;
    private final DensityFunction lavaNoise;
    private final PositionalRandomFactory aquiferPositionalRandomFactory;
    private final PositionalRandomFactory oreVeinsPositionalRandomFactory;
    private final DensityFunction temperature;
    private final DensityFunction humidity;
    private final DensityFunction continents;
    private final DensityFunction erosion;
    private final DensityFunction depth;
    private final DensityFunction ridges;
    private final DensityFunction initialDensityWithoutJaggedness;
    private final DensityFunction finalDensity;
    private final DensityFunction veinToggle;
    private final DensityFunction veinRidged;
    private final DensityFunction veinGap;

    V118NoiseRouter(DensityFunction barrierNoise,
            DensityFunction fluidLevelFloodednessNoise,
            DensityFunction fluidLevelSpreadNoise, DensityFunction lavaNoise,
            PositionalRandomFactory aquiferPositionalRandomFactory,
            PositionalRandomFactory oreVeinsPositionalRandomFactory,
            DensityFunction temperature, DensityFunction humidity,
            DensityFunction continents, DensityFunction erosion, DensityFunction depth,
            DensityFunction ridges, DensityFunction initialDensityWithoutJaggedness,
            DensityFunction finalDensity, DensityFunction veinToggle,
            DensityFunction veinRidged, DensityFunction veinGap) {
        this.barrierNoise = barrierNoise;
        this.fluidLevelFloodednessNoise = fluidLevelFloodednessNoise;
        this.fluidLevelSpreadNoise = fluidLevelSpreadNoise;
        this.lavaNoise = lavaNoise;
        this.aquiferPositionalRandomFactory = aquiferPositionalRandomFactory;
        this.oreVeinsPositionalRandomFactory = oreVeinsPositionalRandomFactory;
        this.temperature = temperature;
        this.humidity = humidity;
        this.continents = continents;
        this.erosion = erosion;
        this.depth = depth;
        this.ridges = ridges;
        this.initialDensityWithoutJaggedness = initialDensityWithoutJaggedness;
        this.finalDensity = finalDensity;
        this.veinToggle = veinToggle;
        this.veinRidged = veinRidged;
        this.veinGap = veinGap;
    }

    public DensityFunction barrierNoise() {
        return barrierNoise;
    }

    public DensityFunction fluidLevelFloodednessNoise() {
        return fluidLevelFloodednessNoise;
    }

    public DensityFunction fluidLevelSpreadNoise() {
        return fluidLevelSpreadNoise;
    }

    public DensityFunction lavaNoise() {
        return lavaNoise;
    }

    public PositionalRandomFactory aquiferPositionalRandomFactory() {
        return aquiferPositionalRandomFactory;
    }

    public PositionalRandomFactory oreVeinsPositionalRandomFactory() {
        return oreVeinsPositionalRandomFactory;
    }

    public DensityFunction temperature() {
        return temperature;
    }

    public DensityFunction humidity() {
        return humidity;
    }

    public DensityFunction continents() {
        return continents;
    }

    public DensityFunction erosion() {
        return erosion;
    }

    public DensityFunction depth() {
        return depth;
    }

    public DensityFunction ridges() {
        return ridges;
    }

    public DensityFunction initialDensityWithoutJaggedness() {
        return initialDensityWithoutJaggedness;
    }

    public DensityFunction finalDensity() {
        return finalDensity;
    }

    public DensityFunction veinToggle() {
        return veinToggle;
    }

    public DensityFunction veinRidged() {
        return veinRidged;
    }

    public DensityFunction veinGap() {
        return veinGap;
    }
}

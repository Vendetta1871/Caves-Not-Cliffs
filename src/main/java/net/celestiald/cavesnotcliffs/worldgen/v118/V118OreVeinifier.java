package net.celestiald.cavesnotcliffs.worldgen.v118;

/** Exact Java 1.18.2 large copper/iron vein material selector. */
public final class V118OreVeinifier {
    private static final float VEININESS_THRESHOLD = 0.4F;
    private static final int EDGE_ROUNDOFF_BEGIN = 20;
    private static final double MAX_EDGE_ROUNDOFF = 0.2D;
    private static final float VEIN_SOLIDNESS = 0.7F;
    private static final float MIN_RICHNESS = 0.1F;
    private static final float MAX_RICHNESS = 0.3F;
    private static final float MAX_RICHNESS_THRESHOLD = 0.6F;
    private static final float CHANCE_OF_RAW_ORE_BLOCK = 0.02F;
    private static final float SKIP_ORE_IF_GAP_NOISE_IS_BELOW = -0.3F;

    private final DensityFunction veinToggle;
    private final DensityFunction veinRidged;
    private final DensityFunction veinGap;
    private final PositionalRandomFactory positionalRandomFactory;

    public V118OreVeinifier(DensityFunction veinToggle, DensityFunction veinRidged,
            DensityFunction veinGap, PositionalRandomFactory positionalRandomFactory) {
        this.veinToggle = requireNonNull(veinToggle, "veinToggle");
        this.veinRidged = requireNonNull(veinRidged, "veinRidged");
        this.veinGap = requireNonNull(veinGap, "veinGap");
        this.positionalRandomFactory = requireNonNull(positionalRandomFactory,
            "positionalRandomFactory");
    }

    /** Returns {@code null} when the default stone should remain unchanged. */
    public V118Material compute(int blockX, int blockY, int blockZ) {
        DensityFunction.SinglePointContext context =
            new DensityFunction.SinglePointContext(blockX, blockY, blockZ);
        double toggle = veinToggle.compute(context);
        VeinType type = toggle > 0.0D ? VeinType.COPPER : VeinType.IRON;
        double veininess = Math.abs(toggle);
        int distanceFromTop = type.maxY - blockY;
        int distanceFromBottom = blockY - type.minY;
        if (distanceFromBottom < 0 || distanceFromTop < 0) {
            return null;
        }

        int edgeDistance = Math.min(distanceFromTop, distanceFromBottom);
        double edgeRoundoff = clampedMap(edgeDistance, 0.0D, EDGE_ROUNDOFF_BEGIN,
            -MAX_EDGE_ROUNDOFF, 0.0D);
        if (veininess + edgeRoundoff < (double) VEININESS_THRESHOLD) {
            return null;
        }

        RandomSource random = positionalRandomFactory.at(blockX, blockY, blockZ);
        if (random.nextFloat() > VEIN_SOLIDNESS) {
            return null;
        }
        if (veinRidged.compute(context) >= 0.0D) {
            return null;
        }

        double richness = clampedMap(veininess, (double) VEININESS_THRESHOLD,
            (double) MAX_RICHNESS_THRESHOLD, (double) MIN_RICHNESS,
            (double) MAX_RICHNESS);
        if ((double) random.nextFloat() < richness
                && veinGap.compute(context) > (double) SKIP_ORE_IF_GAP_NOISE_IS_BELOW) {
            return random.nextFloat() < CHANCE_OF_RAW_ORE_BLOCK
                ? type.rawOreBlock : type.ore;
        }
        return type.filler;
    }

    private static double clampedMap(double value, double fromLow, double fromHigh,
            double toLow, double toHigh) {
        double delta = (value - fromLow) / (fromHigh - fromLow);
        return WorldgenMath.clampedLerp(toLow, toHigh, delta);
    }

    private static <T> T requireNonNull(T value, String name) {
        if (value == null) {
            throw new NullPointerException(name);
        }
        return value;
    }

    private enum VeinType {
        COPPER(V118Material.COPPER_ORE, V118Material.RAW_COPPER_BLOCK,
            V118Material.GRANITE, 0, 50),
        IRON(V118Material.DEEPSLATE_IRON_ORE, V118Material.RAW_IRON_BLOCK,
            V118Material.TUFF, -60, -8);

        private final V118Material ore;
        private final V118Material rawOreBlock;
        private final V118Material filler;
        private final int minY;
        private final int maxY;

        VeinType(V118Material ore, V118Material rawOreBlock, V118Material filler,
                int minY, int maxY) {
            this.ore = ore;
            this.rawOreBlock = rawOreBlock;
            this.filler = filler;
            this.minY = minY;
            this.maxY = maxY;
        }
    }
}

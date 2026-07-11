package net.celestiald.cavesnotcliffs.content;

import java.util.Random;

/** Deterministic Java 1.18.2 lightning deoxidation walk, independent of World. */
public final class CopperLightning {
    public interface Access {
        String blockPathAt(int x, int y, int z);

        void replace(int x, int y, int z, String path);

        void emitCopperParticles(int x, int y, int z);
    }

    public static final class Result {
        private final int blocksChanged;
        private final int particleEvents;

        Result(int blocksChanged, int particleEvents) {
            this.blocksChanged = blocksChanged;
            this.particleEvents = particleEvents;
        }

        public int getBlocksChanged() {
            return blocksChanged;
        }

        public int getParticleEvents() {
            return particleEvents;
        }
    }

    private CopperLightning() {
    }

    /**
     * Cleans the directly struck copper to its first stage, then performs 3-5 walks of 1-8 steps.
     * Each step tries exactly ten random positions in the surrounding 3x3x3 cube.
     */
    public static Result clean(Access access, int targetX, int targetY, int targetZ,
            Random random) {
        CopperWeathering.Variant directlyHit = variant(access, targetX, targetY, targetZ);
        if (!isWeatheringCopper(directlyHit)) {
            return new Result(0, 0);
        }

        int changed = 0;
        CopperWeathering.Variant first = CopperWeathering.counterpart(directlyHit,
                CopperWeathering.Stage.UNAFFECTED, false);
        if (first != directlyHit) {
            access.replace(targetX, targetY, targetZ, first.getPath());
            changed++;
        }

        int particles = 0;
        int walks = random.nextInt(3) + 3;
        for (int walk = 0; walk < walks; walk++) {
            int length = random.nextInt(8) + 1;
            int x = targetX;
            int y = targetY;
            int z = targetZ;
            for (int step = 0; step < length; step++) {
                boolean found = false;
                for (int attempt = 0; attempt < 10; attempt++) {
                    int candidateX = x + random.nextInt(3) - 1;
                    int candidateY = y + random.nextInt(3) - 1;
                    int candidateZ = z + random.nextInt(3) - 1;
                    CopperWeathering.Variant candidate =
                            variant(access, candidateX, candidateY, candidateZ);
                    if (!isWeatheringCopper(candidate)) {
                        continue;
                    }

                    CopperWeathering.Variant previous = CopperWeathering.previous(candidate);
                    if (previous != null) {
                        access.replace(candidateX, candidateY, candidateZ, previous.getPath());
                        changed++;
                    }
                    access.emitCopperParticles(candidateX, candidateY, candidateZ);
                    particles++;
                    x = candidateX;
                    y = candidateY;
                    z = candidateZ;
                    found = true;
                    break;
                }
                if (!found) {
                    break;
                }
            }
        }
        return new Result(changed, particles);
    }

    private static CopperWeathering.Variant variant(Access access, int x, int y, int z) {
        return CopperWeathering.variant(access.blockPathAt(x, y, z));
    }

    private static boolean isWeatheringCopper(CopperWeathering.Variant variant) {
        return variant != null && !variant.isWaxed();
    }
}

package net.celestiald.cavesnotcliffs.registry;

/**
 * Request-order-independent content migration core shared by vanilla chunks, CubicChunks cubes,
 * and save fixtures.
 */
public final class LegacyChunkMigration {
    public static final String LEGACY_GEODE_PATH = "amethyst_geode";
    public static final String AMETHYST_BLOCK_PATH = "amethyst_block";
    public static final String BUDDING_AMETHYST_PATH = "budding_amethyst";

    private static final int BUDDING_DENOMINATOR = 12;

    private LegacyChunkMigration() {
    }

    public interface Volume {
        /** Returns a Caves Not Cliffs registry path at the position, or {@code null}. */
        String blockPathAt(int x, int y, int z);

        boolean hasTarget(String registryPath);

        /** Replaces the position with the target's default state and reports success. */
        boolean replace(int x, int y, int z, String targetRegistryPath);
    }

    public static final class Bounds {
        public final int minX;
        public final int minY;
        public final int minZ;
        public final int sizeX;
        public final int sizeY;
        public final int sizeZ;

        public Bounds(int minX, int minY, int minZ, int sizeX, int sizeY, int sizeZ) {
            if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
                throw new IllegalArgumentException("Migration bounds must have positive sizes");
            }
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
        }
    }

    public static final class Result {
        private final int previousVersion;
        private final int resultingVersion;
        private final int convertedBlocks;
        private final int deferredBlocks;

        private Result(int previousVersion, int resultingVersion,
                int convertedBlocks, int deferredBlocks) {
            this.previousVersion = previousVersion;
            this.resultingVersion = resultingVersion;
            this.convertedBlocks = convertedBlocks;
            this.deferredBlocks = deferredBlocks;
        }

        public int getPreviousVersion() {
            return previousVersion;
        }

        public int getResultingVersion() {
            return resultingVersion;
        }

        public int getConvertedBlocks() {
            return convertedBlocks;
        }

        public int getDeferredBlocks() {
            return deferredBlocks;
        }

        public boolean isComplete() {
            return resultingVersion >= CncDataVersions.CANONICAL_REGISTRY_CONTENT_VERSION;
        }
    }

    public static Result migrate(int previousVersion, long worldSeed, Bounds bounds, Volume volume) {
        if (previousVersion >= CncDataVersions.CANONICAL_REGISTRY_CONTENT_VERSION) {
            return new Result(previousVersion, previousVersion, 0, 0);
        }

        int converted = 0;
        int deferred = 0;
        boolean regularAvailable = volume.hasTarget(AMETHYST_BLOCK_PATH);
        boolean buddingAvailable = volume.hasTarget(BUDDING_AMETHYST_PATH);

        for (int x = bounds.minX; x < bounds.minX + bounds.sizeX; x++) {
            for (int y = bounds.minY; y < bounds.minY + bounds.sizeY; y++) {
                for (int z = bounds.minZ; z < bounds.minZ + bounds.sizeZ; z++) {
                    if (!LEGACY_GEODE_PATH.equals(volume.blockPathAt(x, y, z))) {
                        continue;
                    }

                    boolean budding = isBuddingCandidate(worldSeed, x, y, z);
                    String target = budding ? BUDDING_AMETHYST_PATH : AMETHYST_BLOCK_PATH;
                    boolean available = budding ? buddingAvailable : regularAvailable;
                    if (!available || !volume.replace(x, y, z, target)) {
                        deferred++;
                    } else {
                        converted++;
                    }
                }
            }
        }

        int resultingVersion = deferred == 0
                ? CncDataVersions.CANONICAL_REGISTRY_CONTENT_VERSION
                : previousVersion;
        return new Result(previousVersion, resultingVersion, converted, deferred);
    }

    public static boolean containsLegacyGeode(Bounds bounds, Volume volume) {
        for (int x = bounds.minX; x < bounds.minX + bounds.sizeX; x++) {
            for (int y = bounds.minY; y < bounds.minY + bounds.sizeY; y++) {
                for (int z = bounds.minZ; z < bounds.minZ + bounds.sizeZ; z++) {
                    if (LEGACY_GEODE_PATH.equals(volume.blockPathAt(x, y, z))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Stable one-in-twelve selection with no dependence on traversal or chunk request order. */
    public static boolean isBuddingCandidate(long worldSeed, int x, int y, int z) {
        long hash = worldSeed;
        hash ^= (long) x * 0x9E3779B97F4A7C15L;
        hash ^= (long) y * 0xC2B2AE3D27D4EB4FL;
        hash ^= (long) z * 0x165667B19E3779F9L;
        hash = mix64(hash);
        return Math.floorMod(hash, BUDDING_DENOMINATOR) == 0;
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}

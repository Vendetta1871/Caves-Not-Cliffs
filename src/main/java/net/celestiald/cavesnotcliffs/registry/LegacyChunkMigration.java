package net.celestiald.cavesnotcliffs.registry;

import net.celestiald.cavesnotcliffs.content.LushCaveMechanics;
import net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics;
import net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics.Thickness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Request-order-independent content migration core shared by vanilla chunks, CubicChunks cubes,
 * and save fixtures.
 */
public final class LegacyChunkMigration {
    public static final String LEGACY_GEODE_PATH = "amethyst_geode";
    public static final String AMETHYST_BLOCK_PATH = "amethyst_block";
    public static final String BUDDING_AMETHYST_PATH = "budding_amethyst";
    public static final String POINTED_DRIPSTONE_PATH = "pointed_dripstone";

    private static final int BUDDING_DENOMINATOR = 12;
    private static final Set<String> LEGACY_POINTED_PATHS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                    "stalactite", "bottom_stalactite", "middle_stalactite", "top_stalactite",
                    "stalagmite", "bottom_stalagmite", "middle_stalagmite", "top_stalagmite")));
    private static final String LEGACY_GLOWING_VINE = "glow_berry_vines";
    private static final String LEGACY_PLAIN_VINE = "glow_berry_middle_fill";
    private static final String LEGACY_SMALL_DRIPLEAF = "baby_dripleaf";
    private static final String LEGACY_BIG_DRIPLEAF_STEM = "dripleaf_stem";
    private static final String LEGACY_BIG_DRIPLEAF_PARTIAL = "dripleafplant_1";
    private static final String LEGACY_BIG_DRIPLEAF_FULL = "dripleaf_plant_2";
    private static final int NORTH_HORIZONTAL_INDEX = 2;

    private LegacyChunkMigration() {
    }

    public interface Volume {
        /** Returns a Caves Not Cliffs registry path at the position, or {@code null}. */
        String blockPathAt(int x, int y, int z);

        boolean hasTarget(String registryPath);

        /** Metadata is needed only for state-split v2 draft blocks. */
        default int blockMetadataAt(int x, int y, int z) {
            return 0;
        }

        default boolean isAirAt(int x, int y, int z) {
            return blockPathAt(x, y, z) == null;
        }

        /** Replaces the position with the target's default state and reports success. */
        boolean replace(int x, int y, int z, String targetRegistryPath);

        /** Replaces or creates a position with an exact target metadata state. */
        default boolean replace(int x, int y, int z, String targetRegistryPath,
                int metadata) {
            return metadata == 0 && replace(x, y, z, targetRegistryPath);
        }

        /** Compatibility alias retained for the pointed-dripstone migration contract. */
        default boolean replaceState(int x, int y, int z, String targetRegistryPath,
                int metadata) {
            return replace(x, y, z, targetRegistryPath, metadata);
        }
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
            return resultingVersion >= CncDataVersions.CURRENT_CONTENT_VERSION;
        }
    }

    public static Result migrate(int previousVersion, long worldSeed, Bounds bounds,
            Volume volume) {
        if (previousVersion >= CncDataVersions.CURRENT_CONTENT_VERSION) {
            return new Result(previousVersion, previousVersion, 0, 0);
        }

        int converted = 0;
        int deferred = 0;
        int resultingVersion = previousVersion;

        if (resultingVersion < CncDataVersions.CANONICAL_REGISTRY_CONTENT_VERSION) {
            int[] geode = migrateGeodes(worldSeed, bounds, volume);
            converted += geode[0];
            deferred += geode[1];
            if (geode[1] > 0) {
                return new Result(previousVersion, resultingVersion, converted, deferred);
            }
            resultingVersion = CncDataVersions.CANONICAL_REGISTRY_CONTENT_VERSION;
        }

        if (resultingVersion < CncDataVersions.POINTED_DRIPSTONE_CONTENT_VERSION) {
            int[] pointed = migratePointedDripstone(bounds, volume);
            converted += pointed[0];
            deferred += pointed[1];
            if (pointed[1] > 0) {
                return new Result(previousVersion, resultingVersion, converted, deferred);
            }
            resultingVersion = CncDataVersions.POINTED_DRIPSTONE_CONTENT_VERSION;
        }

        if (resultingVersion < CncDataVersions.LUSH_CAVE_CONTENT_VERSION) {
            int[] lush = migrateLushStates(bounds, volume);
            converted += lush[0];
            deferred += lush[1];
            if (lush[1] == 0) {
                resultingVersion = CncDataVersions.LUSH_CAVE_CONTENT_VERSION;
            }
        }
        return new Result(previousVersion, resultingVersion, converted, deferred);
    }

    private static int[] migrateGeodes(long worldSeed, Bounds bounds, Volume volume) {
        int converted = 0;
        int deferred = 0;
        for (int x = bounds.minX; x < bounds.minX + bounds.sizeX; x++) {
            for (int y = bounds.minY; y < bounds.minY + bounds.sizeY; y++) {
                for (int z = bounds.minZ; z < bounds.minZ + bounds.sizeZ; z++) {
                    if (!LEGACY_GEODE_PATH.equals(volume.blockPathAt(x, y, z))) {
                        continue;
                    }
                    boolean budding = isBuddingCandidate(worldSeed, x, y, z);
                    String target = budding ? BUDDING_AMETHYST_PATH : AMETHYST_BLOCK_PATH;
                    if (!volume.hasTarget(target) || !volume.replace(x, y, z, target)) {
                        deferred++;
                    } else {
                        converted++;
                    }
                }
            }
        }
        return new int[]{converted, deferred};
    }

    private static int[] migratePointedDripstone(Bounds bounds, Volume volume) {
        List<Conversion> conversions = collectPointedConversions(bounds, volume);
        if (!conversions.isEmpty() && !volume.hasTarget(POINTED_DRIPSTONE_PATH)) {
            return new int[]{0, conversions.size()};
        }
        int converted = 0;
        int deferred = 0;
        for (Conversion conversion : conversions) {
            if (volume.replaceState(conversion.x, conversion.y, conversion.z,
                    POINTED_DRIPSTONE_PATH, conversion.metadata)) {
                converted++;
            } else {
                deferred++;
            }
        }
        return new int[]{converted, deferred};
    }

    private static List<Conversion> collectPointedConversions(Bounds bounds, Volume volume) {
        List<Conversion> conversions = new ArrayList<>();
        for (int x = bounds.minX; x < bounds.minX + bounds.sizeX; x++) {
            for (int y = bounds.minY; y < bounds.minY + bounds.sizeY; y++) {
                for (int z = bounds.minZ; z < bounds.minZ + bounds.sizeZ; z++) {
                    String path = volume.blockPathAt(x, y, z);
                    if (isLegacyPointedDripstone(path)) {
                        conversions.add(new Conversion(x, y, z,
                                metadataForLegacySegment(path, x, y, z, bounds, volume)));
                    }
                }
            }
        }
        return conversions;
    }

    public static int metadataForLegacySegment(String path, int x, int y, int z,
            Volume volume) {
        return metadataForLegacySegment(path, x, y, z, null, volume);
    }

    private static int metadataForLegacySegment(String path, int x, int y, int z,
            Bounds bounds, Volume volume) {
        if ("stalactite".equals(path) || "top_stalactite".equals(path)) {
            return PointedDripstoneMechanics.metadata(false, Thickness.TIP);
        }
        if ("middle_stalactite".equals(path)) {
            return PointedDripstoneMechanics.metadata(false, Thickness.MIDDLE);
        }
        if ("bottom_stalactite".equals(path)) {
            String forward = inside(bounds, x, y - 1, z)
                    ? volume.blockPathAt(x, y - 1, z) : null;
            Thickness thickness = "top_stalactite".equals(forward)
                    || "stalactite".equals(forward) ? Thickness.FRUSTUM : Thickness.BASE;
            return PointedDripstoneMechanics.metadata(false, thickness);
        }
        if ("stalagmite".equals(path) || "top_stalagmite".equals(path)) {
            return PointedDripstoneMechanics.metadata(true, Thickness.TIP);
        }
        if ("middle_stalagmite".equals(path)) {
            return PointedDripstoneMechanics.metadata(true, Thickness.MIDDLE);
        }
        if ("bottom_stalagmite".equals(path)) {
            String forward = inside(bounds, x, y + 1, z)
                    ? volume.blockPathAt(x, y + 1, z) : null;
            Thickness thickness = "top_stalagmite".equals(forward)
                    || "stalagmite".equals(forward) ? Thickness.FRUSTUM : Thickness.BASE;
            return PointedDripstoneMechanics.metadata(true, thickness);
        }
        throw new IllegalArgumentException("Not a legacy pointed-dripstone segment: " + path);
    }

    private static int[] migrateLushStates(Bounds bounds, Volume volume) {
        int converted = 0;
        int deferred = 0;
        for (int x = bounds.minX; x < bounds.minX + bounds.sizeX; x++) {
            for (int y = bounds.minY; y < bounds.minY + bounds.sizeY; y++) {
                for (int z = bounds.minZ; z < bounds.minZ + bounds.sizeZ; z++) {
                    String source = volume.blockPathAt(x, y, z);
                    Target target = lushTarget(source, x, y, z, volume);
                    if (target == null) {
                        continue;
                    }
                    if (!volume.hasTarget(target.path)
                            || !volume.replaceState(x, y, z, target.path, target.metadata)) {
                        deferred++;
                        continue;
                    }
                    converted++;
                    if (LEGACY_SMALL_DRIPLEAF.equals(source)
                            && y + 1 < bounds.minY + bounds.sizeY
                            && volume.isAirAt(x, y + 1, z)) {
                        int upperMeta = LushCaveMechanics.smallDripleafMeta(
                                NORTH_HORIZONTAL_INDEX, true, false);
                        if (volume.replaceState(x, y + 1, z, "small_dripleaf", upperMeta)) {
                            converted++;
                        } else {
                            deferred++;
                        }
                    }
                }
            }
        }
        return new int[]{converted, deferred};
    }

    private static Target lushTarget(String source, int x, int y, int z, Volume volume) {
        if (LEGACY_GLOWING_VINE.equals(source) || LEGACY_PLAIN_VINE.equals(source)) {
            boolean berries = LEGACY_GLOWING_VINE.equals(source);
            String below = volume.blockPathAt(x, y - 1, z);
            boolean body = LEGACY_GLOWING_VINE.equals(below)
                    || LEGACY_PLAIN_VINE.equals(below)
                    || LushCaveMechanics.isCaveVine(below);
            if (body) {
                return new Target(LushCaveMechanics.CAVE_VINES_PLANT,
                        LushCaveMechanics.caveVinePlantMeta(berries));
            }
            int age = LushCaveMechanics.CAVE_VINE_MAX_AGE;
            return new Target(LushCaveMechanics.caveVineHeadPath(age),
                    LushCaveMechanics.caveVineHeadMeta(age, berries));
        }
        if (LEGACY_SMALL_DRIPLEAF.equals(source)) {
            return new Target("small_dripleaf", LushCaveMechanics.smallDripleafMeta(
                    NORTH_HORIZONTAL_INDEX, false, false));
        }
        if (LEGACY_BIG_DRIPLEAF_STEM.equals(source)) {
            return new Target("big_dripleaf_stem",
                    LushCaveMechanics.bigDripleafStemMeta(NORTH_HORIZONTAL_INDEX, false));
        }
        if (LEGACY_BIG_DRIPLEAF_PARTIAL.equals(source)) {
            return new Target("big_dripleaf", LushCaveMechanics.bigDripleafMeta(
                    NORTH_HORIZONTAL_INDEX, LushCaveMechanics.Tilt.PARTIAL));
        }
        if (LEGACY_BIG_DRIPLEAF_FULL.equals(source)) {
            return new Target("big_dripleaf", LushCaveMechanics.bigDripleafMeta(
                    NORTH_HORIZONTAL_INDEX, LushCaveMechanics.Tilt.FULL));
        }
        return null;
    }

    private static boolean inside(Bounds bounds, int x, int y, int z) {
        return bounds == null || x >= bounds.minX && x < bounds.minX + bounds.sizeX
                && y >= bounds.minY && y < bounds.minY + bounds.sizeY
                && z >= bounds.minZ && z < bounds.minZ + bounds.sizeZ;
    }

    public static boolean isLegacyPointedDripstone(String path) {
        return LEGACY_POINTED_PATHS.contains(path);
    }

    public static boolean containsLegacyContent(Bounds bounds, Volume volume) {
        for (int x = bounds.minX; x < bounds.minX + bounds.sizeX; x++) {
            for (int y = bounds.minY; y < bounds.minY + bounds.sizeY; y++) {
                for (int z = bounds.minZ; z < bounds.minZ + bounds.sizeZ; z++) {
                    String path = volume.blockPathAt(x, y, z);
                    if (LEGACY_GEODE_PATH.equals(path) || isLegacyPointedDripstone(path)
                            || isLegacyLushState(path)) {
                        return true;
                    }
                }
            }
        }
        return false;
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

    public static boolean containsLegacyLushState(Bounds bounds, Volume volume) {
        for (int x = bounds.minX; x < bounds.minX + bounds.sizeX; x++) {
            for (int y = bounds.minY; y < bounds.minY + bounds.sizeY; y++) {
                for (int z = bounds.minZ; z < bounds.minZ + bounds.sizeZ; z++) {
                    if (isLegacyLushState(volume.blockPathAt(x, y, z))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isLegacyLushState(String path) {
        return LEGACY_GLOWING_VINE.equals(path) || LEGACY_PLAIN_VINE.equals(path)
                || LEGACY_SMALL_DRIPLEAF.equals(path) || LEGACY_BIG_DRIPLEAF_STEM.equals(path)
                || LEGACY_BIG_DRIPLEAF_PARTIAL.equals(path) || LEGACY_BIG_DRIPLEAF_FULL.equals(path);
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

    private static final class Conversion {
        private final int x;
        private final int y;
        private final int z;
        private final int metadata;

        private Conversion(int x, int y, int z, int metadata) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.metadata = metadata;
        }
    }

    private static final class Target {
        private final String path;
        private final int metadata;

        private Target(String path, int metadata) {
            this.path = path;
            this.metadata = metadata;
        }
    }
}

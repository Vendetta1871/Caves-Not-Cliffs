package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118DripstoneFeature.Direction;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118DripstoneFeature.State;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118DripstoneFeature.Thickness;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118DripstoneFeature.WorldAccess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** End-to-end comparison with the official mapped Java 1.18.2 dripstone features. */
public class V118DripstoneDecorationOracleTest {
    private static final String FIXTURE = "/net/celestiald/cavesnotcliffs/worldgen/v118/"
        + "dripstone-decoration-oracle-1.18.2.tsv";

    @Test
    public void placementFeatureRngAndFinalStatesMatchOfficialServer()
            throws IOException, NoSuchAlgorithmException {
        InputStream input = getClass().getResourceAsStream(FIXTURE);
        assertNotNull(FIXTURE, input);
        int cases = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input,
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] fields = line.split("\t", -1);
                long seed = Long.parseLong(fields[1]);
                int chunkX = Integer.parseInt(fields[2]);
                int chunkZ = Integer.parseInt(fields[3]);
                SparseWorld world = new SparseWorld(chunkX, chunkZ);
                V118DripstonePlacements.PlacementResult large =
                    V118DripstonePlacements.decorateLarge(world, seed, chunkX, chunkZ,
                        Collections.singleton(V118Biome.DRIPSTONE_CAVES));
                V118WorldgenRandom random = new V118WorldgenRandom(0L);
                long decorationSeed = random.setDecorationSeed(seed,
                    chunkX << 4, chunkZ << 4);
                V118DripstonePlacements.FeatureResult clusters =
                    V118DripstonePlacements.decorateClusters(world, decorationSeed,
                        chunkX, chunkZ, random);
                V118DripstonePlacements.FeatureResult pointed =
                    V118DripstonePlacements.decoratePointed(world, decorationSeed,
                        chunkX, chunkZ, random);
                Summary summary = world.summary();
                String flags = (large.placed() > 0) + ","
                    + (clusters.placed() > 0) + "," + (pointed.placed() > 0);
                String context = line + "\nactual\t" + flags + '\t' + world.setCalls
                    + '\t' + summary.total + '\t' + summary.counts + '\t' + summary.digest;
                assertEquals(context, fields[4], flags);
                assertEquals(context, Integer.parseInt(fields[5]), world.setCalls);
                assertEquals(context, Integer.parseInt(fields[6]), summary.total);
                assertEquals(context, fields[7], summary.counts);
                assertEquals(context, fields[8], summary.digest);
                ++cases;
            }
        }
        assertEquals(6, cases);
    }

    @Test
    public void configuredConstantsRemainPinnedToTheOfficialCatalog() {
        assertEquals(30, V118LargeDripstoneFeature.FLOOR_TO_CEILING_SEARCH_RANGE);
        assertEquals(12, V118DripstoneClusterFeature.FLOOR_TO_CEILING_SEARCH_RANGE);
        assertEquals(12, V118PointedDripstoneFeature.ENVIRONMENT_SCAN_STEPS);
        assertEquals(2, V118DripstonePlacements.LOCAL_MODIFICATIONS_STEP);
        assertEquals(3, V118DripstonePlacements.LARGE_DRIPSTONE_INDEX);
        assertEquals(7, V118DripstonePlacements.UNDERGROUND_DECORATION_STEP);
        assertEquals(0, V118DripstonePlacements.DRIPSTONE_CLUSTER_INDEX);
        assertEquals(1, V118DripstonePlacements.POINTED_DRIPSTONE_INDEX);
        assertEquals(0.011377778F,
            net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics.GROWTH_CHANCE,
            0.0F);
        assertTrue(V118LargeDripstoneFeature.getDripstoneHeight(
            0.0D, 8.0D, 1.0D, 0.6D) > 0.0D);
    }

    private static final class SparseWorld implements WorldAccess {
        private final Map<String, PlacedState> overrides = new HashMap<String, PlacedState>();
        private final int minBlockX;
        private final int maxBlockX;
        private final int minBlockZ;
        private final int maxBlockZ;
        private int setCalls;

        SparseWorld(int chunkX, int chunkZ) {
            minBlockX = (chunkX - 2) << 4;
            maxBlockX = ((chunkX + 3) << 4) - 1;
            minBlockZ = (chunkZ - 2) << 4;
            maxBlockZ = ((chunkZ + 3) << 4) - 1;
        }

        @Override
        public State getState(int x, int y, int z) {
            return state(x, y, z).state;
        }

        @Override
        public boolean isWaterAt(int x, int y, int z) {
            return state(x, y, z).waterlogged;
        }

        @Override
        public void setDripstoneBlock(int x, int y, int z) {
            set(x, y, z, PlacedState.DRIPSTONE);
        }

        @Override
        public void setPointedDripstone(int x, int y, int z, Direction direction,
                Thickness thickness, boolean waterlogged) {
            set(x, y, z, PlacedState.pointed(direction, thickness, waterlogged));
        }

        @Override
        public void setWater(int x, int y, int z) {
            set(x, y, z, PlacedState.WATER);
        }

        @Override
        public int worldSurfaceHeight(int x, int z) {
            return 320;
        }

        @Override
        public V118Biome biomeAt(int x, int y, int z) {
            return V118Biome.DRIPSTONE_CAVES;
        }

        @Override
        public int minBuildHeight() {
            return -64;
        }

        @Override
        public int maxBuildHeight() {
            return 320;
        }

        Summary summary() throws NoSuchAlgorithmException {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            TreeMap<String, Integer> counts = new TreeMap<String, Integer>();
            int total = 0;
            for (int x = minBlockX; x <= maxBlockX; ++x) {
                for (int y = -64; y < 320; ++y) {
                    for (int z = minBlockZ; z <= maxBlockZ; ++z) {
                        PlacedState state = state(x, y, z);
                        if (state.equals(baseline(x, y, z))) {
                            continue;
                        }
                        Integer count = counts.get(state.encoded);
                        counts.put(state.encoded, count == null ? 1 : count + 1);
                        digest.update((x + "," + y + "," + z + "," + state.encoded + "\n")
                            .getBytes(StandardCharsets.UTF_8));
                        ++total;
                    }
                }
            }
            StringBuilder encodedCounts = new StringBuilder();
            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                if (encodedCounts.length() > 0) {
                    encodedCounts.append(';');
                }
                encodedCounts.append(entry.getKey()).append('=').append(entry.getValue());
            }
            return new Summary(total, encodedCounts.toString(), hex(digest.digest()));
        }

        private void set(int x, int y, int z, PlacedState state) {
            ++setCalls;
            if (!contains(x, y, z)) {
                return;
            }
            overrides.put(key(x, y, z), state);
        }

        private PlacedState state(int x, int y, int z) {
            if (!contains(x, y, z)) {
                return PlacedState.AIR;
            }
            PlacedState state = overrides.get(key(x, y, z));
            return state == null ? baseline(x, y, z) : state;
        }

        private boolean contains(int x, int y, int z) {
            return x >= minBlockX && x <= maxBlockX && z >= minBlockZ && z <= maxBlockZ
                && y >= -64 && y < 320;
        }

        private static PlacedState baseline(int x, int y, int z) {
            int phase = Math.floorMod(y + 64, 32);
            if (phase >= 5 && phase <= 12) {
                long hash = (long) x * 73428767L ^ (long) z * 42317861L
                    ^ (long) (y >> 5) * 912931L;
                return phase <= 6 && (hash & 7L) == 0L
                    ? PlacedState.WATER : PlacedState.AIR;
            }
            return PlacedState.DRIPSTONE;
        }

        private static String key(int x, int y, int z) {
            return x + ":" + y + ":" + z;
        }

        private static String hex(byte[] bytes) {
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                result.append(String.format("%02x", value & 255));
            }
            return result.toString();
        }
    }

    private static final class PlacedState {
        private static final PlacedState AIR = new PlacedState(State.AIR, false, "air");
        private static final PlacedState WATER = new PlacedState(State.WATER, true, "water");
        private static final PlacedState DRIPSTONE =
            new PlacedState(State.DRIPSTONE_BLOCK, false, "dripstone_block");

        private final State state;
        private final boolean waterlogged;
        private final String encoded;

        private PlacedState(State state, boolean waterlogged, String encoded) {
            this.state = state;
            this.waterlogged = waterlogged;
            this.encoded = encoded;
        }

        private static PlacedState pointed(Direction direction, Thickness thickness,
                boolean waterlogged) {
            return new PlacedState(State.POINTED_DRIPSTONE, waterlogged,
                "pointed_dripstone[" + direction.name().toLowerCase() + ','
                    + thickness.name().toLowerCase() + ',' + waterlogged + ']');
        }

        @Override
        public boolean equals(Object value) {
            return value instanceof PlacedState
                && encoded.equals(((PlacedState) value).encoded);
        }

        @Override
        public int hashCode() {
            return encoded.hashCode();
        }
    }

    private static final class Summary {
        private final int total;
        private final String counts;
        private final String digest;

        Summary(int total, String counts, String digest) {
            this.total = total;
            this.counts = counts;
            this.digest = digest;
        }
    }
}

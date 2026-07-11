package net.celestiald.cavesnotcliffs.worldgen.v118;

import net.celestiald.cavesnotcliffs.content.AzaleaTreeFeature;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118LushCaveFeature.Block;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118LushCaveFeature.Direction;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118LushCaveFeature.State;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118LushCaveFeature.WorldAccess;
import net.minecraft.util.math.BlockPos;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/** End-to-end comparison with all seven official Java 1.18.2 lush cave features. */
public class V118LushCaveDecorationOracleTest {
    private static final String FIXTURE = "/net/celestiald/cavesnotcliffs/worldgen/v118/"
        + "lush-cave-decoration-oracle-1.18.2.tsv";

    @Test
    public void placementConfiguredFeatureRngAndFinalStatesMatchOfficialServer()
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
                V118LushCavePlacements.DecorationResult result =
                    V118LushCavePlacements.decorate(world, seed, chunkX, chunkZ,
                        Collections.singleton(V118Biome.LUSH_CAVES));
                Summary summary = world.summary();
                String context = line + "\nactual\t" + result.attempts() + '\t'
                    + result.placed() + '\t' + summary.total + '\t' + summary.counts
                    + '\t' + summary.digest;
                assertEquals(context, Integer.parseInt(fields[5]), summary.total);
                assertEquals(context, fields[6], summary.counts);
                assertEquals(context, fields[7], summary.digest);
                ++cases;
            }
        }
        assertEquals(6, cases);
    }

    @Test
    public void nonLushBiomeUnionSkipsTheDecorationStep() {
        SparseWorld world = new SparseWorld(0, 0);
        V118LushCavePlacements.DecorationResult result =
            V118LushCavePlacements.decorate(world, 1L, 0, 0,
                Collections.singleton(V118Biome.PLAINS));
        assertEquals(0, result.attempts());
        assertEquals(0, result.placed());
        assertEquals(0, world.overrides.size());
    }

    private static final class SparseWorld implements WorldAccess {
        private final Map<String, PlacedState> overrides =
            new HashMap<String, PlacedState>();
        private final int minBlockX;
        private final int maxBlockX;
        private final int minBlockZ;
        private final int maxBlockZ;

        SparseWorld(int chunkX, int chunkZ) {
            minBlockX = (chunkX - 2) << 4;
            maxBlockX = ((chunkX + 3) << 4) - 1;
            minBlockZ = (chunkZ - 2) << 4;
            maxBlockZ = ((chunkZ + 3) << 4) - 1;
        }

        @Override
        public int minBuildHeight() {
            return -64;
        }

        @Override
        public int maxBuildHeight() {
            return 320;
        }

        @Override
        public State getState(int x, int y, int z) {
            return state(x, y, z).state;
        }

        @Override
        public void setState(int x, int y, int z, State state) {
            if (contains(x, y, z)) {
                overrides.put(key(x, y, z), PlacedState.fromFeature(state));
            }
        }

        @Override
        public boolean ensureCanWrite(int x, int y, int z) {
            return contains(x, y, z);
        }

        @Override
        public boolean isMossReplaceable(int x, int y, int z) {
            Block block = getState(x, y, z).block();
            return isBaseStone(block) || isDirt(block)
                || block == Block.CAVE_VINES_BODY || block == Block.CAVE_VINES_HEAD;
        }

        @Override
        public boolean isLushGroundReplaceable(int x, int y, int z) {
            Block block = getState(x, y, z).block();
            return isMossReplaceable(x, y, z) || block == Block.CLAY
                || block == Block.GRAVEL || block == Block.SAND;
        }

        @Override
        public boolean isAzaleaRootReplaceable(int x, int y, int z) {
            Block block = getState(x, y, z).block();
            return isBaseStone(block) || isDirt(block) || block == Block.CLAY
                || block == Block.GRAVEL || block == Block.SAND;
        }

        @Override
        public boolean isAzaleaGrowsOn(int x, int y, int z) {
            Block block = getState(x, y, z).block();
            return isDirt(block) || block == Block.SAND;
        }

        @Override
        public boolean isLeaves(int x, int y, int z) {
            return state(x, y, z).leaves;
        }

        @Override
        public boolean isReplaceablePlant(int x, int y, int z) {
            Block block = getState(x, y, z).block();
            return block == Block.GRASS || block == Block.TALL_GRASS
                || block == Block.VINE || block == Block.HANGING_ROOTS;
        }

        @Override
        public boolean isLavaAt(int x, int y, int z) {
            return getState(x, y, z).block() == Block.LAVA;
        }

        @Override
        public boolean isWaterAt(int x, int y, int z) {
            PlacedState state = state(x, y, z);
            return state.state.block() == Block.WATER || state.state.waterlogged();
        }

        @Override
        public boolean isSolid(int x, int y, int z) {
            return isSolidBlock(getState(x, y, z).block());
        }

        @Override
        public boolean hasSturdyFace(int x, int y, int z, Direction face) {
            return isSolid(x, y, z);
        }

        @Override
        public boolean canSurvive(State state, int x, int y, int z) {
            switch (state.block()) {
                case SPORE_BLOSSOM:
                case HANGING_ROOTS:
                    return hasSturdyFace(x, y + 1, z, Direction.DOWN);
                case SMALL_DRIPLEAF:
                    Block below = getState(x, y - 1, z).block();
                    return below == Block.CLAY || below == Block.MOSS_BLOCK;
                case AZALEA:
                case FLOWERING_AZALEA:
                case MOSS_CARPET:
                case GRASS:
                case TALL_GRASS:
                    return isSolid(x, y - 1, z);
                default:
                    return true;
            }
        }

        @Override
        public boolean isAcceptableVineNeighbor(int x, int y, int z,
                Direction attachmentDirection) {
            return isSolid(x, y, z);
        }

        @Override
        public boolean placeAzaleaTree(java.util.Random random, int x, int y, int z) {
            java.util.List<AzaleaTreeFeature.Placement> placements =
                AzaleaTreeFeature.plan(random, new BlockPos(x, y, z),
                    position -> treeReplaceable(position.getX(), position.getY(),
                        position.getZ()));
            if (placements.isEmpty()) {
                return false;
            }
            for (AzaleaTreeFeature.Placement placement : placements) {
                PlacedState state;
                switch (placement.kind) {
                    case OAK_LOG:
                        state = PlacedState.OAK_LOG;
                        break;
                    case FLOWERING_AZALEA_LEAVES:
                        state = PlacedState.leaves("flowering_azalea_leaves",
                            placement.distance);
                        break;
                    case AZALEA_LEAVES:
                        state = PlacedState.leaves("azalea_leaves", placement.distance);
                        break;
                    case ROOTED_DIRT:
                    default:
                        state = PlacedState.fromFeature(State.ROOTED_DIRT);
                        break;
                }
                if (contains(placement.position.getX(), placement.position.getY(),
                        placement.position.getZ())) {
                    overrides.put(key(placement.position.getX(), placement.position.getY(),
                        placement.position.getZ()), state);
                }
            }
            return true;
        }

        @Override
        public V118Biome biomeAt(int x, int y, int z) {
            return V118Biome.LUSH_CAVES;
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
                if (encodedCounts.length() > 0) encodedCounts.append(';');
                encodedCounts.append(entry.getKey()).append('=').append(entry.getValue());
            }
            return new Summary(total, encodedCounts.toString(), hex(digest.digest()));
        }

        private boolean treeReplaceable(int x, int y, int z) {
            PlacedState state = state(x, y, z);
            Block block = state.state.block();
            return block == Block.AIR || block == Block.WATER || state.leaves
                || isReplaceablePlant(x, y, z);
        }

        private PlacedState state(int x, int y, int z) {
            if (!contains(x, y, z)) return PlacedState.AIR;
            PlacedState state = overrides.get(key(x, y, z));
            return state == null ? baseline(x, y, z) : state;
        }

        private boolean contains(int x, int y, int z) {
            return x >= minBlockX && x <= maxBlockX && z >= minBlockZ && z <= maxBlockZ
                && y >= -64 && y < 320;
        }

        private static boolean isBaseStone(Block block) {
            return block == Block.STONE;
        }

        private static boolean isDirt(Block block) {
            return block == Block.DIRT || block == Block.ROOTED_DIRT
                || block == Block.MOSS_BLOCK;
        }

        private static boolean isSolidBlock(Block block) {
            return isBaseStone(block) || isDirt(block) || block == Block.CLAY
                || block == Block.GRAVEL || block == Block.SAND;
        }

        private static PlacedState baseline(int x, int y, int z) {
            int phase = Math.floorMod(y + 64, 32);
            return phase >= 8 && phase <= 19 ? PlacedState.AIR : PlacedState.DIRT;
        }

        private static String key(int x, int y, int z) {
            return x + ":" + y + ":" + z;
        }

        private static String hex(byte[] bytes) {
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) result.append(String.format("%02x", value & 255));
            return result.toString();
        }
    }

    private static final class PlacedState {
        private static final PlacedState AIR = new PlacedState(State.AIR, "air", false);
        private static final PlacedState DIRT = new PlacedState(State.DIRT, "dirt", false);
        private static final PlacedState OAK_LOG =
            new PlacedState(State.OTHER, "oak_log", false);

        private final State state;
        private final String encoded;
        private final boolean leaves;

        private PlacedState(State state, String encoded, boolean leaves) {
            this.state = state;
            this.encoded = encoded;
            this.leaves = leaves;
        }

        private static PlacedState fromFeature(State state) {
            String encoded = state.encode();
            if (state.block() == Block.CAVE_VINES_BODY) {
                encoded = "cave_vines_plant[berries=" + state.berries() + ']';
            } else if (state.block() == Block.CAVE_VINES_HEAD) {
                encoded = "cave_vines[age=" + state.age() + ",berries="
                    + state.berries() + ']';
            }
            return new PlacedState(state, encoded, false);
        }

        private static PlacedState leaves(String name, int distance) {
            return new PlacedState(State.OTHER, name + "[distance=" + distance + ']', true);
        }

        @Override
        public boolean equals(Object value) {
            return value instanceof PlacedState && encoded.equals(((PlacedState) value).encoded);
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

        private Summary(int total, String counts, String digest) {
            this.total = total;
            this.counts = counts;
            this.digest = digest;
        }
    }
}

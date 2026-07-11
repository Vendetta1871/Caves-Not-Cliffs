package net.celestiald.cavesnotcliffs.worldgen.v118;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118BeeTreeFeature.LogAxis;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118BeeTreePlacements.TreeKind;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Golden values captured by invoking Java 1.18.2's private TreeFeature#doPlace. */
public class V118BeeTreeFeatureOracleTest {
    private static final BlockPos ORIGIN = new BlockPos(-19, 64, 37);
    private static final Comparator<BlockPos> ORDER = Comparator
            .comparingInt(BlockPos::getY).thenComparingInt(BlockPos::getX)
            .thenComparingInt(BlockPos::getZ);

    @Test
    public void straightTreesMatchOfficialBlockAndRandomOracle() {
        check(TreeKind.OAK, 0L, 5, -7647015711054255213L,
                56, -2056514341809798751L, -1083761183081836303L);
        check(TreeKind.OAK, -1L, 7, 3990701465488447790L,
                54, 4862976368930344735L, -8129419283323851490L);
        check(TreeKind.OAK, 123456789L, 6, 4991378608106961084L,
                51, -8888063931945605888L, 4590096229867741023L);

        check(TreeKind.BIRCH, 1L, 6, 4991378608106961084L,
                57, 1338932600795504877L, -1160629452687687109L);
        check(TreeKind.BIRCH, Long.MAX_VALUE, 8, 1962920750768208731L,
                54, 4901630837233349393L, -8129419283323851490L);

        check(TreeKind.SUPER_BIRCH, 0L, 8, 1962920750768208731L,
                56, 9076540669098160317L, -1083761183081836303L);
        check(TreeKind.SUPER_BIRCH, 1L, 10, 1337413028906932428L,
                57, 7306084056147788761L, -1160629452687687109L);
        check(TreeKind.SUPER_BIRCH, -1L, 14, 5339104904250486876L,
                54, 3868838197059432637L, -8129419283323851490L);
    }

    @Test
    public void fancyOakMatchesOfficialLimbFoliageAndRandomOracle() {
        check(TreeKind.FANCY_OAK, 0L, 5, -7647015711054255213L,
                69, 3098140046243263259L, 4437113781045784766L);
        check(TreeKind.FANCY_OAK, 1L, 22, -461352875963563944L,
                318, -5963772327024915727L, -669528114487223426L);
        check(TreeKind.FANCY_OAK, -1L, 11, -2481458439702913003L,
                134, -412636288199905647L, -3206673117535979274L);
        check(TreeKind.FANCY_OAK, 123456789L, 5, -7647015711054255213L,
                70, 3734882206283871238L, 8429272609719263920L);
    }

    @Test
    public void fancyOakMinimumClippingAndExtendedBuildHeightMatchOracle() {
        FakeWorld clipped = worldAt(new BlockPos(4, 64, -9));
        clipped.states.put(new BlockPos(4, 71, -9), State.STONE);
        Random clippedRandom = new Random(11L);
        V118BeeTreeFeature.Result clippedResult = V118BeeTreeFeature.place(
                clipped, clippedRandom, new BlockPos(4, 64, -9), TreeKind.FANCY_OAK);
        assertTrue(clippedResult.placed());
        assertEquals(6, clippedResult.trunks().size());
        assertEquals(4079711282438944956L,
                hashTrunks(clippedResult.trunks(), clipped));
        assertEquals(70, clippedResult.foliage().size());
        assertEquals(4592781493516044970L,
                hashPositions(clippedResult.foliage()));
        assertEquals(7883164322401693319L, clippedRandom.nextLong());

        BlockPos highOrigin = new BlockPos(4, 310, -9);
        FakeWorld high = worldAt(highOrigin);
        Random highRandom = new Random(1L);
        V118BeeTreeFeature.Result highResult = V118BeeTreeFeature.place(
                high, highRandom, highOrigin, TreeKind.SUPER_BIRCH);
        assertTrue(highResult.placed());
        assertEquals(10, highResult.trunks().size());
        assertEquals(3268370212254813028L, hashTrunks(highResult.trunks(), high));
        assertEquals(57, highResult.foliage().size());
        assertEquals(-1503745761126124861L, hashPositions(highResult.foliage()));
        assertEquals(-1160629452687687109L, highRandom.nextLong());
    }

    private static void check(TreeKind kind, long seed, int trunkCount,
            long trunkHash, int foliageCount, long foliageHash, long trailingLong) {
        FakeWorld world = worldAt(ORIGIN);
        Random random = new Random(seed);
        V118BeeTreeFeature.Result result = V118BeeTreeFeature.place(
                world, random, ORIGIN, kind);
        assertTrue(result.placed());
        assertEquals(trunkCount, result.trunks().size());
        assertEquals(trunkHash, hashTrunks(result.trunks(), world));
        assertEquals(foliageCount, result.foliage().size());
        assertEquals(foliageHash, hashPositions(result.foliage()));
        assertEquals(trailingLong, random.nextLong());
    }

    private static FakeWorld worldAt(BlockPos origin) {
        FakeWorld world = new FakeWorld();
        for (int x = origin.getX() - 8; x <= origin.getX() + 8; ++x) {
            for (int z = origin.getZ() - 8; z <= origin.getZ() + 8; ++z) {
                world.states.put(new BlockPos(x, origin.getY() - 1, z), State.GRASS);
            }
        }
        return world;
    }

    private static long hashPositions(List<BlockPos> positions) {
        List<BlockPos> sorted = new ArrayList<>(positions);
        sorted.sort(ORDER);
        long hash = 0xcbf29ce484222325L;
        for (BlockPos pos : sorted) {
            hash ^= officialPackedPosition(pos);
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    private static long hashTrunks(List<BlockPos> positions, FakeWorld world) {
        List<BlockPos> sorted = new ArrayList<>(positions);
        sorted.sort(ORDER);
        long hash = 0xcbf29ce484222325L;
        for (BlockPos pos : sorted) {
            State state = world.states.get(pos);
            int marker = state == State.LOG_X ? 0 : state == State.LOG_Y ? 1
                    : state == State.LOG_Z ? 2 : 3;
            hash ^= officialPackedPosition(pos);
            hash *= 0x100000001b3L;
            hash ^= marker;
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    private static long officialPackedPosition(BlockPos pos) {
        return ((long) pos.getX() & 0x3FFFFFFL) << 38
                | ((long) pos.getZ() & 0x3FFFFFFL) << 12
                | (long) pos.getY() & 0xFFFL;
    }

    private enum State {
        AIR,
        GRASS,
        DIRT,
        STONE,
        LOG_X,
        LOG_Y,
        LOG_Z,
        LEAVES
    }

    private static final class FakeWorld implements V118BeeTreeFeature.WorldAccess {
        final Map<BlockPos, State> states = new HashMap<>();

        private State get(BlockPos pos) {
            State state = states.get(pos);
            return state == null ? State.AIR : state;
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
        public boolean isFree(BlockPos pos) {
            State state = get(pos);
            return state == State.AIR || state == State.LEAVES
                    || state == State.LOG_X || state == State.LOG_Y
                    || state == State.LOG_Z;
        }

        @Override
        public boolean isValidTreePos(BlockPos pos) {
            State state = get(pos);
            return state == State.AIR || state == State.LEAVES;
        }

        @Override
        public boolean isDirtExceptGrassAndMycelium(BlockPos pos) {
            return get(pos) == State.DIRT;
        }

        @Override
        public void setDirt(BlockPos pos) {
            states.put(pos.toImmutable(), State.DIRT);
        }

        @Override
        public void setLog(BlockPos pos, LogAxis axis, TreeKind kind) {
            states.put(pos.toImmutable(), axis == LogAxis.X ? State.LOG_X
                    : axis == LogAxis.Z ? State.LOG_Z : State.LOG_Y);
        }

        @Override
        public void setLeaves(BlockPos pos, TreeKind kind) {
            states.put(pos.toImmutable(), State.LEAVES);
        }
    }
}

package net.celestiald.cavesnotcliffs.content;

import net.minecraft.util.math.BlockPos;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AzaleaTreeFeatureTest {
    private static final long[] SEEDS = {
            0L, 1L, -1L, 123456789L, Long.MIN_VALUE, Long.MAX_VALUE
    };

    @Test
    public void plansAreDeterministicUniqueAndContainTheOfficialMaterialPalette() {
        boolean sawFlowering = false;
        for (long seed : SEEDS) {
            List<AzaleaTreeFeature.Placement> first = AzaleaTreeFeature.plan(
                    new Random(seed), new BlockPos(-17, 40, 23), pos -> true);
            List<AzaleaTreeFeature.Placement> second = AzaleaTreeFeature.plan(
                    new Random(seed), new BlockPos(-17, 40, 23), pos -> true);
            assertEquals(signature(first), signature(second));
            assertFalse(first.isEmpty());

            Set<BlockPos> unique = new HashSet<>();
            int roots = 0;
            int logs = 0;
            int leaves = 0;
            for (AzaleaTreeFeature.Placement placement : first) {
                assertTrue(unique.add(placement.position));
                switch (placement.kind) {
                    case ROOTED_DIRT:
                        roots++;
                        assertEquals(new BlockPos(-17, 39, 23), placement.position);
                        assertEquals(0, placement.distance);
                        break;
                    case OAK_LOG:
                        logs++;
                        assertEquals(0, placement.distance);
                        break;
                    case FLOWERING_AZALEA_LEAVES:
                        sawFlowering = true;
                        // fall through
                    case AZALEA_LEAVES:
                        leaves++;
                        assertTrue(placement.distance >= 1 && placement.distance <= 7);
                        break;
                    default:
                        throw new AssertionError(placement.kind);
                }
            }
            assertEquals(1, roots);
            assertTrue(logs >= 6 && logs <= 9);
            assertTrue(leaves >= 20);
        }
        assertTrue(sawFlowering);
    }

    @Test
    public void rejectsBlockedClearanceAndFiniteHeightOverflow() {
        BlockPos origin = new BlockPos(0, 40, 0);
        assertTrue(AzaleaTreeFeature.plan(new Random(0L), origin,
                pos -> !pos.equals(origin.up())).isEmpty());
        assertTrue(AzaleaTreeFeature.plan(new Random(0L),
                new BlockPos(0, 319, 0), pos -> true).isEmpty());
    }

    private static String signature(List<AzaleaTreeFeature.Placement> placements) {
        StringBuilder result = new StringBuilder();
        for (AzaleaTreeFeature.Placement placement : placements) {
            result.append(placement.position.toLong()).append(':')
                    .append(placement.kind.ordinal()).append(':')
                    .append(placement.distance).append(';');
        }
        return result.toString();
    }
}

package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.block.LushAzaleaBlocks;
import net.celestiald.cavesnotcliffs.block.LushCaveVinesBlock;
import net.celestiald.cavesnotcliffs.block.LushDripleafBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import org.junit.Test;
import org.junit.BeforeClass;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LushCaveRuntimeContractTest {
    @BeforeClass
    public static void bootstrapMinecraftRegistries() {
        net.minecraft.init.Bootstrap.register();
    }

    @Test
    public void runtimeCaveVineBandsRoundTripAllReachableMetadata() {
        int base = 0;
        for (int maxLocal : new int[]{7, 7, 7, 1}) {
            LushCaveVinesBlock.Head block = new LushCaveVinesBlock.Head(base, maxLocal);
            for (int local = 0; local <= maxLocal; local++) {
                for (boolean berries : new boolean[]{false, true}) {
                    int meta = local | (berries ? 8 : 0);
                    IBlockState state = block.getStateFromMeta(meta);
                    assertEquals(base + local, block.age(state));
                    assertEquals(berries,
                            state.getValue(LushCaveVinesBlock.BERRIES));
                    assertEquals(meta, block.getMetaFromState(state));
                }
            }
            base += maxLocal + 1;
        }
        assertEquals(26, base);
    }

    @Test
    public void runtimeDripleafBlocksRoundTripTheirCompleteMatrices() {
        LushDripleafBlocks.Small small = new LushDripleafBlocks.Small();
        LushDripleafBlocks.Head head = new LushDripleafBlocks.Head(false);
        LushDripleafBlocks.Stem stem = new LushDripleafBlocks.Stem();
        for (int meta = 0; meta < 16; meta++) {
            assertEquals(meta, small.getMetaFromState(small.getStateFromMeta(meta)));
            assertEquals(meta, head.getMetaFromState(head.getStateFromMeta(meta)));
        }
        for (int meta = 0; meta < 8; meta++) {
            assertEquals(meta, stem.getMetaFromState(stem.getStateFromMeta(meta)));
        }
    }

    @Test
    public void leavesRoundTripDistanceAndPersistence() {
        LushAzaleaBlocks.AzaleaLeaves leaves =
                new LushAzaleaBlocks.AzaleaLeaves(false);
        Set<Integer> states = new HashSet<>();
        for (int distance = 1; distance <= 7; distance++) {
            for (boolean persistent : new boolean[]{false, true}) {
                IBlockState state = leaves.getDefaultState()
                        .withProperty(LushAzaleaBlocks.AzaleaLeaves.DISTANCE, distance)
                        .withProperty(LushAzaleaBlocks.AzaleaLeaves.PERSISTENT, persistent);
                int meta = leaves.getMetaFromState(state);
                assertTrue(states.add(meta));
                assertEquals(state, leaves.getStateFromMeta(meta));
            }
        }
        assertEquals(14, states.size());
        assertFalse(leaves.isOpaqueCube(leaves.getDefaultState()));
    }

    @Test
    public void soundCatalogHasEveryUniqueCanonicalEvent() {
        Set<String> ids = new HashSet<>();
        LushCaveSounds.events().forEach(sound ->
                assertTrue(ids.add(sound.getSoundName().toString())));
        assertEquals(58, ids.size());
        assertTrue(ids.contains("cavesnotcliffs:block.cave_vines.pick_berries"));
        assertTrue(ids.contains("cavesnotcliffs:block.big_dripleaf.tilt_down"));
        assertTrue(ids.contains("cavesnotcliffs:block.big_dripleaf.tilt_up"));
    }
}

package net.celestiald.cavesnotcliffs.content;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class CopperWeatheringTest {
    @Test
    public void catalogContainsEveryShapeStageAndWaxStateExactlyOnce() {
        assertEquals(40, CopperWeathering.variants().size());
        assertEquals(40, new HashSet<>(CopperWeathering.variantsByPath().keySet()).size());
        assertEquals(32, CopperWeathering.variants().stream()
                .filter(CopperWeathering.Variant::hasPublicItem).count());

        for (boolean waxed : new boolean[]{false, true}) {
            for (CopperWeathering.Stage stage : CopperWeathering.Stage.values()) {
                List<CopperWeathering.Variant> stageVariants =
                        CopperWeathering.variantsByStage(waxed).get(stage);
                assertEquals(5, stageVariants.size());
                for (CopperWeathering.Shape shape : CopperWeathering.Shape.values()) {
                    String path = CopperWeathering.path(stage, shape, waxed);
                    CopperWeathering.Variant variant = CopperWeathering.variant(path);
                    assertEquals(stage, variant.getStage());
                    assertEquals(shape, variant.getShape());
                    assertEquals(waxed, variant.isWaxed());
                    assertEquals(shape != CopperWeathering.Shape.DOUBLE_SLAB,
                            variant.hasPublicItem());
                }
            }
        }
    }

    @Test
    public void canonicalPathsMatchJava1182() {
        assertEquals("copper_block", CopperWeathering.path(
                CopperWeathering.Stage.UNAFFECTED, CopperWeathering.Shape.BLOCK, false));
        assertEquals("exposed_copper", CopperWeathering.path(
                CopperWeathering.Stage.EXPOSED, CopperWeathering.Shape.BLOCK, false));
        assertEquals("waxed_weathered_cut_copper", CopperWeathering.path(
                CopperWeathering.Stage.WEATHERED, CopperWeathering.Shape.CUT, true));
        assertEquals("oxidized_cut_copper_stairs", CopperWeathering.path(
                CopperWeathering.Stage.OXIDIZED, CopperWeathering.Shape.STAIRS, false));
        assertEquals("waxed_cut_copper_slab_double", CopperWeathering.path(
                CopperWeathering.Stage.UNAFFECTED,
                CopperWeathering.Shape.DOUBLE_SLAB, true));
    }

    @Test
    public void deterministicMapsPreserveShapeAndStage() {
        for (CopperWeathering.Variant variant : CopperWeathering.variants()) {
            if (variant.isWaxed()) {
                assertNull(CopperWeathering.waxed(variant));
                CopperWeathering.Variant unwaxed = CopperWeathering.unwaxed(variant);
                assertEquals(variant.getStage(), unwaxed.getStage());
                assertEquals(variant.getShape(), unwaxed.getShape());
                assertFalse(unwaxed.isWaxed());
                assertSame(variant, CopperWeathering.waxed(unwaxed));
            } else {
                assertNull(CopperWeathering.unwaxed(variant));
                assertTrue(CopperWeathering.waxed(variant).isWaxed());
                assertEquals(variant.getShape(), CopperWeathering.waxed(variant).getShape());
                assertEquals(variant.getStage(), CopperWeathering.waxed(variant).getStage());
                assertEquals(variant.getStage().next() == null,
                        CopperWeathering.next(variant) == null);
                assertEquals(variant.getStage().previous() == null,
                        CopperWeathering.previous(variant) == null);
            }
        }
    }

    @Test
    public void axeMatrixScrapesBeforeItUnwaxes() {
        for (CopperWeathering.Variant variant : CopperWeathering.variants()) {
            CopperWeathering.AxeAction expected;
            if (!variant.isWaxed() && variant.getStage() != CopperWeathering.Stage.UNAFFECTED) {
                expected = CopperWeathering.AxeAction.SCRAPE;
            } else if (variant.isWaxed()) {
                expected = CopperWeathering.AxeAction.UNWAX;
            } else {
                expected = CopperWeathering.AxeAction.PASS;
            }
            assertEquals(variant.getPath(), expected, CopperWeathering.axeAction(variant));
            CopperWeathering.Variant result = CopperWeathering.axeResult(variant);
            if (expected == CopperWeathering.AxeAction.PASS) {
                assertNull(result);
            } else if (expected == CopperWeathering.AxeAction.SCRAPE) {
                assertEquals(variant.getStage().previous(), result.getStage());
                assertFalse(result.isWaxed());
            } else {
                assertEquals(variant.getStage(), result.getStage());
                assertFalse(result.isWaxed());
            }
        }
    }

    @Test
    public void youngerNeighborVetoesAgingAnywhereInsideRadiusFour() {
        assertEquals(-1.0F, CopperWeathering.transitionChance(
                CopperWeathering.Stage.WEATHERED,
                Arrays.asList(CopperWeathering.Stage.OXIDIZED,
                        CopperWeathering.Stage.EXPOSED)), 0.0F);
        assertFalse(CopperWeathering.shouldAdvance(CopperWeathering.Stage.WEATHERED,
                Collections.singletonList(CopperWeathering.Stage.EXPOSED),
                0.0F, 0.0F));
    }

    @Test
    public void transitionChanceUsesExactSquaredNeighborRatio() {
        assertEquals(0.75F, CopperWeathering.transitionChance(
                CopperWeathering.Stage.UNAFFECTED, Collections.<CopperWeathering.Stage>emptyList()),
                0.0F);
        assertEquals(1.0F, CopperWeathering.transitionChance(
                CopperWeathering.Stage.EXPOSED, Collections.<CopperWeathering.Stage>emptyList()),
                0.0F);
        assertEquals(0.1875F, CopperWeathering.transitionChance(
                CopperWeathering.Stage.UNAFFECTED,
                Collections.singletonList(CopperWeathering.Stage.UNAFFECTED)), 0.0F);
        assertEquals(4.0F / 9.0F, CopperWeathering.transitionChance(
                CopperWeathering.Stage.EXPOSED,
                Arrays.asList(CopperWeathering.Stage.EXPOSED,
                        CopperWeathering.Stage.WEATHERED)), 0.0000001F);
    }

    @Test
    public void bothRandomRollBoundariesAreStrict() {
        assertTrue(CopperWeathering.shouldAdvance(CopperWeathering.Stage.EXPOSED,
                Collections.<CopperWeathering.Stage>emptyList(),
                Math.nextDown(CopperWeathering.RANDOM_TICK_CHANCE), Math.nextDown(1.0F)));
        assertFalse(CopperWeathering.shouldAdvance(CopperWeathering.Stage.EXPOSED,
                Collections.<CopperWeathering.Stage>emptyList(),
                CopperWeathering.RANDOM_TICK_CHANCE, 0.0F));
        assertFalse(CopperWeathering.shouldAdvance(CopperWeathering.Stage.UNAFFECTED,
                Collections.<CopperWeathering.Stage>emptyList(), 0.0F, 0.75F));
    }
}

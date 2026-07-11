package net.celestiald.cavesnotcliffs.content;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CandleMechanicsTest {
    @Test
    public void allSeventeenCanonicalFamiliesHaveStablePathsAndDyes() {
        List<CandleMechanics.Color> colors = CandleMechanics.colors();
        assertEquals(17, colors.size());
        assertEquals("candle", colors.get(0).getCandlePath());
        assertEquals("candle_cake", colors.get(0).getCandleCakePath());
        assertEquals(-1, colors.get(0).getDyeMetadata());
        assertFalse(colors.get(0).isDyed());

        Set<String> candles = new HashSet<>();
        Set<String> cakes = new HashSet<>();
        for (int i = 0; i < colors.size(); i++) {
            CandleMechanics.Color color = colors.get(i);
            assertTrue(candles.add(color.getCandlePath()));
            assertTrue(cakes.add(color.getCandleCakePath()));
            if (i > 0) {
                assertTrue(color.isDyed());
                assertEquals(i - 1, color.getDyeMetadata());
            }
        }
        assertEquals("light_blue_candle", colors.get(4).getCandlePath());
        assertEquals("light_gray_candle_cake", colors.get(9).getCandleCakePath());
        assertEquals("black_candle", colors.get(16).getCandlePath());
    }

    @Test
    public void metadataRoundTripsEveryOfficialCandleState() {
        Set<Integer> metadata = new HashSet<>();
        for (int candles = 1; candles <= 4; candles++) {
            for (boolean lit : new boolean[]{false, true}) {
                for (boolean waterlogged : new boolean[]{false, true}) {
                    int value = CandleMechanics.metadata(candles, lit, waterlogged);
                    assertTrue(metadata.add(value));
                    CandleMechanics.State decoded =
                            CandleMechanics.stateFromMetadata(value);
                    assertEquals(candles, decoded.getCandles());
                    assertEquals(lit, decoded.isLit());
                    assertEquals(waterlogged, decoded.isWaterlogged());
                }
            }
        }
        assertEquals(16, metadata.size());
        for (int value = 0; value < 16; value++) {
            CandleMechanics.State state = CandleMechanics.stateFromMetadata(value);
            assertEquals(value, CandleMechanics.metadata(state.getCandles(),
                    state.isLit(), state.isWaterlogged()));
        }
    }

    @Test
    public void lightAndWaterTruthTableMatchesJava1182() {
        for (int candles = 1; candles <= 4; candles++) {
            assertEquals(0, CandleMechanics.lightLevel(candles, false, false));
            assertEquals(candles * 3,
                    CandleMechanics.lightLevel(candles, true, false));
            assertEquals(0, CandleMechanics.lightLevel(candles, true, true));
            assertEquals(0, CandleMechanics.lightLevel(candles, false, true));
        }
        assertTrue(CandleMechanics.canLight(false, false));
        assertFalse(CandleMechanics.canLight(true, false));
        assertFalse(CandleMechanics.canLight(false, true));
        CandleMechanics.State waterlogged = CandleMechanics.stateFromMetadata(
                CandleMechanics.waterlog(CandleMechanics.metadata(4, true, false)));
        assertEquals(4, waterlogged.getCandles());
        assertFalse(waterlogged.isLit());
        assertTrue(waterlogged.isWaterlogged());
    }

    @Test
    public void stackingUsesSameItemNoSecondaryUseAndFourCap() {
        for (int candles = 1; candles <= 3; candles++) {
            assertTrue(CandleMechanics.canStack(true, false, candles));
            assertEquals(candles + 1, CandleMechanics.stackedCount(candles));
            assertFalse(CandleMechanics.canStack(false, false, candles));
            assertFalse(CandleMechanics.canStack(true, true, candles));
        }
        assertFalse(CandleMechanics.canStack(true, false, 4));
    }

    @Test
    public void flameOffsetsMatchTheOfficialTableExactly() {
        assertOffset(CandleMechanics.particleOffsets(1).get(0), 0.5, 0.5, 0.5);
        assertEquals(2, CandleMechanics.particleOffsets(2).size());
        assertOffset(CandleMechanics.particleOffsets(2).get(0),
                0.375, 0.44, 0.5);
        assertOffset(CandleMechanics.particleOffsets(2).get(1),
                0.625, 0.5, 0.44);
        assertEquals(3, CandleMechanics.particleOffsets(3).size());
        assertOffset(CandleMechanics.particleOffsets(3).get(0),
                0.5, 0.313, 0.625);
        assertEquals(4, CandleMechanics.particleOffsets(4).size());
        assertOffset(CandleMechanics.particleOffsets(4).get(3),
                0.56, 0.5, 0.375);
    }

    @Test
    public void candleCakeHitAndCakeInsertionBoundariesAreExact() {
        assertFalse(CandleMechanics.extinguishesCandleCake(true, true, 0.5D));
        assertTrue(CandleMechanics.extinguishesCandleCake(true, true,
                Math.nextUp(0.5D)));
        assertFalse(CandleMechanics.extinguishesCandleCake(false, true, 1.0D));
        assertFalse(CandleMechanics.extinguishesCandleCake(true, false, 1.0D));
        assertTrue(CandleMechanics.canInsertIntoCake(0));
        for (int bites = 1; bites <= 6; bites++) {
            assertFalse(CandleMechanics.canInsertIntoCake(bites));
        }
        assertEquals(14, CandleMechanics.FULL_CAKE_COMPARATOR_SIGNAL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsZeroCandles() {
        CandleMechanics.metadata(0, false, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidMetadata() {
        CandleMechanics.stateFromMetadata(16);
    }

    private static void assertOffset(CandleMechanics.ParticleOffset actual,
            double x, double y, double z) {
        assertEquals(x, actual.x, 0.0D);
        assertEquals(y, actual.y, 0.0D);
        assertEquals(z, actual.z, 0.0D);
    }
}

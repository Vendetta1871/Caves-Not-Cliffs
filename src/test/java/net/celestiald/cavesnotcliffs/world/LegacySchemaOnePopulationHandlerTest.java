package net.celestiald.cavesnotcliffs.world;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class LegacySchemaOnePopulationHandlerTest {
    @Test
    public void mapsAndOrdersEveryDraftDecoratorBand() {
        assertEquals(1, LegacySchemaOnePopulationHandler.bit(-4));
        assertEquals(16, LegacySchemaOnePopulationHandler.bit(0));
        assertEquals(128, LegacySchemaOnePopulationHandler.bit(3));
        assertArrayEquals(new int[] {-4, -3, -2, -1, 0, 1, 2, 3},
                LegacySchemaOnePopulationHandler.missingBands(0));
        assertArrayEquals(new int[] {-3, -2, -1, 1, 2},
                LegacySchemaOnePopulationHandler.missingBands(
                        LegacySchemaOnePopulationHandler.bit(-4)
                                | LegacySchemaOnePopulationHandler.bit(0)
                                | LegacySchemaOnePopulationHandler.bit(3)));
        assertArrayEquals(new int[0], LegacySchemaOnePopulationHandler.missingBands(
                LegacySchemaOnePopulationHandler.COMPLETE_MASK));
    }

    @Test
    public void initialMarkerSeparatesVanillaPhaseFromDecoratorBits() {
        NBTTagCompound data = new NBTTagCompound();
        assertNull(LegacySchemaOnePopulationHandler.readProgress(data));

        LegacySchemaOnePopulationHandler.writeInitialMarker(data, 0x03);
        LegacySchemaOnePopulationHandler.Progress needsVanilla =
                LegacySchemaOnePopulationHandler.readProgress(data);
        assertEquals(0x03, needsVanilla.getMask());
        assertEquals(LegacySchemaOnePopulationHandler.Phase.NEEDS_VANILLA,
                needsVanilla.getPhase());
        assertEquals(LegacySchemaOnePopulationHandler.VERSION,
                data.getCompoundTag(LegacySchemaOnePopulationHandler.NBT_KEY)
                        .getInteger("version"));

        int cubeZeroComplete = 0x03 | LegacySchemaOnePopulationHandler.bit(0);
        LegacySchemaOnePopulationHandler.writeInitialMarker(data, cubeZeroComplete);
        LegacySchemaOnePopulationHandler.Progress completeVanilla =
                LegacySchemaOnePopulationHandler.readProgress(data);
        assertEquals(cubeZeroComplete, completeVanilla.getMask());
        assertEquals(LegacySchemaOnePopulationHandler.Phase.COMPLETE,
                completeVanilla.getPhase());
    }

    @Test
    public void roundTripsEveryPossibleImportedBandMask() {
        for (int mask = 0; mask <= LegacySchemaOnePopulationHandler.COMPLETE_MASK; mask++) {
            NBTTagCompound data = new NBTTagCompound();
            LegacySchemaOnePopulationHandler.writeInitialMarker(data, mask);
            LegacySchemaOnePopulationHandler.Progress restored =
                    LegacySchemaOnePopulationHandler.readProgress(data);
            assertEquals(mask, restored.getMask());
            assertEquals((mask & LegacySchemaOnePopulationHandler.bit(0)) == 0
                            ? LegacySchemaOnePopulationHandler.Phase.NEEDS_VANILLA
                            : LegacySchemaOnePopulationHandler.Phase.COMPLETE,
                    restored.getPhase());
        }
    }

    @Test
    public void saveRoundTripRetainsIncompletePhaseAndRemovesCompletedMarker() {
        NBTTagCompound data = new NBTTagCompound();
        LegacySchemaOnePopulationHandler.Progress running =
                new LegacySchemaOnePopulationHandler.Progress(0x03,
                        LegacySchemaOnePopulationHandler.Phase.RUNNING);
        LegacySchemaOnePopulationHandler.writeProgress(data, running, true);
        LegacySchemaOnePopulationHandler.Progress restored =
                LegacySchemaOnePopulationHandler.readProgress(data);
        assertEquals(0x03, restored.getMask());
        assertEquals(LegacySchemaOnePopulationHandler.Phase.RUNNING,
                restored.getPhase());

        LegacySchemaOnePopulationHandler.Progress failed =
                new LegacySchemaOnePopulationHandler.Progress(
                        LegacySchemaOnePopulationHandler.bit(0),
                        LegacySchemaOnePopulationHandler.Phase.FAILED);
        LegacySchemaOnePopulationHandler.writeProgress(data, failed, true);
        assertEquals(LegacySchemaOnePopulationHandler.Phase.FAILED,
                LegacySchemaOnePopulationHandler.readProgress(data).getPhase());

        LegacySchemaOnePopulationHandler.Progress complete =
                new LegacySchemaOnePopulationHandler.Progress(
                        LegacySchemaOnePopulationHandler.COMPLETE_MASK,
                        LegacySchemaOnePopulationHandler.Phase.COMPLETE);
        LegacySchemaOnePopulationHandler.writeProgress(data, complete, true);
        assertFalse(data.hasKey(LegacySchemaOnePopulationHandler.NBT_KEY));
    }

    @Test
    public void readsVersionOneCheckpointMarkersWithoutLosingTheirMeaning() {
        NBTTagCompound data = new NBTTagCompound();
        data.setTag(LegacySchemaOnePopulationHandler.NBT_KEY, markerV1(1, 0x03));
        assertEquals(LegacySchemaOnePopulationHandler.Phase.NEEDS_VANILLA,
                LegacySchemaOnePopulationHandler.readProgress(data).getPhase());

        int mask = 0x03 | LegacySchemaOnePopulationHandler.bit(0);
        data.setTag(LegacySchemaOnePopulationHandler.NBT_KEY, markerV1(1, mask));
        assertEquals(LegacySchemaOnePopulationHandler.Phase.COMPLETE,
                LegacySchemaOnePopulationHandler.readProgress(data).getPhase());
    }

    @Test
    public void rejectsMalformedVersionShapePhaseAndMasks() {
        NBTTagCompound data = new NBTTagCompound();
        data.setString(LegacySchemaOnePopulationHandler.NBT_KEY, "not-a-compound");
        expectFailure(data, "not a compound");

        NBTTagCompound marker = markerV2(2, 0, 0);
        marker.setInteger("extra", 1);
        data.setTag(LegacySchemaOnePopulationHandler.NBT_KEY, marker);
        expectFailure(data, "version or shape");

        data.setTag(LegacySchemaOnePopulationHandler.NBT_KEY, markerV2(2, -1, 0));
        expectFailure(data, "outside 0..255");
        data.setTag(LegacySchemaOnePopulationHandler.NBT_KEY, markerV2(2, 256, 0));
        expectFailure(data, "outside 0..255");
        data.setTag(LegacySchemaOnePopulationHandler.NBT_KEY, markerV2(2, 0, 9));
        expectFailure(data, "phase 9");
        data.setTag(LegacySchemaOnePopulationHandler.NBT_KEY,
                markerV2(2, LegacySchemaOnePopulationHandler.bit(0), 0));
        expectFailure(data, "before vanilla/Forge phase");

        try {
            LegacySchemaOnePopulationHandler.bit(-5);
            fail("Expected low cube rejection");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
        try {
            LegacySchemaOnePopulationHandler.bit(4);
            fail("Expected high cube rejection");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static NBTTagCompound markerV1(int version, int mask) {
        NBTTagCompound marker = new NBTTagCompound();
        marker.setInteger("version", version);
        marker.setInteger("mask", mask);
        return marker;
    }

    private static NBTTagCompound markerV2(int version, int mask, int phase) {
        NBTTagCompound marker = markerV1(version, mask);
        marker.setInteger("phase", phase);
        return marker;
    }

    private static void expectFailure(NBTTagCompound data, String message) {
        try {
            LegacySchemaOnePopulationHandler.readProgress(data);
            fail("Expected marker rejection containing: " + message);
        } catch (IllegalStateException expected) {
            if (!expected.getMessage().contains(message)) {
                throw expected;
            }
        }
    }
}

package net.celestiald.cavesnotcliffs.world;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class LegacySchemaTwoFluidHandlerTest {
    @Test
    public void mapsEveryFiniteSectionIntoThePopulationMask() {
        assertEquals(1, LegacySchemaTwoFluidHandler.bit(-4));
        assertEquals(16, LegacySchemaTwoFluidHandler.bit(0));
        assertEquals(1 << 23, LegacySchemaTwoFluidHandler.bit(19));
        assertArrayEquals(new int[] {-3, 1, 19},
                LegacySchemaTwoFluidHandler.missingBands(
                        LegacySchemaTwoFluidHandler.COMPLETE_MASK
                                & ~LegacySchemaTwoFluidHandler.bit(-3)
                                & ~LegacySchemaTwoFluidHandler.bit(1)
                                & ~LegacySchemaTwoFluidHandler.bit(19)));
    }

    @Test
    public void markerRoundTripsAndCompleteStateRemovesIt() {
        NBTTagCompound data = new NBTTagCompound();
        assertEquals(-1, LegacySchemaTwoFluidHandler.readMask(data));
        int mask = LegacySchemaTwoFluidHandler.COMPLETE_MASK
                & ~LegacySchemaTwoFluidHandler.bit(3);
        LegacySchemaTwoFluidHandler.writeInitialMarker(data, mask);
        assertEquals(mask, LegacySchemaTwoFluidHandler.readMask(data));

        LegacySchemaTwoFluidHandler.writeInitialMarker(
                data, LegacySchemaTwoFluidHandler.COMPLETE_MASK);
        assertFalse(data.hasKey(LegacySchemaTwoFluidHandler.NBT_KEY));
    }

    @Test
    public void rejectsMalformedOrPreFeatureMarkers() {
        NBTTagCompound data = new NBTTagCompound();
        data.setString(LegacySchemaTwoFluidHandler.NBT_KEY, "bad");
        expectFailure(data, "not a compound");

        NBTTagCompound marker = new NBTTagCompound();
        marker.setInteger("version", LegacySchemaTwoFluidHandler.VERSION + 1);
        marker.setInteger("mask", LegacySchemaTwoFluidHandler.COMPLETE_MASK);
        data.setTag(LegacySchemaTwoFluidHandler.NBT_KEY, marker);
        expectFailure(data, "unsupported version or shape");

        marker.setInteger("version", LegacySchemaTwoFluidHandler.VERSION);
        marker.setInteger("mask", LegacySchemaTwoFluidHandler.COMPLETE_MASK
                & ~LegacySchemaTwoFluidHandler.bit(0));
        expectFailure(data, "missing populated cube Y=0");

        try {
            LegacySchemaTwoFluidHandler.writeInitialMarker(data,
                    LegacySchemaTwoFluidHandler.COMPLETE_MASK
                            & ~LegacySchemaTwoFluidHandler.bit(0));
            fail("Expected pre-feature marker rejection");
        } catch (IllegalStateException expected) {
            // Expected.
        }
    }

    private static void expectFailure(NBTTagCompound data, String message) {
        try {
            LegacySchemaTwoFluidHandler.readMask(data);
            fail("Expected marker rejection containing: " + message);
        } catch (IllegalStateException expected) {
            if (!expected.getMessage().contains(message)) {
                throw expected;
            }
        }
    }
}

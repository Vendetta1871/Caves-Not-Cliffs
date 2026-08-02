package net.celestiald.cavesnotcliffs.dripstone;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VanillaCauldronMetaTest {
    @Test
    public void everyValidStateRoundTripsThroughMetadata() {
        assertRoundTrip(CauldronMechanics.empty());
        for (int level = 1; level <= CauldronMechanics.MAX_LEVEL; level++) {
            assertRoundTrip(CauldronMechanics.water(level));
            assertRoundTrip(CauldronMechanics.powderSnow(level));
        }
        assertRoundTrip(CauldronMechanics.lava());
    }

    @Test
    public void waterUsesTheVanillaLevelMetadata() {
        assertEquals(0, VanillaCauldronMeta.toMeta(CauldronMechanics.empty()));
        assertEquals(1, VanillaCauldronMeta.toMeta(CauldronMechanics.water(1)));
        assertEquals(2, VanillaCauldronMeta.toMeta(CauldronMechanics.water(2)));
        assertEquals(3, VanillaCauldronMeta.toMeta(CauldronMechanics.water(3)));
    }

    @Test
    public void lavaUsesTheLegacyHiddenBlockMetadata() {
        assertEquals(VanillaCauldronMeta.LAVA_META,
                VanillaCauldronMeta.toMeta(CauldronMechanics.lava()));
        // The legacy hidden lava cauldron emits meta 7 for lava; 4-6 decode as lava too so any
        // historical metadata round-trips without losing the contents.
        for (int meta = 4; meta <= 7; meta++) {
            assertEquals(CauldronMechanics.lava(), VanillaCauldronMeta.fromMeta(meta));
        }
    }

    @Test
    public void powderSnowLevelsStartAtTheBaseMetadata() {
        assertEquals(8, VanillaCauldronMeta.toMeta(CauldronMechanics.powderSnow(1)));
        assertEquals(9, VanillaCauldronMeta.toMeta(CauldronMechanics.powderSnow(2)));
        assertEquals(10, VanillaCauldronMeta.toMeta(CauldronMechanics.powderSnow(3)));
    }

    @Test
    public void outOfRangePowderSnowMetadataClampsToTheTopLayer() {
        for (int meta = 11; meta <= 15; meta++) {
            assertEquals(CauldronMechanics.powderSnow(3), VanillaCauldronMeta.fromMeta(meta));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeMetadataIsRejected() {
        VanillaCauldronMeta.fromMeta(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void metadataAboveFifteenIsRejected() {
        VanillaCauldronMeta.fromMeta(16);
    }

    private static void assertRoundTrip(CauldronMechanics.State state) {
        assertEquals(state, VanillaCauldronMeta.fromMeta(VanillaCauldronMeta.toMeta(state)));
    }
}

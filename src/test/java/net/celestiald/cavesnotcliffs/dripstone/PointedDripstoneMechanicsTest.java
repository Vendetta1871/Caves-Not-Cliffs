package net.celestiald.cavesnotcliffs.dripstone;

import net.celestiald.cavesnotcliffs.block.BlockPointedDripstone;
import net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics.Neighbor;
import net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics.Thickness;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.EnumFacing;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PointedDripstoneMechanicsTest {
    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void allTenDirectionThicknessStatesRoundTripOnEachStorageIdentity() {
        for (boolean waterlogged : new boolean[]{false, true}) {
            BlockPointedDripstone block = new BlockPointedDripstone(waterlogged);
            for (boolean tipUp : new boolean[]{false, true}) {
                for (Thickness thickness : Thickness.values()) {
                    int meta = PointedDripstoneMechanics.metadata(tipUp, thickness);
                    assertEquals(meta, block.getMetaFromState(block.getStateFromMeta(meta)));
                    assertEquals(tipUp ? EnumFacing.UP : EnumFacing.DOWN,
                            block.getStateFromMeta(meta)
                                    .getValue(BlockPointedDripstone.TIP_DIRECTION));
                    assertEquals(thickness, block.getStateFromMeta(meta)
                            .getValue(BlockPointedDripstone.THICKNESS));
                }
            }
            assertEquals(9, block.getMetaFromState(block.getStateFromMeta(15)));
        }
    }

    @Test
    public void thicknessTransitionsMatchTheJava1182Oracle() {
        Neighbor sameTip = Neighbor.pointed(false, Thickness.TIP);
        Neighbor sameMiddle = Neighbor.pointed(false, Thickness.MIDDLE);
        Neighbor oppositeTip = Neighbor.pointed(true, Thickness.TIP);
        Neighbor oppositeMerged = Neighbor.pointed(true, Thickness.TIP_MERGE);

        assertEquals(Thickness.TIP, PointedDripstoneMechanics.calculateThickness(
                false, Neighbor.OTHER, Neighbor.OTHER, true));
        assertEquals(Thickness.FRUSTUM, PointedDripstoneMechanics.calculateThickness(
                false, sameTip, Neighbor.OTHER, true));
        assertEquals(Thickness.BASE, PointedDripstoneMechanics.calculateThickness(
                false, sameMiddle, Neighbor.OTHER, true));
        assertEquals(Thickness.MIDDLE, PointedDripstoneMechanics.calculateThickness(
                false, sameMiddle, sameMiddle, true));
        assertEquals(Thickness.TIP_MERGE, PointedDripstoneMechanics.calculateThickness(
                false, oppositeTip, Neighbor.OTHER, true));
        assertEquals(Thickness.TIP, PointedDripstoneMechanics.calculateThickness(
                false, oppositeTip, Neighbor.OTHER, false));
        assertEquals(Thickness.TIP_MERGE, PointedDripstoneMechanics.calculateThickness(
                false, oppositeMerged, Neighbor.OTHER, false));
    }

    @Test
    public void numericConstantsAndStrictThresholdsMatchJava1182() {
        assertEquals(0.17578125F,
                PointedDripstoneMechanics.WATER_CAULDRON_FILL_CHANCE, 0.0F);
        assertEquals(0.05859375F,
                PointedDripstoneMechanics.LAVA_CAULDRON_FILL_CHANCE, 0.0F);
        assertEquals(0.011377778F, PointedDripstoneMechanics.GROWTH_CHANCE, 0.0F);
        assertFalse(PointedDripstoneMechanics.shouldBreakFromTrident(0.6D));
        assertTrue(PointedDripstoneMechanics.shouldBreakFromTrident(
                Math.nextUp(0.6D)));
        assertEquals(57, PointedDripstoneMechanics.cauldronDelay(20, 13));
        assertEquals(6.0F,
                PointedDripstoneMechanics.fallingDamagePerDistance(20, 19), 0.0F);
        assertEquals(8.0F,
                PointedDripstoneMechanics.fallingDamagePerDistance(20, 13), 0.0F);
    }
}

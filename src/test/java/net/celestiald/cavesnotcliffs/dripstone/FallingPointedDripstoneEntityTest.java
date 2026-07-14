package net.celestiald.cavesnotcliffs.dripstone;

import net.celestiald.cavesnotcliffs.entity.EntityFallingPointedDripstone;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FallingPointedDripstoneEntityTest {
    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void internalEntityHasStableNamespacedIdentity() {
        assertEquals("cavesnotcliffs:falling_pointed_dripstone",
                EntityFallingPointedDripstone.CncEntity.ID.toString());
    }

    @Test
    public void invalidLandingRestoresTheFluidThatWasPresentBeforePlacement() {
        assertEquals(Blocks.WATER,
                EntityFallingPointedDripstone.EntityCustom
                        .retainedLandingFluid(true).getBlock());
        assertEquals(Blocks.AIR,
                EntityFallingPointedDripstone.EntityCustom
                        .retainedLandingFluid(false).getBlock());
    }

    @Test
    public void draftV2WetInFlightStateLoadsAsDryWithoutChangingItsShapeMetadata() {
        NBTTagCompound saved = new NBTTagCompound();
        saved.setString("Block", "cavesnotcliffs:pointed_dripstone_waterlogged");
        saved.setByte("Data", (byte) 9);
        EntityFallingPointedDripstone.EntityCustom.normalizeSavedFallingState(saved);
        assertEquals("cavesnotcliffs:pointed_dripstone", saved.getString("Block"));
        assertEquals(9, saved.getByte("Data"));

        NBTTagCompound canonical = new NBTTagCompound();
        canonical.setString("Block", "cavesnotcliffs:pointed_dripstone");
        EntityFallingPointedDripstone.EntityCustom.normalizeSavedFallingState(canonical);
        assertEquals("cavesnotcliffs:pointed_dripstone", canonical.getString("Block"));
    }
}

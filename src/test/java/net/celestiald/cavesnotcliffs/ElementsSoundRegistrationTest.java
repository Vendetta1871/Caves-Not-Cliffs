package net.celestiald.cavesnotcliffs;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ElementsSoundRegistrationTest {
    @Test
    public void preNamedSoundsRemainIdempotentForDedicatedServerRegistration() {
        ResourceLocation id = new ResourceLocation("cavesnotcliffs", "test.sound");
        SoundEvent sound = new SoundEvent(id).setRegistryName(id);
        assertSame(sound, ElementsCavesNotCliffs.prepareSoundRegistration(id, sound));
        assertEquals(id, sound.getRegistryName());
    }

    @Test
    public void legacyUnnamedSoundsReceiveTheirExpectedNameOnce() {
        ResourceLocation id = new ResourceLocation("cavesnotcliffs", "legacy.sound");
        SoundEvent sound = new SoundEvent(id);
        assertSame(sound, ElementsCavesNotCliffs.prepareSoundRegistration(id, sound));
        assertEquals(id, sound.getRegistryName());
    }

    @Test(expected = IllegalStateException.class)
    public void mismatchedNamesFailClearlyInsteadOfSilentlyRegisteringWrongIds() {
        ResourceLocation actual = new ResourceLocation("cavesnotcliffs", "actual");
        SoundEvent sound = new SoundEvent(actual).setRegistryName(actual);
        ElementsCavesNotCliffs.prepareSoundRegistration(
            new ResourceLocation("cavesnotcliffs", "expected"), sound);
    }
}

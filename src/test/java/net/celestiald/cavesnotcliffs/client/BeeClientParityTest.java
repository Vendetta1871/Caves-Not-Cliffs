package net.celestiald.cavesnotcliffs.client;

import net.minecraft.client.audio.MovingSound;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BeeClientParityTest {
    @Test
    public void babyModelUsesOfficialAgeableListBodyTransform() {
        assertEquals(0.5F, ModelBee.CHILD_BODY_SCALE, 0.0F);
        assertEquals(1.5F, ModelBee.CHILD_BODY_Y_OFFSET, 0.0F);
    }

    @Test
    public void loopIsATickableMovingSoundWithOfficialSpeedMix() {
        assertTrue(MovingSound.class.isAssignableFrom(MovingSoundBee.class));
        assertEquals(0.0F, MovingSoundBee.volumeForSpeed(0.009F), 0.0F);
        assertEquals(0.12F, MovingSoundBee.volumeForSpeed(0.1F), 0.000001F);
        assertEquals(0.6F, MovingSoundBee.volumeForSpeed(0.5F), 0.000001F);
        assertEquals(0.6F, MovingSoundBee.volumeForSpeed(5.0F), 0.000001F);
        assertEquals(0.98F, MovingSoundBee.pitchForSpeed(0.01F, false),
                0.000001F);
        assertEquals(1.54F, MovingSoundBee.pitchForSpeed(0.01F, true),
                0.000001F);
        assertEquals(0.0F, MovingSoundBee.pitchForSpeed(0.009F, false), 0.0F);
    }
}

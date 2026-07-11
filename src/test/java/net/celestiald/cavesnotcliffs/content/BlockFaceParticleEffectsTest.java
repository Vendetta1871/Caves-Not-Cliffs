package net.celestiald.cavesnotcliffs.content;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class BlockFaceParticleEffectsTest {
    @Test
    public void eachFaceUsesTheOfficialInclusiveThreeToFiveCount() {
        assertEquals(3, BlockFaceParticleEffects.sampleFaceCount(new FixedRandom(0)));
        assertEquals(4, BlockFaceParticleEffects.sampleFaceCount(new FixedRandom(1)));
        assertEquals(5, BlockFaceParticleEffects.sampleFaceCount(new FixedRandom(2)));
    }

    private static final class FixedRandom extends Random {
        private final int value;

        FixedRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            return value;
        }
    }
}

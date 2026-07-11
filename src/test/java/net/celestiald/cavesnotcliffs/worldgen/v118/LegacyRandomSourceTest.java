package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LegacyRandomSourceTest {
    @Test
    public void matchesJavaLegacyRandomAndItsPositionFactories() {
        LegacyRandomSource random = new LegacyRandomSource(0L);
        assertEquals(-4962768465676381896L, random.nextLong());
        assertEquals(1033096058, random.nextInt());
        assertEquals(47, random.nextInt(100));

        LegacyRandomSource positionalSeed = new LegacyRandomSource(123456789L);
        PositionalRandomFactory positional = positionalSeed.forkPositional();
        RandomSource at = positional.at(-17, 31, Integer.MIN_VALUE);
        RandomSource named = positional.fromHashOf("octave_-4");
        assertEquals(-3523974855461075912L, at.nextLong());
        assertEquals(-5904275081896071476L, named.nextLong());
    }

    @Test
    public void fastInverseSquareRootMatchesOfficialRawBits() {
        assertEquals(4607167179904312135L,
            Double.doubleToRawLongBits(WorldgenMath.fastInvSqrt(1.0D)));
        assertEquals(4597838986672175929L,
            Double.doubleToRawLongBits(WorldgenMath.fastInvSqrt(17.25D)));
    }
}

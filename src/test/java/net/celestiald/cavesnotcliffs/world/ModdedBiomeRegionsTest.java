package net.celestiald.cavesnotcliffs.world;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class ModdedBiomeRegionsTest {
    private static final long SEED = 7_331_004L;

    @Test
    public void regionsArePureFunctionsOfSeedAndPosition() {
        ModdedBiomeRegions first = new ModdedBiomeRegions(SEED, 1024);
        ModdedBiomeRegions second = new ModdedBiomeRegions(SEED, 1024);
        for (int index = 0; index < 64; ++index) {
            int x = -40_000 + index * 1_237;
            int z = 25_000 - index * 911;
            assertEquals(first.regionAt(x, z), second.regionAt(x, z));
        }
    }

    @Test
    public void otherSeedsLayOutOtherRegions() {
        ModdedBiomeRegions first = new ModdedBiomeRegions(SEED, 1024);
        ModdedBiomeRegions other = new ModdedBiomeRegions(SEED + 1L, 1024);
        int differing = 0;
        for (int index = 0; index < 64; ++index) {
            int x = index * 2_003;
            int z = -index * 1_511;
            if (first.regionAt(x, z) != other.regionAt(x, z)) {
                ++differing;
            }
        }
        assertTrue(differing > 60);
    }

    @Test
    public void regionsAreLargeContiguousAreas() {
        ModdedBiomeRegions regions = new ModdedBiomeRegions(SEED, 1024);
        // A 32k-block line crosses about one region per region width (borders wander, so
        // the line may cross one border a few times, but it meets few distinct regions).
        Set<Long> crossed = new HashSet<Long>();
        for (int x = 0; x <= 32_768; x += 4) {
            crossed.add(regions.regionAt(x, 0));
        }
        assertTrue("regions " + crossed.size(), crossed.size() >= 16 && crossed.size() <= 64);

        // A 16x16 area mostly belongs to a single region.
        int split = 0;
        for (int chunk = 0; chunk < 256; ++chunk) {
            int minX = (chunk * 37) << 4;
            int minZ = (chunk * -23) << 4;
            Set<Long> seen = new HashSet<Long>();
            for (int offset = 0; offset < 16; offset += 5) {
                seen.add(regions.regionAt(minX + offset, minZ));
                seen.add(regions.regionAt(minX, minZ + offset));
                seen.add(regions.regionAt(minX + 15, minZ + offset));
            }
            if (seen.size() > 1) {
                ++split;
            }
        }
        assertTrue("split chunks " + split, split < 32);
    }

    @Test
    public void rollsAreUniformUnitValuesIndependentPerHost() {
        ModdedBiomeRegions regions = new ModdedBiomeRegions(SEED, 512);
        long region = regions.regionAt(123, 456);
        double sum = 0.0D;
        int count = 0;
        for (int host = 0; host < 64; ++host) {
            double roll = ModdedBiomeRegions.roll(region, host);
            assertTrue(roll >= 0.0D && roll < 1.0D);
            sum += roll;
            ++count;
        }
        assertEquals(0.5D, sum / count, 0.15D);
        assertNotEquals(ModdedBiomeRegions.roll(region, 3), ModdedBiomeRegions.roll(region, 4),
                0.0D);
    }
}

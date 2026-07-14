package net.celestiald.cavesnotcliffs.content;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class CopperLightningTest {
    @Test
    public void directStrikeAndOfficialRandomWalkCleanCopperInExactOrder() {
        FakeAccess access = new FakeAccess();
        access.put(0, 0, 0, "oxidized_cut_copper_stairs");
        access.put(-1, -1, -1, "oxidized_cut_copper_slab");

        CopperLightning.Result result = CopperLightning.clean(access, 0, 0, 0,
                new ZeroRandom());

        assertEquals(4, result.getBlocksChanged());
        assertEquals(3, result.getParticleEvents());
        assertEquals("cut_copper_stairs", access.get(0, 0, 0));
        assertEquals("cut_copper_slab", access.get(-1, -1, -1));
        assertEquals(1, access.replacements.get("0,0,0").intValue());
        assertEquals(3, access.replacements.get("-1,-1,-1").intValue());
    }

    @Test
    public void waxedCopperIsNeverCleanedOrUsedAsAWalkStep() {
        FakeAccess access = new FakeAccess();
        access.put(0, 0, 0, "waxed_oxidized_copper");
        CopperLightning.Result result = CopperLightning.clean(access, 0, 0, 0,
                new ZeroRandom());
        assertEquals(0, result.getBlocksChanged());
        assertEquals(0, result.getParticleEvents());
        assertEquals("waxed_oxidized_copper", access.get(0, 0, 0));
    }

    @Test
    public void lightningRodMetadataRoundTripsEveryDirectionAndPowerState() {
        for (EnumFacing facing : EnumFacing.values()) {
            for (boolean powered : new boolean[]{false, true}) {
                int meta = LightningRodContent.encodeMetadata(facing, powered);
                assertEquals(facing, LightningRodContent.facingFromMetadata(meta));
                assertEquals(powered, LightningRodContent.poweredFromMetadata(meta));
            }
        }
    }

    @Test
    public void lightningRodAttractionUsesTheOfficialThreeDimensionalRadius() {
        BlockPos strike = BlockPos.ORIGIN;
        assertEquals(true, LightningRodContent.isWithinAttractionRange(
                new BlockPos(0, LightningRodContent.RANGE, 0), strike));
        assertEquals(false, LightningRodContent.isWithinAttractionRange(
                new BlockPos(0, LightningRodContent.RANGE + 1, 0), strike));
        assertEquals(false, LightningRodContent.isWithinAttractionRange(
                new BlockPos(100, 100, 0), strike));
        assertEquals(true, LightningRodContent.isWithinAttractionRange(
                new BlockPos(90, 90, 0), strike));
    }

    private static final class ZeroRandom extends Random {
        @Override
        public int nextInt(int bound) {
            return 0;
        }
    }

    private static final class FakeAccess implements CopperLightning.Access {
        private final Map<String, String> blocks = new HashMap<>();
        private final Map<String, Integer> replacements = new HashMap<>();

        void put(int x, int y, int z, String path) {
            blocks.put(key(x, y, z), path);
        }

        String get(int x, int y, int z) {
            return blocks.get(key(x, y, z));
        }

        @Override
        public String blockPathAt(int x, int y, int z) {
            return get(x, y, z);
        }

        @Override
        public void replace(int x, int y, int z, String path) {
            String key = key(x, y, z);
            blocks.put(key, path);
            replacements.put(key, replacements.containsKey(key) ? replacements.get(key) + 1 : 1);
        }

        @Override
        public void emitCopperParticles(int x, int y, int z) {
        }

        private static String key(int x, int y, int z) {
            return x + "," + y + "," + z;
        }
    }
}

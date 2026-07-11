package net.celestiald.cavesnotcliffs.handler;

import net.minecraft.block.BlockCauldron;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LavaCauldronHandlerTest {
    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void persistentMarkerMakesTheFullScanOneTimeAndVersioned() {
        NBTTagCompound data = new NBTTagCompound();
        assertFalse(LavaCauldronHandler.hasCurrentVersion(data));

        LavaCauldronHandler.writeCurrentVersion(data);
        assertTrue(LavaCauldronHandler.hasCurrentVersion(data));
        assertEquals(LavaCauldronHandler.BRIDGE_VERSION,
                data.getInteger(LavaCauldronHandler.BRIDGE_VERSION_KEY));

        data.setInteger(LavaCauldronHandler.BRIDGE_VERSION_KEY,
                LavaCauldronHandler.BRIDGE_VERSION + 1);
        assertTrue("future-compatible markers must not be rescanned",
                LavaCauldronHandler.hasCurrentVersion(data));
    }

    @Test
    public void volumeScanVisitsEveryCellButOnlyConvertsExactVanillaCauldrons() {
        final int minX = -17;
        final int minY = -64;
        final int minZ = 31;
        final Map<String, IBlockState> states = new HashMap<>();
        states.put(key(minX, minY, minZ), Blocks.CAULDRON.getDefaultState()
                .withProperty(BlockCauldron.LEVEL, 0));
        states.put(key(minX + 1, minY + 1, minZ + 1),
                Blocks.CAULDRON.getDefaultState()
                        .withProperty(BlockCauldron.LEVEL, 3));
        states.put(key(minX + 1, minY, minZ), new BlockCauldron().getDefaultState());
        final Set<String> reads = new HashSet<>();
        final Set<String> conversions = new HashSet<>();

        int converted = LavaCauldronHandler.bridgeVolume(
                minX, minY, minZ, 2, 2, 2, new LavaCauldronHandler.Volume() {
                    @Override
                    public IBlockState stateAt(int x, int y, int z) {
                        reads.add(key(x, y, z));
                        IBlockState state = states.get(key(x, y, z));
                        return state == null ? Blocks.AIR.getDefaultState() : state;
                    }

                    @Override
                    public boolean bridgeAt(int x, int y, int z) {
                        conversions.add(key(x, y, z));
                        return true;
                    }
                });

        assertEquals(8, reads.size());
        assertEquals(2, converted);
        assertEquals(2, conversions.size());
        assertTrue(conversions.contains(key(minX, minY, minZ)));
        assertTrue(conversions.contains(key(minX + 1, minY + 1, minZ + 1)));
        assertFalse("a third-party BlockCauldron subclass is not vanilla",
                conversions.contains(key(minX + 1, minY, minZ)));
    }

    @Test
    public void failedWorldMutationIsNotCountedAsAConversion() {
        int converted = LavaCauldronHandler.bridgeVolume(0, 0, 0, 1, 1, 1,
                new LavaCauldronHandler.Volume() {
                    @Override
                    public IBlockState stateAt(int x, int y, int z) {
                        return Blocks.CAULDRON.getDefaultState();
                    }

                    @Override
                    public boolean bridgeAt(int x, int y, int z) {
                        return false;
                    }
                });
        assertEquals(0, converted);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeScanDimensions() {
        LavaCauldronHandler.bridgeVolume(0, 0, 0, 1, -1, 1,
                new LavaCauldronHandler.Volume() {
                    @Override
                    public IBlockState stateAt(int x, int y, int z) {
                        return Blocks.AIR.getDefaultState();
                    }

                    @Override
                    public boolean bridgeAt(int x, int y, int z) {
                        return false;
                    }
                });
    }

    private static String key(int x, int y, int z) {
        return x + "," + y + "," + z;
    }
}

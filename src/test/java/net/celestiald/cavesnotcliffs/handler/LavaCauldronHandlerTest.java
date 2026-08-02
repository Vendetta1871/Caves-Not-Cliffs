package net.celestiald.cavesnotcliffs.handler;

import net.celestiald.cavesnotcliffs.block.BlockLavaCauldron;
import net.celestiald.cavesnotcliffs.block.BlockPowderSnowCauldron;
import net.celestiald.cavesnotcliffs.worldgen.v118.TerrainColumn;
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
    public void versionOneWorldsAreRescannedForTheReverseMigration() {
        NBTTagCompound data = new NBTTagCompound();
        data.setInteger(LavaCauldronHandler.BRIDGE_VERSION_KEY, 1);
        assertFalse("worlds bridged to hidden storage must be migrated back to vanilla",
                LavaCauldronHandler.hasCurrentVersion(data));
    }

    @Test
    public void volumeScanVisitsEveryCellButOnlyConvertsLegacyHiddenCauldrons() {
        final int minX = -17;
        final int minY = -64;
        final int minZ = 31;
        final Map<String, IBlockState> states = new HashMap<>();
        states.put(key(minX, minY, minZ),
                new BlockLavaCauldron.BlockCustom().getDefaultState());
        states.put(key(minX + 1, minY + 1, minZ + 1),
                new BlockPowderSnowCauldron.BlockCustom().getDefaultState());
        states.put(key(minX + 1, minY, minZ), Blocks.CAULDRON.getDefaultState());
        states.put(key(minX, minY + 1, minZ), new BlockCauldron().getDefaultState());
        final Set<String> reads = new HashSet<>();
        final Set<String> conversions = new HashSet<>();

        int converted = LavaCauldronHandler.migrateVolume(
                minX, minY, minZ, 2, 2, 2, new LavaCauldronHandler.Volume() {
                    @Override
                    public IBlockState stateAt(int x, int y, int z) {
                        reads.add(key(x, y, z));
                        IBlockState state = states.get(key(x, y, z));
                        return state == null ? Blocks.AIR.getDefaultState() : state;
                    }

                    @Override
                    public boolean migrateAt(int x, int y, int z) {
                        conversions.add(key(x, y, z));
                        return true;
                    }
                });

        assertEquals(8, reads.size());
        assertEquals(2, converted);
        assertEquals(2, conversions.size());
        assertTrue(conversions.contains(key(minX, minY, minZ)));
        assertTrue(conversions.contains(key(minX + 1, minY + 1, minZ + 1)));
        assertFalse("the vanilla cauldron is the migration target, not a source",
                conversions.contains(key(minX + 1, minY, minZ)));
        assertFalse("a third-party BlockCauldron subclass is never touched",
                conversions.contains(key(minX, minY + 1, minZ)));
    }

    @Test
    public void hiddenStorageStatesMapToTheEquivalentVanillaMetadata() {
        BlockLavaCauldron.BlockCustom primary = new BlockLavaCauldron.BlockCustom();
        assertEquals(0, LavaCauldronHandler.legacyHiddenMeta(primary.getDefaultState()));
        for (int level = 1; level <= 3; level++) {
            assertEquals(level, LavaCauldronHandler.legacyHiddenMeta(
                    primary.getDefaultState().withProperty(BlockCauldron.LEVEL, level)));
        }
        assertEquals(7, LavaCauldronHandler.legacyHiddenMeta(primary.getDefaultState()
                .withProperty(BlockLavaCauldron.BlockCustom.IS_LAVA, true)));

        BlockPowderSnowCauldron.BlockCustom powder = new BlockPowderSnowCauldron.BlockCustom();
        for (int level = 1; level <= 3; level++) {
            assertEquals(8 + level - 1, LavaCauldronHandler.legacyHiddenMeta(
                    powder.getDefaultState().withProperty(BlockCauldron.LEVEL, level)));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void nonHiddenStatesHaveNoLegacyMetadata() {
        LavaCauldronHandler.legacyHiddenMeta(Blocks.CAULDRON.getDefaultState());
    }

    @Test
    public void extendedColumnScanIncludesBottomAndTopBuildHeights() {
        final Set<Integer> reads = new HashSet<>();
        final Set<Integer> conversions = new HashSet<>();
        final IBlockState hidden = new BlockLavaCauldron.BlockCustom().getDefaultState();

        int converted = LavaCauldronHandler.migrateVolume(
                0, TerrainColumn.MIN_Y, 0, 1, TerrainColumn.HEIGHT, 1,
                new LavaCauldronHandler.Volume() {
                    @Override
                    public IBlockState stateAt(int x, int y, int z) {
                        reads.add(y);
                        return y == TerrainColumn.MIN_Y || y == TerrainColumn.MAX_Y
                                ? hidden
                                : Blocks.AIR.getDefaultState();
                    }

                    @Override
                    public boolean migrateAt(int x, int y, int z) {
                        conversions.add(y);
                        return true;
                    }
                });

        assertEquals(TerrainColumn.HEIGHT, reads.size());
        assertTrue(reads.contains(TerrainColumn.MIN_Y));
        assertTrue(reads.contains(TerrainColumn.MAX_Y));
        assertFalse(reads.contains(TerrainColumn.MIN_Y - 1));
        assertFalse(reads.contains(TerrainColumn.MAX_Y_EXCLUSIVE));
        assertEquals(2, converted);
        assertEquals(2, conversions.size());
    }

    @Test
    public void failedWorldMutationIsNotCountedAsAConversion() {
        final IBlockState hidden = new BlockLavaCauldron.BlockCustom().getDefaultState();
        int converted = LavaCauldronHandler.migrateVolume(0, 0, 0, 1, 1, 1,
                new LavaCauldronHandler.Volume() {
                    @Override
                    public IBlockState stateAt(int x, int y, int z) {
                        return hidden;
                    }

                    @Override
                    public boolean migrateAt(int x, int y, int z) {
                        return false;
                    }
                });
        assertEquals(0, converted);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeScanDimensions() {
        LavaCauldronHandler.migrateVolume(0, 0, 0, 1, -1, 1,
                new LavaCauldronHandler.Volume() {
                    @Override
                    public IBlockState stateAt(int x, int y, int z) {
                        return Blocks.AIR.getDefaultState();
                    }

                    @Override
                    public boolean migrateAt(int x, int y, int z) {
                        return false;
                    }
                });
    }

    private static String key(int x, int y, int z) {
        return x + "," + y + "," + z;
    }
}

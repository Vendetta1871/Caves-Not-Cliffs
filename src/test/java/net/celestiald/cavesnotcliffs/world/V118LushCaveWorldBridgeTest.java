package net.celestiald.cavesnotcliffs.world;

import net.minecraft.block.BlockStone;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class V118LushCaveWorldBridgeTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void baseStoneTagDoesNotLeakOntoPolishedLegacyStates() {
        assertTrue(V118LushCaveWorldBridge.isBaseStoneOverworld(stone(
            BlockStone.EnumType.STONE)));
        assertTrue(V118LushCaveWorldBridge.isBaseStoneOverworld(stone(
            BlockStone.EnumType.GRANITE)));
        assertTrue(V118LushCaveWorldBridge.isBaseStoneOverworld(stone(
            BlockStone.EnumType.DIORITE)));
        assertTrue(V118LushCaveWorldBridge.isBaseStoneOverworld(stone(
            BlockStone.EnumType.ANDESITE)));
        assertFalse(V118LushCaveWorldBridge.isBaseStoneOverworld(stone(
            BlockStone.EnumType.GRANITE_SMOOTH)));
        assertFalse(V118LushCaveWorldBridge.isBaseStoneOverworld(stone(
            BlockStone.EnumType.DIORITE_SMOOTH)));
        assertFalse(V118LushCaveWorldBridge.isBaseStoneOverworld(stone(
            BlockStone.EnumType.ANDESITE_SMOOTH)));
    }

    @Test
    public void flattenedOfficialDirtAndTerracottaFamiliesAreRecognized() {
        assertTrue(V118LushCaveWorldBridge.isDirtTag(Blocks.DIRT.getDefaultState()));
        assertTrue(V118LushCaveWorldBridge.isDirtTag(Blocks.GRASS.getDefaultState()));
        assertTrue(V118LushCaveWorldBridge.isDirtTag(Blocks.MYCELIUM.getDefaultState()));
        assertFalse(V118LushCaveWorldBridge.isDirtTag(Blocks.CLAY.getDefaultState()));

        assertTrue(V118LushCaveWorldBridge.isTerracotta(
            Blocks.HARDENED_CLAY.getDefaultState()));
        assertTrue(V118LushCaveWorldBridge.isTerracotta(
            Blocks.STAINED_HARDENED_CLAY.getDefaultState()));
        assertFalse(V118LushCaveWorldBridge.isTerracotta(Blocks.CLAY.getDefaultState()));
    }

    @Test
    public void featureHaloHandlesNegativeChunkBoundaries() {
        assertTrue(V118LushCaveWorldBridge.insideFeatureChunks(-32, -32, -1, -1));
        assertTrue(V118LushCaveWorldBridge.insideFeatureChunks(15, 15, -1, -1));
        assertFalse(V118LushCaveWorldBridge.insideFeatureChunks(16, 0, -1, -1));
        assertFalse(V118LushCaveWorldBridge.insideFeatureChunks(-33, -33, 0, 0));
    }

    private static net.minecraft.block.state.IBlockState stone(BlockStone.EnumType type) {
        return Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, type);
    }
}

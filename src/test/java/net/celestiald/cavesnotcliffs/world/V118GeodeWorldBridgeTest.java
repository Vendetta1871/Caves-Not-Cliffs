package net.celestiald.cavesnotcliffs.world;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class V118GeodeWorldBridgeTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void matchesOfficialInvalidAndCannotReplaceTags() {
        assertTrue(V118GeodeWorldBridge.isInvalidBlock(Blocks.BEDROCK.getDefaultState()));
        assertTrue(V118GeodeWorldBridge.isInvalidBlock(Blocks.WATER.getDefaultState()));
        assertTrue(V118GeodeWorldBridge.isInvalidBlock(Blocks.FLOWING_LAVA.getDefaultState()));
        assertTrue(V118GeodeWorldBridge.isInvalidBlock(Blocks.ICE.getDefaultState()));
        assertTrue(V118GeodeWorldBridge.isInvalidBlock(Blocks.PACKED_ICE.getDefaultState()));
        assertFalse(V118GeodeWorldBridge.isInvalidBlock(Blocks.STONE.getDefaultState()));

        assertTrue(V118GeodeWorldBridge.isProtectedBlock(Blocks.BEDROCK.getDefaultState()));
        assertTrue(V118GeodeWorldBridge.isProtectedBlock(Blocks.MOB_SPAWNER.getDefaultState()));
        assertTrue(V118GeodeWorldBridge.isProtectedBlock(Blocks.CHEST.getDefaultState()));
        assertTrue(V118GeodeWorldBridge.isProtectedBlock(
            Blocks.END_PORTAL_FRAME.getDefaultState()));
        assertFalse(V118GeodeWorldBridge.isProtectedBlock(Blocks.STONE.getDefaultState()));
    }

    @Test
    public void onlyLevelZeroWaterCountsAsAClusterWaterSource() {
        assertTrue(V118GeodeWorldBridge.isSourceLiquid(Blocks.WATER.getDefaultState()
            .withProperty(BlockLiquid.LEVEL, 0), Material.WATER));
        assertFalse(V118GeodeWorldBridge.isSourceLiquid(Blocks.FLOWING_WATER.getDefaultState()
            .withProperty(BlockLiquid.LEVEL, 1), Material.WATER));
        assertFalse(V118GeodeWorldBridge.isSourceLiquid(Blocks.LAVA.getDefaultState()
            .withProperty(BlockLiquid.LEVEL, 0), Material.WATER));
    }
}

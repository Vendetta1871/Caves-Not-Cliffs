package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.block.LushAzaleaBlocks;
import net.celestiald.cavesnotcliffs.block.LushMossBlocks;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class V118MountainSurfaceWorldBridgeTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void sugarCaneUsesTheJava118DirtAndSandTags() {
        assertTrue(V118MountainSurfaceWorldBridge.isSugarCaneGround(Blocks.DIRT));
        assertTrue(V118MountainSurfaceWorldBridge.isSugarCaneGround(Blocks.GRASS));
        assertTrue(V118MountainSurfaceWorldBridge.isSugarCaneGround(Blocks.MYCELIUM));
        assertTrue(V118MountainSurfaceWorldBridge.isSugarCaneGround(Blocks.SAND));
        assertTrue(V118MountainSurfaceWorldBridge.isSugarCaneGround(
            new LushAzaleaBlocks.RootedDirt()));
        assertTrue(V118MountainSurfaceWorldBridge.isSugarCaneGround(
            new LushMossBlocks.Moss()));
        assertFalse(V118MountainSurfaceWorldBridge.isSugarCaneGround(Blocks.STONE));
        assertFalse(V118MountainSurfaceWorldBridge.isSugarCaneGround(Blocks.FARMLAND));
    }
}

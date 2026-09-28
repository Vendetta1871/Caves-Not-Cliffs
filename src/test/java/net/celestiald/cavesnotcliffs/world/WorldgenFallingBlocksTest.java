package net.celestiald.cavesnotcliffs.world;

import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.junit.BeforeClass;
import org.junit.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class WorldgenFallingBlocksTest {
    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void fallTicksAreOnlyDroppedForThePopulatingWorld() throws Exception {
        World populating = allocateWorld();
        World other = allocateWorld();

        assertFalse(WorldgenFallingBlocks.suppressesFallTick(populating, Blocks.GRAVEL));
        World previous = WorldgenFallingBlocks.enter(populating);
        try {
            assertNull(previous);
            assertTrue(WorldgenFallingBlocks.suppressesFallTick(populating, Blocks.GRAVEL));
            assertTrue(WorldgenFallingBlocks.suppressesFallTick(populating, Blocks.SAND));
            assertTrue(WorldgenFallingBlocks.suppressesFallTick(populating, Blocks.ANVIL));
            assertFalse(WorldgenFallingBlocks.suppressesFallTick(other, Blocks.GRAVEL));
            // Fluid ticks (aquifer and spring post-processing) must still be scheduled.
            assertFalse(WorldgenFallingBlocks.suppressesFallTick(populating, Blocks.FLOWING_WATER));
            assertFalse(WorldgenFallingBlocks.suppressesFallTick(populating, Blocks.FLOWING_LAVA));
        } finally {
            WorldgenFallingBlocks.exit(previous);
        }
        assertFalse(WorldgenFallingBlocks.suppressesFallTick(populating, Blocks.GRAVEL));
    }

    @Test
    public void nestedPopulationRestoresTheOuterWorld() throws Exception {
        World outer = allocateWorld();
        World inner = allocateWorld();

        World beforeOuter = WorldgenFallingBlocks.enter(outer);
        try {
            World beforeInner = WorldgenFallingBlocks.enter(inner);
            try {
                assertSame(outer, beforeInner);
                assertTrue(WorldgenFallingBlocks.suppressesFallTick(inner, Blocks.GRAVEL));
                assertFalse(WorldgenFallingBlocks.suppressesFallTick(outer, Blocks.GRAVEL));
            } finally {
                WorldgenFallingBlocks.exit(beforeInner);
            }
            assertTrue(WorldgenFallingBlocks.suppressesFallTick(outer, Blocks.GRAVEL));
        } finally {
            WorldgenFallingBlocks.exit(beforeOuter);
        }
        assertFalse(WorldgenFallingBlocks.suppressesFallTick(outer, Blocks.GRAVEL));
    }

    private static World allocateWorld() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (World) ((Unsafe) field.get(null)).allocateInstance(WorldServer.class);
    }
}

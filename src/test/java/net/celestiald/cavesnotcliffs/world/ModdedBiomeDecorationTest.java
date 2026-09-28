package net.celestiald.cavesnotcliffs.world;

import net.minecraft.init.Bootstrap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ModdedBiomeDecorationTest {
    private static final BlockPos ORIGIN = new BlockPos(32, 0, 48);

    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void healthyDecoratorRuns() {
        CountingBiome biome = new CountingBiome(false);
        assertEquals(ModdedBiomeDecoration.Outcome.DECORATED,
            new ModdedBiomeDecoration().decorate(biome, null, new Random(1L), ORIGIN));
        assertEquals(1, biome.calls);
    }

    @Test
    public void failingDecoratorIsUnwedgedAndRetriedOnLaterChunks() {
        CountingBiome biome = new CountingBiome(true);
        ModdedBiomeDecoration decoration = new ModdedBiomeDecoration();

        assertEquals(ModdedBiomeDecoration.Outcome.FAILED,
            decoration.decorate(biome, null, new Random(1L), ORIGIN));
        // Vanilla leaves the flag set when genDecorations throws; it must be cleared or every
        // later pass of this biome would fail with "Already decorating".
        assertFalse(biome.decorator.decorating);

        assertEquals(ModdedBiomeDecoration.Outcome.FAILED,
            decoration.decorate(biome, null, new Random(2L), ORIGIN.add(16, 0, 0)));
        assertEquals(2, biome.calls);
    }

    @Test
    public void reentrantPopulationSkipsWithoutTouchingTheRunningDecorator() {
        CountingBiome biome = new CountingBiome(false);
        biome.decorator.decorating = true;

        assertEquals(ModdedBiomeDecoration.Outcome.SKIPPED_REENTRANT,
            new ModdedBiomeDecoration().decorate(biome, null, new Random(1L), ORIGIN));
        assertEquals(0, biome.calls);
        assertTrue(biome.decorator.decorating);
    }

    /** Mimics BiomeDecorator: the flag is raised for the pass and only cleared on success. */
    private static final class CountingBiome extends Biome {
        private final boolean failing;
        private int calls;

        private CountingBiome(boolean failing) {
            super(new Biome.BiomeProperties("Test Biome"));
            setRegistryName("testmod", failing ? "failing" : "healthy");
            this.failing = failing;
        }

        @Override
        public void decorate(World world, Random random, BlockPos pos) {
            ++calls;
            decorator.decorating = true;
            if (failing) {
                throw new IllegalStateException("broken mod decorator");
            }
            decorator.decorating = false;
        }
    }
}

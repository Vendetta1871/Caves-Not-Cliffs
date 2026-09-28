package net.celestiald.cavesnotcliffs.world;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeDecorator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Random;
import java.util.Set;

/**
 * Runs the vanilla {@code biome.decorate} pass of modded overlay biomes. Mod decorators are
 * outside our control, so a failing one must neither take the server down mid-population nor
 * leave its {@link BiomeDecorator} wedged: vanilla only clears {@code decorating} after a
 * successful pass, and a stale flag makes every later pass of that biome — here or in any other
 * dimension sharing the biome — fail with "Already decorating".
 */
final class ModdedBiomeDecoration {
    private static final Logger LOGGER = LogManager.getLogger("CavesNotCliffs/ModdedBiomes");

    enum Outcome {
        DECORATED,
        /** The biome's decorator was already running: population re-entered this pass. */
        SKIPPED_REENTRANT,
        FAILED
    }

    private final Set<Biome> reportedFailures =
        Collections.newSetFromMap(new IdentityHashMap<Biome, Boolean>());

    Outcome decorate(Biome biome, World world, Random random, BlockPos chunkOrigin) {
        BiomeDecorator decorator = biome.decorator;
        if (decorator != null && decorator.decorating) {
            // A cascading chunk load populated this chunk while the same biome's decorator was
            // still running further up the stack. Vanilla would throw "Already decorating"; the
            // outer pass is healthy, so only this chunk's modded pass is skipped.
            return Outcome.SKIPPED_REENTRANT;
        }
        try {
            biome.decorate(world, random, chunkOrigin);
            return Outcome.DECORATED;
        } catch (RuntimeException failure) {
            if (decorator != null) {
                decorator.decorating = false;
            }
            if (reportedFailures.add(biome)) {
                LOGGER.warn("Modded biome decoration failed for {} at chunk origin {};"
                        + " further failures of this biome are not logged",
                        biome.getRegistryName(), chunkOrigin, failure);
            }
            return Outcome.FAILED;
        }
    }
}

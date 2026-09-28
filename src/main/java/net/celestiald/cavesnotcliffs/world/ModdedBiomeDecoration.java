package net.celestiald.cavesnotcliffs.world;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.EnumSet;
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
    /**
     * 1.12 terrain features a climate-hosted modded biome's decorator must not add on top of
     * the 1.18 ones already placed: water/lava springs, sand/clay/gravel disks, fossils,
     * desert wells, forest rocks and ice spikes. Vegetation and CUSTOM features stay.
     */
    private static final Set<DecorateBiomeEvent.Decorate.EventType> DENIED_DECORATIONS =
        EnumSet.of(DecorateBiomeEvent.Decorate.EventType.LAKE_WATER,
            DecorateBiomeEvent.Decorate.EventType.LAKE_LAVA,
            DecorateBiomeEvent.Decorate.EventType.SAND,
            DecorateBiomeEvent.Decorate.EventType.CLAY,
            DecorateBiomeEvent.Decorate.EventType.SAND_PASS2,
            DecorateBiomeEvent.Decorate.EventType.FOSSIL,
            DecorateBiomeEvent.Decorate.EventType.DESERT_WELL,
            DecorateBiomeEvent.Decorate.EventType.ROCK,
            DecorateBiomeEvent.Decorate.EventType.ICE);
    private static final ThreadLocal<World> FILTERING = new ThreadLocal<World>();

    enum Outcome {
        DECORATED,
        /** The biome's decorator was already running: population re-entered this pass. */
        SKIPPED_REENTRANT,
        FAILED
    }

    private final Set<Biome> reportedFailures =
        Collections.newSetFromMap(new IdentityHashMap<Biome, Boolean>());

    /**
     * Decorates a climate-hosted modded biome: its vegetation and custom features run, while
     * its 1.12 ores, dirt/gravel/stone blobs and the terrain features in
     * {@link #DENIED_DECORATIONS} are denied through the Forge events it fires, so ores,
     * springs and disks stay those of 1.18.
     */
    Outcome decorateHosted(Biome biome, World world, Random random, BlockPos chunkOrigin) {
        World previous = FILTERING.get();
        FILTERING.set(world);
        try {
            return decorate(biome, world, random, chunkOrigin);
        } finally {
            if (previous == null) {
                FILTERING.remove();
            } else {
                FILTERING.set(previous);
            }
        }
    }

    /** Whether this biome's decorator threw here before (its host vegetation is kept then). */
    boolean hasFailed(Biome biome) {
        return reportedFailures.contains(biome);
    }

    /**
     * Lifts the filter for a nested population pass (a chunk populated by a cascading load
     * from inside a modded decorator) so its own 1.18 events are not denied; returns the
     * state {@link #resumeFiltering} restores.
     */
    static World suspendFiltering() {
        World filtering = FILTERING.get();
        if (filtering != null) {
            FILTERING.remove();
        }
        return filtering;
    }

    static void resumeFiltering(World filtering) {
        if (filtering != null) {
            FILTERING.set(filtering);
        }
    }

    static boolean isFiltering(World world) {
        return world != null && FILTERING.get() == world;
    }

    static boolean deniesWhileFiltering(DecorateBiomeEvent.Decorate.EventType type) {
        return DENIED_DECORATIONS.contains(type);
    }

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

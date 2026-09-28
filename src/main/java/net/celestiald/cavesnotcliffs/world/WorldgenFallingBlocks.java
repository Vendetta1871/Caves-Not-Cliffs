package net.celestiald.cavesnotcliffs.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.world.World;

/**
 * Keeps sand and gravel placed by native population where it generates, as Java 1.18 does.
 *
 * <p>1.18 writes features into proto-chunks without block callbacks, so an ore_gravel blob or a
 * sand disk cut open by a noise cave simply floats. 1.12's {@code setBlockState} runs
 * {@link BlockFalling#onBlockAdded}, which schedules a fall tick for every such block: after
 * spawn preparation thousands of falling-block entities dropped at once and stalled the server
 * for seconds. While a native generator populates, {@code mixin/WorldServerMixin} drops fall
 * ticks for that world. A later neighbour update still makes an unsupported block fall, like
 * in 1.18.</p>
 */
public final class WorldgenFallingBlocks {
    private static final ThreadLocal<World> POPULATING = new ThreadLocal<World>();

    private WorldgenFallingBlocks() {}

    /** Marks {@code world} as populating on this thread; returns the mark to restore. */
    static World enter(World world) {
        World previous = POPULATING.get();
        POPULATING.set(world);
        return previous;
    }

    static void exit(World previous) {
        if (previous == null) {
            POPULATING.remove();
        } else {
            POPULATING.set(previous);
        }
    }

    public static boolean suppressesFallTick(World world, Block block) {
        return block instanceof BlockFalling && world != null && POPULATING.get() == world;
    }
}

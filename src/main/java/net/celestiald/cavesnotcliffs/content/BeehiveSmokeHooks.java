package net.celestiald.cavesnotcliffs.content;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Narrow integration seam for the later campfire checkpoint. */
public final class BeehiveSmokeHooks {
    private static final SmokeProvider NO_SMOKE = (world, pos) -> false;
    private static volatile SmokeProvider provider = NO_SMOKE;

    private BeehiveSmokeHooks() {
    }

    public static boolean isSmokey(World world, BlockPos hivePos) {
        return provider.isSmokey(world, hivePos);
    }

    public static void register(SmokeProvider newProvider) {
        if (newProvider == null) {
            throw new NullPointerException("newProvider");
        }
        provider = newProvider;
    }

    /** Test/lifecycle reset; campfire code should call {@link #register(SmokeProvider)}. */
    public static void clear() {
        provider = NO_SMOKE;
    }

    public interface SmokeProvider {
        boolean isSmokey(World world, BlockPos hivePos);
    }
}

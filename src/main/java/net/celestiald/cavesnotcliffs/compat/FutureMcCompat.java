package net.celestiald.cavesnotcliffs.compat;

import net.celestiald.cavesnotcliffs.content.LushCaveContent;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;

/** Small optional bridge for Future MC's public bee pollination API. */
public final class FutureMcCompat {
    private static final Logger LOGGER = LogManager.getLogger("CavesNotCliffs/FutureMC");
    private static boolean initialized;

    private FutureMcCompat() {
    }

    public static void initialize() {
        if (initialized || !Loader.isModLoaded("futuremc")) {
            return;
        }
        initialized = true;
        try {
            Class<?> api = Class.forName(
                    "thedarkcolour.futuremc.api.BeePollinationTargetsJVM");
            Method addTarget = api.getMethod("addPollinationTarget", IBlockState.class);
            addStates(addTarget, LushCaveContent.FLOWERING_AZALEA);
            addStates(addTarget, LushCaveContent.FLOWERING_AZALEA_LEAVES);
            LOGGER.info("Registered flowering azalea states with Future MC bees");
        } catch (ReflectiveOperationException | LinkageError error) {
            LOGGER.warn("Future MC is present but its bee pollination API is incompatible; "
                    + "continuing without the optional bridge", error);
        }
    }

    private static void addStates(Method addTarget, Block block)
            throws ReflectiveOperationException {
        if (block == null) {
            throw new IllegalStateException("Flowering azalea blocks are not registered");
        }
        for (IBlockState state : block.getBlockState().getValidStates()) {
            addTarget.invoke(null, state);
        }
    }
}

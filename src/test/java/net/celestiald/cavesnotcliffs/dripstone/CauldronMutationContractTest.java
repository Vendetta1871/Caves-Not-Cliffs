package net.celestiald.cavesnotcliffs.dripstone;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Guards the runtime call sites that previously bypassed observer notifications. */
public class CauldronMutationContractTest {
    @Test
    public void cauldronBlocksAndBridgeHandlerDelegateEveryMutation() throws IOException {
        assertDelegatesAllMutations(source(
                "block/BlockLavaCauldron.java"));
        assertDelegatesAllMutations(source(
                "block/BlockPowderSnowCauldron.java"));
        assertDelegatesAllMutations(source(
                "handler/LavaCauldronHandler.java"));
    }

    @Test
    public void pointedDripstoneLazyConversionUsesTheSameObserverPath() throws IOException {
        String pointed = source("block/BlockPointedDripstone.java");
        int start = pointed.indexOf("private static BlockPos findFillableCauldronBelowTip");
        int end = pointed.indexOf("private static boolean canDripThrough", start);
        assertTrue(start >= 0 && end > start);
        String lazyConversion = pointed.substring(start, end);
        assertTrue(lazyConversion.contains("CauldronStateBridge.bridgeVanillaAt"));
        assertFalse(lazyConversion.contains("setBlockState"));
    }

    @Test
    public void sharedMutationIsExactlyFlagThreePlusComparatorRefresh() throws IOException {
        String bridge = source("dripstone/CauldronStateBridge.java");
        assertTrue(bridge.contains("public static final int UPDATE_FLAGS = 3"));
        assertTrue(bridge.contains("world.setBlockState(pos, state, flags)"));
        assertTrue(bridge.contains("world.updateComparatorOutputLevel(pos, block)"));
    }

    private static void assertDelegatesAllMutations(String source) {
        assertTrue(source.contains("CauldronStateBridge"));
        assertFalse(source.contains("world.setBlockState"));
        assertFalse(source.contains("world.updateComparatorOutputLevel"));
    }

    private static String source(String relative) throws IOException {
        Path path = Paths.get(System.getProperty("user.dir"), "src", "main", "java",
                "net", "celestiald", "cavesnotcliffs").resolve(relative);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}

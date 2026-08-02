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
    public void pointedDripstoneAcceptsTheVanillaCauldronWithoutConversion() throws IOException {
        String pointed = source("block/BlockPointedDripstone.java");
        int start = pointed.indexOf("private static BlockPos findFillableCauldronBelowTip");
        int end = pointed.indexOf("private static boolean canDripThrough", start);
        assertTrue(start >= 0 && end > start);
        String lookup = pointed.substring(start, end);
        assertTrue(lookup.contains("Blocks.CAULDRON"));
        assertTrue(lookup.contains("VanillaCauldronMeta"));
        assertFalse("the vanilla cauldron identity must never be replaced",
                lookup.contains("CauldronStateBridge"));
        assertFalse(lookup.contains("setBlockState"));
    }

    @Test
    public void vanillaCauldronMixinMutatesWithFlagThreeAndComparatorRefresh()
            throws IOException {
        String mixin = source("mixin/CauldronMixin.java");
        assertTrue(mixin.contains("world.setBlockState(pos, cncBlockState(contents), 3)"));
        assertTrue(mixin.contains("world.updateComparatorOutputLevel(pos,"));
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

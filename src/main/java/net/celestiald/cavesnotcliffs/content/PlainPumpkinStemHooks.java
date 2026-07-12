package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/** Keeps vanilla pumpkin stems attached to, and growing, the plain pumpkin peer. */
public final class PlainPumpkinStemHooks {
    private PlainPumpkinStemHooks() {
    }

    public static boolean matchesFruit(Block actual, Block expected) {
        if (expected == Blocks.PUMPKIN) {
            Block plainPumpkin = plainPumpkin();
            return plainPumpkin != null && actual == plainPumpkin;
        }
        return actual == expected;
    }

    public static IBlockState fruitState(Block expected) {
        return expected == Blocks.PUMPKIN
                ? fruitState(expected, plainPumpkin())
                : expected.getDefaultState();
    }

    static boolean matchesFruit(Block actual, Block expected, Block plainPumpkin) {
        return expected == Blocks.PUMPKIN
                ? plainPumpkin != null && actual == plainPumpkin
                : actual == expected;
    }

    static IBlockState fruitState(Block expected, Block plainPumpkin) {
        if (expected != Blocks.PUMPKIN) {
            return expected.getDefaultState();
        }
        if (plainPumpkin == null) {
            throw new IllegalStateException(
                    "Plain pumpkin was not registered before pumpkin stem growth");
        }
        return plainPumpkin.getDefaultState();
    }

    private static Block plainPumpkin() {
        Block pumpkin = PlainPumpkinContent.PUMPKIN;
        return pumpkin != null ? pumpkin
                : ForgeRegistries.BLOCKS.getValue(CncRegistryIds.PUMPKIN);
    }
}

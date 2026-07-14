package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.Block;

/** Adds the plain pumpkin to vanilla's connection-exception checks. */
public final class PlainPumpkinConnectionHooks {
    private PlainPumpkinConnectionHooks() {
    }

    public static boolean isPlainPumpkin(Block block) {
        return block != null && CncRegistryIds.PUMPKIN.equals(block.getRegistryName());
    }
}

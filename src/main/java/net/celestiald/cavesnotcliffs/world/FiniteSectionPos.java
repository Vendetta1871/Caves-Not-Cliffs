package net.celestiald.cavesnotcliffs.world;

import net.minecraft.util.math.BlockPos;

/** Signed-Y 16-block section position used by legacy schema-1 decoration. */
final class FiniteSectionPos {
    private final int x;
    private final int y;
    private final int z;

    FiniteSectionPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    int getX() { return x; }
    int getY() { return y; }
    int getZ() { return z; }
    int getMinBlockX() { return x << 4; }
    int getMinBlockY() { return y << 4; }
    int getMinBlockZ() { return z << 4; }
    int getMaxBlockX() { return getMinBlockX() + 15; }
    int getMaxBlockY() { return getMinBlockY() + 15; }
    int getMaxBlockZ() { return getMinBlockZ() + 15; }

    BlockPos getCenterBlockPos() {
        return new BlockPos(getMinBlockX() + 8, getMinBlockY() + 8, getMinBlockZ() + 8);
    }

    boolean containsBlock(BlockPos position) {
        return position.getX() >= getMinBlockX() && position.getX() <= getMaxBlockX()
                && position.getY() >= getMinBlockY() && position.getY() <= getMaxBlockY()
                && position.getZ() >= getMinBlockZ() && position.getZ() <= getMaxBlockZ();
    }
}

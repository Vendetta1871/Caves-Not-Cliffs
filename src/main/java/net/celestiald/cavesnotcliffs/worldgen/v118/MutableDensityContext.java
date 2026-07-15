package net.celestiald.cavesnotcliffs.worldgen.v118;

/** Reusable coordinate context for the thread-confined terrain generation pipeline. */
final class MutableDensityContext implements DensityFunction.FunctionContext {
    private int blockX;
    private int blockY;
    private int blockZ;

    MutableDensityContext set(int blockX, int blockY, int blockZ) {
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        return this;
    }

    @Override
    public int blockX() {
        return blockX;
    }

    @Override
    public int blockY() {
        return blockY;
    }

    @Override
    public int blockZ() {
        return blockZ;
    }
}

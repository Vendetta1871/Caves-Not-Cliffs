package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.BitSet;

/** Java 1.18.2 {@code CarvingMask} indexing for one finite 16x384x16 column. */
public final class V118CarvingMask {
    private final int minY;
    private final int height;
    private final BitSet mask;

    public V118CarvingMask(int height, int minY) {
        if (height <= 0) {
            throw new IllegalArgumentException("height must be positive: " + height);
        }
        this.minY = minY;
        this.height = height;
        mask = new BitSet(256 * height);
    }

    public void set(int localX, int blockY, int localZ) {
        mask.set(index(localX, blockY, localZ));
    }

    public boolean get(int localX, int blockY, int localZ) {
        return mask.get(index(localX, blockY, localZ));
    }

    public int cardinality() {
        return mask.cardinality();
    }

    public long[] toLongArray() {
        return mask.toLongArray();
    }

    private int index(int localX, int blockY, int localZ) {
        if (localX < 0 || localX > 15) {
            throw new IndexOutOfBoundsException("localX outside 0..15: " + localX);
        }
        if (localZ < 0 || localZ > 15) {
            throw new IndexOutOfBoundsException("localZ outside 0..15: " + localZ);
        }
        if (blockY < minY || blockY >= minY + height) {
            throw new IndexOutOfBoundsException("blockY outside " + minY + ".."
                + (minY + height - 1) + ": " + blockY);
        }
        return localX | localZ << 4 | blockY - minY << 8;
    }
}

package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.Arrays;

/**
 * Immutable generated contents of one complete 1.18 Overworld column.
 *
 * <p>Materials are stored in Y-major order so each 16-block Cubic Chunks cube is one contiguous
 * 4,096-value range. Values use unsigned 16-bit generation-local IDs; translating those IDs to
 * runtime {@code IBlockState}s belongs at the later cube-slicing boundary. The storage chooses a
 * packed palette when that is smaller than a direct unsigned-short array.</p>
 *
 * <p>The legacy surface-biome plane has one value per X/Z block. The virtual biome volume follows
 * 1.18's quart resolution (4 x 96 x 4 samples) and therefore covers every block from Y=-64 through
 * Y=319 without depending on Minecraft 1.12's two-dimensional chunk biome array.</p>
 */
public final class TerrainColumn {
    public static final int WIDTH = 16;
    public static final int MIN_Y = -64;
    public static final int MAX_Y = 319;
    public static final int MAX_Y_EXCLUSIVE = MAX_Y + 1;
    public static final int HEIGHT = MAX_Y_EXCLUSIVE - MIN_Y;
    public static final int MIN_CUBE_Y = MIN_Y / 16;
    public static final int MAX_CUBE_Y = MAX_Y / 16;
    public static final int CUBE_COUNT = MAX_CUBE_Y - MIN_CUBE_Y + 1;
    public static final int BLOCKS_PER_CUBE = 16 * 16 * 16;
    public static final int BLOCK_COUNT = WIDTH * HEIGHT * WIDTH;

    public static final int QUART_WIDTH = WIDTH / 4;
    public static final int MIN_QUART_Y = MIN_Y >> 2;
    public static final int MAX_QUART_Y = MAX_Y >> 2;
    public static final int QUART_HEIGHT = MAX_QUART_Y - MIN_QUART_Y + 1;
    public static final int VIRTUAL_BIOME_COUNT = QUART_WIDTH * QUART_HEIGHT * QUART_WIDTH;
    public static final int SURFACE_BIOME_COUNT = WIDTH * WIDTH;
    public static final int MAX_STORAGE_ID = Character.MAX_VALUE;

    private static final long COLUMN_SHALLOW_BYTES = 64L;
    private static final long STORAGE_SHALLOW_BYTES = 32L;
    private static final long ARRAY_HEADER_BYTES = 16L;

    private final int columnX;
    private final int columnZ;
    private final MaterialStorage materials;
    private final char[] surfaceBiomeIds;
    private final char[] virtualBiomeIds;
    private final long[] scheduledFluidUpdates;
    private final long estimatedRetainedBytes;

    private TerrainColumn(Builder builder) {
        columnX = builder.columnX;
        columnZ = builder.columnZ;
        materials = MaterialStorage.compact(builder.materialIds);
        surfaceBiomeIds = builder.surfaceBiomeIds.clone();
        virtualBiomeIds = builder.virtualBiomeIds.clone();
        scheduledFluidUpdates = builder.scheduledFluidUpdates.clone();
        estimatedRetainedBytes = COLUMN_SHALLOW_BYTES + materials.estimatedRetainedBytes()
            + retainedArrayBytes(surfaceBiomeIds.length, Character.BYTES)
            + retainedArrayBytes(virtualBiomeIds.length, Character.BYTES)
            + retainedArrayBytes(scheduledFluidUpdates.length, Long.BYTES);
    }

    public static Builder builder(int columnX, int columnZ) {
        return new Builder(columnX, columnZ);
    }

    public int columnX() {
        return columnX;
    }

    public int columnZ() {
        return columnZ;
    }

    public long minBlockX() {
        return (long) columnX * WIDTH;
    }

    public long minBlockZ() {
        return (long) columnZ * WIDTH;
    }

    public int materialId(int localX, int worldY, int localZ) {
        return materials.get(blockIndex(localX, worldY, localZ));
    }

    /** Copies one complete cube in CubePrimer order: local Y, then Z, then X. */
    public void copyCubeMaterialIds(int cubeY, char[] destination, int destinationOffset) {
        checkCubeY(cubeY);
        checkDestination(destination, destinationOffset, BLOCKS_PER_CUBE);
        int sourceIndex = (cubeY * 16 - MIN_Y) * WIDTH * WIDTH;
        materials.copy(sourceIndex, destination, destinationOffset, BLOCKS_PER_CUBE);
    }

    public int surfaceBiomeId(int localX, int localZ) {
        return surfaceBiomeIds[surfaceIndex(localX, localZ)];
    }

    /** Resolves the quart sample containing the supplied local block coordinate. */
    public int virtualBiomeId(int localX, int worldY, int localZ) {
        checkLocal(localX, "localX");
        checkY(worldY);
        checkLocal(localZ, "localZ");
        return virtualBiomeIdAtQuart(localX >> 2, worldY >> 2, localZ >> 2);
    }

    public int virtualBiomeIdAtQuart(int localQuartX, int quartY, int localQuartZ) {
        return virtualBiomeIds[virtualBiomeIndex(localQuartX, quartY, localQuartZ)];
    }

    public boolean shouldScheduleFluidUpdate(int localX, int worldY, int localZ) {
        int index = blockIndex(localX, worldY, localZ);
        return (scheduledFluidUpdates[index >>> 6] & (1L << (index & 63))) != 0L;
    }

    public void copyCubeFluidUpdateFlags(int cubeY, boolean[] destination,
            int destinationOffset) {
        checkCubeY(cubeY);
        checkDestination(destination, destinationOffset, BLOCKS_PER_CUBE);
        int sourceIndex = (cubeY * 16 - MIN_Y) * WIDTH * WIDTH;
        for (int index = 0; index < BLOCKS_PER_CUBE; ++index) {
            int bitIndex = sourceIndex + index;
            destination[destinationOffset + index] =
                (scheduledFluidUpdates[bitIndex >>> 6] & (1L << (bitIndex & 63))) != 0L;
        }
    }

    /** Conservative, deterministic cache weight including primitive-array headers and alignment. */
    public long estimatedRetainedBytes() {
        return estimatedRetainedBytes;
    }

    public long materialStorageBytes() {
        return materials.payloadBytes();
    }

    public boolean hasPaletteEncodedMaterials() {
        return materials.isPaletteEncoded();
    }

    public int materialPaletteSize() {
        return materials.paletteSize();
    }

    private static int blockIndex(int localX, int worldY, int localZ) {
        checkLocal(localX, "localX");
        checkY(worldY);
        checkLocal(localZ, "localZ");
        return (worldY - MIN_Y) * WIDTH * WIDTH + localZ * WIDTH + localX;
    }

    private static int surfaceIndex(int localX, int localZ) {
        checkLocal(localX, "localX");
        checkLocal(localZ, "localZ");
        return localZ * WIDTH + localX;
    }

    private static int virtualBiomeIndex(int localQuartX, int quartY, int localQuartZ) {
        checkQuartLocal(localQuartX, "localQuartX");
        if (quartY < MIN_QUART_Y || quartY > MAX_QUART_Y) {
            throw new IndexOutOfBoundsException("quartY out of range [" + MIN_QUART_Y + ", "
                + MAX_QUART_Y + "]: " + quartY);
        }
        checkQuartLocal(localQuartZ, "localQuartZ");
        return (quartY - MIN_QUART_Y) * QUART_WIDTH * QUART_WIDTH
            + localQuartZ * QUART_WIDTH + localQuartX;
    }

    private static void checkLocal(int coordinate, String name) {
        if (coordinate < 0 || coordinate >= WIDTH) {
            throw new IndexOutOfBoundsException(name + " out of range [0, 15]: " + coordinate);
        }
    }

    private static void checkQuartLocal(int coordinate, String name) {
        if (coordinate < 0 || coordinate >= QUART_WIDTH) {
            throw new IndexOutOfBoundsException(name + " out of range [0, 3]: " + coordinate);
        }
    }

    private static void checkY(int worldY) {
        if (worldY < MIN_Y || worldY > MAX_Y) {
            throw new IndexOutOfBoundsException("worldY out of range [" + MIN_Y + ", " + MAX_Y
                + "]: " + worldY);
        }
    }

    private static void checkCubeY(int cubeY) {
        if (cubeY < MIN_CUBE_Y || cubeY > MAX_CUBE_Y) {
            throw new IndexOutOfBoundsException("cubeY out of range [" + MIN_CUBE_Y + ", "
                + MAX_CUBE_Y + "]: " + cubeY);
        }
    }

    private static void checkId(int id, String name) {
        if (id < 0 || id > MAX_STORAGE_ID) {
            throw new IllegalArgumentException(name + " must be an unsigned 16-bit value: " + id);
        }
    }

    private static void checkDestination(char[] destination, int offset, int length) {
        if (destination == null) {
            throw new NullPointerException("destination");
        }
        if (offset < 0 || (long) offset + length > destination.length) {
            throw new IndexOutOfBoundsException("destination range " + offset + ".."
                + ((long) offset + length) + " exceeds length " + destination.length);
        }
    }

    private static void checkDestination(boolean[] destination, int offset, int length) {
        if (destination == null) {
            throw new NullPointerException("destination");
        }
        if (offset < 0 || (long) offset + length > destination.length) {
            throw new IndexOutOfBoundsException("destination range " + offset + ".."
                + ((long) offset + length) + " exceeds length " + destination.length);
        }
    }

    private static long retainedArrayBytes(int length, int elementBytes) {
        long unaligned = ARRAY_HEADER_BYTES + (long) length * elementBytes;
        return (unaligned + 7L) & ~7L;
    }

    public static final class Builder {
        private final int columnX;
        private final int columnZ;
        private final char[] materialIds = new char[BLOCK_COUNT];
        private final char[] surfaceBiomeIds = new char[SURFACE_BIOME_COUNT];
        private final char[] virtualBiomeIds = new char[VIRTUAL_BIOME_COUNT];
        private final long[] scheduledFluidUpdates = new long[(BLOCK_COUNT + 63) >>> 6];

        private Builder(int columnX, int columnZ) {
            this.columnX = columnX;
            this.columnZ = columnZ;
        }

        public Builder setMaterialId(int localX, int worldY, int localZ, int materialId) {
            checkId(materialId, "materialId");
            materialIds[blockIndex(localX, worldY, localZ)] = (char) materialId;
            return this;
        }

        public Builder fillMaterialIds(int materialId) {
            checkId(materialId, "materialId");
            Arrays.fill(materialIds, (char) materialId);
            return this;
        }

        public Builder setSurfaceBiomeId(int localX, int localZ, int biomeId) {
            checkId(biomeId, "biomeId");
            surfaceBiomeIds[surfaceIndex(localX, localZ)] = (char) biomeId;
            return this;
        }

        public Builder fillSurfaceBiomeIds(int biomeId) {
            checkId(biomeId, "biomeId");
            Arrays.fill(surfaceBiomeIds, (char) biomeId);
            return this;
        }

        public Builder setVirtualBiomeId(int localX, int worldY, int localZ, int biomeId) {
            checkLocal(localX, "localX");
            checkY(worldY);
            checkLocal(localZ, "localZ");
            return setVirtualBiomeIdAtQuart(localX >> 2, worldY >> 2, localZ >> 2, biomeId);
        }

        public Builder setVirtualBiomeIdAtQuart(int localQuartX, int quartY, int localQuartZ,
                int biomeId) {
            checkId(biomeId, "biomeId");
            virtualBiomeIds[virtualBiomeIndex(localQuartX, quartY, localQuartZ)] = (char) biomeId;
            return this;
        }

        public Builder fillVirtualBiomeIds(int biomeId) {
            checkId(biomeId, "biomeId");
            Arrays.fill(virtualBiomeIds, (char) biomeId);
            return this;
        }

        public Builder setScheduledFluidUpdate(int localX, int worldY, int localZ,
                boolean scheduled) {
            int index = blockIndex(localX, worldY, localZ);
            long mask = 1L << (index & 63);
            if (scheduled) {
                scheduledFluidUpdates[index >>> 6] |= mask;
            } else {
                scheduledFluidUpdates[index >>> 6] &= ~mask;
            }
            return this;
        }

        public TerrainColumn build() {
            return new TerrainColumn(this);
        }
    }

    private abstract static class MaterialStorage {
        static MaterialStorage compact(char[] source) {
            int[] reversePalette = new int[MAX_STORAGE_ID + 1];
            Arrays.fill(reversePalette, -1);
            char[] paletteScratch = new char[MAX_STORAGE_ID + 1];
            int paletteSize = 0;
            for (char value : source) {
                if (reversePalette[value] < 0) {
                    reversePalette[value] = paletteSize;
                    paletteScratch[paletteSize++] = value;
                }
            }

            int bitsPerValue = paletteSize <= 1
                ? 0 : 32 - Integer.numberOfLeadingZeros(paletteSize - 1);
            long wordCount = bitsPerValue == 0 ? 0L
                : ((long) source.length * bitsPerValue + 63L) >>> 6;
            long packedPayloadBytes = (long) paletteSize * Character.BYTES
                + wordCount * Long.BYTES;
            long directPayloadBytes = (long) source.length * Character.BYTES;
            if (packedPayloadBytes >= directPayloadBytes) {
                return new DirectMaterialStorage(source.clone(), paletteSize);
            }

            char[] palette = Arrays.copyOf(paletteScratch, paletteSize);
            long[] packed = new long[(int) wordCount];
            if (bitsPerValue != 0) {
                long mask = (1L << bitsPerValue) - 1L;
                for (int index = 0; index < source.length; ++index) {
                    long paletteIndex = reversePalette[source[index]] & mask;
                    long bitIndex = (long) index * bitsPerValue;
                    int wordIndex = (int) (bitIndex >>> 6);
                    int bitOffset = (int) (bitIndex & 63L);
                    packed[wordIndex] |= paletteIndex << bitOffset;
                    if (bitOffset + bitsPerValue > Long.SIZE) {
                        packed[wordIndex + 1] |= paletteIndex >>> (Long.SIZE - bitOffset);
                    }
                }
            }
            return new PaletteMaterialStorage(palette, packed, bitsPerValue);
        }

        abstract char get(int index);

        abstract void copy(int sourceIndex, char[] destination, int destinationOffset, int length);

        abstract long payloadBytes();

        abstract long estimatedRetainedBytes();

        abstract boolean isPaletteEncoded();

        abstract int paletteSize();
    }

    private static final class DirectMaterialStorage extends MaterialStorage {
        private final char[] values;
        private final int distinctValues;

        private DirectMaterialStorage(char[] values, int distinctValues) {
            this.values = values;
            this.distinctValues = distinctValues;
        }

        @Override
        char get(int index) {
            return values[index];
        }

        @Override
        void copy(int sourceIndex, char[] destination, int destinationOffset, int length) {
            System.arraycopy(values, sourceIndex, destination, destinationOffset, length);
        }

        @Override
        long payloadBytes() {
            return (long) values.length * Character.BYTES;
        }

        @Override
        long estimatedRetainedBytes() {
            return STORAGE_SHALLOW_BYTES + retainedArrayBytes(values.length, Character.BYTES);
        }

        @Override
        boolean isPaletteEncoded() {
            return false;
        }

        @Override
        int paletteSize() {
            return distinctValues;
        }
    }

    private static final class PaletteMaterialStorage extends MaterialStorage {
        private final char[] palette;
        private final long[] packed;
        private final int bitsPerValue;
        private final long mask;

        private PaletteMaterialStorage(char[] palette, long[] packed, int bitsPerValue) {
            this.palette = palette;
            this.packed = packed;
            this.bitsPerValue = bitsPerValue;
            mask = bitsPerValue == 0 ? 0L : (1L << bitsPerValue) - 1L;
        }

        @Override
        char get(int index) {
            if (bitsPerValue == 0) {
                return palette[0];
            }
            long bitIndex = (long) index * bitsPerValue;
            int wordIndex = (int) (bitIndex >>> 6);
            int bitOffset = (int) (bitIndex & 63L);
            long paletteIndex = packed[wordIndex] >>> bitOffset;
            if (bitOffset + bitsPerValue > Long.SIZE) {
                paletteIndex |= packed[wordIndex + 1] << (Long.SIZE - bitOffset);
            }
            return palette[(int) (paletteIndex & mask)];
        }

        @Override
        void copy(int sourceIndex, char[] destination, int destinationOffset, int length) {
            for (int index = 0; index < length; ++index) {
                destination[destinationOffset + index] = get(sourceIndex + index);
            }
        }

        @Override
        long payloadBytes() {
            return (long) palette.length * Character.BYTES + (long) packed.length * Long.BYTES;
        }

        @Override
        long estimatedRetainedBytes() {
            return STORAGE_SHALLOW_BYTES
                + retainedArrayBytes(palette.length, Character.BYTES)
                + retainedArrayBytes(packed.length, Long.BYTES);
        }

        @Override
        boolean isPaletteEncoded() {
            return true;
        }

        @Override
        int paletteSize() {
            return palette.length;
        }
    }
}

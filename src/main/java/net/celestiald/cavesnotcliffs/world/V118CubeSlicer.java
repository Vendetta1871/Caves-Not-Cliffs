package net.celestiald.cavesnotcliffs.world;

import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import net.celestiald.cavesnotcliffs.worldgen.v118.TerrainColumn;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Material;

/** Copies one immutable 16x384x16 terrain column into CubicChunks' 16-cube view. */
final class V118CubeSlicer {
    private static final int CUBE_SIZE = 16;
    private static final int QUART_SAMPLES_PER_CUBE = CUBE_SIZE / 4;

    private final V118BlockStateMapper blockStates;
    private final V118BiomeMapper biomes;
    private final char[] materialIds = new char[TerrainColumn.BLOCKS_PER_CUBE];

    V118CubeSlicer(V118BlockStateMapper blockStates, V118BiomeMapper biomes) {
        if (blockStates == null) {
            throw new NullPointerException("blockStates");
        }
        if (biomes == null) {
            throw new NullPointerException("biomes");
        }
        this.blockStates = blockStates;
        this.biomes = biomes;
    }

    void slice(TerrainColumn column, int cubeY, CubePrimer primer) {
        column.copyCubeMaterialIds(cubeY, materialIds, 0);
        for (int index = 0; index < materialIds.length; ++index) {
            // CubePrimer's private index is (localY << 8) | (localZ << 4) | localX.
            int localY = index >>> 8;
            int localZ = (index >>> 4) & 15;
            int localX = index & 15;
            primer.setBlockState(localX, localY, localZ,
                blockStates.stateFor(materialIds[index]));
        }

        // CubicChunks 1.12 stores one vertical biome plane per cube. It cannot represent the four
        // separate quart-Y samples inside a 16-block cube, so the center quart sample is projected
        // into CubePrimer while TerrainColumn retains the exact 4x96x4 volume for later queries.
        int centerQuartY = cubeY * QUART_SAMPLES_PER_CUBE
            + QUART_SAMPLES_PER_CUBE / 2;
        for (int localQuartZ = 0; localQuartZ < TerrainColumn.QUART_WIDTH; ++localQuartZ) {
            for (int localQuartX = 0; localQuartX < TerrainColumn.QUART_WIDTH; ++localQuartX) {
                int biomeId = column.virtualBiomeIdAtQuart(localQuartX, centerQuartY,
                    localQuartZ);
                primer.setBiome(localQuartX, 0, localQuartZ, biomes.biomeFor(biomeId));
            }
        }
    }

    void forEachScheduledFluid(TerrainColumn column, int cubeY, FluidConsumer consumer) {
        if (consumer == null) {
            throw new NullPointerException("consumer");
        }
        column.copyCubeMaterialIds(cubeY, materialIds, 0);
        int minY = cubeY * CUBE_SIZE;
        for (int index = 0; index < materialIds.length; ++index) {
            int localY = index >>> 8;
            int localZ = (index >>> 4) & 15;
            int localX = index & 15;
            if (!column.shouldScheduleFluidUpdate(localX, minY + localY, localZ)) {
                continue;
            }
            V118Material material = V118Material.fromStorageId(materialIds[index]);
            if (material == V118Material.WATER || material == V118Material.LAVA) {
                consumer.accept(localX, localY, localZ, material);
            }
        }
    }

    interface FluidConsumer {
        void accept(int localX, int localY, int localZ, V118Material material);
    }
}

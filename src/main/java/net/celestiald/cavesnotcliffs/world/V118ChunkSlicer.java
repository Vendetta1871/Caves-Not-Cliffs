package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavebiomes.api.ExtendedChunkAPI;
import net.celestiald.cavesnotcliffs.worldgen.v118.TerrainColumn;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Copies one immutable 16x384x16 terrain column into signed-Y chunk sections. */
final class V118ChunkSlicer {
    private static final Logger LOGGER = LogManager.getLogger("CavesNotCliffs/ModdedBiomes");
    private static final int CUBE_SIZE = 16;

    private final V118BlockStateMapper blockStates;
    private final V118BiomeMapper biomes;
    private final ModdedBiomeOverlay overlay;
    private final ModdedBiomeSurface moddedSurface;
    private final char[] materialIds = new char[TerrainColumn.BLOCKS_PER_CUBE];
    private final IBlockState[] sectionStates = new IBlockState[TerrainColumn.BLOCKS_PER_CUBE];
    private boolean warnedAboutWideModdedBiome;

    V118ChunkSlicer(V118BlockStateMapper blockStates, V118BiomeMapper biomes) {
        this(blockStates, biomes, ModdedBiomeOverlay.disabled());
    }

    V118ChunkSlicer(V118BlockStateMapper blockStates, V118BiomeMapper biomes,
            ModdedBiomeOverlay overlay) {
        if (blockStates == null) {
            throw new NullPointerException("blockStates");
        }
        if (biomes == null) {
            throw new NullPointerException("biomes");
        }
        if (overlay == null) {
            throw new NullPointerException("overlay");
        }
        this.blockStates = blockStates;
        this.biomes = biomes;
        this.overlay = overlay;
        moddedSurface = new ModdedBiomeSurface(biomes);
    }

    void slice(TerrainColumn column, int sectionY, Chunk chunk, boolean skylight) {
        sliceTerrainBlocks(column, sectionY, chunk, skylight);
    }

    void fillStructureTerrain(TerrainColumn column, ChunkPrimer primer) {
        if (primer == null) {
            throw new NullPointerException("primer");
        }
        for (int cubeY = 0; cubeY < 16; ++cubeY) {
            column.copyCubeMaterialIds(cubeY, materialIds, 0);
            int minY = cubeY * CUBE_SIZE;
            for (int index = 0; index < materialIds.length; ++index) {
                int localY = index >>> 8;
                int localZ = (index >>> 4) & 15;
                int localX = index & 15;
                primer.setBlockState(localX, minY + localY, localZ,
                    blockStates.stateFor(materialIds[index]));
            }
        }
    }

    void sliceStructureBlocks(ChunkPrimer structureColumn, TerrainColumn column, int sectionY,
            Chunk chunk, boolean skylight) {
        if (structureColumn == null) {
            throw new NullPointerException("structureColumn");
        }
        if (sectionY < 0 || sectionY >= 16) {
            throw new IllegalArgumentException("1.12 structures only cover section Y 0..15: "
                + sectionY);
        }
        int minY = sectionY * CUBE_SIZE;
        ExtendedBlockStorage section = ExtendedChunkAPI.getOrCreateSection(chunk, minY, skylight);
        copyStructureSectionStates(structureColumn, sectionY, sectionStates);
        for (int index = 0; index < sectionStates.length; ++index) {
            section.set(index & 15, index >>> 8, (index >>> 4) & 15,
                    sectionStates[index]);
        }
    }

    private void sliceTerrainBlocks(TerrainColumn column, int sectionY, Chunk chunk,
            boolean skylight) {
        copySectionStates(column, sectionY, sectionStates);
        ExtendedBlockStorage section = ExtendedChunkAPI.getOrCreateSection(
                chunk, sectionY * CUBE_SIZE, skylight);
        for (int index = 0; index < sectionStates.length; ++index) {
            int localY = index >>> 8;
            int localZ = (index >>> 4) & 15;
            int localX = index & 15;
            section.set(localX, localY, localZ, sectionStates[index]);
        }
    }

    void copySectionStates(TerrainColumn column, int sectionY, IBlockState[] destination) {
        requireSectionDestination(destination);
        column.copyCubeMaterialIds(sectionY, materialIds, 0);
        for (int index = 0; index < materialIds.length; ++index) {
            destination[index] = blockStates.stateFor(materialIds[index]);
        }
    }

    void copyStructureSectionStates(ChunkPrimer structureColumn, int sectionY,
            IBlockState[] destination) {
        requireSectionDestination(destination);
        if (sectionY < 0 || sectionY >= 16) {
            throw new IllegalArgumentException("1.12 structures only cover section Y 0..15: "
                    + sectionY);
        }
        int minY = sectionY * CUBE_SIZE;
        for (int index = 0; index < destination.length; ++index) {
            int localY = index >>> 8;
            int localZ = (index >>> 4) & 15;
            int localX = index & 15;
            destination[index] = structureColumn.getBlockState(
                    localX, minY + localY, localZ);
        }
    }

    private static void requireSectionDestination(IBlockState[] destination) {
        if (destination == null || destination.length != TerrainColumn.BLOCKS_PER_CUBE) {
            throw new IllegalArgumentException("Expected a complete 16x16x16 section array");
        }
    }

    void projectSurfaceBiomes(TerrainColumn column, byte[] legacyBiomeArray) {
        if (legacyBiomeArray == null
                || legacyBiomeArray.length != TerrainColumn.SURFACE_BIOME_COUNT) {
            throw new IllegalArgumentException("Expected a complete 16x16 chunk biome array");
        }
        int[] biomeIds = projectedSurfaceBiomeIds(column);
        for (int index = 0; index < biomeIds.length; ++index) {
            int biomeId = biomeIds[index];
            if (biomeId < 0 || biomeId > 255) {
                throw new IllegalStateException("Projected biome id does not fit the legacy "
                    + "chunk biome array: " + biomeId);
            }
            legacyBiomeArray[index] = (byte) biomeId;
        }
    }

    void projectSurfaceBiomes(TerrainColumn column, Chunk chunk) {
        if (chunk == null) {
            throw new NullPointerException("chunk");
        }
        int[] biomeIds = projectedSurfaceBiomeIds(column);
        V118Biome[] hosts = surfaceHosts(column);
        Biome[] claims = applyModdedBiomeOverlay(biomeIds, hosts, chunk.x, chunk.z);
        if (claims != null && overlay.isClimateHosted()) {
            moddedSurface.apply(chunk, hosts, claims);
        }
        if (ExtendedBiomeStorageCompat.replaceSurfaceBiomes(chunk, biomeIds)) {
            return;
        }
        byte[] legacyBiomeArray = chunk.getBiomeArray();
        if (legacyBiomeArray == null
                || legacyBiomeArray.length != TerrainColumn.SURFACE_BIOME_COUNT) {
            throw new IllegalStateException("Expected a complete 16x16 chunk biome array");
        }
        for (int index = 0; index < biomeIds.length; ++index) {
            int biomeId = biomeIds[index];
            if (biomeId < 0 || biomeId > 255) {
                throw new IllegalStateException("Projected biome id does not fit the legacy "
                    + "chunk biome array: " + biomeId);
            }
            legacyBiomeArray[index] = (byte) biomeId;
        }
    }

    /**
     * Lets modded biomes claim the surface plane so biome-driven mod content — tree worldgen,
     * grass tints, fog — works in native-profile worlds. Returns the claims that were written
     * (null per unclaimed column), or null when nothing was claimed.
     */
    Biome[] applyModdedBiomeOverlay(int[] biomeIds, V118Biome[] hosts, int chunkX,
            int chunkZ) {
        if (!overlay.isEnabled()) {
            return null;
        }
        Biome[] modded = overlay.moddedBlockBiomes(hosts,
            chunkX << 4, chunkZ << 4, TerrainColumn.WIDTH, TerrainColumn.WIDTH);
        if (modded == null) {
            return null;
        }
        boolean extendedStorage = ExtendedBiomeStorageCompat.isAvailable();
        Biome[] applied = new Biome[biomeIds.length];
        boolean any = false;
        for (int index = 0; index < biomeIds.length && index < modded.length; ++index) {
            Biome biome = modded[index];
            if (biome == null) {
                continue;
            }
            int biomeId = Biome.getIdForBiome(biome);
            if (biomeId < 0) {
                continue;
            }
            if (biomeId > 255 && !extendedStorage) {
                // The legacy byte array cannot hold the id and RoughlyEnoughIDs is absent;
                // keep the 1.18 projection for this cell instead of crashing below.
                if (!warnedAboutWideModdedBiome) {
                    LOGGER.warn("Modded biome {} has id {} which needs extended biome storage"
                            + " (e.g. RoughlyEnoughIDs); keeping the 1.18 projection there",
                            biome.getRegistryName(), biomeId);
                    warnedAboutWideModdedBiome = true;
                }
                continue;
            }
            biomeIds[index] = biomeId;
            applied[index] = biome;
            any = true;
        }
        return any ? applied : null;
    }

    /** The 1.18 surface biome of every column, indexed {@code z * 16 + x}. */
    static V118Biome[] surfaceHosts(TerrainColumn column) {
        V118Biome[] hosts = new V118Biome[TerrainColumn.SURFACE_BIOME_COUNT];
        for (int localZ = 0; localZ < TerrainColumn.WIDTH; ++localZ) {
            for (int localX = 0; localX < TerrainColumn.WIDTH; ++localX) {
                hosts[localZ * TerrainColumn.WIDTH + localX] =
                    ModdedBiomeOverlay.hostFor(column.surfaceBiomeId(localX, localZ));
            }
        }
        return hosts;
    }

    private int[] projectedSurfaceBiomeIds(TerrainColumn column) {
        if (column == null) {
            throw new NullPointerException("column");
        }
        int[] biomeIds = new int[TerrainColumn.SURFACE_BIOME_COUNT];
        for (int localZ = 0; localZ < TerrainColumn.WIDTH; ++localZ) {
            for (int localX = 0; localX < TerrainColumn.WIDTH; ++localX) {
                Biome biome = projectedSurfaceBiome(column, localX, localZ);
                int biomeId = Biome.getIdForBiome(biome);
                if (biomeId < 0) {
                    throw new IllegalStateException("Projected biome is not registered: " + biome);
                }
                biomeIds[localZ * TerrainColumn.WIDTH + localX] = biomeId;
            }
        }
        return biomeIds;
    }

    /** Registry projection used by the legacy chunk plane before byte-ID serialization. */
    Biome projectedSurfaceBiome(TerrainColumn column, int localX, int localZ) {
        if (column == null) {
            throw new NullPointerException("column");
        }
        return biomes.biomeFor(column.surfaceBiomeId(localX, localZ));
    }

    void forEachScheduledFluid(TerrainColumn column, int cubeY, FluidConsumer consumer) {
        if (consumer == null) {
            throw new NullPointerException("consumer");
        }
        int minY = cubeY * CUBE_SIZE;
        for (int index = column.nextScheduledFluidUpdateIndex(cubeY, 0);
                index >= 0;
                index = column.nextScheduledFluidUpdateIndex(cubeY, index + 1)) {
            int localY = index >>> 8;
            int localZ = (index >>> 4) & 15;
            int localX = index & 15;
            V118Material material = V118Material.fromStorageId(
                column.materialId(localX, minY + localY, localZ));
            if (material == V118Material.WATER || material == V118Material.LAVA) {
                consumer.accept(localX, localY, localZ, material);
            }
        }
    }

    interface FluidConsumer {
        void accept(int localX, int localY, int localZ, V118Material material);
    }
}

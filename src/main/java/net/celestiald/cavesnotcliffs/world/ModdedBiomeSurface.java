package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavebiomes.api.ExtendedChunkAPI;
import net.celestiald.cavebiomes.api.WorldHeightAPI;
import net.celestiald.cavesnotcliffs.worldgen.v118.TerrainColumn;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

/**
 * Dresses the 1.18 surface of a claimed column in the modded biome's soil.
 *
 * <p>Only the soil changes: the top block and filler run the 1.18 surface rules laid down for
 * the host (grass and dirt, sand, mycelium) become the modded biome's {@code topBlock} and
 * {@code fillerBlock}. Stone, gravel, terracotta, snow, podzol and every other surface-rule
 * choice stay 1.18, and so does the shape of the land. A modded biome whose soil is not a plain
 * full block keeps the host's soil.</p>
 */
final class ModdedBiomeSurface {
    /** Deepest soil run replaced, matching 1.12's biome filler depth. */
    static final int MAX_DEPTH = 6;
    /** Water deeper than this hides the floor; 1.12 never puts topBlock under deep water. */
    static final int MAX_WATER_DEPTH = 4;

    private final V118BiomeMapper biomes;

    ModdedBiomeSurface(V118BiomeMapper biomes) {
        if (biomes == null) {
            throw new NullPointerException("biomes");
        }
        this.biomes = biomes;
    }

    /** Signed-Y section lookup: the storage section holding {@code worldY}, or null. */
    interface Sections {
        ExtendedBlockStorage at(int worldY);
    }

    /** {@code hosts} and {@code claims} are 16x16 column grids indexed {@code z * 16 + x}. */
    void apply(Chunk chunk, V118Biome[] hosts, Biome[] claims) {
        apply(worldY -> ExtendedChunkAPI.getSection(chunk, worldY), hosts, claims);
    }

    void apply(Sections chunk, V118Biome[] hosts, Biome[] claims) {
        for (int index = 0; index < TerrainColumn.SURFACE_BIOME_COUNT; ++index) {
            Biome claim = claims[index];
            if (claim == null || hosts[index] == null) {
                continue;
            }
            Biome host = biomes.biomeFor(hosts[index]);
            IBlockState hostTop = host.topBlock;
            IBlockState hostFiller = host.fillerBlock;
            IBlockState top = soil(claim.topBlock) ? claim.topBlock : hostTop;
            IBlockState filler = soil(claim.fillerBlock) ? claim.fillerBlock : hostFiller;
            boolean topSoil = isReplaceableTop(hostTop);
            boolean fillerSoil = isReplaceableFiller(hostFiller);
            if ((topSoil && !sameState(top, hostTop))
                    || (fillerSoil && !sameState(filler, hostFiller))) {
                // Unchanged soil is still matched so the walk reaches the layer below it.
                dress(chunk, index & 15, index >>> 4, topSoil ? hostTop : null, top,
                        fillerSoil ? hostFiller : null, filler);
            }
        }
    }

    private static void dress(Sections chunk, int localX, int localZ, IBlockState hostTop,
            IBlockState top, IBlockState hostFiller, IBlockState filler) {
        int water = 0;
        int depth = 0;
        int minY = Math.max(TerrainColumn.MIN_Y, WorldHeightAPI.getMinY());
        for (int y = Math.min(TerrainColumn.MAX_Y, WorldHeightAPI.getMaxY() - 1); y >= minY;
                --y) {
            ExtendedBlockStorage section = chunk.at(y);
            if (section == null || section.isEmpty()) {
                y &= ~15;
                continue;
            }
            IBlockState state = section.get(localX, y & 15, localZ);
            Material material = state.getMaterial();
            if (depth == 0) {
                if (material == Material.AIR) {
                    continue;
                }
                if (material.isLiquid()) {
                    if (++water > MAX_WATER_DEPTH) {
                        return;
                    }
                    continue;
                }
            }
            if (hostTop != null && sameState(state, hostTop)) {
                section.set(localX, y & 15, localZ, top);
            } else if (hostFiller != null && sameState(state, hostFiller)) {
                section.set(localX, y & 15, localZ, filler);
            } else {
                return;
            }
            if (++depth >= MAX_DEPTH) {
                return;
            }
        }
    }

    /**
     * A modded soil block usable in place of the host's: a full, opaque, earthy cube (dirt,
     * grass, sand, clay, stone or snow materials) without a tile entity.
     */
    static boolean soil(IBlockState state) {
        if (state == null) {
            return false;
        }
        Material material = state.getMaterial();
        boolean earthy = material == Material.GROUND || material == Material.GRASS
                || material == Material.SAND || material == Material.CLAY
                || material == Material.ROCK || material == Material.CRAFTED_SNOW;
        return earthy && state.isFullCube() && state.isOpaqueCube()
                && !state.getBlock().hasTileEntity(state);
    }

    private static boolean isReplaceableTop(IBlockState state) {
        Block block = state.getBlock();
        return block == Blocks.GRASS || block == Blocks.SAND || block == Blocks.MYCELIUM
                || block == Blocks.DIRT;
    }

    private static boolean isReplaceableFiller(IBlockState state) {
        Block block = state.getBlock();
        return block == Blocks.DIRT || block == Blocks.SAND;
    }

    /** Same block and metadata; grass's snowy flag is render-only and not stored. */
    static boolean sameState(IBlockState first, IBlockState second) {
        return first.getBlock() == second.getBlock()
                && first.getBlock().getMetaFromState(first)
                    == second.getBlock().getMetaFromState(second);
    }
}

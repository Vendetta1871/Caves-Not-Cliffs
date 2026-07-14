package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.content.MountainBiomeContent.Definition;
import net.celestiald.cavesnotcliffs.worldgen.v118.TerrainColumn;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Material;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class V118ChunkSlicerTest {
    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void copiesAll4096ValuesInSectionYMajorOrder() {
        int cubeY = -1;
        TerrainColumn.Builder builder = TerrainColumn.builder(3, -7)
            .fillMaterialIds(V118Material.AIR.storageId())
            .fillSurfaceBiomeIds(V118Biome.PLAINS.ordinal())
            .fillVirtualBiomeIds(V118Biome.PLAINS.ordinal());
        V118Material[] materials = V118Material.values();
        for (int localY = 0; localY < 16; ++localY) {
            for (int localZ = 0; localZ < 16; ++localZ) {
                for (int localX = 0; localX < 16; ++localX) {
                    int linearIndex = (localY << 8) | (localZ << 4) | localX;
                    builder.setMaterialId(localX, cubeY * 16 + localY, localZ,
                        materials[linearIndex % materials.length].storageId());
                }
            }
        }
        V118BlockStateMapper blockMapper = blockMapper();
        V118ChunkSlicer slicer = new V118ChunkSlicer(blockMapper, biomeMapper());
        IBlockState[] section = new IBlockState[TerrainColumn.BLOCKS_PER_CUBE];
        slicer.copySectionStates(builder.build(), cubeY, section);

        for (int localY = 0; localY < 16; ++localY) {
            for (int localZ = 0; localZ < 16; ++localZ) {
                for (int localX = 0; localX < 16; ++localX) {
                    int linearIndex = (localY << 8) | (localZ << 4) | localX;
                    assertEquals(blockMapper.stateFor(materials[linearIndex % materials.length]),
                        section[linearIndex]);
                }
            }
        }
    }

    @Test
    public void emitsOnlyFlaggedWaterAndLavaInsideRequestedCube() {
        TerrainColumn.Builder builder = TerrainColumn.builder(0, 0)
            .fillMaterialIds(V118Material.AIR.storageId())
            .fillSurfaceBiomeIds(V118Biome.PLAINS.ordinal())
            .fillVirtualBiomeIds(V118Biome.PLAINS.ordinal());
        builder.setMaterialId(7, -8, 11, V118Material.WATER.storageId())
            .setScheduledFluidUpdate(7, -8, 11, true);
        builder.setMaterialId(15, -1, 15, V118Material.LAVA.storageId())
            .setScheduledFluidUpdate(15, -1, 15, true);
        builder.setMaterialId(2, -3, 4, V118Material.STONE.storageId())
            .setScheduledFluidUpdate(2, -3, 4, true);
        builder.setMaterialId(1, 0, 1, V118Material.WATER.storageId())
            .setScheduledFluidUpdate(1, 0, 1, true);

        final List<String> fluids = new ArrayList<String>();
        new V118ChunkSlicer(blockMapper(), biomeMapper()).forEachScheduledFluid(
            builder.build(), -1, (x, y, z, material) ->
                fluids.add(x + ":" + y + ":" + z + ":" + material));

        assertEquals(Arrays.asList(
            "7:8:11:WATER",
            "15:15:15:LAVA"), fluids);
    }

    @Test
    public void structurePrimerStartsWithNativeTerrainAndCopiesOnlyTheRequestedCube() {
        TerrainColumn.Builder builder = TerrainColumn.builder(-8, 12)
            .fillMaterialIds(V118Material.AIR.storageId())
            .fillSurfaceBiomeIds(V118Biome.PLAINS.ordinal())
            .fillVirtualBiomeIds(V118Biome.PLAINS.ordinal());
        V118Material[] materials = V118Material.values();
        for (int worldY = 0; worldY < 256; ++worldY) {
            for (int localZ = 0; localZ < 16; ++localZ) {
                for (int localX = 0; localX < 16; ++localX) {
                    int index = (worldY << 8) | (localZ << 4) | localX;
                    builder.setMaterialId(localX, worldY, localZ,
                        materials[index % materials.length].storageId());
                }
            }
        }

        V118BlockStateMapper mapper = blockMapper();
        V118ChunkSlicer slicer = new V118ChunkSlicer(mapper, biomeMapper());
        TerrainColumn column = builder.build();
        ChunkPrimer structureColumn = new ChunkPrimer();
        slicer.fillStructureTerrain(column, structureColumn);
        for (int worldY = 0; worldY < 256; ++worldY) {
            for (int localZ = 0; localZ < 16; ++localZ) {
                for (int localX = 0; localX < 16; ++localX) {
                    int index = (worldY << 8) | (localZ << 4) | localX;
                    assertEquals(mapper.stateFor(materials[index % materials.length]),
                        structureColumn.getBlockState(localX, worldY, localZ));
                }
            }
        }

        structureColumn.setBlockState(3, 173, 7, Blocks.DIAMOND_BLOCK.getDefaultState());
        IBlockState[] section = new IBlockState[TerrainColumn.BLOCKS_PER_CUBE];
        slicer.copyStructureSectionStates(structureColumn, 10, section);
        assertSame(Blocks.DIAMOND_BLOCK,
            section[(13 << 8) | (7 << 4) | 3].getBlock());
        assertEquals(structureColumn.getBlockState(15, 175, 15),
            section[(15 << 8) | (15 << 4) | 15]);
    }

    @Test
    public void projectsSurfaceBiomesIntoLegacyChunkZMajorOrder() {
        TerrainColumn.Builder builder = TerrainColumn.builder(-3, 5)
            .fillMaterialIds(V118Material.AIR.storageId())
            .fillSurfaceBiomeIds(V118Biome.PLAINS.ordinal())
            .fillVirtualBiomeIds(V118Biome.PLAINS.ordinal());
        builder.setSurfaceBiomeId(0, 0, V118Biome.DESERT.ordinal());
        builder.setSurfaceBiomeId(15, 0, V118Biome.FROZEN_PEAKS.ordinal());
        builder.setSurfaceBiomeId(0, 1, V118Biome.DESERT.ordinal());
        builder.setSurfaceBiomeId(15, 15, V118Biome.FROZEN_PEAKS.ordinal());

        byte[] projected = new byte[TerrainColumn.SURFACE_BIOME_COUNT];
        new V118ChunkSlicer(blockMapper(), biomeMapper())
            .projectSurfaceBiomes(builder.build(), projected);

        assertEquals(Biome.getIdForBiome(Biomes.DESERT), projected[0] & 255);
        assertEquals(Biome.getIdForBiome(Biomes.ICE_MOUNTAINS), projected[15] & 255);
        assertEquals(Biome.getIdForBiome(Biomes.DESERT), projected[16] & 255);
        assertEquals(Biome.getIdForBiome(Biomes.PLAINS), projected[17] & 255);
        assertEquals(Biome.getIdForBiome(Biomes.ICE_MOUNTAINS), projected[255] & 255);
    }

    @Test
    public void projectsEveryNativeMountainBiomeThroughTheLegacySurfacePath() {
        V118ChunkSlicer slicer = new V118ChunkSlicer(blockMapper(), biomeMapperWithMountains());
        for (Definition definition : Definition.values()) {
            TerrainColumn column = TerrainColumn.builder(2, -4)
                .fillMaterialIds(V118Material.AIR.storageId())
                .fillSurfaceBiomeIds(definition.virtualBiome().ordinal())
                .fillVirtualBiomeIds(definition.virtualBiome().ordinal())
                .build();
            assertSame(definition.name(), definition.biome(),
                slicer.projectedSurfaceBiome(column, 15, 15));
        }
    }

    private static V118BlockStateMapper blockMapper() {
        return new V118BlockStateMapper(
            Blocks.COAL_BLOCK.getDefaultState(), Blocks.BRICK_BLOCK.getDefaultState(),
            Blocks.GOLD_ORE.getDefaultState(), Blocks.GOLD_BLOCK.getDefaultState(),
            Blocks.IRON_ORE.getDefaultState(), Blocks.IRON_BLOCK.getDefaultState(),
            Blocks.QUARTZ_BLOCK.getDefaultState(), Blocks.WEB.getDefaultState());
    }

    private static V118BiomeMapper biomeMapper() {
        Biome[] biomes = new Biome[V118Biome.values().length];
        Arrays.fill(biomes, Biomes.PLAINS);
        biomes[V118Biome.DESERT.ordinal()] = Biomes.DESERT;
        biomes[V118Biome.FROZEN_PEAKS.ordinal()] = Biomes.ICE_MOUNTAINS;
        return new V118BiomeMapper(biomes);
    }

    private static V118BiomeMapper biomeMapperWithMountains() {
        Biome[] biomes = new Biome[V118Biome.values().length];
        Arrays.fill(biomes, Biomes.PLAINS);
        for (Definition definition : Definition.values()) {
            biomes[definition.virtualBiome().ordinal()] = definition.biome();
        }
        return new V118BiomeMapper(biomes);
    }
}

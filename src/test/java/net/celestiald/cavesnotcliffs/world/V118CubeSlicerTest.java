package net.celestiald.cavesnotcliffs.world;

import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import net.celestiald.cavesnotcliffs.content.MountainBiomeContent.Definition;
import net.celestiald.cavesnotcliffs.worldgen.v118.TerrainColumn;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Material;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class V118CubeSlicerTest {
    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void copiesAll4096ValuesInCubePrimerYMajorOrder() {
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
        builder.setVirtualBiomeIdAtQuart(0, -2, 0, V118Biome.DESERT.ordinal());
        builder.setVirtualBiomeIdAtQuart(3, -2, 3, V118Biome.FROZEN_PEAKS.ordinal());

        V118BlockStateMapper blockMapper = blockMapper();
        V118CubeSlicer slicer = new V118CubeSlicer(blockMapper, biomeMapper());
        CubePrimer primer = new CubePrimer();
        slicer.slice(builder.build(), cubeY, primer);

        for (int localY = 0; localY < 16; ++localY) {
            for (int localZ = 0; localZ < 16; ++localZ) {
                for (int localX = 0; localX < 16; ++localX) {
                    int linearIndex = (localY << 8) | (localZ << 4) | localX;
                    assertEquals(blockMapper.stateFor(materials[linearIndex % materials.length]),
                        primer.getBlockState(localX, localY, localZ));
                }
            }
        }
        assertSame(Biomes.DESERT, primer.getBiome(0, 0, 0));
        assertSame(Biomes.ICE_MOUNTAINS, primer.getBiome(3, 15, 3));
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
        new V118CubeSlicer(blockMapper(), biomeMapper()).forEachScheduledFluid(
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
        V118CubeSlicer slicer = new V118CubeSlicer(mapper, biomeMapper());
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
        CubePrimer cube = new CubePrimer();
        slicer.sliceStructureBlocks(structureColumn, column, 10, cube);
        assertSame(Blocks.DIAMOND_BLOCK, cube.getBlockState(3, 13, 7).getBlock());
        assertEquals(structureColumn.getBlockState(15, 175, 15),
            cube.getBlockState(15, 15, 15));
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
        new V118CubeSlicer(blockMapper(), biomeMapper())
            .projectSurfaceBiomes(builder.build(), projected);

        assertEquals(Biome.getIdForBiome(Biomes.DESERT), projected[0] & 255);
        assertEquals(Biome.getIdForBiome(Biomes.ICE_MOUNTAINS), projected[15] & 255);
        assertEquals(Biome.getIdForBiome(Biomes.DESERT), projected[16] & 255);
        assertEquals(Biome.getIdForBiome(Biomes.PLAINS), projected[17] & 255);
        assertEquals(Biome.getIdForBiome(Biomes.ICE_MOUNTAINS), projected[255] & 255);
    }

    @Test
    public void projectsEveryNativeMountainBiomeThroughBothThreeDimensionalAndSurfacePaths() {
        V118CubeSlicer slicer = new V118CubeSlicer(blockMapper(), biomeMapperWithMountains());
        for (Definition definition : Definition.values()) {
            TerrainColumn column = TerrainColumn.builder(2, -4)
                .fillMaterialIds(V118Material.AIR.storageId())
                .fillSurfaceBiomeIds(definition.virtualBiome().ordinal())
                .fillVirtualBiomeIds(definition.virtualBiome().ordinal())
                .build();
            CubePrimer primer = new CubePrimer();

            slicer.slice(column, 0, primer);

            assertSame(definition.name(), definition.biome(), primer.getBiome(0, 0, 0));
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

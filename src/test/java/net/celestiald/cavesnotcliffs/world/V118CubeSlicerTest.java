package net.celestiald.cavesnotcliffs.world;

import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import net.celestiald.cavesnotcliffs.worldgen.v118.TerrainColumn;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Material;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.world.biome.Biome;
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
        builder.setMaterialId(0, -16, 0, V118Material.STONE.storageId());
        builder.setMaterialId(15, -16, 0, V118Material.BEDROCK.storageId());
        builder.setMaterialId(0, -16, 1, V118Material.GRANITE.storageId());
        builder.setMaterialId(7, -8, 11, V118Material.WATER.storageId());
        builder.setMaterialId(15, -1, 15, V118Material.LAVA.storageId());
        builder.setVirtualBiomeIdAtQuart(0, -2, 0, V118Biome.DESERT.ordinal());
        builder.setVirtualBiomeIdAtQuart(3, -2, 3, V118Biome.FROZEN_PEAKS.ordinal());

        V118CubeSlicer slicer = new V118CubeSlicer(blockMapper(), biomeMapper());
        CubePrimer primer = new CubePrimer();
        slicer.slice(builder.build(), cubeY, primer);

        assertSame(Blocks.STONE.getDefaultState(), primer.getBlockState(0, 0, 0));
        assertSame(Blocks.BEDROCK.getDefaultState(), primer.getBlockState(15, 0, 0));
        assertEquals(Blocks.STONE.getDefaultState().withProperty(
            net.minecraft.block.BlockStone.VARIANT,
            net.minecraft.block.BlockStone.EnumType.GRANITE),
            primer.getBlockState(0, 0, 1));
        assertSame(Blocks.WATER.getDefaultState(), primer.getBlockState(7, 8, 11));
        assertSame(Blocks.LAVA.getDefaultState(), primer.getBlockState(15, 15, 15));
        assertSame(Blocks.AIR.getDefaultState(), primer.getBlockState(14, 15, 15));
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

    private static V118BlockStateMapper blockMapper() {
        return new V118BlockStateMapper(
            Blocks.COAL_BLOCK.getDefaultState(), Blocks.BRICK_BLOCK.getDefaultState(),
            Blocks.GOLD_ORE.getDefaultState(), Blocks.GOLD_BLOCK.getDefaultState(),
            Blocks.IRON_ORE.getDefaultState(), Blocks.IRON_BLOCK.getDefaultState());
    }

    private static V118BiomeMapper biomeMapper() {
        Biome[] biomes = new Biome[V118Biome.values().length];
        Arrays.fill(biomes, Biomes.PLAINS);
        biomes[V118Biome.DESERT.ordinal()] = Biomes.DESERT;
        biomes[V118Biome.FROZEN_PEAKS.ordinal()] = Biomes.ICE_MOUNTAINS;
        return new V118BiomeMapper(biomes);
    }
}

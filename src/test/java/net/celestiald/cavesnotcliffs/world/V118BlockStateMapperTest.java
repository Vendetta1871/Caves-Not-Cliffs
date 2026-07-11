package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118Material;
import net.minecraft.block.BlockStone;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class V118BlockStateMapperTest {
    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void mapsEveryGenerationMaterialToOneDeterministicState() {
        IBlockState deepslate = Blocks.COAL_BLOCK.getDefaultState();
        IBlockState tuff = Blocks.BRICK_BLOCK.getDefaultState();
        IBlockState copperOre = Blocks.GOLD_ORE.getDefaultState();
        IBlockState rawCopperBlock = Blocks.GOLD_BLOCK.getDefaultState();
        IBlockState deepslateIronOre = Blocks.IRON_ORE.getDefaultState();
        IBlockState rawIronBlock = Blocks.IRON_BLOCK.getDefaultState();
        IBlockState calcite = Blocks.QUARTZ_BLOCK.getDefaultState();
        V118BlockStateMapper mapper = new V118BlockStateMapper(deepslate, tuff,
            copperOre, rawCopperBlock, deepslateIronOre, rawIronBlock, calcite);

        assertSame(Blocks.AIR.getDefaultState(), mapper.stateFor(V118Material.AIR));
        assertSame(Blocks.STONE.getDefaultState(), mapper.stateFor(V118Material.STONE));
        assertSame(Blocks.WATER.getDefaultState(), mapper.stateFor(V118Material.WATER));
        assertSame(Blocks.LAVA.getDefaultState(), mapper.stateFor(V118Material.LAVA));
        assertSame(Blocks.BEDROCK.getDefaultState(), mapper.stateFor(V118Material.BEDROCK));
        assertSame(deepslate, mapper.stateFor(V118Material.DEEPSLATE));
        assertSame(tuff, mapper.stateFor(V118Material.TUFF));
        assertEquals(BlockStone.EnumType.GRANITE,
            mapper.stateFor(V118Material.GRANITE).getValue(BlockStone.VARIANT));
        assertSame(copperOre, mapper.stateFor(V118Material.COPPER_ORE));
        assertSame(rawCopperBlock, mapper.stateFor(V118Material.RAW_COPPER_BLOCK));
        assertSame(deepslateIronOre,
            mapper.stateFor(V118Material.DEEPSLATE_IRON_ORE));
        assertSame(rawIronBlock, mapper.stateFor(V118Material.RAW_IRON_BLOCK));
        assertSame(calcite, mapper.stateFor(V118Material.CALCITE));

        for (V118Material material : V118Material.values()) {
            assertSame(mapper.stateFor(material), mapper.stateFor(material.storageId()));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownStorageIds() {
        V118BlockStateMapper mapper = new V118BlockStateMapper(
            Blocks.STONE.getDefaultState(), Blocks.STONE.getDefaultState(),
            Blocks.GOLD_ORE.getDefaultState(), Blocks.GOLD_BLOCK.getDefaultState(),
            Blocks.IRON_ORE.getDefaultState(), Blocks.IRON_BLOCK.getDefaultState(),
            Blocks.QUARTZ_BLOCK.getDefaultState());
        mapper.stateFor(V118Material.values().length);
    }
}

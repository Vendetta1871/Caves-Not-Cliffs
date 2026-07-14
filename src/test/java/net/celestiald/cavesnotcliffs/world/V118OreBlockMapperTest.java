package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118OreMaterial;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSilverfish;
import net.minecraft.block.BlockStone;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class V118OreBlockMapperTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void translatesEveryOutputAndRejectsNonTargets() {
        IBlockState[] states = new IBlockState[V118OreMaterial.values().length];
        for (V118OreMaterial material : V118OreMaterial.values()) {
            if (material != V118OreMaterial.OTHER) {
                states[material.ordinal()] = new TestBlock().getDefaultState();
            }
        }
        states[V118OreMaterial.AIR.ordinal()] = Blocks.AIR.getDefaultState();
        states[V118OreMaterial.STONE.ordinal()] = stone(BlockStone.EnumType.STONE);
        states[V118OreMaterial.GRANITE.ordinal()] = stone(BlockStone.EnumType.GRANITE);
        states[V118OreMaterial.DIORITE.ordinal()] = stone(BlockStone.EnumType.DIORITE);
        states[V118OreMaterial.ANDESITE.ordinal()] = stone(BlockStone.EnumType.ANDESITE);
        states[V118OreMaterial.INFESTED_STONE.ordinal()] =
            Blocks.MONSTER_EGG.getDefaultState().withProperty(BlockSilverfish.VARIANT,
                BlockSilverfish.EnumType.STONE);

        V118OreBlockMapper mapper = new V118OreBlockMapper(states);
        for (V118OreMaterial material : V118OreMaterial.values()) {
            if (material == V118OreMaterial.OTHER) {
                continue;
            }
            assertSame(material.name(), states[material.ordinal()], mapper.stateFor(material));
            assertEquals(material.name(), material,
                mapper.materialFor(states[material.ordinal()]));
        }

        assertEquals(V118OreMaterial.OTHER,
            mapper.materialFor(stone(BlockStone.EnumType.GRANITE_SMOOTH)));
        assertEquals(V118OreMaterial.OTHER,
            mapper.materialFor(stone(BlockStone.EnumType.DIORITE_SMOOTH)));
        assertEquals(V118OreMaterial.OTHER,
            mapper.materialFor(stone(BlockStone.EnumType.ANDESITE_SMOOTH)));
        assertEquals(V118OreMaterial.OTHER,
            mapper.materialFor(Blocks.CHEST.getDefaultState()));
        assertEquals(V118OreMaterial.OTHER,
            mapper.materialFor(Blocks.MONSTER_EGG.getDefaultState().withProperty(
                BlockSilverfish.VARIANT, BlockSilverfish.EnumType.COBBLESTONE)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void otherHasNoOutputState() {
        IBlockState[] states = new IBlockState[V118OreMaterial.values().length];
        for (V118OreMaterial material : V118OreMaterial.values()) {
            if (material != V118OreMaterial.OTHER) {
                states[material.ordinal()] = new TestBlock().getDefaultState();
            }
        }
        new V118OreBlockMapper(states).stateFor(V118OreMaterial.OTHER);
    }

    private static IBlockState stone(BlockStone.EnumType type) {
        return Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, type);
    }

    private static final class TestBlock extends Block {
        private TestBlock() {
            super(Material.ROCK);
        }
    }
}

package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.block.BlockAmethystGrowth;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118GeodeFeature;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.EnumFacing;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class V118GeodeBlockMapperTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void mapsPlainAndDirectionalWaterloggedStatesWithoutPublicCompanionItems() {
        int count = V118GeodeFeature.Material.values().length;
        IBlockState[] plain = new IBlockState[count];
        BlockAmethystGrowth[] dry = new BlockAmethystGrowth[count];
        BlockAmethystGrowth[] wet = new BlockAmethystGrowth[count];
        for (V118GeodeFeature.Material material : V118GeodeFeature.Material.values()) {
            if (material.hasFacing()) {
                dry[material.ordinal()] = new BlockAmethystGrowth(
                    "test_dry_" + material.name().toLowerCase(), 3, 4, 1, false);
                wet[material.ordinal()] = new BlockAmethystGrowth(
                    "test_wet_" + material.name().toLowerCase(), 3, 4, 1, false);
            } else {
                plain[material.ordinal()] = new TestBlock().getDefaultState();
            }
        }
        V118GeodeBlockMapper mapper = new V118GeodeBlockMapper(plain, dry, wet);
        assertSame(plain[V118GeodeFeature.Material.CALCITE.ordinal()],
            mapper.stateFor(V118GeodeFeature.State.CALCITE));

        V118GeodeFeature.State dryState = new V118GeodeFeature.State(
            V118GeodeFeature.Material.SMALL_AMETHYST_BUD,
            V118GeodeFeature.Direction.NORTH, false);
        IBlockState mappedDry = mapper.stateFor(dryState);
        assertSame(dry[V118GeodeFeature.Material.SMALL_AMETHYST_BUD.ordinal()],
            mappedDry.getBlock());
        assertSame(EnumFacing.NORTH, mappedDry.getValue(BlockAmethystGrowth.FACING));
        assertFalse(mappedDry.getValue(BlockAmethystGrowth.WATERLOGGED));

        V118GeodeFeature.State wetState = new V118GeodeFeature.State(
            V118GeodeFeature.Material.AMETHYST_CLUSTER,
            V118GeodeFeature.Direction.DOWN, true);
        IBlockState mappedWet = mapper.stateFor(wetState);
        assertSame(wet[V118GeodeFeature.Material.AMETHYST_CLUSTER.ordinal()],
            mappedWet.getBlock());
        assertSame(EnumFacing.DOWN, mappedWet.getValue(BlockAmethystGrowth.FACING));
        assertTrue(mappedWet.getValue(BlockAmethystGrowth.WATERLOGGED));
    }

    private static final class TestBlock extends Block {
        private TestBlock() {
            super(Material.ROCK);
        }
    }
}

package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.block.BlockDripstone;
import net.celestiald.cavesnotcliffs.block.BlockPointedDripstone;
import net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118DripstoneFeature;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118OreMaterial;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSilverfish;
import net.minecraft.block.BlockStone;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.EnumFacing;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class V118DripstoneWorldBridgeTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void classifiesEveryRuntimeDripstoneInputFamily() {
        Fixture fixture = new Fixture();
        assertState(V118DripstoneFeature.State.AIR, Blocks.AIR.getDefaultState(), fixture);
        assertState(V118DripstoneFeature.State.WATER, Blocks.WATER.getDefaultState(), fixture);
        assertState(V118DripstoneFeature.State.WATER,
            Blocks.FLOWING_WATER.getDefaultState(), fixture);
        assertState(V118DripstoneFeature.State.LAVA, Blocks.LAVA.getDefaultState(), fixture);
        assertState(V118DripstoneFeature.State.LAVA,
            Blocks.FLOWING_LAVA.getDefaultState(), fixture);
        assertState(V118DripstoneFeature.State.DRIPSTONE_BLOCK,
            fixture.dripstone.getDefaultState(), fixture);
        assertState(V118DripstoneFeature.State.POINTED_DRIPSTONE,
            fixture.dry.getDefaultState(), fixture);
        assertState(V118DripstoneFeature.State.POINTED_DRIPSTONE,
            fixture.wet.getDefaultState(), fixture);

        for (V118OreMaterial material : EnumSet.of(V118OreMaterial.STONE,
                V118OreMaterial.DEEPSLATE, V118OreMaterial.TUFF,
                V118OreMaterial.GRANITE, V118OreMaterial.DIORITE,
                V118OreMaterial.ANDESITE)) {
            assertState(V118DripstoneFeature.State.BASE_STONE,
                fixture.states[material.ordinal()], fixture);
        }
        assertState(V118DripstoneFeature.State.OTHER,
            Blocks.CHEST.getDefaultState(), fixture);
        assertState(V118DripstoneFeature.State.OTHER,
            fixture.states[V118OreMaterial.DIRT.ordinal()], fixture);
    }

    @Test
    public void convertsAllTenPointedStatesWithoutOrdinalDrift() {
        assertEquals(EnumFacing.UP,
            V118DripstoneWorldBridge.facing(V118DripstoneFeature.Direction.UP));
        assertEquals(EnumFacing.DOWN,
            V118DripstoneWorldBridge.facing(V118DripstoneFeature.Direction.DOWN));
        for (V118DripstoneFeature.Thickness thickness
                : V118DripstoneFeature.Thickness.values()) {
            assertEquals(PointedDripstoneMechanics.Thickness.valueOf(thickness.name()),
                V118DripstoneWorldBridge.runtimeThickness(thickness));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsHorizontalPointedDirection() {
        V118DripstoneWorldBridge.facing(V118DripstoneFeature.Direction.NORTH);
    }

    @Test
    public void enforcesTheFeatureStatusOneChunkWriteCutoffAtNegativeBoundaries() {
        assertTrue(V118DripstoneWorldBridge.withinWriteRadius(0, 0, -16, -16));
        assertTrue(V118DripstoneWorldBridge.withinWriteRadius(0, 0, 31, 31));
        assertFalse(V118DripstoneWorldBridge.withinWriteRadius(0, 0, -17, 0));
        assertFalse(V118DripstoneWorldBridge.withinWriteRadius(0, 0, 32, 0));

        assertTrue(V118DripstoneWorldBridge.withinWriteRadius(-2, -3, -48, -64));
        assertTrue(V118DripstoneWorldBridge.withinWriteRadius(-2, -3, -1, -32));
        assertFalse(V118DripstoneWorldBridge.withinWriteRadius(-2, -3, 0, -48));
        assertFalse(V118DripstoneWorldBridge.withinWriteRadius(-2, -3, -32, -65));
    }

    @Test
    public void worldSurfaceReturnsFirstAirAboveTheHighestNonAirBlock() {
        Set<Integer> nonAir = new HashSet<Integer>();
        nonAir.add(-64);
        nonAir.add(17);
        nonAir.add(319);
        assertEquals(320, V118DripstoneWorldBridge.firstAvailableSurfaceY(-64, 319,
            nonAir::contains));
        nonAir.remove(319);
        assertEquals(18, V118DripstoneWorldBridge.firstAvailableSurfaceY(-64, 319,
            nonAir::contains));
        assertEquals(-64, V118DripstoneWorldBridge.firstAvailableSurfaceY(-64, 319,
            ignored -> false));
    }

    private static void assertState(V118DripstoneFeature.State expected, IBlockState state,
            Fixture fixture) {
        assertEquals(expected, V118DripstoneWorldBridge.classify(state, fixture.mapper,
            fixture.dripstone, fixture.dry, fixture.wet));
    }

    private static final class Fixture {
        private final IBlockState[] states =
            new IBlockState[V118OreMaterial.values().length];
        private final V118OreBlockMapper mapper;
        private final Block dripstone = new BlockDripstone.BlockCustom();
        private final BlockPointedDripstone dry = new BlockPointedDripstone(false);
        private final BlockPointedDripstone wet = new BlockPointedDripstone(true);

        private Fixture() {
            for (V118OreMaterial material : V118OreMaterial.values()) {
                if (material != V118OreMaterial.OTHER) {
                    states[material.ordinal()] = new TestBlock().getDefaultState();
                }
            }
            states[V118OreMaterial.AIR.ordinal()] = Blocks.AIR.getDefaultState();
            states[V118OreMaterial.WATER.ordinal()] = Blocks.WATER.getDefaultState();
            states[V118OreMaterial.STONE.ordinal()] = stone(BlockStone.EnumType.STONE);
            states[V118OreMaterial.GRANITE.ordinal()] = stone(BlockStone.EnumType.GRANITE);
            states[V118OreMaterial.DIORITE.ordinal()] = stone(BlockStone.EnumType.DIORITE);
            states[V118OreMaterial.ANDESITE.ordinal()] = stone(BlockStone.EnumType.ANDESITE);
            states[V118OreMaterial.INFESTED_STONE.ordinal()] =
                Blocks.MONSTER_EGG.getDefaultState().withProperty(BlockSilverfish.VARIANT,
                    BlockSilverfish.EnumType.STONE);
            mapper = new V118OreBlockMapper(states);
        }
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

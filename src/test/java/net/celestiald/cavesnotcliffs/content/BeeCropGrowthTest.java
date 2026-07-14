package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.block.LushCaveVinesBlock;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockStem;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.math.BlockPos;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BeeCropGrowthTest {
    private static final BlockPos POS = new BlockPos(1, 2, 3);

    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void caveVineHeadAndPlantGainBerriesWithoutEventOrCount() {
        LushCaveVinesBlock.Head head = new LushCaveVinesBlock.Head(0, 7);
        assertCaveVine(head.getDefaultState());
        assertCaveVine(new LushCaveVinesBlock.Body().getDefaultState());
    }

    @Test
    public void caveVineBonemealRemainsUncountedWhenBerriesAlreadyExist() {
        LushCaveVinesBlock.Body body = new LushCaveVinesBlock.Body();
        FakeAccess world = new FakeAccess(body.getDefaultState()
                .withProperty(LushCaveVinesBlock.BERRIES, true));
        BeeCropGrowth.Result result = BeeCropGrowth.grow(world, POS);
        assertEquals(BeeCropGrowth.Result.CAVE_VINE, result);
        assertFalse(result.emitsGrowthEvent());
        assertFalse(result.incrementsPollinationCount());
        assertEquals(Arrays.asList("set"), world.actions);
    }

    @Test
    public void ordinaryCropsEmitEventBeforeStateChangeAndIncrementCount() {
        FakeAccess world = new FakeAccess(Blocks.WHEAT.getDefaultState()
                .withProperty(BlockCrops.AGE, 3));
        BeeCropGrowth.Result result = BeeCropGrowth.grow(world, POS);
        assertEquals(BeeCropGrowth.Result.COUNTED_CROP, result);
        assertTrue(result.emitsGrowthEvent());
        assertTrue(result.incrementsPollinationCount());
        assertEquals(Arrays.asList("event", "set"), world.actions);
        assertEquals(Integer.valueOf(4), world.state.getValue(BlockCrops.AGE));
    }

    @Test
    public void stemsGrowOneAgeAndMaturePlantsDoNothing() {
        FakeAccess stem = new FakeAccess(Blocks.PUMPKIN_STEM.getDefaultState()
                .withProperty(BlockStem.AGE, 6));
        assertEquals(BeeCropGrowth.Result.COUNTED_CROP,
                BeeCropGrowth.grow(stem, POS));
        assertEquals(Integer.valueOf(7), stem.state.getValue(BlockStem.AGE));

        FakeAccess mature = new FakeAccess(Blocks.WHEAT.getDefaultState()
                .withProperty(BlockCrops.AGE, 7));
        assertEquals(BeeCropGrowth.Result.NONE, BeeCropGrowth.grow(mature, POS));
        assertTrue(mature.actions.isEmpty());
    }

    private static void assertCaveVine(IBlockState initial) {
        FakeAccess world = new FakeAccess(initial);
        BeeCropGrowth.Result result = BeeCropGrowth.grow(world, POS);
        assertEquals(BeeCropGrowth.Result.CAVE_VINE, result);
        assertFalse(result.emitsGrowthEvent());
        assertFalse(result.incrementsPollinationCount());
        assertTrue(world.state.getValue(LushCaveVinesBlock.BERRIES));
        assertEquals(Arrays.asList("set"), world.actions);
    }

    private static final class FakeAccess implements BeeCropGrowth.WorldAccess {
        IBlockState state;
        final List<String> actions = new ArrayList<>();

        FakeAccess(IBlockState state) {
            this.state = state;
        }

        @Override public IBlockState getState(BlockPos pos) { return state; }
        @Override public void setState(BlockPos pos, IBlockState state) {
            actions.add("set");
            this.state = state;
        }
        @Override public void growthEvent(BlockPos pos) { actions.add("event"); }
    }
}

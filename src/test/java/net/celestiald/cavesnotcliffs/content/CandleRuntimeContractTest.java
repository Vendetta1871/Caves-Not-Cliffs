package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.block.BlockCandle;
import net.celestiald.cavesnotcliffs.block.BlockCandleCake;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CandleRuntimeContractTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void everyColorUsesOneSixteenStatePublicBlockAndOneTwoStateCake() {
        for (CandleMechanics.Color color : CandleMechanics.colors()) {
            BlockCandle candle = new BlockCandle(color);
            BlockCandleCake cake = new BlockCandleCake(color);
            assertEquals(color.name(), 16, candle.getBlockState().getValidStates().size());
            assertEquals(color.name(), 2, cake.getBlockState().getValidStates().size());
            assertEquals(color.getCandlePath(), candle.getUnlocalizedName().substring(5));
            assertEquals(color.getCandleCakePath(), cake.getUnlocalizedName().substring(5));
        }
    }

    @Test
    public void runtimeMetadataAndDynamicLightCoverAllSixteenStates() {
        BlockCandle candle = new BlockCandle(CandleMechanics.Color.CYAN);
        Set<IBlockState> states = new HashSet<>();
        for (int meta = 0; meta < 16; meta++) {
            IBlockState state = candle.getStateFromMeta(meta);
            assertTrue(states.add(state));
            assertEquals(meta, candle.getMetaFromState(state));
            int expected = state.getValue(BlockCandle.LIT)
                    && !state.getValue(BlockCandle.WATERLOGGED)
                    ? state.getValue(BlockCandle.CANDLES) * 3 : 0;
            assertEquals(expected, candle.getLightValue(state));
            assertEquals(!state.getValue(BlockCandle.LIT)
                            && !state.getValue(BlockCandle.WATERLOGGED),
                    candle.canLight(state));
        }
    }

    @Test
    public void candleAndCakeShapesMatchOfficialVoxelBounds() {
        BlockCandle candle = new BlockCandle(CandleMechanics.Color.UNCOLORED);
        AxisAlignedBB one = candle.getBoundingBox(candle.getDefaultState(), null,
                BlockPos.ORIGIN);
        assertEquals(7.0D / 16.0D, one.minX, 0.0D);
        assertEquals(6.0D / 16.0D, one.maxY, 0.0D);
        AxisAlignedBB four = candle.getBoundingBox(candle.getDefaultState()
                .withProperty(BlockCandle.CANDLES, 4), null, BlockPos.ORIGIN);
        assertEquals(5.0D / 16.0D, four.minZ, 0.0D);
        assertEquals(11.0D / 16.0D, four.maxX, 0.0D);

        assertEquals(8.0D / 16.0D, BlockCandleCake.CAKE_SHAPE.maxY, 0.0D);
        assertEquals(14.0D / 16.0D, BlockCandleCake.CANDLE_SHAPE.maxY, 0.0D);
        assertEquals(1.0D / 16.0D, BlockCandleCake.OUTLINE_SHAPE.minX, 0.0D);
    }

    @Test
    public void cakeStateHasExactLightComparatorAndPickContract() {
        BlockCandleCake cake = new BlockCandleCake(CandleMechanics.Color.RED);
        IBlockState unlit = cake.getDefaultState();
        IBlockState lit = cake.withLit(unlit, true);
        assertEquals(0, cake.getLightValue(unlit));
        assertEquals(3, cake.getLightValue(lit));
        assertFalse(cake.isLit(unlit));
        assertTrue(cake.isLit(lit));
        assertFalse(cake.canLight(lit));
        assertTrue(cake.canLight(unlit));
        assertTrue(cake.hasComparatorInputOverride(unlit));
        assertEquals(14, cake.getComparatorInputOverride(unlit, null,
                BlockPos.ORIGIN));
    }

    @Test
    public void registryCatalogExposesOnlyCandlesAsPublicItems() {
        assertEquals(17, CncRegistryIds.CANDLES.size());
        assertEquals(17, CncRegistryIds.CANDLE_CAKES.size());
        Set<String> all = new HashSet<>();
        CncRegistryIds.CANDLES.forEach(id -> assertTrue(all.add(id.toString())));
        CncRegistryIds.CANDLE_CAKES.forEach(id -> assertTrue(all.add(id.toString())));
        assertEquals(34, all.size());
    }
}

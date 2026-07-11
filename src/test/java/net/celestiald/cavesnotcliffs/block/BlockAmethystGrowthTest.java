package net.celestiald.cavesnotcliffs.block;

import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BlockAmethystGrowthTest {
    @BeforeClass
    public static void bootstrapMinecraftRegistries() {
        Bootstrap.register();
    }

    @Test
    public void allDirectionsAndWaterloggingRoundTripThroughMetadata() {
        BlockAmethystGrowth block = new BlockAmethystGrowth("test_bud", 5, 3, 4, false);
        for (EnumFacing facing : EnumFacing.values()) {
            for (boolean waterlogged : new boolean[]{false, true}) {
                IBlockState state = block.getDefaultState()
                        .withProperty(BlockAmethystGrowth.FACING, facing)
                        .withProperty(BlockAmethystGrowth.WATERLOGGED, waterlogged);
                IBlockState decoded = block.getStateFromMeta(block.getMetaFromState(state));
                assertEquals(facing, decoded.getValue(BlockAmethystGrowth.FACING));
                assertEquals(waterlogged,
                        decoded.getValue(BlockAmethystGrowth.WATERLOGGED));
            }
        }
    }

    @Test
    public void officialLargeBudDimensionsRotateAroundTheSupportingFace() {
        BlockAmethystGrowth block = new BlockAmethystGrowth("large_amethyst_bud",
                5, 3, 4, false);
        assertBox(new AxisAlignedBB(3 / 16.0D, 0.0D, 3 / 16.0D,
                        13 / 16.0D, 5 / 16.0D, 13 / 16.0D),
                box(block, EnumFacing.UP));
        assertBox(new AxisAlignedBB(3 / 16.0D, 11 / 16.0D, 3 / 16.0D,
                        13 / 16.0D, 1.0D, 13 / 16.0D),
                box(block, EnumFacing.DOWN));
        assertBox(new AxisAlignedBB(3 / 16.0D, 3 / 16.0D, 11 / 16.0D,
                        13 / 16.0D, 13 / 16.0D, 1.0D),
                box(block, EnumFacing.NORTH));
        assertBox(new AxisAlignedBB(0.0D, 3 / 16.0D, 3 / 16.0D,
                        5 / 16.0D, 13 / 16.0D, 13 / 16.0D),
                box(block, EnumFacing.EAST));
    }

    @Test
    public void budsDoNotGrowThemselvesAndExposeTheirStageLight() {
        BlockAmethystGrowth small = new BlockAmethystGrowth("small_amethyst_bud",
                3, 4, 1, false);
        BlockAmethystGrowth medium = new BlockAmethystGrowth("medium_amethyst_bud",
                4, 3, 2, false);
        BlockAmethystGrowth large = new BlockAmethystGrowth("large_amethyst_bud",
                5, 3, 4, false);
        BlockAmethystGrowth cluster = new BlockAmethystGrowth("amethyst_cluster",
                7, 3, 5, true);

        assertFalse(small.getTickRandomly());
        assertFalse(medium.getTickRandomly());
        assertFalse(large.getTickRandomly());
        assertFalse(cluster.getTickRandomly());
        assertEquals(1, small.getLightValue(small.getDefaultState()));
        assertEquals(2, medium.getLightValue(medium.getDefaultState()));
        assertEquals(4, large.getLightValue(large.getDefaultState()));
        assertEquals(5, cluster.getLightValue(cluster.getDefaultState()));
        assertEquals(EnumPushReaction.DESTROY,
                cluster.getMobilityFlag(cluster.getDefaultState()));
    }

    @Test
    public void onlyBuddingAmethystRandomTicksAndTintedGlassBlocksAllLight() {
        BlockBuddingAmethyst budding = new BlockBuddingAmethyst();
        BlockTintedGlass tinted = new BlockTintedGlass();
        assertTrue(budding.getTickRandomly());
        assertEquals(EnumPushReaction.DESTROY,
                budding.getMobilityFlag(budding.getDefaultState()));
        assertEquals(255, tinted.getLightOpacity(tinted.getDefaultState()));
        assertFalse(tinted.isOpaqueCube(tinted.getDefaultState()));
    }

    @Test
    public void waterloggedCompanionsAreStableHiddenStageIdentities() {
        BlockAmethystGrowth wet = BlockAmethystGrowth.waterloggedCompanion(
                "large_amethyst_bud", 5, 3, 4, false);
        assertTrue(wet.isWaterloggedStorage());
        assertTrue(wet.isWaterlogged(wet.getDefaultState()));
        assertEquals("large_amethyst_bud", wet.getPublicStagePath());
        assertEquals("large_amethyst_bud_waterlogged",
                AmethystWaterlogging.companionPath(wet.getPublicStagePath()));
        assertEquals("large_amethyst_bud",
                AmethystWaterlogging.publicPath("large_amethyst_bud_waterlogged"));
        assertEquals("large_amethyst_bud_waterlogged",
                AmethystWaterlogging.storagePathForMetadata("large_amethyst_bud", 8));
        assertEquals("large_amethyst_bud",
                AmethystWaterlogging.storagePathForMetadata("large_amethyst_bud", 0));
    }

    @Test
    public void clusterLootAllowsEveryToolButOnlyPickaxesReceiveTheFourShardPath() {
        BlockAmethystGrowth cluster = new BlockAmethystGrowth(
                "amethyst_cluster", 7, 3, 5, true);
        assertTrue(cluster.canHarvestBlock(null, null, null));
        assertEquals(2, BlockAmethystGrowth.clusterDropCount(
                ItemStack.EMPTY, 0, new Random(1L)));
        assertEquals(4, BlockAmethystGrowth.clusterDropCount(
                new ItemStack(Items.WOODEN_PICKAXE), 0, new Random(1L)));
        assertEquals(8, BlockAmethystGrowth.clusterDropCount(
                new ItemStack(Items.WOODEN_PICKAXE), 1, new Random() {
                    @Override
                    public int nextInt(int bound) {
                        return bound - 1;
                    }
                }));
    }

    private static AxisAlignedBB box(BlockAmethystGrowth block, EnumFacing facing) {
        IBlockState state = block.getDefaultState()
                .withProperty(BlockAmethystGrowth.FACING, facing);
        return block.getBoundingBox(state, null, null);
    }

    private static void assertBox(AxisAlignedBB expected, AxisAlignedBB actual) {
        assertEquals(expected.minX, actual.minX, 0.0D);
        assertEquals(expected.minY, actual.minY, 0.0D);
        assertEquals(expected.minZ, actual.minZ, 0.0D);
        assertEquals(expected.maxX, actual.maxX, 0.0D);
        assertEquals(expected.maxY, actual.maxY, 0.0D);
        assertEquals(expected.maxZ, actual.maxZ, 0.0D);
    }
}

package net.celestiald.cavesnotcliffs.world;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BeeSaplingNestHandlerTest {
    private static final BlockPos ORIGIN = new BlockPos(0, 64, 0);

    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void failedOrCancelledGrowthNeverUsesAnAdjacentTree() {
        FakeAccess world = new FakeAccess();
        world.put(ORIGIN, Blocks.SAPLING.getDefaultState());
        addTree(world, new BlockPos(3, 64, 0));
        Set<BlockPos> before = BeeSaplingNestHandler.snapshotTreeBlocks(world, ORIGIN);

        BeeSaplingNestHandler.TreeBlocks tree =
                BeeSaplingNestHandler.collectFinishedTree(world, ORIGIN, before);

        assertTrue(tree.trunks.isEmpty());
        assertTrue(tree.foliage.isEmpty());
    }

    @Test
    public void successfulGrowthIncludesOnlyNewBlocksAnchoredAtTheSapling() {
        FakeAccess world = new FakeAccess();
        world.put(ORIGIN, Blocks.SAPLING.getDefaultState());
        BlockPos oldTree = new BlockPos(3, 64, 0);
        addTree(world, oldTree);
        Set<BlockPos> before = BeeSaplingNestHandler.snapshotTreeBlocks(world, ORIGIN);

        addTree(world, ORIGIN);
        BeeSaplingNestHandler.TreeBlocks tree =
                BeeSaplingNestHandler.collectFinishedTree(world, ORIGIN, before);

        assertEquals(3, tree.trunks.size());
        assertEquals(2, tree.foliage.size());
        assertTrue(tree.trunks.contains(ORIGIN));
        assertFalse(tree.trunks.contains(oldTree));
        assertFalse(tree.foliage.contains(oldTree.up(3).east()));
    }

    @Test
    public void unrelatedNewTreeInScanBoxIsExcludedByOriginConnectivity() {
        FakeAccess world = new FakeAccess();
        world.put(ORIGIN, Blocks.SAPLING.getDefaultState());
        Set<BlockPos> before = BeeSaplingNestHandler.snapshotTreeBlocks(world, ORIGIN);

        addTree(world, ORIGIN);
        BlockPos unrelated = new BlockPos(4, 64, 4);
        addTree(world, unrelated);
        BeeSaplingNestHandler.TreeBlocks tree =
                BeeSaplingNestHandler.collectFinishedTree(world, ORIGIN, before);

        assertEquals(3, tree.trunks.size());
        assertFalse(tree.trunks.contains(unrelated));
        assertFalse(tree.foliage.contains(unrelated.up(3).east()));
    }

    private static void addTree(FakeAccess world, BlockPos base) {
        world.put(base, Blocks.LOG.getDefaultState());
        world.put(base.up(), Blocks.LOG.getDefaultState());
        world.put(base.up(2), Blocks.LOG.getDefaultState());
        world.put(base.up(3).east(), Blocks.LEAVES.getDefaultState());
        world.put(base.up(3).west(), Blocks.LEAVES.getDefaultState());
    }

    private static final class FakeAccess implements IBlockAccess {
        private final Map<BlockPos, IBlockState> states = new HashMap<>();

        void put(BlockPos pos, IBlockState state) {
            states.put(pos.toImmutable(), state);
        }

        @Override public TileEntity getTileEntity(BlockPos pos) { return null; }
        @Override public int getCombinedLight(BlockPos pos, int light) { return 0; }
        @Override public IBlockState getBlockState(BlockPos pos) {
            IBlockState state = states.get(pos);
            return state == null ? Blocks.AIR.getDefaultState() : state;
        }
        @Override public boolean isAirBlock(BlockPos pos) {
            return getBlockState(pos).getBlock() == Blocks.AIR;
        }
        @Override public Biome getBiome(BlockPos pos) { return Biome.getBiome(1); }
        @Override public int getStrongPower(BlockPos pos, EnumFacing facing) { return 0; }
        @Override public WorldType getWorldType() { return WorldType.DEFAULT; }
        @Override public boolean isSideSolid(BlockPos pos, EnumFacing side,
                boolean defaultValue) { return defaultValue; }
    }
}

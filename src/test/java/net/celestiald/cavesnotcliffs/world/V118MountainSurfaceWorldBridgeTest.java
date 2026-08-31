package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.block.LushAzaleaBlocks;
import net.celestiald.cavesnotcliffs.block.LushDripleafBlocks;
import net.celestiald.cavesnotcliffs.block.LushMossBlocks;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118DefaultSpringPlacements.SpringFluid;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockStone;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class V118MountainSurfaceWorldBridgeTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void sugarCaneUsesTheJava118DirtAndSandTags() {
        assertTrue(V118MountainSurfaceWorldBridge.isSugarCaneGround(Blocks.DIRT));
        assertTrue(V118MountainSurfaceWorldBridge.isSugarCaneGround(Blocks.GRASS));
        assertTrue(V118MountainSurfaceWorldBridge.isSugarCaneGround(Blocks.MYCELIUM));
        assertTrue(V118MountainSurfaceWorldBridge.isSugarCaneGround(Blocks.SAND));
        assertTrue(V118MountainSurfaceWorldBridge.isSugarCaneGround(
            new LushAzaleaBlocks.RootedDirt()));
        assertTrue(V118MountainSurfaceWorldBridge.isSugarCaneGround(
            new LushMossBlocks.Moss()));
        assertFalse(V118MountainSurfaceWorldBridge.isSugarCaneGround(Blocks.STONE));
        assertFalse(V118MountainSurfaceWorldBridge.isSugarCaneGround(Blocks.FARMLAND));
    }

    @Test
    public void motionBlockingHeightSeesRetainedVanillaWater() {
        LushDripleafBlocks.Small dripleaf = new LushDripleafBlocks.Small();
        assertTrue(V118MountainSurfaceWorldBridge.isMotionBlockingState(
            Blocks.STONE.getDefaultState()));
        assertTrue(V118MountainSurfaceWorldBridge.isMotionBlockingState(
            Blocks.WATER.getDefaultState()));
        assertTrue(V118MountainSurfaceWorldBridge.isMotionBlockingState(
            Blocks.FLOWING_LAVA.getDefaultState()));
        assertTrue(V118MountainSurfaceWorldBridge.isMotionBlockingState(
            dripleaf.getDefaultState().withProperty(
                LushDripleafBlocks.WATERLOGGED, true)));
        assertFalse(V118MountainSurfaceWorldBridge.isMotionBlockingState(
            dripleaf.getDefaultState()));
        assertFalse(V118MountainSurfaceWorldBridge.isMotionBlockingState(
            Blocks.AIR.getDefaultState()));
    }

    @Test
    public void deadBushUsesOnlyItsJava118DirtSandAndTerracottaSupports() {
        for (BlockDirt.DirtType dirt : BlockDirt.DirtType.values()) {
            assertTrue(dirt.getName(), V118MountainSurfaceWorldBridge.isDeadBushSupport(
                Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, dirt)));
        }
        assertTrue(V118MountainSurfaceWorldBridge.isDeadBushSupport(
            Blocks.GRASS.getDefaultState()));
        assertTrue(V118MountainSurfaceWorldBridge.isDeadBushSupport(
            Blocks.MYCELIUM.getDefaultState()));
        assertTrue(V118MountainSurfaceWorldBridge.isDeadBushSupport(
            new LushAzaleaBlocks.RootedDirt().getDefaultState()));
        assertTrue(V118MountainSurfaceWorldBridge.isDeadBushSupport(
            new LushMossBlocks.Moss().getDefaultState()));
        assertTrue(V118MountainSurfaceWorldBridge.isDeadBushSupport(
            Blocks.SAND.getStateFromMeta(0)));
        assertTrue(V118MountainSurfaceWorldBridge.isDeadBushSupport(
            Blocks.SAND.getStateFromMeta(1)));
        assertTrue(V118MountainSurfaceWorldBridge.isDeadBushSupport(
            Blocks.HARDENED_CLAY.getDefaultState()));
        for (int color = 0; color < 16; ++color) {
            assertTrue(V118MountainSurfaceWorldBridge.isDeadBushSupport(
                Blocks.STAINED_HARDENED_CLAY.getStateFromMeta(color)));
        }

        assertFalse(V118MountainSurfaceWorldBridge.isDeadBushSupport(
            Blocks.FARMLAND.getDefaultState()));
        assertFalse(V118MountainSurfaceWorldBridge.isDeadBushSupport(
            Blocks.STONE.getDefaultState()));
        assertFalse(V118MountainSurfaceWorldBridge.isDeadBushSupport(
            Blocks.CLAY.getDefaultState()));
        assertFalse(V118MountainSurfaceWorldBridge.isDeadBushSupport(
            Blocks.GRAVEL.getDefaultState()));
        assertFalse(V118MountainSurfaceWorldBridge.isDeadBushSupport(
            Blocks.SANDSTONE.getDefaultState()));
    }

    @Test
    public void defaultSpringsUseExactStatesAcrossMergedJava112Blocks() {
        V118BlockStateMapper states = new V118BlockStateMapper(
            Blocks.LOG.getDefaultState(),
            Blocks.END_STONE.getDefaultState(),
            Blocks.GOLD_BLOCK.getDefaultState(),
            Blocks.IRON_BLOCK.getDefaultState(),
            Blocks.DIAMOND_BLOCK.getDefaultState(),
            Blocks.EMERALD_BLOCK.getDefaultState(),
            Blocks.QUARTZ_BLOCK.getDefaultState(),
            Blocks.GLASS.getDefaultState());
        V118MountainSurfaceWorldBridge.SpringValidBlocks valid =
            V118MountainSurfaceWorldBridge.springValidBlocks(states);

        assertTrue(valid.accepts(stone(0), SpringFluid.LAVA));
        assertTrue(valid.accepts(stone(1), SpringFluid.LAVA));
        assertTrue(valid.accepts(stone(3), SpringFluid.LAVA));
        assertTrue(valid.accepts(stone(5), SpringFluid.LAVA));
        assertFalse(valid.accepts(stone(2), SpringFluid.LAVA));
        assertFalse(valid.accepts(stone(4), SpringFluid.LAVA));
        assertFalse(valid.accepts(stone(6), SpringFluid.LAVA));
        assertTrue(valid.accepts(Blocks.DIRT.getDefaultState().withProperty(
            BlockDirt.VARIANT, BlockDirt.DirtType.DIRT), SpringFluid.LAVA));
        assertFalse(valid.accepts(Blocks.DIRT.getDefaultState().withProperty(
            BlockDirt.VARIANT, BlockDirt.DirtType.COARSE_DIRT), SpringFluid.LAVA));
        assertFalse(valid.accepts(Blocks.DIRT.getDefaultState().withProperty(
            BlockDirt.VARIANT, BlockDirt.DirtType.PODZOL), SpringFluid.LAVA));
        assertTrue(valid.accepts(Blocks.LOG.getStateFromMeta(0), SpringFluid.LAVA));
        assertTrue(valid.accepts(Blocks.LOG.getStateFromMeta(4), SpringFluid.LAVA));
        assertTrue(valid.accepts(Blocks.LOG.getStateFromMeta(8), SpringFluid.LAVA));
        assertTrue(valid.accepts(Blocks.LOG.getStateFromMeta(12), SpringFluid.LAVA));
        assertTrue(valid.accepts(Blocks.END_STONE.getDefaultState(), SpringFluid.LAVA));
        assertTrue(valid.accepts(Blocks.QUARTZ_BLOCK.getDefaultState(), SpringFluid.LAVA));
        assertFalse(valid.accepts(Blocks.SNOW.getDefaultState(), SpringFluid.LAVA));
        assertFalse(valid.accepts(Blocks.PACKED_ICE.getDefaultState(), SpringFluid.LAVA));
        assertFalse(valid.accepts(Blocks.GLASS.getDefaultState(), SpringFluid.LAVA));
        assertTrue(valid.accepts(Blocks.SNOW.getDefaultState(), SpringFluid.WATER));
        assertTrue(valid.accepts(Blocks.PACKED_ICE.getDefaultState(), SpringFluid.WATER));
        assertTrue(valid.accepts(Blocks.GLASS.getDefaultState(), SpringFluid.WATER));
        assertFalse(valid.accepts(Blocks.ICE.getDefaultState(), SpringFluid.WATER));
        assertFalse(valid.accepts(Blocks.GRASS.getDefaultState(), SpringFluid.WATER));
        assertFalse(valid.accepts(new LushAzaleaBlocks.RootedDirt().getDefaultState(),
            SpringFluid.WATER));
        assertFalse(valid.accepts(new LushMossBlocks.Moss().getDefaultState(),
            SpringFluid.WATER));
    }

    @Test
    public void scheduledSpringBlocksUseTheFlowing112RuntimePeers() {
        assertSame(Blocks.FLOWING_WATER,
            V118MountainSurfaceWorldBridge.springBlock(SpringFluid.WATER));
        assertSame(Blocks.FLOWING_LAVA,
            V118MountainSurfaceWorldBridge.springBlock(SpringFluid.LAVA));
    }

    @Test
    public void heightCacheMemoizesColumnsUntilInvalidatedOrCleared() {
        V118MountainSurfaceWorldBridge.HeightCache cache =
            new V118MountainSurfaceWorldBridge.HeightCache();
        int[] calls = {0};
        V118MountainSurfaceWorldBridge.HeightCache.Probe probe = (x, z) -> {
            ++calls[0];
            return x * 3 + z;
        };

        assertEquals(17, cache.get(4, 5,
            V118MountainSurfaceWorldBridge.HeightCache.WORLD_SURFACE, probe));
        assertEquals(17, cache.get(4, 5,
            V118MountainSurfaceWorldBridge.HeightCache.WORLD_SURFACE, probe));
        assertEquals("a repeated column query must not rescan", 1, calls[0]);

        assertEquals(17, cache.get(4, 5,
            V118MountainSurfaceWorldBridge.HeightCache.MOTION_BLOCKING, probe));
        assertEquals("each heightmap kind is memoized independently", 2, calls[0]);

        cache.invalidate(4, 5);
        assertEquals(17, cache.get(4, 5,
            V118MountainSurfaceWorldBridge.HeightCache.WORLD_SURFACE, probe));
        assertEquals("a write into the column must drop its entries", 3, calls[0]);

        cache.invalidate(9, 5);
        assertEquals(17, cache.get(4, 5,
            V118MountainSurfaceWorldBridge.HeightCache.MOTION_BLOCKING, probe));
        assertEquals("a write drops every kind of the column; other columns stay cached",
            4, calls[0]);

        assertEquals(65, cache.get(20, 5,
            V118MountainSurfaceWorldBridge.HeightCache.WORLD_SURFACE, probe));
        assertEquals(17, cache.get(4, 5,
            V118MountainSurfaceWorldBridge.HeightCache.WORLD_SURFACE, probe));
        assertEquals("columns colliding in one slot evict each other, never mix up",
            6, calls[0]);

        cache.clear();
        assertEquals(17, cache.get(4, 5,
            V118MountainSurfaceWorldBridge.HeightCache.WORLD_SURFACE, probe));
        assertEquals("a new populate step must rescan", 7, calls[0]);
    }

    private static IBlockState stone(int metadata) {
        return Blocks.STONE.getDefaultState().withProperty(
            BlockStone.VARIANT, BlockStone.EnumType.byMetadata(metadata));
    }
}

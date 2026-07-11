package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.block.LushAzaleaBlocks;
import net.celestiald.cavesnotcliffs.block.LushCaveVinesBlock;
import net.celestiald.cavesnotcliffs.block.LushDripleafBlocks;
import net.celestiald.cavesnotcliffs.block.LushSporeBlossomBlock;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Numeric and public runtime contracts pinned directly to Java 1.18.2. */
public class LushRuntimePolishTest {
    private static final double EPSILON = 0.0D;

    @BeforeClass
    public static void bootstrapMinecraftRegistries() {
        net.minecraft.init.Bootstrap.register();
    }

    @Test
    public void fireOddsMatchTheOfficialBootstrapTable() {
        assertFire(new LushCaveVinesBlock.Head(0, 7), 15, 60);
        assertFire(new LushCaveVinesBlock.Body(), 15, 60);
        assertFire(new LushAzaleaBlocks.AzaleaBush(false), 30, 60);
        assertFire(new LushAzaleaBlocks.AzaleaBush(true), 30, 60);
        assertFire(new LushAzaleaBlocks.AzaleaLeaves(false), 30, 60);
        assertFire(new LushAzaleaBlocks.HangingRoots(false), 30, 60);
        assertFire(new LushAzaleaBlocks.HangingRoots(true), 30, 60);
        assertFire(new LushDripleafBlocks.Small(), 60, 100);
        assertFire(new LushDripleafBlocks.Head(false), 60, 100);
        assertFire(new LushDripleafBlocks.Head(true), 60, 100);
        assertFire(new LushDripleafBlocks.Stem(), 60, 100);
        assertFire(new LushSporeBlossomBlock(), 60, 100);
    }

    @Test
    public void bigDripleafSelectionCombinesDirectionalStemAndTiltLeaf() {
        LushDripleafBlocks.Head head = new LushDripleafBlocks.Head(false);
        for (EnumFacing facing : EnumFacing.Plane.HORIZONTAL) {
            IBlockState stable = state(head, facing, LushDripleafBlocks.Tilt.NONE);
            AxisAlignedBB stableSelection = LushDripleafBlocks.Head.selectionShape(stable);
            assertBox(stableSelection, 0.0D, 0.0D, 0.0D,
                    1.0D, 15.0D / 16.0D, 1.0D);
            assertBox(LushDripleafBlocks.Head.collisionShape(stable),
                    0.0D, 11.0D / 16.0D, 0.0D,
                    1.0D, 15.0D / 16.0D, 1.0D);

            IBlockState partial = state(head, facing, LushDripleafBlocks.Tilt.PARTIAL);
            assertBox(LushDripleafBlocks.Head.selectionShape(partial),
                    0.0D, 0.0D, 0.0D, 1.0D, 13.0D / 16.0D, 1.0D);
            assertBox(LushDripleafBlocks.Head.collisionShape(partial),
                    0.0D, 11.0D / 16.0D, 0.0D,
                    1.0D, 13.0D / 16.0D, 1.0D);

            IBlockState full = state(head, facing, LushDripleafBlocks.Tilt.FULL);
            assertNull(LushDripleafBlocks.Head.collisionShape(full));
            assertDirectionalStem(LushDripleafBlocks.Head.selectionShape(full), facing);
        }
    }

    @Test
    public void azaleaBonemealEligibilityChecksFluidInsteadOfAir() {
        assertTrue(LushAzaleaBlocks.hasEmptyFluid(Blocks.AIR.getDefaultState()));
        assertTrue(LushAzaleaBlocks.hasEmptyFluid(Blocks.STONE.getDefaultState()));
        assertFalse(LushAzaleaBlocks.hasEmptyFluid(Blocks.WATER.getDefaultState()));
        assertFalse(LushAzaleaBlocks.hasEmptyFluid(Blocks.FLOWING_WATER.getDefaultState()));
        assertFalse(LushAzaleaBlocks.hasEmptyFluid(Blocks.LAVA.getDefaultState()));
        assertFalse(LushAzaleaBlocks.hasEmptyFluid(Blocks.FLOWING_LAVA.getDefaultState()));
    }

    @Test
    public void everyHiddenRepresentationOverridesPickBlock() throws Exception {
        Class<?>[] hiddenOrStateful = {
                LushCaveVinesBlock.Head.class,
                LushCaveVinesBlock.Body.class,
                LushDripleafBlocks.Small.class,
                LushDripleafBlocks.Head.class,
                LushDripleafBlocks.Stem.class,
                LushAzaleaBlocks.HangingRoots.class,
                LushAzaleaBlocks.PottedAzalea.class
        };
        for (Class<?> type : hiddenOrStateful) {
            assertEquals(type, type.getDeclaredMethod("getPickBlock", IBlockState.class,
                    RayTraceResult.class, World.class, BlockPos.class,
                    EntityPlayer.class).getDeclaringClass());
        }
    }

    private static IBlockState state(LushDripleafBlocks.Head head, EnumFacing facing,
            LushDripleafBlocks.Tilt tilt) {
        return head.getDefaultState().withProperty(LushDripleafBlocks.FACING, facing)
                .withProperty(LushDripleafBlocks.TILT, tilt);
    }

    private static void assertFire(Block block, int spread, int flammability) {
        assertEquals(spread, block.getFireSpreadSpeed(null, BlockPos.ORIGIN,
                EnumFacing.UP));
        assertEquals(flammability, block.getFlammability(null, BlockPos.ORIGIN,
                EnumFacing.UP));
    }

    private static void assertDirectionalStem(AxisAlignedBB box, EnumFacing facing) {
        switch (facing) {
            case SOUTH:
                assertBox(box, 5.0D / 16.0D, 0.0D, 1.0D / 16.0D,
                        11.0D / 16.0D, 13.0D / 16.0D, 7.0D / 16.0D);
                break;
            case EAST:
                assertBox(box, 1.0D / 16.0D, 0.0D, 5.0D / 16.0D,
                        7.0D / 16.0D, 13.0D / 16.0D, 11.0D / 16.0D);
                break;
            case WEST:
                assertBox(box, 9.0D / 16.0D, 0.0D, 5.0D / 16.0D,
                        15.0D / 16.0D, 13.0D / 16.0D, 11.0D / 16.0D);
                break;
            case NORTH:
            default:
                assertBox(box, 5.0D / 16.0D, 0.0D, 9.0D / 16.0D,
                        11.0D / 16.0D, 13.0D / 16.0D, 15.0D / 16.0D);
                break;
        }
    }

    private static void assertBox(AxisAlignedBB box, double minX, double minY,
            double minZ, double maxX, double maxY, double maxZ) {
        assertEquals(minX, box.minX, EPSILON);
        assertEquals(minY, box.minY, EPSILON);
        assertEquals(minZ, box.minZ, EPSILON);
        assertEquals(maxX, box.maxX, EPSILON);
        assertEquals(maxY, box.maxY, EPSILON);
        assertEquals(maxZ, box.maxZ, EPSILON);
    }
}

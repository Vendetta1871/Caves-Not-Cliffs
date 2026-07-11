package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Bootstrap;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CampfireContentTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void registryIdsAreCanonical() {
        assertEquals("cavesnotcliffs:campfire", CncRegistryIds.CAMPFIRE.toString());
        assertEquals("cavesnotcliffs:soul_campfire",
            CncRegistryIds.SOUL_CAMPFIRE.toString());
        assertEquals("cavesnotcliffs:soul_soil", CncRegistryIds.SOUL_SOIL.toString());
    }

    @Test
    public void runtimeStateUsesEveryLegacyMetadataValueExactlyOnce() {
        CampfireContent.BlockCustom block = new CampfireContent.BlockCustom(false);
        int seen = 0;
        for (int metadata = 0; metadata < 16; ++metadata) {
            IBlockState state = block.getStateFromMeta(metadata);
            assertEquals(metadata, block.getMetaFromState(state));
            assertTrue(state.getValue(CampfireContent.BlockCustom.FACING)
                .getAxis().isHorizontal());
            seen |= 1 << block.getMetaFromState(state);
        }
        assertEquals(0xffff, seen);
    }

    @Test
    public void shapeLightDamageAndPathingMatchBothCampfireTypes() {
        CampfireContent.BlockCustom normal = new CampfireContent.BlockCustom(false);
        CampfireContent.BlockCustom soul = new CampfireContent.BlockCustom(true);
        IBlockState normalLit = normal.getDefaultState();
        IBlockState soulLit = soul.getDefaultState();
        AxisAlignedBB box = normal.getBoundingBox(normalLit, null, BlockPos.ORIGIN);
        assertEquals(7.0D / 16.0D, box.maxY, 0.0D);
        assertEquals(15, normal.getLightValue(normalLit, null, BlockPos.ORIGIN));
        assertEquals(10, soul.getLightValue(soulLit, null, BlockPos.ORIGIN));
        assertEquals(PathNodeType.DAMAGE_FIRE,
            normal.getAiPathNodeType(normalLit, null, BlockPos.ORIGIN));
        assertEquals(PathNodeType.BLOCKED, normal.getAiPathNodeType(
            normalLit.withProperty(CampfireContent.BlockCustom.LIT, false),
            null, BlockPos.ORIGIN));
        assertFalse(normal.isOpaqueCube(normalLit));
        assertFalse(normal.isFullCube(normalLit));
        assertEquals(1, CampfireMechanics.fireDamage(false));
        assertEquals(2, CampfireMechanics.fireDamage(true));
    }

    @Test
    public void smokeObstructionUsesBlockLocalFencePostIntersection() {
        AxisAlignedBB virtualPost = new AxisAlignedBB(6.0D / 16.0D, 0.0D,
            6.0D / 16.0D, 10.0D / 16.0D, 1.0D, 10.0D / 16.0D);
        assertTrue(virtualPost.intersects(Block.FULL_BLOCK_AABB));
        assertFalse(virtualPost.intersects(new AxisAlignedBB(
            0.0D, 0.0D, 0.0D, 0.25D, 1.0D, 0.25D)));
    }

    @Test
    public void rotationsPreserveTheOtherStateProperties() {
        CampfireContent.BlockCustom block = new CampfireContent.BlockCustom(false);
        IBlockState state = block.getDefaultState()
            .withProperty(CampfireContent.BlockCustom.FACING, EnumFacing.NORTH)
            .withProperty(CampfireContent.BlockCustom.LIT, false)
            .withProperty(CampfireContent.BlockCustom.WATERLOGGED, true);
        IBlockState rotated = block.withRotation(state,
            net.minecraft.util.Rotation.CLOCKWISE_90);
        assertEquals(EnumFacing.EAST,
            rotated.getValue(CampfireContent.BlockCustom.FACING));
        assertFalse(rotated.getValue(CampfireContent.BlockCustom.LIT));
        assertTrue(rotated.getValue(CampfireContent.BlockCustom.WATERLOGGED));
    }
}

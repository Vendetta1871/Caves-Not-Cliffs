package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class BlockStonecutterTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void facingMetadataRotationAndMirrorRoundTrip() {
        BlockStonecutter.BlockCustom block = new BlockStonecutter.BlockCustom();
        for (EnumFacing facing : new EnumFacing[]{EnumFacing.NORTH, EnumFacing.SOUTH,
                EnumFacing.WEST, EnumFacing.EAST}) {
            IBlockState state = block.getDefaultState()
                    .withProperty(BlockStonecutter.BlockCustom.FACING, facing);
            assertEquals(facing, block.getStateFromMeta(block.getMetaFromState(state))
                    .getValue(BlockStonecutter.BlockCustom.FACING));
        }
        IBlockState north = block.getDefaultState();
        assertEquals(EnumFacing.EAST, block.withRotation(north, Rotation.CLOCKWISE_90)
                .getValue(BlockStonecutter.BlockCustom.FACING));
        assertEquals(EnumFacing.SOUTH, block.withMirror(north, Mirror.LEFT_RIGHT)
                .getValue(BlockStonecutter.BlockCustom.FACING));
    }

    @Test
    public void geometryAndSupportShapeMatchTheNinePixelHighBlock() {
        BlockStonecutter.BlockCustom block = new BlockStonecutter.BlockCustom();
        IBlockState state = block.getDefaultState();
        assertEquals(0.0D, BlockStonecutter.BlockCustom.SHAPE.minY, 0.0D);
        assertEquals(9.0D / 16.0D, BlockStonecutter.BlockCustom.SHAPE.maxY, 0.0D);
        assertFalse(block.isOpaqueCube(state));
        assertFalse(block.isFullCube(state));
        assertEquals(BlockFaceShape.SOLID, block.getBlockFaceShape(null,
                state, null, EnumFacing.DOWN));
        assertEquals(BlockFaceShape.UNDEFINED, block.getBlockFaceShape(null,
                state, null, EnumFacing.UP));
        assertEquals(3.5F, block.getBlockHardness(state, null, null), 0.0F);
        assertEquals(3.5F, block.getExplosionResistance(null), 0.0F);
        assertEquals("pickaxe", block.getHarvestTool(state));
        assertEquals(0, block.getHarvestLevel(state));
        assertEquals(CreativeTabs.DECORATIONS, block.getCreativeTabToDisplayOn());
    }

    @Test
    public void registryAndSoundIdentitiesAreCanonical() {
        assertEquals("cavesnotcliffs:stonecutter", CncRegistryIds.STONECUTTER.toString());
        assertEquals("cavesnotcliffs:ui.stonecutter.select_recipe",
                BlockStonecutter.SELECT_RECIPE_SOUND.getSoundName().toString());
        assertEquals("cavesnotcliffs:ui.stonecutter.take_result",
                BlockStonecutter.TAKE_RESULT_SOUND.getSoundName().toString());
        assertEquals("stat.interactWithStonecutter",
                BlockStonecutter.INTERACT_STAT.statId);
    }
}

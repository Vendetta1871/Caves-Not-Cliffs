
package net.celestiald.cavesnotcliffs.block;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraft.block.Block;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.block.material.Material;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import java.util.Random;
import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.item.ItemGlowBerries;

@ElementsCavesNotCliffs.ModElement.Tag
public class BlockGlowBerryVines extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:glow_berry_vines")
    public static final Block block = null;

    public BlockGlowBerryVines(ElementsCavesNotCliffs instance) { super(instance, 37); }

    @Override
    public void initElements() {
        elements.blocks.add(() -> new BlockCustom().setRegistryName("glow_berry_vines"));
    }

    public static class BlockCustom extends Block {
        public BlockCustom() {
            super(Material.VINE);
            setUnlocalizedName("glow_berry_vines");
            setSoundType(SoundType.PLANT);
            setHardness(0.2f);
            setLightLevel(0.5f);
            setTickRandomly(true);
        }

        @Override public boolean isOpaqueCube(IBlockState state) { return false; }
        @Override public boolean isFullCube(IBlockState state) { return false; }
        @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) { return NULL_AABB; }
        @SideOnly(Side.CLIENT) @Override public BlockRenderLayer getBlockLayer() { return BlockRenderLayer.CUTOUT; }

        @Override
        public Item getItemDropped(IBlockState state, Random random, int fortune) {
            return ItemGlowBerries.item == null ? net.minecraft.init.Items.AIR : ItemGlowBerries.item;
        }

        @Override
        public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
            if (!worldIn.isRemote && rand.nextInt(10) == 0) {
                BlockPos below = pos.down();
                if (worldIn.isAirBlock(below)) {
                    worldIn.setBlockState(below, BlockGlowBerryVines.block.getDefaultState());
                }
            }
        }
    }
}


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
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;

public class BlockDripleafPlant extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:big_dripleaf")
    public static final Block block = null;

    public BlockDripleafPlant(ElementsCavesNotCliffs instance) { super(instance, 43); }

    @Override
    public void initElements() {
        elements.blocks.add(() -> new BlockCustom().setRegistryName("big_dripleaf"));
        elements.items.add(() -> new ItemBlock(block).setRegistryName(block.getRegistryName()));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
            new ModelResourceLocation("cavesnotcliffs:big_dripleaf", "inventory"));
    }

    public static class BlockCustom extends Block {
        private static final AxisAlignedBB NO_AABB = new AxisAlignedBB(0, 0, 0, 0, 0, 0);

        public BlockCustom() {
            super(Material.PLANTS);
            setUnlocalizedName("big_dripleaf");
            setCreativeTab(net.minecraft.creativetab.CreativeTabs.BUILDING_BLOCKS);
            setSoundType(SoundType.PLANT);
            setHardness(0.0f);
            setResistance(0.0f);
        }

        @Override public boolean isOpaqueCube(IBlockState state) { return false; }
        @Override public boolean isFullCube(IBlockState state) { return false; }
        @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) {
            return new AxisAlignedBB(0, 0, 0, 1, 0.8125, 1);
        }
        @SideOnly(Side.CLIENT) @Override public BlockRenderLayer getBlockLayer() { return BlockRenderLayer.CUTOUT; }

        @Override
        public void onEntityWalk(World worldIn, BlockPos pos, Entity entityIn) {
            if (!worldIn.isRemote && BlockDripleafplant1.block != null) {
                worldIn.setBlockState(pos, BlockDripleafplant1.block.getDefaultState(), 3);
                worldIn.scheduleUpdate(pos, BlockDripleafplant1.block, 10);
            }
        }
    }
}

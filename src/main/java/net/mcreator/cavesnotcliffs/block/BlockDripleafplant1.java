
package net.mcreator.cavesnotcliffs.block;

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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.mcreator.cavesnotcliffs.ElementsCavesNotCliffs;
import java.util.Random;

@ElementsCavesNotCliffs.ModElement.Tag
public class BlockDripleafplant1 extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:dripleafplant_1")
    public static final Block block = null;

    public BlockDripleafplant1(ElementsCavesNotCliffs instance) { super(instance, 199); }

    @Override
    public void initElements() {
        elements.blocks.add(() -> new BlockCustom().setRegistryName("dripleafplant_1"));
        elements.items.add(() -> new ItemBlock(block).setRegistryName(block.getRegistryName()));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
            new ModelResourceLocation("cavesnotcliffs:dripleafplant_1", "inventory"));
    }

    private static class BlockCustom extends Block {
        public BlockCustom() {
            super(Material.AIR);
            setUnlocalizedName("dripleafplant_1");
            setSoundType(SoundType.PLANT);
            setHardness(0.0f);
            setResistance(0.0f);
            setTickRandomly(true);
        }

        @Override public boolean isOpaqueCube(IBlockState state) { return false; }
        @Override public boolean isFullCube(IBlockState state) { return false; }
        @Override public net.minecraft.util.math.AxisAlignedBB getCollisionBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) { return NULL_AABB; }
        @SideOnly(Side.CLIENT) @Override public BlockRenderLayer getBlockLayer() { return BlockRenderLayer.CUTOUT; }

        @Override
        public void onEntityWalk(World worldIn, BlockPos pos, Entity entityIn) {
            if (!worldIn.isRemote && BlockDripleafPlant2.block != null)
                worldIn.setBlockState(pos, BlockDripleafPlant2.block.getDefaultState(), 3);
        }

        @Override
        public void randomTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
            if (!worldIn.isRemote && BlockDripleafPlant.block != null)
                worldIn.setBlockState(pos, BlockDripleafPlant.block.getDefaultState(), 3);
        }
    }
}


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
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.mcreator.cavesnotcliffs.ElementsCavesNotCliffs;

@ElementsCavesNotCliffs.ModElement.Tag
public class BlockDripleafPlant extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:dripleaf_plant")
    public static final Block block = null;

    public BlockDripleafPlant(ElementsCavesNotCliffs instance) { super(instance, 43); }

    @Override
    public void initElements() {
        elements.blocks.add(() -> new BlockCustom().setRegistryName("dripleaf_plant"));
        elements.items.add(() -> new ItemBlock(block).setRegistryName(block.getRegistryName()));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
            new ModelResourceLocation("cavesnotcliffs:dripleaf_plant", "inventory"));
    }

    public static class BlockCustom extends Block {
        private static final AxisAlignedBB NO_AABB = new AxisAlignedBB(0, 0, 0, 0, 0, 0);

        public BlockCustom() {
            super(Material.AIR);
            setUnlocalizedName("dripleaf_plant");
            setSoundType(SoundType.PLANT);
            setHardness(0.0f);
            setResistance(0.0f);
        }

        @Override public boolean isOpaqueCube(IBlockState state) { return false; }
        @Override public boolean isFullCube(IBlockState state) { return false; }
        @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState s, IBlockAccess w, BlockPos p) { return NULL_AABB; }
        @SideOnly(Side.CLIENT) @Override public BlockRenderLayer getBlockLayer() { return BlockRenderLayer.CUTOUT; }
    }
}

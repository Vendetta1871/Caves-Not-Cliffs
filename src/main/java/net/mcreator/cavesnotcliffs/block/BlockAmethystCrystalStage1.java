
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
import net.minecraft.world.World;
import java.util.Random;
import net.mcreator.cavesnotcliffs.ElementsCavesNotCliffs;

@ElementsCavesNotCliffs.ModElement.Tag
public class BlockAmethystCrystalStage1 extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("caves_and_cliffs:amethyst_crystal_stage_1")
    public static final Block block = null;

    public BlockAmethystCrystalStage1(ElementsCavesNotCliffs instance) { super(instance, 32); }

    @Override
    public void initElements() {
        elements.blocks.add(() -> new BlockCustom().setRegistryName("caves_and_cliffs", "amethyst_crystal_stage_1"));
        elements.items.add(() -> new ItemBlock(block).setRegistryName(block.getRegistryName()));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
            new ModelResourceLocation("caves_and_cliffs:amethyst_crystal_stage_1", "inventory"));
    }

    public static class BlockCustom extends Block {
        private static final AxisAlignedBB AABB = new AxisAlignedBB(0.1, 0.0, 0.1, 0.9, 0.8, 0.9);

        public BlockCustom() {
            super(Material.ROCK);
            setUnlocalizedName("amethyst_crystal_stage_1");
            setSoundType(SoundType.GLASS);
            setHardness(1.5f);
            setResistance(1.0f);
            setTickRandomly(true);
        }

        @Override public boolean isOpaqueCube(IBlockState state) { return false; }
        @Override public boolean isFullCube(IBlockState state) { return false; }
        @Override public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) { return AABB; }
        @SideOnly(Side.CLIENT) @Override public BlockRenderLayer getBlockLayer() { return BlockRenderLayer.CUTOUT; }

        @Override
        public void randomTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
            if (!worldIn.isRemote && rand.nextInt(5) == 0) {
                IBlockState below = worldIn.getBlockState(pos.down());
                if (below.getBlock() == BlockAmethystGeode.block || below.getBlock() == BlockGeodeCasing.block) {
                    worldIn.setBlockState(pos, BlockAmethystCrystalStage2.block.getDefaultState());
                }
            }
        }
    }
}

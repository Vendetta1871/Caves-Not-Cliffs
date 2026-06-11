
package net.celestiald.cavesnotcliffs.block;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.Random;
import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;

@ElementsCavesNotCliffs.ModElement.Tag
public class BlockCopperBlockStage2 extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:copper_block_stage2")
    public static final Block block = null;

    public BlockCopperBlockStage2(ElementsCavesNotCliffs instance) { super(instance, 47); }

    @Override
    public void initElements() {
        elements.blocks.add(() -> new BlockCustom().setRegistryName("copper_block_stage2"));
        elements.items.add(() -> new ItemBlock(block).setRegistryName(block.getRegistryName()));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
            new ModelResourceLocation("cavesnotcliffs:copper_block_stage2", "inventory"));
    }

    private static class BlockCustom extends Block {
        public BlockCustom() {
            super(Material.IRON);
            setUnlocalizedName("copper_block_stage2");
            setCreativeTab(net.minecraft.creativetab.CreativeTabs.BUILDING_BLOCKS);
            setSoundType(SoundType.METAL);
            setHardness(3.0f);
            setResistance(6.0f);
            setTickRandomly(true);
        }

        @Override
        public void randomTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
            if (!worldIn.isRemote && rand.nextInt(100) == 0) {
                worldIn.setBlockState(pos, BlockCopperBlockStage3.block.getDefaultState());
            }
        }
    }
}

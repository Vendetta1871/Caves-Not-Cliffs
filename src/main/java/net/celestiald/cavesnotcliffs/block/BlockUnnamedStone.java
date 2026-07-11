
package net.celestiald.cavesnotcliffs.block;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.material.Material;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.content.CncMaterialContent;
import net.celestiald.cavesnotcliffs.content.CncBlockProperties;
import java.util.Random;

@ElementsCavesNotCliffs.ModElement.Tag
public class BlockUnnamedStone extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:deepslate")
    public static final Block block = null;

    public BlockUnnamedStone(ElementsCavesNotCliffs instance) { super(instance, 66); }

    @Override
    public void initElements() {
        elements.blocks.add(() -> new BlockCustom().setRegistryName("deepslate"));
        elements.items.add(() -> new ItemBlock(block).setRegistryName(block.getRegistryName()));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
            new ModelResourceLocation("cavesnotcliffs:deepslate", "inventory"));
    }

    private static class BlockCustom extends BlockRotatedPillar {
        public BlockCustom() {
            super(Material.ROCK, CncBlockProperties.DEEPSLATE);
            setUnlocalizedName("deepslate");
            setCreativeTab(net.minecraft.creativetab.CreativeTabs.BUILDING_BLOCKS);
            setSoundType(SoundType.STONE);
            setHardness(3.0f);
            setResistance(CncBlockProperties.legacyResistance(6.0F));
            setHarvestLevel("pickaxe", 0);
        }

        @Override
        public Item getItemDropped(IBlockState state, Random random, int fortune) {
            return Item.getItemFromBlock(CncMaterialContent.block("cobbled_deepslate"));
        }

        @Override
        protected boolean canSilkHarvest() {
            return true;
        }
    }
}

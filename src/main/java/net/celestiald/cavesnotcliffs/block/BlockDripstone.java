
package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.content.CncBlockProperties;
import net.celestiald.cavesnotcliffs.content.DripstoneSoundEvents;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;

@ElementsCavesNotCliffs.ModElement.Tag
public class BlockDripstone extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:dripstone_block")
    public static final Block block = null;

    public BlockDripstone(ElementsCavesNotCliffs instance) { super(instance, 20); }

    @Override
    public void initElements() {
        elements.blocks.add(() -> new BlockCustom().setRegistryName(CncRegistryIds.DRIPSTONE_BLOCK));
        elements.items.add(() -> new ItemBlock(block).setRegistryName(block.getRegistryName()));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
            new ModelResourceLocation("cavesnotcliffs:dripstone_block", "inventory"));
    }

    public static final class BlockCustom extends Block {
        public BlockCustom() {
            super(Material.ROCK, MapColor.ADOBE);
            setUnlocalizedName("dripstone_block");
            setCreativeTab(net.minecraft.creativetab.CreativeTabs.BUILDING_BLOCKS);
            setSoundType(DripstoneSoundEvents.DRIPSTONE_BLOCK);
            setHardness(1.5f);
            setResistance(CncBlockProperties.legacyResistance(1.0F));
            setHarvestLevel("pickaxe", 0);
        }
    }
}

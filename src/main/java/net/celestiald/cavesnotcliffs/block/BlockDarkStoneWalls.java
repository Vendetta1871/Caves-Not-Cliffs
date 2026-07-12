
package net.celestiald.cavesnotcliffs.block;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.NonNullList;
import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.content.CncBlockProperties;
import net.celestiald.cavesnotcliffs.content.DeepslateSoundEvents;

@ElementsCavesNotCliffs.ModElement.Tag
public class BlockDarkStoneWalls extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:tuff_wall")
    public static final Block block = null;

    public BlockDarkStoneWalls(ElementsCavesNotCliffs instance) { super(instance, 61); }

    @Override
    public void initElements() {
        elements.blocks.add(() -> new BlockCustom().setRegistryName("tuff_wall"));
        elements.items.add(() -> new ItemBlock(block).setRegistryName(block.getRegistryName()));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
            new ModelResourceLocation("cavesnotcliffs:tuff_wall", "inventory"));
        ModelLoader.setCustomStateMapper(block,
            new StateMap.Builder().ignore(BlockWall.VARIANT).build());
    }

    private static class BlockCustom extends BlockWall {
        public BlockCustom() {
            super(new Block(Material.ROCK, MapColor.GRAY_STAINED_HARDENED_CLAY));
            setUnlocalizedName("tuff_wall");
            setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
            setSoundType(DeepslateSoundEvents.TUFF);
            setHarvestLevel("pickaxe", 0);
            setHardness(1.5f);
            setResistance(CncBlockProperties.legacyResistance(6.0F));
        }

        @Override
        public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> list) {
            list.add(new ItemStack(this, 1, 0));
        }
    }
}

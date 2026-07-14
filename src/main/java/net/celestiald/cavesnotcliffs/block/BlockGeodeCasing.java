
package net.celestiald.cavesnotcliffs.block;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
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
import net.minecraft.util.ResourceLocation;
import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.content.AmethystSoundEvents;

@ElementsCavesNotCliffs.ModElement.Tag
public class BlockGeodeCasing extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:smooth_basalt")
    public static final Block block = null;

    public BlockGeodeCasing(ElementsCavesNotCliffs instance) { super(instance, 30); }

    @Override
    public void initElements() {
        elements.blocks.add(() -> new BlockCustom().setRegistryName("cavesnotcliffs", "smooth_basalt"));
        elements.items.add(() -> {
            Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("cavesnotcliffs", "smooth_basalt"));
            return new ItemBlock(b).setRegistryName(b.getRegistryName());
        });
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("cavesnotcliffs", "smooth_basalt"));
        if (item != null)
            ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation("cavesnotcliffs:smooth_basalt", "inventory"));
    }

    private static class BlockCustom extends Block {
        public BlockCustom() {
            super(Material.ROCK, MapColor.BLACK);
            setUnlocalizedName("smooth_basalt");
            setCreativeTab(net.minecraft.creativetab.CreativeTabs.BUILDING_BLOCKS);
            setSoundType(AmethystSoundEvents.BASALT);
            setHardness(1.25f);
            // Modern blast resistance 4.2 translated through 1.12's 3/5 scaling.
            setResistance(7.0f);
            setHarvestLevel("pickaxe", 0);
        }
    }
}

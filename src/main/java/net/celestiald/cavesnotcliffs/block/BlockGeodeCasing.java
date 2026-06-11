
package net.celestiald.cavesnotcliffs.block;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
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
import net.minecraft.util.ResourceLocation;
import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;

@ElementsCavesNotCliffs.ModElement.Tag
public class BlockGeodeCasing extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:geode_casing")
    public static final Block block = null;

    public BlockGeodeCasing(ElementsCavesNotCliffs instance) { super(instance, 30); }

    @Override
    public void initElements() {
        elements.blocks.add(() -> new BlockCustom().setRegistryName("cavesnotcliffs", "geode_casing"));
        elements.items.add(() -> {
            Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("cavesnotcliffs", "geode_casing"));
            return new ItemBlock(b).setRegistryName(b.getRegistryName());
        });
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("cavesnotcliffs", "geode_casing"));
        if (item != null)
            ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation("cavesnotcliffs:geode_casing", "inventory"));
    }

    private static class BlockCustom extends Block {
        public BlockCustom() {
            super(Material.ROCK);
            setUnlocalizedName("geode_casing");
            setCreativeTab(net.minecraft.creativetab.CreativeTabs.BUILDING_BLOCKS);
            setSoundType(SoundType.STONE);
            setHardness(1.5f);
            setResistance(6.0f);
        }
    }
}

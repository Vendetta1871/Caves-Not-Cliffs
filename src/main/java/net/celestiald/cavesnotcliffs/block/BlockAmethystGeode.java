
package net.celestiald.cavesnotcliffs.block;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;

@ElementsCavesNotCliffs.ModElement.Tag
public class BlockAmethystGeode extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:amethyst_block")
    public static final Block block = null;

    public BlockAmethystGeode(ElementsCavesNotCliffs instance) { super(instance, 29); }

    @Override
    public void initElements() {
        elements.blocks.add(() -> new BlockCustom().setRegistryName("cavesnotcliffs", "amethyst_block"));
        elements.items.add(() -> {
            Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("cavesnotcliffs", "amethyst_block"));
            return new ItemBlock(b).setRegistryName(b.getRegistryName());
        });
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("cavesnotcliffs", "amethyst_block"));
        if (item != null)
            ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation("cavesnotcliffs:amethyst_block", "inventory"));
    }

    private static class BlockCustom extends BlockAmethystBase {
        public BlockCustom() {
            super("amethyst_block");
            setCreativeTab(net.minecraft.creativetab.CreativeTabs.BUILDING_BLOCKS);
        }
    }
}


package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ElementsCavesNotCliffs.ModElement.Tag
public class BlockStalactite extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:pointed_dripstone")
    public static final Block block = null;

    public BlockStalactite(ElementsCavesNotCliffs instance) { super(instance, 27); }

    @Override
    public void initElements() {
        BlockPointedDripstone canonical = (BlockPointedDripstone)
                new BlockPointedDripstone(false)
                        .setRegistryName(CncRegistryIds.POINTED_DRIPSTONE);
        elements.blocks.add(() -> canonical);
        elements.items.add(() -> new ItemBlock(canonical)
                .setRegistryName(CncRegistryIds.POINTED_DRIPSTONE)
                .setCreativeTab(CreativeTabs.BUILDING_BLOCKS));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
            new ModelResourceLocation("cavesnotcliffs:pointed_dripstone", "inventory"));
    }
}

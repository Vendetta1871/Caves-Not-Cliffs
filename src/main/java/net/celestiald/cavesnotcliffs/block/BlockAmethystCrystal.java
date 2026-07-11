package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@ElementsCavesNotCliffs.ModElement.Tag
public class BlockAmethystCrystal extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:amethyst_cluster")
    public static final Block block = null;

    public BlockAmethystCrystal(ElementsCavesNotCliffs instance) {
        super(instance, 31);
    }

    @Override
    public void initElements() {
        elements.blocks.add(() -> new BlockCustom()
                .setRegistryName("cavesnotcliffs", "amethyst_cluster"));
        elements.items.add(() -> {
            Block registered = ForgeRegistries.BLOCKS.getValue(
                    new ResourceLocation("cavesnotcliffs", "amethyst_cluster"));
            return new ItemBlock(registered).setRegistryName(registered.getRegistryName());
        });
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        Block registered = ForgeRegistries.BLOCKS.getValue(
                new ResourceLocation("cavesnotcliffs", "amethyst_cluster"));
        if (registered != null) {
            BlockAmethystGrowth.registerItemModel(registered,
                    new ModelResourceLocation("cavesnotcliffs:amethyst_cluster", "inventory"));
        }
    }

    /** Cluster: height 7, offset 3, light level 5. */
    public static class BlockCustom extends BlockAmethystGrowth {
        public BlockCustom() {
            super("amethyst_cluster", 7, 3, 5, true);
            setCreativeTab(net.minecraft.creativetab.CreativeTabs.DECORATIONS);
        }
    }
}

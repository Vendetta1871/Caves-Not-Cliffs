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
public class BlockAmethystCrystalStage2 extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:medium_amethyst_bud")
    public static final Block block = null;

    public BlockAmethystCrystalStage2(ElementsCavesNotCliffs instance) {
        super(instance, 33);
    }

    @Override
    public void initElements() {
        elements.blocks.add(() -> new BlockCustom()
                .setRegistryName("cavesnotcliffs", "medium_amethyst_bud"));
        elements.items.add(() -> {
            Block registered = ForgeRegistries.BLOCKS.getValue(
                    new ResourceLocation("cavesnotcliffs", "medium_amethyst_bud"));
            return new ItemBlock(registered).setRegistryName(registered.getRegistryName());
        });
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        Block registered = ForgeRegistries.BLOCKS.getValue(
                new ResourceLocation("cavesnotcliffs", "medium_amethyst_bud"));
        if (registered != null) {
            BlockAmethystGrowth.registerItemModel(registered,
                    new ModelResourceLocation("cavesnotcliffs:medium_amethyst_bud", "inventory"));
        }
    }

    /** Medium bud: height 4, offset 3, light level 2. */
    public static class BlockCustom extends BlockAmethystGrowth {
        public BlockCustom() {
            super("medium_amethyst_bud", 4, 3, 2, false);
            setCreativeTab(net.minecraft.creativetab.CreativeTabs.DECORATIONS);
        }
    }
}

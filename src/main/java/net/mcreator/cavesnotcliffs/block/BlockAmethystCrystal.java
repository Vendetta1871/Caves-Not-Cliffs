
package net.mcreator.cavesnotcliffs.block;

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
import net.mcreator.cavesnotcliffs.ElementsCavesNotCliffs;

@ElementsCavesNotCliffs.ModElement.Tag
public class BlockAmethystCrystal extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("caves_and_cliffs:amethyst_crystal")
    public static final Block block = null;

    public BlockAmethystCrystal(ElementsCavesNotCliffs instance) { super(instance, 31); }

    @Override
    public void initElements() {
        elements.blocks.add(() -> new BlockCustom().setRegistryName("caves_and_cliffs", "amethyst_crystal"));
        elements.items.add(() -> new ItemBlock(block).setRegistryName(block.getRegistryName()));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
            new ModelResourceLocation("caves_and_cliffs:amethyst_crystal", "inventory"));
    }

    private static class BlockCustom extends Block {
        public BlockCustom() {
            super(Material.ROCK);
            setUnlocalizedName("amethyst_crystal");
            setSoundType(SoundType.STONE);
            setHardness(1.5f);
            setResistance(6.0f);
        }
    }
}

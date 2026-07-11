
package net.celestiald.cavesnotcliffs.block;

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
import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import java.util.Random;

@ElementsCavesNotCliffs.ModElement.Tag
public class BlockStalagmite extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:stalagmite")
    public static final Block block = null;

    public BlockStalagmite(ElementsCavesNotCliffs instance) { super(instance, 28); }

    @Override
    public void initElements() {
        elements.blocks.add(() -> new BlockCustom().setRegistryName("stalagmite"));
    }

    private static class BlockCustom extends Block {
        public BlockCustom() {
            super(Material.ROCK);
            setUnlocalizedName("stalagmite");
            setSoundType(SoundType.STONE);
            setHardness(1.5f);
            setResistance(6.0f);
        }

        @Override
        public Item getItemDropped(IBlockState state, Random random, int fortune) {
            return Item.getItemFromBlock(BlockStalactite.block);
        }
    }
}

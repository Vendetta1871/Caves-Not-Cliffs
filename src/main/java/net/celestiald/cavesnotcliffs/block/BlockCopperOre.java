
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
import net.celestiald.cavesnotcliffs.content.CncMaterialContent;
import net.celestiald.cavesnotcliffs.content.CncBlockProperties;
import net.celestiald.cavesnotcliffs.content.OreDropLogic;
import java.util.Random;

@ElementsCavesNotCliffs.ModElement.Tag
public class BlockCopperOre extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:copper_ore")
    public static final Block block = null;

    public BlockCopperOre(ElementsCavesNotCliffs instance) { super(instance, 49); }

    @Override
    public void initElements() {
        elements.blocks.add(() -> new BlockCustom().setRegistryName("copper_ore"));
        elements.items.add(() -> new ItemBlock(block).setRegistryName(block.getRegistryName()));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
            new ModelResourceLocation("cavesnotcliffs:copper_ore", "inventory"));
    }

    private static class BlockCustom extends Block {
        public BlockCustom() {
            super(Material.ROCK);
            setUnlocalizedName("copper_ore");
            setCreativeTab(net.minecraft.creativetab.CreativeTabs.BUILDING_BLOCKS);
            setSoundType(SoundType.STONE);
            setHardness(3.0f);
            setResistance(CncBlockProperties.legacyResistance(3.0F));
            setHarvestLevel("pickaxe", 1);
        }

        @Override
        public Item getItemDropped(IBlockState state, Random random, int fortune) {
            return CncMaterialContent.item("raw_copper");
        }

        @Override
        public int quantityDropped(Random random) {
            return 2 + random.nextInt(4);
        }

        @Override
        public int quantityDroppedWithBonus(int fortune, Random random) {
            return OreDropLogic.applyOreBonus(quantityDropped(random), fortune, random);
        }

        @Override
        public void dropBlockAsItemWithChance(net.minecraft.world.World world,
                net.minecraft.util.math.BlockPos pos, IBlockState state,
                float chance, int fortune) {
            if (!OreDropLogic.dropWithExplosionDecay(this, world, pos, state,
                    chance, fortune, harvesters.get())) {
                super.dropBlockAsItemWithChance(world, pos, state, chance, fortune);
            }
        }

        @Override
        protected boolean canSilkHarvest() {
            return true;
        }
    }
}

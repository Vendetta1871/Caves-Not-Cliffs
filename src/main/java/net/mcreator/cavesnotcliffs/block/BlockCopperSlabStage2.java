
package net.mcreator.cavesnotcliffs.block;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.material.Material;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSlab;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.IStringSerializable;
import net.mcreator.cavesnotcliffs.ElementsCavesNotCliffs;
import java.util.Random;

@ElementsCavesNotCliffs.ModElement.Tag
public class BlockCopperSlabStage2 extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:copper_slab_stage2")
    public static final Block block = null;

    @GameRegistry.ObjectHolder("cavesnotcliffs:copper_slab_stage2_double")
    public static final Block block_slab_double = null;

    public BlockCopperSlabStage2(ElementsCavesNotCliffs instance) { super(instance, 52); }

    @Override
    public void initElements() {
        elements.blocks.add(() -> new BlockCustom().setRegistryName("copper_slab_stage2"));
        elements.blocks.add(() -> new BlockCustom.Double().setRegistryName("copper_slab_stage2_double"));
        elements.items.add(() -> new ItemSlab(block, (BlockSlab) block, (BlockSlab) block_slab_double)
            .setRegistryName(block.getRegistryName()));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
            new ModelResourceLocation("cavesnotcliffs:copper_slab_stage2", "inventory"));
    }

    public static class BlockCustom extends BlockSlab {
        public static final PropertyEnum<Variant> VARIANT = PropertyEnum.create("variant", Variant.class);

        public BlockCustom() {
            super(Material.IRON);
            setUnlocalizedName("copper_slab_stage2");
            setCreativeTab(net.minecraft.creativetab.CreativeTabs.BUILDING_BLOCKS);
            setSoundType(SoundType.METAL);
            setHarvestLevel("pickaxe", 1);
            setHardness(3.0f);
            setResistance(6.0f);
            IBlockState state = this.blockState.getBaseState().withProperty(VARIANT, Variant.DEFAULT);
            if (!isDouble()) state = state.withProperty(HALF, EnumBlockHalf.BOTTOM);
            setDefaultState(state);
            this.useNeighborBrightness = !isDouble();
        }

        @Override public boolean isDouble() { return false; }
        @Override public IProperty<?> getVariantProperty() { return VARIANT; }
        @Override public Comparable<?> getTypeForItem(ItemStack stack) { return Variant.DEFAULT; }

        @Override
        public Item getItemDropped(IBlockState state, Random rand, int fortune) {
            return Item.getItemFromBlock(BlockCopperSlabStage2.block);
        }

        @Override
        public ItemStack getItem(net.minecraft.world.World worldIn, net.minecraft.util.math.BlockPos pos, IBlockState state) {
            return new ItemStack(BlockCopperSlabStage2.block);
        }

        @Override public String getUnlocalizedName(int meta) { return super.getUnlocalizedName(); }

        @Override
        protected BlockStateContainer createBlockState() {
            return isDouble() ? new BlockStateContainer(this, new IProperty[]{VARIANT})
                             : new BlockStateContainer(this, new IProperty[]{HALF, VARIANT});
        }

        @Override
        public IBlockState getStateFromMeta(int meta) {
            if (isDouble()) return getDefaultState();
            return getDefaultState().withProperty(HALF, BlockSlab.EnumBlockHalf.values()[meta % 2]);
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            if (isDouble()) return 0;
            return state.getValue(HALF).ordinal();
        }

        public enum Variant implements IStringSerializable {
            DEFAULT("default");
            private final String name;
            Variant(String name) { this.name = name; }
            @Override public String getName() { return name; }
            @Override public String toString() { return name; }
        }

        public static class Double extends BlockCustom {
            @Override public boolean isDouble() { return true; }
        }
    }
}

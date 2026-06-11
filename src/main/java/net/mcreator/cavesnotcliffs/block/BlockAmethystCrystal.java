
package net.mcreator.cavesnotcliffs.block;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraft.block.Block;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.block.material.Material;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import java.util.Random;
import net.mcreator.cavesnotcliffs.ElementsCavesNotCliffs;

@ElementsCavesNotCliffs.ModElement.Tag
public class BlockAmethystCrystal extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:amethyst_crystal")
    public static final Block block = null;

    public BlockAmethystCrystal(ElementsCavesNotCliffs instance) { super(instance, 31); }

    @Override
    public void initElements() {
        elements.blocks.add(() -> new BlockCustom().setRegistryName("cavesnotcliffs", "amethyst_crystal"));
        elements.items.add(() -> {
            Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("cavesnotcliffs", "amethyst_crystal"));
            return new ItemBlock(b).setRegistryName(b.getRegistryName());
        });
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("cavesnotcliffs", "amethyst_crystal"));
        if (item != null)
            ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation("cavesnotcliffs:amethyst_crystal", "inventory"));
    }

    public static class BlockCustom extends Block {
        public static final PropertyDirection FACING = PropertyDirection.create("facing");
        private static final AxisAlignedBB AABB = new AxisAlignedBB(0.1, 0.0, 0.1, 0.9, 0.8, 0.9);

        public BlockCustom() {
            super(Material.ROCK);
            setUnlocalizedName("amethyst_crystal");
            setCreativeTab(net.minecraft.creativetab.CreativeTabs.BUILDING_BLOCKS);
            setSoundType(SoundType.GLASS);
            setHardness(1.5f);
            setResistance(1.0f);
            setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.UP));
        }

        @Override public boolean isOpaqueCube(IBlockState state) { return false; }
        @Override public boolean isFullCube(IBlockState state) { return false; }
        @Override public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) { return AABB; }
        @SideOnly(Side.CLIENT) @Override public BlockRenderLayer getBlockLayer() { return BlockRenderLayer.CUTOUT; }

        @Override
        public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
            return getDefaultState().withProperty(FACING, facing);
        }

        @Override
        protected BlockStateContainer createBlockState() {
            return new BlockStateContainer(this, FACING);
        }

        @Override
        public IBlockState getStateFromMeta(int meta) {
            return getDefaultState().withProperty(FACING, EnumFacing.getFront(meta & 7));
        }

        @Override
        public int getMetaFromState(IBlockState state) {
            return state.getValue(FACING).getIndex();
        }

        @Override
        public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
            if (!worldIn.isRemote) {
                EnumFacing facing = state.getValue(FACING);
                BlockPos support = pos.offset(facing.getOpposite());
                if (fromPos.equals(support) && !worldIn.getBlockState(support).isSideSolid(worldIn, support, facing)) {
                    worldIn.destroyBlock(pos, true);
                }
            }
        }
    }
}

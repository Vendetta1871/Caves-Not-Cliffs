
package net.celestiald.cavesnotcliffs.block;

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
import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;

@ElementsCavesNotCliffs.ModElement.Tag
public class BlockAmethystCrystalStage2 extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:medium_amethyst_bud")
    public static final Block block = null;

    public BlockAmethystCrystalStage2(ElementsCavesNotCliffs instance) { super(instance, 33); }

    @Override
    public void initElements() {
        elements.blocks.add(() -> new BlockCustom().setRegistryName("cavesnotcliffs", "medium_amethyst_bud"));
        elements.items.add(() -> {
            Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("cavesnotcliffs", "medium_amethyst_bud"));
            return new ItemBlock(b).setRegistryName(b.getRegistryName());
        });
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("cavesnotcliffs", "medium_amethyst_bud"));
        if (item != null)
            ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation("cavesnotcliffs:amethyst_crystal_stage_2", "inventory"));
    }

    public static class BlockCustom extends Block {
        public static final PropertyDirection FACING = PropertyDirection.create("facing");
        private static final AxisAlignedBB AABB = new AxisAlignedBB(0.1, 0.0, 0.1, 0.9, 0.65, 0.9);

        public BlockCustom() {
            super(Material.ROCK);
            setUnlocalizedName("medium_amethyst_bud");
            setCreativeTab(net.minecraft.creativetab.CreativeTabs.BUILDING_BLOCKS);
            setSoundType(SoundType.GLASS);
            setHardness(1.5f);
            setResistance(1.0f);
            setTickRandomly(true);
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
        public Item getItemDropped(IBlockState state, Random rand, int fortune) {
            Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("cavesnotcliffs", "amethyst_cluster"));
            return b != null ? Item.getItemFromBlock(b) : net.minecraft.init.Items.AIR;
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

        @Override
        public void randomTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
            if (!worldIn.isRemote && rand.nextInt(5) == 0) {
                EnumFacing facing = state.getValue(FACING);
                Block support = worldIn.getBlockState(pos.offset(facing.getOpposite())).getBlock();
                Block geode = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("cavesnotcliffs", "amethyst_block"));
                Block casing = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("cavesnotcliffs", "smooth_basalt"));
                Block crystal = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("cavesnotcliffs", "amethyst_cluster"));
                if (crystal != null && (support == geode || support == casing)) {
                    worldIn.setBlockState(pos, crystal.getDefaultState()
                        .withProperty(BlockAmethystCrystal.BlockCustom.FACING, facing));
                }
            }
        }
    }
}

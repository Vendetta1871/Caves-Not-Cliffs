
package net.mcreator.cavesnotcliffs.block;

import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraft.block.Block;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.block.material.Material;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.entity.Entity;
import net.mcreator.cavesnotcliffs.ElementsCavesNotCliffs;

@ElementsCavesNotCliffs.ModElement.Tag
public class BlockTopStalagmite extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:top_stalagmite")
    public static final Block block = null;

    public BlockTopStalagmite(ElementsCavesNotCliffs instance) { super(instance, 26); }

    @Override
    public void initElements() {
        elements.blocks.add(() -> new BlockCustom().setRegistryName("top_stalagmite"));
        elements.items.add(() -> new ItemBlock(block).setRegistryName(block.getRegistryName()));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
            new ModelResourceLocation("cavesnotcliffs:top_stalagmite", "inventory"));
    }

    public static class BlockCustom extends Block {
        private static final AxisAlignedBB SHAFT_AABB = new AxisAlignedBB(0.3, 0.0, 0.3, 0.7, 1.0, 0.7);

        public BlockCustom() {
            super(Material.ROCK);
            setUnlocalizedName("top_stalagmite");
            setSoundType(SoundType.STONE);
            setHardness(1.5f);
            setResistance(6.0f);
            setTickRandomly(true);
        }

        @Override public boolean isOpaqueCube(IBlockState state) { return false; }
        @Override public boolean isFullCube(IBlockState state) { return false; }
        @Override public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) { return SHAFT_AABB; }
        @Override
        public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean isActualState) {
            addCollisionBoxToList(pos, entityBox, collidingBoxes, SHAFT_AABB);
        }
        @SideOnly(Side.CLIENT) @Override public BlockRenderLayer getBlockLayer() { return BlockRenderLayer.CUTOUT; }

        @Override
        public Item getItemDropped(IBlockState state, Random rand, int fortune) {
            return Item.getItemFromBlock(BlockStalactite.block);
        }

        private boolean hasValidSupport(World worldIn, BlockPos pos) {
            IBlockState below = worldIn.getBlockState(pos.down());
            Block belowBlock = below.getBlock();
            return belowBlock == BlockMiddleStalagmite.block
                || belowBlock == BlockBottomStalagmite.block
                || below.isSideSolid(worldIn, pos.down(), EnumFacing.UP);
        }

        @Override
        public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
            if (!worldIn.isRemote && fromPos.equals(pos.down()) && !hasValidSupport(worldIn, pos)) {
                worldIn.destroyBlock(pos, true);
            }
        }

        @Override
        public void randomTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
            if (!worldIn.isRemote && rand.nextInt(50) == 0) {
                BlockPos above = pos.up();
                if (worldIn.isAirBlock(above)) {
                    Block below = worldIn.getBlockState(pos.down()).getBlock();
                    IBlockState newState = (below == BlockBottomStalagmite.block || below == BlockMiddleStalagmite.block)
                        ? BlockMiddleStalagmite.block.getDefaultState()
                        : BlockBottomStalagmite.block.getDefaultState();
                    worldIn.setBlockState(pos, newState, 3);
                    worldIn.setBlockState(above, BlockCustom.this.getDefaultState(), 3);
                }
            }
        }
    }
}


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
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.mcreator.cavesnotcliffs.ElementsCavesNotCliffs;

@ElementsCavesNotCliffs.ModElement.Tag
public class BlockStalactite extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:stalactite")
    public static final Block block = null;

    public BlockStalactite(ElementsCavesNotCliffs instance) { super(instance, 27); }

    @Override
    public void initElements() {
        elements.blocks.add(() -> new BlockCustom().setRegistryName("stalactite"));
        elements.items.add(() -> new ItemPointedDripstoneBlock(block).setRegistryName(block.getRegistryName()).setCreativeTab(net.minecraft.creativetab.CreativeTabs.BUILDING_BLOCKS));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
            new ModelResourceLocation("cavesnotcliffs:stalactite", "inventory"));
    }

    private static class BlockCustom extends Block {
        public BlockCustom() {
            super(Material.ROCK);
            setUnlocalizedName("stalactite");
            setSoundType(SoundType.STONE);
            setHardness(1.5f);
            setResistance(6.0f);
            setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
        }
    }

    public static class ItemPointedDripstoneBlock extends ItemBlock {
        public ItemPointedDripstoneBlock(Block block) {
            super(block);
            setMaxStackSize(64);
        }

        @Override
        public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos,
                EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
            if (worldIn.isRemote) return EnumActionResult.SUCCESS;

            if (facing == EnumFacing.DOWN) {
                BlockPos target = pos.offset(EnumFacing.DOWN);
                if (worldIn.isAirBlock(target)) {
                    worldIn.setBlockState(target, BlockTopStalactite.block.getDefaultState(), 3);
                    if (!player.capabilities.isCreativeMode)
                        player.getHeldItem(hand).shrink(1);
                    return EnumActionResult.SUCCESS;
                }
            } else if (facing == EnumFacing.UP) {
                BlockPos target = pos.offset(EnumFacing.UP);
                if (worldIn.isAirBlock(target)) {
                    worldIn.setBlockState(target, BlockTopStalagmite.block.getDefaultState(), 3);
                    if (!player.capabilities.isCreativeMode)
                        player.getHeldItem(hand).shrink(1);
                    return EnumActionResult.SUCCESS;
                }
            }
            return EnumActionResult.FAIL;
        }
    }
}

package net.celestiald.cavesnotcliffs.item;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.block.LushCaveVinesBlock;
import net.celestiald.cavesnotcliffs.content.LushCaveContent;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Canonical edible cave-vine planting item. */
@ElementsCavesNotCliffs.ModElement.Tag
public final class ItemGlowBerries extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:glow_berries")
    public static final Item item = null;

    public ItemGlowBerries(ElementsCavesNotCliffs elements) {
        super(elements, 202);
    }

    @Override
    public void initElements() {
        elements.items.add(() -> new GlowBerryItem()
                .setRegistryName(CncRegistryIds.GLOW_BERRIES));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(CncRegistryIds.GLOW_BERRIES, "inventory"));
    }

    private static final class GlowBerryItem extends ItemFood {
        private GlowBerryItem() {
            super(2, 0.1F, false);
            setUnlocalizedName("glow_berries");
            setCreativeTab(net.minecraft.creativetab.CreativeTabs.FOOD);
        }

        @Override
        public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos supportPos,
                EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
            if (facing != EnumFacing.DOWN || LushCaveContent.CAVE_VINES == null) {
                return EnumActionResult.FAIL;
            }

            BlockPos target = supportPos.down();
            ItemStack held = player.getHeldItem(hand);
            IBlockState support = world.getBlockState(supportPos);
            if (!world.isAirBlock(target)
                    || !support.isSideSolid(world, supportPos, EnumFacing.DOWN)
                    && !LushCaveVinesBlock.isVine(support.getBlock())
                    || !player.canPlayerEdit(target, facing, held)) {
                return EnumActionResult.FAIL;
            }

            if (!world.isRemote) {
                world.setBlockState(target,
                        LushCaveVinesBlock.headState(world.rand.nextInt(25), false), 11);
                if (!player.capabilities.isCreativeMode) {
                    held.shrink(1);
                }
            }
            return EnumActionResult.SUCCESS;
        }
    }
}

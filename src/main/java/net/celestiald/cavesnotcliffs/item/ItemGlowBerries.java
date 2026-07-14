package net.celestiald.cavesnotcliffs.item;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.block.LushCaveVinesBlock;
import net.celestiald.cavesnotcliffs.content.LushCaveContent;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
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
        public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos clickedPos,
                EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
            if (LushCaveContent.CAVE_VINES == null) {
                return EnumActionResult.FAIL;
            }

            ItemStack held = player.getHeldItem(hand);
            Block clickedBlock = world.getBlockState(clickedPos).getBlock();
            BlockPos target = clickedBlock.isReplaceable(world, clickedPos)
                ? clickedPos : clickedPos.offset(facing);
            if (held.isEmpty() || !player.canPlayerEdit(target, facing, held)
                    || !world.getBlockState(target).getBlock()
                        .isReplaceable(world, target)) {
                return EnumActionResult.FAIL;
            }

            IBlockState below = world.getBlockState(target.down());
            IBlockState placed = LushCaveVinesBlock.isVine(below.getBlock())
                ? LushCaveVinesBlock.bodyState(false)
                : LushCaveVinesBlock.headState(world.rand.nextInt(25), false);
            if (!world.mayPlace(LushCaveContent.CAVE_VINES,
                    target, false, facing, player)) {
                return EnumActionResult.FAIL;
            }
            if (!world.setBlockState(target, placed, 11)) {
                return EnumActionResult.FAIL;
            }

            IBlockState actual = world.getBlockState(target);
            if (actual.getBlock() == placed.getBlock()) {
                actual.getBlock().onBlockPlacedBy(world, target, actual, player, held);
                if (player instanceof EntityPlayerMP) {
                    CriteriaTriggers.PLACED_BLOCK.trigger(
                        (EntityPlayerMP) player, target, held);
                }
            }
            SoundType sound = actual.getBlock().getSoundType(
                actual, world, target, player);
            world.playSound(player, target, sound.getPlaceSound(),
                SoundCategory.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F,
                sound.getPitch() * 0.8F);
            if (!player.capabilities.isCreativeMode) {
                held.shrink(1);
            }
            return EnumActionResult.SUCCESS;
        }
    }
}

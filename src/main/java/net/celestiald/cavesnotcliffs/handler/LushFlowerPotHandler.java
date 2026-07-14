package net.celestiald.cavesnotcliffs.handler;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.content.LushCaveContent;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFlowerPot;
import net.minecraft.util.EnumActionResult;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Adds the two 1.18.2 azalea flower-pot registrations to the fixed 1.12 pot table. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class LushFlowerPotHandler {
    private LushFlowerPotHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClickPot(PlayerInteractEvent.RightClickBlock event) {
        if (event.getUseItem()
                == net.minecraftforge.fml.common.eventhandler.Event.Result.DENY) {
            return;
        }
        World world = event.getWorld();
        if (world.getBlockState(event.getPos()).getBlock() != Blocks.FLOWER_POT) {
            return;
        }
        if (!isEmptyFlowerPot(world.getTileEntity(event.getPos()))) {
            return;
        }
        ItemStack held = event.getItemStack();
        Block content = Block.getBlockFromItem(held.getItem());
        Block potted;
        if (content == LushCaveContent.AZALEA) {
            potted = LushCaveContent.POTTED_AZALEA;
        } else if (content == LushCaveContent.FLOWERING_AZALEA) {
            potted = LushCaveContent.POTTED_FLOWERING_AZALEA;
        } else {
            return;
        }

        EntityPlayer player = event.getEntityPlayer();
        if (event.getFace() == null
                || !world.isBlockModifiable(player, event.getPos())
                || !player.canPlayerEdit(event.getPos(), event.getFace(), held)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        event.setUseBlock(net.minecraftforge.fml.common.eventhandler.Event.Result.DENY);
        event.setUseItem(net.minecraftforge.fml.common.eventhandler.Event.Result.DENY);
        player.swingArm(event.getHand());
        if (!world.isRemote) {
            world.setBlockState(event.getPos(), potted.getDefaultState(), 3);
            player.addStat(StatList.FLOWER_POTTED);
            if (!player.capabilities.isCreativeMode) {
                held.shrink(1);
            }
        }
    }

    /**
     * Vanilla stores a flower pot's contents in its tile entity rather than block state. Missing
     * or unexpected tile data must fail closed so an azalea interaction can never erase contents.
     */
    static boolean isEmptyFlowerPot(TileEntity tileEntity) {
        return tileEntity instanceof TileEntityFlowerPot
                && ((TileEntityFlowerPot) tileEntity).getFlowerItemStack().isEmpty();
    }
}

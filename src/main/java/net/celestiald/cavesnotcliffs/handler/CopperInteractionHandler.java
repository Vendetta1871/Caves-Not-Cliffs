package net.celestiald.cavesnotcliffs.handler;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.content.CopperContent;
import net.celestiald.cavesnotcliffs.content.CopperInteractionEffects;
import net.celestiald.cavesnotcliffs.content.CopperSoundEvents;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Axe scraping and unwaxing bridge with Java 1.18.2 precedence and durability. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class CopperInteractionHandler {
    private CopperInteractionHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack held = event.getItemStack();
        if (held.isEmpty() || !(held.getItem() instanceof ItemAxe)) {
            return;
        }

        EntityPlayer player = event.getEntityPlayer();
        if (!player.canPlayerEdit(event.getPos(), event.getFace(), held)) {
            return;
        }

        World world = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState state = world.getBlockState(pos);
        IBlockState changed = CopperContent.previous(state);
        boolean scrape = changed != null;
        if (!scrape) {
            changed = CopperContent.unwaxed(state);
        }
        if (changed == null) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        event.setUseBlock(net.minecraftforge.fml.common.eventhandler.Event.Result.DENY);
        event.setUseItem(net.minecraftforge.fml.common.eventhandler.Event.Result.DENY);

        player.swingArm(event.getHand());
        if (world.isRemote) {
            return;
        }

        world.playSound(null, pos,
                scrape ? CopperSoundEvents.AXE_SCRAPE : CopperSoundEvents.AXE_WAX_OFF,
                SoundCategory.BLOCKS, 1.0F, 1.0F);
        CopperInteractionEffects.spawn(world, pos, state, scrape);
        world.setBlockState(pos, changed, 11);
        ItemStack original = held.copy();
        StatBase useStat = StatList.getObjectUseStats(original.getItem());
        if (useStat != null) {
            player.addStat(useStat);
        }
        held.damageItem(1, player);
        if (held.isEmpty()) {
            ForgeEventFactory.onPlayerDestroyItem(player, original, event.getHand());
        }
    }

}

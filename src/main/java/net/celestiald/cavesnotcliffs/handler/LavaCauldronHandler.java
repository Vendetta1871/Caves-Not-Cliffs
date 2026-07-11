package net.celestiald.cavesnotcliffs.handler;

import net.celestiald.cavesnotcliffs.block.BlockLavaCauldron;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Converts an empty vanilla cauldron when a player uses a lava bucket on it. */
public final class LavaCauldronHandler {
    public static final LavaCauldronHandler INSTANCE = new LavaCauldronHandler();

    private LavaCauldronHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onCauldronPlaced(BlockEvent.PlaceEvent event) {
        if (event.getWorld().isRemote || event.getPlacedBlock().getBlock() != Blocks.CAULDRON
                || BlockLavaCauldron.block == null) {
            return;
        }
        event.getWorld().setBlockState(event.getPos(),
                BlockLavaCauldron.block.getDefaultState(), 3);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickCauldron(PlayerInteractEvent.RightClickBlock event) {
        World world = event.getWorld();
        IBlockState state = world.getBlockState(event.getPos());
        EntityPlayer player = event.getEntityPlayer();
        ItemStack held = player.getHeldItem(event.getHand());
        if (world.isRemote || state.getBlock() != Blocks.CAULDRON || held.isEmpty()
                || held.getItem() != Items.LAVA_BUCKET
                || state.getValue(BlockCauldron.LEVEL) != 0
                || BlockLavaCauldron.block == null) {
            return;
        }

        world.setBlockState(event.getPos(), BlockLavaCauldron.block.getDefaultState()
                .withProperty(BlockLavaCauldron.BlockCustom.IS_LAVA, true)
                .withProperty(BlockCauldron.LEVEL, 3), 3);
        if (!player.capabilities.isCreativeMode) {
            player.setHeldItem(event.getHand(), new ItemStack(Items.BUCKET));
        }
        player.addStat(StatList.CAULDRON_FILLED);
        world.playSound(null, event.getPos(), SoundEvents.ITEM_BUCKET_EMPTY,
                SoundCategory.BLOCKS, 1.0F, 1.0F);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        event.setCanceled(true);
    }
}

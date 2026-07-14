package net.celestiald.cavesnotcliffs.handler;

import net.celestiald.cavesnotcliffs.block.BlockCandle;
import net.celestiald.cavesnotcliffs.block.BlockCandleCake;
import net.celestiald.cavesnotcliffs.block.CandleEffects;
import net.celestiald.cavesnotcliffs.block.CandleLightable;
import net.celestiald.cavesnotcliffs.content.CandleSoundEvents;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCake;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/** Cake insertion plus flint, fire-charge, and burning-projectile candle lighting. */
public final class CandleInteractionHandler {
    public static final CandleInteractionHandler INSTANCE = new CandleInteractionHandler();

    private CandleInteractionHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState state = world.getBlockState(pos);
        ItemStack held = event.getItemStack();
        EntityPlayer player = event.getEntityPlayer();

        if (insertCandleIntoCake(world, pos, state, player, held)) {
            succeed(event);
            return;
        }

        if (!(state.getBlock() instanceof CandleLightable)
                || held.isEmpty()
                || (held.getItem() != Items.FLINT_AND_STEEL
                && held.getItem() != Items.FIRE_CHARGE)
                || !((CandleLightable) state.getBlock()).canLight(state)) {
            return;
        }
        if (!world.isRemote) {
            Item used = held.getItem();
            boolean flint = held.getItem() == Items.FLINT_AND_STEEL;
            world.playSound(null, pos,
                    flint ? net.minecraft.init.SoundEvents.ITEM_FLINTANDSTEEL_USE
                            : net.minecraft.init.SoundEvents.ITEM_FIRECHARGE_USE,
                    SoundCategory.BLOCKS, 1.0F,
                    flint ? world.rand.nextFloat() * 0.4F + 0.8F
                            : (world.rand.nextFloat() - world.rand.nextFloat())
                            * 0.2F + 1.0F);
            CandleEffects.light(world, pos, state);
            if (!player.capabilities.isCreativeMode) {
                if (flint) {
                    held.damageItem(1, player);
                } else {
                    held.shrink(1);
                }
            }
            StatBase stat = StatList.getObjectUseStats(used);
            if (stat != null) {
                player.addStat(stat);
            }
        }
        succeed(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onProjectileImpact(ProjectileImpactEvent event) {
        RayTraceResult hit = event.getRayTraceResult();
        Entity projectile = event.getEntity();
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK
                || !projectile.isBurning()) {
            return;
        }
        World world = projectile.world;
        if (world.isRemote) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        CandleEffects.light(world, pos, world.getBlockState(pos));
    }

    private static boolean insertCandleIntoCake(World world, BlockPos pos,
            IBlockState state, EntityPlayer player, ItemStack held) {
        if (state.getBlock() != Blocks.CAKE
                || state.getValue(BlockCake.BITES) != 0
                || held.isEmpty() || !(held.getItem() instanceof ItemBlock)) {
            return false;
        }
        Block heldBlock = ((ItemBlock) held.getItem()).getBlock();
        if (!(heldBlock instanceof BlockCandle)) {
            return false;
        }
        BlockCandle candle = (BlockCandle) heldBlock;
        Block cake = ForgeRegistries.BLOCKS.getValue(CncRegistryIds.id(
                candle.getColor().getCandleCakePath()));
        if (!(cake instanceof BlockCandleCake)) {
            return false;
        }
        if (!world.isRemote) {
            Item used = held.getItem();
            if (!player.capabilities.isCreativeMode) {
                held.shrink(1);
            }
            world.playSound(null, pos, CandleSoundEvents.CAKE_ADD_CANDLE,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);
            world.setBlockState(pos, cake.getDefaultState(), 3);
            StatBase stat = StatList.getObjectUseStats(used);
            if (stat != null) {
                player.addStat(stat);
            }
        }
        return true;
    }

    private static void succeed(PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
    }
}

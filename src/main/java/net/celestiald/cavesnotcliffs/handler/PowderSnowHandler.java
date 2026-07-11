package net.celestiald.cavesnotcliffs.handler;

import net.celestiald.cavesnotcliffs.block.BlockPowderSnow;
import net.celestiald.cavesnotcliffs.powdersnow.PowderSnowMechanics;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityMagmaCube;
import net.minecraft.entity.monster.EntityPolarBear;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.entity.monster.EntityStray;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.UUID;

/** Player bucket bridge plus persisted Java 1.18.2 freezing for 1.12 entities. */
public final class PowderSnowHandler {
    public static final PowderSnowHandler INSTANCE = new PowderSnowHandler();

    private static final String FROZEN_TICKS_KEY = "TicksFrozen";
    private static final UUID FROZEN_SPEED_UUID =
        UUID.fromString("1eaf83ff-7207-4596-b37a-d7a07b3ec4ce");
    private static final DamageSource FREEZE_DAMAGE =
        new DamageSource("freeze").setDamageBypassesArmor();

    private PowderSnowHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPowderSnowPickup(PlayerInteractEvent.RightClickBlock event) {
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        EntityPlayer player = event.getEntityPlayer();
        ItemStack held = player.getHeldItem(event.getHand());
        if (BlockPowderSnow.block == null || BlockPowderSnow.bucket == null
                || world.getBlockState(pos).getBlock() != BlockPowderSnow.block
                || held.isEmpty() || held.getItem() != Items.BUCKET) {
            return;
        }

        event.setCancellationResult(EnumActionResult.SUCCESS);
        event.setCanceled(true);
        if (world.isRemote) {
            return;
        }

        world.setBlockToAir(pos);
        world.playEvent(2001, pos, net.minecraft.block.Block.getStateId(
            BlockPowderSnow.block.getDefaultState()));
        world.playSound(null, pos, BlockPowderSnow.BUCKET_FILL_SOUND,
            SoundCategory.BLOCKS, 1.0F, 1.0F);
        giveFilledBucket(player, event.getHand(), held,
            new ItemStack(BlockPowderSnow.bucket));
        StatBase useBucket = StatList.getObjectUseStats(Items.BUCKET);
        if (useBucket != null) {
            player.addStat(useBucket);
        }
        syncInventory(player);
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        World world = entity.world;
        if (world.isRemote || BlockPowderSnow.block == null || !entity.isEntityAlive()) {
            return;
        }

        boolean inPowderSnow = intersectsPowderSnow(entity);
        boolean canFreeze = canFreeze(entity);
        NBTTagCompound data = entity.getEntityData();
        int current = data.getInteger(FROZEN_TICKS_KEY);
        int next = PowderSnowMechanics.nextFrozenTicks(current, inPowderSnow, canFreeze);
        if (next == 0) {
            data.removeTag(FROZEN_TICKS_KEY);
        } else {
            data.setInteger(FROZEN_TICKS_KEY, next);
        }
        updateFrozenSpeed(entity, next);

        if (PowderSnowMechanics.shouldApplyFrozenDamage(
                entity.ticksExisted, next, canFreeze)) {
            entity.attackEntityFrom(FREEZE_DAMAGE,
                PowderSnowMechanics.frozenDamage(hurtsExtra(entity)));
        }
    }

    static int frozenTicks(EntityLivingBase entity) {
        return entity.getEntityData().getInteger(FROZEN_TICKS_KEY);
    }

    private boolean intersectsPowderSnow(EntityLivingBase entity) {
        AxisAlignedBB box = entity.getEntityBoundingBox();
        int minX = MathHelper.floor(box.minX + 1.0E-7D);
        int minY = MathHelper.floor(box.minY + 1.0E-7D);
        int minZ = MathHelper.floor(box.minZ + 1.0E-7D);
        int maxX = MathHelper.floor(box.maxX - 1.0E-7D);
        int maxY = MathHelper.floor(box.maxY - 1.0E-7D);
        int maxZ = MathHelper.floor(box.maxZ - 1.0E-7D);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = minY; y <= maxY; ++y) {
            for (int z = minZ; z <= maxZ; ++z) {
                for (int x = minX; x <= maxX; ++x) {
                    cursor.setPos(x, y, z);
                    if (entity.world.getBlockState(cursor).getBlock() == BlockPowderSnow.block) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean canFreeze(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer && ((EntityPlayer) entity).isSpectator()) {
            return false;
        }
        if (entity instanceof EntityStray || entity instanceof EntityPolarBear
                || entity instanceof EntitySnowman || entity instanceof EntityWither) {
            return false;
        }
        for (ItemStack armor : entity.getArmorInventoryList()) {
            if (!armor.isEmpty() && armor.getItem() instanceof ItemArmor
                    && ((ItemArmor) armor.getItem()).getArmorMaterial()
                        == ItemArmor.ArmorMaterial.LEATHER) {
                return false;
            }
        }
        // Leather horse armor is a 1.18 freeze-immune wearable, but it has no 1.12 item peer.
        return true;
    }

    private boolean hurtsExtra(EntityLivingBase entity) {
        return entity instanceof EntityBlaze || entity instanceof EntityMagmaCube;
    }

    private void updateFrozenSpeed(EntityLivingBase entity, int frozenTicks) {
        IAttributeInstance movement = entity.getEntityAttribute(
            net.minecraft.entity.SharedMonsterAttributes.MOVEMENT_SPEED);
        if (movement == null) {
            return;
        }
        AttributeModifier existing = movement.getModifier(FROZEN_SPEED_UUID);
        if (existing != null) {
            movement.removeModifier(existing);
        }
        BlockPos supporting = new BlockPos(entity.posX,
            entity.getEntityBoundingBox().minY - 0.2D, entity.posZ);
        if (frozenTicks > 0 && !entity.world.isAirBlock(supporting)) {
            movement.applyModifier(new AttributeModifier(FROZEN_SPEED_UUID,
                "Powder snow slow",
                PowderSnowMechanics.movementSpeedModifier(frozenTicks), 0)
                .setSaved(false));
        }
    }

    private void giveFilledBucket(EntityPlayer player, net.minecraft.util.EnumHand hand,
            ItemStack emptyBuckets, ItemStack filled) {
        if (player.capabilities.isCreativeMode) {
            if (!player.inventory.hasItemStack(filled)) {
                player.inventory.addItemStackToInventory(filled);
            }
            return;
        }
        emptyBuckets.shrink(1);
        if (emptyBuckets.isEmpty()) {
            player.setHeldItem(hand, filled);
        } else if (!player.inventory.addItemStackToInventory(filled)) {
            player.dropItem(filled, false);
        }
    }

    private void syncInventory(EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
        }
    }
}

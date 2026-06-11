package net.celestiald.cavesnotcliffs.mixin;

import net.celestiald.cavesnotcliffs.block.BlockTopStalactite;
import net.celestiald.cavesnotcliffs.block.BlockMiddleStalactite;
import net.celestiald.cavesnotcliffs.block.BlockBottomStalactite;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBanner;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtils;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntityBanner;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

/**
 * Extends vanilla BlockCauldron to support lava (levels 4-6) in addition to water (1-3).
 * Level encoding: 0=empty, 1-3=water, 4-6=lava.
 */
@Mixin(BlockCauldron.class)
public abstract class LavaCauldronMixin extends Block {

    @Shadow @Mutable @Final
    public static PropertyInteger LEVEL;

    protected LavaCauldronMixin(Material materialIn) {
        super(materialIn);
    }

    // Expand LEVEL from 0-3 to 0-6 before BlockCauldron is instantiated
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void expandLevel(CallbackInfo ci) {
        LEVEL = PropertyInteger.create("level", 0, 6);
    }

    // Enable random ticks so dripstone drip logic fires
    @Inject(method = "<init>", at = @At("RETURN"))
    private void enableRandomTicks(CallbackInfo ci) {
        setTickRandomly(true);
    }

    @Overwrite
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer((Block)(Object)this, new IProperty[]{LEVEL});
    }

    @Overwrite
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(LEVEL, MathHelper.clamp(meta, 0, 6));
    }

    @Overwrite
    public int getMetaFromState(IBlockState state) {
        return (Integer) state.getValue(LEVEL);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void setWaterLevel(World worldIn, BlockPos pos, IBlockState state, int level) {
        if ((Integer) state.getValue(LEVEL) < 4) {
            worldIn.setBlockState(pos, state.withProperty(LEVEL, MathHelper.clamp(level, 0, 3)), 2);
            worldIn.updateComparatorOutputLevel(pos, (Block)(Object)this);
        }
    }

    private void setLavaLevel(World worldIn, BlockPos pos, IBlockState state, int level) {
        int current = (Integer) state.getValue(LEVEL);
        if (current < 1 || current > 3) {
            level = MathHelper.clamp(level, 0, 3);
            level = level == 0 ? 0 : level + 3;
            worldIn.setBlockState(pos, state.withProperty(LEVEL, level), 2);
            worldIn.updateComparatorOutputLevel(pos, (Block)(Object)this);
        }
    }

    // Checks whether a matching liquid is dripping from a stalactite above the cauldron
    private boolean isLiquidAbove(World worldIn, BlockPos pos, Material liquidType) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        for (int i = 0; i < 9 && worldIn.isAirBlock(new BlockPos(x, y + 1, z)); i++) y++;
        Block top = worldIn.getBlockState(new BlockPos(x, y + 1, z)).getBlock();
        if (top != BlockTopStalactite.block) return false;
        if (worldIn.getBlockState(new BlockPos(x, y + 2, z)).getBlock() == BlockMiddleStalactite.block) {
            y++;
            if (worldIn.getBlockState(new BlockPos(x, y + 2, z)).getBlock() == BlockBottomStalactite.block) y++;
        }
        return worldIn.getBlockState(new BlockPos(x, y + 3, z)).getMaterial() == liquidType;
    }

    // ── Block interactions ────────────────────────────────────────────────────

    @Overwrite
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
            EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack held = playerIn.getHeldItem(hand);
        if (held.isEmpty()) return true;
        int level = (Integer) state.getValue(LEVEL);
        Item item = held.getItem();

        if (item == Items.WATER_BUCKET) {
            if (level < 3 && !worldIn.isRemote) {
                if (!playerIn.capabilities.isCreativeMode) playerIn.setHeldItem(hand, new ItemStack(Items.BUCKET));
                playerIn.addStat(StatList.CAULDRON_FILLED);
                setWaterLevel(worldIn, pos, state, 3);
                worldIn.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
            return true;
        }
        if (item == Items.LAVA_BUCKET) {
            if (level != 1 && level != 2 && level != 3 && !worldIn.isRemote) {
                if (!playerIn.capabilities.isCreativeMode) playerIn.setHeldItem(hand, new ItemStack(Items.BUCKET));
                playerIn.addStat(StatList.CAULDRON_FILLED);
                setLavaLevel(worldIn, pos, state, 3);
                worldIn.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
            return true;
        }
        if (item == Items.BUCKET) {
            if (level == 3 && !worldIn.isRemote) {
                if (!playerIn.capabilities.isCreativeMode) {
                    held.shrink(1);
                    ItemStack water = new ItemStack(Items.WATER_BUCKET);
                    if (held.isEmpty()) playerIn.setHeldItem(hand, water);
                    else if (!playerIn.inventory.addItemStackToInventory(water)) playerIn.dropItem(water, false);
                }
                playerIn.addStat(StatList.CAULDRON_USED);
                setWaterLevel(worldIn, pos, state, 0);
                worldIn.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
            if (level == 6 && !worldIn.isRemote) {
                if (!playerIn.capabilities.isCreativeMode) {
                    held.shrink(1);
                    ItemStack lava = new ItemStack(Items.LAVA_BUCKET);
                    if (held.isEmpty()) playerIn.setHeldItem(hand, lava);
                    else if (!playerIn.inventory.addItemStackToInventory(lava)) playerIn.dropItem(lava, false);
                }
                playerIn.addStat(StatList.CAULDRON_USED);
                setLavaLevel(worldIn, pos, state, 0);
                worldIn.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
            return true;
        }
        if (item == Items.GLASS_BOTTLE) {
            if (level > 0 && level < 4 && !worldIn.isRemote) {
                if (!playerIn.capabilities.isCreativeMode) {
                    ItemStack bottle = PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.WATER);
                    playerIn.addStat(StatList.CAULDRON_USED);
                    held.shrink(1);
                    if (held.isEmpty()) playerIn.setHeldItem(hand, bottle);
                    else if (!playerIn.inventory.addItemStackToInventory(bottle)) playerIn.dropItem(bottle, false);
                    else if (playerIn instanceof EntityPlayerMP) ((EntityPlayerMP)playerIn).sendContainerToPlayer(playerIn.inventoryContainer);
                }
                worldIn.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                setWaterLevel(worldIn, pos, state, level - 1);
            }
            return true;
        }
        if (item == Items.POTIONITEM && PotionUtils.getPotionFromItem(held) == PotionTypes.WATER) {
            if (level < 3 && !worldIn.isRemote) {
                if (!playerIn.capabilities.isCreativeMode) {
                    playerIn.addStat(StatList.CAULDRON_USED);
                    playerIn.setHeldItem(hand, new ItemStack(Items.GLASS_BOTTLE));
                    if (playerIn instanceof EntityPlayerMP) ((EntityPlayerMP)playerIn).sendContainerToPlayer(playerIn.inventoryContainer);
                }
                worldIn.playSound(null, pos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
                setWaterLevel(worldIn, pos, state, level + 1);
            }
            return true;
        }
        if (level > 0 && level < 4) {
            if (item instanceof ItemArmor) {
                ItemArmor armor = (ItemArmor) item;
                if (armor.getArmorMaterial() == ItemArmor.ArmorMaterial.LEATHER && armor.hasColor(held) && !worldIn.isRemote) {
                    armor.removeColor(held);
                    setWaterLevel(worldIn, pos, state, level - 1);
                    playerIn.addStat(StatList.ARMOR_CLEANED);
                    return true;
                }
            }
            if (item instanceof ItemBanner && TileEntityBanner.getPatterns(held) > 0 && !worldIn.isRemote) {
                ItemStack clean = held.copy();
                clean.setCount(1);
                TileEntityBanner.removeBannerData(clean);
                playerIn.addStat(StatList.BANNER_CLEANED);
                if (!playerIn.capabilities.isCreativeMode) {
                    held.shrink(1);
                    setWaterLevel(worldIn, pos, state, level - 1);
                }
                if (held.isEmpty()) playerIn.setHeldItem(hand, clean);
                else if (!playerIn.inventory.addItemStackToInventory(clean)) playerIn.dropItem(clean, false);
                else if (playerIn instanceof EntityPlayerMP) ((EntityPlayerMP)playerIn).sendContainerToPlayer(playerIn.inventoryContainer);
                return true;
            }
        }
        return false;
    }

    @Overwrite
    public void onEntityCollidedWithBlock(World worldIn, BlockPos pos, IBlockState state, Entity entityIn) {
        int level = (Integer) state.getValue(LEVEL);
        int subLevel = level > 3 ? (level - 3) : level;
        float liquidTop = (float) pos.getY() + (6.0F + (float)(3 * subLevel)) / 16.0F;
        if (!worldIn.isRemote && entityIn.getEntityBoundingBox().minY <= (double) liquidTop) {
            if (level > 0 && level < 4 && entityIn.isBurning()) {
                entityIn.extinguish();
                setWaterLevel(worldIn, pos, state, level - 1);
            } else if (level > 3) {
                entityIn.setFire(5);
            }
        }
    }

    // Forge-added method (not obfuscated), remap=false is correct
    @Inject(method = "getLightValue(Lnet/minecraft/block/state/IBlockState;)I",
            at = @At("RETURN"), cancellable = true, remap = false)
    public void onGetLightValue(IBlockState state, CallbackInfoReturnable<Integer> cir) {
        if ((Integer) state.getValue(LEVEL) > 3) {
            cir.setReturnValue(12);
        }
    }

    // Dripstone drip: randomly fill cauldron from stalactite above
    @Overwrite
    public void randomTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (rand.nextInt(2) == 1) {
            int level = (Integer) state.getValue(LEVEL);
            if (isLiquidAbove(worldIn, pos, Material.LAVA)) {
                if (level == 0 || level > 3) {
                    setLavaLevel(worldIn, pos, state, level + 1);
                }
            } else if (isLiquidAbove(worldIn, pos, Material.WATER)) {
                if (level < 3) {
                    setWaterLevel(worldIn, pos, state, level + 1);
                }
            }
        }
    }

    // Drip particles below stalactites (client-side); remap=false: Forge-added method
    @Inject(method = "randomDisplayTick(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Ljava/util/Random;)V",
            at = @At("TAIL"), remap = false)
    public void onRandomDisplayTick(IBlockState state, World worldIn, BlockPos pos, Random rand, CallbackInfo ci) {
        if ((rand.nextInt(10) + 1) <= 3) {
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            if (isLiquidAbove(worldIn, pos, Material.LAVA)) {
                worldIn.spawnParticle(EnumParticleTypes.LAVA, x, y + 1, z, 0, 1, 0);
            } else if (isLiquidAbove(worldIn, pos, Material.WATER)) {
                worldIn.spawnParticle(EnumParticleTypes.WATER_DROP, x, y + 1, z, 0, 1, 0);
            }
        }
    }

    @Inject(method = "fillWithRain", at = @At("HEAD"), cancellable = true)
    public void onFillWithRain(World worldIn, BlockPos pos, CallbackInfo ci) {
        IBlockState state = worldIn.getBlockState(pos);
        if ((Integer) state.getValue(LEVEL) > 3) {
            ci.cancel();
        }
    }

    @Overwrite
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.CAULDRON;
    }

    @Overwrite
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(Items.CAULDRON);
    }
}

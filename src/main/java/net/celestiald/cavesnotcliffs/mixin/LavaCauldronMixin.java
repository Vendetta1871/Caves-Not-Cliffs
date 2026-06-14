package net.celestiald.cavesnotcliffs.mixin;

import net.celestiald.cavesnotcliffs.block.BlockTopStalactite;
import net.celestiald.cavesnotcliffs.block.BlockMiddleStalactite;
import net.celestiald.cavesnotcliffs.block.BlockBottomStalactite;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
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
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

/**
 * Extends vanilla BlockCauldron with lava support via @Inject (not @Overwrite).
 *
 * @Overwrite renames methods to SRG names in compiled bytecode. At dev runtime,
 * FML has already deobfuscated BlockCauldron to MCP names, so SRG-named methods
 * never match their targets. @Inject stores the target as a string in the annotation;
 * in dev this matches the MCP name directly, in production the searge refmap maps it
 * to the SRG name.
 *
 * State encoding: is_lava=false + level 0-3 = empty/water; is_lava=true + level 1-3 = lava.
 * Metadata 0-3 = water, 4-7 = lava (meta - 4 = lava fill level).
 */
@Mixin(BlockCauldron.class)
public abstract class LavaCauldronMixin extends Block {

    @Shadow @Final
    public static PropertyInteger LEVEL;

    private static final PropertyBool IS_LAVA = PropertyBool.create("is_lava");

    protected LavaCauldronMixin(Material materialIn) {
        super(materialIn);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void enableRandomTicks(CallbackInfo ci) {
        setTickRandomly(true);
    }

    @Inject(method = "createBlockState", at = @At("HEAD"), cancellable = true)
    private void injectCreateBlockState(CallbackInfoReturnable<BlockStateContainer> cir) {
        cir.setReturnValue(new BlockStateContainer((Block)(Object)this, new IProperty[]{LEVEL, IS_LAVA}));
    }

    @Inject(method = "getStateFromMeta", at = @At("HEAD"), cancellable = true)
    private void injectGetStateFromMeta(int meta, CallbackInfoReturnable<IBlockState> cir) {
        if (meta >= 4) {
            cir.setReturnValue(getDefaultState()
                    .withProperty(IS_LAVA, true)
                    .withProperty(LEVEL, MathHelper.clamp(meta - 4, 0, 3)));
        } else {
            cir.setReturnValue(getDefaultState()
                    .withProperty(IS_LAVA, false)
                    .withProperty(LEVEL, MathHelper.clamp(meta, 0, 3)));
        }
    }

    @Inject(method = "getMetaFromState", at = @At("HEAD"), cancellable = true)
    private void injectGetMetaFromState(IBlockState state, CallbackInfoReturnable<Integer> cir) {
        int level = (Integer) state.getValue(LEVEL);
        boolean isLava = (Boolean) state.getValue(IS_LAVA);
        cir.setReturnValue(isLava ? (4 + level) : level);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void cncSetWaterLevel(World worldIn, BlockPos pos, IBlockState state, int level) {
        worldIn.setBlockState(pos,
                state.withProperty(IS_LAVA, false)
                     .withProperty(LEVEL, MathHelper.clamp(level, 0, 3)), 2);
        worldIn.updateComparatorOutputLevel(pos, (Block)(Object)this);
    }

    private void cncSetLavaLevel(World worldIn, BlockPos pos, IBlockState state, int lavaLevel) {
        if (lavaLevel <= 0) {
            worldIn.setBlockState(pos,
                    state.withProperty(IS_LAVA, false).withProperty(LEVEL, 0), 2);
        } else {
            worldIn.setBlockState(pos,
                    state.withProperty(IS_LAVA, true)
                         .withProperty(LEVEL, MathHelper.clamp(lavaLevel, 1, 3)), 2);
        }
        worldIn.updateComparatorOutputLevel(pos, (Block)(Object)this);
    }

    private boolean cncIsLiquidAbove(World worldIn, BlockPos pos, Material liquidType) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        for (int i = 0; i < 9 && worldIn.isAirBlock(new BlockPos(x, y + 1, z)); i++) y++;
        if (worldIn.getBlockState(new BlockPos(x, y + 1, z)).getBlock() != BlockTopStalactite.block) return false;
        if (worldIn.getBlockState(new BlockPos(x, y + 2, z)).getBlock() == BlockMiddleStalactite.block) {
            y++;
            if (worldIn.getBlockState(new BlockPos(x, y + 2, z)).getBlock() == BlockBottomStalactite.block) y++;
        }
        return worldIn.getBlockState(new BlockPos(x, y + 3, z)).getMaterial() == liquidType;
    }

    // ── Block interactions ────────────────────────────────────────────────────

    @Inject(method = "onBlockActivated", at = @At("HEAD"), cancellable = true)
    private void injectOnBlockActivated(World worldIn, BlockPos pos, IBlockState state,
            EntityPlayer playerIn, EnumHand hand, EnumFacing facing,
            float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
        ItemStack held = playerIn.getHeldItem(hand);
        if (held.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }
        int level = (Integer) state.getValue(LEVEL);
        boolean isLava = (Boolean) state.getValue(IS_LAVA);
        Item item = held.getItem();

        if (item == Items.WATER_BUCKET) {
            if (!isLava && level < 3 && !worldIn.isRemote) {
                if (!playerIn.capabilities.isCreativeMode) playerIn.setHeldItem(hand, new ItemStack(Items.BUCKET));
                playerIn.addStat(StatList.CAULDRON_FILLED);
                cncSetWaterLevel(worldIn, pos, state, 3);
                worldIn.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
            cir.setReturnValue(true);
            return;
        }
        if (item == Items.LAVA_BUCKET) {
            if (!isLava && level == 0 && !worldIn.isRemote) {
                if (!playerIn.capabilities.isCreativeMode) playerIn.setHeldItem(hand, new ItemStack(Items.BUCKET));
                playerIn.addStat(StatList.CAULDRON_FILLED);
                cncSetLavaLevel(worldIn, pos, state, 3);
                worldIn.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
            cir.setReturnValue(true);
            return;
        }
        if (item == Items.BUCKET) {
            if (!isLava && level == 3 && !worldIn.isRemote) {
                if (!playerIn.capabilities.isCreativeMode) {
                    held.shrink(1);
                    ItemStack water = new ItemStack(Items.WATER_BUCKET);
                    if (held.isEmpty()) playerIn.setHeldItem(hand, water);
                    else if (!playerIn.inventory.addItemStackToInventory(water)) playerIn.dropItem(water, false);
                }
                playerIn.addStat(StatList.CAULDRON_USED);
                cncSetWaterLevel(worldIn, pos, state, 0);
                worldIn.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
            if (isLava && level == 3 && !worldIn.isRemote) {
                if (!playerIn.capabilities.isCreativeMode) {
                    held.shrink(1);
                    ItemStack lava = new ItemStack(Items.LAVA_BUCKET);
                    if (held.isEmpty()) playerIn.setHeldItem(hand, lava);
                    else if (!playerIn.inventory.addItemStackToInventory(lava)) playerIn.dropItem(lava, false);
                }
                playerIn.addStat(StatList.CAULDRON_USED);
                cncSetLavaLevel(worldIn, pos, state, 0);
                worldIn.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
            cir.setReturnValue(true);
            return;
        }
        if (item == Items.GLASS_BOTTLE) {
            if (!isLava && level > 0 && !worldIn.isRemote) {
                if (!playerIn.capabilities.isCreativeMode) {
                    ItemStack bottle = PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.WATER);
                    playerIn.addStat(StatList.CAULDRON_USED);
                    held.shrink(1);
                    if (held.isEmpty()) playerIn.setHeldItem(hand, bottle);
                    else if (!playerIn.inventory.addItemStackToInventory(bottle)) playerIn.dropItem(bottle, false);
                    else if (playerIn instanceof EntityPlayerMP) ((EntityPlayerMP)playerIn).sendContainerToPlayer(playerIn.inventoryContainer);
                }
                worldIn.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
                cncSetWaterLevel(worldIn, pos, state, level - 1);
            }
            cir.setReturnValue(true);
            return;
        }
        if (item == Items.POTIONITEM && PotionUtils.getPotionFromItem(held) == PotionTypes.WATER) {
            if (!isLava && level < 3 && !worldIn.isRemote) {
                if (!playerIn.capabilities.isCreativeMode) {
                    playerIn.addStat(StatList.CAULDRON_USED);
                    playerIn.setHeldItem(hand, new ItemStack(Items.GLASS_BOTTLE));
                    if (playerIn instanceof EntityPlayerMP) ((EntityPlayerMP)playerIn).sendContainerToPlayer(playerIn.inventoryContainer);
                }
                worldIn.playSound(null, pos, SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
                cncSetWaterLevel(worldIn, pos, state, level + 1);
            }
            cir.setReturnValue(true);
            return;
        }
        if (!isLava && level > 0) {
            if (item instanceof ItemArmor) {
                ItemArmor armor = (ItemArmor) item;
                if (armor.getArmorMaterial() == ItemArmor.ArmorMaterial.LEATHER && armor.hasColor(held) && !worldIn.isRemote) {
                    armor.removeColor(held);
                    cncSetWaterLevel(worldIn, pos, state, level - 1);
                    playerIn.addStat(StatList.ARMOR_CLEANED);
                    cir.setReturnValue(true);
                    return;
                }
            }
            if (item instanceof ItemBanner && TileEntityBanner.getPatterns(held) > 0 && !worldIn.isRemote) {
                ItemStack clean = held.copy();
                clean.setCount(1);
                TileEntityBanner.removeBannerData(clean);
                playerIn.addStat(StatList.BANNER_CLEANED);
                if (!playerIn.capabilities.isCreativeMode) {
                    held.shrink(1);
                    cncSetWaterLevel(worldIn, pos, state, level - 1);
                }
                if (held.isEmpty()) playerIn.setHeldItem(hand, clean);
                else if (!playerIn.inventory.addItemStackToInventory(clean)) playerIn.dropItem(clean, false);
                else if (playerIn instanceof EntityPlayerMP) ((EntityPlayerMP)playerIn).sendContainerToPlayer(playerIn.inventoryContainer);
                cir.setReturnValue(true);
                return;
            }
        }
        cir.setReturnValue(false);
    }

    @Inject(method = "onEntityCollidedWithBlock", at = @At("HEAD"), cancellable = true)
    private void injectOnEntityCollidedWithBlock(World worldIn, BlockPos pos,
            IBlockState state, Entity entityIn, CallbackInfo ci) {
        int level = (Integer) state.getValue(LEVEL);
        boolean isLava = (Boolean) state.getValue(IS_LAVA);
        float liquidTop = (float) pos.getY() + (6.0F + (float)(3 * level)) / 16.0F;
        if (!worldIn.isRemote && level > 0 && entityIn.getEntityBoundingBox().minY <= (double) liquidTop) {
            if (!isLava && entityIn.isBurning()) {
                entityIn.extinguish();
                cncSetWaterLevel(worldIn, pos, state, level - 1);
            } else if (isLava) {
                entityIn.setFire(5);
            }
        }
        ci.cancel();
    }

    // BlockCauldron does not override getLightValue (inherited from Block), so we can't @Inject
    // into it — declare a plain override and let Mixin merge it into the target as a real override.
    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return ((Boolean) state.getValue(IS_LAVA)) ? 15 : super.getLightValue(state, world, pos);
    }

    // Likewise, BlockCauldron does not override randomTick — merge an override rather than @Inject.
    // setTickRandomly(true) is enabled in the <init> inject so this actually fires.
    @Override
    public void randomTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
        if (rand.nextInt(2) == 1) {
            int level = (Integer) state.getValue(LEVEL);
            boolean isLava = (Boolean) state.getValue(IS_LAVA);
            if (cncIsLiquidAbove(worldIn, pos, Material.LAVA)) {
                if (level < 3) cncSetLavaLevel(worldIn, pos, state, level + 1);
            } else if (cncIsLiquidAbove(worldIn, pos, Material.WATER)) {
                if (!isLava && level < 3) cncSetWaterLevel(worldIn, pos, state, level + 1);
            }
        }
    }

    @Inject(method = "fillWithRain", at = @At("HEAD"), cancellable = true)
    private void injectFillWithRain(World worldIn, BlockPos pos, CallbackInfo ci) {
        if (worldIn.rand.nextInt(20) == 1) {
            float f = worldIn.getBiome(pos).getTemperature(pos);
            if (worldIn.getBiomeProvider().getTemperatureAtHeight(f, pos.getY()) >= 0.15F) {
                IBlockState state = worldIn.getBlockState(pos);
                boolean isLava = (Boolean) state.getValue(IS_LAVA);
                int level = (Integer) state.getValue(LEVEL);
                if (!isLava && level < 3) {
                    worldIn.setBlockState(pos, state.withProperty(LEVEL, level + 1), 2);
                }
            }
        }
        ci.cancel();
    }

    @Inject(method = "getComparatorInputOverride", at = @At("HEAD"), cancellable = true)
    private void injectGetComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos,
            CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue((Integer) blockState.getValue(LEVEL));
    }

    @Inject(method = "getItemDropped", at = @At("HEAD"), cancellable = true)
    private void injectGetItemDropped(IBlockState state, Random rand, int fortune,
            CallbackInfoReturnable<Item> cir) {
        cir.setReturnValue(Items.CAULDRON);
    }

    @Inject(method = "getItem", at = @At("HEAD"), cancellable = true)
    private void injectGetItem(World worldIn, BlockPos pos, IBlockState state,
            CallbackInfoReturnable<ItemStack> cir) {
        cir.setReturnValue(new ItemStack(Items.CAULDRON));
    }
}

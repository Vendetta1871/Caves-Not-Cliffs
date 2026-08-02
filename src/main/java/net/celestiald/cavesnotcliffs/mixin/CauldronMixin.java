package net.celestiald.cavesnotcliffs.mixin;

import net.celestiald.cavesnotcliffs.block.BlockPointedDripstone;
import net.celestiald.cavesnotcliffs.block.BlockPowderSnow;
import net.celestiald.cavesnotcliffs.content.DripstoneSoundEvents;
import net.celestiald.cavesnotcliffs.dripstone.CauldronMechanics;
import net.celestiald.cavesnotcliffs.dripstone.CauldronMechanics.Content;
import net.celestiald.cavesnotcliffs.dripstone.CauldronMechanics.DripFluid;
import net.celestiald.cavesnotcliffs.dripstone.CauldronMechanics.Interaction;
import net.celestiald.cavesnotcliffs.dripstone.CauldronMechanics.State;
import net.celestiald.cavesnotcliffs.dripstone.VanillaCauldronMeta;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBanner;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtils;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntityBanner;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
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
 * Stores the 1.18.2 cauldron contents (empty, layered water, full lava, layered powder snow) on
 * the vanilla {@link BlockCauldron} itself, so a placed cauldron keeps the
 * {@code minecraft:cauldron} identity and third-party multiblock checks (e.g. Immersive
 * Engineering's Arc Furnace) keep working. Replaces the hidden v2 storage blocks, which are
 * migrated back to vanilla states on chunk load.
 *
 * Uses @Inject (not @Overwrite): @Overwrite renames methods to SRG names in compiled bytecode,
 * which never matches the MCP-named dev runtime. updateTick and getLightValue are merged
 * @Overrides because BlockCauldron does not declare them (inherited from Block), so there is no
 * method to inject into.
 *
 * Metadata encoding lives in {@link VanillaCauldronMeta}: 0-3 empty/water, 7 lava, 8-10 powder
 * snow. The properties stay private to this mixin — referencing a mixin class from regular code
 * would load a second copy of the static property instances (Mixin moves them into the target).
 */
@Mixin(BlockCauldron.class)
public abstract class CauldronMixin extends Block {
    @Shadow @Final
    public static PropertyInteger LEVEL;

    private static final PropertyBool IS_LAVA = PropertyBool.create("is_lava");
    private static final PropertyBool IS_POWDER_SNOW = PropertyBool.create("is_powder_snow");

    protected CauldronMixin(Material material) {
        super(material);
    }

    // BlockStateContainer picks its base state by hash-ordered property values, so the inherited
    // default can carry is_lava/is_powder_snow = true. Pin a clean default for every vanilla code
    // path that derives states from getDefaultState(). Constructors of the legacy hidden-storage
    // subclasses (different state container) run this inject too — leave them alone.
    @Inject(method = "<init>", at = @At("RETURN"))
    private void cncSanitizeDefaultState(CallbackInfo ci) {
        if (((Block) (Object) this).getClass() != BlockCauldron.class) {
            return;
        }
        setDefaultState(getDefaultState()
                .withProperty(IS_LAVA, false)
                .withProperty(IS_POWDER_SNOW, false));
    }

    // ── State container and metadata ─────────────────────────────────────────

    @Inject(method = "createBlockState", at = @At("HEAD"), cancellable = true)
    private void cncCreateBlockState(CallbackInfoReturnable<BlockStateContainer> cir) {
        cir.setReturnValue(new BlockStateContainer((Block) (Object) this,
                new IProperty<?>[]{LEVEL, IS_LAVA, IS_POWDER_SNOW}));
    }

    @Inject(method = "getStateFromMeta", at = @At("HEAD"), cancellable = true)
    private void cncGetStateFromMeta(int meta, CallbackInfoReturnable<IBlockState> cir) {
        cir.setReturnValue(cncBlockState(VanillaCauldronMeta.fromMeta(meta & 15)));
    }

    @Inject(method = "getMetaFromState", at = @At("HEAD"), cancellable = true)
    private void cncGetMetaFromState(IBlockState state, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(VanillaCauldronMeta.toMeta(cncMechanicsState(state)));
    }

    // ── Content helpers ──────────────────────────────────────────────────────

    private State cncMechanicsState(IBlockState state) {
        if (state.getValue(IS_LAVA)) {
            return CauldronMechanics.lava();
        }
        int level = state.getValue(LEVEL);
        if (state.getValue(IS_POWDER_SNOW)) {
            return CauldronMechanics.powderSnow(Math.max(1, level));
        }
        return level == 0 ? CauldronMechanics.empty() : CauldronMechanics.water(level);
    }

    private IBlockState cncBlockState(State contents) {
        // Never inherit is_lava/is_powder_snow from the default state: set every property
        // explicitly, exactly like the pre-v2 mixin did.
        IBlockState base = getDefaultState()
                .withProperty(IS_LAVA, false)
                .withProperty(IS_POWDER_SNOW, false);
        switch (contents.content) {
            case LAVA:
                return base.withProperty(IS_LAVA, true)
                        .withProperty(LEVEL, CauldronMechanics.MAX_LEVEL);
            case POWDER_SNOW:
                return base.withProperty(IS_POWDER_SNOW, true)
                        .withProperty(LEVEL, contents.level);
            case WATER:
                return base.withProperty(LEVEL, contents.level);
            default:
                return base.withProperty(LEVEL, 0);
        }
    }

    /** setBlockAndUpdate equivalent: flags 1 (neighbors/observers) + 2 (client sync). */
    private void cncSetContents(World world, BlockPos pos, State contents) {
        world.setBlockState(pos, cncBlockState(contents), 3);
        world.updateComparatorOutputLevel(pos, (Block) (Object) this);
    }

    // ── Player interactions ──────────────────────────────────────────────────

    @Inject(method = "onBlockActivated", at = @At("HEAD"), cancellable = true)
    private void cncOnBlockActivated(World world, BlockPos pos, IBlockState state,
            EntityPlayer player, EnumHand hand, EnumFacing facing,
            float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
        ItemStack held = player.getHeldItem(hand);
        if (held.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }

        State contents = cncMechanicsState(state);
        Item item = held.getItem();

        if (item == Items.WATER_BUCKET) {
            if (!world.isRemote) {
                cncConsumeFilledContainer(player, hand, held);
                cncSetContents(world, pos, CauldronMechanics.interact(
                        contents, Interaction.FILL_WATER));
                cncUsed(player, item, StatList.CAULDRON_FILLED);
                cncPlay(world, pos, SoundEvents.ITEM_BUCKET_EMPTY);
            }
            cir.setReturnValue(true);
            return;
        }

        if (item == Items.LAVA_BUCKET) {
            if (!world.isRemote) {
                cncConsumeFilledContainer(player, hand, held);
                cncSetContents(world, pos, CauldronMechanics.interact(
                        contents, Interaction.FILL_LAVA));
                cncUsed(player, item, StatList.CAULDRON_FILLED);
                cncPlay(world, pos, SoundEvents.ITEM_BUCKET_EMPTY_LAVA);
            }
            cir.setReturnValue(true);
            return;
        }

        if (BlockPowderSnow.bucket != null && item == BlockPowderSnow.bucket) {
            if (!world.isRemote) {
                cncConsumeFilledContainer(player, hand, held);
                cncSetContents(world, pos, CauldronMechanics.interact(
                        contents, Interaction.FILL_POWDER_SNOW));
                cncUsed(player, item, StatList.CAULDRON_FILLED);
                cncPlay(world, pos, BlockPowderSnow.BUCKET_EMPTY_SOUND);
            }
            cir.setReturnValue(true);
            return;
        }

        if (item == Items.BUCKET) {
            if (!CauldronMechanics.canInteract(contents, Interaction.TAKE_BUCKET)) {
                cir.setReturnValue(false);
                return;
            }
            if (!world.isRemote) {
                Item filled = contents.content == Content.LAVA ? Items.LAVA_BUCKET
                        : contents.content == Content.POWDER_SNOW ? BlockPowderSnow.bucket
                        : Items.WATER_BUCKET;
                cncGiveFilledResult(player, hand, held, new ItemStack(filled));
                cncSetContents(world, pos, CauldronMechanics.interact(
                        contents, Interaction.TAKE_BUCKET));
                cncUsed(player, item, StatList.CAULDRON_USED);
                cncPlay(world, pos, contents.content == Content.LAVA
                        ? SoundEvents.ITEM_BUCKET_FILL_LAVA
                        : contents.content == Content.POWDER_SNOW
                        ? BlockPowderSnow.BUCKET_FILL_SOUND : SoundEvents.ITEM_BUCKET_FILL);
            }
            cir.setReturnValue(true);
            return;
        }

        if (item == Items.GLASS_BOTTLE) {
            if (!CauldronMechanics.canInteract(contents, Interaction.TAKE_BOTTLE)) {
                cir.setReturnValue(false);
                return;
            }
            if (!world.isRemote) {
                ItemStack waterBottle = PotionUtils.addPotionToItemStack(
                        new ItemStack(Items.POTIONITEM), PotionTypes.WATER);
                cncGiveFilledResult(player, hand, held, waterBottle);
                cncSetContents(world, pos, CauldronMechanics.interact(
                        contents, Interaction.TAKE_BOTTLE));
                cncUsed(player, item, StatList.CAULDRON_USED);
                cncPlay(world, pos, SoundEvents.ITEM_BOTTLE_FILL);
            }
            cir.setReturnValue(true);
            return;
        }

        if (item == Items.POTIONITEM
                && PotionUtils.getPotionFromItem(held) == PotionTypes.WATER) {
            if (!CauldronMechanics.canInteract(contents, Interaction.POUR_WATER_BOTTLE)) {
                cir.setReturnValue(false);
                return;
            }
            if (!world.isRemote) {
                cncGiveFilledResult(player, hand, held, new ItemStack(Items.GLASS_BOTTLE));
                cncSetContents(world, pos, CauldronMechanics.interact(
                        contents, Interaction.POUR_WATER_BOTTLE));
                cncUsed(player, item, StatList.CAULDRON_USED);
                cncPlay(world, pos, SoundEvents.ITEM_BOTTLE_EMPTY);
            }
            cir.setReturnValue(true);
            return;
        }

        if (contents.content == Content.WATER && item instanceof ItemArmor) {
            ItemArmor armor = (ItemArmor) item;
            if (armor.getArmorMaterial() == ItemArmor.ArmorMaterial.LEATHER
                    && armor.hasColor(held)) {
                if (!world.isRemote) {
                    armor.removeColor(held);
                    cncSetContents(world, pos, CauldronMechanics.interact(
                            contents, Interaction.CLEAN));
                    player.addStat(StatList.ARMOR_CLEANED);
                }
                cir.setReturnValue(true);
                return;
            }
        }

        if (contents.content == Content.WATER && item instanceof ItemBanner
                && TileEntityBanner.getPatterns(held) > 0) {
            if (!world.isRemote) {
                ItemStack clean = held.copy();
                clean.setCount(1);
                TileEntityBanner.removeBannerData(clean);
                if (!player.capabilities.isCreativeMode) {
                    held.shrink(1);
                }
                cncDeliverResult(player, hand, held, clean);
                cncSetContents(world, pos, CauldronMechanics.interact(
                        contents, Interaction.CLEAN));
                player.addStat(StatList.BANNER_CLEANED);
            }
            cir.setReturnValue(true);
            return;
        }

        Block heldBlock = Block.getBlockFromItem(item);
        if (contents.content == Content.WATER && heldBlock instanceof BlockShulkerBox) {
            if (!world.isRemote) {
                ItemStack clean = new ItemStack(Blocks.PURPLE_SHULKER_BOX);
                if (held.hasTagCompound()) {
                    clean.setTagCompound(held.getTagCompound().copy());
                }
                player.setHeldItem(hand, clean);
                cncSetContents(world, pos, CauldronMechanics.interact(
                        contents, Interaction.CLEAN));
                if (player instanceof EntityPlayerMP) {
                    ((EntityPlayerMP) player)
                            .sendContainerToPlayer(player.inventoryContainer);
                }
            }
            cir.setReturnValue(true);
            return;
        }
        cir.setReturnValue(false);
    }

    // ── Entity contact ───────────────────────────────────────────────────────

    @Inject(method = "onEntityCollidedWithBlock", at = @At("HEAD"), cancellable = true)
    private void cncOnEntityCollidedWithBlock(World world, BlockPos pos,
            IBlockState state, Entity entity, CallbackInfo ci) {
        State contents = cncMechanicsState(state);
        if (!world.isRemote && contents.content != Content.EMPTY
                && cncInsideContents(contents, pos, entity)) {
            if (contents.content == Content.LAVA) {
                entity.setFire(15);
                entity.attackEntityFrom(DamageSource.LAVA, 4.0F);
            } else if (entity.isBurning() && (!(entity instanceof EntityPlayer)
                    || world.isBlockModifiable((EntityPlayer) entity, pos))) {
                entity.extinguish();
                // 1.18 melts one powder-snow layer into water, then consumes that water layer
                // while extinguishing; plain water just loses one layer.
                cncSetContents(world, pos, contents.content == Content.POWDER_SNOW
                        ? CauldronMechanics.extinguishInPowderSnow(contents)
                        : CauldronMechanics.interact(contents, Interaction.CLEAN));
            }
        }
        ci.cancel();
    }

    private boolean cncInsideContents(State contents, BlockPos pos, Entity entity) {
        return entity.posY < pos.getY() + CauldronMechanics.contentHeight(contents)
                && entity.getEntityBoundingBox().maxY > pos.getY() + 0.25D;
    }

    // ── Stalactite drips ─────────────────────────────────────────────────────

    // BlockCauldron does not override updateTick (inherited from Block), so we can't @Inject
    // into it — declare a plain override and let Mixin merge it into the target as a real
    // override. BlockPointedDripstone.maybeFillCauldron drives this through scheduleUpdate.
    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (world.isRemote || world.getBlockState(pos).getBlock() != (Block) (Object) this) {
            return;
        }
        BlockPos tip = BlockPointedDripstone.findStalactiteTipAboveCauldron(world, pos);
        if (tip == null) {
            return;
        }
        DripFluid fluid = BlockPointedDripstone.cauldronFillFluid(world, tip);
        if (fluid == null) {
            return;
        }
        State contents = cncMechanicsState(state);
        if (!CauldronMechanics.canReceiveDrip(contents, fluid)) {
            return;
        }
        cncSetContents(world, pos, CauldronMechanics.receiveDrip(contents, fluid));
        world.playSound(null, pos, fluid == DripFluid.LAVA
                        ? DripstoneSoundEvents.DRIP_LAVA_CAULDRON
                        : DripstoneSoundEvents.DRIP_WATER_CAULDRON,
                SoundCategory.BLOCKS, 1.0F, 1.0F);
    }

    // ── Precipitation ────────────────────────────────────────────────────────

    @Inject(method = "fillWithRain", at = @At("HEAD"), cancellable = true)
    private void cncFillWithRain(World world, BlockPos pos, CallbackInfo ci) {
        IBlockState current = world.getBlockState(pos);
        if (current.getBlock() == (Block) (Object) this) {
            State contents = cncMechanicsState(current);
            if (contents.content != Content.LAVA) {
                float temperature = world.getBiome(pos).getTemperature(pos);
                boolean snow = world.getBiomeProvider()
                        .getTemperatureAtHeight(temperature, pos.getY()) < 0.15F;
                State next = CauldronMechanics.precipitation(
                        contents, snow, world.rand.nextFloat());
                if (!next.equals(contents)) {
                    cncSetContents(world, pos, next);
                }
            }
        }
        ci.cancel();
    }

    // ── Light ────────────────────────────────────────────────────────────────

    // Likewise, BlockCauldron does not override getLightValue — merge an override.
    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(IS_LAVA) ? 15 : super.getLightValue(state, world, pos);
    }

    // ── Item-stack helpers (ported from the v2 hidden blocks) ────────────────

    private static void cncConsumeFilledContainer(EntityPlayer player, EnumHand hand,
            ItemStack filled) {
        cncGiveFilledResult(player, hand, filled, new ItemStack(Items.BUCKET));
    }

    private static void cncGiveFilledResult(EntityPlayer player, EnumHand hand,
            ItemStack original, ItemStack result) {
        if (player.capabilities.isCreativeMode) {
            if (!player.inventory.hasItemStack(result)) {
                player.inventory.addItemStackToInventory(result);
            }
            return;
        }
        original.shrink(1);
        cncDeliverResult(player, hand, original, result);
    }

    private static void cncDeliverResult(EntityPlayer player, EnumHand hand,
            ItemStack original, ItemStack result) {
        if (original.isEmpty()) {
            player.setHeldItem(hand, result);
        } else if (!player.inventory.addItemStackToInventory(result)) {
            player.dropItem(result, false);
        }
        if (player instanceof EntityPlayerMP) {
            ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
        }
    }

    private static void cncUsed(EntityPlayer player, Item item, StatBase cauldronStat) {
        player.addStat(cauldronStat);
        StatBase itemUse = StatList.getObjectUseStats(item);
        if (itemUse != null) {
            player.addStat(itemUse);
        }
    }

    private static void cncPlay(World world, BlockPos pos, SoundEvent sound) {
        world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }
}

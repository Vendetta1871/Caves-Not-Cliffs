package net.celestiald.cavesnotcliffs.entity;

import com.google.common.base.Predicate;
import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.content.BeeMechanics;
import net.celestiald.cavesnotcliffs.content.BeeSoundEvents;
import net.celestiald.cavesnotcliffs.content.BeeFlowerPredicate;
import net.celestiald.cavesnotcliffs.content.BeeCropGrowth;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.celestiald.cavesnotcliffs.tile.TileEntityBeehive;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockStem;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIFollowParent;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.EntityFlyHelper;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateFlying;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Java 1.12 runtime adaptation of Java 1.18.2's complete bee state contract. */
@ElementsCavesNotCliffs.ModElement.Tag
public final class EntityBee extends ElementsCavesNotCliffs.ModElement {
    public static final int NETWORK_ID = 2;
    private static final int EGG_PRIMARY = 15582019;
    private static final int EGG_SECONDARY = 4400155;

    public EntityBee(ElementsCavesNotCliffs elements) {
        super(elements, 68);
    }

    @Override
    public void initElements() {
        BeeSoundEvents.registerAll();
        EntityRegistry.registerModEntity(CncRegistryIds.BEE,
                EntityCustom.class, "bee", NETWORK_ID, CavesNotCliffs.instance,
                80, 3, true, EGG_PRIMARY, EGG_SECONDARY);
    }

    public static final class EntityCustom extends EntityAnimal {
        private static final DataParameter<Byte> FLAGS =
                EntityDataManager.createKey(EntityCustom.class, DataSerializers.BYTE);
        private static final DataParameter<Integer> ANGER_TIME =
                EntityDataManager.createKey(EntityCustom.class, DataSerializers.VARINT);
        private static final int FLAG_ROLL = 2;
        private static final int FLAG_HAS_STUNG = 4;
        private static final int FLAG_HAS_NECTAR = 8;

        private UUID persistentAngerTarget;
        private int timeSinceSting;
        private int ticksWithoutNectarSinceExitingHive;
        private int stayOutOfHiveCountdown;
        private int cropsGrownSincePollination;
        private int remainingCooldownBeforeLocatingNewHive;
        private int remainingCooldownBeforeLocatingNewFlower;
        private int underWaterTicks;
        private float rollAmount;
        private float previousRollAmount;
        private BlockPos savedFlowerPos;
        private BlockPos hivePos;
        private BeePollinateGoal pollinateGoal;
        private final List<BlockPos> blacklistedHives = new ArrayList<>();

        public EntityCustom(World world) {
            super(world);
            setSize(0.7F, 0.6F);
            moveHelper = new EntityFlyHelper(this);
            setPathPriority(net.minecraft.pathfinding.PathNodeType.DANGER_FIRE, -1.0F);
            // valueOf avoids a production MCP-field reference rejected by verifyReleaseJar.
            setPathPriority(net.minecraft.pathfinding.PathNodeType.valueOf("WATER"), -1.0F);
            setPathPriority(net.minecraft.pathfinding.PathNodeType.FENCE, -1.0F);
            remainingCooldownBeforeLocatingNewFlower = 20 + rand.nextInt(41);
        }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataManager.register(FLAGS, (byte) 0);
            dataManager.register(ANGER_TIME, 0);
        }

        @Override
        protected void initEntityAI() {
            tasks.addTask(0, new BeeAttackGoal(this));
            tasks.addTask(1, new BeeEnterHiveGoal(this));
            tasks.addTask(2, new EntityAIMate(this, 1.0D));
            tasks.addTask(3, new BeeTemptGoal(this));
            pollinateGoal = new BeePollinateGoal(this);
            tasks.addTask(4, pollinateGoal);
            tasks.addTask(5, new EntityAIFollowParent(this, 1.25D));
            tasks.addTask(5, new BeeLocateHiveGoal(this));
            tasks.addTask(5, new BeeGoToHiveGoal(this));
            tasks.addTask(6, new BeeGoToKnownFlowerGoal(this));
            tasks.addTask(7, new BeeGrowCropGoal(this));
            tasks.addTask(8, new BeeWanderGoal(this));
            tasks.addTask(9, new EntityAISwimming(this));
            tasks.addTask(10, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
            tasks.addTask(11, new EntityAILookIdle(this));
            targetTasks.addTask(1, new BeeHurtByOtherGoal(this));
            targetTasks.addTask(2, new BeeAngryPlayerGoal(this));
        }

        @Override
        protected PathNavigate createNavigator(World world) {
            PathNavigateFlying navigator = new PathNavigateFlying(this, world);
            navigator.setCanOpenDoors(false);
            navigator.setCanFloat(false);
            navigator.setCanEnterDoors(true);
            return navigator;
        }

        @Override
        protected void applyEntityAttributes() {
            super.applyEntityAttributes();
            getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(10.0D);
            getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.3D);
            getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE)
                    .setBaseValue(2.0D);
            getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(48.0D);
        }

        @Override
        public void onLivingUpdate() {
            super.onLivingUpdate();
            previousRollAmount = rollAmount;
            rollAmount = isRolling() ? Math.min(1.0F, rollAmount + 0.2F)
                    : Math.max(0.0F, rollAmount - 0.24F);
            if (hasNectar() && cropsGrownSincePollination
                    < BeeMechanics.MAX_CROPS_GROWN && rand.nextFloat() < 0.05F) {
                for (int index = 0; index < rand.nextInt(2) + 1; index++) {
                    world.spawnParticle(EnumParticleTypes.DRIP_LAVA,
                            posX + (rand.nextDouble() * 0.6D - 0.3D),
                            posY + height * 0.5D,
                            posZ + (rand.nextDouble() * 0.6D - 0.3D),
                            0.0D, 0.0D, 0.0D);
                }
            }
            if (world.isRemote) {
                return;
            }
            underWaterTicks = isInWater() ? underWaterTicks + 1 : 0;
            if (underWaterTicks > 20) {
                attackEntityFrom(DamageSource.DROWN, 1.0F);
            }
            if (hasStung()) {
                timeSinceSting++;
                if (BeeMechanics.diesAfterSting(timeSinceSting, rand)) {
                    attackEntityFrom(DamageSource.GENERIC, getHealth());
                }
            }
            if (!hasNectar()) {
                ticksWithoutNectarSinceExitingHive++;
            }
            if (stayOutOfHiveCountdown > 0) {
                stayOutOfHiveCountdown--;
            }
            if (remainingCooldownBeforeLocatingNewHive > 0) {
                remainingCooldownBeforeLocatingNewHive--;
            }
            if (remainingCooldownBeforeLocatingNewFlower > 0) {
                remainingCooldownBeforeLocatingNewFlower--;
            }
            int anger = getRemainingAngerTime();
            if (anger > 0) {
                setRemainingAngerTime(anger - 1);
            } else if (getAttackTarget() != null) {
                setAttackTarget(null);
                persistentAngerTarget = null;
            }
            if (ticksExisted % 20 == 0 && !isHiveValid()) {
                hivePos = null;
            }
            boolean rolling = isAngry() && !hasStung() && getAttackTarget() != null
                    && getDistanceSq(getAttackTarget()) < 4.0D;
            setBeeFlag(FLAG_ROLL, rolling);
            if (ticksExisted % 80 == 0) {
                playSound(isAngry() ? BeeSoundEvents.BEE_LOOP_AGGRESSIVE
                        : BeeSoundEvents.BEE_LOOP, 0.6F, 1.0F);
            }
        }

        @Override
        public boolean attackEntityFrom(DamageSource source, float amount) {
            if (!world.isRemote && pollinateGoal != null) {
                pollinateGoal.stopPollinating();
            }
            boolean hurt = super.attackEntityFrom(source, amount);
            if (hurt && !world.isRemote && source.getTrueSource() instanceof EntityLivingBase) {
                EntityLivingBase attacker = (EntityLivingBase) source.getTrueSource();
                becomeAngryAt(attacker);
                for (EntityCustom bee : world.getEntitiesWithinAABB(EntityCustom.class,
                        getEntityBoundingBox().grow(16.0D))) {
                    if (bee != this && bee.getEntitySenses().canSee(attacker)) {
                        bee.becomeAngryAt(attacker);
                    }
                }
            }
            return hurt;
        }

        public void becomeAngryAt(EntityLivingBase target) {
            if (hasStung()) {
                return;
            }
            persistentAngerTarget = target.getUniqueID();
            setRemainingAngerTime(BeeMechanics.nextAngerTime(rand));
            setAttackTarget(target);
        }

        @Override
        public boolean attackEntityAsMob(Entity target) {
            if (hasStung()) {
                return false;
            }
            boolean hit = target.attackEntityFrom(DamageSource.causeMobDamage(this),
                    (float) getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE)
                            .getAttributeValue());
            if (hit) {
                applyEnchantments(this, target);
                if (target instanceof EntityLivingBase) {
                    int poisonTicks = BeeMechanics.poisonDurationTicks(world.getDifficulty());
                    if (poisonTicks > 0) {
                        ((EntityLivingBase) target).addPotionEffect(
                                new PotionEffect(MobEffects.POISON, poisonTicks, 0));
                    }
                }
                setHasStung(true);
                setRemainingAngerTime(0);
                persistentAngerTarget = null;
                setAttackTarget(null);
                playSound(BeeSoundEvents.BEE_STING, 1.0F, 1.0F);
            }
            return hit;
        }

        @Override
        public EntityAgeable createChild(EntityAgeable mate) {
            return new EntityCustom(world);
        }

        @Override
        public boolean isBreedingItem(ItemStack stack) {
            return isFlower(stack);
        }

        public static boolean isFlower(ItemStack stack) {
            return BeeFlowerPredicate.isFlowerItem(stack);
        }

        public static boolean isFlowerAt(net.minecraft.world.IBlockAccess world,
                BlockPos pos) {
            return BeeFlowerPredicate.isFlower(world, pos);
        }

        public static boolean isPollinationTargetAt(
                net.minecraft.world.IBlockAccess world, BlockPos pos) {
            return BeeFlowerPredicate.isPollinationTarget(world, pos);
        }

        @Override
        public void writeEntityToNBT(NBTTagCompound tag) {
            super.writeEntityToNBT(tag);
            if (hivePos != null) {
                tag.setTag("HivePos", writePos(hivePos));
            }
            if (savedFlowerPos != null) {
                tag.setTag("FlowerPos", writePos(savedFlowerPos));
            }
            tag.setBoolean("HasNectar", hasNectar());
            tag.setBoolean("HasStung", hasStung());
            tag.setInteger("TicksSincePollination", ticksWithoutNectarSinceExitingHive);
            tag.setInteger("CannotEnterHiveTicks", stayOutOfHiveCountdown);
            tag.setInteger("CropsGrownSincePollination", cropsGrownSincePollination);
            tag.setInteger("AngerTime", getRemainingAngerTime());
            if (persistentAngerTarget != null) {
                tag.setUniqueId("AngryAt", persistentAngerTarget);
            }
        }

        @Override
        public void readEntityFromNBT(NBTTagCompound tag) {
            hivePos = tag.hasKey("HivePos", 10) ? readPos(tag.getCompoundTag("HivePos")) : null;
            savedFlowerPos = tag.hasKey("FlowerPos", 10)
                    ? readPos(tag.getCompoundTag("FlowerPos")) : null;
            super.readEntityFromNBT(tag);
            setHasNectar(tag.getBoolean("HasNectar"));
            setHasStung(tag.getBoolean("HasStung"));
            ticksWithoutNectarSinceExitingHive =
                    tag.getInteger("TicksSincePollination");
            stayOutOfHiveCountdown = tag.getInteger("CannotEnterHiveTicks");
            cropsGrownSincePollination =
                    tag.getInteger("CropsGrownSincePollination");
            setRemainingAngerTime(Math.max(0, tag.getInteger("AngerTime")));
            persistentAngerTarget = tag.hasUniqueId("AngryAt")
                    ? tag.getUniqueId("AngryAt") : null;
        }

        public boolean wantsToEnterHive() {
            return BeeMechanics.wantsToEnterHive(stayOutOfHiveCountdown,
                    pollinateGoal != null && pollinateGoal.isPollinating(), hasStung(),
                    getAttackTarget() != null, ticksWithoutNectarSinceExitingHive,
                    world.isRaining(), !world.isDaytime(), hasNectar(), isHiveNearFire());
        }

        private boolean isHiveNearFire() {
            if (hivePos == null) {
                return false;
            }
            net.minecraft.tileentity.TileEntity tile = world.getTileEntity(hivePos);
            return tile instanceof TileEntityBeehive
                    && ((TileEntityBeehive) tile).isFireNearby();
        }

        public boolean isHiveValid() {
            return hivePos != null && world.getTileEntity(hivePos) instanceof TileEntityBeehive;
        }

        public boolean hasNectar() {
            return getBeeFlag(FLAG_HAS_NECTAR);
        }

        public void setHasNectar(boolean hasNectar) {
            if (hasNectar) {
                ticksWithoutNectarSinceExitingHive = 0;
            }
            setBeeFlag(FLAG_HAS_NECTAR, hasNectar);
        }

        public void dropOffNectar() {
            setHasNectar(false);
            cropsGrownSincePollination = 0;
        }

        public boolean hasStung() {
            return getBeeFlag(FLAG_HAS_STUNG);
        }

        public void setHasStung(boolean hasStung) {
            setBeeFlag(FLAG_HAS_STUNG, hasStung);
        }

        public boolean isRolling() {
            return getBeeFlag(FLAG_ROLL);
        }

        public float getRollAmount(float partialTicks) {
            return previousRollAmount
                    + (rollAmount - previousRollAmount) * partialTicks;
        }

        public boolean isAngry() {
            return getRemainingAngerTime() > 0;
        }

        public int getRemainingAngerTime() {
            return dataManager.get(ANGER_TIME);
        }

        public void setRemainingAngerTime(int ticks) {
            dataManager.set(ANGER_TIME, Math.max(0, ticks));
        }

        public BlockPos getSavedFlowerPos() {
            return savedFlowerPos;
        }

        public void setSavedFlowerPos(@Nullable BlockPos pos) {
            savedFlowerPos = pos;
        }

        public BlockPos getHivePos() {
            return hivePos;
        }

        public void setHivePos(@Nullable BlockPos pos) {
            hivePos = pos;
        }

        public void setStayOutOfHiveCountdown(int ticks) {
            stayOutOfHiveCountdown = Math.max(0, ticks);
        }

        public int getCropsGrownSincePollination() {
            return cropsGrownSincePollination;
        }

        public void incrementCropsGrownSincePollination() {
            cropsGrownSincePollination++;
        }

        public int getTicksWithoutNectarSinceExitingHive() {
            return ticksWithoutNectarSinceExitingHive;
        }

        boolean isHiveBlacklisted(BlockPos pos) {
            return blacklistedHives.contains(pos);
        }

        void blacklistHive(BlockPos pos) {
            if (pos == null || blacklistedHives.contains(pos)) {
                return;
            }
            blacklistedHives.add(pos.toImmutable());
            while (blacklistedHives.size() > 3) {
                blacklistedHives.remove(0);
            }
        }

        void clearHiveBlacklist() {
            blacklistedHives.clear();
        }

        private void setBeeFlag(int bit, boolean value) {
            byte flags = dataManager.get(FLAGS);
            dataManager.set(FLAGS, value ? (byte) (flags | bit) : (byte) (flags & ~bit));
        }

        private boolean getBeeFlag(int bit) {
            return (dataManager.get(FLAGS) & bit) != 0;
        }

        private static NBTTagCompound writePos(BlockPos pos) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("X", pos.getX());
            tag.setInteger("Y", pos.getY());
            tag.setInteger("Z", pos.getZ());
            return tag;
        }

        private static BlockPos readPos(NBTTagCompound tag) {
            return new BlockPos(tag.getInteger("X"), tag.getInteger("Y"),
                    tag.getInteger("Z"));
        }

        @Override
        public void fall(float distance, float damageMultiplier) {
        }

        @Override
        protected void updateFallState(double y, boolean onGroundIn,
                IBlockState state, BlockPos pos) {
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return null;
        }

        @Override
        protected SoundEvent getHurtSound(DamageSource source) {
            return BeeSoundEvents.BEE_HURT;
        }

        @Override
        protected SoundEvent getDeathSound() {
            return BeeSoundEvents.BEE_DEATH;
        }

        @Override
        protected float getSoundVolume() {
            return 0.4F;
        }

        @Override
        public EnumCreatureAttribute getCreatureAttribute() {
            return EnumCreatureAttribute.ARTHROPOD;
        }

        @Override
        public float getEyeHeight() {
            return height * 0.5F;
        }

        @Override
        public float getBlockPathWeight(BlockPos pos) {
            return world.isAirBlock(pos) ? 10.0F : 0.0F;
        }

        @Override
        protected void playStepSound(BlockPos pos, Block block) {
        }

        public boolean isFlying() {
            return !onGround;
        }

        public java.util.Random randomSource() {
            return rand;
        }

        @Override
        public void travel(float strafe, float vertical, float forward) {
            if (isServerWorld() && isInWater()) {
                moveRelative(strafe, vertical, forward, 0.02F);
                move(MoverType.SELF, motionX, motionY, motionZ);
                motionX *= 0.8D;
                motionY *= 0.8D;
                motionZ *= 0.8D;
            } else {
                super.travel(strafe, vertical, forward);
            }
        }
    }

    private abstract static class BaseBeeGoal extends EntityAIBase {
        final EntityCustom bee;

        BaseBeeGoal(EntityCustom bee) {
            this.bee = bee;
        }

        abstract boolean canBeeUse();

        boolean canBeeContinue() {
            return canBeeUse();
        }

        @Override
        public final boolean shouldExecute() {
            return !bee.isAngry() && canBeeUse();
        }

        @Override
        public final boolean shouldContinueExecuting() {
            return !bee.isAngry() && canBeeContinue();
        }
    }

    private static final class BeeAttackGoal extends EntityAIBase {
        private final EntityCustom bee;

        BeeAttackGoal(EntityCustom bee) {
            this.bee = bee;
            setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            return bee.isAngry() && !bee.hasStung() && bee.getAttackTarget() != null;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return shouldExecute() && bee.getAttackTarget().isEntityAlive();
        }

        @Override
        public void updateTask() {
            EntityLivingBase target = bee.getAttackTarget();
            bee.getNavigator().tryMoveToEntityLiving(target, 1.4D);
            bee.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
            if (bee.getDistanceSq(target) < 4.0D) {
                bee.attackEntityAsMob(target);
            }
        }
    }

    private static final class BeeHurtByOtherGoal extends EntityAIHurtByTarget {
        BeeHurtByOtherGoal(EntityCustom bee) {
            super(bee, true);
        }

        @Override
        public boolean shouldContinueExecuting() {
            return ((EntityCustom) taskOwner).isAngry() && super.shouldContinueExecuting();
        }
    }

    private static final class BeeAngryPlayerGoal extends EntityAIBase {
        private final EntityCustom bee;

        BeeAngryPlayerGoal(EntityCustom bee) {
            this.bee = bee;
        }

        @Override
        public boolean shouldExecute() {
            if (!bee.isAngry() || bee.hasStung()) {
                return false;
            }
            EntityPlayer target = bee.persistentAngerTarget == null ? null
                    : bee.world.getPlayerEntityByUUID(bee.persistentAngerTarget);
            if (target == null) {
                target = bee.world.getNearestAttackablePlayer(bee, 48.0D, 48.0D);
            }
            if (target == null) {
                return false;
            }
            bee.setAttackTarget(target);
            bee.persistentAngerTarget = target.getUniqueID();
            return true;
        }
    }

    private static final class BeeTemptGoal extends BaseBeeGoal {
        private EntityPlayer player;

        BeeTemptGoal(EntityCustom bee) {
            super(bee);
            setMutexBits(3);
        }

        @Override
        boolean canBeeUse() {
            player = bee.world.getClosestPlayer(bee.posX, bee.posY, bee.posZ,
                    10.0D, new Predicate<Entity>() {
                        @Override
                        public boolean apply(@Nullable Entity entity) {
                            if (!(entity instanceof EntityPlayer)) {
                                return false;
                            }
                            EntityPlayer candidate = (EntityPlayer) entity;
                            return EntityCustom.isFlower(candidate.getHeldItemMainhand())
                                    || EntityCustom.isFlower(candidate.getHeldItemOffhand());
                        }
                    });
            return player != null;
        }

        @Override
        public void updateTask() {
            bee.getLookHelper().setLookPositionWithEntity(player, 30.0F, 30.0F);
            bee.getNavigator().tryMoveToEntityLiving(player, 1.25D);
        }

        @Override
        public void resetTask() {
            player = null;
            bee.getNavigator().clearPath();
        }
    }

    private static final class BeePollinateGoal extends BaseBeeGoal {
        private int successfulTicks;
        private int pollinatingTicks;
        private int lastSoundTick;
        private boolean pollinating;

        BeePollinateGoal(EntityCustom bee) {
            super(bee);
            setMutexBits(1);
        }

        @Override
        boolean canBeeUse() {
            if (bee.remainingCooldownBeforeLocatingNewFlower > 0 || bee.hasNectar()
                    || bee.world.isRaining()) {
                return false;
            }
            BlockPos flower = findNearbyFlower();
            if (flower == null) {
                bee.remainingCooldownBeforeLocatingNewFlower =
                        20 + bee.randomSource().nextInt(41);
                return false;
            }
            bee.savedFlowerPos = flower;
            return true;
        }

        @Override
        boolean canBeeContinue() {
            if (!pollinating || bee.savedFlowerPos == null || bee.world.isRaining()) {
                return false;
            }
            if (BeeMechanics.pollinatedLongEnough(successfulTicks)) {
                return bee.randomSource().nextFloat() < 0.2F;
            }
            if (bee.ticksExisted % 20 == 0
                    && !EntityCustom.isFlowerAt(bee.world, bee.savedFlowerPos)) {
                bee.savedFlowerPos = null;
                return false;
            }
            return true;
        }

        @Override
        public void startExecuting() {
            successfulTicks = 0;
            pollinatingTicks = 0;
            lastSoundTick = 0;
            pollinating = true;
            bee.ticksWithoutNectarSinceExitingHive = 0;
        }

        @Override
        public void resetTask() {
            if (BeeMechanics.pollinatedLongEnough(successfulTicks)) {
                bee.setHasNectar(true);
            }
            pollinating = false;
            bee.getNavigator().clearPath();
            bee.remainingCooldownBeforeLocatingNewFlower =
                    BeeMechanics.LOCATE_FLOWER_COOLDOWN;
        }

        @Override
        public void updateTask() {
            pollinatingTicks++;
            if (BeeMechanics.pollinationTimedOut(pollinatingTicks)) {
                bee.savedFlowerPos = null;
                return;
            }
            BlockPos flower = bee.savedFlowerPos;
            double x = flower.getX() + 0.5D;
            double y = flower.getY() + 1.1D;
            double z = flower.getZ() + 0.5D;
            double distance = bee.getDistanceSq(x, y, z);
            if (distance > 1.0D) {
                bee.getMoveHelper().setMoveTo(x, y, z, 0.35D);
            } else {
                if (bee.randomSource().nextInt(25) == 0) {
                    x += (bee.randomSource().nextFloat() * 2.0F - 1.0F) / 3.0F;
                    z += (bee.randomSource().nextFloat() * 2.0F - 1.0F) / 3.0F;
                }
                bee.getMoveHelper().setMoveTo(x, y, z, 0.35D);
                successfulTicks++;
                if (bee.randomSource().nextFloat() < 0.05F
                        && successfulTicks > lastSoundTick + 60) {
                    lastSoundTick = successfulTicks;
                    bee.playSound(BeeSoundEvents.BEE_POLLINATE, 1.0F, 1.0F);
                }
            }
        }

        boolean isPollinating() {
            return pollinating;
        }

        void stopPollinating() {
            pollinating = false;
        }

        private BlockPos findNearbyFlower() {
            BlockPos origin = bee.getPosition();
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;
            for (int y = -1; y <= 5; y++) {
                for (int x = -5; x <= 5; x++) {
                    for (int z = -5; z <= 5; z++) {
                        BlockPos candidate = origin.add(x, y, z);
                        double distance = origin.distanceSq(candidate);
                        if (distance <= 25.0D && distance < bestDistance
                                && EntityCustom.isPollinationTargetAt(
                                        bee.world, candidate)) {
                            best = candidate;
                            bestDistance = distance;
                        }
                    }
                }
            }
            return best;
        }
    }

    private static final class BeeLocateHiveGoal extends BaseBeeGoal {
        BeeLocateHiveGoal(EntityCustom bee) {
            super(bee);
        }

        @Override
        boolean canBeeUse() {
            return bee.remainingCooldownBeforeLocatingNewHive == 0
                    && bee.hivePos == null && bee.wantsToEnterHive();
        }

        @Override
        public void startExecuting() {
            bee.remainingCooldownBeforeLocatingNewHive =
                    BeeMechanics.LOCATE_HIVE_COOLDOWN;
            List<TileEntityBeehive> candidates = new ArrayList<>();
            for (net.minecraft.tileentity.TileEntity tile : bee.world.loadedTileEntityList) {
                if (tile instanceof TileEntityBeehive && !((TileEntityBeehive) tile).isFull()
                        && bee.getDistanceSqToCenter(tile.getPos())
                        <= BeeMechanics.HIVE_SEARCH_DISTANCE
                                * BeeMechanics.HIVE_SEARCH_DISTANCE) {
                    candidates.add((TileEntityBeehive) tile);
                }
            }
            Collections.sort(candidates, Comparator.comparingDouble(
                    hive -> bee.getDistanceSqToCenter(hive.getPos())));
            if (candidates.isEmpty()) {
                return;
            }
            for (TileEntityBeehive candidate : candidates) {
                if (!bee.isHiveBlacklisted(candidate.getPos())) {
                    bee.hivePos = candidate.getPos();
                    return;
                }
            }
            bee.clearHiveBlacklist();
            bee.hivePos = candidates.get(0).getPos();
        }
    }

    private static final class BeeGoToHiveGoal extends BaseBeeGoal {
        private int travellingTicks;

        BeeGoToHiveGoal(EntityCustom bee) {
            super(bee);
            setMutexBits(1);
        }

        @Override
        boolean canBeeUse() {
            return bee.hivePos != null && bee.wantsToEnterHive()
                    && bee.isHiveValid() && bee.getDistanceSqToCenter(bee.hivePos) > 4.0D;
        }

        @Override
        public void startExecuting() {
            travellingTicks = 0;
        }

        @Override
        public void updateTask() {
            travellingTicks++;
            if (travellingTicks > 600) {
                bee.blacklistHive(bee.hivePos);
                bee.hivePos = null;
                bee.remainingCooldownBeforeLocatingNewHive =
                        BeeMechanics.LOCATE_HIVE_COOLDOWN;
                return;
            }
            if (bee.getDistanceSqToCenter(bee.hivePos)
                    > BeeMechanics.TOO_FAR_DISTANCE * BeeMechanics.TOO_FAR_DISTANCE) {
                bee.hivePos = null;
                bee.remainingCooldownBeforeLocatingNewHive =
                        BeeMechanics.LOCATE_HIVE_COOLDOWN;
                return;
            }
            if (!bee.getNavigator().tryMoveToXYZ(bee.hivePos.getX() + 0.5D,
                    bee.hivePos.getY() + 0.5D,
                    bee.hivePos.getZ() + 0.5D, 1.0D)) {
                bee.blacklistHive(bee.hivePos);
                bee.hivePos = null;
                bee.remainingCooldownBeforeLocatingNewHive =
                        BeeMechanics.LOCATE_HIVE_COOLDOWN;
            }
        }

        @Override
        public void resetTask() {
            bee.getNavigator().clearPath();
        }
    }

    private static final class BeeEnterHiveGoal extends BaseBeeGoal {
        BeeEnterHiveGoal(EntityCustom bee) {
            super(bee);
        }

        @Override
        boolean canBeeUse() {
            if (bee.hivePos == null || !bee.wantsToEnterHive()
                    || bee.getDistanceSqToCenter(bee.hivePos) > 4.0D) {
                return false;
            }
            net.minecraft.tileentity.TileEntity tile = bee.world.getTileEntity(bee.hivePos);
            if (!(tile instanceof TileEntityBeehive)) {
                bee.hivePos = null;
                return false;
            }
            if (((TileEntityBeehive) tile).isFull()) {
                bee.hivePos = null;
                return false;
            }
            return true;
        }

        @Override
        boolean canBeeContinue() {
            return false;
        }

        @Override
        public void startExecuting() {
            net.minecraft.tileentity.TileEntity tile = bee.world.getTileEntity(bee.hivePos);
            if (tile instanceof TileEntityBeehive) {
                ((TileEntityBeehive) tile).addOccupant(bee, bee.hasNectar());
            }
        }
    }

    private static final class BeeGoToKnownFlowerGoal extends BaseBeeGoal {
        private int travellingTicks;

        BeeGoToKnownFlowerGoal(EntityCustom bee) {
            super(bee);
            setMutexBits(1);
        }

        @Override
        boolean canBeeUse() {
            return bee.savedFlowerPos != null
                    && bee.ticksWithoutNectarSinceExitingHive
                            > BeeMechanics.TICKS_BEFORE_GOING_TO_KNOWN_FLOWER
                    && EntityCustom.isFlowerAt(bee.world, bee.savedFlowerPos)
                    && bee.getDistanceSqToCenter(bee.savedFlowerPos) > 4.0D;
        }

        @Override
        public void startExecuting() {
            travellingTicks = 0;
        }

        @Override
        public void updateTask() {
            travellingTicks++;
            if (travellingTicks > 600
                    || bee.getDistanceSqToCenter(bee.savedFlowerPos)
                    > BeeMechanics.TOO_FAR_DISTANCE * BeeMechanics.TOO_FAR_DISTANCE) {
                bee.savedFlowerPos = null;
                return;
            }
            BlockPos flower = bee.savedFlowerPos;
            bee.getNavigator().tryMoveToXYZ(flower.getX() + 0.5D,
                    flower.getY() + 0.5D, flower.getZ() + 0.5D, 1.0D);
        }
    }

    private static final class BeeGrowCropGoal extends BaseBeeGoal {
        BeeGrowCropGoal(EntityCustom bee) {
            super(bee);
        }

        @Override
        boolean canBeeUse() {
            return BeeMechanics.canGrowCrop(bee.cropsGrownSincePollination,
                    bee.randomSource().nextFloat(), bee.hasNectar(), bee.isHiveValid());
        }

        @Override
        public void updateTask() {
            if (bee.randomSource().nextInt(30) != 0) {
                return;
            }
            for (int depth = 1; depth <= 2; depth++) {
                BlockPos pos = bee.getPosition().down(depth);
                BeeCropGrowth.Result result = BeeCropGrowth.grow(bee.world, pos);
                if (result.incrementsPollinationCount()) {
                    bee.incrementCropsGrownSincePollination();
                }
            }
        }
    }

    private static final class BeeWanderGoal extends EntityAIBase {
        private final EntityCustom bee;
        private double x;
        private double y;
        private double z;

        BeeWanderGoal(EntityCustom bee) {
            this.bee = bee;
            setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            if (!bee.getNavigator().noPath()
                    || bee.randomSource().nextInt(10) != 0) {
                return false;
            }
            x = bee.posX + bee.randomSource().nextInt(17) - 8;
            y = bee.posY + bee.randomSource().nextInt(9) - 4;
            z = bee.posZ + bee.randomSource().nextInt(17) - 8;
            return true;
        }

        @Override
        public void startExecuting() {
            bee.getNavigator().tryMoveToXYZ(x, y, z, 1.0D);
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !bee.getNavigator().noPath();
        }
    }
}

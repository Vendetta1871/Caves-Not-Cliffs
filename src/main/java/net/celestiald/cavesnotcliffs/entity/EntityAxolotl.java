package net.celestiald.cavesnotcliffs.entity;

import com.google.common.base.Predicate;
import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.content.AxolotlMechanics;
import net.celestiald.cavesnotcliffs.content.AxolotlSoundEvents;
import net.celestiald.cavesnotcliffs.item.ItemAxolotlBucket;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.celestiald.cavesnotcliffs.world.V118CubicChunksGenerator;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIFollowParent;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITempt;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateSwimmer;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Java 1.12 runtime adaptation of the Java 1.18.2 axolotl contract. */
@ElementsCavesNotCliffs.ModElement.Tag
public final class EntityAxolotl extends ElementsCavesNotCliffs.ModElement {
    public static final int NETWORK_ID = 0;
    private static final int EGG_PRIMARY = 16499171;
    private static final int EGG_SECONDARY = 10890612;

    @GameRegistry.ObjectHolder("cavesnotcliffs:axolotl_bucket")
    public static final Item axolotlBucket = null;
    @GameRegistry.ObjectHolder("cavesnotcliffs:tropical_fish_bucket")
    public static final Item tropicalFishBucket = null;

    public EntityAxolotl(ElementsCavesNotCliffs instance) {
        super(instance, 67);
    }

    @Override
    public void initElements() {
        EntityRegistry.registerModEntity(CncRegistryIds.AXOLOTL,
                EntityCustom.class, "axolotl", NETWORK_ID, CavesNotCliffs.instance,
                80, 3, true, EGG_PRIMARY, EGG_SECONDARY);
        elements.items.add(() -> new ItemAxolotlBucket()
                .setRegistryName(CncRegistryIds.AXOLOTL_BUCKET));
        elements.items.add(() -> new Item()
                .setRegistryName(CncRegistryIds.TROPICAL_FISH_BUCKET)
                .setUnlocalizedName("tropical_fish_bucket")
                .setMaxStackSize(1)
                .setCreativeTab(net.minecraft.creativetab.CreativeTabs.MISC));
    }

    @Override
    public void init(FMLInitializationEvent event) {
        // Native biome spawning is supplied by V118CubicChunksGenerator#getPossibleCreatures;
        // registering against the 1.12 surface projection would leak axolotls into forest ponds.
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(axolotlBucket, 0,
                new ModelResourceLocation(CncRegistryIds.AXOLOTL_BUCKET, "inventory"));
        ModelLoader.setCustomModelResourceLocation(tropicalFishBucket, 0,
                new ModelResourceLocation(CncRegistryIds.TROPICAL_FISH_BUCKET, "inventory"));
    }

    public static final class EntityCustom extends EntityAnimal {
        private static final DataParameter<Integer> VARIANT =
                EntityDataManager.createKey(EntityCustom.class, DataSerializers.VARINT);
        private static final DataParameter<Boolean> PLAYING_DEAD =
                EntityDataManager.createKey(EntityCustom.class, DataSerializers.BOOLEAN);
        private static final DataParameter<Boolean> FROM_BUCKET =
                EntityDataManager.createKey(EntityCustom.class, DataSerializers.BOOLEAN);
        private static final DamageSource DRY_OUT =
                new DamageSource("dry_out").setDamageBypassesArmor();

        private int playDeadTicks;
        private int huntingCooldown;
        private EntityLivingBase combatTarget;

        public EntityCustom(World world) {
            super(world);
            setSize(0.75F, 0.42F);
            stepHeight = 1.0F;
            setAir(AxolotlMechanics.MAX_AIR_SUPPLY);
        }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataManager.register(VARIANT, AxolotlMechanics.Variant.LUCY.id());
            dataManager.register(PLAYING_DEAD, false);
            dataManager.register(FROM_BUCKET, false);
        }

        @Override
        protected void initEntityAI() {
            tasks.addTask(0, new EntityAISwimming(this));
            tasks.addTask(1, new EntityAIMate(this, 0.2D));
            tasks.addTask(2, new EntityAITempt(this, 0.5D,
                    tropicalFishBucket, false));
            tasks.addTask(3, new EntityAIFollowParent(this, 0.6D));
            tasks.addTask(4, new AxolotlMeleeAttack(this));
            tasks.addTask(5, new EntityAIAxolotlSwim(this));
            tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
            tasks.addTask(7, new EntityAILookIdle(this));
            targetTasks.addTask(1, new AxolotlTarget(this));
        }

        @Override
        protected PathNavigate createNavigator(World world) {
            return new PathNavigateSwimmer(this, world);
        }

        @Override
        protected void applyEntityAttributes() {
            super.applyEntityAttributes();
            getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(14.0D);
            getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(1.0D);
            getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE)
                    .setBaseValue(2.0D);
            getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(8.0D);
        }

        @Override
        public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty,
                IEntityLivingData spawnData) {
            AxolotlGroupData group;
            if (spawnData instanceof AxolotlGroupData) {
                group = (AxolotlGroupData) spawnData;
            } else {
                group = new AxolotlGroupData(
                        AxolotlMechanics.Variant.common(rand),
                        AxolotlMechanics.Variant.common(rand));
            }
            if (group.spawnCount >= 2) {
                setGrowingAge(-24000);
            }
            group.spawnCount++;
            setVariant(group.types[rand.nextInt(group.types.length)]);
            super.onInitialSpawn(difficulty, group);
            return group;
        }

        @Override
        public boolean getCanSpawnHere() {
            Block below = world.getBlockState(getPosition().down()).getBlock();
            V118CubicChunksGenerator generator = V118CubicChunksGenerator.forWorld(world);
            return world.getBlockState(getPosition()).getMaterial() == Material.WATER
                    && below == Blocks.CLAY
                    && generator != null
                    && generator.getVirtualBiome(getPosition().getX(), getPosition().getY(),
                        getPosition().getZ()) == V118Biome.LUSH_CAVES
                    && world.checkNoEntityCollision(getEntityBoundingBox());
        }

        @Override
        public boolean canBreatheUnderwater() {
            return true;
        }

        @Override
        public void onEntityUpdate() {
            super.onEntityUpdate();
            if (world.isRemote || isAIDisabled() || !isEntityAlive()) {
                return;
            }
            if (!isWet()) {
                int air = getAir() - 1;
                setAir(air);
                if (air == -20) {
                    setAir(0);
                    attackEntityFrom(DRY_OUT, 2.0F);
                }
            } else {
                setAir(AxolotlMechanics.MAX_AIR_SUPPLY);
            }
        }

        @Override
        public void onLivingUpdate() {
            super.onLivingUpdate();
            if (world.isRemote) {
                return;
            }
            if (playDeadTicks > 0) {
                playDeadTicks--;
                setPlayingDead(playDeadTicks > 0);
                motionX = 0.0D;
                motionY = 0.0D;
                motionZ = 0.0D;
                getNavigator().clearPath();
                setAttackTarget(null);
            }
            if (huntingCooldown > 0) {
                huntingCooldown--;
            }
            updateCombatSupport();
            if (!isWet() && !isPlayingDead() && ticksExisted % 20 == 0) {
                moveTowardNearbyWater();
            }
        }

        @Override
        protected boolean isMovementBlocked() {
            return isPlayingDead() || super.isMovementBlocked();
        }

        @Override
        public boolean attackEntityFrom(DamageSource source, float amount) {
            boolean shouldPlayDead = !world.isRemote && !isAIDisabled()
                    && AxolotlMechanics.shouldPlayDead(rand, amount, getHealth(), getMaxHealth(),
                        isInWater(), source.getTrueSource() != null
                            || source.getImmediateSource() != null, isPlayingDead());
            if (shouldPlayDead) {
                playDeadTicks = AxolotlMechanics.TOTAL_PLAY_DEAD_TIME;
                setPlayingDead(true);
            }
            return super.attackEntityFrom(source, amount);
        }

        @Override
        public boolean attackEntityAsMob(Entity target) {
            boolean hit = target.attackEntityFrom(DamageSource.causeMobDamage(this),
                    (float) getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE)
                        .getAttributeValue());
            if (hit) {
                playSound(AxolotlSoundEvents.ATTACK, 1.0F, 1.0F);
            }
            return hit;
        }

        @Override
        public EntityAgeable createChild(EntityAgeable mate) {
            EntityCustom child = new EntityCustom(world);
            AxolotlMechanics.Variant other = mate instanceof EntityCustom
                    ? ((EntityCustom) mate).getVariant() : getVariant();
            child.setVariant(AxolotlMechanics.childVariant(getVariant(), other, rand));
            child.enablePersistence();
            return child;
        }

        @Override
        public boolean isBreedingItem(ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() == tropicalFishBucket;
        }

        @Override
        protected void consumeItemFromStack(EntityPlayer player, ItemStack stack) {
            if (stack.getItem() != tropicalFishBucket) {
                super.consumeItemFromStack(player, stack);
                return;
            }
            EnumHand hand = player.getHeldItemMainhand() == stack
                    ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;
            // Axolotl#usePlayerItem replaces this bucket even for instant-build players.
            player.setHeldItem(hand, new ItemStack(Items.WATER_BUCKET));
        }

        @Override
        public boolean processInteract(EntityPlayer player, EnumHand hand) {
            ItemStack held = player.getHeldItem(hand);
            if (held.getItem() == Items.WATER_BUCKET && isEntityAlive()
                    && axolotlBucket != null) {
                playSound(AxolotlSoundEvents.BUCKET_FILL, 1.0F, 1.0F);
                if (!world.isRemote) {
                    ItemStack filled = ItemAxolotlBucket.capture(this, axolotlBucket);
                    if (player.capabilities.isCreativeMode) {
                        if (!player.inventory.addItemStackToInventory(filled)) {
                            player.dropItem(filled, false);
                        }
                    } else {
                        player.setHeldItem(hand, filled);
                    }
                    setDead();
                }
                return true;
            }
            return super.processInteract(player, hand);
        }

        @Override
        public void travel(float strafe, float vertical, float forward) {
            if (isServerWorld() && isInWater()) {
                moveRelative(strafe, vertical, forward, getAIMoveSpeed());
                move(MoverType.SELF, motionX, motionY, motionZ);
                motionX *= 0.90D;
                motionY *= 0.90D;
                motionZ *= 0.90D;
            } else {
                super.travel(strafe, vertical, forward);
            }
        }

        @Override
        public void writeEntityToNBT(NBTTagCompound tag) {
            super.writeEntityToNBT(tag);
            tag.setInteger("Variant", getVariant().id());
            tag.setBoolean("FromBucket", fromBucket());
            tag.setInteger("HuntingCooldown", huntingCooldown);
        }

        @Override
        public void readEntityFromNBT(NBTTagCompound tag) {
            super.readEntityFromNBT(tag);
            setVariant(AxolotlMechanics.Variant.byId(tag.getInteger("Variant")));
            setFromBucket(tag.getBoolean("FromBucket"));
            huntingCooldown = Math.max(0, tag.getInteger("HuntingCooldown"));
        }

        public void saveToBucket(NBTTagCompound tag) {
            tag.setInteger("Variant", getVariant().id());
            tag.setInteger("Age", getGrowingAge());
            tag.setInteger("HuntingCooldown", huntingCooldown);
            tag.setFloat("Health", getHealth());
            if (isAIDisabled()) {
                tag.setBoolean("NoAI", true);
            }
            if (isSilent()) {
                tag.setBoolean("Silent", true);
            }
            if (hasNoGravity()) {
                tag.setBoolean("NoGravity", true);
            }
            if (isGlowing()) {
                tag.setBoolean("Glowing", true);
            }
        }

        public void loadFromBucket(ItemStack stack) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag != null) {
                setVariant(AxolotlMechanics.Variant.byId(tag.getInteger("Variant")));
                if (tag.hasKey("Age")) {
                    setGrowingAge(tag.getInteger("Age"));
                }
                huntingCooldown = Math.max(0, tag.getInteger("HuntingCooldown"));
                if (tag.hasKey("Health")) {
                    setHealth(Math.min(getMaxHealth(), tag.getFloat("Health")));
                }
                setNoAI(tag.getBoolean("NoAI"));
                setSilent(tag.getBoolean("Silent"));
                setNoGravity(tag.getBoolean("NoGravity"));
                setGlowing(tag.getBoolean("Glowing"));
            }
            if (stack.hasDisplayName()) {
                setCustomNameTag(stack.getDisplayName());
            }
            setFromBucket(true);
            enablePersistence();
            rehydrate();
        }

        public AxolotlMechanics.Variant getVariant() {
            return AxolotlMechanics.Variant.byId(dataManager.get(VARIANT));
        }

        public void setVariant(AxolotlMechanics.Variant variant) {
            dataManager.set(VARIANT, variant.id());
        }

        public boolean isPlayingDead() {
            return dataManager.get(PLAYING_DEAD);
        }

        private void setPlayingDead(boolean playingDead) {
            dataManager.set(PLAYING_DEAD, playingDead);
        }

        public boolean fromBucket() {
            return dataManager.get(FROM_BUCKET);
        }

        public void setFromBucket(boolean fromBucket) {
            dataManager.set(FROM_BUCKET, fromBucket);
        }

        public void rehydrate() {
            setAir(AxolotlMechanics.rehydratedAir(getAir()));
        }

        @Override
        protected boolean canDespawn() {
            return !fromBucket() && !hasCustomName();
        }

        @Override
        protected SoundEvent getAmbientSound() {
            return isPlayingDead() ? null
                    : isInWater() ? AxolotlSoundEvents.IDLE_WATER
                    : AxolotlSoundEvents.IDLE_AIR;
        }

        @Override
        protected SoundEvent getHurtSound(DamageSource source) {
            return AxolotlSoundEvents.HURT;
        }

        @Override
        protected SoundEvent getDeathSound() {
            return AxolotlSoundEvents.DEATH;
        }

        @Override
        protected SoundEvent getSwimSound() {
            return AxolotlSoundEvents.SWIM;
        }

        @Override
        protected SoundEvent getSplashSound() {
            return AxolotlSoundEvents.SPLASH;
        }

        private void updateCombatSupport() {
            EntityLivingBase current = getAttackTarget();
            if (current != null && current.isEntityAlive()) {
                combatTarget = current;
                return;
            }
            if (combatTarget == null) {
                return;
            }
            if (!combatTarget.isEntityAlive()) {
                DamageSource source = combatTarget.getLastDamageSource();
                Entity killer = source == null ? null : source.getTrueSource();
                if (killer instanceof EntityPlayer
                        && getDistanceSq(killer) <= AxolotlMechanics.SUPPORT_RANGE
                            * AxolotlMechanics.SUPPORT_RANGE) {
                    applySupportingEffects((EntityPlayer) killer);
                }
            }
            huntingCooldown = AxolotlMechanics.HUNTING_COOLDOWN;
            combatTarget = null;
        }

        private void applySupportingEffects(EntityPlayer player) {
            PotionEffect current = player.getActivePotionEffect(MobEffects.REGENERATION);
            int duration = current == null ? 0 : current.getDuration();
            player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION,
                    AxolotlMechanics.regenerationDuration(duration), 0));
            player.removePotionEffect(MobEffects.MINING_FATIGUE);
        }

        private void moveTowardNearbyWater() {
            BlockPos origin = getPosition();
            BlockPos closest = null;
            double distance = Double.MAX_VALUE;
            for (int x = -6; x <= 6; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -6; z <= 6; z++) {
                        BlockPos candidate = origin.add(x, y, z);
                        if (world.getBlockState(candidate).getMaterial() != Material.WATER) {
                            continue;
                        }
                        double candidateDistance = candidate.distanceSq(origin);
                        if (candidateDistance < distance) {
                            closest = candidate;
                            distance = candidateDistance;
                        }
                    }
                }
            }
            if (closest != null) {
                getNavigator().tryMoveToXYZ(closest.getX() + 0.5D,
                        closest.getY() + 0.5D, closest.getZ() + 0.5D, 0.15D);
            }
        }

        private static final class AxolotlGroupData implements IEntityLivingData {
            private final AxolotlMechanics.Variant[] types;
            private int spawnCount;

            private AxolotlGroupData(AxolotlMechanics.Variant... types) {
                this.types = types;
            }
        }

        private static final class EntityAIAxolotlSwim
                extends net.minecraft.entity.ai.EntityAIBase {
            private final EntityCustom axolotl;

            private EntityAIAxolotlSwim(EntityCustom axolotl) {
                this.axolotl = axolotl;
                setMutexBits(1);
            }

            @Override
            public boolean shouldExecute() {
                return !axolotl.isPlayingDead() && axolotl.isInWater()
                        && axolotl.getRNG().nextInt(12) == 0;
            }

            @Override
            public void startExecuting() {
                Random random = axolotl.getRNG();
                axolotl.motionX += (random.nextDouble() - 0.5D) * 0.18D;
                axolotl.motionY += (random.nextDouble() - 0.5D) * 0.10D;
                axolotl.motionZ += (random.nextDouble() - 0.5D) * 0.18D;
            }
        }

        private static final class AxolotlMeleeAttack extends EntityAIAttackMelee {
            private final EntityCustom axolotl;

            private AxolotlMeleeAttack(EntityCustom axolotl) {
                super(axolotl, 0.6D, true);
                this.axolotl = axolotl;
            }

            @Override
            public boolean shouldExecute() {
                return !axolotl.isPlayingDead() && super.shouldExecute();
            }

            @Override
            protected double getAttackReachSqr(EntityLivingBase target) {
                return 1.5D + target.width * 2.0D;
            }
        }

        private static final class AxolotlTarget
                extends net.minecraft.entity.ai.EntityAIBase {
            private final EntityCustom axolotl;
            private EntityLivingBase selected;

            private AxolotlTarget(EntityCustom axolotl) {
                this.axolotl = axolotl;
                setMutexBits(1);
            }

            @Override
            public boolean shouldExecute() {
                if (axolotl.isPlayingDead() || !axolotl.isInWater()
                        || axolotl.isInLove()) {
                    return false;
                }
                AxisAlignedBB area = axolotl.getEntityBoundingBox()
                        .grow(AxolotlMechanics.TARGET_RANGE);
                List<EntityLivingBase> targets = axolotl.world.getEntitiesWithinAABB(
                        EntityLivingBase.class, area, new Predicate<EntityLivingBase>() {
                            @Override
                            public boolean apply(EntityLivingBase target) {
                                if (target == null || !target.isEntityAlive()
                                        || !target.isInWater()) {
                                    return false;
                                }
                                if (target instanceof net.minecraft.entity.monster.EntityGuardian) {
                                    return true;
                                }
                                return axolotl.huntingCooldown == 0
                                        && target instanceof EntitySquid;
                            }
                        });
                if (targets.isEmpty()) {
                    return false;
                }
                targets.sort(Comparator.comparingDouble(axolotl::getDistanceSq));
                selected = targets.get(0);
                return axolotl.getDistanceSq(selected) <= 64.0D;
            }

            @Override
            public void startExecuting() {
                axolotl.setAttackTarget(selected);
            }

            @Override
            public boolean shouldContinueExecuting() {
                return selected != null && selected.isEntityAlive()
                        && selected.isInWater() && !axolotl.isPlayingDead()
                        && axolotl.getDistanceSq(selected) <= 64.0D;
            }

            @Override
            public void resetTask() {
                axolotl.setAttackTarget(null);
                selected = null;
            }
        }
    }
}

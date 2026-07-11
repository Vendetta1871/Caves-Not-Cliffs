package net.celestiald.cavesnotcliffs.tile;

import net.celestiald.cavesnotcliffs.block.BlockBeehive;
import net.celestiald.cavesnotcliffs.content.BeeMechanics;
import net.celestiald.cavesnotcliffs.content.BeeSoundEvents;
import net.celestiald.cavesnotcliffs.content.BeehiveSmokeHooks;
import net.celestiald.cavesnotcliffs.entity.EntityBee;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Java 1.12 tile-entity adaptation of Java 1.18.2's BeehiveBlockEntity.
 *
 * <p>The stored-list format and occupant timers are intentionally byte-for-byte compatible at
 * the named-tag level: Bees / EntityData / TicksInHive / MinOccupationTicks / FlowerPos.</p>
 */
public final class TileEntityBeehive extends TileEntity implements ITickable {
    public static final String TAG_FLOWER_POS = "FlowerPos";
    public static final String TAG_MIN_OCCUPATION_TICKS = "MinOccupationTicks";
    public static final String TAG_ENTITY_DATA = "EntityData";
    public static final String TAG_TICKS_IN_HIVE = "TicksInHive";
    public static final String TAG_HAS_NECTAR = "HasNectar";
    public static final String TAG_BEES = "Bees";
    /** 1.12 adaptation: official 1.18 stores this value in the block state. */
    public static final String TAG_HONEY_LEVEL = "HoneyLevel";

    private static final List<String> IGNORED_BEE_TAGS = Arrays.asList(
            "Air", "ArmorDropChances", "ArmorItems", "Brain", "CanPickUpLoot",
            "DeathTime", "FallDistance", "FallFlying", "Fire", "HandDropChances",
            "HandItems", "HurtByTimestamp", "HurtTime", "LeftHanded", "Motion",
            "NoGravity", "OnGround", "PortalCooldown", "Pos", "Rotation",
            "CannotEnterHiveTicks", "TicksSincePollination",
            "CropsGrownSincePollination", "HivePos", "Passengers", "Leash", "UUID",
            "UUIDMost", "UUIDLeast");

    private final List<BeeData> stored = new ArrayList<>();
    private BlockPos savedFlowerPos;
    private int honeyLevel;
    private boolean breakHandled;

    @Override
    public void update() {
        if (world == null || world.isRemote) {
            return;
        }
        boolean changed = false;
        Iterator<BeeData> iterator = stored.iterator();
        while (iterator.hasNext()) {
            BeeData data = iterator.next();
            if (BeeMechanics.occupationComplete(data.ticksInHive,
                    data.minOccupationTicks)) {
                BeeReleaseStatus status = data.entityData.getBoolean(TAG_HAS_NECTAR)
                        ? BeeReleaseStatus.HONEY_DELIVERED
                        : BeeReleaseStatus.BEE_RELEASED;
                if (releaseOccupant(data, status, null)) {
                    iterator.remove();
                    changed = true;
                }
            }
            data.ticksInHive++;
        }
        if (changed) {
            markDirtyAndSync();
        }
        if (!stored.isEmpty() && world.rand.nextDouble() < 0.005D) {
            world.playSound(null, pos, BeeSoundEvents.BEEHIVE_WORK,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
    }

    public boolean isEmpty() {
        return stored.isEmpty();
    }

    public boolean isFull() {
        return stored.size() >= BeeMechanics.MAX_HIVE_OCCUPANTS;
    }

    public int getOccupantCount() {
        return stored.size();
    }

    public int getHoneyLevel() {
        return honeyLevel;
    }

    public void setHoneyLevel(int level) {
        honeyLevel = BeeMechanics.requireHoneyLevel(level);
        updateVisualState();
        markDirtyAndSync();
    }

    public boolean isSedated() {
        return world != null && BeehiveSmokeHooks.isSmokey(world, pos);
    }

    public boolean isFireNearby() {
        if (world == null) {
            return false;
        }
        for (BlockPos candidate : BlockPos.getAllInBox(pos.add(-1, -1, -1),
                pos.add(1, 1, 1))) {
            if (world.getBlockState(candidate).getBlock() instanceof BlockFire) {
                return true;
            }
        }
        return false;
    }

    public void addOccupant(Entity entity, boolean hasNectar) {
        addOccupantWithPresetTicks(entity, hasNectar, 0);
    }

    public void addOccupantWithPresetTicks(Entity entity, boolean hasNectar,
            int ticksInHive) {
        if (isFull()) {
            return;
        }
        entity.dismountRidingEntity();
        entity.removePassengers();
        NBTTagCompound entityData = new NBTTagCompound();
        entity.writeToNBT(entityData);
        storeBee(entityData, ticksInHive, hasNectar);
        if (entity instanceof EntityBee.EntityCustom) {
            BlockPos flower = ((EntityBee.EntityCustom) entity).getSavedFlowerPos();
            if (flower != null && (savedFlowerPos == null || world.rand.nextBoolean())) {
                savedFlowerPos = flower;
            }
        }
        if (world != null) {
            world.playSound(null, pos, BeeSoundEvents.BEEHIVE_ENTER,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
        entity.setDead();
        markDirtyAndSync();
    }

    /** Used by tree generation and silk-load restoration. */
    public void storeBee(NBTTagCompound entityData, int ticksInHive,
            boolean hasNectar) {
        if (isFull()) {
            return;
        }
        NBTTagCompound copy = entityData.copy();
        copy.setBoolean(TAG_HAS_NECTAR, hasNectar);
        stored.add(new BeeData(copy, Math.max(0, ticksInHive),
                BeeMechanics.minimumOccupationTicks(hasNectar)));
        markDirty();
    }

    public void emptyAllLivingFromHive(@Nullable EntityPlayer player,
            BeeReleaseStatus status) {
        List<Entity> released = releaseAllOccupants(status);
        if (player == null) {
            return;
        }
        for (Entity entity : released) {
            if (!(entity instanceof EntityBee.EntityCustom)
                    || player.getDistanceSq(entity) > 16.0D) {
                continue;
            }
            EntityBee.EntityCustom bee = (EntityBee.EntityCustom) entity;
            if (!isSedated()) {
                bee.becomeAngryAt(player);
            } else {
                bee.setStayOutOfHiveCountdown(
                        BeeMechanics.MIN_TICKS_BEFORE_REENTERING_HIVE);
            }
        }
    }

    public List<Entity> releaseAllOccupants(BeeReleaseStatus status) {
        List<Entity> released = new ArrayList<>();
        Iterator<BeeData> iterator = stored.iterator();
        while (iterator.hasNext()) {
            BeeData data = iterator.next();
            Entity entity = releaseOccupantEntity(data, status, released);
            if (entity != null) {
                iterator.remove();
            }
        }
        if (!released.isEmpty()) {
            markDirtyAndSync();
        }
        return released;
    }

    private boolean releaseOccupant(BeeData data, BeeReleaseStatus status,
            @Nullable List<Entity> released) {
        return releaseOccupantEntity(data, status, released) != null;
    }

    @Nullable
    private Entity releaseOccupantEntity(BeeData data, BeeReleaseStatus status,
            @Nullable List<Entity> released) {
        if (world == null) {
            return null;
        }
        EnumFacing facing = BlockBeehive.facing(world.getBlockState(pos));
        BlockPos exitPos = pos.offset(facing);
        IBlockState exitState = world.getBlockState(exitPos);
        boolean blocked = !exitState.getMaterial().isReplaceable()
                && exitState.getCollisionBoundingBox(world, exitPos) != Block.NULL_AABB;
        boolean emergency = status == BeeReleaseStatus.EMERGENCY;
        if (!BeeMechanics.canRelease(emergency, !world.isDaytime(),
                world.isRaining(), blocked)) {
            return null;
        }

        NBTTagCompound entityData = data.entityData.copy();
        removeIgnoredBeeTags(entityData);
        entityData.setTag("HivePos", writePos(pos));
        entityData.setBoolean("NoGravity", true);
        advanceStoredAge(entityData, data.ticksInHive);
        Entity entity = EntityList.createEntityFromNBT(entityData, world);
        if (!(entity instanceof EntityBee.EntityCustom)) {
            return null;
        }
        EntityBee.EntityCustom bee = (EntityBee.EntityCustom) entity;
        if (savedFlowerPos != null && bee.getSavedFlowerPos() == null
                && world.rand.nextFloat() < 0.9F) {
            bee.setSavedFlowerPos(savedFlowerPos);
        }
        if (status == BeeReleaseStatus.HONEY_DELIVERED) {
            bee.dropOffNectar();
            int increment = BeeMechanics.honeyIncrement(honeyLevel, world.rand);
            if (increment > 0) {
                setHoneyLevel(honeyLevel + increment);
            }
        }

        double offset = blocked ? 0.0D : 0.55D + bee.width / 2.0D;
        double x = pos.getX() + 0.5D + offset * facing.getFrontOffsetX();
        double y = pos.getY() + 0.5D - bee.height / 2.0D;
        double z = pos.getZ() + 0.5D + offset * facing.getFrontOffsetZ();
        bee.setLocationAndAngles(x, y, z, bee.rotationYaw, bee.rotationPitch);
        if (!world.spawnEntity(bee)) {
            return null;
        }
        world.playSound(null, pos, BeeSoundEvents.BEEHIVE_EXIT,
                SoundCategory.BLOCKS, 1.0F, 1.0F);
        if (released != null) {
            released.add(bee);
        }
        return bee;
    }

    private static void advanceStoredAge(NBTTagCompound entityData, int ticks) {
        int age = entityData.getInteger("Age");
        if (age < 0) {
            entityData.setInteger("Age", Math.min(0, age + ticks));
        } else if (age > 0) {
            entityData.setInteger("Age", Math.max(0, age - ticks));
        }
        if (entityData.hasKey("InLove", 99)) {
            entityData.setInteger("InLove",
                    Math.max(0, entityData.getInteger("InLove") - ticks));
        }
    }

    public NBTTagList writeBees() {
        NBTTagList bees = new NBTTagList();
        for (BeeData data : stored) {
            NBTTagCompound entityData = data.entityData.copy();
            entityData.removeTag("UUID");
            entityData.removeTag("UUIDMost");
            entityData.removeTag("UUIDLeast");
            NBTTagCompound entry = new NBTTagCompound();
            entry.setTag(TAG_ENTITY_DATA, entityData);
            entry.setInteger(TAG_TICKS_IN_HIVE, data.ticksInHive);
            entry.setInteger(TAG_MIN_OCCUPATION_TICKS, data.minOccupationTicks);
            bees.appendTag(entry);
        }
        return bees;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        stored.clear();
        NBTTagList bees = tag.getTagList(TAG_BEES, 10);
        for (int index = 0; index < bees.tagCount(); index++) {
            NBTTagCompound entry = bees.getCompoundTagAt(index);
            stored.add(new BeeData(entry.getCompoundTag(TAG_ENTITY_DATA),
                    entry.getInteger(TAG_TICKS_IN_HIVE),
                    entry.getInteger(TAG_MIN_OCCUPATION_TICKS)));
        }
        savedFlowerPos = tag.hasKey(TAG_FLOWER_POS, 10)
                ? readPos(tag.getCompoundTag(TAG_FLOWER_POS)) : null;
        honeyLevel = tag.hasKey(TAG_HONEY_LEVEL, 99)
                ? Math.max(0, Math.min(BeeMechanics.MAX_HONEY_LEVEL,
                        tag.getInteger(TAG_HONEY_LEVEL))) : 0;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setTag(TAG_BEES, writeBees());
        if (savedFlowerPos != null) {
            tag.setTag(TAG_FLOWER_POS, writePos(savedFlowerPos));
        }
        tag.setInteger(TAG_HONEY_LEVEL, honeyLevel);
        return tag;
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 11, getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public void onDataPacket(NetworkManager network, SPacketUpdateTileEntity packet) {
        readFromNBT(packet.getNbtCompound());
    }

    @Override
    public boolean shouldRefresh(net.minecraft.world.World world, BlockPos pos,
            IBlockState oldState, IBlockState newState) {
        return !(oldState.getBlock() instanceof BlockBeehive.BlockCustom
                && newState.getBlock() instanceof BlockBeehive.BlockCustom);
    }

    public boolean isBreakHandled() {
        return breakHandled;
    }

    public void setBreakHandled(boolean handled) {
        breakHandled = handled;
    }

    private void updateVisualState() {
        if (world == null || world.isRemote) {
            return;
        }
        IBlockState current = world.getBlockState(pos);
        if (!(current.getBlock() instanceof BlockBeehive.BlockCustom)) {
            return;
        }
        BlockBeehive.BlockCustom hive = (BlockBeehive.BlockCustom) current.getBlock();
        Block target = BlockBeehive.blockFor(hive.isNest(),
                honeyLevel >= BeeMechanics.MAX_HONEY_LEVEL);
        if (target == null || target == current.getBlock()) {
            return;
        }
        EnumFacing facing = current.getValue(BlockBeehive.BlockCustom.FACING);
        world.setBlockState(pos, target.getDefaultState()
                .withProperty(BlockBeehive.BlockCustom.FACING, facing), 3);
    }

    private void markDirtyAndSync() {
        markDirty();
        if (world != null) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
            world.updateComparatorOutputLevel(pos, state.getBlock());
        }
    }

    public static void removeIgnoredBeeTags(NBTTagCompound tag) {
        for (String ignored : IGNORED_BEE_TAGS) {
            tag.removeTag(ignored);
        }
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

    public enum BeeReleaseStatus {
        HONEY_DELIVERED,
        BEE_RELEASED,
        EMERGENCY
    }

    private static final class BeeData {
        final NBTTagCompound entityData;
        int ticksInHive;
        final int minOccupationTicks;

        BeeData(NBTTagCompound entityData, int ticksInHive, int minOccupationTicks) {
            NBTTagCompound copy = entityData.copy();
            removeIgnoredBeeTags(copy);
            this.entityData = copy;
            this.ticksInHive = Math.max(0, ticksInHive);
            this.minOccupationTicks = Math.max(0, minOccupationTicks);
        }
    }
}

package net.celestiald.cavesnotcliffs.entity;

import io.netty.buffer.ByteBuf;
import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.block.BlockPointedDripstone;
import net.celestiald.cavesnotcliffs.block.BlockStalactite;
import net.celestiald.cavesnotcliffs.content.DripstoneSoundEvents;
import net.celestiald.cavesnotcliffs.handler.DripstoneDamageHandler;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

import java.util.List;

/** Internal falling entity needed for exact stalactite damage, filtering, and landing behavior. */
@ElementsCavesNotCliffs.ModElement.Tag
public final class EntityFallingPointedDripstone extends ElementsCavesNotCliffs.ModElement {
    public EntityFallingPointedDripstone(ElementsCavesNotCliffs elements) {
        super(elements, 205);
    }

    @Override
    public void initElements() {
        EntityRegistry.registerModEntity(CncEntity.ID, EntityCustom.class,
                "falling_pointed_dripstone", 1, CavesNotCliffs.instance, 64, 3, true);
    }

    public static final class CncEntity {
        public static final net.minecraft.util.ResourceLocation ID =
                new net.minecraft.util.ResourceLocation(
                        CavesNotCliffs.MODID, "falling_pointed_dripstone");

        private CncEntity() {
        }
    }

    public static final class EntityCustom extends EntityFallingBlock
            implements IEntityAdditionalSpawnData {
        private static final String DAMAGE_FACTOR_KEY = "CncStalactiteDamageFactor";
        private float damageFactor;

        public EntityCustom(World world) {
            super(world);
        }

        public EntityCustom(World world, double x, double y, double z,
                IBlockState state, float damageFactor) {
            super(world, x, y, z, state);
            this.damageFactor = damageFactor;
        }

        @Override
        public void fall(float distance, float multiplier) {
            if (damageFactor <= 0.0F) {
                return;
            }
            int wholeDistance = MathHelper.ceil(distance - 1.0F);
            if (wholeDistance <= 0) {
                return;
            }
            List<Entity> struck = world.getEntitiesWithinAABBExcludingEntity(this,
                    getEntityBoundingBox());
            float damage = Math.min(MathHelper.floor(wholeDistance * damageFactor), 40);
            for (Entity entity : struck) {
                if (!(entity instanceof EntityLivingBase)
                        || !((EntityLivingBase) entity).isEntityAlive()) {
                    continue;
                }
                if (entity instanceof EntityPlayer
                        && (((EntityPlayer) entity).isSpectator()
                        || ((EntityPlayer) entity).capabilities.isCreativeMode)) {
                    continue;
                }
                entity.attackEntityFrom(DripstoneDamageHandler.FALLING_STALACTITE, damage);
            }
        }

        @Override
        public void onUpdate() {
            boolean alive = !isDead;
            super.onUpdate();
            if (world.isRemote || !alive || !isDead || !onGround || fallTime <= 1) {
                return;
            }
            BlockPos landing = new BlockPos(this);
            IBlockState state = world.getBlockState(landing);
            if (state.getBlock() instanceof BlockPointedDripstone) {
                EnumFacing direction = state.getValue(BlockPointedDripstone.TIP_DIRECTION);
                if (BlockPointedDripstone.validPlacement(world, landing, direction)) {
                    return;
                }
                world.setBlockToAir(landing);
                Item canonical = BlockStalactite.block == null
                        ? null : Item.getItemFromBlock(BlockStalactite.block);
                if (canonical != null) {
                    Block.spawnAsEntity(world, landing, new ItemStack(canonical));
                }
            }
            world.playSound(null, landing, DripstoneSoundEvents.POINTED_LAND,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);
        }

        @Override
        protected void writeEntityToNBT(NBTTagCompound compound) {
            super.writeEntityToNBT(compound);
            compound.setFloat(DAMAGE_FACTOR_KEY, damageFactor);
        }

        @Override
        protected void readEntityFromNBT(NBTTagCompound compound) {
            super.readEntityFromNBT(compound);
            damageFactor = compound.getFloat(DAMAGE_FACTOR_KEY);
        }

        @Override
        public void writeSpawnData(ByteBuf buffer) {
            buffer.writeInt(Block.getStateId(getBlock()));
            buffer.writeFloat(damageFactor);
        }

        @Override
        public void readSpawnData(ByteBuf additionalData) {
            IBlockState state = Block.getStateById(additionalData.readInt());
            NBTTagCompound compound = new NBTTagCompound();
            net.minecraft.util.ResourceLocation id =
                    Block.REGISTRY.getNameForObject(state.getBlock());
            compound.setString("Block", id == null ? "" : id.toString());
            compound.setByte("Data", (byte) state.getBlock().getMetaFromState(state));
            compound.setFloat(DAMAGE_FACTOR_KEY, additionalData.readFloat());
            readEntityFromNBT(compound);
        }

        public float getDamageFactor() {
            return damageFactor;
        }
    }
}

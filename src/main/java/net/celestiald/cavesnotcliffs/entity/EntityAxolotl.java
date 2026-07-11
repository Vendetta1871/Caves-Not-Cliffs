
package net.celestiald.cavesnotcliffs.entity;

import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraft.block.material.Material;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.world.World;
import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.CavesNotCliffs;

import java.util.Random;

@ElementsCavesNotCliffs.ModElement.Tag
public class EntityAxolotl extends ElementsCavesNotCliffs.ModElement {

    public EntityAxolotl(ElementsCavesNotCliffs instance) {
        super(instance, 67);
    }

    @Override
    public void initElements() {
        EntityRegistry.registerModEntity(
            new net.minecraft.util.ResourceLocation("cavesnotcliffs:axolotl"),
            EntityCustom.class, "axolotl", 0, CavesNotCliffs.instance, 64, 3, true);
    }

    public static class EntityCustom extends EntityWaterMob {

        public EntityCustom(World world) {
            super(world);
            this.setSize(0.75f, 0.42f);
        }

        @Override
        protected void initEntityAI() {
            this.tasks.addTask(0, new EntityAISwimming(this));
            this.tasks.addTask(1, new EntityAIAxolotlSwim(this));
            this.tasks.addTask(2, new EntityAILookIdle(this));
        }

        @Override
        protected void applyEntityAttributes() {
            super.applyEntityAttributes();
            this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(14.0);
            this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.2);
        }

        @Override
        public boolean getCanSpawnHere() {
            return world.getBlockState(getPosition()).getMaterial() == Material.WATER
                    && world.checkNoEntityCollision(getEntityBoundingBox());
        }

        @Override
        public void travel(float strafe, float vertical, float forward) {
            if (isInWater()) {
                moveRelative(strafe, vertical, forward, 0.10F);
                move(MoverType.SELF, motionX, motionY, motionZ);
                motionX *= 0.90D;
                motionY *= 0.90D;
                motionZ *= 0.90D;
                if (getAttackTarget() == null) {
                    motionY -= 0.005D;
                }
            } else {
                super.travel(strafe, vertical, forward);
            }
        }

        private static final class EntityAIAxolotlSwim extends EntityAIBase {
            private final EntityCustom axolotl;

            EntityAIAxolotlSwim(EntityCustom axolotl) {
                this.axolotl = axolotl;
            }

            @Override
            public boolean shouldExecute() {
                return axolotl.isInWater() && axolotl.getRNG().nextInt(12) == 0;
            }

            @Override
            public void startExecuting() {
                Random random = axolotl.getRNG();
                axolotl.motionX += (random.nextDouble() - 0.5D) * 0.18D;
                axolotl.motionY += (random.nextDouble() - 0.5D) * 0.10D;
                axolotl.motionZ += (random.nextDouble() - 0.5D) * 0.18D;
            }
        }
    }
}

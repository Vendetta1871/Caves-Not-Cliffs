
package net.mcreator.cavesnotcliffs.entity;

import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.world.World;
import net.mcreator.cavesnotcliffs.ElementsCavesNotCliffs;
import net.mcreator.cavesnotcliffs.CavesNotCliffs;

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

    public static class EntityCustom extends EntityAnimal {

        public EntityCustom(World world) {
            super(world);
            this.setSize(0.75f, 0.42f);
        }

        @Override
        protected void initEntityAI() {
            this.tasks.addTask(0, new EntityAISwimming(this));
            this.tasks.addTask(1, new EntityAIWander(this, 1.0));
            this.tasks.addTask(2, new EntityAILookIdle(this));
        }

        @Override
        protected void applyEntityAttributes() {
            super.applyEntityAttributes();
            this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(14.0);
            this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.2);
        }

        @Override
        public EntityCustom createChild(EntityAgeable mate) {
            return new EntityCustom(this.world);
        }
    }
}

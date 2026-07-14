package net.celestiald.cavesnotcliffs.content;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.UUID;

/** Resolves NeutralMob-style persistent anger UUIDs without assuming a player target. */
public final class BeeAngerTargetResolver {
    private BeeAngerTargetResolver() {
    }

    @Nullable
    public static EntityLivingBase resolve(World world, @Nullable UUID targetId) {
        if (world == null || targetId == null) {
            return null;
        }
        if (world instanceof WorldServer) {
            EntityLivingBase direct = asLivingTarget(
                    ((WorldServer) world).getEntityFromUuid(targetId), targetId);
            if (direct != null) {
                return direct;
            }
        }
        return findLoaded(world.loadedEntityList, targetId);
    }

    @Nullable
    public static EntityLivingBase findLoaded(Iterable<? extends Entity> entities,
            UUID targetId) {
        for (Entity entity : entities) {
            EntityLivingBase target = asLivingTarget(entity, targetId);
            if (target != null) {
                return target;
            }
        }
        return null;
    }

    @Nullable
    private static EntityLivingBase asLivingTarget(@Nullable Entity entity,
            UUID targetId) {
        return entity instanceof EntityLivingBase && !entity.isDead
                && targetId.equals(entity.getUniqueID())
                ? (EntityLivingBase) entity : null;
    }
}

package net.celestiald.cavesnotcliffs.handler;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.block.BlockPointedDripstone;
import net.celestiald.cavesnotcliffs.dripstone.PointedDripstoneMechanics;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Locale;

/** Lets a present or future 1.12 trident bridge break pointed dripstone at the 1.18 threshold. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class DripstoneProjectileHandler {
    private DripstoneProjectileHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        RayTraceResult hit = event.getRayTraceResult();
        Entity projectile = event.getEntity();
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK
                || !isTrident(projectile)
                || !PointedDripstoneMechanics.shouldBreakFromTrident(Math.sqrt(
                        projectile.motionX * projectile.motionX
                                + projectile.motionY * projectile.motionY
                                + projectile.motionZ * projectile.motionZ))) {
            return;
        }
        World world = projectile.world;
        BlockPos pos = hit.getBlockPos();
        IBlockState state = world.getBlockState(pos);
        if (!world.isRemote && state.getBlock() instanceof BlockPointedDripstone
                && state.getBlock().canEntityDestroy(state, world, pos, projectile)) {
            world.destroyBlock(pos, true);
        }
    }

    static boolean isTrident(Entity entity) {
        ResourceLocation key = EntityList.getKey(entity);
        return isTridentIdentity(key == null ? null : key.getResourcePath(),
                entity.getClass().getSimpleName());
    }

    public static boolean isTridentIdentity(String registryPath, String className) {
        return containsTrident(registryPath) || containsTrident(className);
    }

    private static boolean containsTrident(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).contains("trident");
    }
}

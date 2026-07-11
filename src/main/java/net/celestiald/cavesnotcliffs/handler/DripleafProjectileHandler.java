package net.celestiald.cavesnotcliffs.handler;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.block.LushDripleafBlocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Bridges 1.12 projectile impacts to the 1.18.2 immediate full-tilt transition. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class DripleafProjectileHandler {
    private DripleafProjectileHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        RayTraceResult hit = event.getRayTraceResult();
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
            return;
        }
        World world = event.getEntity().world;
        BlockPos pos = hit.getBlockPos();
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof LushDripleafBlocks.Head) {
            ((LushDripleafBlocks.Head) state.getBlock())
                    .projectileHit(world, pos, state);
        }
    }
}

package net.celestiald.cavesnotcliffs.handler;

import net.celestiald.cavesnotcliffs.content.CampfireContent;
import net.celestiald.cavesnotcliffs.content.CampfireMechanics;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Lights dry campfires at the actual impact point of a burning projectile. */
public final class CampfireProjectileHandler {
    public static final CampfireProjectileHandler INSTANCE = new CampfireProjectileHandler();

    private CampfireProjectileHandler() {
    }

    @SubscribeEvent
    public void onProjectileImpact(ProjectileImpactEvent event) {
        RayTraceResult hit = event.getRayTraceResult();
        Entity projectile = event.getEntity();
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK
                || !projectile.isBurning()) {
            return;
        }
        World world = projectile.world;
        if (world.isRemote) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        IBlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof CampfireContent.BlockCustom)
                || !CampfireMechanics.canLight(
                    state.getValue(CampfireContent.BlockCustom.LIT),
                    state.getValue(CampfireContent.BlockCustom.WATERLOGGED))) {
            return;
        }
        world.setBlockState(pos,
            state.withProperty(CampfireContent.BlockCustom.LIT, true), 11);
    }
}

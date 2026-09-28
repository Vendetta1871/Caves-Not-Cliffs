package net.celestiald.cavesnotcliffs.mixin;

import net.celestiald.cavesnotcliffs.content.LightningRodContent;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Moves a lightning strike onto the closest exposed lightning rod before the bolt ignites
 * anything. The 1.12 constructor sets the strike point and four random neighbours on fire right
 * after positioning the bolt, so redirecting it later — when it joins the world — still burned
 * the spot the rod was meant to protect. Java 1.17 picks the rod before the bolt exists; the
 * positioning call is the equivalent point here. Fire then only starts around the rod tip.
 *
 * <p>{@code require = 0}: if another mod redirects the same call, the join-world handler in
 * {@link LightningRodContent} still moves the bolt as before.</p>
 */
@Mixin(EntityLightningBolt.class)
public abstract class LightningBoltMixin {
    @Redirect(method = "<init>(Lnet/minecraft/world/World;DDDZ)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/entity/effect/EntityLightningBolt;setLocationAndAngles(DDDFF)V"),
            require = 0)
    private void cavesnotcliffs$strikeClosestRod(EntityLightningBolt bolt,
            double x, double y, double z, float yaw, float pitch) {
        BlockPos rod = LightningRodContent.resolveStrike(bolt, x, y, z);
        if (rod != null) {
            x = rod.getX() + 0.5D;
            y = rod.getY() + 1.0D;
            z = rod.getZ() + 0.5D;
        }
        bolt.setLocationAndAngles(x, y, z, yaw, pitch);
    }
}

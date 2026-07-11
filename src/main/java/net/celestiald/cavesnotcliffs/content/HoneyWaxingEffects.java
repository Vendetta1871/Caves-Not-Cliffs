package net.celestiald.cavesnotcliffs.content;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/** 1.12 particle bridge for level event 3003 (wax-on sound and 3..5 sparks per face). */
public final class HoneyWaxingEffects {
    private HoneyWaxingEffects() {
    }

    public static void play(World world, BlockPos pos) {
        if (world.isRemote) {
            return;
        }
        world.playSound(null, pos, HoneySoundEvents.WAX_ON,
                SoundCategory.BLOCKS, 1.0F, 1.0F);
        if (!(world instanceof WorldServer)) {
            return;
        }
        WorldServer server = (WorldServer) world;
        for (EnumFacing face : EnumFacing.values()) {
            int count = 3 + world.rand.nextInt(3);
            for (int index = 0; index < count; index++) {
                double x = pos.getX() + 0.5D
                        + (face.getFrontOffsetX() == 0
                            ? world.rand.nextDouble() - 0.5D
                            : face.getFrontOffsetX() * 0.55D);
                double y = pos.getY() + 0.5D
                        + (face.getFrontOffsetY() == 0
                            ? world.rand.nextDouble() - 0.5D
                            : face.getFrontOffsetY() * 0.55D);
                double z = pos.getZ() + 0.5D
                        + (face.getFrontOffsetZ() == 0
                            ? world.rand.nextDouble() - 0.5D
                            : face.getFrontOffsetZ() * 0.55D);
                double velocityX = face.getFrontOffsetX() == 0
                        ? world.rand.nextDouble() * 2.0D - 1.0D : 0.0D;
                double velocityY = face.getFrontOffsetY() == 0
                        ? world.rand.nextDouble() * 2.0D - 1.0D : 0.0D;
                double velocityZ = face.getFrontOffsetZ() == 0
                        ? world.rand.nextDouble() * 2.0D - 1.0D : 0.0D;
                // SPELL_INSTANT is the closest 1.12 sprite to 1.18's WAX_ON particle.
                server.spawnParticle(EnumParticleTypes.SPELL_INSTANT,
                        x, y, z, 0, velocityX, velocityY, velocityZ, 1.0D);
            }
        }
    }
}

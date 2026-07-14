package net.celestiald.cavesnotcliffs.content;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/** Exact ParticleUtils face positions/counts using the target runtime's particle packets. */
public final class BlockFaceParticleEffects {
    private BlockFaceParticleEffects() {
    }

    public static void spawn(World world, BlockPos pos, EnumParticleTypes type,
            int... arguments) {
        if (!(world instanceof WorldServer)) {
            return;
        }
        WorldServer server = (WorldServer) world;
        for (EnumFacing face : EnumFacing.values()) {
            int count = sampleFaceCount(world.rand);
            for (int index = 0; index < count; index++) {
                double x = pos.getX() + 0.5D + coordinateOffset(world,
                        face.getFrontOffsetX());
                double y = pos.getY() + 0.5D + coordinateOffset(world,
                        face.getFrontOffsetY());
                double z = pos.getZ() + 0.5D + coordinateOffset(world,
                        face.getFrontOffsetZ());
                double velocityX = velocity(world, face.getFrontOffsetX());
                double velocityY = velocity(world, face.getFrontOffsetY());
                double velocityZ = velocity(world, face.getFrontOffsetZ());
                server.spawnParticle(type, x, y, z, 0,
                        velocityX, velocityY, velocityZ, 1.0D, arguments);
            }
        }
    }

    public static int sampleFaceCount(java.util.Random random) {
        return 3 + random.nextInt(3);
    }

    private static double coordinateOffset(World world, int direction) {
        return direction == 0 ? world.rand.nextDouble() - 0.5D
                : direction * 0.55D;
    }

    private static double velocity(World world, int direction) {
        return direction == 0 ? world.rand.nextDouble() * 2.0D - 1.0D : 0.0D;
    }
}

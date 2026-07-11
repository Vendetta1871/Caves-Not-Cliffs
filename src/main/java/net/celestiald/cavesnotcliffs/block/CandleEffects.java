package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.content.CandleMechanics;
import net.celestiald.cavesnotcliffs.content.CandleSoundEvents;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.Random;

/** Exact shared flame, ambient, and extinguishing effects for candle-family blocks. */
public final class CandleEffects {
    private CandleEffects() {
    }

    public static boolean light(World world, BlockPos pos, IBlockState state) {
        if (!(state.getBlock() instanceof CandleLightable)) {
            return false;
        }
        CandleLightable block = (CandleLightable) state.getBlock();
        if (!block.canLight(state)) {
            return false;
        }
        if (!world.isRemote) {
            world.setBlockState(pos, block.withLit(state, true), 11);
        }
        return true;
    }

    public static boolean extinguish(@Nullable EntityPlayer player, World world,
            BlockPos pos, IBlockState state) {
        if (!(state.getBlock() instanceof CandleLightable)) {
            return false;
        }
        CandleLightable block = (CandleLightable) state.getBlock();
        if (!block.isLit(state)) {
            return false;
        }
        if (!world.isRemote) {
            world.setBlockState(pos, block.withLit(state, false), 11);
        }
        for (CandleMechanics.ParticleOffset offset : block.particleOffsets(state)) {
            spawnSmoke(world, pos.getX() + offset.x, pos.getY() + offset.y,
                    pos.getZ() + offset.z, 0.1D);
        }
        world.playSound(player, pos, CandleSoundEvents.CANDLE_EXTINGUISH,
                SoundCategory.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    public static void animate(IBlockState state, World world, BlockPos pos,
            Random random) {
        if (!(state.getBlock() instanceof CandleLightable)) {
            return;
        }
        CandleLightable block = (CandleLightable) state.getBlock();
        if (!block.isLit(state)) {
            return;
        }
        for (CandleMechanics.ParticleOffset offset : block.particleOffsets(state)) {
            double x = pos.getX() + offset.x;
            double y = pos.getY() + offset.y;
            double z = pos.getZ() + offset.z;
            float roll = random.nextFloat();
            if (roll < 0.3F) {
                world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                        x, y, z, 0.0D, 0.0D, 0.0D);
                if (roll < 0.17F) {
                    world.playSound(x + 0.5D, y + 0.5D, z + 0.5D,
                            CandleSoundEvents.CANDLE_AMBIENT, SoundCategory.BLOCKS,
                            1.0F + random.nextFloat(),
                            random.nextFloat() * 0.7F + 0.3F, false);
                }
            }
            // 1.12 has no SMALL_FLAME particle; FLAME is its behaviorally equivalent sprite.
            world.spawnParticle(EnumParticleTypes.FLAME,
                    x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void spawnSmoke(World world, double x, double y, double z,
            double ySpeed) {
        if (world instanceof WorldServer) {
            ((WorldServer) world).spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    x, y, z, 1, 0.0D, ySpeed, 0.0D, 0.0D);
        } else {
            world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL,
                    x, y, z, 0.0D, ySpeed, 0.0D);
        }
    }
}

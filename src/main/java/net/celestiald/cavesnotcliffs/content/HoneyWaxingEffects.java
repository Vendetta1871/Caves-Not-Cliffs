package net.celestiald.cavesnotcliffs.content;

import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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
        // SPELL_INSTANT is the closest 1.12 sprite to 1.18's WAX_ON particle.
        BlockFaceParticleEffects.spawn(world, pos, EnumParticleTypes.SPELL_INSTANT);
    }
}

package net.celestiald.cavesnotcliffs.content;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Target-runtime particle bridge for level events 3004 (wax off) and 3005 (scrape). */
public final class CopperInteractionEffects {
    private CopperInteractionEffects() {
    }

    public static void spawn(World world, BlockPos pos, IBlockState oldState,
            boolean scrape) {
        if (scrape) {
            BlockFaceParticleEffects.spawn(world, pos,
                    EnumParticleTypes.BLOCK_CRACK, Block.getStateId(oldState));
        } else {
            BlockFaceParticleEffects.spawn(world, pos,
                    EnumParticleTypes.SPELL_INSTANT);
        }
    }
}

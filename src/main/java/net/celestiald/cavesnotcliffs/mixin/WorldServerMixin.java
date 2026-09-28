package net.celestiald.cavesnotcliffs.mixin;

import net.celestiald.cavesnotcliffs.world.WorldgenFallingBlocks;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drops the fall ticks sand and gravel schedule while a native 1.18 generator populates (see
 * {@link WorldgenFallingBlocks}). Every scheduled tick passes through here, from
 * {@code onBlockAdded} and {@code neighborChanged} alike, so falling-block subclasses that
 * override either callback are covered too.
 */
@Mixin(WorldServer.class)
public abstract class WorldServerMixin {
    @Inject(method = "updateBlockTick(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;II)V",
            at = @At("HEAD"), cancellable = true)
    private void cavesnotcliffs$keepWorldgenFallingBlocks(BlockPos pos, Block block,
            int delay, int priority, CallbackInfo callback) {
        if (WorldgenFallingBlocks.suppressesFallTick((World) (Object) this, block)) {
            callback.cancel();
        }
    }
}

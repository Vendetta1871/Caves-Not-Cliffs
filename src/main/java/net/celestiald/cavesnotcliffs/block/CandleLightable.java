package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.content.CandleMechanics;
import net.minecraft.block.state.IBlockState;

/** Narrow lighting contract shared by candles, candle cakes, items, and projectiles. */
public interface CandleLightable {
    boolean canLight(IBlockState state);

    IBlockState withLit(IBlockState state, boolean lit);

    boolean isLit(IBlockState state);

    Iterable<CandleMechanics.ParticleOffset> particleOffsets(IBlockState state);
}

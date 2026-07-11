package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.content.AmethystSoundEvents;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;

/** Shared full-block behavior for amethyst and budding amethyst. */
public class BlockAmethystBase extends Block implements AmethystChimeSource {
    public BlockAmethystBase(String name) {
        super(Material.ROCK, MapColor.PURPLE);
        setUnlocalizedName(name);
        setSoundType(AmethystSoundEvents.AMETHYST);
        setHardness(1.5F);
        // 1.12 internally scales setResistance by three and reports it divided by five.
        setResistance(2.5F);
    }
}

package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.Block;
import net.minecraftforge.fml.common.registry.GameRegistry;

/** Hidden block-only storage for the ten waterlogged pointed-dripstone states. */
@ElementsCavesNotCliffs.ModElement.Tag
public final class BlockPointedDripstoneWaterlogged extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:pointed_dripstone_waterlogged")
    public static final Block block = null;

    public BlockPointedDripstoneWaterlogged(ElementsCavesNotCliffs elements) {
        super(elements, 203);
    }

    @Override
    public void initElements() {
        BlockPointedDripstone companion = (BlockPointedDripstone)
                new BlockPointedDripstone(true)
                        .setRegistryName(CncRegistryIds.POINTED_DRIPSTONE_WATERLOGGED);
        elements.blocks.add(() -> companion);
    }
}

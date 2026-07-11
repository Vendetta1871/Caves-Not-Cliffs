package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.content.CncBlockProperties;
import net.celestiald.cavesnotcliffs.content.DripstoneSoundEvents;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.init.Items;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.Random;

/**
 * Block-only save alias for the released {@code stalactite} ID. Chunk migration converts it to
 * the canonical downward tip before normal gameplay can expose it.
 */
@ElementsCavesNotCliffs.ModElement.Tag
public final class BlockLegacyStalactiteAlias extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:stalactite")
    public static final Block block = null;

    public BlockLegacyStalactiteAlias(ElementsCavesNotCliffs elements) {
        super(elements, 204);
    }

    @Override
    public void initElements() {
        elements.blocks.add(() -> new LegacyBlock()
                .setRegistryName(CncRegistryIds.id("stalactite")));
    }

    private static final class LegacyBlock extends Block {
        private LegacyBlock() {
            super(Material.ROCK, MapColor.ADOBE);
            setUnlocalizedName("pointed_dripstone");
            setSoundType(DripstoneSoundEvents.POINTED_DRIPSTONE);
            setHardness(1.5F);
            setResistance(CncBlockProperties.legacyResistance(3.0F));
        }

        @Override
        public Item getItemDropped(IBlockState state, Random random, int fortune) {
            return BlockStalactite.block == null
                    ? Items.AIR : Item.getItemFromBlock(BlockStalactite.block);
        }
    }
}

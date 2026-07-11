package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.Random;

/**
 * Hidden marker registered under the released geode-shell ID.
 *
 * <p>Keeping the old block identity prevents Forge's registry remap from erasing the distinction
 * between legacy shell blocks and real v2 amethyst blocks before chunk migration can inspect it.
 * No ItemBlock is registered: old inventory stacks are remapped to {@code amethyst_block}.</p>
 */
@ElementsCavesNotCliffs.ModElement.Tag
public final class BlockLegacyAmethystGeode extends ElementsCavesNotCliffs.ModElement {
    @GameRegistry.ObjectHolder("cavesnotcliffs:amethyst_geode")
    public static final Block block = null;

    public BlockLegacyAmethystGeode(ElementsCavesNotCliffs elements) {
        super(elements, 201);
    }

    @Override
    public void initElements() {
        elements.blocks.add(() -> new MarkerBlock()
                .setRegistryName(CncRegistryIds.LEGACY_AMETHYST_GEODE));
    }

    private static final class MarkerBlock extends Block {
        private MarkerBlock() {
            super(Material.ROCK);
            setUnlocalizedName("legacy_amethyst_geode");
            setSoundType(SoundType.STONE);
            setHardness(1.5F);
            setResistance(6.0F);
        }

        @Override
        public Item getItemDropped(IBlockState state, Random random, int fortune) {
            Item canonical = ForgeRegistries.ITEMS.getValue(CncRegistryIds.AMETHYST_BLOCK);
            return canonical == null ? net.minecraft.init.Items.AIR : canonical;
        }
    }
}

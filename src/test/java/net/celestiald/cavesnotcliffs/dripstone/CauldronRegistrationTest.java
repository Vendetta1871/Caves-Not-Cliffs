package net.celestiald.cavesnotcliffs.dripstone;

import net.celestiald.cavesnotcliffs.ElementsCavesNotCliffs;
import net.celestiald.cavesnotcliffs.block.BlockLavaCauldron;
import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.BlockCauldron;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.block.state.IBlockState;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class CauldronRegistrationTest {
    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void allPurposeStorageIsBlockOnlyAndNormalizesLegacyLayeredLava() {
        ElementsCavesNotCliffs elements = new ElementsCavesNotCliffs();
        new BlockLavaCauldron(elements).initElements();
        assertEquals(1, elements.blocks.size());
        assertTrue(elements.items.isEmpty());
        BlockLavaCauldron.BlockCustom block =
                (BlockLavaCauldron.BlockCustom) elements.blocks.get(0).get();
        assertEquals(CncRegistryIds.LAVA_CAULDRON, block.getRegistryName());
        for (int meta = 4; meta <= 7; ++meta) {
            IBlockState lava = block.getStateFromMeta(meta);
            assertTrue(lava.getValue(BlockLavaCauldron.BlockCustom.IS_LAVA));
            assertEquals(Integer.valueOf(3), lava.getValue(BlockCauldron.LEVEL));
            assertEquals(7, block.getMetaFromState(lava));
            assertEquals(CauldronMechanics.lava(), block.mechanicsState(lava));
        }
        assertSame(Items.CAULDRON,
                block.getItemDropped(block.getDefaultState(), new java.util.Random(0L), 0));
    }

    @Test
    public void storageRoundTripsEveryRepresentableOfficialState() {
        BlockLavaCauldron.BlockCustom block = new BlockLavaCauldron.BlockCustom();
        assertEquals(CauldronMechanics.empty(),
                block.mechanicsState(block.blockState(CauldronMechanics.empty())));
        assertEquals(CauldronMechanics.lava(),
                block.mechanicsState(block.blockState(CauldronMechanics.lava())));
        for (int level = 1; level <= 3; ++level) {
            assertEquals(CauldronMechanics.water(level), block.mechanicsState(
                    block.blockState(CauldronMechanics.water(level))));
        }
    }
}

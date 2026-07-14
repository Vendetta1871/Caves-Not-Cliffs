package net.celestiald.cavesnotcliffs.dripstone;

import net.celestiald.cavesnotcliffs.block.BlockLavaCauldron;
import net.celestiald.cavesnotcliffs.block.BlockPowderSnowCauldron;
import net.celestiald.cavesnotcliffs.dripstone.CauldronMechanics.State;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class CauldronStateBridgeTest {
    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void vanillaLevelsZeroThroughThreeBridgeWithoutLosingWater() {
        BlockLavaCauldron.BlockCustom storage = new BlockLavaCauldron.BlockCustom();
        for (int level = 0; level <= 3; ++level) {
            IBlockState vanilla = Blocks.CAULDRON.getDefaultState()
                    .withProperty(BlockCauldron.LEVEL, level);
            IBlockState bridged = CauldronStateBridge.bridgeVanillaState(vanilla, storage);
            assertSame(storage, bridged.getBlock());
            assertEquals(level == 0 ? CauldronMechanics.empty()
                            : CauldronMechanics.water(level),
                    storage.mechanicsState(bridged));
        }
    }

    @Test
    public void bridgeOnlyRecognizesTheExactVanillaIdentity() {
        BlockLavaCauldron.BlockCustom storage = new BlockLavaCauldron.BlockCustom();
        IBlockState unrelated = Blocks.CHEST.getDefaultState();
        IBlockState moddedCauldron = new BlockCauldron().getDefaultState();

        assertFalse(CauldronStateBridge.isVanillaCauldron(unrelated));
        assertFalse(CauldronStateBridge.isVanillaCauldron(moddedCauldron));
        assertSame(unrelated,
                CauldronStateBridge.bridgeVanillaState(unrelated, storage));
        assertSame(moddedCauldron,
                CauldronStateBridge.bridgeVanillaState(moddedCauldron, storage));
    }

    @Test
    public void hiddenStorageRoundTripsEveryOfficialContentState() {
        BlockLavaCauldron.BlockCustom primary = new BlockLavaCauldron.BlockCustom();
        BlockPowderSnowCauldron.BlockCustom powder =
                new BlockPowderSnowCauldron.BlockCustom();
        State[] states = {
                CauldronMechanics.empty(),
                CauldronMechanics.water(1),
                CauldronMechanics.water(2),
                CauldronMechanics.water(3),
                CauldronMechanics.lava(),
                CauldronMechanics.powderSnow(1),
                CauldronMechanics.powderSnow(2),
                CauldronMechanics.powderSnow(3)
        };

        for (State state : states) {
            IBlockState stored = CauldronStateBridge.stateFor(state, primary, powder);
            if (state.content == CauldronMechanics.Content.POWDER_SNOW) {
                assertSame(powder, stored.getBlock());
                assertEquals(Integer.valueOf(state.level), stored.getValue(BlockCauldron.LEVEL));
            } else {
                assertSame(primary, stored.getBlock());
                assertEquals(state, primary.mechanicsState(stored));
            }
        }
    }

    @Test
    public void mutationUsesFlagThreeAndAlwaysRefreshesComparators() {
        final List<Object> calls = new ArrayList<>();
        final IBlockState next = new BlockLavaCauldron.BlockCustom().getDefaultState();
        CauldronStateBridge.UpdateAccess access = new CauldronStateBridge.UpdateAccess() {
            @Override
            public boolean set(IBlockState state, int flags) {
                calls.add(state);
                calls.add(flags);
                return false;
            }

            @Override
            public void updateComparator(Block block) {
                calls.add(block);
            }
        };

        assertFalse(CauldronStateBridge.setState(access, next));
        assertEquals(3, calls.size());
        assertSame(next, calls.get(0));
        assertEquals(CauldronStateBridge.UPDATE_FLAGS, calls.get(1));
        assertSame(next.getBlock(), calls.get(2));
        assertEquals(3, CauldronStateBridge.UPDATE_FLAGS);
    }

    @Test(expected = IllegalStateException.class)
    public void missingPowderStorageFailsInsteadOfDeletingItsContents() {
        CauldronStateBridge.stateFor(CauldronMechanics.powderSnow(3),
                new BlockLavaCauldron.BlockCustom(), null);
    }
}

package net.celestiald.cavesnotcliffs.block;

import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BlockInfestedDeepslateTest {
    @BeforeClass
    public static void bootstrap() {
        Bootstrap.register();
    }

    @Test
    public void matchesCanonicalHostPropertiesAndHasNoOrdinaryDrop() {
        BlockInfestedDeepslate.BlockCustom block =
            new BlockInfestedDeepslate.BlockCustom();
        assertTrue(block instanceof BlockRotatedPillar);
        assertEquals(1.5F, block.getBlockHardness(block.getDefaultState(), null, null), 0.0F);
        assertEquals(0.75F, block.getExplosionResistance(null), 0.0F);
        assertEquals(0, block.quantityDropped(new java.util.Random(0L)));
        assertEquals(Items.AIR, block.getItemDropped(block.getDefaultState(),
            new java.util.Random(0L), 0));
        assertFalse(block.getDefaultState().getPropertyKeys().isEmpty());
    }

    @Test
    public void canonicalResourcesAndRegistryIdAreComplete() {
        assertEquals("cavesnotcliffs:infested_deepslate",
            CncRegistryIds.INFESTED_DEEPSLATE.toString());
        ClassLoader loader = getClass().getClassLoader();
        assertNotNull(loader.getResource(
            "assets/cavesnotcliffs/blockstates/infested_deepslate.json"));
        assertNotNull(loader.getResource(
            "assets/cavesnotcliffs/models/block/infested_deepslate.json"));
        assertNotNull(loader.getResource(
            "assets/cavesnotcliffs/models/item/infested_deepslate.json"));
    }
}

package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.registry.CncRegistryIds;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.ResourceLocation;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlainPumpkinConnectionHooksTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void onlyTheCanonicalPlainPumpkinIdIsAnException() {
        assertTrue(PlainPumpkinConnectionHooks.isPlainPumpkin(
                new NamedBlock(CncRegistryIds.PUMPKIN)));
        assertFalse(PlainPumpkinConnectionHooks.isPlainPumpkin(
                new NamedBlock(new ResourceLocation("minecraft", "pumpkin"))));
        assertFalse(PlainPumpkinConnectionHooks.isPlainPumpkin(
                new NamedBlock(new ResourceLocation("minecraft", "melon_block"))));
        assertFalse(PlainPumpkinConnectionHooks.isPlainPumpkin(
                new NamedBlock(new ResourceLocation("other", "pumpkin"))));
        assertFalse(PlainPumpkinConnectionHooks.isPlainPumpkin(null));
    }

    private static final class NamedBlock extends Block {
        private NamedBlock(ResourceLocation name) {
            super(Material.GOURD);
            setRegistryName(name);
        }
    }
}

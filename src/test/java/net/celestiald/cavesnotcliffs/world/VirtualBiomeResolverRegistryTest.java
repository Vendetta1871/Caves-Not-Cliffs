package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.minecraft.init.Biomes;
import net.minecraft.init.Bootstrap;
import net.minecraft.world.biome.Biome;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertSame;

public class VirtualBiomeResolverRegistryTest {
    private static Biome magicalForest;

    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
        magicalForest = new Biome(new Biome.BiomeProperties("Magical Forest")) {
        };
        magicalForest.setRegistryName("thaumcraft", "magical_forest");
    }

    @Test
    public void moddedOverlayBiomeWinsOverSurfaceVirtualBiomes() {
        assertSame(magicalForest, VirtualBiomeResolverRegistry.resolveWithOverlay(
            V118Biome.SAVANNA, magicalForest, Biomes.SAVANNA));
        assertSame(magicalForest, VirtualBiomeResolverRegistry.resolveWithOverlay(
            V118Biome.FROZEN_PEAKS, magicalForest, Biomes.ICE_MOUNTAINS));
    }

    @Test
    public void caveVirtualBiomesStillOverrideTheOverlayClaimUnderground() {
        assertSame(Biomes.FOREST, VirtualBiomeResolverRegistry.resolveWithOverlay(
            V118Biome.LUSH_CAVES, magicalForest, Biomes.FOREST));
        assertSame(Biomes.EXTREME_HILLS, VirtualBiomeResolverRegistry.resolveWithOverlay(
            V118Biome.DRIPSTONE_CAVES, magicalForest, Biomes.EXTREME_HILLS));
    }

    @Test
    public void vanillaBaseNeverTriggersTheOverlayRule() {
        assertSame(Biomes.SAVANNA, VirtualBiomeResolverRegistry.resolveWithOverlay(
            V118Biome.SAVANNA, Biomes.PLAINS, Biomes.SAVANNA));
    }

    @Test
    public void ownMountainBiomeBaseKeepsTheVirtualProjection() {
        Biome meadow = new Biome(new Biome.BiomeProperties("Meadow")) {
        };
        meadow.setRegistryName("cavesnotcliffs", "meadow");
        assertSame(Biomes.ICE_MOUNTAINS, VirtualBiomeResolverRegistry.resolveWithOverlay(
            V118Biome.FROZEN_PEAKS, meadow, Biomes.ICE_MOUNTAINS));
    }
}

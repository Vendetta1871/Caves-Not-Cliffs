package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.minecraft.init.Biomes;
import net.minecraft.init.Bootstrap;
import net.minecraft.world.biome.Biome;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ModdedBiomeOverlayTest {
    private static Biome magicalForest;

    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
        magicalForest = new Biome(new Biome.BiomeProperties("Magical Forest")) {
        };
        magicalForest.setRegistryName("thaumcraft", "magical_forest");
    }

    @Test
    public void moddedCellsOverrideAndVanillaCellsKeepTheProjection() {
        Biome[] vanillaGrid = new Biome[] {
                magicalForest, Biomes.FOREST, null, Biomes.DESERT };
        ModdedBiomeOverlay overlay = ModdedBiomeOverlay.of(fixedSampler(
                vanillaGrid, null));

        Biome[] base = new Biome[] {
                Biomes.PLAINS, Biomes.PLAINS, Biomes.PLAINS, Biomes.PLAINS };
        Biome[] result = overlay.overlayForGeneration(base, null, 0, 0, 2, 2);

        assertSame(magicalForest, result[0]);
        assertSame(Biomes.PLAINS, result[1]);
        assertSame(Biomes.PLAINS, result[2]);
        assertSame(Biomes.PLAINS, result[3]);
    }

    @Test
    public void blockOverlaySubstitutesOnlyModdedCells() {
        Biome[] vanillaGrid = new Biome[] {
                Biomes.FOREST, magicalForest, magicalForest, Biomes.DESERT };
        ModdedBiomeOverlay overlay = ModdedBiomeOverlay.of(fixedSampler(
                null, vanillaGrid));

        Biome[] base = new Biome[] {
                Biomes.PLAINS, Biomes.PLAINS, Biomes.PLAINS, Biomes.PLAINS };
        Biome[] result = overlay.overlayBlock(base, null, 0, 0, 2, 2);

        assertSame(Biomes.PLAINS, result[0]);
        assertSame(magicalForest, result[1]);
        assertSame(magicalForest, result[2]);
        assertSame(Biomes.PLAINS, result[3]);
    }

    @Test
    public void moddedBlockBiomesKeepsOnlyModdedCells() {
        Biome[] vanillaGrid = new Biome[] {
                Biomes.FOREST, magicalForest, null, Biomes.DESERT };
        ModdedBiomeOverlay overlay = ModdedBiomeOverlay.of(fixedSampler(
                null, vanillaGrid));

        Biome[] modded = overlay.moddedBlockBiomes(null, 0, 0, 2, 2);

        assertNull(modded[0]);
        assertSame(magicalForest, modded[1]);
        assertNull(modded[2]);
        assertNull(modded[3]);
    }

    @Test
    public void moddedBiomeAtReturnsOnlyTheSampledCell() {
        ModdedBiomeOverlay overlay = ModdedBiomeOverlay.of(fixedSampler(
                null, new Biome[] { magicalForest }));
        assertSame(magicalForest, overlay.moddedBiomeAt(null, 16, 16));

        ModdedBiomeOverlay vanilla = ModdedBiomeOverlay.of(fixedSampler(
                null, new Biome[] { Biomes.FOREST }));
        assertNull(vanilla.moddedBiomeAt(null, 16, 16));

        assertNull(ModdedBiomeOverlay.disabled().moddedBiomeAt(null, 16, 16));
    }

    @Test
    public void disabledOverlayPassesEverythingThrough() {
        ModdedBiomeOverlay overlay = ModdedBiomeOverlay.disabled();
        assertFalse(overlay.isEnabled());

        Biome[] base = new Biome[] { Biomes.PLAINS, Biomes.PLAINS };
        assertArrayEquals(new Biome[] { Biomes.PLAINS, Biomes.PLAINS },
                overlay.overlayForGeneration(base, null, 0, 0, 1, 2));
        assertArrayEquals(new Biome[] { Biomes.PLAINS, Biomes.PLAINS },
                overlay.overlayBlock(base, null, 0, 0, 1, 2));
        assertNull(overlay.moddedBlockBiomes(null, 0, 0, 1, 2));
    }

    @Test
    public void failingSamplerDisablesTheOverlayWithoutThrowing() {
        ModdedBiomeOverlay overlay = ModdedBiomeOverlay.of(new ModdedBiomeOverlay.Sampler() {
            @Override
            public Biome[] generationBiomes(int quartX, int quartZ, int width, int height) {
                throw new RuntimeException("broken mod biome layer");
            }

            @Override
            public Biome[] blockBiomes(int blockX, int blockZ, int width, int length) {
                throw new RuntimeException("broken mod biome layer");
            }
        });
        assertTrue(overlay.isEnabled());

        Biome[] base = new Biome[] { Biomes.PLAINS, Biomes.PLAINS };
        Biome[] result = overlay.overlayForGeneration(base, null, 0, 0, 1, 2);
        assertArrayEquals(new Biome[] { Biomes.PLAINS, Biomes.PLAINS }, result);
        assertFalse(overlay.isEnabled());
        // A disabled overlay stays quiet and never touches the base grid again.
        assertArrayEquals(new Biome[] { Biomes.PLAINS, Biomes.PLAINS },
                overlay.overlayBlock(base, null, 0, 0, 1, 2));
    }

    @Test
    public void biomeWithoutRegistryNameIsNotTreatedAsModded() {
        Biome anonymous = new Biome(new Biome.BiomeProperties("Nameless")) {
        };
        assertFalse(ModdedBiomeOverlay.isModded(anonymous));
        assertTrue(ModdedBiomeOverlay.isModded(magicalForest));
        assertFalse(ModdedBiomeOverlay.isModded(Biomes.FOREST));
    }

    @Test
    public void ownProjectionBiomesAreNotTreatedAsModded() {
        Biome meadow = new Biome(new Biome.BiomeProperties("Meadow")) {
        };
        meadow.setRegistryName("cavesnotcliffs", "meadow");
        assertFalse(ModdedBiomeOverlay.isModded(meadow));
    }

    @Test
    public void climateHostedClaimsOnlyReplaceFittingHostsInWholeRegions() {
        ModdedBiomeOverlay overlay = ModdedBiomeOverlay.climateHosted(
                new ModdedBiomeRegions(42L, 512), () -> forestOnlyTable(0.0D));
        assertTrue(overlay.isClimateHosted());

        // With no vanilla weight every forest column is claimed and nothing else is.
        assertSame(magicalForest, overlay.claim(V118Biome.FOREST, 100, -300));
        assertNull(overlay.claim(V118Biome.PLAINS, 100, -300));
        assertNull(overlay.claim(null, 100, -300));

        V118Biome[] hosts = new V118Biome[] {
                V118Biome.FOREST, V118Biome.PLAINS, V118Biome.FOREST, V118Biome.RIVER };
        Biome[] claims = overlay.moddedBlockBiomes(hosts, 0, 0, 2, 2);
        assertSame(magicalForest, claims[0]);
        assertNull(claims[1]);
        assertSame(magicalForest, claims[2]);
        assertNull(claims[3]);
    }

    @Test
    public void climateHostedClaimsAreSharedAcrossARegion() {
        ModdedBiomeRegions regions = new ModdedBiomeRegions(99L, 1024);
        ModdedBiomeOverlay overlay = ModdedBiomeOverlay.climateHosted(regions,
                () -> forestOnlyTable(10.0D));
        int claimed = 0;
        int samples = 0;
        for (int x = -20_000; x < 20_000; x += 97) {
            int z = x / 3;
            Biome claim = overlay.claim(V118Biome.FOREST, x, z);
            // Two columns of one region always agree.
            if (regions.regionAt(x, z) == regions.regionAt(x + 1, z)) {
                assertSame(claim, overlay.claim(V118Biome.FOREST, x + 1, z));
            }
            claimed += claim == null ? 0 : 1;
            ++samples;
        }
        // Equal weights: about half of the forest regions stay 1.18 forest.
        double share = claimed / (double) samples;
        assertTrue("claimed share " + share, share > 0.25D && share < 0.75D);
    }

    @Test
    public void climateHostedLayoutIgnoresTheLegacyChainAndGenerationGridsFollowHosts() {
        ModdedBiomeOverlay overlay = ModdedBiomeOverlay.climateHosted(
                new ModdedBiomeRegions(5L, 256), () -> forestOnlyTable(0.0D));
        Biome[] base = new Biome[] { Biomes.FOREST, Biomes.PLAINS };
        Biome[] result = overlay.overlayForGeneration(base,
                new V118Biome[] { V118Biome.FOREST, V118Biome.PLAINS }, 0, 0, 2, 1);
        assertSame(magicalForest, result[0]);
        assertSame(Biomes.PLAINS, result[1]);
        // Without host information nothing can be claimed.
        Biome[] untouched = overlay.overlayBlock(new Biome[] { Biomes.FOREST }, null, 0, 0, 1, 1);
        assertSame(Biomes.FOREST, untouched[0]);
    }

    @Test
    public void failingHostTableLeavesTheProjectionAlone() {
        ModdedBiomeOverlay overlay = ModdedBiomeOverlay.climateHosted(
                new ModdedBiomeRegions(5L, 256), () -> {
                    throw new IllegalStateException("broken biome mod");
                });
        overlay.prepare();
        assertNull(overlay.claim(V118Biome.FOREST, 0, 0));
        assertNull(overlay.moddedBlockBiomes(new V118Biome[] { V118Biome.FOREST }, 0, 0, 1, 1));
    }

    private static ModdedBiomeHosts forestOnlyTable(double vanillaWeight) {
        Map<Biome, List<V118Biome>> pinned = new IdentityHashMap<Biome, List<V118Biome>>();
        pinned.put(magicalForest, Collections.singletonList(V118Biome.FOREST));
        ModdedBiomeHosts table = ModdedBiomeHosts.build(
                Collections.singletonMap(magicalForest, 10.0D), pinned, vanillaWeight,
                biome -> null);
        assertNotNull(table);
        assertEquals(1, table.candidates(V118Biome.FOREST).size());
        return table;
    }

    private static ModdedBiomeOverlay.Sampler fixedSampler(
            Biome[] generationGrid, Biome[] blockGrid) {
        return new ModdedBiomeOverlay.Sampler() {
            @Override
            public Biome[] generationBiomes(int quartX, int quartZ, int width, int height) {
                return Arrays.copyOf(generationGrid, generationGrid.length);
            }

            @Override
            public Biome[] blockBiomes(int blockX, int blockZ, int width, int length) {
                return Arrays.copyOf(blockGrid, blockGrid.length);
            }
        };
    }
}

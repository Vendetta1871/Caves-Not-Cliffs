package net.celestiald.cavesnotcliffs.world;

import net.minecraft.init.Biomes;
import net.minecraft.init.Bootstrap;
import net.minecraft.world.biome.Biome;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
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
        Biome[] result = overlay.overlayForGeneration(base, 0, 0, 2, 2);

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
        Biome[] result = overlay.overlayBlock(base, 0, 0, 2, 2);

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

        Biome[] modded = overlay.moddedBlockBiomes(0, 0, 2, 2);

        assertNull(modded[0]);
        assertSame(magicalForest, modded[1]);
        assertNull(modded[2]);
        assertNull(modded[3]);
    }

    @Test
    public void moddedBiomeAtReturnsOnlyTheSampledCell() {
        ModdedBiomeOverlay overlay = ModdedBiomeOverlay.of(fixedSampler(
                null, new Biome[] { magicalForest }));
        assertSame(magicalForest, overlay.moddedBiomeAt(16, 16));

        ModdedBiomeOverlay vanilla = ModdedBiomeOverlay.of(fixedSampler(
                null, new Biome[] { Biomes.FOREST }));
        assertNull(vanilla.moddedBiomeAt(16, 16));

        assertNull(ModdedBiomeOverlay.disabled().moddedBiomeAt(16, 16));
    }

    @Test
    public void disabledOverlayPassesEverythingThrough() {
        ModdedBiomeOverlay overlay = ModdedBiomeOverlay.disabled();
        assertFalse(overlay.isEnabled());

        Biome[] base = new Biome[] { Biomes.PLAINS, Biomes.PLAINS };
        assertArrayEquals(new Biome[] { Biomes.PLAINS, Biomes.PLAINS },
                overlay.overlayForGeneration(base, 0, 0, 1, 2));
        assertArrayEquals(new Biome[] { Biomes.PLAINS, Biomes.PLAINS },
                overlay.overlayBlock(base, 0, 0, 1, 2));
        assertNull(overlay.moddedBlockBiomes(0, 0, 1, 2));
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
        Biome[] result = overlay.overlayForGeneration(base, 0, 0, 1, 2);
        assertArrayEquals(new Biome[] { Biomes.PLAINS, Biomes.PLAINS }, result);
        assertFalse(overlay.isEnabled());
        // A disabled overlay stays quiet and never touches the base grid again.
        assertArrayEquals(new Biome[] { Biomes.PLAINS, Biomes.PLAINS },
                overlay.overlayBlock(base, 0, 0, 1, 2));
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

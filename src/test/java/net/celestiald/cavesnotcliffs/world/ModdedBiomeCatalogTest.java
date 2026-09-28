package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.minecraft.init.Biomes;
import net.minecraft.init.Bootstrap;
import net.minecraft.world.biome.Biome;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ModdedBiomeCatalogTest {
    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void parsesBlacklistAndHostPinsSkippingInvalidEntries() {
        ModdedBiomeCatalog.Settings settings = ModdedBiomeCatalog.parse(
                new String[] {"thaumcraft:eerie", "biomesoplenty:*", "nonsense", " ", null},
                new String[] {
                    "traverse:meadow=plains, minecraft:meadow",
                    "pvj:bog=swamp,lush_caves,not_a_biome",
                    "missing_equals",
                    "pvj:nothing=lush_caves"
                });
        assertTrue(settings.blacklistedBiomes.contains("thaumcraft:eerie"));
        assertTrue(settings.blacklistedMods.contains("biomesoplenty"));
        assertEquals(1, settings.blacklistedBiomes.size());
        assertEquals(Arrays.asList(V118Biome.PLAINS, V118Biome.MEADOW),
                settings.hosts.get("traverse:meadow"));
        // Underground and unknown hosts are dropped; an entry left with none is ignored.
        assertEquals(Arrays.asList(V118Biome.SWAMP), settings.hosts.get("pvj:bog"));
        assertFalse(settings.hosts.containsKey("pvj:nothing"));
        assertEquals(2, settings.hosts.size());
    }

    @Test
    public void weightsKeepDeclaredRaritiesAndDeriveChainOnlyOnes() {
        Biome declared = moddedBiome("zeta", "declared");
        Biome chainOnly = moddedBiome("alpha", "chain_only");
        Biome rare = moddedBiome("alpha", "rare_sub_biome");
        Biome banned = moddedBiome("banned", "biome");
        Map<Biome, Integer> manager = new IdentityHashMap<Biome, Integer>();
        manager.put(Biomes.PLAINS, 30);
        manager.put(Biomes.FOREST, 10);
        manager.put(declared, 5);
        manager.put(banned, 10);
        Map<Biome, Integer> sampled = new IdentityHashMap<Biome, Integer>();
        // 40 weight units sampled 200 times: five samples per unit of weight.
        sampled.put(Biomes.PLAINS, 150);
        sampled.put(Biomes.FOREST, 50);
        sampled.put(chainOnly, 35);
        sampled.put(rare, 1);
        sampled.put(Biomes.RIVER, 40);

        Map<Biome, Double> weights = ModdedBiomeCatalog.weights(manager, sampled,
                ModdedBiomeCatalog.parse(new String[] {"banned:*"}, new String[0]));

        List<Biome> order = new ArrayList<Biome>(weights.keySet());
        assertEquals(Arrays.asList(chainOnly, rare, declared), order);
        assertEquals(5.0D, weights.get(declared), 1.0E-9D);
        assertEquals(7.0D, weights.get(chainOnly), 1.0E-9D);
        assertEquals(1.0D, weights.get(rare), 1.0E-9D);
        assertNull(weights.get(Biomes.PLAINS));
        assertNull(weights.get(banned));
    }

    @Test
    public void chainSamplingCountsEveryGridPoint() {
        Map<Biome, Integer> counts = ModdedBiomeCatalog.sampleChain(
                new ModdedBiomeOverlay.Sampler() {
                    @Override
                    public Biome[] generationBiomes(int quartX, int quartZ, int width,
                            int height) {
                        return new Biome[] {quartX < 0 ? Biomes.DESERT : Biomes.PLAINS};
                    }

                    @Override
                    public Biome[] blockBiomes(int blockX, int blockZ, int width, int length) {
                        throw new AssertionError("discovery samples the generation scale");
                    }
                });
        int total = ModdedBiomeCatalog.SAMPLE_GRID * ModdedBiomeCatalog.SAMPLE_GRID;
        assertEquals(total / 2, (int) counts.get(Biomes.DESERT));
        assertEquals(total / 2, (int) counts.get(Biomes.PLAINS));
    }

    private static Biome moddedBiome(String domain, String path) {
        Biome biome = new Biome(new Biome.BiomeProperties(path)) {
        };
        biome.setRegistryName(domain, path);
        return biome;
    }
}

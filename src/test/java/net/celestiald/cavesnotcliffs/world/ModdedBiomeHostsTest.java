package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.minecraft.init.Biomes;
import net.minecraft.init.Bootstrap;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ModdedBiomeHostsTest {
    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void vanillaLookalikesLandOnTheMatching118Biomes() {
        // A mod biome built like a vanilla one must replace that biome's 1.18 counterpart.
        assertEquals(V118Biome.SWAMP, bestHost(Biomes.SWAMPLAND));
        assertEquals(V118Biome.DESERT, bestHost(Biomes.DESERT));
        assertEquals(V118Biome.SNOWY_TAIGA, bestHost(Biomes.COLD_TAIGA));
        assertEquals(V118Biome.DARK_FOREST, bestHost(Biomes.ROOFED_FOREST));
        assertEquals(V118Biome.BIRCH_FOREST, bestHost(Biomes.BIRCH_FOREST));
        assertEquals(V118Biome.SAVANNA, bestHost(Biomes.SAVANNA));
        assertEquals(V118Biome.WINDSWEPT_FOREST, bestHost(Biomes.EXTREME_HILLS_WITH_TREES));
        assertEquals(V118Biome.RIVER, bestHost(Biomes.RIVER));
        assertEquals(V118Biome.FROZEN_OCEAN, bestHost(Biomes.FROZEN_OCEAN));
        assertEquals(V118Biome.BEACH, bestHost(Biomes.BEACH));
        assertTrue(hosts(Biomes.JUNGLE).contains(V118Biome.JUNGLE));
        assertTrue(hosts(Biomes.PLAINS).contains(V118Biome.PLAINS));
        assertTrue(hosts(Biomes.MESA).contains(V118Biome.BADLANDS));
    }

    @Test
    public void landAndWaterBiomesNeverSwapPlaces() {
        for (Biome biome : Biome.REGISTRY) {
            ModdedBiomeHosts.Traits traits = traits(biome);
            for (V118Biome host : ModdedBiomeHosts.hostsFor(traits)) {
                assertEquals(biome.getRegistryName() + " on " + host, traits.water,
                        ModdedBiomeHosts.hostTraits(host).water);
            }
        }
    }

    @Test
    public void mushroomBiomesOnlyReplaceMushroomFieldsAndViceVersa() {
        assertEquals(Collections.singletonList(V118Biome.MUSHROOM_FIELDS),
                hosts(Biomes.MUSHROOM_ISLAND));
        for (Biome biome : Biome.REGISTRY) {
            if (biome != Biomes.MUSHROOM_ISLAND) {
                assertFalse(biome.getRegistryName().toString(),
                        hosts(biome).contains(V118Biome.MUSHROOM_FIELDS));
            }
        }
    }

    @Test
    public void netherEndAndVoidBiomesAreNeverPlaced() {
        assertTrue(hosts(Biomes.HELL).isEmpty());
        assertTrue(hosts(Biomes.SKY).isEmpty());
        assertTrue(hosts(Biomes.VOID).isEmpty());
    }

    @Test
    public void undergroundBiomesAreNeverHosts() {
        assertNull(ModdedBiomeHosts.hostTraits(V118Biome.LUSH_CAVES));
        assertNull(ModdedBiomeHosts.hostTraits(V118Biome.DRIPSTONE_CAVES));
    }

    @Test
    public void alpineBiomesPreferPeaksAndSlopesOverLowlands() {
        Set<Type> types = new HashSet<Type>(Arrays.asList(Type.MOUNTAIN, Type.SNOWY, Type.COLD));
        ModdedBiomeHosts.Traits alpine = new ModdedBiomeHosts.Traits(-0.5F, 0.4F, 1.8F, 0.5F,
                ModdedBiomeHosts.Water.LAND, types);
        List<V118Biome> hosts = ModdedBiomeHosts.hostsFor(alpine);
        assertTrue(hosts.toString(), hosts.contains(V118Biome.JAGGED_PEAKS)
                || hosts.contains(V118Biome.FROZEN_PEAKS));
        assertFalse(hosts.contains(V118Biome.SNOWY_PLAINS));
    }

    @Test
    public void rollsSplitBetweenTheHostAndItsModdedBiomesByWeight() {
        Biome marsh = moddedBiome("marsh");
        Biome bog = moddedBiome("bog");
        Map<Biome, Double> weights = new LinkedHashMap<Biome, Double>();
        weights.put(marsh, 10.0D);
        weights.put(bog, 20.0D);
        ModdedBiomeHosts table = ModdedBiomeHosts.build(weights, pinned(marsh, bog),
                10.0D, biome -> null);

        // Total weight 40: [0, 10) keeps the 1.18 swamp, [10, 20) marsh, [20, 40) bog.
        assertNull(table.pick(V118Biome.SWAMP, 0.0D));
        assertNull(table.pick(V118Biome.SWAMP, 0.24D));
        assertSame(marsh, table.pick(V118Biome.SWAMP, 0.26D));
        assertSame(bog, table.pick(V118Biome.SWAMP, 0.51D));
        assertSame(bog, table.pick(V118Biome.SWAMP, 0.999D));
        // Hosts without candidates are never replaced.
        assertFalse(table.hasCandidates(V118Biome.PLAINS));
        assertNull(table.pick(V118Biome.PLAINS, 0.9D));
    }

    @Test
    public void zeroVanillaWeightLetsModdedBiomesTakeEveryFittingHost() {
        Biome marsh = moddedBiome("marsh_only");
        Map<Biome, Double> weights = Collections.singletonMap(marsh, 1.0D);
        ModdedBiomeHosts table = ModdedBiomeHosts.build(weights, pinned(marsh), 0.0D,
                biome -> null);
        assertSame(marsh, table.pick(V118Biome.SWAMP, 0.0D));
        assertSame(marsh, table.pick(V118Biome.SWAMP, 0.5D));
    }

    @Test
    public void aBiomeFittingSeveralHostsSplitsItsWeight() {
        Biome grove = moddedBiome("oak_grove");
        Map<Biome, Double> weights = Collections.singletonMap(grove, 12.0D);
        Map<Biome, List<V118Biome>> pinned = new IdentityHashMap<Biome, List<V118Biome>>();
        pinned.put(grove, Arrays.asList(V118Biome.FOREST, V118Biome.BIRCH_FOREST,
                V118Biome.FLOWER_FOREST));
        ModdedBiomeHosts table = ModdedBiomeHosts.build(weights, pinned, 10.0D, biome -> null);
        assertEquals(4.0D, table.candidates(V118Biome.FOREST).get(0).weight, 1.0E-9D);
        assertEquals(4.0D, table.candidates(V118Biome.BIRCH_FOREST).get(0).weight, 1.0E-9D);
    }

    @Test
    public void scoredHostsComeFromTheTraitsFunction() {
        Biome fen = moddedBiome("fen");
        ModdedBiomeHosts table = ModdedBiomeHosts.build(Collections.singletonMap(fen, 10.0D),
                Collections.<Biome, List<V118Biome>>emptyMap(), 10.0D,
                biome -> traits(Biomes.SWAMPLAND));
        assertTrue(table.hasCandidates(V118Biome.SWAMP));
        assertFalse(table.hasCandidates(V118Biome.DESERT));
    }

    private static Map<Biome, List<V118Biome>> pinned(Biome... biomes) {
        Map<Biome, List<V118Biome>> pinned = new IdentityHashMap<Biome, List<V118Biome>>();
        for (Biome biome : biomes) {
            pinned.put(biome, Collections.singletonList(V118Biome.SWAMP));
        }
        return pinned;
    }

    private static Biome moddedBiome(String path) {
        Biome biome = new Biome(new Biome.BiomeProperties(path)) {
        };
        biome.setRegistryName("testmod", path);
        return biome;
    }

    private static V118Biome bestHost(Biome lookalike) {
        List<V118Biome> hosts = hosts(lookalike);
        assertFalse(lookalike.getRegistryName() + " has no host", hosts.isEmpty());
        return hosts.get(0);
    }

    private static List<V118Biome> hosts(Biome lookalike) {
        return ModdedBiomeHosts.hostsFor(traits(lookalike));
    }

    private static ModdedBiomeHosts.Traits traits(Biome biome) {
        return ModdedBiomeHosts.traitsOf(biome,
                new HashSet<Type>(BiomeDictionary.getTypes(biome)));
    }
}

package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.content.MountainBiomeContent;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118NoiseRouterData;
import net.minecraft.init.Biomes;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class V118BiomeProviderTest {
    private static final long SEED = 20240801L;
    private static final List<Biome> VILLAGE_BIOMES =
        Arrays.asList(Biomes.PLAINS, Biomes.DESERT, Biomes.SAVANNA, Biomes.TAIGA);
    private static final List<Biome> OCEAN_BIOMES =
        Arrays.asList(Biomes.OCEAN, Biomes.DEEP_OCEAN);

    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void villageViabilityFollowsThe118ClimateMapInsteadOfVanillaGenLayer() {
        V118BiomeProvider provider = provider(SEED);
        BlockPos oceanAnchor = null;
        BlockPos plainsAnchor = null;
        for (int radius = 0; radius <= 4096 && (oceanAnchor == null || plainsAnchor == null);
                radius += 64) {
            for (int z = -radius; z <= radius; z += 64) {
                for (int x = -radius; x <= radius; x += 64) {
                    BlockPos pos = new BlockPos(x, 64, z);
                    Biome biome = provider.getBiome(pos);
                    if (oceanAnchor == null && OCEAN_BIOMES.contains(biome)
                            && provider.areBiomesViable(x, z, 0, OCEAN_BIOMES)) {
                        oceanAnchor = pos;
                    }
                    if (plainsAnchor == null && biome == Biomes.PLAINS
                            && provider.areBiomesViable(x, z, 0, VILLAGE_BIOMES)) {
                        plainsAnchor = pos;
                    }
                }
            }
        }
        assertNotNull("fixture seed must expose a 1.18 ocean near the origin", oceanAnchor);
        assertNotNull("fixture seed must expose 1.18 plains near the origin", plainsAnchor);
        // The bug report: villages passed their biome check over the 1.18 ocean because the
        // check sampled the unrelated vanilla GenLayer layout. Both checks must agree now.
        assertFalse(provider.areBiomesViable(oceanAnchor.getX(), oceanAnchor.getZ(), 0,
            VILLAGE_BIOMES));
        assertFalse(provider.areBiomesViable(oceanAnchor.getX(), oceanAnchor.getZ(), 32,
            VILLAGE_BIOMES));
        assertTrue(provider.areBiomesViable(plainsAnchor.getX(), plainsAnchor.getZ(), 0,
            VILLAGE_BIOMES));
    }

    @Test
    public void resolvesIdenticallyAcrossInstancesAndProfiles() {
        for (V118NoiseRouterData.Profile profile : V118NoiseRouterData.Profile.values()) {
            V118BiomeProvider first = provider(SEED + profile.ordinal(), profile);
            V118BiomeProvider second = provider(SEED + profile.ordinal(), profile);
            for (int index = 0; index < 64; ++index) {
                int x = -1024 + index * 31;
                int z = 512 - index * 17;
                assertEquals(first.getBiome(new BlockPos(x, 64, z)),
                    second.getBiome(new BlockPos(x, 64, z)));
                Biome[] generationGrid = first.getBiomesForGeneration(null, x >> 2, z >> 2, 4, 4);
                assertEquals(generationGrid,
                    second.getBiomesForGeneration(null, x >> 2, z >> 2, 4, 4));
            }
        }
    }

    @Test
    public void findBiomePositionLocatesAVillageBiomeInThe118Layout() {
        V118BiomeProvider provider = provider(SEED);
        BlockPos found = provider.findBiomePosition(0, 0, 1024,
            Collections.singletonList(Biomes.PLAINS), new Random(7L));
        assertNotNull("fixture seed must have plains within 1024 blocks of the origin", found);
        assertEquals(Biomes.PLAINS, provider.getBiome(new BlockPos(found.getX(), 64,
            found.getZ())));
    }

    private static V118BiomeProvider provider(long seed) {
        return provider(seed, V118NoiseRouterData.Profile.DEFAULT);
    }

    private static V118BiomeProvider provider(long seed, V118NoiseRouterData.Profile profile) {
        return new V118BiomeProvider(seed, profile, V118BiomeMapper.fromResolver(location -> {
            Biome mountain = MountainBiomeContent.biomeFor(location);
            return mountain == null ? Biome.REGISTRY.getObject(location) : mountain;
        }));
    }
}

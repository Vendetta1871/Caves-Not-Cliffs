package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavesnotcliffs.content.MountainBiomeContent;
import net.celestiald.cavesnotcliffs.content.MountainBiomeContent.Definition;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.minecraft.init.Bootstrap;
import net.minecraft.world.biome.Biome;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class V118BiomeMapperTest {
    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void documentsCompleteStableJava118ToJava112ProjectionTable() {
        Object[][] aliases = {
            {V118Biome.BADLANDS, "minecraft:mesa"},
            {V118Biome.BAMBOO_JUNGLE, "minecraft:jungle"},
            {V118Biome.BEACH, "minecraft:beaches"},
            {V118Biome.BIRCH_FOREST, "minecraft:birch_forest"},
            {V118Biome.COLD_OCEAN, "minecraft:ocean"},
            {V118Biome.DARK_FOREST, "minecraft:roofed_forest"},
            {V118Biome.DEEP_COLD_OCEAN, "minecraft:deep_ocean"},
            {V118Biome.DEEP_FROZEN_OCEAN, "minecraft:frozen_ocean"},
            {V118Biome.DEEP_LUKEWARM_OCEAN, "minecraft:deep_ocean"},
            {V118Biome.DEEP_OCEAN, "minecraft:deep_ocean"},
            {V118Biome.DESERT, "minecraft:desert"},
            {V118Biome.DRIPSTONE_CAVES, "minecraft:extreme_hills"},
            {V118Biome.ERODED_BADLANDS, "minecraft:mutated_mesa"},
            {V118Biome.FLOWER_FOREST, "minecraft:mutated_forest"},
            {V118Biome.FOREST, "minecraft:forest"},
            {V118Biome.FROZEN_OCEAN, "minecraft:frozen_ocean"},
            {V118Biome.FROZEN_PEAKS, "cavesnotcliffs:frozen_peaks"},
            {V118Biome.FROZEN_RIVER, "minecraft:frozen_river"},
            {V118Biome.GROVE, "cavesnotcliffs:grove"},
            {V118Biome.ICE_SPIKES, "minecraft:mutated_ice_flats"},
            {V118Biome.JAGGED_PEAKS, "cavesnotcliffs:jagged_peaks"},
            {V118Biome.JUNGLE, "minecraft:jungle"},
            {V118Biome.LUKEWARM_OCEAN, "minecraft:ocean"},
            {V118Biome.LUSH_CAVES, "minecraft:forest"},
            {V118Biome.MEADOW, "cavesnotcliffs:meadow"},
            {V118Biome.MUSHROOM_FIELDS, "minecraft:mushroom_island"},
            {V118Biome.OCEAN, "minecraft:ocean"},
            {V118Biome.OLD_GROWTH_BIRCH_FOREST, "minecraft:mutated_birch_forest"},
            {V118Biome.OLD_GROWTH_PINE_TAIGA, "minecraft:redwood_taiga"},
            {V118Biome.OLD_GROWTH_SPRUCE_TAIGA, "minecraft:mutated_redwood_taiga"},
            {V118Biome.PLAINS, "minecraft:plains"},
            {V118Biome.RIVER, "minecraft:river"},
            {V118Biome.SAVANNA, "minecraft:savanna"},
            {V118Biome.SAVANNA_PLATEAU, "minecraft:savanna_rock"},
            {V118Biome.SNOWY_BEACH, "minecraft:cold_beach"},
            {V118Biome.SNOWY_PLAINS, "minecraft:ice_flats"},
            {V118Biome.SNOWY_SLOPES, "cavesnotcliffs:snowy_slopes"},
            {V118Biome.SNOWY_TAIGA, "minecraft:taiga_cold"},
            {V118Biome.SPARSE_JUNGLE, "minecraft:jungle_edge"},
            {V118Biome.STONY_PEAKS, "cavesnotcliffs:stony_peaks"},
            {V118Biome.STONY_SHORE, "minecraft:stone_beach"},
            {V118Biome.SUNFLOWER_PLAINS, "minecraft:mutated_plains"},
            {V118Biome.SWAMP, "minecraft:swampland"},
            {V118Biome.TAIGA, "minecraft:taiga"},
            {V118Biome.WARM_OCEAN, "minecraft:ocean"},
            {V118Biome.WINDSWEPT_FOREST, "minecraft:extreme_hills_with_trees"},
            {V118Biome.WINDSWEPT_GRAVELLY_HILLS, "minecraft:mutated_extreme_hills"},
            {V118Biome.WINDSWEPT_HILLS, "minecraft:extreme_hills"},
            {V118Biome.WINDSWEPT_SAVANNA, "minecraft:mutated_savanna"},
            {V118Biome.WOODED_BADLANDS, "minecraft:mesa_rock"}
        };

        EnumSet<V118Biome> covered = EnumSet.noneOf(V118Biome.class);
        for (Object[] alias : aliases) {
            V118Biome biome = (V118Biome) alias[0];
            assertTrue("duplicate fixture entry for " + biome, covered.add(biome));
            assertEquals(alias[1], V118BiomeMapper.registryId(biome));
            assertEquals("compatibility accessor", alias[1], V118BiomeMapper.legacyId(biome));
        }
        assertEquals(EnumSet.allOf(V118Biome.class), covered);
    }

    @Test
    public void resolvesEveryProjectionToItsRegisteredBiomeContract() {
        V118BiomeMapper mapper = mapperWithMountainRegistry();
        for (V118Biome biome : V118Biome.values()) {
            Biome resolved = mapper.biomeFor(biome);
            assertNotNull(resolved);
            int id = Biome.getIdForBiome(resolved);
            if (MountainBiomeContent.isMountainBiome(biome)) {
                // Forge assigns the numeric ID during the real Register<Biome> event. The
                // resolver seam keeps this unit test out of the intentionally locked registry.
                assertEquals(V118BiomeMapper.registryId(biome),
                    resolved.getRegistryName().toString());
            } else {
                assertTrue("legacy chunk biome id must fit one byte: " + id,
                    id >= 0 && id <= 255);
            }
            assertEquals(resolved, mapper.biomeFor(biome.ordinal()));
        }
    }

    @Test
    public void nativeMountainRowsResolveToTheirCanonicalRegisteredInstances() {
        V118BiomeMapper mapper = mapperWithMountainRegistry();
        for (Definition definition : Definition.values()) {
            assertEquals(definition.registryName().toString(),
                V118BiomeMapper.registryId(definition.virtualBiome()));
            assertEquals(definition.biome(), mapper.biomeFor(definition.virtualBiome()));
        }
    }

    private static V118BiomeMapper mapperWithMountainRegistry() {
        return V118BiomeMapper.fromResolver(location -> {
            Biome mountain = MountainBiomeContent.biomeFor(location);
            return mountain == null ? Biome.REGISTRY.getObject(location) : mountain;
        });
    }
}

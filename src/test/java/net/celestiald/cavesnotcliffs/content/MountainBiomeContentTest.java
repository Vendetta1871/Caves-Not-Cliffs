package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.content.MountainBiomeContent.DecorationContract;
import net.celestiald.cavesnotcliffs.content.MountainBiomeContent.Definition;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ColorizerFoliage;
import net.minecraft.world.ColorizerGrass;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.WorldGenBigTree;
import net.minecraft.world.gen.feature.WorldGenBirchTree;
import net.minecraft.world.gen.feature.WorldGenTaiga1;
import net.minecraft.world.gen.feature.WorldGenTaiga2;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class MountainBiomeContentTest {
    @BeforeClass
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void exposesSixCanonicalDistinctRegistrations() {
        assertEquals(6, Definition.values().length);
        Set<String> ids = new HashSet<String>();
        for (Definition definition : Definition.values()) {
            Biome biome = definition.biome();
            assertNotNull(biome);
            assertEquals(definition.registryName(), biome.getRegistryName());
            assertEquals("cavesnotcliffs", definition.registryName().getResourceDomain());
            assertTrue("duplicate biome id " + definition.registryName(),
                ids.add(definition.registryName().toString()));
            assertSame(biome, MountainBiomeContent.biomeFor(definition.virtualBiome()));
            assertSame(biome, MountainBiomeContent.biomeFor(definition.registryName()));
            assertTrue(MountainBiomeContent.isMountainBiome(definition.virtualBiome()));
        }
    }

    @Test
    public void forgeRegistrySubscriberPublishesAllSixInOfficialOrder() throws Exception {
        Biome[] expected = new Biome[Definition.values().length];
        for (Definition definition : Definition.values()) {
            expected[definition.ordinal()] = definition.biome();
        }
        assertArrayEquals(expected, MountainBiomeContent.registrationOrder());

        Method subscriber = MountainBiomeContent.class.getDeclaredMethod("registerBiomes",
            RegistryEvent.Register.class);
        assertNotNull(subscriber.getAnnotation(SubscribeEvent.class));
    }

    @Test
    public void climatePrecipitationAndEffectsMatchJava1182() {
        Object[][] rows = {
            {Definition.MEADOW, 0.5F, 0.8F, false, 8_103_167},
            {Definition.GROVE, -0.2F, 0.8F, true, 8_495_359},
            {Definition.SNOWY_SLOPES, -0.3F, 0.9F, true, 8_560_639},
            {Definition.JAGGED_PEAKS, -0.7F, 0.9F, true, 8_756_735},
            {Definition.FROZEN_PEAKS, -0.7F, 0.9F, true, 8_756_735},
            {Definition.STONY_PEAKS, 1.0F, 0.3F, false, 7_776_511}
        };

        for (Object[] row : rows) {
            Definition definition = (Definition) row[0];
            float temperature = (Float) row[1];
            float downfall = (Float) row[2];
            boolean snow = (Boolean) row[3];
            int skyColor = (Integer) row[4];
            Biome biome = definition.biome();

            assertEquals(definition.name(), temperature, definition.temperature(), 0.0F);
            assertEquals(definition.name(), downfall, definition.downfall(), 0.0F);
            assertEquals(definition.name(), temperature, biome.getDefaultTemperature(), 0.0F);
            assertEquals(definition.name(), downfall, biome.getRainfall(), 0.0F);
            assertEquals(definition.name(), snow, definition.hasSnow());
            assertEquals(definition.name(), snow, biome.isSnowyBiome());
            assertEquals(definition.name(), !snow, biome.canRain());
            assertEquals(definition.name(), MountainBiomeContent.NORMAL_WATER_COLOR,
                biome.getWaterColorMultiplier());
            assertEquals(definition.name(), skyColor,
                biome.getSkyColorByTemp(definition.temperature()));
            assertEquals("sky color must not drift with legacy height temperature",
                skyColor, biome.getSkyColorByTemp(-10.0F));
            assertEquals("sky color must not drift with legacy height temperature",
                skyColor, biome.getSkyColorByTemp(10.0F));
            assertEquals("native density owns legacy base height", 0.1F,
                biome.getBaseHeight(), 0.0F);
            assertEquals("native density owns legacy height variation", 0.2F,
                biome.getHeightVariation(), 0.0F);
        }

        assertEquals(329_011, MountainBiomeContent.NORMAL_WATER_FOG_COLOR);
        assertEquals(12_638_463, MountainBiomeContent.OVERWORLD_FOG_COLOR);
    }

    @Test
    public void grassAndFoliageColorsUseBaseClimateAtEveryHeight() {
        int[] indexedColors = new int[65_536];
        for (int index = 0; index < indexedColors.length; ++index) {
            indexedColors[index] = index;
        }
        ColorizerGrass.setGrassBiomeColorizer(indexedColors);
        ColorizerFoliage.setFoliageBiomeColorizer(indexedColors);
        try {
            for (Definition definition : Definition.values()) {
                int expected = climateColorIndex(definition.temperature(),
                    definition.downfall());
                assertEquals(definition.name(), expected,
                    definition.biome().getGrassColorAtPos(new BlockPos(17, -64, -31)));
                assertEquals(definition.name(), expected,
                    definition.biome().getGrassColorAtPos(new BlockPos(17, 319, -31)));
                assertEquals(definition.name(), expected,
                    definition.biome().getFoliageColorAtPos(new BlockPos(17, -64, -31)));
                assertEquals(definition.name(), expected,
                    definition.biome().getFoliageColorAtPos(new BlockPos(17, 319, -31)));
            }
        } finally {
            ColorizerGrass.setGrassBiomeColorizer(new int[65_536]);
            ColorizerFoliage.setFoliageBiomeColorizer(new int[65_536]);
        }
    }

    @Test
    public void orderedDecorationContractsMatchOfficialBiomeFactories() {
        assertEquals(contracts(
            DecorationContract.GLOBAL_OVERWORLD_GENERATION,
            DecorationContract.PLAIN_GRASS,
            DecorationContract.DEFAULT_ORES,
            DecorationContract.DEFAULT_SOFT_DISKS,
            DecorationContract.MEADOW_VEGETATION,
            DecorationContract.EXTRA_EMERALDS,
            DecorationContract.INFESTED_STONE), Definition.MEADOW.decorations());
        assertEquals(contracts(
            DecorationContract.GLOBAL_OVERWORLD_GENERATION,
            DecorationContract.FROZEN_SPRINGS,
            DecorationContract.DEFAULT_ORES,
            DecorationContract.DEFAULT_SOFT_DISKS,
            DecorationContract.GROVE_TREES,
            DecorationContract.DEFAULT_EXTRA_VEGETATION,
            DecorationContract.EXTRA_EMERALDS,
            DecorationContract.INFESTED_STONE), Definition.GROVE.decorations());
        assertEquals(contracts(
            DecorationContract.GLOBAL_OVERWORLD_GENERATION,
            DecorationContract.FROZEN_SPRINGS,
            DecorationContract.DEFAULT_ORES,
            DecorationContract.DEFAULT_SOFT_DISKS,
            DecorationContract.DEFAULT_EXTRA_VEGETATION,
            DecorationContract.EXTRA_EMERALDS,
            DecorationContract.INFESTED_STONE), Definition.SNOWY_SLOPES.decorations());
        assertEquals(contracts(
            DecorationContract.GLOBAL_OVERWORLD_GENERATION,
            DecorationContract.FROZEN_SPRINGS,
            DecorationContract.DEFAULT_ORES,
            DecorationContract.DEFAULT_SOFT_DISKS,
            DecorationContract.EXTRA_EMERALDS,
            DecorationContract.INFESTED_STONE), Definition.JAGGED_PEAKS.decorations());
        assertEquals(Definition.JAGGED_PEAKS.decorations(),
            Definition.FROZEN_PEAKS.decorations());
        assertEquals(contracts(
            DecorationContract.GLOBAL_OVERWORLD_GENERATION,
            DecorationContract.DEFAULT_ORES,
            DecorationContract.DEFAULT_SOFT_DISKS,
            DecorationContract.EXTRA_EMERALDS,
            DecorationContract.INFESTED_STONE), Definition.STONY_PEAKS.decorations());
    }

    @Test
    public void dictionaryTypesPreserveModernCategoryAndPrecipitationSemantics() {
        assertEquals(typeNames("MOUNTAIN"), typeNames(Definition.MEADOW));
        assertEquals(typeNames("FOREST", "CONIFEROUS", "COLD", "SNOWY"),
            typeNames(Definition.GROVE));
        assertEquals(typeNames("MOUNTAIN", "COLD", "SNOWY"),
            typeNames(Definition.SNOWY_SLOPES));
        assertEquals(typeNames(Definition.SNOWY_SLOPES), typeNames(Definition.JAGGED_PEAKS));
        assertEquals(typeNames(Definition.SNOWY_SLOPES), typeNames(Definition.FROZEN_PEAKS));
        assertEquals(typeNames("MOUNTAIN"), typeNames(Definition.STONY_PEAKS));
    }

    @Test
    public void spawnMatricesMatchEveryJava1182MobThatExistsInJava112() {
        List<String> commonMonsters = Arrays.asList(
            "EntitySpider:100:4:4",
            "EntityZombie:95:4:4",
            "EntityZombieVillager:5:1:1",
            "EntitySkeleton:100:4:4",
            "EntityCreeper:100:4:4",
            "EntitySlime:100:4:4",
            "EntityEnderman:10:1:4",
            "EntityWitch:5:1:1");
        List<String> commonAmbient = Arrays.asList("EntityBat:10:8:8");

        assertEquals(Arrays.asList(
            "EntityDonkey:1:1:2", "EntityRabbit:2:2:6", "EntitySheep:2:2:4"),
            signatures(Definition.MEADOW.biome(), EnumCreatureType.CREATURE));
        assertEquals(Arrays.asList(
            "EntitySheep:12:4:4", "EntityPig:10:4:4", "EntityChicken:10:4:4",
            "EntityCow:8:4:4", "EntityWolf:8:4:4", "EntityRabbit:4:2:3"),
            signatures(Definition.GROVE.biome(), EnumCreatureType.CREATURE));
        assertEquals(Arrays.asList("EntityRabbit:4:2:3"),
            signatures(Definition.SNOWY_SLOPES.biome(), EnumCreatureType.CREATURE));
        assertTrue(signatures(Definition.JAGGED_PEAKS.biome(),
            EnumCreatureType.CREATURE).isEmpty());
        assertTrue(signatures(Definition.FROZEN_PEAKS.biome(),
            EnumCreatureType.CREATURE).isEmpty());
        assertTrue(signatures(Definition.STONY_PEAKS.biome(),
            EnumCreatureType.CREATURE).isEmpty());

        for (Definition definition : Definition.values()) {
            assertEquals(definition.name(), commonMonsters,
                signatures(definition.biome(), EnumCreatureType.MONSTER));
            assertEquals(definition.name(), commonAmbient,
                signatures(definition.biome(), EnumCreatureType.AMBIENT));
            assertTrue(definition.name(),
                signatures(definition.biome(), EnumCreatureType.WATER_CREATURE).isEmpty());
        }
    }

    @Test
    public void unsupportedModernMobsAreExplicitAndNeverSubstituted() {
        String glowSquid =
            "minecraft:glow_squid weight=10 min=4 max=6 category=underground_water";
        assertEquals(Arrays.asList(glowSquid), Definition.MEADOW.omittedModernSpawns());
        assertEquals(Arrays.asList("minecraft:fox weight=8 min=2 max=4", glowSquid),
            Definition.GROVE.omittedModernSpawns());
        for (Definition definition : Arrays.asList(Definition.SNOWY_SLOPES,
                Definition.JAGGED_PEAKS, Definition.FROZEN_PEAKS)) {
            assertEquals(Arrays.asList("minecraft:goat weight=5 min=1 max=3", glowSquid),
                definition.omittedModernSpawns());
        }
        assertEquals(Arrays.asList(glowSquid), Definition.STONY_PEAKS.omittedModernSpawns());
    }

    @Test
    public void legacyDecoratorContainsOnlyExactlyRepresentablePlacementCounts() {
        for (Definition definition : Definition.values()) {
            assertEquals(3, definition.biome().decorator.sandPatchesPerChunk);
            assertEquals(1, definition.biome().decorator.clayPerChunk);
            assertEquals(1, definition.biome().decorator.gravelPatchesPerChunk);
            assertTrue(definition.biome().decorator.generateFalls);
            assertEquals(0, definition.biome().decorator.waterlilyPerChunk);
            assertEquals(0, definition.biome().decorator.deadBushPerChunk);
            assertEquals(0, definition.biome().decorator.mushroomsPerChunk);
            assertEquals(0, definition.biome().decorator.reedsPerChunk);
            assertEquals(0, definition.biome().decorator.cactiPerChunk);
            assertEquals(0, definition.biome().decorator.bigMushroomsPerChunk);
        }

        assertEquals(0, Definition.MEADOW.biome().decorator.treesPerChunk);
        assertEquals(0.01F, Definition.MEADOW.biome().decorator.extraTreeChance, 0.0F);
        assertEquals(0, Definition.MEADOW.biome().decorator.flowersPerChunk);
        assertEquals(10, Definition.GROVE.biome().decorator.treesPerChunk);
        assertEquals(0.1F, Definition.GROVE.biome().decorator.extraTreeChance, 0.0F);

        for (Definition definition : Arrays.asList(Definition.SNOWY_SLOPES,
                Definition.JAGGED_PEAKS, Definition.FROZEN_PEAKS,
                Definition.STONY_PEAKS)) {
            assertEquals(0, definition.biome().decorator.treesPerChunk);
            assertEquals(0.0F, definition.biome().decorator.extraTreeChance, 0.0F);
            assertEquals(0, definition.biome().decorator.flowersPerChunk);
        }
    }

    @Test
    public void representableMeadowAndGroveTreeSelectorsMatchOfficialWeights() {
        assertEquals(WorldGenBigTree.class, Definition.MEADOW.biome()
            .getRandomTreeFeature(new FixedFloatRandom(0.499999F)).getClass());
        assertEquals(WorldGenBirchTree.class, Definition.MEADOW.biome()
            .getRandomTreeFeature(new FixedFloatRandom(0.5F)).getClass());
        assertEquals(WorldGenTaiga1.class, Definition.GROVE.biome()
            .getRandomTreeFeature(new FixedFloatRandom(0.3333333F)).getClass());
        assertEquals(WorldGenTaiga2.class, Definition.GROVE.biome()
            .getRandomTreeFeature(new FixedFloatRandom(0.33333334F)).getClass());
    }

    private static List<DecorationContract> contracts(DecorationContract... values) {
        return Arrays.asList(values);
    }

    private static Set<String> typeNames(Definition definition) {
        Set<String> names = new HashSet<String>();
        for (BiomeDictionary.Type type : definition.dictionaryTypes()) {
            names.add(type.getName());
        }
        return names;
    }

    private static Set<String> typeNames(String... names) {
        return new HashSet<String>(Arrays.asList(names));
    }

    private static List<String> signatures(Biome biome, EnumCreatureType creatureType) {
        List<String> signatures = new ArrayList<String>();
        for (Biome.SpawnListEntry entry : biome.getSpawnableList(creatureType)) {
            signatures.add(entry.entityClass.getSimpleName() + ":" + entry.itemWeight + ":"
                + entry.minGroupCount + ":" + entry.maxGroupCount);
        }
        return signatures;
    }

    private static int climateColorIndex(float baseTemperature, float downfall) {
        double temperature = Math.max(0.0, Math.min(1.0, baseTemperature));
        double humidity = Math.max(0.0, Math.min(1.0, downfall)) * temperature;
        int temperatureIndex = (int) ((1.0 - temperature) * 255.0);
        int humidityIndex = (int) ((1.0 - humidity) * 255.0);
        return humidityIndex << 8 | temperatureIndex;
    }

    private static final class FixedFloatRandom extends Random {
        private final float value;

        FixedFloatRandom(float value) {
            this.value = value;
        }

        @Override
        public float nextFloat() {
            return value;
        }
    }
}

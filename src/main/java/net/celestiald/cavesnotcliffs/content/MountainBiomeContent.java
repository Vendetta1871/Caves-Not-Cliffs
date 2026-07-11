package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.celestiald.cavesnotcliffs.worldgen.v118.V118Biome;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityDonkey;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntityRabbit;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.feature.WorldGenBirchTree;
import net.minecraft.world.gen.feature.WorldGenTaiga1;
import net.minecraft.world.gen.feature.WorldGenTaiga2;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Canonical public registrations for the six mountain biomes added in Java 1.18.
 *
 * <p>The climate, effects, spawn weights, and ordered generation contracts are transcribed from
 * Java 1.18.2's {@code OverworldBiomes}. Native schema-2 worlds consume the generation contract
 * through the 1.18 feature pipeline rather than invoking the legacy {@link Biome#decorate}
 * pipeline. The public 1.12 decorator fields retain the portions that can be represented without
 * changing that pipeline: soft-disk counts and Meadow/Grove tree attempts.</p>
 */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class MountainBiomeContent {
    public static final int NORMAL_WATER_COLOR = 4_159_204;
    public static final int NORMAL_WATER_FOG_COLOR = 329_011;
    public static final int OVERWORLD_FOG_COLOR = 12_638_463;

    /** Ordered calls made by Java 1.18.2's biome factory after spawn construction. */
    public enum DecorationContract {
        GLOBAL_OVERWORLD_GENERATION,
        PLAIN_GRASS,
        FROZEN_SPRINGS,
        DEFAULT_ORES,
        DEFAULT_SOFT_DISKS,
        MEADOW_VEGETATION,
        GROVE_TREES,
        DEFAULT_EXTRA_VEGETATION,
        EXTRA_EMERALDS,
        INFESTED_STONE
    }

    /** Immutable Java 1.18.2 oracle row and its registered 1.12 biome instance. */
    public enum Definition {
        MEADOW(
            V118Biome.MEADOW, "meadow", "Meadow", 0.5F, 0.8F, false,
            DefinitionData.types(BiomeDictionary.Type.MOUNTAIN),
            DefinitionData.contracts(
                DecorationContract.GLOBAL_OVERWORLD_GENERATION,
                DecorationContract.PLAIN_GRASS,
                DecorationContract.DEFAULT_ORES,
                DecorationContract.DEFAULT_SOFT_DISKS,
                DecorationContract.MEADOW_VEGETATION,
                DecorationContract.EXTRA_EMERALDS,
                DecorationContract.INFESTED_STONE),
            DefinitionData.omitted(
                "minecraft:glow_squid weight=10 min=4 max=6 category=underground_water")),
        GROVE(
            V118Biome.GROVE, "grove", "Grove", -0.2F, 0.8F, true,
            DefinitionData.types(BiomeDictionary.Type.FOREST, BiomeDictionary.Type.CONIFEROUS,
                BiomeDictionary.Type.COLD, BiomeDictionary.Type.SNOWY),
            DefinitionData.contracts(
                DecorationContract.GLOBAL_OVERWORLD_GENERATION,
                DecorationContract.FROZEN_SPRINGS,
                DecorationContract.DEFAULT_ORES,
                DecorationContract.DEFAULT_SOFT_DISKS,
                DecorationContract.GROVE_TREES,
                DecorationContract.DEFAULT_EXTRA_VEGETATION,
                DecorationContract.EXTRA_EMERALDS,
                DecorationContract.INFESTED_STONE),
            DefinitionData.omitted(
                "minecraft:fox weight=8 min=2 max=4",
                "minecraft:glow_squid weight=10 min=4 max=6 category=underground_water")),
        SNOWY_SLOPES(
            V118Biome.SNOWY_SLOPES, "snowy_slopes", "Snowy Slopes", -0.3F, 0.9F, true,
            DefinitionData.types(BiomeDictionary.Type.MOUNTAIN, BiomeDictionary.Type.COLD,
                BiomeDictionary.Type.SNOWY),
            DefinitionData.contracts(
                DecorationContract.GLOBAL_OVERWORLD_GENERATION,
                DecorationContract.FROZEN_SPRINGS,
                DecorationContract.DEFAULT_ORES,
                DecorationContract.DEFAULT_SOFT_DISKS,
                DecorationContract.DEFAULT_EXTRA_VEGETATION,
                DecorationContract.EXTRA_EMERALDS,
                DecorationContract.INFESTED_STONE),
            DefinitionData.omitted(
                "minecraft:goat weight=5 min=1 max=3",
                "minecraft:glow_squid weight=10 min=4 max=6 category=underground_water")),
        FROZEN_PEAKS(
            V118Biome.FROZEN_PEAKS, "frozen_peaks", "Frozen Peaks", -0.7F, 0.9F, true,
            DefinitionData.types(BiomeDictionary.Type.MOUNTAIN, BiomeDictionary.Type.COLD,
                BiomeDictionary.Type.SNOWY),
            DefinitionData.contracts(
                DecorationContract.GLOBAL_OVERWORLD_GENERATION,
                DecorationContract.FROZEN_SPRINGS,
                DecorationContract.DEFAULT_ORES,
                DecorationContract.DEFAULT_SOFT_DISKS,
                DecorationContract.EXTRA_EMERALDS,
                DecorationContract.INFESTED_STONE),
            DefinitionData.omitted(
                "minecraft:goat weight=5 min=1 max=3",
                "minecraft:glow_squid weight=10 min=4 max=6 category=underground_water")),
        JAGGED_PEAKS(
            V118Biome.JAGGED_PEAKS, "jagged_peaks", "Jagged Peaks", -0.7F, 0.9F, true,
            DefinitionData.types(BiomeDictionary.Type.MOUNTAIN, BiomeDictionary.Type.COLD,
                BiomeDictionary.Type.SNOWY),
            DefinitionData.contracts(
                DecorationContract.GLOBAL_OVERWORLD_GENERATION,
                DecorationContract.FROZEN_SPRINGS,
                DecorationContract.DEFAULT_ORES,
                DecorationContract.DEFAULT_SOFT_DISKS,
                DecorationContract.EXTRA_EMERALDS,
                DecorationContract.INFESTED_STONE),
            DefinitionData.omitted(
                "minecraft:goat weight=5 min=1 max=3",
                "minecraft:glow_squid weight=10 min=4 max=6 category=underground_water")),
        STONY_PEAKS(
            V118Biome.STONY_PEAKS, "stony_peaks", "Stony Peaks", 1.0F, 0.3F, false,
            DefinitionData.types(BiomeDictionary.Type.MOUNTAIN),
            DefinitionData.contracts(
                DecorationContract.GLOBAL_OVERWORLD_GENERATION,
                DecorationContract.DEFAULT_ORES,
                DecorationContract.DEFAULT_SOFT_DISKS,
                DecorationContract.EXTRA_EMERALDS,
                DecorationContract.INFESTED_STONE),
            DefinitionData.omitted(
                "minecraft:glow_squid weight=10 min=4 max=6 category=underground_water"));

        private final V118Biome virtualBiome;
        private final ResourceLocation registryName;
        private final String displayName;
        private final float temperature;
        private final float downfall;
        private final boolean snow;
        private final Set<BiomeDictionary.Type> dictionaryTypes;
        private final List<DecorationContract> decorations;
        private final List<String> omittedModernSpawns;
        private final CncMountainBiome biome;

        Definition(V118Biome virtualBiome, String path, String displayName, float temperature,
                float downfall, boolean snow, Set<BiomeDictionary.Type> dictionaryTypes,
                List<DecorationContract> decorations, List<String> omittedModernSpawns) {
            this.virtualBiome = virtualBiome;
            registryName = new ResourceLocation(CavesNotCliffs.MODID, path);
            this.displayName = displayName;
            this.temperature = temperature;
            this.downfall = downfall;
            this.snow = snow;
            this.dictionaryTypes = dictionaryTypes;
            this.decorations = decorations;
            this.omittedModernSpawns = omittedModernSpawns;
            biome = new CncMountainBiome(this);
        }

        public V118Biome virtualBiome() {
            return virtualBiome;
        }

        public ResourceLocation registryName() {
            return registryName;
        }

        public String displayName() {
            return displayName;
        }

        public float temperature() {
            return temperature;
        }

        public float downfall() {
            return downfall;
        }

        public boolean hasSnow() {
            return snow;
        }

        public Set<BiomeDictionary.Type> dictionaryTypes() {
            return dictionaryTypes;
        }

        public List<DecorationContract> decorations() {
            return decorations;
        }

        /** Modern entities intentionally not substituted with behaviorally different 1.12 mobs. */
        public List<String> omittedModernSpawns() {
            return omittedModernSpawns;
        }

        public Biome biome() {
            return biome;
        }
    }

    private static final Map<V118Biome, Definition> BY_VIRTUAL_BIOME = indexDefinitions();
    private static final Map<ResourceLocation, Definition> BY_REGISTRY_NAME = indexRegistryNames();

    private MountainBiomeContent() {
    }

    @SubscribeEvent
    public static void registerBiomes(RegistryEvent.Register<Biome> event) {
        Biome[] biomes = new Biome[Definition.values().length];
        for (Definition definition : Definition.values()) {
            biomes[definition.ordinal()] = definition.biome();
        }
        event.getRegistry().registerAll(biomes);

        for (Definition definition : Definition.values()) {
            Set<BiomeDictionary.Type> types = definition.dictionaryTypes();
            BiomeDictionary.addTypes(definition.biome(),
                types.toArray(new BiomeDictionary.Type[types.size()]));
        }
    }

    public static Biome biomeFor(V118Biome biome) {
        Definition definition = BY_VIRTUAL_BIOME.get(biome);
        return definition == null ? null : definition.biome();
    }

    public static Biome biomeFor(ResourceLocation registryName) {
        Definition definition = BY_REGISTRY_NAME.get(registryName);
        return definition == null ? null : definition.biome();
    }

    public static boolean isMountainBiome(V118Biome biome) {
        return BY_VIRTUAL_BIOME.containsKey(biome);
    }

    private static Map<V118Biome, Definition> indexDefinitions() {
        EnumMap<V118Biome, Definition> definitions = new EnumMap<V118Biome, Definition>(
            V118Biome.class);
        for (Definition definition : Definition.values()) {
            Definition previous = definitions.put(definition.virtualBiome(), definition);
            if (previous != null) {
                throw new IllegalStateException("Duplicate mountain biome definition for "
                    + definition.virtualBiome());
            }
        }
        return Collections.unmodifiableMap(definitions);
    }

    private static Map<ResourceLocation, Definition> indexRegistryNames() {
        Map<ResourceLocation, Definition> definitions =
            new java.util.LinkedHashMap<ResourceLocation, Definition>();
        for (Definition definition : Definition.values()) {
            Definition previous = definitions.put(definition.registryName(), definition);
            if (previous != null) {
                throw new IllegalStateException("Duplicate mountain biome registry name: "
                    + definition.registryName());
            }
        }
        return Collections.unmodifiableMap(definitions);
    }

    /** Avoids initializing the outer registry indexes while the Definition enum is constructed. */
    private static final class DefinitionData {
        private static Set<BiomeDictionary.Type> types(BiomeDictionary.Type first,
                BiomeDictionary.Type... remaining) {
            Set<BiomeDictionary.Type> result = new LinkedHashSet<BiomeDictionary.Type>();
            result.add(first);
            result.addAll(Arrays.asList(remaining));
            return Collections.unmodifiableSet(result);
        }

        private static List<DecorationContract> contracts(DecorationContract... contracts) {
            return Collections.unmodifiableList(Arrays.asList(contracts));
        }

        private static List<String> omitted(String... spawns) {
            return Collections.unmodifiableList(Arrays.asList(spawns));
        }
    }

    private static final class CncMountainBiome extends Biome {
        private static final WorldGenBirchTree BIRCH = new WorldGenBirchTree(false, false);
        private static final WorldGenTaiga1 PINE = new WorldGenTaiga1();
        private static final WorldGenTaiga2 SPRUCE = new WorldGenTaiga2(false);

        private final Definition definition;

        CncMountainBiome(Definition definition) {
            super(properties(definition));
            this.definition = definition;
            setRegistryName(definition.registryName());
            configureSpawns();
            configureLegacyDecorator();
        }

        @Override
        public WorldGenAbstractTree getRandomTreeFeature(Random random) {
            if (definition.virtualBiome() == V118Biome.MEADOW) {
                // 1.18: fancy oak with weight 0.5, otherwise birch. Bee nests are supplied by the
                // later bee feature checkpoint rather than silently replaced here.
                return random.nextFloat() < 0.5F ? BIG_TREE_FEATURE : BIRCH;
            }
            if (definition.virtualBiome() == V118Biome.GROVE) {
                // 1.18: pine with weight 1/3, otherwise spruce, both filtered to snow support.
                return random.nextFloat() < 1.0F / 3.0F ? PINE : SPRUCE;
            }
            return super.getRandomTreeFeature(random);
        }

        private void configureSpawns() {
            spawnableMonsterList.clear();
            spawnableCreatureList.clear();
            spawnableWaterCreatureList.clear();
            spawnableCaveCreatureList.clear();

            if (definition.virtualBiome() == V118Biome.MEADOW) {
                creature(EntityDonkey.class, 1, 1, 2);
                creature(EntityRabbit.class, 2, 2, 6);
                creature(EntitySheep.class, 2, 2, 4);
            } else if (definition.virtualBiome() == V118Biome.GROVE) {
                farmAnimals();
                creature(EntityWolf.class, 8, 4, 4);
                creature(EntityRabbit.class, 4, 2, 3);
            } else if (definition.virtualBiome() == V118Biome.SNOWY_SLOPES) {
                creature(EntityRabbit.class, 4, 2, 3);
            }

            // Java 1.18.2 commonSpawns: caveSpawns plus the standard hostile matrix. Glow squid
            // uses a category absent from 1.12 and is intentionally not substituted with squid.
            spawnableCaveCreatureList.add(new SpawnListEntry(EntityBat.class, 10, 8, 8));
            monster(EntitySpider.class, 100, 4, 4);
            monster(EntityZombie.class, 95, 4, 4);
            monster(EntityZombieVillager.class, 5, 1, 1);
            monster(EntitySkeleton.class, 100, 4, 4);
            monster(EntityCreeper.class, 100, 4, 4);
            monster(EntitySlime.class, 100, 4, 4);
            monster(EntityEnderman.class, 10, 1, 4);
            monster(EntityWitch.class, 5, 1, 1);
        }

        private void configureLegacyDecorator() {
            // These values are exact where 1.12's integer/extra-chance decorator can express the
            // 1.18 placement. Complex noise/rarity placements remain in Definition.decorations()
            // for the native feature pipeline and are not approximated with unrelated features.
            decorator.waterlilyPerChunk = 0;
            decorator.treesPerChunk = 0;
            decorator.extraTreeChance = 0.0F;
            decorator.flowersPerChunk = 0;
            decorator.grassPerChunk = 0;
            decorator.deadBushPerChunk = 0;
            decorator.mushroomsPerChunk = 0;
            decorator.reedsPerChunk = 0;
            decorator.cactiPerChunk = 0;
            decorator.bigMushroomsPerChunk = 0;
            decorator.sandPatchesPerChunk = 3;
            decorator.clayPerChunk = 1;
            decorator.gravelPatchesPerChunk = 1;
            decorator.generateFalls = true;

            if (definition.virtualBiome() == V118Biome.MEADOW) {
                decorator.extraTreeChance = 0.01F;
                decorator.flowersPerChunk = 1;
            } else if (definition.virtualBiome() == V118Biome.GROVE) {
                decorator.treesPerChunk = 10;
                decorator.extraTreeChance = 0.1F;
            }
        }

        private void farmAnimals() {
            creature(EntitySheep.class, 12, 4, 4);
            creature(EntityPig.class, 10, 4, 4);
            creature(EntityChicken.class, 10, 4, 4);
            creature(EntityCow.class, 8, 4, 4);
        }

        private void creature(Class<? extends net.minecraft.entity.EntityLiving> type,
                int weight, int minimum, int maximum) {
            spawnableCreatureList.add(new SpawnListEntry(type, weight, minimum, maximum));
        }

        private void monster(Class<? extends net.minecraft.entity.EntityLiving> type,
                int weight, int minimum, int maximum) {
            spawnableMonsterList.add(new SpawnListEntry(type, weight, minimum, maximum));
        }

        private static BiomeProperties properties(Definition definition) {
            BiomeProperties properties = new BiomeProperties(definition.displayName())
                // Native density owns terrain shape. Neutral legacy values keep the public biome
                // safe if a diagnostic or third-party system reads the obsolete 2D fields.
                .setBaseHeight(0.1F)
                .setHeightVariation(0.2F)
                .setTemperature(definition.temperature())
                .setRainfall(definition.downfall())
                .setWaterColor(NORMAL_WATER_COLOR);
            if (definition.hasSnow()) {
                properties.setSnowEnabled();
            }
            return properties;
        }
    }
}

package net.celestiald.cavesnotcliffs.world;

import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.ChunkGeneratorSettings;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.MapGenStructure;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Invokes only the six structure-map stages retained from the 1.12 Overworld generator.
 *
 * <p>The caller supplies a {@link ChunkPrimer} already filled with native 1.18 terrain. This
 * bridge deliberately never calls {@link IChunkGenerator#generateChunk(int, int)} or
 * {@link IChunkGenerator#populate(int, int)}, so the old density, cave, ravine, surface, ore, and
 * biome decorator pipelines cannot leak into schema-2 terrain.</p>
 */
final class VanillaStructureBridge {
    private static final String SETTINGS_FIELD = "settings";
    private static final String SETTINGS_SRG_FIELD = "field_186000_s";

    private final World world;
    private final List<MapGenStructure> generators;
    private final List<String> names;

    VanillaStructureBridge(World world, IChunkGenerator generator) {
        if (world == null) {
            throw new NullPointerException("world");
        }
        if (!(generator instanceof ChunkGeneratorOverworld)) {
            throw new IllegalArgumentException("Native 1.18 profiles require the vanilla 1.12 "
                + "Overworld structure source, found "
                + (generator == null ? "null" : generator.getClass().getName()));
        }
        this.world = world;
        ChunkGeneratorOverworld overworld = (ChunkGeneratorOverworld) generator;
        ChunkGeneratorSettings settings = readField(overworld, ChunkGeneratorOverworld.class,
            SETTINGS_FIELD, SETTINGS_SRG_FIELD, ChunkGeneratorSettings.class);
        List<StructureDefinition> enabled = enabledDefinitions(
            world.getWorldInfo().isMapFeaturesEnabled(), settings);
        List<MapGenStructure> resolvedGenerators =
            new ArrayList<MapGenStructure>(enabled.size());
        List<String> resolvedNames = new ArrayList<String>(enabled.size());
        for (StructureDefinition definition : enabled) {
            resolvedGenerators.add(readField(overworld, ChunkGeneratorOverworld.class,
                definition.mcpField, definition.srgField, MapGenStructure.class));
            resolvedNames.add(definition.serializedName);
        }
        generators = Collections.unmodifiableList(resolvedGenerators);
        names = Collections.unmodifiableList(resolvedNames);
    }

    void generate(int chunkX, int chunkZ, ChunkPrimer primer) {
        if (primer == null) {
            throw new NullPointerException("primer");
        }
        for (MapGenStructure generator : generators) {
            generator.generate(world, chunkX, chunkZ, primer);
        }
    }

    int structureCount() {
        return generators.size();
    }

    List<String> structureNames() {
        return names;
    }

    static List<String> enabledStructureNames(boolean mapFeaturesEnabled,
            ChunkGeneratorSettings settings) {
        List<StructureDefinition> definitions = enabledDefinitions(mapFeaturesEnabled, settings);
        List<String> result = new ArrayList<String>(definitions.size());
        for (StructureDefinition definition : definitions) {
            result.add(definition.serializedName);
        }
        return Collections.unmodifiableList(result);
    }

    static void verifyReflectionContracts() {
        resolveField(ChunkGeneratorOverworld.class, SETTINGS_FIELD, SETTINGS_SRG_FIELD);
        for (StructureDefinition definition : StructureDefinition.values()) {
            resolveField(ChunkGeneratorOverworld.class, definition.mcpField,
                definition.srgField);
        }
    }

    private static List<StructureDefinition> enabledDefinitions(boolean mapFeaturesEnabled,
            ChunkGeneratorSettings settings) {
        if (settings == null) {
            throw new NullPointerException("settings");
        }
        if (!mapFeaturesEnabled) {
            return Collections.emptyList();
        }
        List<StructureDefinition> result = new ArrayList<StructureDefinition>(6);
        for (StructureDefinition definition : StructureDefinition.values()) {
            if (definition.enabled(settings)) {
                result.add(definition);
            }
        }
        return result;
    }

    private static <T> T readField(Object owner, Class<?> declaringClass, String mcpName,
            String srgName, Class<T> expectedType) {
        Field field = resolveField(declaringClass, mcpName, srgName);
        try {
            Object value = field.get(owner);
            if (!expectedType.isInstance(value)) {
                throw new IllegalStateException("Field " + declaringClass.getName() + '.'
                    + field.getName() + " contained "
                    + (value == null ? "null" : value.getClass().getName()) + ", expected "
                    + expectedType.getName());
            }
            return expectedType.cast(value);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot read " + declaringClass.getName() + '.'
                + field.getName(), exception);
        }
    }

    private static Field resolveField(Class<?> declaringClass, String mcpName, String srgName) {
        Exception failure = null;
        for (String candidate : new String[] {mcpName, srgName}) {
            try {
                Field field = declaringClass.getDeclaredField(candidate);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException | SecurityException exception) {
                failure = exception;
            }
        }
        throw new IllegalStateException("Neither MCP field '" + mcpName + "' nor SRG field '"
            + srgName + "' exists on " + declaringClass.getName(), failure);
    }

    private enum StructureDefinition {
        MINESHAFT("mineshaft", "mineshaftGenerator", "field_186006_y"),
        VILLAGE("village", "villageGenerator", "field_186005_x"),
        STRONGHOLD("stronghold", "strongholdGenerator", "field_186004_w"),
        TEMPLE("temple", "scatteredFeatureGenerator", "field_186007_z"),
        MONUMENT("monument", "oceanMonumentGenerator", "field_185980_B"),
        MANSION("mansion", "woodlandMansionGenerator", "field_191060_C");

        private final String serializedName;
        private final String mcpField;
        private final String srgField;

        StructureDefinition(String serializedName, String mcpField, String srgField) {
            this.serializedName = serializedName;
            this.mcpField = mcpField;
            this.srgField = srgField;
        }

        private boolean enabled(ChunkGeneratorSettings settings) {
            switch (this) {
                case MINESHAFT:
                    return settings.useMineShafts;
                case VILLAGE:
                    return settings.useVillages;
                case STRONGHOLD:
                    return settings.useStrongholds;
                case TEMPLE:
                    return settings.useTemples;
                case MONUMENT:
                    return settings.useMonuments;
                case MANSION:
                    return settings.useMansions;
                default:
                    throw new AssertionError(this);
            }
        }
    }
}

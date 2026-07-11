package net.celestiald.cavesnotcliffs.world;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldType;
import net.minecraft.world.WorldType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Incrementally registers and resolves hidden schema-2 wrappers with stable names. */
public final class CavesNotCliffsWorldTypes {
    private static final Map<WorldType, CavesNotCliffsWorldTypeWrapper> WRAPPERS =
            new IdentityHashMap<>();
    private static final Map<String, WorldType> BASE_TYPES_BY_NAME = new LinkedHashMap<>();

    private CavesNotCliffsWorldTypes() {
    }

    public static synchronized void registerWrappers() {
        List<WorldType> snapshot = distinctSnapshot();
        Set<String> occupiedNames = new LinkedHashSet<>();
        for (WorldType type : snapshot) {
            occupiedNames.add(normalize(type.getName()));
        }

        List<WorldType> vanillaOrder = Arrays.asList(
                WorldType.DEFAULT,
                WorldType.FLAT,
                WorldType.LARGE_BIOMES,
                WorldType.AMPLIFIED,
                WorldType.CUSTOMIZED,
                WorldType.DEBUG_ALL_BLOCK_STATES,
                WorldType.DEFAULT_1_1);

        for (WorldType baseType : vanillaOrder) {
            if (!WRAPPERS.containsKey(baseType)) {
                register(baseType, fixedName(baseType),
                        TerrainProfile.forVanillaTypeName(baseType.getName()), occupiedNames);
            }
        }

        List<WorldType> moddedTypes = new ArrayList<>();
        for (WorldType type : snapshot) {
            if (!vanillaOrder.contains(type) && !(type instanceof ICubicWorldType)
                    && !WRAPPERS.containsKey(type)) {
                moddedTypes.add(type);
            }
        }
        Collections.sort(moddedTypes, Comparator
                .comparing(WorldType::getName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(type -> type.getClass().getName()));
        for (WorldType baseType : moddedTypes) {
            register(baseType, WorldTypeNaming.moddedWrapperName(
                    baseType.getName(), baseType.getClass().getName()), TerrainProfile.DELEGATED,
                    occupiedNames);
        }
    }

    public static CavesNotCliffsWorldTypeWrapper wrapperForBase(WorldType baseType) {
        return WRAPPERS.get(baseType);
    }

    public static CavesNotCliffsWorldTypeWrapper wrapperForPersistedBase(
            String baseName, String baseClassName) {
        WorldType baseType = BASE_TYPES_BY_NAME.get(normalize(baseName));
        if (baseType == null) {
            return null;
        }
        if (baseClassName != null && !baseClassName.isEmpty()
                && !baseType.getClass().getName().equals(baseClassName)) {
            return null;
        }
        return wrapperForBase(baseType);
    }

    public static boolean isWrapper(WorldType type) {
        return type instanceof CavesNotCliffsWorldTypeWrapper;
    }

    private static void register(WorldType baseType, String wrapperName, TerrainProfile profile,
            Set<String> occupiedNames) {
        String normalizedBaseName = normalize(baseType.getName());
        WorldType duplicate = BASE_TYPES_BY_NAME.put(normalizedBaseName, baseType);
        if (duplicate != null && duplicate != baseType) {
            throw new IllegalStateException("Cannot persist Caves Not Cliffs wrapper unambiguously; "
                    + "multiple world types use the name '" + baseType.getName() + "'");
        }

        String normalizedWrapperName = normalize(wrapperName);
        if (!occupiedNames.add(normalizedWrapperName)) {
            throw new IllegalStateException("Caves Not Cliffs world type name collision: " + wrapperName);
        }
        CavesNotCliffsWorldTypeWrapper wrapper =
                new CavesNotCliffsWorldTypeWrapper(wrapperName, baseType, profile);
        WRAPPERS.put(baseType, wrapper);
    }

    private static List<WorldType> distinctSnapshot() {
        Set<WorldType> distinct = Collections.newSetFromMap(new IdentityHashMap<>());
        List<WorldType> result = new ArrayList<>();
        for (WorldType type : WorldType.WORLD_TYPES) {
            if (type != null && distinct.add(type)) {
                result.add(type);
            }
        }
        return result;
    }

    private static String fixedName(WorldType type) {
        if (type == WorldType.DEFAULT) {
            return "cnc_default";
        }
        if (type == WorldType.FLAT) {
            return "cnc_flat";
        }
        if (type == WorldType.LARGE_BIOMES) {
            return "cnc_large";
        }
        if (type == WorldType.AMPLIFIED) {
            return "cnc_amplified";
        }
        if (type == WorldType.CUSTOMIZED) {
            return "cnc_custom";
        }
        if (type == WorldType.DEBUG_ALL_BLOCK_STATES) {
            return "cnc_debug";
        }
        if (type == WorldType.DEFAULT_1_1) {
            return "cnc_default11";
        }
        throw new IllegalArgumentException("No fixed wrapper name for " + type.getName());
    }

    private static String normalize(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }
}

package net.celestiald.cavesnotcliffs.world;

import java.util.Locale;

/** Stable terrain profile persisted with the v2 world schema. */
public enum TerrainProfile {
    DEFAULT("default"),
    LARGE_BIOMES("large_biomes"),
    AMPLIFIED("amplified"),
    DELEGATED("delegated");

    private final String serializedName;

    TerrainProfile(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public static TerrainProfile forVanillaTypeName(String typeName) {
        if ("default".equals(typeName)) {
            return DEFAULT;
        }
        if ("largeBiomes".equals(typeName)) {
            return LARGE_BIOMES;
        }
        if ("amplified".equals(typeName)) {
            return AMPLIFIED;
        }
        return DELEGATED;
    }

    public static TerrainProfile bySerializedName(String name) {
        if (name != null) {
            String normalized = name.toLowerCase(Locale.ROOT);
            for (TerrainProfile profile : values()) {
                if (profile.serializedName.equals(normalized)) {
                    return profile;
                }
            }
        }
        return DELEGATED;
    }
}

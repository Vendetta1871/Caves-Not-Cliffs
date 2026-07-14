package net.celestiald.cavesnotcliffs.world;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.WorldInfo;

/** Immutable view of the Caves Not Cliffs generator contract stored in level.dat. */
public final class CavesNotCliffsWorldData {
    public static final int LEGACY_SCHEMA = 1;
    public static final int CURRENT_SCHEMA = 2;

    private static final String ROOT_KEY = "cavesnotcliffs";
    private static final String SCHEMA_KEY = "terrainSchema";
    private static final String BASE_TYPE_KEY = "baseType";
    private static final String BASE_CLASS_KEY = "baseTypeClass";
    private static final String OPTIONS_KEY = "generatorOptions";
    private static final String PROFILE_KEY = "terrainProfile";

    private final int terrainSchema;
    private final String baseTypeName;
    private final String baseTypeClass;
    private final String generatorOptions;
    private final String terrainProfileName;
    private final TerrainProfile terrainProfile;

    private CavesNotCliffsWorldData(int terrainSchema, String baseTypeName,
            String baseTypeClass, String generatorOptions, String terrainProfileName) {
        this.terrainSchema = terrainSchema;
        this.baseTypeName = baseTypeName;
        this.baseTypeClass = baseTypeClass;
        this.generatorOptions = generatorOptions;
        this.terrainProfileName = terrainProfileName;
        this.terrainProfile = TerrainProfile.bySerializedName(terrainProfileName);
    }

    public int getTerrainSchema() {
        return terrainSchema;
    }

    public String getBaseTypeName() {
        return baseTypeName;
    }

    public String getBaseTypeClass() {
        return baseTypeClass;
    }

    public String getGeneratorOptions() {
        return generatorOptions;
    }

    public TerrainProfile getTerrainProfile() {
        return terrainProfile;
    }

    public static CavesNotCliffsWorldData read(WorldInfo worldInfo) {
        NBTTagCompound dimensionData = worldInfo.getDimensionData(0);
        if (!dimensionData.hasKey(ROOT_KEY, 10)) {
            return null;
        }
        NBTTagCompound tag = dimensionData.getCompoundTag(ROOT_KEY);
        if (!tag.hasKey(SCHEMA_KEY, 99)) {
            return null;
        }
        return new CavesNotCliffsWorldData(
                tag.getInteger(SCHEMA_KEY),
                tag.getString(BASE_TYPE_KEY),
                tag.getString(BASE_CLASS_KEY),
                tag.getString(OPTIONS_KEY),
                tag.getString(PROFILE_KEY));
    }

    public static CavesNotCliffsWorldData writeLegacy(WorldInfo worldInfo) {
        return write(worldInfo, LEGACY_SCHEMA, WorldType.DEFAULT, TerrainProfile.DEFAULT,
                safeOptions(worldInfo.getGeneratorOptions()));
    }

    public static CavesNotCliffsWorldData writeCurrent(WorldInfo worldInfo, WorldType baseType,
            TerrainProfile profile) {
        return write(worldInfo, CURRENT_SCHEMA, baseType, profile,
                safeOptions(worldInfo.getGeneratorOptions()));
    }

    private static CavesNotCliffsWorldData write(WorldInfo worldInfo, int schema,
            WorldType baseType, TerrainProfile profile, String options) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(SCHEMA_KEY, schema);
        tag.setString(BASE_TYPE_KEY, baseType.getName());
        tag.setString(BASE_CLASS_KEY, baseType.getClass().getName());
        tag.setString(OPTIONS_KEY, options);
        tag.setString(PROFILE_KEY, profile.getSerializedName());

        NBTTagCompound dimensionData = worldInfo.getDimensionData(0);
        dimensionData.setTag(ROOT_KEY, tag);
        worldInfo.setDimensionData(0, dimensionData);
        return new CavesNotCliffsWorldData(schema, baseType.getName(),
                baseType.getClass().getName(), options, profile.getSerializedName());
    }

    /**
     * Verifies that the immutable level.dat contract still selects this exact generator. A saved
     * field is never silently replaced with today's inferred wrapper mapping, because doing so can
     * create a terrain seam in an existing world.
     */
    public void validateGeneratorContract(int expectedSchema, WorldType expectedBaseType,
            TerrainProfile expectedProfile) {
        if (terrainSchema != expectedSchema) {
            throw mismatch("terrain schema", Integer.toString(terrainSchema),
                    Integer.toString(expectedSchema));
        }
        if (!expectedBaseType.getName().equals(baseTypeName)) {
            throw mismatch("base world type", baseTypeName, expectedBaseType.getName());
        }
        String expectedClass = expectedBaseType.getClass().getName();
        if (!expectedClass.equals(baseTypeClass)) {
            throw mismatch("base world type class", baseTypeClass, expectedClass);
        }
        String expectedProfileName = expectedProfile.getSerializedName();
        if (!expectedProfileName.equals(terrainProfileName)) {
            throw mismatch("terrain profile", terrainProfileName, expectedProfileName);
        }
    }

    private static IllegalStateException mismatch(String field, String saved, String expected) {
        return new IllegalStateException("Saved Caves Not Cliffs " + field + " '" + saved
                + "' does not match the registered generator contract '" + expected + "'");
    }

    private static String safeOptions(String options) {
        return options == null ? "" : options;
    }
}

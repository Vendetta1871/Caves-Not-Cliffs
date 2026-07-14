package net.celestiald.cavesnotcliffs.world;

/** Marker shared by the schema-1 compatibility alias and schema-2 finite-world wrappers. */
public interface CavesNotCliffsFiniteWorldType {
    int getTerrainSchema();

    TerrainProfile getTerrainProfile();
}

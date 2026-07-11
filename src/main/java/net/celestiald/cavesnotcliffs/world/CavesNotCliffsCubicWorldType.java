package net.celestiald.cavesnotcliffs.world;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldType;

/** Marker shared by the schema-1 compatibility alias and schema-2 hidden wrappers. */
public interface CavesNotCliffsCubicWorldType extends ICubicWorldType {
    int getTerrainSchema();

    TerrainProfile getTerrainProfile();
}

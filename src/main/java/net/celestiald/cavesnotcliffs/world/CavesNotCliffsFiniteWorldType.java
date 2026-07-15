package net.celestiald.cavesnotcliffs.world;

import net.celestiald.cavebiomes.api.IExtendedHeightWorldType;

/** Marker shared by the schema-1 compatibility alias and schema-2 finite-world wrappers. */
public interface CavesNotCliffsFiniteWorldType extends IExtendedHeightWorldType {
    int getTerrainSchema();

    TerrainProfile getTerrainProfile();
}

package net.celestiald.cavesnotcliffs.registry;

/** Stable save-format versions shared by terrain and content migration code. */
public final class CncDataVersions {
    public static final int LEGACY_TERRAIN_SCHEMA = 1;
    public static final int V118_TERRAIN_SCHEMA = 2;

    /** No Caves Not Cliffs content migration marker was stored by released pre-v2 builds. */
    public static final int LEGACY_CONTENT_VERSION = 0;

    /** Registry canonicalization and the retained-geode-marker conversion checkpoint. */
    public static final int CANONICAL_REGISTRY_CONTENT_VERSION = 1;

    /** Reserved final v2 content schema; later migration steps advance toward this value. */
    public static final int V2_CONTENT_VERSION = 2000;

    private CncDataVersions() {
    }
}

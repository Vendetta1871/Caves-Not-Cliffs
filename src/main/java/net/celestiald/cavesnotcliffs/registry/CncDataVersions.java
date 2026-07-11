package net.celestiald.cavesnotcliffs.registry;

/** Stable save-format versions shared by terrain and content migration code. */
public final class CncDataVersions {
    public static final int LEGACY_TERRAIN_SCHEMA = 1;
    public static final int V118_TERRAIN_SCHEMA = 2;
    public static final int V2_CONTENT_VERSION = 2000;

    private CncDataVersions() {
    }
}

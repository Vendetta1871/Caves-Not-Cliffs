package net.celestiald.cavesnotcliffs.worldgen.v118;

/** Generation-local material palette used before Forge block-state translation. */
public enum V118Material {
    AIR,
    STONE,
    WATER,
    LAVA,
    BEDROCK,
    DEEPSLATE,
    TUFF,
    GRANITE,
    COPPER_ORE,
    RAW_COPPER_BLOCK,
    DEEPSLATE_IRON_ORE,
    RAW_IRON_BLOCK;

    private static final V118Material[] VALUES = values();

    public int storageId() {
        return ordinal();
    }

    public static V118Material fromStorageId(int storageId) {
        if (storageId < 0 || storageId >= VALUES.length) {
            throw new IllegalArgumentException("Unknown 1.18 material storage id: " + storageId);
        }
        return VALUES[storageId];
    }
}

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
    RAW_IRON_BLOCK,

    // SurfaceRuleData and SurfaceSystem outputs.  Append-only: storage ids above are already
    // persisted by draft-v2 terrain columns.
    DIRT,
    PODZOL,
    COARSE_DIRT,
    MYCELIUM,
    GRASS_BLOCK,
    CALCITE,
    GRAVEL,
    SAND,
    SANDSTONE,
    RED_SAND,
    RED_SANDSTONE,
    TERRACOTTA,
    WHITE_TERRACOTTA,
    ORANGE_TERRACOTTA,
    YELLOW_TERRACOTTA,
    BROWN_TERRACOTTA,
    RED_TERRACOTTA,
    LIGHT_GRAY_TERRACOTTA,
    PACKED_ICE,
    SNOW_BLOCK,
    POWDER_SNOW,
    ICE,

    // Ordinary configured ore/blob outputs. Append-only for draft-v2 storage compatibility.
    DIORITE,
    ANDESITE,
    COAL_ORE,
    DEEPSLATE_COAL_ORE,
    IRON_ORE,
    GOLD_ORE,
    DEEPSLATE_GOLD_ORE,
    DEEPSLATE_COPPER_ORE,
    REDSTONE_ORE,
    DEEPSLATE_REDSTONE_ORE,
    LAPIS_ORE,
    DEEPSLATE_LAPIS_ORE,
    DIAMOND_ORE,
    DEEPSLATE_DIAMOND_ORE,
    EMERALD_ORE,
    DEEPSLATE_EMERALD_ORE,
    INFESTED_STONE,
    INFESTED_DEEPSLATE;

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

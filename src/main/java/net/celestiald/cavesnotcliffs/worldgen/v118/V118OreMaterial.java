package net.celestiald.cavesnotcliffs.worldgen.v118;

/**
 * Isolated material palette for ordinary configured ore/blob decoration.
 *
 * <p>This deliberately has no persisted storage id. The column palette is a save contract and
 * decoration integration must translate these values to its own mutable block representation.</p>
 */
public enum V118OreMaterial {
    AIR,
    WATER,
    STONE,
    DEEPSLATE,
    TUFF,
    GRANITE,
    DIORITE,
    ANDESITE,
    DIRT,
    GRAVEL,
    CLAY,
    GRASS_BLOCK,
    SAND,
    SANDSTONE,
    MAGMA,
    COAL_ORE,
    DEEPSLATE_COAL_ORE,
    IRON_ORE,
    DEEPSLATE_IRON_ORE,
    GOLD_ORE,
    DEEPSLATE_GOLD_ORE,
    COPPER_ORE,
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
    INFESTED_DEEPSLATE,
    /** Any non-air runtime block that is not a configured ore replacement target. */
    OTHER
}

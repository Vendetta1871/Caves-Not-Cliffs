package net.celestiald.cavesnotcliffs.content;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Java 1.18.2 compost chances for every item represented by the 1.12 runtime or this backport.
 *
 * <p>Several 1.12 blocks share one item id and use metadata. Exact entries therefore take
 * precedence over wildcard entries, preserving the distinct short-grass, fern, tall-grass,
 * large-fern, and cocoa-bean chances.</p>
 */
public final class ComposterCompostables {
    public static final int WILDCARD = -1;
    public static final float NOT_COMPOSTABLE = -1.0F;

    private static final List<Definition> DEFINITIONS = buildDefinitions();
    private static final Map<String, Float> CHANCES = buildChanceMap(DEFINITIONS);

    private ComposterCompostables() {
    }

    public static float chance(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null) {
            return NOT_COMPOSTABLE;
        }
        return chance(stack.getItem().getRegistryName(), stack.getMetadata());
    }

    public static float chance(ResourceLocation itemId, int metadata) {
        if (itemId == null) {
            return NOT_COMPOSTABLE;
        }
        Float exact = CHANCES.get(key(itemId.toString(), metadata));
        if (exact != null) {
            return exact;
        }
        Float wildcard = CHANCES.get(key(itemId.toString(), WILDCARD));
        return wildcard == null ? NOT_COMPOSTABLE : wildcard;
    }

    public static boolean contains(ItemStack stack) {
        return chance(stack) >= 0.0F;
    }

    public static List<Definition> definitions() {
        return DEFINITIONS;
    }

    private static List<Definition> buildDefinitions() {
        List<Definition> entries = new ArrayList<>();

        // 30 percent.
        add(entries, 0.30F, "minecraft:leaves");
        add(entries, 0.30F, "minecraft:leaves2");
        add(entries, 0.30F, "minecraft:sapling");
        add(entries, 0.30F, "minecraft:beetroot_seeds");
        add(entries, 0.30F, "minecraft:tallgrass", 1); // short grass
        add(entries, 0.30F, "minecraft:melon_seeds");
        add(entries, 0.30F, "minecraft:pumpkin_seeds");
        add(entries, 0.30F, "minecraft:wheat_seeds");
        add(entries, 0.30F, "cavesnotcliffs:azalea_leaves");
        add(entries, 0.30F, "cavesnotcliffs:glow_berries");
        add(entries, 0.30F, "cavesnotcliffs:moss_carpet");
        add(entries, 0.30F, "cavesnotcliffs:small_dripleaf");
        add(entries, 0.30F, "cavesnotcliffs:hanging_roots");

        // 50 percent.
        add(entries, 0.50F, "minecraft:double_plant", 2); // tall grass
        add(entries, 0.50F, "minecraft:cactus");
        add(entries, 0.50F, "minecraft:reeds");
        add(entries, 0.50F, "minecraft:vine");
        add(entries, 0.50F, "minecraft:melon");
        add(entries, 0.50F, "cavesnotcliffs:flowering_azalea_leaves");

        // 65 percent.
        add(entries, 0.65F, "minecraft:waterlily");
        // The target runtime's minecraft:pumpkin is the carved peer.
        add(entries, 0.65F, "cavesnotcliffs:pumpkin");
        add(entries, 0.65F, "minecraft:melon_block");
        add(entries, 0.65F, "minecraft:apple");
        add(entries, 0.65F, "minecraft:beetroot");
        add(entries, 0.65F, "minecraft:carrot");
        add(entries, 0.65F, "minecraft:dye", 3); // cocoa beans
        add(entries, 0.65F, "minecraft:potato");
        add(entries, 0.65F, "minecraft:wheat");
        add(entries, 0.65F, "minecraft:brown_mushroom");
        add(entries, 0.65F, "minecraft:red_mushroom");
        add(entries, 0.65F, "minecraft:nether_wart");
        add(entries, 0.65F, "minecraft:yellow_flower");
        add(entries, 0.65F, "minecraft:red_flower");
        add(entries, 0.65F, "minecraft:tallgrass", 2); // fern
        add(entries, 0.65F, "minecraft:double_plant", 0); // sunflower
        add(entries, 0.65F, "minecraft:double_plant", 1); // lilac
        add(entries, 0.65F, "minecraft:double_plant", 3); // large fern
        add(entries, 0.65F, "minecraft:double_plant", 4); // rose bush
        add(entries, 0.65F, "minecraft:double_plant", 5); // peony
        add(entries, 0.65F, "cavesnotcliffs:spore_blossom");
        add(entries, 0.65F, "cavesnotcliffs:azalea");
        add(entries, 0.65F, "cavesnotcliffs:moss_block");
        add(entries, 0.65F, "cavesnotcliffs:big_dripleaf");

        // 85 percent.
        add(entries, 0.85F, "minecraft:hay_block");
        add(entries, 0.85F, "minecraft:brown_mushroom_block");
        add(entries, 0.85F, "minecraft:red_mushroom_block");
        add(entries, 0.85F, "minecraft:nether_wart_block");
        add(entries, 0.85F, "minecraft:bread");
        add(entries, 0.85F, "minecraft:baked_potato");
        add(entries, 0.85F, "minecraft:cookie");
        add(entries, 0.85F, "cavesnotcliffs:flowering_azalea");

        // Guaranteed.
        add(entries, 1.0F, "minecraft:cake");
        add(entries, 1.0F, "minecraft:pumpkin_pie");

        return Collections.unmodifiableList(entries);
    }

    private static Map<String, Float> buildChanceMap(List<Definition> definitions) {
        Map<String, Float> result = new LinkedHashMap<>();
        for (Definition definition : definitions) {
            String key = key(definition.itemId, definition.metadata);
            if (result.put(key, definition.chance) != null) {
                throw new IllegalStateException("Duplicate composter definition: " + key);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static void add(List<Definition> entries, float chance, String itemId) {
        add(entries, chance, itemId, WILDCARD);
    }

    private static void add(List<Definition> entries, float chance, String itemId,
            int metadata) {
        entries.add(new Definition(itemId, metadata, chance));
    }

    private static String key(String itemId, int metadata) {
        return itemId + '#' + metadata;
    }

    public static final class Definition {
        public final String itemId;
        public final int metadata;
        public final float chance;

        Definition(String itemId, int metadata, float chance) {
            this.itemId = itemId;
            this.metadata = metadata;
            this.chance = chance;
        }
    }
}

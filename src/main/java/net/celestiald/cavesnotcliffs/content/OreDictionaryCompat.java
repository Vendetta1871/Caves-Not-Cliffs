package net.celestiald.cavesnotcliffs.content;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

/** Canonical 1.12 ore names for machine recipes and material unifiers. */
public final class OreDictionaryCompat {
    private OreDictionaryCompat() {
    }

    public static void register() {
        register("oreCoal", "deepslate_coal_ore");
        register("oreIron", "deepslate_iron_ore");
        register("oreCopper", "copper_ore");
        register("oreCopper", "deepslate_copper_ore");
        register("oreGold", "deepslate_gold_ore");
        register("oreRedstone", "deepslate_redstone_ore");
        register("oreLapis", "deepslate_lapis_ore");
        register("oreDiamond", "deepslate_diamond_ore");
        register("oreEmerald", "deepslate_emerald_ore");

        registerItem("ingotCopper", "copper_ingot");
        register("blockCopper", "copper_block");
        registerItem("rawCopper", "raw_copper");
        registerItem("rawIron", "raw_iron");
        registerItem("rawGold", "raw_gold");
        register("blockRawCopper", "raw_copper_block");
        register("blockRawIron", "raw_iron_block");
        register("blockRawGold", "raw_gold_block");

        registerItem("gemAmethyst", "amethyst_shard");
        register("blockAmethyst", "amethyst_block");
        register("blockCalcite", "calcite");
        register("blockDeepslate", "deepslate");
        register("blockTuff", "tuff");
    }

    private static void register(String oreName, String blockName) {
        Block block = CncMaterialContent.block(blockName);
        if (block == null) {
            throw new IllegalStateException("Missing block for ore dictionary: " + blockName);
        }
        OreDictionary.registerOre(oreName, new ItemStack(block));
    }

    private static void registerItem(String oreName, String itemName) {
        Item item = CncMaterialContent.item(itemName);
        if (item == null) {
            throw new IllegalStateException("Missing item for ore dictionary: " + itemName);
        }
        OreDictionary.registerOre(oreName, new ItemStack(item));
    }
}

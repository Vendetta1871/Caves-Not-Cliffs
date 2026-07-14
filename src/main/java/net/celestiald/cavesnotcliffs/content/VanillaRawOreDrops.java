package net.celestiald.cavesnotcliffs.content;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;
import java.util.Random;

/** Backports the 1.18 raw-material loot tables onto 1.12's vanilla iron and gold ores. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class VanillaRawOreDrops {
    private VanillaRawOreDrops() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void replaceVanillaOreDrops(BlockEvent.HarvestDropsEvent event) {
        Block block = event.getState().getBlock();
        String replacement = replacementPath(block, event.isSilkTouching());
        if (replacement == null) {
            return;
        }

        // Transform vanilla's original self-drop early in the event pipeline. If another mod has
        // already replaced or augmented it, leave that deliberate behavior intact.
        if (!isVanillaSelfDrop(block, event.getDrops())) {
            return;
        }

        Item rawMaterial = CncMaterialContent.item(replacement);
        if (rawMaterial == null) {
            throw new IllegalStateException("Raw ore drop is not registered: " + replacement);
        }

        Random random = event.getWorld().rand;
        int count = rollRawCount(event.getFortuneLevel(), random);
        count = applyExplosionDecay(count, event.getDropChance(), random);
        event.getDrops().clear();
        if (count > 0) {
            event.getDrops().add(new ItemStack(rawMaterial, count));
        }
        // Forge rolls this once per stack after the event. Java 1.18's explosion_decay instead
        // filters every raw item independently, so consume the chance above and disable the
        // legacy all-or-nothing roll.
        event.setDropChance(1.0F);
    }

    static String replacementPath(Block block, boolean silkTouching) {
        if (silkTouching) {
            return null;
        }
        if (block == Blocks.IRON_ORE) {
            return "raw_iron";
        }
        if (block == Blocks.GOLD_ORE) {
            return "raw_gold";
        }
        return null;
    }

    static int rollRawCount(int fortune, Random random) {
        return OreDropLogic.applyOreBonus(1, fortune, random);
    }

    static int applyExplosionDecay(int count, float survivalChance, Random random) {
        if (count <= 0 || survivalChance <= 0.0F) {
            return 0;
        }
        if (survivalChance >= 1.0F) {
            return count;
        }
        int surviving = 0;
        for (int item = 0; item < count; item++) {
            if (random.nextFloat() <= survivalChance) {
                surviving++;
            }
        }
        return surviving;
    }

    static boolean isVanillaSelfDrop(Block block, List<ItemStack> drops) {
        return drops.size() == 1
                && drops.get(0).getCount() == 1
                && drops.get(0).getMetadata() == 0
                && drops.get(0).getItem() == Item.getItemFromBlock(block);
    }
}

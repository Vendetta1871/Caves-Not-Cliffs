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

        int count = rollRawCount(event.getFortuneLevel(), event.getWorld().rand);
        event.getDrops().clear();
        event.getDrops().add(new ItemStack(rawMaterial, count));
        // Preserve Forge's incoming drop chance. For base-count-one ores it is the 1.12
        // equivalent of 1.18's survives_explosion/explosion_decay behavior.
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

    static boolean isVanillaSelfDrop(Block block, List<ItemStack> drops) {
        return drops.size() == 1
                && drops.get(0).getCount() == 1
                && drops.get(0).getMetadata() == 0
                && drops.get(0).getItem() == Item.getItemFromBlock(block);
    }
}

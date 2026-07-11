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

import java.util.Random;

/** Backports the 1.18 raw-material loot tables onto 1.12's vanilla iron and gold ores. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class VanillaRawOreDrops {
    private VanillaRawOreDrops() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void replaceVanillaOreDrops(BlockEvent.HarvestDropsEvent event) {
        String replacement = replacementPath(event.getState().getBlock(), event.isSilkTouching());
        if (replacement == null) {
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
}

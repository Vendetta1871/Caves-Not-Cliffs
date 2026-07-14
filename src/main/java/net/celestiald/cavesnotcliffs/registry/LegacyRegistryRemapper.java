package net.celestiald.cavesnotcliffs.registry;

import net.celestiald.cavesnotcliffs.CavesNotCliffs;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Remaps released placeholder IDs to their canonical v2 identities during save loading. */
@Mod.EventBusSubscriber(modid = CavesNotCliffs.MODID)
public final class LegacyRegistryRemapper {
    private static final Logger LOGGER = LogManager.getLogger("CavesNotCliffs/RegistryMigration");

    private LegacyRegistryRemapper() {
    }

    @SubscribeEvent
    public static void remapBlocks(RegistryEvent.MissingMappings<Block> event) {
        for (RegistryEvent.MissingMappings.Mapping<Block> mapping : event.getMappings()) {
            ResourceLocation targetName = LegacyContentMappings.canonicalBlockLocation(mapping.key);
            if (targetName.equals(mapping.key)) {
                continue;
            }
            Block target = ForgeRegistries.BLOCKS.getValue(targetName);
            if (target == null) {
                LOGGER.error("Cannot remap legacy block {}: canonical target {} is not registered",
                        mapping.key, targetName);
            } else {
                mapping.remap(target);
                LOGGER.info("Remapped legacy block {} to {}", mapping.key, targetName);
            }
        }
    }

    @SubscribeEvent
    public static void remapItems(RegistryEvent.MissingMappings<Item> event) {
        for (RegistryEvent.MissingMappings.Mapping<Item> mapping : event.getMappings()) {
            ResourceLocation targetName = LegacyContentMappings.canonicalItemLocation(mapping.key);
            if (targetName.equals(mapping.key)) {
                continue;
            }
            Item target = ForgeRegistries.ITEMS.getValue(targetName);
            if (target == null) {
                LOGGER.error("Cannot remap legacy item {}: canonical target {} is not registered",
                        mapping.key, targetName);
            } else {
                mapping.remap(target);
                LOGGER.info("Remapped legacy item {} to {}", mapping.key, targetName);
            }
        }
    }
}

package net.celestiald.cavesnotcliffs.registry;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;

/**
 * Serialized-ItemStack adapter used by upgrade fixtures and nested inventories.
 *
 * <p>Forge missing mappings remain the primary whole-registry migration path. This adapter uses
 * the same mapping table for inventories serialized outside the normal registry snapshot flow and
 * is deliberately idempotent.</p>
 */
public final class LegacyInventoryMigration {
    private LegacyInventoryMigration() {
    }

    public static boolean migrateSerializedStack(NBTTagCompound stack) {
        if (!stack.hasKey("id", 8) || stack.getString("id").isEmpty()) {
            return false;
        }
        ResourceLocation legacy = new ResourceLocation(stack.getString("id"));
        ResourceLocation canonical = LegacyContentMappings.canonicalItemLocation(legacy);
        if (canonical.equals(legacy)) {
            return false;
        }
        stack.setString("id", canonical.toString());
        return true;
    }

    public static int migrateSerializedStacks(NBTTagList stacks) {
        int changed = 0;
        for (int index = 0; index < stacks.tagCount(); index++) {
            if (migrateSerializedStack(stacks.getCompoundTagAt(index))) {
                changed++;
            }
        }
        return changed;
    }
}

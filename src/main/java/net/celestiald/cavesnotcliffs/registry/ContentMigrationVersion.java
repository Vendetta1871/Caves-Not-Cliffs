package net.celestiald.cavesnotcliffs.registry;

import net.minecraft.nbt.NBTTagCompound;

/** Reads and writes the per-chunk/cube content migration checkpoint without replacing other data. */
public final class ContentMigrationVersion {
    private static final String ROOT_KEY = "cavesnotcliffs";
    private static final String VERSION_KEY = "contentVersion";

    private ContentMigrationVersion() {
    }

    public static int read(NBTTagCompound data) {
        if (!data.hasKey(ROOT_KEY, 10)) {
            return CncDataVersions.LEGACY_CONTENT_VERSION;
        }
        NBTTagCompound root = data.getCompoundTag(ROOT_KEY);
        return root.hasKey(VERSION_KEY, 99)
                ? root.getInteger(VERSION_KEY)
                : CncDataVersions.LEGACY_CONTENT_VERSION;
    }

    public static void write(NBTTagCompound data, int version) {
        NBTTagCompound root = data.hasKey(ROOT_KEY, 10)
                ? data.getCompoundTag(ROOT_KEY)
                : new NBTTagCompound();
        root.setInteger(VERSION_KEY, version);
        data.setTag(ROOT_KEY, root);
    }
}

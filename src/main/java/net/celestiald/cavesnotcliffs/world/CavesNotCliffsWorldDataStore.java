package net.celestiald.cavesnotcliffs.world;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Persists the generator contract outside Forge's provider-owned dimension-data entries. */
final class CavesNotCliffsWorldDataStore {
    private static final String FILE_NAME = "cavesnotcliffs";
    private static final String DATA_KEY = "data";

    private CavesNotCliffsWorldDataStore() {
    }

    static void restore(WorldInfo worldInfo, ISaveHandler saveHandler) {
        File file = saveHandler.getMapFileFromName(FILE_NAME);
        if (file == null || !file.isFile()) {
            return;
        }
        NBTTagCompound levelData = CavesNotCliffsWorldData.copyPersistedTag(worldInfo);
        try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(file))) {
            NBTTagCompound root = CompressedStreamTools.readCompressed(input);
            if (!root.hasKey(DATA_KEY, 10)) {
                throw new IOException("missing data compound");
            }
            NBTTagCompound sidecarData = root.getCompoundTag(DATA_KEY);
            if (levelData != null) {
                if (!levelData.equals(sidecarData)) {
                    throw new IOException("level.dat and sidecar contracts disagree");
                }
                return;
            }
            CavesNotCliffsWorldData.installPersistedTag(worldInfo, sidecarData);
        } catch (IOException | RuntimeException exception) {
            throw failure("read", file, exception);
        }
    }

    static void persist(WorldInfo worldInfo, ISaveHandler saveHandler) {
        NBTTagCompound data = CavesNotCliffsWorldData.copyPersistedTag(worldInfo);
        if (data == null) {
            return;
        }
        File file = saveHandler.getMapFileFromName(FILE_NAME);
        if (file == null) {
            throw new IllegalStateException("Caves Not Cliffs save handler returned no data path");
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IllegalStateException("Could not create Caves Not Cliffs data directory "
                    + parent);
        }
        File temporary = new File(parent, file.getName() + ".tmp");
        NBTTagCompound root = new NBTTagCompound();
        root.setTag(DATA_KEY, data);
        try {
            try (BufferedOutputStream output =
                    new BufferedOutputStream(new FileOutputStream(temporary))) {
                CompressedStreamTools.writeCompressed(root, output);
            }
            moveIntoPlace(temporary, file);
        } catch (IOException exception) {
            temporary.delete();
            throw failure("write", file, exception);
        }
    }

    private static void moveIntoPlace(File temporary, File file) throws IOException {
        try {
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static IllegalStateException failure(String action, File file, Exception cause) {
        return new IllegalStateException("Could not " + action
                + " Caves Not Cliffs world-format data " + file, cause);
    }
}

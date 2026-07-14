package net.celestiald.cavesnotcliffs.migration.cubic;

import net.minecraft.nbt.NBTTagCompound;

import java.nio.file.Path;
import java.util.OptionalInt;

/**
 * A coordinate-verified NBT entry from the legacy RegionLib storage.
 */
public final class CubicRegionEntry {
    public enum Kind {
        COLUMN,
        CUBE
    }

    public enum Storage {
        REGION,
        EXTENSION
    }

    private final Kind kind;
    private final Storage storage;
    private final Path source;
    private final int regionX;
    private final int regionY;
    private final int regionZ;
    private final int entryId;
    private final int x;
    private final int y;
    private final int z;
    private final int sectorOffset;
    private final int sectorCount;
    private final int compressedLength;
    private final NBTTagCompound nbt;

    CubicRegionEntry(Kind kind, Storage storage, Path source,
            int regionX, int regionY, int regionZ, int entryId,
            int x, int y, int z, int sectorOffset, int sectorCount,
            int compressedLength, NBTTagCompound nbt) {
        this.kind = kind;
        this.storage = storage;
        this.source = source;
        this.regionX = regionX;
        this.regionY = regionY;
        this.regionZ = regionZ;
        this.entryId = entryId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.sectorOffset = sectorOffset;
        this.sectorCount = sectorCount;
        this.compressedLength = compressedLength;
        this.nbt = nbt;
    }

    public Kind getKind() {
        return kind;
    }

    public Storage getStorage() {
        return storage;
    }

    public Path getSource() {
        return source;
    }

    public int getRegionX() {
        return regionX;
    }

    public OptionalInt getRegionY() {
        return kind == Kind.CUBE ? OptionalInt.of(regionY) : OptionalInt.empty();
    }

    public int getRegionZ() {
        return regionZ;
    }

    public int getEntryId() {
        return entryId;
    }

    public int getX() {
        return x;
    }

    public OptionalInt getY() {
        return kind == Kind.CUBE ? OptionalInt.of(y) : OptionalInt.empty();
    }

    public int getZ() {
        return z;
    }

    public OptionalInt getSectorOffset() {
        return storage == Storage.REGION ? OptionalInt.of(sectorOffset) : OptionalInt.empty();
    }

    public OptionalInt getSectorCount() {
        return storage == Storage.REGION ? OptionalInt.of(sectorCount) : OptionalInt.empty();
    }

    public int getCompressedLength() {
        return compressedLength;
    }

    public NBTTagCompound getNbt() {
        return nbt;
    }

    public String describeCoordinates() {
        return kind == Kind.CUBE
                ? "cube (" + x + ", " + y + ", " + z + ")"
                : "column (" + x + ", " + z + ")";
    }
}

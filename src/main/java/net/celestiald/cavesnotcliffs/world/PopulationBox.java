package net.celestiald.cavesnotcliffs.world;

import java.util.Objects;

/** Immutable chunk/section dependency box retained for deterministic generation tests. */
final class PopulationBox {
    static final PopulationBox NONE = new PopulationBox(0, 0, 0, 0, 0, 0);

    final int minX;
    final int minY;
    final int minZ;
    final int maxX;
    final int maxY;
    final int maxZ;

    PopulationBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PopulationBox)) {
            return false;
        }
        PopulationBox box = (PopulationBox) other;
        return minX == box.minX && minY == box.minY && minZ == box.minZ
                && maxX == box.maxX && maxY == box.maxY && maxZ == box.maxZ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(minX, minY, minZ, maxX, maxY, maxZ);
    }
}

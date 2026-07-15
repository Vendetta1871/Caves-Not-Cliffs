package net.celestiald.cavesnotcliffs.worldgen.v118;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Exact 3x3 section-biome union used by Java 1.18.2 feature decoration. */
public final class V118BiomeDecorationUnion {
    private static final V118Biome[] BIOMES = V118Biome.values();

    private V118BiomeDecorationUnion() {
    }

    public static Set<V118Biome> collect(int centerChunkX, int centerChunkZ,
            ColumnLookup columns) {
        if (columns == null) {
            throw new NullPointerException("columns");
        }
        EnumSet<V118Biome> result = EnumSet.noneOf(V118Biome.class);
        for (int chunkZ = centerChunkZ - 1; chunkZ <= centerChunkZ + 1; ++chunkZ) {
            for (int chunkX = centerChunkX - 1; chunkX <= centerChunkX + 1; ++chunkX) {
                TerrainColumn column = columns.column(chunkX, chunkZ);
                if (column == null) {
                    throw new IllegalStateException("Missing decoration column " + chunkX
                        + "," + chunkZ);
                }
                for (int quartY = TerrainColumn.MIN_QUART_Y;
                        quartY <= TerrainColumn.MAX_QUART_Y; ++quartY) {
                    for (int quartZ = 0; quartZ < TerrainColumn.QUART_WIDTH; ++quartZ) {
                        for (int quartX = 0; quartX < TerrainColumn.QUART_WIDTH; ++quartX) {
                            int id = column.virtualBiomeIdAtQuart(quartX, quartY, quartZ);
                            if (id < 0 || id >= BIOMES.length) {
                                throw new IllegalStateException("Unknown virtual biome id " + id);
                            }
                            result.add(BIOMES[id]);
                        }
                    }
                }
            }
        }
        return Collections.unmodifiableSet(result);
    }

    public interface ColumnLookup {
        TerrainColumn column(int chunkX, int chunkZ);
    }
}

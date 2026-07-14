package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class V118BiomeDecorationUnionTest {
    @Test
    public void collectsEveryQuartBiomeFromExactlyNineColumns() {
        Map<String, TerrainColumn> columns = new HashMap<>();
        V118Biome[] expected = {
            V118Biome.MEADOW, V118Biome.GROVE, V118Biome.SNOWY_SLOPES,
            V118Biome.JAGGED_PEAKS, V118Biome.FROZEN_PEAKS, V118Biome.STONY_PEAKS,
            V118Biome.BADLANDS, V118Biome.DRIPSTONE_CAVES, V118Biome.PLAINS
        };
        int index = 0;
        for (int chunkZ = -1; chunkZ <= 1; ++chunkZ) {
            for (int chunkX = -1; chunkX <= 1; ++chunkX) {
                V118Biome biome = expected[index++];
                columns.put(key(chunkX, chunkZ), TerrainColumn.builder(chunkX, chunkZ)
                    .fillMaterialIds(V118Material.STONE.storageId())
                    .fillSurfaceBiomeIds(biome.ordinal())
                    .fillVirtualBiomeIds(biome.ordinal())
                    .build());
            }
        }

        AtomicInteger calls = new AtomicInteger();
        Set<V118Biome> union = V118BiomeDecorationUnion.collect(0, 0, (x, z) -> {
            calls.incrementAndGet();
            return columns.get(key(x, z));
        });

        assertEquals(9, calls.get());
        assertEquals(9, union.size());
        for (V118Biome biome : expected) {
            assertTrue(biome.name(), union.contains(biome));
        }
    }

    private static String key(int x, int z) {
        return x + ":" + z;
    }
}

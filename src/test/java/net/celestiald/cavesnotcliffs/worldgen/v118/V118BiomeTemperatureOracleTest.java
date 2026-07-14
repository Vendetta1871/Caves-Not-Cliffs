package net.celestiald.cavesnotcliffs.worldgen.v118;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class V118BiomeTemperatureOracleTest {
    private static final String FIXTURE =
        "/net/celestiald/cavesnotcliffs/worldgen/v118/biome-temperature-oracle-1.18.2.tsv";
    private static final int[][] POINTS = {
        {0, 63, 0}, {0, 320, 0}, {-33, 97, 15}, {1024, 256, -2048},
        {33554431, 319, -33554433}
    };

    @Test
    public void allOverworldBiomeTemperaturesMatchOfficialServerAtRawFloatPrecision()
            throws IOException {
        InputStream input = getClass().getResourceAsStream(FIXTURE);
        assertNotNull("missing fixture " + FIXTURE, input);
        int biomeCount = 0;
        int assertionCount = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\t", -1);
                V118Biome biome;
                try {
                    biome = V118Biome.fromId(fields[0]);
                } catch (IllegalArgumentException outsideOverworldTable) {
                    continue;
                }
                assertEquals(line, Integer.parseUnsignedInt(fields[1]),
                    Float.floatToRawIntBits(biome.baseTemperature()));
                assertEquals(line, "FROZEN".equals(fields[2]),
                    biome.hasFrozenTemperatureModifier());
                for (int point = 0; point < POINTS.length; ++point) {
                    int offset = 3 + point * 3;
                    int x = POINTS[point][0];
                    int y = POINTS[point][1];
                    int z = POINTS[point][2];
                    assertEquals(line, Integer.parseUnsignedInt(fields[offset]),
                        Float.floatToRawIntBits(V118BiomeTemperature.temperature(biome, x, y, z)));
                    assertEquals(line, "1".equals(fields[offset + 1]),
                        V118BiomeTemperature.coldEnoughToSnow(biome, x, y, z));
                    assertEquals(line, "1".equals(fields[offset + 2]),
                        V118BiomeTemperature.shouldMeltFrozenOceanIcebergSlightly(
                            biome, x, y, z));
                    assertionCount += 3;
                }
                assertionCount += 2;
                ++biomeCount;
            }
        }
        assertEquals("Overworld biome metadata rows", V118Biome.values().length, biomeCount);
        assertEquals("temperature oracle assertions", biomeCount * 17, assertionCount);
    }
}

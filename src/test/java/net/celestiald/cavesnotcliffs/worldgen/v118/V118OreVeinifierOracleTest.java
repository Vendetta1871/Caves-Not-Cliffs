package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class V118OreVeinifierOracleTest {
    private static final String FIXTURE =
        "/net/celestiald/cavesnotcliffs/worldgen/v118/ore-vein-oracle-1.18.2.tsv";

    @Test
    public void materialSelectionMatchesOfficialOreVeinifier() throws IOException {
        Map<Long, PositionalRandomFactory> factories =
            new HashMap<Long, PositionalRandomFactory>();
        Map<V118Material, Integer> materialCounts =
            new EnumMap<V118Material, Integer>(V118Material.class);
        Set<Long> seeds = new HashSet<Long>();
        Set<Integer> verticalSamples = new HashSet<Integer>();
        Set<Long> toggleBits = new HashSet<Long>();
        int noneCount = 0;
        int records = 0;

        InputStream input = getClass().getResourceAsStream(FIXTURE);
        assertNotNull("missing fixture " + FIXTURE, input);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] fields = line.split("\\t", -1);
                if (fields.length != 9 || !"sample".equals(fields[0])) {
                    fail("Unknown fixture record: " + line);
                }
                long seed = Long.parseLong(fields[1]);
                int x = Integer.parseInt(fields[2]);
                int y = Integer.parseInt(fields[3]);
                int z = Integer.parseInt(fields[4]);
                double toggle = fromUnsignedBits(fields[5]);
                double ridged = fromUnsignedBits(fields[6]);
                double gap = fromUnsignedBits(fields[7]);
                PositionalRandomFactory factory = factories.get(seed);
                if (factory == null) {
                    factory = new XoroshiroRandomSource(seed).forkPositional()
                        .fromHashOf("minecraft:ore").forkPositional();
                    factories.put(seed, factory);
                }
                V118OreVeinifier veinifier = new V118OreVeinifier(
                    DensityFunctions.constant(toggle), DensityFunctions.constant(ridged),
                    DensityFunctions.constant(gap), factory);
                V118Material actual = veinifier.compute(x, y, z);
                V118Material expected = material(fields[8]);
                assertEquals(line, expected, actual);
                if (actual == null) {
                    ++noneCount;
                } else {
                    Integer count = materialCounts.get(actual);
                    materialCounts.put(actual, count == null ? 1 : count + 1);
                }
                seeds.add(seed);
                verticalSamples.add(y);
                toggleBits.add(Double.doubleToRawLongBits(toggle));
                ++records;
            }
        }

        assertEquals("fixture records", 1034, records);
        assertEquals("edge seeds", 6, seeds.size());
        assertEquals("vertical samples", 19, verticalSamples.size());
        assertEquals("toggle thresholds", 8, toggleBits.size());
        assertEquals("no replacement", 417, noneCount);
        assertCount(materialCounts, V118Material.COPPER_ORE, 76);
        assertCount(materialCounts, V118Material.RAW_COPPER_BLOCK, 2);
        assertCount(materialCounts, V118Material.GRANITE, 211);
        assertCount(materialCounts, V118Material.DEEPSLATE_IRON_ORE, 81);
        assertCount(materialCounts, V118Material.RAW_IRON_BLOCK, 2);
        assertCount(materialCounts, V118Material.TUFF, 245);
    }

    private static double fromUnsignedBits(String bits) {
        return Double.longBitsToDouble(Long.parseUnsignedLong(bits));
    }

    private static V118Material material(String name) {
        if ("none".equals(name)) {
            return null;
        }
        if ("copper_ore".equals(name)) {
            return V118Material.COPPER_ORE;
        }
        if ("raw_copper_block".equals(name)) {
            return V118Material.RAW_COPPER_BLOCK;
        }
        if ("granite".equals(name)) {
            return V118Material.GRANITE;
        }
        if ("deepslate_iron_ore".equals(name)) {
            return V118Material.DEEPSLATE_IRON_ORE;
        }
        if ("raw_iron_block".equals(name)) {
            return V118Material.RAW_IRON_BLOCK;
        }
        if ("tuff".equals(name)) {
            return V118Material.TUFF;
        }
        throw new AssertionError("Unknown official material: " + name);
    }

    private static void assertCount(Map<V118Material, Integer> counts,
            V118Material material, int expected) {
        assertEquals(material.name(), Integer.valueOf(expected), counts.get(material));
    }
}

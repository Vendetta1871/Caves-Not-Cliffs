package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OverworldBiomeBuilderOracleTest {
    private static final String FIXTURE =
        "/net/celestiald/cavesnotcliffs/worldgen/v118/overworld-biomes-oracle-1.18.2.tsv";

    @Test
    public void matchesOfficialOrderedTableAndLookupOracle() throws IOException {
        OverworldBiomeBuilder builder = new OverworldBiomeBuilder();
        List<Climate.Entry<V118Biome>> entries = builder.entries();
        Set<V118Biome> tableBiomes = EnumSet.noneOf(V118Biome.class);
        Set<V118Biome> resolvedBiomes = EnumSet.noneOf(V118Biome.class);
        int entryRecords = 0;
        int lookupRecords = 0;
        int expectedEntryCount = -1;
        int expectedLookupCount = -1;

        InputStream input = OverworldBiomeBuilderOracleTest.class.getResourceAsStream(FIXTURE);
        assertNotNull("missing fixture " + FIXTURE, input);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] fields = line.split("\\t", -1);
                switch (fields[0]) {
                    case "entry-count":
                        expectedEntryCount = Integer.parseInt(fields[1]);
                        assertEquals("table cardinality", expectedEntryCount, entries.size());
                        break;
                    case "entry":
                        verifyEntry(line, fields, entries);
                        tableBiomes.add(entries.get(Integer.parseInt(fields[1])).value());
                        ++entryRecords;
                        break;
                    case "lookup":
                        Climate.TargetPoint target = new Climate.TargetPoint(
                            Long.parseLong(fields[3]), Long.parseLong(fields[4]),
                            Long.parseLong(fields[5]), Long.parseLong(fields[6]),
                            Long.parseLong(fields[7]), Long.parseLong(fields[8]));
                        V118Biome expected = V118Biome.fromId(fields[9]);
                        V118Biome actual = builder.resolve(target);
                        assertEquals(message(line), expected, actual);
                        resolvedBiomes.add(actual);
                        assertEquals("lookup index", lookupRecords, Integer.parseInt(fields[1]));
                        ++lookupRecords;
                        break;
                    case "lookup-count":
                        expectedLookupCount = Integer.parseInt(fields[1]);
                        break;
                    default:
                        fail("unknown fixture record " + line);
                }
            }
        }

        assertEquals(7578, expectedEntryCount);
        assertEquals(expectedEntryCount, entryRecords);
        assertEquals(13280, expectedLookupCount);
        assertEquals(expectedLookupCount, lookupRecords);
        assertEquals("every declared table biome is represented", EnumSet.allOf(V118Biome.class),
            tableBiomes);
        assertEquals("lookup matrix reaches every declared table biome", EnumSet.allOf(V118Biome.class),
            resolvedBiomes);
    }

    @Test
    public void exposesCanonicalImmutableVirtualResolverContracts() {
        OverworldBiomeBuilder builder = new OverworldBiomeBuilder();
        assertEquals(builder.entries(), builder.parameters().values());
        assertEquals("minecraft:meadow", V118Biome.MEADOW.id());
        assertEquals(V118Biome.DRIPSTONE_CAVES,
            V118Biome.fromId("minecraft:dripstone_caves"));

        assertEquals(V118Biome.DRIPSTONE_CAVES,
            builder.resolve(new Climate.TargetPoint(0L, 0L, 9000L, 0L, 5000L, 0L)));
        assertEquals(V118Biome.LUSH_CAVES,
            builder.resolve(new Climate.TargetPoint(0L, 9000L, 0L, 0L, 5000L, 0L)));

        try {
            builder.entries().clear();
            fail("entry view must be immutable");
        } catch (UnsupportedOperationException expected) {
            assertFalse(builder.entries().isEmpty());
        }

        try {
            V118Biome.fromId("minecraft:not_a_real_biome");
            fail("unknown biome ids must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("not_a_real_biome"));
        }
    }

    private static void verifyEntry(String line, String[] fields,
            List<Climate.Entry<V118Biome>> entries) {
        int index = Integer.parseInt(fields[1]);
        assertEquals("ordered entry index", index, Integer.parseInt(fields[1]));
        Climate.Entry<V118Biome> entry = entries.get(index);
        Climate.ParameterPoint point = entry.parameters();
        verifyParameter(line, "temperature", fields, 2, point.temperature());
        verifyParameter(line, "humidity", fields, 4, point.humidity());
        verifyParameter(line, "continentalness", fields, 6, point.continentalness());
        verifyParameter(line, "erosion", fields, 8, point.erosion());
        verifyParameter(line, "depth", fields, 10, point.depth());
        verifyParameter(line, "weirdness", fields, 12, point.weirdness());
        assertEquals(message(line) + " offset", Long.parseLong(fields[14]), point.offset());
        assertEquals(message(line) + " biome", fields[15], entry.value().id());
    }

    private static void verifyParameter(String line, String axis, String[] fields, int offset,
            Climate.Parameter parameter) {
        assertEquals(message(line) + " " + axis + " min",
            Long.parseLong(fields[offset]), parameter.min());
        assertEquals(message(line) + " " + axis + " max",
            Long.parseLong(fields[offset + 1]), parameter.max());
    }

    private static String message(String line) {
        return "1.18.2 biome oracle mismatch in " + line;
    }
}

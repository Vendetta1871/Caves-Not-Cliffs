package net.celestiald.cavesnotcliffs.worldgen.v118;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class V118OreCatalogOracleTest {
    private static final String FIXTURE =
        "/net/celestiald/cavesnotcliffs/worldgen/v118/ore-catalog-oracle-1.18.2.tsv";

    @Test
    public void configuredPlacedFeaturesGlobalIndicesAndBiomeMembershipMatchOfficialServer()
            throws IOException {
        Map<String, V118OrePlacements.PlacedOre> byId =
            new HashMap<String, V118OrePlacements.PlacedOre>();
        for (V118OrePlacements.PlacedOre feature : V118OrePlacements.PlacedOre.values()) {
            byId.put(feature.placedId(), feature);
        }
        Set<String> catalogIds = new HashSet<String>();
        Set<V118Biome> biomeIds = new HashSet<V118Biome>();
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
                if ("catalog".equals(fields[0])) {
                    verifyCatalog(line, fields, byId.get(fields[1]));
                    catalogIds.add(fields[1]);
                } else if ("biome".equals(fields[0])) {
                    V118Biome biome = V118Biome.fromId(fields[1]);
                    Set<String> expected = new HashSet<String>(
                        java.util.Arrays.asList(fields[2].split(",")));
                    Set<String> actual = new HashSet<String>();
                    for (V118OrePlacements.PlacedOre feature
                            : V118OrePlacements.PlacedOre.values()) {
                        if (feature.belongsTo(biome)) {
                            actual.add(feature.placedId());
                        }
                    }
                    assertEquals(line, expected, actual);
                    biomeIds.add(biome);
                } else {
                    fail("unknown fixture row: " + line);
                }
            }
        }
        assertEquals(byId.keySet(), catalogIds);
        assertEquals("every 1.18.2 Overworld biome", V118Biome.values().length,
            biomeIds.size());
    }

    private static void verifyCatalog(String line, String[] fields,
            V118OrePlacements.PlacedOre feature) {
        assertNotNull(line, feature);
        assertEquals(line, fields[2], feature.configuredId());
        assertEquals(line, Integer.parseInt(fields[3]), feature.decorationStep());
        assertEquals(line, Integer.parseInt(fields[4]), feature.globalFeatureIndex());
        assertEquals(line, Integer.parseInt(fields[5]), feature.configuration().size());
        assertEquals(line, Integer.parseUnsignedInt(fields[6]), Float.floatToRawIntBits(
            feature.configuration().discardChanceOnAirExposure()));

        String[] count = fields[7].split(":", -1);
        assertEquals(line, count[0].toUpperCase(Locale.ROOT), feature.count().kind().name());
        assertEquals(line, Integer.parseInt(count[1]), feature.count().minimum());
        assertEquals(line, Integer.parseInt(count[2]), feature.count().maximum());

        String[] height = fields[8].split(":", -1);
        assertEquals(line, height[0].toUpperCase(Locale.ROOT), feature.height().kind().name());
        verifyAnchor(line, height[1], feature.height().minimum());
        verifyAnchor(line, height[2], feature.height().maximum());
        assertEquals(line, Integer.parseInt(height[3]), feature.height().plateau());

        String[] targets = fields[9].split(",", -1);
        List<V118OreFeature.Target> actualTargets = feature.configuration().targets();
        assertEquals(line, targets.length, actualTargets.size());
        for (int index = 0; index < targets.length; ++index) {
            String[] target = targets[index].split(">", -1);
            assertEquals(line, targetRule(target[0]), actualTargets.get(index).rule());
            assertEquals(line, material(target[1]), actualTargets.get(index).result());
        }
    }

    private static void verifyAnchor(String line, String encoded,
            V118OrePlacements.Anchor actual) {
        String[] parts = encoded.split(",", -1);
        assertEquals(line, parts[0].toUpperCase(Locale.ROOT), actual.kind().name());
        assertEquals(line, Integer.parseInt(parts[1]), actual.value());
    }

    private static V118OreFeature.TargetRule targetRule(String tag) {
        if ("base_stone_overworld".equals(tag)) {
            return V118OreFeature.TargetRule.NATURAL_STONE;
        }
        if ("stone_ore_replaceables".equals(tag)) {
            return V118OreFeature.TargetRule.STONE_ORE_REPLACEABLES;
        }
        if ("deepslate_ore_replaceables".equals(tag)) {
            return V118OreFeature.TargetRule.DEEPSLATE_ORE_REPLACEABLES;
        }
        throw new AssertionError("unexpected tag " + tag);
    }

    private static V118Material material(String id) {
        return V118Material.valueOf(id.toUpperCase(Locale.ROOT));
    }
}

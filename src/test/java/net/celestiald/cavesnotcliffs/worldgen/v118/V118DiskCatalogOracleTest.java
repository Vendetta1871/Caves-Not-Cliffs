package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class V118DiskCatalogOracleTest {
    private static final String FIXTURE =
            "/net/celestiald/cavesnotcliffs/worldgen/v118/disk-catalog-oracle-1.18.2.tsv";

    @Test
    public void configurationsGlobalIndicesAndBiomeMembershipMatchOfficialServer()
            throws IOException {
        Map<String, V118DiskPlacements.PlacedDisk> features = new HashMap<>();
        for (V118DiskPlacements.PlacedDisk feature
                : V118DiskPlacements.PlacedDisk.values()) {
            features.put(feature.id(), feature);
        }
        Set<String> catalogs = new HashSet<>();
        Set<V118Biome> biomes = EnumSet.noneOf(V118Biome.class);
        InputStream input = getClass().getResourceAsStream(FIXTURE);
        assertNotNull(FIXTURE, input);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input,
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] fields = line.split("\\t", -1);
                if ("catalog".equals(fields[0])) {
                    V118DiskPlacements.PlacedDisk feature = features.get(fields[1]);
                    assertNotNull(line, feature);
                    V118DiskFeature.Configuration config = feature.configuration();
                    assertEquals(line, 6, Integer.parseInt(fields[2]));
                    assertEquals(line, Integer.parseInt(fields[3]),
                            feature.globalFeatureIndex());
                    assertEquals(line, V118OreMaterial.valueOf(fields[4].toUpperCase()),
                            config.state());
                    assertEquals(line, Integer.parseInt(fields[5]), config.minimumRadius());
                    assertEquals(line, Integer.parseInt(fields[6]), config.maximumRadius());
                    assertEquals(line, Integer.parseInt(fields[7]), config.halfHeight());
                    assertEquals(line, Integer.parseInt(fields[8]), feature.count());
                    String[] targets = fields[9].split(",");
                    assertEquals(line, targets.length, config.targets().size());
                    for (int index = 0; index < targets.length; index++) {
                        assertEquals(line,
                                V118OreMaterial.valueOf(targets[index].toUpperCase()),
                                config.targets().get(index));
                    }
                    catalogs.add(fields[1]);
                } else {
                    V118Biome biome = V118Biome.fromId(fields[1]);
                    Set<String> expected = new HashSet<>(Arrays.asList(fields[2].split(",")));
                    Set<String> actual = new HashSet<>();
                    for (V118DiskPlacements.PlacedDisk feature
                            : V118DiskPlacements.PlacedDisk.values()) {
                        if (feature.belongsTo(biome)) {
                            actual.add(feature.id());
                        }
                    }
                    assertEquals(line, expected, actual);
                    biomes.add(biome);
                }
            }
        }
        assertEquals(features.keySet(), catalogs);
        assertEquals(V118Biome.values().length, biomes.size());
    }
}

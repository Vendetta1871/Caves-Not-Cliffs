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
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class V118SurfaceSystemPrimitiveOracleTest {
    private static final String FIXTURE =
        "/net/celestiald/cavesnotcliffs/worldgen/v118/surface-primitives-oracle-1.18.2.tsv";

    @Test
    public void seededDepthSecondaryNoiseAndClayBandsMatchOfficialServer() throws IOException {
        Map<Long, V118SurfaceSystem> systems = new HashMap<Long, V118SurfaceSystem>();
        Set<Long> seeds = new HashSet<Long>();
        int depthRows = 0;
        int bandRows = 0;
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
                long seed = Long.parseLong(fields[1]);
                seeds.add(seed);
                V118SurfaceSystem system = systems.get(seed);
                if (system == null) {
                    system = new V118SurfaceSystem(seed);
                    systems.put(seed, system);
                }
                int x = Integer.parseInt(fields[2]);
                if ("depth".equals(fields[0])) {
                    int z = Integer.parseInt(fields[3]);
                    assertEquals(line, Integer.parseInt(fields[4]), system.getSurfaceDepth(x, z));
                    assertEquals(line, Long.parseUnsignedLong(fields[5]),
                        Double.doubleToRawLongBits(system.getSurfaceSecondary(x, z)));
                    ++depthRows;
                } else if ("band".equals(fields[0])) {
                    int y = Integer.parseInt(fields[3]);
                    int z = Integer.parseInt(fields[4]);
                    assertEquals(line, material(fields[5]), system.getBand(x, y, z));
                    ++bandRows;
                } else {
                    fail("unknown fixture record: " + line);
                }
            }
        }
        assertEquals("edge seed coverage", 6, seeds.size());
        assertEquals("depth rows", 60, depthRows);
        assertEquals("band rows", 840, bandRows);
    }

    private static V118Material material(String id) {
        String path = id.substring("minecraft:".length());
        return V118Material.valueOf(path.toUpperCase(java.util.Locale.ROOT));
    }
}

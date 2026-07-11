package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TerrainShaperOracleTest {
    private static final String FIXTURE =
        "/net/celestiald/cavesnotcliffs/worldgen/v118/terrain-shaper-oracle-1.18.2.tsv";

    @Test
    public void matchesOfficialOverworldTerrainShapersBitForBit() throws IOException {
        TerrainShaper normal = TerrainShaper.overworld(TerrainShaper.Profile.NORMAL);
        TerrainShaper amplified = TerrainShaper.overworld(TerrainShaper.Profile.AMPLIFIED);
        int samples = 0;
        int peaks = 0;
        int madePoints = 0;
        int assertions = 0;

        InputStream input = TerrainShaperOracleTest.class.getResourceAsStream(FIXTURE);
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
                    case "peaks": {
                        float weirdness = fromBits(fields[1]);
                        assertBits(message(line, 2), fields[2],
                            TerrainShaper.peaksAndValleys(weirdness));
                        ++peaks;
                        ++assertions;
                        break;
                    }
                    case "make": {
                        TerrainShaper.Point point = TerrainShaper.makePoint(fromBits(fields[1]),
                            fromBits(fields[2]), fromBits(fields[3]));
                        assertBits(message(line, 4), fields[4], point.ridges());
                        assertOutputs(line, fields, 5, point, normal, amplified);
                        ++madePoints;
                        assertions += 7;
                        break;
                    }
                    case "sample": {
                        assertEquals("fixture sample sequence", samples,
                            Integer.parseInt(fields[1]));
                        TerrainShaper.Point point = new TerrainShaper.Point(fromBits(fields[2]),
                            fromBits(fields[3]), fromBits(fields[4]), fromBits(fields[5]));
                        assertOutputs(line, fields, 6, point, normal, amplified);
                        ++samples;
                        assertions += 6;
                        break;
                    }
                    case "count":
                        assertEquals("fixture sample count", samples, Integer.parseInt(fields[1]));
                        break;
                    default:
                        fail("Unknown fixture record: " + line);
                }
            }
        }

        assertEquals("boundary and scatter samples", 992, samples);
        assertEquals("peaks-and-valleys samples", 43, peaks);
        assertEquals("makePoint samples", 16, madePoints);
        assertEquals("raw-bit assertions", 6107, assertions);
    }

    @Test
    public void exposesStableProfileAndCoordinateContracts() {
        assertEquals(TerrainShaper.Profile.NORMAL, TerrainShaper.Profile.byName("normal"));
        assertEquals(TerrainShaper.Profile.AMPLIFIED, TerrainShaper.Profile.byName("AMPLIFIED"));
        assertEquals("normal", TerrainShaper.Profile.NORMAL.serializedName());
        assertEquals("amplified", TerrainShaper.Profile.AMPLIFIED.serializedName());

        TerrainShaper.Point point = TerrainShaper.makePoint(-0.25F, 0.5F, -0.75F);
        assertBits("continents coordinate", bits(-0.25F),
            TerrainShaper.Coordinate.CONTINENTS.apply(point));
        assertBits("erosion coordinate", bits(0.5F),
            TerrainShaper.Coordinate.EROSION.apply(point));
        assertBits("weirdness coordinate", bits(-0.75F),
            TerrainShaper.Coordinate.WEIRDNESS.apply(point));
        assertBits("ridges coordinate", bits(point.ridges()),
            TerrainShaper.Coordinate.RIDGES.apply(point));

        TerrainShaper normal = TerrainShaper.overworld(false);
        TerrainShaper amplified = TerrainShaper.overworld(true);
        assertEquals(TerrainShaper.Profile.NORMAL, normal.profile());
        assertEquals(TerrainShaper.Profile.AMPLIFIED, amplified.profile());
        assertTrue(normal.offsetSampler() instanceof CubicSpline.Multipoint);
        assertTrue(normal.factorSampler() instanceof CubicSpline.Multipoint);
        assertTrue(normal.jaggednessSampler() instanceof CubicSpline.Multipoint);
        assertEquals(10, ((CubicSpline.Multipoint<?>) normal.offsetSampler()).locations().length);
        assertEquals(5, ((CubicSpline.Multipoint<?>) normal.factorSampler()).locations().length);
        assertEquals(3, ((CubicSpline.Multipoint<?>) normal.jaggednessSampler()).locations().length);

        try {
            TerrainShaper.Profile.byName("customized");
            fail("unknown profile must be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals("Unknown terrain profile: customized", expected.getMessage());
        }
    }

    private static void assertOutputs(String line, String[] fields, int offset,
            TerrainShaper.Point point, TerrainShaper normal, TerrainShaper amplified) {
        assertBits(message(line, offset), fields[offset], normal.offset(point));
        assertBits(message(line, offset + 1), fields[offset + 1], normal.factor(point));
        assertBits(message(line, offset + 2), fields[offset + 2], normal.jaggedness(point));
        assertBits(message(line, offset + 3), fields[offset + 3], amplified.offset(point));
        assertBits(message(line, offset + 4), fields[offset + 4], amplified.factor(point));
        assertBits(message(line, offset + 5), fields[offset + 5], amplified.jaggedness(point));
    }

    private static float fromBits(String unsignedBits) {
        return Float.intBitsToFloat((int) Long.parseUnsignedLong(unsignedBits));
    }

    private static String bits(float value) {
        return Integer.toUnsignedString(Float.floatToRawIntBits(value));
    }

    private static void assertBits(String message, String expectedBits, float actual) {
        assertEquals(message, Long.parseUnsignedLong(expectedBits),
            Integer.toUnsignedLong(Float.floatToRawIntBits(actual)));
    }

    private static String message(String line, int field) {
        return "oracle mismatch at field " + field + " in " + line;
    }
}

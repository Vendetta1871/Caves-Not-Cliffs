package net.celestiald.cavesnotcliffs.worldgen.v118;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class V118NoiseSettingsOracleTest {
    private static final String FIXTURE =
        "/net/celestiald/cavesnotcliffs/worldgen/v118/noise-settings-oracle-1.18.2.tsv";

    @Test
    public void settingsAndSlidesMatchOfficialMinecraft1182BitForBit() throws IOException {
        int settingsRecords = 0;
        int slideRecords = 0;
        for (String line : fixtureLines()) {
            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }
            String[] fields = line.split("\\t", -1);
            V118NoiseSettings settings = settings(fields[1]);
            if ("settings".equals(fields[0])) {
                assertEquals(message(line, 2), Integer.parseInt(fields[2]), settings.minY());
                assertEquals(message(line, 3), Integer.parseInt(fields[3]), settings.height());
                assertBits(message(line, 4), fields[4], settings.sampling().xzScale());
                assertBits(message(line, 5), fields[5], settings.sampling().yScale());
                assertBits(message(line, 6), fields[6], settings.sampling().xzFactor());
                assertBits(message(line, 7), fields[7], settings.sampling().yFactor());
                assertBits(message(line, 8), fields[8], settings.topSlide().target());
                assertEquals(message(line, 9), Integer.parseInt(fields[9]), settings.topSlide().size());
                assertEquals(message(line, 10), Integer.parseInt(fields[10]), settings.topSlide().offset());
                assertBits(message(line, 11), fields[11], settings.bottomSlide().target());
                assertEquals(message(line, 12), Integer.parseInt(fields[12]), settings.bottomSlide().size());
                assertEquals(message(line, 13), Integer.parseInt(fields[13]), settings.bottomSlide().offset());
                assertEquals(message(line, 14), Integer.parseInt(fields[14]), settings.noiseSizeHorizontal());
                assertEquals(message(line, 15), Integer.parseInt(fields[15]), settings.noiseSizeVertical());
                assertEquals(message(line, 16), Integer.parseInt(fields[16]), settings.getCellHeight());
                assertEquals(message(line, 17), Integer.parseInt(fields[17]), settings.getCellWidth());
                assertEquals(message(line, 18), Integer.parseInt(fields[18]), settings.getCellCountY());
                assertEquals(message(line, 19), Integer.parseInt(fields[19]), settings.getMinCellY());
                ++settingsRecords;
            } else if ("slide".equals(fields[0])) {
                assertBits(message(line, 4), fields[4], settings.applySlide(
                    Double.parseDouble(fields[2]), Double.parseDouble(fields[3])));
                ++slideRecords;
            } else {
                fail("Unknown fixture record: " + line);
            }
        }
        assertEquals(2, settingsRecords);
        assertEquals(144, slideRecords);
    }

    @Test
    public void profilesExposeTheExactFiniteOverworldRange() {
        V118NoiseSettings normal = V118NoiseSettings.overworld(false);
        V118NoiseSettings amplified = V118NoiseSettings.overworld(true);
        assertEquals(-64, normal.minY());
        assertEquals(320, normal.maxYExclusive());
        assertEquals(384, normal.height());
        assertFalse(normal.amplified());
        assertTrue(amplified.amplified());
        assertEquals(4, normal.getCellWidth());
        assertEquals(8, normal.getCellHeight());
        assertEquals(48, normal.getCellCountY());
        assertEquals(-8, normal.getMinCellY());
    }

    private static V118NoiseSettings settings(String name) {
        if ("normal".equals(name)) {
            return V118NoiseSettings.overworld(false);
        }
        if ("amplified".equals(name)) {
            return V118NoiseSettings.overworld(true);
        }
        throw new AssertionError("Unknown settings profile: " + name);
    }

    private static List<String> fixtureLines() throws IOException {
        InputStream input = V118NoiseSettingsOracleTest.class.getResourceAsStream(FIXTURE);
        assertNotNull("missing fixture " + FIXTURE, input);
        List<String> lines = new ArrayList<String>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static void assertBits(String message, String expectedUnsignedBits, double actual) {
        assertEquals(message, Long.parseUnsignedLong(expectedUnsignedBits),
            Double.doubleToRawLongBits(actual));
    }

    private static String message(String line, int field) {
        return "oracle mismatch at field " + field + " in " + line;
    }
}

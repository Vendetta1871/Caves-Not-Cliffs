package net.celestiald.cavesnotcliffs.compat;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

public class NoiseThreaderConflictTest {
    @Test
    public void theRefusalNamesBothModsAndTheRemedy() {
        assertTrue(NoiseThreaderConflict.TITLE.contains("Caves Not Cliffs"));
        assertTrue(NoiseThreaderConflict.TITLE.contains("NoiseThreader"));
        String text = String.join(" ", NoiseThreaderConflict.messageLines());
        assertTrue(text.contains(NoiseThreaderConflict.MODID));
        assertTrue(text.contains("already includes the speedups"));
        assertTrue(text.contains("Remove NoiseThreader"));
    }

    @Test
    public void everyScreenLineFitsTheErrorScreenWidth() {
        List<String> lines = NoiseThreaderConflict.messageLines();
        assertTrue(lines.size() >= 2);
        for (String line : lines) {
            assertTrue("too wide: " + line, line.length() <= 76);
        }
    }
}

package net.celestiald.cavesnotcliffs.world;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LegacySchemaOnePopulationContractTest {
    @Test
    public void schemaOneGeneratorRetainsItsDecoratorWithoutLegacyStateIds() throws IOException {
        String generatorResource = '/' + LegacyFiniteChunkGenerator.class.getName()
                .replace('.', '/') + ".class";
        InputStream generatorInput = LegacyFiniteChunkGenerator.class
                .getResourceAsStream(generatorResource);
        assertNotNull(generatorInput);

        String generator = new String(readFully(generatorInput),
                StandardCharsets.ISO_8859_1);
        assertTrue(generator.contains("CaveBiomeDecorator"));

        String decoratorResource = '/' + CaveBiomeDecorator.class.getName()
                .replace('.', '/') + ".class";
        InputStream decoratorInput = CaveBiomeDecorator.class
                .getResourceAsStream(decoratorResource);
        assertNotNull(decoratorInput);

        String decorator = new String(readFully(decoratorInput),
                StandardCharsets.ISO_8859_1);
        assertFalse(decorator.contains("BlockGlowBerryVines"));
        assertFalse(decorator.contains("BlockGlowBerryMiddleFill"));
        assertFalse(decorator.contains("BlockBabyDripleaf"));
        assertFalse(decorator.contains("BlockDripleafPlant"));
        assertTrue(decorator.contains("LushCaveVinesBlock"));
        assertTrue(decorator.contains("LushDripleafBlocks"));
        assertTrue(decorator.contains("LushCaveContent"));
        assertTrue(decorator.contains("BlockPointedDripstone"));
    }

    @Test
    public void canonicalVinesConsumeTheDraftV2RandomSchedule() {
        CountingRandom random = new CountingRandom();
        assertFalse(CaveBiomeDecorator.legacyVineBerryRoll(random, 0, 4));
        assertFalse(CaveBiomeDecorator.legacyVineBerryRoll(random, 1, 4));
        assertFalse(CaveBiomeDecorator.legacyVineBerryRoll(random, 2, 4));
        assertTrue(CaveBiomeDecorator.legacyVineBerryRoll(random, 3, 4));
        assertTrue(CaveBiomeDecorator.legacyVineBerryRoll(random, 0, 1));
        assertFalse(CaveBiomeDecorator.legacyVineBerryRoll(random, 0, 2));
        assertEquals(4, random.calls);
    }

    private static byte[] readFully(InputStream input) throws IOException {
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private static final class CountingRandom extends Random {
        private int calls;

        @Override
        public double nextDouble() {
            calls++;
            return 0.5D;
        }
    }
}

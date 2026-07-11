package net.celestiald.cavesnotcliffs.world;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class LegacySchemaOnePopulationContractTest {
    @Test
    public void schemaOneGeneratorCannotInvokeTheRemovedAliasDecorator() throws IOException {
        String resource = '/' + CavesNotCliffsCubeGenerator.class.getName()
                .replace('.', '/') + ".class";
        InputStream input = CavesNotCliffsCubeGenerator.class.getResourceAsStream(resource);
        assertNotNull(input);

        String classFile = new String(readFully(input), StandardCharsets.ISO_8859_1);
        assertFalse(classFile.contains("CaveBiomeDecorator"));
        assertFalse(classFile.contains("glow_berry_vines"));
        assertFalse(classFile.contains("glow_berry_middle_fill"));
        assertFalse(classFile.contains("baby_dripleaf"));
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
}

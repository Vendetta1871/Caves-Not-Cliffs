package net.celestiald.cavesnotcliffs.config;

import net.minecraftforge.common.config.Config;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CavesNotCliffsConfigTest {
    @Test
    public void worldGenerationIsEnabledByDefault() {
        assertTrue(new CavesNotCliffsConfig.World().enableForNewOverworlds);
    }

    @Test
    public void annotationProducesTheDocumentedFileAndKey() throws ReflectiveOperationException {
        Config config = CavesNotCliffsConfig.class.getAnnotation(Config.class);
        assertEquals("cavesnotcliffs", config.name());

        Field world = CavesNotCliffsConfig.class.getField("WORLD");
        assertEquals("world", world.getAnnotation(Config.Name.class).value());
        Field toggle = CavesNotCliffsConfig.World.class.getField("enableForNewOverworlds");
        assertEquals("enableForNewOverworlds", toggle.getAnnotation(Config.Name.class).value());
    }
}

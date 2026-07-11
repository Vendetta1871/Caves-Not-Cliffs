package net.celestiald.cavesnotcliffs.world;

import net.minecraft.world.gen.ChunkGeneratorSettings;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class VanillaStructureBridgeTest {
    @Test
    public void defaultSettingsPreserveTheSixFamiliesInVanillaGenerationOrder() {
        ChunkGeneratorSettings settings = defaults().build();
        assertEquals(Arrays.asList("mineshaft", "village", "stronghold", "temple",
            "monument", "mansion"),
            VanillaStructureBridge.enabledStructureNames(true, settings));
    }

    @Test
    public void generatorOptionsAndGlobalMapFeatureSwitchRemainAuthoritative() {
        ChunkGeneratorSettings.Factory factory = defaults();
        factory.useMineShafts = false;
        factory.useVillages = true;
        factory.useStrongholds = false;
        factory.useTemples = true;
        factory.useMonuments = false;
        factory.useMansions = true;
        ChunkGeneratorSettings settings = factory.build();

        assertEquals(Arrays.asList("village", "temple", "mansion"),
            VanillaStructureBridge.enabledStructureNames(true, settings));
        assertEquals(Collections.emptyList(),
            VanillaStructureBridge.enabledStructureNames(false, settings));
    }

    @Test
    public void mcpDevelopmentFieldsResolveAndSrgFallbacksAreDeclared() {
        VanillaStructureBridge.verifyReflectionContracts();
    }

    @Test
    public void rejectsMissingSettingsInsteadOfSilentlyEnablingStructures() {
        try {
            VanillaStructureBridge.enabledStructureNames(true, null);
            fail("Expected missing settings rejection");
        } catch (NullPointerException expected) {
            assertEquals("settings", expected.getMessage());
        }
    }

    private static ChunkGeneratorSettings.Factory defaults() {
        ChunkGeneratorSettings.Factory factory = new ChunkGeneratorSettings.Factory();
        factory.setDefaults();
        return factory;
    }
}

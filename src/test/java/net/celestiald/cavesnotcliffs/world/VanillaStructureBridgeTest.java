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
    public void structurePopulationUsesVanillasOddMultiplierChunkSeed() {
        long[] worldSeeds = {
            0L, 1L, -1L, 123456789L, Long.MIN_VALUE, Long.MAX_VALUE
        };
        int[][] chunks = {
            {0, 0}, {1, -1}, {-17, 31}, {12345, -98765},
            {Integer.MIN_VALUE, Integer.MAX_VALUE}, {33554432, -33554433}
        };
        long[] expected = {
            0L, 5917667254063506247L, 3535419369914194609L,
            -5396765933884790277L, 2710116157784231745L, 5286005299613532062L
        };
        for (int index = 0; index < worldSeeds.length; ++index) {
            assertEquals("seed=" + worldSeeds[index], expected[index],
                VanillaStructureBridge.populationSeed(worldSeeds[index], chunks[index][0],
                    chunks[index][1]));
        }
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

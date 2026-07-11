package net.celestiald.cavesnotcliffs.content;

import net.minecraft.util.ResourceLocation;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ComposterMechanicsTest {
    @Test
    public void matchesEveryLevelAndProbabilityBoundary() {
        for (int level = 0; level <= 8; ++level) {
            for (float chance : new float[]{-1.0F, 0.0F, 0.30F, 0.50F, 0.65F,
                    0.85F, 1.0F}) {
                int expectedAtZero = level < 7 && chance > 0.0F ? level + 1 : level;
                assertEquals(expectedAtZero, ComposterMechanics.addItem(level, chance, 0.0D));

                double atChance = chance > 0.0F && chance < 1.0F ? chance : 0.999999D;
                boolean succeedsAtChance = level < 7 && chance > 0.0F
                    && (level == 0 || atChance < chance);
                int expectedAtChance = succeedsAtChance ? level + 1 : level;
                assertEquals(expectedAtChance,
                    ComposterMechanics.addItem(level, chance, atChance));
            }
            assertEquals(level == 7 ? 8 : level, ComposterMechanics.mature(level));
            assertEquals(level, ComposterMechanics.comparatorOutput(level));
        }
    }

    @Test
    public void emptyComposterAlwaysAcceptsFirstValidItem() {
        assertEquals(1, ComposterMechanics.addItem(0, 0.30F, 0.999999D));
        assertEquals(0, ComposterMechanics.addItem(0, 0.0F, 0.0D));
        assertEquals(0, ComposterMechanics.addItem(0, -1.0F, 0.0D));
    }

    @Test
    public void levelSevenAndReadyCompostersRejectInput() {
        assertFalse(ComposterMechanics.acceptsInput(7, 1.0F));
        assertFalse(ComposterMechanics.acceptsInput(8, 1.0F));
        assertEquals(7, ComposterMechanics.addItem(7, 1.0F, 0.0D));
        assertEquals(8, ComposterMechanics.addItem(8, 1.0F, 0.0D));
    }

    @Test
    public void catalogHasUniqueExact118ChancesForRepresentedItems() {
        Set<String> keys = new HashSet<>();
        for (ComposterCompostables.Definition definition
                : ComposterCompostables.definitions()) {
            assertTrue(keys.add(definition.itemId + '#' + definition.metadata));
            assertTrue(definition.chance == 0.30F || definition.chance == 0.50F
                || definition.chance == 0.65F || definition.chance == 0.85F
                || definition.chance == 1.0F);
        }
        assertEquals(52, keys.size());

        assertChance("cavesnotcliffs:glow_berries", 0, 0.30F);
        assertChance("cavesnotcliffs:small_dripleaf", 0, 0.30F);
        assertChance("cavesnotcliffs:big_dripleaf", 0, 0.65F);
        assertChance("cavesnotcliffs:flowering_azalea", 0, 0.85F);
        assertChance("minecraft:tallgrass", 1, 0.30F);
        assertChance("minecraft:tallgrass", 2, 0.65F);
        assertChance("minecraft:double_plant", 2, 0.50F);
        assertChance("minecraft:double_plant", 3, 0.65F);
        assertChance("minecraft:dye", 3, 0.65F);
        assertChance("minecraft:dye", 4, ComposterCompostables.NOT_COMPOSTABLE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidLevel() {
        ComposterMechanics.addItem(9, 0.30F, 0.0D);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidRandomUnit() {
        ComposterMechanics.addItem(0, 0.30F, 1.0D);
    }

    private static void assertChance(String id, int metadata, float expected) {
        assertEquals(expected,
            ComposterCompostables.chance(new ResourceLocation(id), metadata), 0.0F);
    }
}

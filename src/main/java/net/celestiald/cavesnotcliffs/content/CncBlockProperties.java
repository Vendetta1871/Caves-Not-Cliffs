package net.celestiald.cavesnotcliffs.content;

import net.minecraft.block.material.MapColor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/** Numeric block properties translated from the Java 1.18.2 block API to 1.12.2. */
public final class CncBlockProperties {
    /**
     * 1.12 stores {@code setResistance(value) * 3} and exposes that value divided by five.
     * Passing a modern resistance directly would therefore make every block 40% too fragile.
     */
    public static float legacyResistance(float modernResistance) {
        return modernResistance * 5.0F / 3.0F;
    }

    /** Java 1.18's stable material-color slots, absent from the 1.12 palette. */
    public static final MapColor DEEPSLATE = registerMapColor(59, 0x646464);
    public static final MapColor RAW_IRON = registerMapColor(60, 0xD8AF93);

    private CncBlockProperties() {
    }

    private static MapColor registerMapColor(int index, int color) {
        MapColor existing = MapColor.COLORS[index];
        if (existing != null) {
            if (existing.colorValue != color) {
                throw new IllegalStateException("Map-color slot " + index
                        + " is already occupied by color 0x"
                        + Integer.toHexString(existing.colorValue));
            }
            return existing;
        }

        try {
            Constructor<MapColor> constructor = MapColor.class
                    .getDeclaredConstructor(int.class, int.class);
            constructor.setAccessible(true);
            return constructor.newInstance(index, color);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException
                | InvocationTargetException exception) {
            throw new IllegalStateException("Could not install Java 1.18 map color " + index,
                    exception);
        }
    }
}

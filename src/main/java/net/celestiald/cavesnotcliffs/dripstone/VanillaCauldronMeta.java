package net.celestiald.cavesnotcliffs.dripstone;

import net.celestiald.cavesnotcliffs.dripstone.CauldronMechanics.Content;
import net.celestiald.cavesnotcliffs.dripstone.CauldronMechanics.State;

/**
 * Metadata encoding for the 1.18.2 cauldron contents stored on the vanilla cauldron block by
 * {@code CauldronMixin}. Kept block-free so both the mixin and the legacy migration can share it
 * and unit tests can run without Mixin applied.
 *
 * <ul>
 *   <li>meta 0-3: empty / layered water (level = meta)</li>
 *   <li>meta 4-7: full lava (level is always {@link CauldronMechanics#MAX_LEVEL}; encoded as 7,
 *       matching the legacy hidden lava cauldron's metadata for a seamless migration)</li>
 *   <li>meta 8-15: powder snow (level = meta - 7, clamped to 1-3)</li>
 * </ul>
 */
public final class VanillaCauldronMeta {
    public static final int LAVA_META = 7;
    public static final int POWDER_SNOW_BASE_META = 8;

    private VanillaCauldronMeta() {
    }

    public static int toMeta(State contents) {
        if (contents == null) {
            throw new IllegalArgumentException("Cauldron contents are required");
        }
        switch (contents.content) {
            case EMPTY:
                return 0;
            case WATER:
                return contents.level;
            case LAVA:
                return LAVA_META;
            case POWDER_SNOW:
                return POWDER_SNOW_BASE_META + contents.level - 1;
            default:
                throw new AssertionError(contents.content);
        }
    }

    public static State fromMeta(int meta) {
        if (meta < 0 || meta > 15) {
            throw new IllegalArgumentException("Cauldron metadata must be within 0-15: " + meta);
        }
        if (meta < 4) {
            return meta == 0 ? CauldronMechanics.empty() : CauldronMechanics.water(meta);
        }
        if (meta < POWDER_SNOW_BASE_META) {
            return CauldronMechanics.lava();
        }
        int level = Math.max(1, Math.min(CauldronMechanics.MAX_LEVEL,
                meta - POWDER_SNOW_BASE_META + 1));
        return new State(Content.POWDER_SNOW, level);
    }
}
